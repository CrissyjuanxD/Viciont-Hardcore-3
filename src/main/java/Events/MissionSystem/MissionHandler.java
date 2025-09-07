package Events.MissionSystem;

import Handlers.DayHandler;
import items.EconomyItems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatColor;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MissionHandler implements Listener {
    private final JavaPlugin plugin;
    private final DayHandler dayHandler;
    private final File missionFile;
    private final Map<Integer, Mission> missions = new HashMap<>();
    private final Set<Integer> activeMissions = new HashSet<>();
    private final Map<Integer, Long> penaltyDays = new HashMap<>();

    public MissionHandler(JavaPlugin plugin, DayHandler dayHandler) {
        this.plugin = plugin;
        this.dayHandler = dayHandler;
        this.missionFile = new File(plugin.getDataFolder(), "misiones.yml");

        // Configurar días de penalización
        setupPenaltyDays();

        // Registrar misiones
        registerMissions();
        ensureFileExists();


        // Iniciar tarea de verificación de penalizaciones
        startPenaltyCheckTask();

        loadMissionData();
    }

    private void setupPenaltyDays() {
        // Día 5: penalizar si no tiene 2 de 4 misiones
        penaltyDays.put(5, 2L);
        // Día 9: penalizar si no tiene 2 de 4 misiones (misiones 5-8)
        penaltyDays.put(9, 2L);
        // Continuar patrón cada 4 días hasta día 29
        for (int day = 13; day <= 29; day += 4) {
            penaltyDays.put(day, 2L);
        }
    }

    private void registerMissions() {
        missions.put(1, new Mission1(plugin, this));
        missions.put(2, new Mission2(plugin, this));
        missions.put(3, new Mission3(plugin, this));
        missions.put(4, new Mission4(plugin, this));
        missions.put(5, new Mission5(plugin, this));
        missions.put(6, new Mission6(plugin, this));
        missions.put(7, new Mission7(plugin, this));
        missions.put(8, new Mission8(plugin, this));
        missions.put(9, new Mission9(plugin, this));
        missions.put(10, new Mission10(plugin, this));
        missions.put(11, new Mission11(plugin, this));
        missions.put(12, new Mission12(plugin, this));
    }

    public void registerAllMissionListeners() {
        // Registrar todas las misiones como listeners
        for (Mission mission : missions.values()) {
            if (mission instanceof Listener) {
                plugin.getServer().getPluginManager().registerEvents((Listener) mission, plugin);
            }
        }

        // Registrar también el MissionHandler itself
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void ensureFileExists() {
        try {
            if (!missionFile.exists()) {
                missionFile.getParentFile().mkdirs();
                missionFile.createNewFile();
                YamlConfiguration.loadConfiguration(missionFile).save(missionFile);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error creando archivo de misiones: " + e.getMessage());
        }
    }

    public void activateMission(CommandSender sender, int missionNumber) {
        if (!missions.containsKey(missionNumber)) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "La misión " + missionNumber + " no existe.");
            return;
        }

        if (activeMissions.contains(missionNumber)) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "La misión " + missionNumber + " ya está activada.");
            return;
        }

        activeMissions.add(missionNumber);

        // Inicializar datos para todos los jugadores registrados
        FileConfiguration config = plugin.getConfig();
        Set<String> allPlayers = config.getConfigurationSection("HasJoinedBefore") != null ?
                config.getConfigurationSection("HasJoinedBefore").getKeys(false) : new HashSet<>();

        for (String uuid : allPlayers) {
            String playerName = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
            if (playerName != null) {
                initializePlayerMissionData(playerName, missionNumber);
            }
        }

        saveMissionData();
        sender.sendMessage(org.bukkit.ChatColor.GREEN + "Misión " + missionNumber + " activada correctamente.");

        // Notificar a jugadores online
        String missionName = missions.get(missionNumber).getName();
        Bukkit.broadcastMessage(org.bukkit.ChatColor.GOLD + "¡Nueva misión disponible: " + org.bukkit.ChatColor.YELLOW + missionName + "!");
    }

    public void deactivateMission(CommandSender sender, int missionNumber) {
        if (!missions.containsKey(missionNumber)) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "La misión " + missionNumber + " no existe.");
            return;
        }

        if (!activeMissions.contains(missionNumber)) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "La misión " + missionNumber + " no está activada.");
            return;
        }

        activeMissions.remove(missionNumber);

        // Remover progreso de todos los jugadores
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionFile);
        FileConfiguration config = plugin.getConfig();
        Set<String> allPlayers = config.getConfigurationSection("HasJoinedBefore") != null ?
                config.getConfigurationSection("HasJoinedBefore").getKeys(false) : new HashSet<>();

        for (String uuid : allPlayers) {
            String playerName = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
            if (playerName != null) {
                // Remover progreso de la misión
                data.set("players." + playerName + ".missions." + missionNumber, null);

                // Decrementar contador de misiones completadas si estaba completada
                if (data.getBoolean("players." + playerName + ".missions." + missionNumber + ".completed", false)) {
                    int completed = data.getInt("players." + playerName + ".completed", 0);
                    data.set("players." + playerName + ".completed", Math.max(0, completed - 1));
                }
            }
        }

        try {
            data.save(missionFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar datos de misiones: " + e.getMessage());
        }

        sender.sendMessage(org.bukkit.ChatColor.GREEN + "Misión " + missionNumber + " desactivada correctamente.");
    }

    public void initializePlayerMissionData(String playerName, int missionNumber) {
        if (!missions.containsKey(missionNumber)) return;

        FileConfiguration data = YamlConfiguration.loadConfiguration(missionFile);

        if (!data.contains("players." + playerName)) {
            data.set("players." + playerName + ".completed", 0);
            data.set("players." + playerName + ".penalized", false);
            data.set("players." + playerName + ".apply_penalized", false);
        }

        // Inicializar datos específicos de la misión
        data.set("players." + playerName + ".missions." + missionNumber + ".completed", false);
        data.set("players." + playerName + ".missions." + missionNumber + ".token_received", false);
        data.set("players." + playerName + ".missions." + missionNumber + ".reward_claimed", false);

        missions.get(missionNumber).initializePlayerData(playerName);

        try {
            data.save(missionFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al inicializar datos de misión: " + e.getMessage());
        }
    }

    public boolean completeMission(String playerName, int missionNumber) {
        if (!activeMissions.contains(missionNumber) || !missions.containsKey(missionNumber)) {
            return false;
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(missionFile);

        if (data.getBoolean("players." + playerName + ".missions." + missionNumber + ".completed", false)) {
            return false;
        }

        data.set("players." + playerName + ".missions." + missionNumber + ".completed", true);

        int completed = data.getInt("players." + playerName + ".completed", 0);
        data.set("players." + playerName + ".completed", completed + 1);

        try {
            data.save(missionFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar progreso de misión: " + e.getMessage());
            return false;
        }

        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            String missionName = missions.get(missionNumber).getName();
            String missionDesc = missions.get(missionNumber).getDescription();

            // Dar ficha de misión
            giveMissionToken(player, missionNumber);

            // Mensaje de completado
            String jsonMessage = String.format(
                    "[\"\",{\"text\":\"\\n۞ \",\"bold\":true,\"color\":\"#ffaa00\"}," +
                            "{\"text\":\"%s\",\"bold\":true,\"color\":\"#87ceeb\"}," +
                            "{\"text\":\" ha completado la misión diaria \",\"color\":\"#7eaee4\"}," +
                            "{\"text\":\"[\",\"color\":\"white\"}," +
                            "{\"text\":\"%s\",\"bold\":true,\"color\":\"#dda0dd\"," +
                            "\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"\",\"extra\":[{\"text\":\"%s\",\"color\":\"green\"}]}}}," +
                            "{\"text\":\"]\\n\",\"color\":\"white\"}]",
                    player.getName(),
                    missionName,
                    missionDesc.replace("\"", "\\\"")
            );

            // Mensaje en consola
            String consoleMessage = player.getName() + " ha completado la misión [" + missionName + "] - " + missionDesc;
            plugin.getLogger().info(consoleMessage);

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
                            plugin.getLogger().warning("Error al reproducir sonido personalizado: " + ex.getMessage());
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error al notificar al jugador: " + e.getMessage());
                }
            }

            player.sendMessage(org.bukkit.ChatColor.GREEN + "Progreso: " + org.bukkit.ChatColor.GOLD + (completed + 1) +
                    org.bukkit.ChatColor.GREEN + "/" + org.bukkit.ChatColor.GOLD + activeMissions.size() +
                    org.bukkit.ChatColor.GREEN + " misiones completadas");
        }

        return true;
    }

    public void addMissionToPlayer(CommandSender sender, String playerName, int missionNumber) {
        if (!missions.containsKey(missionNumber)) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "La misión " + missionNumber + " no existe.");
            return;
        }

        if (!activeMissions.contains(missionNumber)) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "La misión " + missionNumber + " no está activada.");
            return;
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(missionFile);

        if (data.getBoolean("players." + playerName + ".missions." + missionNumber + ".completed", false)) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "El jugador ya tiene esta misión completada.");
            return;
        }

        if (completeMission(playerName, missionNumber)) {
            sender.sendMessage(org.bukkit.ChatColor.GREEN + "Misión " + missionNumber + " añadida a " + playerName);

            Player target = Bukkit.getPlayer(playerName);
            if (target != null) {
                target.sendMessage(org.bukkit.ChatColor.GREEN + "Un administrador te ha completado la misión: " +
                        missions.get(missionNumber).getName());
            }
        }
    }

    public void removeMissionFromPlayer(CommandSender sender, String playerName, int missionNumber) {
        if (!missions.containsKey(missionNumber)) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "La misión " + missionNumber + " no existe.");
            return;
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(missionFile);

        if (!data.getBoolean("players." + playerName + ".missions." + missionNumber + ".completed", false)) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "El jugador no tiene esta misión completada.");
            return;
        }

        // Remover completamente todos los datos de la misión
        data.set("players." + playerName + ".missions." + missionNumber, null);

        // Decrementar contador
        int completed = data.getInt("players." + playerName + ".completed", 0);
        data.set("players." + playerName + ".completed", Math.max(0, completed - 1));

        // Reinicializar los datos de la misión
        initializePlayerMissionData(playerName, missionNumber);
        try {
            data.save(missionFile);
            sender.sendMessage(org.bukkit.ChatColor.GREEN + "Misión " + missionNumber + " removida de " + playerName);

            Player target = Bukkit.getPlayer(playerName);
            if (target != null) {
                target.sendMessage(org.bukkit.ChatColor.RED + "Un administrador te ha removido la misión: " +
                        missions.get(missionNumber).getName());
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error al remover misión: " + e.getMessage());
        }
    }

    public void showPenalizedPlayers(CommandSender sender) {
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionFile);
        FileConfiguration config = plugin.getConfig();

        Set<String> allPlayers = config.getConfigurationSection("HasJoinedBefore") != null ?
                config.getConfigurationSection("HasJoinedBefore").getKeys(false) : new HashSet<>();

        List<String> penalizedPlayers = new ArrayList<>();
        int currentDay = dayHandler.getCurrentDay();

        for (String uuid : allPlayers) {
            String playerName = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
            if (playerName == null) continue;

            String penaltyKey = "players." + playerName + ".penalty_applied_day_" + currentDay;
            if (data.getBoolean(penaltyKey, false)) {
                penalizedPlayers.add(playerName);
            }
        }

        if (penalizedPlayers.isEmpty()) {
            sender.sendMessage(org.bukkit.ChatColor.GREEN + "No hay jugadores penalizados en el día " + currentDay);
        } else {
            sender.sendMessage(org.bukkit.ChatColor.RED + "Jugadores penalizados en el día " + currentDay + " (" + penalizedPlayers.size() + "):");
            for (String player : penalizedPlayers) {
                sender.sendMessage(org.bukkit.ChatColor.GRAY + "- " + player);
            }
        }
    }

    private void giveMissionToken(Player player, int missionNumber) {
        ItemStack token = createMissionToken(missionNumber);

        // Intentar agregar al inventario
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(token);

        // Si quedaron items sin agregar (inventario lleno)
        if (!leftover.isEmpty()) {
            // Dropear los items restantes al suelo
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
            player.sendMessage(ChatColor.of("#FFA07A") + "¡Tu inventario estaba lleno! El token de misión fue dropeado al suelo");
        }

        // Guardar en el archivo que el token fue recibido
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionFile);
        data.set("players." + player.getName() + ".missions." + missionNumber + ".token_received", true);

        try {
            data.save(missionFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar token de misión: " + e.getMessage());
        }
    }

    public ItemStack createMissionToken(int missionNumber) {
        ItemStack token = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = token.getItemMeta();

        meta.setDisplayName(org.bukkit.ChatColor.GOLD + "Ficha de Misión #" + missionNumber);
        meta.setCustomModelData(3000 + missionNumber);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(org.bukkit.ChatColor.GRAY + "Usa esta ficha en un bloque de");
        lore.add(org.bukkit.ChatColor.LIGHT_PURPLE + "Terracota Esmaltada Rosa");
        lore.add(org.bukkit.ChatColor.GRAY + "para reclamar tu recompensa.");
        lore.add("");
        meta.setLore(lore);

        token.setItemMeta(meta);
        return token;
    }

    private void startPenaltyCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                int currentDay = dayHandler.getCurrentDay();
                if (penaltyDays.containsKey(currentDay)) {
                    checkAndApplyPenalties(currentDay);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * 60L); // Verificar cada minuto
    }

    private void checkAndApplyPenalties(int currentDay) {
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionFile);
        FileConfiguration config = plugin.getConfig();

        Set<String> allPlayers = config.getConfigurationSection("HasJoinedBefore") != null ?
                config.getConfigurationSection("HasJoinedBefore").getKeys(false) : new HashSet<>();

        List<String> penalizedPlayers = new ArrayList<>();
        long requiredMissions = penaltyDays.get(currentDay);

        // Determinar qué misiones considerar según el día
        Set<Integer> missionsToCheck = getMissionsForDay(currentDay);

        for (String uuid : allPlayers) {
            String playerName = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
            if (playerName == null) continue;

            // Verificar si ya fue penalizado en este ciclo
            String penaltyKey = "players." + playerName + ".penalty_applied_day_" + currentDay;
            if (data.getBoolean(penaltyKey, false)) {
                continue;
            }

            int completedMissions = 0;
            for (int missionNum : missionsToCheck) {
                if (data.getBoolean("players." + playerName + ".missions." + missionNum + ".completed", false)) {
                    completedMissions++;
                }
            }

            if (completedMissions < requiredMissions) {
                penalizedPlayers.add(playerName);
                data.set(penaltyKey, true);

                Player onlinePlayer = Bukkit.getPlayer(UUID.fromString(uuid));
                if (onlinePlayer != null) {
                    applyPenalty(onlinePlayer);
                } else {
                    // Marcar para aplicar cuando se conecte
                    data.set("players." + playerName + ".pending_penalty", true);
                }
            }
        }

        try {
            data.save(missionFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar penalizaciones: " + e.getMessage());
        }

        if (!penalizedPlayers.isEmpty()) {
        }
    }

    private Set<Integer> getMissionsForDay(int day) {
        Set<Integer> missionsToCheck = new HashSet<>();

        if (day == 5) {
            // Misiones 1-4
            for (int i = 1; i <= 4; i++) {
                missionsToCheck.add(i);
            }
        } else if (day == 9) {
            // Misiones 5-8
            for (int i = 5; i <= 8; i++) {
                missionsToCheck.add(i);
            }
        } else {
            // Calcular rango de misiones para otros días
            int cycleStart = ((day - 5) / 4) * 4 + 1;
            for (int i = cycleStart; i < cycleStart + 4 && i <= 27; i++) {
                missionsToCheck.add(i);
            }
        }

        return missionsToCheck;
    }

    private void applyPenalty(Player player) {
        double currentMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        double newMaxHealth = Math.max(2, currentMaxHealth - 4); // Quitar 2 corazones (4 puntos de salud)

        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(newMaxHealth);

        if (player.getHealth() > newMaxHealth) {
            player.setHealth(newMaxHealth);
        }

        player.sendMessage(org.bukkit.ChatColor.RED + "¡Has sido penalizado por no completar suficientes misiones! Perdiste 2 corazones permanentes.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionFile);

        // Verificar si tiene penalización pendiente
        if (data.getBoolean("players." + player.getName() + ".pending_penalty", false)) {
            applyPenalty(player);
            data.set("players." + player.getName() + ".pending_penalty", false);

            try {
                data.save(missionFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Error al guardar penalización aplicada: " + e.getMessage());
            }
        }
    }

    public void saveMissionData() {
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionFile);
        data.set("activeMissions", new ArrayList<>(activeMissions));

        try {
            data.save(missionFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar datos de misiones: " + e.getMessage());
        }
    }

    public void loadMissionData() {
        if (!missionFile.exists()) return;

        FileConfiguration data = YamlConfiguration.loadConfiguration(missionFile);
        List<Integer> savedActiveMissions = data.getIntegerList("activeMissions");
        activeMissions.addAll(savedActiveMissions);
    }

    public Map<Integer, Mission> getMissions() {
        return missions;
    }

    public Set<Integer> getActiveMissions() {
        return activeMissions;
    }

    public File getMissionFile() {
        return missionFile;
    }

    public boolean isMissionActive(int missionNumber) {
        return activeMissions.contains(missionNumber);
    }
}