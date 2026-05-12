package Events.AchievementParty;

import Events.MissionSystem.MissionData;
import Handlers.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AchievementPartyHandler implements Listener {
    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;
    private boolean eventActive = false;
    private final Map<UUID, Map<String, MissionData>> playerCache = new ConcurrentHashMap<>();

    private final List<String> achievementIds = Arrays.asList(
            "fly_with_trident", "collect_all_flowers", "second_chance",
            "piglin_transformation", "touch_grass",
            "payback", "alma_piedra", "sculk_shrieker", "nether_fall" // <--- Añadidos aquí
    );

    public AchievementPartyHandler(JavaPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // 1. Registrar los advancements en la memoria del servidor al iniciar
        registerVirtualAdvancements();

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::autoSaveAll, 6000L, 6000L);
    }

    // =========================================================================
    // SISTEMA DE INYECCIÓN DE ADVANCEMENTS SIN DATAPACK
    // =========================================================================
    private void registerVirtualAdvancements() {
        // 1. Crear la RAÍZ (El fondo y la pestaña principal)
        loadVirtualAdvancement("root", null, """
        {
          "display": {
            "icon": { "item": "minecraft:nether_star", "nbt": "{CustomModelData:3000}" },
            "title": {"text": "Fiesta de Logros", "color": "light_purple", "bold": true},
            "description": {"text": "Completa todos los logros antes de que termine el evento."},
            "background": "minecraft:textures/block/purple_concrete.png",
            "show_toast": false,
            "announce_to_chat": false,
            "hidden": false
          },
          "criteria": { "trigger": { "trigger": "minecraft:impossible" } }
        }
        """);

        // 2. Crear los hijos (Conectados a la raíz)
        loadVirtualAdvancement("fly_with_trident", "root", """
        {
          "display": {
            "icon": { "item": "minecraft:trident" },
            "title": {"text": "Yo sé que puedo volar", "color": "gold"},
            "description": {"text": "Sube 300 bloques de altura en menos de 7 segundos.", "color": "white"},
            "frame": "challenge",
            "show_toast": true,
            "announce_to_chat": false,
            "hidden": false
          },
          "criteria": { "trigger": { "trigger": "minecraft:impossible" } }
        }
        """);

        loadVirtualAdvancement("collect_all_flowers", "fly_with_trident", """
        {
          "display": {
            "icon": { "item": "minecraft:poppy" },
            "title": {"text": "Stardew Valley", "color": "gold"},
            "description": {"text": "Consigue todas las flores del juego", "color": "white"},
            "frame": "challenge",
            "show_toast": true,
            "announce_to_chat": false,
            "hidden": false
          },
          "criteria": { "trigger": { "trigger": "minecraft:impossible" } }
        }
        """);

        loadVirtualAdvancement("second_chance", "root", """
        {
          "display": {
            "icon": { "item": "minecraft:totem_of_undying" },
            "title": {"text": "Second Chance", "color": "gold"},
            "description": {"text": "Activa un tótem de los especiales", "color": "white"},
            "frame": "challenge",
            "show_toast": true,
            "announce_to_chat": false,
            "hidden": false
          },
          "criteria": { "trigger": { "trigger": "minecraft:impossible" } }
        }
        """);

        loadVirtualAdvancement("piglin_transformation", "root", """
        {
          "display": {
            "icon": { "item": "minecraft:zombified_piglin_spawn_egg" },
            "title": {"text": "Desarrollo Personal", "color": "gold"},
            "description": {"text": "Haz que un Piglin se transforme en un Zombified Piglin", "color": "white"},
            "frame": "challenge",
            "show_toast": true,
            "announce_to_chat": false,
            "hidden": false
          },
          "criteria": { "trigger": { "trigger": "minecraft:impossible" } }
        }
        """);

        loadVirtualAdvancement("touch_grass", "root", """
        {
          "display": {
            "icon": { "item": "minecraft:diamond_block" },
            "title": {"text": "Ve a tocar pasto", "color": "gold"},
            "description": {"text": "Rompe Bloques de Cobre, Hierro, Oro, Esmeralda, Diamante y Netherite con Mining Fatigue III", "color": "white"},
            "frame": "challenge",
            "show_toast": true,
            "announce_to_chat": false,
            "hidden": false
          },
          "criteria": { "trigger": { "trigger": "minecraft:impossible" } }
        }
        """);

        loadVirtualAdvancement("payback", "root", """
        {
          "display": {
            "icon": { "item": "minecraft:golden_axe" },
            "title": {"text": "Con su propia medicina", "color": "gold"},
            "description": {"text": "Mata a un Piglin Brute con un hacha de oro y al menos una pieza de armadura de oro", "color": "white"},
            "frame": "challenge",
            "show_toast": true,
            "announce_to_chat": false,
            "hidden": false
          },
          "criteria": { "trigger": { "trigger": "minecraft:impossible" } }
        }
        """);

        loadVirtualAdvancement("alma_piedra", "root", """
        {
          "display": {
            "icon": { "item": "minecraft:snowball" },
            "title": {"text": "Alma de Piedra", "color": "gold"},
            "description": {"text": "Lánzale una bola de nieve a un Warden", "color": "white"},
            "frame": "challenge",
            "show_toast": true,
            "announce_to_chat": false,
            "hidden": false
          },
          "criteria": { "trigger": { "trigger": "minecraft:impossible" } }
        }
        """);

        loadVirtualAdvancement("sculk_shrieker", "root", """
        {
          "display": {
            "icon": { "item": "minecraft:sculk_shrieker" },
            "title": {"text": "Jugando a ser músico", "color": "gold"},
            "description": {"text": "Rompe 15 chilladores en el bioma de Deep Dark", "color": "white"},
            "frame": "challenge",
            "show_toast": true,
            "announce_to_chat": false,
            "hidden": false
          },
          "criteria": { "trigger": { "trigger": "minecraft:impossible" } }
        }
        """);

        loadVirtualAdvancement("nether_fall", "root", """
        {
          "display": {
            "icon": { "item": "minecraft:magma_block" },
            "title": {"text": "Descenso al Inframundo", "color": "gold"},
            "description": {"text": "Cae de Y=255 a Y=1 en el Nether en menos de 10 segundos sin usar totems", "color": "white"},
            "frame": "challenge",
            "show_toast": true,
            "announce_to_chat": false,
            "hidden": false
          },
          "criteria": { "trigger": { "trigger": "minecraft:impossible" } }
        }
        """);
    }

    @SuppressWarnings("deprecation")
    private void loadVirtualAdvancement(String id, String parentId, String jsonBody) {
        NamespacedKey key = new NamespacedKey(plugin, "logro_" + id);

        // Si tiene un padre, inyectamos la línea de parent antes de cargarlo
        String finalJson = jsonBody;
        if (parentId != null) {
            String parentStr = "\"parent\": \"" + plugin.getName().toLowerCase() + ":logro_" + parentId + "\",";
            finalJson = finalJson.replaceFirst("\\{", "{\n  " + parentStr);
        }

        try {
            // Limpiar si ya existía (por si se hace /reload)
            if (Bukkit.getAdvancement(key) != null) {
                Bukkit.getUnsafe().removeAdvancement(key);
            }
            Bukkit.getUnsafe().loadAdvancement(key, finalJson);
        } catch (Exception e) {
            plugin.getLogger().warning("No se pudo inyectar el logro: " + id + " - " + e.getMessage());
        }
    }
    // =========================================================================

    public List<String> getAchievementIds() {
        return achievementIds;
    }

    public boolean isEventActive() {
        return eventActive;
    }

    // --- Manejo de Datos (Caché y Base de Datos) ---
    public MissionData getData(Player player, String achievementId) {
        Map<String, MissionData> pData = playerCache.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        return pData.computeIfAbsent(achievementId, k -> new MissionData());
    }

    public void saveData(Player player, String achievementId, MissionData data) {
        Map<String, MissionData> pData = playerCache.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        pData.put(achievementId, data);
        data.setDirty(true);
    }

    private void autoSaveAll() {
        for (Map.Entry<UUID, Map<String, MissionData>> entry : playerCache.entrySet()) {
            UUID uuid = entry.getKey();
            Player player = Bukkit.getPlayer(uuid);
            String playerName = player != null ? player.getName() : Bukkit.getOfflinePlayer(uuid).getName();

            for (Map.Entry<String, MissionData> achEntry : entry.getValue().entrySet()) {
                if (achEntry.getValue().isDirty()) {
                    dbManager.savePlayerAchievementSync(uuid, playerName, achEntry.getKey(), achEntry.getValue());
                    achEntry.getValue().setDirty(false);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, MissionData> data = dbManager.loadPlayerAchievements(uuid);
            playerCache.put(uuid, data);

            if (!eventActive) {
                checkPenaltyOnJoin(player, data);
            }
        });

        // Desbloquear la pestaña para que sea visible
        if (eventActive) {
            grantAdvancement(player, "root");
        }
    }

    private void checkPenaltyOnJoin(Player player, Map<String, MissionData> data) {
        boolean penalized = data.getOrDefault("system", new MissionData()).getProgressBool("penalized");
        boolean penaltyApplied = data.getOrDefault("system", new MissionData()).getProgressBool("apply_penalized");

        if (penalized && !penaltyApplied) {
            Bukkit.getScheduler().runTask(plugin, () -> applyPenalty(player));
        }
    }

    // --- Lógica del Evento ---
    public void startEvent(CommandSender sender) {
        if (eventActive) {
            sender.sendMessage("§c¡El evento de logros ya está activo!");
            return;
        }
        eventActive = true;

        for (Player p : Bukkit.getOnlinePlayers()) {
            grantAdvancement(p, "root"); // Desbloquea la pestaña L
        }

        Bukkit.broadcastMessage("§d§l¡Fiesta de Logros ha comenzado!");
        Bukkit.broadcastMessage("§7Abre tu menú de Advancements (Letra L) en la pestaña 'Logros'.");
    }

    public void endEvent(CommandSender sender) {
        if (!eventActive) {
            sender.sendMessage("§cNo hay ningún evento activo!");
            return;
        }

        eventActive = false;
        Bukkit.broadcastMessage("§d§l¡Fiesta de Logros ha terminado!");

        for (Player p : Bukkit.getOnlinePlayers()) {
            MissionData sysData = getData(p, "system");

            long completados = achievementIds.stream().filter(id -> getData(p, id).isCompleted()).count();

            if (completados < achievementIds.size()) {
                sysData.setProgressValue("penalized", true);
                saveData(p, "system", sysData);
                applyPenalty(p);
            } else {
                p.sendMessage("§a¡Felicidades! Completaste todos los logros a tiempo.");
            }
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::autoSaveAll);
    }

    public void resetEvent(CommandSender sender) {
        eventActive = false;
        playerCache.clear();

        for (Player p : Bukkit.getOnlinePlayers()) {
            revokeAdvancement(p, "root");
            for (String id : achievementIds) {
                revokeAdvancement(p, id);
            }
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            dbManager.resetAllAchievements();
            sender.sendMessage("§aTodo el progreso de los logros ha sido borrado de la base de datos.");
        });
    }

    public void applyPenalty(Player player) {
        MissionData sysData = getData(player, "system");

        if (sysData.getProgressBool("apply_penalized")) return;

        double currentMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();

        if (currentMaxHealth <= 8) {
            player.setHealth(0);
            player.sendMessage("§c¡Has sido ejecutado por no completar la Fiesta de Logros!");
            Bukkit.broadcastMessage("§c" + player.getName() + " ha sido ejecutado por no completar el Evento de Logros.");
        } else {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(Math.max(2, currentMaxHealth - 8));
            player.sendMessage("§c¡No completaste todos los logros! Has perdido 4 corazones permanentes.");
        }

        sysData.setProgressValue("apply_penalized", true);
        saveData(player, "system", sysData);
    }

    // --- Manejo de la Interfaz Nativa ---
    public boolean completeAchievement(Player player, String achievementId) {
        if (!eventActive) return false;

        MissionData data = getData(player, achievementId);
        if (data.isCompleted()) return false;

        data.setCompleted(true);
        saveData(player, achievementId, data);

        grantAdvancement(player, achievementId);

        return true;
    }

    public boolean removeAchievement(Player player, String achievementId) {
        MissionData data = getData(player, achievementId);
        if (!data.isCompleted()) return false;

        data.setCompleted(false);
        data.getProgress().clear();
        saveData(player, achievementId, data);

        revokeAdvancement(player, achievementId);

        return true;
    }

    private void grantAdvancement(Player player, String id) {
        NamespacedKey key = new NamespacedKey(plugin, "logro_" + id);
        Advancement advancement = Bukkit.getAdvancement(key);

        if (advancement != null) {
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            for (String criteria : progress.getRemainingCriteria()) {
                progress.awardCriteria(criteria);
            }
        }
    }

    private void revokeAdvancement(Player player, String id) {
        NamespacedKey key = new NamespacedKey(plugin, "logro_" + id);
        Advancement advancement = Bukkit.getAdvancement(key);

        if (advancement != null) {
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            for (String criteria : progress.getAwardedCriteria()) {
                progress.revokeCriteria(criteria);
            }
        }
    }
}