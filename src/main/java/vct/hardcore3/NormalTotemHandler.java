package vct.hardcore3;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;

public class NormalTotemHandler implements Listener {
    private final ViciontHardcore3 plugin;
    private final String Prefix = ChatColor.translateAlternateColorCodes('&', "&d&lViciont&5&lHardcore &5&l2&7➤ &f");

    public NormalTotemHandler(ViciontHardcore3 plugin) {
        this.plugin = plugin;
    }

    private void broadcastTotemMessage(Player player) {
        String message = ChatColor.translateAlternateColorCodes('&', "\uDBE8\uDCF6" + ChatColor.YELLOW + ChatColor.BOLD + player.getName() + ChatColor.RESET + ChatColor.YELLOW + " ha consumido un tótem");
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
        }
    }

    @EventHandler
    public void onPlayerTotemUse(EntityResurrectEvent event) {
        // Verificar si la entidad que se resucita es un jugador
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            // Verificar si el tótem fue usado (evento no cancelado)
            if (event.isCancelled() == false) {
                // Enviar el mensaje a todos los jugadores
                broadcastTotemMessage(player);
            }
        }
    }
}
