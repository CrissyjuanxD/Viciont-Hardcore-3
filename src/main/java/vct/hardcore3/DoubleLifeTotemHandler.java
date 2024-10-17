package vct.hardcore3;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public class DoubleLifeTotemHandler implements Listener {
    private final ViciontHardcore3 plugin;
    private final String Prefix = ChatColor.translateAlternateColorCodes('&', "&d&lViciont&5&lHardcore &5&l2&7➤ &f");

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

        // Muestra un mensaje en el chat
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', Prefix + ChatColor.DARK_RED + "Haz consumido 1 vida del " + ChatColor.GOLD + "Totem de Doble Vida"));

        // Muestra un título
        player.sendTitle("\uE062", "", 10, 15, 10);

        // Reproduce un sonido de tótem
        //player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 0.1f); "\uE016"

        // Muestra partículas de tótem
        //player.spawnParticle(Particle.TOTEM, player.getLocation(), 30);

        // Comprueba si el tótem ha sido usado dos veces
        if (life <= 0) {
            // Elimina el tótem del inventario del jugador
            player.getInventory().removeItem(item);
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
            if (isDoubleLifeTotem(mainHandItem)) {
                // Comprueba si el daño causaría la muerte
                if (player.getHealth() - event.getFinalDamage() <= 0) {
                    // Cancela el evento de daño
                    event.setCancelled(true);

                    // Restaura la salud del jugador
                    player.setHealth(player.getMaxHealth());

                    // Reduce en uno la "vida" del tótem
                    useDoubleLifeTotem(player, mainHandItem);
                }
            }
            // Comprueba si el ítem en la offhand es un tótem de doble vida
            else if (isDoubleLifeTotem(offHandItem)) {
                // Comprueba si el daño causaría la muerte
                if (player.getHealth() - event.getFinalDamage() <= 0) {
                    // Cancela el evento de daño
                    event.setCancelled(true);

                    // Restaura la salud del jugador
                    player.setHealth(player.getMaxHealth());

                    // Reduce en uno la "vida" del tótem
                    useDoubleLifeTotem(player, offHandItem);
                }
            }
        }
    }
}

