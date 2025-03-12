    package Dificultades;

    import Dificultades.CustomMobs.CorruptedSpider;
    import Dificultades.CustomMobs.CorruptedZombies;
    import org.bukkit.*;
    import org.bukkit.entity.*;
    import org.bukkit.event.EventHandler;
    import org.bukkit.event.Listener;
    import org.bukkit.event.entity.*;
    import org.bukkit.event.player.PlayerItemConsumeEvent;
    import org.bukkit.event.player.PlayerPortalEvent;
    import org.bukkit.event.raid.RaidTriggerEvent;
    import org.bukkit.inventory.ItemStack;
    import org.bukkit.inventory.ShapedRecipe;
    import org.bukkit.inventory.meta.ItemMeta;
    import org.bukkit.persistence.PersistentDataType;
    import org.bukkit.plugin.java.JavaPlugin;
    import org.bukkit.potion.PotionEffect;
    import org.bukkit.potion.PotionEffectType;
    import vct.hardcore3.DayHandler;
    import vct.hardcore3.ViciontHardcore3;

    import java.util.*;

    public class DayOneChanges implements Listener {
        private final DayHandler dayHandler;
        private final JavaPlugin plugin;
        private final Random random = new Random();
        private boolean isApplied = false;
        private final CorruptedZombies corruptedZombies;
        private final CorruptedSpider corruptedSpider;

        public DayOneChanges(JavaPlugin plugin, DayHandler handler) {
            this.plugin = plugin;
            this.dayHandler = handler;
            this.corruptedZombies = new CorruptedZombies(plugin);
            this.corruptedSpider = new CorruptedSpider(plugin);
        }

        public void apply() {
            if (!isApplied) {
                // eventos solo cuando se aplica
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "gamerule playersSleepingPercentage 0");
                corruptedZombies.apply();
                corruptedSpider.apply();
                Bukkit.getPluginManager().registerEvents(this, plugin);
                registerCustomRecipe();
                isApplied = true;
            }
        }

        public void revert() {
            if (isApplied) {
                corruptedZombies.revert();
                corruptedSpider.revert();
                NamespacedKey key = new NamespacedKey(plugin, "corrupted_steak");
                Bukkit.removeRecipe(key);

                isApplied = false;
            }
        }

        @EventHandler
        public void onCreatureSpawn(CreatureSpawnEvent event) {
            if (!isApplied) return; // Asegúrate de que los cambios estén aplicados

            // Verificar si el mob ya es corrupto (zombie o araña)
            if (event.getEntity().getPersistentDataContainer().has(corruptedZombies.getCorruptedKey(), PersistentDataType.BYTE) ||
                    event.getEntity().getPersistentDataContainer().has(corruptedSpider.getCorruptedKey(), PersistentDataType.BYTE)) {
                return; // Si ya es corrupto, no hacer nada
            }

            int currentDay = dayHandler.getCurrentDay();
            int zombieProbability;
            int spiderProbability;

            if (currentDay >= 10) {
                zombieProbability = 2;
                spiderProbability = 2;
            } else if (currentDay >= 7) {
                zombieProbability = 7;
                spiderProbability = 7;
            } else if (currentDay >= 4) {
                zombieProbability = 13;
                spiderProbability = 13;
            } else {
                zombieProbability = 25;
                spiderProbability = 25;
            }

            if (event.getEntityType() == EntityType.ZOMBIE) {
                if (random.nextInt(zombieProbability) == 0) {
                    Zombie zombie = (Zombie) event.getEntity();
                    corruptedZombies.spawnCorruptedZombie(zombie.getLocation());
                    zombie.remove();
                }
            }

            if (event.getEntityType() == EntityType.SPIDER) {
                if (random.nextInt(spiderProbability) == 0) {
                    Spider spider = (Spider) event.getEntity();
                    corruptedSpider.spawnCorruptedSpider(spider.getLocation());
                    spider.remove();
                }
            }
        }


        public void registerCustomRecipe() {
            NamespacedKey key = new NamespacedKey(plugin, "corrupted_steak");

            if (Bukkit.getRecipe(key) != null) {
                return;
            }

            //item personalizado
            ItemStack customItem = new ItemStack(Material.COOKED_BEEF);
            ItemMeta meta = customItem.getItemMeta();
            meta.setDisplayName(ChatColor.DARK_PURPLE + "Carne Corrupta");
            meta.setCustomModelData(2);
            customItem.setItemMeta(meta);

            //receta
            ShapedRecipe customRecipe = new ShapedRecipe(key, customItem);
            customRecipe.shape(" F ", " F ", " F ");
            customRecipe.setIngredient('F', Material.ROTTEN_FLESH);

            plugin.getServer().addRecipe(customRecipe);
        }


        @EventHandler
        public void onPlayerEat(PlayerItemConsumeEvent event) {
            if (!isApplied) return;

            ItemStack item = event.getItem();
            if (item.getType() != Material.COOKED_BEEF) return;

            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasCustomModelData() || meta.getCustomModelData() != 2) return;

            Player player = event.getPlayer();
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 300, 0, false, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 60, 0, false, false, true));
        }


        @EventHandler
        public void onPortalEnter(PlayerPortalEvent event) {
            if (isApplied) {
                DayHandler dayHandler = ((ViciontHardcore3) plugin).getDayHandler();
                if (dayHandler.getCurrentDay() < 4 && event.getCause() == PlayerPortalEvent.TeleportCause.NETHER_PORTAL) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "¡El Nether está cerrado hasta el día 4!");
                }
            }
        }

        @EventHandler
        public void onRaidTrigger(RaidTriggerEvent event) {
            if (isApplied) {
                DayHandler dayHandler = ((ViciontHardcore3) plugin).getDayHandler();
                if (dayHandler.getCurrentDay() < 2) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "¡Las Raids están deshabilitadas hasta el día 2!");
                }
            }
        }
    }
