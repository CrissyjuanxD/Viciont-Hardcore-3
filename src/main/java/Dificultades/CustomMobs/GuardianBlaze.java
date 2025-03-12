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
    private final Random random = new Random();
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
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Blaze blaze && isGuardianBlaze(blaze)) {
                        blaze.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }


    public void spawnGuardianBlaze(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        Blaze blaze = (Blaze) world.spawnEntity(location, EntityType.BLAZE);

        blaze.setCustomName(ChatColor.GOLD + "" + ChatColor.BOLD + "Guardian Blaze");
        blaze.setCustomNameVisible(true);
        Objects.requireNonNull(blaze.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(35.0);
        blaze.setHealth(35.0);
        Objects.requireNonNull(blaze.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(18.0);
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

            if (random.nextInt(100) < 95) {
                int amount = getRandomNetheriteScrapAmount();
                Bukkit.getLogger().info("Drop Netherite Scrap añadido: " + amount);
                event.getDrops().add(new ItemStack(Material.NETHERITE_SCRAP, amount));
            }

            // Probabilidad de dropear Guardian Blaze Root (50%)
            if (random.nextInt(2) == 0) {
                ItemStack blazeRoot = createBlazeRoot();
                Bukkit.getLogger().info("Drop Guardian Blaze Root añadido.");
                event.getDrops().add(blazeRoot);
            }
        }
    }

    private int getRandomNetheriteScrapAmount() {
        int randomValue = random.nextInt(100); // Número aleatorio entre 0 y 99

        if (randomValue < 50) { // 50% de probabilidad
            return 1; // Más común
        } else if (randomValue < 80) { // 30% de probabilidad
            return 2; // Intermedio
        } else { // 20% de probabilidad
            return 3; // Más raro
        }
    }

    private ItemStack createBlazeRoot() {
        ItemStack blazeRoot = new ItemStack(Material.BLAZE_ROD, 1);
        ItemMeta meta = blazeRoot.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(blazeRootKey, PersistentDataType.INTEGER, 1);
            meta.setDisplayName(ChatColor.GOLD + "Guardian Blaze Root");
            meta.setUnbreakable(true);
            blazeRoot.setItemMeta(meta);
        }
        return blazeRoot;
    }

    private void startAttackPattern(Blaze blaze) {
        trackPlayers(blaze); // Comenzar a seguir a los jugadores

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

                attackCounter++;
            }
        }.runTaskTimer(plugin, 0L, 100L);
    }

    @EventHandler
    public void onVanillaAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Blaze blaze && isGuardianBlaze(blaze)) {
            if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                event.setCancelled(true);
            } else if (event.getEntity() instanceof Player player) {
                knockbackPlayer(blaze, player);
            }
        }
    }

    private void launchFireballAttack(Blaze blaze) {
        List<Player> nearbyPlayers = getNearbyPlayers(blaze.getWorld(), blaze.getLocation(), 40);

        if (nearbyPlayers.isEmpty()) {
            return;
        }

        new BukkitRunnable() {
            int fireballCount = 0;

            @Override
            public void run() {
                if (fireballCount >= 4 || !blaze.isValid()) { // 4 rondas de ataque
                    cancel();
                    return;
                }

                // Seleccionar al jugador objetivo para esta ronda
                Player target = getClosestPlayer(blaze, nearbyPlayers);

                if (target != null) {
                    Location adjustedBlazeLocation = blaze.getLocation().clone();
                    adjustedBlazeLocation.setY(adjustedBlazeLocation.getY() + 2.5); // Ajuste para el tamaño del Blaze

                    Vector baseDirection = target.getLocation().toVector().subtract(adjustedBlazeLocation.toVector()).normalize();

                    // Lanzar 1 bola de fuego
                    Fireball fireball = blaze.launchProjectile(Fireball.class);
                    fireball.setYield(0);
                    fireball.setIsIncendiary(true);
                    fireball.getPersistentDataContainer().set(new NamespacedKey(plugin, "custom_fireball"), PersistentDataType.BYTE, (byte) 1);

                    // Ajustar dirección con un ligero desvío aleatorio
                    Vector direction = baseDirection.clone().add(new Vector(
                            (Math.random() - 0.5) * 0.1, // Pequeño ajuste aleatorio en X
                            (Math.random() - 0.5) * 0.1, // Pequeño ajuste aleatorio en Y
                            (Math.random() - 0.5) * 0.1  // Pequeño ajuste aleatorio en Z
                    ));

                    fireball.setDirection(direction);
                    fireball.setVelocity(fireball.getVelocity().multiply(1.5));
                }

                fireballCount++;
            }
        }.runTaskTimer(plugin, 0L, 20L); // 1 segundo entre cada ronda
    }

    private Player getClosestPlayer(Blaze blaze, List<Player> players) {
        return players.stream()
                .min((p1, p2) -> Double.compare(p1.getLocation().distance(blaze.getLocation()),
                        p2.getLocation().distance(blaze.getLocation())))
                .orElse(null);
    }

    private void trackPlayers(Blaze blaze) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!blaze.isValid()) {
                    cancel();
                    return;
                }

                List<Player> nearbyPlayers = getNearbyPlayers(blaze.getWorld(), blaze.getLocation(), 40);

                if (!nearbyPlayers.isEmpty()) {
                    Player closestPlayer = getClosestPlayer(blaze, nearbyPlayers);
                    if (closestPlayer != null) {
                        // Mover al Blaze hacia el jugador más cercano
                        Vector direction = closestPlayer.getLocation().toVector().subtract(blaze.getLocation().toVector()).normalize();
                        blaze.setVelocity(direction.multiply(0.5)); // Ajustar la velocidad de movimiento
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Ejecutar cada segundo
    }

    @EventHandler
    public void onFireballHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Fireball fireball &&
                fireball.getPersistentDataContainer().has(new NamespacedKey(plugin, "custom_fireball"), PersistentDataType.BYTE) &&
                event.getEntity() instanceof Player player) {

            event.setDamage(12.0);
        }
    }

    @EventHandler
    public void onFireballExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof Fireball fireball &&
                fireball.getPersistentDataContainer().has(new NamespacedKey(plugin, "custom_fireball"), PersistentDataType.BYTE)) {

            event.blockList().clear();

            Location explosionLocation = fireball.getLocation();
            double explosionRadius = 6.0;

            // Partículas para indicar el radio de explosión
            fireball.getWorld().spawnParticle(Particle.EXPLOSION, explosionLocation, 10);

            // Aplicar daño a los jugadores cercanos
            for (Entity entity : fireball.getWorld().getNearbyEntities(explosionLocation, explosionRadius, explosionRadius, explosionRadius)) {
                if (entity instanceof Player player) {
                    double distance = player.getLocation().distance(explosionLocation);
                    double damage = (1.0 - (distance / explosionRadius)) * 12.0;
                    player.damage(damage, fireball);
                    player.setFireTicks(20);
                }
            }
        }
    }

    private void spawnCircleParticles(Blaze blaze) {
        new BukkitRunnable() {
            double radius = 0;

            @Override
            public void run() {
                if (radius >= 15 || !blaze.isValid()) {
                    cancel();
                    return;
                }

                radius += 0.5;
                double increment = Math.PI / 20;

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


    private void knockbackPlayer(Blaze blaze, Player player) {
        Vector knockback = player.getLocation().toVector().subtract(blaze.getLocation().toVector()).normalize();

        if (!isVectorValid(knockback)) {
            return;
        }

        knockback.multiply(2.5);
        player.setVelocity(knockback);

        blaze.setVelocity(player.getLocation().toVector().subtract(blaze.getLocation().toVector()).normalize().multiply(0.5));
    }

    private boolean isVectorValid(Vector vector) {
        return Double.isFinite(vector.getX()) && Double.isFinite(vector.getY()) && Double.isFinite(vector.getZ());
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