package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class CorruptedNetheriteItems {
    private final JavaPlugin plugin;

    public CorruptedNetheriteItems(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack createCorruptedScrapNetherite() {
        ItemStack item = new ItemStack(Material.NETHERITE_SCRAP);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Netherite Scrap");
        meta.setCustomModelData(5);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.of("#996699") + "Un fragmento de Netherite");
        lore.add(ChatColor.of("#996699") + "contaminado por la oscuridad");
        lore.add(ChatColor.of("#996699") + "de este mundo.");
        lore.add("");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCorruptedNetheriteIngot() {
        ItemStack item = new ItemStack(Material.NETHERITE_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#9900cc") + "" + ChatColor.BOLD + "Corrupted Netherite Ingot");
        meta.setCustomModelData(5);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.of("#993399") + "Un lingote de Netherite");
        lore.add(ChatColor.of("#993399") + "contaminado por la oscuridad");
        lore.add(ChatColor.of("#993399") + "de este mundo.");
        lore.add("");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }
}
