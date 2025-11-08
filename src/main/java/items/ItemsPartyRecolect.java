package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;

public class ItemsPartyRecolect {

    // TAG para reconocer que es ítem de fiesta y su subtipo (1..4)
    public static final String KEY_PARTY = "itemparty_kind";

    private final NamespacedKey KEY;

    public ItemsPartyRecolect(JavaPlugin plugin) {
        this.KEY = new NamespacedKey(plugin, KEY_PARTY);
    }

    public ItemStack createCaramelo() { return build(Material.SUGAR, ChatColor.of("#ffd166") + "" + ChatColor.BOLD + "Caramelo", 1); }
    public ItemStack createPiruleta() { return build(Material.PAPER, ChatColor.of("#ef476f") + "" + ChatColor.BOLD + "Piruleta", 2); }
    public ItemStack createAlgodon()  { return build(Material.STRING, ChatColor.of("#bde0fe") + "" + ChatColor.BOLD + "Algodón de Azúcar", 3); }
    public ItemStack createSoda()     { return build(Material.HONEY_BOTTLE, ChatColor.of("#06d6a0") + "" + ChatColor.BOLD + "Soda", 4); }

    private ItemStack build(Material mat, String name, int kind) {
        ItemStack is = new ItemStack(mat, 1);
        ItemMeta meta = is.getItemMeta();
        meta.setDisplayName(name);
        // MARCA para stackeo: SOLO este byte + mismo material/nombre ⇒ stackean.
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) kind);
        is.setItemMeta(meta);
        return is;
    }

    public boolean isPartyItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        Byte v = item.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.BYTE);
        return v != null && v >= 1 && v <= 4;
    }

    public Byte getKind(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.BYTE);
    }
}
