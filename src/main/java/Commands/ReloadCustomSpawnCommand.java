package Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ReloadCustomSpawnCommand implements CommandExecutor {
    private final CustomSpawnerHandler spawnerHandler;

    public ReloadCustomSpawnCommand(CustomSpawnerHandler spawnerHandler) {
        this.spawnerHandler = spawnerHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser ejecutado por jugadores.");
            return true;
        }

        Player player = (Player) sender;

        // Limpiar spawners existentes en el mapa
        spawnerHandler.clearActiveSpawners();

        // Escanear y recargar todos los spawners
        spawnerHandler.loadAllCustomSpawners();

        player.sendMessage(ChatColor.GREEN + "Todos los spawners custom han sido recargados correctamente.");
        return true;
    }
}
