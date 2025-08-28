package Casino;

import org.bukkit.plugin.java.JavaPlugin;

public class CasinoManager {
    private final JavaPlugin plugin;
    private final SlotMachine slotMachine;
    private final BlackJack blackJack;

    public CasinoManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.slotMachine = new SlotMachine(plugin);
        this.blackJack = new BlackJack(plugin);
    }

    public SlotMachine getSlotMachine() {
        return slotMachine;
    }

    public BlackJack getBlackJack() {
        return blackJack;
    }
}