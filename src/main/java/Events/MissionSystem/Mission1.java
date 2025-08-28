package Events.MissionSystem;

import TitleListener.SuccessNotification;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import items.EconomyItems;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Mission1 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;

    public Mission1(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() {
        return "Armadura Básica";
    }

    @Override
    public String getDescription() {
        return "Equípate cada pieza de armadura de diamante";
    }

    @Override
    public int getMissionNumber() {
        return 1;
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

        // 1 Totem
        rewards.add(new ItemStack(Material.TOTEM_OF_UNDYING, 1));

        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());

        // Inicializar progreso de armadura
        String[] armorPieces = {"helmet", "chestplate", "leggings", "boots"};
        for (String piece : armorPieces) {
            data.set("players." + playerName + ".missions.1.armor." + piece, false);
        }

        try {
            data.save(missionHandler.getMissionFile());
        } catch (IOException e) {
            plugin.getLogger().severe("Error al inicializar datos de Misión 1: " + e.getMessage());
        }
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!missionHandler.isMissionActive(1)) return;

        Player player = (Player) event.getWhoClicked();

        // Verificar si ya completó la misión
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        if (data.getBoolean("players." + player.getName() + ".missions.1.completed", false)) {
            return;
        }

        // Verificar después de un tick para asegurar que el inventario se actualice
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkArmorEquipped(player);
        }, 1L);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (!missionHandler.isMissionActive(1)) return;

        Player player = event.getPlayer();

        // Verificar si ya completó la misión
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        if (data.getBoolean("players." + player.getName() + ".missions.1.completed", false)) {
            return;
        }

        // Verificar después de un tick
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkArmorEquipped(player);
        }, 1L);
    }

    private void checkArmorEquipped(Player player) {
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        String playerName = player.getName();

        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        boolean hasHelmet = helmet != null && helmet.getType() == Material.DIAMOND_HELMET;
        boolean hasChestplate = chestplate != null && chestplate.getType() == Material.DIAMOND_CHESTPLATE;
        boolean hasLeggings = leggings != null && leggings.getType() == Material.DIAMOND_LEGGINGS;
        boolean hasBoots = boots != null && boots.getType() == Material.DIAMOND_BOOTS;

        // Actualizar progreso
        boolean updated = false;

        if (hasHelmet && !data.getBoolean("players." + playerName + ".missions.1.armor.helmet", false)) {
            data.set("players." + playerName + ".missions.1.armor.helmet", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (hasChestplate && !data.getBoolean("players." + playerName + ".missions.1.armor.chestplate", false)) {
            data.set("players." + playerName + ".missions.1.armor.chestplate", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (hasLeggings && !data.getBoolean("players." + playerName + ".missions.1.armor.leggings", false)) {
            data.set("players." + playerName + ".missions.1.armor.leggings", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (hasBoots && !data.getBoolean("players." + playerName + ".missions.1.armor.boots", false)) {
            data.set("players." + playerName + ".missions.1.armor.boots", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (updated) {
            try {
                data.save(missionHandler.getMissionFile());
            } catch (IOException e) {
                plugin.getLogger().severe("Error al guardar progreso de Misión 1: " + e.getMessage());
                return;
            }

            // Verificar si completó toda la armadura
            if (hasHelmet && hasChestplate && hasLeggings && hasBoots) {
                missionHandler.completeMission(playerName, 1);
            } else {
                // Mostrar progreso
                int completed = 0;
                if (data.getBoolean("players." + playerName + ".missions.1.armor.helmet", false)) completed++;
                if (data.getBoolean("players." + playerName + ".missions.1.armor.chestplate", false)) completed++;
                if (data.getBoolean("players." + playerName + ".missions.1.armor.leggings", false)) completed++;
                if (data.getBoolean("players." + playerName + ".missions.1.armor.boots", false)) completed++;

                player.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") + "Progreso de armadura: " + ChatColor.of("#FFB6C1") + completed + ChatColor.of("#87CEEB") + "/" + ChatColor.of("#98FB98") + "4");
            }
        }
    }
}