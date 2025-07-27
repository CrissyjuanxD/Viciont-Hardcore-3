package Dificultades.CustomMobs;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BatBoom implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey batBoomKey;
    private boolean eventsRegistered = false;

    public BatBoom(JavaPlugin plugin) {
        this.plugin = plugin;
        this.batBoomKey = new NamespacedKey(plugin, "bat_boom");
    }

    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
        }
    }

    public void revert() {
        if (eventsRegistered) {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Bat bat && isBatBoom(bat)) {
                        bat.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public Bat spawnBatBoom(Location location) {
        Bat batBoom = (Bat) location.getWorld().spawnEntity(location, EntityType.BAT);
        applyBatBoomAttributes(batBoom);
        return batBoom;
    }

    private void applyBatBoomAttributes(Bat bat) {
        bat.setCustomName(ChatColor.RED + "" + ChatColor.BOLD + "Bat Boom");
        bat.setCustomNameVisible(false);
        bat.setAwake(true);
        bat.setAI(true);
        bat.setCollidable(true);
        bat.setInvulnerable(false);
        bat.setSilent(false);

        if (bat.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            bat.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(10);
            bat.setHealth(10);
        }

        if (bat.getAttribute(Attribute.GENERIC_FOLLOW_RANGE) != null) {
            bat.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40);
        }

        if (bat.getAttribute(Attribute.GENERIC_FLYING_SPEED) != null) {
            bat.getAttribute(Attribute.GENERIC_FLYING_SPEED).setBaseValue(0.5);
        }

        bat.getWorld().spawnParticle(
                Particle.TRIAL_SPAWNER_DETECTION,
                bat.getLocation(),
                5,
                0.2, 0.2, 0.2, 0
        );
        bat.getWorld().playSound(bat.getLocation(), Sound.ENTITY_BAT_AMBIENT, 0.8f, 0.5f);

        bat.getPersistentDataContainer().set(batBoomKey, PersistentDataType.BYTE, (byte) 1);

        startTrackingTask(bat);
        startProximityCheck(bat);
    }

    private void startTrackingTask(Bat bat) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (bat.isDead() || !bat.isValid()) {
                    this.cancel();
                    return;
                }

                Player nearest = null;
                double nearestDistance = Double.MAX_VALUE;

                for (Player player : bat.getWorld().getPlayers()) {
                    if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                        double distance = player.getLocation().distance(bat.getLocation());
                        if (distance <= 40 && distance < nearestDistance) {
                            nearest = player;
                            nearestDistance = distance;
                        }
                    }
                }

                if (nearest != null) {
                    if (bat.getTarget() == null || !bat.getTarget().equals(nearest)) {
                        bat.setTarget(nearest);
                    }

                    if (nearestDistance > 2) {
                        Vector direction = nearest.getLocation().toVector()
                                .subtract(bat.getLocation().toVector())
                                .normalize()
                                .multiply(0.5);
                        bat.setVelocity(direction);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void startProximityCheck(Bat bat) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (bat.isDead() || !bat.isValid()) {
                    this.cancel();
                    return;
                }

                for (Entity nearby : bat.getNearbyEntities(2.5, 2.5, 2.5)) {
                    if (nearby instanceof Player player &&
                            (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) &&
                            bat.hasLineOfSight(player)) {

                        startExplosionSequence(bat, player);
                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.getEntity() instanceof Bat bat && isBatBoom(bat)) {
            if (event.getTarget() instanceof Player player &&
                    (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)) {
                event.setCancelled(false);
            } else {
                // Solo perseguir jugadores en Survival/Adventure
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Bat bat && isBatBoom(bat)) {
            if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                    event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBatApproach(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Bat bat && isBatBoom(bat) &&
                event.getEntity() instanceof Player player &&
                (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)) {

            if (bat.getLocation().distance(player.getLocation()) <= 2.5 &&
                    bat.hasLineOfSight(player)) {
                event.setCancelled(true);
                startExplosionSequence(bat, player);
            }
        }
    }


    private void startExplosionSequence(Bat bat, Player player) {
        if (bat.hasMetadata("exploding")) {
            return;
        }

        bat.setMetadata("exploding", new FixedMetadataValue(plugin, true));
        bat.setAI(false);
        bat.setGliding(true);

        bat.getWorld().playSound(bat.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1.5f, 1.2f);

        new BukkitRunnable() {
            int count = 0;
            Location startLoc = bat.getLocation().clone();

            @Override
            public void run() {
                if (count >= 6 || bat.isDead()) {
                    explode(bat, player);
                    this.cancel();
                    return;
                }

                if (count % 2 == 0) {
                    bat.setGlowing(!bat.isGlowing());
                    bat.getWorld().playSound(bat.getLocation(),
                            Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.5f);
                }

                int particles = 5 + (count * 2);
                bat.getWorld().spawnParticle(Particle.FLAME, bat.getLocation(), particles,
                        0.2, 0.2, 0.2, 0.05);
                bat.getWorld().spawnParticle(Particle.LARGE_SMOKE, bat.getLocation(),
                        particles/2, 0.2, 0.2, 0.2, 0.02);

                double yOffset = Math.sin(count * 0.5) * 0.1;
                bat.teleport(startLoc.clone().add(0, yOffset, 0));

                count++;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void explode(Bat bat, Player player) {
        Location explosionLoc = bat.getLocation();

        if (player.isBlocking()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 0));
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.8f);
        } else {
            bat.getWorld().createExplosion(explosionLoc, 10.0f, false, false);
            bat.getWorld().spawnParticle(Particle.EXPLOSION, explosionLoc, 1, 0, 0, 0, 0.1);

            for (Entity nearby : bat.getNearbyEntities(8, 8, 8)) {
                if (nearby instanceof Player nearbyPlayer &&
                        (nearbyPlayer.getGameMode() == GameMode.SURVIVAL || nearbyPlayer.getGameMode() == GameMode.ADVENTURE)) {
                    applyEffects(nearbyPlayer);
                }
            }

            new BukkitRunnable() {
                double radius = 0;

                @Override
                public void run() {
                    if (radius > 8) {
                        this.cancel();
                        return;
                    }

                    for (double theta = 0; theta <= Math.PI; theta += Math.PI / 10) {
                        double dy = radius * Math.cos(theta);
                        for (double phi = 0; phi <= 2 * Math.PI; phi += Math.PI / 10) {
                            double dx = radius * Math.sin(theta) * Math.cos(phi);
                            double dz = radius * Math.sin(theta) * Math.sin(phi);

                            Location particleLoc = explosionLoc.clone().add(dx, dy, dz);
                            bat.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0.01);
                            bat.getWorld().spawnParticle(Particle.LARGE_SMOKE, particleLoc, 1, 0, 0, 0, 0.01);
                        }
                    }

                    radius += 0.5;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }

        bat.remove();
    }

    private void applyEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 600, 4));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 600, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 600, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 600, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 600, 4));
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 300, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 600, 4));
        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 600, 2));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_HURT, 1.0f, 0.5f);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Bat bat && isBatBoom(bat)) {
            event.getDrops().clear();
            event.setDroppedExp(30);
        }
    }

    public NamespacedKey getBatBoomKey() {
        return batBoomKey;
    }

    public boolean isBatBoom(Bat bat) {
        return bat.getPersistentDataContainer().has(batBoomKey, PersistentDataType.BYTE);
    }
}