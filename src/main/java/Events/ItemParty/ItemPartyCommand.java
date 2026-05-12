package Events.ItemParty;

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
import java.util.stream.Collectors;

public class ItemPartyCommand implements CommandExecutor, TabCompleter {

    private final ItemPartyHandler itemPartyHandler;

    public ItemPartyCommand(ItemPartyHandler itemPartyHandler) {
        this.itemPartyHandler = itemPartyHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("viciont_hardcore3.command.itemparty")) {
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
                itemPartyHandler.iniciarEvento();
                sender.sendMessage(ChatColor.GREEN + "ItemParty iniciado.");
                break;
            case "end":
                itemPartyHandler.terminarEvento();
                sender.sendMessage(ChatColor.RED + "ItemParty terminado.");
                break;
            case "reset":
                itemPartyHandler.resetPlayersFile();
                sender.sendMessage(ChatColor.GREEN + "ItemParty: Lista de jugadores/castigos reseteada.");
                break;
            case "reload":
                itemPartyHandler.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "ItemParty: Configuración recargada.");
                break;
            case "castigo":
                if (args.length >= 3 && args[1].equalsIgnoreCase("remove")) {
                    String targetName = args[2];
                    boolean exito = itemPartyHandler.quitarCastigoManualmente(targetName);
                    if (exito) {
                        sender.sendMessage("§a[ItemParty] §fSe ha retirado el castigo al jugador §e" + targetName + "§f.");
                    } else {
                        sender.sendMessage("§c[ItemParty] §fEl jugador §e" + targetName + " §fno tiene un castigo activo o no existe en los registros.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Uso correcto: /itemparty castigo remove <jugador>");
                }
                break;
            default:
                sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Uso: /itemparty <start|end|reset|reload|castigo>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.asList("start", "end", "reset", "reload", "castigo");
            List<String> completions = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], options, completions);
            Collections.sort(completions);
            return completions;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("castigo")) {
            List<String> options = Collections.singletonList("remove");
            List<String> completions = new ArrayList<>();
            StringUtil.copyPartialMatches(args[1], options, completions);
            return completions;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("castigo") && args[1].equalsIgnoreCase("remove")) {
            List<String> options = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            List<String> completions = new ArrayList<>();
            StringUtil.copyPartialMatches(args[2], options, completions);
            return completions;
        }
        return Collections.emptyList();
    }
}