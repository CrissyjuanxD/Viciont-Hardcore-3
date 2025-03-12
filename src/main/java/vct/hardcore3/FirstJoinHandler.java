package vct.hardcore3;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.entity.Player;

public class FirstJoinHandler implements Listener {
    private final JavaPlugin plugin;

    public FirstJoinHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getEntryTeam(player.getName());

        // Verifica si el jugador ya ha entrado antes usando su UUID
        if (!this.plugin.getConfig().getBoolean("HasJoinedBefore." + player.getUniqueId())) {
            // Marcar al jugador como que ya ha entrado
            this.plugin.getConfig().set("HasJoinedBefore." + player.getUniqueId(), true);
            this.plugin.saveConfig();

            // Asignar el equipo "TSurvivor" si existe
            Team survivorTeam = scoreboard.getTeam("TSurvivor");
            if (survivorTeam != null) {
                survivorTeam.addEntry(player.getName());
            }

            // Mensajes adicionales o tareas que desees realizar en el ingreso
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                player.sendMessage("Bienvenido al servidor, " + player.getName() + "!");
            }, 40);
        }
    }
}
