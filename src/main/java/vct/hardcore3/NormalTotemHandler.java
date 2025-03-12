package vct.hardcore3;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class NormalTotemHandler implements Listener {

    private final Plugin plugin;

    public NormalTotemHandler (Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        // Verificar el tótem en ambas manos
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        ItemStack offHandItem = player.getInventory().getItemInOffHand();

        boolean isMainHandTotem = mainHandItem.getType() == Material.TOTEM_OF_UNDYING;
        boolean isOffHandTotem = offHandItem.getType() == Material.TOTEM_OF_UNDYING;

        if (isMainHandTotem || isOffHandTotem) {
            ItemStack totem = isMainHandTotem ? mainHandItem : offHandItem;
            ItemMeta meta = totem.getItemMeta();

            if (meta != null && meta.hasCustomModelData()) {
                int customModelData = meta.getCustomModelData();

                // Enviar el mensaje correspondiente según el CustomModelData
                switch (customModelData) {
                    case 1:
                        broadcastTotemMessage2(player);
                        break;
                    case 2:
                        broadcastTotemMessage(player);
                        break;
                    default:
                        broadcastNormalTotemMessage(player);
                        break;
                }
            } else {
                // Si no tiene CustomModelData, es un tótem normal
                broadcastNormalTotemMessage(player);
            }
        }
    }

    private void broadcastNormalTotemMessage(Player player) {
        String message = ChatColor.translateAlternateColorCodes('&', "\uDBE8\uDCF6"
                + ChatColor.of("#7C01BB") + ChatColor.BOLD + player.getName()
                + ChatColor.RESET + ChatColor.of("#C198F3") + " ha consumido un tótem.");

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
        }
    }

    private void broadcastTotemMessage(Player player) {
        String message = ChatColor.translateAlternateColorCodes('&', "\uDBE8\uDCF6"
                + ChatColor.of("#007EB2") + ChatColor.BOLD + player.getName()
                + ChatColor.RESET + ChatColor.of("#8EBFEC") + " ha " + ChatColor.BOLD + "disminuido un uso "
                + ChatColor.RESET + ChatColor.of("#8EBFEC") + "su tótem de doble vida.");
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
        }
    }

    private void broadcastTotemMessage2(Player player) {
        String message = ChatColor.translateAlternateColorCodes('&', "\uDBE8\uDCF6"
                + ChatColor.of("#7c01bb") + ChatColor.BOLD + player.getName()
                + ChatColor.RESET + ChatColor.of("#c198f3") + " ha consumido un " + ChatColor.BOLD + "tótem de doble vida.");
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
        }
    }
}