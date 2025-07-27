package Armors;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class NightVisionHelmet implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Integer> sneakCountMap = new HashMap<>();
    private final Map<UUID, BossBar> bossBarMap = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activeTasks = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public NightVisionHelmet(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack createNightVisionHelmet() {
        ItemStack item = new ItemStack(Material.IRON_HELMET);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(2);

        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Casco de Visión Nocturna");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Casco para afrontar la oscuridad",
                ChatColor.GRAY + "de este mundo.",
                "",
                ChatColor.GRAY + "" + ChatColor.BOLD + "Shift x3 para activar/desactivar",
                "",
                ChatColor.DARK_PURPLE +  "" + ChatColor.BOLD + ">> Habilidad: " + ChatColor.RESET + ChatColor.DARK_PURPLE + "Visión Nocturna.",
                ChatColor.DARK_GRAY +  "" + ChatColor.BOLD + ">> OJO: " + ChatColor.RESET + ChatColor.DARK_GRAY + "Cada 5 segundos de tener la Vision Activada",
                ChatColor.DARK_GRAY + "se gasta 1 de durabilidad.",
                ""
        ));

        meta.setUnbreakable(false);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        item.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);

        return item;
    }

    public static ItemStack createNightVisionHelmetPlus() {
        ItemStack item = new ItemStack(Material.NETHERITE_HELMET);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(100);

        meta.setDisplayName(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Casco de Visión Nocturna Mejorado");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Casco para afrontar la oscuridad",
                ChatColor.GRAY + "de este mundo.",
                "",
                ChatColor.GRAY + "" + ChatColor.BOLD + "Shift x3 para activar/desactivar",
                "",
                ChatColor.DARK_PURPLE +  "" + ChatColor.BOLD + ">> Habilidad: " + ChatColor.RESET + ChatColor.DARK_PURPLE + "Visión Nocturna.",
                ChatColor.DARK_GRAY +  "" + ChatColor.BOLD + ">> OJO: " + ChatColor.RESET + ChatColor.DARK_GRAY + "Cada 5 segundos de tener la Vision Activada",
                ChatColor.DARK_GRAY + "se gasta 1 de durabilidad.",
                ""
        ));

        meta.setUnbreakable(false);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        item.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);

        return item;
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        ItemStack helmet = player.getInventory().getHelmet();

        if (!isValidHelmet(helmet)) return;

        if (event.isSneaking()) {
            int count = sneakCountMap.getOrDefault(player.getUniqueId(), 0) + 1;
            sneakCountMap.put(player.getUniqueId(), count);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (sneakCountMap.getOrDefault(player.getUniqueId(), 0) == count) {
                    sneakCountMap.remove(player.getUniqueId());
                }
            }, 40L);

            if (count == 3) {
                sneakCountMap.remove(player.getUniqueId());
                toggleNightVision(player, helmet);
            }
        }
    }


    private boolean isValidHelmet(ItemStack helmet) {
        return helmet != null && helmet.hasItemMeta()
                && helmet.getItemMeta().hasDisplayName()
                && helmet.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "" + ChatColor.BOLD + "Casco de Visión Nocturna");
    }

    private void toggleNightVision(Player player, ItemStack helmet) {
        UUID uuid = player.getUniqueId();
        
        if (cooldowns.containsKey(uuid)) {
            long elapsed = System.currentTimeMillis() - cooldowns.get(uuid);
            if (elapsed < 6000) {
                return;
            }
        }

        if (activeTasks.containsKey(uuid)) {
            deactivateNightVision(player, true);
            player.sendMessage(ChatColor.RED + "Visión Nocturna desactivada");
        } else {
            activateNightVision(player, helmet);
            player.sendMessage(ChatColor.GREEN + "Visión Nocturna activada");
        }
    }

    private void activateNightVision(Player player, ItemStack helmet) {
        UUID uuid = player.getUniqueId();

        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 250, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 250, 0, false, false));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);

        BossBar bar = Bukkit.createBossBar(ChatColor.DARK_GREEN + "\uEAA5", BarColor.WHITE, BarStyle.SOLID);
        bar.addPlayer(player);
        bar.setVisible(true);
        bossBarMap.put(uuid, bar);

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !isValidHelmet(player.getInventory().getHelmet())) {
                    deactivateNightVision(player, false);
                    return;
                }

                ticks += 20;

                if (ticks % 100 == 0) {
                    damageHelmet(player.getInventory().getHelmet(), player);
                }

                if (ticks >= 20 * 250) {
                    deactivateNightVision(player, false);
                }
            }
        };

        task.runTaskTimer(plugin, 0, 20);
        activeTasks.put(uuid, task);
    }


    private void damageHelmet(ItemStack helmet, Player player) {
        helmet.setDurability((short) (helmet.getDurability() + 1));

        if (helmet.getType().getMaxDurability() - helmet.getDurability() <= 1) {
            player.getInventory().setHelmet(null);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            player.sendMessage(ChatColor.DARK_PURPLE + "✧ ¡Tu casco de visión nocturna se ha roto!");
        }
    }

    private void deactivateNightVision(Player player, boolean wasManual) {
        UUID uuid = player.getUniqueId();

        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.NAUSEA);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);

        if (bossBarMap.containsKey(uuid)) {
            bossBarMap.get(uuid).removeAll();
            bossBarMap.remove(uuid);
        }

        if (activeTasks.containsKey(uuid)) {
            activeTasks.get(uuid).cancel();
            activeTasks.remove(uuid);
        }

        cooldowns.put(uuid, System.currentTimeMillis());
        player.setCooldown(Material.IRON_HELMET, 100);

    }

    @EventHandler
    public void onEnchant(org.bukkit.event.enchantment.EnchantItemEvent event) {
        ItemStack item = event.getItem();
        if (isValidHelmet(item)) {
            if (event.getEnchantsToAdd().containsKey(Enchantment.MENDING)
                    || event.getEnchantsToAdd().containsKey(Enchantment.UNBREAKING)) {
                event.setCancelled(true);
                event.getEnchanter().sendMessage(ChatColor.RED + "Este casco no puede recibir esos encantamientos.");
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack helmet = player.getInventory().getHelmet();

        if (isValidHelmet(helmet)) {
            fixHelmetEnchantments(helmet);
        }
    }

    private void fixHelmetEnchantments(ItemStack helmet) {
        if (helmet == null || !helmet.hasItemMeta()) return;

        Map<Enchantment, Integer> enchants = new HashMap<>(helmet.getEnchantments());

        if (enchants.size() != 1 || !enchants.containsKey(Enchantment.UNBREAKING) || enchants.get(Enchantment.UNBREAKING) != 1) {
            for (Enchantment ench : enchants.keySet()) {
                helmet.removeEnchantment(ench);
            }
            helmet.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
        }
    }


    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        deactivateNightVision(player, false);
    }
}
