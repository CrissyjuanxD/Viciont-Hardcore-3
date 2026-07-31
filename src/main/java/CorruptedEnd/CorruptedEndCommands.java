package CorruptedEnd;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class CorruptedEndCommands implements CommandExecutor {
    private final JavaPlugin plugin;
    private final PortalManager portalManager;

    public CorruptedEndCommands(JavaPlugin plugin, PortalManager portalManager) {
        this.plugin = plugin;
        this.portalManager = portalManager;
    }

    public void registerCommands() {
        plugin.getCommand("spawnportalce").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Solo jugadores.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("corruptedend.spawnportal")) {
            player.sendMessage(ChatColor.RED + "Sin permisos.");
            return true;
        }

        Location portalLocation;

        if (args.length >= 3) {
            try {
                int x = Integer.parseInt(args[0]);
                int y = Integer.parseInt(args[1]);
                int z = Integer.parseInt(args[2]);
                portalLocation = new Location(player.getWorld(), x, y, z);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Coordenadas inválidas.");
                return true;
            }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("random")) {
            World world = player.getWorld();
            Random random = new Random();
            int x = random.nextInt(5000) - 4000;
            int y = random.nextInt(100) + 100;
            int z = random.nextInt(5000) - 4000;
            portalLocation = new Location(world, x, y, z);
        } else if (args.length == 0) {
            portalLocation = player.getLocation().clone().add(0, 1, 0);
        } else {
            player.sendMessage(ChatColor.RED + "Uso: /spawnportalce [<x> <y> <z> | random]");
            return true;
        }

        if (portalLocation.getWorld().getName().equals(CorruptedEnd.WORLD_NAME)) {
            player.sendMessage(ChatColor.RED + "No puedes crear un portal dentro del Corrupted End.");
            return true;
        }

        portalManager.createOverworldPortal(portalLocation);

        player.sendMessage(ChatColor.GREEN + "Portal del Corrupted End creado en " +
                portalLocation.getBlockX() + ", " + portalLocation.getBlockY() + ", " + portalLocation.getBlockZ());

        if (player.getLocation().distance(portalLocation) > 50) {
            player.teleport(portalLocation);
        }

        return true;
    }
}