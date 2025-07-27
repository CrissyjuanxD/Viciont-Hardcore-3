package Dificultades.Features;

import Commands.TiempoCommand;
import Events.DamageLogListener;
import Handlers.DeathStormHandler;
import TitleListener.SuccessNotification;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
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

    private final Map<UUID, Integer> totemCount = new HashMap<>();
    private final Map<UUID, Integer> levelUpAttempts = new HashMap<>();
    private final Map<UUID, Integer> nightmareLevel = new HashMap<>();
    private final Map<UUID, Long> lastNightmareTime = new HashMap<>();
    private final Map<UUID, BukkitTask> nightmareTasks = new HashMap<>();
    private final Map<UUID, BossBar> nightmareBossBars = new HashMap<>();
    private final Map<UUID, List<LivingEntity>> spawnedMonsters = new HashMap<>();
    private final Map<UUID, BukkitTask> soundTasksng = new HashMap<>();
    private final Map<UUID, BukkitTask> spawnTasks = new HashMap<>();
    private final Map<UUID, Long> lastBlockMessageTime = new HashMap<>();

    private final int TOTEMS_NEEDED = 20;
    private final int ATTEMPTS_NEEDED = 6;
    private final double PROBABILITY_MULTIPLIER = 1.5;
    private final int MAX_LEVEL = 3;
    private final int NIGHTMARE_DURATION = 5 * 60;
    private final int COOLDOWN = 60 * 60; // 15 minutos en segundos
    private final int SPAWN_INTERVAL = 15 * 20;
    private static final String NIGHTMARE_BOSSBAR_ID = "NightmareMode";

    public NightmareMechanic(JavaPlugin plugin, TiempoCommand tiempoCommand, SuccessNotification successNotification, DeathStormHandler deathStormHandler, DamageLogListener damageLogListener) {
        this.plugin = plugin;
        this.tiempoCommand = tiempoCommand;
        this.successNotification = successNotification;
        this.deathStormHandler = deathStormHandler;
        this.damageLogListener = damageLogListener;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        UUID playerId = player.getUniqueId();

        if (isInNightmare(playerId)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && isInNightmare(playerId)) {
                        player.addPotionEffect(new PotionEffect(
                                PotionEffectType.DARKNESS,
                                NIGHTMARE_DURATION * 20,
                                0, false, false, false
                        ));

                        increaseNightmareLevel(playerId);
                    }
                }
            }.runTaskLater(plugin, 10L);
            return;
        }

        if (lastNightmareTime.containsKey(playerId)) {
            long timeSinceLast = (System.currentTimeMillis() - lastNightmareTime.get(playerId)) / 1000;
            if (timeSinceLast < COOLDOWN) return;
        }

        int count = totemCount.getOrDefault(playerId, 0) + 1;
        totemCount.put(playerId, count);

        double probability = calculateActivationProbability(count);

        if (damageLogListener != null) {
            damageLogListener.pauseActionBarForPlayer(playerId);
        }

        if (deathStormHandler != null) {
            deathStormHandler.pauseActionBarForPlayer(playerId);
        }

        TextComponent message = new TextComponent();
        TextComponent progreso = new TextComponent(ChatColor.RED + "Pesadilla: " + ChatColor.DARK_RED + ChatColor.BOLD + String.format("%.1f", probability) + "%");
        message.addExtra(progreso);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, progreso);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (deathStormHandler != null) {
                    deathStormHandler.resumeActionBarForPlayer(playerId);
                }

                if (damageLogListener != null) {
                    damageLogListener.resumeActionBarForPlayer(playerId);
                }
            }
        }.runTaskLater(plugin, 60L);

        if ((probability >= 60.0 || probability >= 100.0) && new Random().nextInt(100) < probability) {
            startNightmare(playerId, 1);
            totemCount.remove(playerId);
        } else if (probability >= 100.0) {
            // Garantizado al 100%
            startNightmare(playerId, 1);
            totemCount.remove(playerId);
        }
    }

    private boolean isInNightmare(UUID playerId) {
        return nightmareLevel.containsKey(playerId);
    }

    private void startNightmare(UUID playerId, int level) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;

        cancelExistingTasks(playerId);

        nightmareLevel.put(playerId, level);
        lastNightmareTime.put(playerId, System.currentTimeMillis());

        broadcastNightmareMessage(player);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.DARKNESS,
                            NIGHTMARE_DURATION * 20,
                            0, false, false, false
                    ));
                }
            }
        }.runTask(plugin);

        createBossBars(player, level);

        nightmareTasks.put(playerId, new BukkitRunnable() {
            @Override
            public void run() {
                endNightmare(playerId);
            }
        }.runTaskLater(plugin, NIGHTMARE_DURATION * 20L));

        soundTasksng.put(playerId, new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && isInNightmare(playerId)) {
                    player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, SoundCategory.VOICE, 2.0f, 1.0f);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L));

        spawnTasks.put(playerId, new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && isInNightmare(playerId)) {
                    spawnMonstersGradually(player, nightmareLevel.get(playerId));
                }
            }
        }.runTaskTimer(plugin, 0L, SPAWN_INTERVAL));
    }

    private void cancelExistingTasks(UUID playerId) {
        if (nightmareTasks.containsKey(playerId)) nightmareTasks.get(playerId).cancel();
        if (soundTasksng.containsKey(playerId)) soundTasksng.get(playerId).cancel();
        if (spawnTasks.containsKey(playerId)) spawnTasks.get(playerId).cancel();
    }

    private void increaseNightmareLevel(UUID playerId) {
        if (!isInNightmare(playerId)) return;

        int currentLevel = nightmareLevel.get(playerId);
        if (currentLevel >= MAX_LEVEL) return;

        int attempts = levelUpAttempts.getOrDefault(playerId, 0) + 1;
        levelUpAttempts.put(playerId, attempts);

        double probability = calculateLevelUpProbability(currentLevel, attempts);

        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            if (damageLogListener != null) {
                damageLogListener.pauseActionBarForPlayer(playerId);
            }
            if (deathStormHandler != null) {
                deathStormHandler.pauseActionBarForPlayer(playerId);
            }
            TextComponent message = new TextComponent(ChatColor.RED + "Pesadilla Lvl " + (currentLevel + 1) + ": " +
                    ChatColor.DARK_RED + ChatColor.BOLD + String.format("%.1f", probability) + "%");
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (deathStormHandler != null) {
                        deathStormHandler.resumeActionBarForPlayer(playerId);
                    }

                    if (damageLogListener != null) {
                        damageLogListener.resumeActionBarForPlayer(playerId);
                    }
                }
            }.runTaskLater(plugin, 80L);
        }

        if ((probability >= 60.0 || probability >= 100.0) && new Random().nextInt(100) < probability) {
            nightmareLevel.put(playerId, currentLevel + 1);
            levelUpAttempts.remove(playerId);

            if (player != null) {
                successNotification.showSuccess(player);
                player.sendMessage(ChatColor.RED + "۞ La pesadilla se intensifica >> " +
                        ChatColor.DARK_RED + ChatColor.BOLD + "Nivel " + (currentLevel + 1));
                player.playSound(player, "entity.wither.death", SoundCategory.VOICE, 2f, 0.5f);
                updateBossBars(player, currentLevel + 1);
            }
        }
    }

    private double calculateActivationProbability(int totemsUsed) {
        return Math.min(100, Math.pow(totemsUsed, PROBABILITY_MULTIPLIER) * 100 / Math.pow(TOTEMS_NEEDED, PROBABILITY_MULTIPLIER));
    }

    private double calculateLevelUpProbability(int currentLevel, int attempts) {
        return Math.min(100, Math.pow(attempts, PROBABILITY_MULTIPLIER) * 100 / Math.pow(ATTEMPTS_NEEDED, PROBABILITY_MULTIPLIER));
    }


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

        Random random = new Random();
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

                monster.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,
                        NIGHTMARE_DURATION * 20, strength - 1, false, false));
                monster.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,
                        NIGHTMARE_DURATION * 20, resistance - 1, false, false));
                monster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,
                        NIGHTMARE_DURATION * 20, speed - 1, false, false));
                monster.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE,
                        NIGHTMARE_DURATION * 20, 0, false, false));
                monster.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING,
                        NIGHTMARE_DURATION * 20, 0, false, false));

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
        Random random = new Random();

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
        Material below = world.getBlockAt((int)x, (int)y-1, (int)z).getType();
        if (!below.isSolid()) {
            return false;
        }

        Material current = world.getBlockAt((int)x, (int)y, (int)z).getType();
        Material above = world.getBlockAt((int)x, (int)y+1, (int)z).getType();

        return current.isAir() && above.isAir();
    }


    private void createBossBars(Player player, int level) {
        endBossBars(player.getUniqueId());

        String barId = player.getUniqueId() + "_" + NIGHTMARE_BOSSBAR_ID;

        tiempoCommand.createPlayerBossBar(
                player,
                ChatColor.translateAlternateColorCodes('&', "&4&lPesadilla &cNivel &4&l" + level + "&c:"),
                NIGHTMARE_DURATION,
                "00:05:00",
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

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (isInNightmare(player.getUniqueId())) {
            event.setCancelled(true);
            sendBlockRestrictionMessage(player);
        }
    }

    private void sendBlockRestrictionMessage(Player player) {
        long currentTime = System.currentTimeMillis();
        long lastMessageTime = lastBlockMessageTime.getOrDefault(player.getUniqueId(), 0L);

        if (currentTime - lastMessageTime >= 10000) {
            player.sendMessage(ChatColor.RED + "۞ Mientras estés en el " + ChatColor.DARK_RED + ChatColor.BOLD + "Modo Pesadilla " + ChatColor.RESET + ChatColor.RED + "no puedes colocar bloques.");
            lastBlockMessageTime.put(player.getUniqueId(), currentTime);
        }
    }


    private void endBossBars(UUID playerId) {
        String barId = playerId + "_" + NIGHTMARE_BOSSBAR_ID;
        tiempoCommand.removeBossBar(barId);

        if (nightmareBossBars.containsKey(playerId)) {
            nightmareBossBars.get(playerId).removeAll();
            nightmareBossBars.remove(playerId);
        }
    }

    private void clearExistingMonsters(UUID playerId) {
        if (spawnedMonsters.containsKey(playerId)) {
            spawnedMonsters.get(playerId).forEach(monster -> {
                if (!monster.isDead()) monster.remove();
            });
            spawnedMonsters.remove(playerId);
        }
    }

    private void endNightmare(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.removePotionEffect(PotionEffectType.DARKNESS);

            player.sendMessage(ChatColor.RED + "۞ La pesadilla ha terminado... " + ChatColor.RED + ChatColor.BOLD + "POR AHORA.");
        }

        cancelExistingTasks(playerId);
        clearExistingMonsters(playerId);
        endBossBars(playerId);
        lastBlockMessageTime.remove(playerId);

        nightmareLevel.remove(playerId);
    }

    private void broadcastNightmareMessage(Player player) {
        String message = ChatColor.translateAlternateColorCodes('&',
                        ChatColor.RED + "\uDBE8\uDCF6"
                        + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                        + ChatColor.RESET + ChatColor.of("#BE517F") + " ha entrado en " + ChatColor.of("#C2144B") + ChatColor.BOLD + "Modo Pesadilla."
                        + "\n\n");
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
        }
    }

    public void onDisableNightmare() {
        new ArrayList<>(nightmareTasks.keySet()).forEach(this::endNightmare);
        nightmareTasks.clear();
        soundTasksng.clear();
        spawnTasks.clear();
        nightmareBossBars.clear();
        spawnedMonsters.clear();
    }

    public void resetCooldown(UUID playerId) {
        lastNightmareTime.remove(playerId);
    }

    public void forceStartNightmare(UUID playerId, int level) {
        startNightmare(playerId, level);
    }

    public void forceEndNightmare(UUID playerId) {
        endNightmare(playerId);
    }
}