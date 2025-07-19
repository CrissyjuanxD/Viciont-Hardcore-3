package Events.AchievementParty;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class AchievementGUI implements Listener {
    private final JavaPlugin plugin;
    private final AchievementPartyHandler achievementHandler;
    private final File achievementsFile;

    // Slots donde se colocarán los logros
    private final int[] achievementSlots = {18, 19, 20, 21, 22, 23, 24, 25, 26,
            29, 30, 31, 32, 33, 40};

    public AchievementGUI(JavaPlugin plugin, AchievementPartyHandler achievementHandler) {
        this.plugin = plugin;
        this.achievementHandler = achievementHandler;
        this.achievementsFile = achievementHandler.getAchievementsFile();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openAchievementGUI(Player player) {
        // Crear inventario de doble cofre (54 slots)
        Inventory gui = Bukkit.createInventory(null, 54, "§6§lFiesta de Logros");

        // Rellenar los bordes con paneles de vidrio
        ItemStack border = createBorderItem();
        for (int i = 0; i < 54; i++) {
            // Solo poner en slots que no son para logros y no son del inventario del jugador
            if (!isAchievementSlot(i) && i < 45) {
                gui.setItem(i, border);
            }
        }

        // Obtener datos del jugador
        FileConfiguration data = YamlConfiguration.loadConfiguration(achievementsFile);
        String playerName = player.getName();

        // Obtener todos los logros registrados
        List<Achievement> allAchievements = new ArrayList<>(achievementHandler.getAchievements().values());

        // Ordenar los logros por orden de registro (logro1, logro2, etc.)
        allAchievements.sort(Comparator.comparingInt(a -> achievementHandler.getAchievementIndex(a)));

        // Colocar los logros en los slots designados
        for (int i = 0; i < Math.min(allAchievements.size(), achievementSlots.length); i++) {
            Achievement achievement = allAchievements.get(i);
            String achievementId = achievementHandler.getAchievementKey(achievement);

            boolean isCompleted = data.getBoolean("players." + playerName + ".achievements." + achievementId + ".completed", false);

            gui.setItem(achievementSlots[i], createAchievementItem(achievement, isCompleted, playerName));
        }

        // Abrir la GUI al jugador
        player.openInventory(gui);
    }

    private ItemStack createAchievementItem(Achievement achievement, boolean isCompleted, String playerName) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        // Asignar nombre y lore básico
        meta.setDisplayName((isCompleted ? "§a" : "§7") + achievement.getName());

        List<String> lore = new ArrayList<>();
        lore.add("§7" + achievement.getDescription());
        lore.add("");
        lore.add(isCompleted ? "§a✔ Completado" : "§c✖ Pendiente");

        // Manejar logros con listas (como el de las flores)
        if (achievement instanceof Achievement2) {
            lore.add("");
            lore.add("§eProgreso de flores:");

            FileConfiguration data = YamlConfiguration.loadConfiguration(achievementsFile);
            Achievement2 flowerAchievement = (Achievement2) achievement;

            for (Material flower : flowerAchievement.getRequiredFlowers()) {
                boolean hasFlower = data.getBoolean(
                        "players." + playerName + ".achievements.collect_all_flowers.collected." + flower.name(), false);

                lore.add((hasFlower ? "§a" : "§7") + "- " + formatMaterialName(flower));
            }
        }

        // Manejar logros con listas (achievement5)
        if (achievement instanceof Achievement5) {
            lore.add("");
            lore.add("§eProgreso de bloques:");

            FileConfiguration data = YamlConfiguration.loadConfiguration(achievementsFile);
            Achievement5 blockAchievement = (Achievement5) achievement;

            for (Material block : blockAchievement.getRequiredBlocks()) {
                boolean hasBlock = data.getBoolean(
                        "players." + playerName + ".achievements.touch_grass.broken." + block.name(), false);

                lore.add((hasBlock ? "§a" : "§7") + "- " + formatMaterialName(block));
            }
        }

        // En el método createAchievementItem, añadir este caso:
        if (achievement instanceof Achievement8) {
            lore.add("");
            lore.add("§eProgreso de chilladores:");

            FileConfiguration data = YamlConfiguration.loadConfiguration(achievementsFile);

            int broken = data.getInt("players." + playerName + ".achievements.sculk_shrieker.broken", 0);
            lore.add("§7- Rotos: §a" + broken + "§7/§a" + Achievement8.REQUIRED_SCULK_SHRIEKERS);
        }

        meta.setLore(lore);

        // Asignar CustomModelData (empezando desde 2)
        int achievementIndex = achievementHandler.getAchievementIndex(achievement);
        meta.setCustomModelData(achievementIndex + 1); // +1 porque el primero es 2

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBorderItem() {
        ItemStack border = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName(" ");
        meta.setCustomModelData(2);
        border.setItemMeta(meta);
        return border;
    }

    private boolean isAchievementSlot(int slot) {
        for (int achievementSlot : achievementSlots) {
            if (slot == achievementSlot) {
                return true;
            }
        }
        return false;
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§6§lFiesta de Logros")) {
            event.setCancelled(true); // Cancelar cualquier interacción
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Puedes agregar lógica adicional si es necesario
    }
}
