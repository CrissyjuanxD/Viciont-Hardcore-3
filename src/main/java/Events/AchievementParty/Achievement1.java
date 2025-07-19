package Events.AchievementParty;

import TitleListener.SuccessNotification;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class Achievement1 implements Achievement, Listener {
    private final JavaPlugin plugin;
    private final AchievementPartyHandler eventHandler;
    private final SuccessNotification successNotification;
    private final Map<String, Long> ascentStartTimes = new HashMap<>();
    private final Map<String, Integer> ascentStartHeights = new HashMap<>();

    public Achievement1(JavaPlugin plugin, AchievementPartyHandler eventHandler) {
        this.plugin = plugin;
        this.eventHandler = eventHandler;
        this.successNotification = new SuccessNotification(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() {
        return "Yo sé que puedo volar";
    }

    @Override
    public String getDescription() {
        return "Sube 300 bloques de altura en menos de 7 segundos.";
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
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!eventHandler.isEventActive()) return;

        Player player = event.getPlayer();

        // Verificar primero si ya completó el logro
        FileConfiguration data = YamlConfiguration.loadConfiguration(eventHandler.getAchievementsFile());
        if (data.getBoolean("players." + player.getName() + ".achievements.fly_with_trident.completed", false)) {
            return;
        }

        int currentY = event.getTo().getBlockY();

        // Verificar si el jugador está subiendo
        if (currentY > event.getFrom().getBlockY()) {
            // Si no hay registro previo, iniciar el seguimiento
            if (!ascentStartTimes.containsKey(player.getName())) {
                ascentStartTimes.put(player.getName(), System.currentTimeMillis());
                ascentStartHeights.put(player.getName(), currentY);
            }

            // Obtener datos del ascenso
            long startTime = ascentStartTimes.get(player.getName());
            int startHeight = ascentStartHeights.get(player.getName());
            int heightGained = currentY - startHeight;

            // Verificar si alcanzó los 400 bloques en menos de 10 segundos
            if (heightGained >= 300 && (System.currentTimeMillis() - startTime) <= 7000) {
                eventHandler.completeAchievement(player.getName(), "fly_with_trident");
                successNotification.showSuccess(player);
                ascentStartTimes.remove(player.getName());
                ascentStartHeights.remove(player.getName());
            }
            // Si pasaron más de 7 segundos, reiniciar el contador
            else if (System.currentTimeMillis() - startTime > 7000) {
                ascentStartTimes.remove(player.getName());
                ascentStartHeights.remove(player.getName());
            }
        }
    }
}