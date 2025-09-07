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
    private int currentMobCap = 70; // Valor por defecto
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

    public synchronized void setMobCap(int mobCapValue) {
        if (!isInitialized) {
            initialize();
        }

        if (this.currentMobCap == mobCapValue) {
            return;
        }

        this.currentMobCap = mobCapValue;

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> setMobCap(mobCapValue));
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            Integer currentLimit = currentLimits.get(world.getName());
            if (currentLimit == null || !currentLimit.equals(mobCapValue)) {
                world.setMonsterSpawnLimit(mobCapValue);
                currentLimits.put(world.getName(), mobCapValue);

                plugin.getLogger().info("Updated mob cap for " + world.getName() +
                        " from " + currentLimit + " to " + mobCapValue);
            }
        }
    }


    public void updateMobCap(int mobCapValue) {
        setMobCap(mobCapValue);
    }

    public void resetMobCap() {
        if (!isInitialized) return;

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::resetMobCap);
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            Integer originalLimit = originalLimits.get(world.getName());
            if (originalLimit != null) {
                world.setMonsterSpawnLimit(originalLimit);
                currentLimits.put(world.getName(), originalLimit);
            }
        }
        currentMobCap = 70;
    }

    public void handleNewWorld(World world) {
        if (!isInitialized) return;

        int originalLimit = world.getMonsterSpawnLimit();
        originalLimits.put(world.getName(), originalLimit);

        world.setMonsterSpawnLimit(currentMobCap);
        currentLimits.put(world.getName(), currentMobCap);
    }

    public int getCurrentMobCap() {
        return currentMobCap;
    }

    public int getOriginalLimit(String worldName) {
        return originalLimits.getOrDefault(worldName, 70);
    }

    public void shutdown() {
        resetMobCap();
        instance = null;
    }
}