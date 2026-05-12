package Events.AchievementParty;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AchievementCommands implements CommandExecutor, TabCompleter {
    private final AchievementPartyHandler achievementHandler;

    public AchievementCommands(AchievementPartyHandler achievementHandler) {
        this.achievementHandler = achievementHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("achievements.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        if (args.length < 1) {
            sendHelp(sender, label);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start":
                achievementHandler.startEvent(sender);
                return true;
            case "end":
                achievementHandler.endEvent(sender);
                return true;
            case "reset":
                achievementHandler.resetEvent(sender);
                return true;
            case "addlogro":
            case "removelogro":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /" + label + " " + subCommand + " <@a|jugador> <logro>");
                    return true;
                }

                String targetName = args[1];
                String achievementId = args[2];

                if (!achievementHandler.getAchievementIds().contains(achievementId)) {
                    sender.sendMessage(ChatColor.RED + "Logro no válido. Usa el autocompletado.");
                    return true;
                }

                if (targetName.equals("@a")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        handleLogro(sender, p, subCommand, achievementId);
                    }
                    sender.sendMessage(ChatColor.GREEN + "Acción aplicada a todos los jugadores online.");
                } else {
                    Player target = Bukkit.getPlayer(targetName);
                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "El jugador debe estar conectado para manipular sus logros por ahora.");
                        return true;
                    }
                    handleLogro(sender, target, subCommand, achievementId);
                }
                return true;
            default:
                sendHelp(sender, label);
                return true;
        }
    }

    private void handleLogro(CommandSender sender, Player target, String subCommand, String achievementId) {
        if (subCommand.equals("addlogro")) {
            // Simplemente lo completa directo, sin rellenar la lista de items.
            if (achievementHandler.completeAchievement(target, achievementId)) {
                sender.sendMessage(ChatColor.GREEN + "Logro " + achievementId + " completado administrativamente para " + target.getName());
            } else {
                sender.sendMessage(ChatColor.YELLOW + target.getName() + " ya tiene ese logro.");
            }
        } else if (subCommand.equals("removelogro")) {
            if (achievementHandler.removeAchievement(target, achievementId)) {
                sender.sendMessage(ChatColor.GREEN + "Logro " + achievementId + " removido de " + target.getName());
            } else {
                sender.sendMessage(ChatColor.YELLOW + target.getName() + " no tiene ese logro.");
            }
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.RED + "Uso: /" + label + " <start|end|reset|addlogro|removelogro> [jugador] [logro]");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("achievements.admin")) return Collections.emptyList();

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], java.util.Arrays.asList("start", "end", "reset", "addlogro", "removelogro"), completions);
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("addlogro") || args[0].equalsIgnoreCase("removelogro"))) {
            List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            names.add("@a");

            // Sugerir algunos offline recientes
            names.addAll(java.util.Arrays.stream(Bukkit.getServer().getOfflinePlayers())
                    .limit(20)
                    .map(OfflinePlayer::getName)
                    .filter(Objects::nonNull)
                    .toList());

            StringUtil.copyPartialMatches(args[1], names, completions);
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("addlogro") || args[0].equalsIgnoreCase("removelogro"))) {
            StringUtil.copyPartialMatches(args[2], achievementHandler.getAchievementIds(), completions);
        }

        Collections.sort(completions);
        return completions;
    }
}