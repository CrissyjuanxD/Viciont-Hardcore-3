package StatueManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class StatueGUI implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, ItemStack> editors = new HashMap<>();
    private final Map<UUID, String> chatInputMode = new HashMap<>();

    public StatueGUI(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void openConfigGUI(Player player, ItemStack item) {
        editors.put(player.getUniqueId(), item);
        Inventory inv = Bukkit.createInventory(null, 36, ChatColor.DARK_AQUA + "Configurar Estatua");

        StatueData data = new StatueData(item.getItemMeta());

        // Fila 1
        inv.setItem(10, createIcon(Material.BEACON, "Radio (Ancho)", "" + data.getRadiusX()));
        inv.setItem(11, createIcon(Material.IRON_BARS, "Radio (Alto)", "" + data.getRadiusY()));

        String colorName = (data.getGlowColor() == null) ? "DESACTIVADO" : data.getGlowColor().name();
        inv.setItem(12, createIcon(Material.GLOW_INK_SAC, "Color Glowing", colorName));

        // Fila 2 (Tipo de Estatua: Efecto vs AntiGrief)
        if (data.isAntiGrief()) {
            inv.setItem(19, createIcon(Material.SHIELD, "Modo: ANTI-GRIEF", "Protege bloques de explosiones y mobs"));
            inv.setItem(20, createIcon(Material.BARRIER, "Nivel Efecto", "N/A (Modo Anti-Grief activo)"));
        } else {
            String effectName = data.getEffectType() != null ? data.getEffectType().getName() : "NINGUNO";
            inv.setItem(19, createIcon(Material.POTION, "Modo: EFECTO DE POCIÓN", effectName + " (Click para cambiar / escribir)"));
            inv.setItem(20, createIcon(Material.BREWING_STAND, "Nivel Efecto", "Nivel: " + (data.getEffectAmplifier() + 1)));
        }

        // Botón para alternar entre AntiGrief y Poción
        inv.setItem(28, createIcon(Material.COMMAND_BLOCK, "Cambiar Tipo de Estatua", "Click para alternar (Poción <-> AntiGrief)"));

        // Fila 2 (Propiedades)
        inv.setItem(15, createIcon(Material.ANVIL, "Vida (Golpes)", "" + data.getHpMax()));

        inv.setItem(23, createIcon(data.isVisible() ? Material.ENDER_EYE : Material.ENDER_PEARL,
                "Visibilidad", data.isVisible() ? "§aVISIBLE" : "§cINVISIBLE"));

        inv.setItem(24, createIcon(data.isInvulnerable() ? Material.BEDROCK : Material.GLASS,
                "Invulnerabilidad", data.isInvulnerable() ? "§aINDESTRUCTIBLE" : "§cVULNERABLE"));

        // Guardar
        inv.setItem(31, createIcon(Material.NETHER_STAR, "GUARDAR Y SALIR", "Click para aplicar cambios"));

        player.openInventory(inv);
    }

    private ItemStack createIcon(Material mat, String name, String val) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + name);
        meta.setLore(Collections.singletonList(ChatColor.WHITE + "Estado: " + ChatColor.GREEN + val));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(ChatColor.DARK_AQUA + "Configurar Estatua")) return;
        e.setCancelled(true);

        Player p = (Player) e.getWhoClicked();
        ItemStack currentItem = e.getCurrentItem();
        if (currentItem == null) return;

        ItemStack handItem = editors.get(p.getUniqueId());
        if (handItem == null) { p.closeInventory(); return; }

        ItemMeta meta = handItem.getItemMeta();
        StatueData data = new StatueData(meta);
        boolean save = false;

        switch (e.getSlot()) {
            case 10: // Radio X
                p.closeInventory();
                p.sendMessage(ChatColor.GREEN + "Escribe el radio X en el chat:");
                chatInputMode.put(p.getUniqueId(), "RAD_X");
                break;
            case 11: // Radio Y
                p.closeInventory();
                p.sendMessage(ChatColor.GREEN + "Escribe el radio Y en el chat:");
                chatInputMode.put(p.getUniqueId(), "RAD_Y");
                break;
            case 12: // Color Cycle + OFF
                ChatColor[] colors = {ChatColor.RED, ChatColor.BLUE, ChatColor.GREEN, ChatColor.YELLOW, ChatColor.WHITE, ChatColor.GOLD, ChatColor.LIGHT_PURPLE, ChatColor.AQUA};
                ChatColor current = data.getGlowColor();

                if (current == null) {
                    data.setGlowColor(colors[0]); // De OFF pasa al primero
                } else {
                    int idx = -1;
                    for(int i=0; i<colors.length; i++) if(colors[i] == current) idx = i;

                    if (idx == colors.length - 1) {
                        data.setGlowColor(null); // Del ultimo pasa a OFF
                    } else {
                        data.setGlowColor(colors[(idx + 1) % colors.length]);
                    }
                }
                save = true;
                break;
            case 19: // Efecto Cycle + CHAT (Solo si no es Anti-Grief)
                if (data.isAntiGrief()) {
                    p.sendMessage(ChatColor.RED + "Desactiva el modo Anti-Grief para añadir efectos.");
                    break;
                }

                PotionEffectType[] types = {PotionEffectType.SPEED, PotionEffectType.STRENGTH, PotionEffectType.REGENERATION, PotionEffectType.RESISTANCE, PotionEffectType.FIRE_RESISTANCE};

                // Si hace click derecho, escribe en chat
                if (e.getClick().isRightClick()) {
                    p.closeInventory();
                    p.sendMessage(ChatColor.GREEN + "Escribe el NOMBRE del efecto en el chat (ej: SATURATION, ABSORPTION, LUCK):");
                    chatInputMode.put(p.getUniqueId(), "EFF_NAME");
                } else {
                    // Click normal cicla los comunes
                    PotionEffectType curEff = data.getEffectType();
                    int idy = 0;
                    if (curEff != null) {
                        for(int i=0; i<types.length; i++) if(types[i].equals(curEff)) idy = i;
                    }
                    PotionEffectType nextEff = types[(idy + 1) % types.length];
                    data.setEffect(nextEff, data.getEffectAmplifier());
                    save = true;
                }
                break;
            case 20: // Amplifier (Solo si no es AntiGrief)
                if (data.isAntiGrief()) break;

                p.closeInventory();
                p.sendMessage(ChatColor.GREEN + "Escribe el NIVEL del efecto (1, 2, 3...):");
                chatInputMode.put(p.getUniqueId(), "EFF_AMP");
                break;
            case 28: // Alternar Modo
                if (data.isAntiGrief()) {
                    data.setAntiGrief(false);
                    data.setEffect(PotionEffectType.SPEED, 0); // Vuelve a un efecto base
                } else {
                    data.setAntiGrief(true); // Esto borra el efecto automáticamente en la clase de datos
                }
                save = true;
                break;
            case 15: // Vida
                p.closeInventory();
                p.sendMessage(ChatColor.GREEN + "Escribe la vida máxima:");
                chatInputMode.put(p.getUniqueId(), "HP");
                break;
            case 23: // Visibilidad Toggle
                data.setVisible(!data.isVisible());
                save = true;
                break;
            case 24: // Invulnerabilidad Toggle
                data.setInvulnerable(!data.isInvulnerable());
                save = true;
                break;
            case 31: // GUARDAR
                p.closeInventory();
                p.sendMessage(ChatColor.GREEN + "Configuración guardada.");
                updateLore(handItem);
                editors.remove(p.getUniqueId());
                break;
        }

        if (save) {
            handItem.setItemMeta(meta);
            openConfigGUI(p, handItem);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!chatInputMode.containsKey(p.getUniqueId())) return;

        e.setCancelled(true);
        String mode = chatInputMode.remove(p.getUniqueId());
        String msg = e.getMessage();
        ItemStack item = editors.get(p.getUniqueId());

        if (item == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemMeta meta = item.getItemMeta();
            StatueData data = new StatueData(meta);
            try {
                if (mode.equals("RAD_X")) data.setRadiusX(Double.parseDouble(msg));
                if (mode.equals("RAD_Y")) data.setRadiusY(Double.parseDouble(msg));
                if (mode.equals("HP")) {
                    int hp = Integer.parseInt(msg);
                    data.setHpMax(hp);
                    data.setHpCurrent(hp);
                }
                if (mode.equals("EFF_AMP")) {
                    int lvl = Integer.parseInt(msg);
                    data.setEffectAmplifier(Math.max(0, lvl - 1));
                }
                if (mode.equals("EFF_NAME")) {
                    PotionEffectType type = PotionEffectType.getByName(msg.toUpperCase());
                    if (type != null) {
                        data.setEffect(type, data.getEffectAmplifier());
                        p.sendMessage(ChatColor.GREEN + "Efecto establecido: " + type.getName());
                    } else {
                        p.sendMessage(ChatColor.RED + "Efecto no encontrado. Usa nombres en inglés (ej: BLINDNESS, LUCK).");
                    }
                }
            } catch (NumberFormatException ex) {
                p.sendMessage(ChatColor.RED + "Valor inválido. Debe ser un número.");
            }
            item.setItemMeta(meta);
            openConfigGUI(p, item);
        });
    }

    private void updateLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        StatueData data = new StatueData(meta);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Radio: " + ChatColor.WHITE + data.getRadiusX() + "x" + data.getRadiusY());
        lore.add(ChatColor.GRAY + "Vida: " + ChatColor.WHITE + (data.isInvulnerable() ? "INFINITA" : data.getHpMax()));

        if (data.isAntiGrief()) {
            lore.add(ChatColor.GRAY + "Modo: " + ChatColor.AQUA + "ANTI-GRIEF ZONA");
        } else {
            String eff = data.getEffectType() != null ? data.getEffectType().getName() + " " + (data.getEffectAmplifier()+1) : "N/A";
            lore.add(ChatColor.GRAY + "Modo: " + ChatColor.AQUA + eff);
        }

        String color = (data.getGlowColor() == null) ? "OFF" : (data.getGlowColor() + data.getGlowColor().name());
        lore.add(ChatColor.GRAY + "Color: " + color);

        if (data.isInvulnerable()) lore.add(ChatColor.GOLD + ">> INDESTRUCTIBLE <<");
        if (!data.isVisible()) lore.add(ChatColor.DARK_GRAY + ">> INVISIBLE <<");

        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}