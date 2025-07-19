package Enchants;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class EnhancedEnchantmentTable {

    private final JavaPlugin plugin;

    public EnhancedEnchantmentTable(JavaPlugin plugin) {
        this.plugin = plugin;
        registerRecipes();
    }

    //ítem personalizado "Placa de Diamante"
    public static ItemStack createDiamondPlate() {
        ItemStack diamondPlate = new ItemStack(Material.DIAMOND);
        ItemMeta meta = diamondPlate.getItemMeta();
        meta.setDisplayName("§bPlaca de Diamante");
        meta.setCustomModelData(1);

        List<String> lore = new ArrayList<>();

        lore.add("");
        lore.add(ChatColor.GRAY + "Una placa reforzada de diamante");
        lore.add(ChatColor.GRAY + "para fabricar la:");
        lore.add(ChatColor.GRAY + "'" + ChatColor.RESET + ChatColor.AQUA + ChatColor.BOLD + "Mesa de Encantamientos Mejorada" + ChatColor.RESET + ChatColor.GRAY + "'");
        lore.add("");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        diamondPlate.setItemMeta(meta);
        return diamondPlate;
    }

    //ítem personalizado "Mesa de Encantamientos Mejorada"
    public static ItemStack createEnhancedEnchantmentTable() {
        ItemStack table = new ItemStack(Material.GREEN_GLAZED_TERRACOTTA);
        ItemMeta meta = table.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Mesa de Encantamientos Mejorada");
        meta.setCustomModelData(1);

        List<String> lore = new ArrayList<>();

        lore.add("");
        lore.add(ChatColor.GRAY + "En esta mesa podrás encantar");
        lore.add(ChatColor.GRAY + "todos los " + ChatColor.GOLD + ChatColor.BOLD + "encantamientos");
        lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "bloqueados" + ChatColor.GRAY + ".");
        lore.add("");
        lore.add(ChatColor.GRAY + "Usa: ");
        lore.add(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + ">>"
                + ChatColor.GRAY + " " + ChatColor.GRAY + ChatColor.BOLD + "4XP"
                + ChatColor.GRAY + " por encantamiento.");
        lore.add(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + ">>"
                + ChatColor.GRAY + " " + ChatColor.GRAY + ChatColor.BOLD + "3 de Lápiz"
                + ChatColor.GRAY + " por encantamiento.");
        lore.add(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + ">>"
                + ChatColor.GRAY + " " + ChatColor.GRAY + ChatColor.BOLD + "1 uso"
                + ChatColor.GRAY + " de la esencia especial");
        lore.add("");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        table.setItemMeta(meta);
        return table;
    }

    // Recetas
    private void registerRecipes() {
        // Receta para la Placa de Diamante
        ShapedRecipe diamondPlateRecipe = new ShapedRecipe(new NamespacedKey(plugin, "diamond_plate"), createDiamondPlate());
        diamondPlateRecipe.shape("CIC", "IDI", "CIC");
        diamondPlateRecipe.setIngredient('I', Material.IRON_INGOT);
        diamondPlateRecipe.setIngredient('C', Material.COPPER_BLOCK);
        diamondPlateRecipe.setIngredient('D', Material.DIAMOND_BLOCK);
        Bukkit.addRecipe(diamondPlateRecipe);

        // Receta para la Mesa de Encantamientos Mejorada
        ShapedRecipe tableRecipe = new ShapedRecipe(new NamespacedKey(plugin, "enhanced_enchantment_table"), createEnhancedEnchantmentTable());
        tableRecipe.shape("DKD", "PTP", "BIB");
        tableRecipe.setIngredient('B', Material.BOOKSHELF);
        tableRecipe.setIngredient('K', Material.BOOK);
        tableRecipe.setIngredient('P', new RecipeChoice.ExactChoice(createDiamondPlate()));
        tableRecipe.setIngredient('T', Material.ENCHANTING_TABLE);
        tableRecipe.setIngredient('I', Material.GOLD_BLOCK);
        tableRecipe.setIngredient('D', Material.IRON_BLOCK);
        Bukkit.addRecipe(tableRecipe);
    }

}

