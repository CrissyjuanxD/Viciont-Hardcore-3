package Enchants;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class EnhancedEnchantmentGUI implements Listener {

    private final JavaPlugin plugin;
    private final ItemStack grayPane = createGrayPane();
    private final Map<Enchantment, Integer> essenceCustomModelDataMap;
    private final Map<Player, ItemStack[]> playerInventoryContents = new HashMap<>();

    public EnhancedEnchantmentGUI(JavaPlugin plugin) {
        this.plugin = plugin;
        this.essenceCustomModelDataMap = new HashMap<>();

        // Inicializar el mapa manualmente
        essenceCustomModelDataMap.put(Enchantment.PROTECTION, 2);
        essenceCustomModelDataMap.put(Enchantment.UNBREAKING, 3);
        essenceCustomModelDataMap.put(Enchantment.MENDING, 4);
        essenceCustomModelDataMap.put(Enchantment.EFFICIENCY, 5);
        essenceCustomModelDataMap.put(Enchantment.FORTUNE, 6);
        essenceCustomModelDataMap.put(Enchantment.SHARPNESS, 7);
        essenceCustomModelDataMap.put(Enchantment.SMITE, 8);
        essenceCustomModelDataMap.put(Enchantment.BANE_OF_ARTHROPODS, 9);
        essenceCustomModelDataMap.put(Enchantment.FEATHER_FALLING, 10);
        essenceCustomModelDataMap.put(Enchantment.LOOTING, 11);
        essenceCustomModelDataMap.put(Enchantment.DEPTH_STRIDER, 12);
        essenceCustomModelDataMap.put(Enchantment.SILK_TOUCH, 13);
        essenceCustomModelDataMap.put(Enchantment.POWER, 14);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Crear un panel gris claro
    private ItemStack createGrayPane() {
        ItemStack pane = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    // Crear y abrir la GUI personalizada
    public void openEnhancedEnchantmentTableGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.AQUA + "Mesa Mejorada");

        // Colocar los libros de encantamiento en sus posiciones
        Enchantment[] enchantments = {
                Enchantment.PROTECTION, Enchantment.UNBREAKING, Enchantment.MENDING,
                Enchantment.EFFICIENCY, Enchantment.FORTUNE, Enchantment.SHARPNESS,
                Enchantment.SMITE, Enchantment.BANE_OF_ARTHROPODS, Enchantment.FEATHER_FALLING,
                Enchantment.LOOTING, Enchantment.DEPTH_STRIDER, Enchantment.SILK_TOUCH,
                Enchantment.POWER
        };
        int[] slots = { 13, 14, 15, 16, 22, 23, 24, 25, 31, 32, 33, 34, 40 };
        for (int i = 0; i < enchantments.length; i++) {
            gui.setItem(slots[i], createEnchantmentBook(enchantments[i]));
        }

        // Colocar el cristal gris, pero dejando libres los slots 36, 37 y 38
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null && i != 36 && i != 37 && i != 38) {
                gui.setItem(i, grayPane);
            }
        }

        playerInventoryContents.put(player, player.getInventory().getContents().clone()); // Guardar contenido del inventario
        player.openInventory(gui);
    }

    // Crear libros de encantamiento de nivel 1 para cada encantamiento deseado
    private ItemStack createEnchantmentBook(Enchantment enchantment) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        book.addUnsafeEnchantment(enchantment, 1);
        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + enchantment.getKey().getKey() + " I");
            book.setItemMeta(meta);
        }
        return book;
    }

    // Método para crear la mesa de encantamientos en una ubicación específica
    public void createCustomEnchantingTable(Block block) {
        block.setType(Material.ENCHANTING_TABLE);
        block.setMetadata("CustomModelData", new FixedMetadataValue(plugin, 1)); // Cambiar el modelo personalizado a 1
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!event.getView().getTitle().equals(ChatColor.AQUA + "Mesa Mejorada")) return;

        int slot = event.getRawSlot();
        if (slot < 54 && slot != 36 && slot != 37 && slot != 38) {
            event.setCancelled(true);
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && clickedItem.getType() == Material.ENCHANTED_BOOK) {
            // Obtener ítems de los slots específicos
            ItemStack itemToEnchant = event.getInventory().getItem(36);
            ItemStack lapis = event.getInventory().getItem(37);
            ItemStack essence = event.getInventory().getItem(38);

            if (itemToEnchant == null) {
                player.sendMessage(ChatColor.RED + "No hay ítem para encantar en el slot 36.");
                return;
            }

            if (lapis == null || lapis.getAmount() < 3) {
                player.sendMessage(ChatColor.RED + "No hay suficiente lapislázuli en el slot 37.");
                return;
            }

            if (essence == null || essence.getType() == Material.AIR) {
                player.sendMessage(ChatColor.RED + "No hay esencia en el slot 38.");
                return;
            }

            // Obtener el encantamiento del libro
            Enchantment enchantment = clickedItem.getEnchantments().keySet().iterator().next();

            // Verificar CustomModelData de la esencia
            ItemMeta essenceMeta = essence.getItemMeta();
            if (essenceMeta == null) {
                player.sendMessage(ChatColor.RED + "Error al obtener la meta de la esencia.");
                return;
            }

            int essenceModelData = essenceCustomModelDataMap.getOrDefault(enchantment, -1);
            if (essenceMeta.getCustomModelData() != essenceModelData) {
                player.sendMessage(ChatColor.RED + "Esencia incorrecta para este encantamiento.");
                return;
            }

            // Verificar y actualizar el nivel de encantamiento
            int currentLevel = itemToEnchant.getEnchantmentLevel(enchantment);
            int nextLevel = currentLevel + 1;
            int maxLevel = enchantment.getMaxLevel();

            if (currentLevel >= maxLevel) {
                player.sendMessage(ChatColor.RED + "El ítem ya tiene el nivel máximo de este encantamiento.");
                return;
            }

            // Aplicar el encantamiento
            itemToEnchant.addUnsafeEnchantment(enchantment, nextLevel);
            lapis.setAmount(lapis.getAmount() - 3);

            // Reducir los usos de la esencia
            PersistentDataContainer data = essenceMeta.getPersistentDataContainer();
            int uses = data.getOrDefault(new NamespacedKey(plugin, "uses"), PersistentDataType.INTEGER, 0);
            if (uses < 4) {
                data.set(new NamespacedKey(plugin, "uses"), PersistentDataType.INTEGER, uses + 1);
                essence.setItemMeta(essenceMeta);
            } else {
                player.getInventory().remove(essence);
            }

            // Actualizar el slot de item encantado
            event.getInventory().setItem(36, itemToEnchant);
            player.sendMessage(ChatColor.GREEN + "Encantamiento aplicado exitosamente.");
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // Recuperar el contenido del inventario original
        if (playerInventoryContents.containsKey(player)) {
            ItemStack[] originalContents = playerInventoryContents.get(player);
            player.getInventory().setContents(originalContents);
            playerInventoryContents.remove(player);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block != null && block.getType() == Material.ENCHANTING_TABLE) {
            // Verificar si el bloque es una mesa de encantamiento personalizada
            if (block.hasMetadata("CustomModelData") && block.getMetadata("CustomModelData").get(0).asInt() == 1) {
                event.setCancelled(true); // Cancela la apertura de la mesa normal
                openEnhancedEnchantmentTableGUI(event.getPlayer()); // Abre la GUI personalizada
            }
        }
    }

}
