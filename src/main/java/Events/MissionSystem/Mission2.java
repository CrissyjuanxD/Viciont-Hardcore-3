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

public class Mission2 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;

    public Mission2(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
    }

    @Override
    public String getName() {
        return "El Héroe Dorado";
    }

    @Override
    public String getDescription() {
        return "Completa una Raid y craftea 40 manzanas de oro";
    }

    @Override
    public int getMissionNumber() {
        return 2;
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
            if (!missionHandler.isMissionActive(player, 2)) continue;

            MissionData data = missionHandler.getData(player, 2);
            if (data.isCompleted() || data.getProgressBool("raid_completed")) continue;

            data.setProgressValue("raid_completed", true);
            missionHandler.saveData(player, 2, data);

            successNotification.showSuccess(player);

            // Evaluamos si con esto ya completó toda la misión
            checkAndSendFeedback(player, data, "raid");
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!missionHandler.isMissionActive(player, 2)) return;

        MissionData data = missionHandler.getData(player, 2);
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

        if (currentCrafted < 40) {
            int newTotal = currentCrafted + craftedAmount;
            if (newTotal > 40) newTotal = 40;

            data.setProgressValue("golden_apples_crafted", newTotal);
            missionHandler.saveData(player, 2, data);

            if (newTotal >= 40) successNotification.showSuccess(player);

            // Evaluamos progreso y enviamos texto
            checkAndSendFeedback(player, data, "apple");
        }
    }

    private void checkAndSendFeedback(Player player, MissionData data, String triggerType) {
        boolean raidCompleted = data.getProgressBool("raid_completed");
        int apples = data.getProgressInt("golden_apples_crafted");

        if (raidCompleted && apples >= 40) {
            // FORMATO: MISIÓN COMPLETADA (Verde)
            String compText = "13%&#9cee8b&lMisión &r&#9cee8b#&l2 &l&+Completada&- 0%&r&+\\uE001 0%&f&-\n\n" +
                    "[left] 10%&#9bb8fdHas completado la misión&f:\n" +
                    "[left] \"&+&#ffd270&lEl Héroe Dorado&r&-\"\n\n" +
                    "[left] 8%&#ad80dbProgreso de Raid&f: &#80d5dbCompletada\n" +
                    "[left] &#db80d8Progreso de Manzanas&f: &#80db9740&f/&#80db9740";

            ViciontMediaAPI.sendText(player, 1, "derecha", "#005726", 12, "topright", false, compText);

            // Completamos la misión con retraso para que guarde bien
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                missionHandler.completeMission(player.getName(), 2);
            }, 20L);

        } else {
            // FORMATO: PROGRESO (Morado)
            if (triggerType.equals("raid")) {
                String raidProgText = "13%&#e453df&lMisión &r&#e453df#&l2 &r\"&#ffd270&lEl Héroe Dorado&r\" 0%&f&+\\uE001&-\n\n" +
                        "[left] 8%&#ad80dbProgreso de Raid&f: &#80d5dbCompletada";
                ViciontMediaAPI.sendText(player, 1, "derecha", "#2b0047", 12, "topright", false, raidProgText);
            }
            else if (triggerType.equals("apple")) {
                String appleProgText = "13%&#e453df&lMisión &r&#e453df#&l2 &r\"&#ffd270&lEl Héroe Dorado&r\" 0%&f&+\\uE001&-\n\n" +
                        "[left] 8%&#db80d8Progreso de Manzanas&f: &#80db97" + apples + "&f/&#80db9740";
                ViciontMediaAPI.sendText(player, 1, "derecha", "#2b0047", 12, "topright", false, appleProgText);
            }
        }
    }
}