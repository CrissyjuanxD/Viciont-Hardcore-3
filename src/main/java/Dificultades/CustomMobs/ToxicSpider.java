package Dificultades.CustomMobs;

import items.ItemsTotems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.*;

public class ToxicSpider implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey ultraCorruptedSpiderKey;
    private static boolean eventsRegistered = false;
    private final Random random = new Random();
    // La remoción de webs es lenta y no crítica, map está bien
    private final Map<UUID, BukkitRunnable> webTasks = new HashMap<>();
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();

    // --- OPTIMIZACIÓN DE PROYECTILES ---
    private static final List<ProjectileData> activeProjectiles = new ArrayList<>();
    private static BukkitTask projectileTask;

    private static class ProjectileData {
        final BlockDisplay display;
        final Vector direction;
        final ProjectileType type;
        int ticksAlive = 0;

        ProjectileData(BlockDisplay display, Vector direction, ProjectileType type) {
            this.display = display;
            this.direction = direction;
            this.type = type;
        }
    }

    private enum ProjectileType { WEB, POISON }
    // -----------------------------------

    private final List<SpiderEffect> possibleEffects = Arrays.asList(
            new SpiderEffect("Velocidad", PotionEffectType.SPEED, 3),
            new SpiderEffect("Regeneración", PotionEffectType.REGENERATION, 2),
            new SpiderEffect("Fuerza", PotionEffectType.STRENGTH, 3),
            new SpiderEffect("Salto", PotionEffectType.JUMP_BOOST, 3),
            new SpiderEffect("Brillo", PotionEffectType.GLOWING, 1),
            new SpiderEffect("Caída lenta", PotionEffectType.SLOW_FALLING, 1),
            new SpiderEffect("Resistencia", PotionEffectType.RESISTANCE, 2)
    );

    public ToxicSpider(JavaPlugin plugin) {
        this.plugin = plugin;
        this.ultraCorruptedSpiderKey = new NamespacedKey(plugin, "ultra_corrupted_spider");
    }

    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
            startProjectileTask();
        }
    }

    public void revert() {
        if (eventsRegistered) {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof CaveSpider spider && isToxicSpider(spider)) {
                        spider.remove();
                    }
                }
            }

            // Limpiar proyectiles custom
            for (ProjectileData pd : activeProjectiles) {
                pd.display.remove();
            }
            activeProjectiles.clear();
            if (projectileTask != null) projectileTask.cancel();

            for (BukkitRunnable task : webTasks.values()) {
                task.cancel();
            }
            webTasks.clear();

            eventsRegistered = false;
        }
    }

    private void startProjectileTask() {
        if (projectileTask != null && !projectileTask.isCancelled()) return;

        // Tarea rápida (1 tick) para mover proyectiles suavemente
        projectileTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeProjectiles.isEmpty()) return;

                Iterator<ProjectileData> it = activeProjectiles.iterator();
                while (it.hasNext()) {
                    ProjectileData pd = it.next();

                    if (pd.display.isDead() || !pd.display.isValid() || pd.ticksAlive >= 100) {
                        pd.display.remove();
                        it.remove();
                        continue;
                    }

                    // Movimiento
                    pd.display.teleport(pd.display.getLocation().add(pd.direction));
                    pd.ticksAlive++;

                    // Partículas
                    if (pd.type == ProjectileType.WEB) {
                        pd.display.getWorld().spawnParticle(Particle.POOF, pd.display.getLocation(), 3, 0.1, 0.1, 0.1, 0.02);
                    } else {
                        pd.display.getWorld().spawnParticle(Particle.WITCH, pd.display.getLocation(), 2, 0.1, 0.1, 0.1, 0.01);
                    }

                    // Colisión simple
                    double radius = (pd.type == ProjectileType.WEB) ? 1.5 : 1.0;
                    boolean hit = false;

                    for (Entity nearby : pd.display.getNearbyEntities(radius, radius, radius)) {
                        if (nearby instanceof Player hitPlayer) {
                            if (pd.type == ProjectileType.WEB) {
                                handleWebHit(hitPlayer, pd.display.getLocation());
                            } else {
                                handlePoisonHit(hitPlayer);
                            }
                            hit = true;
                            break; // Solo golpea al primero
                        }
                    }

                    if (hit) {
                        pd.display.remove();
                        it.remove();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ... Resto de métodos de spawn (spawnToxicSpider, etc) sin cambios ...

    public CaveSpider spawnToxicSpider(Location location) {
        CaveSpider spider = (CaveSpider) location.getWorld().spawnEntity(location, EntityType.CAVE_SPIDER);
        applyToxicSpiderAttributes(spider);
        return spider;
    }

    public void transformspawnToxicSpider(CaveSpider spider) {
        applyToxicSpiderAttributes(spider);
    }

    public void transformToToxicSpider(Spider spider) {
        Location loc = spider.getLocation();
        CaveSpider caveSpider = (CaveSpider) loc.getWorld().spawnEntity(loc, EntityType.CAVE_SPIDER);
        applyToxicSpiderAttributes(caveSpider);
        spider.remove();
    }

    private void applyToxicSpiderAttributes(CaveSpider spider) {
        spider.setCustomName(ChatColor.GREEN + "" + ChatColor.BOLD + "Toxic Spider");
        spider.setCustomNameVisible(false);
        spider.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40);
        spider.setHealth(40);
        spider.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(6.0);
        spider.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(64);
        spider.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(1.2);

        int numEffects = 2 + random.nextInt(3);
        Collections.shuffle(possibleEffects);

        for (int i = 0; i < numEffects && i < possibleEffects.size(); i++) {
            SpiderEffect effect = possibleEffects.get(i);
            spider.addPotionEffect(new PotionEffect(
                    effect.type(),
                    Integer.MAX_VALUE,
                    effect.amplifier(),
                    true,
                    true
            ));
        }

        spider.getPersistentDataContainer().set(ultraCorruptedSpiderKey, PersistentDataType.BYTE, (byte) 1);
    }

    // ... Event handlers (onSpiderHit, etc) sin cambios ...

    @EventHandler
    public void onSpiderHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof CaveSpider spider &&
                isToxicSpider(spider) &&
                event.getEntity() instanceof LivingEntity target) {

            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 600, 1, true, true));

            if (target instanceof Player player) {
                if (!player.isBlocking()) {
                    target.getLocation().getBlock().setType(Material.COBWEB);
                }
                scheduleWebRemoval(target.getLocation());
            }
        }
    }

    @EventHandler
    public void onSpiderTarget(EntityTargetLivingEntityEvent event) {
        if (event.getEntity() instanceof CaveSpider spider &&
                isToxicSpider(spider) &&
                event.getTarget() != null) {

            double chance = random.nextDouble();
            if (chance < 0.25) {
                launchPoisonProjectile(spider, event.getTarget());
            } else if (chance < 0.50) {
                launchWebProjectiles(spider);
            }
        }
    }

    private void launchWebProjectiles(CaveSpider spider) {
        List<Player> targets = new ArrayList<>();
        for (Entity entity : spider.getNearbyEntities(20, 20, 20)) {
            if (entity instanceof Player player) {
                if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                    targets.add(player);
                }
            }
        }
        if (targets.isEmpty()) return;

        int projectiles = 2 + random.nextInt(3);
        Collections.shuffle(targets);

        spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 2.0f, 0.5f);

        for (int i = 0; i < Math.min(projectiles, targets.size()); i++) {
            Player target = targets.get(i);

            BlockDisplay webProjectile = (BlockDisplay) spider.getWorld().spawnEntity(
                    spider.getEyeLocation(),
                    EntityType.BLOCK_DISPLAY
            );
            webProjectile.setBlock(Material.COBWEB.createBlockData());
            webProjectile.setGlowing(true);
            webProjectile.setGlowColorOverride(Color.GREEN);
            webProjectile.setInvulnerable(true);

            Transformation transformation = webProjectile.getTransformation();
            transformation.getScale().set(0.5f, 0.5f, 0.5f);
            webProjectile.setTransformation(transformation);

            Vector direction = target.getEyeLocation().toVector()
                    .subtract(spider.getEyeLocation().toVector())
                    .normalize()
                    .multiply(1);

            // Agregar al manager
            activeProjectiles.add(new ProjectileData(webProjectile, direction, ProjectileType.WEB));
        }
    }

    private void launchPoisonProjectile(CaveSpider spider, LivingEntity target) {
        if (!(target instanceof Player player)) return;

        BlockDisplay poisonProjectile = (BlockDisplay) spider.getWorld().spawnEntity(
                spider.getEyeLocation(),
                EntityType.BLOCK_DISPLAY
        );
        poisonProjectile.setBlock(Material.GREEN_STAINED_GLASS.createBlockData());
        poisonProjectile.setGlowing(true);
        poisonProjectile.setGlowColorOverride(Color.LIME);
        poisonProjectile.setInvulnerable(true);

        Transformation transformation = poisonProjectile.getTransformation();
        transformation.getScale().set(0.3f, 0.3f, 0.3f);
        poisonProjectile.setTransformation(transformation);

        Vector direction = target.getEyeLocation().toVector()
                .subtract(spider.getEyeLocation().toVector())
                .normalize()
                .multiply(0.8);

        spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_WITCH_THROW, 1.0f, 0.7f);

        // Agregar al manager
        activeProjectiles.add(new ProjectileData(poisonProjectile, direction, ProjectileType.POISON));
    }

    private void handleWebHit(Player player, Location impactLocation) {
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;

        if (player.isBlocking()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1, true, true));
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.8f);
            return;
        }

        createWebCube(player.getLocation());
        scheduleWebRemoval(player.getLocation());
    }

    private void createWebCube(Location center) {
        Location centeredLocation = center.getBlock().getLocation().add(0.5, 0, 0.5);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Location blockLoc = centeredLocation.clone().add(x, y, z);
                    if (blockLoc.getBlock().getType().isAir() ||
                            blockLoc.getBlock().getType() == Material.WATER ||
                            blockLoc.getBlock().getType() == Material.LAVA) {
                        blockLoc.getBlock().setType(Material.COBWEB);
                    }
                }
            }
        }
        if (center.getBlock().getType().isAir()) {
            center.getBlock().setType(Material.COBWEB);
        }
    }

    private void scheduleWebRemoval(Location center) {
        UUID taskId = UUID.randomUUID();
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            Location blockLoc = center.clone().add(x, y, z);
                            if (blockLoc.getBlock().getType() == Material.COBWEB) {
                                blockLoc.getBlock().setType(Material.AIR);
                            }
                        }
                    }
                }
                webTasks.remove(taskId);
            }
        };
        webTasks.put(taskId, task);
        task.runTaskLater(plugin, 1200L);
    }

    private void handlePoisonHit(Player player) {
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;

        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 1200, 5, true, true));

        if (activeBossBars.containsKey(player.getUniqueId())) {
            BossBar existingBar = activeBossBars.get(player.getUniqueId());
            existingBar.removeAll();
            activeBossBars.remove(player.getUniqueId());
        }

        BossBar bossBar = Bukkit.createBossBar("\uEAA7", BarColor.WHITE, BarStyle.SOLID);
        bossBar.addPlayer(player);
        bossBar.setVisible(true);
        bossBar.setProgress(1.0);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 0.5f);

        // Esta tarea es temporal para el jugador, aceptable
        new BukkitRunnable() {
            double timeLeft = 15.0;
            final UUID playerUUID = player.getUniqueId();

            @Override
            public void run() {
                if (!player.isOnline()) {
                    bossBar.removeAll();
                    activeBossBars.remove(playerUUID);
                    this.cancel();
                    return;
                }
                timeLeft -= 1.0;
                double progress = Math.max(0.0, timeLeft / 15.0);
                bossBar.setProgress(progress);

                if (timeLeft <= 0) {
                    bossBar.removeAll();
                    activeBossBars.remove(playerUUID);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        activeBossBars.put(player.getUniqueId(), bossBar);
    }

    // ... Resto de eventos de daño/muerte sin cambios ...
    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof CaveSpider spider && isToxicSpider(spider)) {
            Location loc = spider.getLocation();
            double baseDropChance = 0.40;
            double lootingBonus = 0;
            double doubleDropChance = 0;

            if (spider.getKiller() != null) {
                ItemStack weapon = spider.getKiller().getInventory().getItemInMainHand();
                if (weapon != null && weapon.getEnchantments().containsKey(Enchantment.LOOTING)) {
                    int lootingLevel = weapon.getEnchantmentLevel(Enchantment.LOOTING);
                    switch (lootingLevel) {
                        case 1 -> lootingBonus = 0.10;
                        case 2 -> lootingBonus = 0.20;
                        case 3 -> {
                            lootingBonus = 0.25;
                            doubleDropChance = 0.30;
                        }
                    }
                }
            }

            if (Math.random() <= (baseDropChance + lootingBonus)) {
                ItemStack eye = ItemsTotems.createToxicSpiderEye();
                if (doubleDropChance > 0 && Math.random() <= doubleDropChance) {
                    eye.setAmount(2);
                }
                spider.getWorld().dropItemNaturally(loc, eye);
            }
            spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_DEATH, 2.0f, 1.8f);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof CaveSpider spider && isToxicSpider(spider)) {
            spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_HURT, 1.5f, 1.8f);
        }
    }

    public NamespacedKey getUltraCorruptedSpiderKey() {
        return ultraCorruptedSpiderKey;
    }

    public boolean isToxicSpider(CaveSpider spider) {
        return spider.getPersistentDataContainer().has(ultraCorruptedSpiderKey, PersistentDataType.BYTE);
    }

    private record SpiderEffect(String name, PotionEffectType type, int amplifier) {}
}