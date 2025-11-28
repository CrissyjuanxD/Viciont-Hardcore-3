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
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatMessageType;

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

        if (!isMainHandTotem && !isOffHandTotem) return;

        ItemStack totem = isMainHandTotem ? mainHandItem : offHandItem;
        ItemMeta meta = totem.getItemMeta();

        PlayerCorruptionData corruptionData = corruptionManager.getPlayerData(player);
        double beforeCorruption = corruptionData.getCorruption();

        int model = (meta != null && meta.hasCustomModelData()) ? meta.getCustomModelData() : 0;
        if (model == 1) {
            broadcastDoubleTotemlastuse(player);
            return;
        }
        if (model == 2) {
            broadcastDoubleTotem(player);
            return;
        }
        if (model == 9) {
            broadcastDefinitiveTotem(player);
            return;
        }

        // === Reducción según tipo ===
        double reduction = getCorruptionReduction(model, currentDay);

        // Aplicar reducción ANTES de calcular probabilidad
        if (reduction > 0) corruptionManager.removeCorruption(player, reduction);

        // Obtener corrupción actualizada y nivel real de fallo
        double newCorruption = corruptionManager.getPlayerData(player).getCorruption();
        double failChance = corruptionManager.getPlayerData(player).getFailChance(currentDay);

        // Generar número 0–99 y calcular umbral
        int roll = random.nextInt(100);
        int failStart = (int) Math.ceil(100 - (failChance * 100)); // ej. 95 para 5%
        boolean fails = failChance > 0 && roll >= failStart;

        if (fails) {
            event.setCancelled(true);
            broadcastTotemFailByCorruption(player, newCorruption, failChance, roll, 99);

            // Reducir -10% a todos
            for (Player online : Bukkit.getOnlinePlayers()) {
                corruptionManager.removeCorruption(online, 10.0);
                online.sendMessage(ChatColor.of("#B228E7") + "¡La corrupción ansiosa ha disminuido en 10% para todos!");
            }
            return;
        }

        switch (model) {
            case 3 -> broadcastLifeTotem(player, newCorruption, failChance, roll, failStart);
            case 4 -> broadcastSpiderTotem(player, newCorruption, failChance, roll, failStart);
            case 5 -> broadcastInfernalTotem(player, newCorruption, failChance, roll, failStart);
            case 6 -> broadcastIceTotem(player, newCorruption, failChance, roll, failStart);
            case 7 -> broadcastFlyTotem(player, newCorruption, failChance, roll, failStart);
            case 8 -> broadcastInventoryTotem(player, newCorruption, failChance, roll, failStart);
            default -> broadcastNormalTotemMessage(player, newCorruption, failChance, roll, failStart);
        }

        // Aplicar efectos de corrupción
        corruptionEffectsHandler.applyCorruptionEffects(player, currentDay);
    }

    private double getCorruptionReduction(int customModelData, int currentDay) {
        // Tótems que no reducen corrupción (Doble y Definitivo)
        if (customModelData == 1 || customModelData == 2 || customModelData == 9) return 0;

        if (currentDay >= 26) {
            return switch (customModelData) {
                case 0 -> 100.0;
                case 6, 7 -> 14.0;
                case 3 -> 12.0;
                case 4, 5, 8 -> 8.0;
                default -> 0;
            };
        } else if (currentDay >= 22) {
            return switch (customModelData) {
                case 0 -> 50.0;
                case 6, 7 -> 13.0;
                case 3 -> 11.0;
                case 4, 5, 8 -> 7.0;
                default -> 0;
            };
        } else if (currentDay >= 18) {
            return switch (customModelData) {
                case 0 -> 32.0;
                case 6, 7 -> 12.0;
                case 3 -> 10.0;
                case 4, 5, 8 -> 6.0;
                default -> 0;
            };
        } else if (currentDay >= 12) {
            return switch (customModelData) {
                case 0 -> 16.0;
                case 6, 7 -> 9.0;
                case 3 -> 8.0;
                case 4, 5 -> 4.0;
                default -> 0;
            };
        } else if (currentDay >= 8) {
            return switch (customModelData) {
                case 0 -> 8.0;
                case 6, 7 -> 3.0;
                default -> 0;
            };
        } else if (currentDay >= 4) {
            return switch (customModelData) {
                case 0 -> 4.0;
                case 6, 7 -> 1.0;
                default -> 0;
            };
        } else {
            return customModelData == 0 ? 2.0 : 0;
        }
    }

    private void broadcastNormalTotemMessage(Player player, double newCorruption, double failChance, int roll, int failStart) {
        StringBuilder msg = new StringBuilder();
        msg.append("\n").append("\uDBE8\uDCF6")
                .append(ChatColor.of("#F7AD62")).append(ChatColor.BOLD).append(player.getName())
                .append(ChatColor.RESET).append(ChatColor.of("#B684E4"))
                .append(" ha consumido un tótem.");

        msg.append("\n")
                .append(ChatColor.of("#B228E7")).append("۞ ")
                .append(ChatColor.of("#A777E9")).append("Corrupción Ansiosa:")
                .append(ChatColor.of("#EEC185")).append(" ").append(String.format("%.0f", newCorruption)).append("%");

        if (failChance > 0) {
            msg.append(ChatColor.of("#B684E4")).append(" ┃ ")
                    .append(ChatColor.of("#F52F6A")).append("⚠ ")
                    .append(ChatColor.of("#CB769C")).append("Prob. Fallar:")
                    .append(ChatColor.of("#A07396")).append(" (")
                    .append(ChatColor.of("#EEC185")).append(roll)
                    .append(ChatColor.of("#A07396")).append("/")
                    .append(ChatColor.of("#CF5B68")).append(failStart)
                    .append(ChatColor.of("#A07396")).append(")");
        }

        msg.append("\n");
        sendToAll(msg.toString(), player);
    }


    private void broadcastTotemFailByCorruption(Player player, double corruption, double failChance, int roll, int maxValue) {
        int failStart = (int) Math.ceil(100 - (failChance * 100));

        String msg = "\n" + ChatColor.RED + "\uDBE8\uDCF6"
                + ChatColor.RESET + ChatColor.of("#BE517F") + "El tótem del jugador "
                + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                + ChatColor.RESET + ChatColor.of("#BE517F") + " ha fallado por corrupción."
                + "\n" + ChatColor.of("#B228E7") + "۞ "
                + ChatColor.of("#A777E9") + "Corrupción Ansiosa:"
                + ChatColor.of("#EEC185") + " " + String.format("%.0f", corruption) + "%"
                + ChatColor.of("#B684E4") + " ┃ "
                + ChatColor.of("#F52F6A") + "⚠ "
                + ChatColor.of("#CB769C") + "Prob. Fallar:"
                + ChatColor.of("#A07396") + " (" + ChatColor.of("#EEC185") + roll + ChatColor.of("#A07396") + "/"
                + ChatColor.of("#CF5B68") + failStart + ChatColor.of("#A07396") + ")"
                + "\n" + ChatColor.of("#33cc33") + "¡La corrupción ansiosa ha disminuido 10% para todos los jugadores!\n";

        sendToAll(msg, player);
    }

    // ==== TÓTEMS ESPECIALES (formato limpio) ====

    private void broadcastDoubleTotem(Player player) {
        String msg = "\n\uDBE8\uDCF6" + ChatColor.of("#007EB2") + ChatColor.BOLD + player.getName()
                + ChatColor.RESET + ChatColor.of("#8EBFEC") + " ha bajado un uso su "
                + ChatColor.of("#007EB2") + ChatColor.BOLD + "tótem de doble vida" + ChatColor.RESET + ChatColor.of("#8EBFEC") + ".\n";
        sendToAll(msg, player);
    }

    private void broadcastDoubleTotemlastuse(Player player) {
        String msg = "\n\uDBE8\uDCF6" + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                + ChatColor.RESET + ChatColor.of("#B684E4") + " ha consumido un "
                + ChatColor.of("#F7AD62") + ChatColor.BOLD + "tótem de doble vida" + ChatColor.RESET + ChatColor.of("#B684E4") + ".\n";
        sendToAll(msg, player);
    }

    private void broadcastLifeTotem(Player player, double corruption, double failChance, int roll, int failStart) {
        sendCustomTotemMessage(player, corruption, failChance, roll, failStart, "Life Totem", "#33ccff");
    }

    private void broadcastSpiderTotem(Player player, double corruption, double failChance, int roll, int failStart) {
        sendCustomTotemMessage(player, corruption, failChance, roll, failStart, "Spider Totem", "#66ff99");
    }

    private void broadcastInfernalTotem(Player player, double corruption, double failChance, int roll, int failStart) {
        sendCustomTotemMessage(player, corruption, failChance, roll, failStart, "Infernal Totem", "#ff3300");
    }

    private void broadcastIceTotem(Player player, double corruption, double failChance, int roll, int failStart) {
        sendCustomTotemMessage(player, corruption, failChance, roll, failStart, "Ice Totem", "#00ccff");
    }

    private void broadcastFlyTotem(Player player, double corruption, double failChance, int roll, int failStart) {
        sendCustomTotemMessage(player, corruption, failChance, roll, failStart, "Fly Totem", "#ffcc00");
    }

    private void broadcastInventoryTotem(Player player, double corruption, double failChance, int roll, int failStart) {
        sendCustomTotemMessage(player, corruption, failChance, roll, failStart, "Inventory Totem", "#9966ff");
    }

    private void broadcastDefinitiveTotem(Player player) {
        String msg = "\n\uDBE8\uDCF6" + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                + ChatColor.RESET + ChatColor.of("#B684E4") + " ha consumido un "
                + ChatColor.of("#ff66cc") + ChatColor.BOLD + "tótem definitivo" + ChatColor.RESET + ChatColor.of("#B684E4") + ".\n";
        sendToAll(msg, player);
    }

    private void sendCustomTotemMessage(Player player, double corruption, double failChance, int roll, int failStart, String totemName, String colorHex) {
        StringBuilder msg = new StringBuilder();

        msg.append("\n\uDBE8\uDCF6")
                .append(ChatColor.of("#F7AD62")).append(ChatColor.BOLD).append(player.getName())
                .append(ChatColor.RESET).append(ChatColor.of("#B684E4"))
                .append(" ha consumido un ")
                .append(ChatColor.of(colorHex)).append(ChatColor.BOLD).append(totemName)
                .append(ChatColor.RESET).append(ChatColor.of("#B684E4")).append(".");

        // Línea decorada tipo tellraw
        msg.append("\n")
                .append(ChatColor.of("#B228E7")).append("۞ ")
                .append(ChatColor.of("#A777E9")).append("Corrupción Ansiosa:")
                .append(ChatColor.of("#EEC185")).append(" ").append(String.format("%.0f", corruption)).append("%");

        if (failChance > 0) {
            msg.append(ChatColor.of("#B684E4")).append(" ┃ ")
                    .append(ChatColor.of("#F52F6A")).append("⚠ ")
                    .append(ChatColor.of("#CB769C")).append("Prob. Fallar:")
                    .append(ChatColor.of("#A07396")).append(" (")
                    .append(ChatColor.of("#EEC185")).append(roll)
                    .append(ChatColor.of("#A07396")).append("/")
                    .append(ChatColor.of("#CF5B68")).append(failStart)
                    .append(ChatColor.of("#A07396")).append(")");
        }
        msg.append("\n");
        sendToAll(msg.toString(), player);
    }

    private void sendToAll(String message, Player player) {
        String finalMsg = ChatColor.translateAlternateColorCodes('&', message);
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(finalMsg);
            if (!online.equals(player)) {
                online.playSound(online, "item.trident.return", SoundCategory.VOICE, 2f, 2f);
                online.playSound(online, "custom.noti", SoundCategory.VOICE, 2f, 2f);
                online.playSound(online, "entity.allay.item_thrown", SoundCategory.VOICE, 2f, 0.5f);
            }
        }
    }
}
