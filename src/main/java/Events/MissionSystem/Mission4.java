package Events.MissionSystem;

import TitleListener.SuccessNotification;
import items.EconomyItems;
import org.bukkit.Material;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;

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
        // No es necesario inicializar con JSON
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!missionHandler.isMissionActive(player, 4)) return;
        if (!(event.getEntity() instanceof Bee bee)) return;

        // Verificar si es una Abeja Reina
        if (bee.getCustomName() != null && bee.getCustomName().contains("Corrupted Queen Bee")) {
            // Marcar que este jugador dañó a la Abeja Reina
            playerDamagedQueenBee.put(player.getUniqueId(), true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Bee bee)) return;

        // Verificar si es una Abeja Reina
        if (bee.getCustomName() != null && bee.getCustomName().contains("Corrupted Queen Bee")) {
            // Buscar jugadores en un radio de 100 bloques que hayan dañado a la abeja
            for (Player player : bee.getWorld().getPlayers()) {
                if (!missionHandler.isMissionActive(player, 4)) continue;

                if (player.getLocation().distance(bee.getLocation()) <= 100) {
                    // Verificar si este jugador dañó a la abeja
                    if (playerDamagedQueenBee.getOrDefault(player.getUniqueId(), false)) {
                        MissionData data = missionHandler.getData(player, 4);

                        // Verificar si ya completó la misión
                        if (data.isCompleted()) {
                            continue;
                        }

                        // Completar la misión
                        data.setProgressValue("queen_bee_killed", true);
                        missionHandler.saveData(player, 4, data);

                        player.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") + "Has matado a la Abeja Reina!");
                        successNotification.showSuccess(player);
                        missionHandler.completeMission(player.getName(), 4);

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