package Dificultades.CustomMobs;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class GuardianBlaze implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey blazeRootKey;
    private final NamespacedKey attackActiveKey;
    private boolean eventsRegistered = false;

    public GuardianBlaze(JavaPlugin plugin) {
        this.plugin = plugin;
        this.blazeRootKey = new NamespacedKey(plugin, "blaze_root");
        this.attackActiveKey = new NamespacedKey(plugin, "attack_active");
    }

    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
        }
    }

    public void revert() {
        if (eventsRegistered) {
            // Eliminar todos los Bombitas existentes
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Blaze blaze &&
                            blaze.getPersistentDataContainer().has(blazeRootKey, PersistentDataType.BYTE)) {
                        blaze.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public void spawnGuardianBlaze(Location location) {
        Blaze blaze = (Blaze) Objects.requireNonNull(location.getWorld()).spawnEntity(location, EntityType.BLAZE);

        blaze.setCustomName(ChatColor.GOLD + "" + ChatColor.BOLD + "Guardian Blaze");
        blaze.setCustomNameVisible(true);
        Objects.requireNonNull(blaze.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(40.0);
        blaze.setHealth(40.0);
        Objects.requireNonNull(blaze.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(18.0); // Triple de daño
        Objects.requireNonNull(blaze.getAttribute(Attribute.GENERIC_SCALE)).setBaseValue(1.9);
        blaze.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));

        blaze.getPersistentDataContainer().set(blazeRootKey, PersistentDataType.BYTE, (byte) 1);

        startAttackPattern(blaze);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Blaze blaze && isGuardianBlaze(blaze)) {
            if (event.getTarget() instanceof Player player &&
                    (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);

            if (!blaze.getPersistentDataContainer().has(attackActiveKey, PersistentDataType.BYTE)) {
                blaze.getPersistentDataContainer().set(attackActiveKey, PersistentDataType.BYTE, (byte) 1);
                startAttackPattern(blaze);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Blaze blaze && isGuardianBlaze(blaze)) {
            Bukkit.getLogger().info("Guardian Blaze murió. Procesando drops...");

            event.getDrops().clear();
            Random random = new Random();

            // Drop Netherite Scrap con 1/3 de probabilidad
            if (random.nextInt(3) == 0) {
                Bukkit.getLogger().info("Drop Netherite Scrap añadido.");
                event.getDrops().add(new ItemStack(Material.NETHERITE_SCRAP, 1));
            }

            // Drop Guardian Blaze Root con 1/2 de probabilidad
            if (random.nextInt(2) == 0) {
                ItemStack blazeRoot = new ItemStack(Material.BLAZE_ROD, 1);
                ItemMeta meta = blazeRoot.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "blaze_root"), PersistentDataType.INTEGER, 1);
                    meta.setDisplayName(ChatColor.GOLD + "Guardian Blaze Root");
                    meta.setUnbreakable(true);
                    blazeRoot.setItemMeta(meta);
                }
                Bukkit.getLogger().info("Drop Guardian Blaze Root añadido.");
                event.getDrops().add(blazeRoot);
            }
        }
    }


    private void startAttackPattern(Blaze blaze) {
        new BukkitRunnable() {
            int attackCounter = 0;

            @Override
            public void run() {
                if (!blaze.isValid()) {
                    blaze.getPersistentDataContainer().remove(attackActiveKey);
                    cancel();
                    return;
                }

                if (attackCounter % 3 == 0) {
                    launchFireballAttack(blaze);
                }

                if (attackCounter % 5 == 0) {
                    spawnCircleParticles(blaze);
                }

                if (attackCounter % 4 == 0) { // Cada 4 ciclos, intentar ataque melee
                    engageMeleeAttack(blaze);
                }

                attackCounter++;
            }
        }.runTaskTimer(plugin, 0L, 100L);
    }


    @EventHandler
    public void onVanillaAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Blaze blaze && isGuardianBlaze(blaze)) {
            if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                event.setCancelled(true); // Cancela solo los ataques de fireball vanilla
            }
        }
    }


    private void launchFireballAttack(Blaze blaze) {
        List<Player> nearbyPlayers = getNearbyPlayers(blaze.getWorld(), blaze.getLocation(), 30);

        if (nearbyPlayers.isEmpty()) {
            return; // Si no hay jugadores, no atacar
        }

        new BukkitRunnable() {
            int fireballCount = 0;

            @Override
            public void run() {
                if (fireballCount >= 3 || !blaze.isValid()) {
                    cancel();
                    return;
                }

                // Seleccionar el jugador objetivo para este ciclo
                Player target = nearbyPlayers.get(fireballCount % nearbyPlayers.size());

                // Ajustar la posición del Blaze para que los proyectiles se lancen 2 bloques más abajo
                Location adjustedBlazeLocation = blaze.getLocation().clone();
                adjustedBlazeLocation.setY(adjustedBlazeLocation.getY() + 1);

                // Obtener dirección base hacia el jugador objetivo
                Vector baseDirection = target.getLocation().toVector().subtract(adjustedBlazeLocation.toVector()).normalize();

                // Generar bolas en formación dispersa para evitar colisiones
                for (int i = 0; i < 4; i++) {
                    Fireball fireball = blaze.launchProjectile(Fireball.class);

                    fireball.setYield(0); // No romper bloques
                    fireball.setIsIncendiary(true); // Activa el incendio al impacto
                    fireball.getPersistentDataContainer().set(new NamespacedKey(plugin, "custom_fireball"), PersistentDataType.BYTE, (byte) 1);

                    // Ajustar dirección con un ligero desvío aleatorio
                    double angleOffset = Math.toRadians((i - 2) * 10); // Separar por 10° cada bola
                    Vector direction = baseDirection.clone().rotateAroundY(angleOffset).add(new Vector(
                            (Math.random() - 0.5) * 0.1, // Pequeño ajuste aleatorio en X
                            (Math.random() - 0.5) * 0.1, // Pequeño ajuste aleatorio en Y
                            (Math.random() - 0.5) * 0.1  // Pequeño ajuste aleatorio en Z
                    ));

                    fireball.setDirection(direction);

                    // Ajustar velocidad para dispersarlas más (evitar colisiones)
                    fireball.setVelocity(fireball.getVelocity().multiply(1.5)); // Aumentamos la velocidad
                }

                fireballCount++;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }



    @EventHandler
    public void onFireballHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Fireball fireball &&
                fireball.getPersistentDataContainer().has(new NamespacedKey(plugin, "custom_fireball"), PersistentDataType.BYTE) &&
                event.getEntity() instanceof Player player) {

            // Configurar daño directo
            double baseDamage = 12.0; // Daño directo aumentado
            event.setDamage(baseDamage);
        }
    }


    @EventHandler
    public void onFireballExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof Fireball fireball &&
                fireball.getPersistentDataContainer().has(new NamespacedKey(plugin, "custom_fireball"), PersistentDataType.BYTE)) {

            event.blockList().clear(); // No destruye bloques

            // Aplica daño manualmente
            Location explosionLocation = fireball.getLocation();
            double explosionRadius = 6.0;
            fireball.getWorld().getNearbyEntities(explosionLocation, explosionRadius, explosionRadius, explosionRadius)
                    .stream()
                    .filter(entity -> entity instanceof Player)
                    .forEach(entity -> {
                        Player player = (Player) entity;
                        double distance = player.getLocation().distance(explosionLocation);
                        double damage = (1.0 - (distance / explosionRadius)) * 12.0; // Escala de daño
                        player.damage(damage, fireball);
                        player.setFireTicks(Integer.MAX_VALUE); // Aplica fuego
                    });
        }
    }

    private void spawnCircleParticles(Blaze blaze) {
        new BukkitRunnable() {
            double radius = 0;

            @Override
            public void run() {
                if (radius >= 15 || !blaze.isValid()) { // Radio mínimo de 10 bloques
                    cancel();
                    return;
                }

                radius += 0.5;
                double increment = Math.PI / 20; // Más partículas en el círculo

                for (double angle = 0; angle < 2 * Math.PI; angle += increment) {
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    Location particleLocation = blaze.getLocation().clone().add(x, 1, z);

                    blaze.getWorld().spawnParticle(Particle.LAVA, particleLocation, 2);

                    blaze.getWorld().getNearbyEntities(particleLocation, 1.5, 1.5, 1.5).stream()
                            .filter(entity -> entity instanceof Player)
                            .map(entity -> (Player) entity)
                            .forEach(player -> player.setFireTicks(Integer.MAX_VALUE));
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void engageMeleeAttack(Blaze blaze) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!blaze.isValid()) {
                    cancel();
                    return;
                }

                // Encontrar al jugador más cercano dentro del rango
                Player closestPlayer = getNearbyPlayers(blaze.getWorld(), blaze.getLocation(), 30).stream()
                        .min((p1, p2) -> Double.compare(p1.getLocation().distance(blaze.getLocation()),
                                p2.getLocation().distance(blaze.getLocation())))
                        .orElse(null);

                if (closestPlayer != null) {
                    double distance = closestPlayer.getLocation().distance(blaze.getLocation());

                    if (distance <= 2) {
                        // Ataque cuerpo a cuerpo
                        blaze.setTarget(closestPlayer);
                        knockbackPlayer(blaze, closestPlayer);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // Ejecutar cada 2 segundos
    }

    // Función para aplicar el knockback al jugador
    private void knockbackPlayer(Blaze blaze, Player player) {
        Vector knockback = player.getLocation().toVector().subtract(blaze.getLocation().toVector()).normalize();

        // Verificar si el vector de knockback es válido
        if (!Double.isFinite(knockback.getX()) || !Double.isFinite(knockback.getY()) || !Double.isFinite(knockback.getZ())) {
            return; // Si el vector no es válido, no aplicar el knockback
        }

        knockback.multiply(2.5); // Ajustar la fuerza del knockback
        player.setVelocity(knockback);

        // También podemos hacer que el Blaze se mueva hacia el jugador si es necesario
        blaze.setVelocity(player.getLocation().toVector().subtract(blaze.getLocation().toVector()).normalize().multiply(0.5));
    }



    private List<Player> getNearbyPlayers(World world, Location location, double radius) {
        return world.getPlayers().stream()
                .filter(player -> player.getLocation().distance(location) <= radius)
                .filter(player -> player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR)
                .toList();
    }

    private boolean isGuardianBlaze(Blaze blaze) {
        return blaze.getPersistentDataContainer().has(blazeRootKey, PersistentDataType.BYTE);
    }
}
