package Bosses;

import items.EmblemItems;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.*;

public class QueenBeeHandler extends BaseBoss implements Listener {

    // Usado por /debugarena
    public static final Map<UUID, QueenBeeHandler> ACTIVE_BOSSES = new HashMap<>();

    private final Bee bee;
    private final Random random = new Random();

    // Estados
    private int globalTick = 0;
    private boolean runningSpecial = false;
    private boolean runningMelee = false;
    private boolean inRegenerationPhase = false;

    private int meleeDoneSinceLastSpecial = 0;
    private int requiredMeleeBetweenSpecials = 1;

    // Curación
    private final List<Bee> healTotems = new ArrayList<>();
    private BukkitRunnable regenTask;

    private final List<BukkitRunnable> activeAttackTasks = new ArrayList<>();


    public QueenBeeHandler(JavaPlugin plugin, Bee bee) {
        super(plugin, bee);
        this.bee = bee;

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

            Objects.requireNonNull(b.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(800);
            Objects.requireNonNull(b.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.35);
            Objects.requireNonNull(b.getAttribute(Attribute.GENERIC_FOLLOW_RANGE)).setBaseValue(50);
            Objects.requireNonNull(b.getAttribute(Attribute.GENERIC_SCALE)).setBaseValue(3);

            b.setHealth(800);
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
        return 50; // 100x100
    }

    @Override
    protected int getArenaHeightUp() {
        return 30;
    }

    @Override
    protected int getArenaHeightDown() {
        return 5;
    }

    @Override
    protected AreaZone.Shape getArenaShape() {
        return AreaZone.Shape.SQUARE;
    }

    @Override
    protected void onStart() {
        requiredMeleeBetweenSpecials = random.nextInt(3) + 1; // 1–3
    }

    @Override
    protected void onTick() {
        globalTick++;

        bee.setHasStung(false);
        bee.setCannotEnterHiveTicks(Integer.MAX_VALUE);
        bee.setAnger(999999);

        // Si está en regeneración, forzamos posición y cancelamos lógica
        if (inRegenerationPhase) {
            // Seguridad extra para evitar que se mueva si el runnable falla un tick
            bee.setTarget(null);
            return;
        }

        // Si está ejecutando otro ataque, no hacemos nada
        if (runningSpecial || runningMelee) return;

        double max = Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
        double hp = bee.getHealth();

        boolean enraged = hp < max / 2.0;
        int attackDelay = enraged ? 15 : 30;

        if (globalTick % attackDelay == 0) {
            // AQUI está el cambio: La regeneración ahora "intercepta" el ataque
            if (enraged && regenTask == null) {
                // 10% de probabilidad de cancelar el ataque y entrar en regeneración
                if (random.nextDouble() < 0.10) {
                    startRegenerationPhase();
                    return; // Importante: Cancelamos el ataque que iba a ocurrir
                }
            }

            decideNextAttack();
        }
    }

    @Override
    protected void onDeath() {
        int players = currentPlayers.size();
        int drops = (int) Math.ceil(players / 2.0);
        if (drops < 1) drops = 1;

        for (int i = 0; i < drops; i++) {
            bee.getWorld().dropItemNaturally(bee.getLocation(), EmblemItems.createAgujonReal());
        }

        if (regenTask != null) regenTask.cancel();
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
            if (p != null && p.isOnline() && p.getWorld().equals(bee.getWorld())) {
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
        if (getActivePlayers().isEmpty()) return;

        if (meleeDoneSinceLastSpecial < requiredMeleeBetweenSpecials) {
            startRandomMelee();
            meleeDoneSinceLastSpecial++;
        } else {
            startRandomSpecial();
            meleeDoneSinceLastSpecial = 0;
            requiredMeleeBetweenSpecials = random.nextInt(3) + 1; // 1–3 otra vez
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
                    target.damage(5.0, bee);
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
                    target.damage(7.0, bee);
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
                    target.damage(4.0, bee);
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

        // Direcciones base en círculo
        List<Vector> baseDirs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            baseDirs.add(new Vector(Math.cos(angle), -0.1, Math.sin(angle)).normalize().multiply(0.4));
        }

        // Jugadores únicos aleatorios para dirigirles 1 aguijón
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
                            p.damage(8.0, bee);
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

    // Esfera venenosa en el suelo + sonido constante
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
                    w.spawnParticle(Particle.DUST, l, 1,
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
                            p.damage(6.0, bee);
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
                p.damage(4.0, bee);
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

        int count = 7 + random.nextInt(8); // 7–14

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
                    Bee minion = w.spawn(spawnLoc, Bee.class);
                    minion.setCustomName(ChatColor.LIGHT_PURPLE + "Corrupted Bee");
                    minion.setAnger(9999);
                    minion.setCannotEnterHiveTicks(Integer.MAX_VALUE);

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

        Location center = spawnLocation.clone().add(0, 4, 0);
        teleportWithVisual(bee.getLocation(), center);
        bee.teleport(center);

        w.playSound(center, Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 0.6f);

        double[] yOffsets = {-3, 0, 4};

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
                        w.spawnParticle(Particle.DUST, l, 1,
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

        // Cancelar cualquier ataque en curso por seguridad
        runningSpecial = false;
        runningMelee = false;

        World w = bee.getWorld();
        // Punto central (asegurate que spawnLocation esté bien definido)
        Location center = spawnLocation.clone().add(0, 4, 0);

        bee.teleport(center);
        bee.setAI(false);
        bee.setTarget(null);
        bee.setVelocity(new Vector(0, 0, 0));
        bee.setGravity(false);
        // Efecto visual de escudo o regeneración
        w.playSound(center, Sound.ITEM_TOTEM_USE, 1f, 0.5f);

        healTotems.clear();
        double radius = 8; // Un poco más separado para que sea visible

        // --- Spawnear los 4 Tótems ---
        for (int i = 0; i < 4; i++) {
            Location l = center.clone().add(
                    Math.cos(i * Math.PI / 2) * radius,
                    -2, // Un poco más abajo que la abeja
                    Math.sin(i * Math.PI / 2) * radius
            );

            // Spawnear totem (Bee falsa)
            Bee hive = w.spawn(l, Bee.class, b -> {
                b.setCustomName(ChatColor.GOLD + "§lHEAL TOTEM");
                b.setCustomNameVisible(false);
                b.setAI(false);
                b.setGravity(false);
                b.setSilent(true);
                b.setGlowing(true);
                b.setRemoveWhenFarAway(false);
                // Darle algo de vida para que no mueran de 1 golpe (opcional)
                Objects.requireNonNull(b.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(4);
                b.setHealth(4);
            });

            applyGlow(hive); // Tu método de glow team
            healTotems.add(hive);

            w.spawnParticle(Particle.END_ROD, l, 20, 0.5, 0.5, 0.5, 0.05);
        }

        // --- Tarea de Regeneración ---
        regenTask = new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                // 1. Validaciones de existencia
                if (!bee.isValid() || bee.isDead()) {
                    cancel();
                    return;
                }

                // 2. Mantener al Boss INMOVIL en el centro
                if (bee.getLocation().distanceSquared(center) > 1) {
                    bee.teleport(center);
                }
                bee.setAI(false);
                bee.setVelocity(new Vector(0,0,0));

                // 3. Verificar tótems vivos
                long alive = healTotems.stream().filter(Bee::isValid).count();

                // CONDICION DE SALIDA 1: No quedan tótems
                if (alive == 0) {
                    finishRegenerationPhase();
                    cancel();
                    return;
                }

                // 4. Efectos visuales (Rayos hacia la abeja)
                for (Bee totem : healTotems) {
                    if (totem.isValid()) {
                        drawBeam(totem.getLocation(), bee.getLocation().add(0, 0.5, 0), Color.PURPLE);
                    }
                }

                // 5. Lógica de Curación (Cada 40 ticks = 2 segundos)
                if (t % 40 == 0 && t > 0) {
                    double max = Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
                    double current = bee.getHealth();

                    // Calculo: 1 totem = 10 vida, 4 totems = 40 vida
                    double healAmount = alive * 10.0;

                    double newHealth = Math.min(max, current + healAmount);
                    bee.setHealth(newHealth);

                    // Feedback visual y sonoro
                    w.playSound(bee.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f);
                    w.spawnParticle(Particle.HEART, bee.getLocation().add(0, 1, 0), 10, 1, 1, 1);

                    // Mensaje actionbar opcional
                    for(Player p : currentPlayers.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList()) {
                        p.sendActionBar("§d§lLA REINA SE REGENERA: §a+" + (int)healAmount + " HP");
                    }

                    // CONDICION DE SALIDA 2: Vida al 100% (max)
                    // EL ERROR ANTERIOR ESTABA AQUI (usabas 'half' en lugar de 'max')
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

        if (regenTask != null && !regenTask.isCancelled()) {
            regenTask.cancel();
        }
        regenTask = null;

        // Eliminar tótems restantes
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
            w.spawnParticle(Particle.DUST, loc, 1,
                    new Particle.DustOptions(color, 1.1f));
            loc.add(step);
        }
    }

    private void applyGlow(Entity e) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam("bee_heal_totem");

        if (team == null) {
            team = board.registerNewTeam("bee_heal_totem");
            team.setColor(ChatColor.YELLOW);
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

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        Entity damaged = e.getEntity();
        Entity damager = e.getDamager();

        // Abeja invulnerable SOLO durante regeneración
        if (damaged.equals(bee) && inRegenerationPhase) {
            e.setCancelled(true);
            return;
        }

        // Proyectiles hacen menos daño fuera de regeneración
        if (damaged.equals(bee)) {
            if (damager instanceof Projectile) {
                e.setDamage(e.getDamage() * 0.5);
            }
            return;
        }

        // Daño a tótems
        if (damaged instanceof Bee hive && healTotems.contains(hive)) {

            double hp = hive.getHealth() - e.getFinalDamage();

            if (hp <= 0) {
                breakHealTotem(hive, damager instanceof Player p ? p : null);
            } else {
                hive.setHealth(hp);
                hive.getWorld().playSound(hive.getLocation(),
                        Sound.BLOCK_BEEHIVE_SHEAR, 0.7f, 1.2f);
            }

            e.setCancelled(true);
        }
    }

    // Abeja inmune a explosiones (incluyendo sus propias)
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