package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class EmblemItems {
    private final JavaPlugin plugin;

    public EmblemItems(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack createPepitaInfernal() {
        ItemStack item = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Pepita Infernal");
        meta.setCustomModelData(16);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Rara esencia " + ChatColor.RESET + ChatColor.GRAY + "encerrada en roca,");
        lore.add(ChatColor.GRAY + "perteneciente a los restos de");
        lore.add(ChatColor.GRAY + "una" + ChatColor.GRAY + ChatColor.BOLD + " civilización perdida.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Su energía ardiente es necesaria");
        lore.add(ChatColor.GRAY + "para forjar un " + ChatColor.RED + ChatColor.BOLD + "Fragmento Infernal" + ChatColor.GRAY + ChatColor.BOLD + ".");
        lore.add("");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createFragmentoInfernal() {
        ItemStack item = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Fragmento Infernal");
        meta.setCustomModelData(15);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.GRAY + "" + ChatColor.BOLD + "Fragmentos " + ChatColor.RESET + ChatColor.GRAY + "que fueron creados");
        lore.add(ChatColor.GRAY + "por una " + ChatColor.BOLD + "civilización antigua");
        lore.add(ChatColor.GRAY + "en este mundo.");
        lore.add("");
        lore.add(ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "Se dice que pueden atraer criaturas");
        lore.add(ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "extrañas.");
        lore.add(ChatColor.GRAY + " ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    //Infernal Nether Star, que sera un item para poder hacer la End Relic
    public static ItemStack createcorruptedNetherStar() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Corrupted Nether Star");
        meta.setCustomModelData(5);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.GRAY + "Una estrella que se dice que fue");
        lore.add(ChatColor.GRAY + "creada por una civilización antigua,");
        lore.add(ChatColor.GRAY + "con un poder inmenso.");
        lore.add("");
        lore.add(ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "Se dice que con esto se puede ingresar a otra dimension.");
        lore.add(ChatColor.GRAY + " ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createNetherEmblem() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Nether Emblem");
        meta.setCustomModelData(50);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.GRAY + "Con este " + ChatColor.GOLD + "emblema " + ChatColor.GRAY + "podrás");
        lore.add(ChatColor.GRAY + "ingresar a la " + ChatColor.RED + "dimensión del Nether" + ChatColor.GRAY + ".");
        lore.add("");
        lore.add(ChatColor.GRAY + "" + ChatColor.BOLD + "OJO: " + ChatColor.RESET + ChatColor.GRAY + "solo sirve para ingresar,");
        lore.add(ChatColor.RED + "" + ChatColor.BOLD + "NO " + ChatColor.RESET + ChatColor.GRAY + "para salir del Nether.");
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + ChatColor.ITALIC + "Uso: " + ChatColor.RESET + ChatColor.DARK_GRAY + ChatColor.ITALIC + "Tenerlo en el inventario.");
        lore.add(ChatColor.GRAY + " ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createOverworldEmblem() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Overworld Emblem");
        meta.setCustomModelData(51);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.GRAY + "Con este " + ChatColor.GOLD + "emblema " + ChatColor.GRAY + "podrás");
        lore.add(ChatColor.GRAY + "ingresar a la " + ChatColor.GREEN + "dimensión del Overworld" + ChatColor.GRAY + ".");
        lore.add("");
        lore.add(ChatColor.GRAY + "" + ChatColor.BOLD + "OJO: " + ChatColor.RESET + ChatColor.GRAY + "con este emblema puedes");
        lore.add(ChatColor.RED + "Salir del Nether" + ChatColor.RESET + ChatColor.GRAY + ".");
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + ChatColor.ITALIC + "Uso: " + ChatColor.RESET + ChatColor.DARK_GRAY + ChatColor.ITALIC + "Tenerlo en el inventario.");
        lore.add(ChatColor.GRAY + " ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    // End Relic lo mismo que un emeblema

    public static ItemStack createEndEmblem() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "End Emblem");
        meta.setCustomModelData(52);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.GRAY + "Con este " + ChatColor.GOLD + "emblema " + ChatColor.GRAY + "podrás");
        lore.add(ChatColor.GRAY + "ingresar a la " + ChatColor.LIGHT_PURPLE + "dimensión del End" + ChatColor.GRAY + ".");
        lore.add("");
        lore.add(ChatColor.GRAY + "" + ChatColor.BOLD + "OJO: " + ChatColor.RESET + ChatColor.GRAY + "solo sirve para ingresar,");
        lore.add(ChatColor.RED + "" + ChatColor.BOLD + "NO " + ChatColor.RESET + ChatColor.GRAY + "para salir del End.");
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + ChatColor.ITALIC + "Uso: " + ChatColor.RESET + ChatColor.DARK_GRAY + ChatColor.ITALIC + "Tenerlo en el inventario.");
        lore.add(ChatColor.GRAY + " ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }
}
