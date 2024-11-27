package Dificultades;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import vct.hardcore3.DayHandler;
import vct.hardcore3.ViciontHardcore3;
import org.bukkit.metadata.MetadataValue;

import java.util.*;

public class DayOneChanges implements Listener {
    private final JavaPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, Integer> zombieTasks = new HashMap<>();
    private final NamespacedKey corruptedKey;
    private boolean isApplied = false; // Para asegurarnos de que solo se apliquen los cambios una vez

    public DayOneChanges(JavaPlugin plugin) {
        this.plugin = plugin;
        this.corruptedKey = new NamespacedKey(plugin, "corrupted_zombie");
    }

    // Método para aplicar cambios
    public void apply() {
        if (!isApplied) {
            // eventos solo cuando se aplica
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registerCustomRecipe();
            isApplied = true;
        }
    }

    // Método para revertir cambios
    public void revert() {
        if (isApplied) {
            // Esto Remueve todos los zombies y spiders corruptos del mundo
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Zombie zombie && zombie.getPersistentDataContainer().has(corruptedKey, PersistentDataType.BYTE)) {
                        zombie.remove();
                    }
                    if (entity instanceof Spider spider && spider.getCustomName() != null &&
                            spider.getCustomName().equals(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Spider")) {
                        spider.remove();
                    }
                }
            }

            // Esto Remueve recetas personalizadas
            NamespacedKey key = new NamespacedKey(plugin, "corrupted_steak");
            Bukkit.removeRecipe(key);

            // Esto Cancela los runnables de zombies corruptos
            for (Integer taskId : zombieTasks.values()) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            zombieTasks.clear();

