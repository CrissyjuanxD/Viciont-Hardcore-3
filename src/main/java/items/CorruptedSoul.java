package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class CorruptedSoul {
    private final JavaPlugin plugin;

    public CorruptedSoul(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack createCorruptedSoulEssence() {
        ItemStack essence = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = essence.getItemMeta();

        if (meta != null) {
            // Set display name
            meta.setDisplayName(ChatColor.of("#ffffcc") + "" + ChatColor.BOLD + "Corrupted Soul Essence");

            // Set lore
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#ffcc99") + "" + ChatColor.BOLD + "D" + ChatColor.RESET +ChatColor.of("#ffcc66") + "e las sombras nació" + ChatColor.STRIKETHROUGH + ChatColor.RESET +ChatColor.of("#ffcc66") + ".");
            lore.add(ChatColor.of("#ffcc99") + "" + ChatColor.BOLD + "A" + ChatColor.RESET +ChatColor.of("#ffcc66") + "ntiguos susurros corrompen almas,");
            lore.add(ChatColor.of("#ffcc99") + "" + ChatColor.BOLD + "R" + ChatColor.RESET +ChatColor.of("#ffcc66") + "esguarda el " + ChatColor.of("#ffcc99") + ChatColor.BOLD + "K" + ChatColor.RESET +ChatColor.of("#ffcc66") + "arma, la " + ChatColor.of("#ffcc99") + ChatColor.BOLD + "E" + ChatColor.RESET +ChatColor.of("#ffcc66") + "sencia y");
            lore.add(ChatColor.of("#ffcc66") + "el " + ChatColor.of("#ffcc99") + ChatColor.BOLD + "R" + ChatColor.RESET +ChatColor.of("#ffcc66") + "encor.");
            lore.add("");
            meta.setLore(lore);

            // Set custom model data
            meta.setCustomModelData(200);
            meta.setRarity(ItemRarity.EPIC);

            NamespacedKey key = new NamespacedKey(plugin, "invulnerable_item");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

            essence.setItemMeta(meta);
        }
        return essence;
    }
}
