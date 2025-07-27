package Dificultades.CustomMobs;

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
import org.bukkit.inventory.ItemRarity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class QueenBeeHandler implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> activeBehaviors = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> isAttacking = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> isRegenerating = new ConcurrentHashMap<>();
    private final Map<UUID, String> deathCauseMap = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private final NamespacedKey queenBeeKey;

    // Control estricto de procesamiento - CLAVE PARA EVITAR DUPLICACIONES
    private final Set<UUID> processingBees = ConcurrentHashMap.newKeySet();
    private final Set<UUID> fullyInitializedBees = ConcurrentHashMap.newKeySet();

    // SISTEMA INDEPENDIENTE DE BOSSBAR
    private BukkitRunnable globalBossBarManager;
    private static final double BOSSBAR_RANGE = 100.0;

    public enum AttackType {
        SPIKES, EXPLOSIVE_ATTACK, POISON_CIRCLE, SUMMON_BEES, MELEE, FLIGHT_ATTACK, REGENERATION
    }

    public QueenBeeHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.queenBeeKey = new NamespacedKey(plugin, "queen_bee");

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
                        if (isQueenBee(bee)) {
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
                    ChatColor.YELLOW + "Abeja Reina",
                    BarColor.YELLOW,
                    BarStyle.SOLID
            );
            bossBar.setVisible(true);
            bossBars.put(beeId, bossBar);
            plugin.getLogger().info("Created new BossBar for Queen Bee: " + beeId);
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

    public Bee spawnQueenBee(Location location) {
        Bee bee = (Bee) Objects.requireNonNull(location.getWorld()).spawnEntity(location, EntityType.BEE);

        // DELAY CRÍTICO: Esperar 1 tick antes de inicializar para evitar conflictos
        new BukkitRunnable() {
            @Override
            public void run() {
                initializeQueenBee(bee);
            }
        }.runTaskLater(plugin, 1L);

        return bee;
    }

    public void transformToQueenBee(Bee bee) {
        if (!canProcessBee(bee)) return;

        // DELAY CRÍTICO: Esperar 1 tick antes de inicializar
        new BukkitRunnable() {
            @Override
            public void run() {
                if (bee.isValid() && !bee.isDead()) {
                    initializeQueenBee(bee);
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

    private void initializeQueenBee(Bee bee) {
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

            if (!bee.getPersistentDataContainer().has(queenBeeKey, PersistentDataType.BYTE)) {
                setupBeeAttributes(bee);
            }

            isAttacking.put(beeId, false);
            isRegenerating.put(beeId, false);

            startBehavior(bee);

            fullyInitializedBees.add(beeId);

            plugin.getLogger().info("Initialized Queen Bee: " + beeId);

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
        bee.setCustomName(ChatColor.GOLD + "Abeja Reina");
        bee.setCustomNameVisible(true);
        bee.setSilent(true);
        bee.setRemoveWhenFarAway(false);
        bee.setAnger(999999);
        bee.setCannotEnterHiveTicks(Integer.MAX_VALUE);
        bee.setHasStung(false);
        bee.setAI(true);

        Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_FOLLOW_RANGE)).setBaseValue(50);
        Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(500.0);
        Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.35);
        Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_SCALE)).setBaseValue(3);
        bee.setHealth(500.0);

        bee.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        bee.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1, false, false));

        bee.getPersistentDataContainer().set(queenBeeKey, PersistentDataType.BYTE, (byte) 1);

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

        plugin.getLogger().info("Started behavior for Queen Bee: " + beeId);
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

        if (bee.getHealth() < 250.0 && !isRegenerating.getOrDefault(beeId, false) && random.nextDouble() <= 0.10) {
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
            bee.getWorld().spawnParticle(Particle.GLOW, bee.getLocation(), 5);
            bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_LOOP, 0.3f, 1.5f);
        }
    }

    private void executeRandomAttack(Bee bee) {
        UUID beeId = bee.getUniqueId();

        isAttacking.put(beeId, true);

        AttackType[] attacks = {AttackType.SPIKES, AttackType.EXPLOSIVE_ATTACK, AttackType.POISON_CIRCLE,
                AttackType.SUMMON_BEES, AttackType.MELEE, AttackType.FLIGHT_ATTACK};
        AttackType selected = attacks[random.nextInt(attacks.length)];

        switch (selected) {
            case SPIKES -> executeSpikes(bee);
            case EXPLOSIVE_ATTACK -> executeExplosiveAttack(bee);
            case POISON_CIRCLE -> executePoisonCircle(bee);
            case SUMMON_BEES -> executeSummonBees(bee);
            case MELEE -> executeMelee(bee);
            case FLIGHT_ATTACK -> executeFlightAttack(bee);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                isAttacking.put(beeId, false);
            }
        }.runTaskLater(plugin, 100L);
    }

    // ==================== ATAQUES ESPECÍFICOS DE QUEEN BEE ====================

    private void executeSpikes(Bee bee) {
        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 5.0f, 0.1f);

        Location beeLocation = bee.getLocation();
        List<Vector> directions = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(((double) 360 / 8) * i);
            double x = Math.cos(angle);
            double z = Math.sin(angle);
            directions.add(new Vector(x, -0.3, z).normalize());
        }

        for (Vector direction : directions) {
            BlockDisplay spike = bee.getWorld().spawn(beeLocation, BlockDisplay.class);
            spike.setBlock(Bukkit.createBlockData(Material.POINTED_DRIPSTONE));
            spike.setGravity(false);
            spike.setGlowing(true);

            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (ticks++ > 40 || spike.isDead()) {
                        spike.remove();
                        cancel();
                        return;
                    }

                    Location newLocation = spike.getLocation().add(direction.clone().multiply(0.3));
                    if (!isLocationSafe(newLocation) || newLocation.getBlock().getType().isSolid()) {
                        spike.remove();
                        cancel();
                        return;
                    }

                    spike.teleport(newLocation);

                    for (Entity entity : spike.getWorld().getNearbyEntities(spike.getLocation(), 1, 1, 1)) {
                        if (entity instanceof Player player) {
                            player.damage(8);
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.1f);
                            deathCauseMap.put(player.getUniqueId(), "Aguijón de Abeja Reina");
                            spike.remove();
                            cancel();
                            return;
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    private void executeExplosiveAttack(Bee bee) {
        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 5.0f, 0.1f);

        Location beeLocation = bee.getLocation();
        List<Vector> directions = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(((double) 360 / 8) * i);
            double x = Math.cos(angle);
            double z = Math.sin(angle);
            directions.add(new Vector(x, -0.3, z).normalize());
        }

        for (Vector direction : directions) {
            BlockDisplay blockDisplay = bee.getWorld().spawn(beeLocation, BlockDisplay.class);
            blockDisplay.setBlock(Bukkit.createBlockData(Material.POINTED_DRIPSTONE));
            blockDisplay.setGravity(false);
            blockDisplay.setGlowing(true);
            blockDisplay.setGlowColorOverride(Color.GREEN);

            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (ticks++ > 40 || blockDisplay.isDead()) {
                        blockDisplay.remove();
                        cancel();
                        return;
                    }

                    Location newLocation = blockDisplay.getLocation().add(direction.clone().multiply(0.3));
                    if (!isLocationSafe(newLocation) || newLocation.getBlock().getType().isSolid()) {
                        explode(blockDisplay);
                        blockDisplay.remove();
                        cancel();
                        return;
                    }

                    for (Entity entity : blockDisplay.getWorld().getNearbyEntities(blockDisplay.getLocation(), 1, 1, 1)) {
                        if (entity instanceof Player player) {
                            explode(blockDisplay);
                            player.damage(3);
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 2));
                            deathCauseMap.put(player.getUniqueId(), "Aguijón Explosivos de Abeja Reina");
                            blockDisplay.remove();
                            cancel();
                            return;
                        }
                    }

                    blockDisplay.teleport(newLocation);
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
                livingEntity.damage(6);
            }
        }
    }

    private void executePoisonCircle(Bee bee) {
        Location center = bee.getLocation();

        new BukkitRunnable() {
            double radius = 0.5;

            @Override
            public void run() {
                if (radius > 15 || bee.isDead() || !bee.isValid()) {
                    cancel();
                    return;
                }

                bee.getWorld().playSound(center, Sound.ENTITY_BEE_LOOP, 5f, 0.1f);

                for (double angle = 0; angle < 360; angle += 10) {
                    double radians = Math.toRadians(angle);
                    Location particleLoc = center.clone().add(
                            Math.cos(radians) * radius, 0, Math.sin(radians) * radius
                    );

                    bee.getWorld().spawnParticle(Particle.WITCH, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);

                    for (Entity entity : bee.getWorld().getNearbyEntities(particleLoc, 1, 1, 1)) {
                        if (entity instanceof Player player) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 4));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 1));
                        }
                    }
                }

                radius += 0.5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void executeSummonBees(Bee bee) {
        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 5.0f, 0.1f);

        for (int i = 0; i < 4; i++) {
            Bee angryBee = (Bee) bee.getWorld().spawnEntity(bee.getLocation(), EntityType.BEE);
            angryBee.setAnger(2200);
            angryBee.setCannotEnterHiveTicks(Integer.MAX_VALUE);
            angryBee.setCustomName(ChatColor.GOLD + "Abeja Guardiana " + (i + 1));
            angryBee.setCustomNameVisible(true);

            Player target = findTarget(bee);
            if (target != null) {
                angryBee.setTarget(target);
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    angryBee.remove();
                }
            }.runTaskLater(plugin, 1200L);
        }
    }

    private void executeFlightAttack(Bee bee) {
        // Fase 1: Subir
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (ticks < 20) {
                    bee.setVelocity(new Vector(0, 0.5, 0));
                } else {
                    bee.setVelocity(new Vector(0, 0, 0));
                    startDescent(bee);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 1.0f, 0.8f);
        bee.getWorld().spawnParticle(Particle.CLOUD, bee.getLocation(), 10, 0.5, 1.0, 0.5, 0.1);
    }

    private void startDescent(Bee bee) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (ticks < 20) {
                    bee.setVelocity(new Vector(0, -0.5, 0));
                } else {
                    bee.setVelocity(new Vector(0, 0, 0));
                    performGroundAttack(bee);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 1L);
    }

    private void performGroundAttack(Bee bee) {
        executeSpikes(bee);

        new BukkitRunnable() {
            @Override
            public void run() {
                executeExplosiveAttack(bee);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        executePoisonCircle(bee);

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                executeSummonBees(bee);
                                startFlightFinish(bee);
                            }
                        }.runTaskLater(plugin, 20L);
                    }
                }.runTaskLater(plugin, 20L);
            }
        }.runTaskLater(plugin, 20L);
    }

    private void startFlightFinish(Bee bee) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (ticks < 10) {
                    bee.setVelocity(new Vector(0, 0.5, 0));
                } else {
                    bee.setVelocity(new Vector(0, 0, 0));
                    finishFlightAttack(bee);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 1L);
    }

    private void finishFlightAttack(Bee bee) {
        bee.getWorld().spawnParticle(Particle.CLOUD, bee.getLocation(), 10, 0.5, 1.0, 0.5, 0.1);
        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 1f, 0.5f);
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
                        target.damage(4);
                        bee.getWorld().spawnParticle(Particle.GLOW, target.getLocation(), 20);
                        bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_STING, 1f, 0.1f);
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
                    bee.getWorld().spawnParticle(Particle.GLOW, particleLoc, 1);
                }

                bee.getWorld().playSound(bee.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 5.0f, 2f);
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
        if (!(event.getEntity() instanceof Bee bee) || !isQueenBee(bee)) return;

        if (bee.getHealth() - event.getFinalDamage() <= 0) {
            event.setCancelled(true);
            bee.setHealth(0);
            executeBeeDeath(bee);
        }
    }

    @EventHandler
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Bee bee) || !isQueenBee(bee)) return;

        if (event.getDamager() instanceof Projectile) {
            event.setDamage(event.getDamage() * 0.5);
        }
    }

    @EventHandler
    public void onBeeAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Bee bee) || !isQueenBee(bee)) return;

        event.setCancelled(true);

        if (event.getEntity() instanceof Player player) {
            player.damage(4);
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));

            bee.playEffect(EntityEffect.ENTITY_POOF);
            bee.getWorld().spawnParticle(Particle.SMOKE, bee.getLocation(), 10, 0.5, 1.0, 0.5, 0.1);
            bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_STING, 1f, 0.1f);

            deathCauseMap.put(player.getUniqueId(), "Abeja Reina");

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
        if (event.getDamager() instanceof Bee bee && isQueenBee(bee)) {
            bee.setHasStung(false);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Bee bee) || !isQueenBee(bee)) return;

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
                .filter(bee -> bee.getCustomName() != null && bee.getCustomName().contains("Abeja Reina"))
                .forEach(bee -> {
                    if (!isQueenBee(bee)) {
                        transformToQueenBee(bee);
                    } else {
                        // VERIFICACIÓN ESPECÍFICA: Solo reiniciar si realmente no tiene comportamiento
                        UUID beeId = bee.getUniqueId();
                        if (!activeBehaviors.containsKey(beeId) || activeBehaviors.get(beeId).isCancelled()) {
                            plugin.getLogger().info("Player movement: Restarting behavior for dormant queen bee: " + beeId);
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
                        .filter(bee -> bee.getCustomName() != null && bee.getCustomName().contains("Abeja Reina"))
                        .filter(bee -> bee.getPersistentDataContainer().has(queenBeeKey, PersistentDataType.BYTE))
                        .forEach(bee -> {
                            plugin.getLogger().info("World load: Found existing Queen Bee: " + bee.getUniqueId());
                            if (canProcessBee(bee)) {
                                transformToQueenBee(bee);
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
        if (!bee.getPersistentDataContainer().has(queenBeeKey, PersistentDataType.BYTE)) return;

        ItemStack sting = createAguijonAbejaReina();
        bee.getWorld().dropItemNaturally(bee.getLocation(), sting);

        playDeathEffects(bee.getLocation());
        cleanupBee(bee.getUniqueId());
        bee.getPersistentDataContainer().remove(queenBeeKey);
        bee.remove();
    }

    private void playDeathEffects(Location location) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.stopSound("custom.music2_skybattle", SoundCategory.RECORDS);

            if (player.getWorld().equals(location.getWorld()) &&
                    player.getLocation().distanceSquared(location) <= 2500) {

                player.playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0f, 2.0f);
                player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "\u06de" + " " +
                        ChatColor.GOLD + "La " + ChatColor.BOLD + "Abeja Reina" + " " +
                        ChatColor.GOLD + "ha sido derrotada.");
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

    private boolean isQueenBee(Bee bee) {
        return bee != null && bee.isValid() && !bee.isDead() &&
                bee.getCustomName() != null && bee.getCustomName().contains("Abeja Reina") &&
                bee.getPersistentDataContainer().has(queenBeeKey, PersistentDataType.BYTE);
    }

    private boolean isVectorSafe(Vector vector) {
        return !Double.isNaN(vector.getX()) && !Double.isNaN(vector.getY()) && !Double.isNaN(vector.getZ()) &&
                !Double.isInfinite(vector.getX()) && !Double.isInfinite(vector.getY()) && !Double.isInfinite(vector.getZ());
    }

    private boolean isLocationSafe(Location location) {
        return Double.isFinite(location.getX()) && Double.isFinite(location.getY()) && Double.isFinite(location.getZ());
    }

    public static ItemStack createAguijonAbejaReina() {
        ItemStack aguijon = new ItemStack(Material.YELLOW_DYE);
        ItemMeta meta = aguijon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Aguijón de la Abeja Reina");
            meta.setCustomModelData(2);
            meta.setRarity(ItemRarity.EPIC);
            aguijon.setItemMeta(meta);
        }
        return aguijon;
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