package InfestedCaves;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vct.hardcore3.ViciontHardcore3;

public class InfestedCommands implements CommandExecutor {
    private final ViciontHardcore3 plugin;
    private final PortalManager portalManager;

    public InfestedCommands(ViciontHardcore3 plugin, PortalManager portalManager) {
        this.plugin = plugin;
        this.portalManager = portalManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("infestedPortal")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;

            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("summon")) {
                    portalManager.spawnPortal(p.getLocation(), true); // true = hacia Infested
                    p.sendMessage("§aPortal hacia Infested Caves creado.");
                } else if (args[0].equalsIgnoreCase("remove")) {
                    portalManager.removePortalNearby(p.getLocation());
                    p.sendMessage("§cPortal eliminado.");
                }
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("infestedPortalOverworld")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;

            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("summon")) {
                    portalManager.spawnPortal(p.getLocation(), false); // false = hacia Overworld
                    p.sendMessage("§aPortal de regreso creado.");
                } else if (args[0].equalsIgnoreCase("remove")) {
                    portalManager.removePortalNearby(p.getLocation());
                    p.sendMessage("§cPortal eliminado.");
                }
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("InfestedForest")) { // Deje el nombre que pediste
            if (args.length < 2) return false;
            String action = args[0];
            String target = args[1];

            Player pTarget = null;
            if (target.equalsIgnoreCase("@a")) {
                // Lógica para todos (loop)
                for(Player p : Bukkit.getOnlinePlayers()) executeJoinLeave(p, action);
                sender.sendMessage("Ejecutado para todos.");
                return true;
            } else {
                pTarget = Bukkit.getPlayer(target);
            }

            if (pTarget != null) {
                executeJoinLeave(pTarget, action);
                sender.sendMessage("Ejecutado para " + pTarget.getName());
            }
            return true;
        }
        return false;
    }

    private void executeJoinLeave(Player p, String action) {
        if (action.equalsIgnoreCase("join")) {
            // Teleport manual logic
            portalManager.spawnPortal(p.getLocation(), true); // Hack rápido: o usar la logica directa de tp
            // Mejor usar lógica directa de teleport:
            Bukkit.getWorld(ViciontHardcore3.WORLD_NAME); // Asegurar carga
            p.teleport(new org.bukkit.Location(Bukkit.getWorld(ViciontHardcore3.WORLD_NAME), 500, 50, 500)); // Spawn random simplificado
        } else if (action.equalsIgnoreCase("leave")) {
            p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
    }
}