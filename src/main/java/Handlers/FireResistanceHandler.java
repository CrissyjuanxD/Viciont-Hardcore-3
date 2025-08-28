package Handlers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;

public class FireResistanceHandler implements Listener {

    private final JavaPlugin plugin;

    public FireResistanceHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE) && player.getFireTicks() > 0) {
            player.setFireTicks(0);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if ((event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                    event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                    event.getCause() == EntityDamageEvent.DamageCause.LAVA ||
                    event.getCause() == EntityDamageEvent.DamageCause.HOT_FLOOR) &&
                    player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {

                event.setCancelled(true);

                if (player.getFireTicks() > 0) {
                    player.setFireTicks(0);
                }
            }
        }
    }

    public void startFireCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE) && player.getFireTicks() > 0) {
                        player.setFireTicks(0);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}