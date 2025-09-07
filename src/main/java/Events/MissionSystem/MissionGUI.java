package Events.MissionSystem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MissionGUI implements Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final ItemStack grayPane = createGrayPane();

    public MissionGUI(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private ItemStack createGrayPane() {
        ItemStack pane = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.setCustomModelData(2);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    public void openMissionGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "\u3201\u3201" + org.bukkit.ChatColor.WHITE + "\u3202");

        // Llenar slots 0-26 con paneles grises
        for (int i = 0; i <= 26; i++) {
            gui.setItem(i, grayPane);
        }

        // Obtener datos del jugador
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        String playerName = player.getName();

        // Colocar misiones en slots 27-53
        Map<Integer, Mission> allMissions = missionHandler.getMissions();
        for (int missionNum = 1; missionNum <= 27; missionNum++) {
            int slot = 26 + missionNum; // Slot 27 para misión 1, etc.

            if (slot > 53) break; // No exceder el tamaño del inventario

            if (allMissions.containsKey(missionNum)) {
                Mission mission = allMissions.get(missionNum);
                boolean isActive = missionHandler.isMissionActive(missionNum);
                boolean isCompleted = data.getBoolean("players." + playerName + ".missions." + missionNum + ".completed", false);

                gui.setItem(slot, createMissionItem(mission, isActive, isCompleted, playerName));
            } else {
                // Misión no implementada aún
                gui.setItem(slot, createUnknownMissionItem(missionNum));
            }
        }

        player.openInventory(gui);
    }

    private ItemStack createMissionItem(Mission mission, boolean isActive, boolean isCompleted, String playerName) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        String displayName;
        int customModelData;

        if (!isActive) {
            displayName = ChatColor.of("#A0A0A0") + "???";
            customModelData = 2002;
        } else if (isCompleted) {
            displayName = ChatColor.of("#90EE90") + mission.getName();
            customModelData = 2000;
        } else {
            displayName = ChatColor.of("#FFB6C1") + mission.getName();
            customModelData = 2001;
        }

        meta.setDisplayName(displayName);
        meta.setCustomModelData(customModelData);

        List<String> lore = new ArrayList<>();

        if (isActive) {
            lore.add(ChatColor.of("#D3D3D3") + mission.getDescription());
            lore.add("");
            lore.add(isCompleted ? ChatColor.of("#98FB98") + "✔ Completada" : ChatColor.of("#FFA07A") + "✖ Pendiente");

            // Agregar progreso específico para misiones con listas
            addMissionSpecificProgress(mission, playerName, lore);
        } else {
            lore.add(ChatColor.of("#D3D3D3") + "Misión no descubierta");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createUnknownMissionItem(int missionNumber) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#A0A0A0") + "???");
        meta.setCustomModelData(2002);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.of("#D3D3D3") + "Misión no implementada");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private void addMissionSpecificProgress(Mission mission, String playerName, List<String> lore) {
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());

        if (mission instanceof Mission1) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de armadura:");

            String[] armorPieces = {"helmet", "chestplate", "leggings", "boots"};
            String[] armorNames = {"Casco", "Peto", "Pantalones", "Botas"};

            for (int i = 0; i < armorPieces.length; i++) {
                boolean hasArmor = data.getBoolean(
                        "players." + playerName + ".missions.1.armor." + armorPieces[i], false);
                lore.add((hasArmor ? ChatColor.of("#98FB98") : ChatColor.of("#D3D3D3")) + "- " + armorNames[i] + " de Diamante");
            }
        } else if (mission instanceof Mission2) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de encantamientos:");

            String[] armorPieces = {"helmet", "chestplate", "leggings", "boots"};
            String[] armorNames = {"Casco", "Peto", "Pantalones", "Botas"};

            for (int i = 0; i < armorPieces.length; i++) {
                boolean hasEnchant = data.getBoolean(
                        "players." + playerName + ".missions.2.protection." + armorPieces[i], false);
                lore.add((hasEnchant ? ChatColor.of("#98FB98") : ChatColor.of("#D3D3D3")) + "- " + armorNames[i] + " con Protección IV");
            }
        } else if (mission instanceof Mission5) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de armadura:");

            String[] armorPieces = {"helmet", "chestplate", "leggings", "boots"};
            String[] armorNames = {"Casco", "Peto", "Pantalones", "Botas"};

            for (int i = 0; i < armorPieces.length; i++) {
                boolean hasArmor = data.getBoolean("players." + playerName + ".missions.5.netherite_armor." + armorPieces[i], false);
                boolean hasProtection = data.getBoolean("players." + playerName + ".missions.5.protection." + armorPieces[i], false);

                if (hasArmor && hasProtection) {
                    lore.add(ChatColor.of("#98FB98") + "- " + armorNames[i] + " de Netherite con Protección IV");
                } else if (hasArmor) {
                    lore.add(ChatColor.of("#F0E68C") + "- " + armorNames[i] + " de Netherite (sin Protección IV)");
                } else {
                    lore.add(ChatColor.of("#D3D3D3") + "- " + armorNames[i] + " de Netherite con Protección IV");
                }
            }
        } else if (mission instanceof Mission6) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de eliminaciones:");

            int zombiesKilled = data.getInt("players." + playerName + ".missions.6.corrupted_zombies_killed", 0);
            int spidersKilled = data.getInt("players." + playerName + ".missions.6.corrupted_spiders_killed", 0);

            lore.add(ChatColor.of("#DDA0DD") + "- Corrupted Zombies: " + ChatColor.of("#98FB98") + zombiesKilled + ChatColor.of("#D3D3D3") + "/25");
            lore.add(ChatColor.of("#DDA0DD") + "- Corrupted Spiders: " + ChatColor.of("#98FB98") + spidersKilled + ChatColor.of("#D3D3D3") + "/25");
        } else if (mission instanceof Mission7) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de eliminaciones:");

            int skeletonsKilled = data.getInt("players." + playerName + ".missions.7.corrupted_skeletons_killed", 0);
            int creepersKilled = data.getInt("players." + playerName + ".missions.7.corrupted_creepers_killed", 0);

            lore.add(ChatColor.of("#DDA0DD") + "- Corrupted Skeletons: " + ChatColor.of("#98FB98") + skeletonsKilled + ChatColor.of("#D3D3D3") + "/30");
            lore.add(ChatColor.of("#DDA0DD") + "- Corrupted Creepers: " + ChatColor.of("#98FB98") + creepersKilled + ChatColor.of("#D3D3D3") + "/30");
        } else if (mission instanceof Mission8) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de armadura corrupta:");

            String[] armorPieces = {"helmet", "chestplate", "leggings", "boots"};
            String[] armorNames = {"Casco", "Peto", "Pantalones", "Botas"};

            for (int i = 0; i < armorPieces.length; i++) {
                boolean hasArmor = data.getBoolean(
                        "players." + playerName + ".missions.8.corrupted_armor." + armorPieces[i], false);
                lore.add((hasArmor ? ChatColor.of("#98FB98") : ChatColor.of("#D3D3D3")) + "- " + armorNames[i] + " Corrupto");
            }
        } else if (mission instanceof Mission9) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de raids:");

            int raidsCompleted = data.getInt("players." + playerName + ".missions.9.raids_completed", 0);
            lore.add(ChatColor.of("#DDA0DD") + "- Raids completadas: " + ChatColor.of("#98FB98") + raidsCompleted + ChatColor.of("#D3D3D3") + "/5");
        } else if (mission instanceof Mission10) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de totems:");

            boolean hasInfernal = data.getBoolean("players." + playerName + ".missions.10.totems.infernal", false);
            boolean hasSpider = data.getBoolean("players." + playerName + ".missions.10.totems.spider", false);
            boolean hasLife = data.getBoolean("players." + playerName + ".missions.10.totems.life", false);

            lore.add((hasInfernal ? ChatColor.of("#98FB98") : ChatColor.of("#D3D3D3")) + "- Infernal Totem");
            lore.add((hasSpider ? ChatColor.of("#98FB98") : ChatColor.of("#D3D3D3")) + "- Spider Totem");
            lore.add((hasLife ? ChatColor.of("#98FB98") : ChatColor.of("#D3D3D3")) + "- Life Totem");
        } else if (mission instanceof Mission11) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de tiempo:");

            long timeInMushroom = data.getLong("players." + playerName + ".missions.11.time_in_mushroom", 0);
            long requiredTime = 23500;

            lore.add(ChatColor.of("#DDA0DD") + "- Tiempo en Mushroom Island: " +
                    ChatColor.of("#98FB98") + timeInMushroom + ChatColor.of("#D3D3D3") + "/" + requiredTime);
        } else if (mission instanceof Mission12) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de eliminaciones:");

            int bombitasKilled = data.getInt("players." + playerName + ".missions.12.bombitas_killed", 0);
            int brutesKilled = data.getInt("players." + playerName + ".missions.12.brutes_killed", 0);

            lore.add(ChatColor.of("#DDA0DD") + "- Bombitas: " + ChatColor.of("#98FB98") + bombitasKilled + ChatColor.of("#D3D3D3") + "/30");
            lore.add(ChatColor.of("#DDA0DD") + "- Brutes Imperiales: " + ChatColor.of("#98FB98") + brutesKilled + ChatColor.of("#D3D3D3") + "/20");
        } else if (mission instanceof Mission3) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de preparación:");

            boolean raidCompleted = data.getBoolean("players." + playerName + ".missions.3.raid_completed", false);
            int goldenApplesCrafted = data.getInt("players." + playerName + ".missions.3.golden_apples_crafted", 0);

            lore.add((raidCompleted ? ChatColor.of("#98FB98") : ChatColor.of("#D3D3D3")) + "- Raid completada");
            lore.add(ChatColor.of("#DDA0DD") + "- Manzanas de oro: " + ChatColor.of("#98FB98") + goldenApplesCrafted + ChatColor.of("#D3D3D3") + "/20");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("\u3201\u3201" + org.bukkit.ChatColor.WHITE + "\u3202")) {
            event.setCancelled(true); // Cancelar cualquier interacción
        }
    }
}