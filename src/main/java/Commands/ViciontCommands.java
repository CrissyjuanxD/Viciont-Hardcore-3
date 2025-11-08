package Commands;

import Handlers.DayHandler;
import Handlers.DeathStormHandler;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class ViciontCommands implements CommandExecutor, TabCompleter {

    private final Plugin plugin;
    private final DeathStormHandler deathStormHandler;
    private final DayHandler dayHandler;

    public ViciontCommands(Plugin plugin, DeathStormHandler deathStormHandler, DayHandler dayHandler) {
        this.plugin = plugin;
        this.deathStormHandler = deathStormHandler;
        this.dayHandler = dayHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Solo los jugadores pueden usar este comando.");
            return true;
        }

        // /viciont
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {

            case "visibledeathstorm" -> {
                deathStormHandler.togglePlayerVisibility(player);
                return true;
            }

            case "dia" -> {
                int currentDay = dayHandler.getCurrentDay();
                player.sendMessage(ChatColor.of("#e5c0ff") + "🌅 Actualmente estamos en el día "
                        + ChatColor.of("#fd8698") + currentDay + ChatColor.of("#e5c0ff") + " de Viciont Hardcore 3.");
                return true;
            }

            case "ping" -> {
                int ping = player.getPing();
                player.sendMessage(ChatColor.of("#b58eff") + "☁ Tu ping es de: " +
                        ChatColor.of("#fd8698") + ping + "ms");
                return true;
            }

            case "help" -> {
                sendHelp(player);
                return true;
            }

            default -> {
                player.sendMessage(ChatColor.of("#fd8698") + "❌ Subcomando desconocido. Usa " + ChatColor.of("#d37af0") + "/viciont help");
                return true;
            }
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.of("#d37af0") + "╔══════════════╗");
        player.sendMessage(ChatColor.of("#fd8698") + "   ☁ Comandos Viciont ☁");
        player.sendMessage(ChatColor.of("#d37af0") + "╚══════════════╝");
        player.sendMessage(ChatColor.of("#dfc3c9") + " /viciont visibledeathstorm " +
                ChatColor.of("#fd8698") + "→ " + ChatColor.of("#d37af0") + "Activa o desactiva el timer de la DeathStorm.");
        player.sendMessage(ChatColor.of("#dfc3c9") + " /viciont dia " +
                ChatColor.of("#fd8698") + "→ " + ChatColor.of("#d37af0") + "Muestra el día actual del servidor.");
        player.sendMessage(ChatColor.of("#dfc3c9") + " /viciont ping " +
                ChatColor.of("#fd8698") + "→ " + ChatColor.of("#d37af0") + "Muestra tu ping actual.");
    }

    // ======= Autocompletado =======

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("visibledeathstorm");
            completions.add("dia");
            completions.add("ping");
            completions.add("help");
        }

        return completions;
    }
}
