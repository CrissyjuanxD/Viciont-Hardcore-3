package Estructures;

/*
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.EditSession;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.util.Random;

public class CorruptedVillageSpawner {
    private final JavaPlugin plugin;
    private final Random random = new Random();

    public CorruptedVillageSpawner(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void generateCorruptedVillage(Location location) {
        if (random.nextInt(100) < 30) { // 30% de probabilidad
            placeRandomSchematic(location);
        }
    }

    private void placeRandomSchematic(Location location) {
        String[] schematics = {"corruptedVillage1", "corruptedVillage2", "corruptedVillage3", "corruptedVillage4"};
        String chosenSchematic = schematics[random.nextInt(schematics.length)];

        File schematicFile = new File(plugin.getDataFolder() + "/schematics/" + chosenSchematic + ".schem");
        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);

        try (EditSession editSession = WorldEditPlugin.getInstance().getWorldEdit().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
            Clipboard clipboard = format.getReader(new FileInputStream(schematicFile)).read();
            ForwardExtentCopy copy = new ForwardExtentCopy(clipboard, clipboard.getRegion(), clipboard.getOrigin(), editSession, BlockVector3.at(location.getX(), location.getY(), location.getZ()));
            Operations.complete(copy);
        } catch (Exception e) {
            plugin.getLogger().severe("Error al cargar el esquemático: " + e.getMessage());
        }
    }
}
*/
