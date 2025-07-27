package Dificultades.Features;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MobCapManager {
    private static MobCapManager instance;
    private final JavaPlugin plugin;
    private final Map<String, Integer> originalLimits;
    private final Map<String, Integer> currentLimits;
    private int currentMultiplier = 1;
    private boolean isInitialized = false;

    private MobCapManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.originalLimits = new ConcurrentHashMap<>();
        this.currentLimits = new ConcurrentHashMap<>();
        initialize();
    }

    public static MobCapManager getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new MobCapManager(plugin);
        }
        return instance;
    }

    private void initialize() {
        if (isInitialized) return;

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::initialize);
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            int originalLimit = world.getMonsterSpawnLimit();
            originalLimits.put(world.getName(), originalLimit);
            currentLimits.put(world.getName(), originalLimit);
        }
        isInitialized = true;
    }

    public synchronized void updateMobCap(int multiplier) {
        if (!isInitialized) {
            initialize();
        }

        if (this.currentMultiplier == multiplier) {
            return;
        }

        this.currentMultiplier = multiplier;

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> updateMobCap(multiplier));
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            int originalLimit = originalLimits.getOrDefault(world.getName(), 55);
            int newLimit = originalLimit * multiplier;

            Integer currentLimit = currentLimits.get(world.getName());
            if (currentLimit == null || !currentLimit.equals(newLimit)) {
                world.setMonsterSpawnLimit(newLimit);
                currentLimits.put(world.getName(), newLimit);

                plugin.getLogger().info("Updated mob cap for " + world.getName() +
                        " from " + currentLimit + " to " + newLimit);
            }
        }
    }

    public void resetMobCap() {
        updateMobCap(1);
    }

    public void handleNewWorld(World world) {
        if (!isInitialized) return;

        int originalLimit = world.getMonsterSpawnLimit();
        originalLimits.put(world.getName(), originalLimit);

        int newLimit = originalLimit * currentMultiplier;
        world.setMonsterSpawnLimit(newLimit);
        currentLimits.put(world.getName(), newLimit);
    }

    public int getCurrentMultiplier() {
        return currentMultiplier;
    }

    public int getOriginalLimit(String worldName) {
        return originalLimits.getOrDefault(worldName, 55);
    }

    public void shutdown() {
        resetMobCap();
        instance = null;
    }
}