package InfestedCaves;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
        // Comando único para el portal
        if (cmd.getName().equalsIgnoreCase("infestedPortal")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;

            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("remove")) {
                    portalManager.removePortalNearby(p.getLocation());
                    p.sendMessage(ChatColor.RED + "Portal eliminado.");
                } else {
                    // Cualquier otro argumento (o ninguno, si configuras default) lo crea
                    portalManager.spawnPortal(p.getLocation());
                    p.sendMessage(ChatColor.GREEN + "Portal creado. (Funciona en ambas direcciones)");
                }
            } else {
                // Sin argumentos -> crear
                portalManager.spawnPortal(p.getLocation());
                p.sendMessage(ChatColor.GREEN + "Portal creado. (Funciona en ambas direcciones)");
            }
            return true;
        }

        // Comando Admin /InfestedCave
        if (cmd.getName().equalsIgnoreCase("InfestedCave")) {
            if (args.length < 2) return false;
            String action = args[0];
            String target = args[1];

            Player pTarget = null;
            if (target.equalsIgnoreCase("@a")) {
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
            if (Bukkit.getWorld(ViciontHardcore3.WORLD_NAME) == null) {
                p.sendMessage(ChatColor.RED + "El mundo no está cargado.");
                return;
            }
            // Teletransporte directo para admins (usa la misma lógica de spawn seguro)
            // Nota: Aquí podrías exponer el método findSafeSpawn en PortalManager si quieres ser estricto,
            Location loc = portalManager.findSafeSpawn(Bukkit.getWorld(ViciontHardcore3.WORLD_NAME));
            p.teleport(loc);
            p.sendMessage(ChatColor.GREEN + "Teletransportado a Infested Caves.");

        } else if (action.equalsIgnoreCase("leave")) {
            p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            p.sendMessage(ChatColor.GREEN + "Enviado al spawn del Overworld.");
        }
    }
}