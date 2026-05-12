package Events.MissionSystem;

import TitleListener.SuccessNotification;
import com.viciontmedia.api.ViciontMediaAPI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import items.EconomyItems;

import java.util.ArrayList;
import java.util.List;

public class Mission3 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;

    public Mission3(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
    }

    @Override
    public String getName() {
        return "Preparación";
    }

    @Override
    public String getDescription() {
        return "Completa una Raid de Pillagers y craftea 20 manzanas de oro";
    }

    @Override
    public int getMissionNumber() {
        return 3;
    }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack vithiums = EconomyItems.createVithiumCoin();
        vithiums.setAmount(10);
        rewards.add(vithiums);
        rewards.add(new ItemStack(Material.GOLDEN_APPLE, 5));
        rewards.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));

        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onRaidFinish(RaidFinishEvent event) {
        if (event.getRaid().getStatus() != org.bukkit.Raid.RaidStatus.VICTORY) return;

        List<Player> participants = event.getWinners();

        for (Player player : participants) {
            if (!missionHandler.isMissionActive(player, 3)) continue;

            MissionData data = missionHandler.getData(player, 3);
            if (data.isCompleted() || data.getProgressBool("raid_completed")) continue;

            data.setProgressValue("raid_completed", true);
            missionHandler.saveData(player, 3, data);

            successNotification.showSuccess(player);
            sendMissionUI(player, data);
            checkMissionCompletion(player, data);
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!missionHandler.isMissionActive(player, 3)) return;

        MissionData data = missionHandler.getData(player, 3);
        if (data.isCompleted()) return;

        ItemStack recipeResult = event.getRecipe().getResult();
        if (recipeResult.getType() != Material.GOLDEN_APPLE) return;

        int craftedAmount = 0;

        if (event.isShiftClick()) {
            int maxCraftable = Integer.MAX_VALUE;
            for (ItemStack ingredient : event.getInventory().getMatrix()) {
                if (ingredient != null && ingredient.getType() != Material.AIR) {
                    maxCraftable = Math.min(maxCraftable, ingredient.getAmount());
                }
            }
            if (maxCraftable == Integer.MAX_VALUE) maxCraftable = 0;
            craftedAmount = maxCraftable * recipeResult.getAmount();
        } else {
            craftedAmount = recipeResult.getAmount();
        }

        if (craftedAmount <= 0) return;

        int currentCrafted = data.getProgressInt("golden_apples_crafted");

        if (currentCrafted < 20) {
            int newTotal = currentCrafted + craftedAmount;
            if (newTotal > 20) newTotal = 20;

            data.setProgressValue("golden_apples_crafted", newTotal);
            missionHandler.saveData(player, 3, data);

            if (newTotal >= 20) successNotification.showSuccess(player);

            sendMissionUI(player, data);
            checkMissionCompletion(player, data);
        }
    }

    private void sendMissionUI(Player player, MissionData data) {
        boolean raidCompleted = data.getProgressBool("raid_completed");
        int apples = data.getProgressInt("golden_apples_crafted");

        String raidProg = raidCompleted ? "1" : "0";
        String raidColor = raidCompleted ? "&#8BF8B7" : "&#CB5D5E";
        String raidTotalColor = raidCompleted ? "&#8BF8B7" : "&#8BF8B7";

        String appleColor = apples >= 20 ? "&#8BF8B7" : "&#CB5D5E";
        String appleTotalColor = apples >= 20 ? "&#8BF8B7" : "&#8BF8B7";

        String missionText = "&lMISION: &r\"&#9CF2FD&lPREPARACIÓN&r\"\n\n" +
                "&#ed92dbRaid Superada: " + raidColor + raidProg + "&7/" + raidTotalColor + "1\n" +
                "&#b45bdcManzanas Crafteadas: " + appleColor + apples + "&7/" + appleTotalColor + "20";

        ViciontMediaAPI.sendText(player, "48006c", 8, "topright", missionText);
    }

    private void checkMissionCompletion(Player player, MissionData data) {
        if (data.isCompleted()) return;

        boolean raidCompleted = data.getProgressBool("raid_completed");
        int goldenApplesCrafted = data.getProgressInt("golden_apples_crafted");

        if (raidCompleted && goldenApplesCrafted >= 20) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                missionHandler.completeMission(player.getName(), 3);
            }, 20L);
        }
    }
}