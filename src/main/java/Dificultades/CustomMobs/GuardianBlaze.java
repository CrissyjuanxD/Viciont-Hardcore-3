package Dificultades.CustomMobs;

import items.BlazeItems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class GuardianBlaze implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey guardianblazeKey;
    private final Random random = new Random();
    private boolean eventsRegistered = false;

    public GuardianBlaze(JavaPlugin plugin) {
        this.plugin = plugin;
        this.guardianblazeKey = new NamespacedKey(plugin, "guardian_blaze");
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
                    if (entity instanceof Blaze blaze && isGuardianBlaze(blaze)) {
                        blaze.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public void spawnGuardianBlaze(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        Blaze blaze = (Blaze) world.spawnEntity(location, EntityType.BLAZE);

        blaze.setCustomName(ChatColor.GOLD + "" + ChatColor.BOLD + "Guardian Blaze");
        blaze.setCustomNameVisible(true);
        Objects.requireNonNull(blaze.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(40.0);
        blaze.setHealth(40.0);
        Objects.requireNonNull(blaze.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(12.0);
        Objects.requireNonNull(blaze.getAttribute(Attribute.GENERIC_SCALE)).setBaseValue(1.9);
        blaze.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
        blaze.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 1));

        blaze.getPersistentDataContainer().set(guardianblazeKey, PersistentDataType.BYTE, (byte) 1);

        startBehavior(blaze);
    }

    private void startBehavior(Blaze blaze) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!blaze.isValid()) {
                    cancel();
                    return;
                }

                Player target = getClosestPlayer(blaze, 25);
                if (target != null) {
                    Vector direction = target.getLocation().toVector().subtract(blaze.getLocation().toVector()).normalize();
                    blaze.setVelocity(direction.multiply(0.35));

                    if (blaze.getLocation().distance(target.getLocation()) <= 3) {
                        blaze.attack(target);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);

        new BukkitRunnable() {
            int attackCycle = 0;

            @Override
            public void run() {
                if (!blaze.isValid()) {
                    cancel();
                    return;
                }

                attackCycle++;

                if (attackCycle % 8 == 0) {
                    if (random.nextBoolean()) {
                        launchTripleFireballAttack(blaze);
                    } else {
                        spawnCircleParticles(blaze);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void launchTripleFireballAttack(Blaze blaze) {
        List<Player> targets = getRandomPlayers(blaze, 25, 3);
        if (targets.isEmpty()) return;

        blaze.getWorld().playSound(blaze.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 2.0f, 0.5f);

        new BukkitRunnable() {
            int rounds = 0;

            @Override
            public void run() {
                if (rounds >= 3 || !blaze.isValid()) {
                    cancel();
                    return;
                }

                for (Player target : targets) {
                    if (target.isValid()) {
                        launchTriangulatedFireballs(blaze, target);
                    }
                }

                rounds++;
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void launchTriangulatedFireballs(Blaze blaze, Player target) {
        Location adjustedLoc = blaze.getLocation().clone().add(0, 2.5, 0);
        Vector baseDirection = target.getLocation().toVector().subtract(adjustedLoc.toVector()).normalize();

        for (int i = 0; i < 3; i++) {
            Vector direction = baseDirection.clone();
            Location spawnLoc = adjustedLoc.clone();

            switch (i) {
                case 0:
                    break;
                case 1:
                    direction.add(new Vector(-0.3, 0, 0.3));
                    spawnLoc.add(-0.5, 0, 0.5);
                    break;
                case 2:
                    direction.add(new Vector(0.3, 0, -0.3));
                    spawnLoc.add(0.5, 0, -0.5);
                    break;
            }

            Fireball fireball = blaze.getWorld().spawn(spawnLoc, Fireball.class);
            fireball.setDirection(direction.normalize());
            fireball.setVelocity(direction.multiply(1.5));
            fireball.setYield(3);
            fireball.setIsIncendiary(true);
            fireball.setShooter(blaze);
            fireball.getPersistentDataContainer().set(new NamespacedKey(plugin, "custom_fireball"), PersistentDataType.BYTE, (byte) 1);
        }
    }

    private void spawnCircleParticles(Blaze blaze) {
        blaze.getWorld().playSound(blaze.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.7f);
        Set<Player> damagedPlayers = new HashSet<>();

        new BukkitRunnable() {
            double radius = 0;
            int cycles = 0;

            @Override
            public void run() {
                if (cycles >= 30 || !blaze.isValid()) {
                    damagedPlayers.clear();
                    cancel();
                    return;
                }

                radius += 0.5;
                double increment = Math.PI / 12;

                for (double angle = 0; angle < 2 * Math.PI; angle += increment) {
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    Location loc = blaze.getLocation().clone().add(x, 1, z);
                    blaze.getWorld().spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0.05);
                }

                if (cycles % 3 == 0) {
                    blaze.getWorld().spawnParticle(Particle.LAVA, blaze.getLocation().add(0, 1, 0), 5, radius, 0.5, radius, 0.1);
                }

                if (radius >= 2) {
                    for (Entity entity : blaze.getWorld().getNearbyEntities(blaze.getLocation(), radius, 2, radius)) {
                        if (entity instanceof Player player && !damagedPlayers.contains(player)) {
                            player.damage(3, blaze);
                            player.setFireTicks(100);
                            damagedPlayers.add(player);
                        }
                    }
                }

                cycles++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Player getClosestPlayer(Blaze blaze, double radius) {
        return blaze.getWorld().getPlayers().stream()
                .filter(p -> p.getLocation().distance(blaze.getLocation()) <= radius)
                .filter(p -> p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(blaze.getLocation())))
                .orElse(null);
    }

    private List<Player> getRandomPlayers(Blaze blaze, double radius, int count) {
        List<Player> players = blaze.getWorld().getPlayers().stream()
                .filter(p -> p.getLocation().distance(blaze.getLocation()) <= radius)
                .filter(p -> p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)
                .collect(Collectors.toList());

        Collections.shuffle(players);
        return players.stream().limit(count).collect(Collectors.toList());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Blaze blaze && isGuardianBlaze(blaze)) {
            if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                event.setCancelled(true);
            } else if (event.getEntity() instanceof Player player) {
                Vector knockback = player.getLocation().toVector().subtract(blaze.getLocation().toVector()).normalize();
                knockback.setY(0.5);
                player.setVelocity(knockback.multiply(1.5));
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Blaze blaze && isGuardianBlaze(blaze)) {
            if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                    event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onFireballHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Fireball fireball &&
                fireball.getPersistentDataContainer().has(new NamespacedKey(plugin, "custom_fireball"), PersistentDataType.BYTE) &&
                event.getEntity() instanceof Player player) {
            event.setDamage(8.0);
            player.setFireTicks(100);
        }
    }

    @EventHandler
    public void onFireballExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof Fireball fireball &&
                fireball.getPersistentDataContainer().has(new NamespacedKey(plugin, "custom_fireball"), PersistentDataType.BYTE)) {
            event.blockList().clear();

            Location loc = fireball.getLocation();
            fireball.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 3);
            fireball.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Blaze blaze && isGuardianBlaze(blaze)) {
            event.getDrops().clear();

            if (random.nextInt(100) < 90) {
                event.getDrops().add(new ItemStack(Material.NETHERITE_SCRAP, getRandomNetheriteScrapAmount()));
            }

            if (random.nextBoolean()) {
                event.getDrops().add(BlazeItems.createBlazeRod());
            }
        }
    }

    private int getRandomNetheriteScrapAmount() {
        int randomValue = random.nextInt(100);
        if (randomValue < 50) return 1;
        if (randomValue < 80) return 2;
        return 3;
    }

    private boolean isGuardianBlaze(Blaze blaze) {
        return blaze.getPersistentDataContainer().has(guardianblazeKey, PersistentDataType.BYTE);
    }
}