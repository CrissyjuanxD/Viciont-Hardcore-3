package Events.AchievementParty;

import Events.MissionSystem.MissionData;
import TitleListener.SuccessNotification;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Achievement6 implements Achievement, Listener {
    private final AchievementPartyHandler eventHandler;
    private final SuccessNotification successNotification;

    public Achievement6(JavaPlugin plugin, AchievementPartyHandler eventHandler) {
        this.eventHandler = eventHandler;
        this.successNotification = new SuccessNotification(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() { return "Con su propia medicina"; }

    @Override
    public String getDescription() { return "Mata a un Piglin Brute con un hacha de oro y al menos una pieza de armadura de oro"; }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!eventHandler.isEventActive()) return;

        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (entity.getType() == EntityType.PIGLIN_BRUTE && killer != null) {

            MissionData data = eventHandler.getData(killer, "payback");
            if (data.isCompleted()) return;

            ItemStack weapon = killer.getInventory().getItemInMainHand();
            if (weapon.getType() == Material.GOLDEN_AXE) {
                if (hasGoldenArmor(killer)) {
                    if (eventHandler.completeAchievement(killer, "payback")) {
                        successNotification.showSuccess(killer);
                        killer.sendMessage("§a¡Has vencido al Piglin Brute con su propio estilo!");
                    }
                }
            }
        }
    }

    private boolean hasGoldenArmor(Player player) {
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return false;

        ItemStack helmet = equipment.getHelmet();
        ItemStack chestplate = equipment.getChestplate();
        ItemStack leggings = equipment.getLeggings();
        ItemStack boots = equipment.getBoots();

        return (helmet != null && helmet.getType() == Material.GOLDEN_HELMET) ||
                (chestplate != null && chestplate.getType() == Material.GOLDEN_CHESTPLATE) ||
                (leggings != null && leggings.getType() == Material.GOLDEN_LEGGINGS) ||
                (boots != null && boots.getType() == Material.GOLDEN_BOOTS);
    }
}