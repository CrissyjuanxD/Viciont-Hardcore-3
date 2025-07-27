package Dificultades.CustomMobs;

import Dificultades.Features.EnderMobsTP;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class EnderSilverfish extends EnderMobsTP implements Listener {
    private boolean eventsRegistered = false;
    private final Random random = new Random();
    private final NamespacedKey projectileKey;

    private final List<SilverfishEffect> possibleEffects = Arrays.asList(
            new SilverfishEffect("Fuerza", PotionEffectType.STRENGTH, 3),
            new SilverfishEffect("Invisibilidad", PotionEffectType.INVISIBILITY, 0),
            new SilverfishEffect("Glowing", PotionEffectType.GLOWING, 0),
            new SilverfishEffect("Resistencia", PotionEffectType.RESISTANCE, 1),
            new SilverfishEffect("Regeneración", PotionEffectType.REGENERATION, 2),
            new SilverfishEffect("Salto", PotionEffectType.JUMP_BOOST, 4),
            new SilverfishEffect("Slow Falling", PotionEffectType.SLOW_FALLING, 1)
    );

    public EnderSilverfish(JavaPlugin plugin) {
        super(plugin, "ender_silverfish");
        this.projectileKey = new NamespacedKey(plugin, "ender_silverfish_projectile");
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

    public Silverfish spawnEnderSilverfish(Location location) {
        Silverfish enderSilverfish = (Silverfish) location.getWorld().spawnEntity(location, EntityType.SILVERFISH);
        applyEnderSilverfishAttributes(enderSilverfish);
        return enderSilverfish;
    }

    private void applyEnderSilverfishAttributes(Silverfish silverfish) {
        silverfish.setCustomName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Ender Silverfish");
        silverfish.setCustomNameVisible(false);

        silverfish.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(30);
        silverfish.setHealth(30);
        silverfish.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);

        int numEffects = 3 + random.nextInt(3);
        Collections.shuffle(possibleEffects);

        for (int i = 0; i < numEffects && i < possibleEffects.size(); i++) {
            SilverfishEffect effect = possibleEffects.get(i);
            silverfish.addPotionEffect(new PotionEffect(
                    effect.type(),
                    PotionEffect.INFINITE_DURATION,
                    effect.amplifier(),
                    true,
                    true
            ));
        }

        startParticleTask(silverfish);

        startProjectileTask(silverfish);
    }

    private void startParticleTask(Silverfish silverfish) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (silverfish.isDead() || !silverfish.isValid()) {
                    this.cancel();
                    return;
                }

                silverfish.getWorld().spawnParticle(
                        Particle.PORTAL,
                        silverfish.getLocation().add(0, 0.2, 0),
                        3,
                        0.1, 0.1, 0.1, 0.02
                );
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void startProjectileTask(Silverfish silverfish) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (silverfish.isDead() || !silverfish.isValid()) {
                    this.cancel();
                    return;
                }

                LivingEntity target = silverfish.getTarget();
                if (target != null && target instanceof Player) {
                    if (random.nextDouble() < 0.25) {
                        launchTeleportProjectile(silverfish, (Player) target);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 100L);
    }

    private void launchTeleportProjectile(Silverfish silverfish, Player target) {
        Location spawnLoc = silverfish.getEyeLocation();

        BlockDisplay projectile = (BlockDisplay) silverfish.getWorld().spawnEntity(
                spawnLoc,
                EntityType.BLOCK_DISPLAY
        );

        projectile.setBlock(Material.END_ROD.createBlockData());
        projectile.setGlowing(true);
        projectile.setGlowColorOverride(Color.PURPLE);
        projectile.setInvulnerable(true);
        projectile.getPersistentDataContainer().set(projectileKey, PersistentDataType.BYTE, (byte) 1);

        Transformation transformation = projectile.getTransformation();
        transformation.getScale().set(0.3f, 0.3f, 0.3f);
        projectile.setTransformation(transformation);

        Vector direction = target.getEyeLocation().toVector()
                .subtract(spawnLoc.toVector())
                .normalize()
                .multiply(0.8);

        silverfish.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);

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
                        Particle.PORTAL,
                        projectile.getLocation(),
                        2,
                        0.05, 0.05, 0.05, 0.01
                );

                for (Entity nearby : projectile.getNearbyEntities(1.0, 1.0, 1.0)) {
                    if (nearby instanceof Player hitPlayer) {
                        handleProjectileHit(hitPlayer, projectile);
                        projectile.remove();
                        this.cancel();
                        return;
                    }
                }

                if (!projectile.getLocation().getBlock().isPassable()) {
                    projectile.remove();
                    this.cancel();
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void handleProjectileHit(Player player, BlockDisplay projectile) {
        if (player.isBlocking()) {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
            player.getWorld().spawnParticle(
                    Particle.ELECTRIC_SPARK,
                    player.getLocation(),
                    10,
                    0.5, 0.5, 0.5, 0.1
            );
            return;
        }

        Location originalLoc = player.getLocation();
        double angle = Math.random() * 2 * Math.PI;
        double distance = 10 + random.nextDouble() * 20;
        double x = originalLoc.getX() + distance * Math.cos(angle);
        double z = originalLoc.getZ() + distance * Math.sin(angle);

        Location newLoc = new Location(
                player.getWorld(),
                x,
                originalLoc.getY(),
                z
        );

        player.getWorld().playSound(originalLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.PORTAL, originalLoc, 30, 0.5, 0.5, 0.5, 0.1);

        player.teleport(newLoc);

        player.getWorld().playSound(newLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.PORTAL, newLoc, 30, 0.5, 0.5, 0.5, 0.1);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (isCustomMob(event.getEntity())) {
            Silverfish silverfish = (Silverfish) event.getEntity();

            event.getDrops().clear();

            silverfish.getWorld().playSound(silverfish.getLocation(), Sound.ENTITY_SILVERFISH_DEATH, 1.5f, 0.5f);
            silverfish.getWorld().spawnParticle(
                    Particle.PORTAL,
                    silverfish.getLocation(),
                    50,
                    0.5, 0.5, 0.5, 0.2
            );
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (isCustomMob(event.getEntity())) {
            Silverfish silverfish = (Silverfish) event.getEntity();
            silverfish.getWorld().playSound(silverfish.getLocation(), Sound.ENTITY_SILVERFISH_HURT, 1.5f, 0.5f);

            silverfish.addPotionEffect(new PotionEffect(
                    PotionEffectType.GLOWING,
                    20,
                    0,
                    false,
                    false
            ));
        }
    }

    public NamespacedKey getEnderSilverFishKey() {
        return mobKey;
    }

    @Override
    public boolean isCustomMob(Entity entity) {
        return entity instanceof Silverfish &&
                entity.getPersistentDataContainer().has(mobKey, PersistentDataType.BYTE);
    }

    private record SilverfishEffect(String name, PotionEffectType type, int amplifier) {}
}