package StatueManager;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.util.*;

public class StatueDebugManager {

    private final JavaPlugin plugin;
    private final StatueManager statueManager;

    private final Map<UUID, ArmorStand> debugPlayers = new HashMap<>();
    private BukkitRunnable particleTask;

    public StatueDebugManager(JavaPlugin plugin, StatueManager statueManager) {
        this.plugin        = plugin;
        this.statueManager = statueManager;
        startParticleLoop();
    }

    public void toggleDebug(Player player) {

        if (debugPlayers.containsKey(player.getUniqueId())) {
            debugPlayers.remove(player.getUniqueId());
            player.sendMessage("§c§l[DEBUG] §7Visualización de estatua §cdesactivada§7.");
            return;
        }

        ArmorStand target = getTargetStatue(player);
        if (target == null) {
            player.sendMessage("§c§l[DEBUG] §7No estás apuntando a ninguna estatua (máx. 10 bloques).");
            return;
        }

        debugPlayers.put(player.getUniqueId(), target);
        StatueData data = new StatueData(target);
        String tipo = data.isAntiGrief() ? "§4ANTI-GRIEF" : "§9EFECTO";

        // Mensaje enriquecido con toda la información de la estatua
        player.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§a§l[DEBUG] §7Visualizando Estatua de " + tipo);
        player.sendMessage("§7Radio X/Z: §f" + data.getRadiusX() + " §7| Radio Y: §f" + data.getRadiusY());
        player.sendMessage("§7Vida Actual/Max: §f" + data.getHpCurrent() + "§7/§f" + data.getHpMax());

        if (!data.isAntiGrief()) {
            String efName = data.getEffectType() != null ? data.getEffectType().getName() : "NINGUNO";
            player.sendMessage("§7Efecto: §f" + efName + " (Nivel " + (data.getEffectAmplifier() + 1) + ")");
        }

        player.sendMessage("§7Color de brillo: §f" + (data.getGlowColor() == null ? "OFF" : data.getGlowColor().name()));
        player.sendMessage("§7Invulnerable: " + (data.isInvulnerable() ? "§aSí" : "§cNo") + " §7| Visible: " + (data.isVisible() ? "§aSí" : "§cNo"));
        player.sendMessage("§8§o(Escribe /statue debug de nuevo para desactivar)");
        player.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // Cambiado a public para que el comando /statue clone pueda usarlo
    public ArmorStand getTargetStatue(Player player) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                10.0,
                entity -> entity instanceof ArmorStand && StatueData.isStatue((ArmorStand) entity)
        );
        if (result == null || !(result.getHitEntity() instanceof ArmorStand)) return null;
        return (ArmorStand) result.getHitEntity();
    }

    private void startParticleLoop() {
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<UUID, ArmorStand>> it = debugPlayers.entrySet().iterator();

                while (it.hasNext()) {
                    Map.Entry<UUID, ArmorStand> entry = it.next();
                    Player player = plugin.getServer().getPlayer(entry.getKey());
                    ArmorStand stand = entry.getValue();

                    if (player == null || !player.isOnline() || !stand.isValid()) {
                        it.remove();
                        continue;
                    }

                    StatueData data = new StatueData(stand);
                    spawnDebugBox(player, stand.getLocation(), data);
                }
            }
        };
        // Se ejecuta cada 10 ticks (0.5s) lo cual es suficiente para no generar lag visual
        particleTask.runTaskTimer(plugin, 0L, 10L);
    }

    private void spawnDebugBox(Player player, Location center, StatueData data) {
        double rX = data.getRadiusX();
        double rY = data.getRadiusY();

        Particle particle = Particle.DUST;
        Particle.DustOptions dustOptions;

        if (data.isAntiGrief()) {
            dustOptions = new Particle.DustOptions(Color.fromRGB(255, 60, 60), 1.5f);
        } else {
            dustOptions = new Particle.DustOptions(Color.fromRGB(140, 80, 255), 1.5f);
        }

        // OPTIMIZACIÓN DE PARTICULAS:
        // Antes era Math.max(0.5, rX / 25.0) -> creaba demasiadas partículas.
        // Ahora el salto mínimo (step) es de 1.5 bloques, dibujando la caja mucho más ligera.
        double step = Math.max(1.5, rX / 8.0);

        Location playerLoc = player.getLocation();
        double renderDistanceSq = 30.0 * 30.0; // Distancia de dibujado aumentada un poco por seguridad

        for (double x = -rX; x <= rX; x += step) {
            for (double z = -rX; z <= rX; z += step) {
                Location pLocTop = center.clone().add(x, rY, z);
                if (pLocTop.distanceSquared(playerLoc) <= renderDistanceSq) {
                    spawnParticle(player, center, x, rY, z, particle, dustOptions);
                }

                Location pLocBot = center.clone().add(x, -rY, z);
                if (pLocBot.distanceSquared(playerLoc) <= renderDistanceSq) {
                    spawnParticle(player, center, x, -rY, z, particle, dustOptions);
                }
            }
        }

        for (double x = -rX; x <= rX; x += step) {
            for (double y = -rY; y <= rY; y += step) {
                Location pLocFront = center.clone().add(x, y, rX);
                if (pLocFront.distanceSquared(playerLoc) <= renderDistanceSq) {
                    spawnParticle(player, center, x, y, rX, particle, dustOptions);
                }

                Location pLocBack = center.clone().add(x, y, -rX);
                if (pLocBack.distanceSquared(playerLoc) <= renderDistanceSq) {
                    spawnParticle(player, center, x, y, -rX, particle, dustOptions);
                }
            }
        }

        for (double z = -rX; z <= rX; z += step) {
            for (double y = -rY; y <= rY; y += step) {
                Location pLocLeft = center.clone().add(rX, y, z);
                if (pLocLeft.distanceSquared(playerLoc) <= renderDistanceSq) {
                    spawnParticle(player, center, rX, y, z, particle, dustOptions);
                }

                Location pLocRight = center.clone().add(-rX, y, z);
                if (pLocRight.distanceSquared(playerLoc) <= renderDistanceSq) {
                    spawnParticle(player, center, -rX, y, z, particle, dustOptions);
                }
            }
        }
    }

    private void spawnParticle(Player player, Location center, double dx, double dy, double dz,
                               Particle particle, Particle.DustOptions dust) {
        Location loc = center.clone().add(dx, dy, dz);
        if (dust != null) {
            player.spawnParticle(particle, loc, 1, 0, 0, 0, 0, dust);
        } else {
            player.spawnParticle(particle, loc, 1, 0, 0, 0, 0);
        }
    }

    public void cleanup() {
        if (particleTask != null) particleTask.cancel();
        debugPlayers.clear();
    }
}