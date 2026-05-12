package StatueManager;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

public class StatueData {

    private static final NamespacedKey KEY_ID = new NamespacedKey("viciont", "statue_id");
    private static final NamespacedKey KEY_RAD_X = new NamespacedKey("viciont", "statue_rad_x");
    private static final NamespacedKey KEY_RAD_Y = new NamespacedKey("viciont", "statue_rad_y");
    private static final NamespacedKey KEY_HP_MAX = new NamespacedKey("viciont", "statue_hp_max");
    private static final NamespacedKey KEY_HP_CUR = new NamespacedKey("viciont", "statue_hp_current");
    private static final NamespacedKey KEY_COLOR = new NamespacedKey("viciont", "statue_color");
    private static final NamespacedKey KEY_EFF_TYPE = new NamespacedKey("viciont", "statue_eff_type");
    private static final NamespacedKey KEY_EFF_AMP = new NamespacedKey("viciont", "statue_eff_amp");

    private static final NamespacedKey KEY_VISIBLE = new NamespacedKey("viciont", "statue_visible");
    private static final NamespacedKey KEY_INVULNERABLE = new NamespacedKey("viciont", "statue_invulnerable");

    // Nueva Key para el Anti-Grief
    private static final NamespacedKey KEY_ANTI_GRIEF = new NamespacedKey("viciont", "statue_anti_grief");

    private PersistentDataContainer container;

    public StatueData(ArmorStand stand) {
        this.container = stand.getPersistentDataContainer();
    }

    public StatueData(ItemMeta meta) {
        this.container = meta.getPersistentDataContainer();
    }

    public static boolean isStatueItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY_ID, PersistentDataType.STRING);
    }

    public static boolean isStatue(ArmorStand stand) {
        return stand.getPersistentDataContainer().has(KEY_ID, PersistentDataType.STRING);
    }

    public void setDefaults() {
        container.set(KEY_ID, PersistentDataType.STRING, "statue_effect");
        setRadiusX(5.0);
        setRadiusY(3.0);
        setHpMax(10);
        setHpCurrent(10);
        setGlowColor(ChatColor.WHITE); // Default
        setEffect(PotionEffectType.SPEED, 0); // Esto automáticamente desactiva el AntiGrief
        setVisible(true);
        setInvulnerable(false);
    }

    // --- GETTERS Y SETTERS ---

    public void setRadiusX(double val) { container.set(KEY_RAD_X, PersistentDataType.DOUBLE, val); }
    public double getRadiusX() { return container.getOrDefault(KEY_RAD_X, PersistentDataType.DOUBLE, 5.0); }

    public void setRadiusY(double val) { container.set(KEY_RAD_Y, PersistentDataType.DOUBLE, val); }
    public double getRadiusY() { return container.getOrDefault(KEY_RAD_Y, PersistentDataType.DOUBLE, 3.0); }

    public void setHpMax(int val) { container.set(KEY_HP_MAX, PersistentDataType.INTEGER, val); }
    public int getHpMax() { return container.getOrDefault(KEY_HP_MAX, PersistentDataType.INTEGER, 10); }

    public void setHpCurrent(int val) { container.set(KEY_HP_CUR, PersistentDataType.INTEGER, val); }
    public int getHpCurrent() { return container.getOrDefault(KEY_HP_CUR, PersistentDataType.INTEGER, 10); }

    public void setGlowColor(ChatColor color) {
        String val = (color == null) ? "OFF" : color.name();
        container.set(KEY_COLOR, PersistentDataType.STRING, val);
    }

    public ChatColor getGlowColor() {
        String val = container.getOrDefault(KEY_COLOR, PersistentDataType.STRING, "WHITE");
        if (val.equals("OFF")) return null;
        try { return ChatColor.valueOf(val); }
        catch (Exception e) { return ChatColor.WHITE; }
    }

    // Al asignar un efecto de poción, se desactiva el AntiGrief por seguridad.
    public void setEffect(PotionEffectType type, int amp) {
        if (type != null) {
            container.set(KEY_EFF_TYPE, PersistentDataType.STRING, type.getName());
            container.set(KEY_EFF_AMP, PersistentDataType.INTEGER, amp);
            setAntiGrief(false); // Excluyente
        } else {
            container.set(KEY_EFF_TYPE, PersistentDataType.STRING, "NONE");
            container.set(KEY_EFF_AMP, PersistentDataType.INTEGER, 0);
        }
    }

    public PotionEffectType getEffectType() {
        String name = container.getOrDefault(KEY_EFF_TYPE, PersistentDataType.STRING, "NONE");
        if (name.equals("NONE")) return null;
        return PotionEffectType.getByName(name);
    }

    public void setEffectAmplifier(int amp) { container.set(KEY_EFF_AMP, PersistentDataType.INTEGER, amp); }
    public int getEffectAmplifier() { return container.getOrDefault(KEY_EFF_AMP, PersistentDataType.INTEGER, 0); }

    public void setVisible(boolean val) { container.set(KEY_VISIBLE, PersistentDataType.BYTE, val ? (byte)1 : (byte)0); }
    public boolean isVisible() { return container.getOrDefault(KEY_VISIBLE, PersistentDataType.BYTE, (byte)1) == 1; }

    public void setInvulnerable(boolean val) { container.set(KEY_INVULNERABLE, PersistentDataType.BYTE, val ? (byte)1 : (byte)0); }
    public boolean isInvulnerable() { return container.getOrDefault(KEY_INVULNERABLE, PersistentDataType.BYTE, (byte)0) == 1; }

    // --- NUEVO: Anti Grief ---
    public void setAntiGrief(boolean val) {
        container.set(KEY_ANTI_GRIEF, PersistentDataType.BYTE, val ? (byte)1 : (byte)0);
        if (val) {
            // Si activamos Anti-Grief, borramos el efecto de poción automáticamente
            container.set(KEY_EFF_TYPE, PersistentDataType.STRING, "NONE");
            container.set(KEY_EFF_AMP, PersistentDataType.INTEGER, 0);
        }
    }
    public boolean isAntiGrief() { return container.getOrDefault(KEY_ANTI_GRIEF, PersistentDataType.BYTE, (byte)0) == 1; }
}