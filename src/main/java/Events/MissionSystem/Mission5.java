package Events.MissionSystem;

import TitleListener.SuccessNotification;
import items.DoubleLifeTotem;
import items.EconomyItems;
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
import net.md_5.bungee.api.ChatColor;
import vct.hardcore3.ViciontHardcore3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Mission5 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final DoubleLifeTotem doubleLifeTotem;
    private final SuccessNotification successNotification;


    public Mission5(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.doubleLifeTotem = ((ViciontHardcore3) plugin).getDoubleLifeTotemHandler();
        this.successNotification = new SuccessNotification(plugin);
    }

    @Override
    public String getName() {
        return "Armadura Suprema";
    }

    @Override
    public String getDescription() {
        return "Equípate cada pieza de armadura de netherite con Protección IV";
    }

    @Override
    public int getMissionNumber() {
        return 5;
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

        // 1 Double Totem
        rewards.add(doubleLifeTotem.createDoubleLifeTotem());

        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());

        // Inicializar progreso de armadura de netherite
        String[] armorPieces = {"helmet", "chestplate", "leggings", "boots"};
        for (String piece : armorPieces) {
            data.set("players." + playerName + ".missions.5.netherite_armor." + piece, false);
            data.set("players." + playerName + ".missions.5.protection." + piece, false);
        }

        try {
            data.save(missionHandler.getMissionFile());
        } catch (IOException e) {
            plugin.getLogger().severe("Error al inicializar datos de Misión 5: " + e.getMessage());
        }
    }

    @Override
    public void checkCompletion(String playerName) {
        // Se verifica durante los eventos
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!missionHandler.isMissionActive(5)) return;

        Player player = (Player) event.getWhoClicked();

        // Verificar si ya completó la misión
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        if (data.getBoolean("players." + player.getName() + ".missions.5.completed", false)) {
            return;
        }

        // Verificar después de un tick
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkNetheriteArmorWithProtection(player);
        }, 1L);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (!missionHandler.isMissionActive(5)) return;

        Player player = event.getPlayer();

        // Verificar si ya completó la misión
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        if (data.getBoolean("players." + player.getName() + ".missions.5.completed", false)) {
            return;
        }

        // Verificar después de un tick
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkNetheriteArmorWithProtection(player);
        }, 1L);
    }

    private void checkNetheriteArmorWithProtection(Player player) {
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        String playerName = player.getName();

        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        boolean hasHelmet = hasNetheriteWithProtectionIV(helmet, Material.NETHERITE_HELMET);
        boolean hasChestplate = hasNetheriteWithProtectionIV(chestplate, Material.NETHERITE_CHESTPLATE);
        boolean hasLeggings = hasNetheriteWithProtectionIV(leggings, Material.NETHERITE_LEGGINGS);
        boolean hasBoots = hasNetheriteWithProtectionIV(boots, Material.NETHERITE_BOOTS);

        // Actualizar progreso
        boolean updated = false;

        if (hasHelmet && !data.getBoolean("players." + playerName + ".missions.5.netherite_armor.helmet", false)) {
            data.set("players." + playerName + ".missions.5.netherite_armor.helmet", true);
            data.set("players." + playerName + ".missions.5.protection.helmet", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (hasChestplate && !data.getBoolean("players." + playerName + ".missions.5.netherite_armor.chestplate", false)) {
            data.set("players." + playerName + ".missions.5.netherite_armor.chestplate", true);
            data.set("players." + playerName + ".missions.5.protection.chestplate", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (hasLeggings && !data.getBoolean("players." + playerName + ".missions.5.netherite_armor.leggings", false)) {
            data.set("players." + playerName + ".missions.5.netherite_armor.leggings", true);
            data.set("players." + playerName + ".missions.5.protection.leggings", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (hasBoots && !data.getBoolean("players." + playerName + ".missions.5.netherite_armor.boots", false)) {
            data.set("players." + playerName + ".missions.5.netherite_armor.boots", true);
            data.set("players." + playerName + ".missions.5.protection.boots", true);
            successNotification.showSuccess(player);
            updated = true;
        }

        if (updated) {
            try {
                data.save(missionHandler.getMissionFile());
            } catch (IOException e) {
                plugin.getLogger().severe("Error al guardar progreso de Misión 5: " + e.getMessage());
                return;
            }

            // Verificar si completó toda la armadura
            if (hasHelmet && hasChestplate && hasLeggings && hasBoots) {
                missionHandler.completeMission(playerName, 5);
            } else {
                // Mostrar progreso
                int completed = 0;
                if (data.getBoolean("players." + playerName + ".missions.5.netherite_armor.helmet", false)) completed++;
                if (data.getBoolean("players." + playerName + ".missions.5.netherite_armor.chestplate", false)) completed++;
                if (data.getBoolean("players." + playerName + ".missions.5.netherite_armor.leggings", false)) completed++;
                if (data.getBoolean("players." + playerName + ".missions.5.netherite_armor.boots", false)) completed++;

                player.sendMessage(ChatColor.GOLD + "۞ " + ChatColor.of("#87CEEB") + "Progreso de armadura de netherite: " + ChatColor.of("#FFB6C1") + completed + ChatColor.of("#87CEEB") + "/" + ChatColor.of("#98FB98") + "4");
            }
        }
    }

    private boolean hasNetheriteWithProtectionIV(ItemStack armor, Material expectedType) {
        if (armor == null || armor.getType() != expectedType) {
            return false;
        }

        return armor.getEnchantmentLevel(Enchantment.PROTECTION) >= 4;
    }
}