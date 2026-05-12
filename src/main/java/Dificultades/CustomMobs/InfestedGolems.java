package Dificultades.CustomMobs;

import Dificultades.Features.MobSoundManager;
import Dificultades.Features.SpawnerInfestedGolems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class InfestedGolems implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey infestedKey;
    private static boolean eventsRegistered = false;

    private static final Set<UUID> activeGolems = new HashSet<>();
    private static final Map<UUID, Long> attackCooldowns = new HashMap<>();
    private static final Map<UUID, Location> linkedSpawners = new HashMap<>();
    private static BukkitTask mainTask;

    // Configuración
    private static final double DETECTION_RANGE = 32.0;
    private static final double LOSE_TARGET_RANGE = 25.0;
    private static final long ATTACK_COOLDOWN_MS = 2000;

    // AUMENTADO: Ahora tiene 50 bloques de libertad antes de ser teletransportado
    private static final double SPAWNER_TETHER_RANGE = 50.0;

    public InfestedGolems(JavaPlugin plugin) {
        this.plugin = plugin;
        this.infestedKey = new NamespacedKey(plugin, "infested_golem");
    }

    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
            startCentralTask();
        }
    }

    public void revert() {
        if (eventsRegistered) {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (isInfestedGolem(entity)) {
                        entity.remove();
                    }
                }
            }
            if (mainTask != null && !mainTask.isCancelled()) {
                mainTask.cancel();
            }
            activeGolems.clear();
            attackCooldowns.clear();
            linkedSpawners.clear();
            eventsRegistered = false;
        }
    }

    public IronGolem spawnInfestedGolem(Location location, Location spawnerLoc) {
        IronGolem golem = (IronGolem) location.getWorld().spawnEntity(location, EntityType.IRON_GOLEM);
        applyInfestedAttributes(golem);
        activeGolems.add(golem.getUniqueId());

        if (spawnerLoc != null) {
            linkedSpawners.put(golem.getUniqueId(), spawnerLoc);
        }

        startCentralTask();
        return golem;
    }

    public IronGolem spawnInfestedGolem(Location location) {
        return spawnInfestedGolem(location, null);
    }

    public void transformToInfestedGolem(IronGolem golem) {
        applyInfestedAttributes(golem);
        activeGolems.add(golem.getUniqueId());
        startCentralTask();
    }

    private void applyInfestedAttributes(IronGolem golem) {
        golem.setCustomName(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Infested Golem");
        golem.setCustomNameVisible(false);
        golem.setPlayerCreated(false);

        Objects.requireNonNull(golem.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(40);
        golem.setHealth(40);

        Objects.requireNonNull(golem.getAttribute(Attribute.GENERIC_FOLLOW_RANGE)).setBaseValue(DETECTION_RANGE);
        Objects.requireNonNull(golem.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(10);

        golem.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 99));
        golem.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, PotionEffect.INFINITE_DURATION, 0));

        golem.getPersistentDataContainer().set(infestedKey, PersistentDataType.BYTE, (byte) 1);
    }

    private void activarModoFuria(IronGolem golem) {
        if (!golem.isGlowing()) {
            golem.setGlowing(true);
            golem.getWorld().playSound(golem.getLocation(), Sound.ENTITY_PARROT_IMITATE_WARDEN, 2.0f, 0.1f);
            spawnParticleSphere(golem.getLocation().add(0, 1.5, 0), Particle.DUST,
                    new Particle.DustOptions(Color.AQUA, 1.5f));
        }
    }

    private void desactivarModoFuria(IronGolem golem) {
        if (golem.isGlowing()) {
            golem.setGlowing(false);
            try {
                golem.getWorld().playSound(golem.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 2.0f, 0.1f);
            } catch (Exception e) {
                golem.getWorld().playSound(golem.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 0.1f);
            }
            spawnParticleSphere(golem.getLocation().add(0, 1.5, 0), Particle.END_ROD, null);
        }
    }

    private void startCentralTask() {
        if (mainTask != null && !mainTask.isCancelled()) return;

        mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeGolems.isEmpty()) return;

                Iterator<UUID> it = activeGolems.iterator();

                while (it.hasNext()) {
                    UUID uuid = it.next();
                    Entity entity = Bukkit.getEntity(uuid);

                    if (entity == null || !entity.isValid() || entity.isDead()) {
                        if (entity != null && !entity.isValid()) it.remove();
                        attackCooldowns.remove(uuid);
                        continue;
                    }

                    if (entity instanceof IronGolem golem) {

                        // --- LÓGICA DE SPAWNER VINCULADO ---
                        if (linkedSpawners.containsKey(uuid)) {
                            Location spawnerLoc = linkedSpawners.get(uuid);

                            if (spawnerLoc.getWorld().equals(golem.getWorld())) {
                                // Efecto chispas en spawner
                                Location effectLoc = spawnerLoc.clone().add(0.5, 1.2, 0.5);
                                spawnerLoc.getWorld().spawnParticle(Particle.WAX_ON, effectLoc, 1, 0.2, 0.5, 0.2, 0.0);
                                spawnerLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, effectLoc, 1, 0.3, 0.3, 0.3, 0.05);

                                // 2. Teletransporte si se aleja demasiado (50 bloques)
                                if (golem.getLocation().distance(spawnerLoc) > SPAWNER_TETHER_RANGE) {

                                    // Efectos de salida (donde estaba)
                                    spawnParticleSphere(golem.getLocation().add(0, 1, 0), Particle.PORTAL, null);

                                    // Teleport
                                    Location tpLoc = spawnerLoc.clone().add(0.5, 1, 0.5);
                                    golem.teleport(tpLoc);

                                    // Efectos de llegada (en el spawner)
                                    spawnParticleSphere(tpLoc.add(0, 1, 0), Particle.PORTAL, null);

                                    // Sonido siniestro de enderpearl (pitch 0.1)
                                    golem.getWorld().playSound(tpLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.1f);

                                    golem.setTarget(null);
                                }
                            }
                        }
                        // -----------------------------------

                        if (Math.random() < 0.1) {
                            golem.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                                    golem.getLocation().add(0, 1.5, 0), 5, 0.5, 0.5, 0.5, 0.05);
                        }

                        LivingEntity currentTarget = golem.getTarget();
                        boolean shouldBeAngry = false;

                        if (currentTarget instanceof Player player) {
                            double distanceSq = golem.getLocation().distanceSquared(player.getLocation());

                            if (distanceSq > (LOSE_TARGET_RANGE * LOSE_TARGET_RANGE)
                                    || player.getGameMode() == GameMode.CREATIVE
                                    || player.getGameMode() == GameMode.SPECTATOR) {

                                golem.setTarget(null);
                                shouldBeAngry = false;
                            }
                            else {
                                shouldBeAngry = true;
                            }
                        }

                        if (!shouldBeAngry && golem.getTarget() == null) {
                            for (Entity target : golem.getNearbyEntities(DETECTION_RANGE, 10, DETECTION_RANGE)) {
                                if (target instanceof Player p &&
                                        (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)) {

                                    if (golem.hasLineOfSight(p)) {
                                        golem.setTarget(p);
                                        shouldBeAngry = true;
                                        break;
                                    }
                                }
                            }
                        }

                        if (shouldBeAngry) {
                            activarModoFuria(golem);
                        } else {
                            desactivarModoFuria(golem);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    @EventHandler
    public void onGolemTargetChange(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof IronGolem golem) || !isInfestedGolem(golem)) return;
        if (event.getTarget() instanceof Player) {
            activarModoFuria(golem);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (isInfestedGolem(event.getEntity())) {
            IronGolem golem = (IronGolem) event.getEntity();
            if (event.getDamager() instanceof Player || event.getDamager() instanceof Projectile) {
                activarModoFuria(golem);
            }
            if (event.getDamager() instanceof Projectile) {
                event.setCancelled(true);
                golem.getWorld().playSound(golem.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.5f);
                if (((Projectile) event.getDamager()).getShooter() instanceof Player shooter) {
                    golem.setTarget(shooter);
                }
            }
        }

        if (isInfestedGolem(event.getDamager())) {
            UUID golemId = event.getDamager().getUniqueId();
            long currentTime = System.currentTimeMillis();
            long lastAttack = attackCooldowns.getOrDefault(golemId, 0L);
            if (currentTime - lastAttack < ATTACK_COOLDOWN_MS) {
                event.setCancelled(true);
            } else {
                attackCooldowns.put(golemId, currentTime);
            }
        }
    }

    @EventHandler
    public void onInfestedGolemDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof IronGolem golem && isInfestedGolem(golem)) {
            UUID id = golem.getUniqueId();

            if (linkedSpawners.containsKey(id)) {
                Location spawnerLoc = linkedSpawners.get(id);
                SpawnerInfestedGolems.notifyGolemDeath(spawnerLoc);
                linkedSpawners.remove(id);
            }

            activeGolems.remove(id);
            attackCooldowns.remove(id);

            golem.getWorld().playSound(golem.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 0.8f, 0.5f);
            event.getDrops().clear();
        }
    }

    private void spawnParticleSphere(Location center, Particle particle, Object data) {
        double radius = 2.5;
        for (double i = 0; i <= Math.PI; i += Math.PI / 6) {
            double radiusSin = Math.sin(i);
            double y = Math.cos(i);
            for (double a = 0; a < Math.PI * 2; a += Math.PI / 6) {
                double x = Math.cos(a) * radiusSin;
                double z = Math.sin(a) * radiusSin;
                Location loc = center.clone().add(x * radius, y * radius, z * radius);
                if (data != null) {
                    center.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, 0, data);
                } else {
                    center.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    public static boolean isSpawnerOccupied(Location spawnerLoc) {
        return linkedSpawners.containsValue(spawnerLoc);
    }

    public boolean isInfestedGolem(Entity entity) {
        return entity instanceof IronGolem && entity.getPersistentDataContainer().has(infestedKey, PersistentDataType.BYTE);
    }
}