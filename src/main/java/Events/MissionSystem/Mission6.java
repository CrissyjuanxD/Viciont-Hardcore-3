package Events.MissionSystem;

import Handlers.ToastHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import org.bukkit.Material;
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
        // No es necesario inicializar con JSON
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        Player killer = ((LivingEntity) entity).getKiller();

        if (killer == null) return;
        if (!missionHandler.isMissionActive(killer, 6)) return;

        MissionData data = missionHandler.getData(killer, 6);
        if (data.isCompleted()) return;

        boolean isCorruptedZombie = entity instanceof Zombie &&
                entity.getPersistentDataContainer().has(
                        new org.bukkit.NamespacedKey(plugin, "corrupted_zombie"),
                        PersistentDataType.BYTE);

        boolean isCorruptedSpider = entity instanceof Spider &&
                entity.getPersistentDataContainer().has(
                        new org.bukkit.NamespacedKey(plugin, "corruptedspider"),
                        PersistentDataType.BYTE);

        if (isCorruptedZombie) {
            int zombiesKilled = data.getProgressInt("corrupted_zombies_killed");
            if (zombiesKilled < 25) {
                zombiesKilled++;
                data.setProgressValue("corrupted_zombies_killed", zombiesKilled);

                toastHandler.sendToast(
                        killer,
                        ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") +
                                "Corrupted Zombies: " + ChatColor.GREEN +
                                zombiesKilled + ChatColor.of("#87CEEB") + "/" + ChatColor.GRAY + "25",
                        "Progreso de Misión: Exterminador de Corruptos Zombies",
                        "minecraft:netherite_sword"
                );
            }

        } else if (isCorruptedSpider) {
            int spidersKilled = data.getProgressInt("corrupted_spiders_killed");
            if (spidersKilled < 25) {
                spidersKilled++;
                data.setProgressValue("corrupted_spiders_killed", spidersKilled);

                toastHandler.sendToast(
                        killer,
                        ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") +
                                "Corrupted Spiders: " + ChatColor.GREEN +
                                spidersKilled + ChatColor.of("#87CEEB") + "/" + ChatColor.GRAY + "25",
                        "Progreso de Misión: Exterminador de Corruptos Spiders",
                        "minecraft:netherite_sword"
                );
            }
        } else {
            return;
        }

        missionHandler.saveData(killer, 6, data);

        // Verificar si completó ambos objetivos
        if (data.getProgressInt("corrupted_zombies_killed") >= 25 &&
                data.getProgressInt("corrupted_spiders_killed") >= 25) {

            successNotification.showSuccess(killer);
            missionHandler.completeMission(killer.getName(), 6);
        }
    }
}