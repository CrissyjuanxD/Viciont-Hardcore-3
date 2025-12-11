package Dificultades;

import Dificultades.CustomMobs.Bombita;
import Dificultades.CustomMobs.CorruptedSpider;
import Dificultades.CustomMobs.CorruptedZombies;
import Dificultades.CustomMobs.Iceologer;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.raid.RaidSpawnWaveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class DayTwoChanges implements Listener {
    private final JavaPlugin plugin;
    private final Random random = new Random();
    private boolean isApplied = false;

    private final Bombita bombitaSpawner;
    private final Iceologer iceologerSpawner;
    private final CorruptedZombies corruptedZombies;
    private final CorruptedSpider corruptedSpiders;

    // Control de mobs activos en la raid
    private final Map<LivingEntity, Long> trackedMobs = new HashMap<>();
    // Variable para controlar la tarea repetitiva
    private BukkitTask targetTask;

    public DayTwoChanges(JavaPlugin plugin) {
        this.plugin = plugin;
        // Creamos las instancias (que a su vez configuran sus sistemas estáticos)
        this.bombitaSpawner = new Bombita(plugin);
        this.iceologerSpawner = new Iceologer(plugin);
        this.corruptedZombies = new CorruptedZombies(plugin);
        this.corruptedSpiders = new CorruptedSpider(plugin);
    }

    public void apply() {
        if (!isApplied) {
            Bukkit.getPluginManager().registerEvents(this, plugin);

            // Aseguramos que los sistemas estáticos de los mobs estén activos
            bombitaSpawner.apply();
            iceologerSpawner.apply();
            // Nota: Zombies y Spiders suelen activarse en el Día 1, pero no hace daño asegurar.
            corruptedZombies.apply();
            corruptedSpiders.apply();

            isApplied = true;

            // Iniciamos la tarea de IA de tracking
            startTargetTask();
        }
    }

    public void revert() {
        if (isApplied) {
            bombitaSpawner.revert();
            iceologerSpawner.revert();

            // DETENER la tarea de tracking para evitar lag fantasma
            if (targetTask != null && !targetTask.isCancelled()) {
                targetTask.cancel();
                targetTask = null;
            }

            trackedMobs.clear();
            HandlerList.unregisterAll(this);
            isApplied = false;
        }
    }

    private void startTargetTask() {
        // Ejecutar cada segundo (20 ticks)
        targetTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (trackedMobs.isEmpty()) return;
                updateTargets();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // Evento de Raid
    @EventHandler
    public void onRaidWaveSpawn(RaidSpawnWaveEvent event) {
        if (!isApplied) return;

        int currentWave = event.getRaid().getSpawnedGroups();

        // Probabilidad del 10% de spawnear horda corrupta
        if (random.nextDouble() <= 0.10) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!isApplied) return; // Doble chequeo por si se desactivó el día

                sendRaidWarning(event);
                spawnCorruptedMobs(event, "corruptedzombie", 15);
                spawnCorruptedMobs(event, "corruptedspider", 5);
            }, 40L);
        }

        // Reemplazar Raiders con Bombitas
        for (Entity entity : event.getRaiders()) {
            if (entity instanceof Raider) {
                int replacedCount = 0;
                if (replacedCount < currentWave) {
                    Location spawnLocation = entity.getLocation();
                    Creeper bombita = bombitaSpawner.spawnBombita(spawnLocation);
                    trackedMobs.put(bombita, System.currentTimeMillis());
                    entity.remove(); // Removemos el original
                    replacedCount++;
                }
            }
        }

        // Iceologers a partir de la oleada 2
        if (currentWave >= 2) {
            int iceologerCount = random.nextInt(2) + 1;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!isApplied) return;

                int spawned = 0;
                for (Entity entity : event.getRaiders()) {
                    if (entity instanceof Pillager && entity.isValid() && spawned < iceologerCount) {
                        Location spawnLocation = entity.getLocation();
                        iceologerSpawner.spawnIceologer(spawnLocation);
                        entity.remove(); // Reemplazamos el pillager
                        spawned++;
                    }
                }
            }, 80L);
        }
    }

    // Actualiza los objetivos dinámicamente
    private void updateTargets() {
        Iterator<Map.Entry<LivingEntity, Long>> iterator = trackedMobs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LivingEntity, Long> entry = iterator.next();
            LivingEntity mob = entry.getKey();

            // Limpieza automática si el mob murió
            if (mob == null || !mob.isValid() || mob.isDead()) {
                iterator.remove();
                continue;
            }

            // Solo forzamos target si es un Mob con IA
            if (mob instanceof Mob activeMob) {
                LivingEntity currentTarget = activeMob.getTarget();

                // Si ya tiene un target vivo, no gastamos CPU buscando otro
                if (currentTarget != null && currentTarget.isValid() && !currentTarget.isDead()
                        && (currentTarget instanceof Player || currentTarget instanceof Villager)) {
                    continue;
                }

                LivingEntity newTarget = findTarget(mob);
                if (newTarget != null) {
                    activeMob.setTarget(newTarget);
                }
            }
        }
    }

    private LivingEntity findTarget(Entity mob) {
        World world = mob.getWorld();

        // 1. Prioridad: Jugadores (Búsqueda rápida)
        Player closestPlayer = null;
        double minDistanceSq = Double.MAX_VALUE;

        for (Player p : world.getPlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                double distSq = p.getLocation().distanceSquared(mob.getLocation());
                if (distSq < minDistanceSq && distSq <= 2500) { // Max 50 bloques
                    minDistanceSq = distSq;
                    closestPlayer = p;
                }
            }
        }

        if (closestPlayer != null) {
            return closestPlayer;
        }

        // 2. Secundaria: Aldeanos (Búsqueda lenta - solo si no hay jugadores)
        // getNearbyEntities puede ser pesado, lo usamos solo si es necesario
        return world.getNearbyEntities(mob.getLocation(), 50, 50, 50).stream()
                .filter(e -> e instanceof Villager && !e.isDead())
                .map(e -> (LivingEntity) e)
                .min(Comparator.comparingDouble(v -> v.getLocation().distanceSquared(mob.getLocation())))
                .orElse(null);
    }

    private void spawnCorruptedMobs(RaidSpawnWaveEvent event, String mobType, int count) {
        List<Location> spawnLocations = getSpawnLocations(event, count);

        for (Location location : spawnLocations) {
            LivingEntity corruptedMob = null;

            if (mobType.equals("corruptedzombie")) {
                corruptedMob = corruptedZombies.spawnCorruptedZombie(location);
            } else if (mobType.equals("corruptedspider")) {
                corruptedMob = corruptedSpiders.spawnCorruptedSpider(location);
            }

            if (corruptedMob != null) {
                trackedMobs.put(corruptedMob, System.currentTimeMillis());
            }
        }
    }

    // Obtener ubicaciones de spawn cerca de los Raiders
    private List<Location> getSpawnLocations(RaidSpawnWaveEvent event, int count) {
        List<Location> locations = new ArrayList<>();
        List<Entity> raiders = new ArrayList<>(event.getRaiders());

        if (raiders.isEmpty()) return locations;

        for (int i = 0; i < count; i++) {
            Entity raider = raiders.get(random.nextInt(raiders.size()));
            Location spawnLocation = raider.getLocation().clone();

            // Añade un pequeño desplazamiento aleatorio para dispersar los mobs
            spawnLocation.add(random.nextInt(6) - 3, 0, random.nextInt(6) - 3);

            // Validar que no spawnee en bloque solido
            int y = spawnLocation.getWorld().getHighestBlockYAt(spawnLocation);
            spawnLocation.setY(y + 1);

            locations.add(spawnLocation);
        }

        return locations;
    }

    private void sendRaidWarning(RaidSpawnWaveEvent event) {
        Sound sound = Sound.ENTITY_ENDER_DRAGON_GROWL;

        String jsonMessage = "[\"\",{\"text\":\"\\u06de\",\"bold\":true,\"color\":\"#C17CE5\"}," +
                "{\"text\":\" Ha aparecido una oleada de\",\"color\":\"#E28761\"}," +
                "{\"text\":\" Corrupted Mobs \",\"bold\":true,\"color\":\"dark_purple\"}," +
                "{\"text\":\"\\u26a0\",\"bold\":true,\"color\":\"dark_red\"}]";

        Location raidLoc = event.getRaid().getLocation();
        for (Player player : raidLoc.getWorld().getPlayers()) {
            if (raidLoc.distanceSquared(player.getLocation()) <= 10000) { // 100 bloques
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "tellraw " + player.getName() + " " + jsonMessage);

                player.playSound(player.getLocation(), sound, 2.0f, 0.1f);
            }
        }
    }
}