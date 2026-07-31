package Managers;

import Bosses.QueenBeeHandler;
import Dificultades.CustomMobs.*;
import Handlers.DayHandler;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class MobManager {

    private final JavaPlugin plugin;
    private final DayHandler dayHandler;

    // Instancias de Mobs
    private final Bombita bombitaSpawner;
    private final Iceologer iceologerSpawner;
    private final CorruptedZombies corruptedZombieSpawner;
    private final CorruptedSpider corruptedSpider;
    private final GuardianBlaze guardianBlaze;
    private final GuardianCorruptedSkeleton guardianCorruptedSkeleton;
    private final CorruptedSkeleton corruptedSkeleton;
    private final CorruptedInfernalSpider corruptedInfernalSpider;
    private final CorruptedCreeper corruptedCreeper;
    private final PiglinGlobo piglinGloboSpawner;
    private final BuffBreeze buffBreeze;
    private final InvertedGhast invertedGhast;
    private final NetheriteVexGuardian netheriteVexGuardian;
    private final UltraWitherBossHandler ultraWitherBossHandler;
    private final WhiteEnderman whiteEnderman;
    private final InfernalCreeper infernalCreeper;
    private final ToxicSpider toxicSpider;
    private final FastRavager fastRavager;
    private final ImperialBrute imperialBrute;
    private final BatBoom batBoom;
    private final SpectralEye spectralEye;
    private final CustomBoat customBoat;
    private final EspectralGhast espectralGhast;
    private final EspectralCreeper espectralCreeper;
    private final EspectralSilverfish espectralSilverfish;
    private final GuardianShulker_Descartado guardianShulkerDescartado;
    private final DarkCreeper darkCreeper;
    private final DarkVex darkVex;
    private final DarkSkeleton darkSkeleton;
    private final InfestedBeeHandler infestedBeeHandler;
    private final InfernalBeast infernalBeast;
    private final CorruptedDrowned corruptedDrowned;
    private final CorruptedBee corruptedBee;
    private final InfestedGolems infestedGolems;
    private final CustomDolphin customDolphin;
    private final CorruptedInsect corruptedInsect;

    private final List<String> registeredMobs;

    @FunctionalInterface
    public interface SpawnCallback {
        void onSpawned(Entity entity);
    }

    private volatile SpawnCallback pendingCallback = null;
    private volatile Location expectedSpawnLocation = null;

    public void notifyEntitySpawned(Entity entity) {
        if (pendingCallback == null || expectedSpawnLocation == null) return;
        Location eLoc = entity.getLocation();
        Location expLoc = expectedSpawnLocation;
        if (eLoc.getWorld() == null || !eLoc.getWorld().equals(expLoc.getWorld())) return;
        if (eLoc.distanceSquared(expLoc) > 36) return;

        SpawnCallback cb = pendingCallback;
        pendingCallback = null;
        expectedSpawnLocation = null;
        cb.onSpawned(entity);
    }

    public MobManager(JavaPlugin plugin, DayHandler dayHandler) {
        this.plugin = plugin;
        this.dayHandler = dayHandler;

        // Inicialización de todas las clases...
        this.bombitaSpawner = new Bombita(plugin);
        this.iceologerSpawner = new Iceologer(plugin);
        this.corruptedZombieSpawner = new CorruptedZombies(plugin, dayHandler);
        this.corruptedSpider = new CorruptedSpider(plugin, dayHandler);
        this.guardianBlaze = new GuardianBlaze(plugin);
        this.guardianCorruptedSkeleton = new GuardianCorruptedSkeleton(plugin);
        this.corruptedSkeleton = new CorruptedSkeleton(plugin, dayHandler);
        this.customDolphin = new CustomDolphin(plugin);
        this.corruptedInfernalSpider = new CorruptedInfernalSpider(plugin);
        this.corruptedCreeper = new CorruptedCreeper(plugin);
        this.piglinGloboSpawner = new PiglinGlobo(plugin);
        this.buffBreeze = new BuffBreeze(plugin);
        this.invertedGhast = new InvertedGhast(plugin);
        this.netheriteVexGuardian = new NetheriteVexGuardian(plugin);
        this.ultraWitherBossHandler = new UltraWitherBossHandler(plugin);
        this.whiteEnderman = new WhiteEnderman(plugin);
        this.infernalCreeper = new InfernalCreeper(plugin);
        this.toxicSpider = new ToxicSpider(plugin);
        this.fastRavager = new FastRavager(plugin);
        this.imperialBrute = new ImperialBrute(plugin);
        this.batBoom = new BatBoom(plugin);
        this.spectralEye = new SpectralEye(plugin);
        this.customBoat = new CustomBoat(plugin);
        this.espectralGhast = new EspectralGhast(plugin);
        this.espectralCreeper = new EspectralCreeper(plugin);
        this.espectralSilverfish = new EspectralSilverfish(plugin);
        this.guardianShulkerDescartado = new GuardianShulker_Descartado(plugin);
        this.darkCreeper = new DarkCreeper(plugin);
        this.darkVex = new DarkVex(plugin);
        this.darkSkeleton = new DarkSkeleton(plugin);
        this.infestedBeeHandler = new InfestedBeeHandler(plugin);
        this.infernalBeast = new InfernalBeast(plugin);
        this.corruptedDrowned = new CorruptedDrowned(plugin);
        this.corruptedBee = new CorruptedBee(plugin);
        this.infestedGolems = new InfestedGolems(plugin);
        this.corruptedInsect = new CorruptedInsect(plugin, dayHandler);

        this.registeredMobs = new ArrayList<>();
        cargarNombresDeMobs();
    }

    private void cargarNombresDeMobs() {
        String[] mobs = {
                "bombita", "iceologer", "corruptedzombie", "corruptedspider", "corruptedinsect", "queenbee", "hellishbee",
                "guardianblaze", "guardiancorruptedskeleton", "corruptedskeleton", "customdolphin",
                "corruptedinfernalspider", "corruptedcreeper", "piglinglobo", "buffbreeze", "invertedghast",
                "netheritevexguardian", "ultrawitherboss", "whiteenderman", "infernalcreeper", "toxicspider",
                "fastravager", "bruteimperial", "batboom", "spectraleeye", "customboat", "enderghast",
                "endercreeper", "endersilverfish", "guardianshulker", "darkcreeper", "darkvex", "darkskeleton",
                "infestedbee", "infernalbeast", "corrupteddrowned", "corruptedbee", "infestedgolems", "null_statue",
                "estatuarecompensa"
        };
        for (String mob : mobs) {
            registeredMobs.add(mob);
        }
    }

    public boolean spawnMob(String mobType, Location location, Player targetPlayer, String variantArgs) {
        switch (mobType.toLowerCase()) {
            case "bombita": bombitaSpawner.spawnBombita(location); return true;
            case "iceologer": iceologerSpawner.spawnIceologer(location); return true;
            case "corruptedzombie": corruptedZombieSpawner.spawnCorruptedZombie(location); return true;
            case "corruptedspider": corruptedSpider.spawnCorruptedSpider(location); return true;
            case "corruptedinsect": corruptedInsect.spawnCorruptedInsect(location); return true;
            case "queenbee": QueenBeeHandler.spawn(plugin, location); return true;
            case "guardianblaze": guardianBlaze.spawnGuardianBlaze(location); return true;
            case "guardiancorruptedskeleton": guardianCorruptedSkeleton.spawnGuardianCorruptedSkeleton(location); return true;
            case "corruptedskeleton": corruptedSkeleton.spawnCorruptedSkeleton(location, variantArgs); return true;
            case "customdolphin":
                if (variantArgs != null && (variantArgs.equalsIgnoreCase("Pingo") || variantArgs.equalsIgnoreCase("Pinga"))) {
                    customDolphin.spawnPinguin(location, variantArgs);
                    return true;
                }
                return false;
            case "corruptedinfernalspider": corruptedInfernalSpider.spawnCorruptedInfernalSpider(location); return true;
            case "corruptedcreeper": corruptedCreeper.spawnCorruptedCreeper(location); return true;
            case "piglinglobo": piglinGloboSpawner.spawnPiglinGlobo(location); return true;
            case "buffbreeze": buffBreeze.spawnBuffBreeze(location); return true;
            case "invertedghast": invertedGhast.spawnInvertedGhast(location); return true;
            case "netheritevexguardian": netheriteVexGuardian.spawnNetheriteVexGuardian(location); return true;
            case "ultrawitherboss": ultraWitherBossHandler.spawnUltraWither(location); return true;
            case "whiteenderman": whiteEnderman.spawnWhiteEnderman(location); return true;
            case "infernalcreeper": infernalCreeper.spawnInfernalCreeper(location); return true;
            case "toxicspider": toxicSpider.spawnToxicSpider(location); return true;
            case "fastravager": fastRavager.spawnFastRavager(location); return true;
            case "bruteimperial": imperialBrute.spawnBruteImperial(location); return true;
            case "batboom": batBoom.spawnBatBoom(location); return true;
            case "spectraleeye": spectralEye.spawnSpectralEye(location); return true;
            case "customboat": if (targetPlayer != null) customBoat.spawnBoat(location, targetPlayer); return true;
            case "enderghast": espectralGhast.spawnEnderGhast(location); return true;
            case "endercreeper": espectralCreeper.spawnEnderCreeper(location); return true;
            case "endersilverfish": espectralSilverfish.spawnEnderSilverfish(location); return true;
            case "guardianshulker": guardianShulkerDescartado.spawnGuardianShulker(location); return true;
            case "darkcreeper": darkCreeper.spawnDarkCreeper(location); return true;
            case "darkvex": darkVex.spawnDarkVex(location); return true;
            case "darkskeleton": darkSkeleton.spawnDarkSkeleton(location); return true;
            case "infestedbee": infestedBeeHandler.spawnInfestedBee(location); return true;
            case "infernalbeast": infernalBeast.spawnInfernalBeast(location); return true;
            case "corrupteddrowned": corruptedDrowned.spawnCorruptedDrowned(location); return true;
            case "corruptedbee": corruptedBee.spawnCorruptedBee(location); return true;
            case "infestedgolems": infestedGolems.spawnInfestedGolem(location); return true;
            case "null_statue": Null_Statue.spawn(location); return true;
            case "estatuarecompensa": Estatua_Reward.spawn(location); return true;
            default:                           return false;
        }
    }

    public Entity spawnMobAndReturn(String mobType, Location location, Player targetPlayer, String variantArgs) {
        final Entity[] captured = {null};

        // Registrar callback
        pendingCallback = entity -> captured[0] = entity;
        expectedSpawnLocation = location.clone();

        // Spawn normal
        spawnMob(mobType, location, targetPlayer, variantArgs);

        pendingCallback = null;
        expectedSpawnLocation = null;

        return captured[0];
    }

    public List<String> getRegisteredMobs() {
        return registeredMobs;
    }
}