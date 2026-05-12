package Dificultades.CustomMobs;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class NULLEntity {

    private final JavaPlugin plugin;
    private static final String SCARE_CHAR = "\uEAAA";
    private static final String BOSSBAR_CHAR = "\uEAA5";
    private final Random random = new Random();

    public NULLEntity(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void spawn(Player target, Location location, boolean isJumpscare, boolean isAggressive) {
        Piglin nullMob = (Piglin) location.getWorld().spawnEntity(location, EntityType.PIGLIN);
        setupAttributes(nullMob);

        // Ocultar a otros
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(target.getUniqueId())) {
                online.hideEntity(plugin, nullMob);
            }
        }

        if (isJumpscare) {
            startJumpscareLogic(nullMob, target);
        } else if (isAggressive) {
            startAggressiveStalkerLogic(nullMob, target);
        } else {
            startPassiveLogic(nullMob, target);
        }
    }

    private void setupAttributes(Piglin piglin) {
        piglin.setCustomName(ChatColor.BOLD + "NULL");
        piglin.setAdult();
        piglin.setCustomNameVisible(false);
        piglin.setAI(false);
        piglin.setInvulnerable(true);
        piglin.setSilent(true);
        piglin.setImmuneToZombification(true);
        piglin.setRemoveWhenFarAway(false);
        piglin.setCollidable(false);
        Objects.requireNonNull(piglin.getAttribute(Attribute.GENERIC_SCALE)).setBaseValue(1.3);

        if (piglin.getEquipment() != null) {
            piglin.getEquipment().clear();
            piglin.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
            piglin.getEquipment().setItemInOffHand(new ItemStack(Material.AIR));
        }
    }

    // --- OPCIÓN 1: JUMPSCARE ---
    private void startJumpscareLogic(Piglin mob, Player player) {
        player.playSound(player.getLocation(), "minecraft:custom.null_effect", 10.0f, 1.0f);
        lookAtPlayer(mob, player);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!mob.isValid() || !player.isOnline()) {
                    if (mob.isValid()) mob.remove();
                    this.cancel();
                    return;
                }

                lookAtPlayer(mob, player);

                if (ticks == 0) sendTitleColor(player, ChatColor.GRAY);
                else if (ticks == 10) sendTitleColor(player, ChatColor.WHITE);
                else if (ticks == 20) sendTitleColor(player, ChatColor.GRAY);
                else if (ticks == 30) sendTitleColor(player, ChatColor.WHITE);
                else if (ticks == 40) {
                    player.sendTitle(ChatColor.DARK_RED + SCARE_CHAR, "", 0, 10, 50);
                    mob.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 1, false, false));
                    mob.getWorld().spawnParticle(Particle.SQUID_INK, mob.getLocation().add(0, 1, 0), 20, 0.5, 1, 0.5, 0.1);
                }

                if (ticks >= 42) {
                    mob.remove();
                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // --- OPCIÓN 2: PASIVO ---
    private void startPassiveLogic(Piglin mob, Player player) {
        new BukkitRunnable() {
            int ticksAlive = 0;
            final int MAX_LIFE = 10 * 20; // 10 segundos

            @Override
            public void run() {
                if (!mob.isValid() || !player.isOnline()) {
                    if (mob.isValid()) mob.remove();
                    this.cancel(); return;
                }

                lookAtPlayer(mob, player);

                if (ticksAlive >= MAX_LIFE) {
                    removeWithFade(mob);
                    this.cancel(); return;
                }

                if (isLookingAt(player, mob)) {
                    removeWithFade(mob);
                    player.playSound(mob.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 20.0f, 0.6f);
                    this.cancel(); return;
                }
                ticksAlive += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    // --- OPCIÓN 3: AGRESIVO (Clown V2 MEJORADO) ---
    private void startAggressiveStalkerLogic(Piglin mob, Player player) {
        new BukkitRunnable() {
            int ticksAlive = 0;
            int stareTicks = 0;
            final int MAX_LIFE = 10 * 20; // 10 segundos antes de desaparecer si no se le mira

            boolean attackTriggered = false;
            int attackTimer = 0;

            BossBar tempBar = null;
            final List<Location> fakeAirBlocks = new ArrayList<>();

            @Override
            public void run() {
                // 1. Validaciones de seguridad
                if (!player.isOnline()) {
                    cleanup(); return;
                }
                // Si el mob muere por otra razón antes del ataque
                if (!mob.isValid() && !attackTriggered) {
                    cleanup(); return;
                }

                // 2. LÓGICA DE ATAQUE (Dura exactamente 2 segundos)
                if (attackTriggered) {
                    // Mantener mirada fija CONSTANTE
                    lookAtPlayer(mob, player);

                    // Animación de BossBar (Parpadeo rápido Gris/Blanco)
                    // Como el loop corre cada 5 ticks (0.25s), cambiamos el color en cada ejecución
                    if (attackTimer % 2 == 0) {
                        tempBar.setTitle(ChatColor.GRAY + BOSSBAR_CHAR);
                    } else {
                        tempBar.setTitle(ChatColor.WHITE + BOSSBAR_CHAR);
                    }

                    attackTimer++;

                    if (attackTimer >= 8) {
                        cleanup();
                    }
                    return;
                }

                // 3. LÓGICA DE ACECHO
                lookAtPlayer(mob, player);

                if (ticksAlive >= MAX_LIFE) {
                    removeWithFade(mob);
                    this.cancel();
                    return;
                }

                if (isLookingAt(player, mob)) {
                    stareTicks += 5;
                } else {
                    stareTicks = 0;
                }

                // DETONACIÓN DEL ATAQUE
                if (stareTicks >= 40) {
                    triggerStareAttack(mob, player);
                    attackTriggered = true;
                    // No hacemos return aquí para que entre al siguiente ciclo como attackTriggered
                }

                ticksAlive += 5;
            }

            private void triggerStareAttack(Piglin mob, Player p) {
                // 1. Calcular posición (3 bloques al frente)
                Location pLoc = p.getLocation();
                Vector direction = pLoc.getDirection().normalize().multiply(3);
                Location targetLoc = pLoc.add(direction);

                targetLoc.setY(p.getLocation().getY());

                // 2. Fake Air (X-Ray Effect)
                createFakeAirSpace(p, targetLoc);

                // 3. Teleport y Mirar
                mob.teleport(targetLoc);
                lookAtPlayer(mob, p);

                // 4. Efectos (2 segundos exactos = 40 ticks)
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 10));

                // 5. Sonido
                p.playSound(p.getLocation(), "minecraft:custom.null_effect", 10.0f, 1.0f);
                p.spawnParticle(Particle.SQUID_INK, mob.getEyeLocation(), 30, 0.5, 0.5, 0.5, 0.1);

                // 6. BossBar (Siempre Blanca base, el titulo cambia)
                tempBar = Bukkit.createBossBar(ChatColor.GRAY + BOSSBAR_CHAR, BarColor.WHITE, BarStyle.SOLID);
                tempBar.addPlayer(p);

                // 7. Título Creepy
                String phrase = getRandomCreepyPhrase(p.getName());
                p.sendTitle(phrase, "", 10, 50, 10);
            }

            private void createFakeAirSpace(Player p, Location center) {
                int radius = 1;
                Block centerBlock = center.getBlock();
                for (int x = -radius; x <= radius; x++) {
                    for (int y = 0; y <= 2; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            Block b = centerBlock.getRelative(x, y, z);
                            if (b.getType().isSolid()) {
                                fakeAirBlocks.add(b.getLocation());
                                p.sendBlockChange(b.getLocation(), Material.AIR.createBlockData());
                            }
                        }
                    }
                }
            }

            private void cleanup() {
                if (mob.isValid()) mob.remove();
                if (tempBar != null) tempBar.removeAll();

                // Restaurar bloques
                for (Location loc : fakeAirBlocks) {
                    if (player.isOnline()) {
                        player.sendBlockChange(loc, loc.getBlock().getBlockData());
                    }
                }
                fakeAirBlocks.clear();
                this.cancel();
            }

        }.runTaskTimer(plugin, 0L, 5L); // Ejecuta cada 0.25 segundos
    }

    // --- UTILS ---

    private void lookAtPlayer(Piglin mob, Player player) {
        Location mobEye = mob.getEyeLocation();
        Location targetEye = player.getEyeLocation();
        Vector dir = targetEye.toVector().subtract(mobEye.toVector()).normalize();
        Location newLoc = mob.getLocation();
        newLoc.setDirection(dir);
        mob.teleport(newLoc);
    }

    private String getRandomCreepyPhrase(String playerName) {
        String[] phrases = {
                "Por§kq§rue me ob§ks§rer§kv§ras...?",
                "§l§oNO§r de§kb§rerias d§ke§r es§kt§rar a§kq§ru§ki§r...",
                "Y§ko s§re todo so§kb§rre§l ti",
                "Te ob§ks§rervo.. §4" + playerName,
                "§kP§riensas q§ku§re hay §ku§rna salid§ka§r?",
                "N§kO§r PUE§kD§rES §lES§kC§r§lAPAR§r!",
                "Es§kt§ras c§ka§rvando t§ku§r pr§ko§rpia tu§km§rba..."
        };
        return phrases[random.nextInt(phrases.length)];
    }

    private void sendTitleColor(Player p, ChatColor color) {
        p.sendTitle(color + SCARE_CHAR, "", 0, 15, 5);
    }

    private void removeWithFade(Piglin mob) {
        if (!mob.isDead()) {
            mob.getWorld().spawnParticle(Particle.LARGE_SMOKE, mob.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
            mob.remove();
        }
    }

    private boolean isLookingAt(Player player, Piglin mob) {
        Location eye = player.getEyeLocation();
        Vector toEntity = mob.getEyeLocation().toVector().subtract(eye.toVector());
        return eye.getDirection().normalize().dot(toEntity.normalize()) > 0.97;
    }
}