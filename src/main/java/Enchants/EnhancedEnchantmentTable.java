package Enchants;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

public class EnhancedEnchantmentTable {

    private final JavaPlugin plugin;

    public EnhancedEnchantmentTable(JavaPlugin plugin) {
        this.plugin = plugin;
        registerRecipes();
    }

    //ítem personalizado "Placa de Diamante"
    public ItemStack createDiamondPlate() {
        ItemStack diamondPlate = new ItemStack(Material.DIAMOND);
        ItemMeta meta = diamondPlate.getItemMeta();
        meta.setDisplayName("§bPlaca de Diamante");
        meta.setCustomModelData(1);  // Custom Model Data
        diamondPlate.setItemMeta(meta);
        return diamondPlate;
    }

    //ítem personalizado "Mesa de Encantamientos Mejorada"
    public static ItemStack createEnhancedEnchantmentTable() {
        ItemStack table = new ItemStack(Material.GREEN_GLAZED_TERRACOTTA);
        ItemMeta meta = table.getItemMeta();
        meta.setDisplayName("§bMesa de Encantamientos Mejorada");
        meta.setCustomModelData(1);
        table.setItemMeta(meta);
        return table;
    }

    // Recetas
    private void registerRecipes() {
        // Receta para la Placa de Diamante
        ShapedRecipe diamondPlateRecipe = new ShapedRecipe(new NamespacedKey(plugin, "diamond_plate"), createDiamondPlate());
        diamondPlateRecipe.shape(" I ", "IDI", " I ");
        diamondPlateRecipe.setIngredient('I', Material.IRON_INGOT);
        diamondPlateRecipe.setIngredient('D', Material.DIAMOND);
        Bukkit.addRecipe(diamondPlateRecipe);

        // Receta para la Mesa de Encantamientos Mejorada
        ShapedRecipe tableRecipe = new ShapedRecipe(new NamespacedKey(plugin, "enhanced_enchantment_table"), createEnhancedEnchantmentTable());
        tableRecipe.shape("DPD", "PTP", "BIB");
        tableRecipe.setIngredient('B', Material.BOOKSHELF);
        tableRecipe.setIngredient('P', Material.DIAMOND); // "Placa de Diamante", falta la diamond plate
        tableRecipe.setIngredient('T', Material.ENCHANTING_TABLE);
        tableRecipe.setIngredient('I', Material.GOLD_BLOCK);
        tableRecipe.setIngredient('D', Material.IRON_BLOCK);
        Bukkit.addRecipe(tableRecipe);
    }

}

