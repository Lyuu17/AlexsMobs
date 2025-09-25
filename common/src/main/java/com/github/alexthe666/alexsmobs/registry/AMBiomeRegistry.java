package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.config.BiomeConfig;
import com.github.alexthe666.alexsmobs.misc.SpawnBiomeData;
import com.mojang.datafixers.util.Pair;
import dev.architectury.registry.level.biome.BiomeModifications;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;

import java.util.function.Predicate;

public class AMBiomeRegistry {

    public static void register() {
        BiomeModifications.addProperties(
                matchesConfig(BiomeConfig.leafcutter_anthill_spawns, AMConfig.leafcutterAnthillSpawnChance),
                (context, mutable) -> mutable.getGenerationProperties().addFeature(
                        GenerationStep.Feature.SURFACE_STRUCTURES,
                        RegistryKey.of(RegistryKeys.PLACED_FEATURE, AMBlockRegistry.LEAFCUTTER_ANTHILL.getRegistryId()))
        );
    }

    private static Predicate<BiomeModifications.BiomeContext> matchesConfig(Pair<String, SpawnBiomeData> configEntry, double spawnChance) {
        return context -> {
            if (context.getKey().isEmpty()) return false;
            if (spawnChance <= 0) return false;

            Identifier biomeId = context.getKey().get();
            return BiomeConfig.test(configEntry, null, biomeId);
        };
    }
}
