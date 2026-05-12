package Bosses;

import org.bukkit.Bukkit;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class BaseBoss {

    protected final JavaPlugin plugin;
    protected final LivingEntity entity;
    protected final Location spawnLocation;

    // ---- Sistema de Jugadores ----
    protected final Set<UUID> currentPlayers = new HashSet<>();
    protected final Set<UUID> attackers = new HashSet<>();

    // ---- Sistema de Arena ----
    protected AreaZone areaZone;

    // ---- Estado ----
    private boolean hibernating = false; // Nuevo estado

    // ---- Debug ----
    private final Set<UUID> debugPlayers = new HashSet<>();
    private int debugTick = 0;

    // ---- BossBars ----
    protected BossBar mainBar;
    protected BossBar staticBar;

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
                // 1. VALIDACIÓN DE ESTADO (CORREGIDO)
                if (!entity.isValid() || entity.isDead()) {
                    cancel();
                    cleanupBars();

                    if (entity.getHealth() <= 0 || entity.isDead()) {
                        onDeath();
                        sendDeathMessage();
                    }
                    else {
                        onUnload();
                    }
                    return;
                }

                // 2. Actualizar jugadores
                updatePlayers();
                updateBars();

                // 3. Hibernación
                if (currentPlayers.isEmpty()) {
                    if (!hibernating) {
                        enterHibernation();
                    }
                } else {
                    if (hibernating) {
                        exitHibernation();
                    }

                    if (entity.isInvulnerable()) {
                        entity.setInvulnerable(false);
                    }
                }

                // 4. Anti-Escape
                if (!areaZone.isInside(entity.getLocation())) {
                    entity.teleport(spawnLocation);
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 3f, 0.8f);
                }

                // 5. Tick
                debugArenaTick();
                onTick();
            }
        };
        tickTask.runTaskTimer(plugin, 1L, 1L);
    }

    // ===========================
    //      HIBERNACIÓN
    // ===========================
    private void enterHibernation() {
        hibernating = true;
        entity.setAI(false);
        entity.setInvulnerable(true);
        entity.setSilent(true);

        mainBar.removeAll();
        staticBar.removeAll();
    }

    private void exitHibernation() {
        hibernating = false;
        entity.setAI(true);
        entity.setInvulnerable(false);
        entity.setSilent(false);

        for (UUID uuid : currentPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                mainBar.addPlayer(p);
                staticBar.addPlayer(p);
            }
        }
    }

    public boolean isHibernating() {
        return hibernating;
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
        double max = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
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
                if (!hibernating) {
                    mainBar.addPlayer(player);
                    staticBar.addPlayer(player);
                }
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

    public void addAttacker(Player p) {
        if (p.getGameMode() == org.bukkit.GameMode.SURVIVAL || p.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
            attackers.add(p.getUniqueId());
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

        if (debugTick % 10 == 0) {
            for (UUID id : debugPlayers) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    areaZone.debug(p);
                }
            }
        }

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
                .map(id -> Bukkit.getPlayer(id))
                .filter(p -> p != null && (p.getGameMode() == org.bukkit.GameMode.SURVIVAL || p.getGameMode() == org.bukkit.GameMode.ADVENTURE))
                .map(Player::getName)
                .toList();

        if (names.isEmpty()) return;

        String prefix = ChatColor.of("#88F1BC") + "" + ChatColor.BOLD + "\u06de";
        String colorText = ChatColor.of("#74A3D2").toString();
        String colorName = ChatColor.of("#76C6E8") + "" + ChatColor.BOLD;
        String bossName = ChatColor.BOLD + getBossTitle();

        String msg;

        if (names.size() == 1) {
            msg = prefix + colorText + " El jugador " + colorName + names.get(0) +
                    colorText + " ha invocado a la " + bossName;
        } else {
            msg = prefix + colorText + " Los jugadores " + colorName + String.join(", ", names) +
                    colorText + " han invocado a la " + bossName;
        }

        Bukkit.broadcastMessage(msg);
    }

    // ===========================
    //      MENSAJES GLOBALES
    // ===========================
    private void sendDeathMessage() {
        Set<UUID> involvedUUIDs = new HashSet<>(currentPlayers);
        involvedUUIDs.addAll(attackers);

        List<String> names = involvedUUIDs.stream()
                .map(id -> Bukkit.getPlayer(id))
                .filter(p -> p != null && (p.getGameMode() == org.bukkit.GameMode.SURVIVAL || p.getGameMode() == org.bukkit.GameMode.ADVENTURE))
                .map(Player::getName)
                .toList();

        if (names.isEmpty()) return;

        String prefix = ChatColor.of("#F2E66A") + "" + ChatColor.BOLD + "\u06de";
        String colorText = ChatColor.of("#F5A62E").toString();
        String colorName = ChatColor.of("#F55D7A") + "" + ChatColor.BOLD;
        String bossName = ChatColor.BOLD + getBossTitle();

        String msg;

        if (names.size() == 1) {
            msg = prefix + colorText + " El jugador " + colorName + names.get(0) +
                    colorText + " ha derrotado a la " + bossName;
        } else {
            msg = prefix + colorText + " Los jugadores " + colorName + String.join(", ", names) +
                    colorText + " han derrotado a la " + bossName;
        }

        Bukkit.broadcastMessage(msg);
    }

    // ===========================
    //         ABSTRACTOS
    // ===========================
    protected abstract String getBossTitle();
    protected abstract int getArenaRadius();
    protected abstract int getArenaHeightUp();
    protected abstract int getArenaHeightDown();
    protected abstract AreaZone.Shape getArenaShape();

    protected abstract void onStart();
    protected abstract void onTick();
    protected abstract void onDeath();
    protected abstract void onUnload();
}