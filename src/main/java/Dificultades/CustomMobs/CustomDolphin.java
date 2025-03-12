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
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Método para spawnear un delfín personalizado
     */
    public Dolphin spawnDolphin(Location location, String type) {
        Dolphin dolphin = (Dolphin) location.getWorld().spawnEntity(location, EntityType.DOLPHIN);

        if (type.equalsIgnoreCase("Pingo")) {
            dolphin.setCustomName(ChatColor.of("#73B5F4") + "Pingo");
        } else if (type.equalsIgnoreCase("Pinga")) {
            dolphin.setCustomName(ChatColor.of("#EE8BE7") + "Pinga");
        }
        dolphin.setCustomNameVisible(true);

        // Guardar el tipo en PersistentDataContainer
        dolphin.getPersistentDataContainer().set(dolphinKey, PersistentDataType.STRING, type);

        // Tarea para evitar que el delfín salte en tierra firme
        new BukkitRunnable() {
            @Override
            public void run() {
                if (dolphin.isDead() || !dolphin.isValid()) {
                    this.cancel(); // Cancelar la tarea si el delfín está muerto o no es válido
                    return;
                }

                // Evitar que el delfín salte en tierra firme
                if (!dolphin.isInWater()) {
                    dolphin.setVelocity(new Vector(0, 0, 0)); // Detener el movimiento vertical
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Ejecutar cada tick (20 veces por segundo)

        // Tarea para evitar que el delfín muera fuera del agua
        new BukkitRunnable() {
            @Override
            public void run() {
                if (dolphin.isDead() || !dolphin.isValid()) {
                    this.cancel(); // Cancelar la tarea si el delfín está muerto o no es válido
                    return;
                }

                // Curar al delfín si está en tierra firme
                if (!dolphin.isInWater()) {
                    dolphin.setHealth(dolphin.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Ejecutar cada segundo (20 ticks)

        return dolphin;
    }

    /**
     * Si un jugador usa una nametag en un delfín, cambiar el color del nombre
     */
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

    /**
     * Hacer que los delfines sigan a jugadores con pescado en la mano
     */
    @EventHandler
    public void onDolphinFollowFish(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Dolphin)) return;
        if (!(event.getTarget() instanceof Player)) return;

        Dolphin dolphin = (Dolphin) event.getEntity();
        Player player = (Player) event.getTarget();

        // Si el jugador tiene pescado en la mano, el delfín lo sigue
        if (isHoldingFish(player)) {
            event.setCancelled(false);
            event.setTarget(player);

            // Mover manualmente al delfín hacia el jugador
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (dolphin.isDead() || !dolphin.isValid()) {
                        this.cancel(); // Cancelar la tarea si el delfín está muerto o no es válido
                        return;
                    }

                    // Mover al delfín hacia el jugador
                    Location playerLocation = player.getLocation();
                    Location dolphinLocation = dolphin.getLocation();
                    Vector direction = playerLocation.toVector().subtract(dolphinLocation.toVector()).normalize();
                    dolphin.setVelocity(direction.multiply(0.5)); // Velocidad moderada
                }
            }.runTaskTimer(plugin, 0L, 10L); // Ejecutar cada medio segundo (10 ticks)
        } else {
            event.setCancelled(true);
        }
    }

    /**
     * Verifica si un jugador sostiene cualquier tipo de pescado
     */
    private boolean isHoldingFish(Player player) {
        Material itemInHand = player.getInventory().getItemInMainHand().getType();
        return itemInHand == Material.COD || itemInHand == Material.SALMON ||
                itemInHand == Material.TROPICAL_FISH || itemInHand == Material.PUFFERFISH;
    }
}