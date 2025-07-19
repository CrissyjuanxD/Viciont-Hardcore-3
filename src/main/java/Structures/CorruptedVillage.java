package Structures;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.World.Environment;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class CorruptedVillage extends BaseStructure {
    public CorruptedVillage(JavaPlugin plugin) {
        super(plugin, Arrays.asList("CorruptedVillageFV1.schem", "CorruptedVillageFV2.schem"));
    }

    @Override
    public String getName() {
        return "CorruptedVillage";
    }

    @Override
    public void generate(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Location potentialLocation = findValidFloatingLocation();
            if (potentialLocation == null) {
                sender.sendMessage(ChatColor.RED + "No se pudo encontrar una ubicación válida para la estructura.");
                return;
            }

            String selectedSchematic = schematics.get(random.nextInt(schematics.size()));

            loadChunksAround(potentialLocation, 2).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!pasteStructure(potentialLocation, selectedSchematic)) {
                        sender.sendMessage(ChatColor.RED + "Error al pegar la estructura.");
                    } else {
                        String coordinatesMessage = String.format(
                                "ruletavct [\"\",{\"text\":\"\\n\"},{\"text\":\"\\u06de Estructura \\u27a4\",\"bold\":true,\"color\":\"#6E02A5\"},{\"text\":\"\\n\\n\"},{\"text\":\"Se ha generado una Corrupted Village flotante\\nen las coordenadas:\",\"color\":\"#A56CD7\"},{\"text\":\" %d %d %d\",\"color\":\"gold\"}]",
                                potentialLocation.getBlockX(), potentialLocation.getBlockY(), potentialLocation.getBlockZ()
                        );
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), coordinatesMessage);
                    }
                });
            });
        });
    }

    private Location findValidFloatingLocation() {
        org.bukkit.World world = Bukkit.getWorld("world");
        if (world == null || world.getEnvironment() != Environment.NORMAL) {
            plugin.getLogger().warning("No se pudo encontrar el mundo principal.");
            return null;
        }

        for (int attempts = 0; attempts < 50; attempts++) {
            int x = generateRandomCoordinate();
            int z = generateRandomCoordinate();
            int y = 175;

            Location baseLocation = new Location(world, x, y, z);

            if (!isAreaPopulatedBelow(baseLocation)) {
                return baseLocation;
            }
        }
        return null;
    }

    private boolean isAreaPopulatedBelow(Location location) {
        org.bukkit.World world = location.getWorld();
        if (world == null) return true;

        int radius = 5;
        int checkHeight = 30;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy >= -checkHeight; dy--) {
                    Block block = world.getBlockAt(location.getBlockX() + dx, location.getBlockY() + dy, location.getBlockZ() + dz);
                    Material type = block.getType();
                    if (type == Material.CRAFTING_TABLE || type == Material.FURNACE || type == Material.CHEST ||
                            type == Material.BARREL || type == Material.ANVIL || type == Material.LECTERN ||
                            type == Material.BLAST_FURNACE || type == Material.SMOKER || type.name().contains("DOOR") ||
                            type.name().contains("BED")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private int generateRandomCoordinate() {
        return random.nextInt(5000) - 1000;
    }
}