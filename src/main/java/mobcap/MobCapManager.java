package mobcap;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import mobcap.config.MobCapConfig;
import mobcap.optimization.MobCapOptimizer;
import mobcap.optimization.PerformanceMonitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MobCapManager {
    private static MobCapManager instance;
    private final JavaPlugin plugin;
    private final MobCapConfig config;
    private final Map<String, Integer> originalLimits;
    private final Map<String, Integer> currentLimits;
    private final MobCapOptimizer optimizer;
    private final PerformanceMonitor performanceMonitor;

    private int baseMobCap = 70;
    private MobCapMultiplier multiplier = MobCapMultiplier.NORMAL;
    private boolean enabled = true;
    private boolean isInitialized = false;
    private BukkitTask optimizationTask;

    private MobCapManager(JavaPlugin plugin, MobCapConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.originalLimits = new ConcurrentHashMap<>();
        this.currentLimits = new ConcurrentHashMap<>();
        this.optimizer = new MobCapOptimizer(plugin, config);
        this.performanceMonitor = new PerformanceMonitor();
        initialize();
    }

    public static MobCapManager getInstance(JavaPlugin plugin, MobCapConfig config) {
        if (instance == null) {
            instance = new MobCapManager(plugin, config);
        }
        return instance;
    }

    private void initialize() {
        if (isInitialized) return;

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::initialize);
            return;
        }

        // Cargar configuración
        this.baseMobCap = config.getBaseMobCap();
        this.multiplier = config.getMultiplier();
        this.enabled = config.isEnabled();

        // Guardar límites originales
        for (World world : Bukkit.getWorlds()) {
            int originalLimit = world.getMonsterSpawnLimit();
            originalLimits.put(world.getName(), originalLimit);
            currentLimits.put(world.getName(), originalLimit);
        }

        // Aplicar configuración inicial si está habilitado
        if (enabled) {
            applyMobCapToWorlds();
            startOptimizationTask();
        }

        isInitialized = true;
        plugin.getLogger().info("MobCapManager initialized successfully");
    }

    private void startOptimizationTask() {
        if (optimizationTask != null) {
            optimizationTask.cancel();
        }

        if (!config.isOptimizationEnabled()) {
            return;
        }

        // Tarea de optimización cada 30 segundos
        optimizationTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (enabled && shouldOptimize()) {
                optimizer.optimizeMobCap();
            }
        }, 600L, 600L);
    }

    private boolean shouldOptimize() {
        if (!config.isOptimizationEnabled() || !enabled) {
            return false;
        }

        int playerCount = Bukkit.getOnlinePlayers().size();
        int currentCap = baseMobCap * multiplier.getMultiplier();

        return playerCount > config.getOptimizationPlayerThreshold() &&
                currentCap >= config.getOptimizationMobCapThreshold();
    }

    public synchronized void setBaseMobCap(int mobCapValue) {
        if (!isInitialized) {
            initialize();
        }

        if (this.baseMobCap == mobCapValue) {
            return;
        }

        this.baseMobCap = mobCapValue;
        config.setBaseMobCap(mobCapValue);

        if (enabled) {
            applyMobCapToWorlds();
        }

        plugin.getLogger().info("Base mob cap updated to: " + mobCapValue);
    }

    public synchronized void setMultiplier(MobCapMultiplier multiplier) {
        if (this.multiplier == multiplier) {
            return;
        }

        this.multiplier = multiplier;
        config.setMultiplier(multiplier);

        if (enabled) {
            applyMobCapToWorlds();
        }

        String status = multiplier.getDisplayName();
        plugin.getLogger().info("Mob cap multiplier set to: " + status);

        // Mensaje al servidor
        String message = "§e[MobCap] §eMobCap " + status + " activado.";
        Bukkit.broadcastMessage(message);
    }

    public synchronized void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        this.enabled = enabled;
        config.setEnabled(enabled);

        if (enabled) {
            applyMobCapToWorlds();
            startOptimizationTask();
            Bukkit.broadcastMessage("§e[MobCap] §aSistema de MobCap habilitado.");
        } else {
            resetToOriginalLimits();
            if (optimizationTask != null) {
                optimizationTask.cancel();
            }
            Bukkit.broadcastMessage("§e[MobCap] §cSistema de MobCap deshabilitado.");
        }

        plugin.getLogger().info("MobCap system " + (enabled ? "enabled" : "disabled"));
    }

    private void resetToOriginalLimits() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::resetToOriginalLimits);
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            Integer originalLimit = originalLimits.get(world.getName());
            if (originalLimit != null) {
                world.setMonsterSpawnLimit(originalLimit);
                currentLimits.put(world.getName(), originalLimit);
            }
        }
    }

    private void applyMobCapToWorlds() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::applyMobCapToWorlds);
            return;
        }

        int effectiveMobCap = getCurrentEffectiveMobCap();

        for (World world : Bukkit.getWorlds()) {
            try {
                Integer currentLimit = currentLimits.get(world.getName());
                if (currentLimit == null || !currentLimit.equals(effectiveMobCap)) {
                    world.setMonsterSpawnLimit(effectiveMobCap);
                    currentLimits.put(world.getName(), effectiveMobCap);

                    plugin.getLogger().fine("Updated mob cap for " + world.getName() +
                            " from " + currentLimit + " to " + effectiveMobCap);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to update mob cap for world: " + world.getName(), e);
            }
        }
    }

    public int getCurrentEffectiveMobCap() {
        if (!enabled) {
            return 70; // Valor vanilla por defecto
        }

        int effectiveCap = baseMobCap * multiplier.getMultiplier();

        // Aplicar optimización si es necesario - calcular directamente sin llamar a shouldOptimize()
        int playerCount = Bukkit.getOnlinePlayers().size();
        if (config.isOptimizationEnabled() &&
                playerCount > config.getOptimizationPlayerThreshold() &&
                effectiveCap >= config.getOptimizationMobCapThreshold()) {
            effectiveCap = optimizer.getOptimizedMobCap(effectiveCap);
        }

        return effectiveCap;
    }

    public void handleNewWorld(World world) {
        if (!isInitialized) return;

        int originalLimit = world.getMonsterSpawnLimit();
        originalLimits.put(world.getName(), originalLimit);

        if (enabled) {
            int effectiveMobCap = getCurrentEffectiveMobCap();
            world.setMonsterSpawnLimit(effectiveMobCap);
            currentLimits.put(world.getName(), effectiveMobCap);

            plugin.getLogger().info("Applied mob cap to new world: " + world.getName() +
                    " (limit: " + effectiveMobCap + ")");
        }
    }

    public void resetMobCap() {
        if (!isInitialized) return;

        resetToOriginalLimits();

        baseMobCap = 70;
        multiplier = MobCapMultiplier.NORMAL;
        enabled = true;

        config.resetToDefaults();

        plugin.getLogger().info("Mob cap reset to original values");
    }

    public void reload() {
        config.reload();

        this.baseMobCap = config.getBaseMobCap();
        this.multiplier = config.getMultiplier();
        this.enabled = config.isEnabled();

        if (enabled) {
            applyMobCapToWorlds();
            startOptimizationTask();
        } else {
            resetToOriginalLimits();
            if (optimizationTask != null) {
                optimizationTask.cancel();
            }
        }

        plugin.getLogger().info("MobCap configuration reloaded");
    }

    public MobCapInfo getMobCapInfo() {
        return new MobCapInfo(
                baseMobCap,
                multiplier,
                getCurrentEffectiveMobCap(),
                originalLimits,
                currentLimits,
                Bukkit.getOnlinePlayers().size(),
                shouldOptimize(),
                enabled,
                performanceMonitor.getPerformanceData()
        );
    }

    public void shutdown() {
        if (optimizationTask != null) {
            optimizationTask.cancel();
        }
        resetToOriginalLimits();
        instance = null;
        plugin.getLogger().info("MobCapManager shutdown completed");
    }

    // Getters
    public int getBaseMobCap() {
        return baseMobCap;
    }

    public MobCapMultiplier getMultiplier() {
        return multiplier;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getOriginalLimit(String worldName) {
        return originalLimits.getOrDefault(worldName, 70);
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }
}