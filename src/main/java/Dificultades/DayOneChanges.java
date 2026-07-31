    package Dificultades;

    import net.md_5.bungee.api.ChatColor;
    import Dificultades.CustomMobs.CorruptedInsect;
    import Dificultades.CustomMobs.CorruptedSpider;
    import Dificultades.CustomMobs.CorruptedZombies;
    import items.CorruptedMobItems;
    import items.CorruptedNetheriteItems;
    import org.bukkit.*;
    import org.bukkit.enchantments.Enchantment;
    import org.bukkit.entity.*;
    import org.bukkit.event.EventHandler;
    import org.bukkit.event.HandlerList;
    import org.bukkit.event.Listener;
    import org.bukkit.event.entity.*;
    import org.bukkit.event.player.PlayerItemConsumeEvent;
    import org.bukkit.event.player.PlayerPortalEvent;
    import org.bukkit.event.raid.RaidTriggerEvent;
    import org.bukkit.inventory.*;
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
        private final CorruptedInsect corruptedInsect;


        public DayOneChanges(JavaPlugin plugin, DayHandler handler) {
            this.plugin = plugin;
            this.dayHandler = handler;
            this.corruptedZombies = new CorruptedZombies(plugin, handler);
            this.corruptedSpider = new CorruptedSpider(plugin,handler);
            this.corruptedInsect = new CorruptedInsect(plugin, handler);
        }

        public void apply() {
            if (!isApplied) {
                // eventos solo cuando se aplica
                corruptedZombies.apply();
                corruptedSpider.apply();
                corruptedInsect.apply();
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
                corruptedInsect.revert();
                NamespacedKey key = new NamespacedKey(plugin, "corrupted_steak");
                Bukkit.removeRecipe(key);
                // Desregistrar eventos
                HandlerList.unregisterAll(this);

                isApplied = false;
            }
        }

/*        @EventHandler
        public void onCreatureSpawn(CreatureSpawnEvent event) {
            if (!isApplied) return;
            if (dayHandler.getCurrentDay() >= 4) {
                return;
            }
            handleCorruptedZombieConversion(event);
            handleCorruptedSpiderConversion(event);
        }*/


/*        private void handleCorruptedZombieConversion(CreatureSpawnEvent event) {
            if (event.getEntityType() != EntityType.ZOMBIE) return;

            if (event.getEntity().getPersistentDataContainer().has(corruptedZombies.getCorruptedKey(), PersistentDataType.BYTE)) {
                return;
            }

            if (random.nextInt(20) != 0) return;

            Zombie zombie = (Zombie) event.getEntity();
            corruptedZombies.transformToCorruptedZombie(zombie);
        }*/

/*        private void handleCorruptedSpiderConversion(CreatureSpawnEvent event) {
            if (event.getEntityType() != EntityType.SPIDER) return;

            if (event.getLocation().getWorld().getEnvironment() != World.Environment.NORMAL) return;

            if (event.getEntity().getPersistentDataContainer().has(corruptedSpider.getCorruptedSpiderKey(), PersistentDataType.BYTE)) {
                return;
            }

            if (random.nextInt(20) != 0) return;

            Spider spider = (Spider) event.getEntity();
            corruptedSpider.transformspawnCorruptedSpider(spider);
        }*/

        public static ItemStack corruptedSteak() {
            ItemStack item = new ItemStack(Material.COOKED_BEEF);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Carne Corrupta");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#ffcc99") + "Esta carne te otorga estos");
            lore.add(ChatColor.of("#ffcc99") + "efectos" + ChatColor.GRAY + ":");
            lore.add("");
            lore.add(ChatColor.GRAY + "> " + ChatColor.of("#99cc33") + "Náuseas 1" + ChatColor.GRAY + " (" + ChatColor.of("#0099cc") + "10 s" + ChatColor.GRAY + ")");
            lore.add(ChatColor.GRAY + "> " + ChatColor.of("#cc3300") + "Saturación 1" + ChatColor.GRAY + " (" + ChatColor.of("#0099cc") + "1.5 s" + ChatColor.GRAY + ")");
            lore.add("");
            meta.setLore(lore);
            meta.setCustomModelData(2);
            meta.setRarity(ItemRarity.EPIC);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

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

        @EventHandler
        public void onMonsterExplosionDamage(EntityDamageEvent event) {
            if (!isApplied) return;

            Entity entity = event.getEntity();
            if (entity instanceof Monster || entity instanceof Slime || entity instanceof Ghast || entity instanceof Phantom || entity instanceof EnderDragon) {
                EntityDamageEvent.DamageCause cause = event.getCause();

                if (cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                        cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {

                    event.setCancelled(true);
                }
            }
        }

    }
