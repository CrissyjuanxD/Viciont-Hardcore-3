    package Dificultades;

    import Dificultades.CustomMobs.CorruptedSpider;
    import Dificultades.CustomMobs.CorruptedZombies;
    import items.CorruptedMobItems;
    import items.CorruptedNetheriteItems;
    import org.bukkit.*;
    import org.bukkit.entity.*;
    import org.bukkit.event.EventHandler;
    import org.bukkit.event.HandlerList;
    import org.bukkit.event.Listener;
    import org.bukkit.event.entity.*;
    import org.bukkit.event.player.PlayerItemConsumeEvent;
    import org.bukkit.event.player.PlayerPortalEvent;
    import org.bukkit.event.raid.RaidTriggerEvent;
    import org.bukkit.inventory.ItemRarity;
    import org.bukkit.inventory.ItemStack;
    import org.bukkit.inventory.RecipeChoice;
    import org.bukkit.inventory.ShapedRecipe;
    import org.bukkit.inventory.meta.ItemMeta;
    import org.bukkit.persistence.PersistentDataType;
    import org.bukkit.plugin.java.JavaPlugin;
    import org.bukkit.potion.PotionEffect;
    import org.bukkit.potion.PotionEffectType;
    import Handlers.DayHandler;
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
                corruptedZombies.apply();
                corruptedSpider.apply();
                Bukkit.getPluginManager().registerEvents(this, plugin);
                registerCustomRecipe();
                disablePhantomSpawning();
                isApplied = true;
            }
        }

        public void revert() {
            if (isApplied) {
                corruptedZombies.revert();
                corruptedSpider.revert();
                NamespacedKey key = new NamespacedKey(plugin, "corrupted_steak");
                Bukkit.removeRecipe(key);
                // Desregistrar eventos
                HandlerList.unregisterAll(this);

                isApplied = false;
            }
        }

        @EventHandler
        public void onCreatureSpawn(CreatureSpawnEvent event) {
            if (!isApplied) return;
            if (dayHandler.getCurrentDay() >= 4) {
                return;
            }
            handleCorruptedZombieConversion(event);
            handleCorruptedSpiderConversion(event);
        }


        private void handleCorruptedZombieConversion(CreatureSpawnEvent event) {
            if (event.getEntityType() != EntityType.ZOMBIE) return;

            if (event.getEntity().getPersistentDataContainer().has(corruptedZombies.getCorruptedKey(), PersistentDataType.BYTE)) {
                return;
            }

            if (random.nextInt(20) != 0) return;

            Zombie zombie = (Zombie) event.getEntity();
            corruptedZombies.transformToCorruptedZombie(zombie);
        }

        private void handleCorruptedSpiderConversion(CreatureSpawnEvent event) {
            if (event.getEntityType() != EntityType.SPIDER) return;

            if (event.getLocation().getWorld().getEnvironment() != World.Environment.NORMAL) return;

            if (event.getEntity().getPersistentDataContainer().has(corruptedSpider.getCorruptedSpiderKey(), PersistentDataType.BYTE)) {
                return;
            }

            if (random.nextInt(20) != 0) return;

            Spider spider = (Spider) event.getEntity();
            corruptedSpider.transformspawnCorruptedSpider(spider);
        }

        public static ItemStack corruptedSteak() {
            ItemStack item = new ItemStack(Material.COOKED_BEEF);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.DARK_PURPLE + "Carne Corrupta");
            meta.setCustomModelData(2);
            meta.setRarity(ItemRarity.EPIC);
            item.setItemMeta(meta);
            return item;
        }

        public void registerCustomRecipe() {
            NamespacedKey key = new NamespacedKey(plugin, "corrupted_steak");

            if (Bukkit.getRecipe(key) != null) {
                return;
            }

            ShapedRecipe customRecipe = new ShapedRecipe(key, corruptedSteak());
            customRecipe.shape("CCC", "CSC", "CCC");
            customRecipe.setIngredient('C', new RecipeChoice.ExactChoice(CorruptedMobItems.createCorruptedMeet()));
            customRecipe.setIngredient('S' , Material.COOKED_BEEF);

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
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 20, 0, false, false, true));
        }


        @EventHandler
        public void onPortalEnter(PlayerPortalEvent event) {
            if (isApplied) {
                DayHandler dayHandler = ((ViciontHardcore3) plugin).getDayHandler();
                if (dayHandler.getCurrentDay() < 4 && event.getCause() == PlayerPortalEvent.TeleportCause.NETHER_PORTAL) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "۞ El Nether está cerrado hasta el día 4!");
                }
            }
        }

        @EventHandler
        public void onRaidTrigger(RaidTriggerEvent event) {
            if (isApplied) {
                DayHandler dayHandler = ((ViciontHardcore3) plugin).getDayHandler();
                if (dayHandler.getCurrentDay() < 2) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "۞ Las Raids están deshabilitadas hasta el día 2!");
                }
            }
        }

        public void disablePhantomSpawning() {
            for (World world : Bukkit.getWorlds()) {
                world.setGameRule(GameRule.DO_INSOMNIA, false);
            }
        }

        @EventHandler
        public void onStrayBurn(EntityCombustEvent event) {
            if (!isApplied) return;

            if (event.getEntityType() == EntityType.STRAY) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onZombieVillagerBurn(EntityCombustEvent event) {
            if (!isApplied) return;

            if (event.getEntityType() == EntityType.ZOMBIE_VILLAGER) {
                event.setCancelled(true);
            }
        }

    }
