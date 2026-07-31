package StatueManager;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StatueCommand implements CommandExecutor, TabCompleter {

    private final StatueDebugManager debugManager;

    private static final List<String> SUBCOMMANDS = Arrays.asList("give", "debug", "clone");

    public StatueCommand(StatueDebugManager debugManager) {
        this.debugManager = debugManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Solo los jugadores pueden usar este comando.");
            return true;
        }

        Player p = (Player) sender;

        if (!p.hasPermission("viciont.admin")) {
            p.sendMessage(ChatColor.RED + "No tienes permisos para ejecutar este comando.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(p);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                giveStatue(p);
                break;
            case "debug":
                debugManager.toggleDebug(p);
                break;
            case "clone":
                cloneStatue(p);
                break;
            default:
                sendHelp(p);
                break;
        }

        return true;
    }

    private void giveStatue(Player p) {
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
    }

    private void cloneStatue(Player p) {
        // Aprovechamos el ray trace del debug manager
        ArmorStand target = debugManager.getTargetStatue(p);
        if (target == null) {
            p.sendMessage(ChatColor.RED + "Debes mirar a una estatua (máx. 10 bloques) para clonarla.");
            return;
        }

        StatueData targetData = new StatueData(target);

        ItemStack item = new ItemStack(Material.ARMOR_STAND);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Statue Effect (Clon)");

        // Configurar los datos internos en la meta del item nuevo
        StatueData cloneData = new StatueData(meta);
        cloneData.setRadiusX(targetData.getRadiusX());
        cloneData.setRadiusY(targetData.getRadiusY());
        cloneData.setHpMax(targetData.getHpMax());
        cloneData.setHpCurrent(targetData.getHpMax()); // Las estatuas nuevas nacen con HP al máximo
        cloneData.setGlowColor(targetData.getGlowColor());
        cloneData.setVisible(targetData.isVisible());
        cloneData.setInvulnerable(targetData.isInvulnerable());

        if (targetData.isAntiGrief()) {
            cloneData.setAntiGrief(true);
        } else {
            cloneData.setEffect(targetData.getEffectType(), targetData.getEffectAmplifier());
        }

        // Aplicar el identificador estricto para que el plugin sepa que es una estatua
        meta.getPersistentDataContainer().set(
                new NamespacedKey("viciont", "statue_id"),
                PersistentDataType.STRING,
                "statue_effect"
        );

        // Crear el lore visual dinámicamente basándonos en los datos copiados
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Radio: " + ChatColor.WHITE + cloneData.getRadiusX() + "x" + cloneData.getRadiusY());
        lore.add(ChatColor.GRAY + "Vida: " + ChatColor.WHITE + (cloneData.isInvulnerable() ? "INFINITA" : cloneData.getHpMax()));

        if (cloneData.isAntiGrief()) {
            lore.add(ChatColor.GRAY + "Modo: " + ChatColor.AQUA + "ANTI-GRIEF ZONA");
        } else {
            String eff = cloneData.getEffectType() != null ? cloneData.getEffectType().getName() + " " + (cloneData.getEffectAmplifier() + 1) : "N/A";
            lore.add(ChatColor.GRAY + "Modo: " + ChatColor.AQUA + eff);
        }

        String color = (cloneData.getGlowColor() == null) ? "OFF" : (cloneData.getGlowColor() + cloneData.getGlowColor().name());
        lore.add(ChatColor.GRAY + "Color: " + color);

        if (cloneData.isInvulnerable()) lore.add(ChatColor.GOLD + ">> INDESTRUCTIBLE <<");
        if (!cloneData.isVisible()) lore.add(ChatColor.DARK_GRAY + ">> INVISIBLE <<");
        lore.add("");
        lore.add(ChatColor.GRAY + "Shift + Click Der para configurar.");
        lore.add(ChatColor.GRAY + "Click Der en suelo para colocar.");

        meta.setLore(lore);
        item.setItemMeta(meta);

        p.getInventory().addItem(item);
        p.sendMessage(ChatColor.GREEN + "Has clonado la Estatua exitosamente.");
        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 2f);
    }

    private void sendHelp(Player p) {
        p.sendMessage(ChatColor.DARK_PURPLE + "━━━━━━━━━━━━━━━━━━━━━━━");
        p.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "  Comandos de Estatuas");
        p.sendMessage(ChatColor.DARK_PURPLE + "━━━━━━━━━━━━━━━━━━━━━━━");
        p.sendMessage(ChatColor.LIGHT_PURPLE + "/statue give " + ChatColor.GRAY + "— Recibe una estatua por defecto.");
        p.sendMessage(ChatColor.LIGHT_PURPLE + "/statue clone " + ChatColor.GRAY + "— Clona la estatua que estés mirando.");
        p.sendMessage(ChatColor.LIGHT_PURPLE + "/statue debug " + ChatColor.GRAY + "— Visualiza información y rango de la estatua.");
        p.sendMessage(ChatColor.DARK_PURPLE + "━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (!sender.hasPermission("viciont.admin")) return suggestions;

        if (args.length == 1) {
            String typed = args[0].toLowerCase();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(typed)) suggestions.add(sub);
            }
        }

        return suggestions;
    }
}