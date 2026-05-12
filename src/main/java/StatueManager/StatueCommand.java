package StatueManager;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class StatueCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (!p.hasPermission("viciont.admin")) {
            p.sendMessage(ChatColor.RED + "No tienes permisos para ejecutar este comando.");
            return true;
        }

        ItemStack item = new ItemStack(Material.ARMOR_STAND);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "Statue Effect");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Shift + Click Der para configurar.");
        lore.add(ChatColor.GRAY + "Click Der en suelo para colocar.");
        meta.setLore(lore);

        StatueData data = new StatueData(meta);
        data.setDefaults();

        item.setItemMeta(meta);

        p.getInventory().addItem(item);
        p.sendMessage(ChatColor.GREEN + "Has recibido la Estatua de Efectos.");

        return true;
    }
}