package Dificultades.CustomMobs;

import items.ItemsTotems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class WhiteEnderman implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey whiteEndermanKey;
    private boolean eventsRegistered = false;

    public WhiteEnderman(JavaPlugin plugin) {
        this.plugin = plugin;
        this.whiteEndermanKey = new NamespacedKey(plugin, "white_enderman");
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
                    if (entity instanceof Enderman enderman && isWhiteEnderman(enderman)) {
                        enderman.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public Enderman spawnWhiteEnderman(Location location) {
        Enderman whiteEnderman = (Enderman) location.getWorld().spawnEntity(location, EntityType.ENDERMAN);
        applyWhiteEndermanAttributes(whiteEnderman);
        return whiteEnderman;
    }

    public void transformToWhiteEnderman(Enderman enderman) {
        applyWhiteEndermanAttributes(enderman);
    }

    private void applyWhiteEndermanAttributes(Enderman enderman) {
        enderman.setCustomName(ChatColor.WHITE + "" + ChatColor.BOLD + "White Enderman");
        enderman.setCustomNameVisible(false);
        enderman.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40);
        enderman.setHealth(40);
        enderman.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(64);
        enderman.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(10);
        enderman.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(1.2);

        enderman.getPersistentDataContainer().set(whiteEndermanKey, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.getEntity() instanceof Enderman enderman &&
                isWhiteEnderman(enderman) &&
                event.getTarget() != null) {

            startEnderpearlAttack(enderman, event.getTarget());
        }
    }

    private void startEnderpearlAttack(Enderman enderman, LivingEntity target) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (enderman.isDead() || !enderman.isValid() || target.isDead()) {
                    this.cancel();
                    return;
                }

                if (enderman.getLocation().distance(target.getLocation()) > 20 ||
                        !enderman.hasLineOfSight(target)) {
                    return;
                }

                if (Math.random() < 0.8) {
                    launchEnderpearl(enderman, target);
                }
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void launchEnderpearl(Enderman enderman, LivingEntity target) {
        Location eyeLocation = enderman.getEyeLocation();
        Vector direction = target.getEyeLocation().toVector()
                .subtract(eyeLocation.toVector())
                .normalize();

        direction.add(new Vector(
                (Math.random() - 0.5) * 0.1,
                (Math.random() - 0.5) * 0.1,
                (Math.random() - 0.5) * 0.1
        )).normalize();

        EnderPearl pearl = enderman.launchProjectile(EnderPearl.class, direction.multiply(1.8));
        pearl.setShooter(enderman);
        pearl.getPersistentDataContainer().set(whiteEndermanKey, PersistentDataType.BYTE, (byte) 1);

        enderman.getWorld().playSound(enderman.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Enderman enderman &&
                isWhiteEnderman(enderman)) {

            if (event.getCause() == EntityDamageEvent.DamageCause.DROWNING ||
                    event.getCause() == EntityDamageEvent.DamageCause.CONTACT) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Enderman enderman &&
                isWhiteEnderman(enderman)) {

            Location loc = enderman.getLocation();
            enderman.setAI(false);

            double baseDropChance = 0.50;
            double lootingBonus = 0;
            double doubleDropChance = 0;

            if (enderman.getKiller() != null) {
                ItemStack weapon = enderman.getKiller().getInventory().getItemInMainHand();
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
                ItemStack pearl = ItemsTotems.createWhiteEnderPearl();

                if (doubleDropChance > 0 && Math.random() <= doubleDropChance) {
                    pearl.setAmount(2);
                }

                enderman.getWorld().dropItemNaturally(loc, pearl);
            }

            loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_DEATH, 1.5f, 0.8f);

            new BukkitRunnable() {
                int count = 0;
                @Override
                public void run() {
                    if (count >= 6) {
                        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.7f);
                        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 2.0f);

                        loc.getWorld().createExplosion(loc, 7.0f, true, true);

                        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
                        this.cancel();
                        return;
                    }

                    if (enderman.isGlowing()) {
                        enderman.setGlowing(false);
                        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 2f);
                    } else {
                        enderman.setGlowing(true);
                        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 2f);
                        loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_SCREAM, 1.0f, 0.7f);
                    }

                    loc.getWorld().spawnParticle(Particle.FLASH, loc, 5);
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc, 10);

                    count++;
                }
            }.runTaskTimer(plugin, 0L, 10L);
        }
    }

    public NamespacedKey getWhiteEndermanKey() {
        return whiteEndermanKey;
    }

    public boolean isWhiteEnderman(Enderman enderman) {
        return enderman.getPersistentDataContainer().has(whiteEndermanKey, PersistentDataType.BYTE);
    }
}