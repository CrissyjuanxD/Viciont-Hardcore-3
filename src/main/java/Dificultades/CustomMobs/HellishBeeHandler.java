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
import org.bukkit.metadata.FixedMetadataValue;
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

public class HellishBeeHandler implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> activeBehaviors = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> isAttacking = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> isRegenerating = new ConcurrentHashMap<>();
    private final Map<UUID, String> deathCauseMap = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private final NamespacedKey hellishBeeKey;
    private final NamespacedKey guardianVexKey;
    private final BootNetheriteEssence bootNetheriteEssence;

    // Control estricto de procesamiento - CLAVE PARA EVITAR DUPLICACIONES
    private final Set<UUID> processingBees = ConcurrentHashMap.newKeySet();
    private final Set<UUID> fullyInitializedBees = ConcurrentHashMap.newKeySet();

    // SISTEMA INDEPENDIENTE DE BOSSBAR
    private BukkitRunnable globalBossBarManager;
    private static final double BOSSBAR_RANGE = 100.0;

    public enum AttackType {
        PARALYSIS, FIRE_CIRCLE, SUMMON_VEXES, EXPLOSIVE_ATTACK, FIRE_RAYS, MELEE, REGENERATION
    }

    public HellishBeeHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.bootNetheriteEssence = new BootNetheriteEssence(plugin);
        this.hellishBeeKey = new NamespacedKey(plugin, "hellish_bee");
        this.guardianVexKey = new NamespacedKey(plugin, "guardian_vex");

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
                        if (isHellishBee(bee)) {
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
                    ChatColor.RED + "Abeja Infernal",
                    BarColor.RED,
                    BarStyle.SOLID
            );
            bossBar.setVisible(true);
            bossBars.put(beeId, bossBar);
            plugin.getLogger().info("Created new BossBar for hellish bee: " + beeId);
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

    public Bee spawnHellishBee(Location location) {
        Bee bee = (Bee) Objects.requireNonNull(location.getWorld()).spawnEntity(location, EntityType.BEE);

        // DELAY CRÍTICO: Esperar 1 tick antes de inicializar para evitar conflictos
        new BukkitRunnable() {
            @Override
            public void run() {
                initializeHellishBee(bee);
            }
        }.runTaskLater(plugin, 1L);

        return bee;
    }

    public void transformToHellishBee(Bee bee) {
        if (!canProcessBee(bee)) return;

        // DELAY CRÍTICO: Esperar 1 tick antes de inicializar
        new BukkitRunnable() {
            @Override
            public void run() {
                if (bee.isValid() && !bee.isDead()) {
                    initializeHellishBee(bee);
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

    private void initializeHellishBee(Bee bee) {
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

            if (!bee.getPersistentDataContainer().has(hellishBeeKey, PersistentDataType.BYTE)) {
                setupBeeAttributes(bee);
            }

            isAttacking.put(beeId, false);
            isRegenerating.put(beeId, false);

            startBehavior(bee);

            fullyInitializedBees.add(beeId);

            plugin.getLogger().info("Initialized Hellish Bee: " + beeId);

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
        bee.setCustomName(ChatColor.DARK_RED + "Abeja Infernal");
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
        bee.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        bee.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1, false, false));

        bee.getPersistentDataContainer().set(hellishBeeKey, PersistentDataType.BYTE, (byte) 1);

        bee.setTarget(findTarget(bee));
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

        behavior.runTaskTimer(plugin, 0L, 50L);
        activeBehaviors.put(beeId, behavior);

        plugin.getLogger().info("Started behavior for Hellish Bee: " + beeId);
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
            bee.getWorld().spawnParticle(Particle.FLAME, bee.getLocation(), 5);
            bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.3f, 1.5f);
        }
    }

    private void executeRandomAttack(Bee bee) {
        UUID beeId = bee.getUniqueId();

        isAttacking.put(beeId, true);

        AttackType[] attacks = {AttackType.PARALYSIS, AttackType.FIRE_CIRCLE, AttackType.SUMMON_VEXES,
                AttackType.EXPLOSIVE_ATTACK, AttackType.FIRE_RAYS, AttackType.MELEE};
        AttackType selected = attacks[random.nextInt(attacks.length)];

        switch (selected) {
            case PARALYSIS -> executeParalysisAttack(bee);
            case FIRE_CIRCLE -> executeFireCircle(bee);
            case SUMMON_VEXES -> executeSummonGuardianVexes(bee);
            case EXPLOSIVE_ATTACK -> executeExplosiveAttack(bee);
            case FIRE_RAYS -> executeFireRays(bee);
            case MELEE -> executeMeleeAttack(bee);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                isAttacking.put(beeId, false);
            }
        }.runTaskLater(plugin, 100L);
    }

    // ==================== ATAQUES ESPECÍFICOS DE HELLISH BEE ====================

    private void executeParalysisAttack(Bee bee) {
        Location beeLocation = bee.getLocation();

        bee.getWorld().playSound(beeLocation, Sound.ENTITY_WITHER_SHOOT, 5.0f, 0.5f);

        BossBar paralysisBar = Bukkit.createBossBar(ChatColor.DARK_RED + "\uEAA5", BarColor.WHITE, BarStyle.SOLID);
        paralysisBar.setVisible(true);

        for (Player player : beeLocation.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(beeLocation) <= 900) {
                paralysisBar.addPlayer(player);
                player.sendTitle(ChatColor.RED + "☠Parálisis☠", "", 10, 70, 20);

                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 250, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 0, true, true));

                player.spawnParticle(Particle.LAVA, player.getLocation(), 30);
                player.spawnParticle(Particle.SMOKE, player.getLocation(), 15);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!bee.isDead()) {
                    launchFireballAttack(bee);
                }
            }
        }.runTaskLater(plugin, 10L);

        new BukkitRunnable() {
            @Override
            public void run() {
                paralysisBar.removeAll();
            }
        }.runTaskLater(plugin, 200L);
    }

    private void launchFireballAttack(Bee bee) {
        Location beeLocation = bee.getLocation().add(0, 1, 0);

        bee.getWorld().playSound(beeLocation, Sound.ENTITY_BLAZE_SHOOT, 2.0f, 0.8f);

        List<Player> targets = beeLocation.getWorld().getNearbyEntities(beeLocation, 30, 30, 30).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player)e)
                .collect(Collectors.toList());

        for (int i = 0; i < 4; i++) {
            double angle = 2 * Math.PI * i / 4;
            double x = beeLocation.getX() + 1 * Math.cos(angle);
            double z = beeLocation.getZ() + 1 * Math.sin(angle);
            Location spawnLoc = new Location(beeLocation.getWorld(), x, beeLocation.getY() + 0.5, z);

            Player target = null;
            if (!targets.isEmpty()) {
                if (targets.size() <= i) {
                    target = targets.get(random.nextInt(targets.size()));
                } else {
                    target = targets.get(i);
                }
            }

            createCustomFireball(spawnLoc, target, bee);
        }
    }

    private void createCustomFireball(Location startLoc, Player target, Bee bee) {
        BlockDisplay fireball = startLoc.getWorld().spawn(startLoc, BlockDisplay.class);
        fireball.setBlock(Bukkit.createBlockData(Material.MAGMA_BLOCK));
        fireball.setGravity(false);
        fireball.setGlowing(true);
        fireball.setGlowColorOverride(Color.ORANGE);

        Vector initialDirection = new Vector(
                random.nextDouble() - 0.5,
                0.2,
                random.nextDouble() - 0.5
        ).normalize().multiply(0.8);

        fireball.getWorld().playSound(fireball.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1.5f, 0.7f);

        if (target != null) {
            target.sendTitle("", ChatColor.RED + "¡Fireball dirigida hacia ti!", 5, 20, 5);
        }

        new BukkitRunnable() {
            int ticks = 0;
            boolean hasRebounded = false;
            Vector currentDirection = initialDirection.clone();

            @Override
            public void run() {
                if (fireball.isDead() || !fireball.isValid() || bee.isDead()) {
                    if (!fireball.isDead()) fireball.remove();
                    cancel();
                    return;
                }

                // Fase 1: Movimiento inicial
                if (ticks < 30) {
                    Location newLoc = fireball.getLocation().add(currentDirection.clone().multiply(0.5));
                    fireball.teleport(newLoc);

                    fireball.getWorld().spawnParticle(Particle.FLAME, fireball.getLocation(), 2, 0.1, 0.1, 0.1, 0);
                    fireball.getWorld().spawnParticle(Particle.LAVA, fireball.getLocation(), 1);
                }
                // Momento de rebote
                else if (ticks >= 30 && ticks < 40 && !hasRebounded) {
                    hasRebounded = true;

                    fireball.getWorld().playSound(fireball.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.0f, 1.5f);
                    fireball.getWorld().spawnParticle(Particle.FLAME, fireball.getLocation(), 15, 0.5, 0.5, 0.5, 0.1);
                    fireball.getWorld().spawnParticle(Particle.EXPLOSION, fireball.getLocation(), 1);

                    if (target != null && target.isOnline()) {
                        Vector newDirection = target.getLocation().add(0, 1, 0)
                                .subtract(fireball.getLocation()).toVector().normalize();
                        currentDirection = newDirection.multiply(1.2);

                        fireball.getWorld().playSound(fireball.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1.0f, 2.0f);
                    } else {
                        explodeCustomFireball(fireball);
                        cancel();
                        return;
                    }
                }
                // Fase 2: Movimiento hacia el objetivo
                else if (ticks >= 40) {
                    Location newLoc = fireball.getLocation().add(currentDirection.clone().multiply(0.6));
                    fireball.teleport(newLoc);

                    fireball.getWorld().spawnParticle(Particle.FLAME, fireball.getLocation(), 3, 0.2, 0.2, 0.2, 0.05);
                    fireball.getWorld().spawnParticle(Particle.LAVA, fireball.getLocation(), 2);

                    for (Entity entity : fireball.getWorld().getNearbyEntities(fireball.getLocation(), 1.5, 1.5, 1.5)) {
                        if (entity instanceof Player player) {
                            player.damage(8);
                            player.setFireTicks(100);
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                            deathCauseMap.put(player.getUniqueId(), "Fireball de Abeja Infernal");

                            explodeCustomFireball(fireball);
                            cancel();
                            return;
                        }
                    }

                    if (fireball.getLocation().getBlock().getType().isSolid()) {
                        explodeCustomFireball(fireball);
                        cancel();
                        return;
                    }
                }

                ticks++;

                if (ticks > 200) {
                    explodeCustomFireball(fireball);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void explodeCustomFireball(BlockDisplay fireball) {
        Location loc = fireball.getLocation();
        fireball.getWorld().spawnParticle(Particle.EXPLOSION, loc, 3, 1, 1, 1, 0);
        fireball.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);

        for (Entity entity : fireball.getWorld().getNearbyEntities(loc, 4, 4, 4)) {
            if (entity instanceof Player player) {
                player.damage(20);
                player.setFireTicks(300);
            }
        }

        fireball.remove();
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
                    bee.getWorld().spawnParticle(Particle.LAVA, particleLoc, 1);
                    bee.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1);
                }

                bee.getWorld().playSound(bee.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 5.0f, 0.5f);
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

    // Ataque: Rayos de Fuego
    private void executeFireRays(Bee bee) {
        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 5.0f, 0.5f);

        List<Player> nearbyPlayers = bee.getWorld().getNearbyEntities(bee.getLocation(), 30, 30, 30).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .collect(Collectors.toList());

        if (nearbyPlayers.isEmpty()) return;

        Collections.shuffle(nearbyPlayers);
        int targetCount = Math.min(random.nextInt(4) + 2, nearbyPlayers.size());
        List<Player> targets = nearbyPlayers.subList(0, targetCount);

        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, 3.0f, 0.8f);

        for (Player target : targets) {
            createFireRay(bee, target);
        }
    }

    private void createFireRay(Bee bee, Player target) {
        new BukkitRunnable() {
            int ticks = 0;
            int damageTicks = 0;
            final int DURATION = 100;

            @Override
            public void run() {
                if (ticks >= DURATION || bee.isDead() || !bee.isValid() || !target.isOnline()) {
                    cancel();
                    return;
                }

                Location beeLocation = bee.getLocation().add(0, 1, 0);
                Location targetLocation = target.getEyeLocation();

                Vector direction = targetLocation.toVector().subtract(beeLocation.toVector());
                double distance = direction.length();
                direction.normalize();

                double currentDistance = ticks < 20 ? (distance * ticks / 20.0) : distance;

                for (double d = 0; d < currentDistance; d += 0.3) {
                    Location particleLoc = beeLocation.clone().add(direction.clone().multiply(d));

                    Particle.DustOptions dustOptions = new Particle.DustOptions(Color.ORANGE, 1.0f);
                    bee.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0.1, 0.1, 0.1, 0, dustOptions);

                    if (random.nextDouble() < 0.3) {
                        bee.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0.05, 0.05, 0.05, 0);
                    }
                }

                if (ticks % 20 == 0) {
                    bee.getWorld().playSound(beeLocation, Sound.BLOCK_FIRE_AMBIENT, 2.0f, 1.5f);
                    target.playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 2.0f);
                }

                if (ticks >= 20 && ticks % 20 == 0) {
                    damageTicks++;

                    boolean isBlocking = target.isBlocking();

                    if (!isBlocking) {
                        target.damage(3.0);
                        target.setFireTicks(300);

                        target.getWorld().spawnParticle(Particle.FLAME, target.getLocation(), 10, 0.5, 1.0, 0.5, 0.1);
                        target.getWorld().spawnParticle(Particle.LAVA, target.getLocation(), 5);

                        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1.0f, 1.0f);
                    } else {
                        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 5, 0.5, 0.5, 0.5, 0.1);
                        target.playSound(target.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void executeExplosiveAttack(Bee bee) {
        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 5.0f, 2f);

        Location beeLocation = bee.getLocation();
        List<Vector> directions = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(((double) 360 / 8) * i);
            double x = Math.cos(angle);
            double z = Math.sin(angle);
            directions.add(new Vector(x, -0.5, z).normalize());
        }

        for (Vector direction : directions) {
            BlockDisplay blockDisplay = bee.getWorld().spawn(beeLocation.clone().add(0, 1, 0), BlockDisplay.class);
            blockDisplay.setBlock(Bukkit.createBlockData(Material.NETHER_WART_BLOCK));
            blockDisplay.setGravity(false);
            blockDisplay.setGlowing(true);
            blockDisplay.setGlowColorOverride(Color.RED);

            new BukkitRunnable() {
                int ticks = 0;
                final int MAX_TICKS = 100;

                @Override
                public void run() {
                    if (ticks++ > MAX_TICKS || blockDisplay.isDead()) {
                        if (!blockDisplay.isDead()) {
                            explode(blockDisplay);
                            blockDisplay.remove();
                        }
                        cancel();
                        return;
                    }

                    Location newLocation = blockDisplay.getLocation().add(direction.clone().multiply(0.4));
                    if (!isLocationSafe(newLocation)) {
                        blockDisplay.remove();
                        cancel();
                        return;
                    }

                    if (newLocation.getBlock().getType().isSolid()) {
                        explode(blockDisplay);
                        blockDisplay.remove();
                        cancel();
                        return;
                    }

                    for (Entity entity : blockDisplay.getWorld().getNearbyEntities(blockDisplay.getLocation(), 1.5, 1.5, 1.5)) {
                        if (entity instanceof Player player) {
                            explode(blockDisplay);
                            player.damage(7);
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 2));
                            deathCauseMap.put(player.getUniqueId(), "Aguijón Explosivos de Abeja Infernal");
                            blockDisplay.remove();
                            cancel();
                            return;
                        }
                    }

                    blockDisplay.teleport(newLocation);
                    blockDisplay.getWorld().spawnParticle(Particle.LAVA, blockDisplay.getLocation(), 1);
                    blockDisplay.getWorld().spawnParticle(Particle.SMOKE, blockDisplay.getLocation(), 1);
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    private void explode(Entity explosive) {
        explosive.getWorld().spawnParticle(Particle.EXPLOSION, explosive.getLocation(), 1);
        explosive.getWorld().playSound(explosive.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 5.0f, 1.2f);

        for (Entity entity : explosive.getWorld().getNearbyEntities(explosive.getLocation(), 5, 5, 5)) {
            if (entity instanceof Player player) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 150, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 2));
            }
        }

        for (Entity nearbyEntity : explosive.getWorld().getNearbyEntities(explosive.getLocation(), 5, 5, 5)) {
            if (nearbyEntity instanceof LivingEntity livingEntity) {
                livingEntity.damage(12);
            }
        }
    }

    private void executeFireCircle(Bee bee) {
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

                bee.getWorld().playSound(center, Sound.BLOCK_FIRE_AMBIENT, 1f, 0.5f);

                if (pulseCount % 3 == 0) {
                    bee.getWorld().playSound(center, Sound.ENTITY_EVOKER_CAST_SPELL, 1.5f, 1.2f);
                    bee.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, center, 10, 0.5, 1.0, 0.5, 0.1);
                    bee.getWorld().spawnParticle(Particle.ENCHANT, center, 5, 0.5, 1.0, 0.5, 0.5);
                }
                pulseCount++;

                for (double angle = 0; angle < 360; angle += 10) {
                    double radians = Math.toRadians(angle);
                    Location particleLoc = center.clone().add(
                            Math.cos(radians) * radius, 0, Math.sin(radians) * radius
                    );

                    bee.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
                    bee.getWorld().spawnParticle(Particle.LAVA, particleLoc, 1);

                    if (particleLoc.getBlock().getType().isAir()) {
                        particleLoc.getBlock().setType(Material.FIRE);
                    }

                    for (Entity entity : bee.getWorld().getNearbyEntities(particleLoc, 1, 1, 1)) {
                        if (entity instanceof Player player) {
                            player.setFireTicks(Integer.MAX_VALUE);
                        }
                    }
                }

                radius += 0.5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void executeSummonGuardianVexes(Bee bee) {
        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 5.0f, 0.5f);

        for (int i = 0; i < 4; i++) {
            Vex vex = (Vex) bee.getWorld().spawnEntity(bee.getLocation(), EntityType.VEX);
            vex.setCustomName(ChatColor.RED + "Fantasma Guardian " + (i + 1));
            vex.setCustomNameVisible(false);
            vex.setLifeTicks(1200);
            vex.getPersistentDataContainer().set(guardianVexKey, PersistentDataType.BYTE, (byte) 1);

            vex.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
            vex.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));

            Player target = findTarget(bee);
            if (target != null) {
                vex.setTarget(target);
            }
        }
    }

    private void executeMeleeAttack(Bee bee) {
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
                        target.setFireTicks(Integer.MAX_VALUE);
                        bee.getWorld().spawnParticle(Particle.FLAME, target.getLocation(), 20);
                        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_STING, 1f, 0.1f);
                    }

                    bee.setTarget(null);
                }
            }.runTaskLater(plugin, 40L);
        }
    }

    // ==================== EVENTOS ====================

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Bee bee) || !isHellishBee(bee)) return;

        if (bee.getHealth() - event.getFinalDamage() <= 0) {
            event.setCancelled(true);
            bee.setHealth(0);
            executeBeeDeath(bee);
        }
    }

    @EventHandler
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Bee bee) || !isHellishBee(bee)) return;

        if (event.getDamager() instanceof Projectile) {
            event.setDamage(event.getDamage() * 0.5);
        }
    }

    @EventHandler
    public void onBeeAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Bee bee) || !isHellishBee(bee)) return;

        event.setCancelled(true);

        if (event.getEntity() instanceof Player player) {
            player.damage(4);
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));

            bee.playEffect(EntityEffect.ENTITY_POOF);
            bee.getWorld().spawnParticle(Particle.SMOKE, bee.getLocation(), 10, 0.5, 1.0, 0.5, 0.1);
            bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_STING, 1f, 0.1f);

            deathCauseMap.put(player.getUniqueId(), "Abeja Infernal");

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
        if (event.getDamager() instanceof Bee bee && isHellishBee(bee)) {
            bee.setHasStung(false);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Bee bee) || !isHellishBee(bee)) return;

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

        player.getNearbyEntities(30, 30, 30).stream()
                .filter(e -> e instanceof Bee)
                .map(e -> (Bee) e)
                .filter(bee -> bee.getCustomName() != null && bee.getCustomName().contains("Abeja Infernal"))
                .forEach(bee -> {
                    if (!isHellishBee(bee)) {
                        transformToHellishBee(bee);
                    } else {
                        // VERIFICACIÓN ESPECÍFICA: Solo reiniciar si realmente no tiene comportamiento
                        UUID beeId = bee.getUniqueId();
                        if (!activeBehaviors.containsKey(beeId) || activeBehaviors.get(beeId).isCancelled()) {
                            plugin.getLogger().info("Player movement: Restarting behavior for dormant hellish bee: " + beeId);
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
                        .filter(bee -> bee.getCustomName() != null && bee.getCustomName().contains("Abeja Infernal"))
                        .filter(bee -> bee.getPersistentDataContainer().has(hellishBeeKey, PersistentDataType.BYTE))
                        .forEach(bee -> {
                            plugin.getLogger().info("World load: Found existing Hellish Bee: " + bee.getUniqueId());
                            if (canProcessBee(bee)) {
                                transformToHellishBee(bee);
                            }
                        });
            }
        }.runTaskLater(plugin, 100L);
    }

    // ==================== REINICIO SEGURO ====================

    private void restartBehaviorSafely(Bee bee) {
        UUID beeId = bee.getUniqueId();

        // Solo reiniciar si no está siendo procesado Y está completamente inicializado
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
        if (!bee.getPersistentDataContainer().has(hellishBeeKey, PersistentDataType.BYTE)) return;

        int amount = random.nextInt(2) + 1;
        for (int i = 0; i < amount; i++) {
            bee.getWorld().dropItemNaturally(bee.getLocation(), bootNetheriteEssence.createBootNetheriteEssence());
        }

        playDeathEffects(bee.getLocation());
        cleanupBee(bee.getUniqueId());
        bee.getPersistentDataContainer().remove(hellishBeeKey);
        bee.remove();
    }

    private void playDeathEffects(Location location) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.stopSound("custom.music2_skybattle", SoundCategory.RECORDS);

            if (player.getWorld().equals(location.getWorld()) &&
                    player.getLocation().distanceSquared(location) <= 1600) {

                player.playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0f, 0.8f);
                player.sendMessage(ChatColor.of("#FFDD95") + "" + ChatColor.BOLD + "\u06de" + " " +
                        ChatColor.of("#FFDD95") + "La" + " " +
                        ChatColor.DARK_RED + "" + ChatColor.BOLD + "Abeja Infernal" + " " +
                        ChatColor.of("#FFDD95") + "ha sido derrotada."
                );
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

    private boolean isHellishBee(Bee bee) {
        return bee != null && bee.isValid() && !bee.isDead() &&
                bee.getCustomName() != null && bee.getCustomName().contains("Abeja Infernal") &&
                bee.getPersistentDataContainer().has(hellishBeeKey, PersistentDataType.BYTE);
    }

    private boolean isVectorSafe(Vector vector) {
        return !Double.isNaN(vector.getX()) && !Double.isNaN(vector.getY()) && !Double.isNaN(vector.getZ()) &&
                !Double.isInfinite(vector.getX()) && !Double.isInfinite(vector.getY()) && !Double.isInfinite(vector.getZ());
    }

    private boolean isLocationSafe(Location location) {
        return Double.isFinite(location.getX()) && Double.isFinite(location.getY()) && Double.isFinite(location.getZ());
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