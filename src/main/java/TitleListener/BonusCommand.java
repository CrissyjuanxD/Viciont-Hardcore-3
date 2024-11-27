package TitleListener;

import org.bukkit.Bukkit;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BonusCommand implements CommandExecutor {
    private final BonusAnimation bonusAnimation;

    public BonusCommand(BonusAnimation bonusAnimation) {
        this.bonusAnimation = bonusAnimation;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof BlockCommandSender) && !sender.isOp()) {
            sender.sendMessage("Este comando solo puede ser ejecutado por operadores o desde un bloque de comandos.");
            return true;
        }

        String jsonMessage = args.length > 0 ? String.join(" ", args) : "";

        for (Player player : Bukkit.getOnlinePlayers()) {
            bonusAnimation.playAnimation(player, jsonMessage);
        }

        return true;
    }
}
