package SlotMachine.commands;

import SlotMachine.SlotMachineManager;
import SlotMachine.utils.ItemCreator;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vct.hardcore3.ViciontHardcore3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Comandos para SlotMachine - Basado en DTools3
 */
public class SlotMachineCommand implements CommandExecutor, TabCompleter {
    
    private final ViciontHardcore3 plugin;
    private final SlotMachineManager manager;
    private final ItemCreator itemCreator;
    
    public SlotMachineCommand(ViciontHardcore3 plugin, SlotMachineManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.itemCreator = new ItemCreator(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("viciont.slotmachine.admin")) {
            sender.sendMessage(ChatColor.of("#FF6B6B") + "۞ No tienes permisos para usar este comando.");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "spawn":
                return handleSpawn(sender, args);
            case "give":
                return handleGive(sender, args);
            case "remove":
                return handleRemove(sender, args);
            case "reload":
                return handleReload(sender);
            case "info":
                return handleInfo(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.of("#FF6B6B") + "۞ Solo los jugadores pueden usar este comando.");
            return true;
        }
        
        Player player = (Player) sender;
        String machineId = args.length > 1 ? args[1] : "default";
        
        Location location = player.getLocation();
        boolean success = manager.createSlotMachine(location, player, machineId);
        
        if (success) {
            player.sendMessage(ChatColor.of("#B5EAD7") + "۞ Slot Machine '" + machineId + "' creada exitosamente!");
        } else {
            player.sendMessage(ChatColor.of("#FF6B6B") + "۞ Error al crear la Slot Machine.");
        }
        
        return true;
    }
    
    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.of("#FF6B6B") + "۞ Uso: /slotmachine give <jugador> [cantidad]");
            return true;
        }
        
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.of("#FF6B6B") + "۞ Jugador no encontrado: " + args[1]);
            return true;
        }
        
        int amount = 1;
        if (args.length > 2) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    sender.sendMessage(ChatColor.of("#FF6B6B") + "۞ La cantidad debe ser mayor a 0.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.of("#FF6B6B") + "۞ Cantidad inválida: " + args[2]);
                return true;
            }
        }
        
        ItemStack slotMachineItem = createSlotMachineItem(amount);
        target.getInventory().addItem(slotMachineItem);
        
        sender.sendMessage(ChatColor.of("#B5EAD7") + "۞ Has dado " + amount + "x Slot Machine a " + target.getName());
        target.sendMessage(ChatColor.of("#B5EAD7") + "۞ Has recibido " + amount + "x Slot Machine");
        
        return true;
    }
    
    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.of("#FF6B6B") + "۞ Solo los jugadores pueden usar este comando.");
            return true;
        }
        
        Player player = (Player) sender;
        Location location = player.getTargetBlock(null, 10).getLocation();
        
        boolean success = manager.removeSlotMachine(location);
        
        if (success) {
            player.sendMessage(ChatColor.of("#B5EAD7") + "۞ Slot Machine removida exitosamente!");
        } else {
            player.sendMessage(ChatColor.of("#FF6B6B") + "۞ No se encontró una Slot Machine en esa ubicación.");
        }
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender) {
        try {
            manager.reloadConfiguration();
            sender.sendMessage(ChatColor.of("#B5EAD7") + "۞ Configuración de Slot Machine recargada exitosamente!");
            return true;
        } catch (Exception e) {
            sender.sendMessage(ChatColor.of("#FF6B6B") + "۞ Error al recargar la configuración: " + e.getMessage());
            return true;
        }
    }
    
    private boolean handleInfo(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.of("#B5EAD7") + "=== Información de Slot Machine ===");
        sender.sendMessage(ChatColor.of("#E8F4FD") + "• Máquinas activas: " + manager.getActiveMachinesCount());
        sender.sendMessage(ChatColor.of("#E8F4FD") + "• Configuraciones cargadas: " + manager.getLoadedConfigsCount());
        sender.sendMessage(ChatColor.of("#E8F4FD") + "• ModelEngine detectado: " + (manager.isModelEngineAvailable() ? "Sí" : "No"));
        return true;
    }
    
    private ItemStack createSlotMachineItem(int amount) {
        ItemStack item = new ItemStack(Material.ARMOR_STAND, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#FFD700") + "Slot Machine");
            meta.setLore(Arrays.asList(
                ChatColor.of("#B5EAD7") + "Coloca esta máquina para crear",
                ChatColor.of("#B5EAD7") + "una Slot Machine funcional.",
                "",
                ChatColor.of("#FF6B6B") + "Click derecho para colocar"
            ));
            meta.setCustomModelData(1000); // Para el resource pack
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.of("#B5EAD7") + "=== Comandos de Slot Machine ===");
        sender.sendMessage(ChatColor.of("#E8F4FD") + "/slotmachine spawn [id] - Crear una slot machine");
        sender.sendMessage(ChatColor.of("#E8F4FD") + "/slotmachine give <jugador> [cantidad] - Dar item de slot machine");
        sender.sendMessage(ChatColor.of("#E8F4FD") + "/slotmachine remove - Remover slot machine");
        sender.sendMessage(ChatColor.of("#E8F4FD") + "/slotmachine reload - Recargar configuración");
        sender.sendMessage(ChatColor.of("#E8F4FD") + "/slotmachine info - Ver información del sistema");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("spawn", "give", "remove", "reload", "info"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                return null; // Devuelve nombres de jugadores automáticamente
            } else if (args[0].equalsIgnoreCase("spawn")) {
                completions.addAll(manager.getAvailableMachineIds());
            }
        }
        
        return completions;
    }
}