package Enchants;

import Dificultades.DayFourChanges;
import items.UpgradeNTItems;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class LootHandler implements Listener {

    private final JavaPlugin plugin;
    private final Random random = new Random();

    public LootHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        addExperienceBottles(event.getLoot());
        String lootTableKey = event.getLootTable().getKey().getKey();
        if (!isStructureLoot(lootTableKey)) {
            return;
        }

        List<ItemStack> extraLoot = new ArrayList<>();
        int essenceCount = determineEssenceCount();

        addUniqueRandomEssences(extraLoot, essenceCount);

        event.getLoot().addAll(extraLoot);

        limitEssences(event.getLoot());
    }

    private void addExperienceBottles(List<ItemStack> loot) {
        boolean hasExpBottle = loot.stream().anyMatch(item ->
                item != null && item.getType() == Material.EXPERIENCE_BOTTLE);

        if (hasExpBottle) return;

        int bottlesCount = 2 + random.nextInt(5);

        while (bottlesCount > 0) {
            int stackSize = Math.min(bottlesCount, 64);
            ItemStack expBottle = new ItemStack(Material.EXPERIENCE_BOTTLE, stackSize);
            loot.add(expBottle);
            bottlesCount -= stackSize;
        }
    }

    private boolean isStructureLoot(String lootTableKey) {
        return lootTableKey.contains("chests/");
    }

    private int determineEssenceCount() {
        int roll = random.nextInt(100) + 1;
        if (roll <= 40) {
            return 0; // 40% sin esencia
        } else if (roll <= 85) {
            return 1; // 45% con 1 esencia
        } else {
            return 2; // 15% con 2 esencias
        }
    }


    private void addUniqueRandomEssences(List<ItemStack> loot, int count) {
        List<ItemStack> allEssences = getAllEssences();
        Collections.shuffle(allEssences, random);

        for (int i = 0; i < count && i < allEssences.size(); i++) {
            loot.add(allEssences.get(i));
        }
    }

    private List<ItemStack> getAllEssences() {
        List<ItemStack> essences = new ArrayList<>();
        essences.add(EssenceFactory.createProtectionEssence());
        essences.add(EssenceFactory.createUnbreakingEssence());
        essences.add(EssenceFactory.createEfficiencyEssence());
        essences.add(EssenceFactory.createFortuneEssence());
        essences.add(EssenceFactory.createSharpnessEssence());
        essences.add(EssenceFactory.createSmiteEssence());
        essences.add(EssenceFactory.createBaneOfArthropodsEssence());
        essences.add(EssenceFactory.createFeatherFallingEssence());
        essences.add(EssenceFactory.createLootingEssence());
        essences.add(EssenceFactory.createDepthStriderEssence());
        essences.add(EssenceFactory.createPowerEssence());
        essences.add(EssenceFactory.createVoidEssence());
        return essences;
    }

    private void limitEssences(List<ItemStack> loot) {
        List<ItemStack> essences = new ArrayList<>();

        for (ItemStack item : loot) {
            if (item != null && isEssence(item)) {
                essences.add(item);
            }
        }

        if (essences.size() > 2) {
            List<ItemStack> extraEssences = essences.subList(2, essences.size());
            loot.removeAll(extraEssences);
        }
    }


    private boolean isEssence(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        String displayName = item.getItemMeta().getDisplayName();
        return displayName.contains("Esencia") || displayName.contains("Essence");
    }

    // Evento para modificar el loot al generarse en cofres UPGRADE NETHERITE
    @EventHandler
    public void onLootGenerate2(LootGenerateEvent event) {
        if (event.getWorld().getEnvironment() != World.Environment.NETHER) {
            return;
        }

        List<ItemStack> loot = event.getLoot();

        for (int i = 0; i < loot.size(); i++) {
            ItemStack item = loot.get(i);

            // Reemplazar Netherite Upgrade Template por Upgrade Vacío
            if (item != null && item.getType() == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) {
                loot.set(i, UpgradeNTItems.createUpgradeVacio());
            }

            // Eliminar Ancient Debris y Netherite Ingots
            if (item != null && (item.getType() == Material.ANCIENT_DEBRIS ||
                    item.getType() == Material.NETHERITE_INGOT)) {
                loot.remove(i);
                i--;
            }
        }
    }
}
