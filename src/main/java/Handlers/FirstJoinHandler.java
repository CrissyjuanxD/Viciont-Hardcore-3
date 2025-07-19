package Handlers;

import Events.AchievementParty.AchievementPartyHandler;
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
    private final AchievementPartyHandler achievementHandler;

    public FirstJoinHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.achievementHandler = new AchievementPartyHandler(plugin);
    }

    @EventHandler
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

            // Asignar el equipo "TSurvivor" si existe
            Team survivorTeam = scoreboard.getTeam("TSurvivor");
            if (survivorTeam != null) {
                survivorTeam.addEntry(player.getName());
            }

            giveWelcomeKit(player);

            // Mensajes adicionales o tareas que desees realizar en el ingreso
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                player.sendMessage("§e۞ Has recibido tu kit de bienvenida!");
            }, 40);
        }
    }


    private void checkPendingPenalty(Player player) {
        FileConfiguration data = YamlConfiguration.loadConfiguration(achievementHandler.getAchievementsFile());
        if (data.getBoolean("players." + player.getName() + ".penalized", false)) {
            achievementHandler.applyPenalty(player);
            data.set("players." + player.getName() + ".penalized", false);
            try {
                data.save(achievementHandler.getAchievementsFile());
            } catch (IOException e) {
                plugin.getLogger().severe("Error al guardar datos de penalización: " + e.getMessage());
            }
        }
    }

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
