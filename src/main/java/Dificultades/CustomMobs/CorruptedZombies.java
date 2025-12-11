package Dificultades.CustomMobs;

import Dificultades.Features.MobSoundManager;
import items.CorruptedMobItems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class CorruptedZombies implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey corruptedKey;

    private static final Set<UUID> activeZombies = new HashSet<>();
    private static boolean eventsRegistered = false;
    private static BukkitTask mainTask;

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

            scanExistingZombies();
            startCentralTask();
        }
    }

    public void revert() {
        if (eventsRegistered) {
            if (mainTask != null && !mainTask.isCancelled()) {
                mainTask.cancel();
            }

            for (UUID uuid : activeZombies) {
                Entity entity = Bukkit.getEntity(uuid);
                if (entity instanceof Zombie && entity.isValid()) {
                    entity.remove();
                }
            }
            activeZombies.clear();
            eventsRegistered = false;
        }
    }

    private void scanExistingZombies() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(Zombie.class)) {
                if (isCorrupted((Zombie) entity)) {
                    activeZombies.add(entity.getUniqueId());
                }
            }
        }
    }

    private void startCentralTask() {
        if (mainTask != null && !mainTask.isCancelled()) return;

        mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeZombies.isEmpty()) return;

                Iterator<UUID> it = activeZombies.iterator();
                while (it.hasNext()) {
                    UUID id = it.next();
                    Entity entity = Bukkit.getEntity(id);

                    if (entity == null || !entity.isValid() || entity.isDead()) {
                        if (entity != null && !entity.isValid()) it.remove();
                        continue;
                    }

                    if (entity instanceof Zombie zombie) {
                        processZombieAI(zombie);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void processZombieAI(Zombie zombie) {
        if (zombie.getTarget() instanceof Player player) {
            if (isPlayerInRange(zombie, player) && random.nextDouble() < 0.2) {
                lanzarSnowball(zombie, player);
            }
        }
    }

    public Zombie spawnCorruptedZombie(Location location) {
        Zombie CorruptedZombie = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        applyCorruptedZombieAttributes(CorruptedZombie);

        // Registrar en lista estática
        activeZombies.add(CorruptedZombie.getUniqueId());
        startCentralTask();

        return CorruptedZombie;
    }

    public void transformToCorruptedZombie(Zombie zombie) {
        applyCorruptedZombieAttributes(zombie);
        activeZombies.add(zombie.getUniqueId());
        startCentralTask();
    }

    private void applyCorruptedZombieAttributes(Zombie zombie) {
        zombie.getPersistentDataContainer().set(corruptedKey, PersistentDataType.BYTE, (byte) 1);
        zombie.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Zombie");
        zombie.setCustomNameVisible(false);
        zombie.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(32);
        Objects.requireNonNull(zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(3.0);
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0));
        zombie.setSilent(true);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (zombie.isValid() && !zombie.isDead()) {
                soundManager.addCustomMob(zombie);
            }
        }, 5L);

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

    private boolean isPlayerInRange(Zombie zombie, Player player) {
        if (!zombie.getWorld().equals(player.getWorld())) return false;
        double distanceXZ = zombie.getLocation().distanceSquared(player.getLocation()) - Math.pow(zombie.getLocation().getY() - player.getLocation().getY(), 2);
        double distanceY = Math.abs(zombie.getLocation().getY() - player.getLocation().getY());
        return distanceXZ <= 15 * 15 && distanceY <= 15;
    }

    private void lanzarSnowball(Zombie zombie, Player player) {
        WindCharge snowball = zombie.launchProjectile(WindCharge.class);

        Vector direction = player.getLocation().toVector().subtract(zombie.getLocation().toVector());
        if (direction.lengthSquared() == 0) direction = new Vector(0, 0.1, 0);
        else direction = direction.normalize().multiply(1.5);
        direction.setY(direction.getY() - 0.3);

        snowball.setVelocity(direction);
        snowball.setCustomName("Corrupted Zombie WindCharge");

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
        if (event.getDamager() instanceof WindCharge snowball &&
                "Corrupted Zombie WindCharge".equals(snowball.getCustomName())) {

            if (event.getEntity() instanceof Player player) {
                event.setDamage(2);
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 50, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0));
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onCorruptedZombieBurn(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (isCorrupted(zombie)) event.setCancelled(true);
    }

    @EventHandler
    public void onZombieHurt(EntityDamageEvent event) {
        if (event.getEntity() instanceof Zombie zombie && isCorrupted(zombie)) {
            zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_ZOMBIE_HURT, SoundCategory.HOSTILE, 1.0f, 0.6f);
        }
    }

    @EventHandler
    public void onZombieDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Zombie zombie && isCorrupted(zombie)) {
            zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_ZOMBIE_DEATH, SoundCategory.HOSTILE, 1.0f, 0.6f);

            // Limpieza inmediata
            activeZombies.remove(zombie.getUniqueId());

            if (Math.random() <= 0.30) {
                zombie.getWorld().dropItemNaturally(zombie.getLocation(), CorruptedMobItems.createCorruptedMeet());
            }
        }
    }
}