package Events.AchievementParty;

import TitleListener.SuccessNotification;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Achievement3 implements Achievement, Listener {
    private final AchievementPartyHandler eventHandler;
    private final SuccessNotification successNotification;

    public Achievement3(JavaPlugin plugin, AchievementPartyHandler eventHandler) {
        this.eventHandler = eventHandler;
        this.successNotification = new SuccessNotification(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() { return "Second Chance"; }

    @Override
    public String getDescription() { return "Activa un tótem de los especiales"; }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onTotemActivate(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!eventHandler.isEventActive()) return;

        if (eventHandler.getData(player, "second_chance").isCompleted()) return;

        ItemStack totem = null;

        if (player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING) {
            totem = player.getInventory().getItemInMainHand();
        } else if (player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) {
            totem = player.getInventory().getItemInOffHand();
        }

        if (totem != null && totem.hasItemMeta()) {
            ItemMeta meta = totem.getItemMeta();
            if (meta.hasCustomModelData()) {
                int customModelData = meta.getCustomModelData();
                if (customModelData == 3 || customModelData == 4 || customModelData == 5) {
                    eventHandler.completeAchievement(player, "second_chance");
                    successNotification.showSuccess(player);
                }
            }
        }
    }
}