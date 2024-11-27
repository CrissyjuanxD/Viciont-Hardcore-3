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
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateTablistForPlayer(player);
        }
    }

    public void updateTablistForPlayer(Player player) {
        // Formato del header (cabecera)
        String header = ChatColor.DARK_GRAY + "" + "●" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "                 " + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + ChatColor.STRIKETHROUGH + "                 "  + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "                 " + ChatColor.DARK_GRAY + "●\n" +
                ChatColor.GRAY + " \n" +
                ChatColor.WHITE + "" + ChatColor.BOLD + "        \uE073        \n" +
                ChatColor.GRAY + " \n" +
                ChatColor.GRAY + " \n";

        // Formato del footer (pie de página)
        String footer = ChatColor.GRAY + " \n" +
                ChatColor.WHITE + "" + ChatColor.BOLD + "Creado por: " + ChatColor.DARK_AQUA + "" + "CrissyjuanxD" +" \n" +
                ChatColor.GRAY + " \n" +
                ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "●" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           "  + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "     " + ChatColor.DARK_PURPLE + ChatColor.BOLD + "∨" + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "      " + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           " + ChatColor.GRAY + ChatColor.BOLD + "" + ChatColor.STRIKETHROUGH + "           "  + ChatColor.DARK_GRAY + ChatColor.BOLD + "" + "●";

        player.setPlayerListHeaderFooter(header, footer);

        // Actualizar solo el nombre del jugador específico en el tablist
        player.setPlayerListName(ChatColor.GRAY + player.getName() + " " + getPlayerHealth(player));
    }

    private String getPlayerHealth(Player player) {
        double health = player.getHealth();  // Salud actual
        double absorption = player.getAbsorptionAmount();  // Salud de absorción
        double maxHealth = 20.0;

        ChatColor healthColor;

        if (health > 10) {
            healthColor = ChatColor.DARK_PURPLE;  // Salud alta
        } else if (health > 5) {
            healthColor = ChatColor.GOLD;  // Salud media
        } else {
            healthColor = ChatColor.RED;  // Salud baja
        }

        String healthString = healthColor + "\uDB80\uDC64" + String.format("%.0f", health);

        String absorptionString = "";
        if (absorption > 0) {
            absorptionString = ChatColor.YELLOW + "\uDB80\uDD88" + String.format("%.0f", absorption);
        }

        return healthString + absorptionString;
    }
}

