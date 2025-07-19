package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class UpgradeNTItems {
    private final JavaPlugin plugin;

    public UpgradeNTItems(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack createUpgradeVacio() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "" + ChatColor.BOLD + "Upgrade Vacío");
        meta.setCustomModelData(100);

        List<String> lore = new ArrayList<>();

        lore.add("");
        lore.add(ChatColor.GRAY + "Es un misterio el por qué");
        lore.add(ChatColor.GRAY + "este artefacto quedó destruido.");
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Pieza necesaria para");
        lore.add(ChatColor.DARK_GRAY + "obtener la '" + ChatColor.RESET + ChatColor.GOLD + ChatColor.BOLD + "Netherite Upgrade" + ChatColor.RESET + ChatColor.DARK_GRAY + "'.");
        lore.add("");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createFragmentoUpgrade() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Fragmento de Netherite Upgrade");
        meta.setCustomModelData(101);

        List<String> lore = new ArrayList<>();

        lore.add("");
        lore.add(ChatColor.GRAY + "Pareciera como si");
        lore.add(ChatColor.GRAY + "estuviera dividido en partes.");
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Pieza necesaria para");
        lore.add(ChatColor.DARK_GRAY + "obtener la '" + ChatColor.RESET + ChatColor.GOLD + ChatColor.BOLD + "Netherite Upgrade" + ChatColor.RESET + ChatColor.DARK_GRAY + "'.");
        lore.add("");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createDuplicador() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Duplicador");
        meta.setCustomModelData(102);

        List<String> lore = new ArrayList<>();

        lore.add("");
        lore.add(ChatColor.GRAY + "Antiguo artefacto capaz de");
        lore.add(ChatColor.GRAY + "duplicar la '" + ChatColor.RESET + ChatColor.GOLD + ChatColor.BOLD + "Netherite Upgrade" + ChatColor.RESET + ChatColor.GRAY + "'.");
        lore.add("");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }
}
