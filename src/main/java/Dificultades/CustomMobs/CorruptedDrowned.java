package Dificultades.CustomMobs;

import items.TridenteEspectral;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class CorruptedDrowned implements Listener {
    private final JavaPlugin plugin;
    private final TridenteEspectral tridenteEspectral;
    private final NamespacedKey corruptedDrownedKey;
    private final NamespacedKey spectralTridentKey;
    private static boolean eventsRegistered = false;
    private final Random random = new Random();
    private final Set<Drowned> activeCorruptedDrowneds = new HashSet<>();

    // Constantes de optimización
    private static final int CHECK_INTERVAL = 20;
    private static final double MELEE_RANGE_SQUARED = 3.0 * 3.0;
    private static final double TRIDENT_RANGE_SQUARED = 20.0 * 20.0;
    private static final double RIP_TIDE_RANGE_SQUARED = 45.0 * 45.0;

    public CorruptedDrowned(JavaPlugin plugin) {
        this.plugin = plugin;
        this.tridenteEspectral = new TridenteEspectral(plugin);
        this.corruptedDrownedKey = new NamespacedKey(plugin, "corrupted_drowned");
        this.spectralTridentKey = new NamespacedKey(plugin, "spectral_trident");
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
                    if (entity instanceof Drowned drowned && isCorruptedDrowned(drowned)) {
                        drowned.remove();
                    }
                }
            }
            eventsRegistered = false;
            activeCorruptedDrowneds.clear();
        }
    }

    public Drowned spawnCorruptedDrowned(Location location) {
        Drowned corruptedDrowned = (Drowned) location.getWorld().spawnEntity(location, EntityType.DROWNED);
        applyCorruptedDrownedAttributes(corruptedDrowned);
        return corruptedDrowned;
    }

    public void transformToCorruptedDrowned(Drowned drowned) {
        applyCorruptedDrownedAttributes(drowned);
        activeCorruptedDrowneds.add(drowned);
        monitorCorruptedDrowned(drowned);
    }

    private void applyCorruptedDrownedAttributes(Drowned drowned) {
        drowned.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Drowned");
        drowned.setCustomNameVisible(true);

        // Configurar atributos
        drowned.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(8.0);
        drowned.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25);
        drowned.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(64.0);
        drowned.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1.0);

        // Inmunidad al daño de caída
        drowned.getAttribute(Attribute.GENERIC_FALL_DAMAGE_MULTIPLIER).setBaseValue(0.0);

        // Añadir efectos potenciados
        drowned.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1, true, true));
        drowned.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, true, true));

        // Darle el Tridente Espectral
        EntityEquipment equipment = drowned.getEquipment();
        if (equipment != null) {
            ItemStack spectralTrident = tridenteEspectral.createSpectralTrident();
            equipment.setItemInMainHand(spectralTrident);
            equipment.setItemInMainHandDropChance(0.0f);
            equipment.setItemInOffHand(new ItemStack(Material.AIR));
        }

        drowned.getPersistentDataContainer().set(corruptedDrownedKey, PersistentDataType.BYTE, (byte) 1);

        // Efectos de spawn (reducidos)
        drowned.getWorld().spawnParticle(Particle.SQUID_INK, drowned.getLocation(), 10, 0.3, 0.3, 0.3, 0.05);
        drowned.getWorld().playSound(drowned.getLocation(), Sound.ENTITY_DROWNED_AMBIENT_WATER, 0.8f, 0.8f);

        monitorCorruptedDrowned(drowned);
        activeCorruptedDrowneds.add(drowned);
    }

    private void monitorCorruptedDrowned(Drowned drowned) {
        new BukkitRunnable() {
            private int tickCounter = 0;
            private int attackCooldown = 0;
            private int tridentThrowCooldown = 0;
            private int riptideCooldown = 0;
            private boolean isInMeleeMode = false;
            private EntityEquipment cachedEquipment = drowned.getEquipment();

            @Override
            public void run() {
                if (drowned.isDead() || !drowned.isValid()) {
                    cancel();
                    activeCorruptedDrowneds.remove(drowned);
                    return;
                }

                // Ejecutar lógica solo cada CHECK_INTERVAL ticks
                if (tickCounter % CHECK_INTERVAL == 0) {
                    executeDrownedLogic(drowned);
                }

                // Decrementar cooldowns cada tick
                if (attackCooldown > 0) attackCooldown--;
                if (tridentThrowCooldown > 0) tridentThrowCooldown--;
                if (riptideCooldown > 0) riptideCooldown--;

                tickCounter++;
            }

            private void executeDrownedLogic(Drowned drowned) {
                // Verificar si está en el agua para aumentar velocidad (solo cada 2 segundos)
                if (drowned.isInWater() && tickCounter % 40 == 0) {
                    drowned.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 120, 2, true, false));
                }

                // Comportamiento especial cuando tiene target
                if (drowned.getTarget() instanceof Player target) {
                    Location drownedLoc = drowned.getLocation();
                    Location targetLoc = target.getLocation();
                    double distanceSquared = drownedLoc.distanceSquared(targetLoc);

                    // Cambiar entre modo melee y a distancia
                    if (distanceSquared <= MELEE_RANGE_SQUARED && !isInMeleeMode) {
                        // Cambiar a modo melee
                        isInMeleeMode = true;
                        if (cachedEquipment != null) {
                            cachedEquipment.setItemInMainHand(new ItemStack(Material.AIR));
                        }
                    } else if (distanceSquared > MELEE_RANGE_SQUARED && isInMeleeMode) {
                        // Cambiar a modo a distancia
                        isInMeleeMode = false;
                        if (cachedEquipment != null) {
                            cachedEquipment.setItemInMainHand(tridenteEspectral.createSpectralTrident());
                        }
                    }

                    // Ataque melee cuando está cerca
                    if (isInMeleeMode && attackCooldown <= 0 && distanceSquared <= MELEE_RANGE_SQUARED) {
                        drowned.attack(target);
                        attackCooldown = 20; // 1 segundo de cooldown
                    }
                    // Lanzamiento de tridente cuando está lejos
                    else if (!isInMeleeMode && tridentThrowCooldown <= 0 &&
                            distanceSquared > MELEE_RANGE_SQUARED && distanceSquared < TRIDENT_RANGE_SQUARED) {
                        performTridentThrow(drowned, target);
                        tridentThrowCooldown = 40; // 2 segundos de cooldown
                    }

                    // Carga Riptide
                    if (riptideCooldown <= 0 && (drowned.isInWater() || isRaining(drowned.getWorld())) &&
                            distanceSquared > 64.0 && distanceSquared < RIP_TIDE_RANGE_SQUARED && random.nextDouble() < 0.3) {
                        performRiptideCharge(drowned, target);
                        riptideCooldown = 60; // 3 segundos de cooldown
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean isRaining(World world) {
        return world.hasStorm() || world.isThundering();
    }

    private void performTridentThrow(Drowned drowned, LivingEntity target) {
        Location drownedLoc = drowned.getLocation();
        Location targetLoc = target.getLocation();

        // Calcular dirección con predicción de movimiento
        Vector targetVelocity = target.getVelocity();
        Location predictedLocation = targetLoc.clone().add(targetVelocity.multiply(0.5));
        Vector direction = predictedLocation.toVector().subtract(drownedLoc.toVector()).normalize();

        // Lanzar el tridente
        Trident trident = drowned.launchProjectile(Trident.class, direction);

        // Configurar el tridente lanzado
        trident.setPierceLevel(0);
        trident.setCritical(true);
        trident.getPersistentDataContainer().set(spectralTridentKey, PersistentDataType.BYTE, (byte) 1);

        // Efectos visuales y sonoros (optimizados)
        drowned.getWorld().playSound(drownedLoc, Sound.ITEM_TRIDENT_THROW, 0.8f, 0.8f);
        drowned.getWorld().spawnParticle(Particle.NAUTILUS, drownedLoc, 5, 0.3, 0.3, 0.3, 0.05);
    }

    private void performRiptideCharge(Drowned drowned, Player target) {
        Location drownedLoc = drowned.getLocation();
        Location targetLoc = target.getLocation();
        double distanceSquared = drownedLoc.distanceSquared(targetLoc);

        if (distanceSquared < 9.0) return; // No usar Riptide si ya está muy cerca

        // Calcular dirección con mejor predicción
        Vector targetVelocity = target.getVelocity();
        double predictionFactor = Math.sqrt(distanceSquared) * 0.1;
        Location predictedLocation = targetLoc.clone().add(targetVelocity.multiply(predictionFactor));
        Vector direction = predictedLocation.toVector().subtract(drownedLoc.toVector()).normalize();

        // Aplicar impulso
        double speedMultiplier = 2.0 + (Math.sqrt(distanceSquared) * 0.08);
        speedMultiplier = Math.min(speedMultiplier, 3.5);
        drowned.setVelocity(direction.multiply(speedMultiplier));

        // Efectos visuales y sonoros (optimizados)
        drowned.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, drownedLoc, 20, 0.5, 0.5, 0.5, 0.2);
        drowned.getWorld().spawnParticle(Particle.NAUTILUS, drownedLoc, 10, 0.5, 0.5, 0.5, 0.1);
        drowned.getWorld().playSound(drownedLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, 2.0f, 1.0f);

        // Asegurar ataque al llegar cerca del objetivo
        new BukkitRunnable() {
            @Override
            public void run() {
                if (drowned.isDead() || !drowned.isValid() || target.isDead()) return;
                if (drowned.getLocation().distanceSquared(target.getLocation()) < 16.0) {
                    drowned.attack(target);
                }
            }
        }.runTaskLater(plugin, 15L);
    }

    @EventHandler
    public void onTridentHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Trident trident &&
                trident.getPersistentDataContainer().has(spectralTridentKey, PersistentDataType.BYTE)) {

            if (trident.getShooter() instanceof Drowned drowned && isCorruptedDrowned(drowned)) {
                if (event.getHitEntity() instanceof LivingEntity target) {
                    target.damage(8.0, drowned);
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 8, 0.3, 0.3, 0.3);
                    target.getWorld().playSound(target.getLocation(), Sound.ITEM_TRIDENT_HIT, 0.8f, 0.7f);
                }

                // Recuperar el tridente
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (drowned.isDead() || !drowned.isValid()) return;
                        EntityEquipment equipment = drowned.getEquipment();
                        if (equipment != null && equipment.getItemInMainHand().getType() == Material.AIR) {
                            equipment.setItemInMainHand(tridenteEspectral.createSpectralTrident());
                        }
                    }
                }.runTaskLater(plugin, 20L);
            }
        }
    }

    @EventHandler
    public void onCorruptedDrownedDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Drowned drowned && isCorruptedDrowned(drowned)) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                event.setCancelled(true);
            }

            if (event instanceof EntityDamageByEntityEvent damageByEntityEvent) {
                Entity damager = damageByEntityEvent.getDamager();
                if (damager instanceof Trident trident &&
                        trident.getPersistentDataContainer().has(spectralTridentKey, PersistentDataType.BYTE)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onCorruptedDrownedTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Drowned drowned && isCorruptedDrowned(drowned)) {
            if (event.getTarget() instanceof Player target && target.isInWater()) {
                drowned.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2, true, false));
            }
        }
    }

    @EventHandler
    public void onCorruptedDrownedDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Drowned drowned && isCorruptedDrowned(drowned)) {
            drowned.getWorld().playSound(drowned.getLocation(), Sound.ENTITY_DROWNED_DEATH_WATER, 0.8f, 0.7f);
            drowned.getWorld().spawnParticle(Particle.LARGE_SMOKE, drowned.getLocation(), 10, 0.3, 0.3, 0.3, 0.05);

            if (random.nextDouble() < 0.05) {
                event.getDrops().add(tridenteEspectral.createSpectralTrident());
            }

            activeCorruptedDrowneds.remove(drowned);
        }
    }

    public NamespacedKey getCorruptedDrownedKey() {
        return corruptedDrownedKey;
    }

    public boolean isCorruptedDrowned(Drowned drowned) {
        return drowned.getPersistentDataContainer().has(corruptedDrownedKey, PersistentDataType.BYTE);
    }
}