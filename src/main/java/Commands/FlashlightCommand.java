package Commands;

import items.Flashlight.FlashlightManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class FlashlightCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final FlashlightManager flashlightManager;

    public FlashlightCommand(JavaPlugin plugin, FlashlightManager flashlightManager) {
        this.plugin = plugin;
        this.flashlightManager = flashlightManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("viciont_hardcore3.flashlight.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Uso: /flashlight reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            flashlightManager.getConfig().reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Configuración de Flashlight recargada correctamente.");

            // Mostrar configuración actual
            sender.sendMessage(ChatColor.GRAY + "Configuración actual:");
            sender.sendMessage(ChatColor.GRAY + "- Brightness: " + ChatColor.WHITE + flashlightManager.getConfig().getBrightness());
            sender.sendMessage(ChatColor.GRAY + "- Degree: " + ChatColor.WHITE + flashlightManager.getConfig().getDegree());
            sender.sendMessage(ChatColor.GRAY + "- Depth: " + ChatColor.WHITE + flashlightManager.getConfig().getDepth());

            return true;
        }

        sender.sendMessage(ChatColor.RED + "Subcomando no reconocido. Usa: /flashlight reload");
        return true;
    }
}