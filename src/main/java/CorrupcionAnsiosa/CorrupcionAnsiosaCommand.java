package CorrupcionAnsiosa;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CorrupcionAnsiosaCommand implements CommandExecutor, TabCompleter {
    private final CorrupcionAnsiosaManager corruptionManager;

    public CorrupcionAnsiosaCommand(CorrupcionAnsiosaManager corruptionManager) {
        this.corruptionManager = corruptionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add":
                handleAdd(sender, args);
                break;
            case "remove":
                handleRemove(sender, args);
                break;
            case "reset":
                handleReset(sender, args);
                break;
            case "enable":
                handleEnable(sender, true);
                break;
            case "disable":
                handleEnable(sender, false);
                break;
            case "check":
                handleCheck(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /ca add <jugador|@a> <porcentaje>");
            return;
        }

        try {
            double amount = Double.parseDouble(args[2]);
            if (args[1].equalsIgnoreCase("@a")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    corruptionManager.addCorruption(player, amount);
                }
                sender.sendMessage(ChatColor.GREEN + "Corrupción aumentada para todos los jugadores.");
            } else {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                    return;
                }
                corruptionManager.addCorruption(target, amount);
                sender.sendMessage(ChatColor.GREEN + "Corrupción de " + target.getName() + " aumentada en " + amount + "%");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Porcentaje inválido.");
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /ca remove <jugador|@a> <porcentaje>");
            return;
        }

        try {
            double amount = Double.parseDouble(args[2]);
            if (args[1].equalsIgnoreCase("@a")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    corruptionManager.removeCorruption(player, amount);
                }
                sender.sendMessage(ChatColor.GREEN + "Corrupción disminuida para todos los jugadores.");
            } else {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                    return;
                }
                corruptionManager.removeCorruption(target, amount);
                sender.sendMessage(ChatColor.GREEN + "Corrupción de " + target.getName() + " disminuida en " + amount + "%");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Porcentaje inválido.");
        }
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /ca reset <jugador|@a>");
            return;
        }

        if (args[1].equalsIgnoreCase("@a")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                corruptionManager.resetCorruption(player);
                player.sendMessage(ChatColor.YELLOW + "Un Admin ha reseteado tu corrupción ansiosa.");
            }
            sender.sendMessage(ChatColor.GREEN + "Corrupción resetada para todos los jugadores.");
        } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                return;
            }
            corruptionManager.resetCorruption(target);
            sender.sendMessage(ChatColor.GREEN + "Corrupción de " + target.getName() + " resetada al 100%");
            target.sendMessage(ChatColor.YELLOW + "Un Admin ha reseteado tu corrupción ansiosa.");
        }
    }

    private void handleEnable(CommandSender sender, boolean enable) {
        corruptionManager.setEnabled(enable);
        if (enable) {
            sender.sendMessage(ChatColor.GREEN + "Corrupción Ansiosa habilitada.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Corrupción Ansiosa deshabilitada.");
        }
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (args.length < 2 && !(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Uso: /ca check <jugador>");
            return;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                return;
            }
        } else {
            target = (Player) sender;
        }

        double corruption = corruptionManager.getCorruption(target);
        sender.sendMessage(ChatColor.GOLD + "Corrupción Ansiosa de " + target.getName() + ": " +
                String.format("%.1f", corruption) + "%");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Comandos Corrupción Ansiosa ===");
        sender.sendMessage(ChatColor.YELLOW + "/ca add <jugador|@a> <porcentaje> " + ChatColor.WHITE + "- Aumentar corrupción");
        sender.sendMessage(ChatColor.YELLOW + "/ca remove <jugador|@a> <porcentaje> " + ChatColor.WHITE + "- Disminuir corrupción");
        sender.sendMessage(ChatColor.YELLOW + "/ca reset <jugador|@a> " + ChatColor.WHITE + "- Resetear corrupción al 100%");
        sender.sendMessage(ChatColor.YELLOW + "/ca enable/disable " + ChatColor.WHITE + "- Habilitar/deshabilitar mecánica");
        sender.sendMessage(ChatColor.YELLOW + "/ca check [jugador] " + ChatColor.WHITE + "- Ver corrupción actual");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("add");
            completions.add("remove");
            completions.add("reset");
            completions.add("enable");
            completions.add("disable");
            completions.add("check");
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("enable") && !args[0].equalsIgnoreCase("disable")) {
            completions.add("@a");
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }

        return completions;
    }
}