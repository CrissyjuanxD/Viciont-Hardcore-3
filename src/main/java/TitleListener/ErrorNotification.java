package TitleListener;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ErrorNotification {

    private final JavaPlugin plugin;

    public ErrorNotification(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void showSuccess(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, SoundCategory.VOICE, 1f, 0.6f);

        String titleMessage = "§c\uEAA5";
        player.sendTitle(titleMessage, "", 5, 10, 5);
    }

}
