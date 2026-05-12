package TitleListener;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
            sender.sendMessage(ChatColor.RED + "/magictp <jugador|@a> <x> <y> <z> [nombre_mundo]");
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
                    // AQUÍ ESTÁ EL CAMBIO: Buscamos el mundo por su nombre real
                    String worldName = args[4];
                    targetWorld = Bukkit.getWorld(worldName);

                    if (targetWorld == null) {
                        sender.sendMessage(ChatColor.RED + "El mundo '" + worldName + "' no existe o no está cargado.");
                        sender.sendMessage(ChatColor.GRAY + "Mundos disponibles: " +
                                Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.joining(", ")));
                        return true;
                    }
                } else {
                    // Por defecto usar el mundo donde está el sender (si es jugador) o el default
                    if (sender instanceof Player) {
                        targetWorld = ((Player) sender).getWorld();
                    } else {
                        targetWorld = Bukkit.getWorlds().get(0);
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

        // Efectos visuales y de sonido antes de teletransportar
        for (Player player : players) {
            player.playSound(player.getLocation(), "minecraft:custom.transition_1", 1.0f, 1.3f);
            player.sendTitle("\uEAA4", "", 80, 80, 20);
        }

        // Tarea diferida para el teletransporte
        new BukkitRunnable() {
            @Override
            public void run() {
                World mainWorld = Bukkit.getWorlds().get(0);

                for (Player player : players) {
                    if (useSpawn) {
                        Location bedSpawn = player.getBedSpawnLocation();
                        if (bedSpawn != null) {
                            player.teleport(bedSpawn);
                        } else {
                            player.teleport(mainWorld.getSpawnLocation());
                        }
                    } else if (targetLocation != null) {
                        player.teleport(targetLocation);
                    }
                }
            }
        }.runTaskLater(plugin, 120); // 120 ticks = 6 segundos

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
                completions.add(String.valueOf(((Player) sender).getLocation().getBlockX()));
            }
        } else if (args.length == 3 && !args[1].equalsIgnoreCase("spawn")) {
            // Tercer argumento: coordenada Y
            if (sender instanceof Player) {
                completions.add(String.valueOf(((Player) sender).getLocation().getBlockY()));
            }
        } else if (args.length == 4 && !args[1].equalsIgnoreCase("spawn")) {
            // Cuarto argumento: coordenada Z
            if (sender instanceof Player) {
                completions.add(String.valueOf(((Player) sender).getLocation().getBlockZ()));
            }
        } else if (args.length == 5 && !args[1].equalsIgnoreCase("spawn")) {
            // Quinto argumento: LISTA DINÁMICA DE MUNDOS
            // Esto buscará todos los mundos (vanilla y custom) y los pondrá en la lista
            for (World world : Bukkit.getWorlds()) {
                completions.add(world.getName());
            }
        }

        return completions;
    }
}