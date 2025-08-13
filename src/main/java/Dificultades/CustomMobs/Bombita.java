package Dificultades.CustomMobs;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Bombita implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey bombitaKey;
    private boolean eventsRegistered = false;

    public Bombita(JavaPlugin plugin) {
        this.plugin = plugin;
        this.bombitaKey = new NamespacedKey(plugin, "bombita");
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
                    if (isBombita(entity)) {
                        entity.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public Creeper spawnBombita(Location location) {
        Creeper bombita = (Creeper) location.getWorld().spawnEntity(location, EntityType.CREEPER);
        applyBombitaAttributes(bombita);
        return bombita;
    }

    public void transformToBombita(Creeper creeper) {
        applyBombitaAttributes(creeper);
    }

    private void applyBombitaAttributes(Creeper creeper) {
        creeper.setCustomName(ChatColor.RED + "" + ChatColor.BOLD + "Bombita");
        creeper.setCustomNameVisible(false);
        creeper.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(32);
        creeper.setExplosionRadius(2);
        creeper.setMaxFuseTicks(5);
        creeper.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1));
        creeper.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(1.2);
        creeper.getPersistentDataContainer().set(bombitaKey, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Creeper creeper && event.getEntity() instanceof Creeper) {
            if (isBombita(creeper)) {
                event.setCancelled(true);
            }
        }
    }


    public NamespacedKey getBombitaKey() {
        return bombitaKey;
    }

    private boolean isBombita(Entity entity) {
        return entity instanceof Creeper && entity.getPersistentDataContainer().has(bombitaKey, PersistentDataType.BYTE);
    }
}
