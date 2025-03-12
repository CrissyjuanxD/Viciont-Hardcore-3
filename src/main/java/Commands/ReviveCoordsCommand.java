package Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import vct.hardcore3.ViciontHardcore3;

import java.util.Collections;
import java.util.List;

public class ReviveCoordsCommand implements CommandExecutor, TabCompleter {
    private final ViciontHardcore3 plugin;

    public ReviveCoordsCommand(ViciontHardcore3 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Uso incorrecto. Usa: /revivecoords <x> <y> <z>");
            return true;
        }

        try {
            double x = Double.parseDouble(args[0]);
            double y = Double.parseDouble(args[1]);
            double z = Double.parseDouble(args[2]);

            plugin.getConfig().set("revive.x", x);
            plugin.getConfig().set("revive.y", y);
            plugin.getConfig().set("revive.z", z);
            plugin.saveConfig();

            sender.sendMessage(ChatColor.GREEN + "Las coordenadas de revive han sido actualizadas.");
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Las coordenadas deben ser números válidos.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}

