package items.Flashlight;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FlashlightManager implements Listener {
    private final JavaPlugin plugin;
    private final FlashlightItem flashlightItem;
    private final FlashlightConfig flashlightConfig;
    private final Map<UUID, Boolean> playerFlashlightState = new ConcurrentHashMap<>();
    private final Set<BlockLoc> lightBlocks = ConcurrentHashMap.newKeySet();
    private BukkitRunnable updateTask;

    public FlashlightManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.flashlightItem = new FlashlightItem(plugin);
        this.flashlightConfig = new FlashlightConfig(plugin);

        startUpdateTask();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().toString().contains("RIGHT")) return;

        ItemStack item = event.getItem();
        if (!flashlightItem.isFlashlight(item)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        boolean wasOn = flashlightItem.isFlashlightOn(item);
        ItemStack newItem = flashlightItem.toggleFlashlight(item);

        // Actualizar el item en la mano correspondiente
        if (player.getInventory().getItemInMainHand().equals(item)) {
            player.getInventory().setItemInMainHand(newItem);
        } else if (player.getInventory().getItemInOffHand().equals(item)) {
            player.getInventory().setItemInOffHand(newItem);
        }

        // Actualizar estado del jugador
        boolean newState = !wasOn;
        playerFlashlightState.put(player.getUniqueId(), newState);

        // Sonidos
        if (newState) {
            player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 1.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.5f, 2.0f);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 0.8f);
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.3f, 1.0f);
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Verificar si tenía una linterna encendida y ya no la tiene
        if (playerFlashlightState.getOrDefault(playerId, false)) {
            if (!hasFlashlightInHands(player)) {
                playerFlashlightState.put(playerId, false);
                clearPlayerLights(playerId);
            }
        }

        // Verificar si ahora tiene una linterna encendida
        new BukkitRunnable() {
            @Override
            public void run() {
                updatePlayerFlashlightState(player);
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        playerFlashlightState.remove(playerId);
        clearPlayerLights(playerId);
    }

    private boolean hasFlashlightInHands(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        return (flashlightItem.isFlashlight(mainHand) && flashlightItem.isFlashlightOn(mainHand)) ||
                (flashlightItem.isFlashlight(offHand) && flashlightItem.isFlashlightOn(offHand));
    }

    private void updatePlayerFlashlightState(Player player) {
        boolean hasFlashlightOn = hasFlashlightInHands(player);
        playerFlashlightState.put(player.getUniqueId(), hasFlashlightOn);

        if (!hasFlashlightOn) {
            clearPlayerLights(player.getUniqueId());
        }
    }

    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateFlashlights();
            }
        };
        updateTask.runTaskTimer(plugin, 1L, 1L);
    }

    private void updateFlashlights() {
        Set<BlockLoc> newLightBlocks = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerFlashlightState.getOrDefault(player.getUniqueId(), false)) {
                if (hasFlashlightInHands(player)) {
                    newLightBlocks.addAll(calculateLightBlocks(player));
                } else {
                    playerFlashlightState.put(player.getUniqueId(), false);
                }
            }
        }

        // Remover bloques de luz antiguos
        for (BlockLoc blockLoc : lightBlocks) {
            if (!newLightBlocks.contains(blockLoc)) {
                Block block = blockLoc.getBlock();
                if (block.getType() == Material.LIGHT) {
                    block.setType(Material.AIR, false);
                }
            }
        }

        // Añadir nuevos bloques de luz
        for (BlockLoc blockLoc : newLightBlocks) {
            Block block = blockLoc.getBlock();
            if (block.getType() == Material.AIR || block.getType() == Material.LIGHT) {
                block.setType(Material.LIGHT, false);
                Levelled level = (Levelled) block.getBlockData();
                level.setLevel(flashlightConfig.getBrightness());
                block.setBlockData(level, false);
            }
        }

        lightBlocks.clear();
        lightBlocks.addAll(newLightBlocks);
    }

    private Set<BlockLoc> calculateLightBlocks(Player player) {
        Set<BlockLoc> blocks = new HashSet<>();

        int degree = flashlightConfig.getDegree();
        int depth = flashlightConfig.getDepth();

        Vector direction = player.getEyeLocation().getDirection().normalize();
        Location eyeLocation = player.getEyeLocation();

        double maxPhi = Math.toRadians(degree);
        double minPhi = Math.toRadians(5);
        int phiSamples = 8;
        int thetaSamples = 16;

        Vector v = getPerpendicularVector(direction);
        Vector w = direction.clone().crossProduct(v).normalize();

        for (int thetaStep = 0; thetaStep < thetaSamples; thetaStep++) {
            double theta = 2 * Math.PI * thetaStep / thetaSamples;
            for (int phiStep = 0; phiStep < phiSamples; phiStep++) {
                double phi = (maxPhi - minPhi) * phiStep / phiSamples + minPhi;
                for (int d = 1; d <= depth; d++) {
                    Vector ray = w.clone().multiply(Math.sin(phi) * Math.cos(theta))
                            .add(v.clone().multiply(Math.sin(phi) * Math.sin(theta)))
                            .add(direction.clone().multiply(Math.cos(phi)))
                            .normalize().multiply(d);

                    Location location = eyeLocation.clone().add(ray);
                    Block block = location.getBlock();

                    if (block.getType() == Material.AIR || block.getType() == Material.LIGHT) {
                        blocks.add(new BlockLoc(location));
                    } else if (!block.getType().isTransparent()) {
                        break;
                    }
                }
            }
        }

        return blocks;
    }

    private Vector getPerpendicularVector(Vector v) {
        Vector ret = new Vector(v.getZ(), v.getZ(), -v.getX() - v.getY());
        if (ret.isZero()) {
            return new Vector(-v.getY() - v.getZ(), v.getX(), v.getX()).normalize();
        }
        return ret.normalize();
    }

    private void clearPlayerLights(UUID playerId) {
        // Los bloques de luz se limpiarán automáticamente en el próximo tick del updateTask
    }

    public void clearAllLights() {
        for (BlockLoc blockLoc : lightBlocks) {
            Block block = blockLoc.getBlock();
            if (block.getType() == Material.LIGHT) {
                block.setType(Material.AIR, false);
            }
        }
        lightBlocks.clear();
        playerFlashlightState.clear();
    }

    public FlashlightConfig getConfig() {
        return flashlightConfig;
    }

    public void shutdown() {
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
        }
        clearAllLights();
    }

    // Clase interna para representar ubicaciones de bloques
    private static class BlockLoc {
        private final World world;
        private final int x, y, z;

        public BlockLoc(Location location) {
            this.world = location.getWorld();
            this.x = location.getBlockX();
            this.y = location.getBlockY();
            this.z = location.getBlockZ();
        }

        public Block getBlock() {
            return world.getBlockAt(x, y, z);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            BlockLoc blockLoc = (BlockLoc) obj;
            return x == blockLoc.x && y == blockLoc.y && z == blockLoc.z &&
                    Objects.equals(world, blockLoc.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, x, y, z);
        }
    }
}