package TitleListener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DiscoCommand implements Listener, CommandExecutor {

    private final JavaPlugin plugin;
    private final Set<Player> activeDiscoPlayers = new HashSet<>();
    private final Map<Player, BukkitTask> discoTasks = new HashMap<>();
    private final ChatColor[] colors = {
            ChatColor.AQUA, ChatColor.RED, ChatColor.GREEN, ChatColor.YELLOW, ChatColor.DARK_PURPLE,
    };

    public DiscoCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("playdisco")) {
            handleDiscoCommand(sender, true);
            return true;
        }

        if (label.equalsIgnoreCase("stopdisco")) {
            handleDiscoCommand(sender, false);
            return true;
        }

        return false;
    }

    private void handleDiscoCommand(CommandSender sender, boolean start) {
        if (sender instanceof ConsoleCommandSender) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (start) {
                    if (!activeDiscoPlayers.contains(player)) {
                        activeDiscoPlayers.add(player);
                        startDiscoTask(player);
                    }
                } else {
                    if (activeDiscoPlayers.contains(player)) {
                        activeDiscoPlayers.remove(player);
                        stopDiscoTask(player);
                    }
                }
            }
            sender.sendMessage(ChatColor.GREEN + "El modo disco ha sido " + (start ? "habilitado" : "deshabilitado") + " para todos los jugadores en línea.");
        } else if (sender instanceof Player) {
            Player player = (Player) sender;

            if (start) {
                if (activeDiscoPlayers.contains(player)) {
                    player.sendMessage(ChatColor.RED + "El modo disco ya está activo.");
                    return;
                }

                activeDiscoPlayers.add(player);
                startDiscoTask(player);
                player.sendMessage(ChatColor.GREEN + "El modo disco ha sido habilitado.");
            } else {
                if (!activeDiscoPlayers.contains(player)) {
                    player.sendMessage(ChatColor.RED + "El modo disco ya está desactivado.");
                    return;
                }

                activeDiscoPlayers.remove(player);
                stopDiscoTask(player);
                player.sendMessage(ChatColor.GREEN + "El modo disco ha sido deshabilitado.");
            }
        }
    }

    private void startDiscoTask(Player player) {
        BukkitTask task = new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (!activeDiscoPlayers.contains(player)) {
                    this.cancel();
                    return;
                }

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(colors[index] + "\uEAA5"));
                index = (index + 1) % colors.length;
            }
        }.runTaskTimer(plugin, 0L, 10L);

        discoTasks.put(player, task);
    }

    private void stopDiscoTask(Player player) {
        BukkitTask task = discoTasks.remove(player);

        if (task != null) {
            task.cancel();
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
    }
}
