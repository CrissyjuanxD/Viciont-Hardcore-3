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

import org.bukkit.event.player.PlayerBedEnterEvent;

public class DeathStormHandler implements Listener {
    private final JavaPlugin plugin;
    private final DayHandler dayHandler;
    private final DamageLogListener damageLogListener;
    private final Map<UUID, Integer> deathCount = new HashMap<>();
    private int remainingStormSeconds = 0;
    private boolean isDeathStormActive = false;
    private final Random random = new Random();
    private BukkitRunnable stormTask;
    private final Set<Chunk> recentlyStruckChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final long CHUNK_COOLDOWN = 5000; // 5 segundos
    private final Map<UUID, Long> lastLightningCheck = new HashMap<>();
    private final Set<UUID> pausedActionBars = new HashSet<>();

    private static final net.md_5.bungee.api.ChatColor COLOR_PRIMARIO = net.md_5.bungee.api.ChatColor.of("#9179D4");
    private static final net.md_5.bungee.api.ChatColor COLOR_TIEMPO = net.md_5.bungee.api.ChatColor.of("#7700A0");
    private static final net.md_5.bungee.api.ChatColor COLOR_GRIS = net.md_5.bungee.api.ChatColor.GRAY;

    public DeathStormHandler(JavaPlugin plugin, DayHandler dayHandler) {
        this.plugin = plugin;
        this.dayHandler = dayHandler;
        this.damageLogListener = new DamageLogListener(plugin, this);
        loadStormData();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUniqueId();
        deathCount.put(playerUUID, deathCount.getOrDefault(playerUUID, 0) + 1);

        int currentDay = dayHandler.getCurrentDay();
        int increment;

        if (currentDay >= 15 && currentDay < 20) {
            increment = (currentDay - 14);
        } else if (currentDay >= 20) {
            increment = (currentDay - 19);
        } else {
            increment = currentDay;
        }

        remainingStormSeconds += 3600 * increment;
        isDeathStormActive = true;

        startStorm();
        saveStormData();
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!event.toWeatherState()) {
            if (isDeathStormActive) {
                event.setCancelled(true);
            }
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

            mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, effectDuration, 0, false, true));
            mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, effectDuration, 0, false, true));
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
            @Override
            public void run() {
                if (remainingStormSeconds <= 0) {
                    cancel();
                    isDeathStormActive = false;
                    world.setStorm(false);
                    world.setThundering(false);
                    recentlyStruckChunks.clear();
                    return;
                }

                int hours = remainingStormSeconds / 3600;
                int minutes = (remainingStormSeconds % 3600) / 60;
                int seconds = remainingStormSeconds % 60;

                TextComponent message = new TextComponent();
                TextComponent quedanText = new TextComponent("Quedan ");
                quedanText.setColor(COLOR_PRIMARIO);
                message.addExtra(quedanText);

                TextComponent clockSymbol = new TextComponent("⌚ ");
                clockSymbol.setColor(COLOR_GRIS);
                message.addExtra(clockSymbol);
                TextComponent timeComponent = new TextComponent(
                        String.format("%02d:%02d:%02d", hours, minutes, seconds)
                );
                timeComponent.setColor(COLOR_TIEMPO);
                timeComponent.setBold(true);
                timeComponent.setUnderlined(true);
                message.addExtra(timeComponent);
                TextComponent stormMessage = new TextComponent(" horas de DeathStorm");
                stormMessage.setColor(COLOR_PRIMARIO);
                message.addExtra(stormMessage);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (shouldShowDeathStormActionBar(player)) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
                    }
                }

                remainingStormSeconds--;
                spawnRandomLightning(world);
            }
        };

        stormTask.runTaskTimer(plugin, 0, 20);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isDeathStormActive) {
                    cancel();
                    return;
                }

                int currentDay = dayHandler.getCurrentDay();
                if (currentDay >= 15) {
                    checkPlayerLightning();
                }
            }
        }.runTaskTimer(plugin, 0, 20 * 60 * 5);
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
        Collection<Player> players = world.getPlayers();

        if (players.isEmpty()) return;

        Map<Location, Integer> chunkPlayerMap = new HashMap<>();
        for (Player player : players) {
            Location chunkCenter = player.getLocation().getChunk().getBlock(8, 0, 8).getLocation();
            chunkPlayerMap.put(chunkCenter, chunkPlayerMap.getOrDefault(chunkCenter, 0) + 1);
        }

        if (currentDay < 20) {
            chunkPlayerMap.entrySet().removeIf(entry -> entry.getValue() > 1);
        }

        new BukkitRunnable() {
            int strikesRemaining = lightningCount;

            @Override
            public void run() {
                if (strikesRemaining <= 0 || !isDeathStormActive) {
                    cancel();
                    return;
                }

                for (Location chunkCenter : chunkPlayerMap.keySet()) {
                    spawnLightning(world, chunkCenter, currentDay >= 20);
                    break;
                }

                strikesRemaining--;
            }
        }.runTaskTimer(plugin, 4, intervalTicks);
    }

    private void spawnLightning(World world, Location location, boolean afterDay20) {
        Location lightningLocation = findSafeLightningLocation(world, location, afterDay20, 100);
        Chunk targetChunk = lightningLocation.getChunk();
        int currentDay = dayHandler.getCurrentDay();

        recentlyStruckChunks.add(targetChunk);
        new BukkitRunnable() {
            @Override
            public void run() {
                recentlyStruckChunks.remove(targetChunk);
            }
        }.runTaskLater(plugin, CHUNK_COOLDOWN / 50);

        // Siempre efecto visual
        world.strikeLightningEffect(lightningLocation);

        // Solo romper bloques a partir del día 15 (excepto obsidiana y bedrock)
        if (currentDay >= 15) {

            Block block = world.getBlockAt(lightningLocation);

            if (block.getType() != Material.OBSIDIAN && block.getType() != Material.BEDROCK) {
                block.breakNaturally(); // Rompe el bloque dejando drops
            }
        }
    }

    private Location findSafeLightningLocation(World world, Location center, boolean afterDay20, int maxAttempts) {
        int attempts = 0;
        while (attempts < maxAttempts) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * (13 * 16);

            double x = center.getX() + Math.cos(angle) * distance;
            double z = center.getZ() + Math.sin(angle) * distance;

            Location loc = new Location(world, x, 0, z);
            loc.setY(world.getHighestBlockYAt(loc));

            if (!afterDay20) {
                Chunk chunk = loc.getChunk();
                boolean hasPlayers = world.getPlayers().stream()
                        .anyMatch(p -> p.getLocation().getChunk().equals(chunk));
                if (!hasPlayers) {
                    return loc;
                }
            } else {
                return loc;
            }

            attempts++;
        }
        return center;
    }

    private void checkPlayerLightning() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            long now = System.currentTimeMillis();

            // Verificar si ya se chequeó a este jugador recientemente
            if (lastLightningCheck.getOrDefault(playerId, 0L) + (5 * 60 * 1000) > now) {
                continue;
            }

            lastLightningCheck.put(playerId, now);

            // 50% de probabilidad
            if (random.nextBoolean()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.getWorld().strikeLightning(player.getLocation());

                        player.sendMessage(ChatColor.RED + "¡Un rayo te ha alcanzado!");
                    }
                });
            }
        }
    }

    public void resetStorm() {
        remainingStormSeconds = 0;
        isDeathStormActive = false;
        recentlyStruckChunks.clear();
        saveStormData();
    }

    public void addStormSeconds(int seconds) {
        remainingStormSeconds += seconds;
        if (!isDeathStormActive) {
            isDeathStormActive = true;
            startStorm();
        }
        saveStormData();
    }

    public void removeStormSeconds(int seconds) {
        remainingStormSeconds = Math.max(remainingStormSeconds - seconds, 0);
        if (remainingStormSeconds == 0) {
            isDeathStormActive = false;
            recentlyStruckChunks.clear();
        }
        saveStormData();
    }

    public void pauseActionBarForPlayer(UUID playerId) {
        pausedActionBars.add(playerId);
    }

    public void resumeActionBarForPlayer(UUID playerId) {
        pausedActionBars.remove(playerId);
    }

    public boolean isActionBarPausedForPlayer(UUID playerId) {
        return pausedActionBars.contains(playerId);
    }

    private boolean shouldShowDeathStormActionBar(Player player) {
        UUID playerId = player.getUniqueId();

        if (isActionBarPausedForPlayer(playerId)) {
            return false;
        }

        if (MuerteHandler.isDeathMessageActive()) {
            return false;
        }

        if (damageLogListener != null && damageLogListener.isPlayerInDamageLog(playerId)) {
            return false;
        }

        return true;
    }

    public void loadStormData() {
        File file = new File(plugin.getDataFolder(), "DayandStorm.yml");
        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            remainingStormSeconds = config.getInt("TormentaRestante", 0);

            Bukkit.getLogger().info("Storm data loaded: " + remainingStormSeconds + " seconds remaining.");
            if (remainingStormSeconds > 0) {
                isDeathStormActive = true;
                startStorm();
            }
        }
    }

    public void saveStormData() {
        try {
            File file = new File(plugin.getDataFolder(), "DayandStorm.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            config.set("TormentaRestante", remainingStormSeconds);

            config.save(file);
            Bukkit.getLogger().info("Storm data saved: " + remainingStormSeconds + " seconds remaining.");
        } catch (IOException e) {
            Bukkit.getLogger().severe("Error saving storm. " + e.getMessage());
        }
    }
}