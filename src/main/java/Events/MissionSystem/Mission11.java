package Events.MissionSystem;

import TitleListener.SuccessNotification;
import items.EconomyItems;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Mission11 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final Map<UUID, Long> playerStartTimes = new HashMap<>();
    private final Map<UUID, BukkitRunnable> trackingTasks = new HashMap<>();
    private static final long REQUIRED_TIME = 23500; // Tiempo requerido en ticks
    private final SuccessNotification successNotification;

    public Mission11(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
    }

    @Override
    public String getName() {
        return "Habitante de Hongos";
    }

    @Override
    public String getDescription() {
        return "Permanece 1 día y 1 noche completos en una Mushroom Island";
    }

    @Override
    public int getMissionNumber() {
        return 11;
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

        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {
        // No es necesario inicializar con JSON
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!missionHandler.isMissionActive(player, 11)) return;

        UUID playerId = player.getUniqueId();
        MissionData data = missionHandler.getData(player, 11);

        if (data.isCompleted()) {
            return;
        }

        Biome currentBiome = player.getLocation().getBlock().getBiome();
        boolean isInMushroomIsland = currentBiome == Biome.MUSHROOM_FIELDS;

        if (isInMushroomIsland) {
            // Si no estaba siendo trackeado, empezar
            if (!playerStartTimes.containsKey(playerId)) {
                startTracking(player);
            }
        } else {
            // Si estaba siendo trackeado, parar
            if (playerStartTimes.containsKey(playerId)) {
                stopTracking(player);
            }
        }
    }

    private void startTracking(Player player) {
        UUID playerId = player.getUniqueId();
        playerStartTimes.put(playerId, System.currentTimeMillis());

        player.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#F0E68C") + "¡Comenzando a contar tiempo en Mushroom Island!");

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    stopTracking(player);
                    return;
                }

                Biome currentBiome = player.getLocation().getBlock().getBiome();
                if (currentBiome != Biome.MUSHROOM_FIELDS) {
                    stopTracking(player);
                    return;
                }

                MissionData data = missionHandler.getData(player, 11);

                // Asegúrate de que getProgressLong exista en tu MissionData
                long currentTime = data.getProgressLong("time_in_mushroom");
                currentTime += 20; // 20 ticks = 1 segundo

                data.setProgressValue("time_in_mushroom", currentTime);
                missionHandler.saveData(player, 11, data);

                if (currentTime >= REQUIRED_TIME) {
                    successNotification.showSuccess(player);
                    missionHandler.completeMission(player.getName(), 11);
                    stopTracking(player);
                }
            }
        };

        task.runTaskTimer(plugin, 0L, 20L); // Cada segundo
        trackingTasks.put(playerId, task);
    }

    private void stopTracking(Player player) {
        UUID playerId = player.getUniqueId();

        playerStartTimes.remove(playerId);

        BukkitRunnable task = trackingTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }

        player.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#FFA07A") + "Has salido de la Mushroom Island. Tiempo pausado.");
    }
}