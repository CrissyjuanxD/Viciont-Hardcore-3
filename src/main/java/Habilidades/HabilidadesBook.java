package Habilidades;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class HabilidadesBook {

    public static ItemStack createHabilidadesBook() {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();

        meta.setDisplayName(ChatColor.of("#C77DFF") + "" + ChatColor.BOLD + "Libro de Habilidades");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.of("#E0AAFF") + "Este libro místico contiene");
        lore.add(ChatColor.of("#E0AAFF") + "el poder de desbloquear habilidades");
        lore.add(ChatColor.of("#E0AAFF") + "únicas y poderosas.");
        lore.add("");
        lore.add(ChatColor.of("#9D4EDD") + "Click derecho para abrir");
        lore.add("");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        meta.setCustomModelData(9999);

        book.setItemMeta(meta);
        return book;
    }
}
