package ShopSystem;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ShopCommands implements CommandExecutor, TabCompleter {

    private final ShopManager shopManager;
    private final ShopGUI shopGUI;

    public ShopCommands(ShopManager shopManager, ShopGUI shopGUI) {
        this.shopManager = shopManager;
        this.shopGUI = shopGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Solo jugadores.");
            return true;
        }
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("spawnshop")) {
            if (!player.hasPermission("viciont_hardcore3.shop.admin")) return true;
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Uso: /spawnshop <\"Nombre\"> [x y z] [bioma] [profesion]");
                return true;
            }

            String fullArgs = String.join(" ", args);
            String name = "Tienda";

            if (fullArgs.startsWith("\"")) {
                int endQuote = fullArgs.indexOf("\"", 1);
                if (endQuote != -1) {
                    name = fullArgs.substring(1, endQuote);
                    fullArgs = fullArgs.substring(endQuote + 1).trim();
                } else {
                    name = fullArgs.substring(1);
                    fullArgs = "";
                }
            } else {
                String[] split = fullArgs.split(" ", 2);
                name = split[0].replace("_", " ");
                fullArgs = split.length > 1 ? split[1] : "";
            }

            Location loc = player.getLocation();
            Villager.Type type = Villager.Type.PLAINS;
            Villager.Profession profession = Villager.Profession.NONE;

            if (!fullArgs.isEmpty()) {
                String[] remainingArgs = fullArgs.split(" ");
                int argIndex = 0;

                if (remainingArgs.length >= 3 && isDouble(remainingArgs[0])) {
                    try {
                        double x = Double.parseDouble(remainingArgs[0]);
                        double y = Double.parseDouble(remainingArgs[1]);
                        double z = Double.parseDouble(remainingArgs[2]);
                        loc = new Location(player.getWorld(), x, y, z);
                        argIndex = 3;
                    } catch (NumberFormatException ignored) {}
                }

                if (remainingArgs.length > argIndex) {
                    try {
                        type = Villager.Type.valueOf(remainingArgs[argIndex].toUpperCase());
                        argIndex++;
                    } catch (IllegalArgumentException ignored) {}
                }

                if (remainingArgs.length > argIndex) {
                    try {
                        profession = Villager.Profession.valueOf(remainingArgs[argIndex].toUpperCase());
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            shopManager.spawnShop(name, loc, type, profession);
            player.sendMessage(ChatColor.GREEN + "Tienda creada: " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', name) + ChatColor.GREEN + " (" + type.name() + " / " + profession.name() + ")");
            return true;
        }

        if (command.getName().equalsIgnoreCase("removeshop")) {
            if (!player.hasPermission("viciont_hardcore3.shop.admin")) return true;

            RayTraceResult result = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 5.0,
                    entity -> entity instanceof Villager && entity.getPersistentDataContainer().has(shopManager.shopKey, PersistentDataType.STRING));

            Villager target = null;

            if (result != null && result.getHitEntity() != null) {
                target = (Villager) result.getHitEntity();
            } else {
                double minDistance = Double.MAX_VALUE;
                for (Entity e : player.getNearbyEntities(3, 3, 3)) {
                    if (e instanceof Villager) {
                        Villager v = (Villager) e;
                        if (v.getPersistentDataContainer().has(shopManager.shopKey, PersistentDataType.STRING)) {
                            double dist = e.getLocation().distanceSquared(player.getLocation());
                            if (dist < minDistance) {
                                minDistance = dist;
                                target = v;
                            }
                        }
                    }
                }
            }

            if (target != null) {
                String id = target.getPersistentDataContainer().get(shopManager.shopIdKey, PersistentDataType.STRING);
                shopManager.removeShopFromFile(id);
                target.remove();
                player.sendMessage(ChatColor.GREEN + "Tienda eliminada.");
            } else {
                player.sendMessage(ChatColor.RED + "No se encontró ninguna tienda cercana o en tu mira.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("trade")) {
            if (!player.hasPermission("viciont_hardcore3.shop.admin")) return true;

            UUID uuid = player.getUniqueId();
            if (!shopManager.editingTradeIndex.containsKey(uuid)) {
                player.sendMessage(ChatColor.RED + "No estás editando ninguna tienda.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Uso: /trade <item> <cantidad>");
                return true;
            }
            String itemName = args[0];
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Cantidad inválida.");
                return true;
            }
            ItemStack item = CustomItemRegistry.getCustomItem(itemName, amount);
            if (item == null) {
                try {
                    item = new ItemStack(Material.valueOf(itemName.toUpperCase()), amount);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.RED + "Item no encontrado: " + itemName);
                    return true;
                }
            }

            String shopId = shopManager.activeShops.get(uuid);
            Integer tradeIndex = shopManager.editingTradeIndex.get(uuid);
            String slotType = shopManager.editingSlotType.get(uuid);

            if (shopId == null || tradeIndex == null || slotType == null) {
                player.sendMessage(ChatColor.RED + "Error: Sesión perdida. Abre la GUI.");
                return true;
            }

            Villager villager = shopManager.getVillagerById(shopId);
            if (villager == null) {
                player.sendMessage(ChatColor.RED + "El aldeano ya no existe.");
                return true;
            }

            shopManager.updateVillagerTrade(villager, tradeIndex, slotType, item);
            shopManager.saveShopTrades(shopId, villager.getRecipes());

            shopManager.editingTradeIndex.remove(uuid);
            shopManager.editingSlotType.remove(uuid);

            player.sendMessage(ChatColor.GREEN + "Tradeo actualizado: " + slotType + " -> " + item.getType());
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

            return true;
        }
        return false;
    }

    private boolean isDouble(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("trade")) {
            if (args.length == 1) {
                List<String> suggestions = new ArrayList<>();
                for (Material m : Material.values()) {
                    if (m.isItem()) suggestions.add(m.name().toLowerCase());
                }
                suggestions.addAll(CustomItemRegistry.getAllCustomNames());
                return suggestions.stream()
                        .filter(s -> s.startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        if (command.getName().equalsIgnoreCase("spawnshop")) {
            if (args.length == 1) return Arrays.asList("\"Nombre de la Tienda\"");

            String fullArgs = String.join(" ", args);
            String[] remaining = null;
            if (fullArgs.startsWith("\"")) {
                int endQuote = fullArgs.indexOf("\"", 1);
                if (endQuote != -1 && fullArgs.length() > endQuote + 1) {
                    remaining = fullArgs.substring(endQuote + 1).trim().split(" ");
                }
            } else {
                String[] split = fullArgs.split(" ");
                if (split.length > 1) {
                    remaining = Arrays.copyOfRange(split, 1, split.length);
                }
            }

            if (remaining != null && remaining.length > 0) {
                int currentArgIndex = remaining.length - 1; // 0=coordX, 1=coordY, 2=coordZ, 3=Type, 4=Profession
                String lastArg = remaining[currentArgIndex].toUpperCase();

                if (currentArgIndex == 3 || currentArgIndex == 0) {
                    List<String> suggestions = new ArrayList<>();
                    for (Villager.Type t : Villager.Type.values()) suggestions.add(t.name());
                    return suggestions.stream().filter(s -> s.startsWith(lastArg)).collect(Collectors.toList());
                } else if (currentArgIndex == 4 || currentArgIndex == 1) { // Profesión
                    List<String> suggestions = new ArrayList<>();
                    for (Villager.Profession p : Villager.Profession.values()) suggestions.add(p.name());
                    return suggestions.stream().filter(s -> s.startsWith(lastArg)).collect(Collectors.toList());
                }
            }
        }
        return null;
    }
}