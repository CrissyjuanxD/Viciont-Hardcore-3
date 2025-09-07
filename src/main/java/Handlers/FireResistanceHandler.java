package Handlers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FireResistanceHandler implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Integer> playerFireTicks;

    public FireResistanceHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerFireTicks = new HashMap<>();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (player.getFireTicks() > 0) {
            playerFireTicks.put(player.getUniqueId(), player.getFireTicks());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (playerFireTicks.containsKey(playerId)) {
            int savedFireTicks = playerFireTicks.get(playerId);
            player.setFireTicks(savedFireTicks);
            playerFireTicks.remove(playerId);
        }
    }
}