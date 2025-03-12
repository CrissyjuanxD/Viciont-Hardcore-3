package Commands;

import Dificultades.CustomMobs.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import vct.hardcore3.DayHandler;

import java.util.ArrayList;
import java.util.List;

public class SpawnMobs implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final Bombita bombitaSpawner;
    private final Iceologer iceologerSpawner;
    private final CorruptedZombies corruptedZombieSpawner;
    private final CorruptedSpider corruptedSpider;
    private final QueenBeeHandler queenBeeHandler;
    private final GuardianBlaze guardianBlaze;
    private final GuardianCorruptedSkeleton guardianCorruptedSkeleton;
    private final CorruptedSkeleton corruptedSkeleton;
    private final DayHandler dayHandler;

    private final CustomDolphin customDolphin;

    public SpawnMobs(JavaPlugin plugin, DayHandler dayHandler) {
        this.plugin = plugin;
        this.bombitaSpawner = new Bombita(plugin);
        this.iceologerSpawner = new Iceologer(plugin);
        this.corruptedZombieSpawner = new CorruptedZombies(plugin);
        this.corruptedSpider = new CorruptedSpider(plugin);
        this.queenBeeHandler = new QueenBeeHandler(plugin);
        this.guardianBlaze = new GuardianBlaze(plugin);
        this.guardianCorruptedSkeleton = new GuardianCorruptedSkeleton(plugin);
        this.dayHandler = dayHandler;
        this.corruptedSkeleton = new CorruptedSkeleton(plugin, dayHandler);
        this.customDolphin = new CustomDolphin(plugin);
        plugin.getCommand("spawnvct").setExecutor(this);
        plugin.getCommand("spawnvct").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Uso: /spawnvct <mob> [jugador (opcional)] [x] [y] [z]");
            return true;
        }

        // nombre del mob
        String mobType = args[0].toLowerCase();

        // Variables para ubicación
        Location location = null;
        Player targetPlayer = null;

        if (args.length > 1 && Bukkit.getPlayer(args[1]) != null) {
            targetPlayer = Bukkit.getPlayer(args[1]);
            location = targetPlayer.getLocation();
        } else if (args.length >= 4) {
            try {
                World world = sender instanceof Player ? ((Player) sender).getWorld() : Bukkit.getWorlds().get(0);
                double x = Double.parseDouble(args[args.length - 3]);
                double y = Double.parseDouble(args[args.length - 2]);
                double z = Double.parseDouble(args[args.length - 1]);
                location = new Location(world, x, y, z);
            } catch (NumberFormatException e) {
                sender.sendMessage("Las coordenadas deben ser números válidos.");
                return true;
            }
        } else if (sender instanceof Player) {
            targetPlayer = (Player) sender;
            location = targetPlayer.getLocation();
        } else {
            sender.sendMessage("Debes especificar un jugador o coordenadas si no eres un jugador.");
            return true;
        }

        // Spawnear el mob seleccionado
        switch (mobType) {
            case "bombita":
                bombitaSpawner.spawnBombita(location);
                sender.sendMessage("¡Bombita ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "iceologer":
                iceologerSpawner.spawnIceologer(location);
                sender.sendMessage("¡Iceologer ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "corruptedzombie":
                corruptedZombieSpawner.spawnCorruptedZombie(location);
                sender.sendMessage("¡Corrupted Zombie ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "corruptedspider":
                corruptedSpider.spawnCorruptedSpider(location);
                sender.sendMessage("¡Corrupted Spider ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "queenbee":
                queenBeeHandler.spawnQueenBee(location);
                sender.sendMessage("¡Queen Bees ha sido spawneada en " + locationToString(location) + "!");
                break;

            case "guardianblaze":
                guardianBlaze.spawnGuardianBlaze(location);
                sender.sendMessage("¡Guardian Blaze ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "guardiancorruptedskeleton":
                guardianCorruptedSkeleton.spawnGuardianCorruptedSkeleton(location);
                sender.sendMessage("¡Guardian Corrupted Skeleton ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "corruptedskeleton":
                if (args.length >= 2) {
                    String variantName = args[1].toUpperCase(); // Convertir a mayúsculas para coincidir con el enum
                    corruptedSkeleton.spawnCorruptedSkeleton(location, variantName);
                    sender.sendMessage("¡Corrupted Skeleton (" + variantName + ") ha sido spawneado en " + locationToString(location) + "!");
                } else {
                    // Si no se especifica una variante, spawnear una aleatoria
                    corruptedSkeleton.spawnCorruptedSkeleton(location, null);
                    sender.sendMessage("¡Corrupted Skeleton (aleatorio) ha sido spawneado en " + locationToString(location) + "!");
                }
                break;

            case "customdolphin":
                String dolphinType = args[1];
                if (!dolphinType.equalsIgnoreCase("Pingo") && !dolphinType.equalsIgnoreCase("Pinga")) {
                    sender.sendMessage("Tipo de delfín no válido. Usa Pingo o Pinga.");
                    return true;
                }

                customDolphin.spawnDolphin(location, dolphinType);
                sender.sendMessage("¡Delfín " + dolphinType + " ha sido spawneado en " + locationToString(location) + "!");
                break;

            default:
                sender.sendMessage("Mob no reconocido. Usa /spawnvct <bombita|iceologer|corruptedzombie|corruptedspider|queenbee>");
                break;
        }

        return true;
    }

    private String locationToString(Location location) {
        return "(" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            // Sugerencias para los nombres de mobs
            suggestions.add("bombita");
            suggestions.add("iceologer");
            suggestions.add("corruptedzombie");
            suggestions.add("corruptedspider");
            suggestions.add("queenbee");
            suggestions.add("guardianblaze");
            suggestions.add("guardiancorruptedskeleton");
            suggestions.add("corruptedskeleton");
            suggestions.add("customdolphin");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("corruptedskeleton")) {
            // Sugerencias para las variantes del Corrupted Skeleton
            for (CorruptedSkeleton.Variant variant : CorruptedSkeleton.Variant.values()) {
                suggestions.add(variant.name().toLowerCase()); // Convertir a minúsculas para coincidir con el comando
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("customdolphin")) {
            // Sugerencias para los tipos de delfín
            suggestions.add("Pingo");
            suggestions.add("Pinga");
        } else if (args.length == 2) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                suggestions.add(player.getName());
            }
        } else if (args.length == 3 || args.length == 4 || args.length == 5) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                suggestions.add(String.valueOf(player.getLocation().getBlockX()));
                suggestions.add(String.valueOf(player.getLocation().getBlockY()));
                suggestions.add(String.valueOf(player.getLocation().getBlockZ()));
            }
        }

        return suggestions;
    }
}
