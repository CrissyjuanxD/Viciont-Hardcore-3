package Dificultades.Features;

import Dificultades.CustomMobs.Null_Statue;
import items.InfestedCaveItems;
import net.md_5.bungee.api.ChatColor; // Importante para colores HEX
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class Null_Runes implements Listener {

    private final JavaPlugin plugin;

    // Configuración
    private static final double CAPTURE_RADIUS = 2.5;
    private static final double TOTAL_SECONDS = 30.0;
    private static final double PROGRESS_PER_TICK = 1.0 / (TOTAL_SECONDS * 20); // 100% en 30s
    private static final double DECAY_PER_TICK = PROGRESS_PER_TICK; // Baja a la MISMA velocidad

    // Mapa de sesiones activas por jugador
    private final Map<UUID, RuneSession> sessions = new HashMap<>();

    public Null_Runes(JavaPlugin plugin) {
        this.plugin = plugin;
        startLogicLoop();
    }

    private void startLogicLoop() {
        new BukkitRunnable() {
            final Set<UUID> statuesRenderedThisTick = new HashSet<>();

            @Override
            public void run() {
                statuesRenderedThisTick.clear();

                for (Player p : Bukkit.getOnlinePlayers()) {
                    processPlayer(p, statuesRenderedThisTick);
                }
            }
        }.runTaskTimer(plugin, 20L, 1L);
    }

    private void processPlayer(Player p, Set<UUID> statuesRendered) {
        // 1. RESTRICCIÓN: Ignorar Creativo y Espectador
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) {
            // Si tenía una sesión activa, limpiarla para que desaparezca la barra
            if (sessions.containsKey(p.getUniqueId())) {
                sessions.remove(p.getUniqueId()).cleanup();
            }
            return;
        }

        // Buscar estatua cercana en radio pequeño (2 bloques)
        ArmorStand nearbyStatue = null;
        for (Entity ent : p.getNearbyEntities(CAPTURE_RADIUS, CAPTURE_RADIUS, CAPTURE_RADIUS)) {
            if (ent instanceof ArmorStand && Null_Statue.STATUE_NAME.equals(ent.getCustomName())) {
                nearbyStatue = (ArmorStand) ent;
                break;
            }
        }

        RuneSession session = sessions.computeIfAbsent(p.getUniqueId(), k -> new RuneSession(p));
        long now = System.currentTimeMillis();

        // --- ESTADO 1: EN COOLDOWN ---
        if (now < session.cooldownUntil) {
            if (session.bossBar != null) {
                if (now > session.cooldownUntil - 2000) {
                    session.cleanup();
                }
            }
            return;
        }

        // --- ESTADO 2: CAPTURANDO (Jugador cerca de estatua) ---
        if (nearbyStatue != null) {

            // Lógica de partículas GLOBAL (Esfera)
            if (!statuesRendered.contains(nearbyStatue.getUniqueId())) {
                drawSphere(nearbyStatue.getLocation().add(0, 1, 0));
                statuesRendered.add(nearbyStatue.getUniqueId());
            }

            session.isDecaying = false;

            // Aumentar progreso
            session.progress = Math.min(1.0, session.progress + PROGRESS_PER_TICK);

            // Efectos visuales al jugador
            p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 5, 0, false, false, false));
            drawBeam(p, nearbyStatue.getLocation().add(0, 1.5, 0));

            // Sonido Bucle (Cada 2 segundos)
            if (now >= session.nextSoundTime) {
                p.playSound(p.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.0f, 2.0f);
                session.nextSoundTime = now + 2000;
            }

            session.updateBar();

            // RECOMPENSA (100%)
            if (session.progress >= 1.0) {
                completeCapture(p, session);
            }

        }
        // --- ESTADO 3: DECAYENDO (Jugador lejos pero con progreso) ---
        else if (session.progress > 0) {

            if (!session.isDecaying) {
                p.playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
                session.isDecaying = true;
            }

            session.progress = Math.max(0.0, session.progress - DECAY_PER_TICK);
            session.updateBar();

            if (session.progress <= 0) {
                session.cleanup();
                sessions.remove(p.getUniqueId());
            }
        }
        // --- ESTADO 4: INACTIVO ---
        else {
            if (session.bossBar != null) {
                session.cleanup();
                sessions.remove(p.getUniqueId());
            }
        }
    }

    private void completeCapture(Player p, RuneSession session) {
        p.getInventory().addItem(InfestedCaveItems.createEmptyRune());

        p.playSound(p.getLocation(), "minecraft:custom.click_stereo_old", 1.0f, 2.0f);
        p.spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.5);

        // Actualizar BossBar final
        session.bossBar.setTitle(ChatColor.GREEN + "Runa Obtenida");
        session.bossBar.setProgress(1.0);
        session.bossBar.setColor(BarColor.GREEN);

        // Cooldown 5s
        session.cooldownUntil = System.currentTimeMillis() + 5000;
        session.progress = 0.0;
    }

    // --- VISUALES ---

    private void drawSphere(Location center) {
        double radius = 2.5;
        for (double t = 0; t <= Math.PI; t += Math.PI / 10) {
            double r = Math.sin(t) * radius;
            double y = Math.cos(t) * radius;
            for (double phi = 0; phi < 2 * Math.PI; phi += Math.PI / 10) {
                double x = Math.cos(phi) * r;
                double z = Math.sin(phi) * r;
                center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(x, y, z), 0, 0, 0, 0, 0);
            }
        }
    }

    private void drawBeam(Player p, Location target) {
        Location origin = p.getLocation().add(0, 1, 0);
        Vector direction = target.toVector().subtract(origin.toVector());
        double distance = direction.length();
        direction.normalize();

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.AQUA, 1.0f);

        for (double d = 0; d < distance; d += 0.5) {
            origin.add(direction.clone().multiply(0.5));
            p.getWorld().spawnParticle(Particle.DUST, origin, 1, 0, 0, 0, 0, dustOptions);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        RuneSession session = sessions.remove(e.getPlayer().getUniqueId());
        if (session != null) {
            session.cleanup();
        }
    }

    // --- CLASE INTERNA DE SESIÓN ---
    private static class RuneSession {
        private final Player player;
        private BossBar bossBar;

        double progress = 0.0;
        long nextSoundTime = 0;
        long cooldownUntil = 0;
        boolean isDecaying = false;

        public RuneSession(Player player) {
            this.player = player;
        }

        public void updateBar() {
            if (bossBar == null) {
                bossBar = Bukkit.createBossBar("Inicializando...", BarColor.BLUE, BarStyle.SOLID);
                bossBar.addPlayer(player);
            }

            int percentage = (int) (progress * 100);
            String title;

            // Construcción de Strings con colores HEX exactos
            if (!isDecaying) {
                // Progreso (Azules/Morados)
                // [{"text":"ζ","italic":false,"color":"#884bd2"},{"text":" Obteniendo Runa:","italic":false,"color":"#84cde6"},{"text":" ","italic":false},{"text":"40%","italic":false,"color":"#9f7acd"}]
                title = ChatColor.of("#884bd2") + "ζ" +
                        ChatColor.of("#84cde6") + " Obteniendo Runa: " +
                        ChatColor.of("#9f7acd") + percentage + "%";

                if (bossBar.getColor() != BarColor.BLUE) bossBar.setColor(BarColor.BLUE);

            } else {
                // Decay (Rojos/Beige)
                // [{"text":"ζ","italic":false,"color":"#d24b4b"},{"text":" ","italic":false,"color":"#84cde6"},{"text":"Obteniendo Runa:","italic":false,"color":"#d2bdac"},{"text":" ","italic":false},{"text":"40%","italic":false,"color":"#cd7a7a"}]
                title = ChatColor.of("#d24b4b") + "ζ " +
                        ChatColor.of("#d2bdac") + "Obteniendo Runa: " +
                        ChatColor.of("#cd7a7a") + percentage + "%";

                if (bossBar.getColor() != BarColor.RED) bossBar.setColor(BarColor.RED);
            }

            bossBar.setTitle(title);
            bossBar.setProgress(progress);
        }

        public void cleanup() {
            if (bossBar != null) {
                bossBar.removeAll();
                bossBar = null;
            }
        }
    }
}