package Events.MissionSystem;

import TitleListener.SuccessNotification;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Mission2 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;

    public Mission2(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
    }

    @Override
    public String getName() {
        return "Protección Avanzada";
    }

    @Override
    public String getDescription() {
        return "Equípate armadura de diamante con Protección IV en cada pieza";
    }

    @Override
    public int getMissionNumber() {
        return 2;
    }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        // 10 Vithiums
        ItemStack vithiums = EconomyItems.createVithiumCoin();
        vithiums.setAmount(10);
        rewards.add(vithiums);

        // 1 Notch Apple
        rewards.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));

        // 5 Manzanas doradas
        rewards.add(new ItemStack(Material.GOLDEN_APPLE, 5));

        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());

        // Inicializar progreso de protección
        String[] armorPieces = {"helmet", "chestplate", "leggings", "boots"};
        for (String piece : armorPieces) {
            data.set("players." + playerName + ".missions.2.protection." + piece, false);
        }

        try {
            data.save(missionHandler.getMissionFile());
        } catch (IOException e) {
            plugin.getLogger().severe("Error al inicializar datos de Misión 2: " + e.getMessage());
        }
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!missionHandler.isMissionActive(2)) return;

        Player player = (Player) event.getWhoClicked();

        // Verificar si ya completó la misión
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        if (data.getBoolean("players." + player.getName() + ".missions.2.completed", false)) {
            return;
        }

        // Verificar después de un tick
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkProtectionArmor(player);
        }, 1L);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (!missionHandler.isMissionActive(2)) return;

        Player player = event.getPlayer();

        // Verificar si ya completó la misión
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        if (data.getBoolean("players." + player.getName() + ".missions.2.completed", false)) {
            return;
        }

        // Verificar después de un tick
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkProtectionArmor(player);
        }, 1L);
    }

    private void checkProtectionArmor(Player player) {
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        String playerName = player.getName();

        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        boolean hasHelmet = hasProtectionIV(helmet, Material.DIAMOND_HELMET);
        boolean hasChestplate = hasProtectionIV(chestplate, Material.DIAMOND_CHESTPLATE);
        boolean hasLeggings = hasProtectionIV(leggings, Material.DIAMOND_LEGGINGS);
        boolean hasBoots = hasProtectionIV(boots, Material.DIAMOND_BOOTS);

        // Actualizar progreso
        boolean updated = false;

        if (hasHelmet && !data.getBoolean("players." + playerName + ".missions.2.protection.helmet", false)) {
            data.set("players." + playerName + ".missions.2.protection.helmet", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (hasChestplate && !data.getBoolean("players." + playerName + ".missions.2.protection.chestplate", false)) {
            data.set("players." + playerName + ".missions.2.protection.chestplate", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (hasLeggings && !data.getBoolean("players." + playerName + ".missions.2.protection.leggings", false)) {
            data.set("players." + playerName + ".missions.2.protection.leggings", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (hasBoots && !data.getBoolean("players." + playerName + ".missions.2.protection.boots", false)) {
            data.set("players." + playerName + ".missions.2.protection.boots", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (updated) {
            try {
                data.save(missionHandler.getMissionFile());
            } catch (IOException e) {
                plugin.getLogger().severe("Error al guardar progreso de Misión 2: " + e.getMessage());
                return;
            }

            // Verificar si completó toda la armadura
            if (hasHelmet && hasChestplate && hasLeggings && hasBoots) {
                missionHandler.completeMission(playerName, 2);
            } else {
                // Mostrar progreso
                int completed = 0;
                if (data.getBoolean("players." + playerName + ".missions.2.protection.helmet", false)) completed++;
                if (data.getBoolean("players." + playerName + ".missions.2.protection.chestplate", false)) completed++;
                if (data.getBoolean("players." + playerName + ".missions.2.protection.leggings", false)) completed++;
                if (data.getBoolean("players." + playerName + ".missions.2.protection.boots", false)) completed++;

                player.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") + "Progreso de protección: " + ChatColor.of("#FFB6C1") + completed + ChatColor.of("#87CEEB") + "/" + ChatColor.of("#98FB98") + "4");
            }
        }
    }

    private boolean hasProtectionIV(ItemStack armor, Material expectedType) {
        if (armor == null || armor.getType() != expectedType) {
            return false;
        }

        return armor.getEnchantmentLevel(Enchantment.PROTECTION) >= 4;
    }
}