package Commands;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import java.util.Set;
import java.util.UUID;

public class TiempoListener implements Listener {

    private final TiempoCommand tiempoCommand;

    public TiempoListener(TiempoCommand tiempoCommand) {
        this.tiempoCommand = tiempoCommand;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Solo volver a añadir las bossbars que pertenecen a este jugador
        Set<String> playerBars = tiempoCommand.getPlayerBossBars(uuid);
        if (playerBars != null) {
            for (String barId : playerBars) {
                BossBar bar = tiempoCommand.getBossBar(barId);
                if (bar != null) {
                    bar.addPlayer(player);
                }
            }
        }
    }
}
