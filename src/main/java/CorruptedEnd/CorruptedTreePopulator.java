package CorruptedEnd;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class CorruptedTreePopulator extends BlockPopulator {

    @Override
    public void populate(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull LimitedRegion region) {
        if (random.nextInt(100) < 35) return;

        int localX = random.nextInt(16);
        int localZ = random.nextInt(16);
        int globalX = (chunkX * 16) + localX;
        int globalZ = (chunkZ * 16) + localZ;

        int y = 250;
        while (y > 60 && region.getType(globalX, y, globalZ) == Material.AIR) {
            y--;
        }

        if (y > 60 && y < 210) {
            Material ground = region.getType(globalX, y, globalZ);
            BiomeType biome = determineBiome(region, globalX, y, globalZ, ground);

            if (biome != null) {
                generateNaturalTree(region, globalX, y + 1, globalZ, biome, random);
            }
        }
    }

    // Lee el bioma REAL en la coordenada en lugar de adivinar por el bloque del suelo
    private BiomeType determineBiome(LimitedRegion region, int x, int y, int z, Material ground) {
        Biome vanillaBiome = region.getBiome(x, y, z);
        if (vanillaBiome != null && vanillaBiome.getKey() != null) {
            String key = vanillaBiome.getKey().getKey();
            try {
                return BiomeType.valueOf(key.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        // Redundancia en caso de error
        if (ground == Material.OBSIDIAN || ground == Material.BLACK_GLAZED_TERRACOTTA) return BiomeType.OBSIDIAN_PEAKS;
        if (ground == Material.CRIMSON_HYPHAE) return BiomeType.CRIMSON_WASTES;
        if (ground == Material.SNOW_BLOCK || ground == Material.POWDER_SNOW) return BiomeType.CELESTIAL_FOREST;
        if (ground == Material.SCULK) return BiomeType.SCULK_PLAINS;
        return null;
    }

    private void generateNaturalTree(LimitedRegion region, int x, int y, int z, BiomeType biome, Random random) {
        boolean isRare = random.nextInt(100) < 5; // 5% árbol universal

        Material trunkBlock = isRare ? Material.DEAD_HORN_CORAL_BLOCK : getTrunkMaterial(biome);
        Material leafBlock = isRare ? Material.SCULK : getLeafMaterial(biome);
        Material altLeafBlock = isRare ? Material.BROWN_GLAZED_TERRACOTTA : null;

        boolean isCelestial = (!isRare && biome == BiomeType.CELESTIAL_FOREST);

        int height = isCelestial ? (8 + random.nextInt(5)) : (14 + random.nextInt(10));

        for (int dy = 0; dy <= height; dy++) {
            safeSetBlock(region, x, y + dy, z, trunkBlock);
            if (dy < height / 4) {
                if (random.nextBoolean()) safeSetBlock(region, x + 1, y + dy, z, trunkBlock);
                if (random.nextBoolean()) safeSetBlock(region, x - 1, y + dy, z, trunkBlock);
                if (random.nextBoolean()) safeSetBlock(region, x, y + dy, z + 1, trunkBlock);
                if (random.nextBoolean()) safeSetBlock(region, x, y + dy, z - 1, trunkBlock);
            }
        }

        int branches = isCelestial ? (3 + random.nextInt(3)) : (4 + random.nextInt(4));
        int fruitsPlaced = 0;

        for (int i = 0; i < branches; i++) {
            int branchStartY = y + (height / 2) + random.nextInt(height / 2);
            double angle = random.nextDouble() * Math.PI * 2;
            int length = 4 + random.nextInt(5);

            int bx = x;
            int by = branchStartY;
            int bz = z;

            for (int l = 0; l < length; l++) {
                bx += (int) Math.round(Math.cos(angle) * 1.2);
                by += random.nextInt(2);
                bz += (int) Math.round(Math.sin(angle) * 1.2);

                safeSetBlock(region, bx, by, bz, trunkBlock);

                if (l == length - 1 || random.nextInt(3) == 0) {
                    generateLeafCluster(region, bx, by, bz, leafBlock, altLeafBlock, random);

                    // AHORA HAY UN POCO MÁS DE FRUTOS COLGANTES COMO PEDISTE (Max 15)
                    if (isCelestial && fruitsPlaced < 20 && random.nextInt(2) == 0) {
                        if (region.isInRegion(bx, by - 2, bz) && region.getType(bx, by - 2, bz) == Material.AIR) {
                            region.setType(bx, by - 2, bz, Material.GRAY_GLAZED_TERRACOTTA);
                            fruitsPlaced++;
                        }
                    }
                }
            }
        }
        generateLeafCluster(region, x, y + height, z, leafBlock, altLeafBlock, random);
    }

    private void generateLeafCluster(LimitedRegion region, int cx, int cy, int cz, Material leaf, Material altLeaf, Random random) {
        int radius = 2 + random.nextInt(2);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                        if (random.nextInt(100) < 85) {
                            Material toPlace = (altLeaf != null && random.nextInt(5) == 0) ? altLeaf : leaf;
                            int worldX = cx + dx;
                            int worldY = cy + dy;
                            int worldZ = cz + dz;

                            if (region.isInRegion(worldX, worldY, worldZ) && region.getType(worldX, worldY, worldZ) == Material.AIR) {
                                region.setType(worldX, worldY, worldZ, toPlace);
                            }
                        }
                    }
                }
            }
        }
    }

    private void safeSetBlock(LimitedRegion region, int x, int y, int z, Material material) {
        if (region.isInRegion(x, y, z)) region.setType(x, y, z, material);
    }

    private Material getTrunkMaterial(BiomeType biomeType) {
        switch (biomeType) {
            case CELESTIAL_FOREST: return Material.PRISMARINE_BRICKS;
            case OBSIDIAN_PEAKS: return Material.OBSIDIAN;
            case CRIMSON_WASTES: return Material.RED_NETHER_BRICKS;
            default: return Material.POLISHED_BLACKSTONE_BRICKS;
        }
    }

    private Material getLeafMaterial(BiomeType biomeType) {
        switch (biomeType) {
            case CELESTIAL_FOREST: return Material.VERDANT_FROGLIGHT;
            case OBSIDIAN_PEAKS: return Material.WARPED_WART_BLOCK;
            case CRIMSON_WASTES: return Material.SHROOMLIGHT;
            default: return Material.SEA_LANTERN;
        }
    }
}