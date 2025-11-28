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

import java.util.*;

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

    // Arco de hielo
    private final IceBowItem iceBowItem;
    private final IceBowLogic iceBowLogic;

    public Iceologer(JavaPlugin plugin) {
        this.plugin = plugin;
        this.iceologerKey = new NamespacedKey(plugin, "iceologer");
        this.iceAngelKey = new NamespacedKey(plugin, "ice_angel");
        this.iceFangsKey = new NamespacedKey(plugin, "ice_fangs");

        this.iceBowItem = new IceBowItem(plugin);
        this.iceBowLogic = new IceBowLogic(plugin, playerBowCooldowns);
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

    // === SPAWN / CONTROL DEL ICEOLOGER ===

    public Evoker spawnIceologer(Location location) {
        Evoker iceologer = (Evoker) location.getWorld().spawnEntity(location, EntityType.EVOKER);
        iceologer.setCustomName(ChatColor.AQUA + "" + ChatColor.BOLD + "Iceologer");
        iceologer.setCustomNameVisible(true);

        iceologer.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(32);

        // Vida aumentada
        iceologer.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(48.0);
        iceologer.setHealth(48.0);

        iceologer.getPersistentDataContainer().set(iceologerKey, PersistentDataType.BYTE, (byte) 1);

        // Arco de hielo
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
            private int customFangsCooldown = 0;

            @Override
            public void run() {
                if (iceologer.isDead() || !iceologer.isValid()) {
                    cancel();
                    activeIceologers.remove(iceologer);
                    return;
                }

                if (iceologer.getTarget() instanceof Player player) {
                    if ((player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
                            && iceologer.hasLineOfSight(player)) {

                        double distance = iceologer.getLocation().distance(player.getLocation());

                        // Disparo de arco a media / larga distancia
                        if (distance >= 8 && distance <= 20 && bowCooldown <= 0) {
                            iceBowLogic.shootIceBow(iceologer, player, iceologerKey);
                            bowCooldown = 60 + random.nextInt(20); // 3–4 segundos
                        }

                        // Fangs personalizados cada X ticks
                        if (customFangsCooldown <= 0) {
                            performCustomFangsAttack(iceologer, player);
                            customFangsCooldown = 160; // 8 segundos
                        }

                        // Ataque especial esfera de hielo cada 6s
                        if (distance < 15 && iceologer.getTicksLived() % 120 == 0) {
                            performSpecialAttack(iceologer, player);
                        }

                        // Bloques de hielo cada 5s
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

    // === ÁNGELES DE HIELO (VEX CUSTOM) ===

    private void transformToIceAngel(Vex vex) {
        vex.setCustomName(ChatColor.AQUA + "" + ChatColor.BOLD + "Angel de Hielo");
        vex.setCustomNameVisible(false);
        vex.getPersistentDataContainer().set(iceAngelKey, PersistentDataType.BYTE, (byte) 1);

        vex.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
        vex.setHealth(20.0);

        vex.getWorld().spawnParticle(Particle.SNOWFLAKE,
                vex.getLocation().add(0, 1, 0),
                20, 0.5, 0.5, 0.5, 0.1);
        vex.getWorld().playSound(vex.getLocation(),
                Sound.ENTITY_PLAYER_HURT_FREEZE,
                0.8f, 1.2f);
    }

    private void spawnCustomIceAngel(Evoker summoner, Location location) {
        Vex iceAngel = (Vex) summoner.getWorld().spawnEntity(location, EntityType.VEX);
        transformToIceAngel(iceAngel);

        // Objetivo: mismo target que el Iceologer, si existe
        if (summoner.getTarget() != null && summoner.getTarget().isValid()) {
            iceAngel.setTarget(summoner.getTarget());
        }

        // Efectos visuales
        iceAngel.getWorld().spawnParticle(
                Particle.SNOWFLAKE,
                location.clone().add(0, 1, 0),
                30, 0.5, 0.5, 0.5, 0.1);
        iceAngel.getWorld().playSound(location,
                Sound.ENTITY_PLAYER_HURT_FREEZE,
                1.0f, 0.8f);
    }

    // Invocación especial (si la quieres usar en algún momento)
    private void performIceAngelSummon(Evoker iceologer, Player target) {
        iceologer.getWorld().playSound(iceologer.getLocation(),
                Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 0.8f);

        iceologer.getWorld().spawnParticle(Particle.SNOWFLAKE,
                iceologer.getLocation().add(0, 2, 0),
                50, 0.5, 0.5, 0.5, 0.2);

        int angelsToSpawn = 1 + random.nextInt(2);
        Location spawnLocation = iceologer.getLocation().add(0, 1, 0);

        for (int i = 0; i < angelsToSpawn; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Vex iceAngel = (Vex) iceologer.getWorld().spawnEntity(spawnLocation, EntityType.VEX);
                transformToIceAngel(iceAngel);

                if (target != null && target.isValid()) {
                    iceAngel.setTarget(target);
                }
            }, 10L + (i * 5L));
        }
    }

    // === EVENTOS VANILLA: VEX Y FANGS DEL EVOKER ===

    /**
     * Cuando un Vex se spawnea por hechizo (SPELL) de un Evoker:
     * - Si el hechicero es un Iceologer cercano, cancelamos ese Vex
     *   y spawneamos nuestro Ángel de Hielo custom.
     */
    @EventHandler
    public void onVexSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.VEX) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPELL) return;

        Vex vex = (Vex) event.getEntity();

        for (Evoker iceologer : activeIceologers) {
            if (iceologer.getWorld().equals(vex.getWorld())
                    && iceologer.getLocation().distance(vex.getLocation()) <= 12) {

                event.setCancelled(true);
                spawnCustomIceAngel(iceologer, vex.getLocation());
                break;
            }
        }
    }

    /**
     * Cuando se spawnean EvokerFangs (ya sea SPELL o cualquier razón):
     * - Si hay un Iceologer activo cerca, convertimos esas fangs en fangs de hielo.
     *   (para los hechizos vanilla del Evoker).
     * - Las fangs que tú creas manualmente ya vienen marcadas por PDC y no necesitan esto.
     */
    @EventHandler
    public void onFangsSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof EvokerFangs fangs)) return;

        // Si ya está marcada como fang de hielo (spawnCustomFang), no tocamos nada
        if (fangs.getPersistentDataContainer().has(iceFangsKey, PersistentDataType.BYTE)) {
            return;
        }

        for (Evoker iceologer : activeIceologers) {
            if (iceologer.getWorld().equals(fangs.getWorld())
                    && iceologer.getLocation().distance(fangs.getLocation()) <= 15) {

                transformToIceFangs(fangs);
                break;
            }
        }
    }

    // === FANGS DE HIELO ===

    private void transformToIceFangs(EvokerFangs fangs) {
        fangs.setCustomName("Iceologer");
        fangs.setCustomNameVisible(false);
        fangs.getPersistentDataContainer().set(iceFangsKey, PersistentDataType.BYTE, (byte) 1);

        fangs.getWorld().spawnParticle(Particle.SNOWFLAKE,
                fangs.getLocation().add(0, 1, 0),
                20, 0.5, 0.5, 0.5, 0.1);
        fangs.getWorld().playSound(fangs.getLocation(),
                Sound.ENTITY_PLAYER_HURT_FREEZE,
                0.8f, 1.2f);
    }

    private void performCustomFangsAttack(Evoker iceologer, LivingEntity target) {
        Location startLocation = iceologer.getLocation();
        Location targetLocation = target.getLocation();

        iceologer.getWorld().playSound(startLocation,
                Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.5f, 0.7f);

        iceologer.getWorld().spawnParticle(Particle.SNOWFLAKE,
                startLocation.add(0, 2, 0),
                40, 0.7, 0.7, 0.7, 0.2);

        double distance = startLocation.distance(targetLocation);

        int fangsCount;
        if (distance > 30) {
            fangsCount = 8 + random.nextInt(5);
        } else {
            fangsCount = (int) (distance * 0.8);
            fangsCount = Math.min(Math.max(fangsCount, 5), 15);
        }

        Vector direction = targetLocation.toVector().subtract(startLocation.toVector()).normalize();

        for (int i = 0; i < fangsCount; i++) {
            double progress = (i + 1) / (double) fangsCount;

            Location fangLocation;
            if (distance > 30) {
                double adjustedDistance = distance - 10 + (random.nextDouble() * 20);
                fangLocation = startLocation.clone().add(direction.clone().multiply(adjustedDistance * progress));
            } else {
                fangLocation = startLocation.clone().add(direction.clone().multiply(distance * progress));
            }

            fangLocation.setY(findSafeHeight(fangLocation));

            int delay = i * 2;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (iceologer.isValid() && !iceologer.isDead() && target.isValid()) {
                    spawnCustomFang(iceologer, fangLocation);
                }
            }, delay);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (target.isValid()) {
                target.getWorld().playSound(target.getLocation(),
                        Sound.ENTITY_PLAYER_HURT_FREEZE, 1.2f, 0.8f);
                target.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        target.getLocation().add(0, 1, 0),
                        30, 1.0, 1.0, 1.0, 0.3);
            }
        }, fangsCount * 2L);
    }

    private double findSafeHeight(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();

        for (int y = world.getMaxHeight(); y > world.getMinHeight(); y--) {
            Location checkLoc = new Location(world, x, y, z);
            if (!checkLoc.getBlock().getType().isAir()
                    && checkLoc.getBlock().getType().isSolid()) {
                return y + 0.1;
            }
        }
        return location.getY();
    }

    private void spawnCustomFang(Evoker summoner, Location location) {
        if (location.getBlock().getType().isSolid()) {
            location.add(0, 1, 0);
        }

        EvokerFangs fang = (EvokerFangs) summoner.getWorld().spawnEntity(location, EntityType.EVOKER_FANGS);

        fang.setCustomName("Iceologer");
        fang.setCustomNameVisible(false);
        fang.getPersistentDataContainer().set(iceFangsKey, PersistentDataType.BYTE, (byte) 1);

        updateFangsMetadata(fang);

        fang.getWorld().spawnParticle(Particle.SNOWFLAKE,
                location.add(0, 0.5, 0),
                10, 0.3, 0.3, 0.3, 0.1);
        fang.getWorld().playSound(location,
                Sound.BLOCK_GLASS_BREAK, 0.7f, 1.2f);
    }

    private void updateFangsMetadata(EvokerFangs fangs) {
        fangs.setGlowing(true);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (fangs.isValid()) {
                fangs.setGlowing(false);
            }
        }, 2L);
    }

    // Daño de fangs de hielo
    @EventHandler
    public void onEntityDamageByFangs(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof EvokerFangs fangs) {
            if (fangs.getPersistentDataContainer().has(iceFangsKey, PersistentDataType.BYTE)) {
                event.setDamage(10.0); // 4 corazones

                if (event.getEntity() instanceof LivingEntity entity) {
                    entity.setFreezeTicks(entity.getFreezeTicks() + 120); // 6s
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));

                    entity.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            entity.getLocation().add(0, 1, 0),
                            20, 0.7, 0.7, 0.7, 0.2);
                    entity.getWorld().playSound(entity.getLocation(),
                            Sound.ENTITY_PLAYER_HURT_FREEZE, 1.2f, 0.8f);
                }
            }
        }
    }

    // ÁNGEL DE HIELO pega → congela
    @EventHandler
    public void onIceAngelAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Vex vex &&
                vex.getPersistentDataContainer().has(iceAngelKey, PersistentDataType.BYTE)) {

            if (event.getEntity() instanceof LivingEntity entity) {
                entity.setFreezeTicks(entity.getFreezeTicks() + 80);
                entity.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        entity.getLocation().add(0, 1, 0),
                        10, 0.3, 0.3, 0.3, 0.1);
                entity.getWorld().playSound(entity.getLocation(),
                        Sound.ENTITY_PLAYER_HURT_FREEZE, 0.5f, 1.2f);
            }
        }
    }

    // === ESFERA DE HIELO (ataque especial) ===

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
                        player.getWorld().spawnParticle(Particle.EXPLOSION,
                                player.getLocation(),
                                30, 0.5, 0.5, 0.5);
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
        player.getWorld().spawnParticle(Particle.SNOWFLAKE,
                player.getLocation().add(0, 1, 0),
                20, 0.5, 0.5, 0.5, 0.1);
        player.playSound(player.getLocation(),
                Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.1f);
    }

    // === ARCO / TARGET / BLOQUES DE HIELO ===

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Arrow arrow) {
            iceBowLogic.handleArrowDamage(event, arrow, iceologerKey);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Evoker iceologer && activeIceologers.contains(iceologer)) {
            if (event.getTarget() instanceof Player player) {
                if (!blindnessApplied.contains(iceologer.getUniqueId())) {
                    for (Player nearbyPlayer : iceologer.getWorld().getPlayers()) {
                        if (nearbyPlayer.getLocation().distance(iceologer.getLocation()) <= 25) {
                            nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 300, 0));
                        }
                    }
                    blindnessApplied.add(iceologer.getUniqueId());
                }
            } else {
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
                ? nearbyPlayers.get(random.nextInt(nearbyPlayers.size()))
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
            private final double velocity = 0.2;

            @Override
            public void run() {
                Location currentLocation = blockDisplay.getLocation();
                height -= velocity;
                blockDisplay.teleport(currentLocation.subtract(0, velocity, 0));

                currentLocation.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        currentLocation,
                        5, 0.1, 0.1, 0.1, 0.1);

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

    // === DAÑO / MUERTE DEL ICEOLOGER ===

    @EventHandler
    public void onIceologerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Evoker iceologer &&
                iceologer.getPersistentDataContainer().has(iceologerKey, PersistentDataType.BYTE)) {

            iceologer.getWorld().playSound(iceologer.getLocation(),
                    Sound.ENTITY_ILLUSIONER_HURT, 1.0f, 1.5f);
        }
    }

    @EventHandler
    public void onIceologerDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Evoker iceologer &&
                iceologer.getPersistentDataContainer().has(iceologerKey, PersistentDataType.BYTE)) {

            event.getDrops().removeIf(item -> item.getType() == Material.TOTEM_OF_UNDYING);

            if (random.nextDouble() <= 0.1) {
                ItemStack iceBow = iceBowItem.createIceBow();
                iceologer.getWorld().dropItemNaturally(iceologer.getLocation(), iceBow);
            }

            iceologer.getWorld().playSound(iceologer.getLocation(),
                    Sound.ENTITY_ILLUSIONER_DEATH, SoundCategory.HOSTILE, 1.0f, 1.5f);
            iceologer.getWorld().dropItemNaturally(iceologer.getLocation(),
                    ItemsTotems.createIceCrystal());

            activeIceologers.remove(iceologer);
            blindnessApplied.remove(iceologer.getUniqueId());

            playerBowCooldowns.entrySet().removeIf(entry ->
                    System.currentTimeMillis() - entry.getValue() > 300000);
        }
    }

    // === GETTERS / UTIL ===

    public NamespacedKey getIceologerKey() {
        return iceologerKey;
    }

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
