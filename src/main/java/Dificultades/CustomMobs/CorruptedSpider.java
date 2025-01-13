package Dificultades.CustomMobs;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CorruptedSpider implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey corrupedtedspiderKey;
    private boolean eventsRegistered = false;

    public CorruptedSpider(JavaPlugin plugin) {
        this.plugin = plugin;
        this.corrupedtedspiderKey = new NamespacedKey(plugin, "corruptedspider");
    }

    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
        }
    }

    public void revert() {
        if (eventsRegistered) {
            // Eliminar todos los Bombitas existentes
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Spider spider &&
                            spider.getPersistentDataContainer().has(corrupedtedspiderKey, PersistentDataType.BYTE)) {
                        spider.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public Spider spawnCorruptedSpider(Location location) {
        Spider corruptedSpider = (Spider) location.getWorld().spawnEntity(location, EntityType.SPIDER);
        corruptedSpider.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Spider");
        corruptedSpider.setCustomNameVisible(true);
        corruptedSpider.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
        corruptedSpider.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0));
        corruptedSpider.getPersistentDataContainer().set(corrupedtedspiderKey, PersistentDataType.BYTE, (byte) 1);
        return corruptedSpider;
    }

    @EventHandler
    public void onSpiderHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Spider && event.getEntity() instanceof Player) {
            Spider spider = (Spider) event.getDamager();
            Player player = (Player) event.getEntity();

            if (spider.getPersistentDataContainer().has(corrupedtedspiderKey, PersistentDataType.BYTE)) {
                // telaraña en los pies del jugador
                player.getLocation().getBlock().setType(Material.COBWEB);
            }
        }
    }
}
