package Dificultades.CustomMobs;

import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public class Iceologer implements Listener {
    private final JavaPlugin plugin;
    private final Set<Illusioner> activeIceologers = new HashSet<>();
    private final Set<Player> frozenPlayers = new HashSet<>();
    private boolean eventsRegistered = false;
    private final Random random = new Random();

    public Iceologer(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
        }
    }

    public void revert() {
        if (eventsRegistered) {
            for (Illusioner iceologer : activeIceologers) {
                if (iceologer.isValid() && !iceologer.isDead()) {
                    iceologer.remove();
                }
            }
            activeIceologers.clear();
            frozenPlayers.clear();
            eventsRegistered = false;
        }
    }

    public Illusioner spawnIceologer(Location location) {
        Illusioner iceologer = (Illusioner) location.getWorld().spawnEntity(location, EntityType.ILLUSIONER);
        iceologer.setCustomName(ChatColor.AQUA + "" + ChatColor.BOLD + "Iceologer");
        iceologer.setCustomNameVisible(true);

        activeIceologers.add(iceologer);
        monitorIceologer(iceologer);
        return iceologer;
    }

    public void monitorIceologer(Illusioner iceologer) {
        new BukkitRunnable() {

            @Override
            public void run() {
                if (iceologer.isDead() || !iceologer.isValid()) {
                    cancel();
                    activeIceologers.remove(iceologer);
                    return;
                }

                // Comprobar si tiene un objetivo
                if (iceologer.getTarget() instanceof Player player) {
                    if (iceologer.hasLineOfSight(player) && iceologer.getLocation().distance(player.getLocation()) < 15) {
                        // Ataque especial cada 3 segundos
                        if (iceologer.getTicksLived() % 120 == 0) {
                            performSpecialAttack(iceologer, player);
                        }

                        if (iceologer.getTicksLived() % 100 == 0) {
                            performIceBlockAttack(iceologer);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }


    private void performSpecialAttack(Illusioner iceologer, Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 10f, 2f);
        Location startLocation = player.getLocation().add(0, 10, 0);
        BlockData blockData = Material.PACKED_ICE.createBlockData();

        // Crear un solo bloque
        BlockDisplay blockDisplay = (BlockDisplay) iceologer.getWorld().spawnEntity(startLocation, EntityType.BLOCK_DISPLAY);
        blockDisplay.setBlock(blockData);
        blockDisplay.setCustomName("iceSphere");
        blockDisplay.setCustomNameVisible(false);

        // Escala pequeña
        blockDisplay.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf(),
                new Vector3f(0.8f, 0.8f, 0.8f), // Tamaño ajustado
                new Quaternionf()
        ));

        // Añadir glowing celeste
        blockDisplay.setGlowing(true);
        blockDisplay.setGlowColorOverride(Color.AQUA);

        // Animación rápida y suave
        new BukkitRunnable() {
            private float rotationAngle = 0.0f;

            @Override
            public void run() {
                if (player.isDead() || !player.isOnline()) {
                    blockDisplay.remove();
                    cancel();
                    return;
                }

                boolean hit = false;
                Location currentLocation = blockDisplay.getLocation();

                // Verificar impacto con el jugador
                if (currentLocation.distance(player.getLocation()) <= 1.0) {
                    hit = true;
                } else {
                    // Movimiento rápido pero suave
                    Vector direction = player.getLocation().toVector().subtract(currentLocation.toVector()).normalize();
                    blockDisplay.teleport(currentLocation.add(direction.multiply(0.3))); // Movimiento rápido

                    // Rotación fluida
                    rotationAngle += 10.0f;
                    Quaternionf rotation = new Quaternionf().rotateY((float) Math.toRadians(rotationAngle));
                    blockDisplay.setTransformation(new Transformation(
                            new Vector3f(0, 0, 0),
                            rotation,
                            new Vector3f(0.8f, 0.8f, 0.8f), // Mantener tamaño pequeño
                            rotation
                    ));
                }

                if (hit) {
                    if (player.isBlocking()) {
                        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.8f);
                    } else {
                        // Daño y congelación
                        player.damage(2);
                        applyFreezeEffect(player);
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
                        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 30, 0.5, 0.5, 0.5);
                    }
                    blockDisplay.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Animación más suave (1 tick)
    }

    private void applyFreezeEffect(Player player) {
        player.setFreezeTicks(200);
        frozenPlayers.add(player);
        player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.1f);
    }

    // Flechas con partículas blancas y congelamiento al golpear
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Illusioner) {
            if (event.getEntity() instanceof Player player) {
                player.setFreezeTicks(200); // Congelar por 5 segundos
                player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.1f);
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Illusioner iceologer && activeIceologers.contains(iceologer)) {
            if (!(event.getTarget() instanceof Player)) {
                if (event.getTarget() instanceof Player player) {
                    frozenPlayers.remove(player);
                    player.setFreezeTicks(0);
                }
            }
        }
    }

    //ataque de hielo en area

    private void performIceBlockAttack(Illusioner iceologer) {
        // Asegurar que el ataque no sea demasiado frecuente (25% de probabilidad)
        if (random.nextInt(4) != 0) return;

        World world = iceologer.getWorld();

        // Buscar jugadores en un radio de 10 bloques
        List<Player> nearbyPlayers = new ArrayList<>();

        // Obtener entidades cercanas al Iceologer
        for (Entity entity : iceologer.getNearbyEntities(10, 10, 10)) { // Obtener entidades dentro del rango
            if (entity instanceof Player) { // Filtrar solo jugadores
                nearbyPlayers.add((Player) entity); // Añadir a la lista
            }
        }

        // Cancelar si no hay jugadores cercanos
        if (nearbyPlayers.isEmpty()) return;

        // Seleccionar jugador objetivo (aleatorio o más cercano)
        Player target = nearbyPlayers.size() > 1
                ? nearbyPlayers.get(new Random().nextInt(nearbyPlayers.size()))
                : nearbyPlayers.get(0); // Único jugador más cercano

        // Posición objetivo sobre el jugador seleccionado
        Location origin = target.getLocation().add(0, 10, 0); // Bloques caen desde arriba

        // Sonido de preparación
        world.playSound(target.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1f, 0.5f);

        // Crear los 8 bloques alrededor del jugador objetivo
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(45 * i); // Dividir círculo en 8 partes
            double x = Math.cos(angle) * 3; // Radio de 3 bloques
            double z = Math.sin(angle) * 3;
            Location spawnLocation = origin.clone().add(x, 0, z);

            // Crear bloque flotante (usando BlockDisplay)
            BlockData blockData = Material.PACKED_ICE.createBlockData();
            BlockDisplay blockDisplay = (BlockDisplay) world.spawnEntity(spawnLocation, EntityType.BLOCK_DISPLAY);
            blockDisplay.setBlock(blockData);
            blockDisplay.setGlowing(true);
            blockDisplay.setGlowColorOverride(Color.BLUE);

            // Animar caída
            animateFallingBlock(blockDisplay, target.getLocation());
        }
    }


    private void animateFallingBlock(BlockDisplay blockDisplay, Location center) {
        new BukkitRunnable() {
            private double height = 10; // Altura inicial
            private double velocity = 0.2; // Velocidad de caída

            @Override
            public void run() {
                // Actualizar posición
                Location currentLocation = blockDisplay.getLocation();
                height -= velocity;
                blockDisplay.teleport(currentLocation.subtract(0, velocity, 0));

                // Crear partículas mientras cae
                currentLocation.getWorld().spawnParticle(Particle.SNOWFLAKE, currentLocation, 5, 0.1, 0.1, 0.1, 0.1);

                // Al llegar al suelo
                if (height <= 0) {
                    applyExplosionEffect(blockDisplay.getLocation(), center);
                    blockDisplay.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Actualizar cada tick (rápido)
    }

    private void applyExplosionEffect(Location location, Location center) {
        World world = location.getWorld();

        // Crear partículas y sonido al caer
        world.spawnParticle(Particle.EXPLOSION_EMITTER, location, 1);
        world.playSound(location, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 2f, 2f);

        // Aplicar daño y efectos a jugadores en el radio
        double radius = 3.0; // Radio de explosión
        for (Entity entity : world.getNearbyEntities(location, radius, radius, radius)) {
            if (entity instanceof Player player) {
                if (player.getLocation().distance(center) <= radius) { // Solo dentro del círculo
                    player.damage(2); // 1 corazón
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1)); // Lentitud 2 por 3 segundos
                }
            }
        }
    }

}
