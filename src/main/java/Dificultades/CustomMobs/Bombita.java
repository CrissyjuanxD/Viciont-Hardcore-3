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
        applyCorruptedSpiderAttributes(bombita);
        return bombita;
    }

    public void transformToBombita(Creeper creeper) {
        applyCorruptedSpiderAttributes(creeper);
    }

    private void applyCorruptedSpiderAttributes(Creeper creeper) {
        creeper.setCustomName(ChatColor.RED + "" + ChatColor.BOLD + "Bombita");
        creeper.setCustomNameVisible(true);
        creeper.setExplosionRadius(2);
        creeper.setMaxFuseTicks(5);
        creeper.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1)); // Velocidad II
        creeper.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(0.7); // Escala del mob
        creeper.getPersistentDataContainer().set(bombitaKey, PersistentDataType.BYTE, (byte) 1); // Marcar como Bombita
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
            if (entity instanceof Creeper creeper &&
                    creeper.getCustomName() != null &&
                    creeper.getCustomName().equals(ChatColor.RED + "" + ChatColor.BOLD + "Bombita") &&
                    !creeper.getPersistentDataContainer().has(bombitaKey, PersistentDataType.BYTE)) {

                // Usa distanceSquared para evitar la raíz cuadrada
                if (playerLocation.distanceSquared(creeper.getLocation()) <= maxDistanceSquared) {
                    transformToBombita(creeper);
                }
            }
        }
    }
}
