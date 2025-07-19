package Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import Handlers.DeathStormHandler;

import java.util.ArrayList;
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
        if (label.equalsIgnoreCase("resetdeathstorm")) {
            deathStormHandler.resetStorm();
            sender.sendMessage(ChatColor.GREEN + "La DeathStorm ha sido reseteada.");
            return true;
        }

        if ((label.equalsIgnoreCase("adddeathstorm") || label.equalsIgnoreCase("removedeathstorm")) && args.length == 1) {
            int seconds = parseTime(args[0]);
            if (seconds < 0) {
                sender.sendMessage(ChatColor.RED + "Formato inválido. Usa hh:mm:ss (ejemplo: 01:30:00).");
                return true;
            }

            if (label.equalsIgnoreCase("adddeathstorm")) {
                deathStormHandler.addStormSeconds(seconds);
                sender.sendMessage(ChatColor.GREEN + "Se ha añadido " + formatTime(seconds) + " de DeathStorm.");
            } else {
                deathStormHandler.removeStormSeconds(seconds);
                sender.sendMessage(ChatColor.GREEN + "Se ha removido " + formatTime(seconds) + " de DeathStorm.");
            }
            return true;
        }
        return false;
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
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("00:01:00");
            suggestions.add("00:05:00");
            suggestions.add("00:30:00");
            suggestions.add("01:00:00");
            return suggestions;
        }
        return null;
    }
}
