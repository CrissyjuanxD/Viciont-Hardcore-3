package TitleListener;

import org.bukkit.Bukkit;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MuerteCommand implements CommandExecutor {
    private final MuerteAnimation muerteAnimation;

    public MuerteCommand(MuerteAnimation muerteAnimation) {
        this.muerteAnimation = muerteAnimation;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof BlockCommandSender) && !sender.isOp()) {
            sender.sendMessage("Este comando solo puede ser ejecutado por operadores o desde un bloque de comandos.");
            return true;
        }

        // Convertir argumentos JSON si existen
        String jsonMessage = args.length > 0 ? String.join(" ", args) : "";

        // Reproducir la animación para todos los jugadores conectados
        for (Player player : Bukkit.getOnlinePlayers()) {
            muerteAnimation.playAnimation(player, jsonMessage);
        }

        return true;
    }
}

