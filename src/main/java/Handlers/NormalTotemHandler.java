package Handlers;

import CorrupcionAnsiosa.CorrupcionAnsiosaManager;
import CorrupcionAnsiosa.PlayerCorruptionData;
import CorrupcionAnsiosa.CorrupcionEffectsHandler;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Random;

public class NormalTotemHandler implements Listener {

    private final Plugin plugin;
    private final DayHandler dayHandler;
    private final CorrupcionAnsiosaManager corruptionManager;
    private final CorrupcionEffectsHandler corruptionEffectsHandler;
    private final Random random = new Random();

    public NormalTotemHandler(Plugin plugin, DayHandler handler, CorrupcionAnsiosaManager corruptionManager, CorrupcionEffectsHandler corruptionEffectsHandler) {
        this.plugin = plugin;
        this.dayHandler = handler;
        this.corruptionManager = corruptionManager;
        this.corruptionEffectsHandler = corruptionEffectsHandler;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        int currentDay = dayHandler.getCurrentDay();

        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        ItemStack offHandItem = player.getInventory().getItemInOffHand();

        boolean isMainHandTotem = mainHandItem.getType() == Material.TOTEM_OF_UNDYING;
        boolean isOffHandTotem = offHandItem.getType() == Material.TOTEM_OF_UNDYING;

        if (isMainHandTotem || isOffHandTotem) {
            ItemStack totem = isMainHandTotem ? mainHandItem : offHandItem;
            ItemMeta meta = totem.getItemMeta();

            // Obtener datos de corrupción
            PlayerCorruptionData corruptionData = corruptionManager.getPlayerData(player);
            double currentCorruption = corruptionData.getCorruption();
            double failChance = corruptionData.getFailChance(currentDay);

            // Verificar si el tótem falla por corrupción
            if (random.nextDouble() < failChance) {
                event.setCancelled(true);
                broadcastTotemFailByCorruption(player, currentCorruption, failChance);

                // Reducir corrupción a todos los jugadores
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    corruptionManager.removeCorruption(onlinePlayer, 10.0);
                    onlinePlayer.sendMessage(ChatColor.of("#B228E7") + "¡La corrupción ansiosa ha disminuido en 10% para todos!");
                }
                return;
            }

            if (meta != null && meta.hasCustomModelData()) {
                // Tótems especiales (con CustomModelData)
                int customModelData = meta.getCustomModelData();
                double reduction = getCorruptionReduction(customModelData, currentDay);

                switch (customModelData) {
                    case 1:
                        broadcastDoubleTotemlastuse(player, currentCorruption, reduction);
                        break;
                    case 2:
                        broadcastDoubleTotem(player, currentCorruption, reduction);
                        break;
                    case 3:
                        broadcastLifeTotem(player, currentCorruption, reduction);
                        break;
                    case 4:
                        broadcastSpiderTotem(player, currentCorruption, reduction);
                        break;
                    case 5:
                        broadcastInfernalTotem(player, currentCorruption, reduction);
                        break;
                    case 6:
                        broadcastIceTotem(player, currentCorruption, reduction);
                        break;
                    case 7:
                        broadcastFlyTotem(player, currentCorruption, reduction);
                        break;
                    case 8:
                        broadcastInventoryTotem(player, currentCorruption, reduction);
                        break;
                    case 9:
                        broadcastDefinitiveTotem(player, currentCorruption);
                        break;
                    default:
                        broadcastNormalTotemMessage(player, currentCorruption, failChance, reduction);
                        break;
                }

                // Reducir corrupción para tótems especiales
                if (reduction > 0) {
                    corruptionManager.removeCorruption(player, reduction);
                }

            } else {
                // Tótem normal - reducir corrupción
                double reduction = getCorruptionReduction(0, currentDay); // 0 para tótem normal
                if (reduction > 0) {
                    corruptionManager.removeCorruption(player, reduction);
                }

                broadcastNormalTotemMessage(player, currentCorruption, failChance, reduction);
            }

            // Aplicar efectos de corrupción después del uso exitoso del tótem
            corruptionEffectsHandler.applyCorruptionEffects(player, currentDay);
        }
    }

    private double getCorruptionReduction(int customModelData, int currentDay) {
        // Tótems que no reducen corrupción (Doble y Definitivo)
        if (customModelData == 1 || customModelData == 2 || customModelData == 9) {
            return 0;
        }

        if (currentDay >= 26) {
            switch (customModelData) {
                case 0: return 100.0; // Normal
                case 6: case 7: return 14.0; // Fly e Ice
                case 3: return 12.0; // Life
                case 4: case 5: case 8: return 8.0; // Spider, Infernal, Inventory
                default: return 0;
            }
        } else if (currentDay >= 22) {
            switch (customModelData) {
                case 0: return 50.0;
                case 6: case 7: return 13.0;
                case 3: return 11.0;
                case 4: case 5: case 8: return 7.0;
                default: return 0;
            }
        } else if (currentDay >= 18) {
            switch (customModelData) {
                case 0: return 32.0;
                case 6: case 7: return 12.0;
                case 3: return 10.0;
                case 4: case 5: case 8: return 6.0;
                default: return 0;
            }
        } else if (currentDay >= 12) {
            switch (customModelData) {
                case 0: return 16.0;
                case 6: case 7: return 9.0;
                case 3: return 8.0;
                case 4: case 5: return 4.0;
                default: return 0;
            }
        } else if (currentDay >= 8) {
            switch (customModelData) {
                case 0: return 8.0;
                case 6: case 7: return 3.0;
                default: return 0;
            }
        } else if (currentDay >= 4) {
            switch (customModelData) {
                case 0: return 4.0;
                case 6: case 7: return 1.0;
                default: return 0;
            }
        } else {
            // Día 1-3
            return customModelData == 0 ? 2.0 : 0;
        }
    }

    private void broadcastNormalTotemMessage(Player player, double currentCorruption, double failChance, double reduction) {
        String baseMessage = ChatColor.translateAlternateColorCodes('&',
                "\n"
                        + "\uDBE8\uDCF6"
                        + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                        + ChatColor.RESET + ChatColor.of("#B684E4") + " ha consumido un tótem.");

        // Mostrar corrupción ansiosa
        baseMessage += "\n" + ChatColor.of("#B228E7") + "Corrupción Ansiosa: " +
                ChatColor.of("#F7AD62") + String.format("%.1f", currentCorruption) + "%";

        // Mostrar reducción de corrupción
        if (reduction > 0) {
            baseMessage += "\n" + ChatColor.of("#33cc33") + "✓ Reducción: -" +
                    String.format("%.1f", reduction) + "%";
        }

        // Mostrar probabilidad de fallo si es mayor a 0
        if (failChance > 0) {
            baseMessage += "\n" + ChatColor.of("#F52F6A") + ChatColor.BOLD + " ⚠" +
                    ChatColor.of("#B228E7") + " Prob. Fallar " +
                    ChatColor.GRAY + "(" +
                    ChatColor.of("#F7AD62") + String.format("%.0f", failChance * 100) +
                    ChatColor.GRAY + "/" +
                    ChatColor.of("#F52F6A") + "100" +
                    ChatColor.GRAY + ")";
        }

        baseMessage += "\n";

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(baseMessage);
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.playSound(onlinePlayer, "item.trident.return", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "custom.noti", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "entity.allay.item_thrown", SoundCategory.VOICE, 2f, 0.5f);
            }
        }
    }

    private void broadcastDoubleTotem(Player player, double currentCorruption, double reduction) {
        String message = ChatColor.translateAlternateColorCodes('&',
                "\n"
                        + "\uDBE8\uDCF6"
                        + ChatColor.of("#007EB2") + ChatColor.BOLD + player.getName()
                        + ChatColor.RESET + ChatColor.of("#8EBFEC") + " ha " + ChatColor.BOLD + "consumido "
                        + ChatColor.RESET + ChatColor.of("#8EBFEC") + "un tótem de doble vida."
                        + "\n" + ChatColor.of("#B228E7") + "Corrupción Ansiosa: " +
                        ChatColor.of("#F7AD62") + String.format("%.1f", currentCorruption) + "%"
                        + "\n");
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.playSound(onlinePlayer, "item.trident.return", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "custom.noti", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "entity.allay.item_thrown", SoundCategory.VOICE, 2f, 0.5f);
            }
        }
    }

    private void broadcastDoubleTotemlastuse(Player player, double currentCorruption, double reduction) {
        String message = ChatColor.translateAlternateColorCodes('&',
                "\n"
                        + "\uDBE8\uDCF6"
                        + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                        + ChatColor.RESET + ChatColor.of("#B684E4") + " ha gastado su " + ChatColor.BOLD + "tótem de doble vida."
                        + "\n" + ChatColor.of("#B228E7") + "Corrupción Ansiosa: " +
                        ChatColor.of("#F7AD62") + String.format("%.1f", currentCorruption) + "%"
                        + "\n");
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.playSound(onlinePlayer, "item.trident.return", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "custom.noti", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "entity.allay.item_thrown", SoundCategory.VOICE, 2f, 0.5f);
            }
        }
    }

    private void broadcastLifeTotem(Player player, double currentCorruption, double reduction) {
        String message = ChatColor.translateAlternateColorCodes('&',
                "\n"
                        + "\uDBE8\uDCF6"
                        + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                        + ChatColor.RESET + ChatColor.of("#B684E4") + " ha consumido un " + ChatColor.of("#33ccff") + ChatColor.BOLD + "Life Totem."
                        + "\n" + ChatColor.of("#B228E7") + "Corrupción Ansiosa: " +
                        ChatColor.of("#F7AD62") + String.format("%.1f", currentCorruption) + "%");

        if (reduction > 0) {
            message += "\n" + ChatColor.of("#33cc33") + "✓ Reducción: -" +
                    String.format("%.1f", reduction) + "%";
        }
        message += "\n";

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.playSound(onlinePlayer, "item.trident.return", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "custom.noti", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "entity.allay.item_thrown", SoundCategory.VOICE, 2f, 0.5f);
            }
        }
    }

    private void broadcastSpiderTotem(Player player, double currentCorruption, double reduction) {
        String message = ChatColor.translateAlternateColorCodes('&',
                "\n"
                        + "\uDBE8\uDCF6"
                        + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                        + ChatColor.RESET + ChatColor.of("#B684E4") + " ha consumido un " + ChatColor.of("#66ff99") + ChatColor.BOLD + "Spider Totem."
                        + "\n" + ChatColor.of("#B228E7") + "Corrupción Ansiosa: " +
                        ChatColor.of("#F7AD62") + String.format("%.1f", currentCorruption) + "%");

        if (reduction > 0) {
            message += "\n" + ChatColor.of("#33cc33") + "✓ Reducción: -" +
                    String.format("%.1f", reduction) + "%";
        }
        message += "\n";

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.playSound(onlinePlayer, "item.trident.return", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "custom.noti", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "entity.allay.item_thrown", SoundCategory.VOICE, 2f, 0.5f);
            }
        }
    }

    private void broadcastInfernalTotem(Player player, double currentCorruption, double reduction) {
        String message = ChatColor.translateAlternateColorCodes('&',
                "\n"
                        + "\uDBE8\uDCF6"
                        + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                        + ChatColor.RESET + ChatColor.of("#B684E4") + " ha consumido un " + ChatColor.of("#ff3300") + ChatColor.BOLD + "Infernal Totem."
                        + "\n" + ChatColor.of("#B228E7") + "Corrupción Ansiosa: " +
                        ChatColor.of("#F7AD62") + String.format("%.1f", currentCorruption) + "%");

        if (reduction > 0) {
            message += "\n" + ChatColor.of("#33cc33") + "✓ Reducción: -" +
                    String.format("%.1f", reduction) + "%";
        }
        message += "\n";

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.playSound(onlinePlayer, "item.trident.return", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "custom.noti", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "entity.allay.item_thrown", SoundCategory.VOICE, 2f, 0.5f);
            }
        }
    }

    private void broadcastIceTotem(Player player, double currentCorruption, double reduction) {
        String message = ChatColor.translateAlternateColorCodes('&',
                "\n"
                        + "\uDBE8\uDCF6"
                        + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                        + ChatColor.RESET + ChatColor.of("#B684E4") + " ha consumido un " + ChatColor.of("#00ccff") + ChatColor.BOLD + "Ice Totem."
                        + "\n" + ChatColor.of("#B228E7") + "Corrupción Ansiosa: " +
                        ChatColor.of("#F7AD62") + String.format("%.1f", currentCorruption) + "%");

        if (reduction > 0) {
            message += "\n" + ChatColor.of("#33cc33") + "✓ Reducción: -" +
                    String.format("%.1f", reduction) + "%";
        }
        message += "\n";

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.playSound(onlinePlayer, "item.trident.return", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "custom.noti", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "entity.allay.item_thrown", SoundCategory.VOICE, 2f, 0.5f);
            }
        }
    }

    private void broadcastFlyTotem(Player player, double currentCorruption, double reduction) {
        String message = ChatColor.translateAlternateColorCodes('&',
                "\n"
                        + "\uDBE8\uDCF6"
                        + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                        + ChatColor.RESET + ChatColor.of("#B684E4") + " ha consumido un " + ChatColor.of("#ffcc00") + ChatColor.BOLD + "Fly Totem."
                        + "\n" + ChatColor.of("#B228E7") + "Corrupción Ansiosa: " +
                        ChatColor.of("#F7AD62") + String.format("%.1f", currentCorruption) + "%");

        if (reduction > 0) {
            message += "\n" + ChatColor.of("#33cc33") + "✓ Reducción: -" +
                    String.format("%.1f", reduction) + "%";
        }
        message += "\n";

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.playSound(onlinePlayer, "item.trident.return", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "custom.noti", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "entity.allay.item_thrown", SoundCategory.VOICE, 2f, 0.5f);
            }
        }
    }

    private void broadcastInventoryTotem(Player player, double currentCorruption, double reduction) {
        String message = ChatColor.translateAlternateColorCodes('&',
                "\n"
                        + "\uDBE8\uDCF6"
                        + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                        + ChatColor.RESET + ChatColor.of("#B684E4") + " ha consumido un " + ChatColor.of("#9966ff") + ChatColor.BOLD + "Inventory Totem."
                        + "\n" + ChatColor.of("#B228E7") + "Corrupción Ansiosa: " +
                        ChatColor.of("#F7AD62") + String.format("%.1f", currentCorruption) + "%");

        if (reduction > 0) {
            message += "\n" + ChatColor.of("#33cc33") + "✓ Reducción: -" +
                    String.format("%.1f", reduction) + "%";
        }
        message += "\n";

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.playSound(onlinePlayer, "item.trident.return", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "custom.noti", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "entity.allay.item_thrown", SoundCategory.VOICE, 2f, 0.5f);
            }
        }
    }

    private void broadcastDefinitiveTotem(Player player, double currentCorruption) {
        String message = ChatColor.translateAlternateColorCodes('&',
                "\n"
                        + "\uDBE8\uDCF6"
                        + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                        + ChatColor.RESET + ChatColor.of("#B684E4") + " ha consumido un " + ChatColor.of("#ff66cc") + ChatColor.BOLD + "Totem Definitivo."
                        + "\n" + ChatColor.of("#B228E7") + "Corrupción Ansiosa: " +
                        ChatColor.of("#F7AD62") + String.format("%.1f", currentCorruption) + "%"
                        + "\n");

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.playSound(onlinePlayer, "item.trident.return", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "custom.noti", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer, "entity.allay.item_thrown", SoundCategory.VOICE, 2f, 0.5f);
            }
        }
    }

    private void broadcastTotemFailByCorruption(Player player, double currentCorruption, double failChance) {
        String message = ChatColor.translateAlternateColorCodes('&',
                "\n"
                        + ChatColor.RED + "\uDBE8\uDCF6" + ChatColor.RESET + ChatColor.of("#BE517F") + "El tótem del jugador "
                        + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                        + ChatColor.RESET + ChatColor.of("#BE517F") + " ha fallado por corrupción."
                        + "\n" + ChatColor.of("#B228E7") + "Corrupción Ansiosa: " +
                        ChatColor.of("#F7AD62") + String.format("%.1f", currentCorruption) + "%"
                        + "\n" + ChatColor.of("#F52F6A") + ChatColor.BOLD + " ⚠" +
                        ChatColor.of("#B228E7") + " Probabilidad de fallo: " +
                        ChatColor.of("#F7AD62") + String.format("%.1f", failChance * 100) + "%"
                        + "\n" + ChatColor.of("#33cc33") + "¡La corrupción ansiosa ha disminuido 10% para todos los jugadores!"
                        + "\n");

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.playSound(onlinePlayer, "entity.wither.death", SoundCategory.VOICE, 2f, 0.5f);
                onlinePlayer.playSound(onlinePlayer, "custom.noti", SoundCategory.VOICE, 2f, 2f);
            }
        }
    }
}