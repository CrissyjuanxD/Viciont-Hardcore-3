package Dificultades.Features;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public abstract class EnderMobsTP {
    protected final JavaPlugin plugin;
    protected final NamespacedKey mobKey;
    private boolean eventsRegistered = false;

    public EnderMobsTP(JavaPlugin plugin, String keyName) {
        this.plugin = plugin;
        this.mobKey = new NamespacedKey(plugin, keyName);
    }

    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void onMobHurt(EntityDamageEvent event) {
                    if (isCustomMob(event.getEntity())) {
                        handleTeleportOnDamage((LivingEntity) event.getEntity());
                    }
                }
            }, plugin);
            eventsRegistered = true;
        }
    }

    protected void handleTeleportOnDamage(LivingEntity mob) {
        // 50% de probabilidad de teletransporte
        if (Math.random() < 0.5) {
            teleportRandomly(mob, 30);
        }
    }

    protected void teleportRandomly(LivingEntity mob, int radius) {
        World world = mob.getWorld();
        Location originalLoc = mob.getLocation();

        // Generar nueva ubicación aleatoria
        double angle = Math.random() * 2 * Math.PI;
        double distance = Math.random() * radius;
        double x = originalLoc.getX() + distance * Math.cos(angle);
        double z = originalLoc.getZ() + distance * Math.sin(angle);

        // Asegurarse de que la nueva ubicación es válida
        Location newLoc = new Location(world, x, originalLoc.getY(), z);
        newLoc.setY(world.getHighestBlockYAt(newLoc) + 1);

        // Efectos de sonido y partículas antes del TP
        world.playSound(originalLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 3.0f, 1.0f);
        world.spawnParticle(Particle.PORTAL, originalLoc, 50, 0.5, 0.5, 0.5, 0.5);

        // Realizar el teletransporte
        mob.teleport(newLoc);

        // Efectos de sonido y partículas después del TP
        world.playSound(newLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 3.0f, 1.0f);
        world.spawnParticle(Particle.PORTAL, newLoc, 50, 0.5, 0.5, 0.5, 0.5);
    }

    public abstract boolean isCustomMob(Entity entity);
}