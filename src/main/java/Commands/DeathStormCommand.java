package Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import vct.hardcore3.DeathStormHandler;

public class DeathStormCommand implements CommandExecutor {
    private final DeathStormHandler deathStormHandler;

    public DeathStormCommand(DeathStormHandler deathStormHandler) {
        this.deathStormHandler = deathStormHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("resetdeathstorm")) {
            deathStormHandler.resetStorm();
            sender.sendMessage(ChatColor.GREEN + "DeathStorm has been reset.");
        } else if (label.equalsIgnoreCase("adddeathstorm") && args.length == 1) {
            int hours = Integer.parseInt(args[0]);
            deathStormHandler.addStormHours(hours);
            sender.sendMessage(ChatColor.GREEN + "Added " + hours + " hours to DeathStorm.");
        } else if (label.equalsIgnoreCase("removedeathstorm") && args.length == 1) {
            int hours = Integer.parseInt(args[0]);
            deathStormHandler.removeStormHours(hours);
            sender.sendMessage(ChatColor.GREEN + "Removed " + hours + " hours from DeathStorm.");
        } else {
            return false;
        }
        return true;
    }
}
