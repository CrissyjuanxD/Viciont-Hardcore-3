package Events.AchievementParty;

import TitleListener.SuccessNotification;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;

public class Achievement8 implements Achievement, Listener {
    private final JavaPlugin plugin;
    private final AchievementPartyHandler eventHandler;
    private final SuccessNotification successNotification;
    public static final int REQUIRED_SCULK_SHRIEKERS = 15;

    public Achievement8(JavaPlugin plugin, AchievementPartyHandler eventHandler) {
        this.plugin = plugin;
        this.eventHandler = eventHandler;
        this.successNotification = new SuccessNotification(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() {
        return "Jugando a ser músico";
    }

    @Override
    public String getDescription() {
        return "Rompe 15 chilladores en el bioma de Deep Dark";
    }

    @Override
    public void initializePlayerData(String playerName) {
        FileConfiguration data = YamlConfiguration.loadConfiguration(eventHandler.getAchievementsFile());
        data.set("players." + playerName + ".achievements.sculk_shrieker.broken", 0);

        try {
            data.save(eventHandler.getAchievementsFile());
        } catch (IOException e) {
            plugin.getLogger().severe("Error al inicializar datos de chilladores: " + e.getMessage());
        }
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante el evento de romper bloques
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!eventHandler.isEventActive()) return;

        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Verificar que es un Sculk Shrieker y está en Deep Dark
        if (block.getType() != Material.SCULK_SHRIEKER ||
                !block.getBiome().name().equalsIgnoreCase("DEEP_DARK")) {
            return;
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(eventHandler.getAchievementsFile());

        // Verificar si ya completó el logro principal
        if (data.getBoolean("players." + player.getName() + ".achievements.sculk_shrieker.completed", false)) {
            return;
        }

        // Incrementar contador
        int broken = data.getInt("players." + player.getName() + ".achievements.sculk_shrieker.broken", 0);
        broken++;
        data.set("players." + player.getName() + ".achievements.sculk_shrieker.broken", broken);

        try {
            data.save(eventHandler.getAchievementsFile());
            player.sendMessage("§eChilladores rotos: §a" + broken + "§e/§a" + REQUIRED_SCULK_SHRIEKERS);
            successNotification.showSuccess(player);

            // Eliminar el bloque para que no pueda ser reutilizado
            block.setType(Material.AIR);

            if (broken >= REQUIRED_SCULK_SHRIEKERS) {
                eventHandler.completeAchievement(player.getName(), "sculk_shrieker");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar progreso de chilladores: " + e.getMessage());
        }
    }
}