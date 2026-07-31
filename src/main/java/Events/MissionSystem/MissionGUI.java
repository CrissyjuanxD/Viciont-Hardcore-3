/*
package Events.MissionSystem;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MissionGUI implements Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final String guiTitle;

    private final ItemStack purpleDye;
    private final ItemStack blackDye;
    private final ItemStack nextArrow;
    private final ItemStack prevArrow;

    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final int MAX_PAGES = 3;

    // Disposición exacta del Quaso Plugin
    private final int[] MISSION_SLOTS = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            37, 38, 39, 40, 41, 42, 43
    };

    public MissionGUI(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.guiTitle = ChatColor.of("#FFA500") + "Misiones";

        this.purpleDye = createDecorativeItem(Material.PURPLE_DYE);
        this.blackDye = createDecorativeItem(Material.BLACK_DYE);

        this.nextArrow = createArrow("§eSiguiente Página ➔");
        this.prevArrow = createArrow("§e⬅ Anterior Página");

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private ItemStack createDecorativeItem(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createArrow(String name) {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onItemInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.MAP) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) return;

        if (item.getItemMeta().getCustomModelData() == 9999) {
            event.setCancelled(true);
            openMissionGUI(event.getPlayer(), 1);
        }
    }

    public void openMissionGUI(Player player) {
        openMissionGUI(player, 1);
    }

    public void openMissionGUI(Player player, int page) {
        playerPages.put(player.getUniqueId(), page);
        Inventory gui = Bukkit.createInventory(null, 54, guiTitle + " - Pág " + page);

        int[] purpleSlots = {0, 4, 8, 45, 49, 53};
        for (int slot : purpleSlots) {
            gui.setItem(slot, purpleDye);
        }

        int[] blackSlots = {1, 2, 3, 5, 6, 7, 46, 47, 48, 50, 51, 52};
        for (int slot : blackSlots) {
            gui.setItem(slot, blackDye);
        }

        gui.setItem(36, prevArrow);
        gui.setItem(44, nextArrow);

        Map<Integer, Mission> allMissions = missionHandler.getMissions();
        int startIndex = (page - 1) * MISSION_SLOTS.length;

        for (int i = 0; i < MISSION_SLOTS.length; i++) {
            int slot = MISSION_SLOTS[i];
            int missionNum = startIndex + i + 1;

            if (allMissions.containsKey(missionNum)) {
                Mission mission = allMissions.get(missionNum);
                MissionData data = missionHandler.getData(player, missionNum);

                boolean isActive = data.isActive();
                boolean isCompleted = data.isCompleted();

                gui.setItem(slot, createMissionItem(mission, isActive, isCompleted, data));
            } else {
                gui.setItem(slot, createUnknownMissionItem());
            }
        }

        player.openInventory(gui);
    }

    private ItemStack createMissionItem(Mission mission, boolean isActive, boolean isCompleted, MissionData data) {
        ItemStack item;
        ItemMeta meta;
        String displayName;
        int customModelData = 0;

        // Estética de Ítems de Quaso combinada con colores de Viciont
        if (!isActive) {
            item = new ItemStack(Material.MAP);
            displayName = ChatColor.of("#A0A0A0") + "???";
            customModelData = 2002;
        } else if (isCompleted) {
            item = new ItemStack(Material.LIME_BANNER);
            org.bukkit.inventory.meta.BannerMeta bannerMeta = (org.bukkit.inventory.meta.BannerMeta) item.getItemMeta();
            if (bannerMeta != null) {
                bannerMeta.addPattern(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.WHITE, org.bukkit.block.banner.PatternType.FLOWER));
                item.setItemMeta(bannerMeta);
            }
            displayName = ChatColor.of("#90EE90") + mission.getName();
            customModelData = 2000;
        } else {
            item = new ItemStack(Material.GUSTER_BANNER_PATTERN);
            displayName = ChatColor.of("#FFB6C1") + mission.getName();
            customModelData = 2001;
        }

        meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setCustomModelData(customModelData);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        List<String> lore = new ArrayList<>();

        if (isActive) {
            String[] descriptionLines = mission.getDescription().split("\n");
            for (String line : descriptionLines) {
                lore.add(ChatColor.of("#D3D3D3") + line);
            }

            lore.add("");
            lore.add(isCompleted ? ChatColor.of("#98FB98") + "✔ Completada" : ChatColor.of("#FFA07A") + "✖ Pendiente");

            // Agregar progreso específico de Viciont Hardcore
            addMissionSpecificProgress(mission, data, lore);
        } else {
            lore.add(ChatColor.of("#D3D3D3") + "Misión no descubierta");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createUnknownMissionItem() {
        ItemStack item = new ItemStack(Material.MAP);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#A0A0A0") + "???");
        meta.setCustomModelData(2002);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.of("#D3D3D3") + "Misión no implementada");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private void addMissionSpecificProgress(Mission mission, MissionData data, List<String> lore) {
        if (mission instanceof Mission1) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de armadura:");

            String[] armorPieces = {"helmet", "chestplate", "leggings", "boots"};
            String[] armorNames = {"Casco", "Peto", "Pantalones", "Botas"};

            for (int i = 0; i < armorPieces.length; i++) {
                boolean hasEnchant = data.getProgressBool("protection_" + armorPieces[i]);
                lore.add((hasEnchant ? ChatColor.of("#98FB98") : ChatColor.of("#D3D3D3")) + "- " + armorNames[i] + " con Protección IV");
            }
        } else if (mission instanceof Mission2) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de preparación:");

            boolean raidCompleted = data.getProgressBool("raid_completed");
            int goldenApplesCrafted = data.getProgressInt("golden_apples_crafted");

            lore.add((raidCompleted ? ChatColor.of("#98FB98") : ChatColor.of("#D3D3D3")) + "- Raid completada");
            lore.add(ChatColor.of("#DDA0DD") + "- Manzanas de oro: " + ChatColor.of("#98FB98") + goldenApplesCrafted + ChatColor.of("#D3D3D3") + "/40");
        } else if (mission instanceof Mission3) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de combate:");

            String statusSymbol = data.isCompleted() ? ChatColor.of("#98FB98") + "✔" : ChatColor.of("#FFA07A") + "✖";
            lore.add(ChatColor.of("#D3D3D3") + "- Reina Derrotada: " + statusSymbol);
        } else if (mission instanceof Mission5) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de armadura:");

            String[] armorPieces = {"helmet", "chestplate", "leggings", "boots"};
            String[] armorNames = {"Casco", "Peto", "Pantalones", "Botas"};

            for (int i = 0; i < armorPieces.length; i++) {
                boolean hasArmor = data.getProgressBool("netherite_armor_" + armorPieces[i]);
                boolean hasProtection = data.getProgressBool("protection_" + armorPieces[i]);

                if (hasArmor && hasProtection) {
                    lore.add(ChatColor.of("#98FB98") + "- " + armorNames[i] + " de Netherite con Protección IV");
                } else if (hasArmor) {
                    lore.add(ChatColor.of("#F0E68C") + "- " + armorNames[i] + " de Netherite (sin Protección IV)");
                } else {
                    lore.add(ChatColor.of("#D3D3D3") + "- " + armorNames[i] + " de Netherite con Protección IV");
                }
            }
        } else if (mission instanceof Mission6) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de eliminaciones:");

            int zombiesKilled = data.getProgressInt("corrupted_zombies_killed");
            int spidersKilled = data.getProgressInt("corrupted_spiders_killed");

            lore.add(ChatColor.of("#DDA0DD") + "- Corrupted Zombies: " + ChatColor.of("#98FB98") + zombiesKilled + ChatColor.of("#D3D3D3") + "/25");
            lore.add(ChatColor.of("#DDA0DD") + "- Corrupted Spiders: " + ChatColor.of("#98FB98") + spidersKilled + ChatColor.of("#D3D3D3") + "/25");
        } else if (mission instanceof Mission7) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de eliminaciones:");

            int skeletonsKilled = data.getProgressInt("corrupted_skeletons_killed");
            int creepersKilled = data.getProgressInt("corrupted_creepers_killed");

            lore.add(ChatColor.of("#DDA0DD") + "- Corrupted Skeletons: " + ChatColor.of("#98FB98") + skeletonsKilled + ChatColor.of("#D3D3D3") + "/30");
            lore.add(ChatColor.of("#DDA0DD") + "- Corrupted Creepers: " + ChatColor.of("#98FB98") + creepersKilled + ChatColor.of("#D3D3D3") + "/30");
        } else if (mission instanceof Mission8) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de armadura corrupta:");

            String[] armorPieces = {"helmet", "chestplate", "leggings", "boots"};
            String[] armorNames = {"Casco", "Peto", "Pantalones", "Botas"};

            for (int i = 0; i < armorPieces.length; i++) {
                boolean hasArmor = data.getProgressBool("corrupted_armor_" + armorPieces[i]);
                lore.add((hasArmor ? ChatColor.of("#98FB98") : ChatColor.of("#D3D3D3")) + "- " + armorNames[i] + " Corrupto");
            }
        } else if (mission instanceof Mission9) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de raids:");

            int raidsCompleted = data.getProgressInt("raids_completed");
            lore.add(ChatColor.of("#DDA0DD") + "- Raids completadas: " + ChatColor.of("#98FB98") + raidsCompleted + ChatColor.of("#D3D3D3") + "/5");
        } else if (mission instanceof Mission10) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de totems:");

            boolean hasInfernal = data.getProgressBool("totems_infernal");
            boolean hasSpider = data.getProgressBool("totems_spider");
            boolean hasLife = data.getProgressBool("totems_life");

            lore.add((hasInfernal ? ChatColor.of("#98FB98") : ChatColor.of("#D3D3D3")) + "- Infernal Totem");
            lore.add((hasSpider ? ChatColor.of("#98FB98") : ChatColor.of("#D3D3D3")) + "- Spider Totem");
            lore.add((hasLife ? ChatColor.of("#98FB98") : ChatColor.of("#D3D3D3")) + "- Life Totem");
        } else if (mission instanceof Mission11) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de tiempo:");

            long timeInMushroom = data.getProgressLong("time_in_mushroom");
            long requiredTime = 23500;

            lore.add(ChatColor.of("#DDA0DD") + "- Tiempo en Mushroom Island: " +
                    ChatColor.of("#98FB98") + timeInMushroom + ChatColor.of("#D3D3D3") + "/" + requiredTime);
        } else if (mission instanceof Mission12) {
            lore.add("");
            lore.add(ChatColor.of("#F0E68C") + "Progreso de eliminaciones:");

            int bombitasKilled = data.getProgressInt("bombitas_killed");
            int brutesKilled = data.getProgressInt("brutes_killed");

            lore.add(ChatColor.of("#DDA0DD") + "- Bombitas: " + ChatColor.of("#98FB98") + bombitasKilled + ChatColor.of("#D3D3D3") + "/30");
            lore.add(ChatColor.of("#DDA0DD") + "- Brutes Imperiales: " + ChatColor.of("#98FB98") + brutesKilled + ChatColor.of("#D3D3D3") + "/20");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith(guiTitle)) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player player)) return;

            int slot = event.getRawSlot();
            int currentPage = playerPages.getOrDefault(player.getUniqueId(), 1);

            if (slot == 36) {
                // Rotación hacia atrás
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                int newPage = (currentPage == 1) ? MAX_PAGES : currentPage - 1;
                openMissionGUI(player, newPage);
            } else if (slot == 44) {
                // Rotación hacia adelante
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                int newPage = (currentPage == MAX_PAGES) ? 1 : currentPage + 1;
                openMissionGUI(player, newPage);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().startsWith(guiTitle)) {
            event.setCancelled(true);
        }
    }
}*/
