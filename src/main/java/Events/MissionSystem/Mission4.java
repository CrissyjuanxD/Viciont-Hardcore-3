package Events.MissionSystem;

import Dificultades.CustomMobs.QueenBeeHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Mission4 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final Map<UUID, Boolean> playerDamagedQueenBee = new HashMap<>();
    private final SuccessNotification successNotification;

    public Mission4(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
    }

    @Override
    public String getName() {
        return "Cazador de Reinas";
    }

    @Override
    public String getDescription() {
        return "Mata a una Abeja Reina";
    }

    @Override
    public int getMissionNumber() {
        return 4;
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
        data.set("players." + playerName + ".missions.4.queen_bee_killed", false);

        try {
            data.save(missionHandler.getMissionFile());
        } catch (IOException e) {
            plugin.getLogger().severe("Error al inicializar datos de Misión 4: " + e.getMessage());
        }
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!missionHandler.isMissionActive(4)) return;

        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Bee)) return;

        Player player = (Player) event.getDamager();
        Bee bee = (Bee) event.getEntity();

        // Verificar si es una Abeja Reina usando el método del QueenBeeHandler
        if (bee.getCustomName() != null && bee.getCustomName().contains("Abeja Reina")) {
            // Marcar que este jugador dañó a la Abeja Reina
            playerDamagedQueenBee.put(player.getUniqueId(), true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!missionHandler.isMissionActive(4)) return;

        if (!(event.getEntity() instanceof Bee)) return;

        Bee bee = (Bee) event.getEntity();

        // Verificar si es una Abeja Reina
        if (bee.getCustomName() != null && bee.getCustomName().contains("Abeja Reina")) {
            // Buscar jugadores en un radio de 100 bloques que hayan dañado a la abeja
            for (Player player : bee.getWorld().getPlayers()) {
                if (player.getLocation().distance(bee.getLocation()) <= 100) {
                    // Verificar si este jugador dañó a la abeja
                    if (playerDamagedQueenBee.getOrDefault(player.getUniqueId(), false)) {
                        String playerName = player.getName();

                        // Verificar si ya completó la misión
                        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
                        if (data.getBoolean("players." + playerName + ".missions.4.completed", false)) {
                            continue;
                        }

                        // Completar la misión
                        data.set("players." + playerName + ".missions.4.queen_bee_killed", true);

                        try {
                            data.save(missionHandler.getMissionFile());
                            player.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") + "Has matado a la Abeja Reina!");
                            successNotification.showSuccess(player);
                            missionHandler.completeMission(playerName, 4);
                        } catch (IOException e) {
                            plugin.getLogger().severe("Error al guardar progreso de Misión 4: " + e.getMessage());
                        }

                        // Limpiar el registro de daño
                        playerDamagedQueenBee.remove(player.getUniqueId());
                    }
                }
            }

            // Limpiar todos los registros de daño para esta abeja
            playerDamagedQueenBee.clear();
        }
    }
}