package TitleListener;

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
    private final List<String> posKeywords = Arrays.asList("center", "topleft", "topright", "bottomleft", "bottomright");

    public RuletaCommand(RuletaAnimation ruletaAnimation) {
        this.ruletaAnimation = ruletaAnimation;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof BlockCommandSender) && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Sin permisos.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /ruletavct <color> [mode] [pos] [mensaje]");
            return true;
        }

        String color = args[0].toLowerCase();
        String mode = "off";
        String pos = "center";
        int nextArgIndex = 1;

        // Validar Color
        if (!Arrays.asList("verde", "naranja", "morado", "rosa").contains(color)) {
            sender.sendMessage(ChatColor.RED + "Color inválido.");
            return true;
        }

        // Validar Modo si es rosa
        if (color.equals("rosa") && args.length > 1) {
            if (args[1].equalsIgnoreCase("off") || args[1].equalsIgnoreCase("evento")) {
                mode = args[1].toLowerCase();
                nextArgIndex = 2;
            }
        }

        // Validar Posición (opcional)
        if (args.length > nextArgIndex) {
            if (posKeywords.contains(args[nextArgIndex].toLowerCase())) {
                pos = args[nextArgIndex].toLowerCase();
                nextArgIndex++;
            }
        }

        // Capturar Mensaje Restante
        StringBuilder msgBuilder = new StringBuilder();
        for (int i = nextArgIndex; i < args.length; i++) {
            msgBuilder.append(args[i]).append(" ");
        }
        String rawMessage = msgBuilder.toString().trim();

        String jsonMessage = "";
        if (!rawMessage.isEmpty()) {
            if (rawMessage.startsWith("[") || rawMessage.startsWith("{")) {
                jsonMessage = rawMessage;
            } else {
                jsonMessage = buildJsonTemplate(color, rawMessage);
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            ruletaAnimation.playAnimation(player, color, mode, pos, jsonMessage);
        }

        return true;
    }

    private String buildJsonTemplate(String color, String rawMessage) {
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

        // Añadido espacio al principio y al final del mensaje (" " + escaped + " ")
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
            if (args[0].equalsIgnoreCase("rosa")) {
                StringUtil.copyPartialMatches(args[1], Arrays.asList("off", "evento"), completions);
            } else {
                StringUtil.copyPartialMatches(args[1], posKeywords, completions);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("rosa")) {
            StringUtil.copyPartialMatches(args[2], posKeywords, completions);
        }

        return completions;
    }
}