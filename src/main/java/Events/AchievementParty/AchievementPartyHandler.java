package Events.AchievementParty;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AchievementPartyHandler implements Listener {
    private final JavaPlugin plugin;
    private boolean eventActive = false;
    private final File achievementsFile;
    private final File configFile;
    private final Map<String, Achievement> achievements = new HashMap<>();

    public AchievementPartyHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.achievementsFile = new File(plugin.getDataFolder(), "achievements_data.yml");
        this.configFile = new File(plugin.getDataFolder(), "config.yml");

        // Registrar logros iniciales
        registerAchievements();
        ensureFilesExist();

        loadPlayerData();
    }

    private void ensureFilesExist() {
        try {
            if (!achievementsFile.exists()) {
                achievementsFile.createNewFile();
                YamlConfiguration.loadConfiguration(achievementsFile).save(achievementsFile);
            }
            if (!configFile.exists()) {
                configFile.createNewFile();
                YamlConfiguration.loadConfiguration(configFile).save(configFile);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error creando archivos de configuración: " + e.getMessage());
        }
    }

    private void registerAchievements() {
        // Registrar los logros iniciales
        achievements.put("nether_fall", new Achievement9(plugin, this));
        achievements.put("sculk_shrieker", new Achievement8(plugin, this));
        achievements.put("alma_piedra", new Achievement7(plugin, this));
        achievements.put("payback", new Achievement6(plugin, this));
        achievements.put("touch_grass", new Achievement5(plugin, this));
        achievements.put("piglin_transformation", new Achievement4(plugin, this));
        achievements.put("second_chance", new Achievement3(plugin, this));
        achievements.put("collect_all_flowers", new Achievement2(plugin, this));
        achievements.put("fly_with_trident", new Achievement1(plugin, this));
    }


    public Map<String, Achievement> getAchievements() {
        return achievements;
    }

    // Método para obtener el índice numérico del logro (para CustomModelData)
    public int getAchievementIndex(Achievement achievement) {
        int index = 0;
        for (Map.Entry<String, Achievement> entry : achievements.entrySet()) {
            if (entry.getValue().equals(achievement)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    // Método para obtener el ID de cadena del logro (la clave en el Map)
    public String getAchievementKey(Achievement achievement) {
        for (Map.Entry<String, Achievement> entry : achievements.entrySet()) {
            if (entry.getValue().equals(achievement)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void startEvent(CommandSender sender) {
        if (eventActive) {
            sender.sendMessage("§cEl evento de logros ya está activo!");
            return;
        }
        resetEvent(sender);

        eventActive = true;
        loadPlayerData();

        // Inicializar tracking para todos los jugadores registrados
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        Set<String> allPlayers = config.getConfigurationSection("HasJoinedBefore") != null ?
                config.getConfigurationSection("HasJoinedBefore").getKeys(false) : new HashSet<>();

        for (String uuid : allPlayers) {
            String playerName = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
            initializePlayerTracking(playerName);
        }

        saveAchievementData();

        Bukkit.broadcastMessage("§d§l¡Fiesta de Logros ha comenzado!");
        Bukkit.broadcastMessage("§7Completa los logros para evitar la penalización al final.");
    }

    public void endEvent(CommandSender sender) {
        if (!eventActive) {
            sender.sendMessage("§cNo hay ningún evento de logros activo!");
            return;
        }

        eventActive = false;
        saveAchievementData();
        FileConfiguration data = YamlConfiguration.loadConfiguration(achievementsFile);
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        Set<String> allPlayers = config.getConfigurationSection("HasJoinedBefore") != null ?
                config.getConfigurationSection("HasJoinedBefore").getKeys(false) : new HashSet<>();

        int requiredAchievements = achievements.size();
        List<String> penalizedPlayers = new ArrayList<>();
        List<String> rewardedPlayers = new ArrayList<>();

        for (String uuid : allPlayers) {
            String playerName = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
            int completed = data.getInt("players." + playerName + ".completed", 0);

            if (completed < requiredAchievements) {
                penalizedPlayers.add(playerName);
                data.set("players." + playerName + ".penalized", true);
                data.set("players." + playerName + ".apply_penalized", false);

                Player onlinePlayer = Bukkit.getPlayer(UUID.fromString(uuid));
                if (onlinePlayer != null) {
                    applyPenalty(onlinePlayer);
                }
            } else {
                rewardedPlayers.add(playerName);
                Player onlinePlayer = Bukkit.getPlayer(UUID.fromString(uuid));
                if (onlinePlayer != null) {
                    giveReward(onlinePlayer);
                }
            }
        }

        try {
            data.save(achievementsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar datos de logros: " + e.getMessage());
        }

        broadcastResults(penalizedPlayers, rewardedPlayers);
    }

    public void applyPenalty(Player player) {
        FileConfiguration data = YamlConfiguration.loadConfiguration(achievementsFile);
        String penalizedPath = "players." + player.getName() + ".penalized";
        String appliedPath = "players." + player.getName() + ".apply_penalized";

        if (data.getBoolean(appliedPath, false)) {
            return;
        }

        // Aplicar penalización
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(Math.max(2, maxHealth - 8));
        player.sendMessage("§c¡No completaste todos los logros! Has perdido 4 corazones permanentes.");

        // Marcar como penalizado y que ya se aplicó
        data.set(penalizedPath, true);
        data.set(appliedPath, true);

        try {
            data.save(achievementsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar penalización: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FileConfiguration data = YamlConfiguration.loadConfiguration(achievementsFile);
        String penalizedPath = "players." + player.getName() + ".penalized";
        String appliedPath = "players." + player.getName() + ".apply_penalized";

        // Verificar si el jugador debe ser penalizado pero no se ha aplicado aún
        if (data.getBoolean(penalizedPath, false) && !data.getBoolean(appliedPath, false)) {
            double currentMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            if (currentMaxHealth > 12) { // 12 = 20 - 8 (6 corazones)
                applyPenalty(player);
            }
        }
    }

    private void giveReward(Player player) {
        ItemStack rewardChest = new ItemStack(Material.CHEST);
        ItemMeta meta = rewardChest.getItemMeta();
        meta.setDisplayName("§6Recompensa de Logros");
        rewardChest.setItemMeta(meta);

        player.getInventory().addItem(rewardChest);
        player.sendMessage("§a¡Felicidades! Has completado todos los logros.");
    }

    private void broadcastResults(List<String> penalizedPlayers, List<String> rewardedPlayers) {
        Bukkit.broadcastMessage("§d§l¡Fiesta de Logros ha terminado!");

        if (!penalizedPlayers.isEmpty()) {
            Bukkit.broadcastMessage("§cJugadores penalizados (" + penalizedPlayers.size() + "):");
            for (String player : penalizedPlayers) {
                Bukkit.broadcastMessage("§7- " + player);
            }
        }

        if (!rewardedPlayers.isEmpty()) {
            Bukkit.broadcastMessage("§aJugadores recompensados (" + rewardedPlayers.size() + "):");
            for (String player : rewardedPlayers) {
                Bukkit.broadcastMessage("§7- " + player);
            }
        }
    }

    public void resetEvent(CommandSender sender) {
        if (eventActive) {
            eventActive = false;
            sender.sendMessage("§eEl evento estaba activo, ha sido desactivado antes de resetear.");
        }

        if (!achievementsFile.exists()) {
            sender.sendMessage("§aNo hay datos de logros que resetear.");
            return;
        }

        if (achievementsFile.delete()) {
            sender.sendMessage("§aLos datos de Fiesta de Logros han sido reseteados.");
        } else {
            sender.sendMessage("§cNo se pudo resetear los datos de logros.");
        }
    }


    public void initializePlayerTracking(String playerName) {
        FileConfiguration data = YamlConfiguration.loadConfiguration(achievementsFile);

        if (!data.contains("players." + playerName)) {
            data.set("players." + playerName + ".completed", 0);

            // Inicializar el progreso para cada logro
            for (String achievementId : achievements.keySet()) {
                data.set("players." + playerName + ".achievements." + achievementId + ".completed", false);
                achievements.get(achievementId).initializePlayerData(playerName);
            }

            try {
                data.save(achievementsFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Error al inicializar datos del jugador: " + e.getMessage());
            }
        }
    }

    private void loadPlayerData() {
        if (!achievementsFile.exists()) {
            return;
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(achievementsFile);

        eventActive = data.getBoolean("eventActive", false);
    }

    public void saveAchievementData() {
        FileConfiguration data = YamlConfiguration.loadConfiguration(achievementsFile);
        data.set("eventActive", eventActive);

        try {
            data.save(achievementsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar los datos de logros: " + e.getMessage());
        }
    }

    public boolean completeAchievement(String playerName, String achievementId) {
        if (!eventActive || !achievements.containsKey(achievementId)) return false;

        FileConfiguration data = YamlConfiguration.loadConfiguration(achievementsFile);

        if (!data.contains("players." + playerName)) {
            initializePlayerTracking(playerName);
        }

        if (data.getBoolean("players." + playerName + ".achievements." + achievementId + ".completed", false)) {
            return false;
        }

        data.set("players." + playerName + ".achievements." + achievementId + ".completed", true);

        int completed = data.getInt("players." + playerName + ".completed", 0);
        data.set("players." + playerName + ".completed", completed + 1);

        try {
            data.save(achievementsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar el progreso del logro: " + e.getMessage());
            return false;
        }

        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            String achievementName = achievements.get(achievementId).getName();
            String achievementDesc = achievements.get(achievementId).getDescription();

            String jsonMessage = String.format(
                    "[\"\",{\"text\":\"\\n۞ \",\"bold\":true,\"color\":\"#1986DE\"}," +
                            "{\"text\":\"%s\",\"bold\":true,\"color\":\"#E43185\"}," +
                            "{\"text\":\" ha completado el logro \",\"color\":\"#1986DE\"}," +
                            "{\"text\":\"[\",\"color\":\"white\"}," +
                            "{\"text\":\"%s\",\"bold\":true,\"color\":\"#AA66E7\"," +
                            "\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"\",\"extra\":[{\"text\":\"%s\",\"color\":\"green\"}]}}}," +
                            "{\"text\":\"]\\n\",\"color\":\"white\"}]",
                    player.getName(),
                    achievementName,
                    achievementDesc.replace("\"", "\\\"") // Escapar comillas
            );

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "tellraw " + onlinePlayer.getName() + " " + jsonMessage);

                    if (onlinePlayer.equals(player)) {
                        onlinePlayer.playSound(player.getLocation(),
                                Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    } else {
                        try {
                            onlinePlayer.playSound(onlinePlayer.getLocation(), "custom.noti", SoundCategory.VOICE, 1f, 2.0f);
                        } catch (Exception ex) {
                            plugin.getLogger().warning("Error al reproducir sonido personalizado para " + onlinePlayer.getName() + ": " + ex.getMessage());
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error al notificar al jugador " + onlinePlayer.getName() + ": " + e.getMessage());
                }
            }

            player.sendMessage("§eProgreso: §a" + (completed + 1) + "§e/§a" +
                    achievements.size() + " §elogros completados");
        }

        return true;
    }

    /**
     * Remueve un logro completado de un jugador
     * @param playerName Nombre del jugador
     * @param achievementId ID del logro
     * @return true si se removió correctamente, false si el logro no existe o no estaba completado
     */
    public boolean removeAchievement(String playerName, String achievementId) {
        if (!achievements.containsKey(achievementId)) {
            return false;
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(achievementsFile);

        if (!data.getBoolean("players." + playerName + ".achievements." + achievementId + ".completed", false)) {
            return false;
        }

        data.set("players." + playerName + ".achievements." + achievementId + ".completed", false);

        int completed = data.getInt("players." + playerName + ".completed", 0);
        data.set("players." + playerName + ".completed", Math.max(0, completed - 1));

        try {
            data.save(achievementsFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Error al remover logro: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene la lista de IDs de logros disponibles
     * @return Lista de IDs de logros
     */
    public List<String> getAchievementIds() {
        return new ArrayList<>(achievements.keySet());
    }

    public boolean isEventActive() {
        return eventActive;
    }

    public File getAchievementsFile() {
        return achievementsFile;
    }

    public void registerNewAchievement(String id, Achievement achievement) {
        achievements.put(id, achievement);
    }
}
