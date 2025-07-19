package Events.AchievementParty;

import org.bukkit.plugin.java.JavaPlugin;

public interface Achievement {
    String getName();
    String getDescription();
    void initializePlayerData(String playerName);
    void checkCompletion(String playerName);
}