            isApplied = false;
        }
    }


    @EventHandler
    public void onSpiderSpawn(EntitySpawnEvent event) {
        if (isApplied && event.getEntityType() == EntityType.SPIDER) {
            if (random.nextInt(25) == 0) {
                Spider spider = (Spider) event.getEntity();
                spider.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Spider");
                spider.setCustomNameVisible(true);
                spider.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
                spider.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0));
            }
        }
    }

    @EventHandler
    public void onZombieSpawn(EntitySpawnEvent event) {
        if (isApplied && event.getEntityType() == EntityType.ZOMBIE && random.nextInt(2) == 0) {
            Zombie zombie = (Zombie) event.getEntity();
            zombie.getPersistentDataContainer().set(corruptedKey, PersistentDataType.BYTE, (byte) 1);
            zombie.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Zombie");
            zombie.setCustomNameVisible(true);
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
            startSnowballRunnable(zombie);
        }
    }

    public void startSnowballRunnable(Zombie zombie) {
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (zombie == null || zombie.isDead() || !zombie.getPersistentDataContainer().has(corruptedKey, PersistentDataType.BYTE)) {
                Integer zombieTaskId = zombieTasks.remove(zombie.getUniqueId());
                if (zombieTaskId != null) {
                    Bukkit.getScheduler().cancelTask(zombieTaskId);
                }
                return;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isPlayerInRange(zombie, player) && Math.random() < 0.3) {
                    lanzarSnowball(zombie, player);
                }
            }

        }, 0L, 40L).getTaskId();

        zombieTasks.put(zombie.getUniqueId(), taskId);
    }

    @EventHandler
    public void onZombieDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Zombie zombie && zombie.getPersistentDataContainer().has(corruptedKey, PersistentDataType.BYTE)) {
            Integer taskId = zombieTasks.remove(zombie.getUniqueId());
            if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private boolean isPlayerInRange(Zombie zombie, Player player) {
        if (!zombie.getWorld().equals(player.getWorld())) {
            return false;
        }
        // Calcula la distancia si están en el mismo mundo
        double distanceXZ = zombie.getLocation().distanceSquared(player.getLocation()) - Math.pow(zombie.getLocation().getY() - player.getLocation().getY(), 2);
        double distanceY = Math.abs(zombie.getLocation().getY() - player.getLocation().getY());
        return distanceXZ <= 7 * 7 && distanceY <= 7;
    }


    @EventHandler
    public void onZombieAttack(EntityDamageByEntityEvent event) {
        if (isApplied && event.getDamager() instanceof Zombie zombie && event.getEntity() instanceof Player) {
            if (zombie.getCustomName() != null && zombie.getCustomName().equals(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Zombie")) {
                event.setDamage(2);
            }
        }
    }

    private void lanzarSnowball(Zombie zombie, Player player) {
        Snowball snowball = zombie.launchProjectile(Snowball.class);
        Vector direction = player.getLocation().toVector().subtract(zombie.getLocation().toVector()).normalize().multiply(1.5);
        snowball.setVelocity(direction);
        snowball.setCustomName("Corrupted Zombie Snowball");
        snowball.setMetadata("knockback", new FixedMetadataValue(plugin, 2));
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (snowball.isValid()) {
                snowball.getWorld().spawnParticle(Particle.PORTAL, snowball.getLocation(), 10);
                snowball.getWorld().spawnParticle(Particle.SMOKE, snowball.getLocation(), 5, 0.2, 0.2, 0.2, 0.1);
            }
        }, 0L, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (snowball.isValid()) {
                snowball.getWorld().playSound(snowball.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0F, 2.0F);
            }
        }, 20L);
    }


    @EventHandler
    public void onSnowballHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Snowball snowball && event.getEntity() instanceof Player player) {
            if ("Corrupted Zombie Snowball".equals(snowball.getCustomName())) {
                event.setDamage(2); // Daño de bola de nieve
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 50, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0));

                // Knockback al jugador
                int knockbackLevel = snowball.getMetadata("knockback").stream().findFirst().map(MetadataValue::asInt).orElse(0);
                Vector knockback = player.getLocation().toVector().subtract(snowball.getLocation().toVector()).normalize().multiply(knockbackLevel);
                player.setVelocity(knockback);
            } else {
                event.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Snowball snowball) {
            if ("Corrupted Zombie Snowball".equals(snowball.getCustomName())) {
                if (event.getHitBlock() != null || event.getHitEntity() != null) {
                    Location impactLocation = snowball.getLocation();

                    // jugadores cercanos en un radio de 4x4
                    List<Player> nearbyPlayers = snowball.getWorld().getPlayers().stream()
                            .filter(player -> player.getLocation().distance(impactLocation) <= 4)
                            .toList();

                    // knockback a los jugadores cercanos
                    for (Player player : nearbyPlayers) {
                        Vector knockback = player.getLocation().toVector().subtract(impactLocation.toVector()).normalize().multiply(1);
                        player.setVelocity(knockback);
                    }

                    snowball.getWorld().spawnParticle(Particle.EXPLOSION, impactLocation, 8);
                    snowball.getWorld().playSound(impactLocation, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0F, 2.0F);
                }
            }
        }
    }


    @EventHandler
    public void onSpiderHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Spider && event.getEntity() instanceof Player) {
            Spider spider = (Spider) event.getDamager();
            Player player = (Player) event.getEntity();


            if (spider.getCustomName() != null && spider.getCustomName().equals(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Spider")) {
                // telaraña en los pies del jugador
                player.getLocation().getBlock().setType(Material.COBWEB);
            }
        }
    }

    public void registerCustomRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "corrupted_steak");

        if (Bukkit.getRecipe(key) != null) {
            return;
        }

        //item personalizado
        ItemStack customItem = new ItemStack(Material.COOKED_BEEF);
        ItemMeta meta = customItem.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_PURPLE + "Carne Corrupta");
        meta.setCustomModelData(2);
        customItem.setItemMeta(meta);

        //receta
        ShapedRecipe customRecipe = new ShapedRecipe(key, customItem);
        customRecipe.shape(" F ", " F ", " F ");
        customRecipe.setIngredient('F', Material.ROTTEN_FLESH);

        plugin.getServer().addRecipe(customRecipe);
    }


    @EventHandler
    public void onPlayerEat(PlayerItemConsumeEvent event) {
        if (isApplied && event.getItem().getType() == Material.COOKED_BEEF && event.getItem().getItemMeta().hasCustomModelData() && event.getItem().getItemMeta().getCustomModelData() == 2) {
            Player player = event.getPlayer();
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 300, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 60, 0));
        }
    }

    @EventHandler
    public void onPortalEnter(PlayerPortalEvent event) {
        if (isApplied) {
            DayHandler dayHandler = ((ViciontHardcore3) plugin).getDayHandler();
            if (dayHandler.getCurrentDay() < 4 && event.getCause() == PlayerPortalEvent.TeleportCause.NETHER_PORTAL) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "¡El Nether está cerrado hasta el día 4!");
            }
        }
    }

    @EventHandler
    public void onRaidTrigger(RaidTriggerEvent event) {
        if (isApplied) {
            DayHandler dayHandler = ((ViciontHardcore3) plugin).getDayHandler();
            if (dayHandler.getCurrentDay() < 2) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "¡Las Raids están deshabilitadas hasta el día 2!");
            }
        }
    }
}
