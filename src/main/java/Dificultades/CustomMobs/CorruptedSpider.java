package Dificultades.CustomMobs;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
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
        applyCorruptedSpiderAttributes(corruptedSpider);
        return corruptedSpider;


    }

    public void transformspawnCorruptedSpider(Spider spider) {
        applyCorruptedSpiderAttributes(spider);
    }

    private void applyCorruptedSpiderAttributes(Spider spider) {
        spider.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Spider");
        spider.setCustomNameVisible(true);
        spider.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
        spider.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0));
        spider.getPersistentDataContainer().set(corrupedtedspiderKey, PersistentDataType.BYTE, (byte) 1);
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

    public NamespacedKey getCorruptedKey() {
        return  corrupedtedspiderKey; // Asegúrate de que esta variable exista en CorruptedSpider
    }

    public boolean isCorrupted(Spider spider) {
        return spider.getPersistentDataContainer().has(corrupedtedspiderKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        // Verificar si el jugador realmente se movió (no solo giró la cámara)
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Location playerLocation = player.getLocation();
        double maxDistanceSquared = 30 * 30; // 30 bloques al cuadrado

        // Obtiene entidades cercanas y filtra solo arañas sin PersistentDataKey
        for (Entity entity : player.getNearbyEntities(30, 30, 30)) {
            if (entity instanceof Spider spider &&
                    spider.getCustomName() != null &&
                    spider.getCustomName().equals(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Spider") &&
                    !spider.getPersistentDataContainer().has(corrupedtedspiderKey, PersistentDataType.BYTE)) {

                // Usa distanceSquared para evitar la raíz cuadrada
                if (playerLocation.distanceSquared(spider.getLocation()) <= maxDistanceSquared) {
                    transformspawnCorruptedSpider(spider);
                }
            }
        }
    }

}
