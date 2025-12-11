package Handlers;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootManager implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey lootKey;
    private final Random random;

    public LootManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.lootKey = new NamespacedKey(plugin, "custom_loot_table");
        this.random = new Random();
        // Registrar el listener automáticamente
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Método para "marcar" un cofre con una loot table

    public boolean setLootBlock(Block block, String lootTableName) {
        // Verificamos si el estado del bloque es un TileState (Cofres, Barriles, Shulkers, etc.)
        if (!(block.getState() instanceof TileState)) {
            return false;
        }

        // Usamos TileState en lugar de TileEntity
        TileState state = (TileState) block.getState();
        PersistentDataContainer container = state.getPersistentDataContainer();

        container.set(lootKey, PersistentDataType.STRING, lootTableName);

        state.update();
        return true;
    }

    // Evento: Se activa SOLO cuando alguien abre un inventario
    @EventHandler
    public void onChestOpen(InventoryOpenEvent event) {
        // Verificamos si el dueño del inventario es un bloque con estado (TileState)
        if (event.getInventory().getHolder() instanceof TileState) {

            TileState state = (TileState) event.getInventory().getHolder();
            PersistentDataContainer container = state.getPersistentDataContainer();

            if (container.has(lootKey, PersistentDataType.STRING)) {
                String tableName = container.get(lootKey, PersistentDataType.STRING);

                populateInventory(event.getInventory(), tableName);

                container.remove(lootKey);
                state.update();
            }
        }
    }

    // AQUÍ DEFINES TUS LOOT TABLES CUSTOM
    private void populateInventory(Inventory inv, String tableName) {
        inv.clear(); // Limpiar por seguridad

        switch (tableName.toLowerCase()) {
            case "dungeon_bee_loot":
                // Ejemplo: 50% de probabilidad de diamante
                if (random.nextBoolean()) inv.addItem(new ItemStack(Material.DIAMOND, 8));
                inv.addItem(new ItemStack(Material.GOLDEN_APPLE, 1));
                inv.addItem(new ItemStack(Material.HONEY_BOTTLE, 3));
                if (random.nextInt(100) < 5) {
                    inv.addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
                }
                inv.addItem(new ItemStack(Material.COBWEB, 10));
                if (random.nextInt(100) < 30) {
                    inv.addItem(new ItemStack(Material.GOLDEN_CARROT, 5));
                }
                if (random.nextBoolean()) inv.addItem(new ItemStack(Material.GOLD_INGOT, 4));
                break;

            case "runas_basicas":
                // Aquí podrías meter tus items custom de Runas (Pixelart 32x32 que mencionaste)
                inv.addItem(new ItemStack(Material.LAPIS_LAZULI, random.nextInt(10) + 1));
                inv.addItem(new ItemStack(Material.PAPER, 1));
                break;

            case "boss_iceologer":
                // Loot específico para tu Iceologer si quisieras cofres de recompensa
                inv.addItem(new ItemStack(Material.ICE, 10));
                inv.addItem(new ItemStack(Material.BLUE_ICE, 2));
                break;

            default:
                plugin.getLogger().warning("Se intentó generar una LootTable desconocida: " + tableName);
                break;
        }
    }
}