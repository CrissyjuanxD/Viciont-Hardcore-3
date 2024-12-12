package vct.hardcore3;

import Commands.PingCommand;
import Enchants.*;
/*import Estructures.CorruptedVillage;*/
import Estructures.CorruptedVillage;
import Events.Skybattle.EventoHandler;
import TitleListener.*;
import list.VHList;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
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
import org.bukkit.command.Command;

import org.bukkit.entity.Player;

public class ViciontHardcore3 extends JavaPlugin implements Listener {

    public String Prefix = "&d&lViciont&5&lHardcore &5&l3&7➤ &f";
    public String Version = getDescription().getVersion();
    private DeathStormHandler deathStormHandler;
    private DayHandler dayHandler;
    private RuletaAnimation ruletaAnimation;
    private MuerteAnimation muerteAnimation;
    private BonusAnimation bonusAnimation;
    private DayOneChanges dayOneChanges;
    private DoubleLifeTotemHandler doubleLifeTotemHandler;
    private NormalTotemHandler normalTotemHandler;

    //Eventos
    private EventoHandler eventoHandler;

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
        getServer().getPluginManager().registerEvents(new MuerteHandler(this), this);

        // DeathStorm
        deathStormHandler = new DeathStormHandler(this, dayHandler);
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
            changeDayCommand.setExecutor(new DayCommandHandler(dayHandler));
        }
        if (dayCommand != null) {
            dayCommand.setExecutor(new DayCommandHandler(dayHandler));
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

        // Inicializar RuletaAnimation, TP, Disco y MuerteAnimation
        ruletaAnimation = new RuletaAnimation(this);
        muerteAnimation = new MuerteAnimation(this);
        bonusAnimation = new BonusAnimation(this);
        DiscoCommand discoCommand = new DiscoCommand(this);
        this.getCommand("magic").setExecutor(new MagicTP(this));
        this.getCommand("playdisco").setExecutor(discoCommand);
        this.getCommand("stopdisco").setExecutor(discoCommand);
        getServer().getPluginManager().registerEvents(discoCommand, this);

        // Registrar el comando y su ejecutor
        getCommand("ruletavct").setExecutor(new RuletaCommand(ruletaAnimation));
        getCommand("muertevct").setExecutor(new MuerteCommand(muerteAnimation));
        getCommand("bonusvct").setExecutor(new BonusCommand(bonusAnimation));

        //Registrar CorruptedVillage
        getServer().getPluginManager().registerEvents(new CorruptedVillage(this), this);

        //Loottables con Esencias
        getServer().getPluginManager().registerEvents(new LootHandler(this), this);

        // Inicializa los cambios del día 1
        dayOneChanges = new DayOneChanges(this);
        new DayOneChanges(this).registerCustomRecipe(); // Registrar receta personalizada
        applySnowballRunnableToLoadedCorruptedZombies();

        this.getCommand("testanim1").setExecutor(this);

        // Inicializar EventoHandler
                this.eventoHandler = new EventoHandler(this);
                getServer().getPluginManager().registerEvents(eventoHandler, this);

                this.getCommand("start").setExecutor((sender, command, label, args) -> {
                    if (args.length == 1) {
                        switch (args[0].toLowerCase()) {
                            case "evento1":
                                eventoHandler.iniciarEvento();
                                break;
                            case "skybattle":
                                eventoHandler.iniciarSecuenciaInicioSkyBattle();
                                break;
                            case "force":
                                eventoHandler.forzarEvento();
                                break;
                            default:
                                sender.sendMessage("Uso incorrecto del comando. Usa /start <evento1|skybattle|force>");
                        }
                    } else {
                        sender.sendMessage("Uso incorrecto del comando. Usa /start <evento1|skybattle|force>");
                    }
                    return true;
                });

                this.getCommand("end").setExecutor((sender, command, label, args) -> {
                    if (args.length == 1 && args[0].equalsIgnoreCase("evento1")) {
                        eventoHandler.terminarEvento();
                    } else {
                        sender.sendMessage("Uso incorrecto del comando. Usa /end <evento1>");
                    }
                    return true;
                });
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
        String message = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "۞ " + ChatColor.RESET + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + event.getPlayer().getName() + ChatColor.RESET + ChatColor.LIGHT_PURPLE + " ha entrado a" + ChatColor.RESET + ChatColor.GOLD + ChatColor.BOLD + " Viciont Hardcore 3";
        event.setJoinMessage(message);
    }

    //Formateo del chat
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String message = ChatColor.GRAY + "" + ChatColor.BOLD + "۞ " + ChatColor.RESET + ChatColor.GRAY + ChatColor.BOLD + event.getPlayer().getName() + ChatColor.RESET + ChatColor.GRAY + " ha salido de" + ChatColor.RESET + ChatColor.GOLD + ChatColor.BOLD + " Viciont Hardcore 3";
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

                    if (zombie.getCustomName() != null && zombie.getCustomName().equals(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Corrupted Zombie")) {
                        dayOneChanges.startSnowballRunnable(zombie);
                    }
                }
            }
        }
    }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (command.getName().equalsIgnoreCase("testanim1")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    playDeathAnimation(player);
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
                    return false;
                }
            }
            return false;
        }

        public void playDeathAnimation(Player player) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as " + player.getName() + " run title @a title {\"text\":\"\\uE851\", \"color\":\"red\"}");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as " + player.getName() + " run playsound minecraft:entity.blaze.death master @a ~ ~ ~ 1 1 1");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as " + player.getName() + " run particle minecraft:smoke ~ ~ ~ 1 1 1 0.1 10");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a {\"text\":\"Animación de muerte activada\"}");
        }
}

