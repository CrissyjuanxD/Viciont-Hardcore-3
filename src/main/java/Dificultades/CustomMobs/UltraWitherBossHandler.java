package Dificultades.CustomMobs;

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
import org.bukkit.event.player.PlayerToggleSneakEvent;
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
import java.util.stream.Collectors;

public class UltraWitherBossHandler implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Integer> instakillAttempts = new HashMap<>();
    private final Map<UUID, Long> lastAttackTime = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activeBehaviors = new HashMap<>();
    private final Map<UUID, Boolean> isPhaseTwo = new HashMap<>();
    private final Map<UUID, Boolean> isSpecialAttackActive = new HashMap<>();
    private final Map<UUID, Long> lastDrainAttackTime = new HashMap<>();
    private final Map<UUID, Long> lastInstakillAttackTime = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activeDrainTasks = new HashMap<>();
    private final Set<UUID> deathExecuted = new HashSet<>();
    private final Map<UUID, Set<UUID>> playerAttacks = new HashMap<>();
    private static final int SKULL_EFFECT_RADIUS = 10;
    private static final int SKULL_EFFECT_DURATION = 200;
    private static final int RING_ATTACK_DURATION = 300;

    private final Map<UUID, BukkitRunnable> shiftSoundTasks = new HashMap<>();
    private final Random random = new Random();
    private final NamespacedKey ultraWitherKey;
    private final SuccessNotification successNotification;

    public UltraWitherBossHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.ultraWitherKey = new NamespacedKey(plugin, "ultra_wither");
        this.successNotification = new SuccessNotification(plugin);
    }

    public Wither spawnUltraWither(Location location) {
        // Spawn cooldown with particles and sounds
        spawnCooldownEffects(location);

        // Schedule the actual spawn after 10 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                Wither wither = (Wither) location.getWorld().spawnEntity(location, EntityType.WITHER);
                applyUltraWitherAttributes(wither);
            }
        }.runTaskLater(plugin, 200L); // 10 seconds (200 ticks)

        return null; // Temporary return, actual wither will spawn later
    }

    private void spawnCooldownEffects(Location location) {
        // Spawnear un Wither real pero invisible inicialmente
        Wither phantomWither = (Wither) location.getWorld().spawnEntity(location.clone().add(0, 10, 0), EntityType.WITHER);
        phantomWither.setInvulnerable(true);
        phantomWither.setAI(false);
        phantomWither.setGravity(true);
        phantomWither.setCustomNameVisible(false);
        phantomWither.setGlowing(false);
        phantomWither.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));

        // Crear calavera flotante con BlockDisplay (como en skullEffectAttack)
        BlockDisplay skull = location.getWorld().spawn(location.clone().add(0, 3, 0), BlockDisplay.class);
        skull.setBlock(Bukkit.createBlockData(Material.WITHER_SKELETON_SKULL));
        skull.setInvulnerable(true);
        skull.setGravity(false);
        skull.setGlowing(true);
        skull.setGlowColorOverride(Color.PURPLE); // Cambiado a morado para diferenciar
        skull.setBrightness(new Display.Brightness(15, 15));

        // Ajustar transformación de la calavera
        skull.setTransformation(new Transformation(
                new Vector3f(),
                new AxisAngle4f(0, 0, 0, 0),
                new Vector3f(3, 3, 3), // Más grande que en el ataque
                new AxisAngle4f(0, 0, 0, 0)
        ));

        new BukkitRunnable() {
            int ticks = 0;
            boolean visible = false;

            @Override
            public void run() {
                if (ticks >= 200) { // 10 segundos
                    // Eliminar el Wither fantasma
                    phantomWither.remove();
                    skull.remove();

                    // Crear explosión de spawn y animacion de particulas
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

                // Mover la calavera para que siga al Wither fantasma
                skull.teleport(phantomWither.getLocation().add(0, 3, 0));

                // Rotación de la calavera (como en skullEffectAttack)
                skull.setTransformation(new Transformation(
                        new Vector3f(),
                        new AxisAngle4f(0, 1, 0, (float) Math.toRadians(ticks * 3)),
                        new Vector3f(3, 3, 3),
                        new AxisAngle4f(0, 0, 0, 0)
                ));

                // Efecto de parpadeo cada medio segundo
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

                // Mover el Wither hacia abajo lentamente
                if (ticks < 180) { // Los últimos 20 ticks (1 segundo) se queda quieto
                    phantomWither.teleport(phantomWither.getLocation().subtract(0, 0.05, 0));
                }

                if (ticks % 5 == 0) {
                    // Anillo de partículas girando
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

                // Fire and purple particle pillars in a circle
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
                // Blinking effect
                if (ticks % 20 < 10) { // Blink every second
                    location.getWorld().spawnParticle(Particle.LARGE_SMOKE, location, 30, 1, 1, 1, 0.1);
                }

                // Sounds
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

    private void applyUltraWitherAttributes(Wither wither) {
        wither.setCustomName(ChatColor.DARK_PURPLE + "Corrupted Wither Boss");
        Objects.requireNonNull(wither.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(1000.0);
        wither.setHealth(1000.0);
        Objects.requireNonNull(wither.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.4);
        Objects.requireNonNull(wither.getAttribute(Attribute.GENERIC_FOLLOW_RANGE)).setBaseValue(50);
        wither.getPersistentDataContainer().set(ultraWitherKey, PersistentDataType.BYTE, (byte) 1);
        wither.setRemoveWhenFarAway(false);
        wither.setCustomNameVisible(false);

        isPhaseTwo.put(wither.getUniqueId(), false);
        isSpecialAttackActive.put(wither.getUniqueId(), false);
        playerAttacks.put(wither.getUniqueId(), new HashSet<>());

        startBossBehavior(wither);

        // Spawn explosion
        wither.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, wither.getLocation(), 1);
        wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
    }

    private void startBossBehavior(Wither wither) {
        if (activeBehaviors.containsKey(wither.getUniqueId())) {
            return;
        }

        BukkitRunnable behaviorTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (wither.isDead()) {
                    activeBehaviors.remove(wither.getUniqueId());
                    cancel();
                    return;
                }

                // Check for phase transition
                if (!isPhaseTwo.get(wither.getUniqueId()) && wither.getHealth() <= 500.0) {
                    startPhaseTwo(wither);
                    return;
                }

                if (isSpecialAttackActive.getOrDefault(wither.getUniqueId(), false)) {
                    return;
                }

                // Random attack selection
                int attack = random.nextInt(isPhaseTwo.get(wither.getUniqueId()) ? 7 : 6);

                switch (attack) {
                    case 0 -> witherSkullAttack(wither);
                    case 1 -> summonWitherSkeletons(wither);
                    case 2 -> instakillAttack(wither);
                    case 3 -> {
                        if (isPhaseTwo.get(wither.getUniqueId())) {
                            drainAttack(wither);
                        } else {
                            witherSkullAttack(wither); // Fallback if not in phase 2
                        }
                    }
                    case 4 -> skullEffectAttack(wither);
                    case 5 -> witherSkullBarrage(wither);
                    case 6 -> ringAttack(wither);
                }
            }
        };

        behaviorTask.runTaskTimer(plugin, 0L, 60L); // Every 3 seconds
        activeBehaviors.put(wither.getUniqueId(), behaviorTask);
    }

    private void witherSkullAttack(Wither wither) {
        isSpecialAttackActive.put(wither.getUniqueId(), true);
        wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.8f);

        List<Player> targets = getRandomPlayers(wither, 2, 5).stream()
                .filter(this::shouldAffectPlayer) // Filtro adicional por seguridad
                .collect(Collectors.toList());

        for (Player target : targets) {
            WitherSkull skull = wither.launchProjectile(WitherSkull.class);
            skull.setMetadata("UltraWitherSkull", new FixedMetadataValue(plugin, true));
            skull.setGlowing(true);

            // Apply Wither V effect on hit
            skull.setMetadata("WitherEffect", new FixedMetadataValue(plugin, PotionEffectType.WITHER));
            skull.setMetadata("WitherEffectDuration", new FixedMetadataValue(plugin, 200)); // 10 seconds
            skull.setMetadata("WitherEffectAmplifier", new FixedMetadataValue(plugin, 4)); // Wither V

            // Red skull (explosive)
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
        }.runTaskLater(plugin, 40L); // 2 seconds cooldown
    }

    private void witherSkullBarrage(Wither wither) {
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
            }.runTaskTimer(plugin, 0L, 3L); // Fire 5 skulls rapidly
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                isSpecialAttackActive.put(wither.getUniqueId(), false);
            }
        }.runTaskLater(plugin, 60L); // 3 seconds cooldown
    }

    private void summonWitherSkeletons(Wither wither) {
        isSpecialAttackActive.put(wither.getUniqueId(), true);
        wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.7f);

        int min = isPhaseTwo.get(wither.getUniqueId()) ? 4 : 2;
        int max = isPhaseTwo.get(wither.getUniqueId()) ? 4 : 3;
        int count = random.nextInt(max - min + 1) + min;

        // Mejor dispersión de los Wither Skeletons
        double radius = 8.0; // Radio mayor para mejor dispersión
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            // Altura aleatoria entre 0 y 2 bloques
            double y = random.nextDouble() * 2;

            Location spawnLoc = wither.getLocation().clone().add(x, y, z);

            // Asegurarse de que el lugar de spawn es seguro
            while (spawnLoc.getBlock().getType().isSolid()) {
                spawnLoc.add(0, 1, 0);
            }

            WitherSkeleton skeleton = (WitherSkeleton) wither.getWorld().spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);
            skeleton.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));

            // Asignar objetivos diferentes a cada esqueleto
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

    private void instakillAttack(Wither wither) {
        isSpecialAttackActive.put(wither.getUniqueId(), true);
        wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);

        // Obtener jugadores que no hayan recibido este ataque recientemente
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
            bossBar.setProgress(1.0); // Barra completa
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

            // Task para el sonido del shift
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
                        }
                        bossBar.removeAll();
                        instakillAttempts.remove(target.getUniqueId());

                        // Cancelar el task del sonido
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
                        // Cancelar el task del sonido
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
                isSpecialAttackActive.put(wither.getUniqueId(), false);
            }
        }.runTaskLater(plugin, 200L);
    }

    private void drainAttack(Wither wither) {
        isSpecialAttackActive.put(wither.getUniqueId(), true);
        wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_HURT, 2.0f, 0.5f);

        List<Player> targets = getRandomPlayers(wither, 3, 8).stream()
                .filter(this::shouldAffectPlayer) // Filtro principal
                .filter(p -> !lastDrainAttackTime.containsKey(p.getUniqueId()) ||
                        System.currentTimeMillis() - lastDrainAttackTime.get(p.getUniqueId()) > 10000)
                .collect(Collectors.toList());

        for (Player target : targets) {
            lastDrainAttackTime.put(target.getUniqueId(), System.currentTimeMillis());

            // Crear BossBar
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

            // Aplicar efecto de veneno nivel 2 (1 corazón de daño cada 1.25 segundos)
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
                        // Limpiar efectos
                        target.removePotionEffect(PotionEffectType.POISON);
                        bossBar.removeAll();
                        activeDrainTasks.remove(target.getUniqueId());
                        cancel();
                        return;
                    }

                    // Daño adicional de 2 corazones cada 2 segundos (40 ticks)
                    if (ticks % 40 == 0) {
                        target.damage(4.0); // 2 corazones = 4 puntos de daño
                        target.playSound(target.getLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, 1.5f, 0.8f);
                        target.spawnParticle(Particle.DRIPPING_HONEY, target.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5);
                        damageCounter++;

                        // Actualizar BossBar
                        bossBar.setProgress(1.0 - (damageCounter / 15.0)); // 15 daños máximos (30 segundos)
                    }

                    // Sonido de daño cuando el veneno hace efecto
                    if (ticks % 10 == 0) {
                        target.playSound(target.getLocation(), Sound.BLOCK_HONEY_BLOCK_FALL, 1.5f, 1.6f);
                    }

                    ticks++;
                }
            };

            drainTask.runTaskTimer(plugin, 0L, 1L);
            activeDrainTasks.put(target.getUniqueId(), drainTask);

            // Guardar referencia a la BossBar en los metadatos del jugador
            target.setMetadata("DrainBossBar", new FixedMetadataValue(plugin, bossBar));
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                isSpecialAttackActive.put(wither.getUniqueId(), false);
            }
        }.runTaskLater(plugin, 40L);
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() != Material.HONEY_BOTTLE) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (activeDrainTasks.containsKey(playerId)) {
            // Cancelar el drenaje
            activeDrainTasks.get(playerId).cancel();
            activeDrainTasks.remove(playerId);

            // Limpiar efectos
            player.removePotionEffect(PotionEffectType.POISON);

            // Eliminar BossBar si existe
            if (player.hasMetadata("DrainBossBar")) {
                BossBar bossBar = (BossBar) player.getMetadata("DrainBossBar").get(0).value();
                bossBar.removeAll();
                player.removeMetadata("DrainBossBar", plugin);
            }

            player.spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 5);

            // Mostrar notificación
            successNotification.showSuccess(player);
        }
    }

    private void skullEffectAttack(Wither wither) {
        isSpecialAttackActive.put(wither.getUniqueId(), true);
        wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 2.0f, 0.7f);

        // Crear calavera flotante con BlockDisplay
        Location skullLocation = wither.getLocation().add(0, 3, 0);
        BlockDisplay skull = wither.getWorld().spawn(skullLocation, BlockDisplay.class);

        skull.setBlock(Bukkit.createBlockData(Material.WITHER_SKELETON_SKULL));
        skull.setInvulnerable(true);
        skull.setGravity(false);
        skull.setGlowing(true);
        skull.setGlowColorOverride(Color.YELLOW); // Color amarillo para el glowing
        skull.setBrightness(new Display.Brightness(15, 15)); // Máxima luminosidad

        // Ajustar transformación para que se vea como una cabeza flotante
        skull.setTransformation(new Transformation(
                new Vector3f(), // posición
                new AxisAngle4f(0, 0, 0, 0), // rotación
                new Vector3f(2, 2, 2), // escala (más grande)
                new AxisAngle4f(0, 0, 0, 0) // escala de rotación
        ));

        // Efecto de partículas alrededor de la calavera
        new BukkitRunnable() {
            @Override
            public void run() {
                if (skull.isDead()) {
                    cancel();
                    return;
                }

                // Partículas amarillas alrededor del cráneo
                skull.getWorld().spawnParticle(
                        Particle.POOF,
                        skull.getLocation(),
                        20,
                        0.5, 0.5, 0.5
                );

                // Rotación lenta del cráneo
                skull.setTransformation(new Transformation(
                        new Vector3f(),
                        new AxisAngle4f(0, 1, 0, (float) Math.toRadians(skull.getTicksLived() * 2)), // Rotación gradual
                        new Vector3f(2, 2, 2),
                        new AxisAngle4f(0, 0, 0, 0)
                ));
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Después de 1 segundo, aplicar efectos a jugadores cercanos
        new BukkitRunnable() {
            @Override
            public void run() {
                // Aplicar efectos a jugadores en radio 10x10
                for (Entity entity : skull.getNearbyEntities(SKULL_EFFECT_RADIUS, SKULL_EFFECT_RADIUS, SKULL_EFFECT_RADIUS)) {
                    if (entity instanceof Player player) {
                        // Veneno X (nivel 5) por 10 segundos
                        player.addPotionEffect(new PotionEffect(
                                PotionEffectType.NAUSEA,
                                SKULL_EFFECT_DURATION,
                                4,
                                false,
                                true,
                                true // partículas visibles
                        ));

                        // Salto alto 5 por 10 segundos
                        player.addPotionEffect(new PotionEffect(
                                PotionEffectType.JUMP_BOOST,
                                SKULL_EFFECT_DURATION,
                                4,
                                false,
                                true,
                                true
                        ));

                        // Oscuridad por 10 segundos
                        player.addPotionEffect(new PotionEffect(
                                PotionEffectType.DARKNESS,
                                SKULL_EFFECT_DURATION,
                                0,
                                false,
                                true,
                                true
                        ));

                        // Efecto visual en el jugador
                        player.getWorld().spawnParticle(
                                Particle.WITCH,
                                player.getEyeLocation(),
                                15,
                                0.5, 0.5, 0.5,
                                0.1
                        );
                    }
                }

                // Efecto visual y sonoro de explosión
                skull.getWorld().playSound(skull.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.5f);
                skull.getWorld().playSound(skull.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1.5f, 0.8f);

                // Explosión de partículas
                skull.getWorld().spawnParticle(
                        Particle.EXPLOSION_EMITTER,
                        skull.getLocation(),
                        5,
                        1, 1, 1
                );

                // Onda de choque amarilla
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

                // Eliminar la calavera
                skull.remove();

                // Finalizar ataque
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        isSpecialAttackActive.put(wither.getUniqueId(), false);
                    }
                }.runTaskLater(plugin, 40L);
            }
        }.runTaskLater(plugin, 20L); // 1 segundo de espera
    }

    private void ringAttack(Wither wither) {
        isSpecialAttackActive.put(wither.getUniqueId(), true);
        World world = wither.getWorld();

        // Sonidos de preparación
        world.playSound(wither.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.7f);
        world.playSound(wither.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.5f);

        // Obtener jugadores en un radio de 15 bloques
        List<Player> players = world.getNearbyEntities(wither.getLocation(), 15, 15, 15).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player)e)
                .filter(this::shouldAffectPlayer)
                .collect(Collectors.toList());

        if (players.isEmpty()) {
            isSpecialAttackActive.put(wither.getUniqueId(), false);
            return;
        }

        // Ajuste de cantidad de rings por fase

        int ringsToSpawn = isPhaseTwo.get(wither.getUniqueId()) ? 2 : 1;
        List<RingData> rings = new ArrayList<>();

        // Crear anillos sobre jugadores aleatorios
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

        // Task principal del ataque
        new BukkitRunnable() {
            int ticks = 0;
            final Set<UUID> activeSkulls = new HashSet<>();

            @Override
            public void run() {
                if (ticks >= RING_ATTACK_DURATION || wither.isDead()) {
                    endAttack(rings, wither);
                    cancel();
                    return;
                }

                // Rotar y manejar anillos
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

    private Location findSafeRingLocation(Location playerLoc) {
        Location potentialLoc = playerLoc.clone().add(0, 12, 0);
        World world = playerLoc.getWorld();

        // Buscar hacia abajo hasta encontrar aire seguro (máximo 12 bloques)
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
            // Posición dentro del anillo
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 1.5 + random.nextDouble() * 2;
            Location spawnLoc = ring.center.clone().add(
                    Math.cos(angle) * distance,
                    0,
                    Math.sin(angle) * distance
            );

            // Configuración del cráneo
            WitherSkull skull = world.spawn(spawnLoc, WitherSkull.class);
            skull.setShooter(source);
            skull.setCharged(true);
            skull.setGlowing(true);
            skull.setMetadata("UltraWitherSkull", new FixedMetadataValue(plugin, true));
            skull.setMetadata("Explosive", new FixedMetadataValue(plugin, true));
            skull.setMetadata("NoWitherDamage", new FixedMetadataValue(plugin, true));

            // Dirección controlada (65-80 grados)
            double pitch = Math.toRadians(65 + random.nextDouble() * 15);
            Vector direction = new Vector(
                    -Math.sin(angle) * Math.cos(pitch),
                    -Math.sin(pitch),
                    Math.cos(angle) * Math.cos(pitch)
            ).normalize();

            skull.setVelocity(direction.multiply(1.6)); // Velocidad aumentada

            // Configurar para ignorar colisiones con otros proyectiles
            skull.setMetadata("IgnoreCollisions", new FixedMetadataValue(plugin, true));

            // Efectos
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

    private void endAttack(List<RingData> rings, Wither wither) {
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

    // Añade este handler para evitar colisiones entre proyectiles
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

    private void startPhaseTwo(Wither wither) {
        isPhaseTwo.put(wither.getUniqueId(), true);
        isSpecialAttackActive.put(wither.getUniqueId(), true);

        // Preparación inicial
        wither.setInvulnerable(true);
        wither.setAI(false);
        wither.setGlowing(true);
        wither.setCustomName(ChatColor.DARK_RED + "Corrupted Wither Boss - Fase 2");

        // Sonido de inicio de transición (más fuerte)
        wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_WITHER_SPAWN, 5.0f, 0.7f);

        new BukkitRunnable() {
            int beamsSpawned = 0;
            final Location witherLoc = wither.getLocation().clone();
            final double witherHeight = 3.0; // Altura aproximada del Wither

            @Override
            public void run() {
                if (beamsSpawned >= 5 || wither.isDead()) {
                    // Sonido de carga completa
                    wither.getWorld().playSound(witherLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 5.0f, 0.8f);

                    // Esperar 1 segundo antes de la explosión final
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            // Explosión final con sonic boom
                            Location explosionLoc = witherLoc.clone().add(0, witherHeight/2, 0);
                            explosionLoc.getWorld().spawnParticle(Particle.SONIC_BOOM,
                                    explosionLoc, 50, 2, 2, 2, 0.5);

                            // Sonidos más fuertes
                            explosionLoc.getWorld().playSound(explosionLoc,
                                    Sound.ENTITY_WARDEN_SONIC_BOOM, 5.0f, 0.7f);
                            explosionLoc.getWorld().playSound(explosionLoc,
                                    Sound.ENTITY_GENERIC_EXPLODE, 5.0f, 0.5f);

                            // Finalizar transición
                            wither.setInvulnerable(false);
                            wither.setAI(true);
                            isSpecialAttackActive.put(wither.getUniqueId(), false);
                        }
                    }.runTaskLater(plugin, 20L);

                    cancel();
                    return;
                }

                // Calcular dirección del rayo (apuntando al centro del Wither)
                double angle = beamsSpawned * (2 * Math.PI / 5); // 5 rayos distribuidos circularmente
                double distance = 10.0; // Distancia inicial
                double heightOffset = witherHeight * 0.7; // 70% de la altura del Wither

                // Posición inicial del rayo (en un círculo alrededor del Wither)
                Location beamStart = witherLoc.clone().add(
                        Math.cos(angle) * distance,
                        heightOffset,
                        Math.sin(angle) * distance
                );

                // Dirección hacia el centro del Wither
                Vector direction = witherLoc.clone().add(0, heightOffset, 0)
                        .subtract(beamStart).toVector().normalize();

                // Sonido del rayo (más fuerte)
                witherLoc.getWorld().playSound(beamStart,
                        Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 1.0f);
                witherLoc.getWorld().playSound(beamStart,
                        Sound.BLOCK_BEACON_ACTIVATE, 3.0f, 0.8f);

                // Animación del rayo moviéndose hacia el wither
                new BukkitRunnable() {
                    int steps = 0;
                    final Location currentPos = beamStart.clone();
                    final Vector step = direction.clone().multiply(0.7); // Velocidad aumentada
                    final double targetDistance = 1.5; // Distancia para considerar impacto

                    @Override
                    public void run() {
                        if (steps >= 25 || wither.isDead()) { // Tiempo aumentado a 1.25 segundos
                            cancel();
                            return;
                        }

                        // Mover posición actual
                        currentPos.add(step);

                        // Dibujar partículas del rayo (más intensas)
                        witherLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                                currentPos, 10, 0.1, 0.1, 0.1, 0.2);
                        witherLoc.getWorld().spawnParticle(Particle.FIREWORK,
                                currentPos, 5, 0.1, 0.1, 0.1, 0.1);

                        // Cuando llega al wither
                        if (currentPos.distance(witherLoc) < targetDistance) {
                            // Impacto (efectos más intensos)
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
        }.runTaskTimer(plugin, 0L, 15L); // Nuevo rayo cada 0.75 segundos
    }

    private List<Player> getRandomPlayers(Wither wither, int min, int max) {
        List<Player> nearbyPlayers = wither.getWorld().getNearbyEntities(wither.getLocation(), 30, 30, 30).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player)e)
                .filter(this::shouldAffectPlayer) // Filtro añadido
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

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Wither wither && isUltraWither(wither)) {

            if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                    event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                event.setCancelled(true);
                return;
            }
            // Check for death
            if (wither.getHealth() - event.getFinalDamage() <= 0) {
                event.setCancelled(true);
                executeUltraWitherDeath(wither);
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
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (event.isSneaking() && instakillAttempts.containsKey(player.getUniqueId())) {
            int attempts = instakillAttempts.get(player.getUniqueId());
            instakillAttempts.put(player.getUniqueId(), attempts + 1);

            // Feedback con verificación de task activo
            if (shiftSoundTasks.containsKey(player.getUniqueId())) {
                player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 3);
                player.playSound(player.getLocation(), "custom.click_stereo_old", SoundCategory.VOICE, 0.5f, 1.0f + attempts * 0.05f);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Wither wither)) return;
        if (!isUltraWither(wither)) return;

        // Cancelar daño de Smite
        if (event.getDamager() instanceof Player player) {
            ItemStack weapon = player.getInventory().getItemInMainHand();
            if (weapon != null && weapon.containsEnchantment(Enchantment.SMITE)) {
                event.setDamage(event.getDamage() - weapon.getEnchantmentLevel(Enchantment.SMITE) * 2.5);
            }
        }

        Player player = null;

        // Obtener el jugador que causó el daño (manejar tanto ataques directos como proyectiles)
        if (event.getDamager() instanceof Player) {
            player = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player) {
            player = (Player) projectile.getShooter();
        }

        if (player != null) {
            // Registrar el ataque del jugador
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

    private void executeUltraWitherDeath(Wither wither) {
        // Verificar si ya se ejecutó la muerte
        if (deathExecuted.contains(wither.getUniqueId())) {
            return;
        }
        deathExecuted.add(wither.getUniqueId());

        // Death animation
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 40) { // 2 seconds
                    // Final explosion
                    wither.getWorld().spawnParticle(Particle.END_ROD, wither.getLocation(), 100, 2, 2, 2, 0.5);
                    wither.getWorld().playSound(wither.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 2.0f, 0.8f);

                    // Reward players
                    rewardPlayers(wither);

                    activeBehaviors.remove(wither.getUniqueId());
                    playerAttacks.remove(wither.getUniqueId());
                    wither.remove();

                    cancel();
                    return;
                }

                // Rising up
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

                // Intentar añadir al inventario
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(reward);

                // Si no cupo, dropear al suelo
                if (!leftover.isEmpty()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover.get(0));
                }

                player.sendMessage(ChatColor.of("#FFD700") + "" + ChatColor.BOLD + "۞" +
                                ChatColor.RESET + ChatColor.of("#D7B300") + " Has derrotado al" +
                                ChatColor.of("#9400D3") + "" + ChatColor.BOLD + " Ultra Wither Boss");
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }

        // Limpiar el registro de ataques
        playerAttacks.remove(wither.getUniqueId());
    }

    private boolean shouldAffectPlayer(Player player) {
        return player != null &&
                player.isValid() &&
                !player.isDead() &&
                (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE);
    }

    private boolean isUltraWither(Wither wither) {
        return wither != null &&
                wither.getPersistentDataContainer().has(ultraWitherKey, PersistentDataType.BYTE);
    }
}