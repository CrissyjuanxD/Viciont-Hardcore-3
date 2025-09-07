package Events.MissionSystem;

import TitleListener.SuccessNotification;
import items.EconomyItems;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.PiglinBrute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.LivingEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Mission12 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;

    public Mission12(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
    }

    @Override
    public String getName() {
        return "Demoledor Explosivo";
    }

    @Override
    public String getDescription() {
        return "Mata 30 Bombitas y 20 Brutes Imperiales";
    }

    @Override
    public int getMissionNumber() {
        return 12;
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
        data.set("players." + playerName + ".missions.12.bombitas_killed", 0);
        data.set("players." + playerName + ".missions.12.brutes_killed", 0);

        try {
            data.save(missionHandler.getMissionFile());
        } catch (IOException e) {
            plugin.getLogger().severe("Error al inicializar datos de Misión 12: " + e.getMessage());
        }
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!missionHandler.isMissionActive(12)) return;

        Entity entity = event.getEntity();
        Player killer = ((LivingEntity) entity).getKiller();

        if (killer == null) return;

        String playerName = killer.getName();

        // Verificar si ya completó la misión
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        if (data.getBoolean("players." + playerName + ".missions.12.completed", false)) {
            return;
        }

        boolean isBombita = entity instanceof Creeper &&
                entity.getPersistentDataContainer().has(
                        new org.bukkit.NamespacedKey(plugin, "bombita"),
                        PersistentDataType.BYTE);

        boolean isBruteImperial = entity instanceof PiglinBrute &&
                entity.getPersistentDataContainer().has(
                        new org.bukkit.NamespacedKey(plugin, "brute_imperial"),
                        PersistentDataType.BYTE);

        if (isBombita) {
            int bombitasKilled = data.getInt("players." + playerName + ".missions.12.bombitas_killed", 0);
            bombitasKilled++;
            data.set("players." + playerName + ".missions.12.bombitas_killed", bombitasKilled);

            killer.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") + "Bombitas eliminadas: " +
                    ChatColor.of("#FFB6C1") + bombitasKilled + ChatColor.of("#87CEEB") + "/" + ChatColor.of("#98FB98") + "30");
        } else if (isBruteImperial) {
            int brutesKilled = data.getInt("players." + playerName + ".missions.12.brutes_killed", 0);
            brutesKilled++;
            data.set("players." + playerName + ".missions.12.brutes_killed", brutesKilled);

            killer.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") + "Brutes Imperiales eliminados: " +
                    ChatColor.of("#FFB6C1") + brutesKilled + ChatColor.of("#87CEEB") + "/" + ChatColor.of("#98FB98") + "20");
        } else {
            return;
        }

        try {
            data.save(missionHandler.getMissionFile());

            // Verificar si completó ambos objetivos
            int bombitasKilled = data.getInt("players." + playerName + ".missions.12.bombitas_killed", 0);
            int brutesKilled = data.getInt("players." + playerName + ".missions.12.brutes_killed", 0);

            if (bombitasKilled >= 30 && brutesKilled >= 20) {
                successNotification.showSuccess(killer);
                missionHandler.completeMission(playerName, 12);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar progreso de Misión 12: " + e.getMessage());
        }
    }
}