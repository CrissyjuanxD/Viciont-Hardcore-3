package Handlers;

import TitleListener.MuerteHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class DeathStormHandler implements Listener {
    private final JavaPlugin plugin;
    private final DayHandler dayHandler;

    // Thread-safe collections para concurrencia
    private final Set<Chunk> recentlyStruckChunks = ConcurrentHashMap.newKeySet();
    private final Map<Integer, Integer> deathsToday = new ConcurrentHashMap<>();
    private final Set<UUID> hiddenPlayers = ConcurrentHashMap.newKeySet();

    // Variables de estado optimizadas
    private volatile int remainingStormSeconds = 0;
    private volatile boolean isDeathStormActive = false;
    private volatile boolean isDeathStormStopped = false;
    private int totalStormSeconds = 1;

    // Caché para optimizar cálculos repetitivos
    private final Map<Integer, String> timeFormatCache = new ConcurrentHashMap<>();

    // Constantes optimizadas
    private static final long CHUNK_COOLDOWN = 5000; // 5 segundos

    // Objetos reutilizables para reducir GC
    private BukkitRunnable stormTask;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    private BossBar progressBar; // Rosada
    private BossBar timeBar;     // Blanca
    private double bossbarProgress = 1.0; // Guardable

    public DeathStormHandler(JavaPlugin plugin, DayHandler dayHandler) {
        this.plugin = plugin;
        this.dayHandler = dayHandler;
        this.progressBar = Bukkit.createBossBar("\uEAA9", BarColor.PINK, BarStyle.SOLID);
        this.timeBar = Bukkit.createBossBar("00:00:00", BarColor.WHITE, BarStyle.SOLID);
        this.progressBar.setVisible(false);
        this.timeBar.setVisible(false);
        loadStormData();
    }

    public void toggleStopDeathStorm(CommandSender sender) {
        isDeathStormStopped = !isDeathStormStopped;
        sender.sendMessage(isDeathStormStopped ?
                ChatColor.RED + "❌ La DeathStorm ha sido detenida temporalmente." :
                ChatColor.GREEN + "☁ La DeathStorm se reactivará normalmente.");
    }

    public void togglePlayerVisibility(Player player) {
        UUID id = player.getUniqueId();

        if (hiddenPlayers.contains(id)) {
            // 🔹 Volver a activar el temporizador
            hiddenPlayers.remove(id);
            player.sendMessage(ChatColor.of("#d37af0") + "☁ Has vuelto a activar el temporizador de la DeathStorm.");

            // Si hay DeathStorm activa, volver a añadir las bossbars
            if (isDeathStormActive) {
                progressBar.addPlayer(player);
                timeBar.addPlayer(player);
            }

        } else {
            // 🔹 Desactivar bossbars
            hiddenPlayers.add(id);
            progressBar.removePlayer(player);
            timeBar.removePlayer(player);

            // Enviar mensaje al ActionBar (no al chat)
            sendHiddenWarning(player);
        }
    }

    private void sendHiddenWarning(Player player) {
        // 💬 Mensaje con colores hex exactos en formato ActionBar
        String message =
                ChatColor.of("#f3687d") + "⊗" +
                        ChatColor.of("#dfc3c9") + " Tienes" +
                        ChatColor.of("#fd8698") + " desactivado " +
                        ChatColor.of("#dfc3c9") + "el timer de la " +
                        ChatColor.of("#b458d5") + "DeathStorm";

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    public void addDeathStormTime(Player player) {
        int currentDay = dayHandler.getCurrentDay();
        deathsToday.keySet().removeIf(day -> day != currentDay);
        // Registrar muerte del día actual
        deathsToday.merge(currentDay, 1, Integer::sum);
        int deathsTodayCount = deathsToday.get(currentDay);

        // Calcular horas según día
        int increment;
        if (currentDay >= 15 && currentDay < 20) {
            increment = (currentDay - 14);
        } else if (currentDay >= 20) {
            increment = (currentDay - 19);
        } else {
            increment = currentDay;
        }

        // Base 1 hora * increment
        int baseSeconds = 3600 * increment;

        // Por muerte adicional: +10min 30seg
        int extraSeconds = (deathsTodayCount - 1) * (10 * 60 + 30);
        int totalAdded = baseSeconds + extraSeconds;

        remainingStormSeconds += totalAdded;
        totalStormSeconds = remainingStormSeconds;

        if (!isDeathStormStopped) {
            if (!isDeathStormActive) {
                isDeathStormActive = true;
                startStorm();
            } else {
                bossbarProgress = 1.0;
                progressBar.setProgress(1.0);
            }

            Bukkit.getOnlinePlayers().forEach(p -> {
                p.playSound(p.getLocation(), "minecraft:custom.announce_corruptedstorm", 100000.0f, 1.0f);
                p.playSound(p.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 10000.0f, 0.1f);
            });

            int hours = totalAdded / 3600;
            int minutes = (totalAdded % 3600) / 60;
            int seconds = totalAdded % 60;
            String formattedTime = String.format("%02dʜ:%02dᴍ:%02ds", hours, minutes, seconds);

            showDeathStormTitle(formattedTime);
        }
        saveStormDataAsync();
    }

    private void showDeathStormTitle(String formattedTime) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(
                    ChatColor.of("#c37ff0") + "\uE085", // título unicode del font personalizado
                    ChatColor.of("#c37ff0") + "ᴅᴜʀᴀᴄɪóɴ: " + ChatColor.of("#a153d5") + formattedTime,
                    15, // fadeIn
                    120, // stay (6 segundos)
                    15 // fadeOut
            );
        }
    }


    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!event.toWeatherState() && isDeathStormActive) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (isDeathStormActive && !hiddenPlayers.contains(p.getUniqueId())) {
            progressBar.addPlayer(p);
            timeBar.addPlayer(p);
            progressBar.setVisible(true);
            timeBar.setVisible(true);
            refreshBossbarOrder();
        } else if (isDeathStormActive && hiddenPlayers.contains(p.getUniqueId())) {
            sendHiddenWarning(p);
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

        if (stormTask != null && !stormTask.isCancelled()) stormTask.cancel();

        world.setStorm(true);
        world.setThundering(true);

        // Activar las bossbars
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!hiddenPlayers.contains(p.getUniqueId())) {
                progressBar.addPlayer(p);
                timeBar.addPlayer(p);
            } else sendHiddenWarning(p);
        }
        timeBar.setVisible(true);
        progressBar.setVisible(true);
        refreshBossbarOrder();

        stormTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (remainingStormSeconds <= 0) {
                    cancel();
                    endStorm(world);
                    return;
                }

                // Calcular tiempo
                String timeFormat = getFormattedTime(remainingStormSeconds);
                timeBar.setTitle(ChatColor.of("#d37af0") + timeFormat);

                // Progreso (siempre relativo)
                bossbarProgress = Math.min(1.0, Math.max(0.0,
                        (double) remainingStormSeconds / (double) totalStormSeconds));
                progressBar.setProgress(bossbarProgress);

                remainingStormSeconds--;
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
            }
        }.runTaskTimerAsynchronously(plugin, 0, 20 * 60 * 5);
    }

    private void endStorm(World world) {
        isDeathStormActive = false;
        world.setStorm(false);
        world.setThundering(false);
        recentlyStruckChunks.clear();

        // Ocultar bossbars
        progressBar.setVisible(false);
        timeBar.setVisible(false);

        deathsToday.clear();
        saveStormDataAsync();
    }

    private int getTotalStormSeconds() {
        return Math.max(1, (int) (remainingStormSeconds / bossbarProgress));
    }

    private String getFormattedTime(int seconds) {
        return timeFormatCache.computeIfAbsent(seconds, s -> {
            int hours = s / 3600;
            int minutes = (s % 3600) / 60;
            int secs = s % 60;
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        });
    }

    private void refreshBossbarOrder() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            progressBar.removePlayer(p);
            timeBar.removePlayer(p);
            progressBar.addPlayer(p);
            timeBar.addPlayer(p);
        }
    }

    private void spawnRandomLightning(World world) {
        int currentDay = dayHandler.getCurrentDay();

        int minStrikes, maxStrikes;
        long intervalTicks;
        // ... (Tu lógica de if/else de días se mantiene igual) ...
        if (currentDay >= 20) {
            minStrikes = 6; maxStrikes = 15; intervalTicks = 100L;
        } else if (currentDay >= 15) {
            minStrikes = 4; maxStrikes = 7; intervalTicks = 400L;
        } else {
            minStrikes = 1; maxStrikes = 1; intervalTicks = 2500L;
        }

        int lightningCount = random.nextInt(maxStrikes - minStrikes + 1) + minStrikes;

        // Recopilación de datos en Hilo Principal (SEGURO)
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) return;

        // OPTIMIZACIÓN: Pre-calcular TODOS los chunks de jugadores UNA VEZ
        Set<Chunk> allPlayerChunks = new HashSet<>();
        Map<Location, Integer> chunkPlayerMap = new HashMap<>();

        for (Player player : players) {
            Chunk c = player.getLocation().getChunk();
            allPlayerChunks.add(c); // Guardamos para la validación posterior

            Location chunkCenter = c.getBlock(8, 0, 8).getLocation();
            chunkPlayerMap.merge(chunkCenter, 1, Integer::sum);
        }

        if (currentDay < 20) {
            chunkPlayerMap.entrySet().removeIf(entry -> entry.getValue() > 1);
        }

        if (chunkPlayerMap.isEmpty()) return;

        // Lógica de Rayos
        new BukkitRunnable() {
            int strikesRemaining = lightningCount;
            // Convertimos a lista para acceso aleatorio o usamos iterador
            final List<Location> targets = new ArrayList<>(chunkPlayerMap.keySet());

            @Override
            public void run() {
                if (strikesRemaining <= 0 || !isDeathStormActive || targets.isEmpty()) {
                    cancel();
                    return;
                }

                // Selección aleatoria de objetivo para que no sea siempre en orden
                Location chunkCenter = targets.get(random.nextInt(targets.size()));

                // Pasamos el set pre-calculado 'allPlayerChunks'
                spawnLightning(world, chunkCenter, currentDay >= 20, allPlayerChunks);

                strikesRemaining--;
            }
        }.runTaskTimer(plugin, 4, intervalTicks);
    }

    // Añadir el parámetro playerChunksCache
    private void spawnLightning(World world, Location location, boolean afterDay20, Set<Chunk> playerChunksCache) {
        // Pasamos el caché aquí
        Location lightningLocation = findSafeLightningLocationOptimized(world, location, afterDay20, 20, playerChunksCache);
        // He bajado maxAttempts a 20. 100 es excesivo y causa lag si no encuentra sitio.

        Chunk targetChunk = lightningLocation.getChunk();
        int currentDay = dayHandler.getCurrentDay();

        recentlyStruckChunks.add(targetChunk);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            recentlyStruckChunks.remove(targetChunk);
        }, CHUNK_COOLDOWN / 50);

        world.strikeLightningEffect(lightningLocation);

        if (currentDay >= 15) {
            Block block = world.getBlockAt(lightningLocation);
            if (block.getType() != Material.OBSIDIAN && block.getType() != Material.BEDROCK) {
                block.setType(Material.AIR);
            }
        }
    }

    // Método optimizado
    private Location findSafeLightningLocationOptimized(World world, Location center, boolean afterDay20, int maxAttempts, Set<Chunk> playerChunksCache) {
        for (int attempts = 0; attempts < maxAttempts; attempts++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * (13 * 16);

            double x = center.getX() + Math.cos(angle) * distance;
            double z = center.getZ() + Math.sin(angle) * distance;

            // Usar getHighestBlockYAt puede cargar chunks.
            // Verificamos si el chunk está cargado antes de llamar a getHighestBlockYAt para evitar lag de I/O.
            int checkX = (int) x;
            int checkZ = (int) z;

            if (world.isChunkLoaded(checkX >> 4, checkZ >> 4)) {
                Location loc = new Location(world, x, 0, z);
                loc.setY(world.getHighestBlockYAt(loc));

                if (!afterDay20) {
                    Chunk chunk = loc.getChunk();
                    // Usamos el caché en lugar de recalcular
                    if (!playerChunksCache.contains(chunk)) {
                        return loc;
                    }
                } else {
                    return loc;
                }
            }
        }
        return center;
    }

    // Métodos públicos mantienen la misma interfaz
    public void resetStorm() {
        synchronized (this) {
            remainingStormSeconds = 0;
            isDeathStormActive = false;
        }
        recentlyStruckChunks.clear();
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
            }
        }
        saveStormDataAsync();
    }

    public void loadStormData() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(plugin.getDataFolder(), "DayandStorm.yml");
            if (file.exists()) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                int loadedSeconds = config.getInt("TormentaRestante", 0);
                double loadedProgress = config.getDouble("BossbarProgress", 1.0);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    remainingStormSeconds = loadedSeconds;
                    bossbarProgress = loadedProgress;
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
            config.set("BossbarProgress", bossbarProgress);
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving storm: " + e.getMessage());
        }
    }
}