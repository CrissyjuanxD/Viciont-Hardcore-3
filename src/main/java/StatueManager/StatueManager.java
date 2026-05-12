package StatueManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StatueManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, ArmorStand> activeStatues = new HashMap<>();

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
                    }
                }
            }
        });
    }

    public void registerStatue(ArmorStand stand) {
        activeStatues.put(stand.getUniqueId(), stand);
        updateGlowingColor(stand);
    }

    public void unregisterStatue(ArmorStand stand) {
        activeStatues.remove(stand.getUniqueId());
        removeEffectFromPlayers(stand);
    }

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

                    // Si es Anti-Grief, no da efectos de poción
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
        if (current != null) {
            if (current.getDuration() > 40 && current.getAmplifier() >= amplifier) {
                return;
            }
        }
        p.addPotionEffect(new PotionEffect(type, 200, amplifier, true, true));
    }

    private void removeEffectFromPlayers(ArmorStand stand) {
        StatueData data = new StatueData(stand);
        if (data.isAntiGrief()) return; // No hay efecto que quitar

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

    // ==========================================
    //          SISTEMA ANTI-GRIEF
    // ==========================================

    private boolean isLocationProtected(Location loc) {
        for (ArmorStand stand : activeStatues.values()) {
            if (!stand.isValid() || !stand.getChunk().isLoaded()) continue;

            StatueData data = new StatueData(stand);
            if (!data.isAntiGrief()) continue;

            Location sLoc = stand.getLocation();

            // Verificamos si el mundo es el mismo
            if (!sLoc.getWorld().equals(loc.getWorld())) continue;

            double dx = Math.abs(loc.getX() - sLoc.getX());
            double dy = Math.abs(loc.getY() - sLoc.getY());
            double dz = Math.abs(loc.getZ() - sLoc.getZ());

            // Verificación Cilíndrica/Cúbica básica
            if (dx <= data.getRadiusX() && dz <= data.getRadiusX() && dy <= data.getRadiusY()) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        // Copiamos la lista para iterar y borrar sin problemas
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
        // Evita que los Endermans roben bloques, Withers rompan al pasar, etc.
        if (event.getEntity() instanceof Player) return; // Permitimos a los jugadores construir

        if (isLocationProtected(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
}