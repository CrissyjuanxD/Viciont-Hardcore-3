package Handlers;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.function.Predicate;

public class EventInventoryManager implements Listener {

    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;

    // Un método dinámico para saber si el jugador sigue en un evento.
    // Si devuelve true, NO le devolvemos las cosas al conectarse.
    private Predicate<String> isInEventCondition = (name) -> false;

    public EventInventoryManager(JavaPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Define la condición para saber si un jugador está en un evento activo.
     * Ejemplo: (nombre) -> eventoLavaClash.isParticipante(nombre) || hotPotato.isParticipante(nombre)
     */
    public void setIsInEventCondition(Predicate<String> condition) {
        this.isInEventCondition = condition;
    }

    /**
     * Guarda el inventario asíncronamente y lo limpia síncronamente.
     */
    public void saveAndClearInventory(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        ItemStack[] contents = player.getInventory().getContents();

        ItemStack[] clonedContents = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) clonedContents[i] = contents[i].clone();
        }

        player.getInventory().clear();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            dbManager.saveEventInventory(uuid, name, clonedContents);
        });
    }

    /**
     * Restaura el inventario guardado manualmente (por ejemplo al morir o ganar).
     */
    public void restoreInventory(Player player) {
        UUID uuid = player.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ItemStack[] contents = dbManager.getEventInventory(uuid);

            if (contents != null) {
                if (!player.isOnline()) return;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.getInventory().setContents(contents);
                        player.updateInventory();

                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            dbManager.deleteEventInventory(uuid);
                        });
                    }
                });
            }
        });
    }

    /**
     * Al conectarse: Revisa si tiene un inventario en el limbo.
     * Si no está en ningún evento activo, se lo devuelve.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ItemStack[] contents = dbManager.getEventInventory(player.getUniqueId());

            if (contents != null) {
                // Si sigue registrado en un evento activo, no devolvemos nada
                if (isInEventCondition.test(name)) {
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.getInventory().setContents(contents);
                        player.updateInventory();
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            dbManager.deleteEventInventory(player.getUniqueId());
                        });
                        player.sendMessage(ChatColor.GREEN + "۞ Tu inventario del evento ha sido restaurado exitosamente.");
                    }
                });
            }
        });
    }
}