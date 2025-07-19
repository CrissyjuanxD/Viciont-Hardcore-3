package Events;

import Handlers.DeathStormHandler;
import TitleListener.MuerteHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import vct.hardcore3.ViciontHardcore3;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DamageLogListener implements Listener {

    private final HashMap<UUID, Long> damageLogPlayers = new HashMap<>();
    private final HashMap<UUID, BukkitTask> damageLogTasks = new HashMap<>();
    private final Set<UUID> pausedActionBars = new HashSet<>();
    private final int delaySeconds = 15;
    private final JavaPlugin plugin;
    private final DeathStormHandler deathStormHandler;
    private final File dataFile;

    public DamageLogListener(JavaPlugin plugin, DeathStormHandler deathStormHandler) {
        this.plugin = plugin;
        this.deathStormHandler = deathStormHandler;
        this.dataFile = new File(plugin.getDataFolder(), "damagelog.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("No se pudo crear el archivo damagelog.yml: " + e.getMessage());
            }
        }
    }

    public boolean isPlayerInDamageLog(UUID playerId) {
        return damageLogPlayers.containsKey(playerId);
    }

    // Métodos para controlar la visibilidad del ActionBar
    public void pauseActionBarForPlayer(UUID playerId) {
        pausedActionBars.add(playerId);
    }

    public void resumeActionBarForPlayer(UUID playerId) {
        pausedActionBars.remove(playerId);
    }

    public boolean isActionBarPausedForPlayer(UUID playerId) {
        return pausedActionBars.contains(playerId);
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : config.getKeys(false)) {
            UUID playerId = UUID.fromString(key);
            String playerName = config.getString(key + ".name");
            long endTime = config.getLong(key + ".endTime");

            if (endTime > System.currentTimeMillis()) {
                damageLogPlayers.put(playerId, endTime - (delaySeconds * 1000L));
                plugin.getLogger().info("Damage Log cargado para " + playerName + " (UUID: " + playerId + ")");
                startDamageLogTimer(playerId);
            }
        }
    }

    public void saveDamageLogState() {
        YamlConfiguration config = new YamlConfiguration();
        for (UUID playerId : damageLogPlayers.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                long endTime = damageLogPlayers.get(playerId) + (delaySeconds * 1000L);
                config.set(playerId.toString() + ".name", player.getName());
                config.set(playerId.toString() + ".endTime", endTime);
            }
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar el archivo damagelog.yml: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (MuerteHandler.isDeathMessageActive()) {
                return;
            }

            if (event.getCause() == EntityDamageEvent.DamageCause.FALL ||
                    event.getCause() == EntityDamageEvent.DamageCause.VOID ||
                    event.getCause() == EntityDamageEvent.DamageCause.POISON ||
                    event.getCause() == EntityDamageEvent.DamageCause.WITHER ||
                    event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            damageLogPlayers.put(player.getUniqueId(), currentTime);
            startDamageLogTimer(player.getUniqueId());
        }
    }

    private void startDamageLogTimer(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;

        // Cancelar tarea existente para este jugador
        BukkitTask existingTask = damageLogTasks.get(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }

        if (deathStormHandler != null) {
            deathStormHandler.pauseActionBarForPlayer(playerId);
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Player currentPlayer = Bukkit.getPlayer(playerId);
                if (currentPlayer == null || !damageLogPlayers.containsKey(playerId)) {
                    if (deathStormHandler != null) {
                        deathStormHandler.resumeActionBarForPlayer(playerId);
                    }
                    cancel();
                    damageLogTasks.remove(playerId);
                    return;
                }

                // No mostrar si hay mensaje de muerte activo
                if (MuerteHandler.isDeathMessageActive()) {
                    return;
                }

                long startTime = damageLogPlayers.get(playerId);
                int elapsedTime = (int) ((System.currentTimeMillis() - startTime) / 1000);
                int remainingTime = delaySeconds - elapsedTime;

                if (remainingTime > 0) {
                    if (!isActionBarPausedForPlayer(playerId)) {
                        if (deathStormHandler != null) {
                            deathStormHandler.pauseActionBarForPlayer(playerId);
                        }

                        String message = ChatColor.of("#D24346") + "" + ChatColor.BOLD + "۞ " + ChatColor.RESET + ChatColor.of("#9179D4") + "Damage Log: " + ChatColor.RESET + ChatColor.of("#CE2E69") + ChatColor.BOLD + ChatColor.UNDERLINE +
                                String.format("%02d:%02d", remainingTime / 60, remainingTime % 60);
                        currentPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                TextComponent.fromLegacyText(message));
                    }
                } else {
                    if (!isActionBarPausedForPlayer(playerId)) {
                        currentPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                TextComponent.fromLegacyText(ChatColor.of("#D24346") + "" + ChatColor.BOLD + "۞ " + ChatColor.RESET + ChatColor.of("#9179D4") + "Damage Log: " + ChatColor.RESET + ChatColor.of("#CE2E69") + ChatColor.BOLD + ChatColor.UNDERLINE + "00:00"));
                    }
                    damageLogPlayers.remove(playerId);
                    damageLogTasks.remove(playerId);
                    if (deathStormHandler != null) {
                        deathStormHandler.resumeActionBarForPlayer(playerId);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        damageLogTasks.put(playerId, task);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (damageLogPlayers.containsKey(playerId)) {
            if (!ViciontHardcore3.shuttingDown) {
                double damage = 16;
                double newHealth = player.getHealth() - damage;

                if (newHealth > 0) {
                    player.setHealth(newHealth);
                } else {
                    player.setHealth(0);
                }

                Bukkit.broadcastMessage(ChatColor.DARK_RED + "۞ " + player.getName() +
                        " intentó desconectarse mientras estaba en 'Damage Log'. ¡Recibió daño como penalización!");
            }

            damageLogPlayers.remove(player.getUniqueId());
            pausedActionBars.remove(player.getUniqueId());

            BukkitTask task = damageLogTasks.remove(playerId);
            if (task != null) {
                task.cancel();
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();

        pausedActionBars.remove(playerId);

        // Cancelar tarea específica
        BukkitTask task = damageLogTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }
}