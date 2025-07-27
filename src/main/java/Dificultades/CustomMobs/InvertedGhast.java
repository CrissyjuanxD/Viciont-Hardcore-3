package Dificultades.CustomMobs;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class InvertedGhast implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey invertedGhastKey;
    private boolean eventsRegistered = false;
    private final Map<UUID, BossBar> playerBossBars = new HashMap<>();
    private final Map<UUID, BukkitRunnable> hallucinationTasks = new HashMap<>();

    public InvertedGhast(JavaPlugin plugin) {
        this.plugin = plugin;
        this.invertedGhastKey = new NamespacedKey(plugin, "inverted_ghast");
    }

    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
        }
    }

    public void revert() {
        if (eventsRegistered) {

            for (BukkitRunnable task : hallucinationTasks.values()) {
                task.cancel();
            }
            hallucinationTasks.clear();

            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Ghast ghast &&
                            ghast.getPersistentDataContainer().has(invertedGhastKey, PersistentDataType.BYTE)) {
                        ghast.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public Ghast spawnInvertedGhast(Location location) {
        Ghast invertedGhast = (Ghast) location.getWorld().spawnEntity(location, EntityType.GHAST);
        applyInvertedGhastAttributes(invertedGhast);
        return invertedGhast;
    }

    private void applyInvertedGhastAttributes(Ghast ghast) {
        ghast.setCustomName(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Inverted Ghast");
        ghast.setCustomNameVisible(false);
        ghast.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40);
        ghast.setHealth(40);
        ghast.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(96);
        ghast.getPersistentDataContainer().set(invertedGhastKey, PersistentDataType.BYTE, (byte) 1);

        startHallucinationEffects(ghast);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Ghast)) return;

        Ghast ghast = (Ghast) event.getEntity().getShooter();
        if (!ghast.getPersistentDataContainer().has(invertedGhastKey, PersistentDataType.BYTE)) return;

        Location impactLocation = event.getEntity().getLocation();
        createParticleCloud(impactLocation);
    }

    private void createParticleCloud(Location center) {
        AreaEffectCloud cloud = (AreaEffectCloud) center.getWorld().spawnEntity(center, EntityType.AREA_EFFECT_CLOUD);

        cloud.setRadius(8f);
        cloud.setDuration(30 * 20);
        cloud.setParticle(Particle.DRAGON_BREATH);
        cloud.setColor(Color.BLACK);
        cloud.setWaitTime(10);

        cloud.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 20, 9), true);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.WITHER, 20, 9), true);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.NAUSEA, 20, 9), true);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 9), true);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 9), true);

    }

    private void startHallucinationEffects(Ghast ghast) {
        stopHallucinationEffects(ghast);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (ghast.isDead() || !ghast.isValid()) {
                    this.cancel();
                    return;
                }

                for (Player player : ghast.getWorld().getPlayers()) {
                    if (player.getLocation().distance(ghast.getLocation()) <= 64 &&
                            (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)) {
                        createFakeTotemAnimation(player);
                        strikePlayerWithLightning(player);
                    }
                }
            }
        };

        task.runTaskTimer(plugin, 0L, 15 * 20L);
        hallucinationTasks.put(ghast.getUniqueId(), task);
    }

    private void createFakeTotemAnimation(Player player) {
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            return;
        }
        player.playEffect(EntityEffect.TOTEM_RESURRECT);
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
        player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0),
                30, 0.5, 0.5, 0.5, 0.5);
    }

    private void strikePlayerWithLightning(Player player) {
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            return;
        }
        LightningStrike lightning = player.getWorld().strikeLightningEffect(player.getLocation());

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 0));
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
        player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 15, 0.5, 1, 0.5, 0.1);
    }

    private void stopHallucinationEffects(Ghast ghast) {
        BukkitRunnable task = hallucinationTasks.remove(ghast.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Ghast ghast &&
                ghast.getPersistentDataContainer().has(invertedGhastKey, PersistentDataType.BYTE)) {

            event.getDrops().clear();
            ghast.getWorld().playSound(ghast.getLocation(), Sound.ENTITY_GHAST_DEATH, 5.0f, 1.5f);

            stopHallucinationEffects(ghast);
        }
    }

    @EventHandler
    public void onInvertedGhastHurt(EntityDamageEvent event) {
        if (event.getEntity() instanceof Ghast ghast &&
                ghast.getPersistentDataContainer().has(invertedGhastKey, PersistentDataType.BYTE)) {
            ghast.getWorld().playSound(ghast.getLocation(), Sound.ENTITY_GHAST_HURT, SoundCategory.HOSTILE, 5.0f, 1.5f);
        }
    }

    public NamespacedKey getInvertedGhastKey() {
        return invertedGhastKey;
    }

    public boolean isInvertedGhast(Ghast ghast) {
        return ghast.getPersistentDataContainer().has(invertedGhastKey, PersistentDataType.BYTE);
    }
}