package Events.MissionSystem;

import org.bukkit.inventory.ItemStack;
import java.util.List;

public interface Mission {
    String getName();
    String getDescription();
    void initializePlayerData(String playerName);
    void checkCompletion(String playerName);
    List<ItemStack> getRewards();
    int getMissionNumber();
}