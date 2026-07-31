package Dificultades.CustomMobs;

import Dificultades.Features.MobSoundManager;
import net.md_5.bungee.api.ChatColor;
import Handlers.DayHandler;
import items.CorruptedMobItems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import vct.hardcore3.ViciontHardcore3;

import java.util.*;

public class CorruptedZombies implements Listener, Gui.GuiMobProvider {

    private final JavaPlugin plugin;
    private final DayHandler dayHandler;
    private final NamespacedKey corruptedKey;

    private static final Set<UUID> activeZombies = new HashSet<>();
    private static boolean eventsRegistered = false;
    private static BukkitTask mainTask;

    private final Random random = new Random();

    public CorruptedZombies(JavaPlugin plugin, DayHandler dayHandler) {
        this.plugin = plugin;
        this.dayHandler = dayHandler;
        this.corruptedKey = new NamespacedKey(plugin, "corrupted_zombie");
        MobSoundManager.register(corruptedKey, Sound.ENTITY_ZOMBIE_AMBIENT, Sound.ENTITY_ZOMBIE_STEP, 0.6f, 1.0f);

        ((ViciontHardcore3) plugin).getEntidadesGuiManager().registerMob(this);
    }

    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;

            scanExistingZombies();
            startCentralTask();

            ((ViciontHardcore3) plugin).getEntidadesGuiManager().unlockMob(getEntityId());
        }
    }

    public void revert() {
        if (eventsRegistered) {
            if (mainTask != null && !mainTask.isCancelled()) {
                mainTask.cancel();
            }

            for (UUID uuid : activeZombies) {
                Entity entity = Bukkit.getEntity(uuid);
                if (entity instanceof Zombie && entity.isValid()) {
                    entity.remove();
                }
            }
            activeZombies.clear();
            eventsRegistered = false;

            ((ViciontHardcore3) plugin).getEntidadesGuiManager().lockMob(getEntityId());
        }
    }

    // ==========================================
    // IMPLEMENTACIÓN DE GUIMOBPROVIDER
    // ==========================================
    @Override
    public String getEntityId() { return "corrupted_zombie"; }

    @Override
    public String getEntityType() { return "minecraft:zombie"; }

    @Override
    public String getName() { return "Corrupted Zombie"; }

    @Override
    public String getColor() { return "#AA00AA"; }

    @Override
    public int getScale() { return 24; }

    @Override
    public List<String> getDynamicAttributes() {
        int currentDay = dayHandler.getCurrentDay();

        double health = 20.0;
        double damage = 3.0;

        // Ejemplo de escalado: si es día 9 o más, mostramos los stats bufados
/*        if (currentDay >= 9) {
            health += 10.0;
            damage += 2.0;
        }*/

        String cRojo = ChatColor.of("#F51916").toString();
        String cAzul = ChatColor.of("#54A1D1").toString();
        String cNaranja = ChatColor.of("#F29329").toString();
        String cVerde = ChatColor.of("#8AF58F").toString();
        String cMoradoText = ChatColor.of("#BE7DE9").toString();
        String cBlanco = ChatColor.WHITE.toString();

        List<String> attributes = new ArrayList<>();
        attributes.add(cRojo + "❤ " + cMoradoText + "Vida" + cBlanco + ": " + health);
        attributes.add(cAzul + "🗡 " + cMoradoText + "Daño de Ataque" + cBlanco + ": " + damage);
        attributes.add(cNaranja + "⚠ " + cMoradoText + "Rango DE vision" + cBlanco + ": 32.0");
        attributes.add(cVerde + "⚡ " + cMoradoText + "Efectos" + cBlanco + ": Velocidad I - Resistencia al Fuego");
        return attributes;
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(
                "Un cadáver reanimado por la energía corrupta.",
                "Es increíblemente rápido y el fuego no le afecta.",
                "",
                "Tiene la capacidad de lanzar cargas de",
                "viento a distancia. Si logran impactarte,",
                "te infligirán Veneno y Debilidad temporal."
        );
    }

    private void scanExistingZombies() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(Zombie.class)) {
                if (isCorrupted((Zombie) entity)) {
                    activeZombies.add(entity.getUniqueId());
                }
            }
        }
    }

    private void startCentralTask() {
        if (mainTask != null && !mainTask.isCancelled()) return;

        mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeZombies.isEmpty()) return;

                Iterator<UUID> it = activeZombies.iterator();
                while (it.hasNext()) {
                    UUID id = it.next();
                    Entity entity = Bukkit.getEntity(id);

                    if (entity == null || !entity.isValid() || entity.isDead()) {
                        if (entity != null && !entity.isValid()) it.remove();
                        continue;
                    }

                    if (entity instanceof Zombie zombie) {
                        processZombieAI(zombie);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void processZombieAI(Zombie zombie) {
        if (zombie.getTarget() instanceof Player player) {
            if (isPlayerInRange(zombie, player) && random.nextDouble() < 0.2) {
                lanzarSnowball(zombie, player);
            }
        }
    }

    public Zombie spawnCorruptedZombie(Location location) {
        Zombie CorruptedZombie = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        applyCorruptedZombieAttributes(CorruptedZombie);

        // Registrar en lista estática
        activeZombies.add(CorruptedZombie.getUniqueId());
        startCentralTask();

        return CorruptedZombie;
    }

    public void transformToCorruptedZombie(Zombie zombie) {
        applyCorruptedZombieAttributes(zombie);
        activeZombies.add(zombie.getUniqueId());
        startCentralTask();
    }

    private void applyCorruptedZombieAttributes(Zombie zombie) {
        zombie.getPersistentDataContainer().set(corruptedKey, PersistentDataType.BYTE, (byte) 1);
        zombie.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Zombie");
        zombie.setCustomNameVisible(false);
        zombie.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(32);
        zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(3.0);
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0));
        zombie.setSilent(true);

        if (zombie.getVehicle() instanceof Chicken) {
            zombie.getVehicle().remove();
        }
    }

    private boolean isCorrupted(Zombie zombie) {
        return zombie.getPersistentDataContainer().has(corruptedKey, PersistentDataType.BYTE);
    }

    public NamespacedKey getCorruptedKey() {
        return corruptedKey;
    }

    private boolean isPlayerInRange(Zombie zombie, Player player) {
        if (!zombie.getWorld().equals(player.getWorld())) return false;
        double distanceXZ = zombie.getLocation().distanceSquared(player.getLocation()) - Math.pow(zombie.getLocation().getY() - player.getLocation().getY(), 2);
        double distanceY = Math.abs(zombie.getLocation().getY() - player.getLocation().getY());
        return distanceXZ <= 15 * 15 && distanceY <= 15;
    }

    private void lanzarSnowball(Zombie zombie, Player player) {
        WindCharge snowball = zombie.launchProjectile(WindCharge.class);

        Vector direction = player.getLocation().toVector().subtract(zombie.getLocation().toVector());
        if (direction.lengthSquared() == 0) direction = new Vector(0, 0.1, 0);
        else direction = direction.normalize().multiply(1.5);
        direction.setY(direction.getY() - 0.3);

        snowball.setVelocity(direction);

        // AQUÍ ESTÁ EL ARREGLO:
        // En lugar de ponerle un nombre que el cliente pueda ver, le inyectamos tu etiqueta interna.
        snowball.getPersistentDataContainer().set(corruptedKey, PersistentDataType.BYTE, (byte) 1);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (snowball.isValid()) {
                snowball.getWorld().spawnParticle(Particle.PORTAL, snowball.getLocation(), 10);
                snowball.getWorld().spawnParticle(Particle.SMOKE, snowball.getLocation(), 5, 0.2, 0.2, 0.2, 0.1);
            }
        }, 0L, 1L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (snowball.isValid()) {
                snowball.getWorld().playSound(snowball.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0F, 2.0F);
                snowball.getWorld().playSound(snowball.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0F, 0.8F);
            }
        }, 20L);
    }

    @EventHandler
    public void onSnowballHit(EntityDamageByEntityEvent event) {
        // AQUÍ ESTÁ EL ARREGLO:
        // Verificamos si el proyectil tiene tu etiqueta interna en lugar de comparar su nombre.
        if (event.getDamager() instanceof WindCharge snowball &&
                snowball.getPersistentDataContainer().has(corruptedKey, PersistentDataType.BYTE)) {

            if (event.getEntity() instanceof Player player) {
                event.setDamage(2);
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 50, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0));
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onCorruptedZombieBurn(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (isCorrupted(zombie)) event.setCancelled(true);
    }

    @EventHandler
    public void onZombieHurt(EntityDamageEvent event) {
        if (event.getEntity() instanceof Zombie zombie && isCorrupted(zombie)) {
            zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_ZOMBIE_HURT, SoundCategory.HOSTILE, 1.0f, 0.6f);
        }
    }

    @EventHandler
    public void onZombieDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Zombie zombie && isCorrupted(zombie)) {
            zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_ZOMBIE_DEATH, SoundCategory.HOSTILE, 1.0f, 0.6f);

            // Limpieza inmediata
            activeZombies.remove(zombie.getUniqueId());

            if (Math.random() <= 0.30) {
                zombie.getWorld().dropItemNaturally(zombie.getLocation(), CorruptedMobItems.createCorruptedMeet());
            }
        }
    }

/*    EJEMPLO DE ENTIDADES EN LA GUI POR SI CAMBIA ATRIBUTOS DE DIA
    @Override
    public List<String> getDynamicAttributes() {
        // Obtenemos el día en tiempo real desde la instancia principal
        int currentDay = ((vct.hardcore3.ViciontHardcore3) plugin).getDayHandler().getCurrentDay();

        // Atributos base
        double health = 20.0;
        double damage = 3.0;

        // Multiplicadores según el día transcurrido
        if (currentDay >= 9) {
            health += 10.0; // Sube la vida en el día 9
            damage += 2.0;  // Sube el daño en el día 9
        }

        List<String> attributes = new ArrayList<>();
        attributes.add("❤ Vida: " + health);
        attributes.add("🗡 Daño de Ataque: " + damage);
        attributes.add("⚠ Rango: 32.0");

        // Podemos añadir atributos únicos a este zombie si quisiéramos:
        if (currentDay >= 9) {
            attributes.add("⚡ Velocidad: +15%");
        }

        return attributes;
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(
                "Este mob tiene la capacidad de lanzar",
                "wind charges.",
                "",
                "Al impactar con una entidad da",
                "Veneno I y Debilidad I."
        );
    }*/
}