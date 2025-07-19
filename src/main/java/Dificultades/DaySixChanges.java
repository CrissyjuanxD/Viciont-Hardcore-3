package Dificultades;

/*import Dificultades.Features.PassiveMobAggression;*/
import Handlers.DayHandler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Biome;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DaySixChanges implements Listener {

    private final JavaPlugin plugin;
    private final DayHandler dayHandler;
    private boolean isApplied = false;
    private final Random random = new Random();
    private final Map<UUID, Long> lastDayEffectTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastNightEffectTime = new ConcurrentHashMap<>();
    private long interval = 120_000;
    private final int MAX_HOSTILE_MOBS = 100;
    private final int SPAWN_RADIUS = 150;
    /*private final PassiveMobAggression passiveMobAggression;*/


    public DaySixChanges(JavaPlugin plugin, DayHandler handler) {
        this.plugin = plugin;
        this.dayHandler = handler;
        /*this.passiveMobAggression = new PassiveMobAggression(plugin);*/
    }

    public void apply() {
        if (!isApplied) {
            isApplied = true;
            Bukkit.getPluginManager().registerEvents(this, plugin);
            startMushroomIslandSpawner();
            startDayNightEffectsTask();
            /*passiveMobAggression.apply();*/

        }
    }

    public void revert() {
        if (isApplied) {
            isApplied = false;
            /*passiveMobAggression.revert();*/
            HandlerList.unregisterAll(this);

        }
    }

    private void startDayNightEffectsTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isApplied) {
                    cancel();
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID playerId = player.getUniqueId();
                    long currentTime = System.currentTimeMillis();

                    boolean underSky = isUnderOpenSky(player, player.getLocation());

                    boolean isDay = isDaytime(player.getWorld());
                    boolean isNight = !isDay;

                    if (isDay && underSky) {
                        handleDayEffect(player, playerId, currentTime);
                    }

                    if (isNight && underSky) {
                        handleNightEffect(player, playerId, currentTime);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isApplied) return;

        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Location location = player.getLocation();

        //aplica Slowness V
        handleSoulSandEffect(player, location);
    }

    private boolean isUnderOpenSky(Player player, Location location) {
        int highest = player.getWorld().getHighestBlockYAt(location);
        return location.getBlockY() >= highest - 1;
    }

    private boolean isDaytime(World world) {
        long time = world.getTime();
        return time >= 0 && time < 12300;
    }

    private void handleDayEffect(Player player, UUID playerId, long currentTime) {
        if (player.getWorld().getEnvironment() != World.Environment.NORMAL) return;

        // Obtener el día actual
        int currentDay = dayHandler.getCurrentDay();

        // Calcular el intervalo basado en el día
        long currentInterval = interval;
        if (currentDay >= 13) {
            currentInterval = interval / 2;
        }

        if (lastDayEffectTime.getOrDefault(playerId, 0L) + currentInterval <= currentTime) {
            int probability;
            if (currentDay >= 13) {
                probability = 70;
            } else {
                probability = 30;
            }

            if (random.nextInt(100) < probability) {
                player.setFireTicks(Integer.MAX_VALUE);
                player.sendMessage(ChatColor.RED + "۞ El sol te está quemando");
                lastDayEffectTime.put(playerId, currentTime);
            }
        }
    }

    private void handleNightEffect(Player player, UUID playerId, long currentTime) {
        if (player.getWorld().getEnvironment() != World.Environment.NORMAL) return;
        if (lastNightEffectTime.getOrDefault(playerId, 0L) + interval <= currentTime) {
            if (random.nextInt(100) < 70) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 2400, 0, false, true)); // 2 minutos (2400 ticks)
                player.sendMessage(ChatColor.DARK_PURPLE + "۞ La oscuridad te envuelve...");
                lastNightEffectTime.put(playerId, currentTime);
            }
        }
    }

    private void handleSoulSandEffect(Player player, Location location) {
        Block blockUnder = location.clone().subtract(0, 1, 0).getBlock();
        if (blockUnder.getType() == org.bukkit.Material.SOUL_SAND) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4, false, false));
        }
    }


    private void startMushroomIslandSpawner() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isApplied) {
                    cancel();
                    return;
                }

                for (World world : Bukkit.getWorlds()) {
                    // Verificar si hay jugadores en el bioma Mushroom Fields
                    boolean hasMushroomPlayer = world.getPlayers().stream()
                            .anyMatch(player -> player.getLocation().getBlock().getBiome() == Biome.MUSHROOM_FIELDS);

                    if (!hasMushroomPlayer) continue;

                    // Contar la cantidad actual de mobs hostiles en el bioma
                    long hostileMobCount = world.getEntities().stream()
                            .filter(entity -> entity instanceof Monster)
                            .filter(entity -> entity.getLocation().getBlock().getBiome() == Biome.MUSHROOM_FIELDS)
                            .count();

                    if (hostileMobCount >= MAX_HOSTILE_MOBS) continue;

                    // Generar nuevos mobs en ubicaciones aleatorias dentro del bioma
                    for (Player player : world.getPlayers()) {
                        int mobsToSpawn = 5;
                        for (int i = 0; i < mobsToSpawn; i++) {
                            Location spawnLocation = getRandomSpawnLocation(player.getLocation(), world);
                            if (spawnLocation == null) continue;

                            // Lista de mobs que pueden spawnear
                            EntityType[] hostileTypes = {
                                    EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER,
                                    EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.ENDERMAN,
                                    EntityType.WITCH, EntityType.PILLAGER, EntityType.VINDICATOR,
                                    EntityType.HUSK, EntityType.STRAY, EntityType.DROWNED,
                                    EntityType.COW, EntityType.PIG, EntityType.SHEEP,
                                    EntityType.CHICKEN, EntityType.SLIME, EntityType.ILLUSIONER,
                                    EntityType.PHANTOM, EntityType.RAVAGER
                            };

                            EntityType type = hostileTypes[random.nextInt(hostileTypes.length)];
                            world.spawnEntity(spawnLocation, type);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * 10L);
    }

    // Obtener una ubicación aleatoria dentro del bioma y asegurarse de que sea sólida
    private Location getRandomSpawnLocation(Location center, World world) {
        for (int i = 0; i < 10; i++) {
            double x = center.getX() + (random.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS);
            double z = center.getZ() + (random.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS);
            int y = world.getHighestBlockYAt((int) x, (int) z);
            Location spawnLocation = new Location(world, x, y, z);

            // Verificar si la ubicación está dentro del bioma Mushroom Fields y el bloque es sólido
            if (spawnLocation.getBlock().getBiome() == Biome.MUSHROOM_FIELDS &&
                    spawnLocation.getBlock().getType().isSolid()) {
                return spawnLocation.add(0, 1, 0);
            }
        }
        return null;
    }
}
