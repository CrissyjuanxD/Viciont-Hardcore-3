package Habilidades;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class HabilidadesManager {

    private final JavaPlugin plugin;
    private File habilidadesFile;
    private FileConfiguration habilidadesConfig;

    public HabilidadesManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadHabilidadesConfig();
    }

    private void loadHabilidadesConfig() {
        habilidadesFile = new File(plugin.getDataFolder(), "Habilidades.yml");

        if (!habilidadesFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                habilidadesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        habilidadesConfig = YamlConfiguration.loadConfiguration(habilidadesFile);
    }

    public void saveConfig() {
        try {
            habilidadesConfig.save(habilidadesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasHabilidad(UUID playerUUID, HabilidadesType type, int level) {
        String path = playerUUID.toString() + "." + type.name() + "." + level;
        return habilidadesConfig.getBoolean(path, false);
    }

    public void unlockHabilidad(UUID playerUUID, HabilidadesType type, int level) {
        String path = playerUUID.toString() + "." + type.name() + "." + level;
        habilidadesConfig.set(path, true);
        saveConfig();
    }

    public int getHighestLevel(UUID playerUUID, HabilidadesType type) {
        for (int level = 4; level >= 1; level--) {
            if (hasHabilidad(playerUUID, type, level)) {
                return level;
            }
        }
        return 0;
    }

    public Map<HabilidadesType, List<Integer>> getPlayerHabilidades(UUID playerUUID) {
        Map<HabilidadesType, List<Integer>> habilidades = new HashMap<>();

        for (HabilidadesType type : HabilidadesType.values()) {
            List<Integer> levels = new ArrayList<>();
            for (int level = 1; level <= 4; level++) {
                if (hasHabilidad(playerUUID, type, level)) {
                    levels.add(level);
                }
            }
            if (!levels.isEmpty()) {
                habilidades.put(type, levels);
            }
        }

        return habilidades;
    }

    public boolean canUnlock(UUID playerUUID, HabilidadesType type, int level) {
        if (level == 1) {
            return true;
        }

        return hasHabilidad(playerUUID, type, level - 1);
    }
}
