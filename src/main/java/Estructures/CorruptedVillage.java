package Estructures;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class CorruptedVillage implements Listener, CommandExecutor {

    private final JavaPlugin plugin;
    private final Random random = new Random();

    public CorruptedVillage(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("estructure").setExecutor(this);

        File schemFolder = new File(plugin.getDataFolder(), "schem");
        if (!schemFolder.exists() && schemFolder.mkdirs()) {
            plugin.getLogger().info("Carpeta 'schem' creada exitosamente.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("estructure") || args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /estructure <CorruptedVillage>");
            return false;
        }

        String structureName = args[0];
        if (!structureName.equalsIgnoreCase("CorruptedVillage")) {
            sender.sendMessage(ChatColor.RED + "Estructura no válida.");
            return false;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Location potentialLocation = findPotentialLocation();
            if (potentialLocation == null) {
                sender.sendMessage(ChatColor.RED + "No se pudo encontrar una ubicación válida para la estructura.");
                return;
            }

            // Cargar chunks necesarios antes de pegar la estructura
            loadChunksAround(potentialLocation, 2).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!pasteStructure(potentialLocation, "CorruptedVillage1_V6.schem")) {
                        sender.sendMessage(ChatColor.RED + "Error al pegar la estructura.");
                    } else {
                        String coordinatesMessage = String.format(
                                "ruletavct [\"\",{\"text\":\"\\n\"},{\"text\":\"\\u06de Estructura \\u27a4\",\"bold\":true,\"color\":\"#6E02A5\"},{\"text\":\"\\n\\n\"},{\"text\":\"Se ha encontrado una Corrupted Village\\nen las coordenadas:\",\"color\":\"#A56CD7\"},{\"text\":\" %d %d %d\",\"color\":\"gold\"}]",
                                potentialLocation.getBlockX(), potentialLocation.getBlockY(), potentialLocation.getBlockZ()
                        );
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), coordinatesMessage);
                    }
                });
            });
        });

        return true;
    }

    private Location findPotentialLocation() {
        org.bukkit.World bukkitWorld = Bukkit.getWorld("world");
        if (bukkitWorld == null || bukkitWorld.getEnvironment() != Environment.NORMAL) {
            plugin.getLogger().warning("No se pudo encontrar el mundo principal.");
            return null;
        }

        for (int attempts = 0; attempts < 50; attempts++) {
            int x = generateRandomCoordinate();
            int z = generateRandomCoordinate();

            Location baseLocation = findLowestGrassBlock(bukkitWorld, x, z);
            if (baseLocation == null || baseLocation.getY() > 75) {
                continue;
            }

            if (isLocationValid(baseLocation)) {
                return baseLocation;
            }   
        }

        return null;
    }

    private CompletableFuture<Void> loadChunksAround(Location location, int radius) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            org.bukkit.World world = location.getWorld();
            if (world == null) {
                future.completeExceptionally(new IllegalStateException("World is null"));
                return;
            }

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Chunk chunk = world.getChunkAt(location.getChunk().getX() + dx, location.getChunk().getZ() + dz);
                    if (!chunk.isLoaded()) {
                        chunk.load(true); // Forzar la carga del chunk
                    }
                }
            }

            future.complete(null);
        });
        return future;
    }

    private Location findLowestGrassBlock(org.bukkit.World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        Block block;
        do {
            block = world.getBlockAt(x, y--, z);
        } while (y >= 10 && block.getType() != Material.GRASS_BLOCK);

        return (block.getType() == Material.GRASS_BLOCK) ? block.getLocation().add(0, 1, 0) : null;
    }

    private int generateRandomCoordinate() {
        return random.nextInt(10000) - 5000;
    }

    private boolean isLocationValid(Location location) {
        org.bukkit.World world = location.getWorld();
        if (world == null) return false;

        Block baseBlock = world.getBlockAt(location.getBlockX(), location.getBlockY() - 1, location.getBlockZ());
        return baseBlock.getType() == Material.GRASS_BLOCK;
    }

    private boolean pasteStructure(Location location, String schematicName) {
        File schematicFile = new File(plugin.getDataFolder(), "schem/" + schematicName);
        if (!schematicFile.exists()) {
            plugin.getLogger().warning("No se encontró el esquema: " + schematicFile.getAbsolutePath());
            return false;
        }

        try (FileInputStream fis = new FileInputStream(schematicFile)) {
            Clipboard clipboard = ClipboardFormats.findByFile(schematicFile).getReader(fis).read();
            World weWorld = BukkitAdapter.adapt(location.getWorld());

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                ClipboardHolder holder = new ClipboardHolder(clipboard);
                Operations.complete(
                        holder
                                .createPaste(editSession)
                                .to(BukkitAdapter.asBlockVector(location))
                                .ignoreAirBlocks(true)
                                .build()
                );
                plugin.getLogger().info("Estructura pegada correctamente en: " + location);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error al pegar la estructura: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
