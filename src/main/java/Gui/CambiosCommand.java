package Gui;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CambiosCommand implements CommandExecutor, TabCompleter {
    private final CambiosDataManager dataManager;

    public CambiosCommand(CambiosDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("viciont.admin")) {
            sender.sendMessage(ChatColor.RED + "Sin permisos.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /cambios <borrar|editar> <id> [nuevo_mensaje]");
            return true;
        }

        String accion = args[0].toLowerCase();
        String id = args[1];

        if (accion.equals("borrar")) {
            if (dataManager.deleteCambio(id)) {
                sender.sendMessage(ChatColor.GREEN + "Cambio " + id + " borrado con éxito.");
            } else {
                sender.sendMessage(ChatColor.RED + "No se encontró el cambio " + id);
            }
            return true;
        }

        if (accion.equals("editar")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Debes proporcionar el nuevo mensaje.");
                return true;
            }

            CambiosDataManager.CambioEntry entry = dataManager.getEntradas().stream()
                    .filter(e -> e.id.equalsIgnoreCase(id)).findFirst().orElse(null);

            if (entry == null) {
                sender.sendMessage(ChatColor.RED + "No se encontró el cambio " + id);
                return true;
            }

            StringBuilder msgBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                msgBuilder.append(args[i]).append(" ");
            }
            String rawMessage = msgBuilder.toString().trim();

            // Reconstruimos el JSON usando la plantilla del RuletaCommand (sin necesidad de instanciarlo)
            String escapedMsg = rawMessage.replace("\"", "\\\"");
            String iconColor = switch (entry.tipo) {
                case "Anuncio" -> "#7AEA6B";
                case "Cambio" -> "#E99D41";
                case "Estructura" -> "#6E02A5";
                case "Evento" -> "#F977F9";
                default -> "#FFFFFF";
            };

            String jsonMessage = "[\"\",{" +
                    "\"text\":\"\\u06de " + entry.tipo + " \",\"bold\":true,\"color\":\"" + iconColor + "\"},{" +
                    "\"text\":\"\\u25ba\",\"bold\":true,\"color\":\"gray\"},{" +
                    "\"text\":\"\\n\\n\"},{" +
                    "\"text\":\" " + escapedMsg + " \",\"color\":\"" + entry.colorCode + "\"},{" +
                    "\"text\":\"\\n \"}]";

            dataManager.editCambio(id, rawMessage, jsonMessage);
            sender.sendMessage(ChatColor.GREEN + "Mensaje de " + id + " actualizado correctamente.");
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("borrar", "editar"), completions);
        } else if (args.length == 2) {
            List<String> ids = dataManager.getEntradas().stream().map(e -> e.id).collect(Collectors.toList());
            StringUtil.copyPartialMatches(args[1], ids, completions);
        } else if (args.length >= 3 && args[0].equalsIgnoreCase("editar")) {
            // Autocompletar con el mensaje actual si es posible para facilitar la edición
            CambiosDataManager.CambioEntry entry = dataManager.getEntradas().stream()
                    .filter(e -> e.id.equalsIgnoreCase(args[1])).findFirst().orElse(null);

            if (entry != null && args.length == 3) {
                completions.add(entry.rawMessage);
            }
        }
        return completions;
    }
}