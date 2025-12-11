package Dificultades.CustomMobs;

import Dificultades.Features.DarkMobSB;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class DarkCreeper extends DarkMobSB implements Listener {

    private static final Set<UUID> activeMobs = new HashSet<>();
    private static BukkitTask particleTask;

    private static boolean eventsRegistered = false;

    public DarkCreeper(JavaPlugin plugin) {
        super(plugin, "dark_creeper");
    }

    @Override
    public void apply() {
        super.apply();
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
            startGlobalParticleTask();
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
            if (particleTask != null) {
                particleTask.cancel();
                particleTask = null;
            }
            activeMobs.clear();
            eventsRegistered = false;
        }
    }

    public Creeper spawnDarkCreeper(Location location) {
        Creeper darkCreeper = (Creeper) location.getWorld().spawnEntity(location, EntityType.CREEPER);
        applyDarkCreeperAttributes(darkCreeper);

        // Registrar y asegurar tarea
        activeMobs.add(darkCreeper.getUniqueId());
        startGlobalParticleTask();

        return darkCreeper;
    }

    private void applyDarkCreeperAttributes(Creeper creeper) {
        creeper.setCustomName(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Dark Creeper");
        creeper.setCustomNameVisible(false);

        creeper.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(80);
        creeper.setHealth(80);
        creeper.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25);

        creeper.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                PotionEffect.INFINITE_DURATION,
                2, false, false
        ));

        creeper.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                PotionEffect.INFINITE_DURATION,
                1, false, false
        ));

        creeper.setExplosionRadius(8);
        creeper.setPowered(false);

        creeper.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);
    }

    private void startGlobalParticleTask() {
        if (particleTask != null && !particleTask.isCancelled()) return;

        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeMobs.isEmpty()) return;

                Iterator<UUID> it = activeMobs.iterator();
                while (it.hasNext()) {
                    UUID uuid = it.next();
                    Entity entity = Bukkit.getEntity(uuid);

                    if (entity == null || !entity.isValid() || entity.isDead()) {
                        if (entity != null && !entity.isValid()) it.remove();
                        continue;
                    }

                    entity.getWorld().spawnParticle(
                            Particle.LARGE_SMOKE,
                            entity.getLocation().add(0, 1, 0),
                            5, 0.3, 0.5, 0.3, 0.05
                    );
                    entity.getWorld().spawnParticle(
                            Particle.SOUL_FIRE_FLAME,
                            entity.getLocation(),
                            3, 0.3, 0.3, 0.3, 0.05
                    );
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (isCustomMob(event.getEntity())) {
            Creeper creeper = (Creeper) event.getEntity();
            Location explosionLoc = creeper.getLocation();
            World world = creeper.getWorld();

            world.playSound(explosionLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.7f);
            world.spawnParticle(Particle.EXPLOSION_EMITTER, explosionLoc, 1);

            for (Player player : world.getPlayers()) {
                if (player.getLocation().distance(explosionLoc) <= 20) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 600, 0, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 600, 1, false, true));
                    player.spawnParticle(Particle.SQUID_INK, player.getLocation(), 30, 0.5, 1, 0.5, 0.1);
                }
            }
            // No necesitamos remover explícitamente aquí, la task lo limpiará al ver que isValid() es false
        }
    }

    @EventHandler
    public void onCreeperPower(EntityTransformEvent event) {
        if (event.getTransformReason() == EntityTransformEvent.TransformReason.LIGHTNING &&
                isCustomMob(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCreeperPowered(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Creeper creeper &&
                isCustomMob(creeper) &&
                creeper.isPowered()) {
            creeper.setPowered(false);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (isCustomMob(event.getEntity())) {
            Creeper creeper = (Creeper) event.getEntity();
            event.getDrops().clear();

            creeper.getWorld().playSound(creeper.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.5f, 0.8f);
            creeper.getWorld().spawnParticle(Particle.SOUL, creeper.getLocation(), 50, 1, 1, 1, 0.3);

            activeMobs.remove(creeper.getUniqueId());
        }
    }

    @EventHandler
    public void onDarkCreeperHurt(EntityDamageEvent event) {
        if (isCustomMob(event.getEntity())) {
            Creeper creeper = (Creeper) event.getEntity();
            creeper.getWorld().playSound(creeper.getLocation(), Sound.ENTITY_WARDEN_HURT, 1.5f, 0.7f);
            creeper.getWorld().spawnParticle(Particle.SMOKE, creeper.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
        }
    }

    @Override
    public boolean isCustomMob(Entity entity) {
        return entity instanceof Creeper &&
                entity.getPersistentDataContainer().has(mobKey, PersistentDataType.BYTE);
    }
}