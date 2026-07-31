package Events.MissionSystem;

import Handlers.ToastHandler;
import TitleListener.SuccessNotification;
import com.viciontmedia.api.ViciontMediaAPI;
import net.md_5.bungee.api.ChatColor;
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
        return "Protección Avanzada";
    }

    @Override
    public String getDescription() {
        return "Equípate armadura de diamante con Protección IV en cada pieza";
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
        if (!missionHandler.isMissionActive(player, 1)) return;
        MissionData data = missionHandler.getData(player, 1);
        if (data.isCompleted()) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkProtectionArmor(player);
        }, 1L);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!missionHandler.isMissionActive(player, 1)) return;
        MissionData data = missionHandler.getData(player, 1);
        if (data.isCompleted()) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkProtectionArmor(player);
        }, 1L);
    }

    private void checkProtectionArmor(Player player) {
        MissionData data = missionHandler.getData(player, 1);
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
        String equippedPieceName = "";

        // Usamos else if para procesar solo una pieza a la vez y capturar correctamente el nombre
        if (hasHelmet && !data.getProgressBool("protection_helmet")) {
            data.setProgressValue("protection_helmet", true);
            equippedPieceName = "Casco de diamante";
            successNotification.showSuccess(player);
            updated = true;
        } else if (hasChestplate && !data.getProgressBool("protection_chestplate")) {
            data.setProgressValue("protection_chestplate", true);
            equippedPieceName = "Peto de diamante";
            successNotification.showSuccess(player);
            updated = true;
        } else if (hasLeggings && !data.getProgressBool("protection_leggings")) {
            data.setProgressValue("protection_leggings", true);
            equippedPieceName = "Pantalones de diamante";
            successNotification.showSuccess(player);
            updated = true;
        } else if (hasBoots && !data.getProgressBool("protection_boots")) {
            data.setProgressValue("protection_boots", true);
            equippedPieceName = "Botas de diamante";
            successNotification.showSuccess(player);
            updated = true;
        }

        if (updated) {
            missionHandler.saveData(player, 1, data);

            int completed = 0;
            if (data.getProgressBool("protection_helmet")) completed++;
            if (data.getProgressBool("protection_chestplate")) completed++;
            if (data.getProgressBool("protection_leggings")) completed++;
            if (data.getProgressBool("protection_boots")) completed++;

            if (completed < 4) {
                // Formato de PROGRESO
                String progressText = "13%&#e453df&lMisión &r&#e453df#&l&#e453df1 &r\"&#588dc6&l&+&#68b9d4&lProtección Avanzada&#588dc6&l&-&r\" 0%&f&+\\uE000&-\n\n" +
                        "[left] 8%&#ad80dbTe equipaste&f: &#80d5db" + equippedPieceName + "\n" +
                        "[left] &#db80d8Progreso de Armadura&f: &#80db97" + completed + "&f/&#80db974";

                // size=1, anim="derecha", bg="#2b0047", duration=10, pos="topright", sync=false
                ViciontMediaAPI.sendText(player, 1, "derecha", "#2b0047", 12, "topright", false, progressText);
            } else {
                // Formato COMPLETADO
                String completedText = "13%&#9cee8b&lMisión &r&#9cee8b#&#9cee8b&l1 &+Completada&- 0%&f&+\\uE000&-\n\n" +
                        "[left] 10%&#9bb8fdHas completado la misión&f:\n" +
                        "[left] \"&+&#68b9d4&lProtección Avanzada&r&-\"\n\n" +
                        "[left] 8%&#ad80dbTe equipaste&f: &#80d5db" + equippedPieceName + "\n" +
                        "[left] &#db80d8Progreso de Armadura&f: &#80db97" + completed + "&f/&#80db974";

                // size=1, anim="derecha", bg="#005726", duration=10, pos="topright", sync=false
                ViciontMediaAPI.sendText(player, 1, "derecha", "#005726", 12, "topright", false, completedText);

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    missionHandler.completeMission(player.getName(), 1);
                }, 20L);
            }
        }
    }

    private boolean hasProtectionIV(ItemStack armor, Material expectedType) {
        if (armor == null || armor.getType() != expectedType) return false;
        return armor.getEnchantmentLevel(Enchantment.PROTECTION) >= 4;
    }
}