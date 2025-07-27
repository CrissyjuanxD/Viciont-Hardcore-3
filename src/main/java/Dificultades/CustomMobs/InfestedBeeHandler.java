package Dificultades.CustomMobs;

import items.BootNetheriteEssence;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatColor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InfestedBeeHandler implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> activeBehaviors = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> isAttacking = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> isRegenerating = new ConcurrentHashMap<>();
    private final Map<UUID, String> deathCauseMap = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private final NamespacedKey infestedBeeKey;
    private final NamespacedKey miniWardenKey;
    private final BootNetheriteEssence bootNetheriteEssence;

    // Control estricto de procesamiento - CLAVE PARA EVITAR DUPLICACIONES
    private final Set<UUID> processingBees = ConcurrentHashMap.newKeySet();
    private final Set<UUID> fullyInitializedBees = ConcurrentHashMap.newKeySet();

    // SISTEMA INDEPENDIENTE DE BOSSBAR
    private BukkitRunnable globalBossBarManager;
    private static final double BOSSBAR_RANGE = 100.0;

    public enum AttackType {
        NO_MOVE, DARK_CIRCLE, SUMMON_WARDENS, SONIC_BOOM, MELEE, REGENERATION
    }

    public InfestedBeeHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.bootNetheriteEssence = new BootNetheriteEssence(plugin);
        this.infestedBeeKey = new NamespacedKey(plugin, "infested_bee");
        this.miniWardenKey = new NamespacedKey(plugin, "mini_warden");

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        startGlobalBossBarManager();
    }

    // ==================== SISTEMA INDEPENDIENTE DE BOSSBAR ====================

    private void startGlobalBossBarManager() {
        globalBossBarManager = new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    for (Bee bee : world.getEntitiesByClass(Bee.class)) {
                        if (isInfestedBee(bee)) {
                            manageBossBarForBee(bee);
                        }
                    }
                }
            }
        };
        globalBossBarManager.runTaskTimer(plugin, 0L, 10L);
    }

    private void manageBossBarForBee(Bee bee) {
        UUID beeId = bee.getUniqueId();
        BossBar bossBar = bossBars.get(beeId);

        if (bossBar == null && fullyInitializedBees.contains(beeId)) {
            bossBar = Bukkit.createBossBar(
                    ChatColor.of("#00a8a8") + "Infested Bee",
                    BarColor.BLUE,
                    BarStyle.SOLID
            );
            bossBar.setVisible(true);
            bossBars.put(beeId, bossBar);
            plugin.getLogger().info("Created new BossBar for bee: " + beeId);
        }

        if (bossBar != null) {

            double healthPercentage = Math.max(0.0, bee.getHealth() / 500.0);
            bossBar.setProgress(healthPercentage);

            Set<Player> currentPlayers = new HashSet<>(bossBar.getPlayers());
            Set<Player> shouldHavePlayers = new HashSet<>();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(bee.getWorld())) {
                    double distance = player.getLocation().distance(bee.getLocation());
                    if (distance <= BOSSBAR_RANGE) {
                        shouldHavePlayers.add(player);
                    }
                }
            }

            for (Player player : currentPlayers) {
                if (!shouldHavePlayers.contains(player)) {
                    bossBar.removePlayer(player);
                }
            }

            for (Player player : shouldHavePlayers) {
                if (!currentPlayers.contains(player)) {
                    bossBar.addPlayer(player);
                }
            }
        }
    }

    // ==================== SPAWN Y TRANSFORMACIÓN ====================

    public Bee spawnInfestedBee(Location location) {
        Bee bee = (Bee) Objects.requireNonNull(location.getWorld()).spawnEntity(location, EntityType.BEE);

        // DELAY CRÍTICO: Esperar 1 tick antes de inicializar para evitar conflictos
        new BukkitRunnable() {
            @Override
            public void run() {
                initializeInfestedBee(bee);
            }
        }.runTaskLater(plugin, 1L);

        return bee;
    }

    public void transformToInfestedBee(Bee bee) {
        if (!canProcessBee(bee)) return;

        // DELAY CRÍTICO: Esperar 1 tick antes de inicializar
        new BukkitRunnable() {
            @Override
            public void run() {
                if (bee.isValid() && !bee.isDead()) {
                    initializeInfestedBee(bee);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    private boolean canProcessBee(Bee bee) {
        UUID beeId = bee.getUniqueId();

        // VERIFICACIÓN CRÍTICA: Si ya está completamente inicializado, NO procesar
        if (fullyInitializedBees.contains(beeId)) {
            return false;
        }

        if (processingBees.contains(beeId)) {
            return false;
        }

        return true;
    }

    private void initializeInfestedBee(Bee bee) {
        UUID beeId = bee.getUniqueId();

        if (bossBars.containsKey(beeId)) {
            return;
        }

        // VERIFICACIÓN FINAL: Doble check para evitar duplicaciones
        if (fullyInitializedBees.contains(beeId) || processingBees.contains(beeId)) {
            return;
        }

        processingBees.add(beeId);

        try {
            cleanupBee(beeId);

            if (!bee.getPersistentDataContainer().has(infestedBeeKey, PersistentDataType.BYTE)) {
                setupBeeAttributes(bee);
            }

            isAttacking.put(beeId, false);
            isRegenerating.put(beeId, false);

            startBehavior(bee);

            fullyInitializedBees.add(beeId);

            plugin.getLogger().info("Initialized Infested Bee: " + beeId);

        } finally {
            new BukkitRunnable() {
                @Override
                public void run() {
                    processingBees.remove(beeId);
                }
            }.runTaskLater(plugin, 20L);
        }
    }

    private void setupBeeAttributes(Bee bee) {
        bee.setCustomName(ChatColor.of("#00a8a8") + "Infested Bee");
        bee.setCustomNameVisible(true);
        bee.setSilent(true);
        bee.setRemoveWhenFarAway(false);
        bee.setAnger(999999);
        bee.setCannotEnterHiveTicks(Integer.MAX_VALUE);
        bee.setHasStung(false);
        bee.setAI(true);

        Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_FOLLOW_RANGE)).setBaseValue(50);
        Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(500.0);
        Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.95);
        Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_SCALE)).setBaseValue(3);
        bee.setHealth(500.0);

        bee.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        bee.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        bee.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1, false, false));
        bee.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1, false, false));

        bee.getPersistentDataContainer().set(infestedBeeKey, PersistentDataType.BYTE, (byte) 1);
    }

    // ==================== COMPORTAMIENTO PRINCIPAL ====================

    private void startBehavior(Bee bee) {
        UUID beeId = bee.getUniqueId();

        // VERIFICACIÓN CRÍTICA: Solo un comportamiento por abeja
        if (activeBehaviors.containsKey(beeId)) {
            BukkitRunnable existing = activeBehaviors.get(beeId);
            if (existing != null && !existing.isCancelled()) {
                return;
            }
        }

        BukkitRunnable behavior = new BukkitRunnable() {
            @Override
            public void run() {
                if (bee.isDead() || !bee.isValid()) {
                    cancel();
                    activeBehaviors.remove(beeId);
                    return;
                }

                executeBehaviorTick(bee);
            }
        };

        behavior.runTaskTimer(plugin, 0L, 60L);
        activeBehaviors.put(beeId, behavior);

        plugin.getLogger().info("Started behavior for Infested Bee: " + beeId);
    }

    private void executeBehaviorTick(Bee bee) {
        UUID beeId = bee.getUniqueId();

        // VERIFICACIÓN CRÍTICA: Solo un ataque a la vez
        if (isAttacking.getOrDefault(beeId, false)) {
            return;
        }

        Player target = findTarget(bee);
        if (target == null) {
            executeIdleBehavior(bee);
            return;
        }

        double distance = bee.getLocation().distance(target.getLocation());
        if (distance > 50) {
            bee.setTarget(target);
            executeIdleBehavior(bee);
            return;
        }

        if (bee.getHealth() < 250.0 && !isRegenerating.getOrDefault(beeId, false) && random.nextDouble() <= 0.20) {
            executeRegeneration(bee);
            return;
        }

        executeRandomAttack(bee);
    }

    private Player findTarget(Bee bee) {
        List<Player> worldPlayers = bee.getWorld().getPlayers()
                .stream()
                .filter(p -> p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)
                .collect(Collectors.toList());

        return worldPlayers.isEmpty() ? null : worldPlayers.get(random.nextInt(worldPlayers.size()));
    }

    private void executeIdleBehavior(Bee bee) {
        if (random.nextDouble() < 0.3) {
            Vector direction = new Vector(
                    random.nextDouble() * 2 - 1,
                    random.nextDouble() * 0.5 - 0.25,
                    random.nextDouble() * 2 - 1
            ).normalize().multiply(0.8);

            if (isVectorSafe(direction)) {
                bee.setVelocity(direction);
            }
        }

        if (random.nextDouble() < 0.2) {
            bee.getWorld().spawnParticle(Particle.SCULK_SOUL, bee.getLocation(), 5);
            bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_WARDEN_AMBIENT, 0.3f, 1.5f);
        }
    }

    private void executeRandomAttack(Bee bee) {
        UUID beeId = bee.getUniqueId();

        isAttacking.put(beeId, true);

        AttackType[] attacks = {AttackType.NO_MOVE, AttackType.DARK_CIRCLE, AttackType.SUMMON_WARDENS,
                AttackType.SONIC_BOOM, AttackType.SONIC_BOOM, AttackType.MELEE};
        AttackType selected = attacks[random.nextInt(attacks.length)];

        switch (selected) {
            case NO_MOVE -> executeNoMove(bee);
            case DARK_CIRCLE -> executeDarkCircle(bee);
            case SUMMON_WARDENS -> executeSummonWardens(bee);
            case SONIC_BOOM -> executeSonicBoom(bee);
            case MELEE -> executeMelee(bee);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                isAttacking.put(beeId, false);
            }
        }.runTaskLater(plugin, 100L);
    }

    // ==================== ATAQUES ====================

    private void executeNoMove(Bee bee) {
        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 5.0f, 0.5f);

        BossBar noMoveBar = Bukkit.createBossBar(ChatColor.of("#000000") + "\uEAA5", BarColor.WHITE, BarStyle.SOLID);
        noMoveBar.setVisible(true);

        List<Player> targets = getRandomNearbyPlayers(bee, 30, 2, 6);
        targets.forEach(noMoveBar::addPlayer);

        targets.forEach(player -> {
            player.sendTitle(ChatColor.of("#00a8a8") + "✧ No te levantes ✧",
                    ChatColor.GRAY + "Mantente Shifteando!", 10, 70, 20);

            new BukkitRunnable() {
                int seconds = 0;

                @Override
                public void run() {
                    if (seconds >= 20 || bee.isDead() || !bee.isValid()) {
                        cancel();
                        return;
                    }

                    if (!player.isSneaking()) {
                        player.damage(6);
                        player.getWorld().spawnParticle(Particle.SCULK_SOUL, player.getEyeLocation(), 5);
                        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HURT, 0.5f, 1.5f);
                    }

                    seconds++;
                }
            }.runTaskTimer(plugin, 0L, 20L);
        });

        new BukkitRunnable() {
            @Override
            public void run() {
                noMoveBar.removeAll();
            }
        }.runTaskLater(plugin, 400L);
    }

    private void executeDarkCircle(Bee bee) {
        Location center = bee.getLocation();

        new BukkitRunnable() {
            double radius = 0.5;
            int pulseCount = 0;

            @Override
            public void run() {
                if (radius > 15 || bee.isDead() || !bee.isValid()) {
                    cancel();
                    return;
                }

                for (double angle = 0; angle < 360; angle += 10) {
                    double radians = Math.toRadians(angle);
                    Location particleLoc = center.clone().add(
                            Math.cos(radians) * radius, 0, Math.sin(radians) * radius
                    );

                    center.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
                    center.getWorld().spawnParticle(Particle.SOUL, particleLoc, 1);

                    center.getWorld().getNearbyEntities(particleLoc, 1, 1, 1).stream()
                            .filter(e -> e instanceof Player)
                            .map(e -> (Player) e)
                            .forEach(player -> {
                                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 600, 0));
                                player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 600, 4));
                            });
                }

                if (pulseCount % 3 == 0) {
                    center.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.5f, 1.0f);
                    center.getWorld().spawnParticle(Particle.SCULK_SOUL, center, 10, 0.5, 1.0, 0.5, 0.1);
                }

                pulseCount++;
                radius += 0.5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void executeSummonWardens(Bee bee) {
        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_WARDEN_EMERGE, 5.0f, 1.5f);

        int count = random.nextInt(3) + 2;
        Player target = findTarget(bee);

        for (int i = 0; i < count; i++) {
            Warden warden = (Warden) bee.getWorld().spawnEntity(bee.getLocation(), EntityType.WARDEN);
            warden.setCustomName(ChatColor.of("#00a8a8") + "Mini Warden " + (i + 1));
            warden.setCustomNameVisible(false);
            warden.getPersistentDataContainer().set(miniWardenKey, PersistentDataType.BYTE, (byte) 1);

            Objects.requireNonNull(warden.getAttribute(Attribute.GENERIC_SCALE)).setBaseValue(0.4);
            Objects.requireNonNull(warden.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(2.0);
            Objects.requireNonNull(warden.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(8.0);
            warden.setHealth(2.0);

            if (target != null) {
                warden.setTarget(target);
            }
        }
    }

    private void executeSonicBoom(Bee bee) {
        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 5.0f, 1.0f);

        Location start = bee.getLocation().add(0, 1, 0);

        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(45 * i);
            Vector direction = new Vector(Math.cos(angle), -0.5, Math.sin(angle)).normalize();

            new BukkitRunnable() {
                double distance = 0;
                final Location current = start.clone();

                @Override
                public void run() {
                    if (distance >= 30 || bee.isDead() || !bee.isValid()) {
                        createSonicBoomZone(current);
                        cancel();
                        return;
                    }

                    current.add(direction.clone().multiply(0.5));
                    distance += 0.5;

                    current.getWorld().spawnParticle(Particle.SONIC_BOOM, current, 1);
                    current.getWorld().spawnParticle(Particle.SCULK_SOUL, current, 2, 0.1, 0.1, 0.1, 0);

                    current.getWorld().getNearbyEntities(current, 1.5, 1.5, 1.5).stream()
                            .filter(e -> e instanceof Player)
                            .map(e -> (Player) e)
                            .forEach(player -> {
                                player.damage(8);
                                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                                deathCauseMap.put(player.getUniqueId(), "Sonic Boom of Infested Bee");
                            });
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    private void createSonicBoomZone(Location center) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 100) {
                    cancel();
                    return;
                }

                center.getWorld().spawnParticle(Particle.SONIC_BOOM, center, 1, 1.5, 0.5, 1.5, 0);

                center.getWorld().getNearbyEntities(center, 1.5, 1.5, 1.5).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .forEach(player -> {
                            player.damage(2);
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                        });

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void executeMelee(Bee bee) {
        Player target = findTarget(bee);

        if (target != null) {
            bee.setTarget(target);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (bee.isDead() || !bee.isValid()) {
                        cancel();
                        return;
                    }

                    if (bee.getLocation().distance(target.getLocation()) <= 1.5) {
                        target.damage(6);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));
                        bee.getWorld().spawnParticle(Particle.SCULK_SOUL, target.getLocation(), 20);
                        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT, 1f, 1.5f);
                    }

                    bee.setTarget(null);
                }
            }.runTaskLater(plugin, 40L);
        }
    }

    private void executeRegeneration(Bee bee) {
        UUID beeId = bee.getUniqueId();

        if (isRegenerating.getOrDefault(beeId, false)) return;

        isRegenerating.put(beeId, true);

        BukkitRunnable particles = new BukkitRunnable() {
            @Override
            public void run() {
                if (!bee.isValid() || bee.isDead()) {
                    cancel();
                    return;
                }

                for (int i = 0; i < 10; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    Location particleLoc = bee.getLocation().clone().add(
                            Math.cos(angle) * 0.5, 0, Math.sin(angle) * 0.5
                    );
                    bee.getWorld().spawnParticle(Particle.SCULK_SOUL, particleLoc, 1);
                    bee.getWorld().spawnParticle(Particle.SOUL, particleLoc, 1);
                }

                bee.getWorld().playSound(bee.getLocation(), Sound.BLOCK_SCULK_CATALYST_BLOOM, 0.5f, 1.5f);
            }
        };

        particles.runTaskTimer(plugin, 0L, 10L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (bee.isValid() && !bee.isDead()) {
                    double maxHealth = Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
                    double newHealth = Math.min(bee.getHealth() + 40, maxHealth);
                    bee.setHealth(newHealth);
                }

                isRegenerating.put(beeId, false);
                particles.cancel();
            }
        }.runTaskLater(plugin, 100L);
    }

    // ==================== EVENTOS ====================

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Bee bee) || !isInfestedBee(bee)) return;

        if (bee.getHealth() - event.getFinalDamage() <= 0) {
            event.setCancelled(true);
            bee.setHealth(0);
            executeBeeDeath(bee);
        }
    }

    @EventHandler
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Bee bee) || !isInfestedBee(bee)) return;

        if (event.getDamager() instanceof Projectile) {
            event.setDamage(event.getDamage() * 0.5);
        }
    }

    @EventHandler
    public void onBeeAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Bee bee) || !isInfestedBee(bee)) return;

        event.setCancelled(true);

        if (event.getEntity() instanceof Player player) {
            player.damage(4);
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));

            bee.playEffect(EntityEffect.ENTITY_POOF);
            bee.getWorld().spawnParticle(Particle.SCULK_SOUL, bee.getLocation(), 10, 0.5, 1.0, 0.5, 0.1);
            bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 0.5f);

            deathCauseMap.put(player.getUniqueId(), "Infested Bee");

            Vector knockback = player.getLocation().toVector()
                    .subtract(bee.getLocation().toVector())
                    .normalize().multiply(0.4);

            if (isVectorSafe(knockback)) {
                player.setVelocity(knockback);
            }
        }
    }

    @EventHandler
    public void onBeeSting(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Bee bee && isInfestedBee(bee)) {
            bee.setHasStung(false);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Bee bee) || !isInfestedBee(bee)) return;

        bee.setAnger(999999);

        if (event.getTarget() == null || !(event.getTarget() instanceof Player)) {
            Player target = findTarget(bee);
            if (target != null) {
                bee.setTarget(target);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();

        if (deathCauseMap.containsKey(playerId)) {
            event.setDeathMessage(player.getName() + " ha muerto por " + deathCauseMap.remove(playerId));
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlock().equals(event.getTo().getBlock())) return;

        Player player = event.getPlayer();

        player.getNearbyEntities(50, 50, 50).stream()
                .filter(e -> e instanceof Bee)
                .map(e -> (Bee) e)
                .filter(bee -> bee.getCustomName() != null && bee.getCustomName().contains("Infested Bee"))
                .forEach(bee -> {
                    if (!isInfestedBee(bee)) {
                        transformToInfestedBee(bee);
                    } else {
                        // VERIFICACIÓN ESPECÍFICA: Solo reiniciar si realmente no tiene comportamiento
                        UUID beeId = bee.getUniqueId();
                        if (!activeBehaviors.containsKey(beeId) || activeBehaviors.get(beeId).isCancelled()) {
                            plugin.getLogger().info("Player movement: Restarting behavior for dormant bee: " + beeId);
                            restartBehaviorSafely(bee);
                        }
                    }
                });
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();

        new BukkitRunnable() {
            @Override
            public void run() {
                world.getEntitiesByClass(Bee.class).stream()
                        .filter(bee -> bee.getCustomName() != null && bee.getCustomName().contains("Infested Bee"))
                        .filter(bee -> bee.getPersistentDataContainer().has(infestedBeeKey, PersistentDataType.BYTE))
                        .forEach(bee -> {
                            plugin.getLogger().info("World load: Found existing Infested Bee: " + bee.getUniqueId());
                            if (canProcessBee(bee)) {
                                transformToInfestedBee(bee);
                            }
                        });
            }
        }.runTaskLater(plugin, 100L);
    }

    // ==================== REINICIO SEGURO ====================

    private void restartBehaviorSafely(Bee bee) {
        UUID beeId = bee.getUniqueId();

        if (processingBees.contains(beeId) || !fullyInitializedBees.contains(beeId)) {
            return;
        }

        processingBees.add(beeId);

        try {
            BukkitRunnable oldBehavior = activeBehaviors.remove(beeId);
            if (oldBehavior != null && !oldBehavior.isCancelled()) {
                oldBehavior.cancel();
            }

            isAttacking.put(beeId, false);

            startBehavior(bee);

        } finally {
            new BukkitRunnable() {
                @Override
                public void run() {
                    processingBees.remove(beeId);
                }
            }.runTaskLater(plugin, 20L);
        }
    }

    // ==================== MUERTE Y LIMPIEZA ====================

    public void executeBeeDeath(Bee bee) {
        if (!bee.getPersistentDataContainer().has(infestedBeeKey, PersistentDataType.BYTE)) return;

        int amount = random.nextInt(2) + 1;
        for (int i = 0; i < amount; i++) {
            bee.getWorld().dropItemNaturally(bee.getLocation(), bootNetheriteEssence.createBootNetheriteEssence());
        }

        playDeathEffects(bee.getLocation());
        cleanupBee(bee.getUniqueId());
        bee.getPersistentDataContainer().remove(infestedBeeKey);
        bee.remove();
    }

    private void playDeathEffects(Location location) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.stopSound("custom.music2_skybattle", SoundCategory.RECORDS);

            if (player.getWorld().equals(location.getWorld()) &&
                    player.getLocation().distanceSquared(location) <= 1600) {

                player.playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0f, 0.8f);
                player.sendMessage(ChatColor.of("#00a8a8") + "" + ChatColor.BOLD + "\u06de" + " " +
                        ChatColor.of("#00a8a8") + "Haz derrotado a la " + ChatColor.BOLD + "Infested Bee" +
                        ChatColor.of("#00a8a8") + ".");
            }
        }
    }

    private void cleanupBee(UUID beeId) {
        BukkitRunnable behavior = activeBehaviors.remove(beeId);
        if (behavior != null && !behavior.isCancelled()) {
            behavior.cancel();
        }

        BossBar bossBar = bossBars.remove(beeId);
        if (bossBar != null) {
            try {
                bossBar.setVisible(false);
                bossBar.removeAll();
            } catch (Exception e) {
                plugin.getLogger().warning("Error cleaning up BossBar: " + e.getMessage());
            }
        }

        isAttacking.remove(beeId);
        isRegenerating.remove(beeId);
        processingBees.remove(beeId);
        fullyInitializedBees.remove(beeId);
    }

    // ==================== UTILIDADES ====================

    private boolean isInfestedBee(Bee bee) {
        return bee != null && bee.isValid() && !bee.isDead() &&
                bee.getCustomName() != null && bee.getCustomName().contains("Infested Bee") &&
                bee.getPersistentDataContainer().has(infestedBeeKey, PersistentDataType.BYTE);
    }

    private boolean isVectorSafe(Vector vector) {
        return !Double.isNaN(vector.getX()) && !Double.isNaN(vector.getY()) && !Double.isNaN(vector.getZ()) &&
                !Double.isInfinite(vector.getX()) && !Double.isInfinite(vector.getY()) && !Double.isInfinite(vector.getZ());
    }

    private List<Player> getRandomNearbyPlayers(Bee bee, double range, int min, int max) {
        List<Player> nearby = bee.getWorld().getNearbyEntities(bee.getLocation(), range, range, range)
                .stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .collect(Collectors.toList());

        Collections.shuffle(nearby);
        int count = Math.min(random.nextInt(max - min + 1) + min, nearby.size());
        return nearby.subList(0, count);
    }

    // ==================== MÉTODOS PÚBLICOS ====================

    public void shutdown() {
        if (globalBossBarManager != null && !globalBossBarManager.isCancelled()) {
            globalBossBarManager.cancel();
        }

        new ArrayList<>(fullyInitializedBees).forEach(this::cleanupBee);
        processingBees.clear();
        fullyInitializedBees.clear();
    }
}