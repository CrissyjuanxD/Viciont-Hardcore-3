package Dificultades.CustomMobs;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
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
            // Eliminar todos los Bombitas existentes
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Creeper creeper &&
                            creeper.getPersistentDataContainer().has(bombitaKey, PersistentDataType.BYTE)) {
                        creeper.remove();
                    }
                }
            }
            eventsRegistered = false;
        }
    }

    public Creeper spawnBombita(Location location) {
        Creeper bombita = (Creeper) location.getWorld().spawnEntity(location, EntityType.CREEPER);
        bombita.setCustomName(ChatColor.RED + "" + ChatColor.BOLD + "Bombita");
        bombita.setCustomNameVisible(true);
        bombita.setExplosionRadius(2);
        bombita.setMaxFuseTicks(5);
        bombita.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1)); // Velocidad II
        bombita.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(0.7); // Escala del mob
        bombita.getPersistentDataContainer().set(bombitaKey, PersistentDataType.BYTE, (byte) 1); // Marcar como Bombita
        return bombita;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Creeper creeper && event.getEntity() instanceof Creeper) {
            if (creeper.getPersistentDataContainer().has(bombitaKey, PersistentDataType.BYTE)) {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof Creeper creeper) {
            if (creeper.getPersistentDataContainer().has(bombitaKey, PersistentDataType.BYTE)) {
                creeper.removePotionEffect(PotionEffectType.SPEED);
            }
        }
    }
}
