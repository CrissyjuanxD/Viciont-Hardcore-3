package CorrupcionAnsiosa;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class CorrupcionAnsiosaManager {
    private final Plugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final HashMap<UUID, PlayerCorruptionData> playerData;
    private boolean enabled = true;

    public CorrupcionAnsiosaManager(Plugin plugin) {
        this.plugin = plugin;

        // Crear la carpeta del plugin si no existe
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        this.dataFile = new File(plugin.getDataFolder(), "corrupcionAnsiosa.yml");
        this.playerData = new HashMap<>();
        loadData();
    }

    public void loadData() {
        // Crear el archivo si no existe
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
                plugin.getLogger().info("Archivo corrupcionAnsiosa.yml creado exitosamente.");
            } catch (IOException e) {
                plugin.getLogger().severe("No se pudo crear corrupcionAnsiosa.yml: " + e.getMessage());
                return;
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Establecer valores por defecto si el archivo está vacío
        if (!dataConfig.contains("enabled")) {
            dataConfig.set("enabled", true);
        }

        enabled = dataConfig.getBoolean("enabled", true);

        // Cargar datos de jugadores existentes
        if (dataConfig.contains("players")) {
            for (String key : dataConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    double corruption = dataConfig.getDouble("players." + key + ".corruption", 100.0);
                    playerData.put(uuid, new PlayerCorruptionData(corruption));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("UUID inválido en corrupcionAnsiosa.yml: " + key);
                }
            }
        }

        saveData(); // Guardar para asegurar que la estructura sea correcta
    }

    public void saveData() {
        try {
            dataConfig.set("enabled", enabled);

            // Limpiar sección de jugadores antes de guardar
            dataConfig.set("players", null);

            for (UUID uuid : playerData.keySet()) {
                PlayerCorruptionData data = playerData.get(uuid);
                dataConfig.set("players." + uuid.toString() + ".corruption", data.getCorruption());
            }

            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar corrupcionAnsiosa.yml: " + e.getMessage());
        }
    }

    public PlayerCorruptionData getPlayerData(Player player) {
        return playerData.computeIfAbsent(player.getUniqueId(), k -> new PlayerCorruptionData(100.0));
    }

    // Método para inicializar datos de un jugador si no existen
    public void initializePlayerData(Player player) {
        if (!playerData.containsKey(player.getUniqueId())) {
            playerData.put(player.getUniqueId(), new PlayerCorruptionData(100.0));
            saveData();
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        saveData();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void addCorruption(Player player, double amount) {
        if (!enabled) return;
        PlayerCorruptionData data = getPlayerData(player);
        double before = data.getCorruption();

        data.addCorruption(amount);
        double after = data.getCorruption();

        saveData();

        double change = after - before;
        if (change != 0)
            sendCorruptionChange(player, change);
    }

    public void removeCorruption(Player player, double amount) {
        if (!enabled) return;
        PlayerCorruptionData data = getPlayerData(player);
        double before = data.getCorruption();

        data.removeCorruption(amount);
        double after = data.getCorruption();

        saveData();

        double change = after - before;
        if (change != 0)
            sendCorruptionChange(player, change);
    }

    public void resetCorruption(Player player) {
        PlayerCorruptionData data = getPlayerData(player);
        data.setCorruption(100.0);
        saveData();
    }

    public double getCorruption(Player player) {
        return getPlayerData(player).getCorruption();
    }

    // Método para obtener todos los datos (útil para comandos con @a)
    public HashMap<UUID, PlayerCorruptionData> getAllPlayerData() {
        return new HashMap<>(playerData);
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public static void sendCorruptionChange(Player player, double change) {

        String symbol = "\u06de";

        Component prefix = Component.text(symbol + " ")
                .color(TextColor.fromHexString("#B228E7"))
                .append(Component.text("Corrupción Ansiosa: ")
                        .color(TextColor.fromHexString("#A777E9"))
                );

        Component amount;
        if (change > 0) {
            amount = Component.text("+" + String.format("%.0f", change) + "%")
                    .color(TextColor.fromHexString("#9ADE6F"))
                    .decorate(TextDecoration.BOLD);
        } else {
            amount = Component.text(String.format("%.0f", change) + "%")
                    .color(TextColor.fromHexString("#E67B79"))
                    .decorate(TextDecoration.BOLD);
        }

        Component finalMsg = prefix.append(amount);

        player.sendActionBar(finalMsg);
    }

}