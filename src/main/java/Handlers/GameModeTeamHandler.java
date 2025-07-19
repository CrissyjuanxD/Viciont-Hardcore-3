package Handlers;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;

public class GameModeTeamHandler implements Listener {
    private final JavaPlugin plugin;
    private final File playerDataFile;
    private final FileConfiguration playerData;

    public GameModeTeamHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerDataFile = new File(plugin.getDataFolder(), "teams.yml");

        if (!playerDataFile.exists()) {
            try {
                playerDataFile.getParentFile().mkdirs();
                playerDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.playerData = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String playerName = player.getName();

        if (event.getNewGameMode() == GameMode.SPECTATOR) {
            // Guarda el equipo actual antes de cambiar a espectador
            Team currentTeam = scoreboard.getEntryTeam(playerName);
            if (currentTeam != null) {
                playerData.set(playerName, currentTeam.getName());
                saveData();
            }
            // Asigna al equipo de fantasmas
            Team ghostTeam = scoreboard.getTeam("ZFantasma");
            if (ghostTeam != null) {
                ghostTeam.addEntry(playerName);
            }
        } else {
            // Recupera el equipo anterior si existe en el archivo
            String previousTeamName = playerData.getString(playerName);
            Team previousTeam = (previousTeamName != null) ? scoreboard.getTeam(previousTeamName) : null;

            if (previousTeam != null) {
                previousTeam.addEntry(playerName);
            }
            // Elimina el jugador del archivo para evitar datos innecesarios
            playerData.set(playerName, null);
            saveData();
        }
    }

    private void saveData() {
        try {
            playerData.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
