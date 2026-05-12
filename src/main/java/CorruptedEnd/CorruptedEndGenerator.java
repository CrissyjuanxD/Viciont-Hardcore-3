package CorruptedEnd;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.SimplexOctaveGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CorruptedEndGenerator extends ChunkGenerator {
    private final JavaPlugin plugin;
    private static final int BASE_HEIGHT = 100;
    private final SplittableRandom random = new SplittableRandom();

    // Generadores
    private SimplexOctaveGenerator islandShapeGenerator; // Forma detallada de las islas
    private SimplexOctaveGenerator heightGenerator;      // relieve
    private SimplexOctaveGenerator caveGenerator;        // agujeros
    private SimplexOctaveGenerator hillGenerator;        // colinas

    // EL CONTROLADOR MAESTRO
    private SimplexOctaveGenerator regionGenerator;      // Define en qué "Región" estamos

    public CorruptedEndGenerator(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private void initializeGenerators(World world) {
        if (islandShapeGenerator != null) return;

        Random seedRandom = new Random(world.getSeed());

        // 1. REGION GENERATOR (Frecuencia MUY BAJA)
        // Este es el mapa gigante que decide donde están los archipiélagos.
        // Escala 0.0015 -> Estructuras de aprox 600-800 bloques de ancho.
        regionGenerator = new SimplexOctaveGenerator(seedRandom, 2);
        regionGenerator.setScale(0.0015D);

        // 2. ISLAND SHAPE (Frecuencia MEDIA-ALTA)
        // Esto rompe el "continente" del bioma en islas individuales.
        islandShapeGenerator = new SimplexOctaveGenerator(seedRandom, 8);
        islandShapeGenerator.setScale(0.012D); // Aumenté esto para que sean islas más definidas y no tan enormes

        // Decoración
        heightGenerator = new SimplexOctaveGenerator(seedRandom, 6);
        heightGenerator.setScale(0.01D);

        caveGenerator = new SimplexOctaveGenerator(seedRandom, 4);
        caveGenerator.setScale(0.05D);

        hillGenerator = new SimplexOctaveGenerator(seedRandom, 2);
        hillGenerator.setScale(0.008D);
    }

    @Override
    public ChunkData generateChunkData(World world, Random cRandom, int chunkX, int chunkZ, BiomeGrid biomes) {
        initializeGenerators(world);
        ChunkData chunk = createChunkData(world);

        // 1. Pre-cálculo del Bioma Central (para F3 y Spawning)
        BiomeInfo centerInfo = getBiomeInfo(chunkX * 16 + 8, chunkZ * 16 + 8);

        // Si el centro es nulo (Vacío de separación), ponemos THE_END
        setVanillaBiome(world, biomes, centerInfo != null ? centerInfo.type : null);

        // Optimización: Si estamos en medio del vacío profundo, no calculamos bloques
        if (centerInfo == null && isDeepVoid(chunkX * 16 + 8, chunkZ * 16 + 8)) {
            return chunk;
        }

        // 2. Generación de Terreno
        for (int X = 0; X < 16; X++) {
            for (int Z = 0; Z < 16; Z++) {
                int globalX = chunkX * 16 + X;
                int globalZ = chunkZ * 16 + Z;

                BiomeInfo info = getBiomeInfo(globalX, globalZ);

                // Si info es null, estamos en la zona de separación (Aire)
                if (info == null) continue;

                // --- GENERACIÓN DE ISLAS DENTRO DEL BIOMA ---

                // noiseValue: Define la forma local de las islas (-1 a 1)
                double islandNoise = islandShapeGenerator.noise(globalX, globalZ, 0.5D, 0.5D);

                // edgeFactor: Es un número entre 0.0 y 1.0
                // 1.0 = Centro del bioma (Islas completas)
                // 0.0 = Borde del bioma (Islas desaparecen)
                // Esto hace que las islas se "desvanezcan" al acercarse al vacío, evitando cortes rectos.
                double edgeFactor = info.edgeFactor;

                // Umbral dinámico: Cuanto más cerca del borde, más difícil generar tierra
                double threshold = -0.2 + (1.0 - edgeFactor) * 0.8;

                if (islandNoise < threshold) {
                    continue; // Hueco entre islas del mismo bioma
                }

                // Altura y Grosor
                int baseY = BASE_HEIGHT;
                int thickness = 0;
                double heightVar = heightGenerator.noise(globalX, globalZ, 0.5D, 0.5D);

                // Ajustar grosor por borde (Fade out)
                double fadeMultiplier = Math.max(0.2, edgeFactor); // Nunca menos del 20% si ya pasó el threshold

                switch (info.type) {
                    case CELESTIAL_FOREST:
                        // Islas flotantes irregulares
                        baseY = BASE_HEIGHT - 15;
                        thickness = (int) ((islandNoise * 25) + 20);
                        thickness = (int) (thickness * fadeMultiplier);
                        baseY += (int) (heightVar * 15);
                        break;

                    case CRIMSON_WASTES:
                        // Terreno más ondulado y conectado
                        thickness = (int) ((islandNoise * 15) + 15);
                        double hillNoise = hillGenerator.noise(globalX, globalZ, 0.5, 0.5);
                        baseY = BASE_HEIGHT + (int)(hillNoise * 18);
                        thickness = (int) (thickness * fadeMultiplier);
                        break;

                    case OBSIDIAN_PEAKS:
                        // Picos muy altos y afilados
                        baseY = BASE_HEIGHT + 8;
                        thickness = (int) (Math.abs(islandNoise) * 55) + 12;
                        thickness = (int) (thickness * fadeMultiplier);
                        baseY += (int) (Math.abs(heightVar) * 40);
                        break;

                    default: // SCULK_PLAINS
                        thickness = 22;
                        baseY += (int) (heightVar * 10);
                        thickness = (int) (thickness * fadeMultiplier);
                        break;
                }

                if (thickness <= 2) continue;

                generateIslandTerrain(chunk, X, Z, baseY, thickness, info.type, caveGenerator, globalX, globalZ);
                handleSurfaceFeatures(chunk, X, Z, baseY, thickness, info.type);
            }
        }
        return chunk;
    }

    // --- LÓGICA DE BANDAS (LA SOLUCIÓN A LA "ENSALADA") ---

    private static class BiomeInfo {
        BiomeType type;
        double edgeFactor; // 0.0 (Borde peligroso) a 1.0 (Centro seguro)

        public BiomeInfo(BiomeType type, double edgeFactor) {
            this.type = type;
            this.edgeFactor = edgeFactor;
        }
    }

    /*
     * Mapeo de Ruido a Biomas con "Zona Muerta" (Dead Zone)
     * El ruido va de -1.0 a 1.0. Definimos rangos estrictos.
     * Entre rango y rango hay un espacio "null" que crea la separación física.
     */
    private BiomeInfo getBiomeInfo(int x, int z) {
        // Obtenemos un valor maestro para la región (-1 a 1)
        double noise = regionGenerator.noise(x, z, 0.5D, 0.5D);

        // Definimos las "Bandas" de biomas y el tamaño del borde (feathering)
        // fadeSize: Qué tan lejos del vacío empieza a reducirse la isla (ej. 0.1 es aprox 40-50 bloques)
        double fadeSize = 0.15;

        // BANDA 1: CELESTIAL FOREST (-1.0 a -0.6)
        if (noise >= -1.0 && noise < -0.6) {
            double distToEdge = Math.min(noise - (-1.0), (-0.6) - noise);
            return new BiomeInfo(BiomeType.CELESTIAL_FOREST, calculateEdge(distToEdge, fadeSize));
        }

        // ZONA MUERTA 1 (-0.6 a -0.5) -> VACÍO DE 50-80 BLOQUES

        // BANDA 2: SCULK PLAINS (-0.5 a -0.1)
        if (noise >= -0.5 && noise < -0.1) {
            double distToEdge = Math.min(noise - (-0.5), (-0.1) - noise);
            return new BiomeInfo(BiomeType.SCULK_PLAINS, calculateEdge(distToEdge, fadeSize));
        }

        // ZONA MUERTA 2 (-0.1 a 0.1) -> VACÍO CENTRAL

        // BANDA 3: CRIMSON WASTES (0.1 a 0.5)
        if (noise >= 0.1 && noise < 0.5) {
            double distToEdge = Math.min(noise - (0.1), (0.5) - noise);
            return new BiomeInfo(BiomeType.CRIMSON_WASTES, calculateEdge(distToEdge, fadeSize));
        }

        // ZONA MUERTA 3 (0.5 a 0.6) -> VACÍO

        // BANDA 4: OBSIDIAN PEAKS (0.6 a 1.0)
        if (noise >= 0.6 && noise <= 1.0) {
            double distToEdge = Math.min(noise - (0.6), 1.0 - noise);
            return new BiomeInfo(BiomeType.OBSIDIAN_PEAKS, calculateEdge(distToEdge, fadeSize));
        }

        // Si cae en una Zona Muerta, retorna null (Vacío)
        return null;
    }

    // Calcula de 0 a 1 qué tan fuerte es el bioma basado en la distancia al vacío
    private double calculateEdge(double distance, double maxFade) {
        if (distance >= maxFade) return 1.0; // Estamos seguros en el centro
        return distance / maxFade; // Gradiente de 0.0 a 1.0
    }

    // Método auxiliar rápido para saber si estamos en el vacío profundo (para optimizar)
    private boolean isDeepVoid(int x, int z) {
        double noise = regionGenerator.noise(x, z, 0.5D, 0.5D);
        // Si no cae en ninguno de los rangos válidos anteriores
        boolean inBiome = (noise >= -1.0 && noise < -0.6) ||
                (noise >= -0.5 && noise < -0.1) ||
                (noise >= 0.1 && noise < 0.5) ||
                (noise >= 0.6 && noise <= 1.0);
        return !inBiome;
    }

    // --- RESTO DEL CÓDIGO (Igual que antes, solo asegúrate de los Imports) ---

    private void setVanillaBiome(World world, BiomeGrid biomes, BiomeType biomeType) {
        Biome vanillaBiome;
        if (biomeType == null) {
            vanillaBiome = Biome.THE_END;
        } else {
            switch (biomeType) {
                case CELESTIAL_FOREST: vanillaBiome = Biome.END_MIDLANDS; break;
                case OBSIDIAN_PEAKS: vanillaBiome = Biome.SOUL_SAND_VALLEY; break;
                case CRIMSON_WASTES: vanillaBiome = Biome.END_HIGHLANDS; break;
                default: vanillaBiome = Biome.END_BARRENS; break;
            }
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < world.getMaxHeight(); y += 4) {
                    biomes.setBiome(x, y, z, vanillaBiome);
                }
            }
        }
    }

    private void handleSurfaceFeatures(ChunkData chunk, int x, int z, int baseY, int thickness, BiomeType biomeType) {
        if (thickness > 5 && random.nextInt(8) == 0) {
            int surfaceY = findSurfaceY(chunk, x, z, baseY, thickness);
            if (surfaceY > 0) {
                generateSurfaceDecoration(chunk, x, z, surfaceY + 1, biomeType);
            }
        }

        if (biomeType == BiomeType.CELESTIAL_FOREST && thickness > 15 && random.nextInt(50) == 0) {
            int surfaceY = findSurfaceY(chunk, x, z, baseY, thickness);
            if (surfaceY > 0 && surfaceY < BASE_HEIGHT - 5) {
                if (isValidLakeLocation(chunk, x, z, surfaceY)) {
                    generateLake(chunk, x, z, surfaceY);
                }
            }
        }
    }

    private void generateIslandTerrain(ChunkData chunk, int x, int z, int baseY, int thickness,
                                       BiomeType biomeType, SimplexOctaveGenerator caveGenerator,
                                       int globalX, int globalZ) {
        Material primaryBlock = biomeType.getPrimaryBlock();
        Material secondaryBlock = biomeType.getSecondaryBlock();
        Material accentBlock = biomeType.getAccentBlock();

        for (int i = 0; i < thickness / 3; i++) {
            int y = baseY + i;
            if (y >= 0 && y < 256) {
                if (caveGenerator.noise(globalX, y, globalZ, 0.5D, 0.5D) > 0.6) continue;
                chunk.setBlock(x, y, z, getLayerMaterial(i, thickness / 3, primaryBlock, secondaryBlock, accentBlock));
            }
        }
        for (int i = 1; i <= thickness; i++) {
            int y = baseY - i;
            if (y >= 0 && y < 256) {
                if (caveGenerator.noise(globalX, y, globalZ, 0.5D, 0.5D) > 0.6) continue;
                chunk.setBlock(x, y, z, getLayerMaterial(i, thickness, primaryBlock, secondaryBlock, accentBlock));
            }
        }
    }

    private Material getLayerMaterial(int layer, int maxLayers, Material primary, Material secondary, Material accent) {
        double ratio = (double) layer / maxLayers;
        double randomValue = random.nextDouble();
        if (ratio < 0.2 && randomValue < 0.3) return accent;
        else if (randomValue < 0.7) return primary;
        else return secondary;
    }

    private void generateSurfaceDecoration(ChunkData chunk, int x, int z, int y, BiomeType biomeType) {
        Material[] decorations = biomeType.getDecorationBlocks();
        Material decoration;

        if (random.nextInt(100) < 60) {
            decoration = random.nextBoolean() ? Material.SCULK_SENSOR : Material.SCULK_SHRIEKER;
        } else {
            decoration = decorations[random.nextInt(decorations.length)];
        }

        if (y >= 0 && y < 255) {
            chunk.setBlock(x, y, z, decoration);
            if (decoration == Material.SCULK_SHRIEKER && random.nextInt(3) == 0) {
                chunk.setBlock(x, y - 1, z, Material.SCULK_CATALYST);
            }
            if (random.nextInt(4) == 0 && (decoration == Material.SCULK_SENSOR || decoration == Material.SCULK_SHRIEKER)) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if ((dx != 0 || dz != 0) && random.nextBoolean() &&
                                x + dx >= 0 && x + dx < 16 && z + dz >= 0 && z + dz < 16) {
                            if (chunk.getType(x + dx, y - 1, z + dz) != Material.AIR) {
                                chunk.setBlock(x + dx, y, z + dz, Material.SCULK_VEIN);
                            }
                        }
                    }
                }
            }
        }
    }

    private int findSurfaceY(ChunkData chunk, int x, int z, int baseY, int thickness) {
        for (int y = Math.min(255, baseY + thickness + 10); y >= Math.max(0, baseY - thickness - 10); y--) {
            if (chunk.getType(x, y, z) != Material.AIR) {
                return y;
            }
        }
        return -1;
    }

    private boolean isValidLakeLocation(ChunkData chunk, int centerX, int centerZ, int surfaceY) {
        int solidBlocks = 0;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                if (x >= 0 && x < 16 && z >= 0 && z < 16) {
                    if (chunk.getType(x, surfaceY - 1, z) != Material.AIR) {
                        solidBlocks++;
                    }
                }
            }
        }
        return solidBlocks >= 40;
    }

    private void generateLake(ChunkData chunk, int centerX, int centerZ, int surfaceY) {
        int radius = 2 + random.nextInt(3);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                if (x >= 0 && x < 16 && z >= 0 && z < 16) {
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    if (distance <= radius && chunk.getType(x, surfaceY - 1, z) != Material.AIR) {
                        chunk.setBlock(x, surfaceY - 1, z, Material.WATER);
                        if (chunk.getType(x, surfaceY - 2, z) == Material.AIR) {
                            chunk.setBlock(x, surfaceY - 2, z, Material.BLUE_TERRACOTTA);
                        }
                    }
                }
            }
        }
    }

    @NotNull
    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return Collections.singletonList(new CorruptedTreePopulator(plugin));
    }
}