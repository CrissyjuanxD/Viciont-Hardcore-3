package Events.MissionSystem;

import TitleListener.SuccessNotification;
import com.viciontmedia.api.ViciontMediaAPI;
import items.EconomyItems;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Mission3 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final NamespacedKey bossKey;

    // Almacenamiento temporal para contar los golpes por cada abeja: UUID de la abeja -> (UUID del jugador -> cantidad de golpes)
    private final Map<UUID, Map<UUID, Integer>> beeHits = new HashMap<>();

    public Mission3(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);

        // Esta es la llave exacta que tu QueenBeeHandler le pone al Boss original
        this.bossKey = new NamespacedKey(plugin, "is_queen_bee");
    }

    @Override
    public String getName() {
        return "Cazador de Abejas";
    }

    @Override
    public String getDescription() {
        return "Elimina a una Abeja Reina.\nUsa /bosstp para ir a su Dungeon.\nInteractua con el panal del altar.";
    }

    @Override
    public int getMissionNumber() {
        return 3;
    }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(14);
        ItemStack goldenApples = new ItemStack(Material.GOLD_BLOCK, 15);
        ItemStack unBook = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) unBook.getItemMeta();
        if (meta != null) {
            meta.addStoredEnchant(Enchantment.UNBREAKING, 4, true);
            unBook.setItemMeta(meta);
        }
        ItemStack xpFill = new ItemStack(Material.HONEY_BOTTLE, 1);
        for (int i = 0; i < 27; i++) {
            if (i == 10 || i == 11 || i == 12) rewards.add(unBook);
            else if (i == 14) rewards.add(coins);
            else if (i == 16) rewards.add(goldenApples);
            else rewards.add(xpFill.clone());
        }
        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Bee)) return;

        if (!entity.getPersistentDataContainer().has(bossKey, PersistentDataType.BYTE)) return;

        Player damager = null;

        if (event.getDamager() instanceof Player) {
            damager = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                damager = shooter;
            }
        }

        if (damager == null) return;
        if (!missionHandler.isMissionActive(damager, 3)) return;
        if (missionHandler.isMissionCompleted(damager, 3)) return;

        UUID beeUUID = entity.getUniqueId();
        beeHits.putIfAbsent(beeUUID, new HashMap<>());
        Map<UUID, Integer> playerHits = beeHits.get(beeUUID);

        // Sumar 1 golpe al registro del jugador
        playerHits.put(damager.getUniqueId(), playerHits.getOrDefault(damager.getUniqueId(), 0) + 1);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Bee)) return;

        if (!entity.getPersistentDataContainer().has(bossKey, PersistentDataType.BYTE)) return;

        UUID beeUUID = entity.getUniqueId();
        if (!beeHits.containsKey(beeUUID)) return;

        Map<UUID, Integer> playerHits = beeHits.get(beeUUID);

        for (Map.Entry<UUID, Integer> entry : playerHits.entrySet()) {
            if (entry.getValue() >= 3) {
                Player player = plugin.getServer().getPlayer(entry.getKey());

                if (player != null && player.isOnline()) {
                    // Verificar que esté en el mismo mundo y a un radio de 50 bloques (usamos distanceSquared por optimización matemática: 50x50 = 2500)
                    if (player.getWorld().equals(entity.getWorld()) && player.getLocation().distanceSquared(entity.getLocation()) <= 2500) {

                        if (missionHandler.isMissionActive(player, 3) && !missionHandler.isMissionCompleted(player, 3)) {
                            successNotification.showSuccess(player);

                            // Mensaje de victoria (Completada)
                            String compText = "13%&#9cee8b&lMisión &r&#9cee8b#&l3 &l&+Completada&- &f&+ 0%&r\\uE002&-\n\n" +
                                    "[left] 10%&#9bb8fdHas completado la misión&f:\n" +
                                    "[left] \"&+&#cead36&lCazador de Abejas&r&-\"\n\n" +
                                    "[left] &#ffcc99۞ &#ad80dbHas &#ffcc99derrotado &#ad80dba la \n" +
                                    "[left] &#ad80db&lCorrupted Queen Bee";

                            ViciontMediaAPI.sendText(player, 1, "derecha", "#005726", 12, "topright", false, compText);

                            // Completar misión con ligero retraso para dar tiempo a que se registre correctamente
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                missionHandler.completeMission(player.getName(), 3);
                            }, 10L);
                        }
                    }
                }
            }
        }

        beeHits.remove(beeUUID);
    }
}