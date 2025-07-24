package items;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnderiteSwordListener implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Integer> hitCounters = new HashMap<>();

    public EnderiteSwordListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Verificar si es la espada de Enderite
        if (!isEnderiteSword(item)) {
            return;
        }

        // Incrementar el contador de golpes
        int hits = hitCounters.getOrDefault(player.getUniqueId(), 0) + 1;
        hitCounters.put(player.getUniqueId(), hits);

        // Cada 3 golpes
        if (hits % 3 == 0) {
            // Curar medio corazón (1 punto de salud)
            double currentHealth = player.getHealth();
            double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();

            if (currentHealth < maxHealth) {
                player.setHealth(Math.min(currentHealth + 1, maxHealth));

                // Efectos visuales y de sonido
                player.getWorld().spawnParticle(
                        Particle.HAPPY_VILLAGER,
                        player.getLocation().add(0, 1.5, 0),
                        15,
                        0.5, 0.5, 0.5, 0.1
                );

                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.5f);
            }
        }
    }

    private boolean isEnderiteSword(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 2;
    }
}