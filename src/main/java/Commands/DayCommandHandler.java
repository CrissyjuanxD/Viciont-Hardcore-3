package Commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vct.hardcore3.DeathStormHandler;

public class DayCommandHandler implements CommandExecutor {
    private final DeathStormHandler deathStormHandler;

    public DayCommandHandler(DeathStormHandler deathStormHandler) {
        this.deathStormHandler = deathStormHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("cambiardia") && args.length == 1) {
            if (sender.isOp()) {
                int day = Integer.parseInt(args[0]);
                deathStormHandler.changeDay(day);
                sender.sendMessage(ChatColor.GREEN + "Día cambiado a " + day + ".");
            } else {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            }
        } else if (label.equalsIgnoreCase("dia")) {
            int currentDay = deathStormHandler.getCurrentDay();
            sender.sendMessage(ChatColor.GOLD + "Estamos en el día " + currentDay + " de Viciont Hardcore 3.");
        } else {
            return false;
        }
        return true;
    }
}

