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
    private final LootManager lootManager;

    public CorruptedEndCommands(JavaPlugin plugin, PortalManager portalManager, LootManager lootManager) {
        this.plugin = plugin;
        this.portalManager = portalManager;
        this.lootManager = lootManager;
    }

    public void registerCommands() {
        plugin.getCommand("spawnportalce").setExecutor(this);
        plugin.getCommand("givelootable").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "spawnportalce":
                return handleSpawnPortal(sender, args);
            case "givelootable":
                return handleGiveLootable(sender, args);
        }
        return false;
    }

    private boolean handleSpawnPortal(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("corruptedend.spawnportal")) {
            player.sendMessage(ChatColor.RED + "No tienes permisos para usar este comando.");
            return true;
        }

        Location portalLocation;

        if (args.length >= 3) {
            // Coordenadas específicas
            try {
                int x = Integer.parseInt(args[0]);
                int y = Integer.parseInt(args[1]);
                int z = Integer.parseInt(args[2]);
                portalLocation = new Location(player.getWorld(), x, y, z);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Coordenadas inválidas. Uso: /spawnportalce <x> <y> <z> o /spawnportalce random");
                return true;
            }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("random")) {
            // Ubicación aleatoria
            World world = player.getWorld();
            Random random = new Random();
            int x = random.nextInt(5000) - 4000; // -1000 a 1000
            int y = random.nextInt(100) + 100;   // 100 a 200
            int z = random.nextInt(5000) - 4000; // -1000 a 1000
            portalLocation = new Location(world, x, y, z);
        } else if (args.length == 0) {
            // Ubicación del jugador
            portalLocation = player.getLocation().clone().add(0, 1, 0);
        } else {
            player.sendMessage(ChatColor.RED + "Uso: /spawnportalce [<x> <y> <z> | random]");
            return true;
        }

        // Verificar que no sea en el Corrupted End
        if (portalLocation.getWorld().getName().equals(CorruptedEnd.WORLD_NAME)) {
            player.sendMessage(ChatColor.RED + "No puedes crear un portal dentro del Corrupted End.");
            return true;
        }

        portalManager.createOverworldPortal(portalLocation);

        player.sendMessage(ChatColor.GREEN + "Portal del Corrupted End creado en " +
                portalLocation.getBlockX() + ", " + portalLocation.getBlockY() + ", " + portalLocation.getBlockZ());

        // Teleportar al jugador al portal si está lejos
        if (player.getLocation().distance(portalLocation) > 50) {
            player.teleport(portalLocation);
            player.sendMessage(ChatColor.YELLOW + "Has sido teleportado al nuevo portal.");
        }

        return true;
    }

    private boolean handleGiveLootable(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /givelootable <tipo> <jugador>");
            sender.sendMessage(ChatColor.GRAY + "Tipos disponibles: corrupta, infestada");
            return true;
        }

        if (!sender.hasPermission("corruptedend.giveloot")) {
            sender.sendMessage(ChatColor.RED + "No tienes permisos para usar este comando.");
            return true;
        }

        String lootType = args[0].toLowerCase();
        String playerName = args[1];

        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Jugador no encontrado: " + playerName);
            return true;
        }

        LootType type;
        switch (lootType) {
            case "corrupta":
                type = LootType.CAJA_CORRUPTA;
                break;
            case "infestada":
                type = LootType.CAJA_INFESTADA;
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Tipo de loot inválido. Usa: corrupta o infestada");
                return true;
        }

        lootManager.giveLootChest(targetPlayer, type);

        sender.sendMessage(ChatColor.GREEN + "Caja " + type.getDisplayName() +
                " entregada a " + targetPlayer.getName());
        targetPlayer.sendMessage(ChatColor.GOLD + "Has recibido una " + type.getDisplayName() + "!");

        return true;
    }
}