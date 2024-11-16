package vct.hardcore3;

import Commands.PingCommand;
import Enchants.EnchantDelete;
import Enchants.EnhancedEnchantmentGUI;
import Enchants.EnhancedEnchantmentTable;
import Enchants.GiveEssenceCommand;
import TitleListener.*;
import list.VHList;
import org.bukkit.*;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;
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
    private RuletaAnimation ruletaAnimation;
    private MuerteAnimation muerteAnimation;
    private DayOneChanges dayOneChanges;
    private DoubleLifeTotemHandler doubleLifeTotemHandler;
    private NormalTotemHandler normalTotemHandler;

    @Override
    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', Prefix + "&aha sido habilitado!, &eVersion: " + Version));

        // Registra los eventos de esta clase
        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        // Registra evento de revivir
        Objects.requireNonNull(this.getCommand("revive")).setExecutor(new ReviveCommand(this));

        // Inicializa los manejadores de eventos de totems
        doubleLifeTotemHandler = new DoubleLifeTotemHandler(this);
        normalTotemHandler = new NormalTotemHandler(this);

        // Registra los eventos de totems
        getServer().getPluginManager().registerEvents(doubleLifeTotemHandler, this);
        getServer().getPluginManager().registerEvents(normalTotemHandler, this);

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
        Objects.requireNonNull(this.getCommand("ping")).setExecutor(new PingCommand(this));

        // Registrar eventos de list
        new VHList(this).runTaskTimer(this, 0, 20);

        // Registrar el comando "giveessence"
        this.getCommand("giveessence").setExecutor(new GiveEssenceCommand());

        // Registra la clase EnhancedEnchantmentTable para crear los ítems y recetas
        new EnhancedEnchantmentTable(this);
        // Registra la GUI personalizada
        getServer().getPluginManager().registerEvents(new EnhancedEnchantmentGUI(this), this);

        // Registro de EnchantDelete
        getServer().getPluginManager().registerEvents(new EnchantDelete(this), this);

        //Inicializa el manejador de muerte
        new MuerteHandler(this);

        // Inicializar RuletaAnimation y MuerteAnimation
        ruletaAnimation = new RuletaAnimation(this);
        muerteAnimation = new MuerteAnimation(this);

        // Registrar el comando y su ejecutor
        getCommand("ruletavct").setExecutor(new RuletaCommand(ruletaAnimation));
        getCommand("muertevct").setExecutor(new MuerteCommand(muerteAnimation));

        // Inicializa los cambios del día 1
        dayOneChanges = new DayOneChanges(this);
        new DayOneChanges(this).registerCustomRecipe(); // Registrar receta personalizada
        applySnowballRunnableToLoadedCorruptedZombies();

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
        String message = ChatColor.WHITE + "" + ChatColor.BOLD + "\uE003 " + ChatColor.RESET + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + event.getPlayer().getName() + ChatColor.RESET + ChatColor.LIGHT_PURPLE + " ha entrado a" + ChatColor.RESET + ChatColor.GOLD + ChatColor.BOLD + " Viciont Hardcore 3";
        event.setJoinMessage(message);
    }

    //Formateo del chat
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String message = ChatColor.WHITE + "" + ChatColor.BOLD + "\uE004 " + ChatColor.RESET + ChatColor.GRAY + ChatColor.BOLD + event.getPlayer().getName() + ChatColor.RESET + ChatColor.GRAY + " ha salido de" + ChatColor.RESET + ChatColor.GOLD + ChatColor.BOLD + " Viciont Hardcore 3";
        event.setQuitMessage(message);
    }


    // Método para acceder a DayHandler
    public DayHandler getDayHandler() {
        return dayHandler;  // Retorna la instancia correctamente inicializada
    }

    public DoubleLifeTotemHandler getDoubleLifeTotemHandler() {
        return doubleLifeTotemHandler;
    }

    public void applySnowballRunnableToLoadedCorruptedZombies() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Zombie zombie) {
                    // Verifica si es un Corrupted Zombie revisando su nombre o usando PersistentDataContainer
                    if (zombie.getCustomName() != null && zombie.getCustomName().equals(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Zombie")) {
                        dayOneChanges.startSnowballRunnable(zombie); // Inicia el runnable de bolas de nieve para este zombie
                    }
                }
            }
        }
    }
}

