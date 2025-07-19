package items;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyItemsFunctions implements Listener {

    private final JavaPlugin plugin;
    private Connection connection;
    private final Map<UUID, String> mochilasAbiertas = new ConcurrentHashMap<>();
    private final Set<UUID> cooldownGancho = ConcurrentHashMap.newKeySet();
    private final Map<String, ItemStack[]> mochilasCache = new ConcurrentHashMap<>();

    public EconomyItemsFunctions(JavaPlugin plugin) {
        this.plugin = plugin;
        setupDatabase();
    }

    // Configuración mejorada de la base de datos SQLite
    private void setupDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/mochilas.db");

            try (Statement stmt = connection.createStatement()) {
                // Verificar si la tabla existe
                ResultSet rs = connection.getMetaData().getTables(null, null, "mochilas", null);
                if (!rs.next()) {
                    // Crear tabla si no existe
                    stmt.execute("CREATE TABLE mochilas (" +
                            "id TEXT PRIMARY KEY, " +
                            "items TEXT)");
                } else {
                    // Verificar si la columna 'id' existe
                    try {
                        stmt.execute("SELECT id FROM mochilas LIMIT 1");
                    } catch (SQLException e) {
                        // Si falla, la columna no existe - recrear tabla
                        stmt.execute("DROP TABLE mochilas");
                        stmt.execute("CREATE TABLE mochilas (" +
                                "id TEXT PRIMARY KEY, " +
                                "items TEXT)");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            plugin.getLogger().severe("Error al configurar la base de datos de mochilas!");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !event.getAction().toString().contains("RIGHT")) return;

        Player player = event.getPlayer();

        // Ender Bag
        if (isEnderBag(item)) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
            player.openInventory(player.getEnderChest());
            return;
        }

        // Mochila
        if (isMochila(item)) {
            event.setCancelled(true);
            abrirMochila(player, item);
            return;
        }

        // Gancho
        if (isGancho(item)) {
            event.setCancelled(true);
            usarGancho(player, item);
            return;
        }

        // Yunque Reparador Nivel 1
        if (isYunqueNivel1(item)) {
            event.setCancelled(true);
            usarYunque(player, item, 0.25);
            return;
        }

        // Yunque Reparador Nivel 2
        if (isYunqueNivel2(item)) {
            event.setCancelled(true);
            usarYunque(player, item, 1.0);
            return;
        }

        // Manzana del Pánico
        if (isManzanaPanico(item)) {
            event.setCancelled(true);
            usarManzanaPanico(player, item);
            return;
        }
    }

    private boolean isEnderBag(ItemStack item) {
        if (item == null || item.getType() != Material.ECHO_SHARD) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 2030;
    }

    private boolean isMochila(ItemStack item) {
        if (item == null || item.getType() != Material.ECHO_SHARD) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 2020;
    }

    private boolean isGancho(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 10;
    }

    private boolean isManzanaPanico(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_APPLE) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 10;
    }

    private boolean isYunqueNivel1(ItemStack item) {
        if (item == null || item.getType() != Material.ECHO_SHARD) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 2040;
    }

    private boolean isYunqueNivel2(ItemStack item) {
        if (item == null || item.getType() != Material.ECHO_SHARD) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 2050;
    }

    private void abrirMochila(Player player, ItemStack mochila) {
        String mochilaId = getMochilaId(mochila);
        if (mochilaId == null) {
            mochilaId = UUID.randomUUID().toString();
            setMochilaId(mochila, mochilaId);
        }

        mochilasAbiertas.put(player.getUniqueId(), mochilaId);
        Inventory mochilaInv = Bukkit.createInventory(null, 27, "Mochila " + mochilaId.substring(0, 4));

        // Cargar desde cache o base de datos
        if (mochilasCache.containsKey(mochilaId)) {
            mochilaInv.setContents(mochilasCache.get(mochilaId));
        } else {
            try {
                ItemStack[] items = cargarMochila(mochilaId);
                if (items != null) {
                    mochilaInv.setContents(items);
                    mochilasCache.put(mochilaId, items);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        player.playSound(player.getLocation(), Sound.ITEM_BUNDLE_DROP_CONTENTS, 2.0f, 1.0f);
        player.openInventory(mochilaInv);
    }

    private void usarGancho(Player player, ItemStack gancho) {
        UUID playerId = player.getUniqueId();
        if (cooldownGancho.contains(playerId)) {
            return;
        }

        // Aplicar cooldown
        cooldownGancho.add(playerId);
        player.setCooldown(Material.FISHING_ROD, 40);
        new BukkitRunnable() {
            @Override
            public void run() {
                cooldownGancho.remove(playerId);
            }
        }.runTaskLater(plugin, 40);

        // Reducir durabilidad
        if (gancho.getDurability() < gancho.getType().getMaxDurability()) {
            gancho.setDurability((short) (gancho.getDurability() + 1));
        } else {
            player.getInventory().removeItem(gancho);
            player.sendMessage(ChatColor.RED + "¡Tu gancho se ha roto!");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        }

        // Lanzar al jugador
        Vector direction = player.getLocation().getDirection().normalize().multiply(1.5);
        player.setVelocity(direction);
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.5f);
    }

    private void usarYunque(Player player, ItemStack yunque, double porcentaje) {
        repararArmadura(player, porcentaje);

        // Consumir el yunque
        if (yunque.getAmount() > 1) {
            yunque.setAmount(yunque.getAmount() - 1);
        } else {
            player.getInventory().removeItem(yunque);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 2.0f, 2.0f);
    }

    private void usarManzanaPanico(Player player, ItemStack manzana) {
        EconomyItems.applyPanicAppleEffects(player);

        // Consumir la manzana
        if (manzana.getAmount() > 1) {
            manzana.setAmount(manzana.getAmount() - 1);
        } else {
            player.getInventory().removeItem(manzana);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
    }

    private void repararArmadura(Player player, double porcentaje) {
        ItemStack[] armadura = player.getInventory().getArmorContents();

        for (ItemStack item : armadura) {
            if (item == null || item.getType() == Material.AIR) continue;

            if (item.getDurability() > 0) {
                short nuevaDurabilidad = (short) (item.getDurability() - (item.getType().getMaxDurability() * porcentaje));
                item.setDurability((short) Math.max(0, nuevaDurabilidad));
            }
        }

        player.getInventory().setArmorContents(armadura);
    }

    // Manejo de mochilas
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        String title = event.getView().getTitle();
        if (!title.startsWith("Mochila ")) return;

        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (mochilasAbiertas.containsKey(playerId)) {
            String mochilaId = mochilasAbiertas.get(playerId);
            ItemStack[] contents = event.getInventory().getContents();

            // Actualizar cache
            mochilasCache.put(mochilaId, contents);

            // Guardar en base de datos async
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    guardarMochila(mochilaId, contents);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });

            mochilasAbiertas.remove(playerId);
            player.playSound(player.getLocation(), Sound.ITEM_BUNDLE_REMOVE_ONE, 2.0f, 1.0f);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.startsWith("Mochila ")) {
            // Prevenir mover mochilas dentro de mochilas
            if (event.getCurrentItem() != null && isMochila(event.getCurrentItem())) {
                event.setCancelled(true);
            }
            if (event.getCursor() != null && isMochila(event.getCursor())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (mochilasAbiertas.containsKey(playerId)) {
            String mochilaId = mochilasAbiertas.get(playerId);
            Inventory inv = player.getOpenInventory().getTopInventory();

            if (inv != null) {
                ItemStack[] contents = inv.getContents();
                mochilasCache.put(mochilaId, contents);

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        guardarMochila(mochilaId, contents);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
            }

            mochilasAbiertas.remove(playerId);
        }
    }

    // Métodos para manejar IDs de mochilas
    private String getMochilaId(ItemStack mochila) {
        ItemMeta meta = mochila.getItemMeta();
        if (meta == null) return null;
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return null;

        // Buscar línea con el ID
        for (String line : lore) {
            if (line.startsWith(ChatColor.DARK_PURPLE + "ID:")) {
                return line.substring(line.indexOf(":") + 2).trim();
            }
        }
        return null;
    }

    private void setMochilaId(ItemStack mochila, String id) {
        ItemMeta meta = mochila.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();

        // Añadir o actualizar línea de ID
        boolean found = false;
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).startsWith(ChatColor.DARK_PURPLE + "ID:")) {
                lore.set(i, ChatColor.DARK_PURPLE + "ID: " + id);
                found = true;
                break;
            }
        }

        if (!found) {
            lore.add(ChatColor.DARK_PURPLE + "ID: " + id);
        }

        meta.setLore(lore);
        mochila.setItemMeta(meta);
    }

    // Métodos de base de datos mejorados
    private ItemStack[] cargarMochila(String mochilaId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT items FROM mochilas WHERE id = ?")) {
            ps.setString(1, mochilaId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return ItemSerializer.deserialize(rs.getString("items"));
            }
        }
        return new ItemStack[27]; // Mochila vacía si no existe
    }

    private void guardarMochila(String mochilaId, ItemStack[] items) throws SQLException {
        if (connection == null || connection.isClosed()) {
            setupDatabase(); // Reconectar si es necesario
        }

        String serialized = ItemSerializer.serialize(items);
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO mochilas (id, items) VALUES (?, ?)")) {
            ps.setString(1, mochilaId);
            ps.setString(2, serialized);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al guardar mochila " + mochilaId + ": " + e.getMessage());
            throw e;
        }
    }

    public void onDisable() {
        plugin.getLogger().info("Guardando mochilas...");

        // Guardar todas las mochilas abiertas primero
        for (Map.Entry<UUID, String> entry : mochilasAbiertas.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                Inventory inv = player.getOpenInventory().getTopInventory();
                if (inv != null) {
                    mochilasCache.put(entry.getValue(), inv.getContents());
                }
            }
        }

        // Guardar cache
        for (Map.Entry<String, ItemStack[]> entry : mochilasCache.entrySet()) {
            try {
                guardarMochila(entry.getKey(), entry.getValue());
            } catch (SQLException e) {
                plugin.getLogger().warning("Error al guardar mochila " + entry.getKey() + ": " + e.getMessage());
            }
        }

        // Cerrar conexión
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al cerrar conexión: " + e.getMessage());
        }

        plugin.getLogger().info("Mochilas guardadas correctamente");
    }
}