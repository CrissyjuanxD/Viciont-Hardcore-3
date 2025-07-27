package Armors;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CorruptedArmor implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Double> originalMaxHealth = new HashMap<>();
    private final Set<UUID> checkedPlayers = new HashSet<>();
    private final Set<UUID> fullSetPlayers = new HashSet<>();
    private static final double BONUS_HEARTS = 4.0;

    public CorruptedArmor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack createCorruptedHelmet() {
        ItemStack item = new ItemStack(Material.NETHERITE_HELMET);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#9966ff") + "" + ChatColor.BOLD + "Corrupted Netherite Helmet");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.of("#996699") + "Una armadura forjada con",
                ChatColor.of("#996699") + "esencias de criaturas",
                ChatColor.of("#996699") + "extrañas.",
                "",
                ChatColor.GRAY + "El conjunto completo de esta",
                ChatColor.GRAY + "armadura otorga: " + ChatColor.WHITE,
                ChatColor.of("#ff6600") + "" + ChatColor.BOLD + "+2 " + ChatColor.GOLD + ChatColor.BOLD + "Corazones",
                ""
        ));

        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                UUID.randomUUID(),
                "armor_modifier",
                3,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HEAD
        ));
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(
                UUID.randomUUID(),
                "toughness_modifier",
                4,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HEAD
        ));
        meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(
                UUID.randomUUID(),
                "knockback_modifier",
                0.1,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HEAD
        ));
        meta.setCustomModelData(2);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCorruptedChestplate() {
        ItemStack item = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#9966ff") + "" + ChatColor.BOLD + "Corrupted Netherite Chestplate");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.of("#996699") + "Una armadura forjada con",
                ChatColor.of("#996699") + "esencias de criaturas",
                ChatColor.of("#996699") + "extrañas.",
                "",
                ChatColor.GRAY + "El conjunto completo de esta",
                ChatColor.GRAY + "armadura otorga: " + ChatColor.WHITE,
                ChatColor.of("#ff6600") + "" + ChatColor.BOLD + "+2 " + ChatColor.GOLD + ChatColor.BOLD + "Corazones",
                ""
        ));

        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                UUID.randomUUID(),
                "armor_modifier",
                9,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.CHEST
        ));
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(
                UUID.randomUUID(),
                "toughness_modifier",
                4,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.CHEST
        ));
        meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(
                UUID.randomUUID(),
                "knockback_modifier",
                0.1,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.CHEST
        ));
        meta.setCustomModelData(2);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCorruptedLeggings() {
        ItemStack item = new ItemStack(Material.NETHERITE_LEGGINGS);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#9966ff") + "" + ChatColor.BOLD + "Corrupted Netherite Leggings");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.of("#996699") + "Una armadura forjada con",
                ChatColor.of("#996699") + "esencias de criaturas",
                ChatColor.of("#996699") + "extrañas.",
                "",
                ChatColor.GRAY + "El conjunto completo de esta",
                ChatColor.GRAY + "armadura otorga: " + ChatColor.WHITE,
                ChatColor.of("#ff6600") + "" + ChatColor.BOLD + "+2 " + ChatColor.GOLD + ChatColor.BOLD + "Corazones",
                ""
        ));

        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                UUID.randomUUID(),
                "armor_modifier",
                6,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.LEGS
        ));
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(
                UUID.randomUUID(),
                "toughness_modifier",
                4,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.LEGS
        ));
        meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(
                UUID.randomUUID(),
                "knockback_modifier",
                0.1,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.LEGS
        ));
        meta.setCustomModelData(2);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCorruptedBoots() {
        ItemStack item = new ItemStack(Material.NETHERITE_BOOTS);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#9966ff") + "" + ChatColor.BOLD + "Corrupted Netherite Boots");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.of("#996699") + "Una armadura forjada con",
                ChatColor.of("#996699") + "esencias de criaturas",
                ChatColor.of("#996699") + "extrañas.",
                "",
                ChatColor.GRAY + "El conjunto completo de esta",
                ChatColor.GRAY + "armadura otorga: " + ChatColor.WHITE,
                ChatColor.of("#ff6600") + "" + ChatColor.BOLD + "+2 " + ChatColor.GOLD + ChatColor.BOLD + "Corazones",
                ""
        ));

        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                UUID.randomUUID(),
                "armor_modifier",
                4,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.FEET
        ));
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(
                UUID.randomUUID(),
                "toughness_modifier",
                4,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.FEET
        ));
        meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(
                UUID.randomUUID(),
                "knockback_modifier",
                0.1,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.FEET
        ));
        meta.setCustomModelData(2);
        item.setItemMeta(meta);
        return item;
    }

    private boolean hasFullSet(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        return isCorruptedPiece(helmet, "Helmet") &&
                isCorruptedPiece(chestplate, "Chestplate") &&
                isCorruptedPiece(leggings, "Leggings") &&
                isCorruptedPiece(boots, "Boots");
    }

    private boolean isCorruptedPiece(ItemStack item, String pieceName) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        String expectedName = ChatColor.of("#9966ff") + "" + ChatColor.BOLD + "Corrupted Netherite " + pieceName;
        return item.getItemMeta().getDisplayName().equals(expectedName);
    }

    private void updatePlayerHealth(Player player) {
        UUID uuid = player.getUniqueId();
        boolean hasFullSet = hasFullSet(player);

        if (hasFullSet) {
            if (!fullSetPlayers.contains(uuid)) {
                if (!originalMaxHealth.containsKey(uuid)) {
                    originalMaxHealth.put(uuid, player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
                }
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(
                        originalMaxHealth.get(uuid) + BONUS_HEARTS
                );
                fullSetPlayers.add(uuid);
            }
        } else {
            if (fullSetPlayers.contains(uuid)) {
                if (originalMaxHealth.containsKey(uuid)) {
                    double originalHealth = originalMaxHealth.get(uuid);
                    player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(originalHealth);

                    if (player.getHealth() > originalHealth) {
                        player.setHealth(originalHealth);
                    }
                }
                fullSetPlayers.remove(uuid);
            }
        }
    }

    @EventHandler
    public void onPlayerTick(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        checkArmorSet(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        checkArmorSet(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
    }

    @EventHandler
    public void onItemBreak(PlayerItemBreakEvent event) {
        checkArmorSet(event.getPlayer());
    }

    private void checkArmorSet(Player player) {
        if (!checkedPlayers.contains(player.getUniqueId())) {
            checkedPlayers.add(player.getUniqueId());
            new BukkitRunnable() {
                @Override
                public void run() {
                    updatePlayerHealth(player);
                    checkedPlayers.remove(player.getUniqueId());
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    public void forceUpdate(Player player) {
        updatePlayerHealth(player);
    }
}