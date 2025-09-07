package SlotMachine.listeners;

import SlotMachine.SlotMachineManager;
import SlotMachine.api.SlotMachineModel;
import SlotMachine.cache.smachine.SlotMachine;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Listener para SlotMachine - Basado en DTools3
 */
public class SlotMachineListener implements Listener {

    private final JavaPlugin plugin;
    private final SlotMachineManager manager;

    public SlotMachineListener(JavaPlugin plugin, SlotMachineManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ARMOR_STAND) {
            return;
        }

        if (!item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) {
            return;
        }

        if (item.getItemMeta().getCustomModelData() != 1000) {
            return;
        }

        Player player = event.getPlayer();
        Location location = event.getClickedBlock().getLocation().add(0, 1, 0);

        event.setCancelled(true);

        boolean success = manager.createSlotMachine(location, player, "default");
        if (success) {
            item.setAmount(item.getAmount() - 1);
            if (item.getAmount() <= 0) {
                player.getInventory().setItemInMainHand(null);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        // Verificar si es una slot machine
        if (!entity.hasMetadata("slot_machine")) {
            return;
        }

        event.setCancelled(true);

        // Obtener el modelo de la máquina
        SlotMachineModel model = manager.getSlotMachine(entity.getLocation());
        if (model == null) {
            player.sendMessage(ChatColor.of("#FF6B6B") + "۞ Error: Máquina no encontrada.");
            return;
        }

        // Obtener la configuración de la slot machine
        SlotMachine slotMachine = manager.getSlotMachineTool().getDefaultSlotMachine();
        if (slotMachine == null) {
            player.sendMessage(ChatColor.of("#FF6B6B") + "۞ Error: SlotMachine no configurada.");
            return;
        }

        if (model.isActive()) {
            player.sendMessage(ChatColor.of("#FF6B6B") + "۞ La máquina está en uso, espera a que termine.");
            return;
        }

        // Iniciar uso de la máquina
        manager.startUsing(slotMachine, model, player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Limpiar máquinas del jugador que se desconecta
        manager.cleanupPlayerMachines(player);
    }
}