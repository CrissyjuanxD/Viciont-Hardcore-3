package Gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CambiosJoinListener implements Listener {
    private final CambiosDataManager dataManager;

    public CambiosJoinListener(CambiosDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        dataManager.loadPlayer(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        dataManager.unloadPlayer(e.getPlayer().getUniqueId());
    }
}