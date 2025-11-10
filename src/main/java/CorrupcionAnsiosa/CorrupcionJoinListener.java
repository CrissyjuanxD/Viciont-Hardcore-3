package CorrupcionAnsiosa;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class CorrupcionJoinListener implements Listener {
    private final CorrupcionAnsiosaManager corruptionManager;

    public CorrupcionJoinListener(CorrupcionAnsiosaManager corruptionManager) {
        this.corruptionManager = corruptionManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Inicializar datos de corrupción para el jugador
        corruptionManager.initializePlayerData(player);
    }
}