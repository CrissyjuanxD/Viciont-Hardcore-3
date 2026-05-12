package Events.MissionSystem;

import net.md_5.bungee.api.ChatColor;
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

public class MissionCommands implements CommandExecutor, TabCompleter {
    private final MissionHandler missionHandler;
    private final MissionGUI missionGUI;

    public MissionCommands(MissionHandler missionHandler, MissionGUI missionGUI) {
        this.missionHandler = missionHandler;
        this.missionGUI = missionGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("misiones")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
                return true;
            }
            missionGUI.openMissionGUI(player);
            return true;
        }

        if (label.equalsIgnoreCase("missions") || label.equalsIgnoreCase("mission")) {
            if (!sender.hasPermission("viciont_hardcore3.missions.admin")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                return true;
            }

            if (args.length == 0) {
                sendHelpMenu(sender);
                return true;
            }

            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "activar":
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Uso: /missions activar <número>");
                        return true;
                    }
                    try {
                        int missionNumber = Integer.parseInt(args[1]);
                        missionHandler.activateMission(sender, missionNumber);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "El número de misión debe ser válido.");
                    }
                    break;

                case "desactivar":
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Uso: /missions desactivar <número>");
                        return true;
                    }
                    try {
                        int missionNumber = Integer.parseInt(args[1]);
                        missionHandler.deactivateMission(sender, missionNumber);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "El número de misión debe ser válido.");
                    }
                    break;

                case "complete":
                    if (args.length != 3) {
                        sender.sendMessage(ChatColor.RED + "Uso: /missions complete <jugador> <número>");
                        return true;
                    }
                    try {
                        String playerName = args[1];
                        int missionNumber = Integer.parseInt(args[2]);
                        missionHandler.addMissionToPlayer(sender, playerName, missionNumber);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "El número de misión debe ser válido.");
                    }
                    break;

                case "remove":
                    if (args.length != 3) {
                        sender.sendMessage(ChatColor.RED + "Uso: /missions remove <jugador> <número>");
                        return true;
                    }
                    try {
                        String playerName = args[1];
                        int missionNumber = Integer.parseInt(args[2]);
                        missionHandler.removeMissionFromPlayer(sender, playerName, missionNumber);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "El número de misión debe ser válido.");
                    }
                    break;

                case "savedata":
                    sender.sendMessage(ChatColor.YELLOW + "Forzando guardado de datos de misiones...");
                    missionHandler.autoSaveAll();
                    sender.sendMessage(ChatColor.GREEN + "Datos guardados exitosamente en la base de datos.");
                    break;

                // --- NUEVOS SUBCOMANDOS DE PENALIZACIÓN ---
                case "applypenal":
                    missionHandler.applyPenaltyCommand(sender);
                    break;

                case "listpenal":
                    missionHandler.listPenalties(sender);
                    break;

                default:
                    sendHelpMenu(sender);
                    break;
            }
            return true;
        }

        return false;
    }

    private void sendHelpMenu(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Menú de Administración de Misiones ===");
        sender.sendMessage(ChatColor.YELLOW + "/missions activar <número> " + ChatColor.GRAY + "- Activa una misión globalmente.");
        sender.sendMessage(ChatColor.YELLOW + "/missions desactivar <número> " + ChatColor.GRAY + "- Desactiva una misión globalmente.");
        sender.sendMessage(ChatColor.YELLOW + "/missions complete <jugador> <número> " + ChatColor.GRAY + "- Completa forzosamente una misión a un jugador.");
        sender.sendMessage(ChatColor.YELLOW + "/missions remove <jugador> <número> " + ChatColor.GRAY + "- Reinicia la misión a un jugador.");
        sender.sendMessage(ChatColor.YELLOW + "/missions savedata " + ChatColor.GRAY + "- Guarda los datos de todos a la Base de Datos.");
        sender.sendMessage(ChatColor.of("#e06666") + "/missions applypenal " + ChatColor.GRAY + "- Aplica penalización del día actual.");
        sender.sendMessage(ChatColor.of("#e06666") + "/missions listpenal " + ChatColor.GRAY + "- Lista los jugadores penalizados hoy.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("missions") || command.getName().equalsIgnoreCase("mission")) {
            if (args.length == 1) {
                completions.addAll(Arrays.asList("activar", "desactivar", "complete", "remove", "savedata", "applypenal", "listpenal"));
            }
            else if (args.length == 2) {
                String sub = args[0].toLowerCase();
                if (sub.equals("activar") || sub.equals("desactivar")) {
                    for (int i = 1; i <= 36; i++) completions.add(String.valueOf(i));
                } else if (sub.equals("complete") || sub.equals("remove")) {
                    for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                }
            }
            else if (args.length == 3) {
                String sub = args[0].toLowerCase();
                if (sub.equals("complete") || sub.equals("remove")) {
                    for (int i = 1; i <= 36; i++) completions.add(String.valueOf(i));
                }
            }
        }

        if (!args[args.length - 1].isEmpty()) {
            List<String> filtered = new ArrayList<>();
            StringUtil.copyPartialMatches(args[args.length - 1], completions, filtered);
            Collections.sort(filtered);
            return filtered;
        }

        return completions;
    }
}