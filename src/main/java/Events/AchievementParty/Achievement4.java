package Events.AchievementParty;

import Events.MissionSystem.MissionData;
import TitleListener.SuccessNotification;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Achievement4 implements Achievement, Listener {
    private final AchievementPartyHandler eventHandler;
    private final SuccessNotification successNotification;
    private static final int DETECTION_RANGE = 20;

    public Achievement4(JavaPlugin plugin, AchievementPartyHandler eventHandler) {
        this.eventHandler = eventHandler;
        this.successNotification = new SuccessNotification(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() { return "Desarrollo Personal"; }

    @Override
    public String getDescription() { return "Haz que un Piglin se transforme en un Zombified Piglin"; }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onPiglinTransform(EntityTransformEvent event) {
        if (!eventHandler.isEventActive()) return;

        if (event.getEntityType() == EntityType.PIGLIN &&
                event.getTransformReason() == EntityTransformEvent.TransformReason.PIGLIN_ZOMBIFIED) {

            Piglin piglin = (Piglin) event.getEntity();
            Player closestEligiblePlayer = findClosestEligiblePlayer(piglin);

            if (closestEligiblePlayer != null) {
                awardAchievement(closestEligiblePlayer);
            }
        }
    }

    private Player findClosestEligiblePlayer(Piglin piglin) {
        Player closestPlayer = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity nearby : piglin.getNearbyEntities(DETECTION_RANGE, DETECTION_RANGE, DETECTION_RANGE)) {
            if (nearby instanceof Player player) {
                MissionData data = eventHandler.getData(player, "piglin_transformation");

                if (!data.isCompleted()) {
                    double distance = nearby.getLocation().distanceSquared(piglin.getLocation());
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestPlayer = player;
                    }
                }
            }
        }
        return closestPlayer;
    }

    private void awardAchievement(Player player) {
        if (eventHandler.completeAchievement(player, "piglin_transformation")) {
            successNotification.showSuccess(player);
        }
    }
}