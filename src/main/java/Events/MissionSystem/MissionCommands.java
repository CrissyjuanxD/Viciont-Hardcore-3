package Events.MissionSystem;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MissionCommands implements CommandExecutor, TabCompleter {
    private final MissionHandler missionHandler;
    private final MissionGUI missionGUI;

    public MissionCommands(MissionHandler missionHandler, MissionGUI missionGUI) {
        this.missionHandler = missionHandler;
        this.missionGUI = missionGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("activarmision")) {
            if (!sender.hasPermission("viciont_hardcore3.missions.admin")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                return true;
            }

            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Uso: /activarmision <número>");
                return true;
            }

            try {
                int missionNumber = Integer.parseInt(args[0]);
                missionHandler.activateMission(sender, missionNumber);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "El número de misión debe ser válido.");
            }
            return true;
        }

        if (label.equalsIgnoreCase("desactivarmision")) {
            if (!sender.hasPermission("viciont_hardcore3.missions.admin")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                return true;
            }

            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Uso: /desactivarmision <número>");
                return true;
            }

            try {
                int missionNumber = Integer.parseInt(args[0]);
                missionHandler.deactivateMission(sender, missionNumber);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "El número de misión debe ser válido.");
            }
            return true;
        }

        if (label.equalsIgnoreCase("addmision")) {
            if (!sender.hasPermission("viciont_hardcore3.missions.admin")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                return true;
            }

            if (args.length != 2) {
                sender.sendMessage(ChatColor.RED + "Uso: /addmision <jugador> <número>");
                return true;
            }

            try {
                String playerName = args[0];
                int missionNumber = Integer.parseInt(args[1]);
                missionHandler.addMissionToPlayer(sender, playerName, missionNumber);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "El número de misión debe ser válido.");
            }
            return true;
        }

        if (label.equalsIgnoreCase("removemision")) {
            if (!sender.hasPermission("viciont_hardcore3.missions.admin")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                return true;
            }

            if (args.length != 2) {
                sender.sendMessage(ChatColor.RED + "Uso: /removemision <jugador> <número>");
                return true;
            }

            try {
                String playerName = args[0];
                int missionNumber = Integer.parseInt(args[1]);
                missionHandler.removeMissionFromPlayer(sender, playerName, missionNumber);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "El número de misión debe ser válido.");
            }
            return true;
        }

        if (label.equalsIgnoreCase("penalizadosmisiones")) {
            if (!sender.hasPermission("viciont_hardcore3.missions.admin")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Solo jugadores pueden ejecutar este comando.");
                return true;
            }

            Player player = (Player) sender;
            int currentDay = missionHandler.getDayHandler().getCurrentDay();

            if (!missionHandler.getPenaltyDays().containsKey(currentDay)) {
                long nextPenaltyDay = missionHandler.getNextPenaltyDay(currentDay);
                player.sendMessage(ChatColor.of("#ff6666") + "⚠ Hoy no es un día de penalización.");
                player.sendMessage(ChatColor.of("#ffcc66") + "El próximo día de penalización será el día " + nextPenaltyDay + ".");
                return true;
            }

            missionHandler.getPendingPenaltyConfirm().put(player.getUniqueId(), currentDay);
            player.sendMessage(ChatColor.of("#ffaa00") + "Hoy (" + currentDay + ") es un día de penalización.");
            player.sendMessage(ChatColor.of("#ffcc66") + "Escribe 'confirma' para aplicar penalizaciones o 'cancelar' para abortar.");

            return true;
        }


        if (label.equalsIgnoreCase("misiones")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
                return true;
            }

            Player player = (Player) sender;
            missionGUI.openMissionGUI(player);
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("activarmision") ||
                command.getName().equalsIgnoreCase("desactivarmision")) {
            if (args.length == 1) {
                // Sugerir números de misión del 1 al 27
                for (int i = 1; i <= 27; i++) {
                    completions.add(String.valueOf(i));
                }
            }
        } else if (command.getName().equalsIgnoreCase("addmision") ||
                command.getName().equalsIgnoreCase("removemision")) {
            if (args.length == 1) {
                // Sugerir nombres de jugadores
                for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            } else if (args.length == 2) {
                // Sugerir números de misión
                for (int i = 1; i <= 27; i++) {
                    completions.add(String.valueOf(i));
                }
            }
        }

        // Filtrar completions basado en lo que el usuario ha escrito
        if (!args[args.length - 1].isEmpty()) {
            List<String> filtered = new ArrayList<>();
            StringUtil.copyPartialMatches(args[args.length - 1], completions, filtered);
            Collections.sort(filtered);
            return filtered;
        }

        return completions;
    }
}