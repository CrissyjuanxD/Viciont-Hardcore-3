package Dificultades.CustomMobs;

import Dificultades.Features.DarkMobSB;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class DarkPhantom extends DarkMobSB implements Listener {
    private boolean eventsRegistered = false;

    public DarkPhantom(JavaPlugin plugin) {
        super(plugin, "dark_phantom");
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

    public Phantom spawnDarkPhantom(Location location) {
        Phantom phantom = (Phantom) location.getWorld().spawnEntity(location, EntityType.PHANTOM);
        applyDarkPhantomAttributes(phantom);
        return phantom;
    }

    private void applyDarkPhantomAttributes(Phantom phantom) {
        phantom.setCustomName(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Dark Phantom");
        phantom.setCustomNameVisible(false);

        phantom.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(100);
        phantom.setHealth(100);
        phantom.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(10);
        phantom.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3);
        phantom.setSize(3);

        phantom.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 1));
        phantom.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0));

        phantom.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);

        startParticleTask(phantom);
        startTrackingTask(phantom);
    }

    private void startParticleTask(Phantom phantom) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (phantom.isDead() || !phantom.isValid()) {
                    this.cancel();
                    return;
                }

                phantom.getWorld().spawnParticle(
                        Particle.LARGE_SMOKE,
                        phantom.getLocation(),
                        10,
                        1, 1, 1, 0.1
                );
                phantom.getWorld().spawnParticle(
                        Particle.REVERSE_PORTAL,
                        phantom.getLocation(),
                        5,
                        0.5, 0.5, 0.5, 0.1
                );
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void startTrackingTask(Phantom phantom) {
        new BukkitRunnable() {
            private long lastSonicBoomTime = 0;
            private final long COOLDOWN_MS = 10000;

            @Override
            public void run() {
                if (phantom.isDead() || !phantom.isValid()) {
                    this.cancel();
                    return;
                }

                Player nearest = null;
                double nearestDistance = Double.MAX_VALUE;

                for (Player player : phantom.getWorld().getPlayers()) {
                    double distance = player.getLocation().distance(phantom.getLocation());
                    if (distance <= 64 && distance < nearestDistance) {
                        nearest = player;
                        nearestDistance = distance;
                    }
                }

                if (nearest != null) {
                    phantom.setTarget(nearest);

                    long currentTime = System.currentTimeMillis();
                    if (nearestDistance <= 40 &&
                            currentTime - lastSonicBoomTime > COOLDOWN_MS &&
                            phantom.hasLineOfSight(nearest)) {

                        lastSonicBoomTime = currentTime;
                        startSonicBoomSequence(phantom, nearest);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void startSonicBoomSequence(Phantom phantom, Player target) {
        phantom.setAI(false);

        World world = phantom.getWorld();
        Location phantomLoc = phantom.getEyeLocation();

        world.playSound(phantomLoc, Sound.ENTITY_WARDEN_SONIC_CHARGE, 2.0f, 0.7f);
        world.spawnParticle(Particle.SONIC_BOOM, phantomLoc, 10, 0.5, 0.5, 0.5, 0);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (phantom.isDead() || !phantom.isValid() || target.isDead()) {
                    phantom.setAI(true);
                    return;
                }

                launchExpandingSonicBoom(phantom, target);
                phantom.setAI(true);
            }
        }.runTaskLater(plugin, 50L);
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.getEntity() instanceof Phantom phantom &&
                isCustomMob(phantom) &&
                event.getTarget() instanceof Player) {

            Player nearest = findNearestPlayer(phantom.getLocation(), 64);
            if (nearest != null) {
                event.setTarget(nearest);
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (isCustomMob(event.getEntity())) {
            Phantom phantom = (Phantom) event.getEntity();

            event.getDrops().clear();

            phantom.getWorld().playSound(phantom.getLocation(), Sound.ENTITY_WARDEN_DEATH, 2.0f, 0.8f);
            phantom.getWorld().spawnParticle(
                    Particle.SOUL,
                    phantom.getLocation(),
                    50,
                    1, 1, 1, 0.5
            );
        }
    }

    @EventHandler
    public void onDarkPhantomHurt(EntityDamageEvent event) {
        if (isCustomMob(event.getEntity())) {
            Phantom phantom = (Phantom) event.getEntity();

            phantom.getWorld().playSound(phantom.getLocation(), Sound.ENTITY_WARDEN_HURT, 2.0f, 0.8f);
            phantom.getWorld().spawnParticle(
                    Particle.SMOKE,
                    phantom.getLocation(),
                    30,
                    0.5, 0.5, 0.5, 0.1
            );
        }
    }

    @Override
    public boolean isCustomMob(Entity entity) {
        return entity instanceof Phantom &&
                entity.getPersistentDataContainer().has(mobKey, PersistentDataType.BYTE);
    }
}