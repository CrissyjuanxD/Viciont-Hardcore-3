package Dificultades.CustomMobs;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Dolphin;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Objects;

public class CustomDolphin implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey dolphinKey;

    public CustomDolphin(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dolphinKey = new NamespacedKey(plugin, "dolphin_key");
    }


    public Dolphin spawnPinguin(Location location, String type) {
        Dolphin dolphin = (Dolphin) location.getWorld().spawnEntity(location, EntityType.DOLPHIN);

        if (type.equalsIgnoreCase("Pingo")) {
            dolphin.setCustomName(ChatColor.of("#73B5F4") + "Pingo");
        } else if (type.equalsIgnoreCase("Pinga")) {
            dolphin.setCustomName(ChatColor.of("#EE8BE7") + "Pinga");
        }
        dolphin.setCustomNameVisible(true);

        dolphin.getPersistentDataContainer().set(dolphinKey, PersistentDataType.STRING, type);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (dolphin.isDead() || !dolphin.isValid()) {
                    this.cancel();
                    return;
                }

                if (!dolphin.isInWater()) {
                    dolphin.setVelocity(new Vector(0, 0, 0));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (dolphin.isDead() || !dolphin.isValid()) {
                    this.cancel();
                    return;
                }

                if (!dolphin.isInWater()) {
                    dolphin.setHealth(dolphin.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return dolphin;
    }

    @EventHandler
    public void onPlayerNameDolphin(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Dolphin)) return;
        Player player = event.getPlayer();
        Dolphin dolphin = (Dolphin) event.getRightClicked();

        if (player.getInventory().getItemInMainHand().getType() == Material.NAME_TAG) {
            String customName = Objects.requireNonNull(player.getInventory().getItemInMainHand().getItemMeta()).getDisplayName();

            if (customName.equalsIgnoreCase("Pingo")) {
                dolphin.setCustomName(ChatColor.of("#73B5F4") + "Pingo");
                dolphin.getPersistentDataContainer().set(dolphinKey, PersistentDataType.STRING, "Pingo");
            } else if (customName.equalsIgnoreCase("Pinga")) {
                dolphin.setCustomName(ChatColor.of("#EE8BE7") + "Pinga");
                dolphin.getPersistentDataContainer().set(dolphinKey, PersistentDataType.STRING, "Pinga");
            }
        }
    }

    @EventHandler
    public void onDolphinFollowFish(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Dolphin)) return;
        if (!(event.getTarget() instanceof Player)) return;

        Dolphin dolphin = (Dolphin) event.getEntity();
        Player player = (Player) event.getTarget();

        if (isHoldingFish(player)) {
            event.setCancelled(false);
            event.setTarget(player);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (dolphin.isDead() || !dolphin.isValid()) {
                        this.cancel();
                        return;
                    }

                    Location playerLocation = player.getLocation();
                    Location dolphinLocation = dolphin.getLocation();
                    Vector direction = playerLocation.toVector().subtract(dolphinLocation.toVector()).normalize();
                    dolphin.setVelocity(direction.multiply(0.5));
                }
            }.runTaskTimer(plugin, 0L, 10L);
        } else {
            event.setCancelled(true);
        }
    }

    private boolean isHoldingFish(Player player) {
        Material itemInHand = player.getInventory().getItemInMainHand().getType();
        return itemInHand == Material.COD || itemInHand == Material.SALMON ||
                itemInHand == Material.TROPICAL_FISH || itemInHand == Material.PUFFERFISH;
    }
}