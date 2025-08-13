package Commands;

import Dificultades.CustomMobs.*;
import Handlers.DayHandler;
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

    public GiveSpawnerCommand(JavaPlugin plugin) {
        this.plugin = plugin;
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
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "El jugador '" + args[2] + "' no está en línea.");
                    return true;
                }
            }
        } else {
            mobType = args[0].toLowerCase();

            if (args.length > 1) {
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "El jugador '" + args[1] + "' no está en línea.");
                    return true;
                }
            }
        }

        if (target == null && sender instanceof Player) {
            target = (Player) sender;
        } else if (target == null) {
            sender.sendMessage(ChatColor.RED + "Debes especificar un jugador si ejecutas el comando desde la consola.");
            return true;
        }

        ItemStack spawner = createCustomSpawner(mobType);
        if (spawner == null) {
            if (isVanilla) {
                sender.sendMessage(ChatColor.RED + "Mob vanilla no reconocido.");
            } else {
                sender.sendMessage(ChatColor.RED + "Mob custom no reconocido. Usa /givespawner para ver la lista de mobs disponibles.");
            }
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
        int customModelData;

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
            // Inicializar con valores por defecto
            displayName = ChatColor.GRAY + "" + ChatColor.BOLD + "Spawner Custom";
            description = "Genera un mob personalizado";
            customModelData = 1000;

            switch (mobType) {
                case "bombita":
                    displayName = ChatColor.RED + "" + ChatColor.BOLD + "Spawner de Bombita";
                    description = "Genera Bombitas explosivas";
                    customModelData = 1001;
                    break;
                case "iceologer":
                    displayName = ChatColor.AQUA + "" + ChatColor.BOLD + "Spawner de Iceologer";
                    description = "Genera Iceologers con ataques de hielo";
                    customModelData = 1002;
                    break;
                case "corruptedzombie":
                    displayName = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Spawner de Corrupted Zombie";
                    description = "Genera Zombies Corruptos";
                    customModelData = 1003;
                    break;
                case "corruptedspider":
                    displayName = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Spawner de Corrupted Spider";
                    description = "Genera Arañas Corruptas";
                    customModelData = 1004;
                    break;
                case "queenbee":
                    displayName = ChatColor.GOLD + "" + ChatColor.BOLD + "Spawner de Queen Bee";
                    description = "Genera la poderosa Abeja Reina";
                    customModelData = 1005;
                    break;
                case "hellishbee":
                    displayName = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Spawner de Hellish Bee";
                    description = "Genera Abejas Infernales";
                    customModelData = 1006;
                    break;
                case "infestedbee":
                    displayName = ChatColor.of("#00a8a8") + "" + ChatColor.BOLD + "Spawner de Infested Bee";
                    description = "Genera Abejas Infestadas";
                    customModelData = 1007;
                    break;
                case "guardianblaze":
                    displayName = ChatColor.GOLD + "" + ChatColor.BOLD + "Spawner de Guardian Blaze";
                    description = "Genera Guardian Blazes";
                    customModelData = 1008;
                    break;
                case "guardiancorruptedskeleton":
                    displayName = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Spawner de Guardian Corrupted Skeleton";
                    description = "Genera Esqueletos Guardianes Corruptos";
                    customModelData = 1009;
                    break;
                case "corruptedskeleton":
                    displayName = ChatColor.GRAY + "" + ChatColor.BOLD + "Spawner de Corrupted Skeleton";
                    description = "Genera Esqueletos Corruptos de colores";
                    customModelData = 1010;
                    break;
                case "corruptedinfernalspider":
                    displayName = ChatColor.RED + "" + ChatColor.BOLD + "Spawner de Corrupted Infernal Spider";
                    description = "Genera Arañas Infernales Corruptas";
                    customModelData = 1011;
                    break;
                case "corruptedcreeper":
                    displayName = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Spawner de Corrupted Creeper";
                    description = "Genera Creepers Corruptos";
                    customModelData = 1012;
                    break;
                case "corruptedmagma":
                    displayName = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Spawner de Corrupted Magma Cube";
                    description = "Genera Cubos de Magma Corruptos";
                    customModelData = 1013;
                    break;
                case "piglinglobo":
                    displayName = ChatColor.YELLOW + "" + ChatColor.BOLD + "Spawner de Piglin Globo";
                    description = "Genera Piglins Globo";
                    customModelData = 1014;
                    break;
                case "buffbreeze":
                    displayName = ChatColor.AQUA + "" + ChatColor.BOLD + "Spawner de Buff Breeze";
                    description = "Genera Breezes Mejorados";
                    customModelData = 1015;
                    break;
                case "invertedghast":
                    displayName = ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Spawner de Inverted Ghast";
                    description = "Genera Ghasts Invertidos";
                    customModelData = 1016;
                    break;
                case "netheritevexguardian":
                    displayName = ChatColor.of("#B87333") + "" + ChatColor.BOLD + "Spawner de Netherite Vex Guardian";
                    description = "Genera Vex Guardianes de Netherite";
                    customModelData = 1017;
                    break;
                case "ultrawitherboss":
                    displayName = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Spawner de Ultra Wither Boss";
                    description = "Genera el Ultra Wither Boss";
                    customModelData = 1018;
                    break;
                case "whiteenderman":
                    displayName = ChatColor.WHITE + "" + ChatColor.BOLD + "Spawner de White Enderman";
                    description = "Genera Endermans Blancos";
                    customModelData = 1019;
                    break;
                case "infernalcreeper":
                    displayName = ChatColor.RED + "" + ChatColor.BOLD + "Spawner de Infernal Creeper";
                    description = "Genera Creepers Infernales";
                    customModelData = 1020;
                    break;
                case "ultracorruptedspider":
                    displayName = ChatColor.GREEN + "" + ChatColor.BOLD + "Spawner de Ultra Corrupted Spider";
                    description = "Genera Arañas Ultra Corruptas";
                    customModelData = 1021;
                    break;
                case "fastravager":
                    displayName = ChatColor.GOLD + "" + ChatColor.BOLD + "Spawner de Fast Ravager";
                    description = "Genera Ravagers Rápidos";
                    customModelData = 1022;
                    break;
                case "bruteimperial":
                    displayName = ChatColor.YELLOW + "" + ChatColor.BOLD + "Spawner de Brute Imperial";
                    description = "Genera Brutes Imperiales";
                    customModelData = 1023;
                    break;
                case "batboom":
                    displayName = ChatColor.RED + "" + ChatColor.BOLD + "Spawner de Bat Boom";
                    description = "Genera Murciélagos Explosivos";
                    customModelData = 1024;
                    break;
                case "spectraleeye":
                    displayName = ChatColor.GREEN + "" + ChatColor.BOLD + "Spawner de Spectral Eye";
                    description = "Genera Ojos Espectrales";
                    customModelData = 1025;
                    break;
                case "enderghast":
                    displayName = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Spawner de Ender Ghast";
                    description = "Genera Ghasts del End";
                    customModelData = 1026;
                    break;
                case "endercreeper":
                    displayName = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Spawner de Ender Creeper";
                    description = "Genera Creepers del End";
                    customModelData = 1027;
                    break;
                case "endersilverfish":
                    displayName = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Spawner de Ender Silverfish";
                    description = "Genera Silverfish del End";
                    customModelData = 1028;
                    break;
                case "guardianshulker":
                    displayName = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Spawner de Guardian Shulker";
                    description = "Genera Shulkers Guardianes";
                    customModelData = 1029;
                    break;
                case "darkphantom":
                    displayName = ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Spawner de Dark Phantom";
                    description = "Genera Phantoms Oscuros";
                    customModelData = 1030;
                    break;
                case "darkcreeper":
                    displayName = ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Spawner de Dark Creeper";
                    description = "Genera Creepers Oscuros";
                    customModelData = 1031;
                    break;
                case "darkvex":
                    displayName = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Spawner de Dark Vex";
                    description = "Genera Vex Oscuros";
                    customModelData = 1032;
                    break;
                case "darkskeleton":
                    displayName = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Spawner de Dark Skeleton";
                    description = "Genera Esqueletos Oscuros";
                    customModelData = 1033;
                    break;
                default:
                    return null;
            }
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("vanilla");
            suggestions.add("bombita");
            suggestions.add("iceologer");
            suggestions.add("corruptedzombie");
            suggestions.add("corruptedspider");
            suggestions.add("queenbee");
            suggestions.add("hellishbee");
            suggestions.add("infestedbee");
            suggestions.add("guardianblaze");
            suggestions.add("guardiancorruptedskeleton");
            suggestions.add("corruptedskeleton");
            suggestions.add("corruptedinfernalspider");
            suggestions.add("corruptedcreeper");
            suggestions.add("corruptedmagma");
            suggestions.add("piglinglobo");
            suggestions.add("buffbreeze");
            suggestions.add("invertedghast");
            suggestions.add("netheritevexguardian");
            suggestions.add("ultrawitherboss");
            suggestions.add("whiteenderman");
            suggestions.add("infernalcreeper");
            suggestions.add("ultracorruptedspider");
            suggestions.add("fastravager");
            suggestions.add("bruteimperial");
            suggestions.add("batboom");
            suggestions.add("spectraleeye");
            suggestions.add("enderghast");
            suggestions.add("endercreeper");
            suggestions.add("endersilverfish");
            suggestions.add("guardianshulker");
            suggestions.add("darkphantom");
            suggestions.add("darkcreeper");
            suggestions.add("darkvex");
            suggestions.add("darkskeleton");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("vanilla")) {
                for (EntityType type : EntityType.values()) {
                    if (type.isSpawnable() && type.isAlive()) {
                        suggestions.add(type.name().toLowerCase());
                    }
                }
            } else {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    suggestions.add(player.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("vanilla")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                suggestions.add(player.getName());
            }
        }

        return suggestions;
    }
}