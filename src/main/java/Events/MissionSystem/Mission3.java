package Events.MissionSystem;

import TitleListener.SuccessNotification;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Mission3 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private SuccessNotification successNotification;

    public Mission3(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() {
        return "Defensor del Pueblo";
    }

    @Override
    public String getDescription() {
        return "Completa una Raid de Pillagers";
    }

    @Override
    public int getMissionNumber() {
        return 3;
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

        // 1 Notch Apple
        rewards.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));

        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        data.set("players." + playerName + ".missions.3.raid_completed", false);

        try {
            data.save(missionHandler.getMissionFile());
        } catch (IOException e) {
            plugin.getLogger().severe("Error al inicializar datos de Misión 3: " + e.getMessage());
        }
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    @EventHandler
    public void onRaidFinish(RaidFinishEvent event) {
        if (!missionHandler.isMissionActive(3)) return;

        // Verificar que la raid fue exitosa (no fue derrotada)
        if (event.getRaid().getStatus() != org.bukkit.Raid.RaidStatus.VICTORY) {
            return;
        }

        // Encontrar jugadores que participaron en la raid
        List<Player> participants = event.getWinners();

        for (Player player : participants) {
            String playerName = player.getName();

            // Verificar si ya completó la misión
            FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
            if (data.getBoolean("players." + playerName + ".missions.3.completed", false)) {
                continue;
            }

            // Marcar como completada
            data.set("players." + playerName + ".missions.3.raid_completed", true);

            try {
                data.save(missionHandler.getMissionFile());
                player.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") + "¡Has completado una Raid!");
                successNotification.showSuccess(player);
                missionHandler.completeMission(playerName, 3);
            } catch (IOException e) {
                plugin.getLogger().severe("Error al guardar progreso de Misión 3: " + e.getMessage());
            }
        }
    }
}