package Events.AchievementParty;

import Events.MissionSystem.MissionData;
import TitleListener.SuccessNotification;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Achievement8 implements Achievement, Listener {
    private final AchievementPartyHandler eventHandler;
    private final SuccessNotification successNotification;
    public static final int REQUIRED_SCULK_SHRIEKERS = 15;

    public Achievement8(JavaPlugin plugin, AchievementPartyHandler eventHandler) {
        this.eventHandler = eventHandler;
        this.successNotification = new SuccessNotification(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() { return "Jugando a ser músico"; }

    @Override
    public String getDescription() { return "Rompe 15 chilladores en el bioma de Deep Dark"; }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!eventHandler.isEventActive()) return;

        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType() != Material.SCULK_SHRIEKER ||
                !block.getBiome().name().equalsIgnoreCase("DEEP_DARK")) {
            return;
        }

        MissionData data = eventHandler.getData(player, "sculk_shrieker");

        if (data.isCompleted()) return;

        int broken = data.getProgressInt("broken") + 1;
        data.setProgressValue("broken", broken);

        eventHandler.saveData(player, "sculk_shrieker", data);

        player.sendMessage("§eChilladores rotos: §a" + broken + "§e/§a" + REQUIRED_SCULK_SHRIEKERS);
        successNotification.showSuccess(player);

        block.setType(Material.AIR);

        if (broken >= REQUIRED_SCULK_SHRIEKERS) {
            eventHandler.completeAchievement(player, "sculk_shrieker");
        }
    }
}