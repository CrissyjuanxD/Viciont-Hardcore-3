package Commands;

import Managers.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vct.hardcore3.ViciontHardcore3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemsCommands implements CommandExecutor, TabCompleter {

    private final ItemManager itemManager;

    public ItemsCommands(ViciontHardcore3 plugin, ItemManager itemManager) {
        this.itemManager = itemManager;
        plugin.getCommand("givevct").setExecutor(this);
        plugin.getCommand("givevct").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUso: /givevct <item> [jugador|@a] [cantidad] [usos_especiales]");
            return true;
        }

        String itemName = args[0].toLowerCase();
        String targetName = null;
        int cantidad = 1;
        int usosEspeciales = -1; // -1 significa que el sistema usará el valor por defecto/aleatorio

        if (args.length > 1) {
            targetName = args[1];
        }

        if (args.length > 2) {
            try {
                cantidad = Integer.parseInt(args[2]);
                if (cantidad <= 0) {
                    sender.sendMessage("§cLa cantidad debe ser mayor a 0.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cLa cantidad debe ser un número válido.");
                return true;
            }
        }

        if (args.length > 3) {
            try {
                usosEspeciales = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cLos usos especiales deben ser un número válido.");
                return true;
            }
        }

        List<Player> targets = new ArrayList<>();

        if (targetName == null) {
            if (sender instanceof Player) {
                targets.add((Player) sender);
            } else {
                sender.sendMessage("§cDebes especificar un jugador si ejecutas el comando desde la consola.");
                return true;
            }
        } else if (targetName.equalsIgnoreCase("@a")) {
            targets.addAll(Bukkit.getOnlinePlayers());
            if (targets.isEmpty()) {
                sender.sendMessage("§cNo hay jugadores en línea en este momento.");
                return true;
            }
        } else {
            Player targetPlayer = Bukkit.getPlayerExact(targetName);
            if (targetPlayer == null) {
                sender.sendMessage("§cEl jugador '" + targetName + "' no está en línea.");
                return true;
            }
            targets.add(targetPlayer);
        }

        boolean itemExists = false;

        for (Player target : targets) {
            // Ahora le pasamos también los usosEspeciales al Manager
            ItemStack item = itemManager.getItem(itemName, cantidad, target, usosEspeciales);

            if (item == null) {
                sender.sendMessage("§cEse item no existe: " + itemName);
                return true;
            }

            itemExists = true;
            target.getInventory().addItem(item);
        }

        if (itemExists) {
            if (targets.size() > 1) {
                sender.sendMessage("§aHas dado " + cantidad + "x " + itemName + " a todos los jugadores en línea.");
            } else {
                sender.sendMessage("§aHas dado " + cantidad + "x " + itemName + " a " + targets.get(0).getName() + ".");
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return itemManager.getRegisteredItems().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());

        } else if (args.length == 2) {
            List<String> completions = new ArrayList<>();
            completions.add("@a");

            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }

            return completions.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());

        } else if (args.length == 3) {
            List<String> amounts = Arrays.asList("1", "16", "32", "64");
            return amounts.stream()
                    .filter(amount -> amount.startsWith(args[2]))
                    .collect(Collectors.toList());

        } else if (args.length == 4) {
            if (args[0].toLowerCase().startsWith("esencia_") || args[0].equalsIgnoreCase("ficha_mision")) {
                List<String> usos = Arrays.asList("1", "2", "3", "5", "10", "15", "20", "25", "30");
                return usos.stream()
                        .filter(u -> u.startsWith(args[3]))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}