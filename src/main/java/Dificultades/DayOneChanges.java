package Dificultades;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import vct.hardcore3.DayHandler;
import vct.hardcore3.ViciontHardcore3;

import java.util.*;
import java.util.stream.Collectors;

public class DayOneChanges implements Listener {
    private final JavaPlugin plugin;
    private final Random random = new Random();

    public DayOneChanges(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void apply() {
        // Registra el evento para los mobs y portales
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void revert() {
        // Para revertir cambios, puedes añadir lógica para manejar el comportamiento de mobs aquí
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

    // Mapa para rastrear los runnables de cada Corrupted Zombie
    private Map<UUID, Integer> zombieTasks = new HashMap<>();

    @EventHandler
    public void onZombieSpawn(EntitySpawnEvent event) {
        if (event.getEntityType() == EntityType.ZOMBIE) {
            if (random.nextInt(25) == 0) { // 1 de cada 25
                Zombie zombie = (Zombie) event.getEntity(); // Cast a Zombie
                zombie.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Zombie"); // Nombre personalizado
                zombie.setCustomNameVisible(true); // Hacer que el nombre sea visible
                // Añadir efectos
                zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));

                // Comenzar a lanzar bolas de nieve cada cierto tiempo
                startSnowballRunnable(zombie);
            }
        }
    }

    // Método para iniciar un runnable que lanza bolas de nieve
    private void startSnowballRunnable(Zombie zombie) {
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isPlayerInRange(zombie, player)) {
                    if (Math.random() < 0.3) { // 30% de probabilidad
                        lanzarSnowball(zombie, player);
                    }
                }
            }
        }, 0L, 40L).getTaskId(); // Ejecutar cada 40 ticks (2 segundos)

        // Guardar el task ID para este zombie
        zombieTasks.put(zombie.getUniqueId(), taskId);
    }

    // Método para detener el runnable cuando el zombie muere
    @EventHandler
    public void onZombieDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Zombie) {
            Zombie zombie = (Zombie) event.getEntity();

            // Verificar si era un Corrupted Zombie
            if (zombie.getCustomName() != null && zombie.getCustomName().equals(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Zombie")) {
                // Cancelar el runnable asociado
                if (zombieTasks.containsKey(zombie.getUniqueId())) {
                    int taskId = zombieTasks.get(zombie.getUniqueId());
                    Bukkit.getScheduler().cancelTask(taskId);
                    zombieTasks.remove(zombie.getUniqueId()); // Eliminar el task ID de la lista
                }
            }
        }
    }

    private boolean isPlayerInRange(Zombie zombie, Player player) {
        double distanceXZ = zombie.getLocation().distanceSquared(player.getLocation()) - Math.pow(zombie.getLocation().getY() - player.getLocation().getY(), 2);
        double distanceY = Math.abs(zombie.getLocation().getY() - player.getLocation().getY());
        return distanceXZ <= 7 * 7 && distanceY <= 7; // 7 bloques en el plano y altura máxima de 7
    }

    @EventHandler
    public void onZombieAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Zombie && event.getEntity() instanceof Player) {
            Zombie zombie = (Zombie) event.getDamager();
            Player player = (Player) event.getEntity();

            // Verifica si es un "Corrupted Zombie"
            if (zombie.getCustomName() != null && zombie.getCustomName().equals(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Zombie")) {
                // Daño melee
                event.setDamage(2);
            }
        }
    }

    private void lanzarSnowball(Zombie zombie, Player player) {
        Snowball snowball = zombie.launchProjectile(Snowball.class);

        // Dirección y velocidad
        Vector direction = player.getLocation().toVector().subtract(zombie.getLocation().toVector()).normalize();
        direction.multiply(1.5); // Velocidad de la bola de nieve
        snowball.setVelocity(direction);

        snowball.setCustomName("Corrupted Zombie Snowball");

        // Añade Knockback 3
        snowball.setMetadata("knockback", new FixedMetadataValue(plugin, 3));

        // Añade partículas moradas a la bola de nieve
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (snowball.isValid()) {
                // Partículas moradas alrededor de la bola de nieve
                snowball.getWorld().spawnParticle(Particle.PORTAL, snowball.getLocation(), 10); // Color morado

                // Partículas grises para el rastro
                snowball.getWorld().spawnParticle(Particle.SMOKE, snowball.getLocation(), 5, 0.2, 0.2, 0.2, 0.1); // Rastro gris
            }
        }, 0L, 1L); // Ejecuta cada tick para que sigan las partículas

        // Añade comportamiento para cuando la bola impacta
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (snowball.isValid()) {
                // Sonido al impactar
                snowball.getWorld().playSound(snowball.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0F, 2.0F);
            }
        }, 20L); // Tiempo para asegurarse de que la bola esté en vuelo
    }


    @EventHandler
    public void onSnowballHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Snowball) {
            Snowball snowball = (Snowball) event.getDamager();

            // Verifica si es la bola de nieve del Corrupted Zombie
            if (snowball.getCustomName() != null && snowball.getCustomName().equals("Corrupted Zombie Snowball")) {
                if (event.getEntity() instanceof Player) {
                    Player player = (Player) event.getEntity();

                    // Aplica 1 corazón de daño
                    event.setDamage(4);

                    // Aplica knockback al jugador
                    int knockbackLevel = snowball.hasMetadata("knockback") ? snowball.getMetadata("knockback").get(0).asInt() : 0;
                    Vector knockback = player.getLocation().toVector().subtract(snowball.getLocation().toVector()).normalize().multiply(knockbackLevel);
                    player.setVelocity(knockback);
                } else {
                    // Cancelar daño si es otra entidad que no sea un jugador
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Snowball) {
            Snowball snowball = (Snowball) event.getEntity();

            // Verifica si es la bola de nieve del Corrupted Zombie
            if (snowball.getCustomName() != null && snowball.getCustomName().equals("Corrupted Zombie Snowball")) {
                // Si impacta contra un bloque o cualquier entidad
                if (event.getHitBlock() != null || event.getHitEntity() != null) {
                    Location impactLocation = snowball.getLocation();

                    // Busca a jugadores cercanos en un radio de 4x4
                    List<Player> nearbyPlayers = snowball.getWorld().getPlayers().stream()
                            .filter(player -> player.getLocation().distance(impactLocation) <= 4) // 4 bloques de radio
                            .collect(Collectors.toList());

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
        ItemStack customItem = new ItemStack(Material.COOKED_BEEF); // El nuevo ítem que se va a crear
        ItemMeta meta = customItem.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_PURPLE + "Carne Corrupta"); // Nombre del ítem personalizado
        meta.setCustomModelData(2); // Custom Model Data para la textura
        customItem.setItemMeta(meta);

        // Definir la receta: 4 Rotten Flesh
        ShapedRecipe customRecipe = new ShapedRecipe(new NamespacedKey(plugin, "corrupted_steak"), customItem);
        customRecipe.shape(" F ", " F ", " F "); // 4 Rotten Flesh en forma de mesa de crafteo
        customRecipe.setIngredient('F', Material.ROTTEN_FLESH);

        // Registrar la receta
        plugin.getServer().addRecipe(customRecipe);
    }

    @EventHandler
    public void onPlayerEat(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() == Material.COOKED_BEEF && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 2) {
            // Aplicar los efectos al jugador cuando coma la "Carne Corrupta"
            Player player = event.getPlayer();
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 300, 0)); // Náuseas por 15 segundos
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 40, 0)); // Saturación por 2 segundos
        }
    }


    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        ViciontHardcore3 mainClass = (ViciontHardcore3) plugin;
        DayHandler dayHandler = mainClass.getDayHandler(); // Obtén el DayHandler desde la clase principal

        if (dayHandler != null && dayHandler.getCurrentDay() < 4) {
            event.setCancelled(true); // Cancela el evento de portal
            Player player = event.getPlayer();
            player.sendMessage(ChatColor.RED + "No puedes acceder al Nether hasta el día 4 de Viciont Hardcore 3.");
            Bukkit.getLogger().info("Nether bloqueado para " + player.getName() + " en día " + dayHandler.getCurrentDay());
        }
    }

    // Método para manejar la aparición de aldeas corruptas
    private void spawnCorruptedVillages() {
        // Implementar lógica para la aparición de Corrupted Villages en cualquier bioma con un 20% de probabilidad
        if (random.nextInt(100) < 20) { // 20% de probabilidad
            // Aquí debes agregar el código para generar la aldea corrupta usando un esquema de WorldEdit
        }
    }

    @EventHandler
    public void onRaidTrigger(RaidTriggerEvent event) {
        ViciontHardcore3 mainClass = (ViciontHardcore3) plugin;
        DayHandler dayHandler = mainClass.getDayHandler(); // Asegúrate de usar el DayHandler

        if (dayHandler != null && dayHandler.getCurrentDay() < 2) {
            event.setCancelled(true); // Cancela la Raid
            Player player = event.getPlayer();
            player.sendMessage(ChatColor.RED + "No se pueden hacer Raids hasta el día 2.");
        }
    }

}
