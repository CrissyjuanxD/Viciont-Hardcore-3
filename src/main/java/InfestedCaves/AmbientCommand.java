package InfestedCaves;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import vct.hardcore3.ViciontHardcore3;

import java.util.ArrayList;
import java.util.List;

public class AmbientCommand implements CommandExecutor, TabCompleter {

    private final InfestedCaveAmbient ambientManager;

    public AmbientCommand(InfestedCaveAmbient ambientManager) {
        this.ambientManager = ambientManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("viciont.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /icambient <clown/daisy/common> <jugador/@a>");
            return true;
        }

        String typeStr = args[0].toLowerCase();
        String targetStr = args[1];

        InfestedCaveAmbient.EventType type;
        switch (typeStr) {
            case "clown": type = InfestedCaveAmbient.EventType.CLOWN; break;
            case "daisy": type = InfestedCaveAmbient.EventType.DAISY; break;
            case "common": type = InfestedCaveAmbient.EventType.COMMON; break;
            default:
                sender.sendMessage(ChatColor.RED + "Tipo inválido. Usa: clown, daisy o common");
                return true;
        }

        List<Player> targets = new ArrayList<>();
        if (targetStr.equalsIgnoreCase("@a")) {
            targets.addAll(Bukkit.getOnlinePlayers());
        } else {
            Player p = Bukkit.getPlayer(targetStr);
            if (p == null) {
                sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                return true;
            }
            targets.add(p);
        }

        int count = 0;
        for (Player p : targets) {
            if (p.getWorld().getName().equals(ViciontHardcore3.WORLD_NAME)) {
                ambientManager.forceStartEvent(p, type);
                count++;
            }
        }

        if (count > 0) {
            sender.sendMessage(ChatColor.GREEN + "Evento " + typeStr + " iniciado forzosamente para " + count + " jugador(es).");
        } else {
            sender.sendMessage(ChatColor.RED + "Ningún jugador objetivo está en Infested Caves.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("clown");
            completions.add("daisy");
            completions.add("common");
        } else if (args.length == 2) {
            completions.add("@a");
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }
        return completions;
    }
}