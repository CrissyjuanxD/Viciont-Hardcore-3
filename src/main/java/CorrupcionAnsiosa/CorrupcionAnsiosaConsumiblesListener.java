package CorrupcionAnsiosa;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CorrupcionAnsiosaConsumiblesListener implements Listener {

    private final CorrupcionAnsiosaManager corruptionManager;

    public CorrupcionAnsiosaConsumiblesListener(CorrupcionAnsiosaManager manager) {
        this.corruptionManager = manager;
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {

        Player player = event.getPlayer();
        Material type = event.getItem().getType();

        ItemMeta meta = event.getItem().getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return;


        int cmd = meta.getCustomModelData();

        // ================================
        // SÉRUM DE SERENIDAD (Honey Bottle)
        // ================================
        if (type == Material.HONEY_BOTTLE && cmd == 2) {

            double amount = 15;
            corruptionManager.addCorruption(player, amount);

            double totalAfter = corruptionManager.getCorruption(player);
            sendFancyIncreaseMessage(player, amount, totalAfter);

            // EFECTOS DEL SÉRUM (equivalente a golden apple +1 nivel)
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1)); // regen II, 10s
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1));    // absorption II, 10s
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 60, 0));     // saturation I, 3s

            return;
        }

        // ================================
        // MANZANA MARCHITA (Golden Apple NORMAL)
        // ================================
        if (type == Material.GOLDEN_APPLE && cmd == 4) {

            double amount = 4;

            Bukkit.getScheduler().runTask(corruptionManager.getPlugin(), () -> {
                // Se dejan sus efectos vanilla intactos
            });

            corruptionManager.addCorruption(player, amount);

            double finalValue = corruptionManager.getCorruption(player);
            sendFancyIncreaseMessage(player, amount, finalValue);
            return;
        }

        // =======================================
        //  FRAGMENTO CORDURA (instantáneo)
        // =======================================
        if (type == Material.GOLDEN_APPLE && cmd == 2) {

            event.setCancelled(true);
            consumeInstant(player);

            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1f, 0.7f);
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 0.7f);

            double amount = 1;
            corruptionManager.addCorruption(player, amount);

            double after = corruptionManager.getCorruption(player);
            sendFancyIncreaseMessage(player, amount, after);

            return;
        }

        // =======================================
        //  COMPUESTO S13 (instantáneo + sonido)
        // =======================================
        if (type == Material.GOLDEN_APPLE && cmd == 6) {

            event.setCancelled(true);
            consumeInstant(player);

            player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f, 1.4f);

            double amount = 8;
            corruptionManager.addCorruption(player, amount);

            double after = corruptionManager.getCorruption(player);
            sendFancyIncreaseMessage(player, amount, after);

            return;
        }
    }

    private void consumeInstant(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getAmount() > 1)
            hand.setAmount(hand.getAmount() - 1);
        else
            player.getInventory().setItemInMainHand(null);
    }

    private void sendFancyIncreaseMessage(Player player, double amount, double totalAfter) {

        String msg =
                ChatColor.GOLD + "" + ChatColor.BOLD + "⚡" +
                        ChatColor.RESET + ChatColor.of("#AD71F1") + " Corrupción ansiosa aumentada:" +
                        ChatColor.RESET + " " +
                        ChatColor.of("#9ADE6F") + ChatColor.BOLD + "+" + String.format("%.0f", amount) + "%" +
                        ChatColor.RESET + " " +
                        ChatColor.of("#B684E4") + "➤ " +
                        ChatColor.of("#EEC185") + ChatColor.BOLD + String.format("%.0f", totalAfter) + "%";

        player.sendMessage(msg);
    }
}
