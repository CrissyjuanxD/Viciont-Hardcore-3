package Commands;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.boss.BossBar;

public class TiempoListener implements Listener {

    private final TiempoCommand tiempoCommand;

    public TiempoListener(TiempoCommand tiempoCommand) {
        this.tiempoCommand = tiempoCommand;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        for (BossBar bossBar : tiempoCommand.getAllBossBars()) {
            bossBar.addPlayer(event.getPlayer());
        }
    }
}
