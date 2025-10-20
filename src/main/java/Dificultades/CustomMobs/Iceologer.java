package Dificultades.CustomMobs;

import items.ItemsTotems;
import items.IceBow.IceBowItem;
import items.IceBow.IceBowLogic;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;


import java.lang.reflect.Method;
import java.util.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;


public class Iceologer implements Listener {
    private final JavaPlugin plugin;
    private final Set<Evoker> activeIceologers = new HashSet<>();
    private final Set<Player> frozenPlayers = new HashSet<>();
    private boolean eventsRegistered = false;
    private final Random random = new Random();
    private final NamespacedKey iceologerKey;
    private final NamespacedKey iceAngelKey;
    private final NamespacedKey iceFangsKey;
    private final Set<UUID> blindnessApplied = new HashSet<>();
    private final Map<UUID, Long> playerBowCooldowns = new HashMap<>();

    // Instancias para el manejo del arco de hielo
    private final IceBowItem iceBowItem;
    private final IceBowLogic iceBowLogic;

    public Iceologer(JavaPlugin plugin) {
        this.plugin = plugin;
        this.iceologerKey = new NamespacedKey(plugin, "iceologer");
        this.iceAngelKey = new NamespacedKey(plugin, "ice_angel");
        this.iceFangsKey = new NamespacedKey(plugin, "ice_fangs");

        // Inicializar instancias del arco de hielo
        this.iceBowItem = new IceBowItem(plugin);
        this.iceBowLogic = new IceBowLogic(plugin, playerBowCooldowns);
        setupNMSListener();

    }

