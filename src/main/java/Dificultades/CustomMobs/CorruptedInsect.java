package Dificultades.CustomMobs;

import Dificultades.Features.MobSoundManager;
import net.md_5.bungee.api.ChatColor;
import Handlers.DayHandler;
import com.viciontmedia.api.ViciontMediaAPI;
import items.CorruptedMobItems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import vct.hardcore3.ViciontHardcore3;

import java.util.*;

public class CorruptedInsect implements Listener, Gui.GuiMobProvider {
    private final JavaPlugin plugin;
    private final DayHandler dayHandler;
    private final NamespacedKey corruptedKey;
    private static boolean eventsRegistered = false;

    // --- OPTIMIZACIÓN CENTRALIZADA ---
    private static final Set<UUID> activeInsects = new HashSet<>();
    private static final Map<UUID, Long> attackCooldowns = new HashMap<>();
    private static final Map<UUID, Long> playerMediaCooldowns = new HashMap<>();
    private static BukkitTask mainTask;
    // ---------------------------------

    public CorruptedInsect(JavaPlugin plugin, DayHandler dayHandler) {
        this.plugin = plugin;
        this.dayHandler = dayHandler;
        this.corruptedKey = new NamespacedKey(plugin, "corrupted_insect");

        MobSoundManager.register(corruptedKey, Sound.ENTITY_ENDERMITE_AMBIENT, Sound.ENTITY_ENDERMITE_STEP, 1.0f, 2.0f);

        ((ViciontHardcore3) plugin).getEntidadesGuiManager().registerMob(this);
    }

    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            eventsRegistered = true;
            scanExistingInsects();
            startCentralTask();

