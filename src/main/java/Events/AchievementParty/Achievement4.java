package Events.AchievementParty;

import TitleListener.SuccessNotification;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Achievement4 implements Achievement, Listener {
    private final JavaPlugin plugin;
    private final AchievementPartyHandler eventHandler;
    private final SuccessNotification successNotification;
    private static final int DETECTION_RANGE = 20; // Radio de 20 bloques

    public Achievement4(JavaPlugin plugin, AchievementPartyHandler eventHandler) {
        this.plugin = plugin;
        this.eventHandler = eventHandler;
        this.successNotification = new SuccessNotification(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() {
        return "Desarrollo Personal";
    }

    @Override
    public String getDescription() {
        return "Haz que un Piglin se transforme en un Zombified Piglin";
    }

    @Override
    public void initializePlayerData(String playerName) {
        // No necesita inicialización especial
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    @EventHandler
    public void onPiglinTransform(EntityTransformEvent event) {
        if (!eventHandler.isEventActive()) return;

        // Verificar que la transformación sea de Piglin a Zombified Piglin
        if (event.getEntityType() == EntityType.PIGLIN &&
                event.getTransformReason() == EntityTransformEvent.TransformReason.PIGLIN_ZOMBIFIED) {

            Piglin piglin = (Piglin) event.getEntity();
            FileConfiguration data = YamlConfiguration.loadConfiguration(eventHandler.getAchievementsFile());

            Player closestEligiblePlayer = findClosestEligiblePlayer(piglin, data);

            if (closestEligiblePlayer != null) {
                awardAchievement(closestEligiblePlayer);
            }
        }
    }

    private Player findClosestEligiblePlayer(Piglin piglin, FileConfiguration data) {
        Player closestPlayer = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity nearby : piglin.getNearbyEntities(DETECTION_RANGE, DETECTION_RANGE, DETECTION_RANGE)) {
            if (nearby instanceof Player) {
                Player player = (Player) nearby;
                String playerName = player.getName();

                // Verificar si el jugador no ha completado el logro
                if (!data.getBoolean("players." + playerName + ".achievements.piglin_transformation.completed", false)) {
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
        if (eventHandler.completeAchievement(player.getName(), "piglin_transformation")) {
            successNotification.showSuccess(player);
        }
    }
}