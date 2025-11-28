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

        String cmd = command.getName().toLowerCase();

        if (cmd.equals("levelnightmare")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Uso: /" + label + " <player|@a> <Nvl1/Nvl2/Nvl3>");
                return true;
            }
        } else {
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Uso: /" + label + " <player|@a>");
                return true;
            }
        }

        String targetArg = args[0];
        List<Player> targets = new ArrayList<>();

        if (targetArg.equalsIgnoreCase("@a")) {
            targets.addAll(Bukkit.getOnlinePlayers());
        } else {
            Player player = Bukkit.getPlayer(targetArg);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Jugador no encontrado");
                return true;
            }
            targets.add(player);
        }

        switch (cmd) {
            case "addnightmare":
                return handleAddNightmare(sender, targets);
            case "removenightmare":
                return handleRemoveNightmare(sender, targets);
            case "resetnightmarecooldown":
                return handleResetCooldown(sender, targets);
            case "levelnightmare":
                return handleLevelNightmare(sender, targets, args[1]);
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

    private boolean handleLevelNightmare(CommandSender sender, List<Player> targets, String levelArg) {
        int level = parseLevel(levelArg);
        if (level == -1) {
            sender.sendMessage(ChatColor.RED + "Nivel inválido. Usa 1, 2, 3 o Nvl1/Nvl2/Nvl3.");
            return true;
        }

        for (Player target : targets) {
            nightmareMechanic.setNightmareLevel(target.getUniqueId(), level);
        }

        if (targets.size() == 1) {
            sender.sendMessage(ChatColor.GREEN + "Nivel de pesadilla ajustado a " +
                    ChatColor.RED + level + ChatColor.GREEN + " para " + targets.get(0).getName());
        } else {
            sender.sendMessage(ChatColor.GREEN + "Nivel de pesadilla ajustado a " +
                    ChatColor.RED + level + ChatColor.GREEN + " para todos los jugadores seleccionados.");
        }
        return true;
    }

    private int parseLevel(String levelArg) {
        String s = levelArg.toLowerCase()
                .replace("nvl", "")
                .replace("lvl", "")
                .replace("nivel", "")
                .replace("nivel", "")
                .trim();

        if (s.equals("1")) return 1;
        if (s.equals("2")) return 2;
        if (s.equals("3")) return 3;

        return -1;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        String cmd = command.getName().toLowerCase();

        if (args.length == 1) {
            completions.add("@a");
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 2 && cmd.equals("levelnightmare")) {
            completions.add("Nvl1");
            completions.add("Nvl2");
            completions.add("Nvl3");
            completions.add("1");
            completions.add("2");
            completions.add("3");
        }

        return completions;
    }
}
