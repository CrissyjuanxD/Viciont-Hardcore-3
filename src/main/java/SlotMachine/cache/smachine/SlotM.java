package SlotMachine.cache.smachine;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Representación de un slot individual - Basado en DTools3
 */
public class SlotM {
    
    private final String id;
    private final String name;
    private final String itemRewardId;
    private final int itemRewardAmount;
    private final double probability;
    private final String animation;
    private final double waitTime;
    private final String sound;
    private final boolean hasReward;
    
    public SlotM(String id, ConfigurationSection section) {
        this.id = id;
        this.name = section.getString("name", id);
        this.itemRewardId = section.getString("item_reward.id");
        this.itemRewardAmount = section.getInt("item_reward.amount", 1);
        this.animation = section.getString("animation", "default");
        this.waitTime = section.getDouble("wait-time", 5.0);
        this.sound = section.getString("sound", "dtools3:tools.casino.win");
        this.hasReward = itemRewardId != null && !itemRewardId.isEmpty();
        
        // Parsear probabilidad
        String probabilityStr = section.getString("probability", "0%");
        this.probability = parseProbability(probabilityStr);
    }
    
    private double parseProbability(String probabilityStr) {
        try {
            if (probabilityStr.endsWith("%")) {
                return Double.parseDouble(probabilityStr.substring(0, probabilityStr.length() - 1)) / 100.0;
            } else {
                return Double.parseDouble(probabilityStr);
            }
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getItemRewardId() { return itemRewardId; }
    public int getItemRewardAmount() { return itemRewardAmount; }
    public double getProbability() { return probability; }
    public String getAnimation() { return animation; }
    public double getWaitTime() { return waitTime; }
    public String getSound() { return sound; }
    public boolean hasReward() { return hasReward; }
}