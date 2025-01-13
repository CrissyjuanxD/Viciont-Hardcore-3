package Events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class DamageLogListener implements Listener {

    private final HashMap<UUID, Long> damageLogPlayers = new HashMap<>();
    private final HashMap<UUID, BossBar> playerBossBars = new HashMap<>();
    private final int delaySeconds = 15;
    private final JavaPlugin plugin;

    public DamageLogListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            damageLogPlayers.put(player.getUniqueId(), currentTime);

            // Crear o reiniciar BossBar
            BossBar bossBar = playerBossBars.get(player.getUniqueId());
            if (bossBar == null) {
                bossBar = Bukkit.createBossBar(
                        ChatColor.RED + "۞ Damage Log" + ChatColor.WHITE + ": 00:15",
                        BarColor.WHITE,
                        BarStyle.SOLID
                );
                bossBar.addPlayer(player);
                playerBossBars.put(player.getUniqueId(), bossBar);
            }

            bossBar.setProgress(1.0);

            // Actualizar BossBar
            BossBar finalBossBar = bossBar;
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
                        finalBossBar.setTitle(ChatColor.GREEN + "۞ Ya puedes desconectarte!");
                        finalBossBar.setProgress(0.0);
                        damageLogPlayers.remove(player.getUniqueId());

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

            BossBar bossBar = playerBossBars.remove(player.getUniqueId());
            if (bossBar != null) {
                bossBar.removePlayer(player);
            }

            damageLogPlayers.remove(player.getUniqueId());
        }
    }
}
