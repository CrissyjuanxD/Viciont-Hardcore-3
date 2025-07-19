package Structures;

import org.bukkit.command.CommandSender;

public interface Structure {
    String getName();
    void generate(CommandSender sender);
}