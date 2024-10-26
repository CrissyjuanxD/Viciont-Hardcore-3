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
import java.util.*;
import org.bukkit.metadata.MetadataValue;

public class DayOneChanges implements Listener {
    private final JavaPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, Integer> zombieTasks = new HashMap<>();
    // Etiqueta para identificar a los Corrupted Zombies
    private final NamespacedKey corruptedKey;

    public DayOneChanges(JavaPlugin plugin) {

        this.plugin = plugin;
        this.corruptedKey = new NamespacedKey(plugin, "corrupted_zombie"); // Ahora plugin ya está inicializado
    }

    public void apply() {
        // Registra el evento para los mobs y portales
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void revert() {
        // Para revertir cambios, añade la lógica aquí
    }

    @EventHandler
    public void onSpiderSpawn(EntitySpawnEvent event) {
        if (event.getEntityType() == EntityType.SPIDER) {
            if (random.nextInt(25) == 0) { // 1 de cada 25
                Spider spider = (Spider) event.getEntity(); // Cast a Spider
                spider.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Spider"); // Nombre personalizado
                spider.setCustomNameVisible(true); // Hacer que el nombre sea visible
                // Añadir efectos
                spider.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0)); // Velocidad I
                spider.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0)); // Fuerza I (cambia si está disponible)
            }
        }
    }


    // Evento de aparición de zombies
    @EventHandler
    public void onZombieSpawn(EntitySpawnEvent event) {
        if (event.getEntityType() == EntityType.ZOMBIE && random.nextInt(25) == 0) { // 1 de cada 25
            Zombie zombie = (Zombie) event.getEntity();
            zombie.getPersistentDataContainer().set(corruptedKey, PersistentDataType.BYTE, (byte) 1);

            zombie.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Zombie");
            zombie.setCustomNameVisible(true);
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));

            startSnowballRunnable(zombie);
        }
    }



    // Runnable para lanzar bolas de nieve
    public void startSnowballRunnable(Zombie zombie) {
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (zombie == null || zombie.isDead() || !zombie.getPersistentDataContainer().has(corruptedKey, PersistentDataType.BYTE)) {
                if (zombie != null) { // Verificar que zombie no sea null antes de llamar a getUniqueId
                    Integer zombieTaskId = zombieTasks.remove(zombie.getUniqueId());
                    if (zombieTaskId != null) {
                        Bukkit.getScheduler().cancelTask(zombieTaskId);
                    }
                }
                return;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isPlayerInRange(zombie, player) && Math.random() < 0.3) { // 30% probabilidad
                    lanzarSnowball(zombie, player);
                }
            }
        }, 0L, 40L).getTaskId(); // Cada 40 ticks (2 segundos)
        zombieTasks.put(zombie.getUniqueId(), taskId);
    }


    // Evento al morir un zombie para cancelar su runnable
    @EventHandler
    public void onZombieDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Zombie zombie && zombie.getPersistentDataContainer().has(corruptedKey, PersistentDataType.BYTE)) {
            Integer taskId = zombieTasks.remove(zombie.getUniqueId());
            if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private boolean isPlayerInRange(Zombie zombie, Player player) {
        double distanceXZ = zombie.getLocation().distanceSquared(player.getLocation()) - Math.pow(zombie.getLocation().getY() - player.getLocation().getY(), 2);
        double distanceY = Math.abs(zombie.getLocation().getY() - player.getLocation().getY());
        return distanceXZ <= 7 * 7 && distanceY <= 7; // 7 bloques en el plano y altura máxima de 7
    }

    @EventHandler
    public void onZombieAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Zombie zombie && event.getEntity() instanceof Player) {

            // Verifica si es un "Corrupted Zombie"
            if (zombie.getCustomName() != null && zombie.getCustomName().equals(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Zombie")) {
                // Daño melee
                event.setDamage(2);
            }
        }
    }



    // Lanzar bola de nieve personalizada con partículas y efectos
    private void lanzarSnowball(Zombie zombie, Player player) {
        Snowball snowball = zombie.launchProjectile(Snowball.class);

        // Dirección y velocidad
        Vector direction = player.getLocation().toVector().subtract(zombie.getLocation().toVector()).normalize().multiply(1.5);
        snowball.setVelocity(direction);
        snowball.setCustomName("Corrupted Zombie Snowball");

        // Añadir knockback y partículas moradas
        snowball.setMetadata("knockback", new FixedMetadataValue(plugin, 3));
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (snowball.isValid()) {
                snowball.getWorld().spawnParticle(Particle.PORTAL, snowball.getLocation(), 10);
                snowball.getWorld().spawnParticle(Particle.SMOKE, snowball.getLocation(), 5, 0.2, 0.2, 0.2, 0.1);
            }
        }, 0L, 1L);

        // Sonido al impactar
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
                event.setDamage(4); // Daño de bola de nieve
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 50, 0)); // Veneno I (2.5 seg)
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0)); // Debilidad I (5 seg)

                // Knockback al jugador
                int knockbackLevel = snowball.getMetadata("knockback").stream().findFirst().map(MetadataValue::asInt).orElse(0);
                Vector knockback = player.getLocation().toVector().subtract(snowball.getLocation().toVector()).normalize().multiply(knockbackLevel);
                player.setVelocity(knockback);
            } else {
                // Cancelar daño si es otra entidad que no sea un jugador
                event.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Snowball snowball) {
            // Verifica si es la bola de nieve del Corrupted Zombie
            if ("Corrupted Zombie Snowball".equals(snowball.getCustomName())) {
                // Si impacta contra un bloque o cualquier entidad
                if (event.getHitBlock() != null || event.getHitEntity() != null) {
                    Location impactLocation = snowball.getLocation();

                    // Busca a jugadores cercanos en un radio de 4x4
                    List<Player> nearbyPlayers = snowball.getWorld().getPlayers().stream()
                            .filter(player -> player.getLocation().distance(impactLocation) <= 4) // 4 bloques de radio
                            .toList(); // Cambiado a toList()

                    // Aplica knockback a los jugadores cercanos
                    for (Player player : nearbyPlayers) {
                        Vector knockback = player.getLocation().toVector().subtract(impactLocation.toVector()).normalize().multiply(2);
                        player.setVelocity(knockback);
                    }

                    // Partículas de impacto
                    snowball.getWorld().spawnParticle(Particle.EXPLOSION, impactLocation, 8); // Explosión de partículas

                    // Sonido de impacto
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

            // Verifica si es un "Corrupted Spider"
            if (spider.getCustomName() != null && spider.getCustomName().equals(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Spider")) {
                // Coloca una telaraña en los pies del jugador
                player.getLocation().getBlock().setType(Material.COBWEB);
            }
        }
    }

    public void registerCustomRecipe() {
        ItemStack customItem = new ItemStack(Material.COOKED_BEEF);
        ItemMeta meta = customItem.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_PURPLE + "Carne Corrupta");
        meta.setCustomModelData(2);
        customItem.setItemMeta(meta);

        ShapedRecipe customRecipe = new ShapedRecipe(new NamespacedKey(plugin, "corrupted_steak"), customItem);
        customRecipe.shape(" F ", " F ", " F ");
        customRecipe.setIngredient('F', Material.ROTTEN_FLESH);

        plugin.getServer().addRecipe(customRecipe);
    }

    @EventHandler
    public void onPlayerEat(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() == Material.COOKED_BEEF && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 2) {
            Player player = event.getPlayer();
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 300, 0));  // 15 segundos de náuseas
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 60, 0));  // 3 segundos de saturación
        }
    }

    @EventHandler
    public void onPortalEnter(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        DayHandler dayHandler = ((ViciontHardcore3) plugin).getDayHandler();
        if (dayHandler.getCurrentDay() < 4 && event.getCause() == PlayerPortalEvent.TeleportCause.NETHER_PORTAL) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "¡El Nether está cerrado hasta el día 4!");
        }
    }

    @EventHandler
    public void onRaidTrigger(RaidTriggerEvent event) {
        DayHandler dayHandler = ((ViciontHardcore3) plugin).getDayHandler();
        if (dayHandler.getCurrentDay() < 2) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "¡Las Raids están deshabilitadas hasta el día 2!");
        }
    }
}
