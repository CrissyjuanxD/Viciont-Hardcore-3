package Events.MissionSystem;

import TitleListener.SuccessNotification;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Mission3 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;

    public Mission3(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
    }

    @Override
    public String getName() {
        return "Preparación";
    }

    @Override
    public String getDescription() {
        return "Completa una Raid de Pillagers y craftea 20 manzanas de oro";
    }

    @Override
    public int getMissionNumber() {
        return 3;
    }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        // 10 Vithiums
        ItemStack vithiums = EconomyItems.createVithiumCoin();
        vithiums.setAmount(10);
        rewards.add(vithiums);

        // 5 Manzanas doradas
        rewards.add(new ItemStack(Material.GOLDEN_APPLE, 5));

        // 1 Notch Apple
        rewards.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));

        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        data.set("players." + playerName + ".missions.3.raid_completed", false);
        data.set("players." + playerName + ".missions.3.golden_apples_crafted", 0);

        try {
            data.save(missionHandler.getMissionFile());
        } catch (IOException e) {
            plugin.getLogger().severe("Error al inicializar datos de Misión 3: " + e.getMessage());
        }
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    @EventHandler
    public void onRaidFinish(RaidFinishEvent event) {
        if (!missionHandler.isMissionActive(3)) return;

        // Verificar que la raid fue exitosa (no fue derrotada)
        if (event.getRaid().getStatus() != org.bukkit.Raid.RaidStatus.VICTORY) {
            return;
        }

        // Encontrar jugadores que participaron en la raid
        List<Player> participants = event.getWinners();

        for (Player player : participants) {
            String playerName = player.getName();

            // Verificar si ya completó la misión
            FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
            if (data.getBoolean("players." + playerName + ".missions.3.completed", false)) {
                continue;
            }

            // Marcar como completada
            data.set("players." + playerName + ".missions.3.raid_completed", true);

            try {
                data.save(missionHandler.getMissionFile());
                player.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") + "Has completado una Raid!");
                successNotification.showSuccess(player);

                // Verificar si también ha crafteado las manzanas de oro
                checkMissionCompletion(playerName, data);
            } catch (IOException e) {
                plugin.getLogger().severe("Error al guardar progreso de Misión 3: " + e.getMessage());
            }
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!missionHandler.isMissionActive(3)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String playerName = player.getName();

        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());

        // Verificar si ya completó la misión ANTES de procesar el craft
        if (data.getBoolean("players." + playerName + ".missions.3.completed", false)) {
            return;
        }

        ItemStack recipeResult = event.getRecipe().getResult();
        if (recipeResult.getType() != Material.GOLDEN_APPLE) return;

        int craftedAmount = 0;

        if (event.isShiftClick()) {
            int maxCraftable = Integer.MAX_VALUE;
            for (ItemStack ingredient : event.getInventory().getMatrix()) {
                if (ingredient != null && ingredient.getType() != Material.AIR) {
                    maxCraftable = Math.min(maxCraftable, ingredient.getAmount());
                }
            }
            if (maxCraftable == Integer.MAX_VALUE) {
                maxCraftable = 0;
            }
            craftedAmount = maxCraftable * recipeResult.getAmount();
        } else {
            craftedAmount = recipeResult.getAmount();
        }

        if (craftedAmount <= 0) return;

        // Obtener el valor ACTUAL antes de modificarlo
        int currentCrafted = data.getInt("players." + playerName + ".missions.3.golden_apples_crafted", 0);

        // Solo proceder si realmente estamos agregando algo nuevo
        if (craftedAmount > 0) {
            int newTotal = currentCrafted + craftedAmount;
            data.set("players." + playerName + ".missions.3.golden_apples_crafted", newTotal);

            try {
                data.save(missionHandler.getMissionFile());

                if (newTotal >= 20) {
                    successNotification.showSuccess(player);
                } else {
                    player.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") + "Manzanas de oro crafteadas: " +
                            ChatColor.of("#FFB6C1") + newTotal + ChatColor.of("#87CEEB") + "/" + ChatColor.of("#98FB98") + "20");
                }

                // Verificar si completó la misión (solo si craft >= 20)
                checkMissionCompletion(playerName, data);
            } catch (IOException e) {
                plugin.getLogger().severe("Error al guardar progreso de Misión 3: " + e.getMessage());
            }
        }
    }

    private void checkMissionCompletion(String playerName, FileConfiguration data) {
        // Verificar si ya completó la misión para evitar duplicados
        if (data.getBoolean("players." + playerName + ".missions.3.completed", false)) {
            return;
        }

        boolean raidCompleted = data.getBoolean("players." + playerName + ".missions.3.raid_completed", false);
        int goldenApplesCrafted = data.getInt("players." + playerName + ".missions.3.golden_apples_crafted", 0);

        if (raidCompleted && goldenApplesCrafted >= 20) {
            // Marcar como completada ANTES de llamar a completeMission
            data.set("players." + playerName + ".missions.3.completed", true);
            try {
                data.save(missionHandler.getMissionFile());
                missionHandler.completeMission(playerName, 3);
            } catch (IOException e) {
                plugin.getLogger().severe("Error al marcar misión como completada: " + e.getMessage());
            }
        }
    }
}