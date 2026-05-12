package Structures;

import TitleListener.RuletaAnimation;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;

public class EndRing extends BaseStructure {
    private final RuletaAnimation ruletaAnimation;

    public EndRing(JavaPlugin plugin, RuletaAnimation ruletaAnimation) {
        super(plugin, Arrays.asList("EndRingFV1.schem"));
        this.ruletaAnimation = ruletaAnimation;
    }

    @Override
    public String getName() {
        return "EndRing";
    }

    @Override
    public void generate(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Location potentialLocation = findValidEndLocation();
            if (potentialLocation == null) {
                sender.sendMessage(ChatColor.RED + "No se pudo encontrar una ubicación válida para la estructura en el End.");
                return;
            }

            String selectedSchematic = schematics.get(random.nextInt(schematics.size()));

            loadChunksAround(potentialLocation, 2).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!pasteStructure(potentialLocation, selectedSchematic)) {
                        sender.sendMessage(ChatColor.RED + "Error al pegar la estructura.");
                    } else {
                        // Aplicar efectos y sonidos al instante
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 20 * 15, 1));
                            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                        }

                        String jsonMessage = String.format(
                                "[\"\",{\"text\":\"\\n\"},{\"text\":\"\\u06de Estructura \\u25ba \",\"bold\":true,\"color\":\"#6E02A5\"},{\"text\":\"\\n\\n\"},{\"text\":\"Se ha generado un End Ring en las coordenadas:\\n\",\"color\":\"#A175D6\"},{\"text\":\"%d %d %d\",\"color\":\"gold\"},{\"text\":\"\\n \"}]",
                                potentialLocation.getBlockX(), potentialLocation.getBlockY(), potentialLocation.getBlockZ()
                        );

                        // Llamada nativa directa a la animación (Morada para estructuras)
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            ruletaAnimation.playAnimation(player, "morado", "off", "center", jsonMessage);
                        }
                    }
                });
            });
        });
    }

    private Location findValidEndLocation() {
        org.bukkit.World endWorld = null;
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == Environment.THE_END) {
                endWorld = world;
                break;
            }
        }

        if (endWorld == null) {
            plugin.getLogger().warning("No se pudo encontrar el mundo del End.");
            return null;
        }

        for (int attempts = 0; attempts < 50; attempts++) {
            int x = generateRandomEndCoordinate();
            int z = generateRandomEndCoordinate();
            int y = 150;

            Location baseLocation = new Location(endWorld, x, y, z);

            if (!isAreaPopulated(baseLocation)) {
                return baseLocation;
            }
        }
        return null;
    }

    private boolean isAreaPopulated(Location location) {
        org.bukkit.World world = location.getWorld();
        if (world == null) return true;

        int radius = 5;
        int checkHeight = 10;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy >= -checkHeight; dy--) {
                    Block block = world.getBlockAt(location.getBlockX() + dx, location.getBlockY() + dy, location.getBlockZ() + dz);
                    if (!block.getType().isAir()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int generateRandomEndCoordinate() {
        int base = random.nextBoolean() ? 700 : -700;
        return base + (random.nextBoolean() ? random.nextInt(7000) : -random.nextInt(7000));
    }
}