package Structures;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public abstract class BaseStructure implements Structure {
    protected final JavaPlugin plugin;
    protected final Random random = new Random();
    protected final List<String> schematics;

    public BaseStructure(JavaPlugin plugin, List<String> schematics) {
        this.plugin = plugin;
        this.schematics = schematics;

        File schemFolder = new File(plugin.getDataFolder(), "schem");
        if (!schemFolder.exists() && schemFolder.mkdirs()) {
            plugin.getLogger().info("Carpeta 'schem' creada exitosamente.");
        }
    }

    protected CompletableFuture<Void> loadChunksAround(Location location, int radius) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            org.bukkit.World world = location.getWorld();
            if (world == null) {
                future.completeExceptionally(new IllegalStateException("World is null"));
                return;
            }

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Chunk chunk = world.getChunkAt(((location.getBlockX() >> 4) + dx), ((location.getBlockZ() >> 4) + dz));
                    if (!chunk.isLoaded()) {
                        chunk.load(true);
                    }
                }
            }
            future.complete(null);
        });
        return future;
    }

    protected boolean pasteStructure(Location location, String schematicName) {
        File schematicFile = new File(plugin.getDataFolder(), "schem/" + schematicName);
        if (!schematicFile.exists()) {
            plugin.getLogger().warning("No se encontró el esquema: " + schematicFile.getAbsolutePath());
            return false;
        }

        try (FileInputStream fis = new FileInputStream(schematicFile)) {
            Clipboard clipboard = ClipboardFormats.findByFile(schematicFile).getReader(fis).read();
            World weWorld = BukkitAdapter.adapt(location.getWorld());

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                editSession.setFastMode(true);

                ClipboardHolder holder = new ClipboardHolder(clipboard);
                Operations.complete(
                        holder.createPaste(editSession)
                                .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                                .ignoreAirBlocks(false)
                                .copyEntities(true)
                                .build()
                );

                editSession.flushQueue();
                plugin.getLogger().info(getName() + " pegada correctamente en: " + location);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error al pegar la estructura: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}