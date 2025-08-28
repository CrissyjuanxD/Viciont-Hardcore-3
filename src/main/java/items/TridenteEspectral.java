package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.UUID;

public class TridenteEspectral implements Listener {

    private final JavaPlugin plugin;

    private static final ChatColor PRIMARY_COLOR = ChatColor.of("#ffe599");
    private static final ChatColor SECONDARY_COLOR = ChatColor.of("#5d24ac");
    private static final ChatColor LORE_COLOR = ChatColor.of("#f6b26b");

    private final NamespacedKey spectralTridentKey;

    public TridenteEspectral(JavaPlugin plugin) {
        this.plugin = plugin;
        this.spectralTridentKey = new NamespacedKey(plugin, "spectral_trident");
    }

    public ItemStack createSpectralTrident() {
        ItemStack item = new ItemStack(Material.TRIDENT);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(PRIMARY_COLOR + "" + ChatColor.BOLD + "Tridente Espectral");
        meta.setLore(Arrays.asList(
                "",
                LORE_COLOR + "Este tridente se puede usar durante",
                LORE_COLOR + "una DeathStormm, tormenta o lluvia.",
                "",
                SECONDARY_COLOR + "» Daño cuerpo a cuerpo: " + ChatColor.WHITE + "7",
                SECONDARY_COLOR + "» Daño al lanzar: " + ChatColor.WHITE + "12",
                SECONDARY_COLOR + "» Velocidad de ataque: " + ChatColor.WHITE + "+1.0"
        ));

        // Atributos mejorados (+2 sobre netherite)
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(
                UUID.randomUUID(),
                "attack_damage",
                7,  // Netherite: 8, Enderite: 10
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HAND
        ));

        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(
                UUID.randomUUID(),
                "attack_speed",
                -1.6,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HAND
        ));

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        meta.getPersistentDataContainer().set(
                spectralTridentKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        meta.setCustomModelData(2);
        item.setItemMeta(meta);
        return item;
    }


    @EventHandler
    public void onTridentHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident trident) ||
                !(trident.getShooter() instanceof LivingEntity shooter)) {
            return;
        }

        ItemStack tridentItem = trident.getItem();
        if (tridentItem == null || !tridentItem.hasItemMeta()) return;

        ItemMeta meta = tridentItem.getItemMeta();

        boolean isSpectralTrident =
                meta.getPersistentDataContainer().has(spectralTridentKey, PersistentDataType.BYTE) ||
                        (meta.hasCustomModelData() && meta.getCustomModelData() == 2) ||
                        meta.getDisplayName().contains("Tridente Espectral");

        if (isSpectralTrident) {
            if (event.getHitEntity() instanceof LivingEntity target) {
                target.damage(4.0, shooter);

                target.getWorld().playSound(
                        target.getLocation(),
                        Sound.ITEM_TRIDENT_HIT,
                        1.0f,
                        1.6f
                );

                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 10);
            }
        }
    }

    // Método para verificar si un item es el tridente espectral
    public boolean isSpectralTrident(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(spectralTridentKey, PersistentDataType.BYTE) ||
                (meta.hasCustomModelData() && meta.getCustomModelData() == 2);
    }

    public NamespacedKey getSpectralTridentKey() {
        return spectralTridentKey;
    }
}