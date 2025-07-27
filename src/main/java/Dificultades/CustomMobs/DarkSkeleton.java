package Dificultades.CustomMobs;

import Dificultades.Features.DarkMobSB;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class DarkSkeleton extends DarkMobSB implements Listener {
    private boolean eventsRegistered = false;

    public DarkSkeleton(JavaPlugin plugin) {
        super(plugin, "dark_skeleton");
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

    public Skeleton spawnDarkSkeleton(Location location) {
        Skeleton skeleton = (Skeleton) location.getWorld().spawnEntity(location, EntityType.SKELETON);
        applyDarkSkeletonAttributes(skeleton);
        return skeleton;
    }

    private void applyDarkSkeletonAttributes(Skeleton skeleton) {
        skeleton.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Dark Skeleton");
        skeleton.setCustomNameVisible(false);

        skeleton.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(80);
        skeleton.setHealth(80);
        skeleton.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3);

        ItemStack darkBow = new ItemStack(Material.BOW);
        darkBow.addUnsafeEnchantment(Enchantment.POWER, 20);
        darkBow.addUnsafeEnchantment(Enchantment.INFINITY, 1);
        darkBow.addUnsafeEnchantment(Enchantment.UNBREAKING, 10);
        skeleton.getEquipment().setItemInMainHand(darkBow);
        skeleton.getEquipment().setItemInMainHandDropChance(0);

        skeleton.getEquipment().setItemInOffHand(new ItemStack(Material.ARROW, 1));
        skeleton.getEquipment().setItemInOffHandDropChance(0);

        skeleton.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                PotionEffect.INFINITE_DURATION,
                1,
                false, false
        ));

        skeleton.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);

        startParticleTask(skeleton);
    }

    private void startParticleTask(Skeleton skeleton) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (skeleton.isDead() || !skeleton.isValid()) {
                    this.cancel();
                    return;
                }

                skeleton.getWorld().spawnParticle(
                        Particle.LARGE_SMOKE,
                        skeleton.getLocation().add(0, 1, 0),
                        5,
                        0.3, 0.5, 0.3, 0.05
                );

                skeleton.getWorld().spawnParticle(
                        Particle.SOUL_FIRE_FLAME,
                        skeleton.getLocation(),
                        3,
                        0.3, 0.3, 0.3, 0.05
                );
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    @EventHandler
    public void onDarkSkeletonShoot(EntityShootBowEvent event) {
        if (isCustomMob(event.getEntity()) && event.getEntity() instanceof Skeleton skeleton) {
            if (event.getProjectile() instanceof Arrow arrow) {
                arrow.setColor(Color.BLACK);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (arrow.isDead() || !arrow.isValid()) {
                            this.cancel();
                            return;
                        }

                        arrow.getWorld().spawnParticle(
                                Particle.SQUID_INK,
                                arrow.getLocation(),
                                1,
                                0, 0, 0, 0
                        );
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
        }
    }

    @EventHandler
    public void onDarkSkeletonArrowHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Arrow arrow &&
                arrow.getShooter() instanceof Skeleton skeleton &&
                isCustomMob(skeleton)) {

            if (event.getEntity() instanceof LivingEntity target) {
                target.addPotionEffect(new PotionEffect(
                        PotionEffectType.DARKNESS,
                        600,
                        0,
                        false, true
                ));

                target.getWorld().spawnParticle(
                        Particle.SOUL_FIRE_FLAME,
                        target.getLocation(),
                        15,
                        0.5, 0.5, 0.5, 0.1
                );
                target.getWorld().playSound(
                        target.getLocation(),
                        Sound.ENTITY_WARDEN_SONIC_CHARGE,
                        1.0f,
                        1.5f
                );
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (isCustomMob(event.getEntity())) {
            Skeleton skeleton = (Skeleton) event.getEntity();
            event.getDrops().clear();

            skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.5f, 1.0f);
            skeleton.getWorld().spawnParticle(
                    Particle.SOUL,
                    skeleton.getLocation(),
                    50,
                    0.5, 0.5, 0.5, 0.3
            );
        }
    }

    @EventHandler
    public void onDarkSkeletonHurt(EntityDamageEvent event) {
        if (isCustomMob(event.getEntity())) {
            Skeleton skeleton = (Skeleton) event.getEntity();
            skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_WARDEN_HURT, 1.2f, 1.0f);

            skeleton.getWorld().spawnParticle(
                    Particle.DAMAGE_INDICATOR,
                    skeleton.getLocation(),
                    20,
                    0.5, 0.5, 0.5, 0.1
            );
        }
    }

    @Override
    public boolean isCustomMob(Entity entity) {
        return entity instanceof Skeleton &&
                entity.getPersistentDataContainer().has(mobKey, PersistentDataType.BYTE);
    }
}