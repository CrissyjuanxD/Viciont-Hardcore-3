package Gui.vithiums;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VithiumsCommand implements CommandExecutor, TabCompleter {

    private final VithiumsManager vithiumsManager;

    public VithiumsCommand(VithiumsManager vithiumsManager) {
        this.vithiumsManager = vithiumsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vithiums.admin")) {
            sender.sendMessage("§cNo tienes permiso.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUso: /vithiums <add|remove|get|removelista|addlista|habilitarlista|deshabilitarlista> [jugador] [cantidad]");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add":
            case "remove":
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /vithiums " + subCommand + " <jugador> <cantidad>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cEl jugador debe estar online para modificar su monedero físico.");
                    return true;
                }
                int amount;
                try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
                    sender.sendMessage("§cCantidad inválida.");
                    return true;
                }

                if (subCommand.equals("add")) {
                    boolean success = vithiumsManager.addPhysicalVithiums(target, amount);
                    if (success) sender.sendMessage("§aSe añadieron " + amount + " Vithiums a los monederos de " + target.getName());
                    else sender.sendMessage("§eSe añadieron algunos Vithiums, pero el/los monedero(s) se llenaron antes de terminar.");
                } else {
                    boolean success = vithiumsManager.removePhysicalVithiums(target, amount);
                    if (success) sender.sendMessage("§aSe removieron " + amount + " Vithiums de los monederos de " + target.getName());
                    else sender.sendMessage("§eEl jugador no tenía suficientes Vithiums en sus monederos.");
                }
                break;

            case "get":
                if (args.length < 2) {
                    sender.sendMessage("§cUso: /vithiums get <jugador>");
                    return true;
                }
                Player getTarget = Bukkit.getPlayer(args[1]);
                if (getTarget != null) {
                    int total = vithiumsManager.calculatePhysicalVithiums(getTarget);
                    sender.sendMessage("§a" + getTarget.getName() + " tiene un total de " + total + " Vithiums en sus monederos.");
                } else {
                    sender.sendMessage("§cJugador no encontrado u offline.");
                }
                break;

            case "removelista":
                if (args.length < 2) {
                    sender.sendMessage("§cUso: /vithiums removelista <jugador>");
                    return true;
                }
                vithiumsManager.addExcluded(args[1]);
                sender.sendMessage("§a" + args[1] + " excluido de la lista Top.");
                break;

            case "addlista":
                if (args.length < 2) {
                    sender.sendMessage("§cUso: /vithiums addlista <jugador>");
                    return true;
                }
                vithiumsManager.removeExcluded(args[1]);
                sender.sendMessage("§a" + args[1] + " añadido nuevamente a la lista Top.");
                break;

            case "habilitarlista":
                vithiumsManager.setTopEnabled(true);
                sender.sendMessage("§aHUD de Top Vithiums habilitado.");
                break;

            case "deshabilitarlista":
                vithiumsManager.setTopEnabled(false);
                sender.sendMessage("§cHUD de Top Vithiums deshabilitado.");
                break;

            default:
                sender.sendMessage("§cComando desconocido.");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        if (!sender.hasPermission("vithiums.admin")) {
            return completions;
        }

        if (args.length == 1) {
            commands.addAll(Arrays.asList("add", "remove", "get", "removelista", "addlista", "habilitarlista", "deshabilitarlista"));
            StringUtil.copyPartialMatches(args[0], commands, completions);
        }
        else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            // Sugerir jugadores para estos subcomandos
            if (Arrays.asList("add", "remove", "get", "removelista", "addlista").contains(subCommand)) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    commands.add(p.getName());
                }
                StringUtil.copyPartialMatches(args[1], commands, completions);
            }
        }
        else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            // Sugerir cantidades para add y remove
            if (subCommand.equals("add") || subCommand.equals("remove")) {
                commands.addAll(Arrays.asList("1", "10", "32", "64"));
                StringUtil.copyPartialMatches(args[2], commands, completions);
            }
        }

        Collections.sort(completions);
        return completions;
    }
}