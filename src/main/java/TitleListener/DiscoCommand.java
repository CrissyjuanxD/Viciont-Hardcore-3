package TitleListener;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import net.md_5.bungee.api.ChatColor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DiscoCommand implements Listener, CommandExecutor {

    private final JavaPlugin plugin;
    private final Set<Player> activeDiscoPlayers = new HashSet<>();
    private final Map<Player, BukkitTask> discoTasks = new HashMap<>();
    private final Map<Player, BossBar> discoBossBars = new HashMap<>(); // Registro de las BossBars activas
    private final ChatColor[] colors = {
            ChatColor.AQUA, ChatColor.RED, ChatColor.GREEN, ChatColor.YELLOW, ChatColor.DARK_PURPLE,
    };

    public DiscoCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean isStart = label.equalsIgnoreCase("playdisco");

        // Si se pasa un jugador como argumento: /playdisco [jugador] o /stopdisco [jugador]
        if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                if (isStart) {
                    startDisco(target);
                    sender.sendMessage(ChatColor.GREEN + "Modo disco habilitado para " + target.getName() + ".");
                } else {
                    stopDisco(target);
                    sender.sendMessage(ChatColor.GREEN + "Modo disco deshabilitado para " + target.getName() + ".");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
            }
            return true;
        }

        // Comportamiento por defecto si no se especifica jugador
        handleDiscoCommand(sender, isStart);
        return true;
    }

    private void handleDiscoCommand(CommandSender sender, boolean start) {
        if (sender instanceof ConsoleCommandSender) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (start) {
                    startDisco(player);
                } else {
                    stopDisco(player);
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
                startDisco(player);
                player.sendMessage(ChatColor.GREEN + "El modo disco ha sido habilitado.");
            } else {
                if (!activeDiscoPlayers.contains(player)) {
                    player.sendMessage(ChatColor.RED + "El modo disco ya está desactivado.");
                    return;
                }
                stopDisco(player);
                player.sendMessage(ChatColor.GREEN + "El modo disco ha sido deshabilitado.");
            }
        }
    }


    public void startDisco(Player player) {
        if (player != null && !activeDiscoPlayers.contains(player)) {
            activeDiscoPlayers.add(player);
            startDiscoTask(player);
        }
    }

    public void stopDisco(Player player) {
        if (player != null && activeDiscoPlayers.contains(player)) {
            activeDiscoPlayers.remove(player);
            stopDiscoTask(player);
        }
    }

    // --- LÓGICA DE LA BOSSBAR ---

    private void startDiscoTask(Player player) {
        // Crear una BossBar blanca sin título inicial
        BossBar bossBar = Bukkit.createBossBar("", BarColor.WHITE, BarStyle.SOLID);
        bossBar.addPlayer(player);
        discoBossBars.put(player, bossBar);

        BukkitTask task = new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (!activeDiscoPlayers.contains(player)) {
                    this.cancel();
                    return;
                }

                // Cambiar el color del símbolo en el título de la BossBar
                bossBar.setTitle(colors[index] + "\uEAA5");
                index = (index + 1) % colors.length;
            }
        }.runTaskTimer(plugin, 0L, 10L);

        discoTasks.put(player, task);
    }

    private void stopDiscoTask(Player player) {
        // Cancelar el runnable
        BukkitTask task = discoTasks.remove(player);
        if (task != null) {
            task.cancel();
        }

        // Eliminar y ocultar la BossBar
        BossBar bossBar = discoBossBars.remove(player);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }
}