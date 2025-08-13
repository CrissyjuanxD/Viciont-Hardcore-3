package Commands;

import Dificultades.CustomMobs.*;
import Handlers.DayHandler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CustomSpawnerHandler implements Listener {
    private final JavaPlugin plugin;
    private final DayHandler dayHandler;
    private final NamespacedKey spawnerKey;

    private final Map<UUID, ItemStack> editingSpawners = new ConcurrentHashMap<>();
    private final Map<UUID, String> editingProperties = new ConcurrentHashMap<>();
    private final Map<UUID, String> playersWaitingForInput = new ConcurrentHashMap<>();

    // Nuevo: Mapa para trackear spawners con spawn_custom activo
    private final Map<Location, CustomSpawnerData> activeCustomSpawners = new ConcurrentHashMap<>();
    private BukkitRunnable customSpawnTask;

    private long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL = 60000;

    // Instancias de los mobs
    private final Bombita bombitaSpawner;
    private final Iceologer iceologerSpawner;
    private final CorruptedZombies corruptedZombieSpawner;
    private final CorruptedSpider corruptedSpider;
    private final QueenBeeHandler queenBeeHandler;
    private final HellishBeeHandler hellishBeeHandler;
    private final InfestedBeeHandler infestedBeeHandler;
    private final GuardianBlaze guardianBlaze;
    private final GuardianCorruptedSkeleton guardianCorruptedSkeleton;
    private final CorruptedSkeleton corruptedSkeleton;
    private final CorruptedInfernalSpider corruptedInfernalSpider;
    private final CorruptedCreeper corruptedCreeper;
    private final CorruptedMagmaCube corruptedMagmaCube;
    private final PiglinGlobo piglinGloboSpawner;
    private final BuffBreeze buffBreeze;
    private final InvertedGhast invertedGhast;
    private final NetheriteVexGuardian netheriteVexGuardian;
    private final UltraWitherBossHandler ultraWitherBossHandler;
    private final WhiteEnderman whiteEnderman;
    private final InfernalCreeper infernalCreeper;
    private final UltraCorruptedSpider ultraCorruptedSpider;
    private final FastRavager fastRavager;
    private final BruteImperial bruteImperial;
    private final BatBoom batBoom;
    private final SpectralEye spectralEye;
    private final EnderGhast enderGhast;
    private final EnderCreeper enderCreeper;
    private final EnderSilverfish enderSilverfish;
    private final GuardianShulker guardianShulker;
    private final DarkPhantom darkPhantom;
    private final DarkCreeper darkCreeper;
    private final DarkVex darkVex;
    private final DarkSkeleton darkSkeleton;

    // Clase interna para almacenar datos del spawner personalizado
    private static class CustomSpawnerData {
        String mobType;
        int spawnCount;
        int maxNearby;
        int playerRange;
        int delay;
        int minDelay;
        int maxDelay;
        int spawnRange;
        long lastSpawnTime;
        long nextSpawnTime;

        CustomSpawnerData(String mobType, Map<String, Integer> config) {
            this.mobType = mobType;
            this.spawnCount = config.get("spawn_count");
            this.maxNearby = config.get("max_nearby");
            this.playerRange = config.get("player_range");
            this.delay = config.get("delay");
            this.minDelay = config.get("min_delay");
            this.maxDelay = config.get("max_delay");
            this.spawnRange = config.get("spawn_range");
            this.lastSpawnTime = System.currentTimeMillis();
            this.nextSpawnTime = this.lastSpawnTime + (this.delay * 50L); // Convertir ticks a ms
        }
    }

    public CustomSpawnerHandler(JavaPlugin plugin, DayHandler dayHandler) {
        this.plugin = plugin;
        this.dayHandler = dayHandler;
        this.spawnerKey = new NamespacedKey(plugin, "custom_spawner");

        // Inicializar todas las instancias de mobs
        this.bombitaSpawner = new Bombita(plugin);
        this.iceologerSpawner = new Iceologer(plugin);
        this.corruptedZombieSpawner = new CorruptedZombies(plugin);
        this.corruptedSpider = new CorruptedSpider(plugin);
        this.queenBeeHandler = new QueenBeeHandler(plugin);
        this.hellishBeeHandler = new HellishBeeHandler(plugin);
        this.infestedBeeHandler = new InfestedBeeHandler(plugin);
        this.guardianBlaze = new GuardianBlaze(plugin);
        this.guardianCorruptedSkeleton = new GuardianCorruptedSkeleton(plugin);
        this.corruptedSkeleton = new CorruptedSkeleton(plugin, dayHandler);
        this.corruptedInfernalSpider = new CorruptedInfernalSpider(plugin);
        this.corruptedCreeper = new CorruptedCreeper(plugin);
        this.corruptedMagmaCube = new CorruptedMagmaCube(plugin);
        this.piglinGloboSpawner = new PiglinGlobo(plugin);
        this.buffBreeze = new BuffBreeze(plugin);
        this.invertedGhast = new InvertedGhast(plugin);
        this.netheriteVexGuardian = new NetheriteVexGuardian(plugin);
        this.ultraWitherBossHandler = new UltraWitherBossHandler(plugin);
        this.whiteEnderman = new WhiteEnderman(plugin);
        this.infernalCreeper = new InfernalCreeper(plugin);
        this.ultraCorruptedSpider = new UltraCorruptedSpider(plugin);
        this.fastRavager = new FastRavager(plugin);
        this.bruteImperial = new BruteImperial(plugin);
        this.batBoom = new BatBoom(plugin);
        this.spectralEye = new SpectralEye(plugin);
        this.enderGhast = new EnderGhast(plugin);
        this.enderCreeper = new EnderCreeper(plugin);
        this.enderSilverfish = new EnderSilverfish(plugin);
        this.guardianShulker = new GuardianShulker(plugin);
        this.darkPhantom = new DarkPhantom(plugin);
        this.darkCreeper = new DarkCreeper(plugin);
        this.darkVex = new DarkVex(plugin);
        this.darkSkeleton = new DarkSkeleton(plugin);

        // Inicializar el sistema de spawn personalizado
        startCustomSpawnTask();

        Bukkit.getScheduler().runTaskLater(plugin, this::loadAllCustomSpawners, 100L);
    }

    // NUEVO: Evento para cargar spawners cuando se cargan chunks
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // Solo procesar chunks recién cargados, no chunks generados por primera vez
        if (event.isNewChunk()) return;

        Chunk chunk = event.getChunk();

        new BukkitRunnable() {
            @Override
            public void run() {
                int spawnersFound = 0;
                for (BlockState blockState : chunk.getTileEntities()) {
                    if (blockState instanceof CreatureSpawner) {
                        CreatureSpawner spawner = (CreatureSpawner) blockState;
                        if (isCustomSpawner(spawner)) {
                            Location loc = spawner.getLocation();
                            // Verificar si el spawner ya está registrado
                            if (!activeCustomSpawners.containsKey(loc)) {
                                registerCustomSpawner(spawner);
                                spawnersFound++;
                            }
                        }
                    }
                }

                if (spawnersFound > 0) {
                    plugin.getLogger().info("Registrados " + spawnersFound + " spawners custom en chunk cargado: " +
                            chunk.getX() + ", " + chunk.getZ());
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();

        for (BlockState blockState : chunk.getTileEntities()) {
            if (blockState instanceof CreatureSpawner) {
                CreatureSpawner spawner = (CreatureSpawner) blockState;
                Location loc = spawner.getLocation();
                activeCustomSpawners.remove(loc);
            }
        }
    }

    // Iniciar la tarea que maneja los spawns personalizados
    private void startCustomSpawnTask() {
        customSpawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                processCustomSpawners();
            }
        };
        customSpawnTask.runTaskTimer(plugin, 20L, 20L);
    }

    // Procesar todos los spawners con spawn_custom activo
    private void processCustomSpawners() {
        long timelimp = System.currentTimeMillis();

        // Realizar limpieza periódica cada CLEANUP_INTERVAL
        if (timelimp - lastCleanupTime > CLEANUP_INTERVAL) {
            cleanupInvalidSpawners();
            lastCleanupTime = timelimp;
        }

        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }

        Iterator<Map.Entry<Location, CustomSpawnerData>> iterator = activeCustomSpawners.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Location, CustomSpawnerData> entry = iterator.next();
            Location spawnerLoc = entry.getKey();
            CustomSpawnerData data = entry.getValue();

            // Verificar si el bloque sigue siendo un spawner
            if (spawnerLoc.getBlock().getType() != Material.SPAWNER) {
                iterator.remove();
                continue;
            }

            if (hasSoulTorchAbove(spawnerLoc)) {
                // Mostrar partículas de desactivación y no spawner
                showDeactivatedParticles(spawnerLoc);
                continue;
            }

            // Verificar si es momento de spawn
            long currentTime = System.currentTimeMillis();
            if (currentTime < data.nextSpawnTime) continue;

/*            // Solo procesar si la luz es suficiente (≥8)
            if (spawnerLoc.getBlock().getLightLevel() < 8) continue;*/

            // Verificar si hay jugadores en rango
            if (!isPlayerInRange(spawnerLoc, data.playerRange)) continue;

            // Verificar límite de entidades cercanas
            if (getNearbyEntitiesCount(spawnerLoc, data.spawnRange) >= data.maxNearby) continue;

            // Realizar spawn personalizado
            performCustomSpawn(spawnerLoc, data);

            // Actualizar próximo tiempo de spawn
            int randomDelay = data.minDelay + (int)(Math.random() * (data.maxDelay - data.minDelay));
            data.nextSpawnTime = currentTime + (randomDelay * 50L);
        }
    }

    private void cleanupInvalidSpawners() {
        int removedCount = 0;
        Iterator<Map.Entry<Location, CustomSpawnerData>> iterator = activeCustomSpawners.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Location, CustomSpawnerData> entry = iterator.next();
            Location loc = entry.getKey();

            // Verificar si el chunk está cargado
            if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                iterator.remove();
                removedCount++;
                continue;
            }

            // Verificar si el bloque sigue siendo un spawner
            if (loc.getBlock().getType() != Material.SPAWNER) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            plugin.getLogger().info("Limpieza periódica: Eliminados " + removedCount + " spawners inválidos");
        }
    }

    private boolean hasSoulTorchAbove(Location spawnerLoc) {
        Block blockAbove = spawnerLoc.getBlock().getRelative(0, 1, 0);
        return blockAbove.getType() == Material.SOUL_TORCH || blockAbove.getType() == Material.SOUL_WALL_TORCH;
    }

    // NUEVO: Mostrar partículas de desactivación
    private void showDeactivatedParticles(Location spawnerLoc) {
        World world = spawnerLoc.getWorld();
        if (world == null) return;

        Location particleLoc = spawnerLoc.clone().add(0.5, 0.5, 0.5);

        // Partículas de desactivación (humo gris y partículas de alma)
        world.spawnParticle(Particle.LARGE_SMOKE, particleLoc, 3, 0.3, 0.3, 0.3, 0.01);
        world.spawnParticle(Particle.SOUL, particleLoc, 5, 0.2, 0.2, 0.2, 0.02);

        // Sonido sutil de desactivación cada cierto tiempo
        Random random = new Random();
        if (random.nextInt(20) == 0) { // 5% de probabilidad cada segundo
            world.playSound(spawnerLoc, Sound.BLOCK_SOUL_SAND_BREAK, 0.3f, 0.8f);
        }
    }

    // Verificar si hay jugadores en rango
    private boolean isPlayerInRange(Location spawnerLoc, int range) {
        World world = spawnerLoc.getWorld();
        if (world == null) return false;

        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(spawnerLoc) <= range) {
                return true;
            }
        }
        return false;
    }

    // Contar entidades cercanas
    private int getNearbyEntitiesCount(Location spawnerLoc, int range) {
        World world = spawnerLoc.getWorld();
        if (world == null) return 0;

        int count = 0;
        for (Entity entity : world.getNearbyEntities(spawnerLoc, range, range, range)) {
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                count++;
            }
        }
        return count;
    }

    // Realizar spawn personalizado
    private void performCustomSpawn(Location spawnerLoc, CustomSpawnerData data) {
        // Spawn count aleatorio entre 1 y el valor configurado
        int randomSpawnCount = 1 + (int)(Math.random() * data.spawnCount);

        for (int i = 0; i < randomSpawnCount; i++) {
            // Encontrar una ubicación válida para spawn dentro del rango
            Location spawnLoc = findValidSpawnLocation(spawnerLoc, data.spawnRange);
            if (spawnLoc == null) continue;

            // Spawn del mob
            if (data.mobType.startsWith("vanilla_")) {
                spawnVanillaMob(data.mobType, spawnLoc);
            } else {
                spawnCustomMob(data.mobType, spawnLoc);
            }

            // Efectos visuales
            spawnLoc.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, spawnLoc, 10, 0.5, 0.5, 0.5, 0.1);
            spawnLoc.getWorld().playSound(spawnLoc, Sound.BLOCK_SCULK_BREAK, 3.0f, 0.1f);
        }
    }

    // Encontrar ubicación válida para spawn
    private Location findValidSpawnLocation(Location spawnerLoc, int range) {
        World world = spawnerLoc.getWorld();
        if (world == null) return null;

        Random random = new Random();

        for (int attempts = 0; attempts < 10; attempts++) {
            double x = spawnerLoc.getX() + (random.nextDouble() * range * 2 - range);
            double z = spawnerLoc.getZ() + (random.nextDouble() * range * 2 - range);
            double y = spawnerLoc.getY() + (random.nextDouble() * 3 - 1);

            Location testLoc = new Location(world, x, y, z);

            // Verificar que la ubicación sea válida
            if (isValidSpawnLocation(testLoc)) {
                return testLoc;
            }
        }

        return spawnerLoc.clone().add(0.5, 1, 0.5); // Fallback a encima del spawner
    }

    // Verificar si la ubicación es válida para spawn
    private boolean isValidSpawnLocation(Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;

        Block block = world.getBlockAt(loc);
        Block above = world.getBlockAt(loc.clone().add(0, 1, 0));
        Block below = world.getBlockAt(loc.clone().add(0, -1, 0));

        return !block.getType().isSolid() &&
                !above.getType().isSolid() &&
                below.getType().isSolid();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() != Material.SPAWNER) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer itemContainer = meta.getPersistentDataContainer();
        if (!itemContainer.has(spawnerKey, PersistentDataType.STRING)) return;

        String mobType = itemContainer.get(spawnerKey, PersistentDataType.STRING);
        if (mobType == null) return;

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (block.getType() == Material.SPAWNER) {
                    CreatureSpawner spawner = (CreatureSpawner) block.getState();
                    Map<String, Integer> config = readSpawnerConfig(item);
                    boolean spawnCustom = readSpawnCustomConfig(item);

                    // Configurar el tipo base
                    EntityType baseType;
                    if (mobType.startsWith("vanilla_")) {
                        // Extraer el tipo de mob vanilla (ej: "vanilla_zombie" -> "zombie")
                        String vanillaType = mobType.substring(8);
                        try {
                            baseType = EntityType.valueOf(vanillaType.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            baseType = EntityType.PIG; // Fallback si el tipo no es válido
                        }
                    } else {
                        baseType = getBaseEntityType(mobType);
                    }
                    spawner.setSpawnedType(baseType != null ? baseType : EntityType.PIG);

                    // Guardar todos los datos en el bloque
                    saveSpawnerData(spawner, mobType, config, spawnCustom);

                    // Configurar nombre custom si es necesario
                    String customName = getCustomMobName(mobType);
                    if (customName != null) {
                        applyCustomNameToSpawner(spawner, customName);
                    }

                    // Añadir al sistema de spawn personalizado si está activo
                    if (spawnCustom) {
                        activeCustomSpawners.put(block.getLocation(), new CustomSpawnerData(mobType, config));
                    }

                    player.sendMessage(ChatColor.GREEN + "¡Spawner de " + mobType + " colocado correctamente!");

                    // Efectos visuales
                    Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                    loc.getWorld().spawnParticle(Particle.PORTAL, loc, 50, 0.5, 0.5, 0.5, 0.1);
                    loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
                }
            }
        }.runTaskLater(plugin, 2L);
    }

    public void loadAllCustomSpawners() {
        plugin.getLogger().info("Cargando spawners custom...");
        int totalSpawners = 0;

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState blockState : chunk.getTileEntities()) {
                    if (blockState instanceof CreatureSpawner) {
                        CreatureSpawner spawner = (CreatureSpawner) blockState;
                        if (isCustomSpawner(spawner)) {
                            registerCustomSpawner(spawner);
                            totalSpawners++;
                        }
                    }
                }
            }
        }

        plugin.getLogger().info("Cargados " + totalSpawners + " spawners custom desde chunks cargados");
        plugin.getLogger().info("Los spawners en chunks no cargados se registrarán automáticamente cuando los chunks se carguen");
    }

    private void registerCustomSpawner(CreatureSpawner spawner) {
        Location loc = spawner.getLocation();
        if (activeCustomSpawners.containsKey(loc)) {
            return;
        }

        PersistentDataContainer container = spawner.getPersistentDataContainer();
        String mobType = container.get(spawnerKey, PersistentDataType.STRING);
        boolean spawnCustom = container.has(new NamespacedKey(plugin, "spawn_custom"), PersistentDataType.BYTE) &&
                container.get(new NamespacedKey(plugin, "spawn_custom"), PersistentDataType.BYTE) == 1;

        if (mobType != null && spawnCustom) {
            Map<String, Integer> config = loadSpawnerData(spawner);
            activeCustomSpawners.put(loc, new CustomSpawnerData(mobType, config));
        }
    }

    private Map<String, Integer> readSpawnerConfig(ItemStack spawnerItem) {
        Map<String, Integer> config = new HashMap<>();
        // Valores por defecto
        config.put("spawn_count", 4);
        config.put("max_nearby", 6);
        config.put("player_range", 20);
        config.put("delay", 40);
        config.put("min_delay", 200);
        config.put("max_delay", 600);
        config.put("spawn_range", 4);

        if (spawnerItem.hasItemMeta() && spawnerItem.getItemMeta().hasLore()) {
            for (String line : spawnerItem.getItemMeta().getLore()) {
                if (line.contains(":")) {
                    String[] parts = ChatColor.stripColor(line).split(":");
                    String key = parts[0].trim().toLowerCase().replace(" ", "_");
                    try {
                        int value = Integer.parseInt(parts[1].trim());
                        config.put(key, value);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        return config;
    }

    // Nuevo método para leer configuración de spawn_custom
    private boolean readSpawnCustomConfig(ItemStack spawnerItem) {
        if (spawnerItem.hasItemMeta() && spawnerItem.getItemMeta().hasLore()) {
            for (String line : spawnerItem.getItemMeta().getLore()) {
                if (ChatColor.stripColor(line).toLowerCase().contains("spawn custom:")) {
                    String[] parts = ChatColor.stripColor(line).split(":");
                    if (parts.length > 1) {
                        String value = parts[1].trim().toLowerCase();
                        return value.equals("true") || value.equals("activado") || value.equals("enabled");
                    }
                }
            }
        }
        return false; // Por defecto desactivado
    }

    @EventHandler
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        CreatureSpawner spawner = (CreatureSpawner) event.getSpawner().getBlock().getState();
        if (!isCustomSpawner(spawner)) return;

        PersistentDataContainer container = spawner.getPersistentDataContainer();
        String mobType = container.get(spawnerKey, PersistentDataType.STRING);
        boolean spawnCustom = container.has(new NamespacedKey(plugin, "spawn_custom"), PersistentDataType.BYTE) &&
                container.get(new NamespacedKey(plugin, "spawn_custom"), PersistentDataType.BYTE) == 1;

        if (spawnCustom /*&& spawner.getBlock().getLightLevel() >= 8*/) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        Location location = event.getLocation();

        if (mobType.startsWith("vanilla_")) {
            spawnVanillaMob(mobType, location);
        } else {
            spawnCustomMob(mobType, location);
        }

        location.getWorld().spawnParticle(Particle.POOF, location, 10, 0.5, 0.5, 0.5, 0.1);
        location.getWorld().playSound(location, Sound.BLOCK_SCULK_BREAK, 3.0f, 0.1f);
    }

    private void spawnVanillaMob(String mobType, Location location) {
        String vanillaType = mobType.substring(8);

        try {
            EntityType entityType = EntityType.valueOf(vanillaType.toUpperCase());

            // Spawnear el mob vanilla con nombre personalizado si es necesario
            LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, entityType);

        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Tipo de mob vanilla no válido: " + vanillaType);
        }
    }

    private void spawnCustomMob(String mobType, Location location) {
        switch (mobType.toLowerCase()) {
            case "bombita":
                bombitaSpawner.spawnBombita(location);
                break;
            case "iceologer":
                iceologerSpawner.spawnIceologer(location);
                break;
            case "corruptedzombie":
                corruptedZombieSpawner.spawnCorruptedZombie(location);
                break;
            case "corruptedspider":
                corruptedSpider.spawnCorruptedSpider(location);
                break;
            case "queenbee":
                queenBeeHandler.spawnQueenBee(location);
                break;
            case "hellishbee":
                hellishBeeHandler.spawnHellishBee(location);
                break;
            case "infestedbee":
                infestedBeeHandler.spawnInfestedBee(location);
                break;
            case "guardianblaze":
                guardianBlaze.spawnGuardianBlaze(location);
                break;
            case "guardiancorruptedskeleton":
                guardianCorruptedSkeleton.spawnGuardianCorruptedSkeleton(location);
                break;
            case "corruptedskeleton":
                corruptedSkeleton.spawnCorruptedSkeleton(location, null);
                break;
            case "corruptedinfernalspider":
                corruptedInfernalSpider.spawnCorruptedInfernalSpider(location);
                break;
            case "corruptedcreeper":
                corruptedCreeper.spawnCorruptedCreeper(location);
                break;
            case "corruptedmagma":
                corruptedMagmaCube.spawnCorruptedMagmaCube(location);
                break;
            case "piglinglobo":
                piglinGloboSpawner.spawnPiglinGlobo(location);
                break;
            case "buffbreeze":
                buffBreeze.spawnBuffBreeze(location);
                break;
            case "invertedghast":
                invertedGhast.spawnInvertedGhast(location);
                break;
            case "netheritevexguardian":
                netheriteVexGuardian.spawnNetheriteVexGuardian(location);
                break;
            case "ultrawitherboss":
                ultraWitherBossHandler.spawnUltraWither(location);
                break;
            case "whiteenderman":
                whiteEnderman.spawnWhiteEnderman(location);
                break;
            case "infernalcreeper":
                infernalCreeper.spawnInfernalCreeper(location);
                break;
            case "ultracorruptedspider":
                ultraCorruptedSpider.spawnUltraCorruptedSpider(location);
                break;
            case "fastravager":
                fastRavager.spawnFastRavager(location);
                break;
            case "bruteimperial":
                bruteImperial.spawnBruteImperial(location);
                break;
            case "batboom":
                batBoom.spawnBatBoom(location);
                break;
            case "spectraleeye":
                spectralEye.spawnSpectralEye(location);
                break;
            case "enderghast":
                enderGhast.spawnEnderGhast(location);
                break;
            case "endercreeper":
                enderCreeper.spawnEnderCreeper(location);
                break;
            case "endersilverfish":
                enderSilverfish.spawnEnderSilverfish(location);
                break;
            case "guardianshulker":
                guardianShulker.spawnGuardianShulker(location);
                break;
            case "darkphantom":
                darkPhantom.spawnDarkPhantom(location);
                break;
            case "darkcreeper":
                darkCreeper.spawnDarkCreeper(location);
                break;
            case "darkvex":
                darkVex.spawnDarkVex(location);
                break;
            case "darkskeleton":
                darkSkeleton.spawnDarkSkeleton(location);
                break;
            default:
                plugin.getLogger().warning("Tipo de mob desconocido en spawner: " + mobType);
                break;
        }
    }

    private EntityType getBaseEntityType(String mobType) {
        switch (mobType.toLowerCase()) {
            case "bombita":
            case "corruptedcreeper":
            case "infernalcreeper":
            case "endercreeper":
            case "darkcreeper":
                return EntityType.CREEPER;
            case "iceologer":
                return EntityType.ILLUSIONER;
            case "corruptedzombie":
                return EntityType.ZOMBIE;
            case "corruptedspider":
            case "corruptedinfernalspider":
            case "ultracorruptedspider":
                return EntityType.SPIDER;
            case "queenbee":
            case "hellishbee":
            case "infestedbee":
                return EntityType.BEE;
            case "guardianblaze":
                return EntityType.BLAZE;
            case "guardiancorruptedskeleton":
                return EntityType.WITHER_SKELETON;
            case "corruptedskeleton":
            case "darkskeleton":
                return EntityType.SKELETON;
            case "corruptedmagma":
                return EntityType.MAGMA_CUBE;
            case "buffbreeze":
                return EntityType.BREEZE;
            case "invertedghast":
            case "enderghast":
            case "piglinglobo":
                return EntityType.GHAST;
            case "netheritevexguardian":
            case "darkvex":
                return EntityType.VEX;
            case "ultrawitherboss":
                return EntityType.WITHER;
            case "whiteenderman":
                return EntityType.ENDERMAN;
            case "fastravager":
                return EntityType.RAVAGER;
            case "bruteimperial":
                return EntityType.PIGLIN_BRUTE;
            case "batboom":
                return EntityType.BAT;
            case "endersilverfish":
                return EntityType.SILVERFISH;
            case "guardianshulker":
                return EntityType.SHULKER;
            case "darkphantom":
            case "spectraleeye":
                return EntityType.PHANTOM;
            default:
                return null;
        }
    }

    private String getCustomMobName(String mobType) {
        switch (mobType.toLowerCase()) {
            case "bombita":
                return "Bombita";
            case "iceologer":
                return "Iceologer";
            case "corruptedzombie":
                return "Corrupted Zombie";
            case "corruptedspider":
                return "Corrupted Spider";
            case "queenbee":
                return "Abeja Reina";
            case "hellishbee":
                return "Abeja Infernal";
            case "infestedbee":
                return "Infested Bee";
            case "guardianblaze":
                return "Guardian Blaze";
            case "guardiancorruptedskeleton":
                return "Guardian Corrupted Skeleton";
            case "corruptedskeleton":
                return "Corrupted Skeleton";
            case "corruptedinfernalspider":
                return "Corrupted Infernal Spider";
            case "corruptedcreeper":
                return "Corrupted Creeper";
            case "corruptedmagma":
                return "Corrupted Magma Cube";
            case "piglinglobo":
                return "Piglin Globo";
            case "buffbreeze":
                return "Buff Breeze";
            case "invertedghast":
                return "Inverted Ghast";
            case "netheritevexguardian":
                return "Netherite Vex Guardian";
            case "ultrawitherboss":
                return "Corrupted Wither Boss";
            case "whiteenderman":
                return "White Enderman";
            case "infernalcreeper":
                return "Infernal Creeper";
            case "ultracorruptedspider":
                return "Ultra Corrupted Spider";
            case "fastravager":
                return "Fast Ravager";
            case "bruteimperial":
                return "Brute Imperial";
            case "batboom":
                return "Bat Boom";
            case "spectraleeye":
                return "Ojo Espectral";
            case "enderghast":
                return "Ender Ghast";
            case "endercreeper":
                return "Ender Creeper";
            case "endersilverfish":
                return "Ender Silverfish";
            case "guardianshulker":
                return "Guardian Shulker";
            case "darkphantom":
                return "Dark Phantom";
            case "darkcreeper":
                return "Dark Creeper";
            case "darkvex":
                return "Dark Vex";
            case "darkskeleton":
                return "Dark Skeleton";
            default:
                return null;
        }
    }

    private void applyCustomNameToSpawner(CreatureSpawner spawner, String customName) {
        // Usar comandos para aplicar el nombre personalizado al spawner
        Location loc = spawner.getLocation();
        String command = String.format(
                "data merge block %d %d %d {SpawnData:{entity:{CustomName:'[{\"text\":\"%s\"}]'}}}",
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), customName
        );

        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) return;
        if (!event.getAction().toString().contains("RIGHT_CLICK")) return;
        if (!event.getPlayer().isSneaking()) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.SPAWNER) return;

        event.setCancelled(true);
        openConfigGUI(event.getPlayer(), item);
    }

    // GUI Methods
    public void openConfigGUI(Player player, ItemStack spawnerItem) {
        Inventory gui = Bukkit.createInventory(player, 18, "Configuración del Spawner"); // Aumentado para spawn_custom

        editingSpawners.put(player.getUniqueId(), spawnerItem);

        Map<String, Integer> currentConfig = readSpawnerConfig(spawnerItem);
        boolean spawnCustom = readSpawnCustomConfig(spawnerItem);

        gui.setItem(0, createConfigItem("Spawn Count", currentConfig.get("spawn_count")));
        gui.setItem(1, createConfigItem("Max Nearby", currentConfig.get("max_nearby")));
        gui.setItem(2, createConfigItem("Player Range", currentConfig.get("player_range")));
        gui.setItem(3, createConfigItem("Initial Delay", currentConfig.get("delay")));
        gui.setItem(4, createConfigItem("Min Delay", currentConfig.get("min_delay")));
        gui.setItem(5, createConfigItem("Max Delay", currentConfig.get("max_delay")));
        gui.setItem(6, createConfigItem("Spawn Range", currentConfig.get("spawn_range")));
        gui.setItem(7, createSpawnCustomItem(spawnCustom)); // Nuevo item para spawn_custom

        player.openInventory(gui);
    }

    private ItemStack createConfigItem(String name, int currentValue) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + name);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Valor actual: " + ChatColor.WHITE + currentValue,
                "",
                ChatColor.YELLOW + "Click para modificar"
        ));
        item.setItemMeta(meta);
        return item;
    }

    // Nuevo método para crear el item de spawn_custom
    private ItemStack createSpawnCustomItem(boolean currentValue) {
        ItemStack item = new ItemStack(currentValue ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Spawn Custom");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Estado actual: " + (currentValue ? ChatColor.GREEN + "Activado" : ChatColor.RED + "Desactivado"),
                "",
                ChatColor.GRAY + "Cuando está activado:",
                ChatColor.GRAY + "- No le afecta el nivel Luz",
                ChatColor.GRAY + "- Control manual del spawn",
                ChatColor.GRAY + "- Spawn count aleatorio (1 a valor)",
                ChatColor.GRAY + "- Spawn mas agresivo",
                ChatColor.GRAY + "- Respeta todas las configuraciones",
                "",
                ChatColor.YELLOW + "Click para alternar"
        ));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals("Configuración del Spawner")) {
            handleConfigGUIClick(event, player);
        }
    }

    private void handleConfigGUIClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        if (event.getClickedInventory() == null) return;

        int slot = event.getSlot();
        if (slot < 0 || slot > 7) return;

        if (slot == 7) {
            // Manejar toggle de spawn_custom
            handleSpawnCustomToggle(player);
            return;
        }

        String[] propertyNames = {
                "spawn_count", "max_nearby", "player_range",
                "delay", "min_delay", "max_delay", "spawn_range"
        };

        String property = propertyNames[slot];
        editingProperties.put(player.getUniqueId(), property);

        // Preparar al jugador para recibir input por chat
        playersWaitingForInput.put(player.getUniqueId(), property);
        player.closeInventory();

        player.sendMessage(ChatColor.GOLD + "Ingresa el nuevo valor para " +
                getPropertyDisplayName(property) + ChatColor.GOLD + ":");
        player.sendMessage(ChatColor.GRAY + "(Escribe 'cancelar' para abortar)");
        player.sendMessage(ChatColor.GRAY + "Valor actual: " + ChatColor.WHITE +
                getCurrentValue(player, property));
    }

    // Nuevo método para manejar el toggle de spawn_custom
    private void handleSpawnCustomToggle(Player player) {
        ItemStack spawner = editingSpawners.get(player.getUniqueId());
        if (spawner == null) return;

        boolean currentValue = readSpawnCustomConfig(spawner);
        boolean newValue = !currentValue;

        updateSpawnCustomConfig(spawner, newValue);

        player.sendMessage(ChatColor.GREEN + "Spawn Custom " +
                (newValue ? ChatColor.GREEN + "activado" : ChatColor.RED + "desactivado"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);

        // Reabrir la GUI
        openConfigGUI(player, spawner);
    }

    // Nuevo método para actualizar configuración de spawn_custom
    private void updateSpawnCustomConfig(ItemStack spawner, boolean value) {
        ItemMeta meta = spawner.getItemMeta();
        if (meta == null) return;

        java.util.List<String> lore = meta.hasLore() ? new java.util.ArrayList<>(meta.getLore()) : new java.util.ArrayList<>();

        String propertyDisplay = "Spawn Custom";
        String newLine = ChatColor.GRAY + propertyDisplay + ": " + (value ? ChatColor.GREEN + "Activado" : ChatColor.RED + "Desactivado");

        boolean found = false;
        for (int i = 0; i < lore.size(); i++) {
            if (ChatColor.stripColor(lore.get(i)).contains(propertyDisplay + ":")) {
                lore.set(i, newLine);
                found = true;
                break;
            }
        }

        if (!found) {
            lore.add(newLine);
        }

        meta.setLore(lore);
        spawner.setItemMeta(meta);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!playersWaitingForInput.containsKey(uuid)) return;

        event.setCancelled(true);

        String property = playersWaitingForInput.get(uuid);
        String input = event.getMessage().trim();

        if (input.equalsIgnoreCase("cancelar")) {
            player.sendMessage(ChatColor.RED + "Edición cancelada");
            playersWaitingForInput.remove(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (editingSpawners.containsKey(uuid)) {
                    openConfigGUI(player, editingSpawners.get(uuid));
                }
            });
            return;
        }

        try {
            int newValue = Integer.parseInt(input);
            ItemStack spawner = editingSpawners.get(uuid);

            if (!isValidValue(property, newValue)) {
                player.sendMessage(ChatColor.RED + "Valor inválido para " +
                        getPropertyDisplayName(property));
                player.sendMessage(ChatColor.GOLD + "Ingresa un nuevo valor o escribe 'cancelar':");
                return;
            }

            // Actualizar el spawner
            updateSpawnerConfig(spawner, property, newValue);
            player.sendMessage(ChatColor.GREEN + getPropertyDisplayName(property) +
                    " actualizado a " + newValue);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);

            playersWaitingForInput.remove(uuid);
            editingProperties.remove(uuid);

            // Reabrir la GUI
            Bukkit.getScheduler().runTask(plugin, () -> {
                openConfigGUI(player, spawner);
            });

        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Debes ingresar un número válido");
            player.sendMessage(ChatColor.GOLD + "Ingresa un nuevo valor o escribe 'cancelar':");
        }
    }

    private boolean isValidValue(String property, int value) {
        switch (property) {
            case "spawn_count": return value > 0 && value <= 20;
            case "max_nearby": return value > 0 && value <= 50;
            case "player_range": return value > 0 && value <= 42;
            case "delay":
            case "min_delay":
            case "max_delay": return value >= 10 && value <= 1200;
            case "spawn_range": return value > 0 && value <= 10;
            default: return value > 0;
        }
    }

    private int getCurrentValue(Player player, String property) {
        ItemStack spawner = editingSpawners.get(player.getUniqueId());
        Map<String, Integer> config = readSpawnerConfig(spawner);
        return config.get(property);
    }

    private String getPropertyDisplayName(String property) {
        switch (property) {
            case "spawn_count": return "Spawn Count";
            case "max_nearby": return "Max Nearby";
            case "player_range": return "Player Range";
            case "delay": return "Initial Delay";
            case "min_delay": return "Min Delay";
            case "max_delay": return "Max Delay";
            case "spawn_range": return "Spawn Range";
            default: return property;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        String title = event.getView().getTitle();

        if (title.equals("Configuración del Spawner")) {
            // Solo limpiar si no estamos esperando input por chat
            if (!playersWaitingForInput.containsKey(uuid)) {
                editingSpawners.remove(uuid);
                editingProperties.remove(uuid);
            }
        }
    }

    private void updateSpawnerConfig(ItemStack spawner, String property, int value) {
        ItemMeta meta = spawner.getItemMeta();
        if (meta == null) return;

        java.util.List<String> lore = meta.hasLore() ? new java.util.ArrayList<>(meta.getLore()) : new java.util.ArrayList<>();

        String propertyDisplay = getPropertyDisplayName(property);
        String newLine = ChatColor.GRAY + propertyDisplay + ": " + ChatColor.WHITE + value;

        boolean found = false;
        for (int i = 0; i < lore.size(); i++) {
            if (ChatColor.stripColor(lore.get(i)).contains(propertyDisplay + ":")) {
                lore.set(i, newLine);
                found = true;
                break;
            }
        }

        if (!found) {
            lore.add(newLine);
        }

        meta.setLore(lore);
        spawner.setItemMeta(meta);
    }

    //DATOS

    private void saveSpawnerData(CreatureSpawner spawner, String mobType, Map<String, Integer> config, boolean spawnCustom) {
        PersistentDataContainer container = spawner.getPersistentDataContainer();

        // Guardar el tipo de mob
        container.set(spawnerKey, PersistentDataType.STRING, mobType);

        // Guardar spawn_custom
        container.set(new NamespacedKey(plugin, "spawn_custom"), PersistentDataType.BYTE, (byte) (spawnCustom ? 1 : 0));

        // Guardar todas las configuraciones
        for (Map.Entry<String, Integer> entry : config.entrySet()) {
            container.set(new NamespacedKey(plugin, entry.getKey()), PersistentDataType.INTEGER, entry.getValue());
        }

        spawner.update();
    }

    private Map<String, Integer> loadSpawnerData(CreatureSpawner spawner) {
        Map<String, Integer> config = new HashMap<>();
        PersistentDataContainer container = spawner.getPersistentDataContainer();

        // Cargar todas las configuraciones guardadas
        String[] keys = {"spawn_count", "max_nearby", "player_range", "delay",
                "min_delay", "max_delay", "spawn_range"};

        for (String key : keys) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            if (container.has(namespacedKey, PersistentDataType.INTEGER)) {
                config.put(key, container.get(namespacedKey, PersistentDataType.INTEGER));
            }
        }

        return config;
    }

    private boolean isCustomSpawner(CreatureSpawner spawner) {
        return spawner.getPersistentDataContainer().has(spawnerKey, PersistentDataType.STRING);
    }

    public void clearActiveSpawners() {
        activeCustomSpawners.clear();
    }

    private void debugSpawnerItem(ItemStack spawner) {
        if (spawner.hasItemMeta() && spawner.getItemMeta().hasLore()) {
            plugin.getLogger().info("Lore actual del spawner:");
            for (String line : spawner.getItemMeta().getLore()) {
                plugin.getLogger().info(line);
            }
        } else {
            plugin.getLogger().info("El spawner no tiene lore");
        }
    }

    // Método para limpiar recursos al desactivar el plugin
    public void shutdown() {
        if (customSpawnTask != null && !customSpawnTask.isCancelled()) {
            customSpawnTask.cancel();
        }
        activeCustomSpawners.clear();
    }
}