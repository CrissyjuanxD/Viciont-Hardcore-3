package TitleListener;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.ArrayList;
import java.util.List;

public class MagicTP implements CommandExecutor, TabCompleter { // Añadido TabCompleter aquí

    private final JavaPlugin plugin;

    public MagicTP(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Validación básica
        if (args.length != 4 && args.length != 2) { // Corregido de 1 a 2 para coincidir con el mensaje de ayuda
            sender.sendMessage(ChatColor.RED + "Uso correcto: /magictp <jugador|@a> <x> <y> <z> o /magictp <jugador|@a> spawn");
            return true;
        }

        String targetName = args[0];
        boolean useSpawn = args.length == 2 && args[1].equalsIgnoreCase("spawn");

        // Procesar coordenadas si no es spawn
        final Location targetLocation; // Marcada como final para resolver el primer error
        if (!useSpawn && args.length == 4) {
            try {
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);
                targetLocation = new Location(Bukkit.getWorlds().get(0), x, y, z);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Las coordenadas deben ser números válidos.");
                return true;
            }
        } else {
            targetLocation = null; // Inicialización necesaria
        }

        // Obtener jugadores objetivo
        final List<Player> players = new ArrayList<>(); // Marcada como final
        if (targetName.equalsIgnoreCase("@a")) {
            players.addAll(Bukkit.getOnlinePlayers());
        } else {
            Player player = Bukkit.getPlayer(targetName);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "El jugador especificado no está en línea.");
                return true;
            }
            players.add(player);
        }

        // Efectos visuales y de sonido
        for (Player player : players) {
            player.playSound(player.getLocation(), "minecraft:custom.transition_1", 1.0f, 1.0f);
            player.sendTitle("\uEAA4", "", 80, 80, 20);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                World overworld = Bukkit.getWorlds().get(0);

                for (Player player : players) {
                    if (useSpawn) {
                        Location bedSpawn = player.getBedSpawnLocation();
                        if (bedSpawn != null) {
                            player.teleport(bedSpawn);
                        } else {
                            player.teleport(overworld.getSpawnLocation());
                        }
                    } else if (targetLocation != null) { // Verificación añadida para seguridad
                        player.teleport(targetLocation);
                    }
                }
            }
        }.runTaskLater(plugin, 120);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("@a");
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("@a")) {
            completions.add("spawn");
            if (sender instanceof Player) {
                Player p = (Player) sender;
                completions.add(String.valueOf(p.getLocation().getBlockX()));
            }
        } else if (args.length == 3 && !args[1].equalsIgnoreCase("spawn")) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                completions.add(String.valueOf(p.getLocation().getBlockY()));
            }
        } else if (args.length == 4 && !args[1].equalsIgnoreCase("spawn")) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                completions.add(String.valueOf(p.getLocation().getBlockZ()));
            }
        }

        return completions;
    }
}