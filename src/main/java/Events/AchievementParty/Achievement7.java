package Events.AchievementParty;

import Events.MissionSystem.MissionData;
import TitleListener.SuccessNotification;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Achievement7 implements Achievement, Listener {
    private final AchievementPartyHandler eventHandler;
    private final SuccessNotification successNotification;

    public Achievement7(JavaPlugin plugin, AchievementPartyHandler eventHandler) {
        this.eventHandler = eventHandler;
        this.successNotification = new SuccessNotification(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() { return "Alma de Piedra"; }

    @Override
    public String getDescription() { return "Lánzale una bola de nieve a un Warden"; }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!eventHandler.isEventActive()) return;

        Projectile projectile = event.getEntity();
        Entity hitEntity = event.getHitEntity();

        if (projectile instanceof Snowball && hitEntity instanceof Warden) {
            if (projectile.getShooter() instanceof Player player) {

                MissionData data = eventHandler.getData(player, "alma_piedra");
                if (!data.isCompleted()) {
                    if (eventHandler.completeAchievement(player, "alma_piedra")) {
                        successNotification.showSuccess(player);
                    }
                }
            }
        }
    }
}