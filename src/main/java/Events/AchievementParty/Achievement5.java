package Events.AchievementParty;

import Events.MissionSystem.MissionData;
import TitleListener.SuccessNotification;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Achievement5 implements Achievement, Listener {
    private final AchievementPartyHandler eventHandler;
    private final SuccessNotification successNotification;
    private final Set<Material> requiredBlocks = new HashSet<>(Arrays.asList(
            Material.COPPER_BLOCK,
            Material.IRON_BLOCK,
            Material.GOLD_BLOCK,
            Material.EMERALD_BLOCK,
            Material.DIAMOND_BLOCK,
            Material.NETHERITE_BLOCK
    ));

    public Achievement5(JavaPlugin plugin, AchievementPartyHandler eventHandler) {
        this.eventHandler = eventHandler;
        this.successNotification = new SuccessNotification(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() { return "Ve a tocar pasto"; }

    @Override
    public String getDescription() { return "Rompe Bloques de Cobre, Hierro, Oro, Esmeralda, Diamante y Netherite con Mining Fatigue III"; }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    public Set<Material> getRequiredBlocks() { return requiredBlocks; }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!eventHandler.isEventActive()) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        MissionData data = eventHandler.getData(player, "touch_grass");
        if (data.isCompleted()) return;

        if (requiredBlocks.contains(block.getType())) {
            PotionEffect effect = player.getPotionEffect(PotionEffectType.MINING_FATIGUE);

            if (effect != null && effect.getAmplifier() >= 2) {
                String key = "broken_" + block.getType().name();

                if (!data.getProgressBool(key)) {
                    data.setProgressValue(key, true);
                    eventHandler.saveData(player, "touch_grass", data);

                    player.sendMessage("§eHas roto un bloque de " + formatMaterialName(block.getType()) + " con Mining Fatigue III");
                    successNotification.showSuccess(player);

                    checkBlockCompletion(player, data);
                }
            }
        }
    }

    private void checkBlockCompletion(Player player, MissionData data) {
        int brokenCount = 0;
        for (Material block : requiredBlocks) {
            if (data.getProgressBool("broken_" + block.name())) {
                brokenCount++;
            }
        }

        player.sendMessage("§eBloques rotos: §a" + brokenCount + "§e/§a" + requiredBlocks.size());

        if (brokenCount >= requiredBlocks.size()) {
            eventHandler.completeAchievement(player, "touch_grass");
        }
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}