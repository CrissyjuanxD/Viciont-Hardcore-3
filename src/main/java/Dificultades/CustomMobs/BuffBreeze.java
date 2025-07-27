package Dificultades.CustomMobs;

import items.HelmetNetheriteEssence;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

public class BuffBreeze implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey buffBreezeKey;
    private boolean eventsRegistered = false;
    private final Random random = new Random();
    private final HelmetNetheriteEssence helmetNetheriteEssence;

    public BuffBreeze(JavaPlugin plugin) {
        this.plugin = plugin;
        this.helmetNetheriteEssence = new HelmetNetheriteEssence(plugin);
        this.buffBreezeKey = new NamespacedKey(plugin, "buff_breeze");
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
                    if (entity instanceof Breeze breeze &&
                            breeze.getPersistentDataContainer().has(buffBreezeKey, PersistentDataType.BYTE)) {
                        breeze.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public Breeze spawnBuffBreeze(Location location) {
        Breeze buffBreeze = (Breeze) location.getWorld().spawnEntity(location, EntityType.BREEZE);
        applyBuffBreezeAttributes(buffBreeze);
        startSnowballAttack(buffBreeze);
        return buffBreeze;
    }

    private void applyBuffBreezeAttributes(Breeze breeze) {
        breeze.setCustomName(ChatColor.AQUA + "" + ChatColor.BOLD + "Buff Breeze");
        breeze.setCustomNameVisible(true);
        breeze.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(60);
        breeze.setHealth(60);
        breeze.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(32);

        breeze.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0));

        breeze.getPersistentDataContainer().set(buffBreezeKey, PersistentDataType.BYTE, (byte) 1);
    }

    private void startSnowballAttack(Breeze breeze) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (breeze.isDead() || !breeze.isValid()) {
                    this.cancel();
                    return;
                }

                LivingEntity target = breeze.getTarget();
                if (target != null && target instanceof Player) {
                    breeze.getWorld().playSound(breeze.getLocation(), Sound.ENTITY_BREEZE_CHARGE, 5.0f, 0.6f);
                    breeze.getWorld().playSound(breeze.getLocation(), Sound.ENTITY_BREEZE_INHALE, 5.0f, 0.6f);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (breeze.isValid() && target.isValid()) {
                                launchCustomSnowball(breeze, target);
                            }
                        }
                    }.runTaskLater(plugin, 30L);
                }
            }
        }.runTaskTimer(plugin, 0L, 80L);
    }

    private void launchCustomSnowball(Breeze shooter, LivingEntity target) {
        shooter.getWorld().playSound(shooter.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 5.0f, 0.6f);

        Snowball snowball = shooter.launchProjectile(Snowball.class);
        snowball.getPersistentDataContainer().set(buffBreezeKey, PersistentDataType.BYTE, (byte) 1);

        ItemStack snowballItem = new ItemStack(Material.SNOWBALL);
        ItemMeta meta = snowballItem.getItemMeta();
        meta.setCustomModelData(5);
        snowballItem.setItemMeta(meta);
        snowball.setItem(snowballItem);

        Location targetLocation = target.getLocation().clone();
        if (target instanceof Player) {
            Vector velocity = target.getVelocity();
            double distance = shooter.getLocation().distance(targetLocation);
            double timeToImpact = distance / 1.5;
            targetLocation.add(velocity.multiply(timeToImpact * 0.5));
        }

        Vector direction = targetLocation.toVector()
                .subtract(shooter.getLocation().toVector())
                .normalize()
                .multiply(1.5);
        snowball.setVelocity(direction);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball) ||
                !snowball.getPersistentDataContainer().has(buffBreezeKey, PersistentDataType.BYTE)) {
            return;
        }

        snowball.getWorld().playSound(snowball.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 5.0f, 0.6f);
        snowball.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, snowball.getLocation(), 30, 2.5, 2.5, 2.5, 0.1);

        int effect = random.nextInt(4) + 1;
        boolean hitBlock = event.getHitBlock() != null;
        boolean hitEntity = event.getHitEntity() != null;

        if (hitBlock) {
            for (Entity nearby : snowball.getWorld().getNearbyEntities(
                    snowball.getLocation(), 5.0, 5.0, 5.0)) {
                if (nearby instanceof Player player) {
                    applyEffect(effect, player, snowball);
                }
            }
        }
        else if (hitEntity && event.getHitEntity() instanceof Player player) {
            applyEffect(effect, player, snowball);
        }
    }

    private void applyEffect(int effect, Player target, Snowball snowball) {
        switch (effect) {
            case 1:
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 200, 4, true, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 4, true, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 4, true, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 4, true, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 4, true, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 4, true, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 200, 4, true, true));
                target.damage(6);
                target.getWorld().playEffect(target.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 0.5f);
                break;

            case 2:
                snowball.getWorld().createExplosion(
                        snowball.getLocation(),
                        3.0f,
                        false,
                        false,
                        snowball.getShooter() instanceof Entity ? (Entity) snowball.getShooter() : null
                );
                break;

            case 3:
                if (snowball.getShooter() instanceof Breeze shooter && shooter.isValid()) {
                    Location safeLocation = shooter.getLocation().clone();
                    safeLocation.setYaw(target.getLocation().getYaw());
                    safeLocation.setPitch(target.getLocation().getPitch());

                    target.teleport(safeLocation);
                    target.getWorld().playEffect(target.getLocation(), Effect.ENDER_SIGNAL, 0);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

                    String[] mobOptions = {"corruptedskeleton", "iceologer", "corruptedspider"};
                    String selectedMob = mobOptions[random.nextInt(mobOptions.length)];

                    String command = String.format("spawnvct %s %f %f %f",
                            selectedMob,
                            safeLocation.getX(),
                            safeLocation.getY(),
                            safeLocation.getZ());

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
                break;
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Breeze breeze) ||
                !breeze.getPersistentDataContainer().has(buffBreezeKey, PersistentDataType.BYTE)) {
            return;
        }

        if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();

            if (projectile instanceof Arrow) {
                double originalDamage = event.getDamage();
                double reducedDamage = originalDamage * 0.25;
                event.setDamage(reducedDamage);

                breeze.getWorld().playEffect(breeze.getLocation(), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Breeze breeze) ||
                !breeze.getPersistentDataContainer().has(buffBreezeKey, PersistentDataType.BYTE)) {
            return;
        }

        event.getDrops().clear();
        if (Math.random() <= 0.20) {
            event.getDrops().add(helmetNetheriteEssence.createHelmetNetheriteEssence());
        }

        World world = breeze.getWorld();
        world.playEffect(breeze.getLocation(), Effect.EXTINGUISH, 0);
        world.playSound(breeze.getLocation(), Sound.ENTITY_BREEZE_DEATH, 1.0f, 0.7f);
    }

    public NamespacedKey getBuffBreezeKey() {
        return buffBreezeKey;
    }

    public boolean isBuffBreeze(Breeze breeze) {
        return breeze.getPersistentDataContainer().has(buffBreezeKey, PersistentDataType.BYTE);
    }
}