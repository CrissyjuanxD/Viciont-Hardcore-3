package RunicSmithing;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class RunicSmithingGUI implements Listener {

    private final JavaPlugin plugin;
    private final String GUI_TITLE = "\u3201\u3201" + ChatColor.WHITE + "\u3203";
    private final Map<Location, BukkitRunnable> particleTasks = new HashMap<>();
    private final Map<Location, ItemDisplay> activeDisplays = new HashMap<>();

    private final int[] INPUT_SLOTS = {1, 3, 5, 7};
    private final int OUTPUT_SLOT = 22;

    public RunicSmithingGUI(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openRunicGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);
        ItemStack filler = createGrayPane();

        for (int i = 0; i < 27; i++) {
            if (i != 1 && i != 3 && i != 5 && i != 7 && i != OUTPUT_SLOT) {
                gui.setItem(i, filler);
            }
        }
        player.openInventory(gui);
    }

    private ItemStack createGrayPane() {
        ItemStack pane = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.setCustomModelData(2);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    // --- INVENTORY LOGIC ---

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        // 1. Bloqueo preventivo global
        // Esto asegura que nada entre en slots decorativos o el 22 accidentalmente
        event.setCancelled(true);

        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();
        Inventory topInv = event.getView().getTopInventory();

        // -----------------------------------------------------------
        // CASO 1: INTERACCIÓN EN EL INVENTARIO DEL JUGADOR
        // -----------------------------------------------------------
        if (clickedInv.getType() == InventoryType.PLAYER) {

            // Si es clic normal, dejamos que ordene su inventario
            if (!event.isShiftClick()) {
                event.setCancelled(false);
                return;
            }

            // Si es SHIFT-CLICK: Hacemos el movimiento MANUALMENTE hacia 1, 3, 5, 7
            // event.setCancelled(true) ya está activo arriba, así que Bukkit no hará nada.
            // Nosotros lo hacemos:

            ItemStack current = event.getCurrentItem();
            if (current == null || current.getType() == Material.AIR) return;

            boolean moved = false;
            int amountLeft = current.getAmount();

            // Intentar llenar los slots permitidos (1, 3, 5, 7) en orden
            for (int targetSlot : INPUT_SLOTS) {
                ItemStack itemInSlot = topInv.getItem(targetSlot);

                // A) Slot vacío: Colocar todo lo que queda
                if (itemInSlot == null || itemInSlot.getType() == Material.AIR) {
                    ItemStack toPlace = current.clone();
                    toPlace.setAmount(amountLeft);
                    topInv.setItem(targetSlot, toPlace);

                    amountLeft = 0;
                    moved = true;
                    break; // Ya colocamos todo
                }
                // B) Slot con mismo ítem: Intentar stackear
                else if (itemInSlot.isSimilar(current) && itemInSlot.getAmount() < itemInSlot.getMaxStackSize()) {
                    int space = itemInSlot.getMaxStackSize() - itemInSlot.getAmount();
                    int toAdd = Math.min(space, amountLeft);

                    itemInSlot.setAmount(itemInSlot.getAmount() + toAdd);
                    amountLeft -= toAdd;

                    moved = true;

                    if (amountLeft <= 0) break; // Se acabó el ítem
                }
            }

            // Si logramos mover algo, actualizamos el ítem del jugador y la receta
            if (moved) {
                if (amountLeft > 0) {
                    current.setAmount(amountLeft); // Reducir cantidad
                } else {
                    event.setCurrentItem(null); // Eliminar ítem
                }

                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);

                // Actualizar resultado
                new BukkitRunnable() {
                    @Override public void run() { updateResult(topInv); }
                }.runTask(plugin);
            }
            return;
        }

        // -----------------------------------------------------------
        // CASO 2: CLIC EN SLOTS DE INPUT (1, 3, 5, 7)
        // -----------------------------------------------------------
        if (slot == 1 || slot == 3 || slot == 5 || slot == 7) {
            event.setCancelled(false); // Permitir interacción normal
            new BukkitRunnable() {
                @Override public void run() { updateResult(topInv); }
            }.runTask(plugin);
        }

        // -----------------------------------------------------------
        // CASO 3: CRAFTING / OUTPUT (Slot 22)
        // -----------------------------------------------------------
        if (slot == OUTPUT_SLOT) {
            // El evento está cancelado (true), así que NO se puede poner nada aquí.
            // Solo gestionamos si el jugador intenta SACAR algo.

            ItemStack result = topInv.getItem(OUTPUT_SLOT);

            if (result != null && result.getType() != Material.AIR) {

                // Validar que la receta exista antes de dar el premio
                RunicRecipe match = RunicManager.matchRecipe(
                        topInv.getItem(1), topInv.getItem(3),
                        topInv.getItem(5), topInv.getItem(7)
                );

                if (match != null) {
                    // Lógica para SACAR el ítem
                    boolean success = false;

                    if (event.isShiftClick()) {
                        // Shift-Click: Al inventario
                        HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(match.getResult());
                        if (leftOver.isEmpty()) {
                            success = true;
                        } else {
                            player.sendMessage(ChatColor.RED + "Inventario lleno.");
                        }
                    } else {
                        // Click Normal: Al cursor
                        if (event.getCursor() == null || event.getCursor().getType() == Material.AIR) {
                            player.setItemOnCursor(match.getResult());
                            success = true;
                        }
                    }

                    if (success) {
                        consumeItems(topInv, match);
                        topInv.setItem(OUTPUT_SLOT, null); // Limpiar visualmente

                        // Efectos
                        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1.0f, 1.0f);
                        player.spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);

                        // Disparar Evento
                        RunicCraftEvent craftEvent = new RunicCraftEvent(player, match.getResult(), match);
                        Bukkit.getPluginManager().callEvent(craftEvent);

                        // Recalcular
                        new BukkitRunnable() {
                            @Override public void run() { updateResult(topInv); }
                        }.runTask(plugin);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(GUI_TITLE)) {
            // Solo permitir drag en inputs
            for (int slot : event.getRawSlots()) {
                if (slot != 1 && slot != 3 && slot != 5 && slot != 7 && slot >= 54) {
                    event.setCancelled(true);
                    return;
                }
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateResult(event.getView().getTopInventory());
                }
            }.runTask(plugin);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(GUI_TITLE)) {
            Inventory inv = event.getInventory();
            Player player = (Player) event.getPlayer();

            for (int slot : INPUT_SLOTS) {
                ItemStack item = inv.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    player.getInventory().addItem(item).forEach((k, v) ->
                            player.getWorld().dropItemNaturally(player.getLocation(), v));
                }
            }
        }
    }

    // --- RECIPE PROCESSING ---

    private void updateResult(Inventory inv) {
        RunicRecipe match = RunicManager.matchRecipe(
                inv.getItem(1), inv.getItem(3),
                inv.getItem(5), inv.getItem(7)
        );
        if (match != null) {
            inv.setItem(OUTPUT_SLOT, match.getResult());
        } else {
            inv.setItem(OUTPUT_SLOT, null);
        }
    }

    private void consumeItems(Inventory inv, RunicRecipe recipe) {
        for (int slot : INPUT_SLOTS) {
            ItemStack required = recipe.getIngredients().get(slot);
            if (required != null) {
                ItemStack current = inv.getItem(slot);
                if (current != null) {
                    int newAmount = current.getAmount() - required.getAmount();
                    if (newAmount > 0) {
                        current.setAmount(newAmount);
                        inv.setItem(slot, current);
                    } else {
                        inv.setItem(slot, null);
                    }
                }
            }
        }
    }

    // --- BLOCK & PARTICLE LOGIC ---
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.PINK_GLAZED_TERRACOTTA) {
            startParticles(event.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.PINK_GLAZED_TERRACOTTA) {
            Location loc = event.getBlock().getLocation();

            // 1. ELIMINAR EL ITEM DISPLAY INMEDIATAMENTE
            if (activeDisplays.containsKey(loc)) {
                ItemDisplay display = activeDisplays.get(loc);
                if (display != null && display.isValid()) {
                    display.remove(); // Bye bye antorcha
                }
                activeDisplays.remove(loc);
            }

            // 2. Cancelar la tarea de partículas
            if (particleTasks.containsKey(loc)) {
                particleTasks.get(loc).cancel();
                particleTasks.remove(loc);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.PINK_GLAZED_TERRACOTTA) {
                event.setCancelled(true);
                openRunicGUI(event.getPlayer());
            }
        }
    }

    private void startParticles(Location loc) {

        if (particleTasks.containsKey(loc)) return;

        // --- 1. CREAR EL ITEM DISPLAY (Antorcha Estática) ---
        Location center = loc.clone().add(0.5, 0.5, 0.5);
        ItemDisplay display = (ItemDisplay) loc.getWorld().spawnEntity(center, EntityType.ITEM_DISPLAY);

        display.setItemStack(new ItemStack(Material.SOUL_TORCH));
        display.setBillboard(Display.Billboard.FIXED);
        display.setPersistent(false);

        activeDisplays.put(loc, display);

        // --- COLORES ---
        Particle.DustOptions grayDust = new Particle.DustOptions(Color.fromRGB(150, 150, 150), 1.0f);
        Particle.DustOptions cyanDust = new Particle.DustOptions(Color.fromRGB(0, 128, 128), 1.0f);

        BukkitRunnable task = new BukkitRunnable() {
            double angle = 0;
            double yDown = 1.2;
            double yUp = 0.0;

            @Override
            public void run() {
                if (loc.getBlock().getType() != Material.PINK_GLAZED_TERRACOTTA) {
                    if (display.isValid()) display.remove();
                    activeDisplays.remove(loc);
                    this.cancel();
                    particleTasks.remove(loc);
                    return;
                }

                if (!display.isValid()) {
                    this.cancel();
                    particleTasks.remove(loc);
                    activeDisplays.remove(loc);
                    startParticles(loc);
                    return;
                }

                double radius = 0.75;
                double centerX = loc.getX() + 0.5;
                double centerZ = loc.getZ() + 0.5;

                // Espiral Bajada
                double x1 = centerX + radius * Math.cos(angle);
                double z1 = centerZ + radius * Math.sin(angle);
                Location locDown = new Location(loc.getWorld(), x1, loc.getY() + yDown, z1);
                loc.getWorld().spawnParticle(Particle.DUST, locDown, 1, 0, 0, 0, 0, grayDust);
                loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, locDown, 1, 0, 0, 0, 0);

                // Espiral Subida (Opuesto)
                double x2 = centerX + radius * Math.cos(angle + Math.PI);
                double z2 = centerZ + radius * Math.sin(angle + Math.PI);
                Location locUp = new Location(loc.getWorld(), x2, loc.getY() + yUp, z2);
                loc.getWorld().spawnParticle(Particle.DUST, locUp, 1, 0, 0, 0, 0, cyanDust);
                loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, locUp, 1, 0, 0, 0, 0);

                // Actualizar variables
                angle += 0.2;
                yDown -= 0.05; if (yDown < 0) yDown = 1.2;
                yUp += 0.05;   if (yUp > 1.2) yUp = 0.0;
            }
        };
        task.runTaskTimer(plugin, 0, 2);
        particleTasks.put(loc, task);
    }

    public void restoreParticles() {}
}