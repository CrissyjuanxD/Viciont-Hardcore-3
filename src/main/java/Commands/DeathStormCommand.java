package Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import Handlers.DeathStormHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeathStormCommand implements CommandExecutor, TabCompleter {
    private final DeathStormHandler deathStormHandler;

    public DeathStormCommand(DeathStormHandler deathStormHandler) {
        this.deathStormHandler = deathStormHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!label.equalsIgnoreCase("deathstorm")) {
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "۞ Uso: /deathstorm <reset|togglestop|add|remove> [hh:mm:ss]");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reset":
                deathStormHandler.resetStorm();
                sender.sendMessage(ChatColor.GREEN + "☁ La DeathStorm ha sido reseteada.");
                return true;

            case "togglestop":
                deathStormHandler.toggleStopDeathStorm(sender);
                return true;

            case "add":
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "۞ Falta especificar el tiempo. Uso: /deathstorm " + subCommand + " hh:mm:ss");
                    return true;
                }

                int seconds = parseTime(args[1]);
                if (seconds < 0) {
                    sender.sendMessage(ChatColor.RED + "۞ Formato inválido. Usa hh:mm:ss (ejemplo: 01:30:00).");
                    return true;
                }

                if (subCommand.equals("add")) {
                    deathStormHandler.addStormSeconds(seconds);
                    sender.sendMessage(ChatColor.GREEN + "☁ Se ha añadido " + formatTime(seconds) + " de DeathStorm.");
                } else {
                    deathStormHandler.removeStormSeconds(seconds);
                    sender.sendMessage(ChatColor.GREEN + "☁ Se ha removido " + formatTime(seconds) + " de DeathStorm.");
                }
                return true;

            default:
                sender.sendMessage(ChatColor.RED + "۞ Subcomando desconocido. Usa: reset, togglestop, add, remove.");
                return true;
        }
    }

    private int parseTime(String time) {
        Pattern pattern = Pattern.compile("^(\\d{2}):(\\d{2}):(\\d{2})$");
        Matcher matcher = pattern.matcher(time);
        if (!matcher.matches()) {
            return -1;
        }
        int hours = Integer.parseInt(matcher.group(1));
        int minutes = Integer.parseInt(matcher.group(2));
        int seconds = Integer.parseInt(matcher.group(3));

        return (hours * 3600) + (minutes * 60) + seconds;
    }

    private String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            // Sugerir subcomandos
            List<String> subCommands = Arrays.asList("reset", "togglestop", "add", "remove");
            for (String sub : subCommands) {
                if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                    suggestions.add(sub);
                }
            }
            return suggestions;
        }
        else if (args.length == 2) {
            // Sugerir tiempo solo si el subcomando es add o remove
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("add") || subCommand.equals("remove")) {
                List<String> times = Arrays.asList("00:01:00", "00:05:00", "00:30:00", "01:00:00");
                for (String t : times) {
                    if (t.startsWith(args[1])) {
                        suggestions.add(t);
                    }
                }
            }
            return suggestions;
        }

        return suggestions; // Devuelve lista vacía si hay más de 2 argumentos
    }
}