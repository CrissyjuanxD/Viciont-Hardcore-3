package Commands;

import Managers.MobManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SpawnMobs implements CommandExecutor, TabCompleter {

    private final MobManager mobManager;

    public SpawnMobs(JavaPlugin plugin, MobManager mobManager) {
        this.mobManager = mobManager;
        plugin.getCommand("spawnvct").setExecutor(this);
        plugin.getCommand("spawnvct").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Uso: /spawnvct <mob> [jugador|variante] [x] [y] [z]");
            return true;
        }

        String mobType = args[0].toLowerCase();
        Location location = null;
        Player targetPlayer = null;
        String variantArg = null;

        // Comprobación de argumentos
        if (args.length > 1) {
            if (Bukkit.getPlayer(args[1]) != null) {
                targetPlayer = Bukkit.getPlayer(args[1]);
                location = targetPlayer.getLocation();
            } else {
                // Si no es jugador, asumimos que es una variante (ej: "Pingo", "LIME")
                variantArg = args[1];
            }
        }

        if (args.length >= 4) {
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
        }

        if (location == null && sender instanceof Player) {
            targetPlayer = (Player) sender;
            location = targetPlayer.getLocation();
        } else if (location == null) {
            sender.sendMessage("Debes especificar un jugador o coordenadas si no eres un jugador.");
            return true;
        }

        // Llamamos al Manager
        boolean success = mobManager.spawnMob(mobType, location, targetPlayer, variantArg);

        if (success) {
            sender.sendMessage("¡" + mobType + " ha sido spawneado en " + locationToString(location) + "!");
        } else {
            sender.sendMessage("Mob no reconocido o variante inválida. Usa el tabulador para ver opciones.");
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
            return mobManager.getRegisteredMobs().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("corruptedskeleton")) {
            suggestions.add("lime");
            suggestions.add("green");
            suggestions.add("yellow");
            suggestions.add("orange");
            suggestions.add("red");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("customdolphin")) {
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