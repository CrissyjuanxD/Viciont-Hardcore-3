package Handlers;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ToastHandler {

    private final JavaPlugin plugin;

    public ToastHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendToast(Player player, String title, String description, String iconMaterial) {
        String id = "custom:toast_" + System.nanoTime();

        String json = """
    {
      "display": {
        "icon": { "id": "%s" },
        "title": { "text": "%s" },
        "description": { "text": "%s" },
        "frame": "task",
        "announce_to_chat": false,
        "show_toast": true,
        "hidden": true
      },
      "criteria": {
        "trigger": {
          "trigger": "minecraft:impossible"
        }
      }
    }
    """.formatted(iconMaterial, title, description);

        NamespacedKey key = NamespacedKey.fromString(id);
        try {
            Bukkit.getUnsafe().loadAdvancement(key, json);

            Advancement adv = Bukkit.getAdvancement(key);
            if (adv == null) {
                plugin.getLogger().warning("Advancement was null for key " + key);
                return;
            }

            AdvancementProgress progress = player.getAdvancementProgress(adv);
            progress.awardCriteria("trigger");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    for (String crit : adv.getCriteria()) {
                        player.getAdvancementProgress(adv).revokeCriteria(crit);
                    }
                    Bukkit.getUnsafe().removeAdvancement(key);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 20L);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
