package Casino;

import net.md_5.bungee.api.ChatColor; // Importante para colores Hex
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CasinoManager {
    private final JavaPlugin plugin;
    private final File tableFile;
    private FileConfiguration tableConfig;

    private final Map<Location, String> casinoTables = new HashMap<>();
    private final Map<Location, TextDisplay> activeHolograms = new HashMap<>();

    private final SlotMachine slotMachine;
    private final BlackJack blackJack;

    public CasinoManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.tableFile = new File(plugin.getDataFolder(), "CasinoTables.yml");

        loadTables();

        this.slotMachine = new SlotMachine(plugin, this);
        this.blackJack = new BlackJack(plugin, this);

        startParticleTask();
        refreshHolograms();
    }

    private void loadTables() {
        if (!tableFile.exists()) {
            try {
                tableFile.getParentFile().mkdirs();
                tableFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        tableConfig = YamlConfiguration.loadConfiguration(tableFile);

        if (tableConfig.contains("tables")) {
            for (String key : tableConfig.getConfigurationSection("tables").getKeys(false)) {
                String type = tableConfig.getString("tables." + key + ".type");
                String worldName = tableConfig.getString("tables." + key + ".world");
                double x = tableConfig.getDouble("tables." + key + ".x");
                double y = tableConfig.getDouble("tables." + key + ".y");
                double z = tableConfig.getDouble("tables." + key + ".z");

                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    // Usar coordenadas de bloque exactas para el mapa
                    casinoTables.put(new Location(world, Math.floor(x), Math.floor(y), Math.floor(z)), type);
                }
            }
        }
    }

    public void saveTable(Location loc, String type) {
        String key = getLocationKey(loc);
        tableConfig.set("tables." + key + ".type", type);
        tableConfig.set("tables." + key + ".world", loc.getWorld().getName());
        tableConfig.set("tables." + key + ".x", loc.getBlockX());
        tableConfig.set("tables." + key + ".y", loc.getBlockY());
        tableConfig.set("tables." + key + ".z", loc.getBlockZ());

        try {
            tableConfig.save(tableFile);
            Location cleanLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            casinoTables.put(cleanLoc, type);
            spawnHologram(cleanLoc);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeTable(Location loc) {
        String key = getLocationKey(loc);
        tableConfig.set("tables." + key, null);
        try {
            tableConfig.save(tableFile);
            casinoTables.remove(loc);
            removeHologram(loc);
            loc.getBlock().setType(Material.AIR);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getLocationKey(Location loc) {
        return loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }

    public void reload() {
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (e.getScoreboardTags().contains("casino_hologram")) {
                    e.remove();
                }
            }
        }

        activeHolograms.clear();
        casinoTables.clear();

        loadTables();
        refreshHolograms();

        slotMachine.reloadConfig();
        blackJack.reloadConfig();
    }

    // --- LÓGICA DE HOLOGRAMAS ---

    public void setGameActive(Location loc, boolean active) {
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        if (!casinoTables.containsKey(blockLoc)) return;

        if (active) {
            removeHologram(blockLoc);
        } else {
            removeHologram(blockLoc);
            spawnHologram(blockLoc);
        }
    }

    private void refreshHolograms() {
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (e instanceof TextDisplay && e.getScoreboardTags().contains("casino_hologram")) {
                    e.remove();
                }
            }
        }
        activeHolograms.clear();

        for (Location loc : casinoTables.keySet()) {
            spawnHologram(loc);
        }
    }

    private void spawnHologram(Location loc) {
        if (activeHolograms.containsKey(loc)) return;
        if (!loc.getChunk().isLoaded()) return;

        Location holoLoc = loc.clone().add(0.5, 3.0, 0.5);

        String type = casinoTables.get(loc);
        String titleText;

        ChatColor colorTitle = ChatColor.of("#FFB7B2");
        ChatColor colorSub = ChatColor.of("#B5EAD7");
        ChatColor colorArrow = ChatColor.of("#FFF5BA");

        if ("blackjack".equalsIgnoreCase(type)) {
            titleText = colorTitle + "" + ChatColor.BOLD + "BlackJack";
        } else if ("slot".equalsIgnoreCase(type)) {
            titleText = colorTitle + "" + ChatColor.BOLD + "Máquina Tragamonedas";
        } else {
            titleText = colorTitle + "" + ChatColor.BOLD + "Casino";
        }

        TextDisplay display = loc.getWorld().spawn(holoLoc, TextDisplay.class);

        display.setText(
                titleText + "\n" +
                        colorSub + "Click derecho para jugar\n" +
                        colorArrow + "▼"
        );

        display.setBillboard(Display.Billboard.CENTER);
        display.addScoreboardTag("casino_hologram");
        display.setPersistent(true);
        display.setShadowed(true);
        display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

        activeHolograms.put(loc, display);
    }

    private void removeHologram(Location loc) {
        if (activeHolograms.containsKey(loc)) {
            TextDisplay display = activeHolograms.get(loc);
            if (display != null && display.isValid()) {
                display.remove();
            }
            activeHolograms.remove(loc);
        }

        if (loc.getWorld() != null && loc.getChunk().isLoaded()) {
            Location searchLoc = loc.clone().add(0.5, 3.0, 0.5);
            for (Entity e : loc.getChunk().getEntities()) {
                if (e instanceof TextDisplay && e.getScoreboardTags().contains("casino_hologram")) {
                    if (e.getLocation().distanceSquared(searchLoc) < 2.0) {
                        e.remove();
                    }
                }
            }
        }
    }

    // --- PARTICULAS ---

    private void startParticleTask() {
        new BukkitRunnable() {
            double angle = 0;

            @Override
            public void run() {
                angle += 0.2;
                if (angle > Math.PI * 2) angle = 0;

                for (Map.Entry<Location, String> entry : casinoTables.entrySet()) {
                    Location loc = entry.getKey();
                    String type = entry.getValue();

                    if (!loc.getChunk().isLoaded()) continue;

                    double x = loc.getX() + 0.5 + Math.cos(angle) * 1.0;
                    double z = loc.getZ() + 0.5 + Math.sin(angle) * 1.0;

                    Color color = type.equalsIgnoreCase("blackjack") ? Color.LIME : Color.ORANGE;

                    loc.getWorld().spawnParticle(Particle.DUST, x, loc.getY() + 1.2, z, 1,
                            new Particle.DustOptions(color, 1));

                    double x2 = loc.getX() + 0.5 + Math.cos(angle + Math.PI) * 1.0;
                    double z2 = loc.getZ() + 0.5 + Math.sin(angle + Math.PI) * 1.0;

                    loc.getWorld().spawnParticle(Particle.DUST, x2, loc.getY() + 1.2, z2, 1,
                            new Particle.DustOptions(color, 1));
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public boolean isTable(Location loc) {
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return casinoTables.containsKey(blockLoc);
    }

    public String getTableType(Location loc) {
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return casinoTables.get(blockLoc);
    }

    public Set<Location> getTables() {
        return casinoTables.keySet();
    }

    public SlotMachine getSlotMachine() { return slotMachine; }
    public BlackJack getBlackJack() { return blackJack; }
}