package Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PingCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PingCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser ejecutado por jugadores.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        // Obtener el tiempo actual en milisegundos
        long currentTime = System.currentTimeMillis();

        // Verificar si el jugador está en cooldown
        if (cooldowns.containsKey(playerUUID)) {
            long lastUse = cooldowns.get(playerUUID);
            long timeLeft = (lastUse + 10000) - currentTime;

            if (timeLeft > 0) {
                player.sendMessage(ChatColor.GRAY + "Debes esperar " + (timeLeft / 1500) + " segundos antes de volver a usar /ping.");
                return true;
            }
        }

        // Obtener el ping del jugador
        int ping = player.getPing();
        player.sendMessage(ChatColor.GRAY + "Tu ping es de: " + ChatColor.LIGHT_PURPLE + ping + "ms.");

        // Registrar el uso del comando con la marca de tiempo actual
        cooldowns.put(playerUUID, currentTime);

        return true;
    }
}
