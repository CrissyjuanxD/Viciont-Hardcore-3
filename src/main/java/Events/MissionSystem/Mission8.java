package Events.MissionSystem;

import TitleListener.SuccessNotification;
import items.EconomyItems;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class Mission8 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;

    public Mission8(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
    }

    @Override
    public String getName() {
        return "Forjador Corrupto";
    }

    @Override
    public String getDescription() {
        return "Fabricar armadura completa de Corrupted Netherite";
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
        // No es necesario inicializar con JSON
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    @EventHandler
    public void onSmithingTableUse(SmithItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!missionHandler.isMissionActive(player, 8)) return;

        MissionData data = missionHandler.getData(player, 8);
        if (data.isCompleted()) return;

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
            String dataKey = "corrupted_armor_" + armorPiece; // Coincide con la GUI

            if (!data.getProgressBool(dataKey)) {
                data.setProgressValue(dataKey, true);
                missionHandler.saveData(player, 8, data);

                String armorName = armorPiece.substring(0, 1).toUpperCase() + armorPiece.substring(1);
                player.sendMessage(ChatColor.of("#98FB98") + "¡Has fabricado " + armorName + " Corrupto!");

                // Verificar si completó toda la armadura
                boolean allCompleted = true;
                String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
                int completed = 0;

                for (String piece : pieces) {
                    if (data.getProgressBool("corrupted_armor_" + piece)) {
                        completed++;
                    } else {
                        allCompleted = false;
                    }
                }

                if (allCompleted) {
                    missionHandler.completeMission(player.getName(), 8);
                } else {
                    player.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") + "Progreso de armadura corrupta: " + ChatColor.of("#FFB6C1") + completed + ChatColor.of("#87CEEB") + "/" + ChatColor.of("#98FB98") + "4");
                    successNotification.showSuccess(player);
                }
            }
        }
    }
}