package vct.hardcore3;

import Commands.PingCommand;
import list.VHList;
import org.bukkit.*;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import Commands.DeathStormCommand;
import Commands.DayCommandHandler;
import Commands.ReviveCommand;
import chat.chatgeneral;
import Dificultades.DayOneChanges;
import java.util.Objects;

public class ViciontHardcore3 extends JavaPlugin implements Listener {

    public String Prefix = "&d&lViciont&5&lHardcore &5&l3&7➤ &f";
    public String Version = getDescription().getVersion();
    private DeathStormHandler deathStormHandler;
    private DayHandler dayHandler; // Asegurarse de usar esta variable de instancia
    private DayOneChanges dayOneChanges;

    @Override
    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', Prefix + "&aha sido habilitado!, &eVersion: " + Version));

        // Registra los eventos de esta clase
        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        // Registra evento de revivir
        Objects.requireNonNull(this.getCommand("revive")).setExecutor(new ReviveCommand(this));

        // Registra evento de doble tótem
        getServer().getPluginManager().registerEvents(new DoubleLifeTotemHandler(this), this);
        getLogger().info("DoubleLifeTotemHandler registered!");

        // Inicializa correctamente el DayHandler y asigna a la variable de instancia
        dayHandler = new DayHandler(this);

        // DeathStorm
        deathStormHandler = new DeathStormHandler(this, dayHandler); // Pasa dayHandler al DeathStormHandler
        getServer().getPluginManager().registerEvents(deathStormHandler, this);

        // Comandos de DeathStorm
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

        // Comandos de días
        PluginCommand changeDayCommand = getCommand("cambiardia");
        PluginCommand dayCommand = getCommand("dia");

        if (changeDayCommand != null) {
            changeDayCommand.setExecutor(new DayCommandHandler(dayHandler));  // Usa DayHandler correctamente
        }
        if (dayCommand != null) {
            dayCommand.setExecutor(new DayCommandHandler(dayHandler));  // Usa DayHandler correctamente
        }

        // Cargar datos de DeathStorm al iniciar
        deathStormHandler.loadStormData();

        // Registrar eventos de chat
        getServer().getPluginManager().registerEvents(new chatgeneral(), this);

        // Registrar el comando /ping
        this.getCommand("ping").setExecutor(new PingCommand(this));

        // Registrar eventos de list
        new VHList(this).runTaskTimer(this, 0, 20);

        // Registrar el manejador de tótems normales
        getServer().getPluginManager().registerEvents(new NormalTotemHandler(this), this);

        // Inicializa los cambios del día 1
        dayOneChanges = new DayOneChanges(this);
        dayOneChanges.apply(); // Aplica los cambios del día 1
        new DayOneChanges(this).registerCustomRecipe(); // Registrar receta personalizada
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', Prefix + "&aha sido deshabilitado!, &eVersion: " + Version));

        // Guardar datos de DeathStorm al deshabilitar
        if (deathStormHandler != null) {
            deathStormHandler.saveStormData();
        } else {
            Bukkit.getLogger().severe("deathStormHandler is null, cannot save storm data.");
        }
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

    // Método para acceder a DayHandler
    public DayHandler getDayHandler() {
        return dayHandler;  // Retorna la instancia correctamente inicializada
    }


}
