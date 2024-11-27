package vct.hardcore3;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class DoubleLifeTotemHandler implements Listener {

    private final ViciontHardcore3 plugin;
    private boolean doubleLifeTotemUsed = false; // Bandera para evitar mensajes duplicados

    public DoubleLifeTotemHandler(ViciontHardcore3 plugin) {
        this.plugin = plugin;
    }

    public boolean isDoubleLifeTotem(ItemStack item) {
        if (item != null && item.getType() == Material.TOTEM_OF_UNDYING) {
            ItemMeta meta = item.getItemMeta();
            return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 2;
        }
        return false;
    }

    public boolean isDoubleLifeTotemUsed() {
        return doubleLifeTotemUsed;
    }

    private void broadcastTotemMessage(Player player) {
        String message = ChatColor.translateAlternateColorCodes('&', "\uDBE8\uDCF6" + ChatColor.YELLOW + ChatColor.BOLD + player.getName() + ChatColor.RESET + ChatColor.YELLOW + " ha consumido un tótem de doble vida");
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            ItemStack mainHandItem = player.getInventory().getItemInMainHand();
            ItemStack offHandItem = player.getInventory().getItemInOffHand();

            boolean isMainHandTotem = isDoubleLifeTotem(mainHandItem);
            boolean isOffHandTotem = isDoubleLifeTotem(offHandItem);

            // Verifica si el jugador moriría y si algún tótem de doble vida puede salvarlo
            if ((isMainHandTotem || isOffHandTotem) && player.getHealth() - event.getFinalDamage() <= 0) {
                // Si hay un tótem normal en la mano principal, no activamos el tótem de doble vida
                if (!isMainHandTotem && mainHandItem.getType() == Material.TOTEM_OF_UNDYING) {
                    //lanza mensjae del totem normal en vez del doble vida
                    if (offHandItem.getType() == Material.TOTEM_OF_UNDYING) {
                        NormalTotemHandler.broadcastNormalTotemMessage(player);
                    }
                    return; // Se activa el tótem normal en la mano principal, no tocar el de doble vida
                }

                // Marca que el tótem de doble vida se ha usado
                doubleLifeTotemUsed = true;
                broadcastTotemMessage(player);

                // Determina el tótem a activar y en qué ranura reemplazarlo
                int slotIndex = isMainHandTotem ? player.getInventory().getHeldItemSlot() : 40;

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (doubleLifeTotemUsed) { // Reemplaza solo si realmente se usó
                            player.getInventory().setItem(slotIndex, new ItemStack(Material.TOTEM_OF_UNDYING));
                            doubleLifeTotemUsed = false; // Reinicia la bandera
                        }
                    }
                }.runTaskLater(plugin, 5); // Espera de 5 ticks
            }
        }
    }
}