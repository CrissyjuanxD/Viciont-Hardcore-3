package mobcap.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MobCapTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("mobcapinfo")) {
            return completions;
        }

        if (!command.getName().equalsIgnoreCase("mobcap")) {
            return completions;
        }

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("info", "setbase", "doble", "triple", "normal",
                    "habilitar", "deshabilitar", "reset", "reload", "mob");

            for (String subcommand : subcommands) {
                if (subcommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    if (subcommand.equals("info") || sender.hasPermission("mobcap.admin")) {
                        completions.add(subcommand);
                    }
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("setbase") && sender.hasPermission("mobcap.admin")) {
                completions.addAll(Arrays.asList("70", "140", "200", "280", "350"));
            } else if (args[0].equalsIgnoreCase("mob") && sender.hasPermission("mobcap.admin")) {
                // Agregar tipos de mobs hostiles
                completions.addAll(getHostileMobTypes());
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("mob") && sender.hasPermission("mobcap.admin")) {
                completions.addAll(Arrays.asList("vanilla", "custom"));
            }
        }

        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getHostileMobTypes() {
        return Arrays.asList(
                "zombie", "skeleton", "creeper", "spider", "enderman", "witch",
                "slime", "magma_cube", "ghast", "blaze", "wither_skeleton",
                "zombified_piglin", "piglin", "piglin_brute", "hoglin", "zoglin",
                "drowned", "husk", "stray", "phantom", "shulker", "guardian",
                "elder_guardian", "silverfish", "endermite", "vex", "vindicator",
                "pillager", "ravager", "evoker", "cave_spider"
        );
    }
}