package Dificultades.CustomMobs;

import Dificultades.Features.EnderMobsTP;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class EnderGhast extends EnderMobsTP implements Listener {
    private boolean eventsRegistered = false;

    public EnderGhast(JavaPlugin plugin) {
        super(plugin, "ender_ghast");
    }

    @Override
    public void apply() {
        super.apply();
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
        }
    }

    public void revert() {
        if (eventsRegistered) {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (isCustomMob(entity)) {
                        entity.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public Ghast spawnEnderGhast(Location location) {
        Ghast enderGhast = (Ghast) location.getWorld().spawnEntity(location, EntityType.GHAST);
        applyEnderGhastAttributes(enderGhast);
        return enderGhast;
    }

    private void applyEnderGhastAttributes(Ghast ghast) {
        ghast.setCustomName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Ender Ghast");
        ghast.setCustomNameVisible(false);
        ghast.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(100);
        ghast.setHealth(100);
        ghast.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(96);

        ghast.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Ghast) {
            Ghast ghast = (Ghast) event.getEntity().getShooter();

            if (isCustomMob(ghast) && event.getEntity() instanceof Fireball) {
                Fireball fireball = (Fireball) event.getEntity();
                fireball.setYield(5.0f);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (fireball.isValid()) {
                            fireball.getWorld().spawnParticle(
                                    Particle.PORTAL,
                                    fireball.getLocation(),
                                    5,
                                    0.1, 0.1, 0.1, 0.05
                            );
                        } else {
                            this.cancel();
                        }
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (isCustomMob(event.getEntity())) {
            Ghast ghast = (Ghast) event.getEntity();

            event.getDrops().clear();

            ghast.getWorld().playSound(ghast.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 3.0f, 0.7f);
            ghast.getWorld().playSound(ghast.getLocation(), Sound.ENTITY_GHAST_DEATH, 3.0f, 0.7f);
            ghast.getWorld().spawnParticle(Particle.PORTAL, ghast.getLocation(), 100, 1, 1, 1, 0.5);
        }
    }

    @EventHandler
    public void onEnderGhastHurt(EntityDamageEvent event) {
        if (isCustomMob(event.getEntity())) {
            Ghast ghast = (Ghast) event.getEntity();
            ghast.getWorld().playSound(ghast.getLocation(), Sound.ENTITY_GHAST_HURT, 3.0f, 0.7f);
            ghast.getWorld().playSound(ghast.getLocation(), Sound.ENTITY_ENDERMAN_HURT, 3.0f, 0.7f);
            ghast.getWorld().spawnParticle(Particle.PORTAL, ghast.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
        }
    }

    public NamespacedKey getEnderGhastKey() {
        return mobKey;
    }

    @Override
    public boolean isCustomMob(Entity entity) {
        return entity instanceof Ghast &&
                entity.getPersistentDataContainer().has(mobKey, PersistentDataType.BYTE);
    }
}