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
import org.bukkit.event.player.PlayerChangedWorldEvent; // Importante para detectar cambio de dimensión
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

    // Nuevo Evento: Detectar cambio de mundo para actualizar el color si tiene la visión activa
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Si tiene la bossbar activa, la actualizamos
        if (bossBarMap.containsKey(uuid)) {
            BossBar bar = bossBarMap.get(uuid);
            ChatColor color = getDimensionColor(player.getWorld());
            bar.setTitle(color + "\uEAA5");
        }
    }

    private boolean isValidHelmet(ItemStack helmet) {
        return helmet != null && helmet.hasItemMeta()
                && helmet.getItemMeta().hasDisplayName()
                && (helmet.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "" + ChatColor.BOLD + "Casco de Visión Nocturna") ||
                helmet.getItemMeta().getDisplayName().equals(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Casco de Visión Nocturna Mejorado"));
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
        } else {
            activateNightVision(player, helmet);
        }
    }

    // Método auxiliar para obtener el color según la dimensión
    private ChatColor getDimensionColor(World world) {
        switch (world.getEnvironment()) {
            case NORMAL: // Overworld
                return ChatColor.DARK_GREEN;
            case NETHER: // Nether
                return ChatColor.DARK_RED;
            case THE_END: // The End
                return ChatColor.DARK_PURPLE;
            case CUSTOM: // Dimensiones custom
                if (world.getName().equalsIgnoreCase("infested_caves")) {
                    return ChatColor.AQUA;
                }
                return ChatColor.GRAY; // Color por defecto para custom
            default:
                return ChatColor.DARK_GREEN;
        }
    }

    private void activateNightVision(Player player, ItemStack helmet) {
        UUID uuid = player.getUniqueId();

        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, PotionEffect.INFINITE_DURATION, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, false, false));
        player.playSound(player.getLocation(), "custom.vision_nocturna", SoundCategory.VOICE, 1.0f, 1.4f);

        // Aquí obtenemos el color dinámico
        ChatColor dimensionColor = getDimensionColor(player.getWorld());

        BossBar bar = Bukkit.createBossBar(dimensionColor + "\uEAA5", BarColor.WHITE, BarStyle.SOLID);
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

                // Opcional: Actualizar el color periódicamente si cambian de mundo sin el evento
                // ChatColor currentColor = getDimensionColor(player.getWorld());
                // if (!bar.getTitle().startsWith(currentColor.toString())) {
                //     bar.setTitle(currentColor + "\uEAA5");
                // }

                ticks += 20;

                if (ticks % 100 == 0) {
                    damageHelmet(player.getInventory().getHelmet(), player);
                }
            }
        };

        task.runTaskTimer(plugin, 0, 20);
        activeTasks.put(uuid, task);
    }


    private void damageHelmet(ItemStack helmet, Player player) {
        if (helmet == null) return; // Seguridad extra

        helmet.setDurability((short) (helmet.getDurability() + 1));

        if (helmet.getType().getMaxDurability() - helmet.getDurability() <= 1) {
            player.getInventory().setHelmet(null);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            player.sendMessage(ChatColor.DARK_PURPLE + "✧ ¡Tu casco de visión nocturna se ha roto!");
            deactivateNightVision(player, false); // Importante desactivar la visión si se rompe
        }
    }

    private void deactivateNightVision(Player player, boolean wasManual) {
        UUID uuid = player.getUniqueId();

        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.NAUSEA);
        player.playSound(player.getLocation(), "custom.vision_nocturna", SoundCategory.VOICE,1.0f, 0.6f);

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
        player.setCooldown(Material.NETHERITE_HELMET, 100); // Añadido cooldown para la versión netherite también
    }

    @EventHandler
    public void onEnchant(org.bukkit.event.enchantment.EnchantItemEvent event) {
        ItemStack item = event.getItem();
        if (isValidHelmet(item)) {
            if (event.getEnchantsToAdd().containsKey(Enchantment.MENDING)) {
                event.setCancelled(true);
                event.getEnchanter().sendMessage(ChatColor.RED + "Este casco no puede recibir esos encantamientos.");
            }
        }
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (activeTasks.containsKey(player.getUniqueId())) {
            deactivateNightVision(player, false);
        }
    }
}