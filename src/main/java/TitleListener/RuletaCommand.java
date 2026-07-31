package TitleListener;

import Gui.CambiosDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RuletaCommand implements CommandExecutor, TabCompleter {
    private final RuletaAnimation ruletaAnimation;
    private final CambiosDataManager cambiosData;
    private final List<String> posKeywords = Arrays.asList("center", "topleft", "topright", "bottomleft", "bottomright");
    private final List<String> tiposValidos = Arrays.asList("cambio", "estructura", "anuncio", "evento");

    public RuletaCommand(RuletaAnimation ruletaAnimation, CambiosDataManager cambiosData) {
        this.ruletaAnimation = ruletaAnimation;
        this.cambiosData = cambiosData;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof BlockCommandSender) && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Sin permisos.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /ruletavct <color> [tipo] [mode] [pos] [mensaje]");
            return true;
        }

        String color = args[0].toLowerCase();

        // Comprobar si se usó un "tipo" para la GUI de cambios
        boolean guardarEnGui = false;
        String tipoGui = "";
        int nextArgIndex = 1;

        if (args.length > 1 && tiposValidos.contains(args[1].toLowerCase())) {
            guardarEnGui = true;
            tipoGui = args[1].toLowerCase();
            nextArgIndex = 2;
        }

        String mode = "off";
        String pos = "center";

        if (!Arrays.asList("verde", "naranja", "morado", "rosa").contains(color)) {
            sender.sendMessage(ChatColor.RED + "Color inválido.");
            return true;
        }

        // Validar Modo si es rosa
        if (color.equals("rosa") && args.length > nextArgIndex) {
            if (args[nextArgIndex].equalsIgnoreCase("off") || args[nextArgIndex].equalsIgnoreCase("evento")) {
                mode = args[nextArgIndex].toLowerCase();
                nextArgIndex++;
            }
        }

        // Validar Posición
        if (args.length > nextArgIndex && posKeywords.contains(args[nextArgIndex].toLowerCase())) {
            pos = args[nextArgIndex].toLowerCase();
            nextArgIndex++;
        }

        // Capturar Mensaje
        StringBuilder msgBuilder = new StringBuilder();
        for (int i = nextArgIndex; i < args.length; i++) {
            msgBuilder.append(args[i]).append(" ");
        }
        String rawMessage = msgBuilder.toString().trim();

        String jsonMessage = "";
        if (!rawMessage.isEmpty()) {
            if (rawMessage.startsWith("[") || rawMessage.startsWith("{")) {
                jsonMessage = rawMessage;
                if (guardarEnGui && cambiosData != null) {
                    cambiosData.addCambio(tipoGui, rawMessage, jsonMessage);
                }
            } else {
                if (guardarEnGui) {
                    jsonMessage = buildGuiJsonTemplate(tipoGui, rawMessage);
                    if (cambiosData != null) {
                        cambiosData.addCambio(tipoGui, rawMessage, jsonMessage);
                    }
                } else {
                    jsonMessage = buildOldJsonTemplate(color, rawMessage);
                }
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Pasamos 'guardarEnGui' como nuevo parámetro
            ruletaAnimation.playAnimation(player, color, mode, pos, jsonMessage, guardarEnGui);
        }

        return true;
    }

    private String buildGuiJsonTemplate(String tipo, String rawMessage) {
        String title = tipo.substring(0, 1).toUpperCase() + tipo.substring(1).toLowerCase();
        String iconColor = "";
        String textColor = "";

        switch (tipo.toLowerCase()) {
            case "anuncio" -> { iconColor = "#7AEA6B"; textColor = "#9FF0C8"; }
            case "cambio" -> { iconColor = "#E99D41"; textColor = "#F0CC90"; }
            case "estructura" -> { iconColor = "#6E02A5"; textColor = "#A175D6"; }
            case "evento" -> { iconColor = "#F977F9"; textColor = "#AE78C6"; }
        }

        String escapedMsg = rawMessage.replace("\"", "\\\"");

        return "[\"\",{" +
                "\"text\":\"\\u06de " + title + " \",\"bold\":true,\"color\":\"" + iconColor + "\"},{" +
                "\"text\":\"\\u25ba\",\"bold\":true,\"color\":\"gray\"},{" +
                "\"text\":\"\\n\\n\"},{" +
                "\"text\":\" " + escapedMsg + " \",\"color\":\"" + textColor + "\"},{" +
                "\"text\":\"\\n \"}" +
                "]";
    }

    private String buildOldJsonTemplate(String color, String rawMessage) {
        String title = "";
        String iconColor = "";
        String textColor = "";

        switch (color) {
            case "verde" -> { title = "Anuncio"; iconColor = "#7AEA6B"; textColor = "#9FF0C8"; }
            case "naranja" -> { title = "Dificultad/Desafío"; iconColor = "#E99D41"; textColor = "#F0CC90"; }
            case "morado" -> { title = "Estructura"; iconColor = "#6E02A5"; textColor = "#A175D6"; }
            case "rosa" -> { title = "Evento"; iconColor = "#F977F9"; textColor = "#AE78C6"; }
        }

        String escapedMsg = rawMessage.replace("\"", "\\\"");

        return "[\"\",{" +
                "\"text\":\"\\u06de " + title + " \",\"bold\":true,\"color\":\"" + iconColor + "\"},{" +
                "\"text\":\"\\u25ba\",\"bold\":true,\"color\":\"gray\"},{" +
                "\"text\":\"\\n\\n\"},{" +
                "\"text\":\" " + escapedMsg + " \",\"color\":\"" + textColor + "\"},{" +
                "\"text\":\"\\n \"}" +
                "]";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("verde", "naranja", "morado", "rosa"), completions);
        } else if (args.length == 2) {
            List<String> opciones = new ArrayList<>(tiposValidos);
            if (args[0].equalsIgnoreCase("rosa")) opciones.addAll(Arrays.asList("off", "evento"));
            else opciones.addAll(posKeywords);

            StringUtil.copyPartialMatches(args[1], opciones, completions);
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("rosa")) {
                List<String> opciones = new ArrayList<>(posKeywords);
                opciones.addAll(Arrays.asList("off", "evento"));
                StringUtil.copyPartialMatches(args[2], opciones, completions);
            } else {
                StringUtil.copyPartialMatches(args[2], posKeywords, completions);
            }
        }

        return completions;
    }
}