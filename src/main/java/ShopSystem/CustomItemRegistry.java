package ShopSystem;

import Managers.ItemManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import vct.hardcore3.ViciontHardcore3;

import java.util.List;

public class CustomItemRegistry {

    private static ItemManager itemManager;

    public static void init(ViciontHardcore3 pl, ItemManager manager) {
        itemManager = manager;
    }

    public static ItemStack getCustomItem(String name, int amount) {
        ItemStack item = itemManager.getItem(name, amount, null);
        if (item != null) return item;
        try {
            return new ItemStack(Material.valueOf(name.toUpperCase()), amount);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static List<String> getAllCustomNames() {
        return itemManager.getRegisteredItems();
    }
}