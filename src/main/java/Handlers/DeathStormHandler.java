package Handlers;

import Events.DamageLogListener;
import TitleListener.MuerteHandler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.event.player.PlayerBedEnterEvent;

public class DeathStormHandler implements Listener {
    private final JavaPlugin plugin;
    private final DayHandler dayHandler;
    private final DamageLogListener damageLogListener;

    // Thread-safe collections para concurrencia
    private final Map<UUID, Integer> deathCount = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastLightningCheck = new ConcurrentHashMap<>();
    private final Set<UUID> pausedActionBars = ConcurrentHashMap.newKeySet();
    private final Set<Chunk> recentlyStruckChunks = ConcurrentHashMap.newKeySet();

    // Variables de estado optimizadas
    private volatile int remainingStormSeconds = 0;
    private volatile boolean isDeathStormActive = false;

    // Caché para optimizar cálculos repetitivos
    private final Map<Integer, String> timeFormatCache = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> actionBarEligibilityCache = new ConcurrentHashMap<>();
    private long lastCacheClean = System.currentTimeMillis();

    // Constantes optimizadas
    private static final long CHUNK_COOLDOWN = 5000; // 5 segundos
    private static final long CACHE_CLEAN_INTERVAL = 30000; // 30 segundos
    private static final long LIGHTNING_CHECK_COOLDOWN = 5 * 60 * 1000; // 5 minutos
    private static final int CACHE_MAX_SIZE = 1000;

    // Objetos reutilizables para reducir GC
    private BukkitRunnable stormTask;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    // Colores como constantes para evitar recreación
    private static final net.md_5.bungee.api.ChatColor COLOR_PRIMARIO = net.md_5.bungee.api.ChatColor.of("#9179D4");
    private static final net.md_5.bungee.api.ChatColor COLOR_TIEMPO = net.md_5.bungee.api.ChatColor.of("#7700A0");
    private static final net.md_5.bungee.api.ChatColor COLOR_GRIS = net.md_5.bungee.api.ChatColor.GRAY;

