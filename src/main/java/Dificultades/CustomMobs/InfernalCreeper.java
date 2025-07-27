package Dificultades.CustomMobs;

import items.ItemsTotems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class InfernalCreeper implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey infernalCreeperKey;
    private boolean eventsRegistered = false;

    public InfernalCreeper(JavaPlugin plugin) {
        this.plugin = plugin;
        this.infernalCreeperKey = new NamespacedKey(plugin, "infernal_creeper");
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
                    if (entity instanceof Creeper creeper && isInfernalCreeper(creeper)) {
                        creeper.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public Creeper spawnInfernalCreeper(Location location) {
        Creeper creeper = (Creeper) location.getWorld().spawnEntity(location, EntityType.CREEPER);
        applyInfernalCreeperAttributes(creeper);
        return creeper;
    }

    private void applyInfernalCreeperAttributes(Creeper creeper) {
        creeper.setCustomName(ChatColor.RED + "" + ChatColor.BOLD + "Infernal Creeper");
        creeper.setCustomNameVisible(false);
        creeper.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40);
        creeper.setHealth(40);
        creeper.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(64);
        creeper.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25);
        creeper.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1.0);

        creeper.setExplosionRadius(10);
        creeper.setMaxFuseTicks(30);

        creeper.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0, false, false));
        creeper.setPowered(true);

        creeper.getPersistentDataContainer().set(infernalCreeperKey, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.getEntity() instanceof Creeper creeper &&
                isInfernalCreeper(creeper) &&
                event.getTarget() != null) {

            startMagneticPull(creeper, (LivingEntity) event.getTarget());
        }
    }

    private void startMagneticPull(Creeper creeper, LivingEntity target) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (creeper.isDead() || !creeper.isValid() ||
                        target.isDead() || !target.isValid() ||
                        creeper.getTarget() == null || !creeper.getTarget().equals(target)) {
                    this.cancel();
                    return;
                }

                Vector direction = creeper.getLocation().toVector()
                        .subtract(target.getLocation().toVector())
                        .normalize()
                        .multiply(0.10);

                Vector currentVelocity = target.getVelocity();
                Vector newVelocity = direction;

                newVelocity.setY(Math.min(newVelocity.getY(), 0.1));

                target.setVelocity(currentVelocity.multiply(0.8).add(newVelocity.multiply(0.2)));

                if (Math.random() < 0.4) {
                    target.getWorld().spawnParticle(Particle.FLAME, target.getLocation(), 2, 0.2, 0.2, 0.2, 0.01);
                }

                if (Math.random() < 0.1) {
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.4f, 0.8f);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Creeper creeper &&
                isInfernalCreeper(creeper)) {

            if (event.getDamager() instanceof Projectile && creeper.getHealth() <= 10) {
                event.setCancelled(true);

                creeper.getWorld().spawnParticle(Particle.FIREWORK, creeper.getLocation(), 15);
                creeper.getWorld().playSound(creeper.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.8f);
            }

            creeper.getWorld().spawnParticle(Particle.LAVA, creeper.getLocation(), 5);
        }
    }

    @EventHandler
    public void onExplosionPrime(org.bukkit.event.entity.ExplosionPrimeEvent event) {
        if (event.getEntity() instanceof Creeper creeper &&
                isInfernalCreeper(creeper)) {

            event.setRadius(10.0f);

            creeper.getWorld().playSound(creeper.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.8f);

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks >= 20) {
                        this.cancel();
                        return;
                    }

                    creeper.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, creeper.getLocation(), 30, 1, 1, 1, 0.3);
                    creeper.getWorld().playSound(creeper.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.0f);

                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Creeper creeper && isInfernalCreeper(creeper)) {
            Location loc = creeper.getLocation();

            double baseDropChance = 0.50;
            double lootingBonus = 0;
            double doubleDropChance = 0;

            if (creeper.getKiller() != null) {
                ItemStack weapon = creeper.getKiller().getInventory().getItemInMainHand();
                if (weapon != null && weapon.getEnchantments().containsKey(Enchantment.LOOTING)) {
                    int lootingLevel = weapon.getEnchantmentLevel(Enchantment.LOOTING);

                    switch (lootingLevel) {
                        case 1:
                            lootingBonus = 0.10;
                            break;
                        case 2:
                            lootingBonus = 0.20;
                            break;
                        case 3:
                            lootingBonus = 0.25;
                            doubleDropChance = 0.30;
                            break;
                    }
                }
            }

            double totalDropChance = baseDropChance + lootingBonus;

            if (Math.random() <= totalDropChance) {
                ItemStack powder = ItemsTotems.createInfernalCreeperPowder();

                if (doubleDropChance > 0 && Math.random() <= doubleDropChance) {
                    powder.setAmount(2);
                }

                creeper.getWorld().dropItemNaturally(loc, powder);
            }

            creeper.getWorld().playSound(creeper.getLocation(), Sound.ENTITY_CREEPER_DEATH, 2.0f, 1.8f);
        }
    }

    public NamespacedKey getInfernalCreeperKey() {
        return infernalCreeperKey;
    }

    public boolean isInfernalCreeper(Creeper creeper) {
        return creeper.getPersistentDataContainer().has(infernalCreeperKey, PersistentDataType.BYTE);
    }
}