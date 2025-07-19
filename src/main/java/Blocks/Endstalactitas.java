package Blocks;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import items.EndItems;

import java.util.*;

public class Endstalactitas implements Listener {
    private final Random random = new Random();
    private final Map<UUID, BukkitRunnable> fatigueTasks = new HashMap<>();
    private final JavaPlugin plugin;

    public Endstalactitas(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType() != Material.MAGENTA_GLAZED_TERRACOTTA ||
                block.getWorld().getEnvironment() != World.Environment.THE_END) {
            return;
        }

        // Aplicar efecto inmediatamente
        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, Integer.MAX_VALUE, 1, false, false));

        // Sonido al comenzar a picar
        player.playSound(block.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.6f);

        // Cancelar tarea anterior si existe
        cancelFatigueTask(player);

        // Crear nueva tarea para remover el efecto cuando deje de picar
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                fatigueTasks.remove(player.getUniqueId());
            }
        };

        fatigueTasks.put(player.getUniqueId(), task);
        task.runTaskLater(plugin, 20L); // 1 segundo después
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType() != Material.MAGENTA_GLAZED_TERRACOTTA) return;

        // Verificar si está en el End
        if (block.getWorld().getEnvironment() == World.Environment.THE_END) {
            // Cancelar el drop original
            event.setDropItems(false);

            // Dropear fragmentos de enderite
            int amount = random.nextDouble() < 0.3 ? 2 : 1;
            block.getWorld().dropItemNaturally(block.getLocation(), EndItems.createEnderiteNugget(amount));

            // Remover efecto inmediatamente
            cancelFatigueTask(player);
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        } else {
            // En otros mundos dropear el bloque renombrado
            event.setDropItems(false);
            ItemStack renamedBlock = new ItemStack(Material.MAGENTA_GLAZED_TERRACOTTA);
            ItemMeta meta = renamedBlock.getItemMeta();
            meta.setDisplayName("Endstalactita");
            renamedBlock.setItemMeta(meta);
            block.getWorld().dropItemNaturally(block.getLocation(), renamedBlock);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Limpiar efectos al salir
        cancelFatigueTask(event.getPlayer());
        event.getPlayer().removePotionEffect(PotionEffectType.MINING_FATIGUE);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private void handleExplosion(List<Block> blocks) {
        for (Block block : blocks) {
            if (block.getType() == Material.MAGENTA_GLAZED_TERRACOTTA &&
                    block.getWorld().getEnvironment() == World.Environment.THE_END) {
                block.setType(Material.AIR);
                block.getWorld().dropItemNaturally(block.getLocation(), EndItems.createEnderiteNugget(1));
            }
        }
    }

    private void cancelFatigueTask(Player player) {
        if (fatigueTasks.containsKey(player.getUniqueId())) {
            fatigueTasks.get(player.getUniqueId()).cancel();
            fatigueTasks.remove(player.getUniqueId());
        }
    }
}