    public DeathStormHandler(JavaPlugin plugin, DayHandler dayHandler) {
        this.plugin = plugin;
        this.dayHandler = dayHandler;
        this.damageLogListener = new DamageLogListener(plugin, this);
        loadStormData();

        // Tarea asíncrona para limpiar cachés periódicamente
        startCacheCleanupTask();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUniqueId();

        // Operación atómica thread-safe
        deathCount.merge(playerUUID, 1, Integer::sum);

        int currentDay = dayHandler.getCurrentDay();
        int increment;

        if (currentDay >= 15 && currentDay < 20) {
            increment = (currentDay - 14);
        } else if (currentDay >= 20) {
            increment = (currentDay - 19);
        } else {
            increment = currentDay;
        }

        // Operación atómica para evitar condiciones de carrera
        synchronized (this) {
            remainingStormSeconds += 3600 * increment;
            if (!isDeathStormActive) {
                isDeathStormActive = true;
                startStorm();
            }
        }

        // Invalidar caché del jugador
        invalidatePlayerCache(playerUUID);
        saveStormDataAsync();
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!event.toWeatherState() && isDeathStormActive) {
            event.setCancelled(true);
        }
    }

    public boolean isDeathStormActive() {
        return isDeathStormActive;
    }

    @EventHandler
    public void onMonsterSpawnForDeathStorm(CreatureSpawnEvent event) {
        if (!isDeathStormActive) return;

        int currentDay = dayHandler.getCurrentDay();
        if (currentDay < 6) return;

        if (event.getEntity() instanceof org.bukkit.entity.Monster) {
            LivingEntity mob = (LivingEntity) event.getEntity();
            int effectDuration = Math.max(20, remainingStormSeconds * 20);

            // Aplicar efectos de forma asíncrona para no bloquear el hilo principal
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                if (mob.isValid() && !mob.isDead()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, effectDuration, 0, false, true));
                        mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, effectDuration, 0, false, true));
                    });
                }
            });
        }
    }

    private void startStorm() {
        World world = Bukkit.getWorlds().get(0);

        if (stormTask != null && !stormTask.isCancelled()) {
            stormTask.cancel();
        }

        world.setStorm(true);
        world.setThundering(true);

        stormTask = new BukkitRunnable() {
            private final List<Player> cachedPlayers = new ArrayList<>();
            private int tickCounter = 0;

            @Override
            public void run() {
                if (remainingStormSeconds <= 0) {
                    cancel();
                    endStorm(world);
                    return;
                }

                // Caché la lista de jugadores cada 5 ticks para reducir llamadas a getOnlinePlayers()
                if (tickCounter % 5 == 0) {
                    cachedPlayers.clear();
                    cachedPlayers.addAll(Bukkit.getOnlinePlayers());
                }
                tickCounter++;

                // Usar caché para el formato de tiempo
                String timeFormat = getFormattedTime(remainingStormSeconds);
                TextComponent message = createStormMessage(timeFormat);

                // Procesamiento optimizado de jugadores
                for (Player player : cachedPlayers) {
                    if (player.isOnline() && shouldShowDeathStormActionBarCached(player)) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
                    }
                }

                remainingStormSeconds--;

                // Ejecutar lightning spawn de forma asíncrona
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> spawnRandomLightning(world));
            }
        };

        stormTask.runTaskTimer(plugin, 0, 20);

        // Tarea para lightning en jugadores (día 15+)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isDeathStormActive) {
                    cancel();
                    return;
                }

                int currentDay = dayHandler.getCurrentDay();
                if (currentDay >= 15) {
                    checkPlayerLightningOptimized();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, 20 * 60 * 5);
    }

    private void endStorm(World world) {
        isDeathStormActive = false;
        world.setStorm(false);
        world.setThundering(false);
        recentlyStruckChunks.clear();
        clearCaches();
    }

    private String getFormattedTime(int seconds) {
        return timeFormatCache.computeIfAbsent(seconds, s -> {
            int hours = s / 3600;
            int minutes = (s % 3600) / 60;
            int secs = s % 60;
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        });
    }

    private TextComponent createStormMessage(String timeFormat) {
        TextComponent message = new TextComponent();

        TextComponent quedanText = new TextComponent("Quedan ");
        quedanText.setColor(COLOR_PRIMARIO);
        message.addExtra(quedanText);

        TextComponent clockSymbol = new TextComponent("⌚ ");
        clockSymbol.setColor(COLOR_GRIS);
        message.addExtra(clockSymbol);

        TextComponent timeComponent = new TextComponent(timeFormat);
        timeComponent.setColor(COLOR_TIEMPO);
        timeComponent.setBold(true);
        timeComponent.setUnderlined(true);
        message.addExtra(timeComponent);

        TextComponent stormMessage = new TextComponent(" horas de DeathStorm");
        stormMessage.setColor(COLOR_PRIMARIO);
        message.addExtra(stormMessage);

        return message;
    }

    private void spawnRandomLightning(World world) {
        int currentDay = dayHandler.getCurrentDay();

        int minStrikes, maxStrikes;
        long intervalTicks;
        if (currentDay >= 20) {
            minStrikes = 6;
            maxStrikes = 15;
            intervalTicks = 100L;
        } else if (currentDay >= 15) {
            minStrikes = 4;
            maxStrikes = 7;
            intervalTicks = 400L;
        } else {
            minStrikes = 1;
            maxStrikes = 1;
            intervalTicks = 2500L;
        }

        int lightningCount = random.nextInt(maxStrikes - minStrikes + 1) + minStrikes;

        // Optimización: usar array en lugar de Collection para mejor rendimiento
        Player[] players = world.getPlayers().toArray(new Player[0]);
        if (players.length == 0) return;

        // Pre-calcular chunks de jugadores de forma más eficiente
        Map<Location, Integer> chunkPlayerMap = new HashMap<>();
        for (Player player : players) {
            Location chunkCenter = player.getLocation().getChunk().getBlock(8, 0, 8).getLocation();
            chunkPlayerMap.merge(chunkCenter, 1, Integer::sum);
        }

        if (currentDay < 20) {
            chunkPlayerMap.entrySet().removeIf(entry -> entry.getValue() > 1);
        }

        if (chunkPlayerMap.isEmpty()) return;

        // Ejecutar en el hilo principal
        Bukkit.getScheduler().runTask(plugin, () -> {
            new BukkitRunnable() {
                int strikesRemaining = lightningCount;
                final Iterator<Location> locationIterator = chunkPlayerMap.keySet().iterator();

                @Override
                public void run() {
                    if (strikesRemaining <= 0 || !isDeathStormActive) {
                        cancel();
                        return;
                    }

                    if (locationIterator.hasNext()) {
                        Location chunkCenter = locationIterator.next();
                        spawnLightning(world, chunkCenter, currentDay >= 20);
                    }

                    strikesRemaining--;
                }
            }.runTaskTimer(plugin, 4, intervalTicks);
        });
    }

    private void spawnLightning(World world, Location location, boolean afterDay20) {
        Location lightningLocation = findSafeLightningLocationOptimized(world, location, afterDay20, 100);
        Chunk targetChunk = lightningLocation.getChunk();
        int currentDay = dayHandler.getCurrentDay();

        recentlyStruckChunks.add(targetChunk);

        // Programar eliminación del chunk del caché
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            recentlyStruckChunks.remove(targetChunk);
        }, CHUNK_COOLDOWN / 50);

        // Siempre efecto visual
        world.strikeLightningEffect(lightningLocation);

        // Solo romper bloques a partir del día 15 (excepto obsidiana y bedrock)
        if (currentDay >= 15) {
            Block block = world.getBlockAt(lightningLocation);
            if (block.getType() != Material.OBSIDIAN && block.getType() != Material.BEDROCK) {
                block.setType(Material.AIR);
            }
        }
    }

    private Location findSafeLightningLocationOptimized(World world, Location center, boolean afterDay20, int maxAttempts) {
        // Cache para chunks de jugadores si afterDay20 es false
        Set<Chunk> playerChunks = null;
        if (!afterDay20) {
            playerChunks = new HashSet<>();
            for (Player player : world.getPlayers()) {
                playerChunks.add(player.getLocation().getChunk());
            }
        }

        for (int attempts = 0; attempts < maxAttempts; attempts++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * (13 * 16);

            double x = center.getX() + Math.cos(angle) * distance;
            double z = center.getZ() + Math.sin(angle) * distance;

            Location loc = new Location(world, x, 0, z);
            loc.setY(world.getHighestBlockYAt(loc));

            if (!afterDay20) {
                Chunk chunk = loc.getChunk();
                if (!playerChunks.contains(chunk)) {
                    return loc;
                }
            } else {
                return loc;
            }
        }
        return center;
    }

    private void checkPlayerLightningOptimized() {
        long now = System.currentTimeMillis();
        List<Player> eligiblePlayers = new ArrayList<>();

        // Pre-filtrar jugadores elegibles
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();

            if (lastLightningCheck.getOrDefault(playerId, 0L) + LIGHTNING_CHECK_COOLDOWN <= now) {
                eligiblePlayers.add(player);
                lastLightningCheck.put(playerId, now);
            }
        }

        // Procesar en lotes para evitar sobrecargar el servidor
        if (!eligiblePlayers.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player player : eligiblePlayers) {
                    if (player.isOnline() && random.nextBoolean()) {
                        player.getWorld().strikeLightning(player.getLocation());
                        player.sendMessage(ChatColor.RED + "¡Un rayo te ha alcanzado!");
                    }
                }
            });
        }
    }

    private boolean shouldShowDeathStormActionBarCached(Player player) {
        UUID playerId = player.getUniqueId();

        // Usar caché con limpieza periódica
        return actionBarEligibilityCache.computeIfAbsent(playerId, id -> {
            if (isActionBarPausedForPlayer(id)) return false;
            if (MuerteHandler.isDeathMessageActive()) return false;
            if (damageLogListener != null && damageLogListener.isPlayerInDamageLog(id)) return false;
            return true;
        });
    }

    private void invalidatePlayerCache(UUID playerId) {
        actionBarEligibilityCache.remove(playerId);
    }

    private void clearCaches() {
        timeFormatCache.clear();
        actionBarEligibilityCache.clear();
    }

    private void startCacheCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                if (now - lastCacheClean > CACHE_CLEAN_INTERVAL) {
                    // Limpiar cachés si son muy grandes
                    if (timeFormatCache.size() > CACHE_MAX_SIZE) {
                        timeFormatCache.clear();
                    }
                    if (actionBarEligibilityCache.size() > CACHE_MAX_SIZE) {
                        actionBarEligibilityCache.clear();
                    }
                    lastCacheClean = now;
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, 20 * 30); // Cada 30 segundos
    }

    // Métodos públicos mantienen la misma interfaz
    public void resetStorm() {
        synchronized (this) {
            remainingStormSeconds = 0;
            isDeathStormActive = false;
        }
        recentlyStruckChunks.clear();
        clearCaches();
        saveStormDataAsync();
    }

    public void addStormSeconds(int seconds) {
        synchronized (this) {
            remainingStormSeconds += seconds;
            if (!isDeathStormActive) {
                isDeathStormActive = true;
                startStorm();
            }
        }
        saveStormDataAsync();
    }

    public void removeStormSeconds(int seconds) {
        synchronized (this) {
            remainingStormSeconds = Math.max(remainingStormSeconds - seconds, 0);
            if (remainingStormSeconds == 0) {
                isDeathStormActive = false;
                recentlyStruckChunks.clear();
                clearCaches();
            }
        }
        saveStormDataAsync();
    }

    public void pauseActionBarForPlayer(UUID playerId) {
        pausedActionBars.add(playerId);
        invalidatePlayerCache(playerId);
    }

    public void resumeActionBarForPlayer(UUID playerId) {
        pausedActionBars.remove(playerId);
        invalidatePlayerCache(playerId);
    }

    public boolean isActionBarPausedForPlayer(UUID playerId) {
        return pausedActionBars.contains(playerId);
    }

    private boolean shouldShowDeathStormActionBar(Player player) {
        return shouldShowDeathStormActionBarCached(player);
    }

    public void loadStormData() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(plugin.getDataFolder(), "DayandStorm.yml");
            if (file.exists()) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                int loadedSeconds = config.getInt("TormentaRestante", 0);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    remainingStormSeconds = loadedSeconds;
                    Bukkit.getLogger().info("Storm data loaded: " + remainingStormSeconds + " seconds remaining.");
                    if (remainingStormSeconds > 0) {
                        isDeathStormActive = true;
                        startStorm();
                    }
                });
            }
        });
    }

    private void saveStormDataAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveStormData);
    }

    public void saveStormData() {
        try {
            File file = new File(plugin.getDataFolder(), "DayandStorm.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            config.set("TormentaRestante", remainingStormSeconds);

            config.save(file);
            Bukkit.getLogger().info("Storm data saved: " + remainingStormSeconds + " seconds remaining.");
        } catch (IOException e) {
            Bukkit.getLogger().severe("Error saving storm: " + e.getMessage());
        }
    }
}