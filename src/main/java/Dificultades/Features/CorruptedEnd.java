package Dificultades.Features;

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
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.noise.SimplexOctaveGenerator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class CorruptedEnd implements Listener {
    private final JavaPlugin plugin;
    public static final String WORLD_NAME = "corrupted_end";
    public World corruptedWorld;
    private Map<String, Clipboard> loadedSchematics = new HashMap<>();

    private final int STRUCTURE_SPACING = 500;
    private final Map<Long, Boolean> generatedStructures = new HashMap<>();

    public CorruptedEnd(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void createCorruptedWorld() {
        corruptedWorld = Bukkit.getWorld(WORLD_NAME);
        if (corruptedWorld != null) return;

        WorldCreator creator = new WorldCreator(WORLD_NAME);
        creator.environment(World.Environment.THE_END);
        creator.generator(new CorruptedEndGenerator());

        try {
            corruptedWorld = creator.createWorld();
            if (corruptedWorld != null) {
                corruptedWorld.setSpawnLocation(0, 100, 0);
                createReturnPortal();
                plugin.getLogger().info("Mundo Corrupted End creado exitosamente!");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error al crear el mundo Corrupted End: " + e.getMessage());
        }
    }

    private void createReturnPortal() {
        Location spawnLoc = new Location(corruptedWorld, 0, 100, 0);

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                spawnLoc.clone().add(x, -1, z).getBlock().setType(Material.END_STONE_BRICKS);
                spawnLoc.clone().add(x, -2, z).getBlock().setType(Material.END_STONE);
            }
        }

        createPortalStructure(spawnLoc, true);
    }

    public void loadSchematics() {
        File schemFolder = new File(plugin.getDataFolder(), "schem");
        if (!schemFolder.exists()) return;

        String[] schemNames = {"island1", "island2", "island3", "island4", "island5", "ancientend1", "ancientend2"};

        for (String schemName : schemNames) {
            File schemFile = new File(schemFolder, schemName + ".schem");
            if (schemFile.exists()) {
                try {
                    ClipboardFormat format = ClipboardFormats.findByFile(schemFile);
                    if (format != null) {
                        try (ClipboardReader reader = format.getReader(new FileInputStream(schemFile))) {
                            Clipboard clipboard = reader.read();
                            loadedSchematics.put(schemName, clipboard);
                            plugin.getLogger().info("Schematic cargado: " + schemName);
                        }
                    }
                } catch (IOException e) {
                    plugin.getLogger().warning("Error cargando schematic " + schemName + ": " + e.getMessage());
                }
            } else {
                plugin.getLogger().warning("Schematic no encontrado: " + schemName + ".schem");
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.getWorld().getName().equals(WORLD_NAME)) return;
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

        if (random.nextInt(100) < 30) {
            Location structureLoc = findStructureLocation(chunk, random);
            if (structureLoc != null) {
                String[] structureTypes = {"island1", "island2", "island3", "island4", "island5"};
                String structure = structureTypes[random.nextInt(structureTypes.length)];
                pasteSchematic(structure, structureLoc);
            }
        }

        if (random.nextInt(100) < 5) {
            Location structureLoc = findStructureLocation(chunk, random);
            if (structureLoc != null) {
                String specialStructure = isCyanBiome(structureLoc) ? "ancientend1" : "ancientend2";
                pasteSchematic(specialStructure, structureLoc);
            }
        }
    }


    private Location findStructureLocation(Chunk chunk, Random random) {
        int baseX = (chunk.getX() * 16 / STRUCTURE_SPACING) * STRUCTURE_SPACING;
        int baseZ = (chunk.getZ() * 16 / STRUCTURE_SPACING) * STRUCTURE_SPACING;

        int x = baseX + random.nextInt(STRUCTURE_SPACING);
        int z = baseZ + random.nextInt(STRUCTURE_SPACING);

        World world = chunk.getWorld();
        int y = world.getHighestBlockYAt(x, z);

        if (y > 50 && y < 120) {
            return new Location(world, x, y + 1, z);
        }
        return null;
    }

    private boolean isCyanBiome(Location loc) {
        return ((loc.getBlockX() + loc.getBlockZ()) % 100) < 50;
    }

    private void pasteSchematic(String schemName, Location location) {
        Clipboard clipboard = loadedSchematics.get(schemName);
        if (clipboard == null) return;

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                    .ignoreAirBlocks(false)
                    .build();

            Operations.complete(operation);
            plugin.getLogger().info("Estructura " + schemName + " generada en " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
        } catch (Exception e) {
            plugin.getLogger().warning("Error pegando schematic " + schemName + ": " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || event.getItem().getType() != Material.FLINT_AND_STEEL) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BEDROCK) return;

        if (isCustomPortal(block.getLocation())) {
            activatePortal(block.getLocation());
            event.setCancelled(true);
        }
    }

/*    private boolean isValidPortalFrame(Location loc) {
        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 7; x++) {
                Location checkLoc = loc.clone().add(x - 3, y, 0);
                Material type = checkLoc.getBlock().getType();

                boolean shouldBeBedrock = (x == 0 || x == 6 || y == 0 || y == 11);
                if (shouldBeBedrock && type != Material.BEDROCK) return false;
                if (!shouldBeBedrock && type != Material.AIR) return false;
            }
        }
        return true;
    }*/

    private void activatePortal(Location loc) {
        for (int y = 1; y < 11; y++) {
            for (int x = 1; x < 6; x++) {
                Location fillLoc = loc.clone().add(x - 3, y, 0);
                fillLoc.getBlock().setMetadata("CustomPortal", new FixedMetadataValue(plugin, true));
                fillLoc.getBlock().setType(Material.END_GATEWAY);
            }
        }

        loc.getWorld().playSound(loc, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0f, 1.0f);
        loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0, 6, 0), 50, 1.5, 3, 1.5);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlock().equals(event.getTo().getBlock())) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getTo().getBlock();

        if (block.getType() == Material.END_GATEWAY) {
            if (isCustomPortal(block.getLocation())) {
                if (player.getWorld().getName().equals(WORLD_NAME)) {
                    World overworld = Bukkit.getWorlds().get(0);
                    Location overworldSpawn = new Location(overworld, 0, overworld.getHighestBlockYAt(0, 0) + 1, 0);
                    player.teleport(overworldSpawn);
                } else if (!player.getWorld().getEnvironment().equals(World.Environment.THE_END)) {
                    if (corruptedWorld == null) {
                        createCorruptedWorld();
                    }
                    player.teleport(corruptedWorld.getSpawnLocation());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();

        if (event.getTo() != null && event.getTo().getWorld().getName().equals(WORLD_NAME)) {
            return;
        }

        if (player.getWorld().getName().equals(WORLD_NAME) &&
                !isCustomPortal(event.getFrom())) {
            event.setCancelled(true);
            World overworld = Bukkit.getWorlds().get(0);
            Location overworldSpawn = new Location(overworld, 0, overworld.getHighestBlockYAt(0, 0) + 1, 0);
            player.teleport(overworldSpawn);
        }
    }

    private boolean isCustomPortal(Location loc) {
        if (loc.getBlock().hasMetadata("CustomPortal")) {
            return true;
        }

        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 7; x++) {
                Location checkLoc = loc.clone().add(x - 3, y, 0);
                Material type = checkLoc.getBlock().getType();

                boolean shouldBeBedrock = (x == 0 || x == 6 || y == 0 || y == 11);
                if (shouldBeBedrock && type != Material.BEDROCK) return false;
                if (!shouldBeBedrock && (y >= 1 && y <= 10 && x >= 1 && x <= 5)) {
                    if (type != Material.AIR && type != Material.END_GATEWAY) return false;
                }
            }
        }
        return true;
    }

    @EventHandler
    public void onPlayerMoveDimesion(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.getWorld().getName().equals(WORLD_NAME)) return;

        Location loc = player.getLocation();
        Block block = loc.getBlock();

        if (block.getType() == Material.WATER || block.getRelative(BlockFace.DOWN).getType() == Material.WATER) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));

            player.playSound(loc, Sound.BLOCK_HONEY_BLOCK_STEP, 0.5f, 1.0f);
        }
    }

    @EventHandler
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (player.getWorld().getName().equals(WORLD_NAME) && event.isGliding()) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "¡No puedes usar elytras en esta dimensión!");
            }
        }
    }

    public void spawnParticles() {
        for (Player player : corruptedWorld.getPlayers()) {
            Location loc = player.getLocation();
            Random random = new Random();

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

    private void createPortalStructure(Location loc, boolean isReturn) {
        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 7; x++) {
                Location buildLoc = loc.clone().add(x - 3, y, 0);

                if (x == 0 || x == 6 || y == 0 || y == 11) {
                    buildLoc.getBlock().setType(Material.BEDROCK);
                } else if (isReturn) {
                    buildLoc.getBlock().setType(Material.END_GATEWAY);
                }
            }
        }
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        if (event.getWorld().getName().equals(WORLD_NAME)) {
            World world = event.getWorld();

            world.setSpawnFlags(true, true);

            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnCustomMobs(world);
                }
            }.runTaskTimer(plugin, 100L, 200L);
        }
    }

    private void spawnCustomMobs(World world) {
        Random random = new Random();

        for (Player player : world.getPlayers()) {
            Location playerLoc = player.getLocation();

            if (random.nextInt(100) < 10) {
                Location spawnLoc = playerLoc.clone().add(
                        random.nextInt(20) - 10,
                        random.nextInt(5),
                        random.nextInt(20) - 10
                );

                while (spawnLoc.getBlock().getType() == Material.AIR && spawnLoc.getY() > 0) {
                    spawnLoc.subtract(0, 1, 0);
                }
                spawnLoc.add(0, 1, 0);

                EntityType[] mobTypes = {
                        EntityType.SKELETON, EntityType.VEX, EntityType.GHAST,
                        EntityType.CREEPER, EntityType.ZOMBIE, EntityType.SPIDER,
                        EntityType.PHANTOM, EntityType.WARDEN
                };

                EntityType mobType = mobTypes[random.nextInt(mobTypes.length)];
                world.spawnEntity(spawnLoc, mobType);
            }
        }
    }

    // Generador de mundo interno para mantener pocas clases
    public class CorruptedEndGenerator extends ChunkGenerator {
        private static final int HEIGHT = 100;
        private final SplittableRandom random = new SplittableRandom();

        @Override
        public ChunkData generateChunkData(World world, Random cRandom, int chunkX, int chunkZ, BiomeGrid biomes) {
            SimplexOctaveGenerator islandGenerator = new SimplexOctaveGenerator(new Random(world.getSeed()), 8);
            islandGenerator.setScale(0.02D);

            SimplexOctaveGenerator crimsonGenerator = new SimplexOctaveGenerator(new Random(world.getSeed() + 1), 4);
            crimsonGenerator.setScale(0.005D);

            ChunkData chunk = createChunkData(world);

            for (int X = 0; X < 16; X++) {
                for (int Z = 0; Z < 16; Z++) {
                    int globalX = chunkX * 16 + X;
                    int globalZ = chunkZ * 16 + Z;

                    // Ruido para la forma de la isla
                    int noise = (int) (islandGenerator.noise(globalX, globalZ, 0.5D, 0.5D) * 15);

                    if (noise <= 0) {
                        continue;
                    }

                    // Determinar si es zona Crimson (usando un rango continuo en lugar de división modular)
                    double crimsonValue = crimsonGenerator.noise(globalX, globalZ, 0.5D, 0.5D);
                    boolean isCrimsonZone = crimsonValue > 0.3;

                    // Suavizar transiciones entre biomas
                    double blendFactor = 0.0;
                    if (crimsonValue > 0.2 && crimsonValue < 0.4) {
                        blendFactor = (crimsonValue - 0.2) / 0.2;
                    } else if (crimsonValue >= 0.4) {
                        blendFactor = 1.0;
                    }

                    // Bloques base de la isla (con transición suave)
                    Material primaryBlock = Material.SCULK;
                    Material secondaryBlock = Material.CRIMSON_HYPHAE;

                    for (int i = 0; i < noise / 3; i++) {
                        if (random.nextDouble() < blendFactor) {
                            chunk.setBlock(X, i + HEIGHT, Z, secondaryBlock);
                        } else {
                            chunk.setBlock(X, i + HEIGHT, Z, primaryBlock);
                        }
                    }

                    for (int i = 0; i < noise; i++) {
                        if (random.nextDouble() < blendFactor) {
                            chunk.setBlock(X, HEIGHT - i - 1, Z, secondaryBlock);
                        } else {
                            chunk.setBlock(X, HEIGHT - i - 1, Z, primaryBlock);
                        }
                    }

                    // Decoración superficial
                    if (noise > 8 && random.nextInt(13) == 0) {
                        if (blendFactor > 0.7) {
                            Material[] crimsonDecorations = {
                                    Material.SCULK_VEIN,
                                    Material.RED_MUSHROOM,
                                    Material.SCULK_SENSOR,
                                    Material.SCULK_SHRIEKER,
                                    Material.CRIMSON_FUNGUS,
                            };
                            Material decoration = crimsonDecorations[random.nextInt(crimsonDecorations.length)];
                            chunk.setBlock(X, HEIGHT + (noise / 3), Z, decoration);

                        } else {
                            Material sculkBlock = random.nextBoolean() ? Material.SCULK_SENSOR : Material.SCULK_SHRIEKER;
                            chunk.setBlock(X, HEIGHT + (noise / 3), Z, sculkBlock);

                            // 33% de probabilidad de añadir Sculk Catalyst debajo
                            if (random.nextInt(3) == 0) {
                                chunk.setBlock(X, HEIGHT + (noise / 3) - 1, Z, Material.SCULK_CATALYST);
                            }

                            // 20% de probabilidad de añadir Sculk Veins alrededor
                            if (random.nextInt(5) == 0) {
                                for (int dx = -1; dx <= 1; dx++) {
                                    for (int dz = -1; dz <= 1; dz++) {
                                        if ((dx != 0 || dz != 0) && random.nextBoolean() &&
                                                X + dx >= 0 && X + dx < 16 && Z + dz >= 0 && Z + dz < 16) {
                                            chunk.setBlock(X + dx, HEIGHT + (noise / 3), Z + dz, Material.SCULK_VEIN);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return chunk;
        }

        @NotNull
        @Override
        public List<BlockPopulator> getDefaultPopulators(World world) {
            return Collections.singletonList(new TreePopulator());
        }

        public static class TreePopulator extends BlockPopulator {
            private static Set<Coordinates> chunks = ConcurrentHashMap.newKeySet();
            private static Set<Coordinates> unpopulatedChunks = ConcurrentHashMap.newKeySet();

            @Override
            public void populate(World world, Random random, Chunk chunk) {
                int chunkX = chunk.getX();
                int chunkZ = chunk.getZ();
                Coordinates chunkCoordinates = new Coordinates(chunkX, chunkZ);

                if (!chunks.contains(chunkCoordinates)) {
                    chunks.add(chunkCoordinates);
                    unpopulatedChunks.add(chunkCoordinates);
                }

                for (Coordinates unpopulatedChunk : unpopulatedChunks) {
                    if (chunks.contains(unpopulatedChunk.left())
                            && chunks.contains(unpopulatedChunk.right())
                            && chunks.contains(unpopulatedChunk.above())
                            && chunks.contains(unpopulatedChunk.below())
                            && chunks.contains(unpopulatedChunk.upperLeft())
                            && chunks.contains(unpopulatedChunk.upperRight())
                            && chunks.contains(unpopulatedChunk.lowerLeft())
                            && chunks.contains(unpopulatedChunk.lowerRight())) {
                        actuallyPopulate(world, random, world.getChunkAt(unpopulatedChunk.x, unpopulatedChunk.z));
                        unpopulatedChunks.remove(unpopulatedChunk);
                    }
                }
            }

            private void actuallyPopulate(World world, Random random, Chunk chunk) {
                int x = random.nextInt(16);
                int z = random.nextInt(16);
                int y = world.getMaxHeight() - 1;

                while (y > 0 && chunk.getBlock(x, y, z).getType() == Material.AIR) {
                    --y;
                }

                if (y > 0 && y < 255 && y >= 100 && y < 105) {
                    // Determinar si estamos en una isla Crimson
                    boolean isCrimsonIsland = chunk.getBlock(x, y, z).getType() == Material.CRIMSON_HYPHAE;

                    world.generateTree(chunk.getBlock(x, y + 1, z).getLocation(), TreeType.CHORUS_PLANT, new BlockChangeDelegate() {
                        @Override
                        public boolean setBlockData(int i, int i1, int i2, @NotNull BlockData blockData) {
                            Material replacement;
                            if (blockData.getMaterial() == Material.CHORUS_FLOWER) {
                                replacement = isCrimsonIsland ? Material.SHROOMLIGHT : Material.SEA_LANTERN;
                            } else if (blockData.getMaterial() == Material.CHORUS_PLANT) {
                                replacement = isCrimsonIsland ? Material.RED_NETHER_BRICK_WALL : Material.BLACKSTONE_WALL;
                            } else {
                                return true;
                            }
                            world.getBlockAt(i, i1, i2).setType(replacement);
                            return true;
                        }

                        @Override
                        public @NotNull BlockData getBlockData(int i, int i1, int i2) {
                            return null;
                        }

                        @Override
                        public int getHeight() {
                            return 255;
                        }

                        @Override
                        public boolean isEmpty(int i, int i1, int i2) {
                            return false;
                        }
                    });
                }
            }

            private class Coordinates {
                public final int x;
                public final int z;

                public Coordinates(int x, int z) {
                    this.x = x;
                    this.z = z;
                }

                public Coordinates left() {
                    return new Coordinates(x - 1, z);
                }

                public Coordinates right() {
                    return new Coordinates(x + 1, z);
                }

                public Coordinates above() {
                    return new Coordinates(x, z - 1);
                }

                public Coordinates below() {
                    return new Coordinates(x, z + 1);
                }

                public Coordinates upperLeft() {
                    return new Coordinates(x - 1, z - 1);
                }

                public Coordinates upperRight() {
                    return new Coordinates(x + 1, z - 1);
                }

                public Coordinates lowerLeft() {
                    return new Coordinates(x - 1, z + 1);
                }

                public Coordinates lowerRight() {
                    return new Coordinates(x + 1, z + 1);
                }

                @Override
                public int hashCode() {
                    return (x + z) * (x + z + 1) / 2 + x;
                }

                @Override
                public boolean equals(Object obj) {
                    if (this == obj)
                        return true;
                    if (obj == null)
                        return false;
                    if (getClass() != obj.getClass())
                        return false;
                    Coordinates other = (Coordinates) obj;
                    if (x != other.x)
                        return false;
                    return z == other.z;
                }
            }
        }
    }
}
