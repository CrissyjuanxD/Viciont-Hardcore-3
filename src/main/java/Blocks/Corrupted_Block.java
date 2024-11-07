package Blocks;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Corrupted_Block implements CommandExecutor, Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey corruptedKey;
    private final Set<String> corruptedBlocks = new HashSet<>(); // Para almacenar las coordenadas de los bloques corruptos

    public Corrupted_Block(JavaPlugin plugin) {
        this.plugin = plugin;
        this.corruptedKey = new NamespacedKey(plugin, "corrupted_block");
    }

    // Método para crear el Corrupted Block como item
    public ItemStack createCorruptedBlockItem() {
        ItemStack corruptedBlock = new ItemStack(Material.HONEYCOMB_BLOCK); // Usa un bloque existente como base

        ItemMeta meta = corruptedBlock.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_PURPLE + "Corrupted Block");
            meta.setLore(Collections.singletonList(ChatColor.GRAY + "Este bloque es decorativo y corrupto"));
            meta.setCustomModelData(1); // Aquí puedes especificar el Custom Model Data
            corruptedBlock.setItemMeta(meta);
        }

        return corruptedBlock;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("give_corrupted_block")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (!player.isOp()) {
                    player.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                    return true;
                }
                player.getInventory().addItem(createCorruptedBlockItem());
                player.sendMessage(ChatColor.GREEN + "¡Has recibido un Corrupted Block!");
            } else {
                sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
            }
            return true;
        }
        return false;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.hasItemMeta() && item.getType() == Material.HONEYCOMB_BLOCK) { // Verifica si es el bloque de miel
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 1) {
                Block block = event.getBlockPlaced();
                // Almacena las coordenadas del bloque colocado
                String coordinates = block.getLocation().toString();
                corruptedBlocks.add(coordinates);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        // Verifica si el bloque es un "Corrupted Block"
        String coordinates = block.getLocation().toString();
        if (corruptedBlocks.contains(coordinates)) {
            ItemStack corruptedBlockItem = createCorruptedBlockItem();
            event.setDropItems(false); // Evita que se caigan los ítems normales
            block.getWorld().dropItemNaturally(block.getLocation(), corruptedBlockItem); // Lanza el ítem corrupto
            corruptedBlocks.remove(coordinates); // Elimina la coordenada de la lista
        }
    }

    @EventHandler
    public void onPlayerWalkOnCorruptedBlock(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Block blockBelow = player.getLocation().subtract(0, 1, 0).getBlock();
        String coordinates = blockBelow.getLocation().toString();
        // Verifica si el bloque está en la lista de bloques corruptos
        if (corruptedBlocks.contains(coordinates)) {
            // Añadir efecto de lentitud si está en Corrupted Block
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0, false, false, false));
        }
    }
}
