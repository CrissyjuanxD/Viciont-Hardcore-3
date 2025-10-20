package Dificultades;

import Dificultades.CustomMobs.*;
import Handlers.DayHandler;
import Handlers.DeathStormHandler;
import items.TridenteEspectral;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class DayFiveChange implements Listener {
    private final JavaPlugin plugin;
    private boolean isApplied = false;
    private final DayHandler dayHandler;
    private final CorruptedCreeper corruptedCreeper;
    private final CorruptedDrowned corruptedDrowned;
    private final Bombita bombita;
    private final Random random = new Random();
    private final TridenteEspectral tridenteEspectral;
    private static final ChatColor ERROR_COLOR = ChatColor.of("#ff5555");

    public DayFiveChange(JavaPlugin plugin, DayHandler handler) {
        this.plugin = plugin;
        this.dayHandler = handler;
        this.corruptedCreeper = new CorruptedCreeper(plugin);
        this.corruptedDrowned = new CorruptedDrowned(plugin);
        this.tridenteEspectral = new TridenteEspectral(plugin);
        this.bombita = new Bombita(plugin);
    }

    public void apply() {
        if (!isApplied) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            isApplied = true;
            corruptedCreeper.apply();
            corruptedDrowned.apply();

        }
    }

    public void revert() {
        if (isApplied) {
            isApplied = false;
            corruptedCreeper.revert();
            corruptedDrowned.revert();
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isApplied) return;

/*        if (event.getLocation().getWorld().getEnvironment() != World.Environment.NORMAL) {
            return;
        }

        if (event.getEntityType() == EntityType.CREEPER &&
                !isAlreadyCustomCreeper(event.getEntity())) {

            if (dayHandler.getCurrentDay() >= 5 && random.nextInt(3) == 0) {
                Creeper creeper = (Creeper) event.getEntity();

                creeper.getPersistentDataContainer().set(
                        corruptedCreeper.getCorruptedCreeperKey(),
                        PersistentDataType.BYTE,
                        (byte)1
                );

                corruptedCreeper.transformToCorruptedCreeper(creeper);
            }
        }*/

        handleCorruptedDrownedsConversion(event);
        handleICreepertoCorruptedCreepersConversion(event);
        /*handleSquidstoCorruptedDrownedsConversion(event);*/
    }

    private boolean isAlreadyCustomCreeper(Entity creeper) {
        return creeper.getPersistentDataContainer().has(corruptedCreeper.getCorruptedCreeperKey(), PersistentDataType.BYTE) ||
                creeper.getPersistentDataContainer().has(bombita.getBombitaKey(), PersistentDataType.BYTE);
    }

    private void handleCorruptedDrownedsConversion(CreatureSpawnEvent event) {

        if (event.getEntityType() != EntityType.DROWNED) return;

        if (event.getEntity().getPersistentDataContainer()
                .has(corruptedDrowned.getCorruptedDrownedKey(), PersistentDataType.BYTE)) {
            return;
        }

        if (random.nextInt(2) != 0) return;

        Drowned drowned = (Drowned) event.getEntity();
        Location loc = drowned.getLocation();

        corruptedDrowned.spawnCorruptedDrowned(loc);
        drowned.remove();

    }

    private void handleICreepertoCorruptedCreepersConversion(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }

        if (event.getLocation().getWorld().getEnvironment() != World.Environment.NORMAL) {
            return;
        }

        if (event.getEntityType() != EntityType.CREEPER) {
            return;
        }

        // 4. Aplicar tu lógica de probabilidad
        boolean isCorruptedCreeper = random.nextInt(3) == 0;

        if (isCorruptedCreeper) {
            event.setCancelled(true);
            corruptedCreeper.spawnCorruptedCreeper(event.getLocation());
        }
    }

/*    private void handleSquidstoCorruptedDrownedsConversion(CreatureSpawnEvent event) {

        if (event.getEntityType() != EntityType.SQUID) return;

        if (event.getEntity().getPersistentDataContainer()
                .has(corruptedDrowned.getCorruptedDrownedKey(), PersistentDataType.BYTE)) {
            return;
        }

        if (random.nextInt(10) != 0) return;

        Squid squid = (Squid) event.getEntity();
        Location loc = squid.getLocation();

        corruptedDrowned.spawnCorruptedDrowned(loc);
        squid.remove();

    }*/

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isApplied) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isWeatherRestrictive(player.getWorld())) return;

        if (isNormalTridentWithRiptide(item) && !tridenteEspectral.isSpectralTrident(item)) {
            event.setCancelled(true);
            player.sendMessage(ERROR_COLOR + "۞ No puedes usar este item durante una Tormenta");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!isApplied) return;

        if (!(event.getEntity() instanceof Trident trident) ||
                !(trident.getShooter() instanceof Player player)) {
            return;
        }

        if (!isWeatherRestrictive(player.getWorld())) return;

        ItemStack tridentItem = trident.getItem();
        if (tridentItem == null) return;

        if (isNormalTridentWithRiptide(tridentItem) && !tridenteEspectral.isSpectralTrident(tridentItem)) {
            event.setCancelled(true);
            player.sendMessage(ERROR_COLOR + "۞ No puedes usar este item durante una Tormenta");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);

            if (player.getInventory().addItem(tridentItem).isEmpty()) {
                trident.remove();
            }
        }
    }

    private boolean isWeatherRestrictive(World world) {
        return world.hasStorm() || world.isThundering();
    }

    private boolean isNormalTridentWithRiptide(ItemStack item) {
        if (item == null || item.getType() != Material.TRIDENT) return false;

        return item.getEnchantmentLevel(Enchantment.RIPTIDE) >= 1;
    }

}
