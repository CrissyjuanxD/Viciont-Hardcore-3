package Events.AchievementParty;

import Events.MissionSystem.MissionData;
import TitleListener.SuccessNotification;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Achievement2 implements Achievement, Listener {
    private final AchievementPartyHandler eventHandler;
    private final SuccessNotification successNotification;
    private final Set<Material> requiredFlowers = new HashSet<>(Arrays.asList(
            Material.POPPY, Material.DANDELION, Material.BLUE_ORCHID, Material.ALLIUM,
            Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP,
            Material.WHITE_TULIP, Material.PINK_TULIP, Material.OXEYE_DAISY,
            Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY, Material.WITHER_ROSE,
            Material.SUNFLOWER, Material.LILAC, Material.ROSE_BUSH, Material.PEONY,
            Material.SPORE_BLOSSOM, Material.PINK_PETALS, Material.PITCHER_PLANT, Material.TORCHFLOWER
    ));

    public Achievement2(JavaPlugin plugin, AchievementPartyHandler eventHandler) {
        this.eventHandler = eventHandler;
        this.successNotification = new SuccessNotification(plugin);
        if (plugin != null) plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() { return "Stardew Valley"; }

    @Override
    public String getDescription() { return "Consigue todas las flores del juego"; }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    public Set<Material> getRequiredFlowers() { return requiredFlowers; }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!eventHandler.isEventActive()) return;

        MissionData data = eventHandler.getData(player, "collect_all_flowers");
        if (data.isCompleted()) return;

        ItemStack item = event.getItem().getItemStack();
        Material itemType = item.getType();

        if (requiredFlowers.contains(itemType)) {
            String key = "flower_" + itemType.name();

            if (!data.getProgressBool(key)) {
                data.setProgressValue(key, true);
                eventHandler.saveData(player, "collect_all_flowers", data);

                player.sendMessage("§eHaz recolectado la flor: " + formatMaterialName(itemType));
                successNotification.showSuccess(player);

                checkFlowerCompletion(player, data);
            }
        }
    }

    private void checkFlowerCompletion(Player player, MissionData data) {
        int collectedCount = 0;
        for (Material flower : requiredFlowers) {
            if (data.getProgressBool("flower_" + flower.name())) {
                collectedCount++;
            }
        }

        player.sendMessage("§eFlores recolectadas: §a" + collectedCount + "§e/§a" + requiredFlowers.size());

        if (collectedCount >= requiredFlowers.size()) {
            eventHandler.completeAchievement(player, "collect_all_flowers");
        }
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}