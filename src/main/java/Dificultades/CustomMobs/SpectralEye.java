package Dificultades.CustomMobs;

import TitleListener.ErrorNotification;
import TitleListener.SuccessNotification;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpectralEye implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey spectralEyeKey;
    private final NamespacedKey tntBombKey;
    private Team greenGlowTeam;
    private final SuccessNotification successNotification;
    private final ErrorNotification errorNotification;
    private boolean eventsRegistered = false;

    private final Map<UUID, Integer> sneakAttempts = new HashMap<>();
    private final Map<UUID, Boolean> activeBombs = new HashMap<>();
    private final int REQUIRED_SHIFTS = 25;

    public SpectralEye(JavaPlugin plugin) {
        this.plugin = plugin;
        this.spectralEyeKey = new NamespacedKey(plugin, "spectral_eye");
        this.tntBombKey = new NamespacedKey(plugin, "tnt_bomb");
        this.successNotification = new SuccessNotification(plugin);
        this.errorNotification = new ErrorNotification(plugin);
        setupGlowTeam();
    }

    private void setupGlowTeam() {
        if (plugin.getServer().getScoreboardManager() == null) return;

        Team team = plugin.getServer().getScoreboardManager().getMainScoreboard().getTeam("SpectralEyeGlow");
        if (team == null) {
            team = plugin.getServer().getScoreboardManager().getMainScoreboard().registerNewTeam("SpectralEyeGlow");
        }
        team.setColor(ChatColor.GREEN);
        this.greenGlowTeam = team;
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
                    if (entity instanceof Phantom phantom && isSpectralEye(phantom)) {
                        phantom.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public Phantom spawnSpectralEye(Location location) {
        Phantom spectralEye = (Phantom) location.getWorld().spawnEntity(location, EntityType.PHANTOM);
        applySpectralEyeAttributes(spectralEye);
        return spectralEye;
    }

    public void transformSpawnSpectralEye(Phantom phantom) {
        applySpectralEyeAttributes(phantom);
    }

    private void applySpectralEyeAttributes(Phantom phantom) {
        phantom.setCustomName(ChatColor.GREEN + "" + ChatColor.BOLD + "Ojo Espectral");
        phantom.setCustomNameVisible(false);

        try {
            if (phantom.getAttribute(Attribute.GENERIC_FLYING_SPEED) != null) {
                phantom.getAttribute(Attribute.GENERIC_FLYING_SPEED).setBaseValue(0.2);
            }

            if (phantom.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                phantom.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(30);
                phantom.setHealth(30);
            }

            if (phantom.getAttribute(Attribute.GENERIC_FOLLOW_RANGE) != null) {
                phantom.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(64);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error al configurar atributos del Phantom: " + e.getMessage());
        }

        phantom.setGlowing(true);
        if (greenGlowTeam != null) {
            greenGlowTeam.addEntry(phantom.getUniqueId().toString());
        }

        phantom.getPersistentDataContainer().set(spectralEyeKey, PersistentDataType.BYTE, (byte) 1);

        startTrackingTask(phantom);
    }

    private void startTrackingTask(Phantom phantom) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (phantom.isDead() || !phantom.isValid()) {
                    this.cancel();
                    return;
                }

                Player nearest = null;
                double nearestDistance = Double.MAX_VALUE;

                for (Player player : phantom.getWorld().getPlayers()) {
                    double distance = player.getLocation().distance(phantom.getLocation());
                    if (distance <= 100 && distance < nearestDistance) {
                        nearest = player;
                        nearestDistance = distance;
                    }
                }

                if (nearest != null) {
                    phantom.setTarget(nearest);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.getEntity() instanceof Phantom phantom &&
                isSpectralEye(phantom) &&
                event.getTarget() instanceof Player) {

            Player nearest = findNearestPlayer(phantom.getLocation(), 100);
            if (nearest != null) {
                event.setTarget(nearest);
            }
        }
    }

    private Player findNearestPlayer(Location location, double radius) {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : location.getWorld().getPlayers()) {
            double distance = player.getLocation().distance(location);
            if (distance <= radius && distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Phantom phantom &&
                isSpectralEye(phantom) &&
                event.getEntity() instanceof Player player) {

            event.setCancelled(true);

            if (activeBombs.containsKey(player.getUniqueId())) {
                return;
            }

            attachTNTToPlayer(phantom, player);
        }
    }

    private void attachTNTToPlayer(Phantom phantom, Player player) {
        activeBombs.put(player.getUniqueId(), true);

        phantom.getWorld().playSound(phantom.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
        phantom.getWorld().spawnParticle(Particle.LARGE_SMOKE, phantom.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
        phantom.remove();

        ItemDisplay tntDisplay = createTNTDisplay(player);

        sneakAttempts.put(player.getUniqueId(), 0);
        startSneakingChallenge(player, tntDisplay);
    }

    private ItemDisplay createTNTDisplay(Player player) {
        ItemStack tntItem = new ItemStack(Material.TNT);
        ItemMeta meta = tntItem.getItemMeta();
        meta.setCustomModelData(2);
        tntItem.setItemMeta(meta);

        ItemDisplay display = (ItemDisplay) player.getWorld().spawnEntity(
                player.getLocation().add(0, 2, 0),
                EntityType.ITEM_DISPLAY
        );

        display.setItemStack(tntItem);
        display.setGlowing(true);
        display.setBrightness(new Display.Brightness(15, 15));

        display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf(),
                new Vector3f(1, 1, 1),
                new Quaternionf()
        ));

        display.getPersistentDataContainer().set(tntBombKey, PersistentDataType.STRING, player.getUniqueId().toString());

        return display;
    }

    private void startSneakingChallenge(Player player, ItemDisplay tntDisplay) {
        player.sendTitle(
                ChatColor.RED + "¡BOMBA ESPECTRAL!",
                ChatColor.YELLOW + "Presiona Shift " + REQUIRED_SHIFTS + " veces en 15 segundos",
                10, 70, 20
        );
        player.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !tntDisplay.isValid()) {
                    this.cancel();
                    return;
                }

                tntDisplay.teleport(player.getLocation().add(0, 2, 0));
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 2, 0), 3);
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 2, 0), 2);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !activeBombs.containsKey(player.getUniqueId())) {
                    return;
                }

                if (sneakAttempts.getOrDefault(player.getUniqueId(), 0) >= REQUIRED_SHIFTS) {
                    launchTNT(player, tntDisplay);
                } else {
                    explodeTNT(player, tntDisplay);
                }

                sneakAttempts.remove(player.getUniqueId());
                activeBombs.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, 20L * 15);
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!activeBombs.containsKey(playerId)) {
            return;
        }

        int attempts = sneakAttempts.getOrDefault(playerId, 0) + 1;
        sneakAttempts.put(playerId, attempts);

        player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 2);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 0.5f + (attempts * 0.05f));
    }

    private void launchTNT(Player player, ItemDisplay headDisplay) {
        successNotification.showSuccess(player);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);

        headDisplay.remove();

        ItemDisplay tntDisplay = createTNTDisplay(player);
        Location startLoc = player.getEyeLocation();
        tntDisplay.teleport(startLoc);

        // Animación de lanzamiento
        new BukkitRunnable() {
            int ticks = 0;
            final Vector direction = player.getEyeLocation().getDirection().normalize();
            final Location currentLoc = startLoc.clone();
            final int totalTicks = 40;
            final double maxHeight = 3.0;
            final double distance = 10.0;
            final double speedMultiplier = 2.5;

            @Override
            public void run() {
                if (ticks >= totalTicks || !tntDisplay.isValid()) {
                    tntDisplay.setVelocity(direction.multiply(2.5));
                    scheduleExplosion(tntDisplay);
                    this.cancel();
                    return;
                }

                double progress = (double) ticks / totalTicks;

                double xzProgress = Math.min(progress * 2.0, 1.0);
                double yProgress = 4 * progress * (1 - progress);

                Vector motion = direction.clone()
                        .multiply(xzProgress * distance * speedMultiplier)
                        .setY(yProgress * maxHeight);

                Location newLoc = startLoc.clone().add(motion);
                tntDisplay.teleport(newLoc);

                if (ticks % 3 == 0) {
                    tntDisplay.setRotation(
                            player.getLocation().getYaw() + (ticks * 15),
                            (float) (-30 + (progress * 60))
                    );
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void scheduleExplosion(ItemDisplay tntDisplay) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!tntDisplay.isValid()) return;

                Location loc = tntDisplay.getLocation();
                tntDisplay.remove();

                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 5, 5, 5)) {
                    if (entity instanceof Player nearbyPlayer) {

                        if (nearbyPlayer.isBlocking()) {
                            nearbyPlayer.addPotionEffect(new PotionEffect(
                                    PotionEffectType.BLINDNESS,
                                    200,
                                    0
                            ));
                            continue;
                        }
                    }
                }

                loc.getWorld().createExplosion(loc, 7.0f, true, true);
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
                loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
            }
        }.runTaskLater(plugin, 20L * 5);
    }

    private void explodeTNT(Player player, ItemDisplay tntDisplay) {
        if (!player.isOnline() || player.hasMetadata("spectral_eye_exploding")) {
            tntDisplay.remove();
            return;
        }

        tntDisplay.remove();

        errorNotification.showSuccess(player);

        player.setMetadata("spectral_eye_exploding", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

        player.getWorld().createExplosion(player.getLocation(), 7.0f, true, true);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation(), 1);

        if (player.isBlocking()) {
            player.setCooldown(Material.SHIELD, 100);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                player.removeMetadata("spectral_eye_exploding", plugin);
            }
        }.runTaskLater(plugin, 20L);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Phantom phantom && isSpectralEye(phantom)) {
            phantom.getWorld().playSound(phantom.getLocation(), Sound.ENTITY_PHANTOM_HURT, 1.0f, 0.8f);
            phantom.getWorld().spawnParticle(Particle.POOF, phantom.getLocation(), 15,
                    0.5, 0.5, 0.5, 0.1);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player &&
                event.getDamager() instanceof TNTPrimed tnt) {

            if (tnt.getPersistentDataContainer().has(tntBombKey, PersistentDataType.STRING)) {
                String playerId = tnt.getPersistentDataContainer().get(tntBombKey, PersistentDataType.STRING);

                if (player.getUniqueId().toString().equals(playerId)) {
                    return;
                }

                if (player.isBlocking()) {
                    event.setCancelled(true);
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.SLOWNESS,
                            200, // 10 segundos
                            1
                    ));
                }
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Phantom phantom && isSpectralEye(phantom)) {
            event.getDrops().clear();
            event.setDroppedExp(0);

            phantom.getWorld().playSound(phantom.getLocation(), Sound.ENTITY_PHANTOM_DEATH, 1.5f, 0.7f);
            phantom.getWorld().spawnParticle(Particle.LARGE_SMOKE, phantom.getLocation(), 30, 0.5, 0.5, 0.5, 0.2);
            phantom.getWorld().spawnParticle(Particle.SOUL, phantom.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
        }
    }

    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();

        for (Entity entity : player.getNearbyEntities(20, 20, 20)) {
            if (entity instanceof Phantom phantom && isSpectralEye(phantom)) {
                Vector toEntity = phantom.getLocation().toVector().subtract(player.getEyeLocation().toVector());
                double dot = toEntity.normalize().dot(player.getEyeLocation().getDirection());

                if (dot > 0.99) {
                    phantom.setAI(false);
                } else {
                    phantom.setAI(true);
                }
            }
        }
    }

    public NamespacedKey getSpectralEyeKey() {
        return spectralEyeKey;
    }

    public boolean isSpectralEye(Phantom phantom) {
        return phantom.getPersistentDataContainer().has(spectralEyeKey, PersistentDataType.BYTE);
    }
}