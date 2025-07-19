/*
package Dificultades.Features;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PassiveMobAggression implements Listener {

    private final Plugin plugin;
    private boolean isApplied = false;

    public PassiveMobAggression(Plugin plugin) {
        this.plugin = plugin;
    }

    public void apply() {
        if (!isApplied) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            startAggression();
            isApplied = true;
        }
    }

    public void revert() {
        if (isApplied) {
            isApplied = false;
            HandlerList.unregisterAll(this);
        }
    }

    private void startAggression() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isApplied) {
                    cancel();
                    return;
                }

                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntities()) {
                        if (!(entity instanceof Creature)) continue;
                        if (entity instanceof Monster) continue;

                        if (entity.getType() == EntityType.BEE || entity.getType() == EntityType.DOLPHIN) continue;

                        Creature creature = (Creature) entity;

                        Player nearest = getNearestPlayer(creature, 10);
                        if (nearest != null) {
                            creature.setTarget(nearest);
                        } else {
                            creature.setTarget(null);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Cada segundo, suficiente para seguimiento
    }

    private Player getNearestPlayer(Creature creature, double radius) {
        Player nearest = null;
        double closestDist = radius * radius;

        for (Player player : creature.getWorld().getPlayers()) {
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) continue;

            double distSquared = player.getLocation().distanceSquared(creature.getLocation());
            if (distSquared < closestDist) {
                closestDist = distSquared;
                nearest = player;
            }
        }
        return nearest;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Creature && !(event.getDamager() instanceof Monster)) {
            EntityType type = event.getDamager().getType();
            if (type == EntityType.BEE || type == EntityType.DOLPHIN) return;
            event.setDamage(8.0); // 4 corazones
        }
    }
}
*/
