package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CorrupcionAnsiosaItems {

    public static ItemStack createFragmentoCordura() {
        ItemStack item = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#EEC185") + "" + ChatColor.BOLD + "Fragmento de Cordura");
        meta.setCustomModelData(2);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.of("#B228E7") + "۞ " + ChatColor.of("#A777E9") + "Incrementa tu Corrupción Ansiosa");
        lore.add(ChatColor.of("#9ADE6F") + "+1%");
        lore.add("");
        lore.add(ChatColor.of("#B684E4") + "Un pequeño remanente de energía mental.");
        lore.add(ChatColor.of("#B684E4") + "Extrañamente cálido al tacto.");
        lore.add("");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.RARE);
        item.setItemMeta(meta);

        return item;
    }

    public static ItemStack createManzanaMarchita() {
        ItemStack item = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#EEC185") + "" + ChatColor.BOLD + "Manzana Marchita");
        meta.setCustomModelData(4);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.of("#B228E7") + "۞ " + ChatColor.of("#A777E9") + "Incrementa tu Corrupción Ansiosa");
        lore.add(ChatColor.of("#9ADE6F") + "+4%");
        lore.add("");
        lore.add(ChatColor.of("#B684E4") + "Una fruta deformada, drenada de vida.");
        lore.add(ChatColor.of("#B684E4") + "Pese a su olor, parece atraer tu mente.");
        lore.add("");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.RARE);
        item.setItemMeta(meta);

        return item;
    }

    public static ItemStack createCompuestoS13() {
        ItemStack item = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#EEC185") + "" + ChatColor.BOLD + "Compuesto S-13");
        meta.setCustomModelData(6);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.of("#B228E7") + "۞ " + ChatColor.of("#A777E9") + "Incrementa tu Corrupción Ansiosa");
        lore.add(ChatColor.of("#9ADE6F") + "+8%");
        lore.add("");
        lore.add(ChatColor.of("#B684E4") + "Un agente químico prohibido.");
        lore.add(ChatColor.of("#B684E4") + "Su olor te hace sentir... más inquieto.");
        lore.add("");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.RARE);
        item.setItemMeta(meta);

        return item;
    }

    public static ItemStack createSerumSerenidad() {
        ItemStack item = new ItemStack(Material.HONEY_BOTTLE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#EEC185") + "" + ChatColor.BOLD + "Sérum de Serenidad");
        meta.setCustomModelData(2);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.of("#B228E7") + "۞ " + ChatColor.of("#A777E9") + "Incrementa tu Corrupción Ansiosa");
        lore.add(ChatColor.of("#9ADE6F") + "+15%");
        lore.add("");
        lore.add(ChatColor.of("#B684E4") + "Un líquido brillante que promete calma...");
        lore.add(ChatColor.of("#B684E4") + "pero solo causa dependencia mental.");
        lore.add("");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);

        return item;
    }
}
