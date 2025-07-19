package Events.AchievementParty;

import TitleListener.SuccessNotification;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Achievement7 implements Achievement, Listener {
    private final JavaPlugin plugin;
    private final AchievementPartyHandler eventHandler;
    private final SuccessNotification successNotification;

    public Achievement7(JavaPlugin plugin, AchievementPartyHandler eventHandler) {
        this.plugin = plugin;
        this.eventHandler = eventHandler;
        this.successNotification = new SuccessNotification(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() {
        return "Alma de Piedra";
    }

    @Override
    public String getDescription() {
        return "Lánzale una bola de nieve a un Warden";
    }

    @Override
    public void initializePlayerData(String playerName) {

    }

    @Override
    public void checkCompletion(String playerName) {

    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!eventHandler.isEventActive()) return;

        Projectile projectile = event.getEntity();
        Entity hitEntity = event.getHitEntity();

        if (projectile instanceof Snowball && hitEntity instanceof Warden) {
            if (projectile.getShooter() instanceof Player) {
                Player player = (Player) projectile.getShooter();

                // Verifica primero si ya completó el logro
                FileConfiguration data = YamlConfiguration.loadConfiguration(eventHandler.getAchievementsFile());
                if (!data.getBoolean("players." + player.getName() + ".achievements.alma_piedra.completed", false)) {
                    if (eventHandler.completeAchievement(player.getName(), "alma_piedra")) {
                        successNotification.showSuccess(player);
                    }
                }
            }
        }
    }
}