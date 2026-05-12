package Bosses;

import Dificultades.CustomMobs.CorruptedBee;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import net.md_5.bungee.api.ChatColor;

import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class QueenBeeHandler extends BaseBoss implements Listener {

    //  /debugarena
    public static final Map<UUID, QueenBeeHandler> ACTIVE_BOSSES = new HashMap<>();

    private final Bee bee;
    private final Random random = new Random();

    // Estados
    private int globalTick = 0;
    private boolean runningSpecial = false;
    private boolean runningMelee = false;
    private boolean inRegenerationPhase = false;
    private boolean isDying = false;
    private boolean isFinalDeath = false;
    private Player killer = null;

    private int meleeDoneSinceLastSpecial = 0;
    private int requiredMeleeBetweenSpecials = 1;
    private int regenCooldown = 0;

    // --- SISTEMA DE MÚSICA ---
    private static class BossTrack {
        Sound sound;
        float pitch;
        int durationTicks;

        BossTrack(Sound sound, float pitch, int durationTicks) {
            this.sound = sound;
            this.pitch = pitch;
            this.durationTicks = durationTicks;
        }
    }

    private BukkitRunnable musicTask;
    // -------------------------

    // Curación
    private final List<Bee> healTotems = new ArrayList<>();
    private BukkitRunnable regenTask;

    private final List<BukkitRunnable> activeAttackTasks = new ArrayList<>();
    private final CorruptedBee corruptedBee;

    private final NamespacedKey bossKey;
    private final NamespacedKey arenaCenterX;
    private final NamespacedKey arenaCenterY;
    private final NamespacedKey arenaCenterZ;

    private final NamespacedKey musicKey;

    public QueenBeeHandler(JavaPlugin plugin, Bee bee) {
        super(plugin, bee);
        this.bee = bee;

        this.bossKey = new NamespacedKey(plugin, "is_queen_bee");
        this.arenaCenterX = new NamespacedKey(plugin, "arena_x");
        this.arenaCenterY = new NamespacedKey(plugin, "arena_y");
        this.arenaCenterZ = new NamespacedKey(plugin, "arena_z");
        this.musicKey = new NamespacedKey(plugin, "musica_iniciada");

        this.corruptedBee = new CorruptedBee(plugin);

        // --- LÓGICA DE PERSISTENCIA ---
        PersistentDataContainer pdc = bee.getPersistentDataContainer();

        if (pdc.has(bossKey, PersistentDataType.BYTE)) {
            double x = pdc.get(arenaCenterX, PersistentDataType.DOUBLE);
            double y = pdc.get(arenaCenterY, PersistentDataType.DOUBLE);
            double z = pdc.get(arenaCenterZ, PersistentDataType.DOUBLE);

            this.spawnLocation.setX(x);
            this.spawnLocation.setY(y);
            this.spawnLocation.setZ(z);

            this.start();

        } else {
            // CASO 2: PRIMER SPAWN
            pdc.set(bossKey, PersistentDataType.BYTE, (byte) 1);
            pdc.set(arenaCenterX, PersistentDataType.DOUBLE, spawnLocation.getX());
            pdc.set(arenaCenterY, PersistentDataType.DOUBLE, spawnLocation.getY());
            pdc.set(arenaCenterZ, PersistentDataType.DOUBLE, spawnLocation.getZ());

        }

        ACTIVE_BOSSES.put(bee.getUniqueId(), this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ==============================
    //        SPAWN DEL BOSS
    // ==============================

    public static QueenBeeHandler spawn(JavaPlugin plugin, Location center) {
        World world = center.getWorld();
        if (world == null) return null;

        Bee bee = world.spawn(center, Bee.class, b -> {
            b.setCustomName(ChatColor.DARK_PURPLE + "Corrupted Queen Bee");
            b.setCustomNameVisible(true);
            b.setRemoveWhenFarAway(false);
            b.setAnger(999999);
            b.setAI(true);

            Objects.requireNonNull(b.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(1000);
            Objects.requireNonNull(b.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.35);
            Objects.requireNonNull(b.getAttribute(Attribute.GENERIC_FOLLOW_RANGE)).setBaseValue(50);
            Objects.requireNonNull(b.getAttribute(Attribute.GENERIC_SCALE)).setBaseValue(3);

            b.setHealth(1000);
            b.setHasStung(false);
            b.setCannotEnterHiveTicks(Integer.MAX_VALUE);

            b.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
            b.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        });

        QueenBeeHandler handler = new QueenBeeHandler(plugin, bee);
        handler.start();
        return handler;
    }

    // ==============================
    //          BASEBOSS OVERRIDES
    // ==============================

    @Override
    protected String getBossTitle() {
        return ChatColor.DARK_PURPLE + "Corrupted Queen Bee";
    }

    @Override
    protected int getArenaRadius() {
        return 28;
    }

    @Override
    protected int getArenaHeightUp() {
        return 20;
    }

    @Override
    protected int getArenaHeightDown() {
        return 8;
    }

    @Override
    protected AreaZone.Shape getArenaShape() {
        return AreaZone.Shape.CIRCULAR;
    }

    @Override
    protected void onStart() {
        requiredMeleeBetweenSpecials = random.nextInt(3) + 1;
    }

    @Override
    protected void onTick() {
        if (isDying) return;
        globalTick++;

        if (regenCooldown > 0) {
            regenCooldown--;
        }

        bee.setHasStung(false);
        bee.setCannotEnterHiveTicks(Integer.MAX_VALUE);
        bee.setAnger(999999);

        if (inRegenerationPhase) {
            bee.setTarget(null);
            return;
        }

        if (runningSpecial || runningMelee) return;

        double max = Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
        double hp = bee.getHealth();

        boolean enraged = hp < max / 2.0;
        int attackDelay = enraged ? 15 : 30;

        if (globalTick % attackDelay == 0) {
            if (enraged && regenTask == null && regenCooldown <= 0) {
                if (random.nextDouble() < 0.10) {
                    startRegenerationPhase();
                    return;
                }
            }

            decideNextAttack();
        }
    }

    @Override
    protected void onDeath() {
        bee.getPersistentDataContainer().remove(bossKey);
        cleanupResources();
    }

    @Override
    protected void onUnload() {
        cleanupResources();
    }

    private void cleanupResources() {
        if (regenTask != null) regenTask.cancel();

        if (musicTask != null && !musicTask.isCancelled()) {
            musicTask.cancel();
        }

        for (Bee hive : healTotems) {
            if (hive.isValid()) hive.remove();
        }
        healTotems.clear();

        ACTIVE_BOSSES.remove(bee.getUniqueId());
    }

    // ==============================
    //          UTILIDADES
    // ==============================

    private List<Player> getActivePlayers() {
        List<Player> list = new ArrayList<>();
        for (UUID id : currentPlayers) {
            Player p = Bukkit.getPlayer(id);

            if (p != null && p.isOnline() && p.getWorld().equals(bee.getWorld())
                    && p.getGameMode() != GameMode.CREATIVE
                    && p.getGameMode() != GameMode.SPECTATOR) {
                list.add(p);
            }
        }
        return list;
    }

    private Player getNearestPlayer() {
        List<Player> p = getActivePlayers();
        if (p.isEmpty()) return null;

        Player near = null;
        double best = Double.MAX_VALUE;

        for (Player pl : p) {
            double d = pl.getLocation().distanceSquared(bee.getLocation());
            if (d < best) {
                best = d;
                near = pl;
            }
        }
        return near;
    }

    // ==============================
    //      TELEPORT VISUAL
    // ==============================

    private void teleportWithVisual(Location from, Location to) {
        showSphere(from, 2.5, Particle.ELECTRIC_SPARK, Color.WHITE);
        showSphere(to, 2.5, Particle.ELECTRIC_SPARK, Color.WHITE);

        from.getWorld().playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.1f);

        new BukkitRunnable() {
            @Override
            public void run() {
                bee.teleport(to);
                to.getWorld().playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.6f);
            }
        }.runTaskLater(plugin, 10L);
    }

    private void showSphere(Location center, double radius, Particle type, Color c) {
        World w = Objects.requireNonNull(center.getWorld());

        w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.7f, 1.4f);

        for (double phi = 0; phi < Math.PI; phi += Math.PI / 10) {
            double y = radius * Math.cos(phi);
            double r = radius * Math.sin(phi);

            for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / 10) {
                double x = r * Math.cos(theta);
                double z = r * Math.sin(theta);
                Location l = center.clone().add(x, y, z);

                w.spawnParticle(Particle.DUST, l, 1, new Particle.DustOptions(c, 1.3f));
            }
        }
    }

    // ==============================
    //     SELECCIÓN DE ATAQUES
    // ==============================

    private void decideNextAttack() {
        if (getActivePlayers().isEmpty()) {
            bee.setTarget(null);
            return;
        }

        if (meleeDoneSinceLastSpecial < requiredMeleeBetweenSpecials) {
            startRandomMelee();
            meleeDoneSinceLastSpecial++;
        } else {
            startRandomSpecial();
            meleeDoneSinceLastSpecial = 0;
            requiredMeleeBetweenSpecials = random.nextInt(3) + 1;
        }
    }

    // ==============================
    //      ATAQUES A MELEE
    // ==============================

    private enum MeleeType { NORMAL, DASH, TP_COMBO }

    private void startRandomMelee() {
        if (runningMelee || runningSpecial || inRegenerationPhase) return;
        runningMelee = true;

        MeleeType t = MeleeType.values()[random.nextInt(MeleeType.values().length)];

        switch (t) {
            case NORMAL -> meleeNormal();
            case DASH -> meleeDash();
            case TP_COMBO -> meleeTPCombo();
        }
    }

    // 1) Ataque normal
    private void meleeNormal() {
        Player target = getNearestPlayer();
        if (target == null) {
            runningMelee = false;
            return;
        }

        bee.setTarget(target);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!bee.isValid() || bee.isDead()) {
                    cancel();
                    runningMelee = false;
                    return;
                }

                t++;
                if (t > 40) {
                    cancel();
                    runningMelee = false;
                    return;
                }

                if (bee.getLocation().distance(target.getLocation()) <= 2.0) {
                    target.damage(8.0, bee);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 1));
                    bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_STING, 1f, 0.7f);
                    cancel();
                    runningMelee = false;
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // 2) Ataque rápido (DASH)
    private void meleeDash() {
        Player target = getNearestPlayer();
        if (target == null) {
            runningMelee = false;
            return;
        }

        Location start = bee.getLocation();

        Vector dir = target.getLocation()
                .toVector()
                .subtract(start.toVector())
                .normalize()
                .multiply(1.2);

        bee.getWorld().playSound(start, Sound.ENTITY_PHANTOM_SWOOP, 1f, 0.5f);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!bee.isValid() || bee.isDead()) {
                    cancel();
                    runningMelee = false;
                    return;
                }

                t++;
                bee.setVelocity(dir);

                bee.getWorld().spawnParticle(Particle.CLOUD, bee.getLocation(), 5, 0.3, 0.3, 0.3, 0.02);
                bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 0.4f, 1.5f);

                if (bee.getLocation().distance(target.getLocation()) <= 2.5) {
                    target.damage(10.0, bee);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                    bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_STING, 1f, 0.4f);
                    cancel();
                    runningMelee = false;
                }

                if (t > 20) {
                    cancel();
                    runningMelee = false;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // 3) TP Combo: TP al jugador, 4 golpes, vuelve al centro
    private void meleeTPCombo() {
        Player target = getNearestPlayer();
        if (target == null) {
            runningMelee = false;
            return;
        }

        Location start = bee.getLocation();
        Location to = target.getLocation().clone().add(0, 2, 0);
        Location center = spawnLocation.clone().add(0, 4, 0);

        teleportWithVisual(start, to);

        new BukkitRunnable() {
            int hits = 0;

            @Override
            public void run() {
                if (!bee.isValid() || bee.isDead()) {
                    cancel();
                    runningMelee = false;
                    return;
                }

                if (hits == 0) {
                    bee.teleport(to);
                }

                if (bee.getLocation().distance(target.getLocation()) <= 2.0) {
                    target.damage(6.0, bee);
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 10, 0.4, 0.4, 0.4, 0.1);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BEE_STING, 1f, 1.6f);
                }

                hits++;
                if (hits >= 4) {
                    teleportWithVisual(bee.getLocation(), center);
                    runningMelee = false;
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    // ==============================
    //     ATAQUES ESPECIALES
    // ==============================

    private enum SpecialType {
        VENOMOUS_STINGS,
        EXPLOSIVE_STINGS,
        SUMMON_BEES,
        TOXIC_CLOUD
    }

    private void startRandomSpecial() {
        if (runningSpecial || runningMelee || inRegenerationPhase) return;
        runningSpecial = true;

        SpecialType t = SpecialType.values()[random.nextInt(SpecialType.values().length)];

        switch (t) {
            case VENOMOUS_STINGS -> specialVenomousStings();
            case EXPLOSIVE_STINGS -> specialExplosiveStings();
            case SUMMON_BEES -> specialSummonBees();
            case TOXIC_CLOUD -> specialToxicCloud();
        }
    }

    // =============================================================
    // 1) Aguijón VENENOSO: 5 en círculo, algunos dirigidos
    // =============================================================

    private void specialVenomousStings() {
        World w = bee.getWorld();
        Location origin = bee.getLocation().clone().add(0, 1.5, 0);
        List<Player> players = getActivePlayers();

        if (players.isEmpty()) {
            runningSpecial = false;
            return;
        }

        w.playSound(origin, Sound.ENTITY_EVOKER_CAST_SPELL, 1.5f, 0.4f);

        int count = 5;

        List<Vector> baseDirs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            baseDirs.add(new Vector(Math.cos(angle), -0.1, Math.sin(angle)).normalize().multiply(0.4));
        }

        List<Player> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);
        int targetCount = Math.min(shuffled.size(), count);

        for (int i = 0; i < count; i++) {

            Vector dir;
            if (i < targetCount) {
                Player target = shuffled.get(i);
                dir = target.getLocation().add(0, 1, 0)
                        .toVector()
                        .subtract(origin.toVector())
                        .normalize()
                        .multiply(0.45);
            } else {
                dir = baseDirs.get(i);
            }

            BlockDisplay spike = w.spawn(origin, BlockDisplay.class);
            spike.setBlock(Bukkit.createBlockData(Material.POINTED_DRIPSTONE));
            spike.setGlowing(true);
            spike.setGlowColorOverride(Color.PURPLE);
            spike.setGravity(false);

            new BukkitRunnable() {
                int life = 0;

                @Override
                public void run() {
                    if (!spike.isValid()) {
                        cancel();
                        return;
                    }
                    life++;
                    if (life > 80) {
                        spike.remove();
                        cancel();
                        return;
                    }

                    Location newLoc = spike.getLocation().add(dir);

                    if (newLoc.getBlock().getType().isSolid()) {
                        createPoisonSphere(newLoc);
                        spike.remove();
                        cancel();
                        return;
                    }

                    spike.teleport(newLoc);

                    for (Entity e : w.getNearbyEntities(newLoc, 1, 1, 1)) {
                        if (e instanceof Player p && getActivePlayers().contains(p)) {
                            p.damage(11.0, bee);
                            p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 1));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));
                            spike.remove();
                            cancel();
                            return;
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                runningSpecial = false;
            }
        }.runTaskLater(plugin, 60L);
    }

    private void createPoisonSphere(Location center) {
        World w = center.getWorld();
        double radius = 3.0;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 40) {
                    cancel();
                    return;
                }

                w.playSound(center, Sound.BLOCK_BREWING_STAND_BREW, 0.5f, 0.7f);

                for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 16) {
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Location l = center.clone().add(x, 0.1, z);
                    w.spawnParticle(Particle.DUST,
                            l.getX(), l.getY(), l.getZ(),
                            1, 0.0, 0.0, 0.0, 0.0,
                            new Particle.DustOptions(Color.fromRGB(180, 0, 200), 1.3f));
                }

                for (Entity e : w.getNearbyEntities(center, radius, 1.5, radius)) {
                    if (e instanceof Player p && getActivePlayers().contains(p)) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 1));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    // =============================================================
    // 2) Aguijón EXPLOSIVO
    // =============================================================

    private void specialExplosiveStings() {
        World w = bee.getWorld();
        Location origin = bee.getLocation().clone().add(0, 1.5, 0);
        List<Player> players = getActivePlayers();

        if (players.isEmpty()) {
            runningSpecial = false;
            return;
        }

        w.playSound(origin, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.4f, 0.6f);

        int count = 5;

        List<Vector> baseDirs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            baseDirs.add(new Vector(Math.cos(angle), -0.05, Math.sin(angle)).normalize().multiply(0.5));
        }

        List<Player> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);
        int targetCount = Math.min(shuffled.size(), count);

        for (int i = 0; i < count; i++) {
            Vector dir;
            if (i < targetCount) {
                Player target = shuffled.get(i);
                dir = target.getLocation().add(0, 1, 0)
                        .toVector()
                        .subtract(origin.toVector())
                        .normalize()
                        .multiply(0.50);
            } else {
                dir = baseDirs.get(i);
            }

            BlockDisplay spike = w.spawn(origin, BlockDisplay.class);
            spike.setBlock(Bukkit.createBlockData(Material.POINTED_DRIPSTONE));
            spike.setGlowing(true);
            spike.setGlowColorOverride(Color.RED);
            spike.setGravity(false);

            new BukkitRunnable() {
                int life = 0;

                @Override
                public void run() {
                    if (!spike.isValid()) {
                        cancel();
                        return;
                    }
                    life++;
                    if (life > 80) {
                        explodeSpike(spike.getLocation());
                        spike.remove();
                        cancel();
                        return;
                    }

                    Location newLoc = spike.getLocation().add(dir);

                    if (newLoc.getBlock().getType().isSolid()) {
                        explodeSpike(newLoc);
                        spike.remove();
                        cancel();
                        return;
                    }

                    spike.teleport(newLoc);

                    for (Entity e : w.getNearbyEntities(newLoc, 1, 1, 1)) {
                        if (e instanceof Player p && getActivePlayers().contains(p)) {
                            explodeSpike(newLoc);
                            p.damage(9.0, bee);
                            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 0));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));
                            spike.remove();
                            cancel();
                            return;
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                runningSpecial = false;
            }
        }.runTaskLater(plugin, 60L);
    }

    private void explodeSpike(Location loc) {
        World w = loc.getWorld();
        if (w == null) return;

        w.spawnParticle(Particle.EXPLOSION, loc, 1);
        w.spawnParticle(Particle.CLOUD, loc, 15, 0.5, 0.5, 0.5, 0.02);
        w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.7f);

        w.createExplosion(loc.getX(), loc.getY(), loc.getZ(), 3.0f, false, false, bee);

        for (Entity e : w.getNearbyEntities(loc, 4, 3, 4)) {
            if (e instanceof Player p && getActivePlayers().contains(p)) {
                p.damage(6.0, bee);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
            }
        }
    }

    // =============================================================
    // 3) Refuerzos
    // =============================================================

    private void specialSummonBees() {
        World w = bee.getWorld();
        w.playSound(bee.getLocation(), Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 1.5f, 0.7f);

        int count = 8 + random.nextInt(9);

        for (int i = 0; i < count; i++) {
            double dx = random.nextDouble() * getArenaRadius() * 2 - getArenaRadius();
            double dz = random.nextDouble() * getArenaRadius() * 2 - getArenaRadius();

            Location spawnLoc = spawnLocation.clone().add(dx, 1, dz);

            new BukkitRunnable() {
                int y = 0;
                @Override
                public void run() {
                    if (y++ > 10) {
                        cancel();
                        return;
                    }
                    w.playSound(spawnLoc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.6f, 1.8f);
                    for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 12) {
                        double x = Math.cos(angle) * 2;
                        double z = Math.sin(angle) * 2;
                        Location l = spawnLoc.clone().add(x, y * 0.3, z);
                        w.spawnParticle(Particle.DUST, l, 1,
                                new Particle.DustOptions(Color.fromRGB(255, 150, 220), 1.2f));
                    }
                }
            }.runTaskTimer(plugin, 0L, 2L);

            new BukkitRunnable() {
                @Override
                public void run() {
                    Bee minion = corruptedBee.spawnCorruptedBee(spawnLoc);

                    Player near = getNearestPlayer();
                    if (near != null) {
                        minion.setTarget(near);
                    }

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (minion.isValid()) {
                                minion.remove();
                            }
                        }
                    }.runTaskLater(plugin, 20L * 30);
                }
            }.runTaskLater(plugin, 25L);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                runningSpecial = false;
            }
        }.runTaskLater(plugin, 60L);
    }

    // =============================================================
    // 4) Nube tóxica
    // =============================================================

    private void specialToxicCloud() {
        World w = bee.getWorld();

        Location center = spawnLocation.clone().add(0, 0, 0);
        teleportWithVisual(bee.getLocation(), center);
        bee.teleport(center);

        w.playSound(center, Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 0.6f);


        double[] yOffsets = {-6, -3, 1};

        new BukkitRunnable() {
            double radius = 3;

            @Override
            public void run() {
                if (radius > getArenaRadius()) {
                    cancel();
                    runningSpecial = false;
                    return;
                }

                w.playSound(center, Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 0.7f, 0.6f);

                for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 28) {
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    for (double oy : yOffsets) {
                        Location l = center.clone().add(x, oy, z);
                        w.spawnParticle(Particle.DUST, l.getX(), l.getY(), l.getZ(),
                                1, 0.0, 0.0, 0.0, 0.0,
                                new Particle.DustOptions(Color.fromRGB(120, 250, 120), 1.4f));
                    }
                }

                for (Entity e : w.getNearbyEntities(center, radius, 6, radius)) {
                    if (e instanceof Player p && getActivePlayers().contains(p)) {
                        if (areaZone.isInside(p.getLocation())) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 1));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                        }
                    }
                }

                radius += 1.5;
            }
        }.runTaskTimer(plugin, 0L, 6L);
    }

    // =============================================================
    // 5) Regeneración (FASE ESPECIAL)
    // =============================================================

    private void startRegenerationPhase() {
        if (inRegenerationPhase) return;
        inRegenerationPhase = true;

        runningSpecial = false;
        runningMelee = false;

        World w = bee.getWorld();
        Location center = spawnLocation.clone().add(0, 4, 0);

        bee.teleport(center);
        bee.setAI(false);
        bee.setTarget(null);
        bee.setVelocity(new Vector(0, 0, 0));
        bee.setGravity(false);
        w.playSound(center, Sound.ITEM_TOTEM_USE, 1f, 0.5f);

        for (Player p : getActivePlayers()) {
            p.sendMessage("\n" +
                    ChatColor.of("#E6737E") + "\u06de" +
                    ChatColor.of("#E47643") + " Destruye los" +
                    ChatColor.of("#EFDC93") + ChatColor.BOLD + " 4 totems Amarillos" +
                    ChatColor.of("#E47643") + " para que la abeja reina deje de curarse.");
        }

        healTotems.clear();
        double radius = 8;

        // --- Spawnear los 4 Tótems ---
        for (int i = 0; i < 4; i++) {
            Location l = center.clone().add(
                    Math.cos(i * Math.PI / 2) * radius,
                    -2,
                    Math.sin(i * Math.PI / 2) * radius
            );

            Bee hive = w.spawn(l, Bee.class, b -> {
                b.setCustomName(ChatColor.GOLD + "§lHEAL TOTEM");
                b.setCustomNameVisible(false);
                b.setAI(false);
                b.setGravity(false);
                b.setSilent(true);
                b.setGlowing(true);
                b.setRemoveWhenFarAway(false);
                Objects.requireNonNull(b.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(2);
                b.setHealth(2);
            });

            applyGlow(hive);
            healTotems.add(hive);

            w.spawnParticle(Particle.END_ROD, l, 20, 0.5, 0.5, 0.5, 0.05);
        }

        // --- Tarea de Regeneración ---
        regenTask = new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!bee.isValid() || bee.isDead()) {
                    cancel();
                    return;
                }

                if (bee.getLocation().distanceSquared(center) > 1) {
                    bee.teleport(center);
                }
                bee.setAI(false);
                bee.setVelocity(new Vector(0,0,0));

                long alive = healTotems.stream().filter(Bee::isValid).count();

                if (alive == 0) {
                    finishRegenerationPhase();
                    cancel();
                    return;
                }

                for (Bee totem : healTotems) {
                    if (totem.isValid()) {
                        drawBeam(totem.getLocation(), bee.getLocation().add(0, 0.5, 0), Color.PURPLE);
                    }
                }

                if (t % 40 == 0 && t > 0) {
                    double max = Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
                    double current = bee.getHealth();

                    double healAmount = alive * 10.0;

                    double newHealth = Math.min(max, current + healAmount);
                    bee.setHealth(newHealth);

                    w.playSound(bee.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f);
                    w.spawnParticle(Particle.HEART, bee.getLocation().add(0, 1, 0), 10, 1, 1, 1);

                    for(Player p : currentPlayers.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList()) {
                        p.sendActionBar("§d§lLA REINA SE REGENERA: §a+" + (int)healAmount + " HP");
                    }

                    if (newHealth >= max) {
                        finishRegenerationPhase();
                        cancel();
                        return;
                    }
                }

                t++;
            }
        };
        regenTask.runTaskTimer(plugin, 0L, 1L);
    }

    private void finishRegenerationPhase() {
        inRegenerationPhase = false;
        regenCooldown = 1200;

        if (regenTask != null && !regenTask.isCancelled()) {
            regenTask.cancel();
        }
        regenTask = null;

        for (Bee hive : healTotems) {
            if (hive != null && hive.isValid()) {
                hive.getWorld().spawnParticle(Particle.CLOUD, hive.getLocation(), 10);
                hive.remove();
            }
        }
        healTotems.clear();

        bee.setAI(true);
        bee.setGravity(true);
        bee.setTarget(getNearestPlayer());

        World w = bee.getWorld();
        w.playSound(bee.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.8f);
        w.spawnParticle(Particle.END_ROD, bee.getLocation().add(0, 1, 0),
                40, 0.8, 0.8, 0.8, 0.1);
        globalTick = 0;
    }

    private void drawBeam(Location from, Location to, Color color) {
        World w = from.getWorld();
        if (w == null || !Objects.equals(w, to.getWorld())) return;

        Vector diff = to.toVector().subtract(from.toVector());
        double length = diff.length();
        Vector step = diff.normalize().multiply(0.25);

        Location loc = from.clone();
        for (double d = 0; d < length; d += 0.25) {
            w.spawnParticle(Particle.DUST, loc.getX(), loc.getY(), loc.getZ(), 1, 0, 0, 0, 0, new Particle.DustOptions(color, 1.1f));
            loc.add(step);
        }
    }

    private void applyGlow(Entity e) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam("bee_heal_totem");

        if (team == null) {
            team = board.registerNewTeam("bee_heal_totem");
            team.setColor(org.bukkit.ChatColor.YELLOW);
        }

        team.addEntry(e.getUniqueId().toString());
    }

    private void breakHealTotem(Bee hive, Player p) {
        Location loc = hive.getLocation();
        World w = loc.getWorld();

        w.spawnParticle(Particle.EXPLOSION, loc, 2);
        w.spawnParticle(Particle.CLOUD, loc, 20, 0.6, 0.6, 0.6, 0.02);
        w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.4f);

        hive.remove();
        healTotems.remove(hive);

        if (healTotems.isEmpty() && regenTask != null) {
            regenTask.cancel();
            finishRegenerationPhase();
        }
    }

    // ==============================
    //          EVENTOS
    // ==============================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGenericDamage(EntityDamageEvent e) {
        if (!e.getEntity().equals(bee)) return;

        if (isFinalDeath) {
            return;
        }

        if (isDying || isHibernating() || inRegenerationPhase) {
            e.setCancelled(true);
            return;
        }

        if (e.isCancelled() && !bee.isInvulnerable()) e.setCancelled(false);
        if (bee.isInvulnerable()) {
            bee.setInvulnerable(false);
            e.setCancelled(false);
        }

        if (bee.getHealth() - e.getFinalDamage() <= 0) {
            e.setCancelled(true);

            if (e instanceof EntityDamageByEntityEvent eventByEntity) {
                if (eventByEntity.getDamager() instanceof Player p) {
                    this.killer = p;
                } else if (eventByEntity.getDamager() instanceof Projectile proj
                        && proj.getShooter() instanceof Player p) {
                    this.killer = p;
                }
            }

            bee.setHealth(1);
            startDeathAnimation();
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        Entity damaged = e.getEntity();
        Entity damager = e.getDamager();

        if (damaged.equals(bee)) {
            if (damager instanceof Player p) {
                addAttacker(p);
            } else if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) {
                addAttacker(p);
            }
        }

        if (damaged.equals(bee)) {
            if (isFinalDeath) return;

            if (isDying) {
                e.setCancelled(true);
                return;
            }
        }

        if (damaged.equals(bee) && damager instanceof Player player) {
            if (player.getInventory().getItemInMainHand().getType() == Material.MACE) {
                e.setCancelled(true);
                player.playSound(bee.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.5f);
                player.spawnParticle(Particle.CRIT, bee.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.1);
                return;
            }
        }

        if (damaged.equals(bee)) {
            if (damager instanceof Projectile) {
                e.setDamage(e.getDamage() * 0.5);
            }
        }

        if (damaged instanceof Bee hive && healTotems.contains(hive)) {
            double hp = hive.getHealth() - e.getFinalDamage();
            if (hp <= 0) {
                breakHealTotem(hive, damager instanceof Player p ? p : null);
            } else {
                hive.setHealth(hp);
                hive.getWorld().playSound(hive.getLocation(), Sound.BLOCK_BEEHIVE_SHEAR, 0.7f, 1.2f);
            }
            e.setCancelled(true);
            return;
        }

        if (damaged.equals(bee)) {
            if (bee.getHealth() - e.getFinalDamage() <= 0) {
                e.setCancelled(true);

                if (damager instanceof Player p) {
                    this.killer = p;
                } else if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) {
                    this.killer = p;
                }

                bee.setHealth(1);
                startDeathAnimation();
            }
        }
    }

    private void startDeathAnimation() {
        if (isDying) return;
        isDying = true;

        if (regenTask != null) regenTask.cancel();

        bee.setAI(false);
        bee.setInvulnerable(true);
        bee.setGravity(false);
        bee.setGlowing(true);
        bee.setCustomName(ChatColor.DARK_RED + "☠ Abeja Reina Caída ☠");

        mainBar.removeAll();
        staticBar.removeAll();

        World w = bee.getWorld();
        w.playSound(bee.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 5.0f, 0.8f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                if (!bee.isValid()) {
                    cancel();
                    onDeath();
                    return;
                }

                if (ticks >= 60) {
                    try {
                        w.spawnParticle(Particle.EXPLOSION_EMITTER, bee.getLocation(), 5);
                        w.spawnParticle(Particle.FLASH, bee.getLocation(), 1);
                        w.playSound(bee.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 5.0f, 0.6f);
                    } catch (Exception ignored) {}

                    isFinalDeath = true;
                    bee.setInvulnerable(false);

                    if (killer != null && killer.isOnline()) {
                        bee.damage(10000, killer);
                    } else {
                        bee.setHealth(0);
                    }

                    finalizeDeath();
                    cancel();
                    return;
                }

                Location loc = bee.getLocation().add(0, 0.15, 0);
                double offsetX = (random.nextDouble() - 0.5) * 0.2;
                double offsetZ = (random.nextDouble() - 0.5) * 0.2;
                loc.add(offsetX, 0, offsetZ);
                bee.teleport(loc);

                try {
                    w.spawnParticle(Particle.CLOUD, bee.getLocation().add(0, 0.5, 0), 5, 0.1, 0.1, 0.1, 0.05);
                    w.spawnParticle(Particle.WAX_ON, bee.getLocation(), 3, 0.5, 0.5, 0.5);
                } catch (Exception e) {
                }

                if (ticks % 10 == 0) {
                    w.playSound(bee.getLocation(), Sound.ENTITY_BEE_HURT, 2.0f, 0.5f);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void finalizeDeath() {
        onDeath();

        ExperienceOrb orb = (ExperienceOrb) bee.getWorld().spawnEntity(bee.getLocation(), EntityType.EXPERIENCE_ORB);
        orb.setExperience(3500);

        NamespacedKey killsKey = new NamespacedKey(plugin, "queen_bee_kills");

        Set<UUID> rewardPlayers = new HashSet<>(currentPlayers);
        rewardPlayers.addAll(attackers);

        for (UUID uuid : rewardPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline() || (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE)) continue;

            int kills = p.getPersistentDataContainer().getOrDefault(killsKey, PersistentDataType.INTEGER, 0);
            kills++;
            p.getPersistentDataContainer().set(killsKey, PersistentDataType.INTEGER, kills);

            if (kills == 1) {
                ItemStack bundle = new ItemStack(Material.BUNDLE);
                org.bukkit.inventory.meta.BundleMeta meta = (org.bukkit.inventory.meta.BundleMeta) bundle.getItemMeta();

                ItemStack coins = items.EconomyItems.createVithiumCoin();
                coins.setAmount(20);
                meta.addItem(coins);

                meta.addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 5));

                bundle.setItemMeta(meta);
                giveOrDropItem(p, bundle);

                p.sendMessage(ChatColor.of("#EFDC93") + "¡Has derrotado a la Abeja Reina por primera vez! Se te ha entregado un Bundle de recompensa especial.");

            } else if (kills == 2) {
                ItemStack coins = items.EconomyItems.createVithiumCoin();
                coins.setAmount(15);
                giveOrDropItem(p, coins);

                giveOrDropItem(p, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 3));

                p.sendMessage(ChatColor.of("#EFDC93") + "¡Has derrotado a la Abeja Reina por segunda vez! Recibes tu recompensa.");

            } else {
                ItemStack coins = items.EconomyItems.createVithiumCoin();
                coins.setAmount(10);
                giveOrDropItem(p, coins);

                p.sendMessage(ChatColor.of("#EFDC93") + "¡Has derrotado a la Abeja Reina (" + kills + " veces)! Recibes la recompensa estándar.");
            }

            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
    }

    private void giveOrDropItem(Player p, ItemStack item) {
        HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(item);
        for (ItemStack left : leftover.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), left);
        }
    }

    @EventHandler
    public void onEntityDamageExplosions(EntityDamageEvent e) {
        if (!e.getEntity().equals(bee)) return;

        if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            e.setCancelled(true);
        }
    }

    private void registerAttackTask(BukkitRunnable task) {
        activeAttackTasks.add(task);
    }

}