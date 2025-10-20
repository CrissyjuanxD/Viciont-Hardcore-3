package items.Flashlight;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class FlashlightConfig {
    private final JavaPlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public FlashlightConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "Flashlight.yml");
        loadConfig();
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void createDefaultConfig() {
        try {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();

            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(configFile);

            // Configuración por defecto
            defaultConfig.set("brightness", 15);
            defaultConfig.set("degree", 15);
            defaultConfig.set("depth", 30);

            defaultConfig.save(configFile);
            plugin.getLogger().info("Archivo Flashlight.yml creado con configuración por defecto");
        } catch (IOException e) {
            plugin.getLogger().severe("Error creando configuración de Flashlight: " + e.getMessage());
        }
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().info("Configuración de Flashlight recargada");
    }

    public int getBrightness() {
        return config.getInt("brightness", 5);
    }

    public int getDegree() {
        return config.getInt("degree", 15);
    }

    public int getDepth() {
        return config.getInt("depth", 30);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando configuración de Flashlight: " + e.getMessage());
        }
    }
}