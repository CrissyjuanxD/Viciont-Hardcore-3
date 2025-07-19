package Commands;

import Dificultades.CustomMobs.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import Handlers.DayHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpawnMobs implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final Bombita bombitaSpawner;
    private final Iceologer iceologerSpawner;
    private final CorruptedZombies corruptedZombieSpawner;
    private final CorruptedSpider corruptedSpider;
    private final QueenBeeHandler queenBeeHandler;
    private final HellishBeeHandler hellishBeeHandler;
    private final GuardianBlaze guardianBlaze;
    private final GuardianCorruptedSkeleton guardianCorruptedSkeleton;
    private final CorruptedSkeleton corruptedSkeleton;
    private final CorruptedInfernalSpider corruptedInfernalSpider;
    private final CorruptedCreeper corruptedCreeper;
    private final CorruptedMagmaCube corruptedMagmaCube;
    private final PiglinGlobo piglinGloboSpawner;
    private final BuffBreeze buffBreeze;
    private final InvertedGhast invertedGhast;
    private final NetheriteVexGuardian netheriteVexGuardian;
    private final UltraWitherBossHandler ultraWitherBossHandler;
    private final WhiteEnderman whiteEnderman;
    private final InfernalCreeper infernalCreeper;
    private final UltraCorruptedSpider ultraCorruptedSpider;
    private final FastRavager fastRavager;
    private final BruteImperial bruteImperial;
    private final BatBoom batBoom;
    private final SpectralEye spectralEye;
    private final CustomBoat customBoat;
    private final EnderGhast enderGhast;
    private final EnderCreeper enderCreeper;
    private final EnderSilverfish enderSilverfish;
    private final GuardianShulker guardianShulker;
    private final DarkPhantom darkPhantom;
    private final DarkCreeper darkCreeper;
    private final DarkVex darkVex;
    private final DarkSkeleton darkSkeleton;
    private final InfestedBeeHandler infestedBeeHandler;
    private final DayHandler dayHandler;

    private final CustomDolphin customDolphin;

    public SpawnMobs(JavaPlugin plugin, DayHandler dayHandler) {
        this.plugin = plugin;
        this.bombitaSpawner = new Bombita(plugin);
        this.iceologerSpawner = new Iceologer(plugin);
        this.corruptedZombieSpawner = new CorruptedZombies(plugin);
        this.corruptedSpider = new CorruptedSpider(plugin);
        this.queenBeeHandler = new QueenBeeHandler(plugin);
        this.hellishBeeHandler = new HellishBeeHandler(plugin);
        this.guardianBlaze = new GuardianBlaze(plugin);
        this.guardianCorruptedSkeleton = new GuardianCorruptedSkeleton(plugin);
        this.dayHandler = dayHandler;
        this.corruptedSkeleton = new CorruptedSkeleton(plugin, dayHandler);
        this.customDolphin = new CustomDolphin(plugin);
        this.corruptedInfernalSpider = new CorruptedInfernalSpider(plugin);
        this.corruptedCreeper = new CorruptedCreeper(plugin);
        this.corruptedMagmaCube = new CorruptedMagmaCube(plugin);
        this.piglinGloboSpawner = new PiglinGlobo(plugin);
        this.buffBreeze = new BuffBreeze(plugin);
        this.invertedGhast = new InvertedGhast(plugin);
        this.netheriteVexGuardian = new NetheriteVexGuardian(plugin);
        this.ultraWitherBossHandler = new UltraWitherBossHandler(plugin);
        this.whiteEnderman = new WhiteEnderman(plugin);
        this.infernalCreeper = new InfernalCreeper(plugin);
        this.ultraCorruptedSpider = new UltraCorruptedSpider(plugin);
        this.fastRavager = new FastRavager(plugin);
        this.bruteImperial = new BruteImperial(plugin);
        this.batBoom = new BatBoom(plugin);
        this.spectralEye = new SpectralEye(plugin);
        this.customBoat = new CustomBoat(plugin);
        this.enderGhast = new EnderGhast(plugin);
        this.enderCreeper = new EnderCreeper(plugin);
        this.enderSilverfish = new EnderSilverfish(plugin);
        this.guardianShulker = new GuardianShulker(plugin);
        this.darkPhantom = new DarkPhantom(plugin);
        this.darkCreeper = new DarkCreeper(plugin);
        this.darkVex = new DarkVex(plugin);
        this.darkSkeleton = new DarkSkeleton(plugin);
        this.infestedBeeHandler = new InfestedBeeHandler(plugin);
        plugin.getCommand("spawnvct").setExecutor(this);
        plugin.getCommand("spawnvct").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Uso: /spawnvct <mob> [jugador (opcional)] [x] [y] [z]");
            return true;
        }

        String mobType = args[0].toLowerCase();

        Location location = null;
        Player targetPlayer = null;

        if (args.length > 1 && Bukkit.getPlayer(args[1]) != null) {
            targetPlayer = Bukkit.getPlayer(args[1]);
            location = targetPlayer.getLocation();
        } else if (args.length >= 4) {
            try {
                World world = sender instanceof Player ? ((Player) sender).getWorld() : Bukkit.getWorlds().get(0);
                double x = Double.parseDouble(args[args.length - 3]);
                double y = Double.parseDouble(args[args.length - 2]);
                double z = Double.parseDouble(args[args.length - 1]);
                location = new Location(world, x, y, z);
            } catch (NumberFormatException e) {
                sender.sendMessage("Las coordenadas deben ser números válidos.");
                return true;
            }
        } else if (sender instanceof Player) {
            targetPlayer = (Player) sender;
            location = targetPlayer.getLocation();
        } else {
            sender.sendMessage("Debes especificar un jugador o coordenadas si no eres un jugador.");
            return true;
        }

        switch (mobType) {
            case "bombita":
                bombitaSpawner.spawnBombita(location);
                sender.sendMessage("¡Bombita ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "iceologer":
                iceologerSpawner.spawnIceologer(location);
                sender.sendMessage("¡Iceologer ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "corruptedzombie":
                corruptedZombieSpawner.spawnCorruptedZombie(location);
                sender.sendMessage("¡Corrupted Zombie ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "corruptedspider":
                corruptedSpider.spawnCorruptedSpider(location);
                sender.sendMessage("¡Corrupted Spider ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "queenbee":
                queenBeeHandler.spawnQueenBee(location);
                sender.sendMessage("¡Queen Bees ha sido spawneada en " + locationToString(location) + "!");
                break;

            case "hellishbee":
                hellishBeeHandler.spawnHellishBee(location);
                sender.sendMessage("¡Hellish Bee ha sido spawneada en " + locationToString(location) + "!");
                break;

            case "guardianblaze":
                guardianBlaze.spawnGuardianBlaze(location);
                sender.sendMessage("¡Guardian Blaze ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "guardiancorruptedskeleton":
                guardianCorruptedSkeleton.spawnGuardianCorruptedSkeleton(location);
                sender.sendMessage("¡Guardian Corrupted Skeleton ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "corruptedskeleton":
                if (args.length >= 2) {
                    String variantName = args[1].toUpperCase();
                    corruptedSkeleton.spawnCorruptedSkeleton(location, variantName);
                    sender.sendMessage("¡Corrupted Skeleton (" + variantName + ") ha sido spawneado en " + locationToString(location) + "!");
                } else {
                    corruptedSkeleton.spawnCorruptedSkeleton(location, null);
                    sender.sendMessage("¡Corrupted Skeleton (aleatorio) ha sido spawneado en " + locationToString(location) + "!");
                }
                break;

            case "customdolphin":
                String dolphinType = args[1];
                if (!dolphinType.equalsIgnoreCase("Pingo") && !dolphinType.equalsIgnoreCase("Pinga")) {
                    sender.sendMessage("Tipo de delfín no válido. Usa Pingo o Pinga.");
                    return true;
                }

                customDolphin.spawnPinguin(location, dolphinType);
                sender.sendMessage("¡Delfín " + dolphinType + " ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "corruptedinfernalspider":
                corruptedInfernalSpider.spawnCorruptedInfernalSpider(location);
                sender.sendMessage("¡Corrupted Infernal Spider ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "corruptedcreeper":
                corruptedCreeper.spawnCorruptedCreeper(location);
                sender.sendMessage("¡Corrupted Creeper ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "corruptedmagma":
                corruptedMagmaCube.spawnCorruptedMagmaCube(location);
                sender.sendMessage("¡Corrupted Magma Cube ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "piglinglobo":
                piglinGloboSpawner.spawnPiglinGlobo(location);
                sender.sendMessage("¡Piglin Globo ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "buffbreeze":
                buffBreeze.spawnBuffBreeze(location);
                sender.sendMessage("¡Buff Breeze ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "invertedghast":
                invertedGhast.spawnInvertedGhast(location);
                sender.sendMessage("¡Inverted Ghast ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "netheritevexguardian":
                netheriteVexGuardian.spawnNetheriteVexGuardian(location);
                sender.sendMessage("¡Netherite Vex Guardian ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "ultrawitherboss":
                ultraWitherBossHandler.spawnUltraWither(location);
                sender.sendMessage("¡Ultra Wither Boss ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "whiteenderman":
                whiteEnderman.spawnWhiteEnderman(location);
                sender.sendMessage("¡White Enderman ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "infernalcreeper":
                infernalCreeper.spawnInfernalCreeper(location);
                sender.sendMessage("¡Infernal Creeper ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "ultracorruptedspider":
                ultraCorruptedSpider.spawnUltraCorruptedSpider(location);
                sender.sendMessage("¡Ultra Corrupted Spider ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "fastravager":
                fastRavager.spawnFastRavager(location);
                sender.sendMessage("¡Fast Ravager ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "bruteimperial":
                bruteImperial.spawnBruteImperial(location);
                sender.sendMessage("¡Brute Imperial ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "batboom":
                batBoom.spawnBatBoom(location);
                sender.sendMessage("¡Bat Boom ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "spectraleeye":
                spectralEye.spawnSpectralEye(location);
                sender.sendMessage("¡Spectral Eye ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "customboat":
                customBoat.spawnBoat(location, Objects.requireNonNull(targetPlayer));
                sender.sendMessage("¡Custom Boat ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "enderghast":
                enderGhast.spawnEnderGhast(location);
                sender.sendMessage("¡Ender Ghast ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "endercreeper":
                enderCreeper.spawnEnderCreeper(location);
                sender.sendMessage("¡Ender Creeper ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "endersilverfish":
                enderSilverfish.spawnEnderSilverfish(location);
                sender.sendMessage("¡Ender Silverfish ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "guardianshulker":
                guardianShulker.spawnGuardianShulker(location);
                sender.sendMessage("¡Guardian Shulker ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "darkphantom":
                darkPhantom.spawnDarkPhantom(location);
                sender.sendMessage("¡Dark Phantom ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "darkcreeper":
                darkCreeper.spawnDarkCreeper(location);
                sender.sendMessage("¡Dark Creeper ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "darkvex":
                darkVex.spawnDarkVex(location);
                sender.sendMessage("¡Dark Vex ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "darkskeleton":
                darkSkeleton.spawnDarkSkeleton(location);
                sender.sendMessage("¡Dark Skeleton ha sido spawneado en " + locationToString(location) + "!");
                break;

            case "infestedbee":
                infestedBeeHandler.spawnInfestedBee(location);
                sender.sendMessage("¡Infested Bee ha sido spawneada en " + locationToString(location) + "!");
                break;

            default:
                sender.sendMessage("Mob no reconocido. Usa /spawnvct <bombita|iceologer|corruptedzombie|corruptedspider|queenbee>");
                break;
        }

        return true;
    }

    private String locationToString(Location location) {
        return "(" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("bombita");
            suggestions.add("iceologer");
            suggestions.add("corruptedzombie");
            suggestions.add("corruptedspider");
            suggestions.add("queenbee");
            suggestions.add("hellishbee");
            suggestions.add("guardianblaze");
            suggestions.add("guardiancorruptedskeleton");
            suggestions.add("corruptedskeleton");
            suggestions.add("customdolphin");
            suggestions.add("corruptedinfernalspider");
            suggestions.add("corruptedcreeper");
            suggestions.add("corruptedmagma");
            suggestions.add("piglinglobo");
            suggestions.add("buffbreeze");
            suggestions.add("invertedghast");
            suggestions.add("netheritevexguardian");
            suggestions.add("ultrawitherboss");
            suggestions.add("whiteenderman");
            suggestions.add("infernalcreeper");
            suggestions.add("ultracorruptedspider");
            suggestions.add("fastravager");
            suggestions.add("bruteimperial");
            suggestions.add("batboom");
            suggestions.add("spectraleeye");
            suggestions.add("customboat");
            suggestions.add("enderghast");
            suggestions.add("endercreeper");
            suggestions.add("endersilverfish");
            suggestions.add("guardianshulker");
            suggestions.add("darkphantom");
            suggestions.add("darkcreeper");
            suggestions.add("darkvex");
            suggestions.add("darkskeleton");
            suggestions.add("infestedbee");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("corruptedskeleton")) {
            for (CorruptedSkeleton.Variant variant : CorruptedSkeleton.Variant.values()) {
                suggestions.add(variant.name().toLowerCase());
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("customdolphin")) {
            suggestions.add("Pingo");
            suggestions.add("Pinga");
        } else if (args.length == 2) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                suggestions.add(player.getName());
            }
        } else if (args.length == 3 || args.length == 4 || args.length == 5) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                suggestions.add(String.valueOf(player.getLocation().getBlockX()));
                suggestions.add(String.valueOf(player.getLocation().getBlockY()));
                suggestions.add(String.valueOf(player.getLocation().getBlockZ()));
            }
        }

        return suggestions;
    }
}
