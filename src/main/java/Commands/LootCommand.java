package Commands;

import Handlers.LootManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LootCommand implements CommandExecutor {

    private final LootManager lootManager;

    public LootCommand(LootManager lootManager) {
        this.lootManager = lootManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUso: /LootTableVC <nombre_tabla>");
            return true;
        }

        Player player = (Player) sender;
        String tableName = args[0];

        Block targetBlock = player.getLocation().getBlock();

        targetBlock.setType(Material.CHEST);

        boolean exito = lootManager.setLootBlock(targetBlock, tableName);

        if (exito) {
            player.sendMessage("§aCofre generado con LootTable: §e" + tableName);
        } else {
            player.sendMessage("§cError al asignar la data al bloque.");
        }

        return true;
    }
}