package Commands;

import Dificultades.Features.NightmareMechanic;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class NightmareCommand implements CommandExecutor, TabCompleter {

    private final NightmareMechanic nightmareMechanic;
    private final JavaPlugin plugin;

    public NightmareCommand(JavaPlugin plugin, NightmareMechanic nightmareMechanic) {
        this.plugin = plugin;
        this.nightmareMechanic = nightmareMechanic;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /" + label + " <player|@a>");
            return false;
        }

        String target = args[0];
        List<Player> targets = new ArrayList<>();

        if (target.equalsIgnoreCase("@a")) {
            targets.addAll(Bukkit.getOnlinePlayers());
        } else {
            Player player = Bukkit.getPlayer(target);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Jugador no encontrado");
                return false;
            }
            targets.add(player);
        }

        switch (command.getName().toLowerCase()) {
            case "addnightmare":
                return handleAddNightmare(sender, targets);
            case "removenightmare":
                return handleRemoveNightmare(sender, targets);
            case "resetnightmarecooldown":
                return handleResetCooldown(sender, targets);
            default:
                return false;
        }
    }

    private boolean handleAddNightmare(CommandSender sender, List<Player> targets) {
        for (Player target : targets) {
            nightmareMechanic.forceStartNightmare(target.getUniqueId(), 1);
            target.sendMessage(ChatColor.RED + "¡Una pesadilla ha comenzado!");
        }

        if (targets.size() == 1) {
            sender.sendMessage(ChatColor.GREEN + "Pesadilla activada para " + targets.get(0).getName());
        } else {
            sender.sendMessage(ChatColor.GREEN + "Pesadilla activada para todos los jugadores");
        }
        return true;
    }

    private boolean handleRemoveNightmare(CommandSender sender, List<Player> targets) {
        for (Player target : targets) {
            nightmareMechanic.forceEndNightmare(target.getUniqueId());
            target.sendMessage(ChatColor.GREEN + "La pesadilla ha terminado");
        }

        if (targets.size() == 1) {
            sender.sendMessage(ChatColor.GREEN + "Pesadilla removida para " + targets.get(0).getName());
        } else {
            sender.sendMessage(ChatColor.GREEN + "Pesadilla removida para todos los jugadores");
        }
        return true;
    }

    private boolean handleResetCooldown(CommandSender sender, List<Player> targets) {
        for (Player target : targets) {
            nightmareMechanic.resetCooldown(target.getUniqueId());
            target.sendMessage(ChatColor.GREEN + "Cooldown de pesadilla reiniciado");
        }

        if (targets.size() == 1) {
            sender.sendMessage(ChatColor.GREEN + "Cooldown reiniciado para " + targets.get(0).getName());
        } else {
            sender.sendMessage(ChatColor.GREEN + "Cooldown reiniciado para todos los jugadores");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("@a");
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }

        return completions;
    }
}
