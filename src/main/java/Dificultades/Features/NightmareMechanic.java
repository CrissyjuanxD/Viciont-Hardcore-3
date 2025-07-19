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
    private final int NIGHTMARE_DURATION = 5 * 60; // 5 minutos en segundos
    private final int COOLDOWN = 60 * 60; // 15 minutos en segundos
    private final int SPAWN_INTERVAL = 15 * 20;
    private static final String NIGHTMARE_BOSSBAR_ID = "NightmareMode";// 10 segundos en ticks

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

        // Reaplicar efecto darkness si está en pesadilla
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

                        // 30% de probabilidad de subir de nivel
                        increaseNightmareLevel(playerId);
                    }
                }
            }.runTaskLater(plugin, 10L);
            return;
        }

        // Verificar cooldown
        if (lastNightmareTime.containsKey(playerId)) {
            long timeSinceLast = (System.currentTimeMillis() - lastNightmareTime.get(playerId)) / 1000;
            if (timeSinceLast < COOLDOWN) return;
        }

        // Incrementar contador y calcular probabilidad
        int count = totemCount.getOrDefault(playerId, 0) + 1;
        totemCount.put(playerId, count);

        // Fórmula de probabilidad progresiva
        double probability = calculateActivationProbability(count);

        if (damageLogListener != null) {
            damageLogListener.pauseActionBarForPlayer(playerId);
        }

        if (deathStormHandler != null) {
            deathStormHandler.pauseActionBarForPlayer(playerId);
        }

        // Mostrar progreso al jugador
        TextComponent message = new TextComponent();
        TextComponent progreso = new TextComponent(ChatColor.RED + "Pesadilla: " + ChatColor.DARK_RED + ChatColor.BOLD + String.format("%.1f", probability) + "%");
        message.addExtra(progreso);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, progreso);

        // Programar reanudación del ActionBar de DeathStorm después de 3 segundos
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
            totemCount.remove(playerId); // Resetear contador
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

        // Cancelar tareas anteriores
        cancelExistingTasks(playerId);

        // Configurar nivel
        nightmareLevel.put(playerId, level);
        lastNightmareTime.put(playerId, System.currentTimeMillis());

        //mensaje a todos los jugadores
        broadcastNightmareMessage(player);

        // Aplicar efectos
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

        // Crear bossbars
        createBossBars(player, level);

        // Programar fin de la pesadilla
        nightmareTasks.put(playerId, new BukkitRunnable() {
            @Override
            public void run() {
                endNightmare(playerId);
            }
        }.runTaskLater(plugin, NIGHTMARE_DURATION * 20L));

        // Sonido ambiental cada segundo
        soundTasksng.put(playerId, new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && isInNightmare(playerId)) {
                    player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, SoundCategory.VOICE, 2.0f, 1.0f);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L));

        // Spawn gradual de monstruos
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

        // Misma lógica de activación que en onTotemUse
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

        // Configurar efectos según nivel
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
            case 2 -> 1 + random.nextInt(4); // 1-4
            case 3 -> 2 + random.nextInt(3); // 2-4
            default -> 1 + random.nextInt(3); // 1-3
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

        // Radio de Spawn entre 8 a 12 bloques
        int radius = 8 + random.nextInt(5);

        // Intentar encontrar ubicación válida
        for (int attempt = 0; attempt < 40; attempt++) {
            // Calcular posición XZ alrededor del jugador
            double angle = random.nextDouble() * 2 * Math.PI;
            double x = playerLoc.getX() + radius * Math.cos(angle);
            double z = playerLoc.getZ() + radius * Math.sin(angle);

            // Usar la altura del jugador como referencia
            int centerY = (int) playerLoc.getY();

            // Buscar hacia arriba y abajo desde la altura del jugador
            for (int yOffset = -15; yOffset <= 15; yOffset++) {
                int y = centerY + yOffset;

                // Verificar si la ubicación es válida
                if (isValidSimpleSpawnLocation(world, x, y, z)) {
                    return new Location(world, x, y, z);
                }
            }
        }

        return null;
    }

    private boolean isValidSimpleSpawnLocation(World world, double x, double y, double z) {
        // Verificar que el bloque debajo sea sólido
        Material below = world.getBlockAt((int)x, (int)y-1, (int)z).getType();
        if (!below.isSolid()) {
            return false;
        }

        // Verificar que el bloque actual y el de arriba sean aire
        Material current = world.getBlockAt((int)x, (int)y, (int)z).getType();
        Material above = world.getBlockAt((int)x, (int)y+1, (int)z).getType();

        return current.isAir() && above.isAir();
    }


    private void createBossBars(Player player, int level) {
        // Eliminar bossbars existentes primero
        endBossBars(player.getUniqueId());

        // Crear ID único para la bossbar de este jugador
        String barId = player.getUniqueId() + "_" + NIGHTMARE_BOSSBAR_ID;

        // Crear bossbar específica para el jugador con sonido desactivado
        tiempoCommand.createPlayerBossBar(
                player,
                ChatColor.translateAlternateColorCodes('&', "&4&lPesadilla &cNivel &4&l" + level + "&c:"),
                NIGHTMARE_DURATION,
                "00:05:00",
                "on",
                barId
        );

        // Bossbar de unicode
        BossBar unicodeBar = Bukkit.createBossBar("§4\uEAA5", BarColor.WHITE, BarStyle.SOLID);
        unicodeBar.setProgress(1.0);
        unicodeBar.addPlayer(player);
        nightmareBossBars.put(player.getUniqueId(), unicodeBar);
    }

    private void updateBossBars(Player player, int level) {
        String displayName = "&4&lPesadilla &cNivel &4&l" + level + "&c:";
        String barId = player.getUniqueId() + "_" + NIGHTMARE_BOSSBAR_ID;

        // Actualizar el nombre de visualización
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

        // Verificar cooldown de 10 segundos
        if (currentTime - lastMessageTime >= 10000) {
            player.sendMessage(ChatColor.RED + "۞ Mientras estés en el " + ChatColor.DARK_RED + ChatColor.BOLD + "Modo Pesadilla " + ChatColor.RESET + ChatColor.RED + "no puedes colocar bloques.");
            lastBlockMessageTime.put(player.getUniqueId(), currentTime);
        }
    }


    private void endBossBars(UUID playerId) {
        // Eliminar bossbar de tiempo
        String barId = playerId + "_" + NIGHTMARE_BOSSBAR_ID;
        tiempoCommand.removeBossBar(barId);

        // Eliminar bossbar unicode
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
            // Remover efectos
            player.removePotionEffect(PotionEffectType.DARKNESS);

            // Notificación
            player.sendMessage(ChatColor.RED + "۞ La pesadilla ha terminado... " + ChatColor.RED + ChatColor.BOLD + "POR AHORA.");
        }

        // Limpiar todo
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
        // Limpiar todo al desactivar el plugin
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