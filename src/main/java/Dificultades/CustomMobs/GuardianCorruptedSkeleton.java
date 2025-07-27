package Dificultades.CustomMobs;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class GuardianCorruptedSkeleton implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey gcorruptedskelKey;
    private final NamespacedKey guardianProjectileKey;
    private final Random random = new Random();
    private boolean eventsRegistered = false;
    private final Map<Creature, Integer> skeletonTasks = new HashMap<>();

    public GuardianCorruptedSkeleton(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gcorruptedskelKey = new NamespacedKey(plugin, "guardian_corrupted_skeleton");
        this.guardianProjectileKey = new NamespacedKey(plugin, "guardian_projectile");
    }

    public void apply() {
        if (!eventsRegistered) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
        }
    }

    public void revert() {
        if (eventsRegistered) {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof WitherSkeleton skeleton && isCorruptedWither(skeleton)) {
                        skeleton.remove();
                    }
                }
            }
            for (Integer taskId : skeletonTasks.values()) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            skeletonTasks.clear();
            eventsRegistered = false;
        }
    }

    public WitherSkeleton spawnGuardianCorruptedSkeleton(Location location) {
        WitherSkeleton witherSkeleton = (WitherSkeleton) location.getWorld().spawnEntity(location, EntityType.WITHER_SKELETON);
        applyGuardianCorruptedSkeletonAttributes(witherSkeleton);
        return witherSkeleton;
    }

    public void transformToCorruptedSkeleton(WitherSkeleton skeleton) {
        applyGuardianCorruptedSkeletonAttributes(skeleton);
    }

    private void applyGuardianCorruptedSkeletonAttributes(WitherSkeleton skeleton) {
        skeleton.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Guardian Corrupted Skeleton");
        skeleton.setCustomNameVisible(false);

        Objects.requireNonNull(skeleton.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(30.0);
        skeleton.setHealth(30.0);
        Objects.requireNonNull(skeleton.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(6.0);
        skeleton.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0));

        skeleton.getPersistentDataContainer().set(gcorruptedskelKey, PersistentDataType.BYTE, (byte) 1);
        startSkullRunnable(skeleton);
    }

    private boolean isCorruptedWither(WitherSkeleton skeleton) {
        return skeleton.getPersistentDataContainer().has(gcorruptedskelKey, PersistentDataType.BYTE);
    }

    private void startSkullRunnable(WitherSkeleton skeleton) {
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (skeleton == null || skeleton.isDead() || !isCorruptedWither(skeleton)) {
                skeletonTasks.remove(skeleton);
                return;
            }

            LivingEntity target = skeleton.getTarget();
            if (target instanceof Player player) {
                if (skeleton.getLocation().distance(player.getLocation()) <= 20) {
                    if (random.nextDouble() < 0.5) {
                        launchSkull(skeleton, player);
                    }
                }
            }
        }, 0L, 60L).getTaskId();
        skeletonTasks.put(skeleton, taskId);
    }

    private void launchSkull(WitherSkeleton skeleton, Player player) {
        Vector direction = player.getLocation().toVector().subtract(skeleton.getLocation().toVector());
        if (direction.lengthSquared() == 0) {
            direction = new Vector(0, 0.1, 0);
        } else {
            direction = direction.normalize().multiply(1.5);
        }
        WitherSkull skull = skeleton.launchProjectile(WitherSkull.class, direction);
        PersistentDataContainer data = skull.getPersistentDataContainer();
        data.set(guardianProjectileKey, PersistentDataType.BYTE, (byte) 1);
        skull.setCustomName("Corrupted Skeleton Skull");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!skull.isValid()) {
                    cancel();
                    return;
                }
                Location loc = skull.getLocation();
                skull.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 5, 0.2, 0.2, 0.2, 0.01);
                skull.getWorld().spawnParticle(Particle.SMALL_FLAME, loc, 3, 0.1, 0.1, 0.1, 0.01);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (skull.isValid()) {
                skull.getWorld().playSound(skull.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0F, 1.0F);
            }
        }, 5L);
    }

    @EventHandler
    public void onSkullHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof WitherSkull skull)) return;
        PersistentDataContainer data = skull.getPersistentDataContainer();
        if (!data.has(guardianProjectileKey, PersistentDataType.BYTE)) return;

        Location impactLocation = skull.getLocation();
        World world = impactLocation.getWorld();
        if (world != null) {
            world.spawnParticle(Particle.EXPLOSION, impactLocation, 5, 0.5, 0.5, 0.5);
            world.spawnParticle(Particle.SMOKE, impactLocation, 10, 0.5, 0.5, 0.5);
            world.playSound(impactLocation, Sound.ENTITY_WITHER_HURT, 1.0F, 0.7F);
        }

        if (event.getHitEntity() instanceof WitherSkeleton hitSkeleton && isCorruptedWither(hitSkeleton)) {
            skull.remove();
            return;
        }

        if (event.getHitEntity() instanceof Player target) {
            if (target.isBlocking()) {
                skull.remove();
                target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);
                return;
            }

            int duration = 200;
            int amplifier = 4;
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration, amplifier, false, true));
        }
        skull.remove();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof WitherSkeleton skeleton && isCorruptedWither(skeleton)) {

            if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                    event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                event.setCancelled(true);
            }
        }
    }

    public NamespacedKey getGCSkeletonKey() {
        return gcorruptedskelKey ;
    }

    @EventHandler
    public void onSkeletonAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof WitherSkeleton skeleton)) return;
        if (!isCorruptedWither(skeleton)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        if (target.isBlocking()) {
            event.setCancelled(true);
            target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);
            return;
        }
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 120, 2, false, true));
    }

    @EventHandler
    public void onSkeletonDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof WitherSkeleton skeleton && isCorruptedWither(skeleton)) {
            Integer taskId = skeletonTasks.remove(skeleton);
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Entity entity : event.getWorld().getEntities()) {
                if (entity instanceof WitherSkeleton skeleton && isCorruptedWither(skeleton)) {
                    transformToCorruptedSkeleton(skeleton);
                }
            }
        }, 20L);
    }
}
