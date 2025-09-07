package Events.MissionSystem;

import TitleListener.SuccessNotification;
import items.EconomyItems;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Mission10 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;

    public Mission10(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
    }

    @Override
    public String getName() {
        return "Maestro de Totems";
    }

    @Override
    public String getDescription() {
        return "Obtén todos los totems custom (Infernal, Spider y Life Totem)";
    }

    @Override
    public int getMissionNumber() {
        return 10;
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
        data.set("players." + playerName + ".missions.10.totems.infernal", false);
        data.set("players." + playerName + ".missions.10.totems.spider", false);
        data.set("players." + playerName + ".missions.10.totems.life", false);

        try {
            data.save(missionHandler.getMissionFile());
        } catch (IOException e) {
            plugin.getLogger().severe("Error al inicializar datos de Misión 10: " + e.getMessage());
        }
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!missionHandler.isMissionActive(10)) return;

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String playerName = player.getName();

        // Verificar si ya completó la misión
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        if (data.getBoolean("players." + playerName + ".missions.10.completed", false)) {
            return;
        }

        ItemStack result = event.getCurrentItem();
        if (result == null || !result.hasItemMeta()) return;

        ItemMeta meta = result.getItemMeta();
        if (!meta.hasDisplayName() || !meta.hasCustomModelData()) return;

        String displayName = meta.getDisplayName();
        int customModelData = meta.getCustomModelData();

        String totemType = null;
        if (displayName.contains("Infernal Totem") && customModelData == 5) {
            totemType = "infernal";
        } else if (displayName.contains("Spider Totem") && customModelData == 4) {
            totemType = "spider";
        } else if (displayName.contains("Life Totem") && customModelData == 3) {
            totemType = "life";
        }

        if (totemType != null) {
            if (!data.getBoolean("players." + playerName + ".missions.10.totems." + totemType, false)) {
                data.set("players." + playerName + ".missions.10.totems." + totemType, true);

                try {
                    data.save(missionHandler.getMissionFile());

                    String totemName = totemType.substring(0, 1).toUpperCase() + totemType.substring(1);

                    // Verificar si completó todos los totems
                    boolean hasInfernal = data.getBoolean("players." + playerName + ".missions.10.totems.infernal", false);
                    boolean hasSpider = data.getBoolean("players." + playerName + ".missions.10.totems.spider", false);
                    boolean hasLife = data.getBoolean("players." + playerName + ".missions.10.totems.life", false);

                    if (hasInfernal && hasSpider && hasLife) {
                        successNotification.showSuccess(player);
                        missionHandler.completeMission(playerName, 10);
                    } else {
                        int completed = 0;
                        if (hasInfernal) completed++;
                        if (hasSpider) completed++;
                        if (hasLife) completed++;

                        player.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") + "Progreso de totems: " +
                                ChatColor.of("#FFB6C1") + completed + ChatColor.of("#87CEEB") + "/" + ChatColor.of("#98FB98") + "3 " + ChatColor.of("#98FB98") + totemName);
                    }
                } catch (IOException e) {
                    plugin.getLogger().severe("Error al guardar progreso de Misión 10: " + e.getMessage());
                }
            }
        }
    }
}