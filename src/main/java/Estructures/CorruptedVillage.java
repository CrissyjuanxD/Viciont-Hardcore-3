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
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileInputStream;
import java.util.Random;

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

        // Ejecutar la búsqueda de ubicación de manera asíncrona
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Location potentialLocation = findPotentialLocation(); // Buscar coordenadas preliminares (sin chunks)
            if (potentialLocation == null) {
                sender.sendMessage(ChatColor.RED + "No se pudo encontrar una ubicación válida para la estructura.");
                return;
            }

            // Validar y pegar la estructura en el hilo principal
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!isLocationValid(potentialLocation)) { // Validación en el hilo principal
                    sender.sendMessage(ChatColor.RED + "La ubicación encontrada no es válida.");
                    return;
                }

                if (!pasteStructure(potentialLocation, "CorruptedVillage1_V6.schem")) {
                    sender.sendMessage(ChatColor.RED + "Error al pegar la estructura.");
                    return;
                }

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    String coordinatesMessage = String.format(
                            "ruletavct [\"\",{\"text\":\"\\n\"},{\"text\":\"\\u06de Estructura \\u27a4\",\"bold\":true,\"color\":\"#6E02A5\"},{\"text\":\"\\n\\n\"},{\"text\":\"Se ha encontrado una Corrupted Village\\nen las coordenadas:\",\"color\":\"#A56CD7\"},{\"text\":\" %d %d %d\",\"color\":\"gold\"}]",
                            potentialLocation.getBlockX(), potentialLocation.getBlockY(), potentialLocation.getBlockZ()
                    );
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), coordinatesMessage);
                }, 200L);
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

        for (int attempts = 0; attempts < 100; attempts++) { // Limitar a 100 intentos
            int x = generateRandomCoordinate();
            int z = generateRandomCoordinate();
            int y = bukkitWorld.getHighestBlockYAt(x, z);
            return new Location(bukkitWorld, x, y, z);
        }
        return null;
    }

    private boolean isLocationValid(Location location) {
        int baseChunkX = location.getChunk().getX();
        int baseChunkZ = location.getChunk().getZ();
        org.bukkit.World world = location.getWorld();

        // Revisar chunks adyacentes (radio de 4)
        for (int cx = -4; cx <= 4; cx++) {
            for (int cz = -4; cz <= 4; cz++) {
                Chunk chunk = world.getChunkAt(baseChunkX + cx, baseChunkZ + cz);
                if (!chunk.isLoaded()) {
                    chunk.load(true);
                }

                // Solo buscar cofres en este chunk
                for (BlockState state : chunk.getTileEntities()) {
                    if (state.getType() == Material.CHEST) {
                        plugin.getLogger().info("Cofre encontrado en: " + state.getLocation());
                        return false;
                    }
                }
            }
        }

        // Verificar que el bloque debajo es sólido
        Block blockBelow = location.clone().subtract(0, 1, 0).getBlock();
        if (blockBelow.getType().isAir() || blockBelow.isLiquid()) {
            plugin.getLogger().info("Ubicación no válida, bloque inferior no es sólido: " + blockBelow.getType());
            return false;
        }

        return true;
    }

    private int generateRandomCoordinate() {
        int coordinate = random.nextInt(500) + 1000; // Evitar coordenadas cercanas al spawn
        if (random.nextBoolean()) coordinate = -coordinate;
        return coordinate;
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
                Operations.complete(
                        new ClipboardHolder(clipboard)
                                .createPaste(editSession)
                                .to(BukkitAdapter.asBlockVector(location))
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
