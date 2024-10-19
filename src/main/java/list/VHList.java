package list;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class VHList extends BukkitRunnable {

    private final JavaPlugin plugin;

    public VHList (JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // Lógica para actualizar el tablist de todos los jugadores
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateTablistForPlayer(player);
        }
    }

    // Método para actualizar el tablist de un jugador en específico
    public void updateTablistForPlayer(Player player) {
        // Formato del header (cabecera) con el símbolo de imagen y diseño
        String header = ChatColor.GRAY + " \n" +
                ChatColor.WHITE + "" + ChatColor.BOLD + "        \uE073        \n" +
                ChatColor.GRAY + " \n" +
                ChatColor.GRAY + " \n" +
                ChatColor.GRAY + " \n" +
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "╚" + ChatColor.GRAY + ChatColor.BOLD + "≡≡≡≡≡≡≡≡≡≡" + ChatColor.DARK_GRAY + ChatColor.BOLD + "●" + ChatColor.GRAY + ChatColor.BOLD + "≡≡≡≡≡≡≡≡≡≡" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "╝\n";

        // Formato del footer (pie de página) - opcional
        String footer = ChatColor.GRAY + " \n" +
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "∨" + ChatColor.GRAY + ChatColor.BOLD + "≡≡≡≡≡≡≡≡≡≡" + ChatColor.DARK_GRAY + ChatColor.BOLD + "●" + ChatColor.GRAY + ChatColor.BOLD + "≡≡≡≡≡≡≡≡≡≡" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "∨\n" +
                ChatColor.GRAY + " \n" +
                ChatColor.WHITE + "" + ChatColor.BOLD + "Server by: CrissyjuanxD";

        // Asignar el header y footer al jugador
        player.setPlayerListHeaderFooter(header, footer);

        // Cambiar el nombre del jugador en el tablist
        for (Player targetPlayer : Bukkit.getOnlinePlayers()) {
            player.setPlayerListName(ChatColor.GRAY + targetPlayer.getName() + " " + ChatColor.DARK_PURPLE + "♥" + getPlayerHealth(targetPlayer));
        }
    }
    // Método para obtener la salud del jugador y formatearla
    private String getPlayerHealth(Player player) {
        double health = player.getHealth();
        return String.format("%.0f", health); // Mostrará la vida como un número entero
    }
}

