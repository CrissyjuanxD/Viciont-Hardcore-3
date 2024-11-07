package vct.hardcore3;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class NormalTotemHandler implements Listener {

    private final ViciontHardcore3 plugin;

    public NormalTotemHandler(ViciontHardcore3 plugin) {
        this.plugin = plugin;
    }

    private void broadcastNormalTotemMessage(Player player) {
        String message = ChatColor.translateAlternateColorCodes('&', "\uDBE8\uDCF6" + ChatColor.YELLOW + ChatColor.BOLD + player.getName() + ChatColor.RESET + ChatColor.YELLOW + " ha consumido un tótem");
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
        }
    }

    @EventHandler
    public void onPlayerTotemUse(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            // Verifica si ya se activó un tótem de doble vida y no muestra mensaje si es el caso
            if (plugin.getDoubleLifeTotemHandler().isDoubleLifeTotemUsed()) {
                return;
            }

            if (!event.isCancelled()) {
                ItemStack totemItem = player.getInventory().getItemInOffHand();
                if (totemItem.getType() != Material.TOTEM_OF_UNDYING) {
                    totemItem = player.getInventory().getItemInMainHand();
                }

                ItemMeta meta = totemItem.getItemMeta();
                boolean isNormalTotem = meta == null || !meta.hasCustomModelData() || meta.getCustomModelData() == 1;

                if (isNormalTotem) {
                    broadcastNormalTotemMessage(player);
                }
            }
        }
    }
}
