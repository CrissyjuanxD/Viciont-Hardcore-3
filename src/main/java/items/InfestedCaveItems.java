package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class InfestedCaveItems {
    private final JavaPlugin plugin;

    public InfestedCaveItems(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    public static ItemStack createRawInfestedCrystal(int amount) {
        ItemStack item = new ItemStack(Material.RAW_COPPER, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#009999") + "" + ChatColor.BOLD + "Pepita de Enderite");
        meta.setCustomModelData(20);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.of("#333366") + "" + ChatColor.BOLD + "Pepitas de cristales");
        lore.add(ChatColor.of("#336666") + "que aparecieron cuando surgió ");
        lore.add(ChatColor.of("#336666") + "una entidad desconocida en este");
        lore.add(ChatColor.of("#336666") + "mundo.");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createSculkCrystalFragment(int amount) {
        ItemStack item = new ItemStack(Material.IRON_NUGGET, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#009999") + "" + ChatColor.BOLD + "Pepita de Enderite");
        meta.setCustomModelData(50);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.of("#333366") + "" + ChatColor.BOLD + "Pepitas de cristales");
        lore.add(ChatColor.of("#336666") + "que aparecieron cuando surgió ");
        lore.add(ChatColor.of("#336666") + "una entidad desconocida en este");
        lore.add(ChatColor.of("#336666") + "mundo.");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createEmptyRune() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#009999") + "" + ChatColor.BOLD + "Fragmento de Enderite");
        meta.setCustomModelData(3000);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.of("#333366") + "" + ChatColor.BOLD + "Fragmentos de cristales " + ChatColor.RESET + ChatColor.of("#336666") + "que");
        lore.add(ChatColor.of("#336666") + "se han ido forjando con el");
        lore.add(ChatColor.of("#336666") + "paso del tiempo para poder fabricar");
        lore.add(ChatColor.of("#336666") + "nuevas herramientas.");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createNormalRune() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#009999") + "" + ChatColor.BOLD + "Fragmento de Enderite");
        meta.setCustomModelData(3001);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.of("#333366") + "" + ChatColor.BOLD + "Fragmentos de cristales " + ChatColor.RESET + ChatColor.of("#336666") + "que");
        lore.add(ChatColor.of("#336666") + "se han ido forjando con el");
        lore.add(ChatColor.of("#336666") + "paso del tiempo para poder fabricar");
        lore.add(ChatColor.of("#336666") + "nuevas herramientas.");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }
}
