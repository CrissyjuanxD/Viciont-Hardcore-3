package Commands;

import Handlers.ShopHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShopCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final ShopHandler shopHandler;

    public ShopCommand(JavaPlugin plugin, ShopHandler shopHandler) {
        this.plugin = plugin;
        this.shopHandler = shopHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("viciont_hardcore3.shop.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /spawntienda <Monedas|Utilidades|Varios> [x] [y] [z]");
            return true;
        }

        String shopType = args[0];
        if (!Arrays.asList("Monedas", "Utilidades", "Varios").contains(shopType)) {
            sender.sendMessage(ChatColor.RED + "Tipo de tienda inválido. Usa: Monedas, Utilidades, o Varios");
            return true;
        }

        Location spawnLocation;

        if (args.length >= 4) {
            // Coordenadas especificadas
            try {
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);

                if (sender instanceof Player) {
                    spawnLocation = new Location(((Player) sender).getWorld(), x, y, z);
                } else {
                    spawnLocation = new Location(Bukkit.getWorlds().get(0), x, y, z);
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Las coordenadas deben ser números válidos.");
                return true;
            }
        } else {
            // Usar ubicación del jugador
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Debes especificar coordenadas si ejecutas desde consola.");
                return true;
            }
            spawnLocation = ((Player) sender).getLocation();
        }

        shopHandler.spawnShop(shopType, spawnLocation);
        sender.sendMessage(ChatColor.GREEN + "Tienda '" + shopType + "' spawneada en " +
                spawnLocation.getBlockX() + ", " + spawnLocation.getBlockY() + ", " + spawnLocation.getBlockZ());

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("Monedas");
            completions.add("Utilidades");
            completions.add("Varios");
        } else if (args.length >= 2 && args.length <= 4 && sender instanceof Player) {
            Player player = (Player) sender;
            Location loc = player.getLocation();

            if (args.length == 2) {
                completions.add(String.valueOf(loc.getBlockX()));
            } else if (args.length == 3) {
                completions.add(String.valueOf(loc.getBlockY()));
            } else if (args.length == 4) {
                completions.add(String.valueOf(loc.getBlockZ()));
            }
        }

        return completions;
    }
}