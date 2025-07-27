package Dificultades.CustomMobs;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomBoat implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey boatKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey fuelKey;
    private File fuelFile;
    private FileConfiguration fuelData;

    private final Map<UUID, BossBar> fuelBars = new HashMap<>();
    private final Map<UUID, Integer> actionBarTasks = new HashMap<>();
    private final Map<UUID, Boolean> lastSprintState = new HashMap<>();

    public CustomBoat(JavaPlugin plugin) {
        this.plugin = plugin;
        this.boatKey = new NamespacedKey(plugin, "custom_boat");
        this.ownerKey = new NamespacedKey(plugin, "boat_owner");
        this.fuelKey = new NamespacedKey(plugin, "boat_fuel");

        this.fuelFile = new File(plugin.getDataFolder(), "fuel.yml");
        if (!fuelFile.exists()) {
            try {
                fuelFile.getParentFile().mkdirs();
                fuelFile.createNewFile();
                this.fuelData = YamlConfiguration.loadConfiguration(fuelFile);
                fuelData.set("speed.overworld", 0.3);
                fuelData.set("speed.nether", 0.2);
                fuelData.set("speed.end", 0.2);
                fuelData.set("speed.corrupted_end", 0.5);
                fuelData.save(fuelFile);
            } catch (IOException e) {
                plugin.getLogger().severe("No se pudo crear el archivo fuel.yml: " + e.getMessage());
            }
        } else {
            this.fuelData = YamlConfiguration.loadConfiguration(fuelFile);
        }
    }

    public ItemStack createBoatItem(Player owner) {
        ItemStack boatItem = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = boatItem.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "Nave de " + owner.getName());
        meta.setCustomModelData(340);
        boatItem.setItemMeta(meta);

        return boatItem;
    }

    public ItemStack createFuelItem() {
        ItemStack fuelItem = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = fuelItem.getItemMeta();

        meta.setDisplayName(ChatColor.GREEN + "Combustible");
        meta.setCustomModelData(350);
        fuelItem.setItemMeta(meta);

        return fuelItem;
    }

    public Boat spawnBoat(Location location, Player owner) {
        Boat boat = (Boat) location.getWorld().spawnEntity(location, EntityType.BOAT);

        String boatName = ChatColor.GOLD + "Nave de " + owner.getName();
        boat.setCustomName(boatName);
        boat.setCustomNameVisible(true);
        boat.getPersistentDataContainer().set(boatKey, PersistentDataType.STRING, "custom_boat");
        boat.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
        double savedFuel = 0.1;
        if (fuelData != null) {
            savedFuel = fuelData.getDouble(boatName, 0.1);
        }
        boat.getPersistentDataContainer().set(fuelKey, PersistentDataType.DOUBLE, savedFuel);

        boat.setInvulnerable(true);

        return boat;
    }

    @EventHandler
    public void onBoatPlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ECHO_SHARD) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getCustomModelData() != 340) return;

        Player player = event.getPlayer();
        Location spawnLoc = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5, 0, 0.5);

        if (!meta.hasDisplayName()) {
            meta.setDisplayName(ChatColor.GOLD + "Nave de " + player.getName());
            item.setItemMeta(meta);
            player.getInventory().setItemInMainHand(item);
        }

        Boat boat = spawnBoat(spawnLoc, player);

        if (!player.getGameMode().equals(GameMode.CREATIVE)) {
            item.setAmount(item.getAmount() - 1);
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onBoatInteract(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof Boat)) return;

        Boat boat = (Boat) event.getRightClicked();
        if (!boat.getPersistentDataContainer().has(boatKey, PersistentDataType.STRING)) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getHand());

        if (item != null && item.getType() == Material.ECHO_SHARD && item.getItemMeta() != null &&
                item.getItemMeta().getCustomModelData() == 350) {
            event.setCancelled(true);

            String ownerIdStr = boat.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
            if (ownerIdStr == null || !ownerIdStr.equals(player.getUniqueId().toString())) {
                player.sendMessage(ChatColor.RED + "Solo el dueño puede repostar esta nave.");
                return;
            }

            double currentFuel = boat.getPersistentDataContainer().get(fuelKey, PersistentDataType.DOUBLE);
            if (currentFuel >= 1.0) {
                player.sendMessage(ChatColor.YELLOW + "El tanque ya está lleno.");
                event.setCancelled(true);
                return;
            }

            double newFuel = Math.min(1.0, currentFuel + 0.15);
            boat.getPersistentDataContainer().set(fuelKey, PersistentDataType.DOUBLE, newFuel);

            if (boat.getPassengers().contains(player) && fuelBars.containsKey(player.getUniqueId())) {
                fuelBars.get(player.getUniqueId()).setProgress(newFuel);
            }

            player.sendMessage(ChatColor.GREEN + "Combustible repostado: " + (int)(newFuel * 100) + "%");

            if (!player.getGameMode().equals(GameMode.CREATIVE)) {
                item.setAmount(item.getAmount() - 1);
            }

            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.5f);
            return;
        }

        if (player.isSneaking() && event.getHand() == EquipmentSlot.HAND) {
            String ownerIdStr = boat.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
            if (ownerIdStr == null || !ownerIdStr.equals(player.getUniqueId().toString())) {
                player.sendMessage(ChatColor.RED + "Solo el dueño puede recoger esta nave.");
                event.setCancelled(true);
                return;
            }

            if (!boat.getPassengers().isEmpty()) {
                player.sendMessage(ChatColor.RED + "La nave debe estar vacía para recogerla.");
                event.setCancelled(true);
                return;
            }

            double currentFuel = boat.getPersistentDataContainer().get(fuelKey, PersistentDataType.DOUBLE);
            if (fuelData != null) {
                fuelData.set(boat.getCustomName(), currentFuel);
                try {
                    fuelData.save(fuelFile);
                } catch (IOException e) {
                    plugin.getLogger().warning("No se pudo guardar el combustible: " + e.getMessage());
                }
            }

            ItemStack boatItem = createBoatItem(player);
            if (player.getInventory().addItem(boatItem).isEmpty()) {
                boat.remove();
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            } else {
                player.sendMessage(ChatColor.RED + "No tienes espacio en tu inventario.");
            }

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBoatEnter(VehicleEnterEvent event) {
        if (!(event.getVehicle() instanceof Boat)) return;

        Boat boat = (Boat) event.getVehicle();
        if (!boat.getPersistentDataContainer().has(boatKey, PersistentDataType.STRING)) return;

        if (!(event.getEntered() instanceof Player)) {
            event.setCancelled(true);
            return;
        }

        Player player = (Player) event.getEntered();
        String ownerIdStr = boat.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);

        if (ownerIdStr == null || !ownerIdStr.equals(player.getUniqueId().toString())) {
            player.sendMessage(ChatColor.RED + "Solo el dueño puede pilotar esta nave.");
            event.setCancelled(true);
            return;
        }

        BossBar fuelBar = Bukkit.createBossBar(
                ChatColor.GREEN + "Combustible",
                BarColor.GREEN,
                BarStyle.SEGMENTED_10
        );
        fuelBar.setProgress(boat.getPersistentDataContainer().get(fuelKey, PersistentDataType.DOUBLE));
        fuelBar.addPlayer(player);
        fuelBars.put(player.getUniqueId(), fuelBar);

        startBoatMovementTask(boat, player);
    }

    @EventHandler
    public void onBoatExit(VehicleExitEvent event) {
        if (!(event.getVehicle() instanceof Boat)) return;
        if (!(event.getExited() instanceof Player)) return;

        Boat boat = (Boat) event.getVehicle();
        if (!boat.getPersistentDataContainer().has(boatKey, PersistentDataType.STRING)) return;

        Player player = (Player) event.getExited();

        double currentFuel = boat.getPersistentDataContainer().get(fuelKey, PersistentDataType.DOUBLE);
        if (fuelData != null) {
            fuelData.set(boat.getCustomName(), currentFuel);
            try {
                fuelData.save(fuelFile);
            } catch (IOException e) {
                plugin.getLogger().warning("No se pudo guardar el combustible al bajarse: " + e.getMessage());
            }
        }

        if (fuelBars.containsKey(player.getUniqueId())) {
            fuelBars.get(player.getUniqueId()).removePlayer(player);
            fuelBars.remove(player.getUniqueId());
        }

        if (actionBarTasks.containsKey(player.getUniqueId())) {
            Bukkit.getScheduler().cancelTask(actionBarTasks.get(player.getUniqueId()));
            actionBarTasks.remove(player.getUniqueId());
        }

        player.stopSound(Sound.BLOCK_FURNACE_FIRE_CRACKLE);
    }

    private void startBoatMovementTask(Boat boat, Player player) {
        int taskId = new BukkitRunnable() {
            private float pitch = 0.5f;
            private boolean increasing = true;
            private int fuelTickCounter = 0;
            private boolean wasMoving = false;
            private Vector lastDirection = new Vector();
            private long lastSprintPress = 0;

            @Override
            public void run() {
                if (boat.isDead() || !boat.isValid() || player.isDead()) {
                    this.cancel();
                    cleanupPlayerData(player);
                    return;
                }

                if (!boat.getPassengers().contains(player)) {
                    this.cancel();
                    cleanupPlayerData(player);
                    return;
                }

                double currentFuel = boat.getPersistentDataContainer().get(fuelKey, PersistentDataType.DOUBLE);

                boolean tryingToSprint = isTryingToSprint(player);

                if (currentFuel > 0) {
                    Location boatLoc = boat.getLocation();
                    Location playerLoc = player.getLocation();
                    boatLoc.setYaw(playerLoc.getYaw());
                    boatLoc.setPitch(0);
                    boat.teleport(boatLoc);

                    boat.setVelocity(new Vector(0, 0, 0));
                    boat.setGravity(false);

                    if (tryingToSprint) {
                        Vector direction = player.getEyeLocation().getDirection().normalize();
                        double speedMultiplier = getBoatSpeedForWorld(boat.getWorld());
                        Vector velocity = direction.clone().multiply(speedMultiplier);

                        if (wasMoving) {
                            velocity = lastDirection.multiply(0.3).add(velocity.multiply(0.7));
                        }

                        lastDirection = velocity.clone();

                        if (direction.getY() < 0.5) {
                            velocity.setY(velocity.getY() + 0.1);
                        }

                        boat.setVelocity(velocity);
                        wasMoving = true;

                        float pitchAngle = (float) Math.toDegrees(Math.atan(velocity.getY() * 2));
                        pitchAngle = Math.max(-30, Math.min(30, pitchAngle));
                        boatLoc.setPitch(pitchAngle);
                        boat.teleport(boatLoc);

                        updateEngineSound();
                        consumeFuel(boat, currentFuel);

                        Location particleLoc = boat.getLocation().clone().subtract(direction.multiply(0.5));
                        boat.getWorld().spawnParticle(Particle.LARGE_SMOKE, particleLoc, 1, 0.2, 0.2, 0.2, 0.05);
                        boat.getWorld().spawnParticle(Particle.POOF, particleLoc, 1, 0.2, 0.2, 0.2, 0.05);
                    } else {
                        if (wasMoving) {
                            boat.setVelocity(new Vector(0, -0.05, 0));
                            wasMoving = false;
                            player.playSound(player.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, 1.0f, 0.5f);
                        }
                        player.stopSound(Sound.BLOCK_FURNACE_FIRE_CRACKLE);
                        if (System.currentTimeMillis() % 2000 < 50) {
                            player.playSound(player.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, 0.3f, 0.3f);
                        }
                    }
                } else {
                    boat.setGravity(true);
                    player.stopSound(Sound.BLOCK_FURNACE_FIRE_CRACKLE);

                    if (System.currentTimeMillis() % 2000 < 50) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 0.5f);
                    }
                }

                if (currentFuel < 0.2) {
                    sendActionBar(player, ChatColor.YELLOW + "¡Combustible bajo! " + (int)(currentFuel * 100) + "%");
                    if (System.currentTimeMillis() % 2000 < 50) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 0.5f);
                    }
                }
            }

            private boolean isTryingToSprint(Player player) {
                if (player.isSprinting()) {
                    lastSprintPress = System.currentTimeMillis();
                    return true;
                }

                if (player.getVelocity().getZ() != 0 || player.getVelocity().getX() != 0) {
                    long now = System.currentTimeMillis();
                    if (now - lastSprintPress < 300) {
                        lastSprintPress = 0;
                        return true;
                    }
                    lastSprintPress = now;
                }

                return System.currentTimeMillis() - lastSprintPress < 100;
            }

            private void updateEngineSound() {
                if (increasing) {
                    pitch += 0.02f;
                    if (pitch >= 1.5f) increasing = false;
                } else {
                    pitch -= 0.02f;
                    if (pitch <= 0.5f) increasing = true;
                }
                player.playSound(player.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, 1.0f, pitch);
            }

            private void consumeFuel(Boat boat, double currentFuel) {
                fuelTickCounter++;
                if (fuelTickCounter >= 40) {
                    fuelTickCounter = 0;
                    double newFuel = Math.max(0, currentFuel - 0.00167);
                    boat.getPersistentDataContainer().set(fuelKey, PersistentDataType.DOUBLE, newFuel);
                    fuelBars.get(player.getUniqueId()).setProgress(newFuel);

                    if (newFuel <= 0) {
                        explodeBoat(boat);
                    }
                }
            }

            private void cleanupPlayerData(Player player) {
                if (fuelBars.containsKey(player.getUniqueId())) {
                    fuelBars.get(player.getUniqueId()).removePlayer(player);
                    fuelBars.remove(player.getUniqueId());
                }
                actionBarTasks.remove(player.getUniqueId());
                lastSprintState.remove(player.getUniqueId());
            }
        }.runTaskTimer(plugin, 0L, 1L).getTaskId();

        actionBarTasks.put(player.getUniqueId(), taskId);
    }

    private double getBoatSpeedForWorld(World world) {
        if (fuelData == null) {
            return 0.3;
        }

        String worldName = world.getName().toLowerCase();

        if (worldName.contains("nether")) {
            return fuelData.getDouble("speed.nether", 0.2);
        } else if (worldName.contains("end")) {
            if (worldName.contains("corrupted")) {
                return fuelData.getDouble("speed.corrupted_end", 0.5);
            }
            return fuelData.getDouble("speed.end", 0.2);
        }

        return fuelData.getDouble("speed.overworld", 0.2);
    }

    private void explodeBoat(Boat boat) {
        boat.setGlowing(true);

        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= 3 || boat.isDead()) {
                    boat.getWorld().createExplosion(boat.getLocation(), 15.0f, true, true);
                    boat.remove();

                    String ownerIdStr = boat.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
                    if (ownerIdStr != null) {
                        Player owner = Bukkit.getPlayer(UUID.fromString(ownerIdStr));
                        if (owner != null) {
                            fuelData.set(boat.getCustomName(), 0.1);
                            try {
                                fuelData.save(fuelFile);
                            } catch (IOException e) {
                                plugin.getLogger().warning("Error al guardar fuel: " + e.getMessage());
                            }
                        }
                    }
                    this.cancel();
                } else {
                    boat.getWorld().spawnParticle(Particle.FLAME, boat.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
                    count++;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler
    public void onProjectileHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Projectile &&
                event.getEntity() instanceof Boat) {
            Boat boat = (Boat) event.getEntity();
            if (boat.getPersistentDataContainer().has(boatKey, PersistentDataType.STRING)) {
                event.setCancelled(true);
            }
        }
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
    }
}
