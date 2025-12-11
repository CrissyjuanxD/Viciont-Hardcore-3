package InfestedCaves;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import vct.hardcore3.ViciontHardcore3;

public class InfestedListeners implements Listener {
    private final ViciontHardcore3 plugin; // Corregido tipo
    private final PortalManager portalManager;
    private final StructureManager structureManager;
    private boolean templeGenerated = false;

    public InfestedListeners(ViciontHardcore3 plugin, PortalManager portalManager, StructureManager structureManager) {
        this.plugin = plugin;
        this.portalManager = portalManager;
        this.structureManager = structureManager;
    }

    // Fix Muerte al entrar: Dar invulnerabilidad temporal
    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getTo().getWorld().getName().equals(ViciontHardcore3.WORLD_NAME)) {
            // Dar resistencia al daño por caída y resistencia total por 10 segundos
            e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 255));
            e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 1));
        }
    }

    // Generación de Estructuras
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        if (!e.getWorld().getName().equals(ViciontHardcore3.WORLD_NAME)) return;

        // Templo en 0,0
        if (e.getChunk().getX() == 0 && e.getChunk().getZ() == 0 && !templeGenerated) {
            plugin.getLogger().info("Chunk 0,0 cargado. Generando templo...");
            structureManager.pasteTempleAtSpawn(e.getWorld());
            templeGenerated = true;
        }

        // Intentar generar Dungeon (solo en chunks nuevos)
        if (e.isNewChunk()) {
            structureManager.tryGenerateDungeon(e.getChunk());
        }
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent e) {
        if (!e.getLocation().getWorld().getName().equals(ViciontHardcore3.WORLD_NAME)) return;
        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) return;

        EntityType type = e.getEntityType();
        if (type != EntityType.ZOMBIE &&
                type != EntityType.CREEPER &&
                type != EntityType.SPIDER &&
                type != EntityType.SKELETON &&
                type != EntityType.BLOCK_DISPLAY &&
                type != EntityType.PLAYER) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBedInteract(PlayerInteractEvent e) {
        if (!e.getPlayer().getWorld().getName().equals(ViciontHardcore3.WORLD_NAME)) return;

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK &&
                e.getClickedBlock() != null &&
                e.getClickedBlock().getType().name().contains("BED")) {

            // Si el jugador está shifteando y tiene bloque, probablemente quiere poner bloque, no dormir
            if (e.getPlayer().isSneaking() && e.getItem() != null && e.getItem().getType().isBlock()) {
                return;
            }

            e.setCancelled(true);
            e.getPlayer().sendMessage("§cNo puedes dormir ni guardar spawn en esta dimensión maldita.");
        }
    }
}