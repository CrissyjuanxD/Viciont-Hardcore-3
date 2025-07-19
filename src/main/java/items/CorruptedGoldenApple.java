package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class CorruptedGoldenApple {


    public static ItemStack createCorruptedGoldenApple() {
        ItemStack item = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#ccccff") + "" + ChatColor.BOLD + "Corrupted Golden Apple");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.of("#ffcc99") + "Esta manzana te otorga estos");
        lore.add(ChatColor.of("#ffcc99") + "efectos" + ChatColor.GRAY + ":");
        lore.add("");
        lore.add(ChatColor.GRAY + "> " + ChatColor.of("#ffff66") + "Absorción 5" + ChatColor.GRAY + " (" + ChatColor.of("#0099cc") + "2 min" + ChatColor.GRAY + ")");
        lore.add(ChatColor.GRAY + "> " + ChatColor.of("#cc99cc") + "Regeneración 3" + ChatColor.GRAY + " (" + ChatColor.of("#0099cc") + "1 min" + ChatColor.GRAY + ")");
        lore.add(ChatColor.GRAY + "> " + ChatColor.of("#cc3300") + "Saturación 1" + ChatColor.GRAY + " (" + ChatColor.of("#0099cc") + "1 min" + ChatColor.GRAY + ")");
        lore.add("");

        meta.setLore(lore);
        meta.setCustomModelData(10);
        item.setItemMeta(meta);

        return item;
    }

    public static ItemStack createApilateGoldBlock() {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + "Bloque de Oro Apilado");

        meta.setCustomModelData(10);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.KNOCKBACK, 1, true);
        item.setItemMeta(meta);

        return item;
    }

    public static void applyEffects(Player player) {
        // Absorción V (2 minutos)
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.ABSORPTION,
                2400,
                4,
                false,
                false
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.REGENERATION,
                1000,
                2,
                false,
                false
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SATURATION,
                1000,
                0,
                false,
                false
        ));
    }
}