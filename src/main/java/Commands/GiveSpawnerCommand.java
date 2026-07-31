package Commands;

import Managers.MobManager;
import org.bukkit.Bukkit;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class GiveSpawnerCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final NamespacedKey spawnerKey;
    private final MobManager mobManager;

    public GiveSpawnerCommand(JavaPlugin plugin, MobManager mobManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        this.spawnerKey = new NamespacedKey(plugin, "custom_spawner");
        plugin.getCommand("givespawner").setExecutor(this);
        plugin.getCommand("givespawner").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /givespawner <mob> [jugador]");
            sender.sendMessage(ChatColor.RED + "Uso: /givespawner vanilla <mob_vanilla> [jugador]");
            return true;
        }

        boolean isVanilla = args[0].equalsIgnoreCase("vanilla");
        String mobType;
        Player target = null;

        if (isVanilla) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Uso: /givespawner vanilla <mob_vanilla> [jugador]");
                sender.sendMessage(ChatColor.YELLOW + "Mobs vanilla disponibles: zombie, skeleton, creeper, spider, etc.");
                return true;
            }
            mobType = "vanilla_" + args[1].toLowerCase();

            try {
                EntityType.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Mob vanilla no válido: " + args[1]);
                return true;
            }

            if (args.length > 2) {
                target = Bukkit.getPlayer(args[2]);
            }
        } else {
            mobType = args[0].toLowerCase();

            // Validamos dinámicamente si el mob existe en tu MobManager
            if (!mobManager.getRegisteredMobs().contains(mobType)) {
                sender.sendMessage(ChatColor.RED + "Mob custom no reconocido: " + mobType);
                return true;
            }

            if (args.length > 1) {
                target = Bukkit.getPlayer(args[1]);
            }
        }

        if (target == null && sender instanceof Player) {
            target = (Player) sender;
        } else if (target == null) {
            sender.sendMessage(ChatColor.RED + "El jugador no está en línea o debes especificar uno desde la consola.");
            return true;
        }

        ItemStack spawner = createCustomSpawner(mobType);
        if (spawner == null) {
            sender.sendMessage(ChatColor.RED + "Hubo un error al generar el spawner.");
            return true;
        }

        target.getInventory().addItem(spawner);

        String displayName = isVanilla ? args[1] : mobType;
        sender.sendMessage(ChatColor.GREEN + "Has dado un spawner de " + displayName + " a " + target.getName() + ".");
        target.sendMessage(ChatColor.GREEN + "Has recibido un spawner de " + displayName + "!");

        return true;
    }

    private ItemStack createCustomSpawner(String mobType) {
        ItemStack spawner = new ItemStack(Material.SPAWNER);
        ItemMeta meta = spawner.getItemMeta();
        if (meta == null) return null;

        String displayName;
        String description;
        int customModelData = 1000; // Por defecto

        if (mobType.startsWith("vanilla_")) {
            String vanillaType = mobType.substring(8);
            try {
                EntityType entityType = EntityType.valueOf(vanillaType.toUpperCase());
                displayName = ChatColor.GRAY + "" + ChatColor.BOLD + "Spawner de " + formatEntityName(entityType);
                description = "Genera " + formatEntityName(entityType) + " vanilla";
                customModelData = 2000 + entityType.ordinal();
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else {
            // MAGIA: Generación automática de nombres usando el nombre del string
            String formattedName = formatEntityName(mobType);
            displayName = ChatColor.GOLD + "" + ChatColor.BOLD + "Spawner de " + formattedName;
            description = "Genera " + formattedName;
        }

        meta.setDisplayName(displayName);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + description);
        lore.add("");
        if (mobType.startsWith("vanilla_")) {
            lore.add(ChatColor.DARK_GRAY + "Tipo: " + ChatColor.WHITE + "Vanilla");
            lore.add(ChatColor.DARK_GRAY + "Mob: " + ChatColor.WHITE + mobType.substring(8));
        } else {
            lore.add(ChatColor.DARK_GRAY + "Tipo: " + ChatColor.WHITE + "Custom");
            lore.add(ChatColor.DARK_GRAY + "Mob: " + ChatColor.WHITE + mobType);
        }
        lore.add("");
        lore.add(ChatColor.GRAY + "Spawn Count: " + ChatColor.WHITE + "4");
        lore.add(ChatColor.GRAY + "Max Nearby: " + ChatColor.WHITE + "6");
        lore.add(ChatColor.GRAY + "Player Range: " + ChatColor.WHITE + "20");
        lore.add(ChatColor.GRAY + "Initial Delay: " + ChatColor.WHITE + "40");
        lore.add(ChatColor.GRAY + "Min Delay: " + ChatColor.WHITE + "200");
        lore.add(ChatColor.GRAY + "Max Delay: " + ChatColor.WHITE + "600");
        lore.add(ChatColor.GRAY + "Spawn Range: " + ChatColor.WHITE + "4");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Shift + Click derecho para configurar");

        meta.setLore(lore);
        meta.setCustomModelData(customModelData);
        meta.getPersistentDataContainer().set(spawnerKey, PersistentDataType.STRING, mobType);

        spawner.setItemMeta(meta);
        return spawner;
    }

    private String formatEntityName(EntityType entityType) {
        String name = entityType.name().toLowerCase().replace('_', ' ');
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    // Método sobrecargado para formatear strings genéricos
    private String formatEntityName(String name) {
        if (name == null || name.isEmpty()) return name;
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("vanilla");
            // Lee todos los mobs directamente del Manager
            suggestions.addAll(mobManager.getRegisteredMobs());
            suggestions.removeIf(s -> !s.toLowerCase().startsWith(args[0].toLowerCase()));

        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("vanilla")) {
                for (EntityType type : EntityType.values()) {
                    if (type.isSpawnable() && type.isAlive() && type.name().toLowerCase().startsWith(args[1].toLowerCase())) {
                        suggestions.add(type.name().toLowerCase());
                    }
                }
            } else {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        suggestions.add(player.getName());
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("vanilla")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                    suggestions.add(player.getName());
                }
            }
        }

        return suggestions;
    }
}