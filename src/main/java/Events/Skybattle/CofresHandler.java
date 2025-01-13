package Events.Skybattle;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CofresHandler {
    private final JavaPlugin plugin;
    private final Map<Location, ItemStack[]> contenidoCofres = new HashMap<>();

    public CofresHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void guardarContenidoCofres(int minX, int maxX, int minZ, int maxZ) {
        contenidoCofres.clear();

        World world = Bukkit.getWorld("world");
        if (world == null) {
            plugin.getLogger().severe("No se encontró el mundo 'world'.");
            return;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = 0; y < world.getMaxHeight(); y++) {
                    Location loc = new Location(world, x, y, z);
                    Block block = loc.getBlock();
                    if (block.getType() == Material.CHEST) {
                        Chest chest = (Chest) block.getState();
                        ItemStack[] contenidoCopia = chest.getInventory().getContents().clone();
                        contenidoCofres.put(loc, contenidoCopia);
                    }
                }
            }
        }

        guardarCofresEnArchivo();
        plugin.getLogger().info("Contenido de cofres guardado correctamente. Total de cofres: " + contenidoCofres.size());
    }

    public void restaurarContenidoCofres() {
        for (Map.Entry<Location, ItemStack[]> entry : contenidoCofres.entrySet()) {
            Location loc = entry.getKey();
            ItemStack[] contenidoOriginal = entry.getValue();
            Block block = loc.getBlock();
            if (block.getType() != Material.CHEST) {
                block.setType(Material.CHEST);
            }

            Chest chest = (Chest) block.getState();
            chest.getInventory().setContents(contenidoOriginal);
        }

        plugin.getLogger().info("Contenido de cofres restaurado correctamente. Total de cofres restaurados: " + contenidoCofres.size());
    }

    public void guardarCofresEnArchivo() {
        File archivo = new File(plugin.getDataFolder(), "contenido_cofres.yml");
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<Location, ItemStack[]> entry : contenidoCofres.entrySet()) {
            Location loc = entry.getKey();
            ItemStack[] items = entry.getValue();
            String path = loc.getWorld().getName() + "." + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            config.set(path, items);
        }

        try {
            config.save(archivo);
            plugin.getLogger().info("Contenido de cofres guardado en archivo correctamente.");
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar contenido de cofres en archivo: " + e.getMessage());
        }
    }

    public void cargarCofresDesdeArchivo() {
        File archivo = new File(plugin.getDataFolder(), "contenido_cofres.yml");
        if (!archivo.exists()) {
            plugin.getLogger().info("No se encontró archivo de cofres guardados.");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(archivo);
        contenidoCofres.clear();

        for (String worldKey : config.getKeys(false)) {
            World world = Bukkit.getWorld(worldKey);
            if (world == null) {
                plugin.getLogger().warning("El mundo especificado no existe: " + worldKey);
                continue;
            }

            ConfigurationSection section = config.getConfigurationSection(worldKey);
            if (section == null) continue;

            for (String locKey : section.getKeys(false)) {
                try {
                    String[] coords = locKey.split(",");
                    int x = Integer.parseInt(coords[0]);
                    int y = Integer.parseInt(coords[1]);
                    int z = Integer.parseInt(coords[2]);
                    Location loc = new Location(world, x, y, z);

                    List<?> listaItems = section.getList(locKey);
                    if (listaItems == null) continue;

                    ItemStack[] items = listaItems.toArray(new ItemStack[0]);
                    contenidoCofres.put(loc, items);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Error parseando coordenadas: " + locKey);
                }
            }
        }

        plugin.getLogger().info("Contenido de cofres cargado correctamente desde el archivo.");
    }
}

