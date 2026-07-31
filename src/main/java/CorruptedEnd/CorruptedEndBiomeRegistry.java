package CorruptedEnd;

import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class CorruptedEndBiomeRegistry {

    public static void registerAll(JavaPlugin plugin) {
        try {
            Object craftServer = plugin.getServer();
            Object nmsServer = craftServer.getClass().getMethod("getServer").invoke(craftServer);
            Object registryAccess = nmsServer.getClass().getMethod("registryAccess").invoke(nmsServer);

            Class<?> registriesClass = Class.forName("net.minecraft.core.registries.Registries");
            Object biomeRegistryKey = registriesClass.getField("BIOME").get(null);

            Method registryOrThrow = registryAccess.getClass().getMethod("registryOrThrow",
                    Class.forName("net.minecraft.resources.ResourceKey"));
            Object biomeRegistry = registryOrThrow.invoke(registryAccess, biomeRegistryKey);

            // Descongelar el registry
            setFrozen(biomeRegistry, false);

            // Registrar cada bioma con los colores exactos de tus JSONs originales
            registerBiome(biomeRegistry, "corrupted_end", "sculk_plains",
                    6682, 13107, 4159204, 329011, 0.5f);
            registerBiome(biomeRegistry, "corrupted_end", "crimson_wastes",
                    5046297, 6684706, 4159204, 329011, 2.0f);
            registerBiome(biomeRegistry, "corrupted_end", "celestial_forest",
                    19294, 29593, 4159204, 329011, -0.5f);
            registerBiome(biomeRegistry, "corrupted_end", "obsidian_peaks",
                    262170, 1703987, 4159204, 329011, 0.5f);

            // Volver a congelar
            biomeRegistry.getClass().getMethod("freeze").invoke(biomeRegistry);

            plugin.getLogger().info("[CorruptedEnd] Biomas registrados correctamente via NMS.");

        } catch (Exception e) {
            plugin.getLogger().severe("[CorruptedEnd] Error registrando biomas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void registerBiome(Object biomeRegistry, String namespace, String path,
                                      int skyColor, int fogColor,
                                      int waterColor, int waterFogColor,
                                      float temperature) throws Exception {

        // ResourceLocation
        Class<?> resourceLocationClass = Class.forName("net.minecraft.resources.ResourceLocation");
        Method fromNamespaceAndPath = resourceLocationClass.getMethod("fromNamespaceAndPath",
                String.class, String.class);
        Object resourceLocation = fromNamespaceAndPath.invoke(null, namespace, path);

        // ResourceKey<Biome>
        Class<?> resourceKeyClass = Class.forName("net.minecraft.resources.ResourceKey");
        Class<?> registriesClass = Class.forName("net.minecraft.core.registries.Registries");
        Object biomeRegistryKey = registriesClass.getField("BIOME").get(null);
        Method createKey = resourceKeyClass.getMethod("create",
                Class.forName("net.minecraft.resources.ResourceKey"),
                Class.forName("net.minecraft.resources.ResourceLocation"));
        Object biomeKey = createKey.invoke(null, biomeRegistryKey, resourceLocation);

        // BiomeSpecialEffects
        Class<?> effectsBuilderClass = Class.forName(
                "net.minecraft.world.level.biome.BiomeSpecialEffects$Builder");
        Object effectsBuilder = effectsBuilderClass.getDeclaredConstructor().newInstance();
        effectsBuilderClass.getMethod("skyColor", int.class).invoke(effectsBuilder, skyColor);
        effectsBuilderClass.getMethod("fogColor", int.class).invoke(effectsBuilder, fogColor);
        effectsBuilderClass.getMethod("waterColor", int.class).invoke(effectsBuilder, waterColor);
        effectsBuilderClass.getMethod("waterFogColor", int.class).invoke(effectsBuilder, waterFogColor);

        // AmbientMoodSettings.LEGACY_CAVE_SETTINGS
        Class<?> moodClass = Class.forName("net.minecraft.world.level.biome.AmbientMoodSettings");
        Object moodSettings = moodClass.getField("LEGACY_CAVE_SETTINGS").get(null);
        effectsBuilderClass.getMethod("ambientMoodSound", moodClass).invoke(effectsBuilder, moodSettings);
        Object effects = effectsBuilderClass.getMethod("build").invoke(effectsBuilder);

        // MobSpawnSettings.EMPTY
        Class<?> mobSpawnClass = Class.forName("net.minecraft.world.level.biome.MobSpawnSettings");
        Object mobSpawnEmpty = mobSpawnClass.getField("EMPTY").get(null);

        // BiomeGenerationSettings.EMPTY
        Class<?> genSettingsClass = Class.forName(
                "net.minecraft.world.level.biome.BiomeGenerationSettings");
        Object genEmpty = genSettingsClass.getField("EMPTY").get(null);

        // Biome
        Class<?> biomeBuilderClass = Class.forName(
                "net.minecraft.world.level.biome.Biome$BiomeBuilder");
        Object biomeBuilder = biomeBuilderClass.getDeclaredConstructor().newInstance();
        biomeBuilderClass.getMethod("hasPrecipitation", boolean.class).invoke(biomeBuilder, false);
        biomeBuilderClass.getMethod("temperature", float.class).invoke(biomeBuilder, temperature);
        biomeBuilderClass.getMethod("downfall", float.class).invoke(biomeBuilder, 0.0f);
        biomeBuilderClass.getMethod("specialEffects",
                        Class.forName("net.minecraft.world.level.biome.BiomeSpecialEffects"))
                .invoke(biomeBuilder, effects);
        biomeBuilderClass.getMethod("mobSpawnSettings", mobSpawnClass)
                .invoke(biomeBuilder, mobSpawnEmpty);
        biomeBuilderClass.getMethod("generationSettings", genSettingsClass)
                .invoke(biomeBuilder, genEmpty);
        Object biome = biomeBuilderClass.getMethod("build").invoke(biomeBuilder);

        // RegistrationInfo.BUILT_IN
        Class<?> registrationInfoClass = Class.forName("net.minecraft.core.RegistrationInfo");
        Object builtIn = registrationInfoClass.getField("BUILT_IN").get(null);

        // Registrar: register(ResourceKey, Object, RegistrationInfo)
        // llamado directamente sobre la instancia del registry, NO como estático
        Method registerMethod = biomeRegistry.getClass().getMethod("register",
                Class.forName("net.minecraft.resources.ResourceKey"),
                Object.class,
                registrationInfoClass);
        registerMethod.invoke(biomeRegistry, biomeKey, biome, builtIn);
    }

    private static void setFrozen(Object registry, boolean frozen) throws Exception {
        Class<?> clazz = registry.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField("frozen");
                field.setAccessible(true);
                field.set(registry, frozen);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(
                "No se encontró el campo 'frozen' en " + registry.getClass());
    }
}