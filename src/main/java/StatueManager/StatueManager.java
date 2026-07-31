package StatueManager;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class StatueManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, ArmorStand> activeStatues = new HashMap<>();

    // Mapa de tasks de partículas por UUID de estatua
    private final Map<UUID, BukkitRunnable> particleTasks = new HashMap<>();

    // Tick counter por estatua para la animación de partículas
    private final Map<UUID, Double> particleAngles = new HashMap<>();

    public StatueManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startEffectLoop();
    }

    public void loadStatues() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof ArmorStand && StatueData.isStatue((ArmorStand) entity)) {
                        ArmorStand stand = (ArmorStand) entity;
                        activeStatues.put(entity.getUniqueId(), stand);

                        StatueData data = new StatueData(stand);
                        stand.setVisible(data.isVisible());
                        updateGlowingColor(stand);
                        updateStatueName(stand);
                        startParticleTask(stand);
                    }
                }
            }
        });
    }

    public void registerStatue(ArmorStand stand) {
        activeStatues.put(stand.getUniqueId(), stand);
        updateGlowingColor(stand);
        updateStatueName(stand);
        startParticleTask(stand);
    }

    public void unregisterStatue(ArmorStand stand) {
        activeStatues.remove(stand.getUniqueId());
        removeEffectFromPlayers(stand);
        stopParticleTask(stand.getUniqueId());
        particleAngles.remove(stand.getUniqueId());
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  NOMBRE DINÁMICO
    // ──────────────────────────────────────────────────────────────────────────

    public void updateStatueName(ArmorStand stand) {
        StatueData data = new StatueData(stand);
        if (data.isAntiGrief()) {
            stand.setCustomName(ChatColor.translateAlternateColorCodes('&', "&c&lStatue Grief"));
        } else {
            stand.setCustomName(ChatColor.translateAlternateColorCodes('&', "&6&lStatue Effect"));
        }
        // El nombre solo se ve en debug/hover, no visible por defecto
        stand.setCustomNameVisible(false);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  SISTEMA DE PARTÍCULAS MÁGICAS
    // ──────────────────────────────────────────────────────────────────────────

    public void startParticleTask(ArmorStand stand) {
        // Reiniciar siempre: cancelar el existente primero
        stopParticleTask(stand.getUniqueId());

        StatueData data = new StatueData(stand);

        // Solo estatuas de efecto tienen partículas mágicas
        if (data.isAntiGrief() || data.getEffectType() == null) return;

        particleAngles.put(stand.getUniqueId(), 0.0);

        BukkitRunnable task = new BukkitRunnable() {
            double tick = 0;

            @Override
            public void run() {
                if (!stand.isValid() || !stand.getChunk().isLoaded()) {
                    cancel();
                    particleTasks.remove(stand.getUniqueId());
                    particleAngles.remove(stand.getUniqueId());
                    return;
                }

                StatueData freshData = new StatueData(stand);

                // Si cambió a Anti-Grief, dejar de emitir
                if (freshData.isAntiGrief() || freshData.getEffectType() == null) {
                    cancel();
                    particleTasks.remove(stand.getUniqueId());
                    particleAngles.remove(stand.getUniqueId());
                    return;
                }

                spawnMagicParticles(stand, freshData.getEffectType(), tick);
                tick += 0.12; // velocidad de rotación
            }
        };

        task.runTaskTimer(plugin, 0L, 1L); // cada tick para fluidez
        particleTasks.put(stand.getUniqueId(), task);
    }

    private void stopParticleTask(UUID uuid) {
        BukkitRunnable existing = particleTasks.remove(uuid);
        if (existing != null) {
            try { existing.cancel(); } catch (Exception ignored) {}
        }
    }

    private void spawnMagicParticles(ArmorStand stand, PotionEffectType effectType, double tick) {
        Location base = stand.getLocation().add(0, 1.2, 0); // centro del cuerpo

        Color color = getEffectColor(effectType);
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.0f);

        int souls = 3;         // número de "almas" orbitantes
        double orbitRadius = 0.65;
        double heightAmp   = 0.55; // amplitud de oscilación vertical

        for (int i = 0; i < souls; i++) {
            double phase = (Math.PI * 2.0 / souls) * i;

            // Ángulo principal de órbita con pequeña variación caótica
            double angle  = tick + phase;
            double angleY = tick * 0.7 + phase; // velocidad vertical distinta → efecto no uniforme

            double x = Math.cos(angle) * orbitRadius;
            double z = Math.sin(angle) * orbitRadius;
            double y = Math.sin(angleY) * heightAmp;

            Location loc = base.clone().add(x, y, z);

            // Partícula principal
            stand.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);

            // Estela: 2 puntos anteriores con alpha reducido (tamaño menor)
            Particle.DustOptions dustSmall = new Particle.DustOptions(color, 0.6f);
            double angleBack  = angle  - 0.25;
            double angleYBack = angleY - 0.18;

            double xb = Math.cos(angleBack) * (orbitRadius * 0.85);
            double zb = Math.sin(angleBack) * (orbitRadius * 0.85);
            double yb = Math.sin(angleYBack) * heightAmp * 0.8;
            stand.getWorld().spawnParticle(Particle.DUST, base.clone().add(xb, yb, zb), 1, 0, 0, 0, 0, dustSmall);

            // Segunda estela más pequeña
            Particle.DustOptions dustTiny = new Particle.DustOptions(color, 0.35f);
            double angleBack2  = angle  - 0.45;
            double angleYBack2 = angleY - 0.32;

            double xb2 = Math.cos(angleBack2) * (orbitRadius * 0.7);
            double zb2 = Math.sin(angleBack2) * (orbitRadius * 0.7);
            double yb2 = Math.sin(angleYBack2) * heightAmp * 0.6;
            stand.getWorld().spawnParticle(Particle.DUST, base.clone().add(xb2, yb2, zb2), 1, 0, 0, 0, 0, dustTiny);
        }

        // Destello esporádico: cada ~2 segundos un flash de partícula brillante
        if (tick % (Math.PI * 2) < 0.15) {
            double fx = (Math.random() - 0.5) * 0.8;
            double fy = Math.random() * 1.8;
            double fz = (Math.random() - 0.5) * 0.8;
            Particle.DustOptions dustFlash = new Particle.DustOptions(color, 1.5f);
            stand.getWorld().spawnParticle(Particle.DUST, base.clone().add(fx, fy, fz), 1, 0, 0, 0, 0, dustFlash);
        }
    }

    private Color getEffectColor(PotionEffectType type) {
        if (type == null) return Color.fromRGB(200, 200, 200);

        String name = type.getName().toUpperCase();

        // ── Efectos positivos (colores pastel) ──
        if (name.equals("SPEED"))           return Color.fromRGB(130, 220, 255); // celeste
        if (name.equals("FAST_DIGGING"))    return Color.fromRGB(255, 210, 120); // ámbar suave
        if (name.equals("INCREASE_DAMAGE")) return Color.fromRGB(255, 120, 120); // rojo pastel
        if (name.equals("JUMP"))            return Color.fromRGB(160, 255, 160); // verde menta
        if (name.equals("REGENERATION"))    return Color.fromRGB(255, 140, 200); // rosa
        if (name.equals("DAMAGE_RESISTANCE")
                || name.equals("RESISTANCE"))      return Color.fromRGB(100, 140, 255); // azul índigo
        if (name.equals("FIRE_RESISTANCE")) return Color.fromRGB(255, 160, 60);  // naranja fuego
        if (name.equals("WATER_BREATHING")) return Color.fromRGB(60, 200, 220);  // turquesa
        if (name.equals("INVISIBILITY"))    return Color.fromRGB(220, 220, 235); // gris perla
        if (name.equals("ABSORPTION"))      return Color.fromRGB(255, 230, 80);  // dorado
        if (name.equals("HEALTH_BOOST"))    return Color.fromRGB(255, 80, 100);  // rojo brillante
        if (name.equals("SATURATION"))      return Color.fromRGB(120, 210, 90);  // verde hoja
        if (name.equals("LUCK"))            return Color.fromRGB(180, 255, 180); // verde lima pastel
        if (name.equals("CONDUIT_POWER"))   return Color.fromRGB(60, 220, 210);  // cian marino
        if (name.equals("DOLPHINS_GRACE"))  return Color.fromRGB(100, 180, 255); // azul delfín
        if (name.equals("HERO_OF_THE_VILLAGE")) return Color.fromRGB(255, 200, 130); // piel cálida
        if (name.equals("NIGHT_VISION"))    return Color.fromRGB(200, 160, 255); // lavanda
        if (name.equals("HASTE"))           return Color.fromRGB(255, 195, 80);  // ocre brillante
        if (name.equals("SLOW_FALLING"))    return Color.fromRGB(230, 230, 255); // blanco azulado
        if (name.equals("GLOWING"))         return Color.fromRGB(255, 255, 120); // amarillo neón
        if (name.equals("LEVITATION"))      return Color.fromRGB(200, 150, 255); // morado pastel
        if (name.equals("TRIAL_OMEN"))      return Color.fromRGB(255, 140, 80);  // naranja tenue

        // ── Efectos negativos (colores oscuros / saturados) ──
        if (name.equals("SLOWNESS") || name.equals("SLOW")) return Color.fromRGB(80, 80, 160);    // azul marino
        if (name.equals("MINING_FATIGUE")
                || name.equals("SLOW_DIGGING"))    return Color.fromRGB(90, 60, 40);    // marrón oscuro
        if (name.equals("NAUSEA")
                || name.equals("CONFUSION"))       return Color.fromRGB(100, 60, 100);  // púrpura enfermizo
        if (name.equals("BLINDNESS"))       return Color.fromRGB(40, 40, 40);    // casi negro
        if (name.equals("HUNGER"))          return Color.fromRGB(130, 80, 20);   // ocre quemado
        if (name.equals("WEAKNESS"))        return Color.fromRGB(80, 60, 100);   // violeta grisáceo
        if (name.equals("POISON"))          return Color.fromRGB(80, 160, 40);   // verde venenoso
        if (name.equals("WITHER"))          return Color.fromRGB(50, 30, 50);    // negro violáceo
        if (name.equals("INSTANT_DAMAGE")
                || name.equals("HARM"))            return Color.fromRGB(200, 40, 40);   // rojo sangre
        if (name.equals("UNLUCK")
                || name.equals("BAD_LUCK"))        return Color.fromRGB(100, 60, 60);   // rojo apagado
        if (name.equals("BAD_OMEN"))        return Color.fromRGB(60, 100, 40);   // verde amenazante
        if (name.equals("RAID_OMEN"))       return Color.fromRGB(180, 60, 40);   // rojo incursión
        if (name.equals("DARKNESS"))        return Color.fromRGB(30, 20, 40);    // negro profundo

        // ── Efectos instantáneos positivos ──
        if (name.equals("INSTANT_HEALTH")
                || name.equals("HEAL"))            return Color.fromRGB(255, 100, 150); // rosa curación

        // Default: gris mágico
        return Color.fromRGB(180, 160, 255);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  LOOP DE EFECTOS (sin cambios en lógica, igual que antes)
    // ──────────────────────────────────────────────────────────────────────────

    private void startEffectLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ArmorStand stand : new ArrayList<>(activeStatues.values())) {
                    if (!stand.isValid() || !stand.getChunk().isLoaded()) {
                        if (stand.isDead()) activeStatues.remove(stand.getUniqueId());
                        continue;
                    }

                    StatueData data = new StatueData(stand);

                    if (data.isAntiGrief()) continue;

                    double radiusX = data.getRadiusX();
                    double radiusY = data.getRadiusY();
                    PotionEffectType type = data.getEffectType();
                    int amp = data.getEffectAmplifier();

                    if (type == null) continue;

                    for (Entity ent : stand.getNearbyEntities(radiusX, radiusY, radiusX)) {
                        if (ent instanceof Player) {
                            Player p = (Player) ent;
                            applySmartEffect(p, type, amp);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void applySmartEffect(Player p, PotionEffectType type, int amplifier) {
        PotionEffect current = p.getPotionEffect(type);
        boolean isNewEffect = false;

        if (current != null) {
            if (current.getDuration() > 40 && current.getAmplifier() >= amplifier) {
                return;
            }
        } else {
            isNewEffect = true;
        }
        p.addPotionEffect(new PotionEffect(type, 200, amplifier, true, true));
        if (isNewEffect) {
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);
        }
    }

    private void removeEffectFromPlayers(ArmorStand stand) {
        StatueData data = new StatueData(stand);
        if (data.isAntiGrief()) return;

        PotionEffectType type = data.getEffectType();
        if (type == null) return;

        double rX = data.getRadiusX();
        double rY = data.getRadiusY();

        for (Entity ent : stand.getNearbyEntities(rX, rY, rX)) {
            if (ent instanceof Player) {
                Player p = (Player) ent;
                PotionEffect current = p.getPotionEffect(type);
                if (current != null && current.getDuration() <= 205) {
                    p.removePotionEffect(type);
                    p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 2.0f);
                }
            }
        }
    }

    public void updateGlowingColor(ArmorStand stand) {
        StatueData data = new StatueData(stand);
        ChatColor color = data.getGlowColor();

        if (color == null) {
            stand.setGlowing(false);
            return;
        }

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "SE_" + color.name();

        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
            team.setColor(color);
        }

        if (!team.hasEntry(stand.getUniqueId().toString())) {
            team.addEntry(stand.getUniqueId().toString());
        }

        stand.setGlowing(true);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  SISTEMA ANTI-GRIEF (sin cambios)
    // ──────────────────────────────────────────────────────────────────────────

    private boolean isLocationProtected(Location loc) {
        for (ArmorStand stand : activeStatues.values()) {
            if (!stand.isValid() || !stand.getChunk().isLoaded()) continue;

            StatueData data = new StatueData(stand);
            if (!data.isAntiGrief()) continue;

            Location sLoc = stand.getLocation();

            if (!sLoc.getWorld().equals(loc.getWorld())) continue;

            double dx = Math.abs(loc.getX() - sLoc.getX());
            double dy = Math.abs(loc.getY() - sLoc.getY());
            double dz = Math.abs(loc.getZ() - sLoc.getZ());

            if (dx <= data.getRadiusX() && dz <= data.getRadiusX() && dy <= data.getRadiusY()) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        List<Block> blocks = new ArrayList<>(event.blockList());
        for (Block block : blocks) {
            if (isLocationProtected(block.getLocation())) {
                event.blockList().remove(block);
            }
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        List<Block> blocks = new ArrayList<>(event.blockList());
        for (Block block : blocks) {
            if (isLocationProtected(block.getLocation())) {
                event.blockList().remove(block);
            }
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Player) return;
        if (isLocationProtected(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
}