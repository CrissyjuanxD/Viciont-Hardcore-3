package vct.hardcore3;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReviveCommand implements CommandExecutor {
    private ViciontHardcore3 plugin;
    private String Prefix = ChatColor.translateAlternateColorCodes('&', "&d&lViciont&5&lHardcore &5&l3&7➤ &f");

    public ReviveCommand(ViciontHardcore3 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Prefix + ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.isOp()) {
            player.sendMessage(Prefix + ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Prefix + ChatColor.RED + "Debes especificar el nombre del jugador a revivir.");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(Prefix + ChatColor.RED + "El jugador especificado no está en línea.");
            return true;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run playsound minecraft:block.fire.extinguish ambient @a ~ ~ ~ 20 0.2"), 10L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run playsound minecraft:block.bubble_column.whirlpool_ambient ambient @a ~ ~ ~ 25 0.5"), 10L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tp " + target.getName() + " 1321 68 2792 "), 60L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "weather thunder"), 60L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run title @a times 20 50 20"), 61L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run title @a title {\"text\":\"\uE072\"}"), 61L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run title @a subtitle [\"\",{\"text\":\"el jugador \",\"bold\":true,\"italic\":true,\"color\":\"aqua\"},{\"text\":\"" + target.getName() + "\",\"bold\":true,\"italic\":true,\"color\":\"gold\"}]"), 61L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run playsound minecraft:item.totem.use ambient @a ~ ~ ~ 5 0.6"), 62L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run playsound minecraft:item.trident.thunder ambient @a ~ ~ ~ 5 2"), 62L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect give " + target.getName() + " minecraft:slowness 15 4"), 63L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "effect give " + target.getName() + " minecraft:nausea 15 5"), 63L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamemode survival " + target.getName()), 83L);

        return true;
    }
}
