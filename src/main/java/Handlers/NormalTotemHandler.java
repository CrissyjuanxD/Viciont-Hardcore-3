package Handlers;

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
    private final Random random = new Random();

    public NormalTotemHandler(Plugin plugin, DayHandler handler) {
        this.plugin = plugin;
        this.dayHandler = handler;
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

            if (meta != null && meta.hasCustomModelData()) {
                // Tótems especiales (con CustomModelData) siempre funcionan
                int customModelData = meta.getCustomModelData();
                switch (customModelData) {
                    case 1:
                        broadcastTotemMessage2(player);
                        break;
                    case 2:
                        broadcastTotemMessage(player);
                        break;
                    case 3:
                        broadcastTotemMessage3(player);
                        break;
                    case 4:
                        broadcastTotemMessage4(player);
                        break;
                    case 5:
                        broadcastTotemMessage5(player);
                        break;
                    default:
                        broadcastNormalTotemMessage(player, 100, 100);
                        break;
                }
            } else {
                if (currentDay >= 7) {
                    int randomValue = random.nextInt(100); // 0-99
                    int displayThreshold = 99; // Siempre mostramos X/99

                    if (currentDay >= 11) {
                        // 10% de probabilidad de fallo (valores 90-99)
                        if (randomValue >= 90) { // 90,91,...,99 (10 valores)
                            event.setCancelled(true);
                            broadcastTotemFail(player, randomValue, displayThreshold);
                            return;
                        }
                    } else {
                        // Días 7-10: 1% de probabilidad (valor 99)
                        if (randomValue == 99) {
                            event.setCancelled(true);
                            broadcastTotemFail(player, randomValue, displayThreshold);
                            return;
                        }
                    }
                    broadcastNormalTotemMessage(player, displayThreshold, randomValue);
                } else {
                    // Antes del día 7, siempre funciona
                    broadcastNormalTotemMessage(player, 100, 100);
                }
            }
        }
    }

    private void broadcastNormalTotemMessage(Player player, int successProbability, int randomValue) {
        int currentDay = dayHandler.getCurrentDay();
        String baseMessage = ChatColor.translateAlternateColorCodes('&',
                "\n"
                        + "\uDBE8\uDCF6"
                        + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                        + ChatColor.RESET + ChatColor.of("#B684E4") + " ha consumido un tótem.");

        if (currentDay >= 7 && successProbability < 100) {
            String threshold = (currentDay >= 11) ? "90-99" : "99";
            baseMessage += "\n"
                    + ChatColor.of("#F52F6A") + ChatColor.BOLD + " ⚠"
                    + ChatColor.of("#B228E7") + " Probabilidad de fallo"
                    + ChatColor.GRAY + " ("
                    + ChatColor.of("#F7AD62") + ChatColor.BOLD + randomValue
                    + ChatColor.GRAY + "/"
                    + ChatColor.of("#F52F6A") + ChatColor.BOLD + threshold
                    + ChatColor.GRAY + ")";
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

    private void broadcastTotemMessage(Player player) {
        String message = ChatColor.translateAlternateColorCodes('&',
                "\n"
                + "\uDBE8\uDCF6"
                + ChatColor.of("#007EB2") + ChatColor.BOLD + player.getName()
                + ChatColor.RESET + ChatColor.of("#8EBFEC") + " ha " + ChatColor.BOLD + "disminuido un uso "
                + ChatColor.RESET + ChatColor.of("#8EBFEC") + "su tótem de doble vida."
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

    private void broadcastTotemMessage2(Player player) {
        String message = ChatColor.translateAlternateColorCodes('&',
                "\n"
                + "\uDBE8\uDCF6"
                + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                + ChatColor.RESET + ChatColor.of("#B684E4") + " ha consumido un " + ChatColor.BOLD + "tótem de doble vida."
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

    private void broadcastTotemMessage3(Player player) {
        String message = ChatColor.translateAlternateColorCodes('&',
                "\n"
                + "\uDBE8\uDCF6"
                + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                + ChatColor.RESET + ChatColor.of("#B684E4") + " ha consumido un " + ChatColor.of("#33ccff") + ChatColor.BOLD + "Life Totem."
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

    private void broadcastTotemMessage4(Player player) {
        String message = ChatColor.translateAlternateColorCodes('&',
                "\n"
                        + "\uDBE8\uDCF6"
                        + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                        + ChatColor.RESET + ChatColor.of("#B684E4") + " ha consumido un " + ChatColor.of("#66ff99") + ChatColor.BOLD + "Spider Totem."
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

    private void broadcastTotemMessage5(Player player) {
        String message = ChatColor.translateAlternateColorCodes('&',
                "\n"
                        + "\uDBE8\uDCF6"
                        + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                        + ChatColor.RESET + ChatColor.of("#B684E4") + " ha consumido un " + ChatColor.of("#ff3300") + ChatColor.BOLD + "Infernal Totem."
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

    private void broadcastTotemFail(Player player, int randomValue, int expectedValue) {
        String playerFailed = "\n"
                + ChatColor.RED + "\uDBE8\uDCF6" + ChatColor.RESET + ChatColor.of("#BE517F") + "El tótem del jugador "
                + ChatColor.of("#F7AD62") + ChatColor.BOLD + player.getName()
                + ChatColor.RESET + ChatColor.of("#BE517F") + " ha fallado.";
        String failureNotice = "\n"
                + ChatColor.of("#F52F6A") + ChatColor.BOLD + " ⚠" // Icono de advertencia
                + ChatColor.of("#B228E7") + " Probabilidad de fallo"
                + ChatColor.GRAY + " ("
                + ChatColor.of("#F52F6A") + ChatColor.BOLD + randomValue
                + ChatColor.GRAY + "/"
                + ChatColor.of("#F52F6A") + ChatColor.BOLD + expectedValue
                + ChatColor.GRAY + ")";
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage("\n" + playerFailed + "\n" + failureNotice + "\n");
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.playSound(onlinePlayer, "entity.wither.death", SoundCategory.VOICE, 2f, 0.5f);
                onlinePlayer.playSound(onlinePlayer, "custom.noti", SoundCategory.VOICE, 2f, 2f);

            }
        }
    }

}