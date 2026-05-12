package Commands;

import Handlers.LootManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LootCommand implements CommandExecutor, TabCompleter {

    private final LootManager lootManager;

    public LootCommand(LootManager lootManager) {
        this.lootManager = lootManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Comando: /LootTableVC <spawn/give> <jugador/@a> <nombre_tabla>

        // 1. Validaciones básicas
        if (args.length < 3) {
            sender.sendMessage("§cUso: /LootTableVC <spawn/give> <jugador/@a> <nombre_tabla>");
            return true;
        }

        String subCommand = args[0].toLowerCase(); // spawn o give
        String targetArg = args[1]; // jugador o @a
        String tableName = args[2].toLowerCase(); // nombre de la tabla

        // 2. Validar que la tabla exista
        if (!lootManager.isValidTable(tableName)) {
            sender.sendMessage("§cLa LootTable '" + tableName + "' no existe.");
            return true;
        }

        // 3. Obtener lista de jugadores afectados
        List<Player> targets = new ArrayList<>();
        if (targetArg.equals("@a")) {
            targets.addAll(Bukkit.getOnlinePlayers());
        } else {
            Player target = Bukkit.getPlayer(targetArg);
            if (target == null) {
                sender.sendMessage("§cJugador no encontrado: " + targetArg);
                return true;
            }
            targets.add(target);
        }

        // 4. Ejecutar acción según subcomando
        int count = 0;

        for (Player p : targets) {
            if (subCommand.equals("spawn")) {
                // SPAWN: Poner bloque en los pies del jugador
                Block targetBlock = p.getLocation().getBlock();
                targetBlock.setType(Material.CHEST);
                if (lootManager.setLootBlock(targetBlock, tableName)) {
                    p.sendMessage("§a¡Un cofre de loot ha aparecido en tus pies!");
                    count++;
                }

            } else if (subCommand.equals("give")) {
                // GIVE: Dar ítem de cofre al inventario
                p.getInventory().addItem(lootManager.getLootChestItem(tableName));
                p.sendMessage("§aHas recibido un cofre con LootTable: §e" + tableName);
                count++;

            } else {
                sender.sendMessage("§cSubcomando desconocido. Usa 'spawn' o 'give'.");
                return true;
            }
        }

        sender.sendMessage("§aComando ejecutado exitosamente para " + count + " jugador(es).");
        return true;
    }

    // --- AUTOCOMPLETADO (TAB COMPLETER) ---
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Primer argumento: Subcomandos
            List<String> subCommands = new ArrayList<>();
            subCommands.add("spawn");
            subCommands.add("give");
            StringUtil.copyPartialMatches(args[0], subCommands, completions);

        } else if (args.length == 2) {
            // Segundo argumento: Jugadores + @a
            List<String> playerNames = new ArrayList<>();
            playerNames.add("@a");
            for (Player p : Bukkit.getOnlinePlayers()) {
                playerNames.add(p.getName());
            }
            StringUtil.copyPartialMatches(args[1], playerNames, completions);

        } else if (args.length == 3) {
            // Tercer argumento: Lista de Loot Tables (desde LootManager)
            StringUtil.copyPartialMatches(args[2], lootManager.getLootTableNames(), completions);
        }

        Collections.sort(completions);
        return completions;
    }
}