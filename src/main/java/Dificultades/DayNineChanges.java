package Dificultades;

import Handlers.DayHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DayNineChanges implements Listener {
    private final DayHandler dayHandler;
    private final JavaPlugin plugin;
    private boolean isApplied = false;
    private final Random random = new Random();

    // Lista de mobs considerados "Raiders"
    private static final List<EntityType> RAIDERS = Arrays.asList(
            EntityType.PILLAGER,
            EntityType.VINDICATOR,
            EntityType.EVOKER,
            EntityType.RAVAGER,
            EntityType.WITCH,
            EntityType.ILLUSIONER
    );

    public DayNineChanges(JavaPlugin plugin, DayHandler handler) {
        this.plugin = plugin;
        this.dayHandler = handler;
    }

    public void apply() {
        if (!isApplied) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            isApplied = true;
        }
    }

    public void revert() {
        if (isApplied) {
            isApplied = false;
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!isApplied) return;

        // Evokers sueltan tótems con 75% de probabilidad (reemplazando su drop normal)
        if (event.getEntityType() == EntityType.EVOKER) {
            event.getDrops().removeIf(item -> item.getType() == Material.TOTEM_OF_UNDYING);
            if (random.nextDouble() < 0.75) {
                event.getDrops().add(new ItemStack(Material.TOTEM_OF_UNDYING));
            }
        }

        // Ravagers sueltan tótems con 15% de probabilidad (sin eliminar otros drops)
        if (event.getEntityType() == EntityType.RAVAGER) {
            if (random.nextDouble() < 0.20) {
                event.getDrops().add(new ItemStack(Material.TOTEM_OF_UNDYING));
            }
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isApplied) return;

        Entity entity = event.getEntity();


        // 2. Modificaciones para Raiders
        if (RAIDERS.contains(entity.getType())) {
            LivingEntity raider = (LivingEntity) entity;

            // Resistencia al fuego infinita para todos los Raiders
            raider.addPotionEffect(new PotionEffect(
                    PotionEffectType.FIRE_RESISTANCE,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    false
            ));

            // Pillagers: Ballestas con Quick Charge II
            if (entity.getType() == EntityType.PILLAGER) {
                Pillager pillager = (Pillager) entity;
                ItemStack crossbow = new ItemStack(Material.CROSSBOW);
                crossbow.addEnchantment(Enchantment.QUICK_CHARGE, 2);
                pillager.getEquipment().setItemInMainHand(crossbow);
            }

            // Vindicators: Fuerza II
            if (entity.getType() == EntityType.VINDICATOR) {
                raider.addPotionEffect(new PotionEffect(
                        PotionEffectType.STRENGTH,
                        Integer.MAX_VALUE,
                        1,  // Fuerza II
                        false,
                        false
                ));
            }

            // Ravagers: Fuerza II, Speed I y Resistencia I
            if (entity.getType() == EntityType.RAVAGER) {
                raider.addPotionEffect(new PotionEffect(
                        PotionEffectType.STRENGTH,
                        Integer.MAX_VALUE,
                        1,  // Fuerza II
                        false,
                        false
                ));
                raider.addPotionEffect(new PotionEffect(
                        PotionEffectType.SPEED,
                        Integer.MAX_VALUE,
                        0,  // Speed I
                        false,
                        false
                ));
                raider.addPotionEffect(new PotionEffect(
                        PotionEffectType.RESISTANCE,
                        Integer.MAX_VALUE,
                        0,  // Resistencia I
                        false,
                        false
                ));
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!isApplied) return;

        // 1. Congelación hace 3x de daño
        if (event.getCause() == EntityDamageEvent.DamageCause.FREEZE) {
            event.setDamage(event.getDamage() * 3);
        }

        // 2. Prevenir daño por caída a todos los Raiders
        if (RAIDERS.contains(event.getEntityType())) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                event.setCancelled(true);
            }
        }
    }
}