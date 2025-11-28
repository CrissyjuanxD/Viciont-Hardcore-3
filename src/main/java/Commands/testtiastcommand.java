package Commands;

import Handlers.ToastHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class testtiastcommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final ToastHandler toastHandler;

    public testtiastcommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.toastHandler = new ToastHandler(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo lo puede usar un jugador.");
            return true;
        }

        Player player = (Player) sender;

        // Parámetros de prueba:
        String title = "TEST TOAST";
        String description = "Esto es una prueba de toast";
        String iconMaterial = "minecraft:stone";

        toastHandler.sendToast(player, title, description, iconMaterial);

        player.sendMessage("Se ha enviado el toast de prueba."); // Mensaje opcional en chat

        return true;
    }
}
