package Commands;

import Bosses.BaseBoss;
import Bosses.QueenBeeHandler;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class DebugArenaCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando solo puede usarse en el juego.");
            return true;
        }

        World w = player.getWorld();
        BaseBoss foundBoss = null;

        // Buscar cualquier boss activo basado en BaseBoss
        for (Entity e : w.getEntities()) {
            // Solo bosses del tipo QueenBee por ahora (puedes añadir más)
            if (QueenBeeHandler.ACTIVE_BOSSES.containsKey(e.getUniqueId())) {
                foundBoss = QueenBeeHandler.ACTIVE_BOSSES.get(e.getUniqueId());
                break;
            }
        }

        if (foundBoss == null) {
            player.sendMessage("§cNo hay ningún boss activo en este mundo.");
            return true;
        }

        foundBoss.toggleDebug(player);
        return true;
    }
}
