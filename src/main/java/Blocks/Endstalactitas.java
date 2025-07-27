package Blocks;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemRarity;
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
    private final Set<Material> allowedPickaxes = EnumSet.of(
            Material.WOODEN_PICKAXE,
            Material.STONE_PICKAXE,
            Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE,
            Material.DIAMOND_PICKAXE,
            Material.NETHERITE_PICKAXE
    );

    public Endstalactitas(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack createEndstalactita() {
        ItemStack item = new ItemStack(Material.MAGENTA_GLAZED_TERRACOTTA);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#9933ff") + "" + ChatColor.BOLD + "Endstalactita");
        meta.setCustomModelData(405);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + " ");
        lore.add(ChatColor.of("#336666") + "Una estalactita que se formó");
        lore.add(ChatColor.of("#336666") + "con los cristales del End.");
        lore.add(" ");

        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType() != Material.MAGENTA_GLAZED_TERRACOTTA) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!allowedPickaxes.contains(tool.getType())) {
            event.setCancelled(true);
            return;
        }

        event.setInstaBreak(false);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.MINING_FATIGUE,
                100,
                2,
                false,
                false,
                true
        ));

        player.playSound(block.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.6f);
        cancelFatigueTask(player);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                fatigueTasks.remove(player.getUniqueId());
            }
        };

        fatigueTasks.put(player.getUniqueId(), task);
        task.runTaskLater(plugin, 40L);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType() != Material.MAGENTA_GLAZED_TERRACOTTA) return;

        event.setDropItems(false);

        int amount;
        double rand = random.nextDouble();

        if (rand < 0.05) {
            amount = 3;
        } else if (rand < 0.3) {
            amount = 2;
        } else {
            amount = 1;
        }

        block.getWorld().dropItemNaturally(block.getLocation(), EndItems.createEnderiteNugget(amount));

        cancelFatigueTask(player);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);

        player.playSound(block.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 0.8f);
        player.spawnParticle(org.bukkit.Particle.PORTAL, block.getLocation().add(0.5, 0.5, 0.5), 20);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        if (item.getType() == Material.MAGENTA_GLAZED_TERRACOTTA) {
            event.setCancelled(true);
        }
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
            if (block.getType() == Material.MAGENTA_GLAZED_TERRACOTTA) {
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