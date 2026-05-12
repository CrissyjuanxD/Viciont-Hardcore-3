package Events.MissionSystem;

import Handlers.ToastHandler;
import TitleListener.SuccessNotification;
import com.viciontmedia.api.ViciontMediaAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import items.EconomyItems;

import java.util.ArrayList;
import java.util.List;

public class Mission1 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ToastHandler toastHandler;

    public Mission1(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.toastHandler = new ToastHandler(plugin);
    }

    @Override
    public String getName() {
        return "Armadura Básica";
    }

    @Override
    public String getDescription() {
        return "Equípate cada pieza de armadura de diamante";
    }

    @Override
    public int getMissionNumber() {
        return 1;
    }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack vithiums = EconomyItems.createVithiumCoin();
        vithiums.setAmount(10);
        rewards.add(vithiums);
        rewards.add(new ItemStack(Material.GOLDEN_APPLE, 5));
        rewards.add(new ItemStack(Material.TOTEM_OF_UNDYING, 1));

        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!missionHandler.isMissionActive(player, 1)) return;
        MissionData data = missionHandler.getData(player, 1);
        if (data.isCompleted()) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkArmorEquipped(player);
        }, 1L);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!missionHandler.isMissionActive(player, 1)) return;
        MissionData data = missionHandler.getData(player, 1);
        if (data.isCompleted()) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkArmorEquipped(player);
        }, 1L);
    }

    private void checkArmorEquipped(Player player) {
        MissionData data = missionHandler.getData(player, 1);
        if (data.isCompleted()) return;

        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        boolean hasHelmet = helmet != null && helmet.getType() == Material.DIAMOND_HELMET;
        boolean hasChestplate = chestplate != null && chestplate.getType() == Material.DIAMOND_CHESTPLATE;
        boolean hasLeggings = leggings != null && leggings.getType() == Material.DIAMOND_LEGGINGS;
        boolean hasBoots = boots != null && boots.getType() == Material.DIAMOND_BOOTS;

        boolean updated = false;

        if (hasHelmet && !data.getProgressBool("armor_helmet")) {
            data.setProgressValue("armor_helmet", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (hasChestplate && !data.getProgressBool("armor_chestplate")) {
            data.setProgressValue("armor_chestplate", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (hasLeggings && !data.getProgressBool("armor_leggings")) {
            data.setProgressValue("armor_leggings", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (hasBoots && !data.getProgressBool("armor_boots")) {
            data.setProgressValue("armor_boots", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (updated) {
            missionHandler.saveData(player, 1, data);

            int completed = 0;
            if (data.getProgressBool("armor_helmet")) completed++;
            if (data.getProgressBool("armor_chestplate")) completed++;
            if (data.getProgressBool("armor_leggings")) completed++;
            if (data.getProgressBool("armor_boots")) completed++;

            String progressColor = completed == 4 ? "&#8BF8B7" : "&#CB5D5E";
            String totalColor = completed == 4 ? "&#8BF8B7" : "&#8BF8B7";

            String missionText = "&lMISION: &r\"&#9CF2FD&lARMADURA BÁSICA&r\"\n\n" +
                    "&#ed92dbProgreso de Armadura: " + progressColor + completed + "&7/" + totalColor + "4";

            ViciontMediaAPI.sendText(player, "48006c", 8, "topright", missionText);

            if (completed == 4) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    missionHandler.completeMission(player.getName(), 1);
                }, 20L);
            }
        }
    }
}