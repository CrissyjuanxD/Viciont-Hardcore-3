package SlotMachine.cache.tools.smachine;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Frame de configuración para SlotMachine - Basado en DTools3
 */
public class SlotMachineFrame {

    private final int maxTicks;
    private final int fadeIn;
    private final int stay;
    private final int fadeOut;
    private final String messageType;

    public SlotMachineFrame(ConfigurationSection section) {
        if (section == null) {
            this.maxTicks = 0;
            this.fadeIn = 0;
            this.stay = 5;
            this.fadeOut = 0;
            this.messageType = "TITLE";
        } else {
            this.maxTicks = section.getInt("max-ticks", 0);
            this.fadeIn = section.getInt("fade-in", 0);
            this.stay = section.getInt("stay", 5);
            this.fadeOut = section.getInt("fade-out", 0);
            this.messageType = section.getString("message-type", "TITLE");
        }
    }

    // Getters
    public int getMaxTicks() { return maxTicks; }
    public int getFadeIn() { return fadeIn; }
    public int getStay() { return stay; }
    public int getFadeOut() { return fadeOut; }
    public String getMessageType() { return messageType; }
}