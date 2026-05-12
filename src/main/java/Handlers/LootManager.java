package Handlers;

import items.InfestedCaveItems;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class LootManager implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey lootKey;
    private final Map<String, Consumer<Inventory>> lootTables = new HashMap<>();
    private final Map<String, String> lootTitles = new HashMap<>();

    public LootManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.lootKey = new NamespacedKey(plugin, "custom_loot_table");

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // --- TUS LOOT TABLES ---
        registerLoot("dungeon_bee_loot", "&6Tesoro de la Abeja", this::lootDungeonBee);
        registerLoot("runas_basicas", "&bAlijo Rúnico", this::lootRunas);
        registerLoot("corrupted_end_1", "&5Cofre Corrupto", this::lootCorruptedEnd1);
        registerLoot("infested_loot", "&3Cofre Infestado", this::lootInfestedLoot);
    }

    private void registerLoot(String id, String title, Consumer<Inventory> logic) {
        lootTables.put(id.toLowerCase(), logic);
        lootTitles.put(id.toLowerCase(), ChatColor.translateAlternateColorCodes('&', title));
    }

    public Set<String> getLootTableNames() {
        return lootTables.keySet();
    }

    public boolean isValidTable(String tableName) {
        return lootTables.containsKey(tableName.toLowerCase());
    }

    // --- LÓGICA DE BLOQUE (SPAWN) ---
    public boolean setLootBlock(Block block, String lootTableName) {
        if (!(block.getState() instanceof TileState)) return false;

        TileState state = (TileState) block.getState();
        PersistentDataContainer container = state.getPersistentDataContainer();
        String cleanName = lootTableName.toLowerCase();

        container.set(lootKey, PersistentDataType.STRING, cleanName);

        if (lootTitles.containsKey(cleanName) && state instanceof org.bukkit.Nameable) {
            ((org.bukkit.Nameable) state).setCustomName(lootTitles.get(cleanName));
        }

        state.update();
        return true;
    }

    // --- LÓGICA DE ÍTEM (GIVE) ---
    public ItemStack getLootChestItem(String tableName) {
        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta meta = chest.getItemMeta();

        if (meta != null) {
            String cleanName = tableName.toLowerCase();
            String displayName = lootTitles.getOrDefault(cleanName, cleanName);

            meta.setDisplayName(ChatColor.GOLD + "Loot: " + displayName);
            meta.getPersistentDataContainer().set(lootKey, PersistentDataType.STRING, cleanName);
            chest.setItemMeta(meta);
        }
        return chest;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() == Material.CHEST && item.hasItemMeta()) {
            PersistentDataContainer itemPdc = item.getItemMeta().getPersistentDataContainer();

            if (itemPdc.has(lootKey, PersistentDataType.STRING)) {
                String tableName = itemPdc.get(lootKey, PersistentDataType.STRING);
                setLootBlock(event.getBlockPlaced(), tableName);
                event.getPlayer().sendMessage(ChatColor.GREEN + "Cofre de loot colocado correctamente.");
            }
        }
    }

    @EventHandler
    public void onChestOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof Container)) return;

        Container containerBlock = (Container) event.getInventory().getHolder();
        TileState state = (TileState) containerBlock;
        PersistentDataContainer pdc = state.getPersistentDataContainer();

        if (pdc.has(lootKey, PersistentDataType.STRING)) {
            String tableName = pdc.get(lootKey, PersistentDataType.STRING);
            pdc.remove(lootKey);
            state.update();

            if (lootTables.containsKey(tableName)) {
                Inventory liveInventory = event.getInventory();
                liveInventory.clear();
                lootTables.get(tableName).accept(liveInventory);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // Verificamos si es un contenedor (Cofre, Barril, Shulker...)
        if (!(block.getState() instanceof Container)) return;

        Container container = (Container) block.getState();
        PersistentDataContainer pdc = container.getPersistentDataContainer();

        // Si tiene la marca de loot custom
        if (pdc.has(lootKey, PersistentDataType.STRING)) {
            String tableName = pdc.get(lootKey, PersistentDataType.STRING);

            if (lootTables.containsKey(tableName)) {
                // 1. Obtenemos el inventario del bloque QUE VA A CAER
                Inventory inv = container.getInventory();
                inv.clear(); // Limpiamos por seguridad

                // 2. Llenamos el inventario usando tu lógica de loot
                lootTables.get(tableName).accept(inv);
            }

            // Opcional: Borrar la key antes de romper (aunque el bloque desaparece igual)
            pdc.remove(lootKey);
            container.update();
        }
    }

    // ==========================================
    //      MÉTODOS DE PROBABILIDAD (SCATTERING)
    // ==========================================

    // Versión ORIGINAL (Material)
    private void addDiminishingChance(Inventory inv, Material mat, int maxAmount) {
        // Redirige a la versión de ItemStack creando uno básico
        addDiminishingChance(inv, new ItemStack(mat), maxAmount);
    }

    // NUEVA Versión (ItemStack Custom)
    private void addDiminishingChance(Inventory inv, ItemStack prototype, int maxAmount) {
        int midAmount = (int) Math.max(1, maxAmount * 0.60);
        int minAmount = (int) Math.max(1, maxAmount * 0.35);

        int roll = ThreadLocalRandom.current().nextInt(100);

        // Clonamos el item base para no modificar el original de la clase Items
        ItemStack toAdd = prototype.clone();

        if (roll < 25) {
            toAdd.setAmount(maxAmount);
        } else if (roll < 60) {
            toAdd.setAmount(midAmount);
        } else {
            toAdd.setAmount(minAmount);
        }

        // Lo mandamos a esparcir
        scatterItem(inv, toAdd);
    }

    // Versión ORIGINAL (Material)
    private void addFixedChance(Inventory inv, Material mat, int amount, int chancePercent) {
        addFixedChance(inv, new ItemStack(mat), amount, chancePercent);
    }

    // NUEVA Versión (ItemStack Custom)
    private void addFixedChance(Inventory inv, ItemStack prototype, int amount, int chancePercent) {
        if (ThreadLocalRandom.current().nextInt(100) < chancePercent) {
            ItemStack toAdd = prototype.clone();
            toAdd.setAmount(amount);
            scatterItem(inv, toAdd);
        }
    }

    /**
     * MÉTODO CLAVE: SCATTER (Esparcir)
     */
    private void scatterItem(Inventory inv, ItemStack item) {
        int amountLeft = item.getAmount();

        // Obtenemos una lista de todos los slots vacíos disponibles
        List<Integer> emptySlots = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack current = inv.getItem(i);
            if (current == null || current.getType() == Material.AIR) {
                emptySlots.add(i);
            }
        }

        // Mientras tengamos items por poner Y tengamos espacio
        while (amountLeft > 0 && !emptySlots.isEmpty()) {

            int splitSize = ThreadLocalRandom.current().nextInt(1, 5);
            int actualSize = Math.min(amountLeft, splitSize);

            // Creamos el item pequeño copiando data del original
            ItemStack splitStack = item.clone();
            splitStack.setAmount(actualSize);

            int randomIndex = ThreadLocalRandom.current().nextInt(emptySlots.size());
            int targetSlot = emptySlots.get(randomIndex);

            inv.setItem(targetSlot, splitStack);

            amountLeft -= actualSize;
            emptySlots.remove(randomIndex);
        }

        // Sobrantes
        if (amountLeft > 0) {
            ItemStack remainder = item.clone();
            remainder.setAmount(amountLeft);
            inv.addItem(remainder);
        }
    }

    // ==========================================
    //            LOOT TABLES DEFINITION
    // ==========================================

    private void lootDungeonBee(Inventory inv) {
        // Ejemplo Diamantes: Si salen 10, se esparcirán en grupos de 1 a 4 por todo el cofre
        addDiminishingChance(inv, Material.DIAMOND, 8);
        addDiminishingChance(inv, Material.GOLD_INGOT, 12);
        addFixedChance(inv, Material.ENCHANTED_GOLDEN_APPLE, 1, 5);
        scatterItem(inv, new ItemStack(Material.HONEY_BOTTLE, 5));
        addDiminishingChance(inv, Material.COBWEB, 15);
        addDiminishingChance(inv, Material.GOLDEN_CARROT, 6);
    }

    private void lootRunas(Inventory inv) {
        addDiminishingChance(inv, Material.LAPIS_LAZULI, 10);
        addFixedChance(inv, Material.PAPER, 1, 50);
    }

    private void lootCorruptedEnd1(Inventory inv) {
        addDiminishingChance(inv, Material.GOLD_INGOT, 10);
        addDiminishingChance(inv, Material.DIAMOND, 4);
        addDiminishingChance(inv, Material.GOLDEN_CARROT, 8);
        addFixedChance(inv, Material.TOTEM_OF_UNDYING, 2, 20);
        addDiminishingChance(inv, Material.GOLD_BLOCK, 3);
        addDiminishingChance(inv, Material.GOLDEN_APPLE, 3);
        addDiminishingChance(inv, Material.EXPERIENCE_BOTTLE, 15);
    }

    private void lootInfestedLoot(Inventory inv) {
        addDiminishingChance(inv, Material.GOLD_INGOT, 7);
        scatterItem(inv, new ItemStack(Material.DIAMOND, 2));
        addDiminishingChance(inv, Material.ECHO_SHARD, 15);
        addDiminishingChance(inv, Material.LAPIS_LAZULI, 10);
        addFixedChance(inv, Material.TOTEM_OF_UNDYING, 1, 10);
        addFixedChance(inv, Material.ENCHANTED_GOLDEN_APPLE,1, 5);
        addFixedChance(inv, Material.GOLD_BLOCK, 2, 30);
        addFixedChance(inv, InfestedCaveItems.createRawSculkCrystal(1), 2, 40);
        addDiminishingChance(inv, Material.GOLDEN_APPLE, 3);
        addDiminishingChance(inv, Material.EXPERIENCE_BOTTLE, 10);
        addFixedChance(inv, Material.NETHERITE_SCRAP, 3, 20);
    }
}