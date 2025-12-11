package Bosses;

import items.BootNetheriteEssence;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class HellishBeeHandler extends BaseBoss implements Listener {

    public static final Map<UUID, HellishBeeHandler> ACTIVE_BOSSES = new HashMap<>();

    private final Bee bee;
    private final Random random = new Random();
    private final BootNetheriteEssence bootNetheriteEssence;

    // Estados
    private int globalTick = 0;
    private boolean runningSpecial = false;
    private boolean runningMelee = false;
    private boolean inRegenerationPhase = false;

    // Variables de Regeneración
    private int snowballHits = 0;
    private final int REQUIRED_SNOWBALLS = 8;
    private BukkitRunnable regenTask;

    // Control de ataques
    private int meleeDoneSinceLastSpecial = 0;
    private int requiredMeleeBetweenSpecials = 2;

    // Trampas de suelo
    private final List<Location> igneousSpheres = new ArrayList<>();
    private BukkitRunnable sphereTask;

    public HellishBeeHandler(JavaPlugin plugin, Bee bee) {
        super(plugin, bee);
        this.bee = bee;
        this.bootNetheriteEssence = new BootNetheriteEssence(plugin);

        ACTIVE_BOSSES.put(bee.getUniqueId(), this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ==============================
    //        SPAWN
    // ==============================

    public static HellishBeeHandler spawn(JavaPlugin plugin, Location center) {
        World world = center.getWorld();
        if (world == null) return null;

        Bee bee = world.spawn(center, Bee.class, b -> {
            b.setCustomName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Abeja Infernal");
            b.setCustomNameVisible(true);
            b.setRemoveWhenFarAway(false);
            b.setAnger(999999);
            b.setAI(true);

            Objects.requireNonNull(b.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(2000);
            b.setHealth(2000);

            Objects.requireNonNull(b.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.45);
            Objects.requireNonNull(b.getAttribute(Attribute.GENERIC_FOLLOW_RANGE)).setBaseValue(80);
            Objects.requireNonNull(b.getAttribute(Attribute.GENERIC_SCALE)).setBaseValue(3.5);

            b.setHasStung(false);
            b.setCannotEnterHiveTicks(Integer.MAX_VALUE);

            b.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            b.setVisualFire(false);
        });

        HellishBeeHandler handler = new HellishBeeHandler(plugin, bee);
        handler.start();
        return handler;
    }

    // ==============================
    //      OVERRIDES BASEBOSS
    // ==============================

    @Override
    protected String getBossTitle() {
        return ChatColor.DARK_RED + "Abeja Infernal";
    }

    @Override
    protected int getArenaRadius() { return 60; }

    @Override
    protected int getArenaHeightUp() { return 30; }

    @Override
    protected int getArenaHeightDown() { return 3; }

    @Override
    protected AreaZone.Shape getArenaShape() { return AreaZone.Shape.CIRCULAR; }

    @Override
    protected void onStart() {
        this.mainBar.setColor(BarColor.RED);
        this.mainBar.setStyle(BarStyle.SEGMENTED_10);
        startIgneousSpheresLoop();
    }

    @Override
    protected void onTick() {
        globalTick++;
        bee.setHasStung(false);
        bee.setCannotEnterHiveTicks(Integer.MAX_VALUE);
        bee.setAnger(999999);

        if (inRegenerationPhase) {
            // Forzar posición central durante regeneración
            bee.teleport(spawnLocation.clone().add(0, 5, 0));
            bee.setTarget(null);
            return;
        }

        if (runningSpecial || runningMelee) return;

        double max = Objects.requireNonNull(bee.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
        double hp = bee.getHealth();
        boolean enraged = hp < max / 2.0;
        int attackDelay = enraged ? 20 : 35;

        if (globalTick % attackDelay == 0) {
            if (enraged && regenTask == null) {
                // 10% Chance de Regeneración si < 50% HP
                if (random.nextDouble() < 0.8) {
                    startRegenerationPhase();
                    return;
                }
            }
            decideNextAttack();
        }
    }

    @Override
    protected void onDeath() {
        playSoundGlobal(Sound.ENTITY_WITHER_DEATH, 1f, 0.5f);

        int drops = random.nextInt(3) + 2;
        for (int i = 0; i < drops; i++) {
            bee.getWorld().dropItemNaturally(bee.getLocation(), bootNetheriteEssence.createBootNetheriteEssence());
        }

        if (regenTask != null) regenTask.cancel();
        if (sphereTask != null) sphereTask.cancel();

        for(Location l : igneousSpheres) {
            l.getWorld().spawnParticle(Particle.SMOKE, l, 10);
        }
        igneousSpheres.clear();

        ACTIVE_BOSSES.remove(bee.getUniqueId());
    }

    // ==============================
    //      SISTEMA DE ATAQUES
    // ==============================

    private void decideNextAttack() {
        if (currentPlayers.isEmpty()) return;

        if (meleeDoneSinceLastSpecial < requiredMeleeBetweenSpecials) {
            startRandomMelee();
            meleeDoneSinceLastSpecial++;
        } else {
            startRandomSpecial();
            meleeDoneSinceLastSpecial = 0;
            requiredMeleeBetweenSpecials = random.nextInt(2) + 1;
        }
    }

    private void startRandomMelee() {
        runningMelee = true;
        if (random.nextBoolean()) meleeInfernalDash();
        else meleeInfernalTeleport();
    }

    // MELEE 1: DASH
    private void meleeInfernalDash() {
        Player target = getNearestPlayer();
        if (target == null) { runningMelee = false; return; }

        playSoundGlobal(Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f);
        bee.getWorld().spawnParticle(Particle.FLAME, bee.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);

        Vector dir = target.getLocation().subtract(bee.getLocation()).toVector().normalize().multiply(1.5);
        bee.setVelocity(dir);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!bee.isValid() || t++ > 20) {
                    runningMelee = false;
                    cancel();
                    return;
                }
                bee.getWorld().spawnParticle(Particle.FLAME, bee.getLocation(), 5, 0.2, 0.2, 0.2, 0.01);

                if (bee.getLocation().distance(target.getLocation()) < 2.5) {
                    target.damage(12.0, bee);
                    target.setFireTicks(100);
                    target.setVelocity(dir.normalize().multiply(1.2).setY(0.5));
                    playSoundGlobal(Sound.ENTITY_GENERIC_EXPLODE, 1f, 2f);
                    runningMelee = false;
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // MELEE 2: TP + GOLPE
    private void meleeInfernalTeleport() {
        Player target = getNearestPlayer();
        if (target == null) { runningMelee = false; return; }

        teleportWithEffects(bee.getLocation(), target.getLocation().add(0, 1.5, 0));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (bee.isValid() && bee.getLocation().distance(target.getLocation()) < 4) {
                    target.damage(15.0, bee);
                    target.setFireTicks(60);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1));
                    bee.getWorld().spawnParticle(Particle.LAVA, bee.getLocation(), 15);
                    playSoundGlobal(Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 1f, 0.8f);
                }
                runningMelee = false;
            }
        }.runTaskLater(plugin, 15L);
    }

    // --- ATAQUES ESPECIALES ---

    private enum SpecialAttack {
        PARALYSIS, HELL_CIRCLES, FIRE_RAYS, MAGMA_BARRAGE, REINFORCEMENTS
    }

    private void startRandomSpecial() {
        runningSpecial = true;
        SpecialAttack attack = SpecialAttack.values()[random.nextInt(SpecialAttack.values().length)];

        switch (attack) {
            case PARALYSIS -> specialParalysis();
            case HELL_CIRCLES -> specialHellCircles();
            case FIRE_RAYS -> specialFireRays();
            case MAGMA_BARRAGE -> specialMagmaBarrage();
            case REINFORCEMENTS -> specialReinforcements();
        }
    }

    // 1. PARALISIS + METEOROS
    private void specialParalysis() {
        playSoundGlobal(Sound.AMBIENT_BASALT_DELTAS_MOOD, 2f, 0.5f);

        BossBar paralysisBar = Bukkit.createBossBar(ChatColor.DARK_RED + "\uEAA5", BarColor.WHITE, BarStyle.SOLID);
        paralysisBar.setVisible(true);

        for (UUID id : currentPlayers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                paralysisBar.addPlayer(p);
                p.sendTitle(ChatColor.RED + "¡PARÁLISIS!", ChatColor.GOLD + "Cúbrete con el escudo", 5, 40, 10);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 10));
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                p.spawnParticle(Particle.LAVA, p.getLocation(), 30);
            }
        }

        // Lluvia de Meteoros desde el techo
        new BukkitRunnable() {
            int wave = 0;
            @Override
            public void run() {
                if (!bee.isValid() || bee.isDead() || wave++ > 10) {
                    paralysisBar.removeAll();
                    runningSpecial = false;
                    cancel();
                    return;
                }

                for(int i=0; i<3; i++) { // 3 meteoros por oleada
                    double rx = (random.nextDouble() * getArenaRadius() * 2) - getArenaRadius();
                    double rz = (random.nextDouble() * getArenaRadius() * 2) - getArenaRadius();

                    // Spawn en el techo de la arena
                    Location spawn = areaZone.getCenter().clone().add(rx, getArenaHeightUp(), rz);

                    LargeFireball fireball = bee.getWorld().spawn(spawn, LargeFireball.class);
                    fireball.setDirection(new Vector(0, -2, 0)); // Cae rápido hacia abajo
                    fireball.setYield(4f);
                    fireball.setIsIncendiary(true);
                    fireball.setShooter(bee);
                    fireball.setMetadata("BOSS_PROJECTILE", new FixedMetadataValue(plugin, true));

                    spawn.getWorld().playSound(spawn, Sound.ENTITY_GHAST_SHOOT, 5f, 0.5f);
                }
            }
        }.runTaskTimer(plugin, 20L, 10L);
    }

    // 2. CIRCULO INFERNAL (3 Aros de fuego densos)
    private void specialHellCircles() {
        Location center = spawnLocation.clone().add(0, 1, 0);
        teleportWithEffects(bee.getLocation(), center.clone().add(0, 4, 0));
        playSoundGlobal(Sound.ITEM_GOAT_HORN_SOUND_7, 2f, 0.8f);

        new BukkitRunnable() {
            int ringCount = 0;
            @Override
            public void run() {
                if (ringCount >= 3) {
                    runningSpecial = false;
                    cancel();
                    return;
                }
                spawnExpandingFireRing(center);
                ringCount++;
            }
        }.runTaskTimer(plugin, 40L, 30L);
    }

    private void spawnExpandingFireRing(Location center) {
        new BukkitRunnable() {
            double r = 0;

            @Override
            public void run() {
                if (r > getArenaRadius()) { cancel(); return; }

                for (double ang = 0; ang < 360; ang += 10) {
                    double rad = Math.toRadians(ang);

                    double x = Math.cos(rad) * r;
                    double z = Math.sin(rad) * r;

                    Location l = center.clone().add(x, 0.5, z);

                    l.getWorld().spawnParticle(Particle.FLAME, l, 1, 0, 0, 0, 0.03);
                    l.getWorld().spawnParticle(Particle.SMOKE, l, 1, 0, 0, 0, 0);
                }

                playSoundGlobal(Sound.BLOCK_FIRE_AMBIENT, 0.5f, 2f);

                for (UUID id : currentPlayers) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null && p.getWorld().equals(center.getWorld())) {

                        double dist = Math.sqrt(Math.pow(p.getLocation().getX() - center.getX(), 2) +
                                Math.pow(p.getLocation().getZ() - center.getZ(), 2));

                        double distY = Math.abs(p.getLocation().getY() - center.getY());

                        if (Math.abs(dist - r) < 1.5 && distY < 2.0) {
                            p.damage(12.0, bee);
                            p.setFireTicks(100);
                            Vector knockback = p.getLocation().toVector().subtract(center.toVector()).normalize().multiply(0.4).setY(0.2);
                            p.setVelocity(knockback);
                        }
                    }
                }

                r += 0.8;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // 3. BOMBAS DE MAGMA (Estilo Stings de QueenBee: 2 Olas de 8 proyectiles en anillo)
    private void specialMagmaBarrage() {
        Location center = spawnLocation.clone().add(0, 4, 0);
        teleportWithEffects(bee.getLocation(), center);
        playSoundGlobal(Sound.ENTITY_BLAZE_SHOOT, 2f, 0.5f);

        new BukkitRunnable() {
            int wave = 0; // 2 ráfagas
            @Override
            public void run() {
                if (wave >= 2 || !bee.isValid()) {
                    runningSpecial = false;
                    cancel();
                    return;
                }
                launchMagmaWave();
                wave++;
            }
        }.runTaskTimer(plugin, 10L, 30L);
    }

    private void launchMagmaWave() {
        World w = bee.getWorld();
        Location origin = bee.getLocation().clone().add(0, 1.5, 0);

        List<Player> players = new ArrayList<>();
        for(UUID id : currentPlayers) {
            Player p = Bukkit.getPlayer(id);
            if(p != null) players.add(p);
        }
        Collections.shuffle(players);

        int count = 8;
        int targetCount = Math.min(players.size(), count);

        List<Vector> baseDirs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;

            baseDirs.add(new Vector(Math.cos(angle), -0.2, Math.sin(angle)).normalize().multiply(0.65));
        }

        w.playSound(origin, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.4f, 0.6f);

        for (int i = 0; i < count; i++) {
            Vector dir;
            if (i < targetCount) {
                Player target = players.get(i);
                dir = target.getLocation().add(0, 1, 0)
                        .toVector().subtract(origin.toVector())
                        .normalize().multiply(0.7);
            } else {
                dir = baseDirs.get(i);
            }

            BlockDisplay bomb = w.spawn(origin, BlockDisplay.class);
            bomb.setBlock(Bukkit.createBlockData(Material.MAGMA_BLOCK));
            bomb.setGlowing(true);
            bomb.setGlowColorOverride(Color.RED);

            new BukkitRunnable() {
                int life = 0;
                @Override
                public void run() {
                    if (!bomb.isValid() || life++ > 80) {
                        if (bomb.isValid()) {
                            explodeMagmaBomb(bomb.getLocation());
                            bomb.remove();
                        }
                        cancel();
                        return;
                    }

                    Location next = bomb.getLocation().add(dir);

                    // Colisión con bloques o salirse de arena
                    if (next.getBlock().getType().isSolid() || !areaZone.isInside(next)) {
                        explodeMagmaBomb(next);
                        bomb.remove();
                        cancel();
                        return;
                    }

                    // Colisión con jugadores
                    for (Entity e : next.getWorld().getNearbyEntities(next, 1, 1, 1)) {
                        if (e instanceof Player p && currentPlayers.contains(p.getUniqueId())) {
                            explodeMagmaBomb(next);
                            p.damage(8.0, bee);
                            p.setFireTicks(100);
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                            bomb.remove();
                            cancel();
                            return;
                        }
                    }

                    bomb.teleport(next);
                    next.getWorld().spawnParticle(Particle.FLAME, next, 1, 0, 0, 0, 0);
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    private void explodeMagmaBomb(Location loc) {
        loc.getWorld().createExplosion(loc, 4f, true, false, bee);
        loc.getWorld().spawnParticle(Particle.LAVA, loc, 10);
    }

    // 4. RAYOS DE FUEGO
    private void specialFireRays() {
        Location center = spawnLocation.clone().add(0, 5, 0);
        teleportWithEffects(bee.getLocation(), center);
        playSoundGlobal(Sound.BLOCK_BEACON_ACTIVATE, 2f, 0.5f);

        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick++ > 60) {
                    runningSpecial = false;
                    cancel();
                    return;
                }

                for (UUID id : currentPlayers) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) {
                        Location target = p.getEyeLocation().subtract(0, 0.5, 0);
                        Location start = bee.getLocation();
                        Vector dir = target.toVector().subtract(start.toVector()).normalize();

                        double dist = start.distance(target);
                        for (double d = 0; d < dist; d += 0.5) {
                            Location point = start.clone().add(dir.clone().multiply(d));
                            point.getWorld().spawnParticle(Particle.DUST, point, 1,
                                    new Particle.DustOptions(Color.ORANGE, 1));
                            if (d % 1.0 == 0) {
                                point.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0, 0, 0, 0);
                            }
                        }

                        if (tick % 5 == 0) {
                            if (p.isHandRaised() && p.getInventory().getItemInMainHand().getType().name().contains("SHIELD")) {
                                p.playSound(p.getLocation(), Sound.ITEM_SHIELD_BREAK, 1f, 1f);
                                ItemStack shield = p.getInventory().getItemInMainHand();
                                shield.setDurability((short) (shield.getDurability() + 5));
                                p.setCooldown(Material.SHIELD, 20);
                            } else {
                                p.damage(15.0, bee);
                                p.setFireTicks(60);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 1L);
    }

    // 5. REFUERZOS (Estilo QueenBee: Invocación con efecto)
    private void specialReinforcements() {
        int count = random.nextInt(7) + 6;
        playSoundGlobal(Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 2f, 0.5f);
        playSoundGlobal(Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 1f, 0.5f);

        for (int i = 0; i < count; i++) {
            double dx = random.nextDouble() * getArenaRadius() * 2 - getArenaRadius();
            double dz = random.nextDouble() * getArenaRadius() * 2 - getArenaRadius();

            // Buscar el suelo en esa coordenada (respetando la altura de la arena)
            Location floorCenter = areaZone.getCenter().clone();
            double floorY = floorCenter.getY() - getArenaHeightDown();

            Location spawnLoc = new Location(floorCenter.getWorld(), floorCenter.getX() + dx, floorY + 1, floorCenter.getZ() + dz);

            int type = random.nextInt(4);

            // EFECTO DE INVOCACION (Espiral subiendo)
            new BukkitRunnable() {
                int y = 0;
                @Override
                public void run() {
                    if (y++ > 20) {
                        spawnMob(spawnLoc, type);
                        cancel();
                        return;
                    }

                    double angle = y * 0.5;
                    double radius = 1.0;
                    double px = Math.cos(angle) * radius;
                    double pz = Math.sin(angle) * radius;

                    spawnLoc.getWorld().spawnParticle(Particle.FLAME, spawnLoc.clone().add(px, y * 0.1, pz), 1, 0, 0, 0, 0);
                    spawnLoc.getWorld().spawnParticle(Particle.SMOKE, spawnLoc.clone().add(-px, y * 0.1, -pz), 1, 0, 0, 0, 0);
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }

        new BukkitRunnable() {
            @Override public void run() { runningSpecial = false; }
        }.runTaskLater(plugin, 40L);
    }

    private void spawnMob(Location loc, int type) {
        loc.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 0.5f);
        switch (type) {
            case 0 -> {
                Bee b = loc.getWorld().spawn(loc, Bee.class);
                b.setCustomName(ChatColor.GOLD + "Nether Corrupted Bee");
                b.setVisualFire(true);
                b.setTarget(getNearestPlayer());
            }
            case 1 -> {
                Spider s = loc.getWorld().spawn(loc, Spider.class);
                s.setCustomName(ChatColor.RED + "Infernal Spider");
                s.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 9999, 2));
                s.setVisualFire(true);
            }
            case 2 -> {
                Vex v = loc.getWorld().spawn(loc, Vex.class);
                v.setCustomName(ChatColor.DARK_PURPLE + "Netherite Vex");
            }
            case 3 -> {
                Blaze z = loc.getWorld().spawn(loc, Blaze.class);
                z.setCustomName(ChatColor.GOLD + "Guardian Blaze");
            }
        }
    }

    // ==============================
    //      REGENERACION (Aura Espiral + 8 Golpes)
    // ==============================

    private void startRegenerationPhase() {
        if (inRegenerationPhase) return;
        inRegenerationPhase = true;
        runningMelee = false;
        runningSpecial = false;

        snowballHits = 0;

        Location center = spawnLocation.clone().add(0, 5, 0);
        teleportWithEffects(bee.getLocation(), center);

        bee.setAI(false);
        bee.setGravity(false);

        playSoundGlobal(Sound.BLOCK_PORTAL_TRIGGER, 1f, 0.5f);
        Bukkit.broadcastMessage(ChatColor.RED + "¡La Abeja Infernal se regenera! ¡Golpéala 8 VECES con Bolas de Nieve!");

        regenTask = new BukkitRunnable() {
            int tick = 0;
            double angle = 0;

            @Override
            public void run() {
                if (!bee.isValid() || bee.isDead()) { cancel(); return; }

                bee.teleport(center);
                bee.setVelocity(new Vector(0,0,0));

                drawSpiralAura(center, tick);

                if (tick % 40 == 0 && tick > 0) {
                    double max = bee.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                    double newHp = Math.min(max, bee.getHealth() + 20);
                    bee.setHealth(newHp);

                    center.getWorld().spawnParticle(Particle.HEART, center.clone().add(0,2,0), 5);
                    playSoundGlobal(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.5f);

                    if (newHp >= max) {
                        stopRegeneration();
                        cancel();
                    }
                }
                tick++;
            }
        };
        regenTask.runTaskTimer(plugin, 0L, 1L);
    }

    private void drawSpiralAura(Location center, int tick) {
        double height = 4.0;
        double radius = 2.0;

        double currentY = (tick % 40) / 10.0;

        double angle = tick * 0.10;

        for(int i=0; i<3; i++) {
            double y = currentY + (i * 0.5);
            if (y > height) y -= height;

            double x = Math.cos(angle + y) * radius;
            double z = Math.sin(angle + y) * radius;

            Location p = center.clone().add(x, y - 1.2, z);

            Color c = (i == 0) ? Color.RED : (i == 1) ? Color.ORANGE : Color.YELLOW;

            p.getWorld().spawnParticle(Particle.DUST, p, 1, new Particle.DustOptions(c, 1.5f));

            if (i % 2 == 0) {
                p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0, 0, 0, 0);
            }
        }
    }

    private void stopRegeneration() {
        if (!inRegenerationPhase) return;
        inRegenerationPhase = false;

        if (regenTask != null) regenTask.cancel();
        regenTask = null;

        bee.setAI(true);
        bee.setGravity(true);

        playSoundGlobal(Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1f, 0.5f);
        bee.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, bee.getLocation(), 1);
        globalTick = 0;
    }

    // ==============================
    //      ESFERAS IGNEAS
    // ==============================

    private void startIgneousSpheresLoop() {
        sphereTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!bee.isValid() || bee.isDead()) { cancel(); return; }

                if (igneousSpheres.size() < 10 && globalTick % 60 == 0) {
                    Player p = getNearestPlayer();
                    if (p != null) {
                        double rx = (random.nextDouble() * getArenaRadius() * 2) - getArenaRadius();
                        double rz = (random.nextDouble() * getArenaRadius() * 2) - getArenaRadius();

                        Location floorCenter = areaZone.getCenter().clone();
                        double floorY = floorCenter.getY() - getArenaHeightDown();

                        Location loc = new Location(floorCenter.getWorld(), floorCenter.getX() + rx, floorY, floorCenter.getZ() + rz);
                        igneousSpheres.add(loc);
                    }
                }

                Iterator<Location> it = igneousSpheres.iterator();
                while (it.hasNext()) {
                    Location loc = it.next();
                    drawHalfSphere(loc);

                    for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                        if (e instanceof Player p && currentPlayers.contains(p.getUniqueId())) {
                            p.damage(10.0, bee);
                            p.setFireTicks(100);
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2));
                        }
                    }
                    if (random.nextDouble() < 0.8) it.remove();
                }
            }
        };
        sphereTask.runTaskTimer(plugin, 0L, 5L);
    }

    private void drawHalfSphere(Location center) {
        for (double phi = 0; phi <= Math.PI / 2; phi += Math.PI / 6) {
            double r = 1.5 * Math.sin(phi);
            double y = 1.5 * Math.cos(phi);
            for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / 6) {
                double x = r * Math.cos(theta);
                double z = r * Math.sin(theta);
                center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(x, y, z), 1, 0, 0, 0, 0);
            }
        }
    }

    // ==============================
    //        EVENTOS Y UTILIDADES
    // ==============================

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (!e.getEntity().equals(bee)) return;

        if (inRegenerationPhase) {
            if (e.getDamager() instanceof Snowball) {
                e.setCancelled(false);

                snowballHits++;
                bee.getWorld().playSound(bee.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1.5f + (snowballHits * 0.1f));
                bee.getWorld().spawnParticle(Particle.CRIT, bee.getLocation(), 10);

                for(UUID id : currentPlayers) {
                    Player p = Bukkit.getPlayer(id);
                }

                if (snowballHits >= REQUIRED_SNOWBALLS) {
                    stopRegeneration();
                    bee.damage(10);
                    playSoundGlobal(Sound.BLOCK_GLASS_BREAK, 1f, 0.6f);
                }
            } else {
                e.setCancelled(true);
            }
            return;
        }

        Entity damager = e.getDamager();
        if (damager instanceof Projectile) {
            e.setDamage(e.getDamage() * 0.5);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if (!e.getEntity().equals(bee)) return;

        switch (e.getCause()) {
            case FIRE: case FIRE_TICK: case LAVA: case HOT_FLOOR:
            case MELTING: case ENTITY_EXPLOSION: case BLOCK_EXPLOSION:
                e.setCancelled(true);
                return;
            default: break;
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        if (e.getEntity().equals(bee) || e.getEntity().hasMetadata("BOSS_PROJECTILE") || e.getEntity().hasMetadata("BOSS_METEOR")) {
            e.blockList().clear();
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        if (e.getEntity() instanceof LargeFireball fireball && fireball.hasMetadata("BOSS_PROJECTILE")) {
        }
    }

    private Player getNearestPlayer() {
        Player near = null;
        double minD = Double.MAX_VALUE;
        for (UUID id : currentPlayers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.getWorld().equals(bee.getWorld())) {
                double d = p.getLocation().distanceSquared(bee.getLocation());
                if (d < minD) { minD = d; near = p; }
            }
        }
        return near;
    }

    private void teleportWithEffects(Location from, Location to) {
        createFireSphere(from);
        playSoundGlobal(Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);

        new BukkitRunnable() {
            public void run() {
                bee.teleport(to);
                createFireSphere(to);
                playSoundGlobal(Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);
            }
        }.runTaskLater(plugin, 5L);
    }

    private void createFireSphere(Location center) {
        for (double phi = 0; phi <= Math.PI; phi += Math.PI / 10) {
            double r = 2.5 * Math.sin(phi);
            double y = 2.5 * Math.cos(phi);
            for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / 10) {
                double x = r * Math.cos(theta);
                double z = r * Math.sin(theta);
                center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(x, y, z), 1, 0, 0, 0, 0);
            }
        }
    }

    private void playSoundGlobal(Sound s, float vol, float pitch) {
        for (UUID id : currentPlayers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.playSound(p.getLocation(), s, vol, pitch);
        }
    }
}