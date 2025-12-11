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
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class DarkVex extends DarkMobSB implements Listener {

    // --- OPTIMIZACIÓN INTERNA ---
    private static final Set<UUID> activeMobs = new HashSet<>();
    private static BukkitTask particleTask;

    private static boolean eventsRegistered = false;

    public DarkVex(JavaPlugin plugin) {
        super(plugin, "dark_vex");
    }

    @Override
    public void apply() {
        super.apply();
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
            startGlobalParticleTask();
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
            if (particleTask != null) {
                particleTask.cancel();
                particleTask = null;
            }
            activeMobs.clear();
            eventsRegistered = false;
        }
    }

    public Vex spawnDarkVex(Location location) {
        Vex vex = (Vex) location.getWorld().spawnEntity(location, EntityType.VEX);
        applyDarkVexAttributes(vex);

        activeMobs.add(vex.getUniqueId());
        startGlobalParticleTask();

        return vex;
    }

    private void applyDarkVexAttributes(Vex vex) {
        vex.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Dark Vex");
        vex.setCustomNameVisible(false);

        vex.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(60);
        vex.setHealth(60);
        vex.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(8);
        vex.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.4);

        ItemStack netheriteSword = new ItemStack(Material.NETHERITE_SWORD);
        vex.getEquipment().setItemInMainHand(netheriteSword);

        ItemStack endCrystal = new ItemStack(Material.END_CRYSTAL);
        vex.getEquipment().setItemInOffHand(endCrystal);
        vex.getEquipment().setItemInOffHandDropChance(0);

        vex.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                PotionEffect.INFINITE_DURATION,
                1, false, false
        ));

        vex.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);
    }

    private void startGlobalParticleTask() {
        if (particleTask != null && !particleTask.isCancelled()) return;

        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeMobs.isEmpty()) return;

                Iterator<UUID> it = activeMobs.iterator();
                while (it.hasNext()) {
                    UUID uuid = it.next();
                    Entity entity = Bukkit.getEntity(uuid);

                    if (entity == null || !entity.isValid() || entity.isDead()) {
                        if (entity != null && !entity.isValid()) it.remove();
                        continue;
                    }

                    entity.getWorld().spawnParticle(
                            Particle.LARGE_SMOKE,
                            entity.getLocation(),
                            5, 0.3, 0.3, 0.3, 0.05
                    );
                    entity.getWorld().spawnParticle(
                            Particle.SOUL_FIRE_FLAME,
                            entity.getLocation(),
                            3, 0.3, 0.3, 0.3, 0.05
                    );
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    @EventHandler
    public void onDarkVexAttack(EntityDamageByEntityEvent event) {
        if (isCustomMob(event.getDamager()) && event.getDamager() instanceof Vex vex) {
            if (event.getEntity() instanceof Player player) {
                Location hitLoc = player.getLocation();
                vex.getWorld().spawnParticle(Particle.EXPLOSION, hitLoc, 10, 0.5, 0.5, 0.5, 0.1);
                vex.getWorld().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

                player.damage(4);

                Vector direction = player.getLocation().toVector()
                        .subtract(vex.getLocation().toVector())
                        .normalize()
                        .multiply(0.5)
                        .setY(0.2);
                player.setVelocity(direction);
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (isCustomMob(event.getEntity())) {
            Vex vex = (Vex) event.getEntity();
            event.getDrops().clear();

            vex.getWorld().playSound(vex.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.5f, 1.2f);
            vex.getWorld().spawnParticle(Particle.SOUL, vex.getLocation(), 30, 0.5, 0.5, 0.5, 0.3);

            activeMobs.remove(vex.getUniqueId());
        }
    }

    @EventHandler
    public void onDarkVexHurt(EntityDamageEvent event) {
        if (isCustomMob(event.getEntity())) {
            Vex vex = (Vex) event.getEntity();
            vex.getWorld().playSound(vex.getLocation(), Sound.ENTITY_WARDEN_HURT, 1.2f, 1.1f);
            vex.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, vex.getLocation(), 15, 0.5, 0.5, 0.5, 0.1);
        }
    }

    @Override
    public boolean isCustomMob(Entity entity) {
        return entity instanceof Vex &&
                entity.getPersistentDataContainer().has(mobKey, PersistentDataType.BYTE);
    }
}