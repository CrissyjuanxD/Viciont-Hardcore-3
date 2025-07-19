package items;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class InvulnerableItemProtection implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey invulnerableKey;

    public InvulnerableItemProtection(JavaPlugin plugin) {
        this.plugin = plugin;
        this.invulnerableKey = new NamespacedKey(plugin, "invulnerable_item");
    }

    private boolean isProtectedItem(Item item) {
        if (item == null || !item.isValid()) return false;
        ItemStack itemStack = item.getItemStack();
        if (itemStack.getItemMeta() == null) return false;

        Byte val = itemStack.getItemMeta().getPersistentDataContainer().get(invulnerableKey, PersistentDataType.BYTE);
        return val != null && val == 1;
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        // Proteger items en la lista de bloques afectados
        event.blockList().removeIf(block -> {
            // Obtener el mundo y buscar entidades cerca de la ubicación del bloque
            return block.getWorld().getNearbyEntities(block.getLocation(), 0.5, 0.5, 0.5).stream()
                    .anyMatch(entity -> {
                        if (entity instanceof Item) {
                            return isProtectedItem((Item) entity);
                        }
                        return false;
                    });
        });

        // Proteger items en las entidades afectadas directamente
        event.getEntity().getNearbyEntities(5, 5, 5).forEach(entity -> {
            if (entity instanceof Item) {
                Item item = (Item) entity;
                if (isProtectedItem(item)) {
                    item.setInvulnerable(true);
                    item.setUnlimitedLifetime(true);
                }
            }
        });
    }

    @EventHandler
    public void onBurn(EntityCombustEvent event) {
        if (event.getEntity() instanceof Item item && isProtectedItem(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item item && isProtectedItem(item)) {
            // Cancelar cualquier daño al item (lava, fuego, etc.)
            if (event.getCause() == EntityDamageEvent.DamageCause.FIRE
                    || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
                    || event.getCause() == EntityDamageEvent.DamageCause.LAVA
                    || event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                    || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                event.setCancelled(true);
            }
        }
    }

}