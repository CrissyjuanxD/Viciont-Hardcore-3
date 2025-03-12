package Commands;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import vct.hardcore3.ViciontHardcore3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReviveCommand implements CommandExecutor, TabCompleter {
    private final ViciontHardcore3 plugin;

    public ReviveCommand(ViciontHardcore3 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Debes especificar el nombre del jugador a revivir.");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "El jugador especificado no está en línea.");
            return true;
        }

        FileConfiguration config = plugin.getConfig();
        if (!config.contains("revive.x") || !config.contains("revive.y") || !config.contains("revive.z")) {
            player.sendMessage(ChatColor.RED + "Las coordenadas de revive no han sido configuradas. Usa /revivecoords <x> <y> <z>.");
            return true;
        }

        double x = config.getDouble("revive.x");
        double y = config.getDouble("revive.y");
        double z = config.getDouble("revive.z");

        Location reviveLocation = new Location(target.getWorld(), x, y, z);

        // Sonidos iniciales
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.playSound(online.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 20f, 0.2f);
                online.playSound(online.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 25f, 0.5f);
            }
        }, 10L);

        // Teletransportar y cambiar clima
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            target.teleport(reviveLocation);
            target.getWorld().setStorm(true);
        }, 60L);

        // Mensaje y efectos visuales con título
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendTitle(ChatColor.WHITE + "\uE072",
                        ChatColor.AQUA + "El jugador " + ChatColor.GOLD + target.getName(),
                        20, 50, 20);
            }
        }, 61L);

        // Sonidos de revive
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            target.getWorld().playSound(target.getLocation(), Sound.ITEM_TOTEM_USE, 5f, 0.6f);
            target.getWorld().playSound(target.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 5f, 2f);
        }, 62L);

        // Efectos de poción
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 300, 4));
            target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 300, 5));
        }, 63L);

        // Cambio de modo de juego
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            target.setGameMode(GameMode.SURVIVAL);
        }, 83L);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("revive") && args.length == 1) {
            List<String> playerNames = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                playerNames.add(p.getName());
            }
            return playerNames;
        }
        return Collections.emptyList();
    }
}
