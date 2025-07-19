package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemsTotems {

    private final JavaPlugin plugin;

    public ItemsTotems(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack createUltraCorruptedSpiderEye() {
        ItemStack spiderEye = new ItemStack(Material.SPIDER_EYE);
        ItemMeta meta = spiderEye.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#cc0099") + "" + ChatColor.BOLD + "Ultra Corrupted Spider Eye");
            meta.setCustomModelData(10);
            meta.setRarity(ItemRarity.EPIC);

            spiderEye.setItemMeta(meta);
        }

        return spiderEye;
    }

    public static ItemStack createInfernalCreeperPowder() {
        ItemStack powder = new ItemStack(Material.GUNPOWDER);
        ItemMeta meta = powder.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#FF4500") + "" + ChatColor.BOLD + "Infernal Powder");
            meta.setCustomModelData(10);
            meta.setRarity(ItemRarity.EPIC);

            powder.setItemMeta(meta);
        }

        return powder;
    }

    public static ItemStack createWhiteEnderPearl() {
        ItemStack pearl = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = pearl.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#FFFFFF") + "" + ChatColor.BOLD + "White Ender Pearl");
            meta.setCustomModelData(5);
            meta.setRarity(ItemRarity.EPIC);

            pearl.setItemMeta(meta);
        }

        return pearl;
    }

    public static ItemStack createSpecialTotem() {
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = totem.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00FFFF") + "" + ChatColor.BOLD + "Totem Especial");
            meta.setRarity(ItemRarity.RARE);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addEnchant(Enchantment.KNOCKBACK, 2, true);

            totem.setItemMeta(meta);
        }

        return totem;
    }
}
