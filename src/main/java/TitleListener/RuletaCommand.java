package TitleListener;

import org.bukkit.Bukkit;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RuletaCommand implements CommandExecutor {
    private final RuletaAnimation ruletaAnimation;

    public RuletaCommand(RuletaAnimation ruletaAnimation) {
        this.ruletaAnimation = ruletaAnimation;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof BlockCommandSender) && !sender.isOp()) {
            sender.sendMessage("Este comando solo puede ser ejecutado por operadores o desde un bloque de comandos.");
            return true;
        }

        String jsonMessage = args.length > 0 ? String.join(" ", args) : "";

        for (Player player : Bukkit.getOnlinePlayers()) {
            ruletaAnimation.playAnimation(player, jsonMessage);
        }

        return true;
    }
}
