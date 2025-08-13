package Events.UltraWitherBattle;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class UltraWitherCompass {
    private final JavaPlugin plugin;

    public UltraWitherCompass(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack createUltraWitherCompass() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§e§lUltra Wither Compass");
        meta.setCustomModelData(800);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + ""); // línea vacía
        lore.add(ChatColor.of("#660000") + "Este artefacto te enviará a una");
        lore.add(ChatColor.of("#660000") + "zona de la que pocas personas han");
        lore.add(ChatColor.of("#660000") + "salido con " + ChatColor.BOLD + "vida" + ChatColor.RESET + ChatColor.WHITE + ".");
        lore.add(ChatColor.GRAY + "");
        lore.add(ChatColor.of("#cc0066") + "Un ente llamado " + ChatColor.BOLD + "Ultra Wither");
        lore.add(ChatColor.of("#cc0066") + "acecha ese lugar" + ChatColor.RESET + ChatColor.WHITE + ".");
        lore.add(ChatColor.GRAY + "");
        lore.add(ChatColor.WHITE + ">> " + ChatColor.GRAY + "Click derecho al item para usarlo");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isUltraWitherCompass(ItemStack item) {
        if (item == null || item.getType() != Material.ECHO_SHARD) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData() || meta.getCustomModelData() != 800) {
            return false;
        }

        return meta.hasDisplayName() && meta.getDisplayName().equals(ChatColor.of("#0099ff") + "" + ChatColor.BOLD + "Ultra Wither Compass");
    }
}
