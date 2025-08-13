package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ReviveItems {

    private final JavaPlugin plugin;

    public ReviveItems(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack createResurrectOrb() {
        ItemStack item = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#0099ff") + "" + ChatColor.BOLD + "Orbe de la Vida");
        meta.setCustomModelData(2);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "");
        lore.add(ChatColor.of("#33cccc") + "Con este " + ChatColor.of("#66ccff") + "orbe mágico");
        lore.add(ChatColor.of("#33cccc") + "podrás traer de vuelta");
        lore.add(ChatColor.of("#33cccc") + "a este mundo un " + ChatColor.of("#66ccff") + "alma" + ChatColor.of("#33cccc") + " atrapada");
        lore.add(ChatColor.of("#33cccc") + "en la " + ChatColor.of("#990066") + "oscura corrupción" + ChatColor.of("gray") + ".");
        lore.add(ChatColor.GRAY + "");
        lore.add(ChatColor.GRAY + "(Se usa en el Spawn)");
        lore.add(ChatColor.GRAY + "");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

}
