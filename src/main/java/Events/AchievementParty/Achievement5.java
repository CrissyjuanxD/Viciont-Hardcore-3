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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Achievement5 implements Achievement, Listener {
    private final JavaPlugin plugin;
    private final AchievementPartyHandler eventHandler;
    private final SuccessNotification successNotification;
    private final Set<Material> requiredBlocks = new HashSet<>(Arrays.asList(
            Material.IRON_BLOCK,
            Material.GOLD_BLOCK,
            Material.EMERALD_BLOCK,
            Material.DIAMOND_BLOCK
    ));

    public Achievement5(JavaPlugin plugin, AchievementPartyHandler eventHandler) {
        this.plugin = plugin;
        this.eventHandler = eventHandler;
        this.successNotification = new SuccessNotification(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() {
        return "Ve a tocar pasto";
    }

    @Override
    public String getDescription() {
        return "Rompe Bloques de Hierro, Oro, Esmeralda y Diamante con Mining Fatigue III";
    }

    @Override
    public void initializePlayerData(String playerName) {
        FileConfiguration data = YamlConfiguration.loadConfiguration(eventHandler.getAchievementsFile());

        // Inicializar el progreso de bloques rotos
        for (Material block : requiredBlocks) {
            data.set("players." + playerName + ".achievements.touch_grass.broken." + block.name(), false);
        }

        try {
            data.save(eventHandler.getAchievementsFile());
        } catch (Exception e) {
            plugin.getLogger().severe("Error al inicializar datos de bloques: " + e.getMessage());
        }
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    public Set<Material> getRequiredBlocks() {
        return requiredBlocks;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!eventHandler.isEventActive()) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Verificar primero si ya completó el logro principal
        FileConfiguration data = YamlConfiguration.loadConfiguration(eventHandler.getAchievementsFile());
        if (data.getBoolean("players." + player.getName() + ".achievements.touch_grass.completed", false)) {
            return;
        }

        // Verificar si el bloque es uno de los requeridos
        if (requiredBlocks.contains(block.getType())) {
            // Verificar si tiene Mining Fatigue III
            PotionEffect effect = player.getPotionEffect(PotionEffectType.MINING_FATIGUE);
            if (effect != null && effect.getAmplifier() >= 2) { // Nivel III es amplificador 2
                String path = "players." + player.getName() + ".achievements.touch_grass.broken." + block.getType().name();

                // Marcar el bloque como roto si no lo estaba
                if (!data.getBoolean(path, false)) {
                    data.set(path, true);

                    try {
                        data.save(eventHandler.getAchievementsFile());
                        player.sendMessage("§eHas roto un bloque de " + formatMaterialName(block.getType()) + " con Mining Fatigue III");
                        successNotification.showSuccess(player);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error al guardar progreso de bloque: " + e.getMessage());
                        return;
                    }

                    // Verificar si ha roto todos los bloques requeridos
                    checkBlockCompletion(player, data);
                }
            }
        }
    }

    private void checkBlockCompletion(Player player, FileConfiguration data) {
        boolean allBroken = true;
        int brokenCount = 0;

        for (Material block : requiredBlocks) {
            if (data.getBoolean("players." + player.getName() + ".achievements.touch_grass.broken." + block.name(), false)) {
                brokenCount++;
            } else {
                allBroken = false;
            }
        }

        player.sendMessage("§eBloques rotos: §a" + brokenCount + "§e/§a" + requiredBlocks.size());

        if (allBroken) {
            if (eventHandler.completeAchievement(player.getName(), "touch_grass")) {
            }
        }
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}