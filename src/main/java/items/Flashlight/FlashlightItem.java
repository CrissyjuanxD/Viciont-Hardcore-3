package items.Flashlight;

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

public class FlashlightItem {
    private final JavaPlugin plugin;
    private final NamespacedKey flashlightKey;

    public FlashlightItem(JavaPlugin plugin) {
        this.plugin = plugin;
        this.flashlightKey = new NamespacedKey(plugin, "flashlight_item");
    }

    public ItemStack createFlashlight() {
        ItemStack flashlight = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = flashlight.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#FFD700") + "" + ChatColor.BOLD + "Linterna");
            meta.setCustomModelData(10000); // Apagada por defecto

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#CCCCCC") + "Sirve para cuando las noches");
            lore.add(ChatColor.of("#CCCCCC") + "son completamente oscuras.");
            lore.add("");
            lore.add(ChatColor.GRAY + "Uso:");
            lore.add(ChatColor.GRAY + "> " + ChatColor.WHITE + "Click derecho");
            lore.add("");

            meta.setLore(lore);
            meta.setRarity(ItemRarity.EPIC);

            // Marcar como linterna y estado apagado
            meta.getPersistentDataContainer().set(flashlightKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "flashlight_state"), PersistentDataType.BOOLEAN, false);

            flashlight.setItemMeta(meta);
        }

        return flashlight;
    }

    public ItemStack createFlashlightOn() {
        ItemStack flashlight = createFlashlight();
        ItemMeta meta = flashlight.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#FFFF00") + "" + ChatColor.BOLD + "Linterna " + ChatColor.of("#00FF00") + ChatColor.BOLD + "(Encendida)");
            meta.setCustomModelData(10010); // Encendida

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#CCCCCC") + "Sirve para cuando las noches");
            lore.add(ChatColor.of("#CCCCCC") + "son completamente oscuras.");
            lore.add("");
            lore.add(ChatColor.of("#00FF00") + "Estado: " + ChatColor.BOLD + "Encendida");
            lore.add("");
            lore.add(ChatColor.GRAY + "Uso:");
            lore.add(ChatColor.GRAY + "> " + ChatColor.WHITE + "Click derecho para apagar");
            lore.add("");

            meta.setLore(lore);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "flashlight_state"), PersistentDataType.BOOLEAN, true);

            flashlight.setItemMeta(meta);
        }

        return flashlight;
    }

    public boolean isFlashlight(ItemStack item) {
        if (item == null || item.getType() != Material.ECHO_SHARD) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(flashlightKey, PersistentDataType.BYTE);
    }

    public boolean isFlashlightOn(ItemStack item) {
        if (!isFlashlight(item)) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().getOrDefault(
                new NamespacedKey(plugin, "flashlight_state"),
                PersistentDataType.BOOLEAN,
                false
        );
    }

    public ItemStack toggleFlashlight(ItemStack item) {
        if (!isFlashlight(item)) return item;

        boolean currentState = isFlashlightOn(item);
        return currentState ? createFlashlight() : createFlashlightOn();
    }
}