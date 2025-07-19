package Events.AchievementParty;

import TitleListener.SuccessNotification;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Achievement2 implements Achievement, Listener {
    private final JavaPlugin plugin;
    private final AchievementPartyHandler eventHandler;
    private final SuccessNotification successNotification;
    private final Set<Material> requiredFlowers = new HashSet<>(Arrays.asList(
            Material.POPPY,
            Material.DANDELION,
            Material.BLUE_ORCHID,
            Material.ALLIUM,
            Material.AZURE_BLUET,
            Material.RED_TULIP,
            Material.ORANGE_TULIP,
            Material.WHITE_TULIP,
            Material.PINK_TULIP,
            Material.OXEYE_DAISY,
            Material.CORNFLOWER,
            Material.LILY_OF_THE_VALLEY,
            Material.WITHER_ROSE,
            Material.SUNFLOWER,
            Material.LILAC,
            Material.ROSE_BUSH,
            Material.PEONY,
            Material.SPORE_BLOSSOM,
            Material.PINK_PETALS,
            Material.PITCHER_PLANT,
            Material.TORCHFLOWER
    ));

    public Achievement2(JavaPlugin plugin, AchievementPartyHandler eventHandler) {
        this.plugin = plugin;
        this.eventHandler = eventHandler;
        this.successNotification = new SuccessNotification(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() {
        return "Stardew Valley";
    }

    @Override
    public String getDescription() {
        return "Consigue todas las flores del juego";
    }

    @Override
    public void initializePlayerData(String playerName) {
        FileConfiguration data = YamlConfiguration.loadConfiguration(eventHandler.getAchievementsFile());

        // Inicializar el progreso de flores recolectadas
        for (Material flower : requiredFlowers) {
            data.set("players." + playerName + ".achievements.collect_all_flowers.collected." + flower.name(), false);
        }

        try {
            data.save(eventHandler.getAchievementsFile());
        } catch (IOException e) {
            plugin.getLogger().severe("Error al inicializar datos de flores: " + e.getMessage());
        }
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante el evento de recoger items
    }

    public Set<Material> getRequiredFlowers() {
        return requiredFlowers;
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!eventHandler.isEventActive()) return;

        Player player = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();
        Material itemType = item.getType();

        // Verificar primero si ya completó el logro principal
        FileConfiguration data = YamlConfiguration.loadConfiguration(eventHandler.getAchievementsFile());
        if (data.getBoolean("players." + player.getName() + ".achievements.collect_all_flowers.completed", false)) {
            return;
        }

        if (requiredFlowers.contains(itemType)) {
            String path = "players." + player.getName() + ".achievements.collect_all_flowers.collected." + itemType.name();

            // Marcar la flor como recolectada si no lo estaba
            if (!data.getBoolean(path, false)) {
                data.set(path, true);

                try {
                    data.save(eventHandler.getAchievementsFile());
                    player.sendMessage("§eHaz recolectado la flor: " + formatMaterialName(itemType));
                    successNotification.showSuccess(player);
                } catch (IOException e) {
                    plugin.getLogger().severe("Error al guardar progreso de flor: " + e.getMessage());
                    return;
                }

                // Verificar progreso
                checkFlowerCompletion(player, data);
            }
        }
    }

    private void checkFlowerCompletion(Player player, FileConfiguration data) {
        boolean allCollected = true;
        int collectedCount = 0;

        for (Material flower : requiredFlowers) {
            if (data.getBoolean("players." + player.getName() + ".achievements.collect_all_flowers.collected." + flower.name(), false)) {
                collectedCount++;
            } else {
                allCollected = false;
            }
        }

        player.sendMessage("§eFlores recolectadas: §a" + collectedCount + "§e/§a" + requiredFlowers.size());

        if (allCollected) {
            // Usamos el nuevo método que verifica internamente si ya estaba completado
            if (eventHandler.completeAchievement(player.getName(), "collect_all_flowers")) {
            }
        }
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}