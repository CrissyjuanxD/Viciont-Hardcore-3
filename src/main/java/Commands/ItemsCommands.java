package Commands;

import items.DoubleLifeTotem;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vct.hardcore3.ViciontHardcore3;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ItemsCommands implements CommandExecutor, TabCompleter {

    private final ViciontHardcore3 plugin;
    private final DoubleLifeTotem doubleLifeTotem;

    public ItemsCommands(ViciontHardcore3 plugin) {
        this.plugin = plugin;
        this.doubleLifeTotem = new DoubleLifeTotem(plugin);
        plugin.getCommand("givevct").setExecutor(this);
        plugin.getCommand("givevct").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUso: /givevct <item> [cantidad] [jugador]");
            return true;
        }

        String itemName = args[0].toLowerCase();
        int cantidad = 1; // Por defecto, 1 unidad
        Player target = null;

        // Identificar los argumentos opcionales
        if (args.length > 1) {
            try {
                cantidad = Integer.parseInt(args[1]);
                if (cantidad <= 0) {
                    sender.sendMessage("§cLa cantidad debe ser mayor a 0.");
                    return true;
                }
            } catch (NumberFormatException e) {
                // Si no es un número, se asume que es un jugador
                target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cEl jugador '" + args[1] + "' no está en línea.");
                    return true;
                }
            }
        }

        if (args.length > 2) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage("§cEl jugador '" + args[2] + "' no está en línea.");
                return true;
            }
        }

        // Si no se especifica un jugador, asignar al ejecutor si es un jugador
        if (target == null) {
            if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cDebes especificar un jugador si ejecutas el comando desde la consola.");
                return true;
            }
        }

        // Crear el item
        ItemStack item;
        switch (itemName) {
            case "doubletotem":
                item = doubleLifeTotem.createDoubleLifeTotem();
                item.setAmount(cantidad);
                break;
            default:
                sender.sendMessage("§cEse item no existe.");
                return true;
        }

        // Dar el item al jugador
        target.getInventory().addItem(item);
        sender.sendMessage("§aHas dado " + cantidad + "x " + itemName + " a " + target.getName() + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("doubletotem");
        } else if (args.length == 2) {
            // Sugerencias para cantidad o nombres de jugadores
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 3) {
            // Si el segundo argumento ya es un número, entonces el tercero es un jugador
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}
