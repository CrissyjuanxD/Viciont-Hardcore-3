package Dificultades.CustomMobs;

import items.EndItems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.Collectors;

public class GuardianShulker implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey guardianShulkerKey;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Random random = new Random();
    private boolean eventsRegistered = false;
    private Team projectileTeam;
    private final Map<UUID, Boolean> isAttacking = new HashMap<>();

    private static final int BOSSBAR_RANGE = 100;
    private final Map<UUID, BukkitRunnable> bossBarUpdates = new HashMap<>();

    public GuardianShulker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.guardianShulkerKey = new NamespacedKey(plugin, "guardian_shulker");

        // Configurar equipo para el glowing azul
        if (Bukkit.getScoreboardManager().getMainScoreboard().getTeam("GuardianShulkerProjectile") == null) {
            projectileTeam = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam("GuardianShulkerProjectile");
            projectileTeam.setColor(ChatColor.BLUE);
        } else {
            projectileTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam("GuardianShulkerProjectile");
        }
    }

    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
        }
    }

    public void revert() {
        if (eventsRegistered) {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (isGuardianShulker(entity)) {
                        entity.remove();
                    }
                }
            }

            // Limpiar boss bars
            bossBars.values().forEach(BossBar::removeAll);
            bossBars.clear();

            eventsRegistered = false;
        }
    }

    public Shulker spawnGuardianShulker(Location location) {
        Shulker shulker = (Shulker) location.getWorld().spawnEntity(location, EntityType.SHULKER);
        applyGuardianShulkerAttributes(shulker);
        return shulker;
    }

    private void applyGuardianShulkerAttributes(Shulker shulker) {
        shulker.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Guardian Shulker");
        shulker.setCustomNameVisible(true);

        // Atributos
        shulker.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(300);
        shulker.setHealth(300);
        shulker.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(10);
        shulker.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        shulker.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(2.0);

        // Configurar para que siempre esté abierto
        shulker.setPeek(1.0f);
        shulker.setAI(false); // Desactivar AI normal para controlar manualmente

        // Marcar como mob personalizado
        shulker.getPersistentDataContainer().set(guardianShulkerKey, PersistentDataType.BYTE, (byte) 1);

        // Crear boss bar
        BossBar bossBar = Bukkit.createBossBar(
                ChatColor.LIGHT_PURPLE + "Guardian Shulker",
                BarColor.PURPLE,
                BarStyle.SEGMENTED_10
        );
        bossBar.setProgress(1.0);
        bossBar.addPlayer((Player) Bukkit.getOnlinePlayers().toArray()[0]); // Añadir a todos los jugadores en producción
        bossBars.put(shulker.getUniqueId(), bossBar);

        // Iniciar comportamiento del boss
        startBossBehavior(shulker);
    }

    private void startBossBehavior(Shulker shulker) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (shulker.isDead() || !shulker.isValid()) {
                    cancel();
                    return;
                }

                // Verificar si hay jugadores válidos antes de atacar
                if (getRandomPlayer(shulker, 70) == null) {
                    return;
                }

                // Seleccionar ataque aleatorio solo si no está atacando actualmente
                if (shulker.getNoDamageTicks() == 0 && !isAttacking.getOrDefault(shulker.getUniqueId(), false)) {
                    int attack = random.nextInt(5);
                    switch (attack) {
                        case 0 -> launchTNTAttack(shulker);
                        case 1 -> vulnerabilityAttack(shulker);
                        case 2 -> burstAttack(shulker);
                        case 3 -> normalProjectileAttack(shulker);
                        case 4 -> burstAttackV2(shulker);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Revisar cada segundo
    }

    private void startBossBarUpdateTask(Shulker shulker, BossBar bossBar) {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (shulker.isDead()) {
                    cancel();
                    bossBarUpdates.remove(shulker.getUniqueId());
                    return;
                }
                updateBossBarPlayers(shulker, bossBar);
            }
        };
        task.runTaskTimer(plugin, 0L, 20L); // Actualizar cada segundo (20 ticks)
        bossBarUpdates.put(shulker.getUniqueId(), task);
    }

    private void updateBossBarPlayers(Shulker shulker, BossBar bossBar) {
        // Obtener todos los jugadores en el rango
        Collection<Player> playersInRange = shulker.getWorld().getNearbyEntities(shulker.getLocation(), BOSSBAR_RANGE, BOSSBAR_RANGE, BOSSBAR_RANGE)
                .stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player)e)
                .collect(Collectors.toList());

        // Actualizar jugadores de la bossbar
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean shouldHaveBar = playersInRange.contains(player);
            boolean hasBar = bossBar.getPlayers().contains(player);

            if (shouldHaveBar && !hasBar) {
                bossBar.addPlayer(player);
            } else if (!shouldHaveBar && hasBar) {
                bossBar.removePlayer(player);
            }
        }
    }


    private void updateBossBar(Shulker shulker) {
        UUID shulkerId = shulker.getUniqueId();
        BossBar bossBar = bossBars.get(shulkerId);

        if (bossBar != null && !shulker.isDead()) {
            double maxHealth = shulker.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double healthPercentage = Math.max(0.0, shulker.getHealth() / maxHealth);
            bossBar.setProgress(healthPercentage);
            updateBossBarPlayers(shulker, bossBar);
        }
    }

    // Ataque 1: LaunchTNT
    private void launchTNTAttack(Shulker shulker) {
        isAttacking.put(shulker.getUniqueId(), true);
        Location center = shulker.getLocation().add(0, 10, 0);

        // Crear rayo de partículas
        BlockDisplay particleBeam = (BlockDisplay) shulker.getWorld().spawnEntity(shulker.getLocation(), EntityType.BLOCK_DISPLAY);
        particleBeam.setBlock(Material.END_ROD.createBlockData());
        particleBeam.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf(),
                new Vector3f(0.5f, 10f, 0.5f),
                new Quaternionf()
        ));
        particleBeam.setGlowing(true);
        particleBeam.setGlowColorOverride(Color.PURPLE);

        shulker.getWorld().playSound(shulker.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 2.0f, 0.7f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 100 || shulker.isDead()) { // 5 segundos de duración
                    particleBeam.remove();
                    isAttacking.put(shulker.getUniqueId(), false);
                    cancel();
                    return;
                }

                // Girar el shulker
                float yaw = shulker.getLocation().getYaw() + 10f;
                Location newLoc = shulker.getLocation();
                newLoc.setYaw(yaw);
                shulker.teleport(newLoc);

                // Lanzar TNT cada 5 ticks
                if (ticks % 5 == 0) {
                    double angle = Math.toRadians(random.nextDouble() * 360);
                    double distance = 5 + random.nextDouble() * 10; // Entre 5 y 15 bloques

                    TNTPrimed tnt = shulker.getWorld().spawn(center.clone().add(
                            Math.cos(angle) * distance,
                            0,
                            Math.sin(angle) * distance
                    ), TNTPrimed.class);

                    tnt.setFuseTicks(40); // Explotará en 2 segundos
                    tnt.setYield(4.0f); // Explosión más poderosa
                    tnt.setMetadata("NoBlockDamage", new FixedMetadataValue(plugin, true));

                    // Efecto visual
                    shulker.getWorld().spawnParticle(Particle.FLAME, tnt.getLocation(), 5, 0.2, 0.2, 0.2, 0.05);
                }

                // Actualizar posición del rayo
                particleBeam.teleport(shulker.getLocation());

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // Ataque 2: Desprotección
    private void vulnerabilityAttack(Shulker shulker) {
        // Aplicar glowing azul
        projectileTeam.addEntry(shulker.getUniqueId().toString());
        shulker.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0, false, false));

        shulker.getWorld().playSound(shulker.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 0.8f);

        new BukkitRunnable() {
            @Override
            public void run() {
                // Remover glowing
                projectileTeam.removeEntry(shulker.getUniqueId().toString());
                shulker.removePotionEffect(PotionEffectType.GLOWING);
            }
        }.runTaskLater(plugin, 200L); // 10 segundos de vulnerabilidad
    }

    // Ataque 3: Rafaga
    private void burstAttack(Shulker shulker) {
        Player target = getRandomPlayer(shulker, 20);
        if (target == null) return;

        // Lanzar proyectil teletransportador desde una posición más alta
        Location spawnLoc = shulker.getLocation().add(0, 1.5, 0);
        BlockDisplay projectile = (BlockDisplay) shulker.getWorld().spawnEntity(
                spawnLoc,
                EntityType.BLOCK_DISPLAY
        );
        projectile.setBlock(Material.BLACK_CONCRETE.createBlockData());
        projectile.setGlowing(true);
        projectile.setGlowColorOverride(Color.BLACK);
        projectile.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf(),
                new Vector3f(0.4f, 0.4f, 0.4f),
                new Quaternionf()
        ));

        // Calcular dirección
        Vector direction = target.getLocation().add(0, 1, 0)
                .toVector()
                .subtract(spawnLoc.toVector())
                .normalize()
                .add(new Vector(
                        (random.nextDouble() - 0.5) * 0.1,
                        (random.nextDouble() - 0.5) * 0.1,
                        (random.nextDouble() - 0.5) * 0.1
                )).normalize();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 100 || shulker.isDead() || projectile.isDead()) {
                    if (!projectile.isDead()) {
                        projectile.remove();
                    }
                    cancel();
                    return;
                }

                projectile.teleport(projectile.getLocation().add(direction.clone().multiply(0.8)));

                // Verificar colisión con jugadores
                for (Entity nearby : projectile.getNearbyEntities(1.5, 1.5, 1.5)) {
                    if (nearby instanceof Player hitPlayer && hitPlayer.equals(target)) {
                        // Verificar si el jugador está bloqueando con escudo
                        if (!hitPlayer.isBlocking()) {
                            handleBurstHit(shulker, hitPlayer);
                        } else {
                            // Efecto cuando se bloquea el proyectil
                            hitPlayer.getWorld().playSound(hitPlayer.getLocation(),
                                    Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
                        }
                        projectile.remove();
                        cancel();
                        return;
                    }
                }

                // Verificar colisión con bloques
                if (!projectile.getLocation().getBlock().isPassable()) {
                    projectile.remove();
                    cancel();
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void handleBurstHit(Shulker shulker, Player player) {
        // Guardar la dirección original del jugador
        float originalYaw = player.getLocation().getYaw();
        float originalPitch = player.getLocation().getPitch();

        // Teletransportar jugador 5 bloques frente al shulker pero sobre el suelo
        Vector direction = shulker.getLocation().getDirection().normalize();
        Location tpLocation = shulker.getLocation().add(direction.multiply(5));
        tpLocation.setY(shulker.getWorld().getHighestBlockYAt(tpLocation) + 1);

        // Restaurar la dirección original después del teleport
        tpLocation.setYaw(originalYaw);
        tpLocation.setPitch(originalPitch);
        player.teleport(tpLocation);

        // Efectos de sonido y partículas al teletransportar
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 50);

        // Aplicar efectos
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 100, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 255));

        // Crear rayo de partículas
        BlockDisplay beam = (BlockDisplay) shulker.getWorld().spawnEntity(shulker.getLocation(), EntityType.BLOCK_DISPLAY);
        beam.setBlock(Material.END_ROD.createBlockData());
        beam.setGlowing(true);
        beam.setGlowColorOverride(Color.RED);
        beam.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf(),
                new Vector3f(0.5f, 5f, 0.5f),
                new Quaternionf()
        ));

        // Disparar ráfaga de 7 shulker bullets cada medio segundo
        new BukkitRunnable() {
            int shotsFired = 0;

            @Override
            public void run() {
                if (shotsFired >= 7 || shulker.isDead()) {
                    beam.remove();
                    cancel();
                    return;
                }

                beam.teleport(shulker.getLocation());

                ShulkerBullet bullet = shulker.launchProjectile(ShulkerBullet.class);
                bullet.teleport(shulker.getLocation().add(0, 1.5, 0)
                        .add(shulker.getLocation().getDirection().multiply(1.5)));
                bullet.setTarget(player);
                shotsFired++;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    // Ataque 4: NormalProyectil
    private void normalProjectileAttack(Shulker shulker) {
        int projectiles = 1 + random.nextInt(2); // 1-2 proyectiles

        for (int i = 0; i < projectiles; i++) {
            Player target = getRandomPlayer(shulker, 20);
            if (target == null) continue;

            // Añadir delay aleatorio entre proyectiles (0-10 ticks)
            final int delay = i * 10;

            new BukkitRunnable() {
                @Override
                public void run() {
                    // Posición de spawn más alta y centrada
                    Location spawnLoc = shulker.getLocation().add(0, 1.5, 0);

                    BlockDisplay projectile = (BlockDisplay) shulker.getWorld().spawnEntity(
                            spawnLoc,
                            EntityType.BLOCK_DISPLAY
                    );
                    projectile.setBlock(Material.PURPUR_BLOCK.createBlockData());
                    projectile.setGlowing(true);
                    projectile.setGlowColorOverride(Color.PURPLE);
                    projectile.setTransformation(new Transformation(
                            new Vector3f(0, 0, 0),
                            new Quaternionf(),
                            new Vector3f(0.4f, 0.4f, 0.4f),
                            new Quaternionf()
                    ));

                    // Dirección con pequeño offset aleatorio
                    Vector direction = target.getLocation().add(0, 1, 0)
                            .toVector()
                            .subtract(spawnLoc.toVector())
                            .normalize()
                            .add(new Vector(
                                    (random.nextDouble() - 0.5) * 0.2,
                                    (random.nextDouble() - 0.5) * 0.2,
                                    (random.nextDouble() - 0.5) * 0.2
                            )).normalize();

                    new BukkitRunnable() {
                        int ticks = 0;

                        @Override
                        public void run() {
                            if (ticks >= 60 || shulker.isDead() || projectile.isDead()) {
                                if (!projectile.isDead()) {
                                    projectile.remove();
                                }
                                cancel();
                                return;
                            }

                            // Mover proyectil
                            projectile.teleport(projectile.getLocation().add(direction.clone().multiply(0.7)));

                            // Verificar colisión
                            for (Entity nearby : projectile.getNearbyEntities(1.2, 1.2, 1.2)) {
                                if (nearby instanceof Player hitPlayer) {
                                    if (hitPlayer.isBlocking()) {
                                        hitPlayer.getWorld().playSound(hitPlayer.getLocation(),
                                                Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
                                    } else {
                                        hitPlayer.damage(8);
                                        hitPlayer.getWorld().playSound(hitPlayer.getLocation(),
                                                Sound.ENTITY_SHULKER_HURT, 1.0f, 1.5f);
                                    }
                                    projectile.remove();
                                    cancel();
                                    return;
                                }
                            }

                            // Verificar colisión con bloques
                            if (!projectile.getLocation().getBlock().isPassable()) {
                                projectile.remove();
                                cancel();
                                return;
                            }

                            ticks++;
                        }
                    }.runTaskTimer(plugin, 0L, 1L);
                }
            }.runTaskLater(plugin, delay);
        }
    }

    // Ataque 5: RafagaV2
    private void burstAttackV2(Shulker shulker) {
        shulker.getWorld().playSound(shulker.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 2.0f, 0.5f);

        // Crear rayo de partículas central como en handleBurstHit
        BlockDisplay beam = (BlockDisplay) shulker.getWorld().spawnEntity(shulker.getLocation(), EntityType.BLOCK_DISPLAY);
        beam.setBlock(Material.END_ROD.createBlockData());
        beam.setGlowing(true);
        beam.setGlowColorOverride(Color.BLACK);
        beam.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf(),
                new Vector3f(0.5f, 5f, 0.5f),
                new Quaternionf()
        ));

        new BukkitRunnable() {
            int ticks = 0;
            float rotation = 0;
            int shotsFired = 0;

            @Override
            public void run() {
                if (ticks >= 100 || shulker.isDead()) {
                    beam.remove();
                    cancel();
                    return;
                }

                // Girar el shulker
                rotation += 18;
                Location loc = shulker.getLocation();
                loc.setYaw(rotation);
                shulker.teleport(loc);

                // Actualizar posición del rayo
                beam.teleport(shulker.getLocation());

                // Disparar ráfaga de 7 shulker bullets cada medio segundo (10 ticks)
                if (ticks % 10 == 0 && shotsFired < 7) {
                    Player target = getRandomPlayer(shulker, 20);
                    if (target != null) {
                        ShulkerBullet bullet = shulker.launchProjectile(ShulkerBullet.class);
                        bullet.teleport(shulker.getLocation().add(0, 1.5, 0)
                                .add(shulker.getLocation().getDirection().multiply(1.5)));
                        bullet.setTarget(target);
                        shotsFired++;
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Player getRandomPlayer(Shulker shulker, double radius) {
        List<Player> validPlayers = new ArrayList<>();

        for (Entity entity : shulker.getWorld().getNearbyEntities(shulker.getLocation(), radius, radius, radius)) {
            if (entity instanceof Player player) {
                // Solo considerar jugadores en modo supervivencia o aventura
                if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                    validPlayers.add(player);
                }
            }
        }

        if (validPlayers.isEmpty()) return null;
        return validPlayers.get(random.nextInt(validPlayers.size()));
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (isGuardianShulker(event.getEntity())) {
            Shulker shulker = (Shulker) event.getEntity();

            // Resistencia a explosiones
            if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                    event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                event.setCancelled(true);
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> updateBossBar(shulker));
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (isGuardianShulker(event.getEntity())) {
            Shulker shulker = (Shulker) event.getEntity();

            // Verificar si el daño proviene de un proyectil
            if (event.getDamager() instanceof Projectile) {
                // Solo permitir daño si el Shulker está en estado vulnerable (con glowing)
                if (!shulker.hasPotionEffect(PotionEffectType.GLOWING)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof ShulkerBullet bullet &&
                bullet.getShooter() instanceof Shulker shulker &&
                isGuardianShulker(shulker)) {

            if (event.getHitEntity() instanceof Player player) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SHULKER_BULLET_HIT, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof TNTPrimed tnt) {
            if (tnt.hasMetadata("NoBlockDamage")) {
                event.blockList().clear();
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (isGuardianShulker(event.getEntity())) {
            Shulker shulker = (Shulker) event.getEntity();
            UUID shulkerId = shulker.getUniqueId();
            // Limpiar drops
            event.setDroppedExp(100);

            int amount = random.nextInt(2) + 1;
            for (int i = 0; i < amount; i++) {
                shulker.getWorld().dropItemNaturally(shulker.getLocation(), EndItems.createGuardianShulkerShell());
            }

            // Limpiar boss bar
            if (bossBars.containsKey(shulkerId)) {
                BossBar bossBar = bossBars.remove(shulkerId);
                if (bossBar != null) {
                    bossBar.removeAll();
                }
            }

            // Cancelar tarea de actualización de BossBar
            BukkitRunnable updateTask = bossBarUpdates.remove(shulkerId);
            if (updateTask != null) {
                updateTask.cancel();
            }

            // Efectos de muerte
            shulker.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, shulker.getLocation(), 1);
            shulker.getWorld().playSound(shulker.getLocation(), Sound.ENTITY_SHULKER_DEATH, 2.0f, 0.8f);
        }
    }

    public boolean isGuardianShulker(Entity entity) {
        return entity instanceof Shulker &&
                entity.getPersistentDataContainer().has(guardianShulkerKey, PersistentDataType.BYTE);
    }

    public NamespacedKey getGuardianShulkerKey() {
        return guardianShulkerKey;
    }
}