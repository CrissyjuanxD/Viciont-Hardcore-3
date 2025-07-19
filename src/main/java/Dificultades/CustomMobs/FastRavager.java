package Dificultades.CustomMobs;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FastRavager implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey FastRavagerKey;
    private boolean eventsRegistered = false;

    public FastRavager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.FastRavagerKey = new NamespacedKey(plugin, "fast_ravager");
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
                    if (entity instanceof Ravager ravager && isFastRavager(ravager)) {
                        ravager.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public Ravager spawnFastRavager(Location location) {
        Ravager fastravager = (Ravager) location.getWorld().spawnEntity(location, EntityType.RAVAGER);
        applyFastRavagerAttributes(fastravager);
        return fastravager;
    }

    public void transformspawnFastRavager(Ravager ravager) {
        applyFastRavagerAttributes(ravager);
    }

    private void applyFastRavagerAttributes(Ravager ravager) {
        ravager.setCustomName(ChatColor.GOLD + "" + ChatColor.BOLD + "Fast Ravager");
        ravager.setCustomNameVisible(false);
        ravager.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(200);
        ravager.setHealth(200);
        ravager.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 4));
        ravager.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0));
        ravager.getPersistentDataContainer().set(FastRavagerKey, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler
    public void onFastRavagerDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Ravager ravager && isFastRavager(ravager)) {
            ravager.getWorld().playSound(ravager.getLocation(), Sound.ENTITY_RAVAGER_DEATH, SoundCategory.HOSTILE, 1.0f, 2f);

            event.getDrops().add(new ItemStack(Material.GOLD_INGOT, 15));
        }
    }

    public NamespacedKey getFastRavagerKey() {
        return FastRavagerKey;
    }

    public boolean isFastRavager(Ravager ravager) {
        return ravager.getPersistentDataContainer().has(FastRavagerKey, PersistentDataType.BYTE);
    }

}