package Events.MissionSystem;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

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
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Entity entity = event.getRightClicked();
        if (entity.getType() != EntityType.FOX) return;
        if (!entity.getScoreboardTags().contains("reward_statue") &&
                !entity.getName().contains("Estatua de Recompensas")) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isMissionToken(item)) {
            player.sendMessage(ChatColor.RED + "✖ " + ChatColor.GRAY + "Solo puedes interactuar usando una " +
                    ChatColor.GOLD + ChatColor.BOLD + "Ficha de Misión" + ChatColor.GRAY +
                    ", las cuales se consiguen completando misiones, para recibir tu recompensa.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1.0f, 0.6f);
            return;
        }

        int missionNumber = getMissionNumberFromToken(item);
        if (missionNumber == -1) return;

        event.setCancelled(true);

        MissionData data = missionHandler.getData(player, missionNumber);

        if (!data.isCompleted()) {
            player.sendMessage(ChatColor.RED + "No has completado esta misión.");
            return;
        }

        if (data.isRewardClaimed()) {
            player.sendMessage(ChatColor.RED + "Ya has reclamado la recompensa de esta misión.");
            return;
        }

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        startRewardAnimation(player, entity.getLocation(), missionNumber);

        data.setRewardClaimed(true);
        missionHandler.saveData(player, missionNumber, data);
    }

    private boolean isMissionToken(ItemStack item) {
        if (item == null || item.getType() != Material.POPPED_CHORUS_FRUIT) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return false;
        int cmd = meta.getCustomModelData();
        return cmd >= 3001 && cmd <= 3027;
    }

    private int getMissionNumberFromToken(ItemStack item) {
        if (!isMissionToken(item)) return -1;

        ItemMeta meta = item.getItemMeta();
        return meta.getCustomModelData() - 3000; // 3001 -> 1, 3002 -> 2, etc.
    }

    private void startRewardAnimation(Player player, Location blockLocation, int missionNumber) {
        Location center = blockLocation.clone().add(0.5, 0.5, 0.5);
        World world = center.getWorld();

        new BukkitRunnable() {
            int ticks = 0;
            int particleCount = 0;

            @Override
            public void run() {
                if (ticks >= 40) {
                    this.cancel();
                    return;
                }

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
        startRotatingPoofAnimation(player, center, missionNumber);
    }

    private void startRotatingPoofAnimation(Player player, Location center, int missionNumber) {
        World world = center.getWorld();

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
                if (ticks >= 200) {
                    createFinalExplosionAndBeam(center, missionNumber, player);
                    this.cancel();
                    return;
                }

                int colorIndex = (ticks / 40) % colors.length;
                Particle.DustOptions currentColor = colors[colorIndex];

                if (ticks % 40 == 0 && ticks > 0) {
                    world.playSound(center, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.VOICE,1.0f, 1.0f + (colorIndex * 0.2f));
                }

                if (ticks - lastSoundTick >= 20) {
                    world.playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.VOICE,0.3f, 1.5f);
                    lastSoundTick = ticks;
                }

                for (int i = 0; i < 360; i += 15) {
                    double angle = Math.toRadians(i + rotation);
                    double x = 5 * Math.cos(angle);
                    double z = 5 * Math.sin(angle);
                    Location particleLoc = center.clone().add(x, 0, z);
                    world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, currentColor);
                }

                rotation += 8;
                ticks++;
            }
        }.runTaskTimer(plugin, 40L, 1L);
    }

    private void createFinalExplosionAndBeam(Location center, int missionNumber, Player originalPlayer) {
        World world = center.getWorld();

        for (int i = 0; i < 360; i += 10) {
            double angle = Math.toRadians(i);
            double x = 5 * Math.cos(angle);
            double z = 5 * Math.sin(angle);
            Location particleLoc = center.clone().add(x, 0, z);
            world.spawnParticle(Particle.EXPLOSION, particleLoc, 1);
        }

        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.VOICE, 2.0f, 1.0f);

        new BukkitRunnable() {
            int height = 0;

            @Override
            public void run() {
                if (height >= 7) {
                    createPlayerBeam(center.clone().add(0, 7, 0), missionNumber, originalPlayer);
                    this.cancel();
                    return;
                }

                Location beamLoc = center.clone().add(0, height, 0);
                Particle.DustOptions blackDust = new Particle.DustOptions(Color.BLACK, 2.0f);
                world.spawnParticle(Particle.DUST, beamLoc, 10, 0.2, 0.2, 0.2, 0, blackDust);

                world.playSound(beamLoc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.VOICE, 0.5f, 2.0f);

                height++;
            }
        }.runTaskTimer(plugin, 20L, 3L);
    }

    private void createPlayerBeam(Location beamEnd, int missionNumber, Player originalPlayer) {
        World world = beamEnd.getWorld();
        Player targetPlayer = originalPlayer;

        if (targetPlayer != null) {
            final Player finalTargetPlayer = targetPlayer;

            new BukkitRunnable() {
                double progress = 0;

                @Override
                public void run() {
                    if (progress >= 1.0) {
                        finalTargetPlayer.getWorld().spawnParticle(Particle.FIREWORK,
                                finalTargetPlayer.getLocation(), 50, 1, 1, 1, 0.1);
                        finalTargetPlayer.getWorld().playSound(finalTargetPlayer.getLocation(),
                                Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.VOICE, 2.0f, 1.0f);

                        giveRewardChest(finalTargetPlayer, missionNumber);
                        this.cancel();
                        return;
                    }

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
        Mission mission = missionHandler.getMissions().get(missionNumber);
        if (mission == null) return;

        List<ItemStack> rewards = mission.getRewards();

        ItemStack rewardChest = new ItemStack(Material.CHEST);
        ItemMeta chestMeta = rewardChest.getItemMeta();
        chestMeta.setDisplayName(ChatColor.of("#FFD700") + "Recompensa de Misión #" + missionNumber);

        if (chestMeta instanceof BlockStateMeta) {
            BlockStateMeta blockStateMeta = (BlockStateMeta) chestMeta;
            Chest chestState = (Chest) blockStateMeta.getBlockState();

            for (int i = 0; i < Math.min(rewards.size(), 27); i++) {
                chestState.getInventory().setItem(i, rewards.get(i));
            }

            blockStateMeta.setBlockState(chestState);
            rewardChest.setItemMeta(blockStateMeta);
        }

        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(rewardChest);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), rewardChest);
            player.sendMessage(ChatColor.of("#FFA07A") + "¡Tu inventario estaba lleno! El cofre se ha dejado caer al suelo.");
        }

        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.VOICE, 1.0f, 1.0f);
        player.sendMessage(ChatColor.of("#98FB98") + "¡Has reclamado la recompensa de la Misión #" + missionNumber + "!");
    }
}