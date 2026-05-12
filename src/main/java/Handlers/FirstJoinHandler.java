package Handlers;

import CorrupcionAnsiosa.CorrupcionAnsiosaManager;
import Events.AchievementParty.AchievementPartyHandler;
import Events.MissionSystem.MissionHandler;
import Handlers.Teams.TeamType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.entity.Player;

import java.io.IOException;

public class FirstJoinHandler implements Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
/*    private final AchievementPartyHandler achievementHandler;*/
    private final CorrupcionAnsiosaManager corruptionManager;
    private final DatabaseManager databaseManager;
    private final TeamsHandler teamsHandler;


    public FirstJoinHandler(JavaPlugin plugin, MissionHandler missionHandler, CorrupcionAnsiosaManager corruptionManager, DatabaseManager databaseManager, TeamsHandler teamsHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
/*        this.achievementHandler = new AchievementPartyHandler(plugin);*/
        this.corruptionManager = corruptionManager;
        this.databaseManager = databaseManager;
        this.teamsHandler = teamsHandler;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!databaseManager.hasJoinedBefore(player.getUniqueId())) {
            databaseManager.registerPlayer(player.getUniqueId(), player.getName(), TeamType.Z_MIEMBRO.getId());
            teamsHandler.addPlayerToTeam(player, TeamType.Z_MIEMBRO);

            giveWelcomeKit(player);
        }
    }

/*    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getEntryTeam(player.getName());

        // Verifica si el jugador ya ha entrado antes usando su UUID
        if (!this.plugin.getConfig().getBoolean("HasJoinedBefore." + player.getUniqueId())) {
            // Marcar al jugador como que ya ha entrado
            this.plugin.getConfig().set("HasJoinedBefore." + player.getUniqueId(), true);
            this.plugin.saveConfig();

            // Inicializar datos si el evento está activo
            if (achievementHandler.isEventActive()) {
                achievementHandler.initializePlayerTracking(player.getName());
            }

            // Inicializar datos de misiones para misiones activas
            if (missionSystemCommands != null) {
                for (int missionNumber : missionSystemCommands.getMissionHandler().getActiveMissions()) {
                    missionSystemCommands.getMissionHandler().initializePlayerMissionData(player.getName(), missionNumber);
                }
            }

            // Asignar el equipo "TSurvivor" si existe
            Team survivorTeam = scoreboard.getTeam("TSurvivor");
            if (survivorTeam != null) {
                survivorTeam.addEntry(player.getName());
            }

            corruptionManager.initializePlayerData(player);
            giveWelcomeKit(player);

            // Mensajes adicionales o tareas que desees realizar en el ingreso
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                player.sendMessage("§e۞ Has recibido tu kit de bienvenida!");
            }, 40);
        } else {
            // Para jugadores que ya han entrado antes, asegurar que tengan datos de corrupción
            corruptionManager.initializePlayerData(player);
        }
    }*/

    private void giveWelcomeKit(Player player) {
        // Crear los items del kit
        ItemStack cookedBeef = new ItemStack(Material.COOKED_BEEF, 16);
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        ItemStack clock = new ItemStack(Material.CLOCK, 1);

        // Dar los items al jugador
        player.getInventory().addItem(cookedBeef, totem, clock);

        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), cookedBeef);
            player.getWorld().dropItemNaturally(player.getLocation(), totem);
            player.getWorld().dropItemNaturally(player.getLocation(), clock);
            player.sendMessage("§c¡Tu inventario estaba lleno! Los items se han dejado caer al suelo.");
        }
    }
}
