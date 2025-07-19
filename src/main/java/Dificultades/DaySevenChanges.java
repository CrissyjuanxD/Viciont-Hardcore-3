package Dificultades;

import Dificultades.CustomMobs.CorruptedCreeper;
import Dificultades.CustomMobs.CorruptedSkeleton;
import Dificultades.CustomMobs.InvertedGhast;
import Dificultades.CustomMobs.PiglinGlobo;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import Handlers.DayHandler;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DaySevenChanges implements Listener {

    private final JavaPlugin plugin;
    private boolean isApplied = false;
    private final Random random = new Random();
    private final CorruptedSkeleton corruptedSkeleton;
    private final CorruptedCreeper corruptedCreeper;
    private final InvertedGhast invertedGhast;
    private final PiglinGlobo piglinGlobo;
    private final DayHandler dayHandler;

    public DaySevenChanges(JavaPlugin plugin, DayHandler handler) {
        this.plugin = plugin;
        this.corruptedSkeleton = new CorruptedSkeleton(plugin, handler);
        this.corruptedCreeper = new CorruptedCreeper(plugin);
        this.invertedGhast = new InvertedGhast(plugin);
        this.piglinGlobo = new PiglinGlobo(plugin);
        this.dayHandler = handler;
    }

    public void apply() {
        if (!isApplied) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            corruptedSkeleton.apply();
            invertedGhast.apply();
            piglinGlobo.apply();
            isApplied = true;

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!isApplied) {
                        this.cancel();
                        return;
                    }

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        World world = player.getWorld();
                        Location playerLoc = player.getLocation();

                        world.getNearbyEntities(playerLoc, 60, 60, 60, entity ->
                                entity.getType() == EntityType.CHICKEN || entity.getType() == EntityType.PIG
                        ).forEach(entity -> {
                            Location loc = entity.getLocation();
                            entity.remove();
                            world.spawnEntity(loc, EntityType.RAVAGER);
                        });
                    }
                }
            }.runTaskTimer(plugin, 0L, 80L);
        }
    }

    public void revert() {
        if (isApplied) {
            corruptedSkeleton.revert();
            invertedGhast.revert();
            piglinGlobo.revert();
            HandlerList.unregisterAll(this);
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

        // Todos los Creepers son eléctricos, excepto los Corrupted Creepers
        if (entity.getType() == EntityType.CREEPER && !corruptedCreeper.isCorruptedCreeper(entity)) {
            ((Creeper) entity).setPowered(true);
        }

        // Todos los Pollos y Cerdos son Ravagers
        if (entity.getType() == EntityType.CHICKEN || entity.getType() == EntityType.PIG) {
            Location loc = entity.getLocation();
            entity.remove();
            entity.getWorld().spawnEntity(loc, EntityType.RAVAGER);
        }

        handleCorruptedSkeletonConversion(event);
        handleGhastTransformations(event);

    }

    private void handleCorruptedSkeletonConversion(CreatureSpawnEvent event) {
        if (event.getLocation().getWorld().getEnvironment() != World.Environment.NORMAL &&
                event.getLocation().getWorld().getEnvironment() != World.Environment.NETHER &&
                event.getLocation().getWorld().getEnvironment() != World.Environment.THE_END) {
            return;
        }

        Entity entity = event.getEntity();

        // Verificar si el mob ya es corrupto
        if (entity.getPersistentDataContainer().has(corruptedSkeleton.getCorruptedKey(), PersistentDataType.BYTE)) {
            return;
        }

        if (entity.getType() == EntityType.SKELETON) {
            int day = dayHandler.getCurrentDay();
            int chance;

            if (day >= 10) {
                chance = 4;
            } else if (day >= 7) {
                chance = 5;
            } else {
                return;
            }

            if (random.nextInt(chance) == 0) {
                Skeleton skeleton = (Skeleton) entity;
                corruptedSkeleton.spawnCorruptedSkeleton(skeleton.getLocation(), null);
                skeleton.remove();
            }
        }
    }

    private void handleGhastTransformations(CreatureSpawnEvent event) {
        if (event.getLocation().getWorld().getEnvironment() != World.Environment.NETHER) {
            return;
        }

        Entity entity = event.getEntity();

        if (entity.getType() != EntityType.GHAST || isAlreadyCustomGhast(entity)) {
            return;
        }

        int day = dayHandler.getCurrentDay();
        if (day >= 7 && random.nextInt(2) == 0) {
            Ghast ghast = (Ghast) entity;
            PersistentDataContainer pdc = ghast.getPersistentDataContainer();

            if (day >= 8 && random.nextBoolean()) {
                // Marcar como Piglin Globo
                pdc.set(piglinGlobo.getPiglinGloboKey(), PersistentDataType.BYTE, (byte)1);
                piglinGlobo.spawnPiglinGlobo(ghast.getLocation());
            } else {
                // Marcar como Inverted Ghast (día 7 o 50% de probabilidad día 8+)
                pdc.set(invertedGhast.getInvertedGhastKey(), PersistentDataType.BYTE, (byte)1);
                invertedGhast.spawnInvertedGhast(ghast.getLocation());
            }
            ghast.remove();
        }
    }

    private boolean isAlreadyCustomGhast(Entity ghast) {
        // Verificar ambas keys de mobs custom
        return ghast.getPersistentDataContainer().has(invertedGhast.getInvertedGhastKey(), PersistentDataType.BYTE) ||
                ghast.getPersistentDataContainer().has(piglinGlobo.getPiglinGloboKey(), PersistentDataType.BYTE);
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
                phantom.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40.0);
                phantom.setHealth(40.0);
                phantom.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 2)); // Fuerza III
            }
        }
    }

    //Si te pega un phantom Normal te revuelve el inventatio, no aplica para SpectralEye - SpectralEyeLv2 - DarkPhantom


    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!isApplied) return;

        if (event.getEntity() instanceof Fireball) {
            Fireball fireball = (Fireball) event.getEntity();
            if (fireball.getShooter() instanceof Ghast) {
                fireball.setYield(3.0f);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!isApplied) return;

        if (event.getEntityType() == EntityType.ZOMBIFIED_PIGLIN || event.getEntityType() == EntityType.PIGLIN) {
            // Lista de items que NO queremos que dropeen
            List<Material> bannedDrops = Arrays.asList(
                    Material.DIAMOND_HELMET,
                    Material.DIAMOND_CHESTPLATE,
                    Material.DIAMOND_LEGGINGS,
                    Material.DIAMOND_BOOTS
            );
            // Filtrar y eliminar los drops prohibidos
            event.getDrops().removeIf(item -> bannedDrops.contains(item.getType()));
        }
    }
}
