package TitleListener;

import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SuccessNotification {

    private final JavaPlugin plugin;

    public SuccessNotification(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void showSuccess(Player player) {
        player.playSound(player.getLocation(), "custom.noti", SoundCategory.VOICE, 1f, 0.6f);

        String titleMessage = "§a\uEAA5";
        player.sendTitle(titleMessage, "", 5, 10, 5);
    }

}
