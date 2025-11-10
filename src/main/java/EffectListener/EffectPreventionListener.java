package EffectListener;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EffectPreventionListener implements Listener {

    // === Efectos de poción prohibidos ===
    private static final Set<PotionEffectType> BLOCKED_EFFECTS = new HashSet<>();

    static {
        BLOCKED_EFFECTS.add(PotionEffectType.WEAVING);
    }

    private boolean hasBlockedEffect(PotionMeta meta) {
        if (meta == null) return false;

        if (meta.hasCustomEffects()) {
            for (PotionEffect effect : meta.getCustomEffects()) {
                if (BLOCKED_EFFECTS.contains(effect.getType())) return true;
            }
        }

        try {
            if (meta.getBasePotionData() != null) {
                PotionType type = meta.getBasePotionData().getType();
                PotionEffectType baseType = PotionEffectType.getByName(type.name());
                if (baseType != null && BLOCKED_EFFECTS.contains(baseType)) {
                    return true;
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    private void playCancelEffect(org.bukkit.Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1f, 1.2f);
    }


    @EventHandler
    public void onPotionCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;
        ItemStack result = event.getRecipe().getResult();
        if (result == null) return;

        Material type = result.getType();
        if (type == Material.POTION || type == Material.SPLASH_POTION || type == Material.LINGERING_POTION) {
            if (!(result.getItemMeta() instanceof PotionMeta)) return;
            PotionMeta meta = (PotionMeta) result.getItemMeta();

            if (hasBlockedEffect(meta)) {
                event.getInventory().setResult(new ItemStack(Material.AIR));
            }
        }
    }

    @EventHandler
    public void onBrew(BrewEvent event) {
        BrewerInventory inv = event.getContents();
        for (int i = 0; i < 3; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            if (!(item.getItemMeta() instanceof PotionMeta)) continue;

            PotionMeta meta = (PotionMeta) item.getItemMeta();
            if (hasBlockedEffect(meta)) {
                inv.setItem(i, new ItemStack(Material.AIR));
                playCancelEffect(((BrewingStand) event.getBlock().getState()).getLocation());
            }
        }
    }

    @EventHandler
    public void onPotionDrink(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.POTION) return;
        if (!(item.getItemMeta() instanceof PotionMeta)) return;

        PotionMeta meta = (PotionMeta) item.getItemMeta();

        if (hasBlockedEffect(meta)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§5✖ §7La corrupción distorsiona esta poción. No puedes beberla.");
            playCancelEffect(event.getPlayer().getLocation());
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        boolean blocked = false;
        for (PotionEffect effect : event.getPotion().getEffects()) {
            if (BLOCKED_EFFECTS.contains(effect.getType())) {
                blocked = true;
                break;
            }
        }

        if (!blocked) {
            try {
                PotionMeta meta = (PotionMeta) event.getPotion().getItem().getItemMeta();
                if (hasBlockedEffect(meta)) blocked = true;
            } catch (Exception ignored) {}
        }

        if (blocked) {
            event.setCancelled(true);
            playCancelEffect(event.getPotion().getLocation());
            event.getPotion().remove();
        }
    }

    @EventHandler
    public void onAreaEffectCloudApply(AreaEffectCloudApplyEvent event) {
        AreaEffectCloud cloud = event.getEntity();
        boolean blocked = false;

        try {
            List<PotionEffect> effects = (List<PotionEffect>) cloud.getClass()
                    .getMethod("getCustomEffects")
                    .invoke(cloud);

            if (effects != null) {
                for (PotionEffect effect : effects) {
                    if (BLOCKED_EFFECTS.contains(effect.getType())) {
                        blocked = true;
                        break;
                    }
                }
            }

            if (!blocked && cloud.getBasePotionData() != null) {
                PotionType type = cloud.getBasePotionData().getType();
                PotionEffectType baseType = PotionEffectType.getByName(type.name());
                if (baseType != null && BLOCKED_EFFECTS.contains(baseType)) {
                    blocked = true;
                }
            }

        } catch (Exception ignored) {}

        if (blocked) {
            event.getAffectedEntities().clear();
            cloud.remove();
            playCancelEffect(cloud.getLocation());
        }
    }
}
