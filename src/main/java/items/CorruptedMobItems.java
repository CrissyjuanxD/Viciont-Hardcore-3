package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CorruptedMobItems {
    public enum BoneVariant {
        LIME("#80C71F", "Lime", 2),
        GREEN("#3F4E1B", "Green", 3),
        YELLOW("#FED83E", "Yellow", 4),
        ORANGE("#F99039", "Orange", 5),
        RED("#B02E26", "Red", 6);

        private final String hexColor;
        private final String displayName;
        private final int customModelData;

        BoneVariant(String hexColor, String displayName, int customModelData) {
            this.hexColor = hexColor;
            this.displayName = displayName;
            this.customModelData = customModelData;
        }

        public String getHexColor() {
            return hexColor;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getCustomModelData() {
            return customModelData;
        }
    }

    public static ItemStack createCorruptedBone(BoneVariant variant) {
        ItemStack bone = new ItemStack(Material.BONE);
        ItemMeta meta = bone.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of(variant.getHexColor()).toString() +
                    ChatColor.BOLD + variant.getDisplayName() +
                    " Corrupted Bone");

            meta.setCustomModelData(variant.getCustomModelData());
            meta.setRarity(ItemRarity.EPIC);
            bone.setItemMeta(meta);
        }

        return bone;
    }

    public static ItemStack createCorruptedPowder() {
        ItemStack powder = new ItemStack(Material.GUNPOWDER);
        ItemMeta meta = powder.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#8B008B") + "" + ChatColor.BOLD + "Corrupted Powder");
            meta.setCustomModelData(5);
            meta.setRarity(ItemRarity.EPIC);

            powder.setItemMeta(meta);
        }

        return powder;
    }

    public static ItemStack createCorruptedMeet() {
        ItemStack meat = new ItemStack(Material.ROTTEN_FLESH);
        ItemMeta meta = meat.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#8B008B") + "" + ChatColor.BOLD + "Corrupted Rotten Flesh");
            meta.setCustomModelData(5);
            meta.setRarity(ItemRarity.EPIC);

            meat.setItemMeta(meta);
        }

        return meat;
    }

    public static ItemStack createCorruptedSpiderEye() {
        ItemStack spiderEye = new ItemStack(Material.SPIDER_EYE);
        ItemMeta meta = spiderEye.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#8B008B") + "" + ChatColor.BOLD + "Corrupted Spider Eye");
            meta.setCustomModelData(5);
            meta.setRarity(ItemRarity.EPIC);

            spiderEye.setItemMeta(meta);
        }

        return spiderEye;

    }
}
