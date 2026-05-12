package Events.MissionSystem;

import TitleListener.SuccessNotification;
import items.DoubleLifeTotem;
import items.EconomyItems;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;
import vct.hardcore3.ViciontHardcore3;

import java.util.ArrayList;
import java.util.List;

public class Mission5 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final DoubleLifeTotem doubleLifeTotem;
    private final SuccessNotification successNotification;


    public Mission5(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.doubleLifeTotem = ((ViciontHardcore3) plugin).getDoubleLifeTotemHandler();
        this.successNotification = new SuccessNotification(plugin);
    }

    @Override
    public String getName() {
        return "Armadura Suprema";
    }

    @Override
    public String getDescription() {
        return "Equípate cada pieza de armadura de netherite con Protección IV";
    }

    @Override
    public int getMissionNumber() {
        return 5;
    }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        // 10 Vithiums
        ItemStack vithiums = EconomyItems.createVithiumCoin();
        vithiums.setAmount(10);
        rewards.add(vithiums);

        // 1 Notch Apple
        rewards.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));

        // 5 Manzanas doradas
        rewards.add(new ItemStack(Material.GOLDEN_APPLE, 5));

        // 1 Double Totem
        rewards.add(doubleLifeTotem.createDoubleLifeTotem());

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
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!missionHandler.isMissionActive(player, 5)) return;

        MissionData data = missionHandler.getData(player, 5);
        if (data.isCompleted()) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkNetheriteArmorWithProtection(player);
        }, 1L);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!missionHandler.isMissionActive(player, 5)) return;

        MissionData data = missionHandler.getData(player, 5);
        if (data.isCompleted()) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkNetheriteArmorWithProtection(player);
        }, 1L);
    }

    private void checkNetheriteArmorWithProtection(Player player) {
        MissionData data = missionHandler.getData(player, 5);
        if (data.isCompleted()) return;

        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        boolean hasHelmet = hasNetheriteWithProtectionIV(helmet, Material.NETHERITE_HELMET);
        boolean hasChestplate = hasNetheriteWithProtectionIV(chestplate, Material.NETHERITE_CHESTPLATE);
        boolean hasLeggings = hasNetheriteWithProtectionIV(leggings, Material.NETHERITE_LEGGINGS);
        boolean hasBoots = hasNetheriteWithProtectionIV(boots, Material.NETHERITE_BOOTS);

        boolean updated = false;

        // Utilizamos netherite_armor_X y protection_X para que coincida con lo que lee la GUI
        if (hasHelmet && !data.getProgressBool("netherite_armor_helmet")) {
            data.setProgressValue("netherite_armor_helmet", true);
            data.setProgressValue("protection_helmet", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (hasChestplate && !data.getProgressBool("netherite_armor_chestplate")) {
            data.setProgressValue("netherite_armor_chestplate", true);
            data.setProgressValue("protection_chestplate", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (hasLeggings && !data.getProgressBool("netherite_armor_leggings")) {
            data.setProgressValue("netherite_armor_leggings", true);
            data.setProgressValue("protection_leggings", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (hasBoots && !data.getProgressBool("netherite_armor_boots")) {
            data.setProgressValue("netherite_armor_boots", true);
            data.setProgressValue("protection_boots", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (updated) {
            missionHandler.saveData(player, 5, data);

            if (data.getProgressBool("netherite_armor_helmet") &&
                    data.getProgressBool("netherite_armor_chestplate") &&
                    data.getProgressBool("netherite_armor_leggings") &&
                    data.getProgressBool("netherite_armor_boots")) {
                missionHandler.completeMission(player.getName(), 5);
            } else {
                int completed = 0;
                if (data.getProgressBool("netherite_armor_helmet")) completed++;
                if (data.getProgressBool("netherite_armor_chestplate")) completed++;
                if (data.getProgressBool("netherite_armor_leggings")) completed++;
                if (data.getProgressBool("netherite_armor_boots")) completed++;

                player.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") + "Progreso de armadura de netherite: " + ChatColor.of("#FFB6C1") + completed + ChatColor.of("#87CEEB") + "/" + ChatColor.of("#98FB98") + "4");
            }
        }
    }

    private boolean hasNetheriteWithProtectionIV(ItemStack armor, Material expectedType) {
        if (armor == null || armor.getType() != expectedType) {
            return false;
        }

        return armor.getEnchantmentLevel(Enchantment.PROTECTION) >= 4;
    }
}