package Commands;

import Dificultades.CustomMobs.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class EggSpawnerCommand implements CommandExecutor, TabCompleter, Listener {
    private final JavaPlugin plugin;
    private final Bombita bombitaSpawner;
    private final Iceologer iceologerSpawner;
    private final CorruptedZombies corruptedZombieSpawner;
    private final CorruptedSpider corruptedSpider;
    private final QueenBeeHandler queenBeeHandler;
    private final GuardianBlaze guardianBlaze;

    public EggSpawnerCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.bombitaSpawner = new Bombita(plugin);
        this.iceologerSpawner = new Iceologer(plugin);
        this.corruptedZombieSpawner = new CorruptedZombies(plugin);
        this.corruptedSpider = new CorruptedSpider(plugin);
        this.queenBeeHandler = new QueenBeeHandler(plugin);
        this.guardianBlaze = new GuardianBlaze(plugin);

        plugin.getCommand("eggvct").setExecutor(this);
        plugin.getCommand("eggvct").setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Uso: /eggvct <mob>");
            return true;
        }

        String mobType = args[0].toLowerCase();
        ItemStack egg = createMobEgg(mobType);

        if (egg != null) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.getInventory().addItem(egg);
                player.sendMessage("¡Has recibido un huevo de spawn para " + mobType + "!");
            } else {
                sender.sendMessage("Este comando solo puede ser usado por jugadores.");
            }
        } else {
            sender.sendMessage("Mob no reconocido. Usa /eggvct <bombita|iceologer|corruptedzombie|corruptedspider|queenbee|guardianblaze>");
        }
        return true;
    }

    private ItemStack createMobEgg(String mobType) {
        switch (mobType) {
            case "bombita":
                return createCustomEgg("Bombita");
            case "iceologer":
                return createCustomEgg("Iceologer");
            case "corruptedzombie":
                return createCustomEgg("Corrupted Zombie");
            case "corruptedspider":
                return createCustomEgg("Corrupted Spider");
            case "queenbee":
                return createCustomEgg("Queen Bee");
            case "guardianblaze":
                return createCustomEgg("Guardian Blaze");
            default:
                return null;
        }
    }

    private ItemStack createCustomEgg(String mobName) {
        ItemStack egg = new ItemStack(Material.ORANGE_DYE);
        ItemMeta meta = egg.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("Huevo de " + mobName);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(new NamespacedKey(plugin, "mobName"), PersistentDataType.STRING, mobName);
            container.set(new NamespacedKey(plugin, "isCustomSpawnEgg"), PersistentDataType.BYTE, (byte) 1);
            meta.setCustomModelData(50);
            egg.setItemMeta(meta);
        }
        return egg;
    }

    @EventHandler
    public void onPlayerUseEgg(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.ORANGE_DYE) { // Asegurarte de que es el huevo personalizado
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 50) {
                    PersistentDataContainer container = meta.getPersistentDataContainer();
                    if (container.has(new NamespacedKey(plugin, "mobName"), PersistentDataType.STRING)) {
                        String mobName = container.get(new NamespacedKey(plugin, "mobName"), PersistentDataType.STRING);

                        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.SPAWNER) {
                            CreatureSpawner spawner = (CreatureSpawner) event.getClickedBlock().getState();
                            spawner.getPersistentDataContainer().set(new NamespacedKey(plugin, "mobName"), PersistentDataType.STRING, mobName);
                            spawner.update();

                            event.getPlayer().sendMessage("¡Spawner configurado para generar " + mobName + "!");

                            item.setAmount(item.getAmount() - 1);
                            event.setCancelled(true);
                        } else {
                            Location location = event.getClickedBlock().getLocation().add(0.5, 1, 0.5);

                            switch (mobName) {
                                case "Bombita":
                                    bombitaSpawner.spawnBombita(location);
                                    break;
                                case "Iceologer":
                                    iceologerSpawner.spawnIceologer(location);
                                    break;
                                case "Corrupted Zombie":
                                    corruptedZombieSpawner.spawnCorruptedZombie(location);
                                    break;
                                case "Corrupted Spider":
                                    corruptedSpider.spawnCorruptedSpider(location);
                                    break;
                                case "Queen Bee":
                                    queenBeeHandler.spawnQueenBee(location);
                                    break;
                                case "Guardian Blaze":
                                    guardianBlaze.spawnGuardianBlaze(location);
                                    break;
                            }

                            item.setAmount(item.getAmount() - 1);
                            event.getPlayer().sendMessage("¡Has generado un " + mobName + "!");
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        CreatureSpawner spawner = (CreatureSpawner) event.getSpawner().getBlock().getState();
        PersistentDataContainer container = spawner.getPersistentDataContainer();

        if (container.has(new NamespacedKey(plugin, "mobName"), PersistentDataType.STRING)) {
            String mobName = container.get(new NamespacedKey(plugin, "mobName"), PersistentDataType.STRING);
            Location location = event.getLocation();

            switch (mobName) {
                case "Bombita":
                    bombitaSpawner.spawnBombita(location);
                    break;
                case "Iceologer":
                    iceologerSpawner.spawnIceologer(location);
                    break;
                case "Corrupted Zombie":
                    corruptedZombieSpawner.spawnCorruptedZombie(location);
                    break;
                case "Corrupted Spider":
                    corruptedSpider.spawnCorruptedSpider(location);
                    break;
                case "Queen Bee":
                    queenBeeHandler.spawnQueenBee(location);
                    break;
                case "Guardian Blaze":
                    guardianBlaze.spawnGuardianBlaze(location);
                    break;
            }

            event.setCancelled(true);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            List<String> mobs = List.of("bombita", "iceologer", "corruptedzombie", "corruptedspider", "queenbee", "guardianblaze");
            for (String mob : mobs) {
                if (mob.toLowerCase().startsWith(args[0].toLowerCase())) {
                    suggestions.add(mob);
                }
            }
        }

        return suggestions;
    }

}
