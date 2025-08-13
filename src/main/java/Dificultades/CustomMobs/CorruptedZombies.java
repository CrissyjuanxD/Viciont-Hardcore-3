package Dificultades.CustomMobs;

import Dificultades.Features.MobSoundManager;
import items.CorruptedMobItems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class CorruptedZombies implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey corruptedKey;
    private final Map<UUID, Integer> zombieTasks = new HashMap<>();
    private boolean eventsRegistered = false;
    private final Random random = new Random();
    private final MobSoundManager soundManager;

    public CorruptedZombies(JavaPlugin plugin) {
        this.plugin = plugin;
        this.corruptedKey = new NamespacedKey(plugin, "corrupted_zombie");
        this.soundManager = new MobSoundManager(plugin);
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
                    if (entity instanceof Zombie zombie && isCorrupted(zombie)) {
                        zombie.remove();
                    }
                }
            }

            for (Integer taskId : zombieTasks.values()) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            zombieTasks.clear();
            eventsRegistered = false;
        }
    }

    public Zombie spawnCorruptedZombie(Location location) {
        Zombie CorruptedZombie = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        applyCorruptedZombieAttributes(CorruptedZombie);
        return CorruptedZombie;
    }

    public void transformToCorruptedZombie(Zombie zombie) {
        applyCorruptedZombieAttributes(zombie);
    }

    private void applyCorruptedZombieAttributes(Zombie zombie) {
        zombie.getPersistentDataContainer().set(corruptedKey, PersistentDataType.BYTE, (byte) 1);
        zombie.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Zombie");
        zombie.setCustomNameVisible(false);
        zombie.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(32);
        Objects.requireNonNull(zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(3.0);
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0));
        zombie.setSilent(true);
        soundManager.addCustomMob(zombie);

        startSnowballRunnable(zombie);

        if (zombie.getVehicle() instanceof Chicken) {
            zombie.getVehicle().remove();
        }
    }

    private boolean isCorrupted(Zombie zombie) {
        return zombie.getPersistentDataContainer().has(corruptedKey, PersistentDataType.BYTE);
    }

    public NamespacedKey getCorruptedKey() {
        return corruptedKey;
    }

    private void startSnowballRunnable(Zombie zombie) {
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (zombie == null || zombie.isDead() || !isCorrupted(zombie)) {
                Integer zombieTaskId = zombieTasks.remove(zombie.getUniqueId());
                if (zombieTaskId != null) {
                    Bukkit.getScheduler().cancelTask(zombieTaskId);
                }
                return;
            }

            if (zombie.getTarget() instanceof Player player) {
                if (isPlayerInRange(zombie, player) && Math.random() < 0.4) {
                    lanzarSnowball(zombie, player);
                }
            }
        }, 0L, 40L).getTaskId();

        zombieTasks.put(zombie.getUniqueId(), taskId);
    }

    private boolean isPlayerInRange(Zombie zombie, Player player) {
        if (!zombie.getWorld().equals(player.getWorld())) {
            return false;
        }
        double distanceXZ = zombie.getLocation().distanceSquared(player.getLocation()) - Math.pow(zombie.getLocation().getY() - player.getLocation().getY(), 2);
        double distanceY = Math.abs(zombie.getLocation().getY() - player.getLocation().getY());
        return distanceXZ <= 15 * 15 && distanceY <= 15;
    }

    private void lanzarSnowball(Zombie zombie, Player player) {
        Snowball snowball = zombie.launchProjectile(Snowball.class);

        Vector direction = player.getLocation().toVector().subtract(zombie.getLocation().toVector());

        if (direction.lengthSquared() == 0) {
            direction = new Vector(0, 0.1, 0);
        } else {
            direction = direction.normalize().multiply(1.5);
        }

        snowball.setVelocity(direction);
        snowball.setCustomName("Corrupted Zombie Snowball");
        snowball.setMetadata("knockback", new FixedMetadataValue(plugin, 2));

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (snowball.isValid()) {
                snowball.getWorld().spawnParticle(Particle.PORTAL, snowball.getLocation(), 10);
                snowball.getWorld().spawnParticle(Particle.SMOKE, snowball.getLocation(), 5, 0.2, 0.2, 0.2, 0.1);
            }
        }, 0L, 1L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (snowball.isValid()) {
                snowball.getWorld().playSound(snowball.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0F, 2.0F);
                snowball.getWorld().playSound(snowball.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0F, 0.8F);
            }
        }, 20L);
    }

    @EventHandler
    public void onSnowballHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Snowball snowball &&
                "Corrupted Zombie Snowball".equals(snowball.getCustomName())) {

            if (event.getEntity() instanceof Player player) {
                event.setDamage(2);
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 50, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0));

                int knockbackLevel = snowball.getMetadata("knockback").stream()
                        .findFirst().map(MetadataValue::asInt).orElse(0);
                Vector knockback = player.getLocation().toVector()
                        .subtract(snowball.getLocation().toVector()).normalize().multiply(knockbackLevel);
                player.setVelocity(knockback);
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Snowball snowball &&
                "Corrupted Zombie Snowball".equals(snowball.getCustomName())) {

            Location impactLocation = snowball.getLocation();

            List<Player> nearbyPlayers = snowball.getWorld().getPlayers().stream()
                    .filter(player -> player.getLocation().distance(impactLocation) <= 4)
                    .toList();

            for (Player player : nearbyPlayers) {
                Vector knockback = player.getLocation().toVector()
                        .subtract(impactLocation.toVector()).normalize().multiply(1);
                player.setVelocity(knockback);
            }
            snowball.getWorld().spawnParticle(Particle.EXPLOSION, impactLocation, 8);
            snowball.getWorld().playSound(impactLocation, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0F, 2.0F);
        }
    }

    //SONIDOS
    @EventHandler
    public void onZombieHurt(EntityDamageEvent event) {
        if (event.getEntity() instanceof Zombie zombie && isCorrupted(zombie)) {
            zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_ZOMBIE_HURT, SoundCategory.HOSTILE, 1.0f, 0.6f);
        }
    }

    @EventHandler
    public void onZombieDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Zombie zombie && isCorrupted(zombie)) {
            soundManager.removeCustomMob(zombie);
            zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_ZOMBIE_DEATH, SoundCategory.HOSTILE, 1.0f, 0.6f);
            Integer taskId = zombieTasks.remove(zombie.getUniqueId());
            if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);

            if (Math.random() <= 0.30) {
                zombie.getWorld().dropItemNaturally(zombie.getLocation(), CorruptedMobItems.createCorruptedMeet());
            }
        }
    }

}
