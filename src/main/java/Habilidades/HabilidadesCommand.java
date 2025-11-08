package Habilidades;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HabilidadesCommand implements CommandExecutor, TabCompleter {

    private final HabilidadesManager manager;

    public HabilidadesCommand(HabilidadesManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.of("#FF6B6B") + "Uso: /habilidades <jugador>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.of("#FF6B6B") + "Jugador no encontrado.");
            return true;
        }

        Map<HabilidadesType, List<Integer>> habilidades = manager.getPlayerHabilidades(target.getUniqueId());

        sender.sendMessage("");
        sender.sendMessage(ChatColor.of("#C77DFF") + "" + ChatColor.BOLD + "═══════════════════════════");
        sender.sendMessage(ChatColor.of("#E0AAFF") + "" + ChatColor.BOLD + "Habilidades de " + target.getName());
        sender.sendMessage(ChatColor.of("#C77DFF") + "" + ChatColor.BOLD + "═══════════════════════════");
        sender.sendMessage("");

        if (habilidades.isEmpty()) {
            sender.sendMessage(ChatColor.of("#9D4EDD") + "Este jugador no tiene habilidades desbloqueadas.");
        } else {
            for (HabilidadesType type : HabilidadesType.values()) {
                if (habilidades.containsKey(type)) {
                    List<Integer> levels = habilidades.get(type);
                    String levelsStr = levels.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", "));

                    sender.sendMessage(ChatColor.of("#C77DFF") + "• " + type.getDisplayName() + ": " +
                            ChatColor.of("#E0AAFF") + "Niveles " + levelsStr);
                }
            }
        }

        sender.sendMessage("");
        sender.sendMessage(ChatColor.of("#C77DFF") + "" + ChatColor.BOLD + "═══════════════════════════");
        sender.sendMessage("");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