            ((ViciontHardcore3) plugin).getEntidadesGuiManager().unlockMob(getEntityId());
        }
    }

    public void revert() {
        if (eventsRegistered) {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntitiesByClass(Endermite.class)) {
                    if (isCorruptedInsect(entity)) {
                        entity.remove();
                    }
                }
            }
            if (mainTask != null && !mainTask.isCancelled()) {
                mainTask.cancel();
            }
            activeInsects.clear();
            attackCooldowns.clear();
            playerMediaCooldowns.clear();
            eventsRegistered = false;

            ((ViciontHardcore3) plugin).getEntidadesGuiManager().lockMob(getEntityId());
        }
    }

    // ==========================================
    // IMPLEMENTACIÓN DE GUIMOBPROVIDER
    // ==========================================
    @Override
    public String getEntityId() { return "corrupted_insect"; }

    @Override
    public String getEntityType() { return "minecraft:endermite"; }

    @Override
    public String getName() { return "Corrupted Insect"; }

    @Override
    public String getColor() { return "#AA00AA"; }

    @Override
    public int getScale() { return 35; }

    @Override
    public List<String> getDynamicAttributes() {
        int currentDay = dayHandler.getCurrentDay();

        double health = 24.0;
        double damage = 3.0;

/*        if (currentDay >= 9) {
            health += 10.0;
            damage += 3.0;
        }*/

        String cRojo = ChatColor.of("#F51916").toString();
        String cAzul =ChatColor.of("#54A1D1").toString();
        String cNaranja = ChatColor.of("#F29329").toString();
        String cVerde = ChatColor.of("#8AF58F").toString();
        String cAmarillo = ChatColor.of("#EEDB7E").toString();
        String cMoradoText = ChatColor.of("#BE7DE9").toString();
        String cBlanco = ChatColor.WHITE.toString();

        List<String> attributes = new ArrayList<>();
        attributes.add(cRojo + "❤ " + cMoradoText + "Vida" + cBlanco + ": " + health);
        attributes.add(cAzul + "🗡 " + cMoradoText + "Daño de Ataque" + cBlanco + ": " + damage);
        attributes.add(cNaranja + "⚠ " + cMoradoText + "Rango DE vision" + cBlanco + ": 32.0");
        attributes.add(cVerde + "⚡ " + cMoradoText + "Efectos" + cBlanco + ": Velocidad I");
        attributes.add(cAmarillo + "☠ " + cMoradoText + "Habilidad" + cBlanco + ": Toxina en Área");
        return attributes;
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(
                "Un parásito gigante mutado por la corrupción.",
                "",
                "Cada cierto tiempo, carga y libera una",
                "explosión de esporas venenosas a su alrededor.",
                "Si te alcanza, nublará tu visión por",
                "completo y te dejará gravemente envenenado."
        );
    }

    private void scanExistingInsects() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(Endermite.class)) {
                if (isCorruptedInsect(entity)) {
                    activeInsects.add(entity.getUniqueId());
                }
            }
        }
    }

    public Endermite spawnCorruptedInsect(Location location) {
        Endermite insect = (Endermite) location.getWorld().spawnEntity(location, EntityType.ENDERMITE);
        applyCorruptedAttributes(insect);

        activeInsects.add(insect.getUniqueId());
        startCentralTask();

        return insect;
    }

    public void transformToCorruptedInsect(Endermite insect) {
        applyCorruptedAttributes(insect);
        activeInsects.add(insect.getUniqueId());
        startCentralTask();
    }

    private void applyCorruptedAttributes(Endermite insect) {
        insect.setCustomName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Insect");
        insect.setCustomNameVisible(false);

        if (insect.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            insect.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(24.0);
            insect.setHealth(24.0);
        }

        if (insect.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            insect.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(3.0);
        }

        if (insect.getAttribute(Attribute.GENERIC_SCALE) != null) {
            insect.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(2.0);
        }

        if (insect.getAttribute(Attribute.GENERIC_FOLLOW_RANGE) != null) {
            insect.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(32);
        }
        insect.setSilent(true);
        insect.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0));

        insect.getPersistentDataContainer().set(corruptedKey, PersistentDataType.BYTE, (byte) 1);
    }

    // --- TAREA CENTRAL DE ATAQUES EN ÁREA ---
    private void startCentralTask() {
        if (mainTask != null && !mainTask.isCancelled()) return;

        mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeInsects.isEmpty()) return;

                long now = System.currentTimeMillis();
                Iterator<UUID> it = activeInsects.iterator();

                while (it.hasNext()) {
                    UUID id = it.next();
                    Entity entity = Bukkit.getEntity(id);

                    if (entity == null || !entity.isValid() || entity.isDead()) {
                        it.remove();
                        attackCooldowns.remove(id);
                        continue;
                    }

                    if (entity instanceof Endermite insect) {
                        long lastAttack = attackCooldowns.getOrDefault(id, 0L);
                        if (now - lastAttack >= 10000L) {

                            if (insect.getTarget() instanceof Player target) {
                                if (insect.getLocation().distanceSquared(target.getLocation()) <= 64) {
                                    attackCooldowns.put(id, now);
                                    triggerAoEAttack(insect);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void triggerAoEAttack(Endermite insect) {
        insect.getWorld().playSound(insect.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 2.0f);

        new BukkitRunnable() {
            double radius = 0.5;

            @Override
            public void run() {
                // Si el bicho muere durante la animación de la esfera, la cancelamos
                if (!insect.isValid() || insect.isDead()) {
                    cancel();
                    return;
                }

                Location center = insect.getLocation().add(0, 1.0, 0);

                if (radius > 5.0) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, 5.5, 5.5, 5.5)) {
                        if (e instanceof Player p && (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)) {
                            if (p.getLocation().distance(center) <= 5.5) {
                                applyPoisonAndMedia(p);
                            }
                        }
                    }
                    cancel();
                    return;
                }

                for (int i = 0; i < 40; i++) {
                    double phi = Math.random() * Math.PI * 2;
                    double costheta = Math.random() * 2 - 1;
                    double theta = Math.acos(costheta);

                    double x = radius * Math.sin(theta) * Math.cos(phi);
                    double y = radius * Math.sin(theta) * Math.sin(phi);
                    double z = radius * Math.cos(theta);

                    Location particleLoc = center.clone().add(x, y, z);
                    center.getWorld().spawnParticle(Particle.WITCH, particleLoc, 1, 0, 0, 0, 0);
                    center.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 1, 0, 0, 0, 0);
                }

                radius += 1.5;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void applyPoisonAndMedia(Player player) {
        UUID pid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (playerMediaCooldowns.containsKey(pid) && playerMediaCooldowns.get(pid) > now) {
            return;
        }
        playerMediaCooldowns.put(pid, now + 8000L);

        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 10, 0));

        new BukkitRunnable() {
            int runs = 0;

            @Override
            public void run() {
                if (runs >= 2 || !player.isOnline()) {
                    cancel();
                    return;
                }

                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0f, 2.0f);
                player.playSound(player.getLocation(),Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 2.0f);


                ViciontMediaAPI.sendMedia(
                        player,
                        "corrupted_insect.png",
                        "",
                        8000L,
                        -1,
                        "center",
                        100,
                        true,
                        false
                );

                runs++;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    // --- SONIDOS PERSONALIZADOS ---

    @EventHandler
    public void onCorruptedInsectHurt(EntityDamageEvent event) {
        if (event.getEntity() instanceof Endermite insect && isCorruptedInsect(insect)) {
            insect.getWorld().playSound(insect.getLocation(), Sound.ENTITY_ENDERMITE_HURT, SoundCategory.HOSTILE, 1.0f, 2.0f);
        }
    }

    @EventHandler
    public void onCorruptedInsectDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Endermite insect && isCorruptedInsect(insect)) {
            insect.getWorld().playSound(insect.getLocation(), Sound.ENTITY_ENDERMITE_DEATH, SoundCategory.HOSTILE, 1.0f, 2.0f);

            activeInsects.remove(insect.getUniqueId());
            attackCooldowns.remove(insect.getUniqueId());
        }
    }

    public NamespacedKey getCorruptedKey() {
        return corruptedKey;
    }

    public boolean isCorruptedInsect(Entity entity) {
        return entity instanceof Endermite && entity.getPersistentDataContainer().has(corruptedKey, PersistentDataType.BYTE);
    }
}