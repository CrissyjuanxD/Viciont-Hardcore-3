package Dificultades.CustomMobs;

import items.ChestplateNetheriteEssence;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class PiglinGlobo implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey piglinGloboKey;
    private boolean eventsRegistered = false;
    private final ChestplateNetheriteEssence chestplateNetheriteEssence;

    public PiglinGlobo(JavaPlugin plugin) {
        this.plugin = plugin;
        this.chestplateNetheriteEssence = new ChestplateNetheriteEssence(plugin);
        this.piglinGloboKey = new NamespacedKey(plugin, "piglin_globo");
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
                    if (entity instanceof Ghast ghast &&
                            ghast.getPersistentDataContainer().has(piglinGloboKey, PersistentDataType.BYTE)) {
                        ghast.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public Ghast spawnPiglinGlobo(Location location) {
        Ghast piglinGlobo = (Ghast) location.getWorld().spawnEntity(location, EntityType.GHAST);
        applyPiglinGloboAttributes(piglinGlobo);
        return piglinGlobo;
    }

    private void applyPiglinGloboAttributes(Ghast ghast) {
        ghast.setCustomName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Piglin Globo");
        ghast.setCustomNameVisible(true);
        ghast.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(60);
        ghast.setHealth(60);
        ghast.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(96);
        ghast.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(1.4);

        ghast.getPersistentDataContainer().set(piglinGloboKey, PersistentDataType.BYTE, (byte) 1);
        startArrowAttackTask(ghast);
    }

    private void startArrowAttackTask(Ghast piglinGlobo) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (piglinGlobo.isDead() || !piglinGlobo.isValid()) {
                    this.cancel();
                    return;
                }

                LivingEntity target = piglinGlobo.getTarget();
                if (target != null && target instanceof Player) {
                    SpectralArrow arrow = piglinGlobo.launchProjectile(SpectralArrow.class);
                    arrow.setDamage(15);
                    arrow.setGlowing(true);
                    arrow.setShooter(piglinGlobo);

                    arrow.setVelocity(target.getEyeLocation().toVector()
                            .subtract(piglinGlobo.getLocation().toVector())
                            .normalize()
                            .multiply(3));
                }
            }
        }.runTaskTimer(plugin, 60L, 60L);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Ghast) {
            Ghast ghast = (Ghast) event.getEntity().getShooter();

            if (isPiglinGlobo(ghast)) {
                if (event.getEntity() instanceof Fireball) {
                    Fireball fireball = (Fireball) event.getEntity();
                    fireball.setYield(6.0f);
                }
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Ghast ghast &&
                isPiglinGlobo(ghast)) {

            event.getDrops().clear();

            if (Math.random() <= 0.18) {
                ghast.getWorld().dropItemNaturally(ghast.getLocation(), chestplateNetheriteEssence.createChestplateNetheriteEssence());
            }

            ghast.getWorld().playSound(ghast.getLocation(), Sound.ENTITY_GHAST_DEATH, 3.0f, 0.8f);
            ghast.getWorld().playSound(ghast.getLocation(), Sound.ENTITY_PIGLIN_BRUTE_DEATH, SoundCategory.HOSTILE, 5.0f, 0.6f);
        }
    }

    @EventHandler
    public void onPiglinGloboHurt(EntityDamageEvent event) {
        if (event.getEntity() instanceof Ghast ghast && isPiglinGlobo(ghast)) {
            ghast.getWorld().playSound(ghast.getLocation(), Sound.ENTITY_GHAST_HURT, SoundCategory.HOSTILE, 3.0f, 0.8f);
            ghast.getWorld().playSound(ghast.getLocation(), Sound.ENTITY_PIGLIN_BRUTE_HURT, SoundCategory.HOSTILE,5.0f, 0.6f);
        }
    }

    public NamespacedKey getPiglinGloboKey() {
        return piglinGloboKey;
    }

    public boolean isPiglinGlobo(Ghast ghast) {
        return ghast.getPersistentDataContainer().has(piglinGloboKey, PersistentDataType.BYTE);
    }
}