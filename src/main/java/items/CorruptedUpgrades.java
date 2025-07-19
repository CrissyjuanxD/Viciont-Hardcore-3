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

public class CorruptedUpgrades {
    private final JavaPlugin plugin;
    private final NamespacedKey CORRUPTED_UPGRADE_KEY;

    public CorruptedUpgrades(JavaPlugin plugin) {
        this.plugin = plugin;
        this.CORRUPTED_UPGRADE_KEY = new NamespacedKey(plugin, "corrupted_upgrade");
    }

    public ItemStack createHelmetNetheriteUpgrade() {
        ItemStack essence = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = essence.getItemMeta();

        if (meta != null) {
            // Set display name
            meta.setDisplayName(ChatColor.of("#cc3366") + "" + ChatColor.BOLD + "Helmet Netherite Upgrade");

            // Set lore
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#ffcc99") + "Mejora necesaria para");
            lore.add(ChatColor.of("#ffcc99") + "fabricar la pieza de armadura:");
            lore.add("");
            lore.add(ChatColor.GRAY + "" + ChatColor.BOLD + "> " + ChatColor.of("#9966ff") + ChatColor.BOLD + "Corrupted Netherite Helmet");
            lore.add("");
            meta.setLore(lore);

            // Set custom model data
            meta.setCustomModelData(300);
            meta.setRarity(ItemRarity.EPIC);

            meta.getPersistentDataContainer().set(CORRUPTED_UPGRADE_KEY, PersistentDataType.STRING, "helmet");

            essence.setItemMeta(meta);
        }
        return essence;
    }

    public ItemStack createChestplateNetheriteUpgrade() {
        ItemStack essence = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = essence.getItemMeta();

        if (meta != null) {
            // Set display name
            meta.setDisplayName(ChatColor.of("#cc3366") + "" + ChatColor.BOLD + "Chestplate Netherite Upgrade");

            // Set lore
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#ffcc99") + "Mejora necesaria para");
            lore.add(ChatColor.of("#ffcc99") + "fabricar la pieza de armadura:");
            lore.add("");
            lore.add(ChatColor.GRAY + "" + ChatColor.BOLD + "> " + ChatColor.of("#9966ff") + ChatColor.BOLD + "Corrupted Netherite Chestplate");
            lore.add("");
            meta.setLore(lore);

            // Set custom model data
            meta.setCustomModelData(305);
            meta.setRarity(ItemRarity.EPIC);

            meta.getPersistentDataContainer().set(CORRUPTED_UPGRADE_KEY, PersistentDataType.STRING, "chestplate");

            essence.setItemMeta(meta);
        }
        return essence;
    }

    public ItemStack createLeggingsNetheriteUpgrade() {
        ItemStack essence = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = essence.getItemMeta();

        if (meta != null) {
            // Set display name
            meta.setDisplayName(ChatColor.of("#cc3366") + "" + ChatColor.BOLD + "Leggings Netherite Upgrade");

            // Set lore
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#ffcc99") + "Mejora necesaria para");
            lore.add(ChatColor.of("#ffcc99") + "fabricar la pieza de armadura:");
            lore.add("");
            lore.add(ChatColor.GRAY + "" + ChatColor.BOLD + "> " + ChatColor.of("#9966ff") + ChatColor.BOLD + "Corrupted Netherite Leggings");
            lore.add("");
            meta.setLore(lore);

            // Set custom model data
            meta.setCustomModelData(310);
            meta.setRarity(ItemRarity.EPIC);

            meta.getPersistentDataContainer().set(CORRUPTED_UPGRADE_KEY, PersistentDataType.STRING, "leggings");

            essence.setItemMeta(meta);
        }
        return essence;
    }

    public ItemStack createBootsNetheriteUpgrade() {
        ItemStack essence = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = essence.getItemMeta();

        if (meta != null) {
            // Set display name
            meta.setDisplayName(ChatColor.of("#cc3366") + "" + ChatColor.BOLD + "Boots Netherite Upgrade");

            // Set lore
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#ffcc99") + "Mejora necesaria para");
            lore.add(ChatColor.of("#ffcc99") + "fabricar la pieza de armadura:");
            lore.add("");
            lore.add(ChatColor.GRAY + "" + ChatColor.BOLD + "> " + ChatColor.of("#9966ff") + ChatColor.BOLD + "Corrupted Netherite Boots");
            lore.add("");
            meta.setLore(lore);

            // Set custom model data
            meta.setCustomModelData(315);
            meta.setRarity(ItemRarity.EPIC);

            meta.getPersistentDataContainer().set(CORRUPTED_UPGRADE_KEY, PersistentDataType.STRING, "boots");

            essence.setItemMeta(meta);
        }
        return essence;
    }

    public NamespacedKey getCorruptedUpgradeKey() {
        return CORRUPTED_UPGRADE_KEY;
    }
}
