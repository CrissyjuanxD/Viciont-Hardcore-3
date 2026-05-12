package Structures;

import TitleListener.RuletaAnimation;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class StructureCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final Map<String, Structure> structures = new HashMap<>();
    private final RuletaAnimation ruletaAnimation;

    public StructureCommand(JavaPlugin plugin, RuletaAnimation ruletaAnimation) {
        this.plugin = plugin;
        this.ruletaAnimation = ruletaAnimation;
        plugin.getCommand("structure").setExecutor(this);

        // Registrar estructuras
        registerStructure(new CorruptedVillage(plugin, ruletaAnimation));
        registerStructure(new EndRing(plugin, ruletaAnimation));
        registerStructure(new GuardianBlazeZone(plugin, ruletaAnimation));
    }

    private void registerStructure(Structure structure) {
        structures.put(structure.getName().toLowerCase(), structure);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("structure") || args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /structure <nombre>");
            sender.sendMessage(ChatColor.GRAY + "Estructuras disponibles: " + String.join(", ", structures.keySet()));
            return false;
        }

        Structure structure = structures.get(args[0].toLowerCase());
        if (structure == null) {
            sender.sendMessage(ChatColor.RED + "Estructura no válida. Opciones: " + String.join(", ", structures.keySet()));
            return false;
        }

        structure.generate(sender);
        return true;
    }
}