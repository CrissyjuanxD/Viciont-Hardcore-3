package Bosses;

import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public abstract class BaseBoss {

    protected final JavaPlugin plugin;
    protected final LivingEntity entity;
    protected final Location spawnLocation;

    // ---- Sistema de Jugadores ----
    protected final Set<UUID> currentPlayers = new HashSet<>();

    // ---- Sistema de Arena ----
    protected AreaZone areaZone;
    // ---- Debug ----
    private boolean debugEnabled = false;
    private final Set<UUID> debugPlayers = new HashSet<>();
    private int debugTick = 0;

    // ---- BossBars ----
    protected BossBar mainBar;       // barra normal (rosa)
    protected BossBar staticBar;     // barra estática (blanca, solo números)

    private BukkitRunnable tickTask;
    private boolean initialized = false;

    public BaseBoss(JavaPlugin plugin, LivingEntity entity) {
        this.plugin = plugin;
        this.entity = entity;
        this.spawnLocation = entity.getLocation().clone();
    }

    // ===========================
    //          INIT
    // ===========================
    public final void start() {
        if (initialized) return;
        initialized = true;

        setupBars();
        setupArena();
        detectInitialPlayers();
        sendSummonMessage();
        onStart();

        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isDead() || !entity.isValid()) {
                    cancel();
                    onDeath();
                    cleanupBars();
                    sendDeathMessage();
                    return;
                }

                updateBars();
                updatePlayers();
                debugArenaTick();
                onTick();
            }
        };
        tickTask.runTaskTimer(plugin, 1L, 1L);
    }

    // ===========================
    //         BOSSBARS
    // ===========================
    private void setupBars() {
        mainBar = Bukkit.createBossBar(getBossTitle(), BarColor.PINK, BarStyle.SOLID);
        staticBar = Bukkit.createBossBar(" ", BarColor.WHITE, BarStyle.SOLID);
        staticBar.setProgress(1.0);
    }

    private void updateBars() {
        double max = entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        double hp = Math.max(0, entity.getHealth());

        mainBar.setProgress(hp / max);
        staticBar.setTitle(ChatColor.WHITE + "> " + (int) hp + " <");
    }

    private void cleanupBars() {
        mainBar.removeAll();
        staticBar.removeAll();
    }

    // ===========================
    //          ARENA
    // ===========================
    private void setupArena() {
        this.areaZone = new AreaZone(
                spawnLocation,
                getArenaRadius(),
                getArenaHeightUp(),
                getArenaHeightDown(),
                getArenaShape()
        );
    }

    private void updatePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean inside = areaZone.isInside(player.getLocation());
            boolean already = currentPlayers.contains(player.getUniqueId());

            if (inside && !already) {
                currentPlayers.add(player.getUniqueId());
                mainBar.addPlayer(player);
                staticBar.addPlayer(player);
            }
            if (!inside && already) {
                currentPlayers.remove(player.getUniqueId());
                mainBar.removePlayer(player);
                staticBar.removePlayer(player);
            }
        }
    }

    private void detectInitialPlayers() {
        for (Player p : entity.getWorld().getPlayers()) {
            if (areaZone.isInside(p.getLocation())) {
                currentPlayers.add(p.getUniqueId());
                mainBar.addPlayer(p);
                staticBar.addPlayer(p);
            }
        }
    }

    public void toggleDebug(Player player) {
        if (debugPlayers.contains(player.getUniqueId())) {
            debugPlayers.remove(player.getUniqueId());
            player.sendMessage("§cDebug de arena desactivado.");
        } else {
            debugPlayers.add(player.getUniqueId());
            player.sendMessage("§aDebug de arena activado.");
            player.sendMessage("§7Se mostrarán los bordes y avisos de entrada/salida.");
        }
    }

    private void debugArenaTick() {
        if (debugPlayers.isEmpty()) return;

        debugTick++;

        // cada 5 ticks dibujamos borde
        if (debugTick % 5 == 0) {
            for (UUID id : debugPlayers) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    areaZone.debug(p);
                }
            }
        }

        // ahora avisamos entrada/salida
        for (UUID id : debugPlayers) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;

            boolean inside = areaZone.isInside(p.getLocation());

            if (inside && !p.hasMetadata("DEBUG_ARENA_INSIDE")) {
                p.setMetadata("DEBUG_ARENA_INSIDE", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                p.sendMessage("§a[DEBUG] §fEntraste a la arena.");
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
            }

            if (!inside && p.hasMetadata("DEBUG_ARENA_INSIDE")) {
                p.removeMetadata("DEBUG_ARENA_INSIDE", plugin);
                p.sendMessage("§c[DEBUG] §fSaliste de la arena.");
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            }
        }
    }


    // ===========================
    //      MENSAJES GLOBALES
    // ===========================
    private void sendSummonMessage() {
        if (currentPlayers.isEmpty()) return;

        List<String> names = currentPlayers.stream()
                .map(id -> Bukkit.getPlayer(id) != null ? Bukkit.getPlayer(id).getName() : "")
                .toList();

        String msg;

        if (names.size() == 1) {
            msg = ChatColor.LIGHT_PURPLE + "El jugador " + names.get(0) + " ha invocado a " + getBossTitle();
        } else {
            msg = ChatColor.LIGHT_PURPLE + "Los jugadores " + ChatColor.YELLOW +
                    String.join(", ", names) +
                    ChatColor.LIGHT_PURPLE + " han invocado a " + getBossTitle();
        }

        Bukkit.broadcastMessage(msg);
    }

    private void sendDeathMessage() {
        List<String> names = currentPlayers.stream()
                .map(id -> Bukkit.getPlayer(id) != null ? Bukkit.getPlayer(id).getName() : "")
                .toList();

        if (names.isEmpty()) return;

        String msg;

        if (names.size() == 1) {
            msg = ChatColor.GOLD + names.get(0) + " ha derrotado a " + getBossTitle();
        } else {
            msg = ChatColor.GOLD + "Los jugadores " + ChatColor.YELLOW +
                    String.join(", ", names) +
                    ChatColor.GOLD + " han derrotado a " + getBossTitle();
        }

        Bukkit.broadcastMessage(msg);
    }

    // ===========================
    //         ABSTRACTOS
    // ===========================
    protected abstract String getBossTitle();     // nombre del boss
    protected abstract int getArenaRadius();
    protected abstract int getArenaHeightUp();
    protected abstract int getArenaHeightDown();
    protected abstract AreaZone.Shape getArenaShape(); // tipo arena (CUADRADA / CIRCULAR)

    protected abstract void onStart();    // cuando inicia
    protected abstract void onTick();     // cada tick
    protected abstract void onDeath();    // cuando muere

}
