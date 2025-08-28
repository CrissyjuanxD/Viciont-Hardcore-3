package Events.MissionSystem;

import items.EconomyItems;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Mission7 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;

    public Mission7(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        data.set("players." + playerName + ".missions.7.corrupted_skeletons_killed", 0);
        data.set("players." + playerName + ".missions.7.corrupted_creepers_killed", 0);

        try {
            data.save(missionHandler.getMissionFile());
        } catch (IOException e) {
            plugin.getLogger().severe("Error al inicializar datos de Misión 7: " + e.getMessage());
        }
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!missionHandler.isMissionActive(7)) return;

        Entity entity = event.getEntity();
        Player killer = ((LivingEntity) entity).getKiller();

        if (killer == null) return;

        String playerName = killer.getName();

        // Verificar si ya completó la misión
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        if (data.getBoolean("players." + playerName + ".missions.7.completed", false)) {
            return;
        }

        boolean isCorruptedSkeleton = entity instanceof Skeleton &&
                entity.getPersistentDataContainer().has(
                        new org.bukkit.NamespacedKey(plugin, "corrupted_skeleton"),
                        PersistentDataType.BYTE);

        boolean isCorruptedCreeper = entity instanceof Creeper &&
                entity.getPersistentDataContainer().has(
                        new org.bukkit.NamespacedKey(plugin, "corrupted_creeper"),
                        PersistentDataType.BYTE);

        if (isCorruptedSkeleton) {
            int skeletonsKilled = data.getInt("players." + playerName + ".missions.7.corrupted_skeletons_killed", 0);
            skeletonsKilled++;
            data.set("players." + playerName + ".missions.7.corrupted_skeletons_killed", skeletonsKilled);

            killer.sendMessage(ChatColor.of("#DDA0DD") + "Corrupted Skeletons eliminados: " +
                    ChatColor.of("#98FB98") + skeletonsKilled + ChatColor.of("#D3D3D3") + "/30");
        } else if (isCorruptedCreeper) {
            int creepersKilled = data.getInt("players." + playerName + ".missions.7.corrupted_creepers_killed", 0);
            creepersKilled++;
            data.set("players." + playerName + ".missions.7.corrupted_creepers_killed", creepersKilled);

            killer.sendMessage(ChatColor.of("#DDA0DD") + "Corrupted Creepers eliminados: " +
                    ChatColor.of("#98FB98") + creepersKilled + ChatColor.of("#D3D3D3") + "/30");
        } else {
            return;
        }

        try {
            data.save(missionHandler.getMissionFile());

            // Verificar si completó ambos objetivos
            int skeletonsKilled = data.getInt("players." + playerName + ".missions.7.corrupted_skeletons_killed", 0);
            int creepersKilled = data.getInt("players." + playerName + ".missions.7.corrupted_creepers_killed", 0);

            if (skeletonsKilled >= 30 && creepersKilled >= 30) {
                missionHandler.completeMission(playerName, 7);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar progreso de Misión 7: " + e.getMessage());
        }
    }
}