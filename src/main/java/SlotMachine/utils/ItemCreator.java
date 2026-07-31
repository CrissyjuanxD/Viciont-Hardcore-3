package SlotMachine.utils;

import Managers.ItemManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import vct.hardcore3.ViciontHardcore3;

/**
 * Creador de items para SlotMachine - Integrado con ViciontHardcore3 a través de ItemManager
 */
public class ItemCreator {

    private final ViciontHardcore3 plugin;
    private final ItemManager itemManager;

    public ItemCreator(ViciontHardcore3 plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    public ItemStack createItem(String itemId, int amount) {
        try {
            ItemStack customItem = itemManager.getItem(itemId, amount, null);
            if (customItem != null) {
                return customItem;
            }

            Material vanillaMaterial = getVanillaMaterial(itemId.toLowerCase());
            if (vanillaMaterial != null) {
                return new ItemStack(vanillaMaterial, amount);
            }

            plugin.getLogger().warning("Item desconocido en SlotMachine: " + itemId);
            return null;

        } catch (Exception e) {
            plugin.getLogger().severe("Error creando item " + itemId + ": " + e.getMessage());
            return null;
        }
    }

    private Material getVanillaMaterial(String materialName) {
        try {
            Material material = Material.matchMaterial(materialName);
            if (material == null && !materialName.startsWith("minecraft:")) {
                material = Material.matchMaterial("minecraft:" + materialName);
            }
            return material;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isVanillaItem(String itemId) {
        return getVanillaMaterial(itemId.toLowerCase()) != null;
    }
}