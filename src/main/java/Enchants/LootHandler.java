package Enchants;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import vct.hardcore3.ViciontHardcore3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class LootHandler implements Listener {

    private final ViciontHardcore3 plugin;
    private final Random random = new Random();

    public LootHandler(ViciontHardcore3 plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        String lootTableKey = event.getLootTable().getKey().getKey();
        if (!isStructureLoot(lootTableKey)) {
            return;
        }

        List<ItemStack> extraLoot = new ArrayList<>();
        int essenceCount = determineEssenceCount();

        // Añadir esencias únicas
        addUniqueRandomEssences(extraLoot, essenceCount);

        // Agregar las esencias al loot del evento
        event.getLoot().addAll(extraLoot);

        // Limitar las esencias a un máximo de 3
        limitEssences(event.getLoot());
    }

    private boolean isStructureLoot(String lootTableKey) {
        return lootTableKey.contains("chests/");
    }

    private int determineEssenceCount() {
        int roll = random.nextInt(100) + 1;

        if (roll <= 25) {
            return 3;
        } else if (roll <= 50) {
            return 2;
        } else {
            return 1;
        }
    }


    private void addUniqueRandomEssences(List<ItemStack> loot, int count) {
        List<ItemStack> allEssences = getAllEssences();
        Collections.shuffle(allEssences, random); // Mezclar aleatoriamente

        for (int i = 0; i < count && i < allEssences.size(); i++) {
            loot.add(allEssences.get(i)); // Añadir esencias únicas
        }
    }

    private List<ItemStack> getAllEssences() {
        List<ItemStack> essences = new ArrayList<>();
        essences.add(EssenceFactory.createProtectionEssence());
        essences.add(EssenceFactory.createUnbreakingEssence());
        essences.add(EssenceFactory.createMendingEssence());
        essences.add(EssenceFactory.createEfficiencyEssence());
        essences.add(EssenceFactory.createFortuneEssence());
        essences.add(EssenceFactory.createSharpnessEssence());
        essences.add(EssenceFactory.createSmiteEssence());
        essences.add(EssenceFactory.createBaneOfArthropodsEssence());
        essences.add(EssenceFactory.createFeatherFallingEssence());
        essences.add(EssenceFactory.createLootingEssence());
        essences.add(EssenceFactory.createDepthStriderEssence());
        essences.add(EssenceFactory.createSilkTouchEssence());
        essences.add(EssenceFactory.createPowerEssence());
        return essences;
    }

    private void limitEssences(List<ItemStack> loot) {
        List<ItemStack> essences = new ArrayList<>();

        // Filtrar las esencias en el loot
        for (ItemStack item : loot) {
            if (item != null && isEssence(item)) {
                essences.add(item);
            }
        }

        // Si hay más de 3, eliminar las extras
        if (essences.size() > 3) {
            // Mantener solo las primeras 3 esencias
            List<ItemStack> extraEssences = essences.subList(3, essences.size());

            // Remover las esencias adicionales
            loot.removeAll(extraEssences);
        }
    }

    private boolean isEssence(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        String displayName = item.getItemMeta().getDisplayName();
        return displayName.contains("Esencia") || displayName.contains("Essence"); // Ajustar según el nombre
    }
}
