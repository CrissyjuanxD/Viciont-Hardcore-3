package CorruptedEnd;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CorruptedEndBiomeProvider extends BiomeProvider {

    private final CorruptedEndGenerator generator;
    private final Map<BiomeType, Biome> biomeCache = new HashMap<>();
    private Biome fallbackBiome;

    public CorruptedEndBiomeProvider(CorruptedEndGenerator generator) {
        this.generator = generator;
    }

    @NotNull
    @Override
    public Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        generator.initializeGenerators(worldInfo.getSeed());
        CorruptedEndGenerator.BiomeInfo info = generator.getBiomeInfo(x, z);
        return getCachedBiome(info != null ? info.type : null);
    }

    @NotNull
    @Override
    public List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        List<Biome> list = new ArrayList<>();
        for (BiomeType type : BiomeType.values()) {
            Biome b = getCachedBiome(type);
            if (!list.contains(b)) list.add(b);
        }
        if (list.isEmpty()) {
            list.add(getFallback());
        }
        return list;
    }

    private Biome getCachedBiome(BiomeType type) {
        if (type == null) return getFallback();

        // Si ya está cacheado con éxito, devolver directo
        if (biomeCache.containsKey(type)) {
            return biomeCache.get(type);
        }

        NamespacedKey key = new NamespacedKey("corrupted_end", type.name().toLowerCase());
        Biome found = null;

        // Intento 1: Bukkit Registry estándar
        try {
            found = Registry.BIOME.get(key);
        } catch (Throwable ignored) {}

        // Intento 2: Paper Registry (más actualizado en tiempo de carga)
        if (found == null) {
            try {
                found = RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.BIOME)
                        .get(key);
            } catch (Throwable ignored) {}
        }

        if (found != null) {
            // Solo cacheamos si realmente lo encontramos
            biomeCache.put(type, found);
            generator.getPlugin().getLogger().info(
                    "[CorruptedEnd] Bioma cargado correctamente: " + key
            );
            return found;
        }

        // No encontrado aún: NO cacheamos el fallo para que reintente en la próxima llamada
        return getFallback();
    }

    private Biome getFallback() {
        if (fallbackBiome == null) {
            fallbackBiome = Registry.BIOME.get(NamespacedKey.minecraft("the_end"));
        }
        return fallbackBiome;
    }

    /**
     * Intenta pre-cachear todos los biomas del datapack.
     * Llamar desde ServerLoadEvent para garantizar que el registry ya está completo.
     */
    public void tryPreloadBiomes() {
        boolean allLoaded = true;
        generator.getPlugin().getLogger().info("[CorruptedEnd] Escaneando registry de biomas...");
        for (Biome b : Registry.BIOME) {
            String key = b.getKey().toString();
            if (key.contains("corrupted_end")) {
                generator.getPlugin().getLogger().info("[CorruptedEnd] Encontrado: " + key);
            }
        }
        for (BiomeType type : BiomeType.values()) {
            if (!biomeCache.containsKey(type)) {
                Biome result = getCachedBiome(type);
                if (result == getFallback()) {
                    allLoaded = false;
                    generator.getPlugin().getLogger().warning(
                            "[CorruptedEnd] No se pudo pre-cargar el bioma: "
                                    + type.name().toLowerCase()
                                    + " — asegúrate de que el datapack esté activo."
                    );
                }
            }
        }
        if (allLoaded) {
            generator.getPlugin().getLogger().info(
                    "[CorruptedEnd] Todos los biomas del datapack cargados correctamente."
            );
        }
    }
}