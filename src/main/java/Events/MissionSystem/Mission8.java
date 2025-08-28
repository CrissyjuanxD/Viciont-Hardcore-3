package Events.MissionSystem;

import items.EconomyItems;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Mission8 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;

    public Mission8(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() {
        return "Forjador Corrupto";
    }

    @Override
    public String getDescription() {
        return "Fabrica armadura completa de Corrupted Netherite";
    }

    @Override
    public int getMissionNumber() {
        return 8;
    }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        // 10 Vithiums
        ItemStack vithiums = EconomyItems.createVithiumCoin();
        vithiums.setAmount(10);
        rewards.add(vithiums);

        // 5 Manzanas doradas
        rewards.add(new ItemStack(Material.GOLDEN_APPLE, 5));

        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());

        String[] armorPieces = {"helmet", "chestplate", "leggings", "boots"};
        for (String piece : armorPieces) {
            data.set("players." + playerName + ".missions.8.corrupted_armor." + piece, false);
        }

        try {
            data.save(missionHandler.getMissionFile());
        } catch (IOException e) {
            plugin.getLogger().severe("Error al inicializar datos de Misión 8: " + e.getMessage());
        }
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    @EventHandler
    public void onSmithingTableUse(SmithItemEvent event) {
        if (!missionHandler.isMissionActive(8)) return;

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String playerName = player.getName();

        // Verificar si ya completó la misión
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        if (data.getBoolean("players." + playerName + ".missions.8.completed", false)) {
            return;
        }

        ItemStack result = event.getCurrentItem();
        if (result == null || !result.hasItemMeta()) return;

        ItemMeta meta = result.getItemMeta();
        if (!meta.hasDisplayName()) return;

        String displayName = meta.getDisplayName();

        // Verificar si es una pieza de armadura corrupta
        String armorPiece = null;
        if (displayName.contains("Corrupted Netherite Helmet")) {
            armorPiece = "helmet";
        } else if (displayName.contains("Corrupted Netherite Chestplate")) {
            armorPiece = "chestplate";
        } else if (displayName.contains("Corrupted Netherite Leggings")) {
            armorPiece = "leggings";
        } else if (displayName.contains("Corrupted Netherite Boots")) {
            armorPiece = "boots";
        }

        if (armorPiece != null) {
            if (!data.getBoolean("players." + playerName + ".missions.8.corrupted_armor." + armorPiece, false)) {
                data.set("players." + playerName + ".missions.8.corrupted_armor." + armorPiece, true);

                try {
                    data.save(missionHandler.getMissionFile());

                    String armorName = armorPiece.substring(0, 1).toUpperCase() + armorPiece.substring(1);
                    player.sendMessage(ChatColor.of("#98FB98") + "¡Has fabricado " + armorName + " Corrupto!");

                    // Verificar si completó toda la armadura
                    boolean allCompleted = true;
                    String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
                    int completed = 0;

                    for (String piece : pieces) {
                        if (data.getBoolean("players." + playerName + ".missions.8.corrupted_armor." + piece, false)) {
                            completed++;
                        } else {
                            allCompleted = false;
                        }
                    }

                    if (allCompleted) {
                        missionHandler.completeMission(playerName, 8);
                    } else {
                        player.sendMessage(ChatColor.of("#F0E68C") + "Progreso de armadura corrupta: " +
                                ChatColor.of("#98FB98") + completed + ChatColor.of("#F0E68C") + "/4");
                    }
                } catch (IOException e) {
                    plugin.getLogger().severe("Error al guardar progreso de Misión 8: " + e.getMessage());
                }
            }
        }
    }
}