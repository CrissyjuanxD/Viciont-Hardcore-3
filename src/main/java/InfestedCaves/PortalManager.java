package InfestedCaves;

import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import vct.hardcore3.ViciontHardcore3;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class PortalManager implements Listener {
    private final JavaPlugin plugin;
    private final Set<UUID> activePortals = new HashSet<>(); // Guardamos UUID de las entidades

    public PortalManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void spawnPortal(Location loc, boolean isToInfested) {
        World world = loc.getWorld();
        // Spawneamos un Block Display
        BlockDisplay display = (BlockDisplay) world.spawnEntity(loc, EntityType.BLOCK_DISPLAY);

        // Configurar bloque
        display.setBlock(Bukkit.createBlockData(Material.PURPLE_GLAZED_TERRACOTTA));

        // Transformación: Aplanar y estirar a 4x4
        // Escala X=4, Y=0.1 (plano), Z=4
        Transformation transformation = new Transformation(
                new Vector3f(-1.5f, 0, -1.5f), // Traslación para centrar (ajustar según necesidad)
                new AxisAngle4f(0, 0, 0, 0),
                new Vector3f(4f, 0.1f, 4f), // Escala
                new AxisAngle4f(0, 0, 0, 0)
        );
        display.setTransformation(transformation);

        // Glow effect
        display.setGlowing(true);
        display.setGlowColorOverride(Color.PURPLE);

        // Metadata para identificar el tipo de portal
        String tag = isToInfested ? "InfestedEntry" : "InfestedExit";
        display.setMetadata(tag, new FixedMetadataValue(plugin, true));
        activePortals.add(display.getUniqueId());

        // Partículas alrededor
        new BukkitRunnable() {
            @Override
            public void run() {
                if (display.isDead()) {
                    this.cancel();
                    return;
                }
                world.spawnParticle(Particle.DRAGON_BREATH, display.getLocation(), 5, 1, 0, 1, 0.05);
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public void removePortalNearby(Location loc) {
        loc.getWorld().getNearbyEntities(loc, 2, 2, 2).forEach(entity -> {
            if (entity instanceof BlockDisplay && (entity.hasMetadata("InfestedEntry") || entity.hasMetadata("InfestedExit"))) {
                entity.remove();
                activePortals.remove(entity.getUniqueId());
            }
        });
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        // Optimización: Solo chequear si se movió bloque completo
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() &&
                e.getFrom().getBlockY() == e.getTo().getBlockY() &&
                e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        Player p = e.getPlayer();
        Location pLoc = p.getLocation();

        // Buscar entidades cercanas que sean nuestros portales
        p.getWorld().getNearbyEntities(pLoc, 1, 1, 1).stream()
                .filter(ent -> ent instanceof BlockDisplay)
                .forEach(ent -> {
                    if (ent.hasMetadata("InfestedEntry")) {
                        triggerTeleport(p, true); // Ir a dimensión
                    } else if (ent.hasMetadata("InfestedExit")) {
                        triggerTeleport(p, false); // Ir a Overworld
                    }
                });
    }

    private void triggerTeleport(Player p, boolean toInfested) {
        if (p.hasMetadata("Teleporting")) return; // Evitar spam
        p.setMetadata("Teleporting", new FixedMetadataValue(plugin, true));

        // Efecto visual y Levitation
        p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 2));
        p.playSound(p.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1f, 0.5f);
        p.sendTitle("§5§lINFESTED", "§7Viajando...", 10, 40, 10);

        new BukkitRunnable() {
            @Override
            public void run() {
                p.removeMetadata("Teleporting", plugin);
                p.removePotionEffect(PotionEffectType.LEVITATION);

                if (toInfested) {
                    teleportToInfested(p);
                } else {
                    teleportToOverworld(p);
                }
            }
        }.runTaskLater(plugin, 50L); // 2.5 segundos de espera
    }

    private void teleportToInfested(Player p) {
        World infested = Bukkit.getWorld(ViciontHardcore3.WORLD_NAME);
        if (infested == null) return;

        // Spawn Random seguro > 1000 bloques
        Location safe = findSafeSpawn(infested);
        p.teleport(safe);
        p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1f);
    }

    private void teleportToOverworld(Player p) {
        World overworld = Bukkit.getWorlds().get(0);
        Location spawn = p.getBedSpawnLocation();
        if (spawn == null) spawn = overworld.getSpawnLocation();
        p.teleport(spawn);
    }

    private Location findSafeSpawn(World world) {
        java.util.Random r = new java.util.Random();
        int min = 1000;
        int max = 1900;

        for (int i = 0; i < 20; i++) { // Intentar 20 veces encontrar un buen lugar
            int x = r.nextBoolean() ? r.nextInt(max - min) + min : -(r.nextInt(max - min) + min);
            int z = r.nextBoolean() ? r.nextInt(max - min) + min : -(r.nextInt(max - min) + min);

            // Buscar Y seguro desde el medio hacia arriba/abajo
            for (int y = 50; y < 110; y++) {
                if (world.getBlockAt(x, y, z).getType().isSolid() &&
                        world.getBlockAt(x, y+1, z).getType() == Material.AIR &&
                        world.getBlockAt(x, y+2, z).getType() == Material.AIR) {

                    return new Location(world, x + 0.5, y + 1.5, z + 0.5); // +0.5 para centrar y +1.5 para no bugear pies
                }
            }
        }
        return new Location(world, 1500, 100, 1500);
    }
}