package Habilidades;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HabilidadesCommand implements CommandExecutor, TabCompleter {

    private final HabilidadesManager manager;
    private final HabilidadesEffects effects;

    public HabilidadesCommand(HabilidadesManager manager, HabilidadesEffects effects) {
        this.manager = manager;
        this.effects = effects;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            if (args.length != 2) {
                sender.sendMessage(ChatColor.RED + "Uso: /habilidades list <jugador>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                showSkills(sender, target);
            } else {
                sender.sendMessage(ChatColor.RED + "Jugador no encontrado o desconectado.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off")) {
            if (!sender.hasPermission("viciont.admin")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso.");
                return true;
            }
            if (args.length != 2) {
                sender.sendMessage(ChatColor.RED + "Uso: /habilidades " + args[0] + " <jugador/GLOBAL>");
                return true;
            }

            String targetName = args[1];

            if (targetName.equalsIgnoreCase("global")) {
                if (args[0].equalsIgnoreCase("off")) {
                    manager.setGlobalDisabled(true);
                    sender.sendMessage(ChatColor.YELLOW + "Habilidades DESACTIVADAS globalmente.");
                } else {
                    manager.setGlobalDisabled(false);
                    sender.sendMessage(ChatColor.GREEN + "Habilidades ACTIVADAS globalmente.");
                }

                for (Player p : Bukkit.getOnlinePlayers()) {
                    effects.reapplyAllEffects(p, manager);
                }
                return true;
            }

            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                return true;
            }

            if (args[0].equalsIgnoreCase("off")) {
                manager.disableHabilidades(target);
                sender.sendMessage(ChatColor.YELLOW + "Habilidades DESACTIVADAS para " + target.getName());
            } else {
                manager.enableHabilidades(target);
                sender.sendMessage(ChatColor.GREEN + "Habilidades ACTIVADAS para " + target.getName());
            }

            effects.reapplyAllEffects(target, manager);
            return true;
        }

        if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {
            if (!sender.hasPermission("viciont.admin")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso.");
                return true;
            }

            if (args.length != 4) {
                sender.sendMessage(ChatColor.RED + "Uso: /habilidades " + args[0] + " <jugador> <TIPO> <nivel>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                return true;
            }

            HabilidadesType type;
            try {
                type = HabilidadesType.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Tipo inválido (VITALIDAD, RESISTENCIA, AGILIDAD).");
                return true;
            }

            int level;
            try {
                level = Integer.parseInt(args[3]);
                if (level < 1 || level > 4) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Nivel debe ser del 1 al 4.");
                return true;
            }

            if (args[0].equalsIgnoreCase("add")) {
                manager.unlockHabilidad(target.getUniqueId(), type, level);
                sender.sendMessage(ChatColor.GREEN + "Habilidad añadida a " + target.getName());
            } else {
                manager.removeHabilidad(target.getUniqueId(), type, level);
                sender.sendMessage(ChatColor.GREEN + "Habilidad removida de " + target.getName());
            }
            effects.reapplyAllEffects(target, manager);
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Uso: /habilidades list <jugador>");
        if (sender.hasPermission("viciont.admin")) {
            sender.sendMessage(ChatColor.RED + "Uso admin: /habilidades <add/remove> <jugador> <TIPO> <nivel>");
            sender.sendMessage(ChatColor.RED + "Uso admin: /habilidades <on/off> <jugador/GLOBAL>");
        }
    }

    private void showSkills(CommandSender sender, Player target) {
        Map<HabilidadesType, List<Integer>> habilidades = manager.getPlayerHabilidades(target.getUniqueId());

        boolean isGlobalDisabled = manager.isGlobalDisabled();
        boolean isPlayerDisabled = manager.areHabilidadesDisabled(target.getUniqueId()) && !isGlobalDisabled;

        String status;
        if (isGlobalDisabled) {
            status = ChatColor.RED + "Desactivadas (GLOBAL)";
        } else if (isPlayerDisabled) {
            status = ChatColor.RED + "Desactivadas (Personal)";
        } else {
            status = ChatColor.GREEN + "Activadas";
        }

        sender.sendMessage("");
        sender.sendMessage(ChatColor.of("#C77DFF") + "" + ChatColor.BOLD + "═══════════════════════════");
        sender.sendMessage(ChatColor.of("#E0AAFF") + "" + ChatColor.BOLD + "Habilidades de " + target.getName());
        sender.sendMessage(ChatColor.of("#C77DFF") + "Estado: " + status);
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
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("list");
            if (sender.hasPermission("viciont.admin")) {
                list.add("add");
                list.add("remove");
                list.add("on");
                list.add("off");
            }
            return StringUtil.copyPartialMatches(args[0], list, new ArrayList<>());
        }
        if (args.length == 2) {
            List<String> list = new ArrayList<>();
            if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {
                for (Player p : Bukkit.getOnlinePlayers()) list.add(p.getName());
            } else if (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off")) {
                list.add("GLOBAL");
                for (Player p : Bukkit.getOnlinePlayers()) list.add(p.getName());
            }
            return StringUtil.copyPartialMatches(args[1], list, new ArrayList<>());
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            List<String> types = new ArrayList<>();
            for (HabilidadesType t : HabilidadesType.values()) types.add(t.name());
            return StringUtil.copyPartialMatches(args[2], types, new ArrayList<>());
        }
        if (args.length == 4 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            return List.of("1", "2", "3", "4");
        }
        return new ArrayList<>();
    }
}