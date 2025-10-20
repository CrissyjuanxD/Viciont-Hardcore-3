package mobcap.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import mobcap.MobCapMultiplier;
import mobcap.spawn.SpawnMode;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MobCapConfig {
    private final JavaPlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public MobCapConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "mobcap.yml");
        loadConfig();
        setupDefaults();
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("mobcap.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void setupDefaults() {
        // Configuración general
        config.addDefault("enabled", true);
        config.addDefault("base-mobcap", 70);
        config.addDefault("multiplier", "NORMAL");

        // Optimización
        config.addDefault("optimization.enabled", true);
        config.addDefault("optimization.player-threshold", 20);
        config.addDefault("optimization.mobcap-threshold", 140);
        config.addDefault("optimization.reduction-factor", 0.75);
        config.addDefault("optimization.minimum-percentage", 0.6);
        config.addDefault("optimization.cleanup-distance", 32);
        config.addDefault("optimization.max-cleanup-per-cycle", 50);

        // Spawn personalizado
        config.addDefault("custom-spawn.enabled", false);
        config.addDefault("custom-spawn.oval-pattern", true);
        config.addDefault("custom-spawn.closer-to-player", true);
        config.addDefault("custom-spawn.radius-multiplier", 0.8);

        // Configuración de mobs individuales
        setupMobDefaults();

        config.options().copyDefaults(true);
        saveConfig();
    }

    private void setupMobDefaults() {
        // Configurar todos los mobs hostiles con spawn vanilla por defecto
        String[] hostileMobs = {
                "ZOMBIE", "SKELETON", "CREEPER", "SPIDER", "ENDERMAN", "WITCH",
                "SLIME", "MAGMA_CUBE", "GHAST", "BLAZE", "WITHER_SKELETON",
                "ZOMBIFIED_PIGLIN", "PIGLIN", "PIGLIN_BRUTE", "HOGLIN", "ZOGLIN",
                "DROWNED", "HUSK", "STRAY", "PHANTOM", "SHULKER", "GUARDIAN",
                "ELDER_GUARDIAN", "SILVERFISH", "ENDERMITE", "VEX", "VINDICATOR",
                "PILLAGER", "RAVAGER", "EVOKER", "ILLUSIONER", "CAVE_SPIDER"
        };

        for (String mob : hostileMobs) {
            config.addDefault("mob-spawn-settings." + mob.toLowerCase(), "vanilla");
        }
    }

    public void reload() {
        loadConfig();
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save mobcap.yml: " + e.getMessage());
        }
    }

    // Getters para configuración general
    public boolean isEnabled() {
        return config.getBoolean("enabled", true);
    }

    public int getBaseMobCap() {
        return config.getInt("base-mobcap", 70);
    }

    public MobCapMultiplier getMultiplier() {
        String multiplierStr = config.getString("multiplier", "NORMAL");
        return MobCapMultiplier.fromString(multiplierStr);
    }

    // Getters para optimización
    public boolean isOptimizationEnabled() {
        return config.getBoolean("optimization.enabled", true);
    }

    public int getOptimizationPlayerThreshold() {
        return config.getInt("optimization.player-threshold", 20);
    }

    public int getOptimizationMobCapThreshold() {
        return config.getInt("optimization.mobcap-threshold", 140);
    }

    public double getOptimizationReductionFactor() {
        return config.getDouble("optimization.reduction-factor", 0.75);
    }

    public double getOptimizationMinimumPercentage() {
        return config.getDouble("optimization.minimum-percentage", 0.6);
    }

    public int getOptimizationCleanupDistance() {
        return config.getInt("optimization.cleanup-distance", 32);
    }

    public int getOptimizationMaxCleanupPerCycle() {
        return config.getInt("optimization.max-cleanup-per-cycle", 50);
    }

    // Getters para spawn personalizado
    public boolean isCustomSpawnEnabled() {
        return config.getBoolean("custom-spawn.enabled", false);
    }

    public boolean isOvalPatternEnabled() {
        return config.getBoolean("custom-spawn.oval-pattern", true);
    }

    public boolean isCloserToPlayerEnabled() {
        return config.getBoolean("custom-spawn.closer-to-player", true);
    }

    public double getRadiusMultiplier() {
        return config.getDouble("custom-spawn.radius-multiplier", 0.8);
    }

    // Getters para configuración de mobs
    public SpawnMode getMobSpawnMode(EntityType entityType) {
        String mode = config.getString("mob-spawn-settings." + entityType.name().toLowerCase(), "vanilla");
        return SpawnMode.fromString(mode);
    }

    public Map<EntityType, SpawnMode> getAllMobSpawnModes() {
        Map<EntityType, SpawnMode> modes = new HashMap<>();

        if (config.getConfigurationSection("mob-spawn-settings") != null) {
            for (String key : config.getConfigurationSection("mob-spawn-settings").getKeys(false)) {
                try {
                    EntityType entityType = EntityType.valueOf(key.toUpperCase());
                    SpawnMode mode = SpawnMode.fromString(config.getString("mob-spawn-settings." + key, "vanilla"));
                    modes.put(entityType, mode);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid entity type in config: " + key);
                }
            }
        }

        return modes;
    }

    // Setters
    public void setEnabled(boolean enabled) {
        config.set("enabled", enabled);
        saveConfig();
    }

    public void setBaseMobCap(int baseMobCap) {
        config.set("base-mobcap", baseMobCap);
        saveConfig();
    }

    public void setMultiplier(MobCapMultiplier multiplier) {
        config.set("multiplier", multiplier.name());
        saveConfig();
    }

    public void setMobSpawnMode(EntityType entityType, SpawnMode mode) {
        config.set("mob-spawn-settings." + entityType.name().toLowerCase(), mode.name().toLowerCase());
        saveConfig();
    }

    public void resetToDefaults() {
        config.set("enabled", true);
        config.set("base-mobcap", 70);
        config.set("multiplier", "NORMAL");
        saveConfig();
    }
}