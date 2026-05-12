package Events.MissionSystem;

import TitleListener.SuccessNotification;
import com.viciontmedia.api.ViciontMediaAPI;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;

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
        return "Protección Avanzada";
    }

    @Override
    public String getDescription() {
        return "Equípate armadura de diamante con Protección IV en cada pieza";
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
        rewards.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
        rewards.add(new ItemStack(Material.GOLDEN_APPLE, 5));

        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!missionHandler.isMissionActive(player, 2)) return;
        MissionData data = missionHandler.getData(player, 2);
        if (data.isCompleted()) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkProtectionArmor(player);
        }, 1L);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!missionHandler.isMissionActive(player, 2)) return;
        MissionData data = missionHandler.getData(player, 2);
        if (data.isCompleted()) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkProtectionArmor(player);
        }, 1L);
    }

    private void checkProtectionArmor(Player player) {
        MissionData data = missionHandler.getData(player, 2);
        if (data.isCompleted()) return;

        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        boolean hasHelmet = hasProtectionIV(helmet, Material.DIAMOND_HELMET);
        boolean hasChestplate = hasProtectionIV(chestplate, Material.DIAMOND_CHESTPLATE);
        boolean hasLeggings = hasProtectionIV(leggings, Material.DIAMOND_LEGGINGS);
        boolean hasBoots = hasProtectionIV(boots, Material.DIAMOND_BOOTS);

        boolean updated = false;

        if (hasHelmet && !data.getProgressBool("protection_helmet")) {
            data.setProgressValue("protection_helmet", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (hasChestplate && !data.getProgressBool("protection_chestplate")) {
            data.setProgressValue("protection_chestplate", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (hasLeggings && !data.getProgressBool("protection_leggings")) {
            data.setProgressValue("protection_leggings", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (hasBoots && !data.getProgressBool("protection_boots")) {
            data.setProgressValue("protection_boots", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (updated) {
            missionHandler.saveData(player, 2, data);

            int completed = 0;
            if (data.getProgressBool("protection_helmet")) completed++;
            if (data.getProgressBool("protection_chestplate")) completed++;
            if (data.getProgressBool("protection_leggings")) completed++;
            if (data.getProgressBool("protection_boots")) completed++;

            String progressColor = completed == 4 ? "&#8BF8B7" : "&#CB5D5E";
            String totalColor = completed == 4 ? "&#8BF8B7" : "&#8BF8B7";

            String missionText = "&lMISION: &r\"&#9CF2FD&lPROTECCIÓN AVANZADA&r\"\n\n" +
                    "&#ed92dbProgreso de Armadura: " + progressColor + completed + "&7/" + totalColor + "4";

            ViciontMediaAPI.sendText(player, "48006c", 8, "topright", missionText);

            if (completed == 4) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    missionHandler.completeMission(player.getName(), 2);
                }, 20L);
            }
        }
    }

    private boolean hasProtectionIV(ItemStack armor, Material expectedType) {
        if (armor == null || armor.getType() != expectedType) return false;
        return armor.getEnchantmentLevel(Enchantment.PROTECTION) >= 4;
    }
}