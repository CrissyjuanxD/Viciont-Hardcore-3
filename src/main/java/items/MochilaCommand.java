package items;

import Handlers.DatabaseManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MochilaCommand implements CommandExecutor {

    private final EconomyItemsFunctions functions;

    public MochilaCommand(EconomyItemsFunctions functions) {
        this.functions = functions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ismanusmp.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso.");
            return true;
        }

        // --- /delmochilas <nombre> ---
        if (label.equalsIgnoreCase("delmochilas")) {
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Uso: /delmochilas <jugador>");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage("Solo jugadores.");
                return true;
            }
            // Iniciamos búsqueda en base de datos
            buscarJugadorYAbrirMenu((Player) sender, args[0], true);
            return true;
        }

        // --- /mochilas <nombre> [give] ---
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /mochilas <jugador> [give]");
            return true;
        }

        if (args.length > 1 && args[0].equalsIgnoreCase("give")) {
            Player targetOnline = Bukkit.getPlayer(args[1]);
            if (targetOnline == null) {
                sender.sendMessage(ChatColor.RED + "El jugador " + args[1] + " no está conectado.");
                return true;
            }
            if (sender instanceof Player) openGiveMenu((Player) sender, targetOnline);
            return true;
        }

        if (sender instanceof Player) {
            buscarJugadorYAbrirMenu((Player) sender, args[0], false);
        }

        return true;
    }

    // --- LÓGICA CENTRALIZADA DE BÚSQUEDA ---
    private void buscarJugadorYAbrirMenu(Player admin, String targetName, boolean isDeleteMode) {
        admin.sendMessage(ChatColor.YELLOW + "🔍 Buscando datos de " + targetName + " en la base de datos...");

        new BukkitRunnable() {
            @Override
            public void run() {
                UUID targetUUID = functions.getDbManager().getUuidByName(targetName);

                if (targetUUID == null) {
                    try {
                        targetUUID = Bukkit.getOfflinePlayer(targetName).getUniqueId();
                    } catch (Exception ignored) {}
                }

                if (targetUUID == null) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            admin.sendMessage(ChatColor.RED + "❌ Jugador no encontrado en la base de datos ni caché.");
                        }
                    }.runTask(functions.getPlugin());
                    return;
                }

                List<DatabaseManager.BackpackInfo> backpacks = functions.getDbManager().getPlayerBackpacks(targetUUID);
                final UUID finalUUID = targetUUID;

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (backpacks.isEmpty()) {
                            admin.sendMessage(ChatColor.RED + "⚠ El jugador " + targetName + " no tiene mochilas guardadas.");
                            return;
                        }

                        String titulo = isDeleteMode ?
                                ChatColor.RED + "BORRAR Mochilas de: " + targetName :
                                ChatColor.DARK_RED + "Mochilas de: " + targetName;

                        Inventory inv = Bukkit.createInventory(null, 54, titulo);
                        fillBackpackGui(inv, backpacks, isDeleteMode);
                        admin.openInventory(inv);
                        admin.sendMessage(ChatColor.GREEN + "✔ Datos cargados correctamente. (UUID: " + finalUUID.toString().substring(0,8) + "...)");
                    }
                }.runTask(functions.getPlugin());
            }
        }.runTaskAsynchronously(functions.getPlugin());
    }

    private void openGiveMenu(Player admin, Player target) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.DARK_PURPLE + "Dar Mochila a " + target.getName());
        inv.setItem(0, EconomyItems.createNormalMochila());
        inv.setItem(1, EconomyItems.createGreenMochila());
        inv.setItem(2, EconomyItems.createRedMochila());
        inv.setItem(3, EconomyItems.createBlueMochila());
        inv.setItem(4, EconomyItems.createPurpleMochila());
        admin.openInventory(inv);
    }

    private void fillBackpackGui(Inventory inv, List<DatabaseManager.BackpackInfo> backpacks, boolean isDeleteMode) {
        for (DatabaseManager.BackpackInfo info : backpacks) {
            ItemStack icon = functions.getBackpackItemByLevel(info.level);
            ItemMeta meta = icon.getItemMeta();

            meta.setDisplayName(info.itemName != null ? info.itemName : ChatColor.GOLD + "Mochila");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Nivel: " + ChatColor.AQUA + info.level);
            lore.add(ChatColor.GRAY + "Actualizada: " + ChatColor.AQUA + info.updatedAt);
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "ID: " + info.uuid);
            lore.add("");

            if (isDeleteMode) {
                lore.add(ChatColor.RED + "➤ Click Izquierdo: " + ChatColor.DARK_RED + "" + ChatColor.BOLD + "BORRAR PARA SIEMPRE");
            } else {
                lore.add(ChatColor.GREEN + "➤ Click Izquierdo: " + ChatColor.WHITE + "Recuperar (Copia)");
                lore.add(ChatColor.YELLOW + "➤ Click Derecho: " + ChatColor.WHITE + "Espiar / Editar Contenido");
            }

            meta.setLore(lore);
            icon.setItemMeta(meta);
            inv.addItem(icon);
        }
    }
}