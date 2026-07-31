package Handlers;

import Managers.ItemManager;
import Managers.MobManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.*;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.FluidCollisionMode;
import org.bukkit.util.RayTraceResult;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import vct.hardcore3.ViciontHardcore3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TrialSpawnerHandler implements Listener {

    private final ViciontHardcore3 plugin;
    private final ItemManager itemManager;
    private final MobManager mobManager;

    // =========================================================================
    // PDC KEYS — bloque e ítem
    // =========================================================================

    public static NamespacedKey presetIdKey;
    private NamespacedKey totalMobsKey;
    private NamespacedKey simMinKey;
    private NamespacedKey simMaxKey;
    private NamespacedKey ticksKey;
    private NamespacedKey rangeKey;
    private NamespacedKey cooldownKey;
    private NamespacedKey playerRangeKey;
    // Player-bonus configurable
    private NamespacedKey playerBonusEnabledKey;  // INTEGER 0/1
    private NamespacedKey playerBonusMaxKey;       // INTEGER cap de jugadores extra

    public static NamespacedKey normalLootKey;
    public static NamespacedKey ominousLootKey;
    public static NamespacedKey vanillaNormalLootKey;
    public static NamespacedKey vanillaOminousLootKey;
    public static NamespacedKey lootMaxItemsKey;
    public static NamespacedKey lootSpecialMaxKey;

    // Estado persistente
    private NamespacedKey phaseKey;
    private NamespacedKey wavesCompletedKey;
    private NamespacedKey spawnedInCurrentWaveKey;
    private NamespacedKey cooldownEndKey;

    private static final String DISPLAY_TAG = "viciont_trial_display";

    // Color pastel dorado para el texto del contador de cooldown
    private static final net.md_5.bungee.api.ChatColor PASTEL_GOLD = net.md_5.bungee.api.ChatColor.of("#F5DEB0");

    // Glifo flotante mostrado durante el cooldown
    private static final String COOLDOWN_ICON = "\uDB80\uDC66";

    // Mapa temporal para interceptar los mobs exactos al nacer
    private final Map<Location, TrialSpawnerData> expectingSpawns = new ConcurrentHashMap<>();

    // =========================================================================
    // DATOS EN MEMORIA
    // =========================================================================

    private static class TrialSpawnerData {

        String mobType;
        int totalMobs;
        int simMin;
        int simMax;
        // Si simMin == simMax el número de mobs por oleada es siempre fijo.
        boolean fixedWaveSize() { return simMin == simMax; }

        int ticksBetween;
        int spawnRange;
        int cooldownSecs;
        int playerRange;

        // Player bonus configurable
        boolean playerBonusEnabled;
        int playerBonusMax;

        String lootNormal, lootOminous, vanillaLootNormal, vanillaLootOminous;
        int lootMaxItems, lootSpecialMax;

        int totalWaves;
        int wavesCompleted;
        int spawnedInCurrentWave;
        int aliveInCurrentWave;

        Phase phase;
        long nextWaveSpawnTime;
        long cooldownEndTime;
        boolean ominousActive;
        boolean hasActivatedOnce;

        long waveStartTime;
        static final long WAVE_TIMEOUT_MS = 5 * 60 * 1000L;

        final Set<UUID> trackedMobs = ConcurrentHashMap.newKeySet();

        // Displays de cooldown: icono flotante + contador estático debajo
        TextDisplay iconDisplay;
        TextDisplay timerDisplay;

        Phase lastSavedPhase = null;
        int lastSavedWaves = -1;

        int totalSpawned;

        enum Phase { WAITING, WAVE_SPAWNING, WAVE_WAITING_DEATH, BETWEEN_WAVES, COOLDOWN }

        TrialSpawnerData(String mobType, Map<String, Integer> cfg,
                         String lootNormal, String lootOminous,
                         String vanillaLootNormal, String vanillaLootOminous,
                         int lootMaxItems, int lootSpecialMax,
                         Phase savedPhase, int savedWavesCompleted,
                         int savedSpawnedInCurrentWave, long savedCooldownEnd) {
            this.mobType      = mobType;
            this.totalMobs    = cfg.getOrDefault("viciont_ts_total_mobs", 6);
            this.simMin       = Math.max(1, cfg.getOrDefault("viciont_ts_sim_min", 2));
            this.simMax       = Math.max(this.simMin, cfg.getOrDefault("viciont_ts_sim_max", 2));
            this.ticksBetween = cfg.getOrDefault("viciont_ts_ticks", 40);
            this.spawnRange   = cfg.getOrDefault("viciont_ts_range", 4);
            this.cooldownSecs = cfg.getOrDefault("viciont_ts_cooldown", 30);
            this.playerRange  = cfg.getOrDefault("viciont_ts_player_range", 16);

            this.playerBonusEnabled = cfg.getOrDefault("viciont_ts_player_bonus_enabled", 0) == 1;
            this.playerBonusMax     = Math.max(0, cfg.getOrDefault("viciont_ts_player_bonus_max", 4));

            this.lootNormal         = lootNormal;
            this.lootOminous        = lootOminous;
            this.vanillaLootNormal  = vanillaLootNormal;
            this.vanillaLootOminous = vanillaLootOminous;
            this.lootMaxItems  = lootMaxItems;
            this.lootSpecialMax = lootSpecialMax;

            this.totalWaves = (int) Math.ceil((double) totalMobs / Math.max(1, simMax));

            this.phase                = savedPhase;
            this.wavesCompleted       = savedWavesCompleted;
            this.spawnedInCurrentWave = savedSpawnedInCurrentWave;
            this.aliveInCurrentWave   = 0;
            this.cooldownEndTime      = savedCooldownEnd;
            this.nextWaveSpawnTime    = 0;
            this.waveStartTime        = System.currentTimeMillis();
            this.ominousActive        = false;
            this.iconDisplay          = null;
            this.timerDisplay         = null;

            this.totalSpawned         = savedWavesCompleted * 0;
        }

        /*
         * Mobs a spawnear en la oleada actual:
         *
         * — Modo FIJO (simMin == simMax):
         *     base = simMin siempre, sin importar la oleada ni los jugadores.
         *
         * — Modo ESCALADO (simMin < simMax):
         *     base sube 1 por oleada completada: simMin, simMin+1, …, simMax.
         *     Si playerBonusEnabled, cada jugador extra (a partir del 2°)
         *     suma 1, hasta un máximo de playerBonusMax jugadores extra.
         *
         * En ambos modos el resultado nunca supera los mobs restantes.
         */
        int mobsForWave(int waveIndex, int playersInRange) {
            int base;
            if (fixedWaveSize()) {
                base = simMin;
            } else {
                base = Math.min(simMin + waveIndex, simMax);
                if (playerBonusEnabled) {
                    int extraPlayers = Math.min(Math.max(0, playersInRange - 1), playerBonusMax);
                    base += extraPlayers;
                }
            }
            int remaining = totalMobs - totalSpawned - spawnedInCurrentWave;
            return Math.min(base, Math.max(0, remaining));
        }

        boolean allWavesDone() {
            return totalSpawned >= totalMobs;
        }
    }

    private final Map<Location, TrialSpawnerData> activeSpawners = new ConcurrentHashMap<>();

    // GUI
    private final Map<UUID, ItemStack> editingSpawners        = new ConcurrentHashMap<>();
    private final Map<UUID, String>    playersWaitingForInput = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean>   editingOminousMode     = new ConcurrentHashMap<>();
    private final Map<UUID, Integer>   currentPage            = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean>   editingVanillaPool     = new ConcurrentHashMap<>();

    private final List<String>   allAvailableItems;
    private final List<Material> allVanillaMaterials;

    private BukkitRunnable spawnTask;
    private BukkitTask     cooldownDisplayTask;

    private long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL = 60_000;
    private long lastRescanTime = 0;
    private static final long RESCAN_INTERVAL = 10_000;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    public TrialSpawnerHandler(ViciontHardcore3 plugin, ItemManager itemManager, MobManager mobManager) {
        this.plugin      = plugin;
        this.itemManager = itemManager;
        this.mobManager  = mobManager;

        presetIdKey              = new NamespacedKey(plugin, "viciont_trial_mob");
        totalMobsKey             = new NamespacedKey(plugin, "viciont_ts_total_mobs");
        simMinKey                = new NamespacedKey(plugin, "viciont_ts_sim_min");
        simMaxKey                = new NamespacedKey(plugin, "viciont_ts_sim_max");
        ticksKey                 = new NamespacedKey(plugin, "viciont_ts_ticks");
        rangeKey                 = new NamespacedKey(plugin, "viciont_ts_range");
        cooldownKey              = new NamespacedKey(plugin, "viciont_ts_cooldown");
        playerRangeKey           = new NamespacedKey(plugin, "viciont_ts_player_range");
        playerBonusEnabledKey    = new NamespacedKey(plugin, "viciont_ts_player_bonus_enabled");
        playerBonusMaxKey        = new NamespacedKey(plugin, "viciont_ts_player_bonus_max");
        normalLootKey            = new NamespacedKey(plugin, "viciont_trial_loot_normal");
        ominousLootKey           = new NamespacedKey(plugin, "viciont_trial_loot_ominous");
        vanillaNormalLootKey     = new NamespacedKey(plugin, "viciont_trial_loot_vanilla_normal");
        vanillaOminousLootKey    = new NamespacedKey(plugin, "viciont_trial_loot_vanilla_ominous");
        lootMaxItemsKey          = new NamespacedKey(plugin, "viciont_trial_loot_max_items");
        lootSpecialMaxKey        = new NamespacedKey(plugin, "viciont_trial_loot_special_max");
        phaseKey                 = new NamespacedKey(plugin, "viciont_ts_phase");
        wavesCompletedKey        = new NamespacedKey(plugin, "viciont_ts_waves_completed");
        spawnedInCurrentWaveKey  = new NamespacedKey(plugin, "viciont_ts_spawned_wave");
        cooldownEndKey           = new NamespacedKey(plugin, "viciont_ts_cooldown_end");

        this.allAvailableItems = itemManager.getRegisteredItems();
        this.allVanillaMaterials = Arrays.stream(Material.values())
                .filter(m -> !m.isAir() && m.isItem() && m != Material.SPAWNER)
                .sorted(Comparator.comparing(Material::name))
                .collect(Collectors.toList());

        startSpawnTask();
        startCooldownDisplayTask();
        Bukkit.getScheduler().runTaskLater(plugin, this::loadAllCustomSpawners, 100L);
    }

    // =========================================================================
    // TAREA PRINCIPAL
    // =========================================================================

    private void startSpawnTask() {
        spawnTask = new BukkitRunnable() {
            @Override public void run() { processAllSpawners(); }
        };
        spawnTask.runTaskTimer(plugin, 20L, 4L);
    }

    private void processAllSpawners() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime > CLEANUP_INTERVAL) { cleanupInvalidSpawners(); lastCleanupTime = now; }
        if (now - lastRescanTime  > RESCAN_INTERVAL)  { rescanLoadedChunks();     lastRescanTime  = now; }

        for (Iterator<Map.Entry<Location, TrialSpawnerData>> iter = activeSpawners.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<Location, TrialSpawnerData> entry = iter.next();
            Location loc = entry.getKey();
            TrialSpawnerData data = entry.getValue();
            if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) continue;
            if (loc.getBlock().getType() != Material.SPAWNER) { cleanupDisplay(data); iter.remove(); continue; }
            processSpawner(loc, data, now);
        }
    }

    private void rescanLoadedChunks() {
        for (World world : Bukkit.getWorlds())
            for (Chunk chunk : world.getLoadedChunks())
                for (BlockState bs : chunk.getTileEntities()) {
                    if (!(bs instanceof CreatureSpawner)) continue;
                    CreatureSpawner cs = (CreatureSpawner) bs;
                    if (!cs.getPersistentDataContainer().has(presetIdKey, PersistentDataType.STRING)) continue;
                    Location loc = cs.getLocation();
                    if (!activeSpawners.containsKey(loc)) {
                        registerSpawnerFromBlock(cs);
                        plugin.getLogger().info("[TrialSpawnerHandler] Schematic detectado en " + loc);
                    }
                }
    }

    private void processSpawner(Location loc, TrialSpawnerData data, long now) {
        World world = loc.getWorld();
        if (world == null) return;
        Location center = loc.clone().add(0.5, 0.5, 0.5);

        switch (data.phase) {
            case WAITING:            handleWaiting(loc, center, data, now, world);           break;
            case WAVE_SPAWNING:      handleWaveSpawning(loc, center, data, now, world);      break;
            case WAVE_WAITING_DEATH: handleWaveWaitingDeath(loc, center, data, now, world);  break;
            case BETWEEN_WAVES:      handleBetweenWaves(loc, center, data, now, world);      break;
            case COOLDOWN:           handleCooldown(loc, center, data, now, world);          break;
        }
    }

    // =========================================================================
    // MÁQUINA DE ESTADOS
    // =========================================================================

    private void handleWaiting(Location loc, Location center, TrialSpawnerData data, long now, World world) {
        if (now % 1000 < 200) playIdleParticles(center, world);

        boolean playerInRange = data.hasActivatedOnce
                ? isPlayerInRange(loc, data.playerRange)
                : isPlayerInRangeWithLineOfSight(loc, data.playerRange);

        if (!playerInRange) return;

        Player ominousPlayer = getOminousPlayerNearby(loc, data.playerRange);
        boolean wasOminous = data.ominousActive;

        if (ominousPlayer != null && !data.ominousActive) {
            data.ominousActive = true;
            ominousPlayer.removePotionEffect(PotionEffectType.BAD_OMEN);
            ominousPlayer.addPotionEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.TRIAL_OMEN, 72000, 0));
        }

        if (data.ominousActive != wasOminous) {
            playOminousToggleEffect(center, data.ominousActive);
        }
        // ---------------------------------

        if (data.allWavesDone()) {
            data.phase = TrialSpawnerData.Phase.COOLDOWN;
            data.cooldownEndTime = now + (data.cooldownSecs * 1000L);
            saveStateToBlock(loc, data);
            return;
        }

        int playersNearby = countPlayersInRange(loc, data.playerRange);
        int mobsThisWave  = data.mobsForWave(data.wavesCompleted, playersNearby);

        if (data.spawnedInCurrentWave >= mobsThisWave) {
            data.phase = TrialSpawnerData.Phase.WAVE_WAITING_DEATH;
            data.waveStartTime = now;
        } else {
            data.phase = TrialSpawnerData.Phase.WAVE_SPAWNING;
            data.nextWaveSpawnTime = now;
        }
        data.hasActivatedOnce = true;
        saveStateToBlock(loc, data);
        playActivationEffect(center, world);
    }

    private void handleWaveSpawning(Location loc, Location center, TrialSpawnerData data, long now, World world) {
        if (now % 1000 < 200) playActiveParticles(center, world, data.ominousActive);
        if (!isPlayerInRange(loc, data.playerRange)) {
            data.phase = TrialSpawnerData.Phase.WAITING;
            saveStateToBlock(loc, data); return;
        }
        if (now < data.nextWaveSpawnTime) return;

        int playersNearby = countPlayersInRange(loc, data.playerRange);
        int mobsThisWave  = data.mobsForWave(data.wavesCompleted, playersNearby);
        int toSpawn       = mobsThisWave - data.spawnedInCurrentWave;

        if (toSpawn <= 0) {
            data.waveStartTime = now;
            data.phase = TrialSpawnerData.Phase.WAVE_WAITING_DEATH;
            saveStateToBlock(loc, data); return;
        }

        for (int i = 0; i < toSpawn; i++) {
            Location spawnLoc = findValidSpawnLocation(loc, data.spawnRange);

            if (spawnLoc == null) {
                spawnLoc = loc.clone().add(0.5, 1, 0.5);
                while (!spawnLoc.getBlock().isPassable() && spawnLoc.getY() < world.getMaxHeight()) {
                    spawnLoc.add(0, 1, 0);
                }
            }

            spawnMob(data.mobType, spawnLoc, data.ominousActive, data.trackedMobs);
            data.spawnedInCurrentWave++;
            final Location fl = spawnLoc; final boolean om = data.ominousActive;
            new BukkitRunnable() { @Override public void run() { playSpawnEffect(fl, fl.getWorld(), om); } }.runTaskLater(plugin, (long)(i * 2));
        }

        data.aliveInCurrentWave = data.trackedMobs.size();
        data.waveStartTime = now;
        data.phase = TrialSpawnerData.Phase.WAVE_WAITING_DEATH;
        saveStateToBlock(loc, data);
    }

    private void handleWaveWaitingDeath(Location loc, Location center, TrialSpawnerData data, long now, World world) {
        if (now % 1000 < 200) playActiveParticles(center, world, data.ominousActive);
        if (!isPlayerInRange(loc, data.playerRange)) {
            data.phase = TrialSpawnerData.Phase.WAITING;
            saveStateToBlock(loc, data); return;
        }

        data.trackedMobs.removeIf(uuid -> {
            Entity e = Bukkit.getEntity(uuid);
            return e == null || !e.isValid() || e.isDead();
        });

        data.aliveInCurrentWave = data.trackedMobs.size();

        if (data.aliveInCurrentWave > 0 && (now - data.waveStartTime) > TrialSpawnerData.WAVE_TIMEOUT_MS) {
            for (UUID uid : new HashSet<>(data.trackedMobs)) {
                Entity e = Bukkit.getEntity(uid);
                if (e != null && e.isValid() && !e.isDead()) e.remove();
            }
            data.trackedMobs.clear();
            data.aliveInCurrentWave = 0;
            world.playSound(center, Sound.BLOCK_TRIAL_SPAWNER_AMBIENT, 1.0f, 0.5f);
            world.spawnParticle(Particle.LARGE_SMOKE, center, 15, 0.4, 0.4, 0.4, 0.05);
            plugin.getLogger().info("[TrialSpawnerHandler] Timeout de oleada forzado en " + loc);
        }

        // Si todavía hay mobs de esta oleada vivos, no hacemos nada y esperamos
        if (data.aliveInCurrentWave > 0) return;

        data.totalSpawned += data.spawnedInCurrentWave;
        data.wavesCompleted++;
        data.spawnedInCurrentWave = 0;
        data.trackedMobs.clear();
        saveStateToBlock(loc, data);

        if (data.allWavesDone()) {
            data.phase = TrialSpawnerData.Phase.COOLDOWN;
            data.cooldownEndTime = now + (data.cooldownSecs * 1000L);
            saveStateToBlock(loc, data);
            playCooldownStartEffect(center, world, data.ominousActive);
            spawnCooldownDisplay(loc, data);
            scheduleLootDrop(loc, data);
        } else {
            data.phase = TrialSpawnerData.Phase.BETWEEN_WAVES;
            data.nextWaveSpawnTime = now + (data.ticksBetween * 50L);
            saveStateToBlock(loc, data);
            playBetweenWavesEffect(center, world);
        }
    }

    private void handleBetweenWaves(Location loc, Location center, TrialSpawnerData data, long now, World world) {
        if (now % 1000 < 200) playActiveParticles(center, world, data.ominousActive);
        if (!isPlayerInRange(loc, data.playerRange)) {
            data.phase = TrialSpawnerData.Phase.WAITING;
            saveStateToBlock(loc, data); return;
        }
        if (now < data.nextWaveSpawnTime) return;
        data.phase = TrialSpawnerData.Phase.WAVE_SPAWNING;
        saveStateToBlock(loc, data);
    }

    private void handleCooldown(Location loc, Location center, TrialSpawnerData data, long now, World world) {
        if (now % 1000 < 200) playCooldownParticles(center, world, data.ominousActive);
        if (now >= data.cooldownEndTime) {
            data.phase = TrialSpawnerData.Phase.WAITING;
            data.wavesCompleted = 0; data.spawnedInCurrentWave = 0; data.totalSpawned = 0; data.trackedMobs.clear();
            data.hasActivatedOnce = false;
            data.ominousActive = false;

            saveStateToBlock(loc, data);
            playReactivateEffect(center, world);
            cleanupDisplay(data);
        }
    }

    // =========================================================================
    // SPAWN
    // =========================================================================

    private void spawnMob(String mobType, Location loc, boolean ominous, Set<UUID> trackedSet) {
        TrialSpawnerData data = activeSpawners.values().stream()
                .filter(d -> d.trackedMobs == trackedSet)
                .findFirst()
                .orElse(null);

        if (data != null) {
            expectingSpawns.put(loc, data);
        }

        if (mobType.startsWith("vanilla_")) {
            try {
                EntityType type = EntityType.valueOf(mobType.substring(8).toUpperCase());
                loc.getWorld().spawnEntity(loc, type);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("[TrialSpawnerHandler] Tipo vanilla inválido: " + mobType);
            }
        } else {
            mobManager.spawnMob(mobType, loc, null, null);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> expectingSpawns.remove(loc), 5L);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInterceptMobSpawn(org.bukkit.event.entity.EntitySpawnEvent event) {
        if (expectingSpawns.isEmpty()) return;

        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity) || entity instanceof ArmorStand) return;

        Location loc = entity.getLocation();

        Location matchedKey = null;
        TrialSpawnerData targetData = null;

        for (Map.Entry<Location, TrialSpawnerData> entry : expectingSpawns.entrySet()) {
            Location expectedLoc = entry.getKey();
            if (expectedLoc.getWorld().equals(loc.getWorld()) && expectedLoc.distanceSquared(loc) <= 4.0) {
                matchedKey = expectedLoc;
                targetData = entry.getValue();
                break;
            }
        }

        if (targetData != null) {
            targetData.trackedMobs.add(entity.getUniqueId());

            String spawnerId = "viciont_spawner_" + targetData.hashCode();
            entity.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "viciont_trial_parent"),
                    PersistentDataType.STRING,
                    spawnerId
            );

            expectingSpawns.remove(matchedKey);
        }
    }

    // =========================================================================
    // LOOT
    // =========================================================================

    private void scheduleLootDrop(Location spawnerLoc, TrialSpawnerData data) {
        World world = spawnerLoc.getWorld();
        if (world == null) return;
        boolean isOminous     = data.ominousActive;
        String customLootStr  = isOminous ? data.lootOminous       : data.lootNormal;
        String vanillaLootStr = isOminous ? data.vanillaLootOminous : data.vanillaLootNormal;

        if (isOminous) {
            if (customLootStr == null || customLootStr.isEmpty()) customLootStr = data.lootNormal;
            if (vanillaLootStr == null || vanillaLootStr.isEmpty()) vanillaLootStr = data.vanillaLootNormal;
        }

        if ((customLootStr == null || customLootStr.isEmpty()) && (vanillaLootStr == null || vanillaLootStr.isEmpty())) return;

        List<ItemStack> finalLoot = buildLootList(customLootStr, vanillaLootStr, data.lootMaxItems, data.lootSpecialMax);
        if (finalLoot.isEmpty()) return;

        Location dropLoc = new Location(world, spawnerLoc.getBlockX() + 0.5, spawnerLoc.getBlockY() + 1.15, spawnerLoc.getBlockZ() + 0.5);
        world.playSound(dropLoc, Sound.BLOCK_TRIAL_SPAWNER_ABOUT_TO_SPAWN_ITEM, 1.0f, 1.2f);
        if (isOminous) world.spawnParticle(Particle.OMINOUS_SPAWNING,        dropLoc, 20, 0.2, 0.2, 0.2, 0.04);
        else           world.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, dropLoc, 20, 0.2, 0.2, 0.2, 0.04);

        final int total = finalLoot.size();
        new BukkitRunnable() {
            int index = 0;
            @Override public void run() {
                if (spawnerLoc.getBlock().getType() != Material.SPAWNER) { cancel(); return; }
                if (index >= total) {
                    world.playSound(dropLoc, Sound.BLOCK_TRIAL_SPAWNER_CLOSE_SHUTTER, 1.0f, 0.9f);
                    world.spawnParticle(Particle.ENCHANT, dropLoc, 15, 0.3, 0.2, 0.3, 0.3);
                    cancel(); return;
                }
                org.bukkit.entity.Item dropped = world.dropItem(dropLoc, finalLoot.get(index));
                dropped.setVelocity(new org.bukkit.util.Vector(0, 0.12, 0));
                dropped.setPickupDelay(20);
                world.playSound(dropLoc, Sound.BLOCK_TRIAL_SPAWNER_EJECT_ITEM, 1.0f, 0.85f + (float)(Math.random() * 0.3));
                if (isOminous) {
                    world.spawnParticle(Particle.OMINOUS_SPAWNING,                dropLoc, 10, 0.15, 0.15, 0.15, 0.03);
                    world.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, dropLoc,  6, 0.1,  0.1,  0.1,  0.02);
                } else {
                    world.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION,         dropLoc, 10, 0.15, 0.15, 0.15, 0.03);
                    world.spawnParticle(Particle.HAPPY_VILLAGER,                  dropLoc,  5, 0.1,  0.1,  0.1,  0.0);
                }
                world.spawnParticle(Particle.ENCHANT, dropLoc, 6, 0.15, 0.15, 0.15, 0.3);
                index++;
            }
        }.runTaskTimer(plugin, 10L, 20L);
    }

    private List<ItemStack> buildLootList(String customStr, String vanillaStr, int maxItems, int specialMax) {
        List<ItemStack> result = new ArrayList<>();
        Random rng = new Random();

        // Contadores de tiradas en lugar de usar result.size()
        int tiradasTotales = 0;
        int tiradasEspeciales = 0;

        if (customStr != null && !customStr.isEmpty()) {
            Map<String, int[]> pool = parseLootStringStatic(customStr);
            List<String> keys = new ArrayList<>(pool.keySet());
            Collections.shuffle(keys, rng);

            for (String id : keys) {
                // Evaluamos con nuestros contadores, ya no con result.size()
                if (tiradasEspeciales >= specialMax || tiradasTotales >= maxItems) break;

                int[] r = pool.get(id);
                int amt = r[0] == r[1] ? r[0] : r[0] + rng.nextInt(r[1] - r[0] + 1);
                boolean generatedAny = false;

                if (id.startsWith("esencia_")) {
                    for (int i = 0; i < amt; i++) {
                        ItemStack it = itemManager.getItem(id, 1, null);
                        if (it != null) {
                            result.add(it);
                            generatedAny = true;
                        }
                    }
                } else {
                    ItemStack it = itemManager.getItem(id, amt, null);
                    if (it != null) {
                        result.add(it);
                        generatedAny = true;
                    }
                }

                if (generatedAny) {
                    tiradasEspeciales++;
                    tiradasTotales++;
                }
            }
        }

        if (vanillaStr != null && !vanillaStr.isEmpty() && tiradasTotales < maxItems) {
            Map<String, int[]> pool = parseLootStringStatic(vanillaStr);
            List<String> keys = new ArrayList<>(pool.keySet());
            Collections.shuffle(keys, rng);

            for (String matName : keys) {
                if (tiradasTotales >= maxItems) break;
                try {
                    Material mat = Material.valueOf(matName);
                    int[] r = pool.get(matName);
                    int amt = r[0] == r[1] ? r[0] : r[0] + rng.nextInt(r[1] - r[0] + 1);
                    result.add(new ItemStack(mat, Math.max(1, amt)));
                    tiradasTotales++;
                } catch (IllegalArgumentException ignored) {}
            }
        }

        return result;
    }

    // =========================================================================
    // PERSISTENCIA
    // =========================================================================

    private void saveStateToBlock(Location loc, TrialSpawnerData data) {
        Block block = loc.getBlock();
        if (block.getType() != Material.SPAWNER) return;
        if (data.phase == data.lastSavedPhase && data.wavesCompleted == data.lastSavedWaves) return;
        CreatureSpawner cs = (CreatureSpawner) block.getState();
        PersistentDataContainer pdc = cs.getPersistentDataContainer();
        pdc.set(phaseKey,                PersistentDataType.STRING,  data.phase.name());
        pdc.set(wavesCompletedKey,       PersistentDataType.INTEGER, data.wavesCompleted);
        pdc.set(spawnedInCurrentWaveKey, PersistentDataType.INTEGER, data.spawnedInCurrentWave);
        pdc.set(cooldownEndKey,          PersistentDataType.STRING,  String.valueOf(data.cooldownEndTime));
        cs.update();
        data.lastSavedPhase = data.phase; data.lastSavedWaves = data.wavesCompleted;
    }

    private void forceSaveStateToBlock(Location loc, TrialSpawnerData data) {
        Block block = loc.getBlock();
        if (block.getType() != Material.SPAWNER) return;
        CreatureSpawner cs = (CreatureSpawner) block.getState();
        PersistentDataContainer pdc = cs.getPersistentDataContainer();
        pdc.set(phaseKey,                PersistentDataType.STRING,  data.phase.name());
        pdc.set(wavesCompletedKey,       PersistentDataType.INTEGER, data.wavesCompleted);
        pdc.set(spawnedInCurrentWaveKey, PersistentDataType.INTEGER, data.spawnedInCurrentWave);
        pdc.set(cooldownEndKey,          PersistentDataType.STRING,  String.valueOf(data.cooldownEndTime));
        cs.update();
    }

    // =========================================================================
    // TEXT DISPLAY — Cooldown (icono flotante + contador estático)
    // =========================================================================

    /**
     * Crea los dos displays usados durante el cooldown:
     *  - iconDisplay : glifo "\uDB80\uDC66" que flota suavemente arriba/abajo.
     *  - timerDisplay: texto "00:00:00" estático, en dorado pastel, sin negrita,
     *                  colocado debajo del icono para que no se solapen.
     */
    private void spawnCooldownDisplay(Location spawnerLoc, TrialSpawnerData data) {
        cleanupDisplay(data);

        World w = spawnerLoc.getWorld();
        if (w == null) return;

        // Limpieza de displays huérfanos que pudieran quedar en esta posición
        Location checkCenter = spawnerLoc.clone().add(0.5, 1.5, 0.5);
        for (Entity e : checkCenter.getWorld().getNearbyEntities(checkCenter, 0.8, 1.0, 0.8))
            if (e instanceof TextDisplay && e.getScoreboardTags().contains(DISPLAY_TAG)) e.remove();

        Location iconLoc  = spawnerLoc.clone().add(0.5, 1.75, 0.5);
        Location timerLoc = spawnerLoc.clone().add(0.5, 1.35, 0.5);

        data.iconDisplay = w.spawn(iconLoc, TextDisplay.class, entity -> {
            entity.addScoreboardTag(DISPLAY_TAG);
            entity.setBillboard(Billboard.CENTER);
            entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            entity.setViewRange(24.0f);
            entity.setShadowed(true);
            entity.setText(COOLDOWN_ICON);
        });

        long remaining = data.cooldownEndTime - System.currentTimeMillis();
        data.timerDisplay = w.spawn(timerLoc, TextDisplay.class, entity -> {
            entity.addScoreboardTag(DISPLAY_TAG);
            entity.setBillboard(Billboard.CENTER);
            entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            entity.setViewRange(24.0f);
            entity.setShadowed(true);
            entity.setText(PASTEL_GOLD + formatCooldownTime(remaining));
        });
    }

    /** Actualiza únicamente el texto del contador (no se mueve, no se anima). */
    private void updateCooldownTimer(TrialSpawnerData data, long remainingMs) {
        if (data.timerDisplay == null || !data.timerDisplay.isValid()) return;
        data.timerDisplay.setText(PASTEL_GOLD + formatCooldownTime(remainingMs));
    }

    /** Formatea milisegundos restantes como HH:MM:SS. */
    private String formatCooldownTime(long remainingMs) {
        long totalSeconds = Math.max(0, remainingMs) / 1000L;
        long hours   = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void cleanupDisplay(TrialSpawnerData data) {
        if (data.iconDisplay != null && data.iconDisplay.isValid()) data.iconDisplay.remove();
        if (data.timerDisplay != null && data.timerDisplay.isValid()) data.timerDisplay.remove();
        data.iconDisplay = null;
        data.timerDisplay = null;
    }

    private void startCooldownDisplayTask() {
        cooldownDisplayTask = new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                tick++;
                for (TrialSpawnerData d : activeSpawners.values()) {
                    if (d.phase != TrialSpawnerData.Phase.COOLDOWN) continue;

                    if (d.iconDisplay != null && d.iconDisplay.isValid()) {
                        float offset = (float) (Math.sin(tick * 0.12) * 0.08);
                        d.iconDisplay.setTransformation(new Transformation(
                                new Vector3f(0f, offset, 0f),
                                new Quaternionf(),
                                new Vector3f(1f, 1f, 1f),
                                new Quaternionf()));
                    }

                    if (tick % 20 == 0) {
                        long remaining = d.cooldownEndTime - System.currentTimeMillis();
                        updateCooldownTimer(d, remaining);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // =========================================================================
    // EFECTOS Y PARTÍCULAS
    // =========================================================================

    private void playIdleParticles(Location c, World w) {
        w.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, c, 2, 0.4, 0.4, 0.4, 0.01);
        w.spawnParticle(Particle.SOUL_FIRE_FLAME,         c, 1, 0.3, 0.3, 0.3, 0.02);
    }
    private void playActivationEffect(Location c, World w) {
        w.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, c, 20, 0.5, 0.5, 0.5, 0.1);
        w.spawnParticle(Particle.FLAME,                   c, 10, 0.3, 0.3, 0.3, 0.05);
        w.playSound(c, Sound.BLOCK_TRIAL_SPAWNER_ABOUT_TO_SPAWN_ITEM, 1.0f, 1.0f);
        w.playSound(c, Sound.BLOCK_TRIAL_SPAWNER_DETECT_PLAYER,       1.0f, 1.0f);
    }
    private void playActiveParticles(Location c, World w, boolean ominous) {
        if (ominous) {
            w.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, c, 5, 0.4, 0.4, 0.4, 0.05);
            w.spawnParticle(Particle.OMINOUS_SPAWNING,                c, 3, 0.3, 0.3, 0.3, 0.03);
        } else {
            w.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, c, 4, 0.4, 0.4, 0.4, 0.05);
            w.spawnParticle(Particle.FLAME,                   c, 2, 0.3, 0.3, 0.3, 0.03);
        }
    }
    private void playSpawnEffect(Location sl, World w, boolean ominous) {
        if (w == null) return;
        w.playSound(sl, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, 1.0f, 1.0f);
        if (ominous) {
            w.spawnParticle(Particle.OMINOUS_SPAWNING,                sl, 30, 0.5, 1.0, 0.5, 0.1);
            w.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, sl, 15, 0.3, 0.5, 0.3, 0.05);
        } else {
            w.spawnParticle(Particle.POOF,                    sl, 20, 0.5, 1.0, 0.5, 0.1);
            w.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, sl, 10, 0.3, 0.5, 0.3, 0.05);
        }
    }
    private void playBetweenWavesEffect(Location c, World w) {
        w.playSound(c, Sound.BLOCK_TRIAL_SPAWNER_AMBIENT, 0.8f, 1.1f);
        w.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, c, 10, 0.4, 0.4, 0.4, 0.04);
    }
    private void playCooldownStartEffect(Location c, World w, boolean ominous) {
        w.playSound(c, Sound.BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER, 1.0f, 1.0f);
        if (ominous) w.spawnParticle(Particle.OMINOUS_SPAWNING,        c, 30, 0.6, 0.6, 0.6, 0.08);
        else { w.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, c, 25, 0.6, 0.6, 0.6, 0.08); w.spawnParticle(Particle.HAPPY_VILLAGER, c, 10, 0.4, 0.4, 0.4, 0.05); }
    }
    private void playCooldownParticles(Location c, World w, boolean ominous) {
        if (ominous) w.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, c, 3, 0.3, 0.3, 0.3, 0.02);
        else         w.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION,          c, 2, 0.3, 0.3, 0.3, 0.02);
    }
    private void playReactivateEffect(Location c, World w) {
        w.playSound(c, Sound.BLOCK_TRIAL_SPAWNER_CLOSE_SHUTTER, 1.0f, 1.0f);
        w.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, c, 15, 0.4, 0.4, 0.4, 0.06);
    }
    private void playOminousToggleEffect(Location c, boolean ominous) {
        World w = c.getWorld(); if (w == null) return;
        if (ominous) { w.playSound(c, Sound.BLOCK_TRIAL_SPAWNER_DETECT_PLAYER, 1.0f, 1.0f); w.spawnParticle(Particle.OMINOUS_SPAWNING, c, 20, 0.5, 0.5, 0.5, 0.1); }
        else           w.playSound(c, Sound.BLOCK_TRIAL_SPAWNER_CLOSE_SHUTTER, 1.0f, 0.8f);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Location findValidSpawnLocation(Location loc, int range) {
        World w = loc.getWorld();
        if (w == null) return null;
        Random rng = new Random();

        for (int i = 0; i < 30; i++) {
            double x = loc.getX() + (rng.nextDouble() * range * 2 - range);
            double z = loc.getZ() + (rng.nextDouble() * range * 2 - range);

            for (int dy = 3; dy >= -4; dy--) {
                int blockY = (int) Math.floor(loc.getY()) + dy;
                int blockX = (int) Math.floor(x);
                int blockZ = (int) Math.floor(z);

                Block floor = w.getBlockAt(blockX, blockY - 1, blockZ);
                Block feet  = w.getBlockAt(blockX, blockY,     blockZ);
                Block head  = w.getBlockAt(blockX, blockY + 1, blockZ);

                String floorName = floor.getType().name();
                if (!floor.getType().isSolid() || floorName.contains("FENCE") || floorName.contains("WALL") || floorName.contains("LEAVES")) {
                    continue;
                }

                if (!feet.isPassable() || !head.isPassable()) continue;

                boolean clearOfWalls = true;
                for (int ox = -1; ox <= 1; ox++) {
                    for (int oz = -1; oz <= 1; oz++) {
                        if (ox == 0 && oz == 0) continue;

                        Block sideFeet = w.getBlockAt(blockX + ox, blockY, blockZ + oz);
                        Block sideHead = w.getBlockAt(blockX + ox, blockY + 1, blockZ + oz);

                        if (!sideFeet.isPassable() || !sideHead.isPassable()) {
                            clearOfWalls = false;
                            break;
                        }
                    }
                    if (!clearOfWalls) break;
                }

                if (!clearOfWalls) continue;

                return new Location(w, blockX + 0.5, blockY, blockZ + 0.5);
            }
        }
        return null;
    }

    private boolean isPlayerInRange(Location loc, int range) {
        World w = loc.getWorld(); if (w == null) return false;
        for (Player p : w.getPlayers()) {
            if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) continue;
            if (p.getLocation().distance(loc) <= range) return true;
        }
        return false;
    }
    private int countPlayersInRange(Location loc, int range) {
        World w = loc.getWorld(); if (w == null) return 0;
        int count = 0;
        for (Player p : w.getPlayers()) {
            if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) continue;
            if (p.getLocation().distance(loc) <= range) count++;
        }
        return count;
    }

    private Player getOminousPlayerNearby(Location loc, int range) {
        World w = loc.getWorld(); if (w == null) return null;
        for (Player p : w.getPlayers()) {
            if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) continue;
            if (p.getLocation().distance(loc) > range) continue;
            if (p.hasPotionEffect(PotionEffectType.BAD_OMEN)) return p;
        }
        return null;
    }

    private boolean isPlayerInRangeWithLineOfSight(Location loc, int range) {
        World w = loc.getWorld(); if (w == null) return false;
        Location center = loc.clone().add(0.5, 0.5, 0.5);
        for (Player p : w.getPlayers()) {
            if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) continue;
            if (p.getLocation().distance(loc) > range) continue;
            if (hasLineOfSight(p, center)) return true;
        }
        return false;
    }

    private boolean hasLineOfSight(Player p, Location target) {
        Location eye = p.getEyeLocation();
        World w = eye.getWorld();
        if (w == null || target.getWorld() == null || !w.equals(target.getWorld())) return false;

        org.bukkit.util.Vector direction = target.toVector().subtract(eye.toVector());
        double distance = direction.length();
        if (distance < 0.01) return true;
        direction.normalize();

        RayTraceResult result = w.rayTraceBlocks(eye, direction, distance, FluidCollisionMode.NEVER, true);

        if (result == null || result.getHitBlock() == null) return true;

        Block hitBlock = result.getHitBlock();
        Block targetBlock = target.getBlock();

        return hitBlock.getX() == targetBlock.getX() &&
                hitBlock.getY() == targetBlock.getY() &&
                hitBlock.getZ() == targetBlock.getZ();
    }

    // =========================================================================
    // COLOCACIÓN
    // =========================================================================

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() != Material.SPAWNER || !item.hasItemMeta()) return;
        PersistentDataContainer itemPdc = item.getItemMeta().getPersistentDataContainer();
        if (!itemPdc.has(presetIdKey, PersistentDataType.STRING)) return;
        String mobType = itemPdc.get(presetIdKey, PersistentDataType.STRING);
        if (mobType == null) return;

        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();

        new BukkitRunnable() {
            @Override public void run() {
                if (block.getType() != Material.SPAWNER) return;
                CreatureSpawner cs = (CreatureSpawner) block.getState();
                cs.setSpawnedType(getBaseEntityType(mobType));
                PersistentDataContainer pdc = cs.getPersistentDataContainer();
                pdc.set(presetIdKey, PersistentDataType.STRING, mobType);
                copyIntKey(itemPdc, pdc, totalMobsKey,          6);
                copyIntKey(itemPdc, pdc, simMinKey,             2);
                copyIntKey(itemPdc, pdc, simMaxKey,             2);
                copyIntKey(itemPdc, pdc, ticksKey,              40);
                copyIntKey(itemPdc, pdc, rangeKey,              4);
                copyIntKey(itemPdc, pdc, cooldownKey,           30);
                copyIntKey(itemPdc, pdc, playerRangeKey,        16);
                copyIntKey(itemPdc, pdc, playerBonusEnabledKey, 0);
                copyIntKey(itemPdc, pdc, playerBonusMaxKey,     4);
                copyStrKey(itemPdc, pdc, normalLootKey);
                copyStrKey(itemPdc, pdc, ominousLootKey);
                copyStrKey(itemPdc, pdc, vanillaNormalLootKey);
                copyStrKey(itemPdc, pdc, vanillaOminousLootKey);
                copyIntKey(itemPdc, pdc, lootMaxItemsKey,       4);
                copyIntKey(itemPdc, pdc, lootSpecialMaxKey,     2);
                pdc.set(phaseKey,               PersistentDataType.STRING,  "WAITING");
                pdc.set(wavesCompletedKey,       PersistentDataType.INTEGER, 0);
                pdc.set(spawnedInCurrentWaveKey, PersistentDataType.INTEGER, 0);
                pdc.set(cooldownEndKey,          PersistentDataType.STRING,  "0");
                cs.update();

                // Aplicar nombre custom al SpawnData para que el disfraz/textura del mob
                // se muestre correctamente dentro de la jaula del spawner (igual que CustomSpawnerHandler).
                if (!mobType.startsWith("vanilla_")) {
                    String customName = getCustomMobName(mobType);
                    if (customName != null) applyCustomNameToSpawner(cs, customName);
                }

                registerSpawnerFromBlock(cs);
                Location center = block.getLocation().add(0.5, 0.5, 0.5);
                block.getWorld().playSound(center, Sound.BLOCK_TRIAL_SPAWNER_DETECT_PLAYER, 1.0f, 1.0f);
                block.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, center, 25, 0.5, 0.5, 0.5, 0.1);
                block.getWorld().spawnParticle(Particle.PORTAL, center, 30, 0.4, 0.4, 0.4, 0.2);
                player.sendMessage(ChatColor.GREEN + "¡Trial Spawner de "
                        + formatEntityNameStatic(mobType.startsWith("vanilla_") ? mobType.substring(8) : mobType) + " colocado!");
            }
        }.runTaskLater(plugin, 2L);
    }

    @EventHandler
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        CreatureSpawner cs = (CreatureSpawner) event.getSpawner().getBlock().getState();
        if (cs.getPersistentDataContainer().has(presetIdKey, PersistentDataType.STRING)) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) return;
        BlockState state = block.getState();
        if (!(state instanceof CreatureSpawner)) return;
        CreatureSpawner cs = (CreatureSpawner) state;
        if (!cs.getPersistentDataContainer().has(presetIdKey, PersistentDataType.STRING)) return;
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
            return;
        }
        TrialSpawnerData data = activeSpawners.remove(block.getLocation());
        if (data != null) cleanupDisplay(data);
    }

    // =========================================================================
    // CARGA DE CHUNKS
    // =========================================================================

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        new BukkitRunnable() {
            @Override
            public void run() {
                // Limpiar displays huérfanos (de spawners que ya no existen o que no están en cooldown)
                for (Entity e : chunk.getEntities()) {
                    if (!(e instanceof TextDisplay) || !e.getScoreboardTags().contains(DISPLAY_TAG)) continue;
                    Location el = e.getLocation();
                    boolean valid = false;
                    for (int dy = 1; dy <= 2; dy++) {
                        Block b = el.clone().subtract(0, dy, 0).getBlock();
                        if (b.getType() == Material.SPAWNER
                                && ((CreatureSpawner) b.getState()).getPersistentDataContainer().has(presetIdKey, PersistentDataType.STRING)) {
                            TrialSpawnerData d = activeSpawners.get(b.getLocation());
                            if (d != null && d.phase == TrialSpawnerData.Phase.COOLDOWN) { valid = true; }
                            break;
                        }
                    }
                    if (!valid) e.remove();
                }
                for (BlockState bs : chunk.getTileEntities()) {
                    if (!(bs instanceof CreatureSpawner)) continue;
                    CreatureSpawner cs = (CreatureSpawner) bs;
                    if (!cs.getPersistentDataContainer().has(presetIdKey, PersistentDataType.STRING)) continue;
                    Location loc = cs.getLocation();
                    if (!activeSpawners.containsKey(loc)) registerSpawnerFromBlock(cs);
                    else {
                        TrialSpawnerData data = activeSpawners.get(loc);
                        if (data.phase == TrialSpawnerData.Phase.COOLDOWN
                                && (data.iconDisplay == null || !data.iconDisplay.isValid()))
                            spawnCooldownDisplay(loc, data);
                    }
                }
            }
        }.runTaskLater(plugin, 5L);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (BlockState bs : event.getChunk().getTileEntities()) {
            if (!(bs instanceof CreatureSpawner)) continue;
            Location loc = bs.getLocation();
            TrialSpawnerData data = activeSpawners.remove(loc);
            if (data != null) { forceSaveStateToBlock(loc, data); data.iconDisplay = null; data.timerDisplay = null; }
        }
    }

    public void loadAllCustomSpawners() {
        int total = 0;
        for (World world : Bukkit.getWorlds())
            for (Chunk chunk : world.getLoadedChunks())
                for (BlockState bs : chunk.getTileEntities()) {
                    if (!(bs instanceof CreatureSpawner)) continue;
                    CreatureSpawner cs = (CreatureSpawner) bs;
                    if (!cs.getPersistentDataContainer().has(presetIdKey, PersistentDataType.STRING)) continue;
                    if (!activeSpawners.containsKey(cs.getLocation())) { registerSpawnerFromBlock(cs); total++; }
                }
        plugin.getLogger().info("[TrialSpawnerHandler] Cargados " + total + " trial spawners.");
    }

    private void registerSpawnerFromBlock(CreatureSpawner cs) {
        PersistentDataContainer pdc = cs.getPersistentDataContainer();
        String mobType = pdc.get(presetIdKey, PersistentDataType.STRING);
        if (mobType == null) return;

        Map<String, Integer> cfg = new HashMap<>();
        cfg.put("viciont_ts_total_mobs",            pdc.getOrDefault(totalMobsKey,          PersistentDataType.INTEGER, 6));
        cfg.put("viciont_ts_sim_min",               pdc.getOrDefault(simMinKey,             PersistentDataType.INTEGER, 2));
        cfg.put("viciont_ts_sim_max",               pdc.getOrDefault(simMaxKey,             PersistentDataType.INTEGER, 2));
        cfg.put("viciont_ts_ticks",                 pdc.getOrDefault(ticksKey,              PersistentDataType.INTEGER, 40));
        cfg.put("viciont_ts_range",                 pdc.getOrDefault(rangeKey,              PersistentDataType.INTEGER, 4));
        cfg.put("viciont_ts_cooldown",              pdc.getOrDefault(cooldownKey,           PersistentDataType.INTEGER, 30));
        cfg.put("viciont_ts_player_range",          pdc.getOrDefault(playerRangeKey,        PersistentDataType.INTEGER, 16));
        cfg.put("viciont_ts_player_bonus_enabled",  pdc.getOrDefault(playerBonusEnabledKey, PersistentDataType.INTEGER, 0));
        cfg.put("viciont_ts_player_bonus_max",      pdc.getOrDefault(playerBonusMaxKey,     PersistentDataType.INTEGER, 4));

        String lootNorm = pdc.getOrDefault(normalLootKey,         PersistentDataType.STRING, "");
        String lootOmin = pdc.getOrDefault(ominousLootKey,        PersistentDataType.STRING, "");
        String vanNorm  = pdc.getOrDefault(vanillaNormalLootKey,  PersistentDataType.STRING, "");
        String vanOmin  = pdc.getOrDefault(vanillaOminousLootKey, PersistentDataType.STRING, "");
        int maxItems   = pdc.getOrDefault(lootMaxItemsKey,   PersistentDataType.INTEGER, 4);
        int specialMax = pdc.getOrDefault(lootSpecialMaxKey, PersistentDataType.INTEGER, 2);

        String phaseStr        = pdc.getOrDefault(phaseKey,               PersistentDataType.STRING,  "WAITING");
        int savedWaves         = pdc.getOrDefault(wavesCompletedKey,       PersistentDataType.INTEGER, 0);
        int savedSpawnedInWave = pdc.getOrDefault(spawnedInCurrentWaveKey, PersistentDataType.INTEGER, 0);
        long savedCooldownEnd;
        try { savedCooldownEnd = Long.parseLong(pdc.getOrDefault(cooldownEndKey, PersistentDataType.STRING, "0")); }
        catch (NumberFormatException e) { savedCooldownEnd = 0; }

        TrialSpawnerData.Phase savedPhase;
        try { savedPhase = TrialSpawnerData.Phase.valueOf(phaseStr); }
        catch (IllegalArgumentException e) { savedPhase = TrialSpawnerData.Phase.WAITING; }

        if (savedPhase == TrialSpawnerData.Phase.COOLDOWN && savedCooldownEnd > 0 && System.currentTimeMillis() >= savedCooldownEnd)
        { savedPhase = TrialSpawnerData.Phase.WAITING; savedWaves = 0; savedSpawnedInWave = 0; savedCooldownEnd = 0; }
        if (savedPhase == TrialSpawnerData.Phase.WAVE_SPAWNING || savedPhase == TrialSpawnerData.Phase.WAVE_WAITING_DEATH || savedPhase == TrialSpawnerData.Phase.BETWEEN_WAVES)
        { savedPhase = TrialSpawnerData.Phase.WAITING; savedSpawnedInWave = 0; }

        TrialSpawnerData data = new TrialSpawnerData(mobType, cfg, lootNorm, lootOmin, vanNorm, vanOmin,
                maxItems, specialMax, savedPhase, savedWaves, savedSpawnedInWave, savedCooldownEnd);
        activeSpawners.put(cs.getLocation(), data);

        if (savedPhase == TrialSpawnerData.Phase.COOLDOWN) {
            spawnCooldownDisplay(cs.getLocation(), data);
        }
    }

    private void cleanupInvalidSpawners() {
        int removed = 0;
        for (Iterator<Map.Entry<Location, TrialSpawnerData>> iter = activeSpawners.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<Location, TrialSpawnerData> e = iter.next();
            Location loc = e.getKey();
            if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4) || loc.getBlock().getType() != Material.SPAWNER)
            { cleanupDisplay(e.getValue()); iter.remove(); removed++; }
        }
        if (removed > 0) plugin.getLogger().info("[TrialSpawnerHandler] Limpieza: " + removed + " inválidos.");
    }

    // =========================================================================
    // GUI PRINCIPAL
    // Slots de spawn (fila 1):  10 11 12 13 14 15
    // Slots de loot  (fila 3):  28 29
    // Separador visual          30 (pane de cristal)
    // Slots bonus jugadores     31 32   ← nuevas configs aisladas
    // Slots de loot botones     37 38 39 40 44
    // =========================================================================

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.getAction().toString().contains("RIGHT_CLICK")) return;
        if (!player.isSneaking() || !player.hasPermission("viciont.admin")) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.SPAWNER || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getPersistentDataContainer().has(presetIdKey, PersistentDataType.STRING)) return;
        event.setCancelled(true);
        editingSpawners.put(player.getUniqueId(), item);
        openMainGUI(player, item);
    }

    private void openMainGUI(Player player, ItemStack spawnerItem) {
        Inventory gui = Bukkit.createInventory(null, 54, "Configuración Trial Spawner");
        PersistentDataContainer pdc = spawnerItem.getItemMeta().getPersistentDataContainer();

        int simMin = pdc.getOrDefault(simMinKey, PersistentDataType.INTEGER, 2);
        int simMax = pdc.getOrDefault(simMaxKey, PersistentDataType.INTEGER, 2);
        boolean fixed = (simMin == simMax);
        boolean bonusEnabled = pdc.getOrDefault(playerBonusEnabledKey, PersistentDataType.INTEGER, 0) == 1;
        int bonusCap = pdc.getOrDefault(playerBonusMaxKey, PersistentDataType.INTEGER, 4);

        // ── Fila 1: configuración de spawn ──────────────────────────────────
        gui.setItem(10, makeConfigPaper("Total Mobs",
                pdc.getOrDefault(totalMobsKey, PersistentDataType.INTEGER, 6),
                "Total de mobs en toda la ronda."));

        gui.setItem(11, makeWaveSizePaper(simMin, simMax, fixed));

        gui.setItem(12, makeConfigPaper("Ticks Entre Oleadas",
                pdc.getOrDefault(ticksKey, PersistentDataType.INTEGER, 40),
                "Ticks de espera entre oleadas."));
        gui.setItem(13, makeConfigPaper("Spawn Range",
                pdc.getOrDefault(rangeKey, PersistentDataType.INTEGER, 4),
                "Radio de spawn en bloques."));
        gui.setItem(14, makeConfigPaper("Cooldown Segundos",
                pdc.getOrDefault(cooldownKey, PersistentDataType.INTEGER, 30),
                "Segundos de enfriamiento."));
        gui.setItem(15, makeConfigPaper("Player Range",
                pdc.getOrDefault(playerRangeKey, PersistentDataType.INTEGER, 16),
                "Rango de detección de jugadores."));

        // ── Fila 3: loot y separador + bonus jugadores ───────────────────────
        gui.setItem(28, makeConfigPaper("Loot Max Items",
                pdc.getOrDefault(lootMaxItemsKey, PersistentDataType.INTEGER, 4),
                "Items totales en recompensa."));
        gui.setItem(29, makeConfigPaper("Loot Especial Max",
                pdc.getOrDefault(lootSpecialMaxKey, PersistentDataType.INTEGER, 2),
                "Máx. items custom especiales."));

        // Separador visual (solo si el modo es escalado — bonus solo aplica ahí)
        gui.setItem(30, makeGlassPane(fixed
                ? "§8— Bonus jugadores (inactivo en modo fijo) —"
                : "§8— Bonus por jugadores extra —"));

        // Toggle bonus
        gui.setItem(31, makeBonusToggle(bonusEnabled, fixed));

        // Cap de jugadores extra (solo visible/útil si bonus activo y modo escalado)
        gui.setItem(32, makeBonusCapPaper(bonusCap, bonusEnabled, fixed));

        // ── Fila 4: botones de loot ──────────────────────────────────────────
        gui.setItem(37, makeLootButton(Material.CHEST,         "§aBotín Especial Normal",  "§7Items custom sin Bad Omen.", "§eClick para configurar."));
        gui.setItem(38, makeLootButton(Material.ENDER_CHEST,   "§5Botín Especial Ominous", "§7Items custom con Bad Omen.", "§eClick para configurar."));
        gui.setItem(39, makeLootButton(Material.BARREL,        "§eBotín Vanilla Normal",   "§7Items vanilla sin Bad Omen.", "§eClick para configurar."));
        gui.setItem(40, makeLootButton(Material.TRAPPED_CHEST, "§6Botín Vanilla Ominous",  "§7Items vanilla con Bad Omen.", "§eClick para configurar."));
        gui.setItem(44, makeLootButton(Material.BOOK,          "§bVer Loot Configurado",   "§7Ver todos los items asignados.", "§eClick para ver."));

        player.openInventory(gui);
    }

    // ── Helpers de ítems GUI ─────────────────────────────────────────────────

    private ItemStack makeConfigPaper(String name, int value, String desc) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + name);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + desc, "",
                ChatColor.GRAY + "Valor actual: " + ChatColor.WHITE + value, "",
                ChatColor.YELLOW + "Click para modificar"));
        item.setItemMeta(meta); return item;
    }

    /**
     * Muestra el estado actual del tamaño de oleada.
     * - Modo FIJO:    "Tamaño de Oleada  §f3  (fijo)"
     * - Modo ESCALADO:"Tamaño de Oleada  §f2 → 5"
     */
    private ItemStack makeWaveSizePaper(int simMin, int simMax, boolean fixed) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Tamaño de Oleada");
        String currentVal = fixed
                ? ChatColor.WHITE + String.valueOf(simMin) + ChatColor.GRAY + " (fijo)"
                : ChatColor.WHITE + "" + simMin + ChatColor.GRAY + " → " + ChatColor.WHITE + simMax + ChatColor.GRAY + " (escalado)";
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Mobs spawneados por oleada.",
                "",
                ChatColor.GRAY + "Actual: " + currentVal,
                "",
                ChatColor.GRAY + "Escribe §fun número§7 → mobs siempre fijos.",
                ChatColor.GRAY + "Escribe §fmin-max§7 → escala oleada a oleada.",
                "",
                ChatColor.YELLOW + "Click para modificar"));
        item.setItemMeta(meta); return item;
    }

    private ItemStack makeGlassPane(String label) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(label);
        item.setItemMeta(meta); return item;
    }

    /**
     * Toggle de bonus de jugadores.
     * Verde = activado, Rojo = desactivado.
     * Si el modo es fijo, muestra advertencia.
     */
    private ItemStack makeBonusToggle(boolean enabled, boolean fixedMode) {
        Material mat = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((enabled ? ChatColor.GREEN : ChatColor.RED) + "Bonus por Jugadores");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Suma mobs extra según cuántos");
        lore.add(ChatColor.GRAY + "jugadores haya en rango.");
        lore.add("");
        if (fixedMode) {
            lore.add(ChatColor.RED + "⚠ Solo activo en modo escalado");
            lore.add(ChatColor.RED + "  (min ≠ max).");
            lore.add("");
        }
        lore.add(ChatColor.GRAY + "Estado: " + (enabled ? ChatColor.GREEN + "ACTIVADO" : ChatColor.RED + "DESACTIVADO"));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click para " + (enabled ? "desactivar" : "activar"));
        meta.setLore(lore);
        item.setItemMeta(meta); return item;
    }

    /**
     * Configura el cap de jugadores extra que suman bonus.
     * Aparece atenuado si el bonus está desactivado o el modo es fijo.
     */
    private ItemStack makeBonusCapPaper(int cap, boolean bonusEnabled, boolean fixedMode) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        boolean active = bonusEnabled && !fixedMode;
        meta.setDisplayName((active ? ChatColor.YELLOW : ChatColor.DARK_GRAY) + "Cap de Jugadores Extra");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Máximo de jugadores extra que");
        lore.add(ChatColor.GRAY + "suman +1 mob cada uno.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Valor actual: " + (active ? ChatColor.WHITE : ChatColor.DARK_GRAY) + cap);
        lore.add("");
        if (!active) lore.add(ChatColor.DARK_GRAY + "(Activa el bonus para configurar)");
        else         lore.add(ChatColor.YELLOW + "Click para modificar");
        meta.setLore(lore);
        item.setItemMeta(meta); return item;
    }

    private ItemStack makeLootButton(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat); ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name); meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta); return item;
    }

    // =========================================================================
    // EVENTOS DE GUI
    // =========================================================================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        if (title.equals("Configuración Trial Spawner")) {
            event.setCancelled(true); handleMainGUIClick(player, event.getSlot()); return;
        }
        if (title.contains("Pool Especial") || title.contains("Pool Vanilla")) {
            event.setCancelled(true); handlePoolGUIClick(player, event); return;
        }
        if (title.equals("Loot Configurado")) {
            event.setCancelled(true);
            if (event.getSlot() == 49) openMainGUI(player, editingSpawners.get(player.getUniqueId()));
        }
    }

    private void handleMainGUIClick(Player player, int slot) {
        Map<Integer, NamespacedKey> simple = new HashMap<>();
        simple.put(10, totalMobsKey);
        simple.put(12, ticksKey);
        simple.put(13, rangeKey);
        simple.put(14, cooldownKey);
        simple.put(15, playerRangeKey);
        simple.put(28, lootMaxItemsKey);
        simple.put(29, lootSpecialMaxKey);
        if (simple.containsKey(slot)) { askForInput(player, simple.get(slot)); return; }

        if (slot == 11) { askForWaveSizeInput(player); return; }

        // Toggle bonus jugadores
        if (slot == 31) { togglePlayerBonus(player); return; }

        // Cap de jugadores extra — solo si bonus activo y modo escalado
        if (slot == 32) {
            ItemStack sp = editingSpawners.get(player.getUniqueId());
            if (sp == null) return;
            PersistentDataContainer pdc = sp.getItemMeta().getPersistentDataContainer();
            boolean bonusOn  = pdc.getOrDefault(playerBonusEnabledKey, PersistentDataType.INTEGER, 0) == 1;
            int simMin = pdc.getOrDefault(simMinKey, PersistentDataType.INTEGER, 2);
            int simMax = pdc.getOrDefault(simMaxKey, PersistentDataType.INTEGER, 2);
            if (!bonusOn || simMin == simMax) {
                player.sendMessage(ChatColor.RED + "Activa el bonus y usa modo escalado (min-max) primero.");
                return;
            }
            askForInput(player, playerBonusMaxKey);
            return;
        }

        switch (slot) {
            case 37: editingOminousMode.put(player.getUniqueId(), false); editingVanillaPool.put(player.getUniqueId(), false); openPoolGUI(player, 0); break;
            case 38: editingOminousMode.put(player.getUniqueId(), true);  editingVanillaPool.put(player.getUniqueId(), false); openPoolGUI(player, 0); break;
            case 39: editingOminousMode.put(player.getUniqueId(), false); editingVanillaPool.put(player.getUniqueId(), true);  openPoolGUI(player, 0); break;
            case 40: editingOminousMode.put(player.getUniqueId(), true);  editingVanillaPool.put(player.getUniqueId(), true);  openPoolGUI(player, 0); break;
            case 44: openConfiguredLootGUI(player); break;
        }
    }

    /** Alterna playerBonusEnabled directamente sin pedir input de chat. */
    private void togglePlayerBonus(Player player) {
        ItemStack spawner = editingSpawners.get(player.getUniqueId());
        if (spawner == null) return;
        ItemMeta meta = spawner.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean current = pdc.getOrDefault(playerBonusEnabledKey, PersistentDataType.INTEGER, 0) == 1;
        pdc.set(playerBonusEnabledKey, PersistentDataType.INTEGER, current ? 0 : 1);
        spawner.setItemMeta(meta);
        rebuildSpawnerLore(spawner, plugin);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, current ? 0.8f : 1.2f);
        openMainGUI(player, spawner); // refrescar GUI
    }

    private void askForWaveSizeInput(Player player) {
        playersWaitingForInput.put(player.getUniqueId(), "wave_size");
        player.closeInventory();
        player.sendMessage(ChatColor.GOLD + "Ingresa el tamaño de oleada §7(o §ccancelar§7):");
        player.sendMessage(ChatColor.GRAY + "  §fN       §7→ siempre N mobs por oleada (fijo).");
        player.sendMessage(ChatColor.GRAY + "  §fmin-max §7→ escala desde min hasta max oleada a oleada.");
        player.sendMessage(ChatColor.GRAY + "Ejemplo: §f3 §7o §f2-5");
    }

    private void askForInput(Player player, NamespacedKey key) {
        playersWaitingForInput.put(player.getUniqueId(), key.getKey());
        player.closeInventory();
        String label = key.getKey().replace("viciont_ts_", "").replace("viciont_trial_", "").replace("_", " ");
        player.sendMessage(ChatColor.GOLD + "Ingresa el nuevo valor para §e" + label + ChatColor.GOLD + " §7(o §ccancelar§7):");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!playersWaitingForInput.containsKey(uuid)) return;
        event.setCancelled(true);

        String property = playersWaitingForInput.get(uuid);
        String input    = event.getMessage().trim();

        if (input.equalsIgnoreCase("cancelar")) {
            playersWaitingForInput.remove(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> { if (editingSpawners.containsKey(uuid)) openMainGUI(player, editingSpawners.get(uuid)); });
            return;
        }

        if (property.contains("_item_amount|")) { handlePoolItemInput(player, uuid, property, input); return; }
        if (property.equals("wave_size"))       { handleWaveSizeInput(player, uuid, input);           return; }

        // Inputs numéricos genéricos
        try {
            int newValue = Integer.parseInt(input);
            ItemStack spawner = editingSpawners.get(uuid);
            if (spawner == null) { playersWaitingForInput.remove(uuid); return; }
            NamespacedKey key = resolveKey(property);
            if (key == null) { playersWaitingForInput.remove(uuid); return; }
            ItemMeta meta = spawner.getItemMeta();
            meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, newValue);
            spawner.setItemMeta(meta);
            rebuildSpawnerLore(spawner, plugin);
            player.sendMessage(ChatColor.GREEN + "¡Valor actualizado a " + ChatColor.WHITE + newValue + ChatColor.GREEN + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
            playersWaitingForInput.remove(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> openMainGUI(player, spawner));
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Debes ingresar un número válido.");
        }
    }

    /**
     * Procesa el input del tamaño de oleada.
     * Acepta:  "3"   → min=max=3 (fijo)
     *          "2-5" → min=2 max=5 (escalado)
     */
    private void handleWaveSizeInput(Player player, UUID uuid, String input) {
        ItemStack spawner = editingSpawners.get(uuid);
        if (spawner == null) { playersWaitingForInput.remove(uuid); return; }

        int min, max;
        if (input.contains("-")) {
            String[] parts = input.split("-", 2);
            try { min = Integer.parseInt(parts[0].trim()); max = Integer.parseInt(parts[1].trim()); }
            catch (Exception e) { player.sendMessage(ChatColor.RED + "Formato inválido. Usa §fN §co §fmin-max §c(ej: 3 o 2-5)."); return; }
        } else {
            try { min = max = Integer.parseInt(input.trim()); }
            catch (Exception e) { player.sendMessage(ChatColor.RED + "Formato inválido. Escribe un número o min-max."); return; }
        }

        if (min < 1 || max < 1 || min > max || max > 64) {
            player.sendMessage(ChatColor.RED + "Valores inválidos. Min ≥ 1, max ≤ 64, min ≤ max."); return;
        }

        ItemMeta meta = spawner.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(simMinKey, PersistentDataType.INTEGER, min);
        pdc.set(simMaxKey, PersistentDataType.INTEGER, max);
        spawner.setItemMeta(meta);
        rebuildSpawnerLore(spawner, plugin);

        boolean fixed = (min == max);
        player.sendMessage(ChatColor.GREEN + "Oleadas configuradas: "
                + (fixed ? ChatColor.WHITE + String.valueOf(min) + ChatColor.GREEN + " (fijo)"
                : ChatColor.WHITE + "" + min + ChatColor.GREEN + " → " + ChatColor.WHITE + max + ChatColor.GREEN + " (escalado)"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
        playersWaitingForInput.remove(uuid);
        Bukkit.getScheduler().runTask(plugin, () -> openMainGUI(player, spawner));
    }

    private NamespacedKey resolveKey(String keyStr) {
        if (keyStr.equals(totalMobsKey.getKey()))          return totalMobsKey;
        if (keyStr.equals(ticksKey.getKey()))              return ticksKey;
        if (keyStr.equals(rangeKey.getKey()))              return rangeKey;
        if (keyStr.equals(cooldownKey.getKey()))           return cooldownKey;
        if (keyStr.equals(playerRangeKey.getKey()))        return playerRangeKey;
        if (keyStr.equals(playerBonusMaxKey.getKey()))     return playerBonusMaxKey;
        if (keyStr.equals(lootMaxItemsKey.getKey()))       return lootMaxItemsKey;
        if (keyStr.equals(lootSpecialMaxKey.getKey()))     return lootSpecialMaxKey;
        return null;
    }

    private void openConfiguredLootGUI(Player player) {
        ItemStack spawnerItem = editingSpawners.get(player.getUniqueId());
        if (spawnerItem == null) return;
        PersistentDataContainer pdc = spawnerItem.getItemMeta().getPersistentDataContainer();

        Map<String, int[]> norm    = parseLootStringStatic(pdc.getOrDefault(normalLootKey,         PersistentDataType.STRING, ""));
        Map<String, int[]> omin    = parseLootStringStatic(pdc.getOrDefault(ominousLootKey,        PersistentDataType.STRING, ""));
        Map<String, int[]> vanNorm = parseLootStringStatic(pdc.getOrDefault(vanillaNormalLootKey,  PersistentDataType.STRING, ""));
        Map<String, int[]> vanOmin = parseLootStringStatic(pdc.getOrDefault(vanillaOminousLootKey, PersistentDataType.STRING, ""));

        Inventory gui = Bukkit.createInventory(null, 54, "Loot Configurado");
        int slot = 0;
        slot = fillConfiguredSection(gui, slot, norm,    "§aNormal Custom",  player, false);
        slot = fillConfiguredSection(gui, slot, omin,    "§5Ominous Custom", player, false);
        slot = fillConfiguredSection(gui, slot, vanNorm, "§eNormal Vanilla", player, true);
        fillConfiguredSection(gui, slot, vanOmin, "§6Ominous Vanilla", player, true);

        if (norm.isEmpty() && omin.isEmpty() && vanNorm.isEmpty() && vanOmin.isEmpty()) {
            ItemStack b = new ItemStack(Material.BARRIER);
            ItemMeta m = b.getItemMeta(); m.setDisplayName(ChatColor.RED + "No hay loot configurado."); b.setItemMeta(m);
            gui.setItem(22, b);
        }
        gui.setItem(49, makeLootButton(Material.BARRIER, "§cVolver"));
        player.openInventory(gui);
    }

    private int fillConfiguredSection(Inventory gui, int slot, Map<String, int[]> lootMap, String label, Player player, boolean isVanilla) {
        if (lootMap.isEmpty()) return slot;
        for (Map.Entry<String, int[]> entry : lootMap.entrySet()) {
            if (slot >= 45) break;
            ItemStack display;
            if (isVanilla) {
                try { display = new ItemStack(Material.valueOf(entry.getKey())); } catch (IllegalArgumentException e) { display = new ItemStack(Material.BARRIER); }
            } else {
                display = itemManager.getItem(entry.getKey(), 1, player);
                if (display == null) display = new ItemStack(Material.BARRIER);
            }
            ItemMeta meta = display.getItemMeta();
            if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(display.getType());
            int[] r = entry.getValue();
            String qty = r[0] == r[1] ? String.valueOf(r[0]) : r[0] + "-" + r[1];
            meta.setDisplayName(ChatColor.WHITE + entry.getKey() + ChatColor.YELLOW + " x" + qty);
            meta.setLore(Arrays.asList(label, ChatColor.GRAY + "Cantidad: §f" + qty));
            display.setItemMeta(meta);
            gui.setItem(slot++, display);
        }
        return slot;
    }

    private void handlePoolGUIClick(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();
        int page = currentPage.getOrDefault(player.getUniqueId(), 0);

        if (slot == 45 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) { openPoolGUI(player, page - 1); return; }
        if (slot == 53 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) { openPoolGUI(player, page + 1); return; }
        if (slot == 49) { openMainGUI(player, editingSpawners.get(player.getUniqueId())); return; }
        if (slot == 50) { editingVanillaPool.put(player.getUniqueId(), !editingVanillaPool.getOrDefault(player.getUniqueId(), false)); openPoolGUI(player, 0); return; }
        if (slot < 0 || slot >= 45) return;

        boolean isVanilla = editingVanillaPool.getOrDefault(player.getUniqueId(), false);
        boolean isOminous = editingOminousMode.getOrDefault(player.getUniqueId(), false);
        ItemStack spawnerItem = editingSpawners.get(player.getUniqueId());
        if (spawnerItem == null) return;

        int itemIndex = (page * 45) + slot;
        List<?> sourceList = isVanilla ? allVanillaMaterials : allAvailableItems;
        if (itemIndex >= sourceList.size()) return;

        String entryKey = isVanilla ? ((Material) sourceList.get(itemIndex)).name() : (String) sourceList.get(itemIndex);
        NamespacedKey targetKey = isVanilla ? (isOminous ? vanillaOminousLootKey : vanillaNormalLootKey) : (isOminous ? ominousLootKey : normalLootKey);

        if (event.isLeftClick()) {
            playersWaitingForInput.put(player.getUniqueId(), (isVanilla ? "vanilla_item_amount|" : "custom_item_amount|") + entryKey + "|" + (isOminous ? "1" : "0"));
            player.closeInventory();
            player.sendMessage(ChatColor.GOLD + "Escribe la cantidad para §e" + entryKey + ChatColor.GOLD + ":");
            player.sendMessage(ChatColor.GRAY + "Formato: §fmin-max §7o un número. Escribe §cquitar§7 para eliminar.");
        } else if (event.isRightClick()) {
            Map<String, int[]> loot = parseLootStringStatic(spawnerItem.getItemMeta().getPersistentDataContainer().getOrDefault(targetKey, PersistentDataType.STRING, ""));
            loot.remove(entryKey);
            saveLootToItem(spawnerItem, targetKey, loot);
            rebuildSpawnerLore(spawnerItem, plugin);
            openPoolGUI(player, page);
        }
    }

    private void handlePoolItemInput(Player player, UUID uuid, String property, String input) {
        String[] parts = property.split("\\|");
        boolean isVanillaEntry = parts[0].equals("vanilla_item_amount");
        String entryKey = parts[1]; boolean isOminous = parts[2].equals("1");
        ItemStack spawnerItem = editingSpawners.get(uuid);
        if (spawnerItem == null) { playersWaitingForInput.remove(uuid); return; }

        NamespacedKey tk = isVanillaEntry ? (isOminous ? vanillaOminousLootKey : vanillaNormalLootKey) : (isOminous ? ominousLootKey : normalLootKey);
        Map<String, int[]> loot = parseLootStringStatic(spawnerItem.getItemMeta().getPersistentDataContainer().getOrDefault(tk, PersistentDataType.STRING, ""));

        if (input.equalsIgnoreCase("quitar")) {
            loot.remove(entryKey); saveLootToItem(spawnerItem, tk, loot); rebuildSpawnerLore(spawnerItem, plugin);
            playersWaitingForInput.remove(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> openPoolGUI(player, currentPage.getOrDefault(uuid, 0)));
            return;
        }

        int min, max;
        if (input.contains("-")) {
            String[] r = input.split("-");
            try { min = Integer.parseInt(r[0].trim()); max = Integer.parseInt(r[1].trim()); } catch (Exception e) { player.sendMessage(ChatColor.RED + "Formato inválido."); return; }
        } else {
            try { min = max = Integer.parseInt(input.trim()); } catch (Exception e) { player.sendMessage(ChatColor.RED + "Número inválido."); return; }
        }
        if (min <= 0 || max <= 0 || min > max || max > 64) { player.sendMessage(ChatColor.RED + "Valores inválidos (1-64, min ≤ max)."); return; }

        loot.put(entryKey, new int[]{min, max});
        saveLootToItem(spawnerItem, tk, loot);
        rebuildSpawnerLore(spawnerItem, plugin);
        player.sendMessage(ChatColor.GREEN + "¡Loot actualizado!");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
        playersWaitingForInput.remove(uuid);
        Bukkit.getScheduler().runTask(plugin, () -> openPoolGUI(player, currentPage.getOrDefault(uuid, 0)));
    }

    private void saveLootToItem(ItemStack spawnerItem, NamespacedKey targetKey, Map<String, int[]> lootMap) {
        ItemMeta meta = spawnerItem.getItemMeta();
        meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, serializeLoot(lootMap));
        spawnerItem.setItemMeta(meta);
    }

    private void openPoolGUI(Player player, int page) {
        boolean isOminous = editingOminousMode.getOrDefault(player.getUniqueId(), false);
        boolean isVanilla = editingVanillaPool.getOrDefault(player.getUniqueId(), false);
        ItemStack spawnerItem = editingSpawners.get(player.getUniqueId());
        if (spawnerItem == null) return;

        List<?> sourceList = isVanilla ? allVanillaMaterials : allAvailableItems;
        int totalPages = (int) Math.ceil(sourceList.size() / 45.0);
        page = Math.max(0, Math.min(page, Math.max(0, totalPages - 1)));

        NamespacedKey targetKey = isVanilla ? (isOminous ? vanillaOminousLootKey : vanillaNormalLootKey) : (isOminous ? ominousLootKey : normalLootKey);
        String title = (isVanilla ? (isOminous ? "Pool Vanilla Ominous" : "Pool Vanilla Normal")
                : (isOminous ? "Pool Especial Ominous" : "Pool Especial Normal"))
                + " (Pag " + (page + 1) + ")";
        Inventory gui = Bukkit.createInventory(null, 54, title);
        Map<String, int[]> currentLoot = parseLootStringStatic(spawnerItem.getItemMeta().getPersistentDataContainer().getOrDefault(targetKey, PersistentDataType.STRING, ""));
        int startIndex = page * 45;

        for (int i = 0; i < 45 && (startIndex + i) < sourceList.size(); i++) {
            ItemStack displayItem; String entryKey;
            if (isVanilla) {
                Material mat = (Material) sourceList.get(startIndex + i);
                entryKey = mat.name(); displayItem = new ItemStack(mat);
                ItemMeta meta = displayItem.getItemMeta();
                if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(mat);
                meta.setDisplayName(ChatColor.WHITE + entryKey);
                int[] r = currentLoot.get(entryKey);
                meta.setLore(Arrays.asList(r != null ? "§aSeleccionado: §f" + r[0] + "-" + r[1] : "§cNo seleccionado", "§eClick Izq: Configurar", "§cClick Der: Quitar"));
                displayItem.setItemMeta(meta);
            } else {
                entryKey = (String) sourceList.get(startIndex + i);
                displayItem = itemManager.getItem(entryKey, 1, player);
                if (displayItem == null) displayItem = new ItemStack(Material.BARRIER);
                ItemMeta meta = displayItem.getItemMeta();
                List<String> lore = (meta != null && meta.hasLore()) ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                int[] r = currentLoot.get(entryKey);
                lore.add(" "); lore.add(r != null ? "§aSeleccionado: §f" + r[0] + "-" + r[1] : "§cNo seleccionado");
                lore.add("§eClick Izq: Configurar"); lore.add("§cClick Der: Quitar");
                if (meta != null) { meta.setLore(lore); displayItem.setItemMeta(meta); }
            }
            gui.setItem(i, displayItem);
        }

        if (page > 0) gui.setItem(45, makeNavItem("§ePágina Anterior", Material.ARROW));
        if (startIndex + 45 < sourceList.size()) gui.setItem(53, makeNavItem("§eSiguiente Página", Material.ARROW));
        gui.setItem(49, makeNavItem("§cVolver", Material.BARRIER));
        gui.setItem(50, makeNavItem(isVanilla ? "§bVer pool Especial" : "§eVer pool Vanilla", isVanilla ? Material.NETHER_STAR : Material.BOOK, "§7Click para cambiar"));
        currentPage.put(player.getUniqueId(), page);
        player.openInventory(gui);
    }

    private ItemStack makeNavItem(String name, Material mat, String... lore) {
        ItemStack item = new ItemStack(mat); ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name); if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta); return item;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String title = event.getPlayer().getOpenInventory().getTitle();
            if (!title.contains("Pool") && !title.contains("Configuración Trial") && !title.equals("Loot Configurado")) {
                UUID id = event.getPlayer().getUniqueId();
                if (!playersWaitingForInput.containsKey(id))
                { editingSpawners.remove(id); editingOminousMode.remove(id); editingVanillaPool.remove(id); currentPage.remove(id); }
            }
        }, 2L);
    }

    // =========================================================================
    // LORE Y SERIALIZACIÓN
    // =========================================================================

    public static void rebuildSpawnerLore(ItemStack spawner, JavaPlugin plugin) {
        ItemMeta meta = spawner.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String mobType = pdc.get(new NamespacedKey(plugin, "viciont_trial_mob"), PersistentDataType.STRING);
        if (mobType == null) return;

        boolean isVanilla = mobType.startsWith("vanilla_");
        String displayName = formatEntityNameStatic(isVanilla ? mobType.substring(8) : mobType);
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Trial Spawner de " + displayName);

        int total  = pdc.getOrDefault(new NamespacedKey(plugin, "viciont_ts_total_mobs"), PersistentDataType.INTEGER, 6);
        int simMin = pdc.getOrDefault(new NamespacedKey(plugin, "viciont_ts_sim_min"),    PersistentDataType.INTEGER, 2);
        int simMax = pdc.getOrDefault(new NamespacedKey(plugin, "viciont_ts_sim_max"),    PersistentDataType.INTEGER, 2);
        boolean fixed = (simMin == simMax);
        int waves = (int) Math.ceil((double) total / Math.max(1, simMax));

        boolean bonusEnabled = pdc.getOrDefault(new NamespacedKey(plugin, "viciont_ts_player_bonus_enabled"), PersistentDataType.INTEGER, 0) == 1;
        int bonusCap = pdc.getOrDefault(new NamespacedKey(plugin, "viciont_ts_player_bonus_max"), PersistentDataType.INTEGER, 4);

        List<String> lore = new ArrayList<>();
        lore.add(""); lore.add(ChatColor.GRAY + "Oleadas de " + displayName); lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Tipo: " + ChatColor.WHITE + (isVanilla ? "Vanilla" : "Custom"));
        lore.add(ChatColor.DARK_GRAY + "Mob: "  + ChatColor.WHITE + (isVanilla ? mobType.substring(8) : mobType));
        lore.add("");
        lore.add(ChatColor.GRAY + "Total Mobs: "    + ChatColor.WHITE + total);
        lore.add(ChatColor.GRAY + "Oleadas:      "  + ChatColor.WHITE + waves);

        if (fixed) {
            lore.add(ChatColor.GRAY + "Mobs/Oleada: " + ChatColor.WHITE + simMin + ChatColor.GRAY + " (fijo)");
        } else {
            lore.add(ChatColor.GRAY + "Mobs/Oleada: " + ChatColor.WHITE + simMin + ChatColor.GRAY + " → " + ChatColor.WHITE + simMax + ChatColor.GRAY + " (escalado)");
            if (bonusEnabled)
                lore.add(ChatColor.GRAY + "Bonus jugadores: " + ChatColor.GREEN + "ON" + ChatColor.GRAY + "  cap +" + bonusCap);
            else
                lore.add(ChatColor.GRAY + "Bonus jugadores: " + ChatColor.RED + "OFF");
        }

        lore.add(ChatColor.GRAY + "Ticks Entre Oleadas: " + ChatColor.WHITE + pdc.getOrDefault(new NamespacedKey(plugin, "viciont_ts_ticks"),        PersistentDataType.INTEGER, 40));
        lore.add(ChatColor.GRAY + "Spawn Range: "         + ChatColor.WHITE + pdc.getOrDefault(new NamespacedKey(plugin, "viciont_ts_range"),         PersistentDataType.INTEGER, 4));
        lore.add(ChatColor.GRAY + "Cooldown Segundos: "   + ChatColor.WHITE + pdc.getOrDefault(new NamespacedKey(plugin, "viciont_ts_cooldown"),      PersistentDataType.INTEGER, 30));
        lore.add(ChatColor.GRAY + "Player Range: "        + ChatColor.WHITE + pdc.getOrDefault(new NamespacedKey(plugin, "viciont_ts_player_range"),  PersistentDataType.INTEGER, 16));
        lore.add("");
        lore.add(ChatColor.GRAY + "Loot Max Items: "    + ChatColor.WHITE + pdc.getOrDefault(new NamespacedKey(plugin, "viciont_trial_loot_max_items"),   PersistentDataType.INTEGER, 4));
        lore.add(ChatColor.GRAY + "Loot Especial Max: " + ChatColor.WHITE + pdc.getOrDefault(new NamespacedKey(plugin, "viciont_trial_loot_special_max"),  PersistentDataType.INTEGER, 2));

        Map<String, int[]> norm    = parseLootStringStatic(pdc.getOrDefault(new NamespacedKey(plugin, "viciont_trial_loot_normal"),          PersistentDataType.STRING, ""));
        Map<String, int[]> omin    = parseLootStringStatic(pdc.getOrDefault(new NamespacedKey(plugin, "viciont_trial_loot_ominous"),         PersistentDataType.STRING, ""));
        Map<String, int[]> vanNorm = parseLootStringStatic(pdc.getOrDefault(new NamespacedKey(plugin, "viciont_trial_loot_vanilla_normal"),  PersistentDataType.STRING, ""));
        Map<String, int[]> vanOmin = parseLootStringStatic(pdc.getOrDefault(new NamespacedKey(plugin, "viciont_trial_loot_vanilla_ominous"), PersistentDataType.STRING, ""));

        if (!norm.isEmpty() || !omin.isEmpty() || !vanNorm.isEmpty() || !vanOmin.isEmpty()) {
            lore.add("");
            appendLootSection(lore, "Botín Especial Normal",  norm);
            appendLootSection(lore, "Botín Especial Ominous", omin);
            appendLootSection(lore, "Botín Vanilla Normal",   vanNorm);
            appendLootSection(lore, "Botín Vanilla Ominous",  vanOmin);
        }
        lore.add(""); lore.add(ChatColor.YELLOW + "Shift + Click derecho para configurar");
        meta.setLore(lore);
        spawner.setItemMeta(meta);
    }

    private static void appendLootSection(List<String> lore, String title, Map<String, int[]> map) {
        if (map.isEmpty()) return;
        lore.add(ChatColor.DARK_GRAY + title + ":");
        StringBuilder line = new StringBuilder(ChatColor.GRAY + "  ");
        int count = 0;
        for (Map.Entry<String, int[]> e : map.entrySet()) {
            String qty = e.getValue()[0] == e.getValue()[1] ? String.valueOf(e.getValue()[0]) : e.getValue()[0] + "-" + e.getValue()[1];
            if (count > 0) line.append(ChatColor.DARK_GRAY).append(", ");
            line.append(ChatColor.WHITE).append(formatEntityNameStatic(e.getKey())).append(ChatColor.YELLOW).append(" x").append(qty);
            count++;
            if (count == 3) { lore.add(line.toString()); line = new StringBuilder(ChatColor.GRAY + "  "); count = 0; }
        }
        if (count > 0) lore.add(line.toString());
    }

    public static Map<String, int[]> parseLootStringStatic(String data) {
        Map<String, int[]> map = new LinkedHashMap<>();
        if (data == null || data.isEmpty()) return map;
        for (String entry : data.split(",")) {
            if (entry.isEmpty()) continue;
            String[] p = entry.split(":");
            try {
                if (p.length == 2) map.put(p[0], new int[]{Integer.parseInt(p[1]), Integer.parseInt(p[1])});
                else if (p.length == 3) map.put(p[0], new int[]{Integer.parseInt(p[1]), Integer.parseInt(p[2])});
            } catch (Exception ignored) {}
        }
        return map;
    }

    private String serializeLoot(Map<String, int[]> map) {
        return map.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()[0] + ":" + e.getValue()[1]).collect(Collectors.joining(","));
    }

    public static String formatEntityNameStatic(String name) {
        if (name == null || name.isEmpty()) return name;
        return Arrays.stream(name.toLowerCase().split("_"))
                .map(s -> s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining(" "));
    }

    // =========================================================================
    // PROTECCIÓN ANTI-GRIEF (Explosiones y Entidades)
    // =========================================================================

    @EventHandler
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        event.blockList().removeIf(this::isCustomTrialSpawner);
    }

    @EventHandler
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        event.blockList().removeIf(this::isCustomTrialSpawner);
    }

    @EventHandler
    public void onEntityChangeBlock(org.bukkit.event.entity.EntityChangeBlockEvent event) {
        if (isCustomTrialSpawner(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    private boolean isCustomTrialSpawner(Block block) {
        if (block.getType() != Material.SPAWNER) return false;
        BlockState state = block.getState();
        if (!(state instanceof CreatureSpawner)) return false;
        CreatureSpawner cs = (CreatureSpawner) state;
        return cs.getPersistentDataContainer().has(presetIdKey, PersistentDataType.STRING);
    }

    // =========================================================================
    // HELPERS PDC / ENTITY TYPE
    // =========================================================================

    private void copyIntKey(PersistentDataContainer src, PersistentDataContainer dst, NamespacedKey key, int def) {
        dst.set(key, PersistentDataType.INTEGER, src.getOrDefault(key, PersistentDataType.INTEGER, def));
    }
    private void copyStrKey(PersistentDataContainer src, PersistentDataContainer dst, NamespacedKey key) {
        if (src.has(key, PersistentDataType.STRING)) dst.set(key, PersistentDataType.STRING, src.get(key, PersistentDataType.STRING));
    }

    /**
     * Aplica un CustomName al SpawnData del bloque spawner mediante comando NBT,
     * igual que hace CustomSpawnerHandler. Esto permite que el mob mostrado
     * dentro de la jaula del spawner use el disfraz/textura del mob custom
     * en vez del modelo vanilla "sin textura".
     */
    private void applyCustomNameToSpawner(CreatureSpawner spawner, String customName) {
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

    /**
     * Nombre custom usado para el SpawnData del spawner, copiado de CustomSpawnerHandler,
     * para que el mob mostrado dentro de la jaula tenga el disfraz/textura correcto.
     */
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
            case "corruptedinsect":
                return "Corrupted Insect";
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
            case "toxicspider":
                return "Toxic Spider";
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
            case "infernalbeast":
                return "Infernal Beast";
            case "corrupteddrowned":
                return "Corrupted Drowned";
            case "corruptedbee":
                return "Corrupted Bee";
            default:
                return null;
        }
    }

    private EntityType getBaseEntityType(String mobType) {
        if (mobType.startsWith("vanilla_")) { try { return EntityType.valueOf(mobType.substring(8).toUpperCase()); } catch (IllegalArgumentException e) { return EntityType.PIG; } }
        switch (mobType.toLowerCase()) {
            case "bombita": case "corruptedcreeper": case "infernalcreeper": case "endercreeper": case "darkcreeper": return EntityType.CREEPER;
            case "iceologer": return EntityType.ILLUSIONER;
            case "corruptedzombie": return EntityType.ZOMBIE;
            case "corruptedspider": case "corruptedinfernalspider": return EntityType.SPIDER;
            case "toxicspider": return EntityType.CAVE_SPIDER;
            case "queenbee": case "hellishbee": case "infestedbee": case "corruptedbee": return EntityType.BEE;
            case "guardianblaze": return EntityType.BLAZE;
            case "guardiancorruptedskeleton": return EntityType.WITHER_SKELETON;
            case "corruptedskeleton": case "darkskeleton": return EntityType.SKELETON;
            case "buffbreeze": return EntityType.BREEZE;
            case "invertedghast": case "enderghast": case "piglinglobo": return EntityType.GHAST;
            case "netheritevexguardian": case "darkvex": return EntityType.VEX;
            case "ultrawitherboss": return EntityType.WITHER;
            case "whiteenderman": return EntityType.ENDERMAN;
            case "corruptedinsect": return EntityType.ENDERMITE;
            case "fastravager": return EntityType.RAVAGER;
            case "bruteimperial": return EntityType.PIGLIN_BRUTE;
            case "infernalbeast": return EntityType.HOGLIN;
            case "batboom": return EntityType.BAT;
            case "endersilverfish": return EntityType.SILVERFISH;
            case "guardianshulker": return EntityType.SHULKER;
            case "spectraleeye": return EntityType.PHANTOM;
            case "corrupteddrowned": return EntityType.DROWNED;
            default: return EntityType.PIG;
        }
    }

    // =========================================================================
    // SHUTDOWN
    // =========================================================================

    public void shutdown() {
        if (spawnTask != null && !spawnTask.isCancelled()) spawnTask.cancel();
        if (cooldownDisplayTask != null && !cooldownDisplayTask.isCancelled()) cooldownDisplayTask.cancel();
        for (Map.Entry<Location, TrialSpawnerData> e : activeSpawners.entrySet()) forceSaveStateToBlock(e.getKey(), e.getValue());
        activeSpawners.clear();
    }

    public void clearActiveSpawners() { activeSpawners.clear(); }
}