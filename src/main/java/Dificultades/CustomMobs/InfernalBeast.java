package Dificultades.CustomMobs;

import items.LegginsNetheriteEssence;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.Random;

public class InfernalBeast implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey infernalBeastKey;
    private final NamespacedKey hellspawnHoglinKey;
    private static boolean eventsRegistered = false;
    private final LegginsNetheriteEssence legginsNetheriteEssence;
    private final Random random = new Random();

    public InfernalBeast(JavaPlugin plugin) {
        this.plugin = plugin;
        this.legginsNetheriteEssence = new LegginsNetheriteEssence(plugin);
        this.infernalBeastKey = new NamespacedKey(plugin, "infernal_beast");
        this.hellspawnHoglinKey = new NamespacedKey(plugin, "hellspawn_hoglin");
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
                    if (entity instanceof Hoglin hoglin &&
                            (isInfernalBeast(hoglin) || isHellspawnHoglin(hoglin))) {
                        hoglin.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public Hoglin spawnInfernalBeast(Location location) {
        Hoglin infernalBeast = (Hoglin) location.getWorld().spawnEntity(location, EntityType.HOGLIN);
        applyInfernalBeastAttributes(infernalBeast);
        return infernalBeast;
    }

    public void transformHoglin(Hoglin hoglin) {
        applyInfernalBeastAttributes(hoglin);
    }

    private void applyInfernalBeastAttributes(Hoglin hoglin) {
        hoglin.setCustomName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Infernal Beast");
        hoglin.setCustomNameVisible(true);
        hoglin.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(100);
        hoglin.setHealth(100);
        hoglin.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(15);
        hoglin.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(64);
        hoglin.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.6);
        hoglin.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(1.4);

        hoglin.setImmuneToZombification(true);

        hoglin.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));

        hoglin.getPersistentDataContainer().set(infernalBeastKey, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (event.getEntity() instanceof Hoglin hoglin &&
                isInfernalBeast(hoglin) &&
                event.getTarget() != null) {

            // 30% chance to launch projectile when targeting a player
            if (event.getTarget() instanceof Player && random.nextDouble() < 0.3) {
                launchHellfireProjectile(hoglin, event.getTarget());
            }
        }
    }

    private void launchHellfireProjectile(Hoglin hoglin, LivingEntity target) {
        BlockDisplay projectile = (BlockDisplay) hoglin.getWorld().spawnEntity(
                hoglin.getEyeLocation(),
                EntityType.BLOCK_DISPLAY
        );

        projectile.setBlock(Material.REDSTONE_BLOCK.createBlockData());
        projectile.setGlowing(true);
        projectile.setGlowColorOverride(Color.RED);

        Transformation transformation = projectile.getTransformation();
        transformation.getScale().set(0.4f, 0.4f, 0.4f);
        projectile.setTransformation(transformation);

        Vector direction = target.getLocation().toVector()
                .subtract(hoglin.getLocation().toVector())
                .normalize()
                .multiply(0.8);

        hoglin.getWorld().playSound(hoglin.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.7f);

        new BukkitRunnable() {
            int ticks = 0;
            Location currentLoc = projectile.getLocation().clone();

            @Override
            public void run() {
                if (projectile.isDead() || ticks >= 100) {
                    projectile.remove();
                    this.cancel();
                    return;
                }

                currentLoc.add(direction);
                projectile.teleport(currentLoc);

                projectile.getWorld().spawnParticle(
                        Particle.FLAME,
                        projectile.getLocation(),
                        3,
                        0.1, 0.1, 0.1, 0.01
                );
                projectile.getWorld().spawnParticle(
                        Particle.LARGE_SMOKE,
                        projectile.getLocation(),
                        1,
                        0.1, 0.1, 0.1, 0.01
                );

                for (Entity nearby : projectile.getNearbyEntities(1.5, 1.5, 1.5)) {
                    if (nearby instanceof Player player) {
                        if (player.isBlocking()) {
                            player.getWorld().playSound(player.getLocation(),
                                    Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.8f);
                            player.getWorld().spawnParticle(
                                    Particle.WAX_OFF,
                                    player.getLocation(),
                                    20,
                                    0.5, 0.5, 0.5,
                                    Material.REDSTONE_BLOCK.createBlockData()
                            );
                        } else {
                            spawnHellspawnHoglins(player.getLocation());
                            player.getWorld().playSound(player.getLocation(),
                                    Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
                        }
                        projectile.remove();
                        this.cancel();
                        return;
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnHellspawnHoglins(Location location) {
        int amount = 2 + random.nextInt(2);

        for (int i = 0; i < amount; i++) {
            Location spawnLoc = location.clone().add(
                    random.nextDouble() * 2 - 1,
                    0,
                    random.nextDouble() * 2 - 1
            );

            Hoglin miniHoglin = (Hoglin) location.getWorld().spawnEntity(spawnLoc, EntityType.HOGLIN);
            applyHellspawnAttributes(miniHoglin);
        }

        location.getWorld().spawnParticle(Particle.LAVA, location, 20);
        location.getWorld().spawnParticle(Particle.FLAME, location, 30);
        location.getWorld().playSound(location, Sound.ENTITY_HOGLIN_ANGRY, 1.5f, 0.8f);
    }

    private void applyHellspawnAttributes(Hoglin hoglin) {
        hoglin.setCustomName(ChatColor.RED + "Mini Beast");
        hoglin.setCustomNameVisible(true);
        hoglin.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(15);
        hoglin.setHealth(15);
        hoglin.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(10);
        hoglin.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(0.5);
        hoglin.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.45);

        hoglin.setImmuneToZombification(true);
        hoglin.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));

        hoglin.getPersistentDataContainer().set(hellspawnHoglinKey, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Hoglin hoglin && isInfernalBeast(hoglin)) {
            if (event.getEntity() instanceof Player) {
                Vector direction = event.getEntity().getLocation().toVector()
                        .subtract(hoglin.getLocation().toVector())
                        .normalize()
                        .multiply(2.5)
                        .setY(0.5);
                event.getEntity().setVelocity(direction);
            }
        }

        if (event.getDamager() instanceof Hoglin hoglin && isHellspawnHoglin(hoglin)) {
            if (event.getEntity() instanceof Player) {
                // Small knockback
                Vector direction = event.getEntity().getLocation().toVector()
                        .subtract(hoglin.getLocation().toVector())
                        .normalize()
                        .multiply(1.2)
                        .setY(0.3);
                event.getEntity().setVelocity(direction);
            }
        }
    }

    @EventHandler
    public void onInfernalBeastHurt(EntityDamageEvent event) {
        if (event.getEntity() instanceof Hoglin hoglin && isInfernalBeast(hoglin)) {
            hoglin.getWorld().playSound(hoglin.getLocation(),
                    Sound.ENTITY_HOGLIN_HURT, SoundCategory.HOSTILE, 1.0f, 0.6f);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Hoglin hoglin) {
            if (isInfernalBeast(hoglin)) {
                event.getDrops().clear();
                event.setDroppedExp(400);

                if (Math.random() <= 0.15) {
                    hoglin.getWorld().dropItemNaturally(
                            hoglin.getLocation(),
                            legginsNetheriteEssence.createLegginsNetheriteEssence()
                    );
                }

                hoglin.getWorld().playSound(hoglin.getLocation(),
                        Sound.ENTITY_HOGLIN_DEATH, 1.0f, 0.6f);
                hoglin.getWorld().spawnParticle(Particle.LAVA, hoglin.getLocation(), 30);
            } else if (isHellspawnHoglin(hoglin)) {
                event.getDrops().clear();
                event.setDroppedExp(15);
                hoglin.getWorld().spawnParticle(Particle.FLAME, hoglin.getLocation(), 10);
            }
        }
    }

    public NamespacedKey getInfernalBeastKey() {
        return infernalBeastKey;
    }

    public boolean isInfernalBeast(Hoglin hoglin) {
        return hoglin.getPersistentDataContainer().has(infernalBeastKey, PersistentDataType.BYTE);
    }

    public boolean isHellspawnHoglin(Hoglin hoglin) {
        return hoglin.getPersistentDataContainer().has(hellspawnHoglinKey, PersistentDataType.BYTE);
    }
}