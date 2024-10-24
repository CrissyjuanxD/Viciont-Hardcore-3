package vct.hardcore3;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class DoubleLifeTotemHandler implements Listener {

    private final ViciontHardcore3 plugin;

    public DoubleLifeTotemHandler(ViciontHardcore3 plugin) {
        this.plugin = plugin;
    }

    public boolean isDoubleLifeTotem(ItemStack item) {
        // Comprueba si el ítem tiene un CustomModelData de 2
        return item.hasItemMeta() && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 2;
    }

    public void useDoubleLifeTotem(Player player, ItemStack item) {
        // Reduce en uno la "vida" del tótem
        ItemMeta meta = item.getItemMeta();
        int life = meta.getCustomModelData() - 1;
        meta.setCustomModelData(life);
        item.setItemMeta(meta);

        // Mensaje a todos los jugadores
        broadcastTotemMessage(player);

        // Muestra un título
        player.sendTitle("\uE062", "", 10, 15, 10);

        // Comprueba si el tótem ha sido usado completamente
        if (life <= 0) {
            // Elimina el tótem del inventario del jugador
            player.getInventory().removeItem(item);
        }
    }

    //Método para enviar un mensaje a todos los jugadores cuando se usa un tótem de doble vida
    private void broadcastTotemMessage(Player player) {
        String message = ChatColor.translateAlternateColorCodes('&',"\uDBE8\uDCF6" + ChatColor.YELLOW + ChatColor.BOLD + player.getName() + ChatColor.RESET + ChatColor.YELLOW + " ha consumido un tótem de doble vida");
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
        }
    }
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Comprueba si la entidad dañada es un jugador
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            ItemStack mainHandItem = player.getInventory().getItemInMainHand();
            ItemStack offHandItem = player.getInventory().getItemInOffHand();

            // Comprueba si el ítem en la mano principal es un tótem de doble vida
            if (isDoubleLifeTotem(mainHandItem) || isDoubleLifeTotem(offHandItem)) {
                ItemStack itemToUse = isDoubleLifeTotem(mainHandItem) ? mainHandItem : offHandItem;

                // Comprueba si el daño causaría la muerte
                if (player.getHealth() - event.getFinalDamage() <= 0) {
                    // Cancela el evento de daño
                    event.setCancelled(true);

                    // Restaura la salud del jugador
                    player.setHealth(player.getMaxHealth());

                    // Reduce en uno la "vida" del tótem
                    useDoubleLifeTotem(player, itemToUse);
                }
            }
        }
    }
}


