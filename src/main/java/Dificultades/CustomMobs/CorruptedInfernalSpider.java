package Dificultades.CustomMobs;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class CorruptedInfernalSpider implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey corrupedInfernaltedspiderKey;
    private boolean eventsRegistered = false;
    private final Random random = new Random();

    private final List<SpiderEffect> possibleEffects = Arrays.asList(
            new SpiderEffect("Velocidad", PotionEffectType.SPEED, 3),
            new SpiderEffect("Regeneración", PotionEffectType.REGENERATION, 3),
            new SpiderEffect("Fuerza", PotionEffectType.STRENGTH, 3),
            new SpiderEffect("Salto", PotionEffectType.JUMP_BOOST, 3),
            new SpiderEffect("Brillo", PotionEffectType.GLOWING, 1),
            new SpiderEffect("Caída lenta", PotionEffectType.SLOW_FALLING, 1),
            new SpiderEffect("Resistencia", PotionEffectType.RESISTANCE, 2)
    );

    public CorruptedInfernalSpider(JavaPlugin plugin) {
        this.plugin = plugin;
        this.corrupedInfernaltedspiderKey = new NamespacedKey(plugin, "corruptedinfernalspider");
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
                    if (entity instanceof Spider spider &&
                            spider.getPersistentDataContainer().has(corrupedInfernaltedspiderKey, PersistentDataType.BYTE)) {
                        spider.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public Spider spawnCorruptedInfernalSpider(Location location) {
        Spider corruptedInfernalSpider = (Spider) location.getWorld().spawnEntity(location, EntityType.SPIDER);
        applyCorruptedInfernalSpiderAttributes(corruptedInfernalSpider);
        return corruptedInfernalSpider;

    }

    public void transformspawnCorruptedInfernalSpider(Spider spider) {
        applyCorruptedInfernalSpiderAttributes(spider);
    }

    private void applyCorruptedInfernalSpiderAttributes(Spider spider) {
        spider.setCustomName(ChatColor.RED + "" + ChatColor.BOLD + "Corrupted Infernal Spider");
        spider.setCustomNameVisible(true);
        spider.getPersistentDataContainer().set(corrupedInfernaltedspiderKey, PersistentDataType.BYTE, (byte) 1);
        Objects.requireNonNull(spider.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(3.0);

        int numEffects = 3 + random.nextInt(3);
        Collections.shuffle(possibleEffects);

        for (int i = 0; i < numEffects && i < possibleEffects.size(); i++) {
            SpiderEffect effect = possibleEffects.get(i);
            spider.addPotionEffect(new PotionEffect(
                    effect.type(),
                    Integer.MAX_VALUE,
                    effect.amplifier() - 1,
                    true,
                    true
            ));
        }
    }

    @EventHandler
    public void onSpiderHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Spider spider && event.getEntity() instanceof Player player) {

            if (spider.getPersistentDataContainer().has(corrupedInfernaltedspiderKey, PersistentDataType.BYTE)) {
                if (!player.isBlocking()) {
                    player.getLocation().getBlock().setType(Material.COBWEB);
                    player.setFireTicks(Integer.MAX_VALUE);
                }
            }
        }
    }

    @EventHandler
    public void onSpiderTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Spider spider && event.getTarget() instanceof Player) {

            if (spider.getPersistentDataContainer().has(corrupedInfernaltedspiderKey, PersistentDataType.BYTE)) {
                if (random.nextDouble() < 0.2) {
                    launchFireballAttack(spider, (Player) event.getTarget());
                }
            }
        }
    }

    private void launchFireballAttack(Spider spider, Player target) {
        Location loc = spider.getLocation();
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 50, 1, 1, 1, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                Fireball fireball = spider.launchProjectile(Fireball.class);
                Vector direction = target.getLocation().toVector().subtract(spider.getLocation().toVector()).normalize();
                fireball.setDirection(direction);
                fireball.setYield(3.0f);
                fireball.setIsIncendiary(true);

                fireball.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, "infernalspiderfireball"),
                        PersistentDataType.BYTE,
                        (byte)1
                );
            }
        }.runTaskLater(plugin, 10L);
    }

    @EventHandler
    public void onFireballHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Fireball fireball) {

            if (fireball.getPersistentDataContainer().has(
                    new NamespacedKey(plugin, "infernalspiderfireball"),
                    PersistentDataType.BYTE)) {

                if (event.getEntity() instanceof Spider &&
                        event.getEntity().getPersistentDataContainer().has(
                                corrupedInfernaltedspiderKey,
                                PersistentDataType.BYTE)) {

                    event.setCancelled(true);
                }
            }
        }
    }

    public NamespacedKey getCorruptedInfernalKey() {
        return corrupedInfernaltedspiderKey;
    }

    public boolean isCorruptedInfernalSpider(Spider spider) {
        return spider.getPersistentDataContainer().has(corrupedInfernaltedspiderKey, PersistentDataType.BYTE);
    }

        private record SpiderEffect(String name, PotionEffectType type, int amplifier) {
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Location playerLocation = player.getLocation();
        double maxDistanceSquared = 30 * 30;

        for (Entity entity : player.getNearbyEntities(30, 30, 30)) {
            if (entity instanceof Spider spider &&
                    spider.getCustomName() != null &&
                    spider.getCustomName().equals(ChatColor.RED + "" + ChatColor.BOLD + "Corrupted Infernal Spider") &&
                    !spider.getPersistentDataContainer().has(corrupedInfernaltedspiderKey, PersistentDataType.BYTE)) {

                if (playerLocation.distanceSquared(spider.getLocation()) <= maxDistanceSquared) {
                    transformspawnCorruptedInfernalSpider(spider);
                }
            }
        }
    }

}
