package Events.AchievementParty;

import TitleListener.SuccessNotification;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Achievement3 implements Achievement, Listener {
    private final JavaPlugin plugin;
    private final AchievementPartyHandler eventHandler;
    private final SuccessNotification successNotification;

    public Achievement3(JavaPlugin plugin, AchievementPartyHandler eventHandler) {
        this.plugin = plugin;
        this.eventHandler = eventHandler;
        this.successNotification = new SuccessNotification(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() {
        return "Second Chance";
    }

    @Override
    public String getDescription() {
        return "Activa un tótem de los especiales";
    }

    @Override
    public void initializePlayerData(String playerName) {
        // No necesita inicialización especial
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    @EventHandler
    public void onTotemActivate(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!eventHandler.isEventActive()) return;

        Player player = (Player) event.getEntity();

        // Verificar primero si ya completó el logro
        FileConfiguration data = YamlConfiguration.loadConfiguration(eventHandler.getAchievementsFile());
        if (data.getBoolean("players." + player.getName() + ".achievements.second_chance.completed", false)) {
            return;
        }

        ItemStack totem = null;

        // Buscar el tótem en ambas manos
        if (player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING) {
            totem = player.getInventory().getItemInMainHand();
        } else if (player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) {
            totem = player.getInventory().getItemInOffHand();
        }

        if (totem != null && totem.hasItemMeta()) {
            ItemMeta meta = totem.getItemMeta();
            if (meta.hasCustomModelData()) {
                int customModelData = meta.getCustomModelData();
                // Verificar si es un tótem especial (3, 4 o 5)
                if (customModelData == 3 || customModelData == 4 || customModelData == 5) {
                    eventHandler.completeAchievement(player.getName(), "second_chance");
                    successNotification.showSuccess(player);
                }
            }
        }
    }
}