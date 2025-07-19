package Enchants;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;

import java.util.*;

public class EnhancedEnchantmentGUI implements Listener {

    private final JavaPlugin plugin;
    private final ItemStack grayPane = createGrayPane();
    private final Map<Player, ItemStack[]> playerInventoryContents = new HashMap<>();
    private final Map<Location, BukkitRunnable> particleTasks = new HashMap<>();

    public EnhancedEnchantmentGUI(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private ItemStack createGrayPane() {
        ItemStack pane = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.setCustomModelData(2);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    public void openEnhancedEnchantmentTableGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54,"\u3201\u3201" + ChatColor.WHITE + "\u3200");
        Enchantment[] enchantments = {
                Enchantment.PROTECTION, Enchantment.UNBREAKING,
                Enchantment.EFFICIENCY, Enchantment.FORTUNE, Enchantment.SHARPNESS,
                Enchantment.SMITE, Enchantment.BANE_OF_ARTHROPODS, Enchantment.FEATHER_FALLING,
                Enchantment.LOOTING, Enchantment.DEPTH_STRIDER,
                Enchantment.POWER
        };
        int[] slots = { 13, 14, 15, 16, 22, 23, 24, 25, 31, 32, 33, };
        for (int i = 0; i < enchantments.length; i++) {
            gui.setItem(slots[i], createEnchantmentBook(enchantments[i], 1));
        }

        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null && i != 36 && i != 37 && i != 38) {
                gui.setItem(i, grayPane);
            }
        }

        playerInventoryContents.put(player, player.getInventory().getContents().clone());
        player.openInventory(gui);
    }

    private ItemStack createEnchantmentBook(Enchantment enchantment, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        if (meta != null) {
            meta.addStoredEnchant(enchantment, level, true);

            String enchantmentName = formatEnchantmentName(enchantment.getKey().getKey());
            meta.setDisplayName(ChatColor.GOLD + enchantmentName + " " + ChatColor.BLUE + "Nivel " + level);

            book.setItemMeta(meta);
        }
        return book;
    }

    private boolean isIllegalEnchantedBook(ItemStack item) {
        if (item == null || item.getType() != Material.ENCHANTED_BOOK || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return false;

        String name = ChatColor.stripColor(meta.getDisplayName());

        // Detectar por una parte específica del nombre, ajusta según tu caso
        return name.contains("Nivel") && !name.contains("Esencia"); // por ejemplo
    }

    private String formatEnchantmentName(String key) {
        Map<String, String> enchantmentNames = new HashMap<>();
        enchantmentNames.put("protection", "Protección");
        enchantmentNames.put("unbreaking", "Irrompibilidad");
        enchantmentNames.put("efficiency", "Eficiencia");
        enchantmentNames.put("fortune", "Fortuna");
        enchantmentNames.put("sharpness", "Filo");
        enchantmentNames.put("smite", "Castigo");
        enchantmentNames.put("bane_of_arthropods", "Perdición de los Artrópodos");
        enchantmentNames.put("feather_falling", "Caída de Pluma");
        enchantmentNames.put("looting", "Saqueo");
        enchantmentNames.put("depth_strider", "Agilidad Acuática");
        enchantmentNames.put("power", "Poder");

        return enchantmentNames.getOrDefault(key, key);
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        String title = "\u3201\u3201" + ChatColor.WHITE + "\u3200";

        if (event.getView().getTitle().equals(title)) {
            int slot = event.getRawSlot();
            Player player = (Player) event.getWhoClicked();
            Inventory gui = event.getInventory();
            Inventory clickedInventory = event.getClickedInventory();

            // Evitar doble procesamiento en una interacción
            if (event.isCancelled()) {
                return;
            }
            event.setCancelled(true);

            // Permitir que los jugadores interactúen con su propio inventario
            if (clickedInventory == null || clickedInventory.equals(player.getInventory())) {
                event.setCancelled(false);
                return;
            }

            if (event.isShiftClick() && clickedInventory.equals(player.getInventory())) {
                ItemStack currentItem = event.getCurrentItem();
                if (currentItem != null && inventory.getItem(36) == null) {
                    inventory.setItem(36, currentItem.clone());
                    event.getClickedInventory().setItem(event.getSlot(), null);
                    updateEnchantmentBooksInGUI(inventory, currentItem);
                    player.updateInventory();
                    return;
                }
            }

            if (slot == 36 && event.isShiftClick()) {
                ItemStack currentItem = event.getCurrentItem();
                if (currentItem == null) {
                    resetEnchantmentBooksToLevel1(inventory);
                }
            }

            if (slot == 36 && event.getAction() == InventoryAction.PLACE_ALL && event.getCursor() == null) {
                resetEnchantmentBooksToLevel1(inventory);
            }

            if (slot == 36 || slot == 37 || slot == 38) {
                event.setCancelled(false);
                if (slot == 36 && event.getCursor() != null) {
                    updateEnchantmentBooksInGUI(inventory, event.getCursor());
                }
            } else {
                event.setCancelled(true);
            }

            if (slot < inventory.getSize()) {
                if (slot >= 13 && slot <= 40 && slot != 36 && slot != 37 && slot != 38) {
                    ItemStack book = gui.getItem(slot);
                    if (book == null || !book.getType().equals(Material.ENCHANTED_BOOK)) {
                        return;
                    }

                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
                    if (meta == null) return;

                    Map<Enchantment, Integer> enchantments = meta.getStoredEnchants();
                    if (enchantments.isEmpty()) return;

                    Enchantment selectedEnchantment = enchantments.keySet().iterator().next();
                    int currentLevel = enchantments.get(selectedEnchantment);

                    ItemStack itemToEnchant = gui.getItem(36);
                    if (itemToEnchant == null) {
                        return;
                    }

                    if (!selectedEnchantment.canEnchantItem(itemToEnchant)) {
                        sendMessageOnce(player, ChatColor.RED + "۞ Este encantamiento no se puede aplicar a este objeto.");
                        return;
                    }

                    ItemMeta itemMeta = itemToEnchant.getItemMeta();
                    int existingLevel = itemMeta != null ? itemMeta.getEnchantLevel(selectedEnchantment) : 0;

                    if (existingLevel >= selectedEnchantment.getMaxLevel()) {
                        return;
                    }

                    ItemStack lapis = gui.getItem(37);
                    if (lapis == null || lapis.getAmount() < 3) {
                        sendMessageOnce(player, ChatColor.RED + "۞ Necesitas al menos 3 de lapislázuli.");
                        return;
                    }

                    ItemStack essence = gui.getItem(38);
                    if (essence == null || !isValidEssence(essence, selectedEnchantment)) {
                        sendMessageOnce(player, ChatColor.RED + "۞ Necesitas la esencia correspondiente.");
                        return;
                    }

                    // Verificar que solo haya una esencia en el slot 38
                    if (essence.getAmount() > 1) {
                        sendMessageOnce(player, ChatColor.GRAY + "۞ Solo puedes usar una esencia.");
                        return;
                    }

                    if (player.getLevel() < 4) {
                        sendMessageOnce(player, ChatColor.RED + "۞ Necesitas al menos 4 niveles de experiencia.");
                        return;
                    }

                    // Aumentar el nivel del encantamiento
                    int newLevel = existingLevel + 1;
                    if (newLevel > selectedEnchantment.getMaxLevel()) {
                        newLevel = selectedEnchantment.getMaxLevel();
                    }
                    itemMeta.addEnchant(selectedEnchantment, newLevel, true);
                    itemToEnchant.setItemMeta(itemMeta);

                    updateEnchantmentBook(gui, slot, selectedEnchantment, newLevel);

                    lapis.setAmount(lapis.getAmount() - 3);
                    gui.setItem(37, lapis);

                    int usesLeft = decrementEssenceUsage(essence);
                    if (usesLeft > 0) {
                        updateEssenceLore(essence, usesLeft);
                        gui.setItem(38, essence);
                    } else {
                        gui.setItem(38, null);
                    }

                    player.setLevel(player.getLevel() - 4);
                    player.updateInventory();

                    player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
                }
            }
        }
    }


    private void sendMessageOnce(Player player, String message) {
        // Método auxiliar para evitar duplicación de mensajes en una sesión de evento
        player.sendMessage(message);
    }

    private boolean isValidEssence(ItemStack essence, Enchantment enchantment) {
        if (essence == null || !essence.hasItemMeta()) {
            return false;
        }
        String essenceName = essence.getItemMeta().getDisplayName();

        switch (enchantment.getKey().getKey()) {
            case "protection":
                return essenceName.equals(ChatColor.BLUE + "Esencia de Protección");
            case "unbreaking":
                return essenceName.equals(ChatColor.BLUE + "Esencia de Irrompibilidad");
            case "efficiency":
                return essenceName.equals(ChatColor.BLUE + "Esencia de Eficiencia");
            case "fortune":
                return essenceName.equals(ChatColor.BLUE + "Esencia de Fortuna");
            case "sharpness":
                return essenceName.equals(ChatColor.BLUE + "Esencia de Filo");
            case "smite":
                return essenceName.equals(ChatColor.BLUE + "Esencia de Castigo");
            case "bane_of_arthropods":
                return essenceName.equals(ChatColor.BLUE + "Esencia de Perdición de los Artrópodos");
            case "feather_falling":
                return essenceName.equals(ChatColor.BLUE + "Esencia de Caída de Pluma");
            case "looting":
                return essenceName.equals(ChatColor.BLUE + "Esencia de Saqueo");
            case "depth_strider":
                return essenceName.equals(ChatColor.BLUE + "Esencia de Agilidad Acuática");
            case "power":
                return essenceName.equals(ChatColor.BLUE + "Esencia de Poder");
            default:
                return false;
        }
    }


    private int decrementEssenceUsage(ItemStack essence) {
        if (essence == null || !essence.hasItemMeta()) {
            return 0;
        }

        ItemMeta meta = essence.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey usesKey = new NamespacedKey("vicionthardcore3", "uses");

        if (data.has(usesKey, PersistentDataType.INTEGER)) {
            int uses = data.get(usesKey, PersistentDataType.INTEGER);
            int newUses = uses - 1;
            data.set(usesKey, PersistentDataType.INTEGER, Math.max(newUses, 0));
            essence.setItemMeta(meta);
            return newUses;
        }

        return 0;
    }

    private void updateEssenceLore(ItemStack essence, int usesLeft) {
        ItemMeta meta = essence.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore();

            if (lore == null || lore.isEmpty()) {
                lore = new ArrayList<>();
                lore.add(ChatColor.DARK_PURPLE + "Con esta Esencia podrás encantar");
                lore.add(ChatColor.DARK_PURPLE + "cualquier ítem en la " + ChatColor.GOLD + "Mesa de Encantamientos Mejorada");
                lore.add(" ");
            } else {
                if (lore.size() > 0 && lore.get(lore.size() - 1).startsWith(ChatColor.GRAY + "Usos restantes:")) {
                    lore.remove(lore.size() - 1);
                }
            }

            lore.add(ChatColor.GRAY + "Usos restantes: " + usesLeft);
            meta.setLore(lore);
            essence.setItemMeta(meta);
        }
    }

    private void updateEnchantmentBooksInGUI(Inventory gui, ItemStack itemInSlot36) {
        if (itemInSlot36 != null && itemInSlot36.hasItemMeta()) {
            ItemMeta itemMeta = itemInSlot36.getItemMeta();
            if (itemMeta != null && itemMeta.hasEnchants()) {
                for (Map.Entry<Enchantment, Integer> entry : itemMeta.getEnchants().entrySet()) {
                    Enchantment enchantment = entry.getKey();
                    int currentLevel = entry.getValue();
                    if (currentLevel < enchantment.getMaxLevel()) {
                        updateAllMatchingBooks(gui, enchantment, currentLevel);
                    }
                }
            }
        }
    }

    private void updateAllMatchingBooks(Inventory gui, Enchantment enchantment, int currentLevel) {
        int[] slots = { 13, 14, 15, 16, 22, 23, 24, 25, 31, 32, 33 };

        for (int slot : slots) {
            ItemStack book = gui.getItem(slot);
            if (book != null && book.getType() == Material.ENCHANTED_BOOK) {
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
                if (meta != null && meta.hasStoredEnchant(enchantment)) {
                    updateEnchantmentBook(gui, slot, enchantment, currentLevel);
                }
            }
        }
    }

    private void updateEnchantmentBook(Inventory gui, int slot, Enchantment enchantment, int currentLevel) {
        ItemStack book = gui.getItem(slot);
        if (book != null && book.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
            if (meta != null) {
                int newLevel = currentLevel + 1;
                if (newLevel > enchantment.getMaxLevel()) {
                    newLevel = enchantment.getMaxLevel();
                }
                meta.removeStoredEnchant(enchantment);
                meta.addStoredEnchant(enchantment, newLevel, true);
                String enchantmentName = formatEnchantmentName(enchantment.getKey().getKey());
                meta.setDisplayName(ChatColor.GOLD + enchantmentName + " " + ChatColor.BLUE + "Nivel " + newLevel);
                book.setItemMeta(meta);
                gui.setItem(slot, book);
            }
        }
    }

    private void resetEnchantmentBooksToLevel1(Inventory gui) {
        int[] slots = { 13, 14, 15, 16, 22, 23, 24, 25, 31, 32, 33 };
        for (int slot : slots) {
            ItemStack book = gui.getItem(slot);
            if (book != null && book.getType() == Material.ENCHANTED_BOOK) {
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
                if (meta != null) {
                    for (Enchantment enchantment : meta.getStoredEnchants().keySet()) {
                        meta.removeStoredEnchant(enchantment);
                        meta.addStoredEnchant(enchantment, 1, true);
                        String enchantmentName = formatEnchantmentName(enchantment.getKey().getKey());
                        meta.setDisplayName(ChatColor.GOLD + enchantmentName + " " + ChatColor.BLUE + "Nivel 1");
                    }
                    book.setItemMeta(meta);
                    gui.setItem(slot, book);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = "\u3201\u3201" +  ChatColor.WHITE + "\u3200";

        if (event.getView().getTitle().equals(title)) {
            for (int slot : event.getRawSlots()) {
                if (slot < event.getInventory().getSize() && (slot < 36 || slot > 38)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteractWithBlock(PlayerInteractEvent event) {
        // Ignorar si no es la mano principal (evita múltiples ejecuciones)
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && clickedBlock.getType() == Material.GREEN_GLAZED_TERRACOTTA) {
                event.setCancelled(true);

                int bookshelfCount = countBookshelvesAround(clickedBlock.getLocation(), 4);
                if (bookshelfCount >= 30) {
                    openEnhancedEnchantmentTableGUI(event.getPlayer());
                } else {
                    sendMessageOnce(event.getPlayer(), ChatColor.GRAY + "۞ Necesitas al menos 30 estanterías alrededor para usar esta mesa.");
                }
            }
        }
    }

    private int countBookshelvesAround(Location center, int radius) {
        int count = 0;

        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = cy - radius; y <= cy + radius; y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    if (x == cx && y == cy && z == cz) continue;
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.BOOKSHELF) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    @EventHandler
    public void onPlayerPlaceItemInFurnace(InventoryClickEvent event) {
        if (event.getClickedInventory() != null && event.getView().getTopInventory().getType() == InventoryType.FURNACE) {
            if (event.getWhoClicked() instanceof Player player) {
                ItemStack item = event.getCurrentItem();

                if (item != null && item.getType() == Material.GREEN_TERRACOTTA) {
                    event.setCancelled(true);

                    boolean itemAlreadyInInventory = false;
                    for (ItemStack inventoryItem : player.getInventory().getContents()) {
                        if (inventoryItem != null && inventoryItem.isSimilar(item)) {
                            itemAlreadyInInventory = true;
                            break;
                        }
                    }

                    if (!itemAlreadyInInventory) {
                        player.getInventory().addItem(item);
                    }
                    player.sendMessage(ChatColor.RED + "۞ No puedes colocar este bloque en el horno.");
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Location blockLocation = block.getLocation();
        World world = block.getWorld();

        if (block.getType() == Material.GREEN_GLAZED_TERRACOTTA) {
            if (particleTasks.containsKey(blockLocation)) {
                particleTasks.get(blockLocation).cancel();
                particleTasks.remove(blockLocation);
            }

            // Remover los bloques de luz alrededor del bloque
            Block aboveBlock = world.getBlockAt(blockLocation.clone().add(0, 1, 0));
            if (aboveBlock.getType() == Material.LIGHT) {
                aboveBlock.setType(Material.AIR);
            }

            if (tool.getType() == Material.DIAMOND_PICKAXE || tool.getType() == Material.NETHERITE_PICKAXE) {
                event.setDropItems(false);
                block.setType(Material.AIR);
                block.getWorld().dropItemNaturally(block.getLocation(), EnhancedEnchantmentTable.createEnhancedEnchantmentTable());
            } else {
                event.setCancelled(true);

                long currentTime = System.currentTimeMillis();
                long lastMessageTime = player.getMetadata("lastMessageTime").stream()
                        .map(MetadataValue::asLong)
                        .findFirst()
                        .orElse(0L);

                if (currentTime - lastMessageTime >= 10000) {
                    player.setMetadata("lastMessageTime", new FixedMetadataValue(plugin, currentTime));
                    player.sendMessage(ChatColor.GRAY + "۞ Necesitas un pico de diamante o mejor para romper la Mesa de Encantamientos Mejorada.");
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();
        World world = block.getWorld();
        Location blockLocation = block.getLocation();

        if (block.getType() == Material.GREEN_GLAZED_TERRACOTTA) {
            BukkitRunnable particleTask = new BukkitRunnable() {
                double angle = 0;

                @Override
                public void run() {
                    if (!block.getType().equals(Material.GREEN_GLAZED_TERRACOTTA)) {
                        this.cancel();
                        return;
                    }

                    double radius = 1.5;
                    double centerX = blockLocation.getX() + 0.5;
                    double centerY = blockLocation.getY() + 0.5;
                    double centerZ = blockLocation.getZ() + 0.5;

                    // Número de partículas en el círculo
                    int numParticles = 15;

                    // Calculamos las posiciones para las partículas a lo largo del círculo
                    for (int i = 0; i < numParticles; i++) {
                        double angle = (2 * Math.PI / numParticles) * i;  // Distribuir las partículas de manera uniforme en el círculo

                        double x = centerX + radius * Math.cos(angle);
                        double z = centerZ + radius * Math.sin(angle);

                        // Genera la partícula en la posición calculada
                        world.spawnParticle(Particle.PORTAL, x, centerY, z, 1, 0, 0, 0, 0);
                    }
                }
            };
            particleTask.runTaskTimer(plugin, 0, 2);
            particleTasks.put(blockLocation, particleTask);

            // Coloca los bloques de luz alrededor del bloque
            Block aboveBlock = world.getBlockAt(blockLocation.clone().add(0, 1, 0));
            if (aboveBlock.getType() == Material.AIR) {
                aboveBlock.setType(Material.LIGHT);
                aboveBlock.setBlockData(Bukkit.createBlockData("minecraft:light[level=10]"));
            }
        }
    }

    @EventHandler
    public void onHopperMoveItem(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() == Material.GREEN_TERRACOTTA) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        String title = "\u3201\u3201" +  ChatColor.WHITE + "\u3200";
        Player player = (Player) event.getPlayer();

        if (event.getView().getTitle().equals(title)) {

            int[] allowedSlots = {36, 37, 38};
            boolean hasItems = false;

            for (int slot : allowedSlots) {
                ItemStack item = inventory.getItem(slot);
                if (item != null) {
                    hasItems = true;
                    player.getInventory().addItem(item);
                    inventory.setItem(slot, null);
                }
            }
            if (hasItems) {
                player.sendMessage(ChatColor.YELLOW + "۞ Los objetos de la mesa mejorada han sido devueltos.");
            }
        }

        for (ItemStack item : player.getInventory().getContents()) {
            if (isIllegalEnchantedBook(item)) {
                player.getInventory().remove(item);
                player.sendMessage(ChatColor.GRAY + "۞ Se eliminó un libro prohibido de tu inventario.");
            }
        }
    }
}
