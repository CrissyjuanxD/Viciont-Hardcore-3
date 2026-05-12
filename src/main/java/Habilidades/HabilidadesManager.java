package Habilidades;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

import java.util.HashSet;
import org.bukkit.entity.Player;

public class HabilidadesManager {

    private final JavaPlugin plugin;
    private File habilidadesFile;
    private FileConfiguration habilidadesConfig;

    private final Set<UUID> disabledPlayers = new HashSet<>();
    private boolean globalDisabled = false;

    public HabilidadesManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadHabilidadesConfig();
    }

    public void disableHabilidades(Player player) {
        disabledPlayers.add(player.getUniqueId());
        habilidadesConfig.set("disabled_players." + player.getUniqueId().toString(), true);
        saveConfig();
    }

    public void enableHabilidades(Player player) {
        disabledPlayers.remove(player.getUniqueId());
        habilidadesConfig.set("disabled_players." + player.getUniqueId().toString(), false);
        saveConfig();
    }

    public void setGlobalDisabled(boolean disabled) {
        this.globalDisabled = disabled;
        habilidadesConfig.set("global_disabled", disabled);
        saveConfig();
    }

    public boolean isGlobalDisabled() {
        return globalDisabled;
    }

    public boolean areHabilidadesDisabled(UUID playerUUID) {
        return globalDisabled || disabledPlayers.contains(playerUUID);
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
        globalDisabled = habilidadesConfig.getBoolean("global_disabled", false);

        if (habilidadesConfig.contains("disabled_players")) {
            for (String uuidStr : habilidadesConfig.getConfigurationSection("disabled_players").getKeys(false)) {
                if (habilidadesConfig.getBoolean("disabled_players." + uuidStr)) {
                    disabledPlayers.add(UUID.fromString(uuidStr));
                }
            }
        }
    }

    public void saveConfig() {
        try {
            habilidadesConfig.save(habilidadesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasHabilidad(UUID playerUUID, HabilidadesType type, int level) {
        if (areHabilidadesDisabled(playerUUID)) {
            return false;
        }
        String path = playerUUID.toString() + "." + type.name() + "." + level;
        return habilidadesConfig.getBoolean(path, false);
    }

    public boolean hasHabilidadPurchased(UUID playerUUID, HabilidadesType type, int level) {
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
                if (hasHabilidadPurchased(playerUUID, type, level)) {
                    levels.add(level);
                }
            }
            if (!levels.isEmpty()) {
                habilidades.put(type, levels);
            }
        }
        return habilidades;
    }

    public void removeHabilidad(UUID playerUUID, HabilidadesType type, int level) {
        String path = playerUUID.toString() + "." + type.name() + "." + level;
        habilidadesConfig.set(path, false);
        saveConfig();
    }

    public boolean canUnlock(UUID playerUUID, HabilidadesType type, int level) {
        if (level == 1) {
            return true;
        }
        return hasHabilidadPurchased(playerUUID, type, level - 1);
    }
}