package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class EndItems {
    private final JavaPlugin plugin;

    public EndItems(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    public static ItemStack createEnderiteNugget(int amount) {
        ItemStack item = new ItemStack(Material.IRON_NUGGET, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#009999") + "" + ChatColor.BOLD + "Pepita de Enderite");
        meta.setCustomModelData(100);

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

    public static ItemStack createFragmentoEnderite() {
        ItemStack item = new ItemStack(Material.RAW_COPPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#009999") + "" + ChatColor.BOLD + "Fragmento de Enderite");
        meta.setCustomModelData(400);

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

    public static ItemStack createIngotEnderite() {
        ItemStack item = new ItemStack(Material.NETHERITE_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#009999") + "" + ChatColor.BOLD + "Lingote de Enderite");
        meta.setCustomModelData(410);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.of("#336666") + "Con los lingotes podrás fabricar");
        lore.add(ChatColor.of("#336666") + "las" + ChatColor.GRAY + ": ");
        lore.add(" ");
        lore.add(ChatColor.GRAY + "" + ChatColor.BOLD + "> " + ChatColor.of("#3366cc") + ChatColor.BOLD + "Enderite Upgrades");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }


    public static ItemStack createEnderiteUpgrades() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#3366cc") + "" + ChatColor.BOLD + "Enderite Upgrade");
        meta.setCustomModelData(420);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.of("#336666") + "Mejora necesaria para");
        lore.add(ChatColor.of("#336666") + "fabricar las" + ChatColor.GRAY + ": ");
        lore.add(" ");
        lore.add(ChatColor.GRAY + "" + ChatColor.BOLD + "> " + ChatColor.of("#9c59d1") + ChatColor.BOLD + "Herramientas de Enderite");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    //Guardian Shulker Shell

    public static ItemStack createGuardianShulkerShell() {
        ItemStack item = new ItemStack(Material.SHULKER_SHELL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#3399ff") + "" + ChatColor.BOLD + "Guardian Shulker Shell");
        meta.setCustomModelData(10);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.of("#336666") + "Una concha de Shulker que");
        lore.add(ChatColor.of("#336666") + "ha sido bendecida por un");
        lore.add(ChatColor.of("#336666") + "guardián del End.");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    //End Amatist

    public static ItemStack createEndAmatist(int amount) {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#9933ff") + "Amatista del End");
        meta.setCustomModelData(10);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

}
