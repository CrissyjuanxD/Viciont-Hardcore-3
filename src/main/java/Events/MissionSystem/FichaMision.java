package Events.MissionSystem;

import com.fastasyncworldedit.core.entity.Metadatable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class FichaMision {
    private final NamespacedKey missionKey;

    public FichaMision(JavaPlugin plugin) {
        this.missionKey = new NamespacedKey(plugin, "mission_number");
    }

    public ItemStack createToken(int missionNumber, String missionName) {
        ItemStack token = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = token.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "Ficha de Misión #" + missionNumber);
        meta.setCustomModelData(2015);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Misión Completada:");
        lore.add(ChatColor.of("#FFCC99") + "Misión: " + ChatColor.WHITE + missionName);
        lore.add("");
        lore.add(ChatColor.GRAY + "Entrégalo en el spawn");
        lore.add(ChatColor.GRAY + "para " + ChatColor.BOLD + ChatColor.of("#FFCC99") + "terminar la Misión.");
        lore.add(ChatColor.GRAY + "> Click Derecho a la:");
        lore.add(ChatColor.of("#FFB347") + "Estatua de Recompensas");

        meta.setLore(lore);

        meta.getPersistentDataContainer().set(missionKey, PersistentDataType.INTEGER, missionNumber);
        meta.setRarity(ItemRarity.EPIC);
        token.setItemMeta(meta);
        return token;
    }

    public boolean isMissionToken(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(missionKey, PersistentDataType.INTEGER);
    }

    public int getMissionNumber(ItemStack item) {
        if (!isMissionToken(item)) return -1;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(missionKey, PersistentDataType.INTEGER, -1);
    }
}