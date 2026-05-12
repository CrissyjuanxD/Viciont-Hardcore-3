package Events.MissionSystem;

import TitleListener.SuccessNotification;
import items.EconomyItems;
import org.bukkit.Material;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class Mission7 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;

    public Mission7(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
    }

    @Override
    public String getName() {
        return "Cazador de Esqueletos";
    }

    @Override
    public String getDescription() {
        return "Mata 30 Corrupted Skeletons y 30 Corrupted Creepers";
    }

    @Override
    public int getMissionNumber() {
        return 7;
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
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        Player killer = ((LivingEntity) entity).getKiller();

        if (killer == null) return;
        if (!missionHandler.isMissionActive(killer, 7)) return;

        MissionData data = missionHandler.getData(killer, 7);
        if (data.isCompleted()) return;

        boolean isCorruptedSkeleton = entity instanceof Skeleton &&
                entity.getPersistentDataContainer().has(
                        new org.bukkit.NamespacedKey(plugin, "corrupted_skeleton"),
                        PersistentDataType.BYTE);

        boolean isCorruptedCreeper = entity instanceof Creeper &&
                entity.getPersistentDataContainer().has(
                        new org.bukkit.NamespacedKey(plugin, "corrupted_creeper"),
                        PersistentDataType.BYTE);

        if (isCorruptedSkeleton) {
            int skeletonsKilled = data.getProgressInt("corrupted_skeletons_killed");
            if (skeletonsKilled < 30) {
                skeletonsKilled++;
                data.setProgressValue("corrupted_skeletons_killed", skeletonsKilled);

                killer.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") + "Corrupted Skeletons eliminados: " +
                        ChatColor.of("#FFB6C1") + skeletonsKilled + ChatColor.of("#87CEEB") + "/" + ChatColor.of("#98FB98") + "30");
            }
        } else if (isCorruptedCreeper) {
            int creepersKilled = data.getProgressInt("corrupted_creepers_killed");
            if (creepersKilled < 30) {
                creepersKilled++;
                data.setProgressValue("corrupted_creepers_killed", creepersKilled);

                killer.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") + "Corrupted Creepers eliminados: " +
                        ChatColor.of("#FFB6C1") + creepersKilled + ChatColor.of("#87CEEB") + "/" + ChatColor.of("#98FB98") + "30");
            }
        } else {
            return;
        }

        missionHandler.saveData(killer, 7, data);

        // Verificar si completó ambos objetivos
        if (data.getProgressInt("corrupted_skeletons_killed") >= 30 &&
                data.getProgressInt("corrupted_creepers_killed") >= 30) {

            successNotification.showSuccess(killer);
            missionHandler.completeMission(killer.getName(), 7);
        }
    }
}