package Dificultades.CustomMobs;

import items.BlazeItems;
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
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class GuardianBlaze implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey guardianblazeKey;
    private final NamespacedKey lastAttackKey;
    private final Random random = new Random();
    private static boolean eventsRegistered = false;

    // --- OPTIMIZACIÓN: Manager Interno Estático ---
    private static final Set<UUID> activeBlazes = new HashSet<>();
    private static BukkitTask mainTask;
    // ---------------------------------------------

    public GuardianBlaze(JavaPlugin plugin) {
        this.plugin = plugin;
        this.guardianblazeKey = new NamespacedKey(plugin, "guardian_blaze");
        this.lastAttackKey = new NamespacedKey(plugin, "last_attack_time");
    }

    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
            // Escanear por si hubo un reload
            scanExistingBlazes();
            startGlobalTask();
        }
    }

    public void revert() {
        if (eventsRegistered) {
            // Limpiar tarea global
            if (mainTask != null && !mainTask.isCancelled()) {
                mainTask.cancel();
                mainTask = null;
            }

            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Blaze blaze && isGuardianBlaze(blaze)) {
                        blaze.remove();
                    }
                }
            }
            activeBlazes.clear();
            eventsRegistered = false;
        }
    }

    private void scanExistingBlazes() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(Blaze.class)) {
                if (isGuardianBlaze((Blaze) entity)) {
                    activeBlazes.add(entity.getUniqueId());
                }
            }
        }
    }

    public void spawnGuardianBlaze(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        Blaze blaze = (Blaze) world.spawnEntity(location, EntityType.BLAZE);

        blaze.setCustomName(ChatColor.GOLD + "" + ChatColor.BOLD + "Guardian Blaze");
        blaze.setCustomNameVisible(true);
        Objects.requireNonNull(blaze.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(40.0);
        blaze.setHealth(40.0);
        Objects.requireNonNull(blaze.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(12.0);
        Objects.requireNonNull(blaze.getAttribute(Attribute.GENERIC_SCALE)).setBaseValue(1.4);
        blaze.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
        blaze.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 1));

        blaze.getPersistentDataContainer().set(guardianblazeKey, PersistentDataType.BYTE, (byte) 1);
        blaze.getPersistentDataContainer().set(lastAttackKey, PersistentDataType.LONG, 0L);

        // Registrar en el sistema optimizado
        activeBlazes.add(blaze.getUniqueId());
        startGlobalTask();
    }

    // --- TAREA GLOBAL ÚNICA (Reemplaza startBehavior) ---
    private void startGlobalTask() {
        if (mainTask != null && !mainTask.isCancelled()) return;

        // Ejecutamos cada 10 ticks (0.5s) que es suficiente para IA de movimiento
        mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeBlazes.isEmpty()) return;

                Iterator<UUID> it = activeBlazes.iterator();
                while (it.hasNext()) {
                    UUID uuid = it.next();
                    Entity entity = Bukkit.getEntity(uuid);

                    // Limpieza automática
                    if (entity == null || !entity.isValid() || entity.isDead()) {
                        if (entity != null && !entity.isValid()) it.remove();
                        continue;
                    }

                    if (entity instanceof Blaze blaze) {
                        processBlazeAI(blaze);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void processBlazeAI(Blaze blaze) {
        // 1. Lógica de Movimiento y Melee
        Player target = getClosestPlayer(blaze, 20);
        if (target != null) {
            Vector direction = target.getLocation().toVector().subtract(blaze.getLocation().toVector()).normalize();
            blaze.setVelocity(direction.multiply(0.35));

            if (blaze.getLocation().distance(target.getLocation()) <= 2) {
                long currentTime = System.currentTimeMillis();
                // Usamos getOrDefault para evitar nulos
                Long lastAttackObj = blaze.getPersistentDataContainer().get(lastAttackKey, PersistentDataType.LONG);
                long lastAttack = (lastAttackObj != null) ? lastAttackObj : 0L;

                if (currentTime - lastAttack >= 2000) {
                    blaze.attack(target);
                    blaze.getPersistentDataContainer().set(lastAttackKey, PersistentDataType.LONG, currentTime);
                }
            }
        }

        // 2. Lógica de Ataque Especial (Cada 8 segundos aprox = 160 ticks)
        // Usamos ticksLived para sincronizar sin variables extra
        if (blaze.getTicksLived() % 160 == 0) {
            if (random.nextBoolean()) {
                launchHorizontalFireballAttack(blaze);
            } else {
                spawnCircleParticles(blaze);
            }
        }
    }

    // Las tareas de ataques especiales son temporales (duran poco), así que está bien dejarlas como Runnables individuales
    private void launchHorizontalFireballAttack(Blaze blaze) {
        List<Player> targets = getPlayersInLineOfSight(blaze, 20);
        if (targets.isEmpty()) return;

        new BukkitRunnable() {
            int rounds = 0;
            @Override
            public void run() {
                if (rounds >= 3 || !blaze.isValid()) {
                    cancel();
                    return;
                }

                blaze.getWorld().playSound(blaze.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 2.0f, 0.3f);
                blaze.getWorld().playSound(blaze.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 2.0f, 0.8f);

                for (Player target : targets) {
                    if (target.isValid() && hasLineOfSight(blaze, target)) {
                        launchHorizontalFireballs(blaze, target);
                    }
                }
                rounds++;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void launchHorizontalFireballs(Blaze blaze, Player target) {
        Location blazeLoc = blaze.getLocation().clone().add(0, 2.5, 0);
        Vector baseDirection = target.getLocation().toVector().subtract(blazeLoc.toVector()).normalize();
        Vector rightVector = baseDirection.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        for (int i = 0; i < 3; i++) {
            Vector direction = baseDirection.clone();
            Location spawnLoc = blazeLoc.clone();
            double offset = (i - 1) * 1.5;
            spawnLoc.add(rightVector.clone().multiply(offset));

            SmallFireball fireball = blaze.getWorld().spawn(spawnLoc, SmallFireball.class);
            fireball.setDirection(direction.normalize());
            fireball.setVelocity(direction.multiply(2.0));
            fireball.setShooter(blaze);
            fireball.setYield(3);
            fireball.setIsIncendiary(true);
            fireball.getPersistentDataContainer().set(new NamespacedKey(plugin, "custom_fireball"), PersistentDataType.BYTE, (byte) 1);
        }
    }

    private void spawnCircleParticles(Blaze blaze) {
        blaze.getWorld().playSound(blaze.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.7f);
        Set<Player> damagedPlayers = new HashSet<>();

        new BukkitRunnable() {
            double radius = 0;
            int cycles = 0;

            @Override
            public void run() {
                if (cycles >= 30 || !blaze.isValid()) {
                    damagedPlayers.clear();
                    cancel();
                    return;
                }

                radius += 0.5;
                double increment = Math.PI / 12;

                for (double angle = 0; angle < 2 * Math.PI; angle += increment) {
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    Location loc = blaze.getLocation().clone().add(x, 1, z);
                    blaze.getWorld().spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0.05);
                }

                if (cycles % 3 == 0) {
                    blaze.getWorld().spawnParticle(Particle.LAVA, blaze.getLocation().add(0, 1, 0), 5, radius, 0.5, radius, 0.1);
                }

                if (radius >= 2) {
                    for (Entity entity : blaze.getWorld().getNearbyEntities(blaze.getLocation(), radius, 2, radius)) {
                        if (entity instanceof Player player && !damagedPlayers.contains(player)) {
                            player.damage(4, blaze);
                            player.setFireTicks(Integer.MAX_VALUE);
                            damagedPlayers.add(player);
                        }
                    }
                }
                cycles++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // Optimización leve: streams pueden ser costosos si hay muchos jugadores,
    // pero getPlayers() suele ser una lista pequeña. Está aceptable.
    private Player getClosestPlayer(Blaze blaze, double radius) {
        return blaze.getWorld().getPlayers().stream()
                .filter(p -> p.getLocation().distance(blaze.getLocation()) <= radius)
                .filter(p -> p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(blaze.getLocation())))
                .orElse(null);
    }

    private List<Player> getPlayersInLineOfSight(Blaze blaze, double radius) {
        return blaze.getWorld().getPlayers().stream()
                .filter(p -> p.getLocation().distance(blaze.getLocation()) <= radius)
                .filter(p -> p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)
                .filter(p -> hasLineOfSight(blaze, p))
                .collect(Collectors.toList());
    }

    private boolean hasLineOfSight(Blaze blaze, Player player) {
        Location blazeEye = blaze.getEyeLocation();
        Location playerEye = player.getEyeLocation();
        return blaze.getWorld().rayTraceBlocks(blazeEye, playerEye.toVector().subtract(blazeEye.toVector()).normalize(),
                blazeEye.distance(playerEye)) == null;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Blaze blaze && isGuardianBlaze(blaze)) {
            if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                event.setCancelled(true);
            } else if (event.getEntity() instanceof Player player) {
                Vector knockback = player.getLocation().toVector().subtract(blaze.getLocation().toVector()).normalize();
                knockback.setY(0.3);
                player.setVelocity(knockback.multiply(0.8));
            }
        }
    }

    @EventHandler
    public void onEntityDamageEnvironment(EntityDamageEvent event) {
        if (event.getEntity() instanceof Blaze blaze && isGuardianBlaze(blaze)) {
            if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                    event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onFireballHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof SmallFireball fireball &&
                fireball.getPersistentDataContainer().has(new NamespacedKey(plugin, "custom_fireball"), PersistentDataType.BYTE) &&
                event.getEntity() instanceof Player player) {
            event.setDamage(10.0);
            player.setFireTicks(Integer.MAX_VALUE);
        }
    }

    @EventHandler
    public void onFireballExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof SmallFireball fireball &&
                fireball.getPersistentDataContainer().has(new NamespacedKey(plugin, "custom_fireball"), PersistentDataType.BYTE)) {
            Location loc = fireball.getLocation();
            fireball.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 2);
            fireball.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
        }
    }

    @EventHandler
    public void onGuardianBlazeHurt(EntityDamageEvent event) {
        if (event.getEntity() instanceof Blaze blaze && isGuardianBlaze(blaze)) {
            blaze.getWorld().playSound(blaze.getLocation(), Sound.ENTITY_BLAZE_HURT, SoundCategory.HOSTILE, 2.0f, 0.6f);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Blaze blaze && isGuardianBlaze(blaze)) {
            event.getDrops().clear();
            blaze.getWorld().playSound(blaze.getLocation(), Sound.ENTITY_BLAZE_DEATH, SoundCategory.HOSTILE, 2.0f, 0.6f);

            // IMPORTANTE: Limpiar de la lista activa al morir
            activeBlazes.remove(blaze.getUniqueId());

            if (random.nextInt(100) < 90) {
                event.getDrops().add(new ItemStack(Material.NETHERITE_SCRAP, getRandomNetheriteScrapAmount()));
            }
            if (random.nextBoolean()) {
                event.getDrops().add(BlazeItems.createBlazeRod());
            }
        }
    }

    private int getRandomNetheriteScrapAmount() {
        int randomValue = random.nextInt(100);
        if (randomValue < 50) return 1;
        if (randomValue < 80) return 2;
        return 3;
    }

    private boolean isGuardianBlaze(Blaze blaze) {
        return blaze.getPersistentDataContainer().has(guardianblazeKey, PersistentDataType.BYTE);
    }
}