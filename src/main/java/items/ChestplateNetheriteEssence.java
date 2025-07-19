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

public class ChestplateNetheriteEssence {
    private final JavaPlugin plugin;

    public ChestplateNetheriteEssence(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack createChestplateNetheriteEssence() {
        ItemStack essence = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = essence.getItemMeta();

        if (meta != null) {
            // Set display name
            meta.setDisplayName(ChatColor.of("#cc3300") + "" + ChatColor.BOLD + "Chestplate Netherite Essence");

            // Set lore
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#ffcc99") + "Esencia necesaria para");
            lore.add(ChatColor.of("#ffcc99") + "fabricar una mejora de armadura.");
            lore.add("");
            meta.setLore(lore);

            // Set custom model data
            meta.setCustomModelData(151);
            meta.setRarity(ItemRarity.EPIC);

            NamespacedKey key = new NamespacedKey(plugin, "invulnerable_item");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

            essence.setItemMeta(meta);
        }
        return essence;
    }
}