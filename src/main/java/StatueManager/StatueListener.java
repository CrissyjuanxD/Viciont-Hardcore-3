package StatueManager;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatueListener implements Listener {

    private final StatueManager manager;
    private final StatueGUI gui;
    private final StatueSchematic schematic;
    private final JavaPlugin plugin;

    private final Map<UUID, Long> pickaxeCooldowns = new HashMap<>();

    public StatueListener(JavaPlugin plugin, StatueManager manager, StatueGUI gui, StatueSchematic schema) {
        this.plugin = plugin;
        this.manager = manager;
        this.gui = gui;
        this.schematic = schema;
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
        stand.getPersistentDataContainer().set(
                org.bukkit.NamespacedKey.fromString("viciont:statue_id"),
                PersistentDataType.STRING,
                "true"
        );

        manager.registerStatue(stand); // registra, aplica nombre y partículas

        if (p.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
        p.playSound(loc, Sound.ENTITY_ARMOR_STAND_PLACE, 1f, 1f);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  BLOQUEAR EQUIPAR ITEMS / ARMOR EN ARMOR STANDS DEL PLUGIN
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        if (StatueData.isStatue(e.getRightClicked())) {
            e.setCancelled(true);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  DAÑO Y DESTRUCCIÓN
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof ArmorStand)) return;
        ArmorStand stand = (ArmorStand) e.getEntity();
        if (!StatueData.isStatue(stand)) return;

        e.setCancelled(true);

        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();

        StatueData data = new StatueData(stand);

        if (data.isInvulnerable()) {
            if (p.getGameMode() == GameMode.CREATIVE && p.isSneaking()) {
                p.sendMessage(ChatColor.RED + "Estatua indestructible eliminada por Admin.");
                removeStatue(stand, p);
            }
            return;
        }

        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!hand.getType().name().contains("PICKAXE") && p.getGameMode() != GameMode.CREATIVE) {
            return;
        }

        if (p.getGameMode() != GameMode.CREATIVE) {
            long now = System.currentTimeMillis();
            long last = pickaxeCooldowns.getOrDefault(p.getUniqueId(), 0L);
            long cooldownMs = 1000L;

            if (now - last < cooldownMs) {
                return;
            }
            pickaxeCooldowns.put(p.getUniqueId(), now);
            p.setCooldown(hand.getType(), 20);
        }

        int currentHp = data.getHpCurrent() - 1;

        stand.getWorld().playSound(stand.getLocation(), Sound.ITEM_MACE_SMASH_GROUND, SoundCategory.BLOCKS, 1.0F, 2.0F);

        Location hitLoc = stand.getLocation().add(0, 1, 0);
        stand.getWorld().spawnParticle(Particle.BLOCK, hitLoc, 25, 0.3, 0.4, 0.3, 0.1, Material.STONE.createBlockData());
        stand.getWorld().spawnParticle(Particle.BLOCK, hitLoc, 25, 0.3, 0.4, 0.3, 0.1, Material.BLACKSTONE.createBlockData());

        if (currentHp <= 0) {
            removeStatue(stand, p);
        } else {
            data.setHpCurrent(currentHp);
            spawnDamageDisplay(stand.getLocation(), currentHp, data.getHpMax());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  ANIMACIÓN DE DAÑO (TextDisplay Flotante)
    // ──────────────────────────────────────────────────────────────────────────

    private void spawnDamageDisplay(Location statueLoc, int currentHp, int maxHp) {
        Location spawnLoc = statueLoc.clone().add(0, 2.3, 0);

        TextDisplay display = (TextDisplay) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.TEXT_DISPLAY);

        display.setBillboard(Display.Billboard.CENTER);
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        display.setShadowed(true);
        display.setViewRange(24.0f / 64.0f);

        String col1 = ChatColor.of("#67B3E0") + "" + ChatColor.BOLD;
        String col2 = ChatColor.of("#DBECF2") + "" + ChatColor.BOLD;
        String col3 = ChatColor.of("#43A0DE") + "" + ChatColor.BOLD;

        String formatText = "§f\uDB80\uDC67 " + col1 + currentHp + col2 + "/" + col3 + maxHp;
        display.setText(formatText);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 40;

            @Override
            public void run() {
                if (display.isDead() || !display.isValid()) {
                    cancel();
                    return;
                }

                ticks++;

                if (ticks > maxTicks) {
                    display.remove();
                    cancel();
                    return;
                }

                Transformation transform = display.getTransformation();
                transform.getTranslation().add(0f, 0.02f, 0f);

                display.setInterpolationDelay(0);
                display.setInterpolationDuration(2);
                display.setTransformation(transform);

                int fadeStart = maxTicks - 20;
                if (ticks > fadeStart) {
                    int ticksLeft = maxTicks - ticks;
                    float progress = Math.max(0, ticksLeft / 20.0f);
                    byte textOpacity = (byte) (255 * progress);
                    display.setTextOpacity(textOpacity);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void removeStatue(ArmorStand stand, Player p) {
        Location loc = stand.getLocation().add(0, 1, 0);

        stand.getWorld().spawnParticle(
                Particle.BLOCK,
                loc,
                80,
                0.3, 0.4, 0.3,
                0.15,
                Material.SMOOTH_STONE.createBlockData()
        );

        stand.getWorld().playSound(loc, Sound.BLOCK_DEEPSLATE_BREAK, 1f, 0.8f);
        stand.getWorld().playSound(loc, Sound.ITEM_SHIELD_BREAK, 1.0F, 0.75F);
        stand.getWorld().playSound(loc, Sound.ITEM_TRIDENT_THUNDER, 1.0F, 0.75F);

        schematic.onStatueRemoved(stand.getUniqueId());
        manager.unregisterStatue(stand);
        stand.remove();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        for (Entity ent : e.getChunk().getEntities()) {
            if (ent instanceof ArmorStand && StatueData.isStatue((ArmorStand) ent)) {
                manager.registerStatue((ArmorStand) ent);
            }
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        for (Entity ent : e.getChunk().getEntities()) {
            if (ent instanceof ArmorStand && StatueData.isStatue((ArmorStand) ent)) {
                manager.unregisterStatue((ArmorStand) ent);
            }
        }
    }
}