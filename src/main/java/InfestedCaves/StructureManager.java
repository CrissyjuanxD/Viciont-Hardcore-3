package InfestedCaves;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.util.Random;

public class StructureManager {
    private final JavaPlugin plugin;
    private Clipboard templeSchematic;
    private Clipboard dungeonSchematic;
    private boolean isLoaded = false;

    public StructureManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadSchematics() {
        File schemFolder = new File(plugin.getDataFolder(), "schematics");

        // Cargar Templo
        templeSchematic = loadSchem(new File(schemFolder, "TemploRunico.schem"));

        // Cargar Dungeon
        dungeonSchematic = loadSchem(new File(schemFolder, "InfestedDungeon.schem"));

        if (templeSchematic != null) isLoaded = true;
    }

    private Clipboard loadSchem(File file) {
        if (!file.exists()) {
            plugin.getLogger().warning("Schematic no encontrado: " + file.getName());
            return null;
        }
        try (ClipboardReader reader = ClipboardFormats.findByFile(file).getReader(new FileInputStream(file))) {
            return reader.read();
        } catch (Exception e) {
            plugin.getLogger().severe("Error cargando " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }

    public void pasteTempleAtSpawn(World world) {
        if (templeSchematic == null) return;

        // SE PEGA EXACTAMENTE EN LAS COORDENADAS SOLICITADAS.
        // El generador ya se encargó de dejar el espacio vacío de forma natural.
        plugin.getLogger().info("Pegando TemploRunico en 0, -56, 0");
        pasteSchematic(templeSchematic, new Location(world, 0, -56, 0));
    }

    public void tryGenerateDungeon(Chunk chunk) {
        if (dungeonSchematic == null) return;

        // Solo a partir de 500 bloques del centro
        int centerX = chunk.getX() * 16;
        int centerZ = chunk.getZ() * 16;
        double dist = Math.sqrt(centerX * centerX + centerZ * centerZ);

        if (dist < 500) return;

        // Probabilidad baja
        Random random = new Random(chunk.getWorld().getSeed() + chunk.getX() * 341873128712L + chunk.getZ() * 132897987541L);
        if (random.nextInt(200) != 0) return;

        int y = 20 + random.nextInt(60);
        if (chunk.getBlock(8, y, 8).getType() == Material.AIR) {
            plugin.getLogger().info("Generando InfestedDungeon en " + centerX + ", " + y + ", " + centerZ);
            pasteSchematic(dungeonSchematic, new Location(chunk.getWorld(), centerX + 8, y, centerZ + 8));
        }
    }

    private void pasteSchematic(Clipboard clipboard, Location loc) {
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(loc.getWorld()))) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()))
                    .ignoreAirBlocks(true)
                    .build();
            Operations.complete(operation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}