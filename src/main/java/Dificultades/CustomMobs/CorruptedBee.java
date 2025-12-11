package Dificultades.CustomMobs;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class CorruptedBee implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey corruptedBeeKey;

    private static final Set<UUID> activeBees = new HashSet<>();
    private static boolean eventsRegistered = false;
    private static BukkitTask mainTask;

    public CorruptedBee(JavaPlugin plugin) {
        this.plugin = plugin;
        this.corruptedBeeKey = new NamespacedKey(plugin, "corrupted_bee");
    }

    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;

            scanExistingBees();
            startCentralTask();
        }
    }

    public void revert() {
        if (eventsRegistered) {
            if (mainTask != null && !mainTask.isCancelled()) {
                mainTask.cancel();
                mainTask = null;
            }

            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Bee bee && isCorruptedBee(bee)) {
                        bee.remove();
                    }
                }
            }
            activeBees.clear();
            eventsRegistered = false;
        }
    }

    private void scanExistingBees() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(Bee.class)) {
                if (isCorruptedBee(entity)) {
                    activeBees.add(entity.getUniqueId());
                }
            }
        }
    }

    private void startCentralTask() {
        if (mainTask != null && !mainTask.isCancelled()) return;

        // Ejecutar cada 20 ticks (1 segundo) es suficiente para target y resetear estado
        mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeBees.isEmpty()) return;

                Iterator<UUID> it = activeBees.iterator();
                while (it.hasNext()) {
                    UUID id = it.next();
                    Entity entity = Bukkit.getEntity(id);

                    if (entity == null || !entity.isValid() || entity.isDead()) {
                        if (entity != null && !entity.isValid()) it.remove();
                        continue;
                    }

                    if (entity instanceof Bee bee) {
                        manageBeeAI(bee);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void manageBeeAI(Bee bee) {
        bee.setHasStung(false);
        bee.setAnger(999999);

        LivingEntity currentTarget = bee.getTarget();

        // Si no tiene target o el target murió/se alejó mucho, buscar uno nuevo
        if (currentTarget == null || !currentTarget.isValid() || currentTarget.isDead() ||
                bee.getLocation().distanceSquared(currentTarget.getLocation()) > 400) {

            Player nearest = findNearestPlayer(bee);
            if (nearest != null) {
                bee.setTarget(nearest);
            }
        }
    }

    private Player findNearestPlayer(Bee bee) {
        Player nearest = null;
        double minDistanceSq = 400;

        for (Player p : bee.getWorld().getPlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                double distSq = p.getLocation().distanceSquared(bee.getLocation());
                if (distSq < minDistanceSq) {
                    minDistanceSq = distSq;
                    nearest = p;
                }
            }
        }
        return nearest;
    }


    public Bee spawnCorruptedBee(Location location) {
        Bee bee = (Bee) location.getWorld().spawnEntity(location, EntityType.BEE);
        applyAttributes(bee);

        activeBees.add(bee.getUniqueId());
        startCentralTask();

        return bee;
    }

    public void transformToCorruptedBee(Bee bee) {
        applyAttributes(bee);
        activeBees.add(bee.getUniqueId());
        startCentralTask();
    }

    private void applyAttributes(Bee bee) {
        bee.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Bee");
        bee.setCustomNameVisible(false);

        // Atributos base
        Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(30.0);
        bee.setHealth(30.0);

        Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(8.0);

        bee.setAnger(999999);
        bee.setHasStung(false);

        bee.getPersistentDataContainer().set(corruptedBeeKey, PersistentDataType.BYTE, (byte) 1);
    }


    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Bee bee)) return;
        if (!isCorruptedBee(bee)) return;

        bee.setHasStung(false);

        if (event.getEntity() instanceof LivingEntity target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 2));

            target.getWorld().spawnParticle(Particle.SQUID_INK, target.getEyeLocation(), 5, 0.2, 0.2, 0.2, 0.1);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BEE_STING, 1.0f, 0.8f);
        }
    }

    public boolean isCorruptedBee(Entity entity) {
        return entity instanceof Bee && entity.getPersistentDataContainer().has(corruptedBeeKey, PersistentDataType.BYTE);
    }
}