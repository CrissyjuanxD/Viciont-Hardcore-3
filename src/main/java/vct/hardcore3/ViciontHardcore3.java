package vct.hardcore3;

import org.bukkit.*;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import Commands.DeathStormCommand;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Scoreboard;



import java.util.Objects;

public class ViciontHardcore3 extends JavaPlugin implements Listener {

    public String Prefix = "&d&lViciont&5&lHardcore &5&l3&7➤ &f";
    public String Version = getDescription().getVersion();
    private DeathStormHandler deathStormHandler;


    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', Prefix + "&aha sido habilitado!, &eVersion: " + Version));
        // Registra los eventos de esta clase
        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        //resgistra evento de revivir
        Objects.requireNonNull(this.getCommand("revive")).setExecutor(new ReviveCommand(this));
        //registra evento de doble totem
        getServer().getPluginManager().registerEvents(new DoubleLifeTotemHandler(this), this);
        getLogger().info("DoubleLifeTotemHandler registered!");

        //registra los cambios de los dias
        PluginCommand changeDayCommand = getCommand("cambiardia");
        PluginCommand dayCommand = getCommand("dia");

        //DeathStorm
        deathStormHandler = new DeathStormHandler(this);
        getServer().getPluginManager().registerEvents(deathStormHandler, this);

        PluginCommand resetCommand = getCommand("resetdeathstorm");
        PluginCommand addCommand = getCommand("adddeathstorm");
        PluginCommand removeCommand = getCommand("removedeathstorm");

        if (resetCommand != null) {
            resetCommand.setExecutor(new DeathStormCommand(deathStormHandler));
        }
        if (addCommand != null) {
            addCommand.setExecutor(new DeathStormCommand(deathStormHandler));
        }
        if (removeCommand != null) {
            removeCommand.setExecutor(new DeathStormCommand(deathStormHandler));
        }
        if (changeDayCommand != null) {
            changeDayCommand.setExecutor(new DayCommandHandler(deathStormHandler));
        }
        if (dayCommand != null) {
            dayCommand.setExecutor(new DayCommandHandler(deathStormHandler));
        }

        deathStormHandler.loadStormData(); // Cargar datos de la tormenta al iniciar
    }

    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', Prefix + "&aha sido deshabilitado!, &eVersion: " + Version));

        //deathsotrm
        deathStormHandler.saveStormData(); // Guardar datos de la tormenta al deshabilitar
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String message = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "∨ " + event.getPlayer().getName() + ChatColor.RESET + ChatColor.LIGHT_PURPLE + " ha entrado a Viciont Hardcore 3";
        event.setJoinMessage(message);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String message = ChatColor.GRAY + "" + ChatColor.BOLD + "∨ " + event.getPlayer().getName() + ChatColor.RESET + ChatColor.GRAY + " ha salido de Viciont Hardcore 3";
        event.setQuitMessage(message);
    }

    //Formateo del chat

}
