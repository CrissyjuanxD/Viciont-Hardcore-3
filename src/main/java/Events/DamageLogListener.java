package Events;

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
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitRunnable;
import vct.hardcore3.ViciontHardcore3;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class DamageLogListener implements Listener {

    private final HashMap<UUID, Long> damageLogPlayers = new HashMap<>();
    private final HashMap<UUID, BossBar> playerBossBars = new HashMap<>();
    private final int delaySeconds = 15;
    private final JavaPlugin plugin;
    private final File dataFile;

    public DamageLogListener(JavaPlugin plugin) {
        this.plugin = plugin;
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

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        // Cargar el estado del Damage Log al iniciar el servidor
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : config.getKeys(false)) {
            UUID playerId = UUID.fromString(key);
            String playerName = config.getString(key + ".name");
            long endTime = config.getLong(key + ".endTime");

            if (endTime > System.currentTimeMillis()) {
                damageLogPlayers.put(playerId, endTime - (delaySeconds * 1000L));
                plugin.getLogger().info("Damage Log cargado para " + playerName + " (UUID: " + playerId + ")");
            }
        }
    }

    public void saveDamageLogState() {
        // Guardar el estado del Damage Log antes de reiniciar el servidor
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

            // Ignorar ciertos tipos de daño
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL ||
                    event.getCause() == EntityDamageEvent.DamageCause.VOID ||
                    event.getCause() == EntityDamageEvent.DamageCause.POISON ||
                    event.getCause() == EntityDamageEvent.DamageCause.WITHER ||
                    event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            damageLogPlayers.put(player.getUniqueId(), currentTime);

            // Obtener o crear la BossBar
            BossBar bossBar = playerBossBars.get(player.getUniqueId());
            if (bossBar == null) {
                bossBar = Bukkit.createBossBar(
                        ChatColor.RED + "۞ Damage Log" + ChatColor.WHITE + ": 00:15",
                        BarColor.WHITE,
                        BarStyle.SOLID
                );
                bossBar.addPlayer(player);
                playerBossBars.put(player.getUniqueId(), bossBar);
            } else {
                // Reiniciar la BossBar existente
                bossBar.setTitle(ChatColor.RED + "۞ Damage Log" + ChatColor.WHITE + ": 00:15");
                bossBar.setProgress(1.0);
            }

            // Crear una copia efectivamente final de la BossBar
            final BossBar finalBossBar = bossBar;

            // Actualizar BossBar
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!damageLogPlayers.containsKey(player.getUniqueId())) {
                        cancel();
                        return;
                    }

                    long startTime = damageLogPlayers.get(player.getUniqueId());
                    int elapsedTime = (int) ((System.currentTimeMillis() - startTime) / 1000);
                    int remainingTime = delaySeconds - elapsedTime;

                    if (remainingTime > 0) {
                        finalBossBar.setTitle(ChatColor.RED + "۞ Damage Log" + ChatColor.WHITE + String.format(": %02d:%02d", remainingTime / 60, remainingTime % 60));
                        finalBossBar.setProgress((double) remainingTime / delaySeconds);
                    } else {
                        finalBossBar.setTitle(ChatColor.GREEN + "۞ Damage Log" + ChatColor.WHITE + ": 00:00");
                        finalBossBar.setProgress(0.0);
                        damageLogPlayers.remove(player.getUniqueId());

                        // Eliminar BossBar después de 1 segundo
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                finalBossBar.removePlayer(player);
                                playerBossBars.remove(player.getUniqueId());
                            }
                        }.runTaskLater(plugin, 20L);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (damageLogPlayers.containsKey(player.getUniqueId())) {
            // Verificar si el servidor se está reiniciando
            if (!ViciontHardcore3.shuttingDown){
                // Penalización: daño de 8 corazones
                double damage = 16;
                double newHealth = player.getHealth() - damage;

                if (newHealth > 0) {
                    player.setHealth(newHealth);
                } else {
                    player.setHealth(0);
                }

                // Mensaje de penalización
                Bukkit.broadcastMessage(ChatColor.DARK_RED + "۞ " + player.getName() + " intentó desconectarse mientras estaba en 'Damage Log'. ¡Recibió daño como penalización!");
            }

            // Eliminar BossBar
            BossBar bossBar = playerBossBars.remove(player.getUniqueId());
            if (bossBar != null) {
                bossBar.removePlayer(player);
            }

            // Eliminar del Damage Log
            damageLogPlayers.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Eliminar BossBar y Damage Log si el jugador muere
        BossBar bossBar = playerBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }

        damageLogPlayers.remove(player.getUniqueId());
    }
}