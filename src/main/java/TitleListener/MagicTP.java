package TitleListener;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.ArrayList;
import java.util.List;

public class MagicTP implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;

    public MagicTP(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Validación básica
        if (args.length != 4 && args.length != 5 && args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Uso correcto:");
            sender.sendMessage(ChatColor.RED + "/magictp <jugador|@a> <x> <y> <z> [nether|end|dimensioncustom]");
            sender.sendMessage(ChatColor.RED + "/magictp <jugador|@a> spawn");
            return true;
        }

        String targetName = args[0];
        boolean useSpawn = args.length == 2 && args[1].equalsIgnoreCase("spawn");

        // Procesar coordenadas y dimensión si no es spawn
        final Location targetLocation;
        if (!useSpawn && (args.length == 4 || args.length == 5)) {
            try {
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);

                // Determinar la dimensión
                World targetWorld;
                if (args.length == 5) {
                    String dimension = args[4].toLowerCase();
                    switch (dimension) {
                        case "nether":
                            targetWorld = getWorldByEnvironment(World.Environment.NETHER);
                            if (targetWorld == null) {
                                sender.sendMessage(ChatColor.RED + "No se encontró un mundo del Nether.");
                                return true;
                            }
                            break;
                        case "end":
                            targetWorld = getWorldByEnvironment(World.Environment.THE_END);
                            if (targetWorld == null) {
                                sender.sendMessage(ChatColor.RED + "No se encontró un mundo del End.");
                                return true;
                            }
                            break;
                        case "dimensioncustom":
                            // Buscar una dimensión custom (que no sea overworld, nether o end)
                            targetWorld = getCustomDimension();
                            if (targetWorld == null) {
                                sender.sendMessage(ChatColor.RED + "No se encontró una dimensión custom.");
                                return true;
                            }
                            break;
                        default:
                            sender.sendMessage(ChatColor.RED + "Dimensión no válida. Use: nether, end, o dimensioncustom");
                            return true;
                    }
                } else {
                    // Por defecto usar overworld
                    targetWorld = getWorldByEnvironment(World.Environment.NORMAL);
                    if (targetWorld == null) {
                        targetWorld = Bukkit.getWorlds().get(0); // Fallback al primer mundo
                    }
                }

                targetLocation = new Location(targetWorld, x, y, z);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Las coordenadas deben ser números válidos.");
                return true;
            }
        } else {
            targetLocation = null;
        }

        // Obtener jugadores objetivo
        final List<Player> players = new ArrayList<>();
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
                    } else if (targetLocation != null) {
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
            // Primer argumento: jugadores
            completions.add("@a");
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 2) {
            // Segundo argumento: spawn o coordenada X
            completions.add("spawn");
            if (sender instanceof Player) {
                Player p = (Player) sender;
                completions.add(String.valueOf(p.getLocation().getBlockX()));
            }
        } else if (args.length == 3 && !args[1].equalsIgnoreCase("spawn")) {
            // Tercer argumento: coordenada Y
            if (sender instanceof Player) {
                Player p = (Player) sender;
                completions.add(String.valueOf(p.getLocation().getBlockY()));
            }
        } else if (args.length == 4 && !args[1].equalsIgnoreCase("spawn")) {
            // Cuarto argumento: coordenada Z
            if (sender instanceof Player) {
                Player p = (Player) sender;
                completions.add(String.valueOf(p.getLocation().getBlockZ()));
            }
        } else if (args.length == 5 && !args[1].equalsIgnoreCase("spawn")) {
            // Quinto argumento: dimensión
            completions.add("nether");
            completions.add("end");
            if (getCustomDimension() != null) {
                completions.add("dimensioncustom");
            }
        }

        return completions;
    }

    /**
     * Busca un mundo por su tipo de entorno
     */
    private World getWorldByEnvironment(World.Environment environment) {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == environment) {
                return world;
            }
        }
        return null;
    }

    /**
     * Busca una dimensión custom (que no sea overworld, nether o end)
     */
    private World getCustomDimension() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL &&
                    world.getEnvironment() != World.Environment.NETHER &&
                    world.getEnvironment() != World.Environment.THE_END) {
                return world;
            }
        }
        // Si no hay mundos custom, buscar por nombre que contenga "custom"
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().toLowerCase().contains("custom")) {
                return world;
            }
        }
        return null;
    }
}