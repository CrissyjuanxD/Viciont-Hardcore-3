package items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BlazeItems {
    private final JavaPlugin plugin;

    public BlazeItems(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack createBlazeRod() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Vara de Guardian Blaze");
        meta.setCustomModelData(5);
        meta.setRarity(ItemRarity.RARE);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createGuardianBlazePowder() {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Polvo de Guardian Blaze");
        meta.setCustomModelData(10);
        meta.setRarity(ItemRarity.UNCOMMON);
        item.setItemMeta(meta);
        return item;
    }


    public static ItemStack createPotionOfFireResistance() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Ultra Poción de Resistencia al Fuego");
        meta.setCustomModelData(5);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 14400, 0, true, true, true), true);
        meta.setRarity(ItemRarity.RARE);

        potion.setItemMeta(meta);
        return potion;
    }
}
