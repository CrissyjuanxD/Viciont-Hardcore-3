package CorruptedEnd;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class StructureManager implements Listener {
    private final JavaPlugin plugin;
    private final Map<String, Clipboard> loadedSchematics = new HashMap<>();

    private final int STRUCTURE_SPACING = 500;
    private final Map<Long, Boolean> generatedStructures = new HashMap<>();

    public StructureManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadSchematics() {
        String[] schemNames = {
                "island1", "island2", "island3", "island4", "island5",
                "ancientend1", "ancientend2",
                "celestial_structure", "obsidian_structure"
        };

        for (String schemName : schemNames) {
            try {
                // Intentar cargar desde el JAR del plugin
                java.io.InputStream inputStream = plugin.getResource("schem/" + schemName + ".schem");

                if (inputStream != null) {
                    ClipboardFormat format = ClipboardFormats.findByAlias("schem");
                    if (format != null) {
                        try (ClipboardReader reader = format.getReader(inputStream)) {
                            Clipboard clipboard = reader.read();
                            loadedSchematics.put(schemName, clipboard);
                            plugin.getLogger().info("Schematic cargado desde JAR: " + schemName);
                        } catch (IOException e) {
                            plugin.getLogger().warning("Error leyendo schematic " + schemName + " desde JAR: " + e.getMessage());
                        }
                    } else {
                        plugin.getLogger().warning("Formato de schematic no soportado para: " + schemName);
                    }
                } else {
                    // Si no está en el JAR, intentar desde la carpeta del plugin
                    loadSchematicFromFile(schemName);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error general cargando schematic " + schemName + ": " + e.getMessage());
                // Continuar con el siguiente schematic en lugar de fallar completamente
            }
        }

        if (loadedSchematics.isEmpty()) {
            plugin.getLogger().warning("No se pudieron cargar schematics. Las estructuras no se generarán.");
        } else {
            plugin.getLogger().info("Cargados " + loadedSchematics.size() + " schematics exitosamente.");
        }
    }

    private void loadSchematicFromFile(String schemName) {
        File schemFolder = new File(plugin.getDataFolder(), "schem");
        File schemFile = new File(schemFolder, schemName + ".schem");

        if (schemFile.exists()) {
            try {
                ClipboardFormat format = ClipboardFormats.findByFile(schemFile);
                if (format != null) {
                    try (ClipboardReader reader = format.getReader(new FileInputStream(schemFile))) {
                        Clipboard clipboard = reader.read();
                        loadedSchematics.put(schemName, clipboard);
                        plugin.getLogger().info("Schematic cargado desde archivo: " + schemName);
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Error cargando schematic " + schemName + " desde archivo: " + e.getMessage());
            }
        } else {
            // Solo mostrar advertencia si tampoco está en el JAR
            if (plugin.getResource("schem/" + schemName + ".schem") == null) {
                plugin.getLogger().warning("Schematic no encontrado: " + schemName + ".schem (ni en JAR ni en carpeta)");
            } else {
                plugin.getLogger().info("Schematic " + schemName + " no encontrado en carpeta, pero está disponible en JAR");
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.getWorld().getName().equals(CorruptedEnd.WORLD_NAME)) return;
        if (event.isNewChunk()) {
            generateStructures(event.getChunk());
        }
    }

    private void generateStructures(Chunk chunk) {
        int structureX = Math.floorDiv(chunk.getX() * 16, STRUCTURE_SPACING);
        int structureZ = Math.floorDiv(chunk.getZ() * 16, STRUCTURE_SPACING);
        long structureKey = ((long)structureX << 32) | (structureZ & 0xFFFFFFFFL);

        if (generatedStructures.containsKey(structureKey)) return;
        generatedStructures.put(structureKey, true);

        if (chunk.getX() * 16 % STRUCTURE_SPACING < 16 &&
                chunk.getZ() * 16 % STRUCTURE_SPACING < 16) {

            new BukkitRunnable() {
                @Override
                public void run() {
                    tryGenerateStructure(chunk);
                }
            }.runTaskLater(plugin, 20L);
        }
    }

    private void tryGenerateStructure(Chunk chunk) {
        Random random = new Random(chunk.getX() * 341873128712L + chunk.getZ() * 132897987541L);

        // Probabilidad base de estructuras normales
        if (random.nextInt(100) < 35) {
            Location structureLoc = findStructureLocation(chunk, random);
            if (structureLoc != null) {
                BiomeType biome = determineBiomeAtLocation(structureLoc);
                String[] structures = getStructuresForBiome(biome, false);

                if (structures.length > 0) {
                    String structure = structures[random.nextInt(structures.length)];
                    pasteSchematic(structure, structureLoc);
                }
            }
        }

        // Probabilidad de estructuras especiales/raras
        if (random.nextInt(100) < 8) {
            Location structureLoc = findStructureLocation(chunk, random);
            if (structureLoc != null) {
                BiomeType biome = determineBiomeAtLocation(structureLoc);
                String[] structures = getStructuresForBiome(biome, true);

                if (structures.length > 0) {
                    String structure = structures[random.nextInt(structures.length)];
                    pasteSchematic(structure, structureLoc);
                }
            }
        }
    }

    private BiomeType determineBiomeAtLocation(Location location) {
        // Verificar bloques cercanos para determinar el bioma
        Map<Material, Integer> blockCounts = new HashMap<>();

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    Location checkLoc = location.clone().add(dx, dy, dz);
                    Material blockType = checkLoc.getBlock().getType();
                    blockCounts.put(blockType, blockCounts.getOrDefault(blockType, 0) + 1);
                }
            }
        }

        // Determinar bioma basado en bloques predominantes
        if (blockCounts.getOrDefault(Material.BLUE_TERRACOTTA, 0) > 5) {
            return BiomeType.CELESTIAL_FOREST;
        } else if (blockCounts.getOrDefault(Material.OBSIDIAN, 0) > 8) {
            return BiomeType.OBSIDIAN_PEAKS;
        } else if (blockCounts.getOrDefault(Material.CRIMSON_HYPHAE, 0) > 5) {
            return BiomeType.CRIMSON_WASTES;
        } else {
            return BiomeType.SCULK_PLAINS;
        }
    }

    private String[] getStructuresForBiome(BiomeType biome, boolean isSpecial) {
        if (isSpecial) {
            // Estructuras especiales por bioma
            switch (biome) {
                case CELESTIAL_FOREST:
                    return new String[]{"celestial_structure", "ancientend1"};
                case OBSIDIAN_PEAKS:
                    return new String[]{"obsidian_structure", "ancientend2"};
                case CRIMSON_WASTES:
                    return new String[]{"ancientend1"};
                default:
                    return new String[]{"ancientend1", "ancientend2"};
            }
        } else {
            // Estructuras comunes
            return new String[]{"island1", "island2", "island3", "island4", "island5"};
        }
    }

    private Location findStructureLocation(Chunk chunk, Random random) {
        int baseX = (chunk.getX() * 16 / STRUCTURE_SPACING) * STRUCTURE_SPACING;
        int baseZ = (chunk.getZ() * 16 / STRUCTURE_SPACING) * STRUCTURE_SPACING;

        int x = baseX + random.nextInt(STRUCTURE_SPACING);
        int z = baseZ + random.nextInt(STRUCTURE_SPACING);

        World world = chunk.getWorld();
        int y = world.getHighestBlockYAt(x, z);

        if (y > 30 && y < 200) {
            return new Location(world, x, y + 1, z);
        }
        return null;
    }

    private void pasteSchematic(String schemName, Location location) {
        Clipboard clipboard = loadedSchematics.get(schemName);
        if (clipboard == null) {
            plugin.getLogger().warning("Schematic no disponible para generar: " + schemName + " (estructura omitida)");
            return;
        }

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                    .ignoreAirBlocks(false)
                    .build();

            Operations.complete(operation);
            plugin.getLogger().info("Estructura " + schemName + " generada en " +
                    location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
        } catch (Exception e) {
            plugin.getLogger().warning("Error pegando schematic " + schemName + ": " + e.getMessage());
        }
    }

    public void spawnParticles() {
        World corruptedWorld = Bukkit.getWorld(CorruptedEnd.WORLD_NAME);
        if (corruptedWorld == null) return;

        for (Player player : corruptedWorld.getPlayers()) {
            Location loc = player.getLocation();
            Random random = new Random();

            // Partículas ambientales diferentes según el bioma
            BiomeType biome = determineBiomeAtLocation(loc);
            spawnBiomeParticles(loc, biome, random);

            // Partículas generales del portal
            if (random.nextInt(100) < 5) {
                Location particleLoc = loc.clone().add(
                        random.nextInt(10) - 5,
                        -2,
                        random.nextInt(10) - 5
                );

                corruptedWorld.spawnParticle(Particle.PORTAL, particleLoc, 1, 0, 0, 0, 0);
            }
        }
    }

    private void spawnBiomeParticles(Location loc, BiomeType biome, Random random) {
        World world = loc.getWorld();

        switch (biome) {
            case CELESTIAL_FOREST:
                if (random.nextInt(100) < 8) {
                    Location particleLoc = loc.clone().add(
                            random.nextInt(8) - 4,
                            random.nextInt(4),
                            random.nextInt(8) - 4
                    );
                    world.spawnParticle(Particle.SNOWFLAKE, particleLoc, 2, 0.2, 0.2, 0.2);
                    world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0.1, 0.1, 0.1);
                }
                break;

            case OBSIDIAN_PEAKS:
                if (random.nextInt(100) < 6) {
                    Location particleLoc = loc.clone().add(
                            random.nextInt(6) - 3,
                            random.nextInt(3),
                            random.nextInt(6) - 3
                    );
                    world.spawnParticle(Particle.SQUID_INK, particleLoc, 3, 0.3, 0.3, 0.3, 1.0f); // ✅ Corregido
                    world.spawnParticle(Particle.ASH, particleLoc, 2, 0.2, 0.2, 0.2);
                }
                break;

            case CRIMSON_WASTES:
                if (random.nextInt(100) < 7) {
                    Location particleLoc = loc.clone().add(
                            random.nextInt(10) - 5,
                            random.nextInt(3),
                            random.nextInt(10) - 5
                    );
                    world.spawnParticle(Particle.CRIMSON_SPORE, particleLoc, 2, 0.3, 0.3, 0.3);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0.1, 0.1, 0.1);
                }
                break;

            default: // SCULK_PLAINS
                if (random.nextInt(100) < 4) {
                    Location particleLoc = loc.clone().add(
                            random.nextInt(8) - 4,
                            random.nextInt(2),
                            random.nextInt(8) - 4
                    );
                    world.spawnParticle(Particle.SCULK_SOUL, particleLoc, 1, 0.2, 0.2, 0.2, 0.5f);
                    world.spawnParticle(Particle.SCULK_CHARGE, particleLoc, 1, 0.1, 0.1, 0.1, 0.3f);
                }
                break;
        }
    }
}