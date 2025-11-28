package Events.MissionSystem;

import Handlers.ToastHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Zombie;
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

public class Mission6 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ToastHandler toastHandler;

    public Mission6(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.toastHandler = new ToastHandler(plugin);
    }

    @Override
    public String getName() {
        return "Exterminador de Corruptos";
    }

    @Override
    public String getDescription() {
        return "Mata 25 Corrupted Zombies y 25 Corrupted Spiders";
    }

    @Override
    public int getMissionNumber() {
        return 6;
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
        data.set("players." + playerName + ".missions.6.corrupted_zombies_killed", 0);
        data.set("players." + playerName + ".missions.6.corrupted_spiders_killed", 0);

        try {
            data.save(missionHandler.getMissionFile());
        } catch (IOException e) {
            plugin.getLogger().severe("Error al inicializar datos de Misión 6: " + e.getMessage());
        }
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!missionHandler.isMissionActive(6)) return;

        Entity entity = event.getEntity();
        Player killer = ((LivingEntity) entity).getKiller();

        if (killer == null) return;

        String playerName = killer.getName();

        // Verificar si ya completó la misión
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        if (data.getBoolean("players." + playerName + ".missions.6.completed", false)) {
            return;
        }

        boolean isCorruptedZombie = entity instanceof Zombie &&
                entity.getPersistentDataContainer().has(
                        new org.bukkit.NamespacedKey(plugin, "corrupted_zombie"),
                        PersistentDataType.BYTE);

        boolean isCorruptedSpider = entity instanceof Spider &&
                entity.getPersistentDataContainer().has(
                        new org.bukkit.NamespacedKey(plugin, "corruptedspider"),
                        PersistentDataType.BYTE);

        if (isCorruptedZombie) {
            int zombiesKilled = data.getInt("players." + playerName + ".missions.6.corrupted_zombies_killed", 0);
            zombiesKilled++;
            data.set("players." + playerName + ".missions.6.corrupted_zombies_killed", zombiesKilled);

           /* killer.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") + "Corrupted Zombies eliminados: " +
                    ChatColor.of("#FFB6C1") + zombiesKilled + ChatColor.of("#87CEEB") + "/" + ChatColor.of("#98FB98") + "25");*/
            toastHandler.sendToast(
                    killer,
                    ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") +
                            "Corrupted Zombies: " + ChatColor.GREEN +
                            zombiesKilled + ChatColor.of("#87CEEB") + "/" + ChatColor.GRAY + "25",
                    "Progreso de Misión: Exterminador de Corruptos Zombies",
                    "minecraft:netherite_sword"
            );

        } else if (isCorruptedSpider) {
            int spidersKilled = data.getInt("players." + playerName + ".missions.6.corrupted_spiders_killed", 0);
            spidersKilled++;
            data.set("players." + playerName + ".missions.6.corrupted_spiders_killed", spidersKilled);

            /*killer.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") + "Corrupted Spiders eliminadas: " +
                    ChatColor.of("#FFB6C1") + spidersKilled + ChatColor.of("#87CEEB") + "/" + ChatColor.of("#98FB98") + "25");*/
            toastHandler.sendToast(
                    killer,
                    ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") +
                            "Corrupted Spiders: " + ChatColor.GREEN +
                            spidersKilled + ChatColor.of("#87CEEB") + "/" + ChatColor.GRAY + "25",
                    "Progreso de Misión: Exterminador de Corruptos Spiders",
                    "minecraft:netherite_sword"
            );
        } else {
            return;
        }

        try {
            data.save(missionHandler.getMissionFile());

            // Verificar si completó ambos objetivos
            int zombiesKilled = data.getInt("players." + playerName + ".missions.6.corrupted_zombies_killed", 0);
            int spidersKilled = data.getInt("players." + playerName + ".missions.6.corrupted_spiders_killed", 0);

            if (zombiesKilled >= 25 && spidersKilled >= 25) {
                successNotification.showSuccess(killer);
                missionHandler.completeMission(playerName, 6);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar progreso de Misión 6: " + e.getMessage());
        }
    }
}