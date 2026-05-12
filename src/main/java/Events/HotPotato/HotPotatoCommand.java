package Events.HotPotato;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

public class HotPotatoCommand implements CommandExecutor, TabCompleter {

    private final HotPotatoHandler eventoHandler;

    public HotPotatoCommand(HotPotatoHandler eventoHandler) {
        this.eventoHandler = eventoHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("viciont_hardcore3.command.hotpotato")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso.");
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start":
                eventoHandler.iniciarEvento();
                sender.sendMessage(ChatColor.GREEN + "HotPotato: Evento iniciado.");
                break;
            case "force":
                eventoHandler.forzarTeletransporte();
                sender.sendMessage(ChatColor.GREEN + "HotPotato: Jugadores forzados a la arena.");
                break;
            case "reglas":
                eventoHandler.mostrarReglas();
                sender.sendMessage(ChatColor.GREEN + "HotPotato: Mostrando reglas en el chat...");
                break;
            case "battle":
                eventoHandler.iniciarBatalla();
                sender.sendMessage(ChatColor.GREEN + "HotPotato: ¡Comienza la batalla!");
                break;
            case "stop":
                eventoHandler.terminarEvento();
                sender.sendMessage(ChatColor.RED + "HotPotato: Evento detenido forzosamente.");
                break;
            case "list":
                eventoHandler.listParticipantes(sender);
                break;
            case "add":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /hotpotato add <jugador>");
                    return true;
                }
                eventoHandler.addParticipante(sender, args[1]);
                break;
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /hotpotato remove <jugador>");
                    return true;
                }
                eventoHandler.removeParticipante(sender, args[1]);
                break;
            case "reload":
                eventoHandler.crearYcargarConfig();
                sender.sendMessage(ChatColor.GREEN + "HotPotato: Archivo de configuración recargado exitosamente.");
                break;
            default:
                sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Uso: /hotpotato <start|force|reglas|battle|stop|list|add|remove|reload>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.asList("start", "force", "reglas", "battle", "stop", "list", "add", "remove", "reload");
            List<String> completions = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], options, completions);
            Collections.sort(completions);
            return completions;
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {
                List<String> playerNames = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    playerNames.add(p.getName());
                }
                return playerNames;
            }
        }
        return Collections.emptyList();
    }
}