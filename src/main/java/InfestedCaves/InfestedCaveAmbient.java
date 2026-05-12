package InfestedCaves;

import Dificultades.CustomMobs.NULLEntity;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import vct.hardcore3.ViciontHardcore3;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class InfestedCaveAmbient implements Listener {

    private final JavaPlugin plugin;
    private final NULLEntity nullEntityCreator;

    private final Map<UUID, AmbientSession> sessions = new HashMap<>();

    // Cooldowns individuales
    private static final long COOLDOWN_COMMON_MIN = 2 * 60 * 1000;
    private static final long COOLDOWN_COMMON_MAX = 4 * 60 * 1000;
    private static final long COOLDOWN_CLOWN_MIN = 8 * 60 * 1000;
    private static final long COOLDOWN_CLOWN_MAX = 12 * 60 * 1000;
    private static final long COOLDOWN_DAISY = 30 * 60 * 1000;

    // Pausa GLOBAL fija
    private static final long GLOBAL_PAUSE_TIME = 60 * 1000;

    // Duraciones (Audio)
    private static final long DURATION_COMMON = (1 * 60 + 55) * 1000;
    private static final long DURATION_CLOWN_V1 = (1 * 60 + 51) * 1000;
    private static final long DURATION_CLOWN_V2 = (2 * 60 + 20) * 1000; // 2m 20s

    // Variantes Daisy
    private static final long DURATION_DAISY_V1 = (1 * 60 + 13) * 1000;
    private static final long DURATION_DAISY_V2 = (1 * 60 + 31) * 1000;

    // Duración para el Loop de fondo
    private static final long DURATION_LOOP = (1 * 60 + 59) * 1000;

    public InfestedCaveAmbient(JavaPlugin plugin) {
        this.plugin = plugin;
        this.nullEntityCreator = new NULLEntity(plugin);
        startAmbientLoop();
    }

    private void startAmbientLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.getWorld().getName().equals(ViciontHardcore3.WORLD_NAME)) continue;

                    AmbientSession session = sessions.computeIfAbsent(p.getUniqueId(), k -> new AmbientSession());

                    // 1. Loop de fondo
                    processBackgroundLoop(p, session);

                    // 2. Si hay evento activo, esperar
                    if (session.isEventActive()) continue;

                    // 3. Si hay pausa global, esperar
                    if (System.currentTimeMillis() < session.nextGlobalEventStart) continue;

                    // 4. Lógica de eventos
                    processAmbientLogic(p, session);
                }
            }
        }.runTaskTimer(plugin, 100L, 20L);
    }

    private void processBackgroundLoop(Player p, AmbientSession session) {
        long now = System.currentTimeMillis();
        if (now >= session.nextLoopTime) {
            p.playSound(p.getLocation(), "minecraft:custom.cueva_loop", SoundCategory.VOICE, 10.0f, 1.0f);
            session.nextLoopTime = now + DURATION_LOOP;
        }
    }

    private void processAmbientLogic(Player p, AmbientSession session) {
        long now = System.currentTimeMillis();
        boolean inSafeZone = isInSafeZone(p);

        List<EventType> candidates = new ArrayList<>();

        if (now >= session.nextCommon) candidates.add(EventType.COMMON);
        if (!inSafeZone && now >= session.nextClown) candidates.add(EventType.CLOWN);
        if (!inSafeZone && now >= session.nextDaisy) candidates.add(EventType.DAISY);

        if (candidates.isEmpty()) return;

        EventType selected = selectEventByTension(candidates, session);

        if (selected != null) {
            triggerEvent(p, session, selected);
        }
    }

    private EventType selectEventByTension(List<EventType> candidates, AmbientSession session) {
        int tension = session.tensionLevel;

        int weightCommon = 0;
        int weightClown = 0;
        int weightDaisy = 0;

        if (tension == 0) {
            weightCommon = 100;
        } else if (tension == 1) {
            weightCommon = 50;
            weightClown = 50;
        } else {
            weightCommon = 20;
            weightClown = 40;
            weightDaisy = 40;
        }

        if (!candidates.contains(EventType.COMMON)) weightCommon = 0;
        if (!candidates.contains(EventType.CLOWN)) weightClown = 0;
        if (!candidates.contains(EventType.DAISY)) weightDaisy = 0;

        int totalWeight = weightCommon + weightClown + weightDaisy;
        if (totalWeight == 0) return null;

        int random = ThreadLocalRandom.current().nextInt(totalWeight);

        if (random < weightCommon) return EventType.COMMON;
        if (random < weightCommon + weightClown) return EventType.CLOWN;
        return EventType.DAISY;
    }

    public void triggerEvent(Player p, AmbientSession session, EventType type) {
        long now = System.currentTimeMillis();
        long eventDuration = 0;

        switch (type) {
            case COMMON:
                eventDuration = DURATION_COMMON;
                long commonCd = ThreadLocalRandom.current().nextLong(COOLDOWN_COMMON_MIN, COOLDOWN_COMMON_MAX);
                session.nextCommon = now + eventDuration + commonCd;
                session.tensionLevel++;
                startCommonEvent(p);
                break;

            case CLOWN:
                // --- LÓGICA DE VARIANTE CLOWN (50/50) ---
                boolean isClownV2 = ThreadLocalRandom.current().nextBoolean();

                if (isClownV2) {
                    eventDuration = DURATION_CLOWN_V2;
                    startClownEventV2(p, session);
                } else {
                    eventDuration = DURATION_CLOWN_V1;
                    startClownEventV1(p, session);
                }

                long clownCd = ThreadLocalRandom.current().nextLong(COOLDOWN_CLOWN_MIN, COOLDOWN_CLOWN_MAX);
                session.nextClown = now + eventDuration + clownCd;
                session.tensionLevel = 0;
                break;

            case DAISY:
                boolean isVariant2 = ThreadLocalRandom.current().nextDouble() < 0.65;
                if (isVariant2) {
                    eventDuration = DURATION_DAISY_V2;
                    startDaisyEventV2(p, session);
                } else {
                    eventDuration = DURATION_DAISY_V1;
                    startDaisyEventV1(p, session);
                }
                session.nextDaisy = now + eventDuration + COOLDOWN_DAISY;
                session.tensionLevel = 0;
                break;
        }

        session.eventEndTime = now + eventDuration;
        session.nextGlobalEventStart = session.eventEndTime + GLOBAL_PAUSE_TIME;
    }

    // --- EVENTOS ---

    private void startCommonEvent(Player p) {
        p.playSound(p.getLocation(), "minecraft:custom.ambient_infestedcave", SoundCategory.VOICE, 10.0f, 1.0f);
    }

    // CLOWN V1 (Original)
    private void startClownEventV1(Player p, AmbientSession session) {
        p.playSound(p.getLocation(), "minecraft:custom.clown-ambient-ic", SoundCategory.VOICE, 10.0f, 1.0f);
        int[] spawnTimes = {10, 30, 60, 90, 110};

        BukkitTask task = new BukkitRunnable() {
            int second = 0;
            int spawnIndex = 0;
            @Override
            public void run() {
                if (!p.isOnline() || !p.getWorld().getName().equals(ViciontHardcore3.WORLD_NAME)) {
                    this.cancel(); return;
                }
                if (spawnIndex < spawnTimes.length && second == spawnTimes[spawnIndex]) {
                    spawnNullMobStalker(p, 18, 22, false); // isAggressive = false
                    spawnIndex++;
                }
                second++;
                if (second > 115) this.cancel();
            }
        }.runTaskTimer(plugin, 0L, 20L);
        session.currentTask = task;
    }

    // CLOWN V2 (Nuevo: Agresivo, 6 spawns)
    private void startClownEventV2(Player p, AmbientSession session) {
        p.playSound(p.getLocation(), "minecraft:custom.clown-ambient-ic2", SoundCategory.VOICE, 10.0f, 1.0f);
        // 6 spawns distribuidos en 2m 20s (140s)
        int[] spawnTimes = {15, 35, 55, 75, 95, 120};

        BukkitTask task = new BukkitRunnable() {
            int second = 0;
            int spawnIndex = 0;
            @Override
            public void run() {
                if (!p.isOnline() || !p.getWorld().getName().equals(ViciontHardcore3.WORLD_NAME)) {
                    this.cancel(); return;
                }
                if (spawnIndex < spawnTimes.length && second == spawnTimes[spawnIndex]) {
                    // isAggressive = true (Permite el Stare Attack)
                    spawnNullMobStalker(p, 18, 22, true);
                    spawnIndex++;
                }
                second++;
                if (second > 145) this.cancel();
            }
        }.runTaskTimer(plugin, 0L, 20L);
        session.currentTask = task;
    }

    // DAISY V1
    private void startDaisyEventV1(Player p, AmbientSession session) {
        p.playSound(p.getLocation(), "minecraft:custom.daisy-ambient", SoundCategory.VOICE, 10.0f, 1.0f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 70 * 20, 1));

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (p.isOnline() && p.getWorld().getName().equals(ViciontHardcore3.WORLD_NAME)) {
                    spawnNullMobJumpscare(p);
                }
            }
        }.runTaskLater(plugin, 70 * 20L);
        session.currentTask = task;
    }

    // DAISY V2
    private void startDaisyEventV2(Player p, AmbientSession session) {
        p.playSound(p.getLocation(), "minecraft:custom.daisy-ambient2", SoundCategory.VOICE, 10.0f, 1.0f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 88 * 20, 1));

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (p.isOnline() && p.getWorld().getName().equals(ViciontHardcore3.WORLD_NAME)) {
                    spawnNullMobJumpscare(p);
                    p.playEffect(EntityEffect.TOTEM_RESURRECT);
                }
            }
        }.runTaskLater(plugin, 88 * 20L);
        session.currentTask = task;
    }

    // --- SPAWN LOGIC ---

    private void spawnNullMobJumpscare(Player p) {
        Location pLoc = p.getLocation();
        Vector direction = pLoc.getDirection().normalize().multiply(4);
        Location spawnLoc = pLoc.add(direction);
        spawnLoc.setY(p.getLocation().getY());
        spawnLoc.setYaw(pLoc.getYaw() + 180);

        if (spawnLoc.getBlock().getType().isSolid()) {
            spawnLoc = p.getLocation().add(p.getLocation().getDirection().normalize().multiply(1));
            spawnLoc.setY(p.getLocation().getY());
            spawnLoc.setYaw(pLoc.getYaw() + 180);
        }
        nullEntityCreator.spawn(p, spawnLoc, true, false);
    }

    private void spawnNullMobStalker(Player p, int minRadius, int maxRadius, boolean isAggressive) {
        Location spawnLoc = findSafeSpawnLocation(p.getLocation(), minRadius, maxRadius);
        if (spawnLoc != null) {
            Location pLoc = p.getLocation();
            Vector direction = pLoc.toVector().subtract(spawnLoc.toVector()).normalize();
            spawnLoc.setDirection(direction);

            // Pasamos isAggressive al spawner
            nullEntityCreator.spawn(p, spawnLoc, false, isAggressive);
        }
    }

    private Location findSafeSpawnLocation(Location center, int minRadius, int maxRadius) {
        World world = center.getWorld();
        Random random = ThreadLocalRandom.current();
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = minRadius + random.nextDouble() * (maxRadius - minRadius);
            int x = (int) (center.getX() + Math.cos(angle) * distance);
            int z = (int) (center.getZ() + Math.sin(angle) * distance);
            int startY = center.getBlockY();

            for (int y = startY - 5; y <= startY + 5; y++) {
                if (y < -64 || y > 319) continue;
                if (world.getBlockAt(x, y - 1, z).getType().isSolid() &&
                        !world.getBlockAt(x, y, z).getType().isSolid() &&
                        !world.getBlockAt(x, y + 1, z).getType().isSolid()) {
                    return new Location(world, x + 0.5, y, z + 0.5);
                }
            }
        }
        return null;
    }

    // --- UTILS ---

    private boolean isInSafeZone(Player p) {
        return p.getLocation().distance(new Location(p.getWorld(), 0, 0, 0)) < 200;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { cleanSession(e.getPlayer().getUniqueId()); }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        if (e.getFrom().getName().equals(ViciontHardcore3.WORLD_NAME)) {
            cleanSession(e.getPlayer().getUniqueId());
        }
    }

    private void cleanSession(UUID uuid) {
        AmbientSession session = sessions.remove(uuid);
        if (session != null && session.currentTask != null) session.currentTask.cancel();
    }

    public void forceStartEvent(Player p, EventType type) {
        AmbientSession session = sessions.computeIfAbsent(p.getUniqueId(), k -> new AmbientSession());
        if (session.currentTask != null) session.currentTask.cancel();
        triggerEvent(p, session, type);
    }

    private static class AmbientSession {
        long nextCommon = 0;
        long nextClown = 0;
        long nextDaisy = 0;
        long nextLoopTime = 0;
        long eventEndTime = 0;
        long nextGlobalEventStart = 0;
        int tensionLevel = 0;
        BukkitTask currentTask;

        boolean isEventActive() {
            return System.currentTimeMillis() < eventEndTime;
        }
    }

    public enum EventType {
        COMMON, CLOWN, DAISY
    }
}