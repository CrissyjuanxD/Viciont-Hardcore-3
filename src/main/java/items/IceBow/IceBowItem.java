package items.IceBow;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase responsable de crear y gestionar el item Ice Bow
 */
public class IceBowItem {
    private final JavaPlugin plugin;
    private final NamespacedKey iceBowKey;

    public IceBowItem(JavaPlugin plugin) {
        this.plugin = plugin;
        this.iceBowKey = new NamespacedKey(plugin, "ice_bow");
    }

    /**
     * Crea un arco de hielo con todas sus propiedades
     * @return ItemStack del arco de hielo
     */
    public ItemStack createIceBow() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();

        if (meta != null) {
            // Nombre del item
            meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Arco de Hielo");

            // Custom model data
            meta.setCustomModelData(5);

            // Lore descriptivo
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.AQUA + "Un arco forjado con hielo eterno");
            lore.add(ChatColor.AQUA + "que congela a sus víctimas.");
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.YELLOW + "▶ " + ChatColor.WHITE + "Congela enemigos por " + ChatColor.AQUA + "8 segundos");
            lore.add(ChatColor.YELLOW + "▶ " + ChatColor.WHITE + "Cooldown de " + ChatColor.RED + "10 segundos");
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "Arma del Iceologer");
            meta.setLore(lore);

            // Encantamientos
            meta.addEnchant(Enchantment.INFINITY, 1, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);

            // Marcar como arco de hielo usando PersistentDataContainer
            meta.getPersistentDataContainer().set(iceBowKey, PersistentDataType.BYTE, (byte) 1);

            bow.setItemMeta(meta);
        }

        return bow;
    }

    /**
     * Verifica si un ItemStack es un arco de hielo
     * @param item El ItemStack a verificar
     * @return true si es un arco de hielo, false en caso contrario
     */
    public boolean isIceBow(ItemStack item) {
        if (item == null || item.getType() != Material.BOW) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        // Verificar por PersistentDataContainer (método preferido)
        if (meta.getPersistentDataContainer().has(iceBowKey, PersistentDataType.BYTE)) {
            return true;
        }

        // Verificar por CustomModelData (método de respaldo)
        return meta.hasCustomModelData() && meta.getCustomModelData() == 5;
    }

    /**
     * Crea un arco de hielo para jugadores (con menor duración de efectos)
     * @return ItemStack del arco de hielo para jugadores
     */
    public ItemStack createPlayerIceBow() {
        ItemStack bow = createIceBow();
        ItemMeta meta = bow.getItemMeta();

        if (meta != null) {
            // Modificar el lore para jugadores
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.AQUA + "Un arco forjado con hielo eterno");
            lore.add(ChatColor.AQUA + "que congela a sus víctimas.");
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.YELLOW + "▶ " + ChatColor.WHITE + "Congela enemigos por " + ChatColor.AQUA + "3 segundos");
            lore.add(ChatColor.YELLOW + "▶ " + ChatColor.WHITE + "Cooldown de " + ChatColor.RED + "10 segundos");
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.BLUE + "" + ChatColor.ITALIC + "Versión para jugadores");
            meta.setLore(lore);

            bow.setItemMeta(meta);
        }

        return bow;
    }

    /**
     * Obtiene la clave del arco de hielo
     * @return NamespacedKey del arco de hielo
     */
    public NamespacedKey getIceBowKey() {
        return iceBowKey;
    }
}