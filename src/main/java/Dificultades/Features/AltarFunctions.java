package Dificultades.Features;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatColor; // Importante para colores HEX

import java.util.*;

public class AltarFunctions implements Listener, CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final NamespacedKey cooldownKey;

    // OPTIMIZACIÓN: Lista única para gestionar todos los hologramas activos
    private final List<TextDisplay> activeAltars = new ArrayList<>();

    // Color Hexadecimal #bc74ec
    private final ChatColor customHexColor = ChatColor.of("#bc74ec");

    public AltarFunctions(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cooldownKey = new NamespacedKey(plugin, "altar_cooldown_end");

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        if (plugin.getCommand("altarvct") != null) {
            plugin.getCommand("altarvct").setExecutor(this);
            plugin.getCommand("altarvct").setTabCompleter(this);
        }

        // Recuperar y arrancar el motor global
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            restoreAllLoadedCooldowns();
            startGlobalTicker();
        }, 40L);
    }

    // --- ESTRUCTURA QUEEN BEE ---
    private final Material[][][] queenBeeAltarLayers = {
            { // Layer 1
                    {Material.HONEY_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEY_BLOCK},
                    {Material.HONEYCOMB_BLOCK, Material.HONEY_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEY_BLOCK, Material.HONEYCOMB_BLOCK},
                    {Material.HONEYCOMB_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEYCOMB_BLOCK},
                    {Material.HONEYCOMB_BLOCK, Material.HONEY_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEY_BLOCK, Material.HONEYCOMB_BLOCK},
                    {Material.HONEY_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEYCOMB_BLOCK, Material.HONEY_BLOCK}
            },
            { // Layer 2
                    {Material.HONEY_BLOCK, Material.AIR, Material.AIR, Material.AIR, Material.HONEY_BLOCK},
                    {Material.AIR, Material.AIR, Material.TORCH, Material.AIR, Material.AIR},
                    {Material.AIR, Material.TORCH, Material.HONEYCOMB_BLOCK, Material.TORCH, Material.AIR},
                    {Material.AIR, Material.AIR, Material.TORCH, Material.AIR, Material.AIR},
                    {Material.HONEY_BLOCK, Material.AIR, Material.AIR, Material.AIR, Material.HONEY_BLOCK}
            },
            { // Layer 3
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.BEE_NEST, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR}
            },
            { // Layer 4
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.HONEY_BLOCK, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR},
                    {Material.AIR, Material.AIR, Material.AIR, Material.AIR, Material.AIR}
            }
    };

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        if (block.getType() == Material.BEE_NEST) {
            if (isValidAltar(block.getLocation(), queenBeeAltarLayers)) {

                if (isCooldownActive(block.getLocation())) {
                    event.getPlayer().sendMessage(ChatColor.RED + "⏳ Este altar se está regenerando...");
                    event.getPlayer().playSound(block.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                    return;
                }

                AltarActivateEvent activateEvent = new AltarActivateEvent(event.getPlayer(), block.getLocation(), "queen_bee");
                Bukkit.getPluginManager().callEvent(activateEvent);

                if (activateEvent.isCancelled()) return;

                if (activateEvent.getCooldownSeconds() > 0) {
                    createCooldownHologram(block.getLocation(), activateEvent.getCooldownSeconds());
                }
            }
        }
    }

    // --- GESTIÓN DE COOLDOWNS ---

    private void createCooldownHologram(Location loc, int seconds) {
        long endTime = System.currentTimeMillis() + (seconds * 1000L);
        // Altura +2.5 (1 bloque más arriba que el 1.5 original)
        Location hologramLoc = loc.clone().add(0.5, 2.5, 0.5);

        TextDisplay display = (TextDisplay) loc.getWorld().spawnEntity(hologramLoc, EntityType.TEXT_DISPLAY);
        display.setBillboard(Display.Billboard.CENTER);
        display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
        display.setShadowed(true);

        display.getPersistentDataContainer().set(cooldownKey, PersistentDataType.LONG, endTime);
        activeAltars.add(display);

        // Ordenar por tiempo restante (opcional, para que #1 sea siempre el que acaba antes)
        // activeAltars.sort(Comparator.comparingLong(d -> d.getPersistentDataContainer().get(cooldownKey, PersistentDataType.LONG)));
    }

    private boolean isCooldownActive(Location loc) {
        Location checkLoc = loc.clone().add(0.5, 2.5, 0.5);
        for (Entity e : loc.getWorld().getNearbyEntities(checkLoc, 1, 2, 1)) {
            if (e instanceof TextDisplay display && display.getPersistentDataContainer().has(cooldownKey, PersistentDataType.LONG)) {
                return true;
            }
        }
        return false;
    }

    // --- MOTOR GLOBAL DE ACTUALIZACIÓN ---
    private void startGlobalTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeAltars.isEmpty()) return;

                Iterator<TextDisplay> it = activeAltars.iterator();
                int index = 1;

                while (it.hasNext()) {
                    TextDisplay display = it.next();

                    if (display == null || !display.isValid()) {
                        it.remove();
                        continue;
                    }

                    PersistentDataContainer data = display.getPersistentDataContainer();
                    if (!data.has(cooldownKey, PersistentDataType.LONG)) {
                        it.remove();
                        continue;
                    }

                    long endTime = data.get(cooldownKey, PersistentDataType.LONG);
                    long remainingMillis = endTime - System.currentTimeMillis();

                    if (remainingMillis <= 0) {
                        // Tiempo acabado
                        display.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, display.getLocation(), 15, 0.5, 0.5, 0.5);
                        display.getWorld().playSound(display.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 2f);
                        display.remove();
                        it.remove();
                    } else {
                        // Actualizar Texto
                        long totalSeconds = remainingMillis / 1000;
                        long h = totalSeconds / 3600;
                        long m = (totalSeconds % 3600) / 60;
                        long s = totalSeconds % 60;

                        display.setText(
                                customHexColor + "" + ChatColor.BOLD + "Altar #" + index + " en Cooldown\n" +
                                        ChatColor.WHITE + String.format("%02d:%02d:%02d", h, m, s)
                        );
                        index++;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // --- RESTAURACIÓN ---
    private void restoreAllLoadedCooldowns() {
        activeAltars.clear();
        for (World world : Bukkit.getWorlds()) {
            for (TextDisplay display : world.getEntitiesByClass(TextDisplay.class)) {
                if (display.getPersistentDataContainer().has(cooldownKey, PersistentDataType.LONG)) {
                    activeAltars.add(display);
                }
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof TextDisplay display &&
                    display.getPersistentDataContainer().has(cooldownKey, PersistentDataType.LONG)) {

                if (!activeAltars.contains(display)) {
                    activeAltars.add(display);
                }
            }
        }
    }

    // --- COMANDOS ---

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Sin permisos.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "info" -> {
                sender.sendMessage(ChatColor.GOLD + "=== Altares en Cooldown (" + activeAltars.size() + ") ===");
                int i = 1;
                for (TextDisplay display : activeAltars) {
                    Location l = display.getLocation();
                    long end = display.getPersistentDataContainer().get(cooldownKey, PersistentDataType.LONG);
                    long left = (end - System.currentTimeMillis()) / 1000;
                    sender.sendMessage(ChatColor.YELLOW + "#" + i + " " + ChatColor.WHITE +
                            "[" + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() + "] " +
                            ChatColor.RED + left + "s restantes");
                    i++;
                }
            }
            case "create" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Solo jugadores.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /altarvct create <nombre>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("queenbee")) {
                    buildStructure(player.getLocation(), queenBeeAltarLayers);
                    player.sendMessage(ChatColor.GREEN + "Altar de Queen Bee construido.");
                } else {
                    player.sendMessage(ChatColor.RED + "Altar no reconocido.");
                }
            }
            case "addcooldown", "delcooldown" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /altarvct " + action + " <#altar> <tiempo>");
                    return true;
                }
                try {
                    int index = Integer.parseInt(args[1]) - 1;
                    if (index < 0 || index >= activeAltars.size()) {
                        sender.sendMessage(ChatColor.RED + "Número de altar inválido. Usa /altarvct info");
                        return true;
                    }

                    long seconds = parseTime(args[2]);
                    if (seconds == 0) {
                        sender.sendMessage(ChatColor.RED + "Formato inválido. Ej: 300, 5m, 00:05:00");
                        return true;
                    }

                    TextDisplay display = activeAltars.get(index);
                    long currentEnd = display.getPersistentDataContainer().get(cooldownKey, PersistentDataType.LONG);
                    long millis = seconds * 1000L;

                    long newEnd = action.equals("addcooldown") ? currentEnd + millis : currentEnd - millis;

                    // Si restamos tanto que ya terminó
                    if (newEnd <= System.currentTimeMillis()) {
                        display.remove();
                        activeAltars.remove(index);
                        sender.sendMessage(ChatColor.GREEN + "Cooldown eliminado.");
                    } else {
                        display.getPersistentDataContainer().set(cooldownKey, PersistentDataType.LONG, newEnd);
                        sender.sendMessage(ChatColor.GREEN + "Tiempo actualizado.");
                    }

                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Número inválido.");
                }
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp()) return Collections.emptyList();

        if (args.length == 1) {
            return filter(Arrays.asList("info", "create", "addcooldown", "delcooldown"), args[0]);
        }
        if (args.length == 2) {
            String action = args[0].toLowerCase();
            if (action.equals("create")) {
                return filter(Collections.singletonList("queenbee"), args[1]);
            }
            if (action.equals("addcooldown") || action.equals("delcooldown")) {
                List<String> indexes = new ArrayList<>();
                for (int i = 1; i <= activeAltars.size(); i++) indexes.add(String.valueOf(i));
                return indexes;
            }
        }
        if (args.length == 3) {
            String action = args[0].toLowerCase();
            if (action.equals("addcooldown") || action.equals("delcooldown")) {
                return Arrays.asList("60", "5m", "1h", "00:10:00");
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String arg) {
        List<String> result = new ArrayList<>();
        for (String s : list) if (s.toLowerCase().startsWith(arg.toLowerCase())) result.add(s);
        return result;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "--- Altar VCT ---");
        sender.sendMessage("/altarvct info");
        sender.sendMessage("/altarvct create <nombre>");
        sender.sendMessage("/altarvct addcooldown <#> <tiempo>");
        sender.sendMessage("/altarvct delcooldown <#> <tiempo>");
    }

    private long parseTime(String input) {
        try {
            if (input.contains(":")) {
                String[] parts = input.split(":");
                long h = Long.parseLong(parts[0]);
                long m = Long.parseLong(parts[1]);
                long s = Long.parseLong(parts[2]);
                return (h * 3600) + (m * 60) + s;
            } else if (input.endsWith("m")) {
                return Long.parseLong(input.replace("m", "")) * 60;
            } else if (input.endsWith("h")) {
                return Long.parseLong(input.replace("h", "")) * 3600;
            } else {
                return Long.parseLong(input.replace("s", ""));
            }
        } catch (Exception e) {
            return 0;
        }
    }

    // --- ESTRUCTURA ---
    private boolean isValidAltar(Location center, Material[][][] structure) {
        int baseY = center.getBlockY();
        int startY = baseY - 2;
        for (int y = 0; y < structure.length; y++) {
            for (int x = 0; x < 5; x++) {
                for (int z = 0; z < 5; z++) {
                    Material expected = structure[y][x][z];
                    Block worldBlock = center.getWorld().getBlockAt(center.getBlockX() + (x - 2), startY + y, center.getBlockZ() + (z - 2));
                    if (expected != Material.AIR && worldBlock.getType() != expected) return false;
                }
            }
        }
        return true;
    }

    private void buildStructure(Location center, Material[][][] structure) {
        int startY = center.getBlockY();
        int baseOffsetY = -2;
        for (int y = 0; y < structure.length; y++) {
            for (int x = 0; x < 5; x++) {
                for (int z = 0; z < 5; z++) {
                    Material mat = structure[y][x][z];
                    if (mat != Material.AIR) {
                        Block block = center.getWorld().getBlockAt(center.getBlockX() + (x - 2), startY + baseOffsetY + y, center.getBlockZ() + (z - 2));
                        block.setType(mat);
                    }
                }
            }
        }
    }
}