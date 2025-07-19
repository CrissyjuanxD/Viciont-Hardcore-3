package Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import Handlers.DayHandler;

public class DayCommandHandler implements CommandExecutor {
    private final DayHandler dayHandler;

    public DayCommandHandler(DayHandler dayHandler) {
        this.dayHandler = dayHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("cambiardia") && args.length == 1) {
            if (sender.isOp()) {
                try {
                    int day = Integer.parseInt(args[0]);
                    dayHandler.changeDay(day);
                    sender.sendMessage(ChatColor.GREEN + "Día cambiado a " + day + ".");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Por favor ingresa un número válido.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            }
        } else if (label.equalsIgnoreCase("dia")) {
            int currentDay = dayHandler.getCurrentDay();
            sender.sendMessage(ChatColor.GOLD + "Estamos en el día " + currentDay + " de Viciont Hardcore 3.");
        } else {
            return false;
        }
        return true;
    }
}
