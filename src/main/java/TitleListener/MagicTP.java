package TitleListener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
        if (args.length != 5 && args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Uso correcto: /magic tp <jugador o @a> <x> <y> <z> o /magic tp <jugador o @a> spawn");
            return true;
        }

        String targetName = args[1];
        boolean useSpawn = args.length == 3 && args[2].equalsIgnoreCase("spawn");
        double x = 0, y = 0, z = 0;
        if (!useSpawn && args.length == 5) {
            try {
                x = Double.parseDouble(args[2]);
                y = Double.parseDouble(args[3]);
                z = Double.parseDouble(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Las coordenadas deben ser números válidos.");
                return true;
            }
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

        boolean finalUseSpawn = useSpawn;
        double finalX = x, finalY = y, finalZ = z;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : players) {
                    if (finalUseSpawn) {
                        Location bedSpawn = player.getBedSpawnLocation();
                        if (bedSpawn != null) {
                            player.teleport(bedSpawn);
                        } else {
                            player.teleport(player.getWorld().getSpawnLocation());
                        }
                    } else {
                        player.teleport(new Location(player.getWorld(), finalX, finalY, finalZ));
                    }
                }
            }
        }.runTaskLater(plugin, 120);

        return true;
    }
}
