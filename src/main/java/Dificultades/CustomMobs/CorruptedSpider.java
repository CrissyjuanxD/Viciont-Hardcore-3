package Dificultades.CustomMobs;

import Dificultades.Features.MobSoundManager;
import items.CorruptedMobItems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;

public class CorruptedSpider implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey corrupedtedspiderKey;
    private boolean eventsRegistered = false;
    private final MobSoundManager soundManager;

    public CorruptedSpider(JavaPlugin plugin) {
        this.plugin = plugin;
        this.corrupedtedspiderKey = new NamespacedKey(plugin, "corruptedspider");
        this.soundManager = new MobSoundManager(plugin);
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
                    if (entity instanceof Spider spider && isCorruptedSpider(spider)) {
                        spider.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public Spider spawnCorruptedSpider(Location location) {
        Spider corruptedSpider = (Spider) location.getWorld().spawnEntity(location, EntityType.SPIDER);
        applyCorruptedSpiderAttributes(corruptedSpider);
        return corruptedSpider;
    }

    public void transformspawnCorruptedSpider(Spider spider) {
        applyCorruptedSpiderAttributes(spider);
    }

    private void applyCorruptedSpiderAttributes(Spider spider) {
        spider.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Spider");
        spider.setCustomNameVisible(true);
        Objects.requireNonNull(spider.getAttribute(Attribute.GENERIC_FOLLOW_RANGE)).setBaseValue(32);
        spider.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
        spider.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0));
        spider.getPersistentDataContainer().set(corrupedtedspiderKey, PersistentDataType.BYTE, (byte) 1);
    }


    @EventHandler
    public void onSpiderHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Spider && event.getEntity() instanceof Player) {
            Spider spider = (Spider) event.getDamager();
            Player player = (Player) event.getEntity();

             if (isCorruptedSpider(spider) && !player.isBlocking()) {
                player.getLocation().getBlock().setType(Material.COBWEB);
            }
        }
    }

    //SONIDOS
    @EventHandler
    public void onCorruptedSpiderHurt(EntityDamageEvent event) {
        if (event.getEntity() instanceof Spider spider && isCorruptedSpider(spider)) {
            spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_HURT, SoundCategory.HOSTILE, 1.0f, 0.6f);
        }
    }

    @EventHandler
    public void onCorruptedSpiderDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Spider spider && isCorruptedSpider(spider)) {
            soundManager.removeCustomMob(spider);
            spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_DEATH, SoundCategory.HOSTILE, 1.0f, 0.6f);

            if (Math.random() <= 0.35) {
                spider.getWorld().dropItemNaturally(spider.getLocation(), CorruptedMobItems.createCorruptedSpiderEye());
            }
        }
    }

    public NamespacedKey getCorruptedSpiderKey() {
        return  corrupedtedspiderKey;
    }

    public boolean isCorruptedSpider(Spider spider) {
        return spider.getPersistentDataContainer().has(corrupedtedspiderKey, PersistentDataType.BYTE);
    }

}
