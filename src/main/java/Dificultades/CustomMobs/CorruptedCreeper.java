package Dificultades.CustomMobs;

import Dificultades.Features.MobSoundManager;
import items.CorruptedMobItems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CorruptedCreeper implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey corruptedKey;
    private final Random random = new Random();
    private boolean eventsRegistered = false;
    private final Map<UUID, BukkitRunnable> creeperTasks = new HashMap<>();
    private final MobSoundManager soundManager;

    private static final int SLOWNESS_DURATION = 10 * 20;
    private static final int SLOWNESS_AMPLIFIER = 2;
    private static final int EFFECT_RADIUS = 8;
    private static final String TITLE_UNICODE = "\uEAA4";
    private static final int TITLE_DURATION = 4 * 20;
    private static final int TITLE_FADE_IN = 10;
    private static final int TITLE_FADE_OUT = 10;
    private static final int UPDATE_INTERVAL = 20;

    public CorruptedCreeper(JavaPlugin plugin) {
        this.plugin = plugin;
        this.corruptedKey = new NamespacedKey(plugin, "corrupted_creeper");
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
                    if (isCorruptedCreeper(entity)) {
                        entity.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public Creeper spawnCorruptedCreeper(Location location) {
        Creeper creeper = (Creeper) location.getWorld().spawnEntity(location, EntityType.CREEPER);
        applyCorruptedAttributes(creeper);
        return creeper;
    }

    private void applyCorruptedAttributes(Creeper creeper) {
        creeper.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Creeper");
        creeper.setCustomNameVisible(false);
        Objects.requireNonNull(creeper.getAttribute(Attribute.GENERIC_FOLLOW_RANGE)).setBaseValue(32);

        creeper.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 2)); // Velocidad II
        creeper.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0)); // Resistencia al fuego

        creeper.setExplosionRadius(3);
        creeper.setMaxFuseTicks(20);
        creeper.setPowered(false);

        creeper.getPersistentDataContainer().set(corruptedKey, PersistentDataType.BYTE, (byte) 1);
        startParticleEffect(creeper);
    }

    private void startParticleEffect(Creeper creeper) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!creeper.isValid() || creeper.isDead()) {
                    cancel();
                    return;
                }
                creeper.getWorld().spawnParticle(Particle.WITCH, creeper.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.1);
            }
        }.runTaskTimer(plugin, 0, 10);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!isCorruptedCreeper(event.getEntity()) || !(event.getTarget() instanceof Player)) {
            return;
        }

        Creeper creeper = (Creeper) event.getEntity();
        Player target = (Player) event.getTarget();

        cancelExistingTask(creeper);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!creeper.isValid() || creeper.isDead() || creeper.getTarget() == null) {
                    cancel();
                    creeperTasks.remove(creeper.getUniqueId());
                    return;
                }

                int torchCount = countTorchesInInventory(target);
                int explosionRadius = calculateExplosionRadius(torchCount);
                creeper.setExplosionRadius(explosionRadius);
            }
        };

        task.runTaskTimer(plugin, 0, UPDATE_INTERVAL);
        creeperTasks.put(creeper.getUniqueId(), task);
    }

    private void cancelExistingTask(Creeper creeper) {
        if (creeperTasks.containsKey(creeper.getUniqueId())) {
            creeperTasks.get(creeper.getUniqueId()).cancel();
            creeperTasks.remove(creeper.getUniqueId());
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!isCorruptedCreeper(event.getEntity())) {
            return;
        }

        Creeper creeper = (Creeper) event.getEntity();
        cancelExistingTask(creeper);

        for (Entity entity : creeper.getNearbyEntities(EFFECT_RADIUS, EFFECT_RADIUS, EFFECT_RADIUS)) {
            if (entity instanceof Player player &&
                    (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)) {
                applyExplosionEffects(player, creeper.getLocation());
            }
        }
    }

    private int countTorchesInInventory(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isTorch(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private boolean isTorch(ItemStack item) {
        return item != null && item.getType() == Material.TORCH;
    }

    private int calculateExplosionRadius(int torchCount) {
        if (torchCount >= 40) return 7; // 40+ antorchas = radio 7
        if (torchCount >= 30) return 6; // 30-39 antorchas = radio 6
        if (torchCount >= 20) return 5; // 20-29 antorchas = radio 5
        if (torchCount >= 10) return 4; // 10-19 antorchas = radio 4
        return 3;
    }

    private void applyExplosionEffects(Player player, Location explosionLoc) {
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, SLOWNESS_DURATION, SLOWNESS_AMPLIFIER));
        player.sendTitle(
                "",
                TITLE_UNICODE,
                TITLE_FADE_IN,
                TITLE_DURATION,
                TITLE_FADE_OUT
        );

        if (player.getLocation().distance(explosionLoc) <= EFFECT_RADIUS) {
            player.playSound(player.getLocation(), "custom.flashbang_effect", SoundCategory.VOICE, 2.0f, 0.8f);
        }
    }

    @EventHandler
    public void onCreeperPower(EntityTransformEvent event) {
        if (event.getTransformReason() == EntityTransformEvent.TransformReason.LIGHTNING &&
                isCorruptedCreeper(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCreeperPowered(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Creeper creeper &&
                isCorruptedCreeper(creeper) &&
                creeper.isPowered()) {
            creeper.setPowered(false);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && isCorruptedCreeper(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    //SONIDOS
    @EventHandler
    public void onCorruptedCreeperHurt(EntityDamageEvent event) {
        if (event.getEntity() instanceof Creeper creeper && isCorruptedCreeper(creeper)) {
            creeper.getWorld().playSound(creeper.getLocation(), Sound.ENTITY_CREEPER_HURT, SoundCategory.HOSTILE, 1.0f, 0.6f);
        }
    }

    @EventHandler
    public void onCorruptedCreeperDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Creeper creeper && isCorruptedCreeper(creeper)) {
            soundManager.removeCustomMob(creeper);
            creeper.getWorld().playSound(creeper.getLocation(), Sound.ENTITY_CREEPER_DEATH, SoundCategory.HOSTILE, 1.0f, 0.6f);

            if (Math.random() <= 0.40) {
                creeper.getWorld().dropItemNaturally(creeper.getLocation(), CorruptedMobItems.createCorruptedPowder());
            }
        }
    }

    public NamespacedKey getCorruptedCreeperKey() {
        return corruptedKey;
    }

    public boolean isCorruptedCreeper(Entity entity) {
        return entity instanceof Creeper && entity.getPersistentDataContainer().has(corruptedKey, PersistentDataType.BYTE);
    }
}