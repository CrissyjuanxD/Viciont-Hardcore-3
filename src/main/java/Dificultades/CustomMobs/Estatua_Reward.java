package Dificultades.CustomMobs;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Estatua_Reward {

    public static final String STATUE_NAME = ChatColor.of("#FFB347") + "" + ChatColor.BOLD + "Estatua de Recompensas";

    public static void spawn(Location loc) {
        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);

        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setCollidable(false);
        stand.setSilent(true);

        stand.setBasePlate(false);
        stand.setArms(true);

        stand.setCustomName(STATUE_NAME);
        stand.setCustomNameVisible(false);

        stand.addScoreboardTag("reward_statue");
    }
}