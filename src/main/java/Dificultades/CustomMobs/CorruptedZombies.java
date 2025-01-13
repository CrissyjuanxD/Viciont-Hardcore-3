package Dificultades.CustomMobs;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
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

    public CorruptedZombies(JavaPlugin plugin) {
        this.plugin = plugin;
        this.corruptedKey = new NamespacedKey(plugin, "corrupted_zombie");
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


    // Crear un Corrupted Zombie personalizado
    public Zombie spawnCorruptedZombie(Location location) {
        Zombie CorruptedZombie = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        CorruptedZombie.getPersistentDataContainer().set(corruptedKey, PersistentDataType.BYTE, (byte) 1);
        CorruptedZombie.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Zombie");
        CorruptedZombie.setCustomNameVisible(true);
        CorruptedZombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0)); // Velocidad
        startSnowballRunnable(CorruptedZombie);
        return CorruptedZombie;
    }

    private boolean isCorrupted(Zombie zombie) {
        return zombie.getPersistentDataContainer().has(corruptedKey, PersistentDataType.BYTE);
    }

    // Runnable para lanzar bolas de nieve
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
                // Verificar que el jugador está en rango
                if (isPlayerInRange(zombie, player) && Math.random() < 0.3) {
                    lanzarSnowball(zombie, player);
                }
            }
        }, 0L, 40L).getTaskId();

        zombieTasks.put(zombie.getUniqueId(), taskId);
    }

    @EventHandler
    public void onZombieDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Zombie zombie && isCorrupted(zombie)) {
            Integer taskId = zombieTasks.remove(zombie.getUniqueId());
            if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private boolean isPlayerInRange(Zombie zombie, Player player) {
        if (!zombie.getWorld().equals(player.getWorld())) {
            return false;
        }
        double distanceXZ = zombie.getLocation().distanceSquared(player.getLocation()) - Math.pow(zombie.getLocation().getY() - player.getLocation().getY(), 2);
        double distanceY = Math.abs(zombie.getLocation().getY() - player.getLocation().getY());
        return distanceXZ <= 7 * 7 && distanceY <= 7;
    }

    @EventHandler
    public void onZombieAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Zombie zombie && event.getEntity() instanceof Player player && isCorrupted(zombie)) {
            event.setDamage(2);
        }
    }

    private void lanzarSnowball(Zombie zombie, Player player) {
        Snowball snowball = zombie.launchProjectile(Snowball.class);

        Vector direction = player.getLocation().toVector().subtract(zombie.getLocation().toVector());

        // Evitar errores de NaN
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
            }
        }, 20L);
    }

    @EventHandler
    public void onSnowballHit(EntityDamageByEntityEvent event) {
        // Verificar que el proyectil es una bola de nieve del zombie
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
                // Si golpea otro mob, cancelar el daño para evitar peleas
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

}
