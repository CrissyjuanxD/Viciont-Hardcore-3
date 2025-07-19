    package Dificultades;

    import Dificultades.CustomMobs.Bombita;
    import Dificultades.CustomMobs.CorruptedSpider;
    import Dificultades.CustomMobs.CorruptedZombies;
    import Dificultades.CustomMobs.Iceologer;
    import org.bukkit.*;
    import org.bukkit.entity.*;
    import org.bukkit.event.EventHandler;
    import org.bukkit.event.Listener;
    import org.bukkit.event.raid.RaidSpawnWaveEvent;
    import org.bukkit.plugin.java.JavaPlugin;
    import org.bukkit.potion.PotionEffect;
    import org.bukkit.potion.PotionEffectType;

    import java.util.*;

    public class DayTwoChanges implements Listener {
        private final JavaPlugin plugin;
        private final Random random = new Random();
        private boolean isApplied = false;
        private final Bombita bombitaSpawner;
        private final Iceologer iceologerSpawner;
        private final CorruptedZombies corruptedZombies;
        private final CorruptedSpider corruptedSpiders;
        private final Map<LivingEntity, Long> trackedMobs = new HashMap<>();

        public DayTwoChanges(JavaPlugin plugin) {
            this.plugin = plugin;
            this.bombitaSpawner = new Bombita(plugin);
            this.iceologerSpawner = new Iceologer(plugin);
            this.corruptedZombies = new CorruptedZombies(plugin);
            this.corruptedSpiders = new CorruptedSpider(plugin);
        }

        public void apply() {
            if (!isApplied) {
                Bukkit.getPluginManager().registerEvents(this, plugin);
                bombitaSpawner.apply();
                iceologerSpawner.apply();
                isApplied = true;

                Bukkit.getScheduler().runTaskTimer(plugin, this::updateTargets, 20, 20);
            }
        }

        public void revert() {
            if (isApplied) {
                bombitaSpawner.revert();
                iceologerSpawner.revert();
                isApplied = false;
                trackedMobs.clear();
            }
        }

        // Evento de Raid
        @EventHandler
        public void onRaidWaveSpawn(RaidSpawnWaveEvent event) {
            if (!isApplied) return;

            int currentWave = event.getRaid().getSpawnedGroups();

            if (random.nextDouble() <= 0.10) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {

                    sendRaidWarning(event);

                    // Spawnear 15 Corrupted Zombies
                    spawnCorruptedMobs(event, "corruptedzombie", 15);

                    // Spawnear 5 Corrupted Spiders
                    spawnCorruptedMobs(event, "corruptedspider", 5);
                }, 40L);
            }

            // Spawnear Bombitas
            for (int i = 0; i < currentWave; i++) {
                for (Entity entity : event.getRaiders()) {
                    if (entity instanceof Raider) {
                        Location spawnLocation = entity.getLocation();
                        Creeper bombita = bombitaSpawner.spawnBombita(spawnLocation);
                        trackedMobs.put(bombita, System.currentTimeMillis());
                        break;
                    }
                }
            }

            if (currentWave >= 2) {
                int iceologerCount = random.nextInt(2) + 1;

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (int i = 0; i < iceologerCount; i++) {
                        for (Entity entity : event.getRaiders()) {
                            if (entity instanceof Pillager) {
                                Location spawnLocation = entity.getLocation();

                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                        "spawnvct iceologer " +
                                                spawnLocation.getBlockX() + " " +
                                                spawnLocation.getBlockY() + " " +
                                                spawnLocation.getBlockZ());

                                break;
                            }
                        }
                    }
                }, 80L);
            }


        }

        // Actualiza los objetivos dinámicamente
        private void updateTargets() {
            for (Iterator<Map.Entry<LivingEntity, Long>> iterator = trackedMobs.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<LivingEntity, Long> entry = iterator.next();
                LivingEntity mob = entry.getKey();

                if (mob.isDead() || !mob.isValid()) {
                    iterator.remove();
                    continue;
                }

                LivingEntity target = findTarget(mob);
                if (target != null && mob instanceof Mob) {
                    ((Mob) mob).setTarget(target);
                }
            }
        }

        private LivingEntity findTarget(Entity mob) {
            World world = mob.getWorld();

            Player closestPlayer = world.getPlayers().stream()
                    .filter(p -> !p.isDead() && p.getGameMode() == GameMode.SURVIVAL)
                    .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(mob.getLocation())))
                    .orElse(null);

            if (closestPlayer != null) {
                return closestPlayer;
            }

            // Si no hay jugadores, busca aldeanos
            return world.getNearbyEntities(mob.getLocation(), 50, 50, 50).stream()
                    .filter(e -> e instanceof Villager && !e.isDead())
                    .map(e -> (LivingEntity) e)
                    .min(Comparator.comparingDouble(v -> v.getLocation().distanceSquared(mob.getLocation())))
                    .orElse(null);
        }

        private void spawnCorruptedMobs(RaidSpawnWaveEvent event, String mobType, int count) {
            List<Location> spawnLocations = getSpawnLocations(event, count);

            for (Location location : spawnLocations) {
                LivingEntity corruptedMob = null;

                if (mobType.equals("corruptedzombie")) {
                    corruptedMob = corruptedZombies.spawnCorruptedZombie(location);
                } else if (mobType.equals("corruptedspider")) {
                    corruptedMob = corruptedSpiders.spawnCorruptedSpider(location);
                }

                if (corruptedMob != null) {
                    trackedMobs.put(corruptedMob, System.currentTimeMillis());
                }
            }
        }


        // Obtener ubicaciones de spawn cerca de los Raiders
        private List<Location> getSpawnLocations(RaidSpawnWaveEvent event, int count) {
            List<Location> locations = new ArrayList<>();
            List<Entity> raiders = new ArrayList<>(event.getRaiders());

            for (int i = 0; i < count && !raiders.isEmpty(); i++) {
                Entity raider = raiders.get(random.nextInt(raiders.size()));
                Location spawnLocation = raider.getLocation().clone();

                // Añade un pequeño desplazamiento aleatorio para dispersar los mobs
                spawnLocation.add(random.nextInt(6) - 3, 0, random.nextInt(6) - 3);
                locations.add(spawnLocation);
            }

            return locations;
        }

        private void sendRaidWarning(RaidSpawnWaveEvent event) {
            Sound sound = Sound.ENTITY_ENDER_DRAGON_GROWL;

            String jsonMessage = "[\"\",{\"text\":\"\\u06de\",\"bold\":true,\"color\":\"#C17CE5\"}," +
                    "{\"text\":\" Ha aparecido una oleada de\",\"color\":\"#E28761\"}," +
                    "{\"text\":\" Corrupted Mobs \",\"bold\":true,\"color\":\"dark_purple\"}," +
                    "{\"text\":\"\\u26a0\",\"bold\":true,\"color\":\"dark_red\"}]";

            for (Player player : event.getRaid().getLocation().getWorld().getPlayers()) {
                if (event.getRaid().getLocation().distanceSquared(player.getLocation()) <= 10000) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "tellraw " + player.getName() + " " + jsonMessage);

                    player.playSound(player.getLocation(), sound, 2.0f, 0.1f);
                }
            }
        }

    }


