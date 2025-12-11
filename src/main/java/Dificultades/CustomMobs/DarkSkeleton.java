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
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class DarkSkeleton extends DarkMobSB implements Listener {

    // --- OPTIMIZACIÓN INTERNA ---
    // Una lista estática para trackear todos los DarkSkeletons vivos
    private static final Set<UUID> activeMobs = new HashSet<>();
    // Una sola tarea estática para manejar las partículas de TODOS
    private static BukkitTask particleTask;

    private static boolean eventsRegistered = false;

    public DarkSkeleton(JavaPlugin plugin) {
        super(plugin, "dark_skeleton");
    }

    @Override
    public void apply() {
        super.apply();
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
            startGlobalParticleTask(); // Iniciamos la tarea global al activar el evento
        }
    }

    public void revert() {
        if (eventsRegistered) {
            // Limpiar mobs del mundo
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (isCustomMob(entity)) {
                        entity.remove();
                    }
                }
            }
            // Detener la tarea global y limpiar la lista
            if (particleTask != null) {
                particleTask.cancel();
                particleTask = null;
            }
            activeMobs.clear();
            eventsRegistered = false;
        }
    }

    public Skeleton spawnDarkSkeleton(Location location) {
        Skeleton skeleton = (Skeleton) location.getWorld().spawnEntity(location, EntityType.SKELETON);
        applyDarkSkeletonAttributes(skeleton);

        // Registrar este mob en la lista optimizada
        activeMobs.add(skeleton.getUniqueId());
        // Asegurar que la tarea esté corriendo (por si acaso)
        startGlobalParticleTask();

        return skeleton;
    }

    private void applyDarkSkeletonAttributes(Skeleton skeleton) {
        skeleton.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Dark Skeleton");
        skeleton.setCustomNameVisible(false);

        skeleton.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(80);
        skeleton.setHealth(80);
        skeleton.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3);

        ItemStack darkBow = new ItemStack(Material.BOW);
        darkBow.addUnsafeEnchantment(Enchantment.POWER, 20);
        darkBow.addUnsafeEnchantment(Enchantment.INFINITY, 1);
        darkBow.addUnsafeEnchantment(Enchantment.UNBREAKING, 10);
        skeleton.getEquipment().setItemInMainHand(darkBow);
        skeleton.getEquipment().setItemInMainHandDropChance(0);

        skeleton.getEquipment().setItemInOffHand(new ItemStack(Material.ARROW, 1));
        skeleton.getEquipment().setItemInOffHandDropChance(0);

        skeleton.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                PotionEffect.INFINITE_DURATION,
                1,
                false, false
        ));

        skeleton.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);

        // NOTA: Ya no llamamos a startParticleTask(skeleton) aquí.
    }

    // --- TAREA GLOBAL OPTIMIZADA (1 sola task para 100 esqueletos) ---
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

                    // Limpieza automática si el mob no existe o murió
                    if (entity == null || !entity.isValid() || entity.isDead()) {
                        if (entity != null && !entity.isValid()) {
                            it.remove();
                        }
                        // Si es null (chunk descargado), podemos decidir mantenerlo o no.
                        // Para máxima limpieza, aquí asumimos que si no está cargado no mostramos partículas.
                        continue;
                    }

                    // Efectos visuales
                    entity.getWorld().spawnParticle(
                            Particle.LARGE_SMOKE,
                            entity.getLocation().add(0, 1, 0),
                            5, 0.3, 0.5, 0.3, 0.05
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
    public void onDarkSkeletonShoot(EntityShootBowEvent event) {
        if (isCustomMob(event.getEntity()) && event.getEntity() instanceof Skeleton skeleton) {
            if (event.getProjectile() instanceof Arrow arrow) {
                arrow.setColor(Color.BLACK);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (arrow.isDead() || !arrow.isValid()) {
                            this.cancel();
                            return;
                        }
                        arrow.getWorld().spawnParticle(
                                Particle.SQUID_INK,
                                arrow.getLocation(),
                                1, 0, 0, 0, 0
                        );
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
        }
    }

    @EventHandler
    public void onDarkSkeletonArrowHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Arrow arrow &&
                arrow.getShooter() instanceof Skeleton skeleton &&
                isCustomMob(skeleton)) {

            if (event.getEntity() instanceof LivingEntity target) {
                target.addPotionEffect(new PotionEffect(
                        PotionEffectType.DARKNESS,
                        600, 0, false, true
                ));
                target.getWorld().spawnParticle(
                        Particle.SOUL_FIRE_FLAME,
                        target.getLocation(),
                        15, 0.5, 0.5, 0.5, 0.1
                );
                target.getWorld().playSound(
                        target.getLocation(),
                        Sound.ENTITY_WARDEN_SONIC_CHARGE,
                        1.0f, 1.5f
                );
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (isCustomMob(event.getEntity())) {
            Skeleton skeleton = (Skeleton) event.getEntity();
            event.getDrops().clear();

            skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.5f, 1.0f);
            skeleton.getWorld().spawnParticle(
                    Particle.SOUL,
                    skeleton.getLocation(),
                    50, 0.5, 0.5, 0.5, 0.3
            );

            // Remover de la lista optimizada
            activeMobs.remove(skeleton.getUniqueId());
        }
    }

    @EventHandler
    public void onDarkSkeletonHurt(EntityDamageEvent event) {
        if (isCustomMob(event.getEntity())) {
            Skeleton skeleton = (Skeleton) event.getEntity();
            skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_WARDEN_HURT, 1.2f, 1.0f);

            skeleton.getWorld().spawnParticle(
                    Particle.DAMAGE_INDICATOR,
                    skeleton.getLocation(),
                    20, 0.5, 0.5, 0.5, 0.1
            );
        }
    }

    @Override
    public boolean isCustomMob(Entity entity) {
        return entity instanceof Skeleton &&
                entity.getPersistentDataContainer().has(mobKey, PersistentDataType.BYTE);
    }
}