    public void apply() {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            Bukkit.getPluginManager().registerEvents(iceBowLogic, plugin);
            eventsRegistered = true;
        }
    }

    public void revert() {
        if (eventsRegistered) {
            for (Evoker iceologer : activeIceologers) {
                if (iceologer.isValid() && !iceologer.isDead()) {
                    iceologer.remove();
                }
            }
            activeIceologers.clear();
            frozenPlayers.clear();
            blindnessApplied.clear();
            eventsRegistered = false;
        }
    }

    public Evoker spawnIceologer(Location location) {
        Evoker iceologer = (Evoker) location.getWorld().spawnEntity(location, EntityType.EVOKER);
        iceologer.setCustomName(ChatColor.AQUA + "" + ChatColor.BOLD + "Iceologer");
        iceologer.setCustomNameVisible(true);
        iceologer.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(32);

        // Aumentar la vida del Iceologer (el doble de vida normal)
        iceologer.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(48.0); // Normal: 24.0
        iceologer.setHealth(48.0);

        iceologer.getPersistentDataContainer().set(iceologerKey, PersistentDataType.BYTE, (byte) 1);

        // Equipar el arco de hielo usando la nueva clase
        ItemStack iceBow = iceBowItem.createIceBow();
        iceologer.getEquipment().setItemInMainHand(iceBow);
        iceologer.getEquipment().setItemInMainHandDropChance(0.0f);

        activeIceologers.add(iceologer);
        monitorIceologer(iceologer);
        return iceologer;
    }

    public void monitorIceologer(Evoker iceologer) {
        new BukkitRunnable() {
            private int bowCooldown = 0;
            private int summonCooldown = 0;
            private int customFangsCooldown = 0;

            @Override
            public void run() {
                if (iceologer.isDead() || !iceologer.isValid()) {
                    cancel();
                    activeIceologers.remove(iceologer);
                    return;
                }

                if (iceologer.getTarget() instanceof Player player) {
                    if ((player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) &&
                            iceologer.hasLineOfSight(player)) {

                        double distance = iceologer.getLocation().distance(player.getLocation());

                        // Disparar arco cada 3-4 segundos si está a distancia media/larga
                        if (distance >= 8 && distance <= 20 && bowCooldown <= 0) {
                            iceBowLogic.shootIceBow(iceologer, player, iceologerKey);
                            bowCooldown = 60 + random.nextInt(20); // 3-4 segundos
                        }

                        //if (summonCooldown <= 0) {
                            //performIceAngelSummon(iceologer, player);
                            //summonCooldown = 400;
                        //}

                        if (customFangsCooldown <= 0) {
                            performCustomFangsAttack(iceologer, player);
                            customFangsCooldown = 160; // 8 segundos exactos
                        }

                        // Ataque especial cada 6 segundos
                        if (distance < 15 && iceologer.getTicksLived() % 120 == 0) {
                            performSpecialAttack(iceologer, player);
                        }

                        // Ataque de bloques de hielo cada 5 segundos
                        if (iceologer.getTicksLived() % 100 == 0) {
                            performIceBlockAttack(iceologer);
                        }
                    }
                }

                if (bowCooldown > 0) bowCooldown--;
                if (customFangsCooldown > 0) customFangsCooldown--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // Método para transformar Vex en Ángeles de Hielo
    private void transformToIceAngel(Vex vex) {
        vex.setCustomName(ChatColor.AQUA + "" + ChatColor.BOLD + "Angel de Hielo");
        vex.setCustomNameVisible(false);
        vex.getPersistentDataContainer().set(iceAngelKey, PersistentDataType.BYTE, (byte) 1);

        // Mejorar atributos
        vex.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
        vex.setHealth(20.0);

        // Efectos visuales
        vex.getWorld().spawnParticle(Particle.SNOWFLAKE,
                vex.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        vex.getWorld().playSound(vex.getLocation(),
                Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1.2f);
    }

    private void transformToIceFangs(EvokerFangs vex) {
        vex.setCustomName("Iceologer");
        vex.setCustomNameVisible(false);
        vex.getPersistentDataContainer().set(iceFangsKey, PersistentDataType.BYTE, (byte) 1);

        // Efectos visuales
        vex.getWorld().spawnParticle(Particle.SNOWFLAKE,
                vex.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        vex.getWorld().playSound(vex.getLocation(),
                Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1.2f);
    }

    // Implementación del ataque personalizado de invocación
    private void performIceAngelSummon(Evoker iceologer, Player target) {
        iceologer.getWorld().playSound(iceologer.getLocation(),
                Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 0.8f);


        // Partículas de invocación (cambiar a azul/hielo)
        iceologer.getWorld().spawnParticle(Particle.SNOWFLAKE,
                iceologer.getLocation().add(0, 2, 0), 50, 0.5, 0.5, 0.5, 0.2);

        // Spawnear entre 2 y 3 Ángeles de Hielo
        int angelsToSpawn = 1 + random.nextInt(2);
        Location spawnLocation = iceologer.getLocation().add(0, 1, 0);

        for (int i = 0; i < angelsToSpawn; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Vex iceAngel = (Vex) iceologer.getWorld().spawnEntity(spawnLocation, EntityType.VEX);
                transformToIceAngel(iceAngel);

                // Establecer el objetivo del ángel
                if (target != null && target.isValid()) {
                    iceAngel.setTarget(target);
                }
            }, 10L + (i * 5L)); // Espaciar las invocaciones
        }

        // Cooldown para el ataque
        iceologer.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "last_summon_time"),
                PersistentDataType.LONG,
                System.currentTimeMillis()
        );
    }

    // Interceptar el ataque normal de invocación de vexes
// Reemplaza tu método onVexSpawn con esta versión mejorada
    @EventHandler
    public void onVexSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.VEX) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPELL) return;

        Vex vex = (Vex) event.getEntity();

        // Buscar Iceologers activos en un radio
        for (Evoker iceologer : activeIceologers) {
            if (iceologer.getWorld().equals(vex.getWorld()) &&
                    iceologer.getLocation().distance(vex.getLocation()) <= 12) {

                // Cancelar el spawn del Vex vanilla
                event.setCancelled(true);

                // Spawnear nuestro Ángel de Hielo personalizado
                spawnCustomIceAngel(iceologer, vex.getLocation());
                break;
            }
        }
    }

    private void spawnCustomIceAngel(Evoker summoner, Location location) {
        // Crear un Vex personalizado
        Vex iceAngel = (Vex) summoner.getWorld().spawnEntity(location, EntityType.VEX);
        transformToIceAngel(iceAngel);

        try {
            // Obtener la entidad NMS del Vex
            Object nmsVex = getNMSEntity(iceAngel);
            Object nmsEvoker = getNMSEntity(summoner);

            if (nmsVex != null && nmsEvoker != null) {
                // Usar reflexión para establecer el propietario
                Class<?> vexClass = nmsVex.getClass();
                java.lang.reflect.Field ownerField = vexClass.getDeclaredField("b"); // Campo "owner" en NMS
                ownerField.setAccessible(true);
                ownerField.set(nmsVex, nmsEvoker);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("No se pudo establecer el summoner del Vex: " + e.getMessage());
        }

        // Establecer objetivo si el summoner tiene uno
        if (summoner.getTarget() != null) {
            iceAngel.setTarget(summoner.getTarget());
        }

        // Efectos de spawn
        iceAngel.getWorld().spawnParticle(Particle.SNOWFLAKE,
                location.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        iceAngel.getWorld().playSound(location,
                Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.8f);
    }

    // Método de utilidad para obtener la entidad NMS
    private Object getNMSEntity(org.bukkit.entity.Entity bukkitEntity) {
        try {
            return bukkitEntity.getClass().getMethod("getHandle").invoke(bukkitEntity);
        } catch (Exception e) {
            plugin.getLogger().warning("Error al obtener entidad NMS: " + e.getMessage());
            return null;
        }
    }

    private void setupNMSListener() {
        try {
            // Obtener las clases NMS necesarias
            Class<?> entityEvokerClass = NMSUtils.getNMSClass("EntityEvoker");
            Class<?> entityVexClass = NMSUtils.getNMSClass("EntityVex");
            Class<?> entityEvokerFangsClass = NMSUtils.getNMSClass("EntityEvokerFangs");

            if (entityEvokerClass != null && entityVexClass != null && entityEvokerFangsClass != null) {
                // Usar reflexión para interceptar ambos métodos
                interceptEvokerSummon();
                interceptEvokerFangs();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("No se pudo configurar NMS listener: " + e.getMessage());
            // Fallback al sistema de eventos de Bukkit
            setupBukkitFallback();
        }
    }

    private void interceptEvokerSummon() {
        try {
            Class<?> entityEvokerClass = NMSUtils.getNMSClass("EntityEvoker");
            java.lang.reflect.Method summonMethod = entityEvokerClass.getDeclaredMethod("s"); // Método summonVex en algunas versiones

            // Crear un proxy para el método
            java.lang.reflect.InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().equals("s") || method.getName().equals("summonVex")) {
                    Object evokerEntity = args[0];
                    Entity bukkitEvoker = getBukkitEntity(evokerEntity);

                    if (bukkitEvoker instanceof Evoker evoker && activeIceologers.contains(evoker)) {
                        // Cancelar la invocación normal y usar la nuestra
                        if (evoker.getTarget() instanceof Player player) {
                            performIceAngelSummon(evoker, player);
                        }
                        return null;
                    }
                }
                return method.invoke(proxy, args);
            };

            // Aplicar el proxy al método
            Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                    entityEvokerClass.getClassLoader(),
                    new Class<?>[]{entityEvokerClass},
                    handler
            );

        } catch (Exception e) {
            plugin.getLogger().warning("Error interceptando invocación de Vex: " + e.getMessage());
        }
    }

    private void interceptEvokerFangs() {
        try {
            Class<?> entityEvokerClass = NMSUtils.getNMSClass("EntityEvoker");

            // Buscar el método que crea EvokerFangs
            final java.lang.reflect.Method[] fangsMethodHolder = new java.lang.reflect.Method[1];
            java.lang.reflect.Method[] methods = entityEvokerClass.getDeclaredMethods();

            for (java.lang.reflect.Method method : methods) {
                if (method.getReturnType().getName().contains("EntityEvokerFangs") ||
                        method.getName().equals("summonFangs") ||
                        method.getName().equals("a") || method.getName().equals("b") || method.getName().equals("c")) {
                    fangsMethodHolder[0] = method;
                    break;
                }
            }

            if (fangsMethodHolder[0] != null) {
                java.lang.reflect.InvocationHandler handler = (proxy, method, args) -> {
                    if (method.getName().equals(fangsMethodHolder[0].getName())) {
                        Object evokerEntity = proxy;
                        Entity bukkitEvoker = getBukkitEntity(evokerEntity);

                        if (bukkitEvoker instanceof Evoker evoker && activeIceologers.contains(evoker)) {
                            return null;
                        }
                    }
                    return method.invoke(proxy, args);
                };

                Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                        entityEvokerClass.getClassLoader(),
                        new Class<?>[]{entityEvokerClass},
                        handler
                );
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error interceptando EvokerFangs: " + e.getMessage());
        }
    }

    private Entity getBukkitEntity(Object nmsEntity) {
        try {
            if (nmsEntity == null) return null;

            Class<?> entityClass = nmsEntity.getClass();
            // Buscar el método getBukkitEntity en la clase NMS
            java.lang.reflect.Method getBukkitEntity = null;

            for (java.lang.reflect.Method method : entityClass.getMethods()) {
                if (method.getName().equals("getBukkitEntity") &&
                        method.getReturnType().isAssignableFrom(Entity.class)) {
                    getBukkitEntity = method;
                    break;
                }
            }

            if (getBukkitEntity != null) {
                return (Entity) getBukkitEntity.invoke(nmsEntity);
            }

            // Fallback: intentar con el método de CraftEntity
            Class<?> craftEntityClass = NMSUtils.getCraftClass("entity.CraftEntity");
            if (craftEntityClass != null) {
                java.lang.reflect.Method getHandle = craftEntityClass.getMethod("getHandle");
                Object craftEntity = getHandle.invoke(nmsEntity);
                if (craftEntity instanceof Entity) {
                    return (Entity) craftEntity;
                }
            }

            return null;
        } catch (Exception e) {
            plugin.getLogger().warning("Error en getBukkitEntity: " + e.getMessage());
            return null;
        }
    }

    private void setupBukkitFallback() {
        // Sistema de respaldo usando eventos de Bukkit
        plugin.getLogger().info("Usando sistema de respaldo para detectar Vex");

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onCreatureSpawn(CreatureSpawnEvent event) {
                if (event.getEntity() instanceof Vex vex &&
                        event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPELL) {

                    handleVexSpawn(vex);
                }
                if (event.getEntity() instanceof EvokerFangs fangs) {
                    debugFangsSpawn(fangs, event.getSpawnReason());
                    handleFangsSpawn(fangs);
                }
            }
        }, plugin);
    }

    private void handleVexSpawn(Vex vex) {
        // Buscar Iceologers cercanos
        for (Evoker iceologer : activeIceologers) {
            if (iceologer.getWorld().equals(vex.getWorld()) &&
                    iceologer.getLocation().distance(vex.getLocation()) <= 15) {

                // Transformar el Vex después de un pequeño delay
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (vex.isValid() && !vex.isDead()) {
                        transformToIceAngel(vex);
                    }
                }, 2L);
                break;
            }
        }
    }

    private void handleFangsSpawn(EvokerFangs fangs) {
        // Buscar el Evoker dueño de estos fangs
        try {
            Object nmsFang = getNMSEntity(fangs);
            if (nmsFang != null) {
                Class<?> fangClass = nmsFang.getClass();
                Field ownerField = fangClass.getDeclaredField("owner");
                ownerField.setAccessible(true);
                Object owner = ownerField.get(nmsFang);

                if (owner != null) {
                    Entity bukkitOwner = getBukkitEntity(owner);
                    if (bukkitOwner instanceof Evoker evoker &&
                            activeIceologers.contains(evoker)) {

                        transformToIceFangs(fangs);
                    }
                }
            }
        } catch (Exception e) {
            // Fallback a búsqueda por proximidad
            for (Evoker iceologer : activeIceologers) {
                if (iceologer.getWorld().equals(fangs.getWorld()) &&
                        iceologer.getLocation().distance(fangs.getLocation()) <= 15) {

                    transformToIceFangs(fangs);
                    break;
                }
            }
        }
    }

    private void performCustomFangsAttack(Evoker iceologer, LivingEntity target) {
        // ✅ Cooldown más consistente usando ticks en lugar de milisegundos
        // (ya lo manejamos en el monitor con customFangsCooldown)

        Location startLocation = iceologer.getLocation();
        Location targetLocation = target.getLocation();

        // Sonido de preparación
        iceologer.getWorld().playSound(startLocation, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.5f, 0.7f);

        // Efectos visuales en el Iceologer
        iceologer.getWorld().spawnParticle(Particle.SNOWFLAKE,
                startLocation.add(0, 2, 0), 40, 0.7, 0.7, 0.7, 0.2);

        // Calcular dirección y distancia
        double distance = startLocation.distance(targetLocation);

        // ✅ AJUSTE: Si la distancia es muy grande, limitar el número de fangs
        int fangsCount;
        if (distance > 30) {
            // Para distancias largas, crear un ataque más concentrado
            fangsCount = 8 + random.nextInt(5);
        } else {
            fangsCount = (int) (distance * 0.8);
            fangsCount = Math.min(Math.max(fangsCount, 5), 15);
        }

        Vector direction = targetLocation.toVector().subtract(startLocation.toVector()).normalize();

        for (int i = 0; i < fangsCount; i++) {
            double progress = (i + 1) / (double) fangsCount;

            // ✅ MEJORA: Para distancias largas, hacer que los fangs aparezcan más cerca del jugador
            Location fangLocation;
            if (distance > 30) {
                // Aparecer más cerca del jugador para distancias largas
                double adjustedDistance = distance - 10 + (random.nextDouble() * 20);
                fangLocation = startLocation.clone().add(direction.clone().multiply(adjustedDistance * progress));
            } else {
                fangLocation = startLocation.clone().add(direction.clone().multiply(distance * progress));
            }

            // Ajustar altura - buscar terreno adecuado
            fangLocation.setY(findSafeHeight(fangLocation));

            // Delay para efecto de onda
            int delay = i * 2;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (iceologer.isValid() && !iceologer.isDead() && target.isValid()) {
                    spawnCustomFang(iceologer, fangLocation);
                }
            }, delay);
        }

        // ✅ EFECTO FINAL
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (target.isValid()) {
                target.getWorld().playSound(target.getLocation(),
                        Sound.ENTITY_PLAYER_HURT_FREEZE, 1.2f, 0.8f);
                target.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        target.getLocation().add(0, 1, 0), 30, 1.0, 1.0, 1.0, 0.3);
            }
        }, fangsCount * 2L);
    }

    // ✅ MÉTODO AUXILIAR para encontrar altura segura
    private double findSafeHeight(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();

        // Buscar desde arriba hacia abajo para encontrar la primera superficie sólida
        for (int y = world.getMaxHeight(); y > world.getMinHeight(); y--) {
            Location checkLoc = new Location(world, x, y, z);
            if (!checkLoc.getBlock().getType().isAir() &&
                    checkLoc.getBlock().getType().isSolid()) {
                return y + 0.1; // Justo encima del bloque
            }
        }
        return location.getY(); // Fallback
    }

    private void spawnCustomFang(Evoker summoner, Location location) {

        if (location.getBlock().getType().isSolid()) {
            // Ajustar hacia arriba si está dentro de un bloque
            location.add(0, 1, 0);
        }

        // Crear el EvokerFang custom
        EvokerFangs fang = (EvokerFangs) summoner.getWorld().spawnEntity(location, EntityType.EVOKER_FANGS);

        fang.setCustomName("Iceologer");
        fang.setCustomNameVisible(false);

        // Marcar como fang de hielo
        fang.getPersistentDataContainer().set(iceFangsKey, PersistentDataType.BYTE, (byte) 1);

        updateFangsMetadata(fang);

        // Establecer el owner
        try {
            Object nmsFang = getNMSEntity(fang);
            Object nmsEvoker = getNMSEntity(summoner);

            if (nmsFang != null && nmsEvoker != null) {
                Class<?> fangClass = nmsFang.getClass();
                java.lang.reflect.Field ownerField = fangClass.getDeclaredField("owner");
                ownerField.setAccessible(true);
                ownerField.set(nmsFang, nmsEvoker);
            }
        } catch (Exception e) {
            // Silencioso, no es crítico
        }

        // Efectos de hielo
        fang.getWorld().spawnParticle(Particle.SNOWFLAKE,
                location.add(0, 0.5, 0), 10, 0.3, 0.3, 0.3, 0.1);
        fang.getWorld().playSound(location,
                Sound.BLOCK_GLASS_BREAK, 0.7f, 1.2f);
    }


    private void updateFangsMetadata(EvokerFangs fangs) {
        try {
            // Forzar la actualización de la metadata del cliente
            fangs.setGlowing(true);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (fangs.isValid()) {
                    fangs.setGlowing(false);
                }
            }, 2L);

        } catch (Exception e) {
            plugin.getLogger().warning("Error actualizando metadata: " + e.getMessage());
        }
    }

    private void debugFangsSpawn(EvokerFangs fangs, CreatureSpawnEvent.SpawnReason reason) {
        plugin.getLogger().info("EvokerFangs spawn - Razón: " + reason +
                ", Loc: " + fangs.getLocation() +
                ", CustomName: " + fangs.getCustomName());
    }


    @EventHandler
    public void onEntityDamageByFangs(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof EvokerFangs fangs) {
            if (fangs.getPersistentDataContainer().has(iceFangsKey, PersistentDataType.BYTE)) {
                // ✅ Aumentar daño para que sea significativo a cualquier distancia
                event.setDamage(10.0); // Daño fijo de 4 corazones

                if (event.getEntity() instanceof LivingEntity entity) {
                    entity.setFreezeTicks(entity.getFreezeTicks() + 120); // 6 segundos
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2)); // Slowness más fuerte

                    // Efectos visuales
                    entity.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            entity.getLocation().add(0, 1, 0), 20, 0.7, 0.7, 0.7, 0.2);
                    entity.getWorld().playSound(entity.getLocation(),
                            Sound.ENTITY_PLAYER_HURT_FREEZE, 1.2f, 0.8f);
                }
            }
        }
    }

    // Hacer que los Ángeles de Hielo también apliquen efectos de congelación
    @EventHandler
    public void onIceAngelAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Vex vex &&
                vex.getPersistentDataContainer().has(iceAngelKey, PersistentDataType.BYTE)) {

            if (event.getEntity() instanceof LivingEntity entity) {
                // Aplicar congelación
                entity.setFreezeTicks(entity.getFreezeTicks() + 80); // 4 segundos
                entity.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        entity.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
                entity.getWorld().playSound(entity.getLocation(),
                        Sound.ENTITY_PLAYER_HURT_FREEZE, 0.5f, 1.2f);
            }
        }
    }

    private void performSpecialAttack(Evoker iceologer, Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, 10f, 2f);
        Location startLocation = player.getLocation().add(0, 10, 0);
        BlockData blockData = Material.PACKED_ICE.createBlockData();

        BlockDisplay blockDisplay = (BlockDisplay) iceologer.getWorld().spawnEntity(startLocation, EntityType.BLOCK_DISPLAY);
        blockDisplay.setBlock(blockData);
        blockDisplay.setCustomName("iceSphere");
        blockDisplay.setCustomNameVisible(false);

        blockDisplay.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf(),
                new Vector3f(0.8f, 0.8f, 0.8f),
                new Quaternionf()
        ));

        blockDisplay.setGlowing(true);
        blockDisplay.setGlowColorOverride(Color.AQUA);

        new BukkitRunnable() {
            private float rotationAngle = 0.0f;

            @Override
            public void run() {
                if (player.isDead() || !player.isOnline()) {
                    blockDisplay.remove();
                    cancel();
                    return;
                }

                boolean hit = false;
                Location currentLocation = blockDisplay.getLocation();

                if (currentLocation.distance(player.getLocation()) <= 1.0) {
                    hit = true;
                } else {
                    Vector direction = player.getLocation().toVector().subtract(currentLocation.toVector()).normalize();
                    blockDisplay.teleport(currentLocation.add(direction.multiply(0.3)));

                    rotationAngle += 10.0f;
                    Quaternionf rotation = new Quaternionf().rotateY((float) Math.toRadians(rotationAngle));
                    blockDisplay.setTransformation(new Transformation(
                            new Vector3f(0, 0, 0),
                            rotation,
                            new Vector3f(0.8f, 0.8f, 0.8f),
                            rotation
                    ));
                }

                if (hit) {
                    if (player.isBlocking()) {
                        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.8f);
                    } else {
                        player.damage(4);
                        applyFreezeEffect(player);
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
                        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 30, 0.5, 0.5, 0.5);
                    }
                    blockDisplay.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void applyFreezeEffect(Player player) {
        player.setFreezeTicks(300);
        frozenPlayers.add(player);
        player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.1f);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // El manejo de flechas ahora está en IceBowLogic
        if (event.getDamager() instanceof Arrow arrow) {
            iceBowLogic.handleArrowDamage(event, arrow, iceologerKey);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Evoker iceologer && activeIceologers.contains(iceologer)) {
            if (event.getTarget() instanceof Player player) {
                // Aplicar ceguera a todos los jugadores en un radio de 25 bloques (solo una vez)
                if (!blindnessApplied.contains(iceologer.getUniqueId())) {
                    for (Player nearbyPlayer : iceologer.getWorld().getPlayers()) {
                        if (nearbyPlayer.getLocation().distance(iceologer.getLocation()) <= 25) {
                            nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 300, 0)); // 15 segundos
                        }
                    }
                    blindnessApplied.add(iceologer.getUniqueId());
                }
            } else {
                // Limpiar efectos de congelación si el objetivo no es un jugador
                if (event.getTarget() instanceof Player player) {
                    frozenPlayers.remove(player);
                    player.setFreezeTicks(0);
                }
            }
        }
    }

    private void performIceBlockAttack(Evoker iceologer) {
        if (random.nextInt(4) != 0) return;

        World world = iceologer.getWorld();
        List<Player> nearbyPlayers = new ArrayList<>();

        for (Entity entity : iceologer.getNearbyEntities(10, 10, 10)) {
            if (entity instanceof Player player &&
                    (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)) {
                nearbyPlayers.add(player);
            }
        }

        if (nearbyPlayers.isEmpty()) return;

        Player target = nearbyPlayers.size() > 1
                ? nearbyPlayers.get(new Random().nextInt(nearbyPlayers.size()))
                : nearbyPlayers.get(0);

        Location origin = target.getLocation().add(0, 10, 0);

        world.playSound(target.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 0.5f);

        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(45 * i);
            double x = Math.cos(angle) * 3;
            double z = Math.sin(angle) * 3;
            Location spawnLocation = origin.clone().add(x, 0, z);

            BlockData blockData = Material.PACKED_ICE.createBlockData();
            BlockDisplay blockDisplay = (BlockDisplay) world.spawnEntity(spawnLocation, EntityType.BLOCK_DISPLAY);
            blockDisplay.setBlock(blockData);
            blockDisplay.setGlowing(true);
            blockDisplay.setGlowColorOverride(Color.BLUE);

            animateFallingBlock(blockDisplay, target.getLocation());
        }
    }

    private void animateFallingBlock(BlockDisplay blockDisplay, Location center) {
        new BukkitRunnable() {
            private double height = 10;
            private double velocity = 0.2;

            @Override
            public void run() {
                Location currentLocation = blockDisplay.getLocation();
                height -= velocity;
                blockDisplay.teleport(currentLocation.subtract(0, velocity, 0));

                currentLocation.getWorld().spawnParticle(Particle.SNOWFLAKE, currentLocation, 5, 0.1, 0.1, 0.1, 0.1);

                if (height <= 0) {
                    applyExplosionEffect(blockDisplay.getLocation(), center);
                    blockDisplay.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void applyExplosionEffect(Location location, Location center) {
        World world = location.getWorld();

        world.spawnParticle(Particle.EXPLOSION_EMITTER, location, 1);
        world.playSound(location, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 2f, 2f);

        double radius = 3.0;
        for (Entity entity : world.getNearbyEntities(location, radius, radius, radius)) {
            if (entity instanceof Player player &&
                    (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)) {

                if (player.getLocation().distance(center) <= radius) {
                    player.damage(5);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                }
            }
        }
    }

    @EventHandler
    public void onIceologerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Evoker iceologer &&
                iceologer.getPersistentDataContainer().has(iceologerKey, PersistentDataType.BYTE)) {

            // Sonido de daño de Illusioner
            iceologer.getWorld().playSound(iceologer.getLocation(), Sound.ENTITY_ILLUSIONER_HURT, 1.0f, 1.5f);
        }
    }

    @EventHandler
    public void onIceologerDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Evoker iceologer &&
                iceologer.getPersistentDataContainer().has(iceologerKey, PersistentDataType.BYTE)) {

            // Prevenir que suelte totems
            event.getDrops().removeIf(item -> item.getType() == Material.TOTEM_OF_UNDYING);

            // 10% de probabilidad de dropear el arco
            if (random.nextDouble() <= 0.1) {
                ItemStack iceBow = iceBowItem.createIceBow();
                iceologer.getWorld().dropItemNaturally(iceologer.getLocation(), iceBow);
            }

            // Sonido de muerte de Illusioner
            iceologer.getWorld().playSound(iceologer.getLocation(), Sound.ENTITY_ILLUSIONER_DEATH, SoundCategory.HOSTILE, 1.0f, 1.5f);
            iceologer.getWorld().dropItemNaturally(iceologer.getLocation(), ItemsTotems.createIceCrystal());

            activeIceologers.remove(iceologer);
            blindnessApplied.remove(iceologer.getUniqueId());

            // Limpiar cooldowns de jugadores si es necesario
            playerBowCooldowns.entrySet().removeIf(entry ->
                    System.currentTimeMillis() - entry.getValue() > 300000); // Limpiar entradas de más de 5 minutos
        }
    }

    public NamespacedKey getIceologerKey() {
        return iceologerKey;
    }

    private boolean isIceologer(Entity entity) {
        return entity instanceof Evoker && entity.getPersistentDataContainer().has(iceologerKey, PersistentDataType.BYTE);
    }

    // Getters para acceder a las instancias desde otros lugares
    public Set<Evoker> getActiveIceologers() {
        return activeIceologers;
    }

    public IceBowItem getIceBowItem() {
        return iceBowItem;
    }

    public IceBowLogic getIceBowLogic() {
        return iceBowLogic;
    }
}