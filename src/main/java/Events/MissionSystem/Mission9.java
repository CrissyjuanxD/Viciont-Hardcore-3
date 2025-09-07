package Events.MissionSystem;

import TitleListener.SuccessNotification;
import items.EconomyItems;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Mission9 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;

    public Mission9(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
    }

    @Override
    public String getName() {
        return "Defensor Veterano";
    }

    @Override
    public String getDescription() {
        return "Completa 5 Raids de Pillagers";
    }

    @Override
    public int getMissionNumber() {
        return 9;
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
        data.set("players." + playerName + ".missions.9.raids_completed", 0);

        try {
            data.save(missionHandler.getMissionFile());
        } catch (IOException e) {
            plugin.getLogger().severe("Error al inicializar datos de Misión 9: " + e.getMessage());
        }
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    @EventHandler
    public void onRaidFinish(RaidFinishEvent event) {
        if (!missionHandler.isMissionActive(9)) return;

        // Verificar que la raid fue exitosa
        if (event.getRaid().getStatus() != org.bukkit.Raid.RaidStatus.VICTORY) {
            return;
        }

        List<Player> participants = event.getWinners();

        for (Player player : participants) {
            String playerName = player.getName();

            // Verificar si ya completó la misión
            FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
            if (data.getBoolean("players." + playerName + ".missions.9.completed", false)) {
                continue;
            }

            int raidsCompleted = data.getInt("players." + playerName + ".missions.9.raids_completed", 0);
            raidsCompleted++;
            data.set("players." + playerName + ".missions.9.raids_completed", raidsCompleted);

            try {
                data.save(missionHandler.getMissionFile());

                player.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") + "Raids completadas: " +
                        ChatColor.of("#FFB6C1") + raidsCompleted + ChatColor.of("#87CEEB") + "/" + ChatColor.of("#98FB98") + "5");

                if (raidsCompleted >= 5) {
                    successNotification.showSuccess(player);
                    missionHandler.completeMission(playerName, 9);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Error al guardar progreso de Misión 9: " + e.getMessage());
            }
        }
    }
}