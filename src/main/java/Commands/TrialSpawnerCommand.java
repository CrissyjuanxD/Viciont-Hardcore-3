package Commands;

import Handlers.TrialSpawnerHandler;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class TrialSpawnerCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final MobManager mobManager;

    // Valores por defecto al crear un spawner nuevo — alineados con TrialSpawnerHandler
    private static final int DEFAULT_TOTAL_MOBS      = 6;
    private static final int DEFAULT_SIM_MIN          = 2;   // fijo: min == max
    private static final int DEFAULT_SIM_MAX          = 2;
    private static final int DEFAULT_TICKS            = 40;
    private static final int DEFAULT_RANGE            = 4;
    private static final int DEFAULT_COOLDOWN         = 30;
    private static final int DEFAULT_PLAYER_RANGE     = 16;
    private static final int DEFAULT_BONUS_ENABLED    = 0;   // desactivado por defecto
    private static final int DEFAULT_BONUS_MAX        = 4;
    private static final int DEFAULT_LOOT_MAX         = 4;
    private static final int DEFAULT_LOOT_SPEC_MAX    = 2;

    /**
     * CustomModelData para el ítem en inventario.
     * En assets/minecraft/models/item/spawner.json añade:
     *   { "predicate": { "custom_model_data": 1001 }, "model": "minecraft:block/trial_spawner_custom" }
     */
    private static final int TRIAL_SPAWNER_CMD = 1001;

    public TrialSpawnerCommand(JavaPlugin plugin, MobManager mobManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        plugin.getCommand("trialspawner").setExecutor(this);
        plugin.getCommand("trialspawner").setTabCompleter(this);
    }

    // =========================================================================
    // COMMAND
    // =========================================================================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) { sendUsage(sender); return true; }

        boolean isVanilla = args[0].equalsIgnoreCase("vanilla");
        String mobType;
        Player target = null;

        if (isVanilla) {
            if (args.length < 2) { sendUsage(sender); return true; }
            try { EntityType.valueOf(args[1].toUpperCase()); }
            catch (IllegalArgumentException e) { sender.sendMessage(ChatColor.RED + "Mob vanilla no válido: " + args[1]); return true; }
            mobType = "vanilla_" + args[1].toLowerCase();
            if (args.length > 2) target = Bukkit.getPlayer(args[2]);
        } else {
            mobType = args[0].toLowerCase();
            if (!mobManager.getRegisteredMobs().contains(mobType)) {
                sender.sendMessage(ChatColor.RED + "Mob custom no reconocido: " + mobType); return true;
            }
            if (args.length > 1) target = Bukkit.getPlayer(args[1]);
        }

        if (target == null && sender instanceof Player) target = (Player) sender;
        else if (target == null) { sender.sendMessage(ChatColor.RED + "El jugador no está en línea."); return true; }

        ItemStack spawner = createTrialSpawnerItem(mobType);
        if (spawner == null) { sender.sendMessage(ChatColor.RED + "Error al crear el spawner."); return true; }

        target.getInventory().addItem(spawner);
        String displayName = TrialSpawnerHandler.formatEntityNameStatic(isVanilla ? args[1] : mobType);
        if (!sender.getName().equals(target.getName()))
            sender.sendMessage(ChatColor.GREEN + "Trial Spawner de " + ChatColor.WHITE + displayName + ChatColor.GREEN + " entregado a " + target.getName() + ".");
        target.sendMessage(ChatColor.GREEN + "¡Has recibido un Trial Spawner de " + ChatColor.WHITE + displayName + ChatColor.GREEN + "!");
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Uso:");
        sender.sendMessage(ChatColor.YELLOW + "  /trialspawner <mob_custom> [jugador]");
        sender.sendMessage(ChatColor.YELLOW + "  /trialspawner vanilla <mob_vanilla> [jugador]");
    }

    // =========================================================================
    // ITEM FACTORY — claves 1:1 con TrialSpawnerHandler
    // =========================================================================

    private ItemStack createTrialSpawnerItem(String mobType) {
        ItemStack spawner = new ItemStack(Material.SPAWNER);
        ItemMeta meta = spawner.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        pdc.set(new NamespacedKey(plugin, "viciont_trial_mob"),              PersistentDataType.STRING,  mobType);

        // Spawn
        pdc.set(new NamespacedKey(plugin, "viciont_ts_total_mobs"),          PersistentDataType.INTEGER, DEFAULT_TOTAL_MOBS);
        pdc.set(new NamespacedKey(plugin, "viciont_ts_sim_min"),             PersistentDataType.INTEGER, DEFAULT_SIM_MIN);
        pdc.set(new NamespacedKey(plugin, "viciont_ts_sim_max"),             PersistentDataType.INTEGER, DEFAULT_SIM_MAX);
        pdc.set(new NamespacedKey(plugin, "viciont_ts_ticks"),               PersistentDataType.INTEGER, DEFAULT_TICKS);
        pdc.set(new NamespacedKey(plugin, "viciont_ts_range"),               PersistentDataType.INTEGER, DEFAULT_RANGE);
        pdc.set(new NamespacedKey(plugin, "viciont_ts_cooldown"),            PersistentDataType.INTEGER, DEFAULT_COOLDOWN);
        pdc.set(new NamespacedKey(plugin, "viciont_ts_player_range"),        PersistentDataType.INTEGER, DEFAULT_PLAYER_RANGE);

        // Bonus jugadores (nuevas keys)
        pdc.set(new NamespacedKey(plugin, "viciont_ts_player_bonus_enabled"), PersistentDataType.INTEGER, DEFAULT_BONUS_ENABLED);
        pdc.set(new NamespacedKey(plugin, "viciont_ts_player_bonus_max"),     PersistentDataType.INTEGER, DEFAULT_BONUS_MAX);

        // Loot
        pdc.set(new NamespacedKey(plugin, "viciont_trial_loot_max_items"),   PersistentDataType.INTEGER, DEFAULT_LOOT_MAX);
        pdc.set(new NamespacedKey(plugin, "viciont_trial_loot_special_max"), PersistentDataType.INTEGER, DEFAULT_LOOT_SPEC_MAX);
        pdc.set(new NamespacedKey(plugin, "viciont_trial_loot_normal"),          PersistentDataType.STRING, "");
        pdc.set(new NamespacedKey(plugin, "viciont_trial_loot_ominous"),         PersistentDataType.STRING, "");
        pdc.set(new NamespacedKey(plugin, "viciont_trial_loot_vanilla_normal"),  PersistentDataType.STRING, "");
        pdc.set(new NamespacedKey(plugin, "viciont_trial_loot_vanilla_ominous"), PersistentDataType.STRING, "");

        // CMD para resource pack
        meta.setCustomModelData(TRIAL_SPAWNER_CMD);

        spawner.setItemMeta(meta);
        TrialSpawnerHandler.rebuildSpawnerLore(spawner, plugin);
        return spawner;
    }

    // =========================================================================
    // TAB COMPLETE
    // =========================================================================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("vanilla");
            suggestions.addAll(mobManager.getRegisteredMobs());
            suggestions.removeIf(s -> !s.toLowerCase().startsWith(args[0].toLowerCase()));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("vanilla")) {
                for (EntityType type : EntityType.values())
                    if (type.isSpawnable() && type.isAlive() && type.name().toLowerCase().startsWith(args[1].toLowerCase()))
                        suggestions.add(type.name().toLowerCase());
            } else {
                for (Player p : Bukkit.getOnlinePlayers())
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase()))
                        suggestions.add(p.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("vanilla")) {
            for (Player p : Bukkit.getOnlinePlayers())
                if (p.getName().toLowerCase().startsWith(args[2].toLowerCase()))
                    suggestions.add(p.getName());
        }
        return suggestions;
    }
}