package Dificultades;

import Dificultades.Features.PassiveMobAggression;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Biome;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
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
    private boolean isApplied = false;
    private final Random random = new Random();
    private final Map<UUID, Long> lastDayEffectTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastNightEffectTime = new ConcurrentHashMap<>();
    private final long interval = 120_000; // 2 minutos en milisegundos
    private final int MAX_HOSTILE_MOBS = 100; // Límite de mobs hostiles en el bioma
    private final int SPAWN_RADIUS = 150; // Radio en el que pueden aparecer los mobs
    private final PassiveMobAggression passiveMobAggression;

    public DaySixChanges(JavaPlugin plugin) {
        this.plugin = plugin;
        this.passiveMobAggression = new PassiveMobAggression(plugin);
    }

    public void apply() {
        if (!isApplied) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            isApplied = true;
            startMushroomIslandSpawner();
            startDayNightEffectsTask();
            passiveMobAggression.apply();

        }
    }

    public void revert() {
        if (isApplied) {
            isApplied = false;
            passiveMobAggression.revert();
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

                    // Verificar si el jugador está al aire
                    boolean underSky = isUnderOpenSky(player, player.getLocation());

                    // Verificar si es de día o de noche
                    boolean isDay = isDaytime(player.getWorld());
                    boolean isNight = !isDay;

                    // Durante el día: 5% de probabilidad de quemarse si está bajo el sol
                    if (isDay && underSky) {
                        handleDayEffect(player, playerId, currentTime);
                    }

                    // Durante la noche: 1% de probabilidad de recibir Darkness si está bajo la luna
                    if (isNight && underSky) {
                        handleNightEffect(player, playerId, currentTime);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, interval); // Ejecutar cada 2 minutos
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isApplied) return;

        // Verificar si el jugador realmente se movió de bloque
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return; // El jugador no se movió de bloque, ignorar
        }

        Player player = event.getPlayer();
        Location location = player.getLocation();

        // Si el jugador camina sobre Soul Sand, se le aplica Slowness V
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
        if (lastDayEffectTime.getOrDefault(playerId, 0L) + interval <= currentTime) {
            if (random.nextInt(100) < 70) {
                player.setFireTicks(100); // Quemar al jugador durante 5 segundos (100 ticks)
                player.sendMessage(ChatColor.RED + "¡El sol te está quemando!");
                lastDayEffectTime.put(playerId, currentTime);
            }
        }
    }

    private void handleNightEffect(Player player, UUID playerId, long currentTime) {
        if (lastNightEffectTime.getOrDefault(playerId, 0L) + interval <= currentTime) {
            if (random.nextInt(100) < 70) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 2400, 0, false, true)); // 2 minutos (2400 ticks)
                player.sendMessage(ChatColor.DARK_PURPLE + "La oscuridad te envuelve...");
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

                    if (!hasMushroomPlayer) continue; // Si no hay jugadores en el bioma, no hacer nada

                    // Contar la cantidad actual de mobs hostiles en el bioma
                    long hostileMobCount = world.getEntities().stream()
                            .filter(entity -> entity instanceof Monster) // Solo contar mobs agresivos
                            .filter(entity -> entity.getLocation().getBlock().getBiome() == Biome.MUSHROOM_FIELDS)
                            .count();

                    if (hostileMobCount >= MAX_HOSTILE_MOBS) continue; // No spawnear si ya hay suficientes mobs

                    // Generar nuevos mobs en ubicaciones aleatorias dentro del bioma
                    for (Player player : world.getPlayers()) {
                        int mobsToSpawn = 5; // Número de mobs que pueden aparecer por ejecución
                        for (int i = 0; i < mobsToSpawn; i++) {
                            Location spawnLocation = getRandomSpawnLocation(player.getLocation(), world);
                            if (spawnLocation == null) continue; // Si no hay ubicación válida, ignorar

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
        }.runTaskTimer(plugin, 0L, 20L * 10L); // Ejecutar cada 10 segundos
    }

    // Obtener una ubicación aleatoria dentro del bioma y asegurarse de que sea sólida
    private Location getRandomSpawnLocation(Location center, World world) {
        for (int i = 0; i < 10; i++) { // Intentar encontrar un lugar válido hasta 10 veces
            double x = center.getX() + (random.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS);
            double z = center.getZ() + (random.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS);
            int y = world.getHighestBlockYAt((int) x, (int) z); // Obtener la altura más alta
            Location spawnLocation = new Location(world, x, y, z);

            // Verificar si la ubicación está dentro del bioma Mushroom Fields y el bloque es sólido
            if (spawnLocation.getBlock().getBiome() == Biome.MUSHROOM_FIELDS &&
                    spawnLocation.getBlock().getType().isSolid()) {
                return spawnLocation.add(0, 1, 0); // Ajustar para que el mob no spawnee dentro del suelo
            }
        }
        return null; // No encontró una ubicación válida
    }


}
