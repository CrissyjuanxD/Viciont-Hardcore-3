package chat;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.util.*;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scoreboard.Team;
public class chatgeneral implements Listener {
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        // Obtener el equipo del jugador
        Team team = player.getScoreboard().getEntryTeam(player.getName());
        // Variables de prefijo y sufijo por defecto
        String prefix = "";
        String suffix = "";

        // Si el jugador tiene un equipo, extraemos los valores
        if (team != null) {
            prefix = team.getPrefix() != null ? team.getPrefix() : ""; // Obtener prefijo del equipo
            suffix = team.getSuffix() != null ? team.getSuffix() : ""; // Obtener sufijo del equipo
        }

        // Formatear el mensaje eliminando los "<>" y agregando prefijos y sufijos
        String formattedMessage = prefix + player.getName() + suffix + " > " + message;
        // Establecer el nuevo formato del chat
        event.setFormat(formattedMessage);
    }
}
