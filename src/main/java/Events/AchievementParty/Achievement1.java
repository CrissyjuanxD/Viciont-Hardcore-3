package Events.AchievementParty;

import TitleListener.SuccessNotification;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class Achievement1 implements Achievement, Listener {
    private final AchievementPartyHandler eventHandler;
    private final SuccessNotification successNotification;
    private final Map<String, Long> ascentStartTimes = new HashMap<>();
    private final Map<String, Integer> ascentStartHeights = new HashMap<>();

    public Achievement1(JavaPlugin plugin, AchievementPartyHandler eventHandler) {
        this.eventHandler = eventHandler;
        this.successNotification = new SuccessNotification(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() { return "Yo sé que puedo volar"; }

    @Override
    public String getDescription() { return "Sube 300 bloques de altura en menos de 7 segundos."; }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!eventHandler.isEventActive()) return;

        Player player = event.getPlayer();

        if (eventHandler.getData(player, "fly_with_trident").isCompleted()) return;

        int currentY = event.getTo().getBlockY();
        if (currentY > event.getFrom().getBlockY()) {
            if (!ascentStartTimes.containsKey(player.getName())) {
                ascentStartTimes.put(player.getName(), System.currentTimeMillis());
                ascentStartHeights.put(player.getName(), currentY);
            }

            long startTime = ascentStartTimes.get(player.getName());
            int startHeight = ascentStartHeights.get(player.getName());
            int heightGained = currentY - startHeight;

            if (heightGained >= 300 && (System.currentTimeMillis() - startTime) <= 7000) {
                eventHandler.completeAchievement(player, "fly_with_trident");
                successNotification.showSuccess(player);
                ascentStartTimes.remove(player.getName());
                ascentStartHeights.remove(player.getName());
            } else if (System.currentTimeMillis() - startTime > 7000) {
                ascentStartTimes.remove(player.getName());
                ascentStartHeights.remove(player.getName());
            }
        }
    }
}