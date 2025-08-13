package Dificultades.CustomMobs;

import Dificultades.Features.UltraWitherDefeatedEvent;
import TitleListener.SuccessNotification;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import net.md_5.bungee.api.ChatColor;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class UltraWitherBossHandler implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, BukkitRunnable> activeBehaviors = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> isAttacking = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> isPhaseTwo = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> isSpecialAttackActive = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> isDrainActive = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> isInstakillActive = new ConcurrentHashMap<>();
    private final Map<UUID, String> deathCauseMap = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private final NamespacedKey ultraWitherKey;
    private final SuccessNotification successNotification;

    // Control estricto de procesamiento - CLAVE PARA EVITAR DUPLICACIONES
    private final Set<UUID> processingWithers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> fullyInitializedWithers = ConcurrentHashMap.newKeySet();

    // Estados específicos de ataques
    private final Map<UUID, Integer> instakillAttempts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAttackTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDrainAttackTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastInstakillAttackTime = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> activeDrainTasks = new ConcurrentHashMap<>();
    private final Set<UUID> deathExecuted = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<UUID>> playerAttacks = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> shiftSoundTasks = new ConcurrentHashMap<>();

    private static final int SKULL_EFFECT_RADIUS = 10;
    private static final int SKULL_EFFECT_DURATION = 200;
    private static final int RING_ATTACK_DURATION = 300;

    public enum AttackType {
        WITHER_SKULL, SUMMON_SKELETONS, INSTAKILL, DRAIN, SKULL_EFFECT, SKULL_BARRAGE, RING_ATTACK
    }

    public UltraWitherBossHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.ultraWitherKey = new NamespacedKey(plugin, "ultra_wither");
        this.successNotification = new SuccessNotification(plugin);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ==================== SPAWN Y TRANSFORMACIÓN ====================

    public Wither spawnUltraWither(Location location) {
        spawnCooldownEffects(location);

        new BukkitRunnable() {
            @Override
            public void run() {
                Wither wither = (Wither) location.getWorld().spawnEntity(location, EntityType.WITHER);

                // DELAY CRÍTICO: Esperar 1 tick antes de inicializar para evitar conflictos
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        initializeUltraWither(wither);
                    }
                }.runTaskLater(plugin, 1L);
            }
        }.runTaskLater(plugin, 200L);

        return null;
    }

    public void transformToUltraWither(Wither wither) {
        if (!canProcessWither(wither)) return;

        // DELAY CRÍTICO: Esperar 1 tick antes de inicializar
        new BukkitRunnable() {
            @Override
            public void run() {
                if (wither.isValid() && !wither.isDead()) {
                    initializeUltraWither(wither);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    private boolean canProcessWither(Wither wither) {
        UUID witherId = wither.getUniqueId();

        // VERIFICACIÓN CRÍTICA: Si ya está completamente inicializado, NO procesar
        if (fullyInitializedWithers.contains(witherId)) {
            return false;
        }

        // Si ya está siendo procesado, NO procesar
        if (processingWithers.contains(witherId)) {
            return false;
        }

        return true;
    }

    private void initializeUltraWither(Wither wither) {
        UUID witherId = wither.getUniqueId();

        // VERIFICACIÓN FINAL: Doble check para evitar duplicaciones
        if (fullyInitializedWithers.contains(witherId) || processingWithers.contains(witherId)) {
            return;
        }

        // Marcar como siendo procesado INMEDIATAMENTE
        processingWithers.add(witherId);

        try {
            // Limpiar cualquier estado anterior
            cleanupWither(witherId);

            // Aplicar atributos solo si no los tiene ya
            if (!wither.getPersistentDataContainer().has(ultraWitherKey, PersistentDataType.BYTE)) {
                setupWitherAttributes(wither);
            }

            // Inicializar estados
            isAttacking.put(witherId, false);
            isPhaseTwo.put(witherId, false);
            isSpecialAttackActive.put(witherId, false);
            isDrainActive.put(witherId, false);
            isInstakillActive.put(witherId, false);
            playerAttacks.put(witherId, ConcurrentHashMap.newKeySet());

            // Iniciar comportamiento
            startBehavior(wither);

            // MARCAR COMO COMPLETAMENTE INICIALIZADO
            fullyInitializedWithers.add(witherId);

            plugin.getLogger().info("Initialized Ultra Wither: " + witherId);

        } finally {
            // Remover de procesamiento después de un delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    processingWithers.remove(witherId);
                }
            }.runTaskLater(plugin, 20L);
        }
    }

    private void setupWitherAttributes(Wither wither) {
        wither.setCustomName(ChatColor.DARK_PURPLE + "Corrupted Wither Boss");
        Objects.requireNonNull(wither.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(1000.0);
        wither.setHealth(1000.0);
        Objects.requireNonNull(wither.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.4);
        Objects.requireNonNull(wither.getAttribute(Attribute.GENERIC_FOLLOW_RANGE)).setBaseValue(50);
        wither.getPersistentDataContainer().set(ultraWitherKey, PersistentDataType.BYTE, (byte) 1);
        wither.setRemoveWhenFarAway(false);
        wither.setCustomNameVisible(false);

        wither.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, wither.getLocation(), 1);
        wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
    }

    // ==================== COMPORTAMIENTO PRINCIPAL ====================

    private void startBehavior(Wither wither) {
        UUID witherId = wither.getUniqueId();

        // VERIFICACIÓN CRÍTICA: Solo un comportamiento por wither
        if (activeBehaviors.containsKey(witherId)) {
            BukkitRunnable existing = activeBehaviors.get(witherId);
            if (existing != null && !existing.isCancelled()) {
                return; // Ya tiene comportamiento activo
            }
        }

        BukkitRunnable behavior = new BukkitRunnable() {
            @Override
            public void run() {
                if (wither.isDead() || !wither.isValid()) {
                    cancel();
                    activeBehaviors.remove(witherId);
                    return;
                }

                executeBehaviorTick(wither);
            }
        };

        behavior.runTaskTimer(plugin, 0L, 60L); // Cada 3 segundos
        activeBehaviors.put(witherId, behavior);

        plugin.getLogger().info("Started behavior for Ultra Wither: " + witherId);
    }

    private void executeBehaviorTick(Wither wither) {
        UUID witherId = wither.getUniqueId();

        // VERIFICACIÓN CRÍTICA: Solo un ataque a la vez
        if (isAttacking.getOrDefault(witherId, false)) {
            return; // Ya está atacando
        }

        // Verificar transición a fase 2
        if (!isPhaseTwo.getOrDefault(witherId, false) && wither.getHealth() <= 500.0) {
            startPhaseTwo(wither);
            return;
        }

        // Si hay un ataque especial activo, no hacer nada más
        if (isSpecialAttackActive.getOrDefault(witherId, false)) {
            return;
        }

        // Ejecutar ataque aleatorio
        executeRandomAttack(wither);
    }

    private void executeRandomAttack(Wither wither) {
        UUID witherId = wither.getUniqueId();

        // MARCAR COMO ATACANDO PARA PREVENIR DUPLICADOS
        isAttacking.put(witherId, true);

        boolean isPhase2 = isPhaseTwo.getOrDefault(witherId, false);
        AttackType[] attacks = isPhase2 ?
                new AttackType[]{AttackType.WITHER_SKULL, AttackType.SUMMON_SKELETONS, AttackType.INSTAKILL,
                        AttackType.DRAIN, AttackType.SKULL_EFFECT, AttackType.SKULL_BARRAGE, AttackType.RING_ATTACK} :
                new AttackType[]{AttackType.WITHER_SKULL, AttackType.SUMMON_SKELETONS, AttackType.INSTAKILL,
                        AttackType.SKULL_EFFECT, AttackType.SKULL_BARRAGE};

        AttackType selected = attacks[random.nextInt(attacks.length)];

        // CONTROL CRÍTICO: Evitar superposición de instakill y drain
        if (selected == AttackType.INSTAKILL && isDrainActive.getOrDefault(witherId, false)) {
            selected = AttackType.WITHER_SKULL; // Cambiar a ataque básico
        } else if (selected == AttackType.DRAIN && isInstakillActive.getOrDefault(witherId, false)) {
            selected = AttackType.WITHER_SKULL; // Cambiar a ataque básico
        }

        switch (selected) {
            case WITHER_SKULL -> executeWitherSkullAttack(wither);
            case SUMMON_SKELETONS -> executeSummonWitherSkeletons(wither);
            case INSTAKILL -> executeInstakillAttack(wither);
            case DRAIN -> executeDrainAttack(wither);
            case SKULL_EFFECT -> executeSkullEffectAttack(wither);
            case SKULL_BARRAGE -> executeWitherSkullBarrage(wither);
            case RING_ATTACK -> executeRingAttack(wither);
        }

        // Resetear estado de ataque después de un delay
        new BukkitRunnable() {
            @Override
            public void run() {
                isAttacking.put(witherId, false);
            }
        }.runTaskLater(plugin, 100L); // 5 segundos
    }

    // ==================== ATAQUES ESPECÍFICOS ====================

    private void executeWitherSkullAttack(Wither wither) {
        isSpecialAttackActive.put(wither.getUniqueId(), true);
        wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.8f);

        List<Player> targets = getRandomPlayers(wither, 2, 5).stream()
                .filter(this::shouldAffectPlayer)
                .collect(Collectors.toList());

        for (Player target : targets) {
            WitherSkull skull = wither.launchProjectile(WitherSkull.class);
            skull.setMetadata("UltraWitherSkull", new FixedMetadataValue(plugin, true));
            skull.setGlowing(true);

            skull.setMetadata("WitherEffect", new FixedMetadataValue(plugin, PotionEffectType.WITHER));
            skull.setMetadata("WitherEffectDuration", new FixedMetadataValue(plugin, 200));
            skull.setMetadata("WitherEffectAmplifier", new FixedMetadataValue(plugin, 4));

            if (random.nextBoolean()) {
                skull.setCharged(true);
                skull.setMetadata("Explosive", new FixedMetadataValue(plugin, true));
            }

            Vector direction = target.getEyeLocation().subtract(skull.getLocation()).toVector().normalize();
            skull.setVelocity(direction.multiply(1.2));
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                isSpecialAttackActive.put(wither.getUniqueId(), false);
            }
        }.runTaskLater(plugin, 40L);
    }

    private void executeWitherSkullBarrage(Wither wither) {
        isSpecialAttackActive.put(wither.getUniqueId(), true);
        wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_SHOOT, 3.0f, 0.7f);

        List<Player> targets = getRandomPlayers(wither, 3, 8);

        for (Player target : targets) {
            new BukkitRunnable() {
                int skullsFired = 0;

                @Override
                public void run() {
                    if (skullsFired >= 5 || wither.isDead()) {
                        cancel();
                        return;
                    }

                    WitherSkull skull = wither.launchProjectile(WitherSkull.class);
                    skull.setMetadata("UltraWitherSkull", new FixedMetadataValue(plugin, true));
                    skull.setGlowing(true);

                    Vector direction = target.getEyeLocation().subtract(skull.getLocation()).toVector().normalize();
                    skull.setVelocity(direction.multiply(1.3));

                    skullsFired++;
                }
            }.runTaskTimer(plugin, 0L, 3L);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                isSpecialAttackActive.put(wither.getUniqueId(), false);
            }
        }.runTaskLater(plugin, 60L);
    }

    private void executeSummonWitherSkeletons(Wither wither) {
        isSpecialAttackActive.put(wither.getUniqueId(), true);
        wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.7f);

        boolean isPhase2 = isPhaseTwo.getOrDefault(wither.getUniqueId(), false);
        int min = isPhase2 ? 4 : 2;
        int max = isPhase2 ? 4 : 3;
        int count = random.nextInt(max - min + 1) + min;

        double radius = 8.0;
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = random.nextDouble() * 2;

            Location spawnLoc = wither.getLocation().clone().add(x, y, z);

            while (spawnLoc.getBlock().getType().isSolid()) {
                spawnLoc.add(0, 1, 0);
            }

            WitherSkeleton skeleton = (WitherSkeleton) wither.getWorld().spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);
            skeleton.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));

            Player target = getRandomPlayer(wither);
            if (target != null && shouldAffectPlayer(target)) {
                skeleton.setTarget(target);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                isSpecialAttackActive.put(wither.getUniqueId(), false);
            }
        }.runTaskLater(plugin, 40L);
    }

    private void executeInstakillAttack(Wither wither) {
        UUID witherId = wither.getUniqueId();

        // CONTROL CRÍTICO: No ejecutar si drain está activo
        if (isDrainActive.getOrDefault(witherId, false)) {
            return;
        }

        isSpecialAttackActive.put(witherId, true);
        isInstakillActive.put(witherId, true);

        wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);

        List<Player> targets = getRandomPlayers(wither, 3, 8).stream()
                .filter(this::shouldAffectPlayer)
                .filter(p -> !lastInstakillAttackTime.containsKey(p.getUniqueId()) ||
                        System.currentTimeMillis() - lastInstakillAttackTime.get(p.getUniqueId()) > 10000)
                .collect(Collectors.toList());

        for (Player target : targets) {
            lastInstakillAttackTime.put(target.getUniqueId(), System.currentTimeMillis());

            BossBar bossBar = Bukkit.createBossBar(
                    ChatColor.RED + "\uEAA5",
                    BarColor.WHITE,
                    BarStyle.SOLID
            );
            bossBar.setProgress(1.0);
            bossBar.addPlayer(target);

            target.sendTitle(ChatColor.DARK_RED + "☠ INSTAKILL ☠", "", 10, 50, 20);
            target.sendMessage(" ");
            target.sendMessage(ChatColor.of("#FF0000") + "" + ChatColor.BOLD + "۞" +
                    ChatColor.RESET + ChatColor.of("#D03313") + " Presiona" +
                    ChatColor.of("#F7484B") + ChatColor.BOLD + " SHIFT 20 veces" +
                    ChatColor.RESET + ChatColor.of("#D03313") + " en" +
                    ChatColor.of("#BD2465") + ChatColor.BOLD + " 10 segundos" +
                    ChatColor.RESET + ChatColor.of("#D03313") + " para evitar el ataque.");
            target.sendMessage(" ");
            instakillAttempts.put(target.getUniqueId(), 0);

            BukkitRunnable soundTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!instakillAttempts.containsKey(target.getUniqueId())) {
                        this.cancel();
                        shiftSoundTasks.remove(target.getUniqueId());
                    }
                }
            };
            soundTask.runTaskTimer(plugin, 0L, 1L);
            shiftSoundTasks.put(target.getUniqueId(), soundTask);

            new BukkitRunnable() {
                int ticks = 0;
                boolean success = false;

                @Override
                public void run() {
                    if (ticks >= 200 || wither.isDead()) {
                        if (!success) {
                            target.damage(100.0);
                            target.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, target.getLocation(), 1);
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
                            deathCauseMap.put(target.getUniqueId(), "Instakill de Ultra Wither");
                        }
                        bossBar.removeAll();
                        instakillAttempts.remove(target.getUniqueId());

                        if (shiftSoundTasks.containsKey(target.getUniqueId())) {
                            shiftSoundTasks.get(target.getUniqueId()).cancel();
                            shiftSoundTasks.remove(target.getUniqueId());
                        }

                        cancel();
                        return;
                    }

                    if (instakillAttempts.getOrDefault(target.getUniqueId(), 0) >= 20) {
                        success = true;
                        successNotification.showSuccess(target);

                        bossBar.removeAll();
                        if (shiftSoundTasks.containsKey(target.getUniqueId())) {
                            shiftSoundTasks.get(target.getUniqueId()).cancel();
                            shiftSoundTasks.remove(target.getUniqueId());
                        }

                        cancel();
                        return;
                    }

                    ticks += 5;
                }
            }.runTaskTimer(plugin, 0L, 5L);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                isSpecialAttackActive.put(witherId, false);
                isInstakillActive.put(witherId, false);
            }
        }.runTaskLater(plugin, 200L);
    }

    private void executeDrainAttack(Wither wither) {
        UUID witherId = wither.getUniqueId();

        // CONTROL CRÍTICO: No ejecutar si instakill está activo
        if (isInstakillActive.getOrDefault(witherId, false)) {
            return;
        }

        isSpecialAttackActive.put(witherId, true);
        isDrainActive.put(witherId, true);

        wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0f, 0.5f);

        List<Player> targets = getRandomPlayers(wither, 3, 8).stream()
                .filter(this::shouldAffectPlayer)
                .filter(p -> !lastDrainAttackTime.containsKey(p.getUniqueId()) ||
                        System.currentTimeMillis() - lastDrainAttackTime.get(p.getUniqueId()) > 10000)
                .collect(Collectors.toList());

        for (Player target : targets) {
            lastDrainAttackTime.put(target.getUniqueId(), System.currentTimeMillis());

            BossBar bossBar = Bukkit.createBossBar(
                    ChatColor.DARK_PURPLE + "\uEAA5",
                    BarColor.WHITE,
                    BarStyle.SOLID
            );
            bossBar.setProgress(1.0);
            bossBar.addPlayer(target);

            target.sendTitle(ChatColor.DARK_PURPLE + "⚠ DRENADOR ⚠", "", 10, 50, 20);
            target.sendMessage(" ");
            target.sendMessage(ChatColor.of("#9400B3") + "" + ChatColor.BOLD + "۞" +
                    ChatColor.RESET + ChatColor.of("#B867E6") + " Usa" +
                    ChatColor.of("#D48E19") + ChatColor.BOLD + " botellas de miel" +
                    ChatColor.RESET + ChatColor.of("#B867E6") + " para curarte del " +
                    ChatColor.of("#9400B3") + ChatColor.BOLD + "drenaje" +
                    ChatColor.RESET + ChatColor.of("#009963") + ".");
            target.sendMessage(" ");

            target.addPotionEffect(new PotionEffect(
                    PotionEffectType.POISON,
                    600,
                    19,
                    false,
                    false,
                    true
            ));

            BukkitRunnable drainTask = new BukkitRunnable() {
                int ticks = 0;
                int damageCounter = 0;

                @Override
                public void run() {
                    if (ticks >= 600 || wither.isDead() || !target.isOnline()) {
                        target.removePotionEffect(PotionEffectType.POISON);
                        bossBar.removeAll();
                        activeDrainTasks.remove(target.getUniqueId());
                        cancel();
                        return;
                    }

                    if (ticks % 40 == 0) {
                        target.damage(5.0);
                        target.playSound(target.getLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, 1.5f, 0.8f);
                        target.spawnParticle(Particle.DRIPPING_HONEY, target.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5);
                        damageCounter++;

                        bossBar.setProgress(1.0 - (damageCounter / 15.0));
                        deathCauseMap.put(target.getUniqueId(), "Drenaje de Ultra Wither");
                    }

                    if (ticks % 10 == 0) {
                        target.playSound(target.getLocation(), Sound.BLOCK_HONEY_BLOCK_FALL, 1.5f, 1.6f);
                    }

                    ticks++;
                }
            };

            drainTask.runTaskTimer(plugin, 0L, 1L);
            activeDrainTasks.put(target.getUniqueId(), drainTask);

            target.setMetadata("DrainBossBar", new FixedMetadataValue(plugin, bossBar));
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                isSpecialAttackActive.put(witherId, false);
                isDrainActive.put(witherId, false);
            }
        }.runTaskLater(plugin, 40L);
    }

    private void executeSkullEffectAttack(Wither wither) {
        isSpecialAttackActive.put(wither.getUniqueId(), true);
        wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 2.0f, 0.7f);

        Location skullLocation = wither.getLocation().add(0, 3, 0);
        BlockDisplay skull = wither.getWorld().spawn(skullLocation, BlockDisplay.class);

        skull.setBlock(Bukkit.createBlockData(Material.WITHER_SKELETON_SKULL));
        skull.setInvulnerable(true);
        skull.setGravity(false);
        skull.setGlowing(true);
        skull.setGlowColorOverride(Color.YELLOW);
        skull.setBrightness(new Display.Brightness(15, 15));

        skull.setTransformation(new Transformation(
                new Vector3f(),
                new AxisAngle4f(0, 0, 0, 0),
                new Vector3f(2, 2, 2),
                new AxisAngle4f(0, 0, 0, 0)
        ));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (skull.isDead()) {
                    cancel();
                    return;
                }

                skull.getWorld().spawnParticle(
                        Particle.POOF,
                        skull.getLocation(),
                        20,
                        0.5, 0.5, 0.5
                );

                skull.setTransformation(new Transformation(
                        new Vector3f(),
                        new AxisAngle4f(0, 1, 0, (float) Math.toRadians(skull.getTicksLived() * 2)),
                        new Vector3f(2, 2, 2),
                        new AxisAngle4f(0, 0, 0, 0)
                ));
            }
        }.runTaskTimer(plugin, 0L, 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity entity : skull.getNearbyEntities(SKULL_EFFECT_RADIUS, SKULL_EFFECT_RADIUS, SKULL_EFFECT_RADIUS)) {
                    if (entity instanceof Player player && shouldAffectPlayer(player)) {
                        player.addPotionEffect(new PotionEffect(
                                PotionEffectType.NAUSEA,
                                SKULL_EFFECT_DURATION,
                                4,
                                false,
                                true,
                                true
                        ));

                        player.addPotionEffect(new PotionEffect(
                                PotionEffectType.JUMP_BOOST,
                                SKULL_EFFECT_DURATION,
                                4,
                                false,
                                true,
                                true
                        ));

                        player.addPotionEffect(new PotionEffect(
                                PotionEffectType.DARKNESS,
                                SKULL_EFFECT_DURATION,
                                0,
                                false,
                                true,
                                true
                        ));

                        player.getWorld().spawnParticle(
                                Particle.WITCH,
                                player.getEyeLocation(),
                                15,
                                0.5, 0.5, 0.5,
                                0.1
                        );

                        deathCauseMap.put(player.getUniqueId(), "Skull Effect de Ultra Wither");
                    }
                }

                skull.getWorld().playSound(skull.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.5f);
                skull.getWorld().playSound(skull.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1.5f, 0.8f);

                skull.getWorld().spawnParticle(
                        Particle.EXPLOSION_EMITTER,
                        skull.getLocation(),
                        5,
                        1, 1, 1
                );

                for (int i = 1; i <= SKULL_EFFECT_RADIUS; i++) {
                    final int radius = i;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (double angle = 0; angle < 360; angle += 360 / (radius * 4.0)) {
                                double x = Math.cos(Math.toRadians(angle)) * radius;
                                double z = Math.sin(Math.toRadians(angle)) * radius;
                                Location particleLoc = skull.getLocation().add(x, 0, z);

                                skull.getWorld().spawnParticle(
                                        Particle.POOF,
                                        particleLoc,
                                        1,
                                        0, 0, 0,
                                        0
                                );
                            }
                        }
                    }.runTaskLater(plugin, i * 2L);
                }

                skull.remove();

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        isSpecialAttackActive.put(wither.getUniqueId(), false);
                    }
                }.runTaskLater(plugin, 40L);
            }
        }.runTaskLater(plugin, 20L);
    }

    private void executeRingAttack(Wither wither) {
        isSpecialAttackActive.put(wither.getUniqueId(), true);
        World world = wither.getWorld();

        world.playSound(wither.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.7f);
        world.playSound(wither.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.5f);

        List<Player> players = world.getNearbyEntities(wither.getLocation(), 15, 15, 15).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player)e)
                .filter(this::shouldAffectPlayer)
                .collect(Collectors.toList());

        if (players.isEmpty()) {
            isSpecialAttackActive.put(wither.getUniqueId(), false);
            return;
        }

        boolean isPhase2 = isPhaseTwo.getOrDefault(wither.getUniqueId(), false);
        int ringsToSpawn = isPhase2 ? 2 : 1;
        List<RingData> rings = new ArrayList<>();

        Collections.shuffle(players);
        for (int i = 0; i < Math.min(ringsToSpawn, players.size()); i++) {
            Location ringLoc = findSafeRingLocation(players.get(i).getLocation());
            if (ringLoc != null) {
                RingData ring = createRing(ringLoc);
                rings.add(ring);
                world.playSound(ringLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.6f);
            }
        }

        if (rings.isEmpty()) {
            isSpecialAttackActive.put(wither.getUniqueId(), false);
            return;
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= RING_ATTACK_DURATION || wither.isDead()) {
                    endRingAttack(rings, wither);
                    cancel();
                    return;
                }

                for (RingData ring : rings) {
                    if (ring.display.isDead()) continue;

                    rotateRing(ring, ticks);

                    if (ticks % 5 == 0) {
                        spawnFixedRingParticles(ring);
                    }

                    if (ticks % 10 == 0) {
                        shootControlledSkulls(ring, wither);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 20L, 1L);
    }

    // ==================== EVENTOS ====================

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Wither wither) || !isUltraWither(wither)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            event.setCancelled(true);
            return;
        }

        if (wither.getHealth() - event.getFinalDamage() <= 0) {
            event.setCancelled(true);
            executeUltraWitherDeath(wither);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Wither wither) || !isUltraWither(wither)) return;

        if (event.getDamager() instanceof Player player) {
            ItemStack weapon = player.getInventory().getItemInMainHand();
            if (weapon != null && weapon.containsEnchantment(Enchantment.SMITE)) {
                event.setDamage(event.getDamage() - weapon.getEnchantmentLevel(Enchantment.SMITE) * 2.5);
            }
        }

        Player player = null;

        if (event.getDamager() instanceof Player) {
            player = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player) {
            player = (Player) projectile.getShooter();
        }

        if (player != null) {
            Set<UUID> attackers = playerAttacks.get(wither.getUniqueId());
            if (attackers != null) {
                attackers.add(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Wither wither && isUltraWither(wither)) {
            event.setDroppedExp(0);
            event.getDrops().clear();
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
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() != Material.HONEY_BOTTLE) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (activeDrainTasks.containsKey(playerId)) {
            activeDrainTasks.get(playerId).cancel();
            activeDrainTasks.remove(playerId);

            player.removePotionEffect(PotionEffectType.POISON);

            if (player.hasMetadata("DrainBossBar")) {
                BossBar bossBar = (BossBar) player.getMetadata("DrainBossBar").get(0).value();
                bossBar.removeAll();
                player.removeMetadata("DrainBossBar", plugin);
            }

            player.spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 5);
            successNotification.showSuccess(player);
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (event.isSneaking() && instakillAttempts.containsKey(player.getUniqueId())) {
            int attempts = instakillAttempts.get(player.getUniqueId());
            instakillAttempts.put(player.getUniqueId(), attempts + 1);

            if (shiftSoundTasks.containsKey(player.getUniqueId())) {
                player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 3);
                player.playSound(player.getLocation(), "custom.click_stereo_old", SoundCategory.VOICE, 0.5f, 1.0f + attempts * 0.05f);
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof WitherSkull skull && skull.hasMetadata("UltraWitherSkull")) {
            if (skull.hasMetadata("Explosive")) {
                skull.getWorld().createExplosion(skull.getLocation(), 3.0f, false, false);
            }

            if (event.getHitEntity() instanceof LivingEntity livingEntity && skull.hasMetadata("WitherEffect")) {
                PotionEffectType type = (PotionEffectType) skull.getMetadata("WitherEffect").get(0).value();
                int duration = skull.getMetadata("WitherEffectDuration").get(0).asInt();
                int amplifier = skull.getMetadata("WitherEffectAmplifier").get(0).asInt();

                livingEntity.addPotionEffect(new PotionEffect(type, duration, amplifier));
            }
        }
    }

    @EventHandler
    public void onProjectileSkullHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof WitherSkull skull &&
                skull.hasMetadata("IgnoreCollisions") &&
                event.getHitEntity() instanceof Projectile) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageSkullByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof WitherSkull &&
                event.getDamager().hasMetadata("NoWitherDamage") &&
                (event.getEntity() instanceof Wither || event.getEntity() instanceof WitherSkeleton)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Wither wither && isUltraWither(wither)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlock().equals(event.getTo().getBlock())) return;

        Player player = event.getPlayer();

        player.getNearbyEntities(50, 50, 50).stream()
                .filter(e -> e instanceof Wither)
                .map(e -> (Wither) e)
                .filter(wither -> wither.getCustomName() != null && wither.getCustomName().contains("Corrupted Wither Boss"))
                .forEach(wither -> {
                    if (!isUltraWither(wither)) {
                        transformToUltraWither(wither);
                    } else {
                        // VERIFICACIÓN ESPECÍFICA: Solo reiniciar si realmente no tiene comportamiento
                        UUID witherId = wither.getUniqueId();
                        if (!activeBehaviors.containsKey(witherId) || activeBehaviors.get(witherId).isCancelled()) {
                            plugin.getLogger().info("Player movement: Restarting behavior for dormant wither: " + witherId);
                            restartBehaviorSafely(wither);
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
                world.getEntitiesByClass(Wither.class).stream()
                        .filter(wither -> wither.getCustomName() != null && wither.getCustomName().contains("Corrupted Wither Boss"))
                        .filter(wither -> wither.getPersistentDataContainer().has(ultraWitherKey, PersistentDataType.BYTE))
                        .forEach(wither -> {
                            plugin.getLogger().info("World load: Found existing Ultra Wither: " + wither.getUniqueId());
                            if (canProcessWither(wither)) {
                                transformToUltraWither(wither);
                            }
                        });
            }
        }.runTaskLater(plugin, 100L);
    }

    // ==================== REINICIO SEGURO ====================

    private void restartBehaviorSafely(Wither wither) {
        UUID witherId = wither.getUniqueId();

        // Solo reiniciar si no está siendo procesado Y está completamente inicializado
        if (processingWithers.contains(witherId) || !fullyInitializedWithers.contains(witherId)) {
            return;
        }

        processingWithers.add(witherId);

        try {
            // Cancelar comportamiento anterior
            BukkitRunnable oldBehavior = activeBehaviors.remove(witherId);
            if (oldBehavior != null && !oldBehavior.isCancelled()) {
                oldBehavior.cancel();
            }

            // Reiniciar estados de ataque
            isAttacking.put(witherId, false);

            // Reiniciar comportamiento
            startBehavior(wither);

        } finally {
            new BukkitRunnable() {
                @Override
                public void run() {
                    processingWithers.remove(witherId);
                }
            }.runTaskLater(plugin, 20L);
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private void spawnCooldownEffects(Location location) {
        Wither phantomWither = (Wither) location.getWorld().spawnEntity(location.clone().add(0, 10, 0), EntityType.WITHER);
        phantomWither.setInvulnerable(true);
        phantomWither.setAI(false);
        phantomWither.setGravity(true);
        phantomWither.setCustomNameVisible(false);
        phantomWither.setGlowing(false);
        phantomWither.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));

        BlockDisplay skull = location.getWorld().spawn(location.clone().add(0, 3, 0), BlockDisplay.class);
        skull.setBlock(Bukkit.createBlockData(Material.WITHER_SKELETON_SKULL));
        skull.setInvulnerable(true);
        skull.setGravity(false);
        skull.setGlowing(true);
        skull.setGlowColorOverride(Color.PURPLE);
        skull.setBrightness(new Display.Brightness(15, 15));

        skull.setTransformation(new Transformation(
                new Vector3f(),
                new AxisAngle4f(0, 0, 0, 0),
                new Vector3f(3, 3, 3),
                new AxisAngle4f(0, 0, 0, 0)
        ));

        new BukkitRunnable() {
            int ticks = 0;
            boolean visible = false;

            @Override
            public void run() {
                if (ticks >= 200) {
                    phantomWither.remove();
                    skull.remove();

                    location.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, location, 30, 1, 1, 1, 0.5);
                    location.getWorld().playSound(location, Sound.ENTITY_WITHER_SPAWN, 4.0f, 0.7f);
                    location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 4.0f, 0.8f);

                    for (int i = 1; i <= 15; i++) {
                        final int radius = i;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                for (double angle = 0; angle < 360; angle += 360 / (radius * 4.0)) {
                                    double x = Math.cos(Math.toRadians(angle)) * radius;
                                    double z = Math.sin(Math.toRadians(angle)) * radius;
                                    Location particleLoc = location.clone().add(x, 0, z);
                                    particleLoc.getWorld().spawnParticle(
                                            Particle.POOF,
                                            particleLoc,
                                            1,
                                            0, 0, 0,
                                            0
                                    );
                                }
                            }
                        }.runTaskLater(plugin, i);
                    }

                    cancel();
                    return;
                }

                skull.teleport(phantomWither.getLocation().add(0, 3, 0));

                skull.setTransformation(new Transformation(
                        new Vector3f(),
                        new AxisAngle4f(0, 1, 0, (float) Math.toRadians(ticks * 3)),
                        new Vector3f(3, 3, 3),
                        new AxisAngle4f(0, 0, 0, 0)
                ));

                if (ticks % 10 == 0) {
                    visible = !visible;

                    if (visible) {
                        phantomWither.removePotionEffect(PotionEffectType.INVISIBILITY);
                        phantomWither.setGlowing(true);
                        phantomWither.getWorld().spawnParticle(Particle.FLASH,
                                phantomWither.getLocation().add(0, 2, 0),
                                20, 0.5, 1, 0.5, 0);
                    } else {
                        phantomWither.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20, 1, false, false));
                        phantomWither.setGlowing(false);
                    }
                }

                if (ticks < 180) {
                    phantomWither.teleport(phantomWither.getLocation().subtract(0, 0.05, 0));
                }

                if (ticks % 5 == 0) {
                    for (int i = 0; i < 12; i++) {
                        double angle = Math.toRadians((ticks * 5) + (i * 30));
                        double x = Math.cos(angle) * 2.5;
                        double z = Math.sin(angle) * 2.5;
                        skull.getWorld().spawnParticle(
                                Particle.SOUL_FIRE_FLAME,
                                skull.getLocation().add(x, 0.5, z),
                                2,
                                0.1, 0.1, 0.1,
                                0.05
                        );
                    }
                }

                if (ticks % 7 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double angle = 2 * Math.PI * i / 8;
                        double x = Math.cos(angle) * 3;
                        double z = Math.sin(angle) * 3;

                        Location particleLoc = location.clone().add(x, 0, z);
                        for (int j = 0; j < 5; j++) {
                            particleLoc.getWorld().spawnParticle(Particle.FLAME, particleLoc, 3, 0.1, 0.1, 0.1, 0.05);
                            particleLoc.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 3, 0.1, 0.1, 0.1, 0.05);
                            particleLoc.add(0, 1, 0);
                        }
                    }
                }
                if (ticks % 20 < 10) {
                    location.getWorld().spawnParticle(Particle.LARGE_SMOKE, location, 30, 1, 1, 1, 0.1);
                }

                if (ticks % 20 == 0) {
                    location.getWorld().playSound(location, Sound.ENTITY_WITHER_SPAWN, 4.0f, 0.7f);
                }

                if (ticks % 40 == 0) {
                    location.getWorld().playSound(location, Sound.ENTITY_WITHER_AMBIENT, 2f, 0.7f);
                }

                if (ticks % 30 == 0) {
                    location.getWorld().playSound(location, Sound.ENTITY_WARDEN_HEARTBEAT, 4.0f, 0.6f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startPhaseTwo(Wither wither) {
        UUID witherId = wither.getUniqueId();
        isPhaseTwo.put(witherId, true);
        isSpecialAttackActive.put(witherId, true);

        wither.setInvulnerable(true);
        wither.setAI(false);
        wither.setGlowing(true);
        wither.setCustomName(ChatColor.DARK_RED + "Corrupted Wither Boss - Fase 2");

        wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_SPAWN, 5.0f, 0.7f);

        new BukkitRunnable() {
            int beamsSpawned = 0;
            final Location witherLoc = wither.getLocation().clone();
            final double witherHeight = 3.0;

            @Override
            public void run() {
                if (beamsSpawned >= 5 || wither.isDead()) {
                    wither.getWorld().playSound(witherLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 5.0f, 0.8f);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Location explosionLoc = witherLoc.clone().add(0, witherHeight/2, 0);
                            explosionLoc.getWorld().spawnParticle(Particle.SONIC_BOOM,
                                    explosionLoc, 50, 2, 2, 2, 0.5);

                            explosionLoc.getWorld().playSound(explosionLoc,
                                    Sound.ENTITY_WARDEN_SONIC_BOOM, 5.0f, 0.7f);
                            explosionLoc.getWorld().playSound(explosionLoc,
                                    Sound.ENTITY_GENERIC_EXPLODE, 5.0f, 0.5f);

                            wither.setInvulnerable(false);
                            wither.setAI(true);
                            isSpecialAttackActive.put(witherId, false);
                        }
                    }.runTaskLater(plugin, 20L);

                    cancel();
                    return;
                }

                double angle = beamsSpawned * (2 * Math.PI / 5);
                double distance = 10.0;
                double heightOffset = witherHeight * 0.7;

                Location beamStart = witherLoc.clone().add(
                        Math.cos(angle) * distance,
                        heightOffset,
                        Math.sin(angle) * distance
                );

                Vector direction = witherLoc.clone().add(0, heightOffset, 0)
                        .subtract(beamStart).toVector().normalize();

                witherLoc.getWorld().playSound(beamStart,
                        Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 1.0f);
                witherLoc.getWorld().playSound(beamStart,
                        Sound.BLOCK_BEACON_ACTIVATE, 3.0f, 0.8f);

                new BukkitRunnable() {
                    int steps = 0;
                    final Location currentPos = beamStart.clone();
                    final Vector step = direction.clone().multiply(0.7);
                    final double targetDistance = 1.5;

                    @Override
                    public void run() {
                        if (steps >= 25 || wither.isDead()) {
                            cancel();
                            return;
                        }

                        currentPos.add(step);

                        witherLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                                currentPos, 10, 0.1, 0.1, 0.1, 0.2);
                        witherLoc.getWorld().spawnParticle(Particle.FIREWORK,
                                currentPos, 5, 0.1, 0.1, 0.1, 0.1);

                        if (currentPos.distance(witherLoc) < targetDistance) {
                            witherLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER,
                                    currentPos, 5, 0.3, 0.3, 0.3);
                            witherLoc.getWorld().playSound(currentPos,
                                    Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 1.2f);
                            cancel();
                        }

                        steps++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);

                beamsSpawned++;
            }
        }.runTaskTimer(plugin, 0L, 15L);
    }

    private List<Player> getRandomPlayers(Wither wither, int min, int max) {
        List<Player> nearbyPlayers = wither.getWorld().getNearbyEntities(wither.getLocation(), 30, 30, 30).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player)e)
                .filter(this::shouldAffectPlayer)
                .collect(Collectors.toList());

        if (nearbyPlayers.isEmpty()) {
            return new ArrayList<>();
        }

        int count = Math.min(random.nextInt(max - min + 1) + min, nearbyPlayers.size());
        Collections.shuffle(nearbyPlayers);
        return nearbyPlayers.subList(0, count);
    }

    private Player getRandomPlayer(Wither wither) {
        List<Player> players = getRandomPlayers(wither, 1, 1);
        return players.isEmpty() ? null : players.get(0);
    }

    // Ring Attack Helper Methods
    private Location findSafeRingLocation(Location playerLoc) {
        Location potentialLoc = playerLoc.clone().add(0, 12, 0);
        World world = playerLoc.getWorld();

        for (int i = 0; i < 12; i++) {
            if (isAirSafe(potentialLoc)) {
                return potentialLoc;
            }
            potentialLoc.add(0, -1, 0);
        }
        return null;
    }

    private boolean isAirSafe(Location loc) {
        World world = loc.getWorld();
        return world.getBlockAt(loc).getType().isAir() &&
                world.getBlockAt(loc.clone().add(0, 1, 0)).getType().isAir();
    }

    private static class RingData {
        BlockDisplay display;
        Location center;
        List<Location> particlePoints;

        RingData(BlockDisplay display, Location center) {
            this.display = display;
            this.center = center;
            this.particlePoints = calculateFixedPoints(center);
        }

        private List<Location> calculateFixedPoints(Location center) {
            List<Location> points = new ArrayList<>();
            double radius = 3.5;

            for (int i = 0; i < 20; i++) {
                double angle = 2 * Math.PI * i / 20;
                points.add(center.clone().add(
                        Math.cos(angle) * radius,
                        0,
                        Math.sin(angle) * radius
                ));
            }
            return points;
        }
    }

    private RingData createRing(Location loc) {
        BlockDisplay ring = loc.getWorld().spawn(loc, BlockDisplay.class);
        ring.setBlock(Bukkit.createBlockData(Material.END_ROD));
        ring.setBrightness(new Display.Brightness(15, 15));
        ring.setGlowing(true);
        ring.setGlowColorOverride(Color.PURPLE);
        ring.setInvulnerable(true);
        ring.setGravity(false);
        ring.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf(),
                new Vector3f(7, 0.1f, 7),
                new Quaternionf()
        ));
        return new RingData(ring, loc);
    }

    private void rotateRing(RingData ring, int ticks) {
        float angle = (float) Math.toRadians(ticks * 10);
        Quaternionf rotation = new Quaternionf().rotateY(angle);
        ring.display.setTransformation(new Transformation(
                new Vector3f(),
                rotation,
                new Vector3f(7, 0.1f, 7),
                new Quaternionf()
        ));
    }

    private void spawnFixedRingParticles(RingData ring) {
        World world = ring.center.getWorld();
        for (Location point : ring.particlePoints) {
            world.spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.PORTAL, point, 1, 0, 0, 0, 0);
        }
    }

    private void shootControlledSkulls(RingData ring, Wither source) {
        World world = ring.center.getWorld();
        int skullsToShoot = 1 + random.nextInt(3);

        for (int i = 0; i < skullsToShoot; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 1.5 + random.nextDouble() * 2;
            Location spawnLoc = ring.center.clone().add(
                    Math.cos(angle) * distance,
                    0,
                    Math.sin(angle) * distance
            );

            WitherSkull skull = world.spawn(spawnLoc, WitherSkull.class);
            skull.setShooter(source);
            skull.setCharged(true);
            skull.setGlowing(true);
            skull.setMetadata("UltraWitherSkull", new FixedMetadataValue(plugin, true));
            skull.setMetadata("Explosive", new FixedMetadataValue(plugin, true));
            skull.setMetadata("NoWitherDamage", new FixedMetadataValue(plugin, true));

            double pitch = Math.toRadians(65 + random.nextDouble() * 15);
            Vector direction = new Vector(
                    -Math.sin(angle) * Math.cos(pitch),
                    -Math.sin(pitch),
                    Math.cos(angle) * Math.cos(pitch)
            ).normalize();

            skull.setVelocity(direction.multiply(1.6));
            skull.setMetadata("IgnoreCollisions", new FixedMetadataValue(plugin, true));

            world.playSound(spawnLoc, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.2f);
            startControlledSkullTrail(skull);
        }
    }

    private void startControlledSkullTrail(WitherSkull skull) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (skull.isDead()) {
                    cancel();
                    return;
                }
                skull.getWorld().spawnParticle(Particle.PORTAL, skull.getLocation(), 2, 0.1, 0.1, 0.1, 0.05);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void endRingAttack(List<RingData> rings, Wither wither) {
        for (RingData ring : rings) {
            if (!ring.display.isDead()) {
                Location loc = ring.display.getLocation();
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 30);
                loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.8f);
                ring.display.remove();
            }
        }
        isSpecialAttackActive.put(wither.getUniqueId(), false);
    }

    // ==================== MUERTE Y LIMPIEZA ====================

    private void executeUltraWitherDeath(Wither wither) {
        UUID witherId = wither.getUniqueId();

        if (deathExecuted.contains(witherId)) {
            return;
        }
        deathExecuted.add(witherId);

        Bukkit.getPluginManager().callEvent(new UltraWitherDefeatedEvent(wither));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 40) {
                    wither.getWorld().spawnParticle(Particle.END_ROD, wither.getLocation(), 100, 2, 2, 2, 0.5);
                    wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 2.0f, 0.8f);

                    rewardPlayers(wither);
                    cleanupWither(witherId);
                    wither.remove();

                    cancel();
                    return;
                }

                wither.teleport(wither.getLocation().add(0, 0.1, 0));
                wither.getWorld().spawnParticle(Particle.END_ROD, wither.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void rewardPlayers(Wither wither) {
        Set<UUID> attackers = playerAttacks.get(wither.getUniqueId());
        if (attackers == null) return;

        for (Entity entity : wither.getWorld().getNearbyEntities(wither.getLocation(), 100, 100, 100)) {
            if (entity instanceof Player player && attackers.contains(player.getUniqueId())) {
                ItemStack reward = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);

                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(reward);

                if (!leftover.isEmpty()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover.get(0));
                }

                player.sendMessage(ChatColor.of("#FFD700") + "" + ChatColor.BOLD + "۞" +
                        ChatColor.RESET + ChatColor.of("#D7B300") + " Has derrotado al" +
                        ChatColor.of("#9400D3") + "" + ChatColor.BOLD + " Ultra Wither Boss");
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }
    }

    private void cleanupWither(UUID witherId) {
        // Cancelar comportamiento
        BukkitRunnable behavior = activeBehaviors.remove(witherId);
        if (behavior != null && !behavior.isCancelled()) {
            behavior.cancel();
        }

        // Limpiar estados
        isAttacking.remove(witherId);
        isPhaseTwo.remove(witherId);
        isSpecialAttackActive.remove(witherId);
        isDrainActive.remove(witherId);
        isInstakillActive.remove(witherId);
        playerAttacks.remove(witherId);
        processingWithers.remove(witherId);
        fullyInitializedWithers.remove(witherId);
        deathExecuted.remove(witherId);
    }

    // ==================== UTILIDADES ====================

    private boolean shouldAffectPlayer(Player player) {
        return player != null &&
                player.isValid() &&
                !player.isDead() &&
                (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE);
    }

    private boolean isUltraWither(Wither wither) {
        return wither != null && wither.isValid() && !wither.isDead() &&
                wither.getPersistentDataContainer().has(ultraWitherKey, PersistentDataType.BYTE);
    }

    // ==================== MÉTODOS PÚBLICOS ====================

    public void shutdown() {
        // Limpiar todos los withers
        new ArrayList<>(fullyInitializedWithers).forEach(this::cleanupWither);
        processingWithers.clear();
        fullyInitializedWithers.clear();
        deathExecuted.clear();
    }
}