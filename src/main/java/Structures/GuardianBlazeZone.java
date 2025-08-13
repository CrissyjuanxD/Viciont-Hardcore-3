package Structures;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;

public class GuardianBlazeZone extends BaseStructure {
    public GuardianBlazeZone(JavaPlugin plugin) {
        super(plugin, Arrays.asList("GuardianBlazeZone.schem"));
    }

    @Override
    public String getName() {
        return "GuardianBlazeZone";
    }

    @Override
    public void generate(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Location potentialLocation = findValidNetherLocation();
            if (potentialLocation == null) {
                sender.sendMessage(ChatColor.RED + "No se pudo encontrar una ubicación válida para la estructura en el Nether.");
                return;
            }

            String selectedSchematic = schematics.get(random.nextInt(schematics.size()));

            loadChunksAround(potentialLocation, 2).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!pasteStructure(potentialLocation, selectedSchematic)) {
                        sender.sendMessage(ChatColor.RED + "Error al pegar la estructura.");
                    } else {
                        // Ejecutar comando después de 15 segundos
                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ruletavct");
                        }, 20 * 15);

                        String coordinatesMessage = String.format(
                                "ruletavct [\"\",{\"text\":\"\\n\"},{\"text\":\"\\u06de Estructura \\u27a4\",\"bold\":true,\"color\":\"#6E02A5\"},{\"text\":\"\\n\\n\"},{\"text\":\"Se ha generado una Guardian Blaze Zone en las coordenadas:\",\"color\":\"#A56CD7\"},{\"text\":\" %d %d %d\",\"color\":\"gold\"}]",
                                potentialLocation.getBlockX(), potentialLocation.getBlockY(), potentialLocation.getBlockZ()
                        );
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), coordinatesMessage);
                    }
                });
            });
        });
    }

    private Location findValidNetherLocation() {
        org.bukkit.World netherWorld = null;
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == Environment.NETHER) {
                netherWorld = world;
                break;
            }
        }

        if (netherWorld == null) {
            plugin.getLogger().warning("No se pudo encontrar el mundo del Nether.");
            return null;
        }

        // Aumentamos el número de intentos ya que ahora la búsqueda es más compleja
        for (int attempts = 0; attempts < 500; attempts++) {
            int x = 500 + random.nextInt(4500); // Entre 500 y 5000
            int z = 500 + random.nextInt(4500); // Entre 500 y 5000

            // Buscamos primero un área de lava entre Y=31 y Y=105
            Location lavaLocation = findLavaLake(netherWorld, x, z);
            if (lavaLocation != null) {
                // Si encontramos lava, colocamos la estructura 12 bloques arriba
                return new Location(netherWorld, x, lavaLocation.getY() + 12, z);
            }
        }
        return null;
    }

    private Location findLavaLake(org.bukkit.World world, int x, int z) {
        // Buscamos de abajo hacia arriba (desde Y=31 hasta Y=105)
        for (int y = 31; y >= 25; y++) {
            if (checkLavaAtLevel(world, x, y, z)) {
                return new Location(world, x, y, z);
            }
        }
        return null;
    }

    private boolean checkLavaAtLevel(org.bukkit.World world, int x, int y, int z) {
        int lavaCount = 0;
        int radius = 2; // Para un área de 5x5 (radio de 2 en cada dirección)

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Block block = world.getBlockAt(x + dx, y, z + dz);
                if (block.getType() == Material.LAVA) {
                    lavaCount++;
                    // Si ya tenemos 25 bloques de lava (5x5), podemos salir
                    if (lavaCount >= 25) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}