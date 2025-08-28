package Events.MissionSystem;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatColor;

import java.io.IOException;
import java.util.List;

public class MissionRewardHandler implements Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;

    public MissionRewardHandler(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.PINK_GLAZED_TERRACOTTA) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isMissionToken(item)) return;

        int missionNumber = getMissionNumberFromToken(item);
        if (missionNumber == -1) return;

        // Verificar que la misión esté completada y no haya reclamado la recompensa
        FileConfiguration data = YamlConfiguration.loadConfiguration(missionHandler.getMissionFile());
        String playerName = player.getName();

        if (!data.getBoolean("players." + playerName + ".missions." + missionNumber + ".completed", false)) {
            player.sendMessage(ChatColor.RED + "No has completado esta misión.");
            return;
        }

        if (data.getBoolean("players." + playerName + ".missions." + missionNumber + ".reward_claimed", false)) {
            player.sendMessage(ChatColor.RED + "Ya has reclamado la recompensa de esta misión.");
            return;
        }

        event.setCancelled(true);

        // Consumir la ficha
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // Iniciar animación de recompensa
        startRewardAnimation(player, block.getLocation(), missionNumber);

        // Marcar recompensa como reclamada
        data.set("players." + playerName + ".missions." + missionNumber + ".reward_claimed", true);
        try {
            data.save(missionHandler.getMissionFile());
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar reclamación de recompensa: " + e.getMessage());
        }
    }

    private boolean isMissionToken(ItemStack item) {
        if (item == null || item.getType() != Material.ECHO_SHARD) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return false;

        int cmd = meta.getCustomModelData();
        return cmd >= 3001 && cmd <= 3027; // Fichas de misión 1-27
    }

    private int getMissionNumberFromToken(ItemStack item) {
        if (!isMissionToken(item)) return -1;

        ItemMeta meta = item.getItemMeta();
        return meta.getCustomModelData() - 3000; // 3001 -> 1, 3002 -> 2, etc.
    }

    private void startRewardAnimation(Player player, Location blockLocation, int missionNumber) {
        Location center = blockLocation.clone().add(0.5, 0.5, 0.5);
        World world = center.getWorld();

        // Fase 1: Círculo de Sonic Boom (radio 5) - solo 2 segundos
        new BukkitRunnable() {
            int ticks = 0;
            int particleCount = 0;

            @Override
            public void run() {
                if (ticks >= 40) { // 2 segundos
                    this.cancel();
                    return;
                }

                // Generar solo 4 partículas por ejecución (en lugar de 36)
                for (int i = 0; i < 4; i++) {
                    double angle = Math.toRadians(particleCount * 10);
                    double x = 5 * Math.cos(angle);
                    double z = 5 * Math.sin(angle);
                    Location particleLoc = center.clone().add(x, 0, z);
                    world.spawnParticle(Particle.SONIC_BOOM, particleLoc, 1);
                    world.playSound(particleLoc, Sound.BLOCK_CANDLE_EXTINGUISH, SoundCategory.VOICE, 1.5f, 0.6f);
                    particleCount++;
                }

                if (ticks == 0) {
                    world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.VOICE, 1.0f, 1.0f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 10L, 5L);

        // Fase 2: Círculo de Poof giratorio con cambios de color
        startRotatingPoofAnimation(player, center, missionNumber);
    }

    private void startRotatingPoofAnimation(Player player, Location center, int missionNumber) {
        World world = center.getWorld();

        // Colores que cambian cada 2 segundos
        Particle.DustOptions[] colors = {
                new Particle.DustOptions(Color.ORANGE, 1.0f),
                new Particle.DustOptions(Color.GREEN, 1.0f),
                new Particle.DustOptions(Color.PURPLE, 1.0f),
                new Particle.DustOptions(Color.YELLOW, 1.0f),
                new Particle.DustOptions(Color.RED, 1.0f)
        };

        new BukkitRunnable() {
            int ticks = 0;
            double rotation = 0;
            int lastSoundTick = 0;

            @Override
            public void run() {
                if (ticks >= 200) { // 10 segundos
                    // Fase 3: Explosión final y rayo
                    createFinalExplosionAndBeam(center, missionNumber, player);
                    this.cancel();
                    return;
                }

                // Cambiar color cada 40 ticks (2 segundos)
                int colorIndex = (ticks / 40) % colors.length;
                Particle.DustOptions currentColor = colors[colorIndex];

                // Sonido cuando cambia de color
                if (ticks % 40 == 0 && ticks > 0) {
                    world.playSound(center, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.VOICE,1.0f, 1.0f + (colorIndex * 0.2f));
                }

                // Sonido de rotación cada 20 ticks
                if (ticks - lastSoundTick >= 20) {
                    world.playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.VOICE,0.3f, 1.5f);
                    lastSoundTick = ticks;
                }

                // Crear círculo giratorio
                for (int i = 0; i < 360; i += 15) {
                    double angle = Math.toRadians(i + rotation);
                    double x = 5 * Math.cos(angle);
                    double z = 5 * Math.sin(angle);
                    Location particleLoc = center.clone().add(x, 0, z);
                    world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, currentColor);
                }

                rotation += 8; // Velocidad de rotación
                ticks++;
            }
        }.runTaskTimer(plugin, 40L, 1L); // Empezar después de sonic boom
    }

    private void createFinalExplosionAndBeam(Location center, int missionNumber, Player originalPlayer) {
        World world = center.getWorld();

        // Explosión en círculo
        for (int i = 0; i < 360; i += 10) {
            double angle = Math.toRadians(i);
            double x = 5 * Math.cos(angle);
            double z = 5 * Math.sin(angle);
            Location particleLoc = center.clone().add(x, 0, z);
            world.spawnParticle(Particle.EXPLOSION, particleLoc, 1);
        }

        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.VOICE, 2.0f, 1.0f);

        // Rayo de partículas negras hacia arriba
        new BukkitRunnable() {
            int height = 0;

            @Override
            public void run() {
                if (height >= 7) {
                    // Rayo hacia el jugador
                    createPlayerBeam(center.clone().add(0, 7, 0), missionNumber, originalPlayer);
                    this.cancel();
                    return;
                }

                Location beamLoc = center.clone().add(0, height, 0);
                Particle.DustOptions blackDust = new Particle.DustOptions(Color.BLACK, 2.0f);
                world.spawnParticle(Particle.DUST, beamLoc, 10, 0.2, 0.2, 0.2, 0, blackDust);

                // Sonido del rayo
                world.playSound(beamLoc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.VOICE, 0.5f, 2.0f);

                height++;
            }
        }.runTaskTimer(plugin, 20L, 3L); // Más rápido - cada 3 ticks
    }

    private void createPlayerBeam(Location beamEnd, int missionNumber, Player originalPlayer) {
        World world = beamEnd.getWorld();
        Player targetPlayer = originalPlayer;

        if (targetPlayer != null) {
            final Player finalTargetPlayer = targetPlayer;

            // Crear rayo morado hacia el jugador
            new BukkitRunnable() {
                double progress = 0;

                @Override
                public void run() {
                    if (progress >= 1.0) {
                        // Explosión de fireworks en el jugador
                        finalTargetPlayer.getWorld().spawnParticle(Particle.FIREWORK,
                                finalTargetPlayer.getLocation(), 50, 1, 1, 1, 0.1);
                        finalTargetPlayer.getWorld().playSound(finalTargetPlayer.getLocation(),
                                Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.VOICE, 2.0f, 1.0f);

                        // Dar recompensa
                        giveRewardChest(finalTargetPlayer, missionNumber);
                        this.cancel();
                        return;
                    }

                    // Crear línea de partículas moradas
                    Location start = beamEnd;
                    Location end = finalTargetPlayer.getEyeLocation();

                    for (double i = 0; i <= progress; i += 0.1) {
                        Location particleLoc = start.clone().add(
                                (end.getX() - start.getX()) * i,
                                (end.getY() - start.getY()) * i,
                                (end.getZ() - start.getZ()) * i
                        );

                        Particle.DustOptions purpleDust = new Particle.DustOptions(Color.PURPLE, 1.5f);
                        world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, purpleDust);
                    }

                    world.playSound(beamEnd, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.VOICE, 0.3f, 2.0f);
                    progress += 0.05;
                }
            }.runTaskTimer(plugin, 10L, 1L);
        }
    }

    private void giveRewardChest(Player player, int missionNumber) {
        // Obtener las recompensas de la misión
        Mission mission = missionHandler.getMissions().get(missionNumber);
        if (mission == null) return;

        List<ItemStack> rewards = mission.getRewards();

        // Crear cofre con las recompensas dentro usando NBT
        ItemStack rewardChest = new ItemStack(Material.CHEST);
        ItemMeta chestMeta = rewardChest.getItemMeta();
        chestMeta.setDisplayName(ChatColor.of("#FFD700") + "Recompensa de Misión #" + missionNumber);

        // Usar BlockStateMeta para añadir items al cofre
        if (chestMeta instanceof BlockStateMeta) {
            BlockStateMeta blockStateMeta = (BlockStateMeta) chestMeta;
            Chest chestState = (Chest) blockStateMeta.getBlockState();

            // Añadir items al inventario del cofre
            for (int i = 0; i < Math.min(rewards.size(), 27); i++) {
                chestState.getInventory().setItem(i, rewards.get(i));
            }

            blockStateMeta.setBlockState(chestState);
            rewardChest.setItemMeta(blockStateMeta);
        }

        // Intentar dar el cofre al jugador
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(rewardChest);
        } else {
            // Si no hay espacio, dropear al suelo
            player.getWorld().dropItemNaturally(player.getLocation(), rewardChest);
            player.sendMessage(ChatColor.of("#FFA07A") + "¡Tu inventario estaba lleno! El cofre se ha dejado caer al suelo.");
        }

        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.VOICE, 1.0f, 1.0f);
        player.sendMessage(ChatColor.of("#98FB98") + "¡Has reclamado la recompensa de la Misión #" + missionNumber + "!");
    }
}