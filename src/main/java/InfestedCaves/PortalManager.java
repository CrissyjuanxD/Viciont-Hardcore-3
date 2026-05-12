package InfestedCaves;

import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import vct.hardcore3.ViciontHardcore3;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class PortalManager implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey PORTAL_KEY;

    public PortalManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.PORTAL_KEY = new NamespacedKey(plugin, "infested_portal_active");

        startParticleTask();
    }

    private void startParticleTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    // Escaneamos solo BlockDisplays para ahorrar recursos
                    for (Entity entity : world.getEntitiesByClass(BlockDisplay.class)) {
                        if (entity.getPersistentDataContainer().has(PORTAL_KEY, PersistentDataType.BYTE)) {
                            world.spawnParticle(Particle.DRAGON_BREATH, entity.getLocation().add(0, 0.2, 0), 3, 1.5, 0, 1.5, 0.02);
                            world.spawnParticle(Particle.PORTAL, entity.getLocation().add(0, 0.5, 0), 2, 1.0, 0.5, 1.0, 0.1);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void spawnPortal(Location loc) {
        World world = loc.getWorld();

        Location spawnLoc = loc.clone();
        spawnLoc.setPitch(0);
        spawnLoc.setYaw(0);
        spawnLoc.setX(spawnLoc.getBlockX() + 0.5);
        spawnLoc.setZ(spawnLoc.getBlockZ() + 0.5);
        spawnLoc.setY(Math.floor(spawnLoc.getY()) + 0.02);

        BlockDisplay display = (BlockDisplay) world.spawnEntity(spawnLoc, EntityType.BLOCK_DISPLAY);
        display.setBlock(Bukkit.createBlockData(Material.PURPLE_GLAZED_TERRACOTTA));

        Transformation transformation = new Transformation(
                new Vector3f(-2.0f, 0.0f, -2.0f),
                new AxisAngle4f((float) (Math.PI / 2), 1, 0, 0),
                new Vector3f(4f, 4f, 0.1f),
                new AxisAngle4f(0, 0, 0, 0)
        );

        display.setTransformation(transformation);
        display.setGlowing(true);
        display.setGlowColorOverride(Color.PURPLE);

        display.getPersistentDataContainer().set(PORTAL_KEY, PersistentDataType.BYTE, (byte) 1);
    }

    public void removePortalNearby(Location loc) {
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 5, 5, 5)) {
            if (entity instanceof BlockDisplay) {
                if (entity.getPersistentDataContainer().has(PORTAL_KEY, PersistentDataType.BYTE)) {
                    entity.remove();
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        // Optimización básica
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() &&
                e.getFrom().getBlockY() == e.getTo().getBlockY() &&
                e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        Player p = e.getPlayer();
        if (p.hasMetadata("Teleporting")) return;

        for (Entity ent : p.getNearbyEntities(2.5, 2.0, 2.5)) {
            if (ent instanceof BlockDisplay) {
                if (ent.getPersistentDataContainer().has(PORTAL_KEY, PersistentDataType.BYTE)) {
                    Location portalLoc = ent.getLocation();
                    Location playerLoc = p.getLocation();

                    if (Math.abs(portalLoc.getX() - playerLoc.getX()) <= 2.0 &&
                            Math.abs(portalLoc.getZ() - playerLoc.getZ()) <= 2.0 &&
                            Math.abs(portalLoc.getY() - playerLoc.getY()) <= 1.5) {

                        triggerTeleport(p);
                        break;
                    }
                }
            }
        }
    }

    private void triggerTeleport(Player p) {
        p.setMetadata("Teleporting", new FixedMetadataValue(plugin, true));

        p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 100, 1));
        p.playSound(p.getLocation(), "minecraft:custom.transition_1", 10.0f, 1.3f);
        p.sendTitle("\uEAA4", "", 50, 80, 20);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!p.isOnline() || ticks >= 80) {
                    this.cancel();
                    return;
                }
                p.spawnParticle(Particle.PORTAL, p.getLocation().add(0, 1, 0), 10, 0.5, 1, 0.5, 0.1);
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);

        new BukkitRunnable() {
            @Override
            public void run() {
                p.removeMetadata("Teleporting", plugin);
                p.removePotionEffect(PotionEffectType.LEVITATION);

                if (p.isOnline()) {
                    decideDestination(p);
                }
            }
        }.runTaskLater(plugin, 80L);
    }

    private void decideDestination(Player p) {
        if (p.getWorld().getName().equals(ViciontHardcore3.WORLD_NAME)) {
            teleportToOverworld(p);
        } else {
            teleportToInfested(p);
        }
    }

    private void teleportToInfested(Player p) {
        World infested = Bukkit.getWorld(ViciontHardcore3.WORLD_NAME);
        if (infested == null) {
            p.sendMessage(ChatColor.RED + "La dimensión Infested Caves no está cargada.");
            return;
        }
        Location safe = findSafeSpawn(infested);
        p.teleport(safe);
        p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1f);
    }

    private void teleportToOverworld(Player p) {
        World overworld = Bukkit.getWorlds().get(0);
        Location spawn = p.getBedSpawnLocation();
        if (spawn == null) spawn = overworld.getSpawnLocation();
        p.teleport(spawn);
        p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 1f);
    }

    public Location findSafeSpawn(World world) {
        Random r = ThreadLocalRandom.current();
        int min = 2500;
        int max = 3000;

        for (int i = 0; i < 20; i++) {
            int x = r.nextInt(max - min + 1) + min;
            int z = r.nextInt(max - min + 1) + min;
            if (r.nextBoolean()) x *= -1;
            if (r.nextBoolean()) z *= -1;

            // Escaneo vertical
            for (int y = 50; y < 110; y++) {
                if (world.getBlockAt(x, y, z).getType().isSolid() &&
                        !world.getBlockAt(x, y+1, z).getType().isSolid() &&
                        !world.getBlockAt(x, y+2, z).getType().isSolid()) {

                    return new Location(world, x + 0.5, y + 1.0, z + 0.5);
                }
            }
        }
        return new Location(world, 2500.5, 100, 2500.5);
    }
}