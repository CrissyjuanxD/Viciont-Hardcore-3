package list;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

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
        String header = ChatColor.DARK_GRAY + "" + "●" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "                 " + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + ChatColor.STRIKETHROUGH + "                 "  + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "                 " + ChatColor.DARK_GRAY + "●\n" +
                ChatColor.GRAY + " \n" +
                ChatColor.WHITE + "" + ChatColor.BOLD + "        \uE073        \n" +
                ChatColor.GRAY + " \n" +
                ChatColor.GRAY + " \n";

        // Formato del footer (pie de página) - opcional
        String footer = ChatColor.GRAY + " \n" +
                ChatColor.WHITE + "" + ChatColor.BOLD + "Creado por: " + ChatColor.DARK_AQUA + "" + "CrissyjuanxD" +" \n" +
                ChatColor.GRAY + " \n" +
                ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "●" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           "  + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "     " + ChatColor.DARK_PURPLE + ChatColor.BOLD + "∨" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "      " + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           "  + ChatColor.DARK_GRAY + ChatColor.BOLD + "" + "●";

        // Asignar el header y footer al jugador
        player.setPlayerListHeaderFooter(header, footer);

        // Actualizar solo el nombre del jugador específico en el tablist
        player.setPlayerListName(ChatColor.GRAY + player.getName() + " " + getPlayerHealth(player));
    }

    // Método para obtener la salud del jugador y cambiar el color según el nivel de salud
    private String getPlayerHealth(Player player) {
        double health = player.getHealth();  // Salud actual (sin absorción)
        double absorption = player.getAbsorptionAmount();  // Salud de absorción (corazones amarillos)
        double maxHealth = 20.0;  // Salud máxima por defecto en Minecraft (10 corazones)

        ChatColor healthColor;

        // Cambiar el color basado en el nivel de salud
        if (health > 10) {
            healthColor = ChatColor.DARK_PURPLE;  // Salud alta
        } else if (health > 5) {
            healthColor = ChatColor.GOLD;  // Salud media
        } else {
            healthColor = ChatColor.RED;  // Salud baja
        }

        // Mostrar la salud sin incluir corazones de absorción y formatearla como un número entero
        String healthString = healthColor + "\uDB80\uDC64" + String.format("%.0f", health);

        // Formatear la absorción si el jugador tiene corazones de absorción
        String absorptionString = "";
        if (absorption > 0) {
            absorptionString = ChatColor.YELLOW + "\uDB80\uDD88" + String.format("%.0f", absorption);
        }

        // Devolver la cadena completa que incluye la salud y la absorción (si tiene)
        return healthString + absorptionString;
    }
}

