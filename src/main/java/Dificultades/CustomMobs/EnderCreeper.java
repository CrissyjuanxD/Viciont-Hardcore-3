package Dificultades.CustomMobs;

import Dificultades.Features.EnderMobsTP;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class EnderCreeper extends EnderMobsTP implements Listener {
    private boolean eventsRegistered = false;

    public EnderCreeper(JavaPlugin plugin) {
        super(plugin, "ender_creeper");
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

    public Creeper spawnEnderCreeper(Location location) {
        Creeper enderCreeper = (Creeper) location.getWorld().spawnEntity(location, EntityType.CREEPER);
        applyEnderCreeperAttributes(enderCreeper);
        return enderCreeper;
    }

    private void applyEnderCreeperAttributes(Creeper creeper) {
        creeper.setCustomName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Ender Creeper");
        creeper.setCustomNameVisible(false);

        creeper.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(30);
        creeper.setHealth(30);
        creeper.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 2));
        creeper.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 1));
        creeper.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0));

        creeper.setExplosionRadius(6);
        creeper.setPowered(true);

        creeper.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);

        startParticleTask(creeper);
    }

    private void startParticleTask(Creeper creeper) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (creeper.isDead() || !creeper.isValid()) {
                    this.cancel();
                    return;
                }

                creeper.getWorld().spawnParticle(
                        Particle.TRIAL_SPAWNER_DETECTION_OMINOUS,
                        creeper.getLocation().add(0, 1, 0),
                        5,
                        0.3, 0.5, 0.3, 0.05
                );
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (isCustomMob(event.getEntity())) {
            Creeper creeper = (Creeper) event.getEntity();

            creeper.getWorld().playSound(creeper.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 2.0f, 0.8f);
            creeper.getWorld().playSound(creeper.getLocation(), Sound.ENTITY_CREEPER_DEATH, 2.0f, 0.8f);
            creeper.getWorld().spawnParticle(
                    Particle.DRAGON_BREATH,
                    creeper.getLocation(),
                    100,
                    3, 3, 3, 0.5
            );
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (isCustomMob(event.getEntity())) {
            Creeper creeper = (Creeper) event.getEntity();

            event.getDrops().clear();

            creeper.getWorld().playSound(creeper.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 2.0f, 0.7f);
            creeper.getWorld().playSound(creeper.getLocation(), Sound.ENTITY_CREEPER_DEATH, 2.0f, 0.7f);
            creeper.getWorld().spawnParticle(
                    Particle.TRIAL_SPAWNER_DETECTION_OMINOUS,
                    creeper.getLocation(),
                    100,
                    1, 1, 1, 0.5
            );
        }
    }

    @EventHandler
    public void onEnderCreeperHurt(EntityDamageEvent event) {
        if (isCustomMob(event.getEntity())) {
            Creeper creeper = (Creeper) event.getEntity();

            creeper.getWorld().playSound(creeper.getLocation(), Sound.ENTITY_ENDERMAN_HURT, 2.0f, 0.7f);
            creeper.getWorld().playSound(creeper.getLocation(), Sound.ENTITY_CREEPER_HURT, 2.0f, 0.7f);
            creeper.getWorld().spawnParticle(
                    Particle.PORTAL,
                    creeper.getLocation(),
                    30,
                    0.5, 0.5, 0.5, 0.1
            );

            creeper.addPotionEffect(new PotionEffect(
                    PotionEffectType.GLOWING,
                    20,
                    0,
                    false, false
            ));
        }
    }

    public NamespacedKey getEnderCreeperKey() {
        return mobKey;
    }

    @Override
    public boolean isCustomMob(Entity entity) {
        return entity instanceof Creeper &&
                entity.getPersistentDataContainer().has(mobKey, PersistentDataType.BYTE);
    }
}