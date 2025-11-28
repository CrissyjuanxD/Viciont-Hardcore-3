package Dificultades.Features;

import Commands.TiempoCommand;
import Events.DamageLogListener;
import Handlers.DeathStormHandler;
import TitleListener.SuccessNotification;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class NightmareMechanic implements Listener {

    private final JavaPlugin plugin;
    private final TiempoCommand tiempoCommand;
    private final SuccessNotification successNotification;
    private final DeathStormHandler deathStormHandler;
    private final DamageLogListener damageLogListener;

    // Pesadilla
    private final Map<UUID, Integer> levelUpAttempts = new HashMap<>();
    private final Map<UUID, Integer> nightmareLevel = new HashMap<>();
    private final Map<UUID, Long> lastNightmareTime = new HashMap<>();
    private final Map<UUID, BukkitTask> nightmareTasks = new HashMap<>();
    private final Map<UUID, BossBar> nightmareBossBars = new HashMap<>();
    private final Map<UUID, List<LivingEntity>> spawnedMonsters = new HashMap<>();
    private final Map<UUID, BukkitTask> soundTasksng = new HashMap<>();
    private final Map<UUID, BukkitTask> spawnTasks = new HashMap<>();

    private static final int ATTEMPTS_NEEDED = 5;          // 4 intentos → 100%
    private static final int MAX_LEVEL = 3;

    // Duración aleatoria por pesadilla
    private static final int MIN_DURATION_SECONDS = 3 * 60; // 3 minutos
    private static final int MAX_DURATION_SECONDS = 5 * 60; // 5 minutos

    // Cooldown para volver a tener pesadilla (p.ej. si se activa por cama)
    private static final int COOLDOWN_SECONDS = 15 * 60;    // 15 minutos

    private static final int SPAWN_INTERVAL = 15 * 20;      // ticks
    private static final String NIGHTMARE_BOSSBAR_ID = "NightmareMode";

    private final Random random = new Random();

    public NightmareMechanic(JavaPlugin plugin, TiempoCommand tiempoCommand, SuccessNotification successNotification, DeathStormHandler deathStormHandler, DamageLogListener damageLogListener) {
        this.plugin = plugin;
        this.tiempoCommand = tiempoCommand;
        this.successNotification = successNotification;
        this.deathStormHandler = deathStormHandler;
        this.damageLogListener = damageLogListener;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ===========================
    //         GETTERS
    // ===========================

    public boolean isInNightmare(UUID playerId) {
        return nightmareLevel.containsKey(playerId);
    }

    public int getNightmareLevel(UUID playerId) {
        return nightmareLevel.getOrDefault(playerId, 0);
    }

    public boolean canTriggerNightmare(UUID playerId) {
        if (!lastNightmareTime.containsKey(playerId)) return true;
        long elapsed = (System.currentTimeMillis() - lastNightmareTime.get(playerId)) / 1000;
        return elapsed >= COOLDOWN_SECONDS;
    }

    // ===========================
    //       EVENTOS
    // ===========================

    @EventHandler
    public void onTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        UUID playerId = player.getUniqueId();

        // 👉 Ahora SOLO importa el tótem si el jugador YA está en Nightmare
        if (!isInNightmare(playerId)) return;

        increaseNightmareLevel(playerId);
    }

    // ===========================
    //    INICIAR / TERMINAR
    // ===========================

    public void forceStartNightmare(UUID playerId, int level) {
        startNightmare(playerId, Math.max(1, Math.min(level, MAX_LEVEL)));
    }

    public void forceEndNightmare(UUID playerId) {
        endNightmare(playerId);
    }

    public void resetCooldown(UUID playerId) {
        lastNightmareTime.remove(playerId);
    }

    private int getRandomDurationSeconds() {
        if (MIN_DURATION_SECONDS >= MAX_DURATION_SECONDS) return MAX_DURATION_SECONDS;
        return MIN_DURATION_SECONDS + random.nextInt((MAX_DURATION_SECONDS - MIN_DURATION_SECONDS) + 1);
    }

    /**
     * Inicia la pesadilla para un jugador a un nivel concreto.
     * Se llama tanto desde comandos como desde BedEvents.
     */
    private void startNightmare(UUID playerId, int level) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;

        cancelExistingTasks(playerId);
        clearExistingMonsters(playerId);

        int durationSeconds = getRandomDurationSeconds();
        int levelClamped = Math.max(1, Math.min(level, MAX_LEVEL));

        nightmareLevel.put(playerId, levelClamped);
        lastNightmareTime.put(playerId, System.currentTimeMillis());
        levelUpAttempts.remove(playerId);

        broadcastNightmareMessage(player);
        applyNightmareEffects(player, levelClamped);

        createBossBars(player, levelClamped, durationSeconds);

        // Programar fin de la pesadilla
        BukkitTask endTask = new BukkitRunnable() {
            @Override
            public void run() {
                endNightmare(playerId);
            }
        }.runTaskLater(plugin, durationSeconds * 20L);
        nightmareTasks.put(playerId, endTask);

        // Sonidos ambiente
        soundTasksng.put(playerId, new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && isInNightmare(playerId)) {
                    player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, SoundCategory.VOICE, 2.0f, 1.0f);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L));

        // Spawneo de mobs
        spawnTasks.put(playerId, new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && isInNightmare(playerId)) {
                    spawnMonstersGradually(player, nightmareLevel.get(playerId));
                }
            }
        }.runTaskTimer(plugin, 0L, SPAWN_INTERVAL));
    }

    private void endNightmare(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);

        cancelExistingTasks(playerId);
        clearExistingMonsters(playerId);
        endBossBars(playerId);
        levelUpAttempts.remove(playerId);

        nightmareLevel.remove(playerId);

        if (player != null) {
            // Quitar todos los efectos de la pesadilla
            player.removePotionEffect(PotionEffectType.DARKNESS);
            player.removePotionEffect(PotionEffectType.WEAVING);
            player.removePotionEffect(PotionEffectType.UNLUCK);
            player.removePotionEffect(PotionEffectType.WIND_CHARGED);

            player.sendMessage(ChatColor.RED + "۞ La pesadilla ha terminado... " +
                    ChatColor.RED + ChatColor.BOLD + "POR AHORA.");
        }
    }

    private void cancelExistingTasks(UUID playerId) {
        BukkitTask t = nightmareTasks.remove(playerId);
        if (t != null) t.cancel();

        BukkitTask s = soundTasksng.remove(playerId);
        if (s != null) s.cancel();

        BukkitTask sp = spawnTasks.remove(playerId);
        if (sp != null) sp.cancel();
    }

    // ===========================
    //   EFECTOS Y NIVELES
    // ===========================

    private double calculateLevelUpProbability(int attempts) {
        double perAttempt = 100.0 / ATTEMPTS_NEEDED;
        return Math.min(100.0, perAttempt * attempts);
    }

    private void increaseNightmareLevel(UUID playerId) {
        if (!isInNightmare(playerId)) return;

        int currentLevel = nightmareLevel.get(playerId);
        if (currentLevel >= MAX_LEVEL) return;

        int attempts = levelUpAttempts.getOrDefault(playerId, 0) + 1;
        levelUpAttempts.put(playerId, attempts);

        double probability = calculateLevelUpProbability(attempts);
        Player player = Bukkit.getPlayer(playerId);

        if (player != null) {
            player.sendMessage(
                    ChatColor.RED + "۞ Tu pesadilla se retuerce... " +
                            ChatColor.GRAY + "probabilidad de subir a " +
                            ChatColor.DARK_RED + ChatColor.BOLD + "Nivel " + (currentLevel + 1) +
                            ChatColor.GRAY + ": " +
                            ChatColor.DARK_RED + ChatColor.BOLD + String.format("%.1f", probability) + "%"
            );
        }

        if (random.nextDouble() * 100 < probability) {
            int newLevel = currentLevel + 1;
            nightmareLevel.put(playerId, newLevel);
            levelUpAttempts.remove(playerId);

            if (player != null) {
                if (successNotification != null) {
                    successNotification.showSuccess(player);
                }
                player.sendMessage(ChatColor.RED + "۞ La pesadilla se intensifica >> " +
                        ChatColor.DARK_RED + ChatColor.BOLD + "Nivel " + newLevel);
                player.playSound(player, "entity.wither.death", SoundCategory.VOICE, 2f, 0.5f);

                applyNightmareEffects(player, newLevel);
                updateBossBars(player, newLevel);
            }
        }
    }

    /**
     * Aplica los efectos correspondientes al nivel actual de Nightmare.
     * Todos con partículas visibles. No importa la duración real porque
     * al finalizar Nightmare se eliminan manualmente.
     */
    private void applyNightmareEffects(Player player, int level) {
        int amplifierWind = 1; // Wind Charged II -> amplifier 1
        int durationTicks = MAX_DURATION_SECONDS * 20;

        // Limpiar antes de aplicar
        player.removePotionEffect(PotionEffectType.DARKNESS);
        player.removePotionEffect(PotionEffectType.WEAVING);
        player.removePotionEffect(PotionEffectType.UNLUCK);
        player.removePotionEffect(PotionEffectType.WIND_CHARGED);

        // Nivel 1: Darkness + Weaving
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.DARKNESS,
                durationTicks,
                0,
                false,
                true,
                true
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.WEAVING,
                durationTicks,
                0,
                false,
                true,
                true
        ));

        if (level >= 2) {
            // Unluck I
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.UNLUCK,
                    durationTicks,
                    0,
                    false,
                    true,
                    true
            ));
        }

        if (level >= 3) {
            // Wind Charged II
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.WIND_CHARGED,
                    durationTicks,
                    amplifierWind,
                    false,
                    true,
                    true
            ));
        }
    }

    /**
     * Permite cambiar el nivel manualmente (comando /levelnightmare).
     * Si el jugador no está en pesadilla, la inicia en ese nivel.
     * Si ya está, NO reinicia el tiempo, solo cambia el nivel y efectos.
     */
    public void setNightmareLevel(UUID playerId, int newLevel) {
        int levelClamped = Math.max(1, Math.min(newLevel, MAX_LEVEL));
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;

        if (!isInNightmare(playerId)) {
            // Si no tiene Nightmare, la iniciamos directamente en ese nivel
            startNightmare(playerId, levelClamped);
            return;
        }

        nightmareLevel.put(playerId, levelClamped);
        applyNightmareEffects(player, levelClamped);
        updateBossBars(player, levelClamped);

        player.sendMessage(ChatColor.RED + "۞ El nivel de tu pesadilla ha sido ajustado a " +
                ChatColor.DARK_RED + ChatColor.BOLD + "Nivel " + levelClamped);
    }

    // ===========================
    //       BOSSBARS
    // ===========================

    private void createBossBars(Player player, int level, int durationSeconds) {
        endBossBars(player.getUniqueId());

        String barId = player.getUniqueId() + "_" + NIGHTMARE_BOSSBAR_ID;
        String timeFormatted = formatTime(durationSeconds);

        tiempoCommand.createPlayerBossBar(
                player,
                ChatColor.translateAlternateColorCodes('&',
                        "&4&lPesadilla &cNivel &4&l" + level + "&c:"),
                durationSeconds,
                timeFormatted,
                "on",
                barId
        );

        BossBar unicodeBar = Bukkit.createBossBar("§4\uEAA5", BarColor.WHITE, BarStyle.SOLID);
        unicodeBar.setProgress(1.0);
        unicodeBar.addPlayer(player);
        nightmareBossBars.put(player.getUniqueId(), unicodeBar);
    }

    private void updateBossBars(Player player, int level) {
        String displayName = "&4&lPesadilla &cNivel &4&l" + level + "&c:";
        String barId = player.getUniqueId() + "_" + NIGHTMARE_BOSSBAR_ID;

        tiempoCommand.updateBossBarDisplayName(
                barId,
                ChatColor.translateAlternateColorCodes('&', displayName)
        );
    }

    private void endBossBars(UUID playerId) {
        String barId = playerId + "_" + NIGHTMARE_BOSSBAR_ID;
        tiempoCommand.removeBossBar(barId);

        BossBar unicode = nightmareBossBars.remove(playerId);
        if (unicode != null) {
            unicode.removeAll();
        }
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("00:%02d:%02d", minutes, seconds);
    }

    // ===========================
    //       MOBS PESADILLA
    // ===========================

    private void spawnMonstersGradually(Player player, int level) {
        List<LivingEntity> monsters = spawnedMonsters.getOrDefault(player.getUniqueId(), new ArrayList<>());
        World world = player.getWorld();

        int strength = switch (level) {
            case 2 -> 4;
            case 3 -> 6;
            default -> 2;
        };
        int resistance = switch (level) {
            case 2 -> 4;
            case 3 -> 6;
            default -> 2;
        };
        int speed = switch (level) {
            case 2 -> 4;
            case 3 -> 6;
            default -> 2;
        };

        EntityType[] monsterTypes = {
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER,
                EntityType.CREEPER, EntityType.ENDERMAN, EntityType.WITCH,
                EntityType.PHANTOM, EntityType.HUSK, EntityType.DROWNED,
                EntityType.ZOMBIE_VILLAGER, EntityType.STRAY, EntityType.SLIME,
                EntityType.BOGGED
        };

        int monstersToSpawn = switch (level) {
            case 2 -> 1 + random.nextInt(4);
            case 3 -> 2 + random.nextInt(3);
            default -> 1 + random.nextInt(3);
        };

        for (int i = 0; i < monstersToSpawn; i++) {
            EntityType type = monsterTypes[random.nextInt(monsterTypes.length)];
            Location spawnLoc = findSimpleSpawnLocation(player.getLocation());

            if (spawnLoc != null) {
                LivingEntity monster = (LivingEntity) world.spawnEntity(spawnLoc, type);

                monster.addPotionEffect(new PotionEffect(
                        PotionEffectType.STRENGTH,
                        MAX_DURATION_SECONDS * 20,
                        strength - 1,
                        false,
                        false,
                        false
                ));
                monster.addPotionEffect(new PotionEffect(
                        PotionEffectType.RESISTANCE,
                        MAX_DURATION_SECONDS * 20,
                        resistance - 1,
                        false,
                        false,
                        false
                ));
                monster.addPotionEffect(new PotionEffect(
                        PotionEffectType.SPEED,
                        MAX_DURATION_SECONDS * 20,
                        speed - 1,
                        false,
                        false,
                        false
                ));
                monster.addPotionEffect(new PotionEffect(
                        PotionEffectType.FIRE_RESISTANCE,
                        MAX_DURATION_SECONDS * 20,
                        0,
                        false,
                        false,
                        false
                ));
                monster.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOW_FALLING,
                        MAX_DURATION_SECONDS * 20,
                        0,
                        false,
                        false,
                        false
                ));

                if (monster instanceof Monster) {
                    ((Monster) monster).setTarget(player);
                }

                monsters.add(monster);
            }
        }

        spawnedMonsters.put(player.getUniqueId(), monsters);
    }

    private Location findSimpleSpawnLocation(Location playerLoc) {
        World world = playerLoc.getWorld();
        int radius = 8 + random.nextInt(5);

        for (int attempt = 0; attempt < 40; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double x = playerLoc.getX() + radius * Math.cos(angle);
            double z = playerLoc.getZ() + radius * Math.sin(angle);

            int centerY = (int) playerLoc.getY();

            for (int yOffset = -15; yOffset <= 15; yOffset++) {
                int y = centerY + yOffset;

                if (isValidSimpleSpawnLocation(world, x, y, z)) {
                    return new Location(world, x, y, z);
                }
            }
        }

        return null;
    }

    private boolean isValidSimpleSpawnLocation(World world, double x, double y, double z) {
        Material below = world.getBlockAt((int) x, (int) y - 1, (int) z).getType();
        if (!below.isSolid()) return false;

        Material current = world.getBlockAt((int) x, (int) y, (int) z).getType();
        Material above = world.getBlockAt((int) x, (int) y + 1, (int) z).getType();

        return current.isAir() && above.isAir();
    }

    private void clearExistingMonsters(UUID playerId) {
        List<LivingEntity> list = spawnedMonsters.remove(playerId);
        if (list == null) return;

        for (LivingEntity entity : list) {
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
    }

    // ===========================
    //   MENSAJES GENERALES
    // ===========================

    private void broadcastNightmareMessage(Player player) {
        String message = ChatColor.translateAlternateColorCodes('&',
                ChatColor.RED + "\uDBE8\uDCF6"
                        + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                        + ChatColor.RESET + ChatColor.of("#BE517F") + " ha entrado en "
                        + ChatColor.of("#C2144B") + ChatColor.BOLD + "Modo Pesadilla."
                        + "\n"
        );
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(message);
        }
    }

    public void onDisableNightmare() {
        new ArrayList<>(nightmareTasks.keySet()).forEach(this::endNightmare);
        nightmareTasks.clear();
        soundTasksng.clear();
        spawnTasks.clear();
        nightmareBossBars.clear();
        spawnedMonsters.clear();
        levelUpAttempts.clear();
        nightmareLevel.clear();
    }
}
