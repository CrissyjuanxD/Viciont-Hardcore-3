package TitleListener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class MagicTP implements CommandExecutor {

    private final JavaPlugin plugin;

    public MagicTP(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 5) {
            sender.sendMessage(ChatColor.RED + "Uso correcto: /magic tp <jugador o @a> <x> <y> <z>");
            return true;
        }

        String targetName = args[1];
        double x, y, z;

        try {
            x = Double.parseDouble(args[2]);
            y = Double.parseDouble(args[3]);
            z = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Las coordenadas deben ser números válidos.");
            return true;
        }

        Player[] players;
        if (targetName.equalsIgnoreCase("@a")) {
            players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        } else {
            Player player = Bukkit.getPlayer(targetName);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "El jugador especificado no está en línea.");
                return true;
            }
            players = new Player[]{player};
        }

        for (Player player : players) {
            player.playSound(player.getLocation(), "minecraft:custom.transition_1", 1.0f, 1.0f);
            player.sendTitle("\uEAA4", "", 80, 80, 20);
        }


        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : players) {
                    player.teleport(new Location(player.getWorld(), x, y, z));
                }
            }
        }.runTaskLater(plugin, 120);

        return true;
    }


}
