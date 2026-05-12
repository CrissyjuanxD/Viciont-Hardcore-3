package Handlers;

import Events.MissionSystem.MissionData;
import items.ItemSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private String host, database, username, password;
    private int port;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        connectWithRetry(3);
    }

    public void loadConfig() {
        this.host = plugin.getConfig().getString("Database1.host");
        this.port = plugin.getConfig().getInt("Database1.port");
        this.database = plugin.getConfig().getString("Database1.database");
        this.username = plugin.getConfig().getString("Database1.username");
        this.password = plugin.getConfig().getString("Database1.password");
    }

    private Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database +
                "?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=5000";
        return DriverManager.getConnection(url, this.username, this.password);
    }

    private void connectWithRetry(int maxRetries) {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                initializeDatabase();
                return; // Si tiene éxito, salimos del bucle
            } catch (SQLException e) {
                attempt++;
                plugin.getLogger().warning("Intento " + attempt + " fallido al conectar a MySQL: " + e.getMessage());
                if (attempt >= maxRetries) {
                    plugin.getLogger().severe("¡No se pudo conectar a la base de datos después de " + maxRetries + " intentos!");
                } else {
                    try {
                        Thread.sleep(2000); // Esperar 2 segundos antes del siguiente intento
                    } catch (InterruptedException ignored) {}
                }
            }
        }
    }

    private void initializeDatabase() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16), " +
                    "first_join DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "team_name VARCHAR(20) DEFAULT 'ZMiembro');");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS global_missions (" +
                    "mission_id INT PRIMARY KEY, " +
                    "is_active BOOLEAN DEFAULT 1, " +
                    "activation_date DATETIME DEFAULT CURRENT_TIMESTAMP);");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_missions (" +
                    "uuid VARCHAR(36), " +
                    "player_name VARCHAR(16), " +
                    "mission_id INT, " +
                    "mission_name VARCHAR(64), " +
                    "is_completed BOOLEAN DEFAULT 0, " +
                    "reward_claimed BOOLEAN DEFAULT 0, " +
                    "progress_json TEXT, " +
                    "PRIMARY KEY (uuid, mission_id));");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS mission_penalties (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "player_name VARCHAR(16), " +
                    "penalty_day INT);");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_backpacks (" +
                    "backpack_uuid VARCHAR(36) PRIMARY KEY, " +
                    "owner_uuid VARCHAR(36), " +
                    "owner_name VARCHAR(16), " +
                    "item_name VARCHAR(128), " +
                    "item_level INT DEFAULT 1, " +
                    "contents LONGTEXT, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_achievements (" +
                    "uuid VARCHAR(36), " +
                    "player_name VARCHAR(16), " +
                    "achievement_id VARCHAR(64), " +
                    "is_completed BOOLEAN DEFAULT 0, " +
                    "progress_json TEXT, " +
                    "PRIMARY KEY (uuid, achievement_id));");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS event_inventories (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "player_name VARCHAR(16), " +
                    "inventory_contents LONGTEXT, " +
                    "saved_at DATETIME DEFAULT CURRENT_TIMESTAMP);");

            plugin.getLogger().info("Conectado a MySQL y tablas verificadas.");

        }
    }

    public void closeConnection() {
    }

    public void saveEventInventory(UUID uuid, String playerName, ItemStack[] contents) {
        String data = ItemSerializer.serialize(contents);
        if (data == null || data.isEmpty()) return;

        String sql = "INSERT INTO event_inventories (uuid, player_name, inventory_contents) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE player_name=?, inventory_contents=?, saved_at=CURRENT_TIMESTAMP";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName);
            stmt.setString(3, data);

            stmt.setString(4, playerName);
            stmt.setString(5, data);

            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error guardando inventario de evento para " + playerName + ": " + e.getMessage());
        }
    }

    public ItemStack[] getEventInventory(UUID uuid) {
        String sql = "SELECT inventory_contents FROM event_inventories WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String data = rs.getString("inventory_contents");
                if (data != null && !data.isEmpty()) {
                    return ItemSerializer.deserialize(data);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error cargando inventario de evento: " + e.getMessage());
        }
        return null;
    }

    public void deleteEventInventory(UUID uuid) {
        String sql = "DELETE FROM event_inventories WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error borrando inventario de evento: " + e.getMessage());
        }
    }

    public boolean hasJoinedBefore(UUID uuid) {
        String sql = "SELECT uuid FROM players WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            plugin.getLogger().severe("¡ERROR CRÍTICO EN BASE DE DATOS! " + e.getMessage());
            return true;
        }
    }

    public void registerPlayer(UUID uuid, String name, String teamName) {
        String sql = "INSERT INTO players (uuid, name, team_name) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.setString(3, teamName);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Set<Integer> getGlobalActiveMissions() {
        Set<Integer> activeMissions = new HashSet<>();
        String sql = "SELECT mission_id FROM global_missions WHERE is_active = 1";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                activeMissions.add(rs.getInt("mission_id"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error cargando misiones activas globales: " + e.getMessage());
        }
        return activeMissions;
    }

    public void setMissionGlobalState(int missionId, boolean active) {
        String sql = "INSERT INTO global_missions (mission_id, is_active) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE is_active = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, missionId);
            stmt.setBoolean(2, active);
            stmt.setBoolean(3, active);
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().severe("Error actualizando estado global de misión: " + e.getMessage());
        }
    }

    public Map<Integer, MissionData> loadPlayerMissions(UUID uuid) {
        Map<Integer, MissionData> missions = new HashMap<>();
        String sql = "SELECT mission_id, is_completed, reward_claimed, progress_json FROM player_missions WHERE uuid = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("mission_id");
                boolean completed = rs.getBoolean("is_completed");
                boolean claimed = rs.getBoolean("reward_claimed");
                String json = rs.getString("progress_json");

                missions.put(id, new MissionData(false, completed, claimed, json));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error cargando misiones: " + e.getMessage());
        }
        return missions;
    }

    public void savePlayerMissionsBatchSync(UUID uuid, String playerName, Map<Integer, MissionData> missionsToSave, Map<Integer, String> missionNames) {
        if (missionsToSave.isEmpty()) return;

        String sql = "INSERT INTO player_missions (uuid, player_name, mission_id, mission_name, is_completed, reward_claimed, progress_json) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE player_name=?, mission_name=?, is_completed=?, reward_claimed=?, progress_json=?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (Map.Entry<Integer, MissionData> entry : missionsToSave.entrySet()) {
                int missionId = entry.getKey();
                MissionData data = entry.getValue();
                String json = data.getJsonProgress();
                String missionName = missionNames.getOrDefault(missionId, "Unknown");

                stmt.setString(1, uuid.toString());
                stmt.setString(2, playerName);
                stmt.setInt(3, missionId);
                stmt.setString(4, missionName);
                stmt.setBoolean(5, data.isCompleted());
                stmt.setBoolean(6, data.isRewardClaimed());
                stmt.setString(7, json);

                stmt.setString(8, playerName);
                stmt.setString(9, missionName);
                stmt.setBoolean(10, data.isCompleted());
                stmt.setBoolean(11, data.isRewardClaimed());
                stmt.setString(12, json);

                stmt.addBatch();
            }

            stmt.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error en guardado Batch (Masivo) para " + playerName + ": " + e.getMessage());
        }
    }

    public void addPendingPenalty(UUID uuid, String playerName, int day) {
        String sql = "INSERT INTO mission_penalties (uuid, player_name, penalty_day) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE penalty_day = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName);
            stmt.setInt(3, day);
            stmt.setInt(4, day);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al añadir penalización pendiente: " + e.getMessage());
        }
    }

    public boolean hasPendingPenalty(UUID uuid) {
        String sql = "SELECT uuid FROM mission_penalties WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al buscar penalización pendiente: " + e.getMessage());
            return false;
        }
    }

    public void removePendingPenalty(UUID uuid) {
        String sql = "DELETE FROM mission_penalties WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al eliminar penalización pendiente: " + e.getMessage());
        }
    }

    public ItemStack[] loadBackpackContents(String backpackUuid) {
        String sql = "SELECT contents FROM player_backpacks WHERE backpack_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, backpackUuid);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String data = rs.getString("contents");
                if (data != null && !data.isEmpty()) {
                    return ItemSerializer.deserialize(data);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error cargando mochila " + backpackUuid + ": " + e.getMessage());
        }
        return null;
    }

    public void saveBackpack(String backpackUuid, UUID ownerUuid, String ownerName, String itemName, int level, ItemStack[] items) throws SQLException {
        String contents = ItemSerializer.serialize(items);
        if (contents == null || contents.isEmpty()) return;

        String sql = "INSERT INTO player_backpacks (backpack_uuid, owner_uuid, owner_name, item_name, item_level, contents) VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE item_name=?, item_level=?, contents=?, updated_at=CURRENT_TIMESTAMP";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, backpackUuid);
            stmt.setString(2, ownerUuid.toString());
            stmt.setString(3, ownerName);
            stmt.setString(4, itemName);
            stmt.setInt(5, level);
            stmt.setString(6, contents);

            stmt.setString(7, itemName);
            stmt.setInt(8, level);
            stmt.setString(9, contents);

            stmt.executeUpdate();
        }
    }

    public boolean deleteBackpack(String backpackId) {
        String sql = "DELETE FROM player_backpacks WHERE backpack_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, backpackId);
            int affected = stmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static class BackpackInfo {
        public String uuid;
        public String itemName;
        public int level;
        public String updatedAt;

        public BackpackInfo(String uuid, String itemName, int level, String updatedAt) {
            this.uuid = uuid;
            this.itemName = itemName;
            this.level = level;
            this.updatedAt = updatedAt;
        }
    }

    public List<BackpackInfo> getPlayerBackpacks(UUID ownerUuid) {
        List<BackpackInfo> list = new ArrayList<>();
        String sql = "SELECT backpack_uuid, item_name, item_level, updated_at FROM player_backpacks WHERE owner_uuid = ? ORDER BY updated_at DESC";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ownerUuid.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new BackpackInfo(
                        rs.getString("backpack_uuid"),
                        rs.getString("item_name"),
                        rs.getInt("item_level"),
                        rs.getString("updated_at")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public UUID getUuidByName(String playerName) {
        String sql = "SELECT uuid FROM players WHERE LOWER(name) = LOWER(?) LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return UUID.fromString(rs.getString("uuid"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error buscando UUID por nombre: " + e.getMessage());
        }
        return null;
    }

    public Map<String, MissionData> loadPlayerAchievements(UUID uuid) {
        Map<String, MissionData> achievements = new HashMap<>();
        String sql = "SELECT achievement_id, is_completed, progress_json FROM player_achievements WHERE uuid = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String id = rs.getString("achievement_id");
                boolean completed = rs.getBoolean("is_completed");
                String json = rs.getString("progress_json");

                achievements.put(id, new MissionData(true, completed, false, json));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error cargando logros: " + e.getMessage());
        }
        return achievements;
    }

    public void savePlayerAchievementSync(UUID uuid, String playerName, String achievementId, MissionData data) {
        String sql = "INSERT INTO player_achievements (uuid, player_name, achievement_id, is_completed, progress_json) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE player_name=?, is_completed=?, progress_json=?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String json = data.getJsonProgress();

            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName);
            stmt.setString(3, achievementId);
            stmt.setBoolean(4, data.isCompleted());
            stmt.setString(5, json);

            stmt.setString(6, playerName);
            stmt.setBoolean(7, data.isCompleted());
            stmt.setString(8, json);

            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error en guardado de logro para " + playerName + ": " + e.getMessage());
        }
    }

    public void resetAllAchievements() {
        String sql = "TRUNCATE TABLE player_achievements";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al resetear todos los logros: " + e.getMessage());
        }
    }

    // EN DatabaseManager.java
    public Set<UUID> getAllRegisteredPlayers() {
        Set<UUID> players = new HashSet<>();
        String sql = "SELECT uuid FROM players";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                players.add(UUID.fromString(rs.getString("uuid")));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error obteniendo todos los jugadores: " + e.getMessage());
        }
        return players;
    }

    public void reload() {
        loadConfig();
        connectWithRetry(3);
        plugin.getLogger().info("¡Configuración de base de datos recargada!");
    }
}