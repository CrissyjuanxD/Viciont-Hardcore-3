package Enchants;

import Armors.CorruptedArmor;
import Handlers.DayHandler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
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

import java.util.*;

public class EnhancedEnchantmentGUI implements Listener {

    private final JavaPlugin plugin;
    private final DayHandler dayHandler;
    private final ItemStack grayPane = createGrayPane();
    private final Map<Player, ItemStack[]> playerInventoryContents = new HashMap<>();
    private final Map<Location, BukkitRunnable> particleTasks = new HashMap<>();

    private final Map<UUID, Long> messageCooldowns = new HashMap<>();

    private static final String TABLE_TAG = "viciont_enchant_table";

    public EnhancedEnchantmentGUI(JavaPlugin plugin, DayHandler dayHandler) {
        this.plugin = plugin;
        this.dayHandler = dayHandler;

        loadAllActiveTables();
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
        Inventory gui = Bukkit.createInventory(null, 54, "\u3201\u3201" + ChatColor.WHITE + "\u3200");
        Enchantment[] enchantments = {
                Enchantment.PROTECTION, Enchantment.UNBREAKING,
                Enchantment.EFFICIENCY, Enchantment.FORTUNE, Enchantment.SHARPNESS,
                Enchantment.SMITE, Enchantment.BANE_OF_ARTHROPODS, Enchantment.FEATHER_FALLING,
                Enchantment.LOOTING, Enchantment.DEPTH_STRIDER,
                Enchantment.POWER
        };
        int[] slots = {13, 14, 15, 16, 22, 23, 24, 25, 31, 32, 33};
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

            // Nombres Hermosos Hexadecimales
            meta.setDisplayName(net.md_5.bungee.api.ChatColor.of("#fca37d") + "۞ " +
                    net.md_5.bungee.api.ChatColor.of("#ffb3ba") + enchantmentName + " " +
                    net.md_5.bungee.api.ChatColor.of("#bae1ff") + "Nivel " + level);

            book.setItemMeta(meta);
        }
        return book;
    }

    private boolean isIllegalEnchantedBook(ItemStack item) {
        if (item == null || item.getType() != Material.ENCHANTED_BOOK || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return false;
        String name = ChatColor.stripColor(meta.getDisplayName());
        return name.contains("Nivel") && !name.contains("Esencia");
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
            Inventory clickedInventory = event.getClickedInventory();

            if (event.isCancelled()) return;
            event.setCancelled(true);

            if (clickedInventory != null && clickedInventory.equals(player.getInventory()) && !event.isShiftClick()) {
                event.setCancelled(false);
                return;
            }

            if (event.isShiftClick() && clickedInventory != null && clickedInventory.equals(player.getInventory())) {
                ItemStack currentItem = event.getCurrentItem();
                if (currentItem == null || currentItem.getType() == Material.AIR) return;

                if (currentItem.getType() == Material.LAPIS_LAZULI) {
                    moveItemToSlot(inventory, 37, currentItem);
                } else if (isEssence(currentItem)) {
                    moveItemToSlot(inventory, 38, currentItem);
                } else {
                    if (inventory.getItem(36) == null) {
                        inventory.setItem(36, currentItem.clone());
                        currentItem.setAmount(0);
                        updateEnchantmentBooksInGUI(inventory, inventory.getItem(36));
                    }
                }
                player.updateInventory();
                return;
            }

/*            // Shift click para sacar cosas de la mesa
            if (slot == 36 || slot == 37 || slot == 38) {
                if (event.isShiftClick()) {
                    event.setCancelled(false); // Dejamos que Minecraft vanilla haga el movimiento natural

                    if (slot == 36) {
                        // Usamos un pequeño delay de 1 tick para asegurarnos de que el ítem
                        // realmente se movió (por si el inventario del jugador estaba lleno)
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (inventory.getItem(36) == null) {
                                resetEnchantmentBooksToLevel1(inventory);
                            }
                        });
                    }
                    return;
                }
            }*/

            if (slot == 36 || slot == 37 || slot == 38) {
                if (event.isShiftClick()) {
                    event.setCancelled(false);

                    if (slot == 36) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (inventory.getItem(36) == null) {
                                resetEnchantmentBooksToLevel1(inventory);
                            }
                        });
                    }
                    return;
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

            if (slot < inventory.getSize() && slot >= 13 && slot <= 40 && slot != 36 && slot != 37 && slot != 38) {
                ItemStack book = inventory.getItem(slot);
                if (book == null || !book.getType().equals(Material.ENCHANTED_BOOK)) return;

                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
                if (meta == null || meta.getStoredEnchants().isEmpty()) return;

                Enchantment selectedEnchantment = meta.getStoredEnchants().keySet().iterator().next();

                ItemStack itemToEnchant = inventory.getItem(36);
                if (itemToEnchant == null) return;

                if (itemToEnchant.getAmount() > 1) {
                    sendMessageOnce(player, net.md_5.bungee.api.ChatColor.of("#ffadad") + "۞ Solo puedes encantar 1 ítem a la vez.");
                    return;
                }

                boolean isBook = itemToEnchant.getType() == Material.BOOK;
                boolean isEnchantedBook = itemToEnchant.getType() == Material.ENCHANTED_BOOK;

                if (!selectedEnchantment.canEnchantItem(itemToEnchant) && !isBook && !isEnchantedBook) {
                    sendMessageOnce(player, net.md_5.bungee.api.ChatColor.of("#ffadad") + "۞ Este encantamiento no se puede aplicar a este objeto.");
                    return;
                }

                ItemMeta itemMeta = itemToEnchant.getItemMeta();
                int existingLevel = 0;

                if (itemMeta instanceof EnchantmentStorageMeta storageMeta) {
                    existingLevel = storageMeta.getStoredEnchantLevel(selectedEnchantment);
                } else if (itemMeta != null) {
                    existingLevel = itemMeta.getEnchantLevel(selectedEnchantment);
                }

                int customMaxLevel = getCustomMaxLevel(selectedEnchantment, itemToEnchant);
                if (existingLevel >= customMaxLevel) return;

                ItemStack lapis = inventory.getItem(37);
                if (lapis == null || lapis.getAmount() < 3) {
                    sendMessageOnce(player, net.md_5.bungee.api.ChatColor.of("#ffadad") + "۞ Necesitas al menos 3 de lapislázuli.");
                    return;
                }

                ItemStack essence = inventory.getItem(38);
                if (essence == null || !isValidEssence(essence, selectedEnchantment)) {
                    sendMessageOnce(player, net.md_5.bungee.api.ChatColor.of("#ffadad") + "۞ Necesitas la esencia correspondiente.");
                    return;
                }

                if (essence.getAmount() > 1) {
                    sendMessageOnce(player, net.md_5.bungee.api.ChatColor.of("#ffd6a5") + "۞ Solo puedes usar una esencia en el slot.");
                    return;
                }

                if (player.getLevel() < 4) {
                    sendMessageOnce(player, net.md_5.bungee.api.ChatColor.of("#ffadad") + "۞ Necesitas al menos 4 niveles de experiencia.");
                    return;
                }

                int newLevel = Math.min(existingLevel + 1, customMaxLevel);

                if (isBook) {
                    itemToEnchant.setType(Material.ENCHANTED_BOOK);
                    itemMeta = itemToEnchant.getItemMeta();
                }

                if (itemMeta instanceof EnchantmentStorageMeta storageMeta) {
                    storageMeta.addStoredEnchant(selectedEnchantment, newLevel, true);
                } else if (itemMeta != null) {
                    itemMeta.addEnchant(selectedEnchantment, newLevel, true);
                }
                itemToEnchant.setItemMeta(itemMeta);

                updateEnchantmentBook(inventory, slot, selectedEnchantment, newLevel);

                lapis.setAmount(lapis.getAmount() - 3);
                inventory.setItem(37, lapis);

                int usesLeft = decrementEssenceUsage(essence);
                if (usesLeft > 0) {
                    updateEssenceLore(essence, usesLeft);
                    inventory.setItem(38, essence);
                } else {
                    inventory.setItem(38, null);
                }

                player.setLevel(player.getLevel() - 4);
                player.updateInventory();
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
            }
        }
    }

    private void moveItemToSlot(Inventory gui, int slot, ItemStack currentItem) {
        ItemStack existing = gui.getItem(slot);
        if (existing == null) {
            gui.setItem(slot, currentItem.clone());
            currentItem.setAmount(0);
        } else if (existing.isSimilar(currentItem)) {
            int space = existing.getMaxStackSize() - existing.getAmount();
            if (space > 0) {
                int toAdd = Math.min(space, currentItem.getAmount());
                existing.setAmount(existing.getAmount() + toAdd);
                currentItem.setAmount(currentItem.getAmount() - toAdd);
            }
        }
    }

    private boolean isEssence(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        return name != null && name.contains("Esencia");
    }

    private void sendMessageOnce(Player player, String message) {
        player.sendMessage(message);
    }

    private boolean isValidEssence(ItemStack essence, Enchantment enchantment) {
        if (essence == null || !essence.hasItemMeta()) return false;
        String essenceName = ChatColor.stripColor(essence.getItemMeta().getDisplayName());
        if (essenceName == null) return false;

        switch (enchantment.getKey().getKey()) {
            case "protection": return essenceName.equals("Esencia de Protección");
            case "unbreaking": return essenceName.equals("Esencia de Irrompibilidad");
            case "efficiency": return essenceName.equals("Esencia de Eficiencia");
            case "fortune": return essenceName.equals("Esencia de Fortuna");
            case "sharpness": return essenceName.equals("Esencia de Filo");
            case "smite": return essenceName.equals("Esencia de Castigo");
            case "bane_of_arthropods": return essenceName.equals("Esencia de Perdición de los Artrópodos");
            case "feather_falling": return essenceName.equals("Esencia de Caída de Pluma");
            case "looting": return essenceName.equals("Esencia de Saqueo");
            case "depth_strider": return essenceName.equals("Esencia de Agilidad Acuática");
            case "power": return essenceName.equals("Esencia de Poder");
            default: return false;
        }
    }

    private int decrementEssenceUsage(ItemStack essence) {
        if (essence == null || !essence.hasItemMeta()) return 0;
        ItemMeta meta = essence.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey usesKey = new NamespacedKey("vicionthardcore3", "uses");

        if (data.has(usesKey, PersistentDataType.INTEGER)) {
            int uses = data.get(usesKey, PersistentDataType.INTEGER);
            int newUses = Math.max(uses - 1, 0);
            data.set(usesKey, PersistentDataType.INTEGER, newUses);
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
                if (lore.size() > 0 && ChatColor.stripColor(lore.get(lore.size() - 1)).startsWith("Usos restantes:")) {
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
            Map<Enchantment, Integer> enchants = new HashMap<>();

            if (itemMeta instanceof EnchantmentStorageMeta storageMeta) {
                enchants = storageMeta.getStoredEnchants();
            } else if (itemMeta.hasEnchants()) {
                enchants = itemMeta.getEnchants();
            }

            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                Enchantment enchantment = entry.getKey();
                int currentLevel = entry.getValue();
                updateAllMatchingBooks(gui, enchantment, currentLevel);
            }
        }
    }

    private void updateAllMatchingBooks(Inventory gui, Enchantment enchantment, int currentLevel) {
        int[] slots = { 13, 14, 15, 16, 22, 23, 24, 25, 31, 32, 33 };
        ItemStack itemToEnchant = gui.getItem(36);

        for (int slot : slots) {
            ItemStack book = gui.getItem(slot);
            if (book != null && book.getType() == Material.ENCHANTED_BOOK) {
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
                if (meta != null && meta.hasStoredEnchant(enchantment)) {
                    int maxLevel = getCustomMaxLevel(enchantment, itemToEnchant);
                    if (currentLevel < maxLevel) {
                        updateEnchantmentBook(gui, slot, enchantment, currentLevel);
                    }
                }
            }
        }
    }

    private void updateEnchantmentBook(Inventory gui, int slot, Enchantment enchantment, int currentLevel) {
        ItemStack book = gui.getItem(slot);
        if (book != null && book.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
            if (meta != null) {
                ItemStack itemToEnchant = gui.getItem(36);
                int customMaxLevel = getCustomMaxLevel(enchantment, itemToEnchant);
                int newLevel = Math.min(currentLevel + 1, customMaxLevel);

                meta.removeStoredEnchant(enchantment);
                meta.addStoredEnchant(enchantment, newLevel, true);

                String enchantmentName = formatEnchantmentName(enchantment.getKey().getKey());
                meta.setDisplayName(net.md_5.bungee.api.ChatColor.of("#fca37d") + "۞ " +
                        net.md_5.bungee.api.ChatColor.of("#ffb3ba") + enchantmentName + " " +
                        net.md_5.bungee.api.ChatColor.of("#bae1ff") + "Nivel " + newLevel);

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

                        meta.setDisplayName(net.md_5.bungee.api.ChatColor.of("#fca37d") + "۞ " +
                                net.md_5.bungee.api.ChatColor.of("#ffb3ba") + enchantmentName + " " +
                                net.md_5.bungee.api.ChatColor.of("#bae1ff") + "Nivel 1");
                    }
                    book.setItemMeta(meta);
                    gui.setItem(slot, book);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteractWithBlock(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && clickedBlock.getType() == Material.GREEN_GLAZED_TERRACOTTA) {
                event.setCancelled(true);
                Player player = event.getPlayer();

                int bookshelfCount = countBookshelvesAround(clickedBlock.getLocation(), 4);
                if (bookshelfCount >= 30) {
                    openEnhancedEnchantmentTableGUI(player);
                } else {
                    long now = System.currentTimeMillis();
                    if (now - messageCooldowns.getOrDefault(player.getUniqueId(), 0L) > 2000) {
                        int missing = 30 - bookshelfCount;
                        player.sendMessage(net.md_5.bungee.api.ChatColor.of("#ffadad") + "۞ Faltan " + missing + " estanterías alrededor para usar esta mesa.");
                        messageCooldowns.put(player.getUniqueId(), now);
                    }
                }
            }
        }
    }

    private int countBookshelvesAround(Location center, int radius) {
        int count = 0;
        World world = center.getWorld();
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = cy - radius; y <= cy + radius; y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    if (x == cx && y == cy && z == cz) continue;
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.BOOKSHELF) count++;
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
                    player.sendMessage(net.md_5.bungee.api.ChatColor.of("#ffadad") + "۞ No puedes colocar este bloque en el horno.");
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Location blockLoc = block.getLocation();

        if (block.getType() == Material.GREEN_GLAZED_TERRACOTTA) {
            if (tool.getType() == Material.DIAMOND_PICKAXE || tool.getType() == Material.NETHERITE_PICKAXE) {
                event.setDropItems(false);

                // --- NUEVO LÓGICA DE LIMPIEZA DE MARKERS ---
                if (particleTasks.containsKey(blockLoc)) {
                    particleTasks.get(blockLoc).cancel();
                    particleTasks.remove(blockLoc);
                }

                // Limpiar el bloque Marker asociado
                Location searchLoc = blockLoc.clone().add(0.5, 0.5, 0.5);
                for (Entity e : blockLoc.getWorld().getNearbyEntities(searchLoc, 0.5, 0.5, 0.5)) {
                    if (e instanceof Marker && e.getScoreboardTags().contains(TABLE_TAG)) {
                        e.remove();
                    }
                }

                Block aboveBlock = block.getWorld().getBlockAt(blockLoc.clone().add(0, 1, 0));
                if (aboveBlock.getType() == Material.LIGHT) aboveBlock.setType(Material.AIR);

                block.setType(Material.AIR);
                block.getWorld().dropItemNaturally(blockLoc, EnhancedEnchantmentTable.createEnhancedEnchantmentTable());
            } else {
                event.setCancelled(true);
                long now = System.currentTimeMillis();
                if (now - messageCooldowns.getOrDefault(player.getUniqueId(), 0L) > 10000) {
                    player.sendMessage(net.md_5.bungee.api.ChatColor.of("#ffadad") + "۞ Necesitas un pico de diamante o mejor para romper la Mesa de Encantamientos Mejorada.");
                    messageCooldowns.put(player.getUniqueId(), now);
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Location blockLoc = block.getLocation();

        if (block.getType() == Material.GREEN_GLAZED_TERRACOTTA) {
            // --- NUEVO: CREAR LA ENTIDAD MARKER ---
            blockLoc.getWorld().spawn(blockLoc.clone().add(0.5, 0.5, 0.5), Marker.class, marker -> {
                marker.addScoreboardTag(TABLE_TAG);
            });

            startParticleTask(blockLoc);

            Block aboveBlock = block.getWorld().getBlockAt(blockLoc.clone().add(0, 1, 0));
            if (aboveBlock.getType() == Material.AIR) {
                aboveBlock.setType(Material.LIGHT);
                aboveBlock.setBlockData(Bukkit.createBlockData("minecraft:light[level=10]"));
            }
        }
    }

    @EventHandler
    public void onHopperMoveItem(InventoryMoveItemEvent event) {
        if (event.getItem().getType() == Material.GREEN_TERRACOTTA) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        String title = "\u3201\u3201" + ChatColor.WHITE + "\u3200";
        Player player = (Player) event.getPlayer();

        if (event.getView().getTitle().equals(title)) {
            int[] allowedSlots = {36, 37, 38};
            for (int slot : allowedSlots) {
                ItemStack item = inventory.getItem(slot);
                if (item != null) {
                    player.getInventory().addItem(item);
                    inventory.setItem(slot, null);
                }
            }
        }

        for (ItemStack item : player.getInventory().getContents()) {
            if (isIllegalEnchantedBook(item)) {
                player.getInventory().remove(item);
                player.sendMessage(ChatColor.GRAY + "۞ Se eliminó un libro prohibido de tu inventario.");
            }
        }
    }

    private int getCustomMaxLevel(Enchantment enchantment, ItemStack item) {
        int maxNormal = enchantment.getMaxLevel();
        int day = dayHandler.getCurrentDay();

        if (item != null && (item.getType() == Material.BOOK || item.getType() == Material.ENCHANTED_BOOK)) {
            return maxNormal;
        }

        if (CorruptedArmor.isCorruptedArmor(item)) {

            if (enchantment.equals(Enchantment.UNBREAKING)) {
                if (day >= 8) return 7;
                return 3;
            }
        }

        return maxNormal;
    }

    // --- NUEVO SISTEMA DE PARTÍCULAS BASADO EN CHUNKS ---
    private void startParticleTask(Location loc) {
        if (particleTasks.containsKey(loc)) return; // Evitar tareas duplicadas

        BukkitRunnable particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                Block block = loc.getBlock();
                if (block.getType() != Material.GREEN_GLAZED_TERRACOTTA) {
                    this.cancel();
                    particleTasks.remove(loc);
                    return;
                }

                double radius = 1.5;
                double centerX = loc.getX() + 0.5;
                double centerY = loc.getY() + 0.5;
                double centerZ = loc.getZ() + 0.5;
                int numParticles = 15;

                for (int i = 0; i < numParticles; i++) {
                    double angle = (2 * Math.PI / numParticles) * i;
                    double x = centerX + radius * Math.cos(angle);
                    double z = centerZ + radius * Math.sin(angle);
                    loc.getWorld().spawnParticle(Particle.PORTAL, x, centerY, z, 1, 0, 0, 0, 0);
                }
            }
        };
        particleTask.runTaskTimer(plugin, 0, 2);
        particleTasks.put(loc, particleTask);
    }

    private void loadAllActiveTables() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                startParticlesForChunk(chunk);
            }
        }
    }

    private void startParticlesForChunk(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Marker && entity.getScoreboardTags().contains(TABLE_TAG)) {
                Location blockLoc = entity.getLocation().getBlock().getLocation();
                if (blockLoc.getBlock().getType() == Material.GREEN_GLAZED_TERRACOTTA) {
                    startParticleTask(blockLoc);
                } else {
                    entity.remove(); // Limpieza automática si el bloque fue roto de forma irregular
                }
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        startParticlesForChunk(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Marker && entity.getScoreboardTags().contains(TABLE_TAG)) {
                Location loc = entity.getLocation().getBlock().getLocation();
                if (particleTasks.containsKey(loc)) {
                    particleTasks.get(loc).cancel();
                    particleTasks.remove(loc);
                }
            }
        }
    }
}