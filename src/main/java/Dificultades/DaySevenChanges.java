package Dificultades;

import Dificultades.CustomMobs.CorruptedSkeleton;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import vct.hardcore3.DayHandler;

import java.util.Random;

public class DaySevenChanges implements Listener {

    private final JavaPlugin plugin;
    private boolean isApplied = false;
    private final Random random = new Random();
    private final CorruptedSkeleton corruptedSkeleton;
    private final DayHandler dayHandler;

    public DaySevenChanges(JavaPlugin plugin, DayHandler handler) {
        this.plugin = plugin;
        this.corruptedSkeleton = new CorruptedSkeleton(plugin, handler);
        this.dayHandler = handler;
    }

    public void apply() {
        if (!isApplied) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            corruptedSkeleton.apply();
            isApplied = true;

            // Iniciar el chequeo periódico cada 5 segundos (100 ticks)
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!isApplied) {
                        this.cancel();
                        return;
                    }

                    // 🔍 Revisamos cerca de cada jugador
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        World world = player.getWorld();
                        Location playerLoc = player.getLocation();

                        // Obtener entidades cercanas en un radio de 30 bloques
                        world.getNearbyEntities(playerLoc, 30, 30, 30, entity ->
                                entity.getType() == EntityType.CHICKEN || entity.getType() == EntityType.PIG
                        ).forEach(entity -> {
                            Location loc = entity.getLocation();
                            entity.remove();
                            world.spawnEntity(loc, EntityType.RAVAGER);
                        });
                    }
                }
            }.runTaskTimer(plugin, 0L, 100L); // Repite cada 5 segundos (100 ticks)
        }
    }

    public void revert() {
        if (isApplied) {
            corruptedSkeleton.revert();
            CreatureSpawnEvent.getHandlerList().unregister(this);
            EntitySpawnEvent.getHandlerList().unregister(this);
            EntityDamageByEntityEvent.getHandlerList().unregister(this);
            ProjectileLaunchEvent.getHandlerList().unregister(this);
            isApplied = false;
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isApplied) return;

        Entity entity = event.getEntity();

        // 1 de cada 8 murciélagos son Blazes
        if (entity.getType() == EntityType.BAT) {
            if (random.nextInt(8) == 0) {
                Location loc = entity.getLocation();
                entity.remove();
                entity.getWorld().spawnEntity(loc, EntityType.BLAZE);
            }
        }

        // Todos los Creepers son eléctricos
        if (entity.getType() == EntityType.CREEPER) {
            ((Creeper) entity).setPowered(true);
        }

        // Todos los Pollos y Cerdos son Ravagers
        if (entity.getType() == EntityType.CHICKEN || entity.getType() == EntityType.PIG) {
            Location loc = entity.getLocation();
            entity.remove();
            entity.getWorld().spawnEntity(loc, EntityType.RAVAGER);
        }

        // Verificar si el mob ya es corrupto (skeleton)
        if (event.getEntity().getPersistentDataContainer().has(corruptedSkeleton.getCorruptedKey(), PersistentDataType.BYTE)) {
            return; // Si ya es corrupto, no hacer nada
        }

        int currentDay = dayHandler.getCurrentDay();
        int skeletonProbability = 25;

        if (currentDay >= 7) {
            skeletonProbability = 2;
        }

        if (event.getEntityType() == EntityType.SKELETON) {
            if (random.nextInt(skeletonProbability) == 0) {
                Skeleton skeleton = (Skeleton) event.getEntity();
                corruptedSkeleton.spawnCorruptedSkeleton(skeleton.getLocation(), null);
                skeleton.remove();
            }
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!isApplied) return;

        Entity entity = event.getEntity();

        // Todos los Piglins y Piglins zombificados tienen armadura de diamante
        if (entity.getType() == EntityType.PIGLIN || entity.getType() == EntityType.ZOMBIFIED_PIGLIN) {
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                livingEntity.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                livingEntity.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                livingEntity.getEquipment().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                livingEntity.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            }
        }

        // Los Phantoms tendrán x2 de vida y Fuerza II
        if (entity.getType() == EntityType.PHANTOM) {
            if (entity instanceof Phantom) {
                Phantom phantom = (Phantom) entity;
                phantom.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40.0); // Doble de la salud original
                phantom.setHealth(40.0);
                phantom.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 2)); // Fuerza III
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!isApplied) return;

        if (event.getDamager() instanceof Phantom && event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 9)); // Levitación X por 2 segundos (40 ticks)
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!isApplied) return;

        if (event.getEntity() instanceof Fireball) {
            Fireball fireball = (Fireball) event.getEntity();
            if (fireball.getShooter() instanceof Ghast) {
                fireball.setYield(4.0f); // Poder de explosión nivel 4
            }
        }
    }
}
