package Events.MissionSystem;

import items.EconomyItems;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatColor;

import java.io.IOException;
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

    public Mission11(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        data.set("players." + playerName + ".missions.11.time_in_mushroom", 0L);
        data.set("players." + playerName + ".missions.11.start_time", 0L);

        try {
            data.save(missionHandler.getMissionFile());
        } catch (IOException e) {
            plugin.getLogger().severe("Error al inicializar datos de Misión 11: " + e.getMessage());
        }
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!missionHandler.isMissionActive(11)) return;

        Player player = event.getPlayer();
        String playerName = player.getName();
        UUID playerId = player.getUniqueId();

        // Verificar si ya completó la misión
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        if (data.getBoolean("players." + playerName + ".missions.11.completed", false)) {
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
        String playerName = player.getName();

        playerStartTimes.put(playerId, System.currentTimeMillis());

        player.sendMessage(ChatColor.of("#F0E68C") + "¡Comenzando a contar tiempo en Mushroom Island!");

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

                // Actualizar tiempo acumulado
                FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
                long currentTime = data.getLong("players." + playerName + ".missions.11.time_in_mushroom", 0);
                currentTime += 20; // 20 ticks = 1 segundo
                data.set("players." + playerName + ".missions.11.time_in_mushroom", currentTime);

                try {
                    data.save(missionHandler.getMissionFile());

                    // Verificar si completó el tiempo requerido
                    if (currentTime >= REQUIRED_TIME) {
                        missionHandler.completeMission(playerName, 11);
                        stopTracking(player);
                    }
                } catch (IOException e) {
                    plugin.getLogger().severe("Error al guardar progreso de Misión 11: " + e.getMessage());
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

        player.sendMessage(ChatColor.of("#FFA07A") + "Has salido de la Mushroom Island. Tiempo pausado.");
    }
}