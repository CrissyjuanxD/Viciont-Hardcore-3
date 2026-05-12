package Dificultades.CustomMobs;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;

public class Null_Statue {

    public static final String STATUE_NAME = "Blackstone_Herobrine_Statue";

    public static void spawn(Location loc) {
        ArmorStand as = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);

        // Propiedades base para que no moleste pero se detecte
        as.setCustomName(STATUE_NAME);
        as.setCustomNameVisible(false);
        as.setGravity(false);
        as.setInvulnerable(true);
        as.setBasePlate(true);
        as.setArms(false);

        // Equipamiento visual (opcional, para que parezca una estatua)
        if (as.getEquipment() != null) {
            // Bloqueamos los slots para que no le roben la armadura
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                as.addEquipmentLock(slot, ArmorStand.LockType.REMOVING_OR_CHANGING);
            }
        }
    }
}