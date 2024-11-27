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
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ZombieVillager;
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

        // Crear carpeta "schem" si no existe
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

        // Ejecutar la generación de la estructura de manera asíncrona
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int x = generateRandomCoordinate();
            int z = generateRandomCoordinate();
            org.bukkit.World bukkitWorld = Bukkit.getWorld("world");

            if (bukkitWorld == null || bukkitWorld.getEnvironment() != Environment.NORMAL) {
                sender.sendMessage(ChatColor.RED + "No se pudo encontrar el mundo principal.");
                return;
            }

            Chunk chunk = bukkitWorld.getChunkAt(x >> 4, z >> 4);
            if (!chunk.isLoaded()) {
                chunk.load(true);
            }

            int y = bukkitWorld.getHighestBlockYAt(x, z);
            Location location = new Location(bukkitWorld, x, y, z);

            // Ejecutar la pegada de la estructura en el hilo principal
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!pasteStructure(location, "CorruptedVillage1_V6.schem")) {
                    sender.sendMessage(ChatColor.RED + "Error al pegar la estructura.");
                    return;
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        String coordinatesMessage = String.format(
                                "ruletavct [\"\",{\"text\":\"\\n\"},{\"text\":\"\\u06de Estructura \\u27a4\",\"bold\":true,\"color\":\"#6E02A5\"},{\"text\":\"\\n\\n\"},{\"text\":\"Se ha encontrado una Corrupted Village\\nen las coordenadas:\",\"color\":\"#A56CD7\"},{\"text\":\" %d %d %d\",\"color\":\"gold\"}]",
                                x, y, z
                        );
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), coordinatesMessage);

                        sender.sendMessage(ChatColor.GREEN + "Estructura Corrupted Village generada en: " + x + ", " + y + ", " + z);
                    }
                }.runTaskLater(plugin, 20 * 10); // 20 ticks = 1 segundo
            });
        });
        return true;
    }

    private int generateRandomCoordinate() {
        int coordinate = random.nextInt(500) + 1000; // Evita coordenadas cercanas al spawn
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

            Bukkit.getScheduler().runTask(plugin, () -> {
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                    Operations.complete(
                            new ClipboardHolder(clipboard)
                                    .createPaste(editSession)
                                    .to(BukkitAdapter.asBlockVector(location))
                                    .build()
                    );
                    plugin.getLogger().info("Estructura pegada correctamente en: " + location);
                } catch (Exception e) {
                    plugin.getLogger().severe("Error al pegar la estructura: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error al leer el esquema: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

}
