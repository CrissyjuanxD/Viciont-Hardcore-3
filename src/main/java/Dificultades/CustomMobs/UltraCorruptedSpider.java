package Dificultades.CustomMobs;

import items.ItemsTotems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
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
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.*;

public class UltraCorruptedSpider implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey ultraCorruptedSpiderKey;
    private boolean eventsRegistered = false;
    private final Random random = new Random();
    private final Map<UUID, BukkitRunnable> webTasks = new HashMap<>();

    private final List<SpiderEffect> possibleEffects = Arrays.asList(
            new SpiderEffect("Velocidad", PotionEffectType.SPEED, 3),
            new SpiderEffect("Regeneración", PotionEffectType.REGENERATION, 3),
            new SpiderEffect("Fuerza", PotionEffectType.STRENGTH, 3),
            new SpiderEffect("Salto", PotionEffectType.JUMP_BOOST, 3),
            new SpiderEffect("Brillo", PotionEffectType.GLOWING, 1),
            new SpiderEffect("Caída lenta", PotionEffectType.SLOW_FALLING, 1),
            new SpiderEffect("Resistencia", PotionEffectType.RESISTANCE, 2)
    );

    public UltraCorruptedSpider(JavaPlugin plugin) {
        this.plugin = plugin;
        this.ultraCorruptedSpiderKey = new NamespacedKey(plugin, "ultra_corrupted_spider");
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
                    if (entity instanceof Spider spider && isUltraCorruptedSpider(spider)) {
                        spider.remove();
                    }
                }
            }

            for (BukkitRunnable task : webTasks.values()) {
                task.cancel();
            }
            webTasks.clear();

            eventsRegistered = false;
        }
    }

    public Spider spawnUltraCorruptedSpider(Location location) {
        Spider spider = (Spider) location.getWorld().spawnEntity(location, EntityType.SPIDER);
        applyUltraCorruptedSpiderAttributes(spider);
        return spider;
    }

    public void transformspawnUltraCorruptedSpider(Spider spider) {
        applyUltraCorruptedSpiderAttributes(spider);
    }

    private void applyUltraCorruptedSpiderAttributes(Spider spider) {
        spider.setCustomName(ChatColor.GREEN + "" + ChatColor.BOLD + "Ultra Corrupted Spider");
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

    @EventHandler
    public void onSpiderHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Spider spider &&
                isUltraCorruptedSpider(spider) &&
                event.getEntity() instanceof LivingEntity target) {

            target.addPotionEffect(new PotionEffect(
                    PotionEffectType.POISON,
                    600,
                    1,
                    true,
                    true
            ));

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
        if (event.getEntity() instanceof Spider spider &&
                isUltraCorruptedSpider(spider) &&
                event.getTarget() != null) {

            if (random.nextDouble() < 0.25) {
                launchWebProjectiles(spider);
            }
        }
    }

    private void launchWebProjectiles(Spider spider) {
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

            new BukkitRunnable() {
                int ticks = 0;
                Location currentLoc = webProjectile.getLocation().clone();

                @Override
                public void run() {
                    if (webProjectile.isDead() || ticks >= 100) {
                        webProjectile.remove();
                        this.cancel();
                        return;
                    }

                    currentLoc.add(direction);
                    webProjectile.teleport(currentLoc);

                    webProjectile.getWorld().spawnParticle(
                            Particle.POOF,
                            webProjectile.getLocation(),
                            3,
                            0.1, 0.1, 0.1, 0.02
                    );

                    for (Entity nearby : webProjectile.getNearbyEntities(1.5, 1.5, 1.5)) {
                        if (nearby instanceof Player hitPlayer) {
                            handleWebHit(hitPlayer, webProjectile.getLocation());
                            webProjectile.remove();
                            this.cancel();
                            return;
                        }
                    }

                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    private void handleWebHit(Player player, Location impactLocation) {
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            return;
        }

        if (player.isBlocking()) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS,
                    100,
                    2,
                    true,
                    true
            ));

            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.WEAKNESS,
                    100,
                    1,
                    true,
                    true
            ));

            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.8f);
            return;
        }


        createWebCube(player.getLocation());
        scheduleWebRemoval(player.getLocation());
    }

    private void createWebCube(Location center) {
        // Asegurarnos de que el centro esté en el bloque exacto
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

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Spider spider && isUltraCorruptedSpider(spider)) {
            Location loc = spider.getLocation();

            double baseDropChance = 0.50;
            double lootingBonus = 0;
            double doubleDropChance = 0;

            if (spider.getKiller() != null) {
                ItemStack weapon = spider.getKiller().getInventory().getItemInMainHand();
                if (weapon != null && weapon.getEnchantments().containsKey(Enchantment.LOOTING)) {
                    int lootingLevel = weapon.getEnchantmentLevel(Enchantment.LOOTING);

                    switch (lootingLevel) {
                        case 1:
                            lootingBonus = 0.10;
                            break;
                        case 2:
                            lootingBonus = 0.20;
                            break;
                        case 3:
                            lootingBonus = 0.25;
                            doubleDropChance = 0.30;
                            break;
                    }
                }
            }

            double totalDropChance = baseDropChance + lootingBonus;

            if (Math.random() <= totalDropChance) {
                ItemStack eye = ItemsTotems.createUltraCorruptedSpiderEye();

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
        if (event.getEntity() instanceof Spider spider && isUltraCorruptedSpider(spider)) {
            spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_HURT, 1.5f, 1.8f);
        }
    }

    public NamespacedKey getUltraCorruptedSpiderKey() {
        return ultraCorruptedSpiderKey;
    }

    public boolean isUltraCorruptedSpider(Spider spider) {
        return spider.getPersistentDataContainer().has(ultraCorruptedSpiderKey, PersistentDataType.BYTE);
    }

    private record SpiderEffect(String name, PotionEffectType type, int amplifier) {}
}