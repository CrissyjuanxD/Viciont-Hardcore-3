package Commands;

import Handlers.DatabaseManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class ViciontReloadCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;

    public ViciontReloadCommand(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("quaso.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar esto.");
            return true;
        }

        plugin.reloadConfig();
        databaseManager.reload();

        sender.sendMessage(ChatColor.GREEN + "§l[QuasoPlugin] §aConfiguración y Base de Datos recargadas correctamente.");
        return true;
    }
}