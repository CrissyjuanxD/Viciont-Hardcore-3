package Dificultades.Features;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class DarkMobSB {
    protected final JavaPlugin plugin;
    protected final NamespacedKey mobKey;
    private final Map<UUID, Integer> beamTasks = new HashMap<>();
    private boolean eventsRegistered = false;

    public DarkMobSB(JavaPlugin plugin, String keyName) {
        this.plugin = plugin;
        this.mobKey = new NamespacedKey(plugin, keyName);
    }

    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void onMobHurt(EntityDamageEvent event) {
                    if (isCustomMob(event.getEntity()) && event.getEntity() instanceof LivingEntity mob) {
                        handleBeamAttack(mob);
                    }
                }

                @EventHandler
                public void onTarget(EntityTargetLivingEntityEvent event) {
                    if (isCustomMob(event.getEntity())) {
                        LivingEntity mob = (LivingEntity) event.getEntity();
                        if (event.getTarget() instanceof Player) {
                            startSonicBoomTracking(mob, (Player) event.getTarget());
                        }
                    }
                }
            }, plugin);
            eventsRegistered = true;
        }
    }

    protected void handleBeamAttack(LivingEntity mob) {
        if (Math.random() < 0.5) {
            Player target = findNearestPlayer(mob.getLocation(), 35);
            if (target != null) {
                launchDarkBeam(mob, target);
            }
        }
    }

    protected void launchDarkBeam(LivingEntity mob, Player target) {
        mob.setAI(false);
        Location startLoc = mob.getEyeLocation();
        World world = mob.getWorld();

        world.playSound(startLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.7f);
        world.spawnParticle(Particle.SQUID_INK, startLoc, 15, 0.5, 0.5, 0.5, 0.1);

        Particle.DustOptions blackBeam = new Particle.DustOptions(
                Color.fromRGB(0, 150, 255),
                2.0f
        );

        new BukkitRunnable() {
            @Override
            public void run() {
                if (mob.isDead() || !mob.isValid() || target.isDead()) {
                    mob.setAI(true);
                    this.cancel();
                    return;
                }

                Location currentEnd = target.getEyeLocation();
                drawBlackBeam(startLoc, currentEnd, blackBeam);

                RayTraceResult rayTrace = world.rayTrace(
                        startLoc,
                        currentEnd.toVector().subtract(startLoc.toVector()).normalize(),
                        startLoc.distance(currentEnd),
                        FluidCollisionMode.NEVER,
                        true,
                        1.5,
                        entity -> entity instanceof LivingEntity && entity != mob
                );

                if (rayTrace != null && rayTrace.getHitEntity() instanceof LivingEntity hit) {
                    hit.damage(6);
                    hit.getWorld().playSound(hit.getLocation(), Sound.ENTITY_WARDEN_HURT, 1.0f, 1.2f);
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        mob.setAI(true);
                    }
                }.runTaskLater(plugin, 5L);

                this.cancel();
            }
        }.runTaskTimer(plugin, 10L, 1L);
    }

    protected void drawBlackBeam(Location start, Location end, Particle.DustOptions options) {
        World world = start.getWorld();
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = start.distance(end);

        for (double d = 0; d < distance; d += 0.3) {
            Location point = start.clone().add(direction.clone().normalize().multiply(d));
            world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, options);

            if (d % 1.0 == 0) {
                world.spawnParticle(Particle.LARGE_SMOKE, point, 1, 0, 0, 0, 0.05);
            }
        }

        world.spawnParticle(Particle.SOUL_FIRE_FLAME, start, 5, 0.2, 0.2, 0.2, 0.05);
        world.spawnParticle(Particle.SQUID_INK, end, 3, 0.3, 0.3, 0.3, 0.1);
    }

    protected void startSonicBoomTracking(LivingEntity mob, Player target) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (mob.isDead() || !mob.isValid() || target.isDead() || !target.isValid()) {
                    this.cancel();
                    return;
                }

                double distance = mob.getLocation().distance(target.getLocation());
                if (distance <= 35) {
                    mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 2.0f, 0.8f);
                    mob.getWorld().spawnParticle(Particle.SONIC_BOOM, mob.getEyeLocation(), 1);

                    new BukkitRunnable() {
                        int ticks = 0;

                        @Override
                        public void run() {
                            if (mob.isDead() || !mob.isValid() || ticks >= 50) {
                                if (!mob.isDead() && mob.isValid()) {
                                    launchExpandingSonicBoom(mob, target);
                                }
                                this.cancel();
                                return;
                            }

                            if (ticks % 5 == 0) {
                                mob.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                                        mob.getEyeLocation(), 2, 0.3, 0.3, 0.3, 0.1);
                            }

                            ticks++;
                        }
                    }.runTaskTimer(plugin, 0L, 1L);

                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    protected void launchExpandingSonicBoom(LivingEntity mob, Player target) {
        Location startLoc = mob.getEyeLocation();
        World world = mob.getWorld();

        world.playSound(startLoc, Sound.ENTITY_WARDEN_SONIC_CHARGE, 2.0f, 0.8f);

        world.spawnParticle(Particle.SONIC_BOOM, startLoc, 5, 0.5, 0.5, 0.5, 0);

        new BukkitRunnable() {
            private int chargeTicks = 0;
            private final int CHARGE_DURATION = 50;

            @Override
            public void run() {
                if (chargeTicks >= CHARGE_DURATION) {
                    launchSonicBeam(mob, target);
                    this.cancel();
                    return;
                }

                if (chargeTicks % 10 == 0) {
                    world.spawnParticle(Particle.ELECTRIC_SPARK, startLoc, 10, 0.5, 0.5, 0.5, 0.1);
                }

                chargeTicks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    protected void launchSonicBeam(LivingEntity mob, Player target) {
        World world = mob.getWorld();
        final Location fixedTargetLoc = target.getEyeLocation().clone();

        world.playSound(mob.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.0f);

        Particle.DustOptions beamColor = new Particle.DustOptions(
                Color.fromRGB(0, 150, 255),
                2.0f
        );

        new BukkitRunnable() {
            private int activeTicks = 0;
            private final int DURATION = 40;

            @Override
            public void run() {
                if (activeTicks >= DURATION || mob.isDead() || !mob.isValid() || target.isDead()) {
                    this.cancel();
                    return;
                }

                Location currentStart = mob.getEyeLocation();
                Location currentEnd = fixedTargetLoc;
                Vector direction = currentEnd.toVector().subtract(currentStart.toVector());
                double distance = currentStart.distance(currentEnd);

                drawContinuousBeam(currentStart, currentEnd);

                if (activeTicks % 4 == 0) {
                    checkCollisionAndDamage(mob, currentStart, currentEnd);
                }

                activeTicks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    protected void drawContinuousBeam(Location start, Location end) {
        World world = start.getWorld();
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = start.distance(end);

        for (double d = 0; d < distance; d += 0.3) {
            Location point = start.clone().add(direction.clone().normalize().multiply(d));
            world.spawnParticle(Particle.SONIC_BOOM, point, 1, 0, 0, 0, 0);
        }

        world.spawnParticle(Particle.END_ROD, start, 3, 0.2, 0.2, 0.2, 0.05);
        world.spawnParticle(Particle.SONIC_BOOM, end, 2, 0.3, 0.3, 0.3, 0);
    }

    protected void checkCollisionAndDamage(LivingEntity mob, Location start, Location end) {
        World world = start.getWorld();
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);

        RayTraceResult rayTrace = world.rayTrace(
                start,
                direction,
                distance,
                FluidCollisionMode.NEVER,
                true,
                1.0,
                entity -> entity instanceof LivingEntity && entity != mob
        );

        if (rayTrace != null && rayTrace.getHitEntity() instanceof LivingEntity hit) {
            hit.damage(8);
            hit.setVelocity(direction.multiply(0.7).setY(0.3));
            world.playSound(hit.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);
        }
    }

    protected Player findNearestPlayer(Location location, double radius) {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : location.getWorld().getPlayers()) {
            double distance = player.getLocation().distance(location);
            if (distance <= radius && distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    public abstract boolean isCustomMob(Entity entity);
}