package ShopSystem;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ShopGUI {

    private final ShopManager shopManager;

    public ShopGUI(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    public void openShopGUI(Player player, Villager villager) {
        String villagerName = villager.getCustomName() != null ? villager.getCustomName() : "Tienda";
        String title = ChatColor.GOLD + "" + ChatColor.BOLD + "Tienda: " + ChatColor.RESET + villagerName;
        if (title.length() > 32) title = title.substring(0, 32);

        Inventory gui = Bukkit.createInventory(null, 54, title);
        setupSession(player, villager, gui, false);
    }

    public void openConfigGUI(Player player, Villager villager) {
        String villagerName = villager.getCustomName() != null ? villager.getCustomName() : "Tienda";
        String title = ChatColor.RED + "" + ChatColor.BOLD + "Configurar: " + ChatColor.RESET + villagerName;
        if (title.length() > 32) title = title.substring(0, 32);

        Inventory gui = Bukkit.createInventory(null, 54, title);
        setupSession(player, villager, gui, true);
    }

    private void setupSession(Player player, Villager villager, Inventory gui, boolean isConfig) {
        String shopId = villager.getPersistentDataContainer().get(shopManager.shopIdKey, PersistentDataType.STRING);
        shopManager.activeShops.put(player.getUniqueId(), shopId);
        shopManager.loadShopTrades(shopId, villager);

        populateBaseGUI(gui, villager, isConfig);
        player.openInventory(gui);
    }

    private void populateBaseGUI(Inventory gui, Villager villager, boolean isConfig) {
        gui.setItem(0, createHeader(Material.ORANGE_DYE, ChatColor.GOLD + "Precio #1", false));
        gui.setItem(1, createHeader(Material.ORANGE_DYE, ChatColor.GOLD + "Precio #2", false));
        gui.setItem(2, createHeader(Material.IRON_NUGGET, ChatColor.GOLD + "\u279C", true));
        gui.setItem(3, createHeader(Material.LIME_DYE, ChatColor.GREEN + "Producto", false));

        gui.setItem(5, createHeader(Material.ORANGE_DYE, ChatColor.GOLD + "Precio #1", false));
        gui.setItem(6, createHeader(Material.ORANGE_DYE, ChatColor.GOLD + "Precio #2", false));
        gui.setItem(7, createHeader(Material.IRON_NUGGET, ChatColor.GOLD + "\u279C", true));
        gui.setItem(8, createHeader(Material.LIME_DYE, ChatColor.GREEN + "Producto", false));

        List<MerchantRecipe> recipes = villager.getRecipes();

        for (int i = 0; i < 10; i++) {
            int row = (i % 5) + 1;
            int colBase = (i < 5) ? 0 : 5;

            MerchantRecipe recipe = (i < recipes.size()) ? recipes.get(i) : null;
            ItemStack ing1 = null, ing2 = null, res = null;

            if (recipe != null) {
                if (recipe.getIngredients().size() > 0) ing1 = recipe.getIngredients().get(0);
                if (recipe.getIngredients().size() > 1) ing2 = recipe.getIngredients().get(1);
                res = recipe.getResult();
            }

            int baseSlot = row * 9 + colBase;
            gui.setItem(baseSlot, createDisplayItem(ing1, "Ingrediente 1", i, isConfig));
            gui.setItem(baseSlot + 1, createDisplayItem(ing2, "Ingrediente 2", i, isConfig));
            gui.setItem(baseSlot + 2, createHeader(Material.IRON_NUGGET, ChatColor.GOLD + "\u279C", true));
            gui.setItem(baseSlot + 3, createDisplayItem(res, "Producto", i, isConfig));
        }
    }

    private ItemStack createHeader(Material mat, String name, boolean enchant) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (enchant) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createDisplayItem(ItemStack item, String type, int tradeIndex, boolean isConfig) {
        boolean isPlaceholder = shopManager.isEmpty(item);
        ItemStack displayItem;

        if (isPlaceholder) {
            if (type.equals("Producto")) {
                displayItem = new ItemStack(Material.BARRIER);
                ItemMeta m = displayItem.getItemMeta();
                if (m != null) {
                    m.setDisplayName(ChatColor.RED + "Vacío");
                    displayItem.setItemMeta(m);
                }
            } else {
                displayItem = shopManager.createEmptyTradeItem();
            }
        } else {
            displayItem = item.clone();
        }

        if (isConfig) {
            ItemMeta meta = displayItem.getItemMeta();
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GOLD + "Rol: " + ChatColor.YELLOW + type);
            lore.add(ChatColor.GOLD + "Tradeo #: " + ChatColor.YELLOW + (tradeIndex + 1));
            lore.add("");
            lore.add(ChatColor.GREEN + "Click Izquierdo para editar.");
            lore.add(ChatColor.RED + "Click Derecho para borrar.");
            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        } else if (!isPlaceholder && type.equals("Producto")) {
            ItemMeta meta = displayItem.getItemMeta();
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.AQUA + ChatColor.BOLD.toString() + "\u25B6 ¡Click para Comprar!");
            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        }

        return displayItem;
    }
}