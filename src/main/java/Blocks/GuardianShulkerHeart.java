package Blocks;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class GuardianShulkerHeart implements Listener {

    private final JavaPlugin plugin;

    public GuardianShulkerHeart(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack createGuardianShulkerHeart() {
        ItemStack block = new ItemStack(Material.PURPLE_GLAZED_TERRACOTTA);
        ItemMeta meta = block.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#009999") + "" + ChatColor.BOLD + "Corazón de Shulker Guardián");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#333366") + "Se dice que una antigua civilización");
            lore.add(ChatColor.of("#333366") + "usaba este corazón para");
            lore.add(ChatColor.of("#333366") + "invocar un ser poderoso.");
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

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.PURPLE_GLAZED_TERRACOTTA) {

            event.setCancelled(true);
            block.setType(Material.AIR);

            block.getWorld().dropItemNaturally(block.getLocation(), createGuardianShulkerHeart());

            Player player = event.getPlayer();
            player.playSound(block.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        }
    }
}
