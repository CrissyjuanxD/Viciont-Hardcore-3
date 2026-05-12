package StatueManager;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class StatueListener implements Listener {

    private final StatueManager manager;
    private final StatueGUI gui;

    public StatueListener(StatueManager manager, StatueGUI gui) {
        this.manager = manager;
        this.gui = gui;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = e.getItem();
        if (!StatueData.isStatueItem(item)) return;

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (e.getPlayer().isSneaking()) {
                e.setCancelled(true);
                gui.openConfigGUI(e.getPlayer(), item);
            } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                e.setCancelled(true);
                spawnStatue(e.getPlayer(), item, e.getClickedBlock().getLocation().add(0.5, 1, 0.5));
            }
        }
    }

    private void spawnStatue(Player p, ItemStack item, Location loc) {
        ItemMeta meta = item.getItemMeta();
        StatueData itemData = new StatueData(meta);

        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setGravity(false);
        stand.setBasePlate(false);
        stand.setArms(true);
        stand.setCustomName(ChatColor.translateAlternateColorCodes('&', "&6&lStatue Effect"));
        stand.setCustomNameVisible(false);

        // Configurar Datos
        StatueData standData = new StatueData(stand);
        standData.setRadiusX(itemData.getRadiusX());
        standData.setRadiusY(itemData.getRadiusY());
        standData.setGlowColor(itemData.getGlowColor());
        standData.setHpMax(itemData.getHpMax());
        standData.setHpCurrent(itemData.getHpMax());
        standData.setVisible(itemData.isVisible());
        standData.setInvulnerable(itemData.isInvulnerable());

        // Conservamos el modo AntiGrief o Efecto
        if (itemData.isAntiGrief()) {
            standData.setAntiGrief(true);
        } else {
            standData.setEffect(itemData.getEffectType(), itemData.getEffectAmplifier());
        }

        // Aplicar propiedades visuales inmediatas
        stand.setVisible(itemData.isVisible());

        // Marca obligatoria para identificarla
        stand.getPersistentDataContainer().set(org.bukkit.NamespacedKey.fromString("viciont:statue_id"), org.bukkit.persistence.PersistentDataType.STRING, "true");

        manager.registerStatue(stand);

        if (p.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
        p.playSound(loc, Sound.ENTITY_ARMOR_STAND_PLACE, 1f, 1f);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof ArmorStand)) return;
        ArmorStand stand = (ArmorStand) e.getEntity();
        if (!StatueData.isStatue(stand)) return;

        // Cancelamos daño vanilla siempre
        e.setCancelled(true);

        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();

        StatueData data = new StatueData(stand);

        // LÓGICA DE INVULNERABILIDAD
        if (data.isInvulnerable()) {
            if (p.getGameMode() == GameMode.CREATIVE && p.isSneaking() && p.hasPermission("viciont.admin")) {
                p.sendMessage(ChatColor.RED + "Estatua indestructible eliminada por Admin.");
                removeStatue(stand, p);
            } else {
                p.sendMessage(ChatColor.RED + "Esta estatua es indestructible.");
            }
            return;
        }

        // Lógica normal de daño
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!hand.getType().name().contains("PICKAXE") && p.getGameMode() != GameMode.CREATIVE) {
            return;
        }

        int currentHp = data.getHpCurrent() - 1;

        p.playSound(stand.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 0.5f);
        p.playSound(stand.getLocation(), Sound.BLOCK_NETHERITE_BLOCK_BREAK, 1f, 1f);

        if (currentHp <= 0) {
            removeStatue(stand, p);
        } else {
            data.setHpCurrent(currentHp);
            p.sendMessage(ChatColor.GRAY + "Vida Estatua: " + currentHp + "/" + data.getHpMax());
        }
    }

    private void removeStatue(ArmorStand stand, Player p) {
        p.playSound(stand.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
        manager.unregisterStatue(stand);
        stand.remove();
    }

    @EventHandler
    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent e) {
        for (Entity ent : e.getChunk().getEntities()) {
            if (ent instanceof ArmorStand && StatueData.isStatue((ArmorStand) ent)) {
                manager.registerStatue((ArmorStand) ent);
            }
        }
    }

    @EventHandler
    public void onChunkUnload(org.bukkit.event.world.ChunkUnloadEvent e) {
        for (Entity ent : e.getChunk().getEntities()) {
            if (ent instanceof ArmorStand && StatueData.isStatue((ArmorStand) ent)) {
                manager.unregisterStatue((ArmorStand) ent);
            }
        }
    }
}