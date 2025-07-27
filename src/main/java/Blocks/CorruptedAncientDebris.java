package Blocks;

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

public class CorruptedAncientDebris {
    private final JavaPlugin plugin;

    public CorruptedAncientDebris(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack createcorruptedancientdebris() {
        ItemStack block = new ItemStack(Material.ANCIENT_DEBRIS);
        ItemMeta meta = block.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#990066") + "" + ChatColor.BOLD + "Corrupted Ancient Debris");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#996699") + "Parece que un poder oscuro ha");
            lore.add(ChatColor.of("#996699") + "corrompido los restos de antiguos");
            lore.add(ChatColor.of("#996699") + "escombros de Netherite.");
            lore.add("");
            meta.setLore(lore);

            meta.setCustomModelData(5);
            meta.setRarity(ItemRarity.EPIC);

            NamespacedKey key = new NamespacedKey(plugin, "invulnerable_item");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

            block.setItemMeta(meta);
        }
        return block;
    }
}
