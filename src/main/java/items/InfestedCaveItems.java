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


    public static ItemStack createRawSculkCrystal(int amount) {
        ItemStack item = new ItemStack(Material.RAW_COPPER, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#3e88a8") + "" + ChatColor.BOLD + "Sculk Crystal en Bruto");
        meta.setCustomModelData(20);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.of("#43988e") + "Mineral proveniente de una ");
        lore.add(ChatColor.of("#43988e") + "dimensión extraña. ");
        lore.add(ChatColor.of("#62b5b7") + "Se dice que un ser omnipresente");
        lore.add(ChatColor.of("#62b5b7") + "lo creó para fines " + ChatColor.of("#349698") + "desconocidos" + ChatColor.of("#62b5b7") + "."
        );
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createSculkCrystalFragment() {
        ItemStack item = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#4ca3c8") + "" + ChatColor.BOLD + "Fragmento de Sculk Crystal");
        meta.setCustomModelData(50);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.of("#43988e") + "Fragmento que surge del");
        lore.add(ChatColor.of("#43988e") + "extraño mineral de Sculk.");
        lore.add(ChatColor.of("#44bbad") + "Su nivel de energía es tan alto");
        lore.add(ChatColor.of("#44bbad") + "que causaría " + ChatColor.of("#bb7444") + "quemaduras graves" + ChatColor.of("#44bbad") + ".");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createEmptyRune() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#c1d3d7") + "" + ChatColor.BOLD + "Runa Vacia");
        meta.setCustomModelData(3000);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.GRAY + ">" + " " + ChatColor.of("#99c2d6") + "Esta runa no tiene");
        lore.add(ChatColor.of("#99c2d6") + "ningún poder de energía en ");
        lore.add(ChatColor.of("#99c2d6") + "su interior.");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createNormalRune() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#58bee9") + "" + ChatColor.BOLD + "Runa de Sculk Crystal");
        meta.setCustomModelData(3001);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.GRAY + ">" + " " + ChatColor.of("#99c2d6") + "Esta runa tiene");
        lore.add(ChatColor.of("#99c2d6") + "el poder de la energía del");
        lore.add(ChatColor.of("#4ca3c8") + "" + ChatColor.BOLD + "Sculk Crystal" + ChatColor.of("#99c2d6") + ".");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }
}
