package Structures;

import TitleListener.RuletaAnimation;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class GuardianBlazeZone extends BaseStructure {
    private final RuletaAnimation ruletaAnimation;

    public GuardianBlazeZone(JavaPlugin plugin, RuletaAnimation ruletaAnimation) {
        super(plugin, Arrays.asList("GuardianBlazeZone.schem"));
        this.ruletaAnimation = ruletaAnimation;
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
                        String jsonMessage = String.format(
                                "[\"\",{\"text\":\"\\n\"},{\"text\":\"\\u06de Estructura \\u25ba \",\"bold\":true,\"color\":\"#6E02A5\"},{\"text\":\"\\n\\n\"},{\"text\":\"Se ha generado una Guardian Blaze Zone en las coordenadas:\\n\",\"color\":\"#A175D6\"},{\"text\":\"%d %d %d\",\"color\":\"gold\"},{\"text\":\"\\n \"}]",
                                potentialLocation.getBlockX(), potentialLocation.getBlockY(), potentialLocation.getBlockZ()
                        );

                        // Llamada nativa directa a la animación (Morada)
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            ruletaAnimation.playAnimation(player, "morado", "off", "center", jsonMessage);
                        }
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

        for (int attempts = 0; attempts < 500; attempts++) {
            int x = 500 + random.nextInt(4500);
            int z = 500 + random.nextInt(4500);

            Location lavaLocation = findLavaLake(netherWorld, x, z);
            if (lavaLocation != null) {
                return new Location(netherWorld, x, lavaLocation.getY() + 12, z);
            }
        }
        return null;
    }

    private Location findLavaLake(org.bukkit.World world, int x, int z) {
        for (int y = 31; y >= 25; y--) {
            if (checkLavaAtLevel(world, x, y, z)) {
                return new Location(world, x, y, z);
            }
        }
        return null;
    }

    private boolean checkLavaAtLevel(org.bukkit.World world, int x, int y, int z) {
        int lavaCount = 0;
        int radius = 2;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Block block = world.getBlockAt(x + dx, y, z + dz);
                if (block.getType() == Material.LAVA) {
                    lavaCount++;
                    if (lavaCount >= 25) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}