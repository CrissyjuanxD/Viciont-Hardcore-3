package Commands;

import Events.MissionSystem.MissionCommands;
import Events.MissionSystem.MissionGUI;
import Events.MissionSystem.MissionHandler;
import Handlers.DayHandler;
import org.bukkit.plugin.java.JavaPlugin;

public class MissionSystemCommands {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final MissionGUI missionGUI;
    private final MissionCommands missionCommands;

    public MissionSystemCommands(JavaPlugin plugin, DayHandler dayHandler) {
        this.plugin = plugin;
        this.missionHandler = new MissionHandler(plugin, dayHandler);
        this.missionGUI = new MissionGUI(plugin, missionHandler);
        this.missionCommands = new MissionCommands(missionHandler, missionGUI);

        registerCommands();
    }

    private void registerCommands() {
        plugin.getCommand("activarmision").setExecutor(missionCommands);
        plugin.getCommand("activarmision").setTabCompleter(missionCommands);

        plugin.getCommand("desactivarmision").setExecutor(missionCommands);
        plugin.getCommand("desactivarmision").setTabCompleter(missionCommands);

        plugin.getCommand("addmision").setExecutor(missionCommands);
        plugin.getCommand("addmision").setTabCompleter(missionCommands);

        plugin.getCommand("removemision").setExecutor(missionCommands);
        plugin.getCommand("removemision").setTabCompleter(missionCommands);

        plugin.getCommand("penalizadosmisiones").setExecutor(missionCommands);

        plugin.getCommand("misiones").setExecutor(missionCommands);
    }

    public MissionHandler getMissionHandler() {
        return missionHandler;
    }

    public MissionGUI getMissionGUI() {
        return missionGUI;
    }
}