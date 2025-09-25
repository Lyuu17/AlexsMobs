package com.github.alexthe666.alexsmobs.world;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.BiomeConfig;
import com.github.alexthe666.alexsmobs.misc.SpawnBiomeData;
import com.mojang.datafixers.util.Pair;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

public class AMWorldRegistry {
//
//    public static void modifyStructure(RegistryEntry<Structure> structure, ModifiableStructureInfo.StructureInfo.Builder builder) {
//        if (AMConfig.mimicubeSpawnInEndCity && structure.matchesKey(StructureKeys.END_CITY) && AMConfig.mimicubeSpawnWeight > 0) {
//            builder.getStructureSettings().getOrAddSpawnOverrides(SpawnGroup.MONSTER).addSpawn(new SpawnSettings.SpawnEntry(AMEntityRegistry.MIMICUBE.get(), AMConfig.mimicubeSpawnWeight, 1, 3));
//        }
//        if (AMConfig.soulVultureSpawnOnFossil && structure.matchesKey(StructureKeys.NETHER_FOSSIL) && AMConfig.soulVultureSpawnWeight > 0) {
//            builder.getStructureSettings().getOrAddSpawnOverrides(SpawnGroup.MONSTER).addSpawn(new SpawnSettings.SpawnEntry(AMEntityRegistry.SOUL_VULTURE.get(), AMConfig.soulVultureSpawnWeight, 1, 1));
//        }
//        if (AMConfig.restrictSkelewagSpawns && structure.matchesKey(StructureKeys.SHIPWRECK) && AMConfig.skelewagSpawnWeight > 0) {
//            builder.getStructureSettings().getOrAddSpawnOverrides(SpawnGroup.MONSTER).addSpawn(new SpawnSettings.SpawnEntry(AMEntityRegistry.SKELEWAG.get(), AMConfig.skelewagSpawnWeight, 1, 2));
//        }
//        if (AMConfig.restrictUnderminerSpawns && structure.isIn(AMTagRegistry.SPAWNS_UNDERMINERS) && AMConfig.underminerSpawnWeight > 0) {
//            builder.getStructureSettings().getOrAddSpawnOverrides(SpawnGroup.AMBIENT).addSpawn(new SpawnSettings.SpawnEntry(AMEntityRegistry.UNDERMINER.get(), AMConfig.underminerSpawnWeight, 1, 1));
//        }
//    }
//
    private static Identifier getBiomeName(RegistryEntry<Biome> biome) {
        return biome.getKeyOrValue().map(RegistryKey::getValue, (noKey) -> null);
    }

    public static boolean testBiome(Pair<String, SpawnBiomeData> entry, RegistryEntry<Biome> biome) {
        boolean result = false;
        try {
            result = BiomeConfig.test(entry, biome, getBiomeName(biome));
        } catch (Exception e) {
            AlexsMobs.LOGGER.warn("could not test biome config for " + entry.getFirst() + ", defaulting to no spawns for mob");
            result = false;
        }
        return result;
    }
//
//    public static void addBiomeSpawns(RegistryEntry<Biome> biome, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
//        if (testBiome(BiomeConfig.grizzlyBear, biome) && AMConfig.grizzlyBearSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.GRIZZLY_BEAR.get(), AMConfig.grizzlyBearSpawnWeight, 2, 3));
//        }
//        if (testBiome(BiomeConfig.roadrunner, biome) && AMConfig.roadrunnerSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.ROADRUNNER.get(), AMConfig.roadrunnerSpawnWeight, 2, 2));
//        }
//        if (testBiome(BiomeConfig.boneSerpent, biome) && AMConfig.boneSerpentSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.MONSTER).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.BONE_SERPENT.get(), AMConfig.boneSerpentSpawnWeight, 1, 1));
//        }
//        if (testBiome(BiomeConfig.gazelle, biome) && AMConfig.gazelleSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.GAZELLE.get(), AMConfig.gazelleSpawnWeight, 7, 7));
//        }
//        if (testBiome(BiomeConfig.crocodile, biome) && AMConfig.crocodileSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.CROCODILE.get(), AMConfig.crocodileSpawnWeight, 1, 2));
//        }
//        if (testBiome(BiomeConfig.fly, biome) && AMConfig.flySpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.AMBIENT).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.FLY.get(), AMConfig.flySpawnWeight, 2, 3));
//        }
//        if (testBiome(BiomeConfig.hummingbird, biome) && AMConfig.hummingbirdSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.HUMMINGBIRD.get(), AMConfig.hummingbirdSpawnWeight, 7, 7));
//        }
//        if (testBiome(BiomeConfig.orca, biome) && AMConfig.orcaSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.WATER_CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.ORCA.get(), AMConfig.orcaSpawnWeight, 3, 4));
//        }
//        if (testBiome(BiomeConfig.sunbird, biome) && AMConfig.sunbirdSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.SUNBIRD.get(), AMConfig.sunbirdSpawnWeight, 1, 1));
//        }
//        if (testBiome(BiomeConfig.gorilla, biome) && AMConfig.gorillaSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.GORILLA.get(), AMConfig.gorillaSpawnWeight, 7, 7));
//        }
//        if (testBiome(BiomeConfig.crimsonMosquito, biome) && AMConfig.crimsonMosquitoSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.MONSTER).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.CRIMSON_MOSQUITO.get(), AMConfig.crimsonMosquitoSpawnWeight, 4, 4));
//        }
//        if (testBiome(BiomeConfig.rattlesnake, biome) && AMConfig.rattlesnakeSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.RATTLESNAKE.get(), AMConfig.rattlesnakeSpawnWeight, 1, 2));
//        }
//        if (testBiome(BiomeConfig.endergrade, biome) && AMConfig.endergradeSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.ENDERGRADE.get(), AMConfig.endergradeSpawnWeight, 2, 6));
//        }
//        if (testBiome(BiomeConfig.hammerheadShark, biome) && AMConfig.hammerheadSharkSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.WATER_CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.HAMMERHEAD_SHARK.get(), AMConfig.hammerheadSharkSpawnWeight, 2, 3));
//        }
//        if (testBiome(BiomeConfig.lobster, biome) && AMConfig.lobsterSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.WATER_AMBIENT).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.LOBSTER.get(), AMConfig.lobsterSpawnWeight, 3, 5));
//        }
//        if (testBiome(BiomeConfig.komodoDragon, biome) && AMConfig.komodoDragonSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.KOMODO_DRAGON.get(), AMConfig.komodoDragonSpawnWeight, 1, 2));
//        }
//        if (testBiome(BiomeConfig.capuchinMonkey, biome) && AMConfig.capuchinMonkeySpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.CAPUCHIN_MONKEY.get(), AMConfig.capuchinMonkeySpawnWeight, 9, 16));
//        }
//        if (testBiome(BiomeConfig.caveCentipede, biome) && AMConfig.caveCentipedeSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.MONSTER).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.CENTIPEDE_HEAD.get(), AMConfig.caveCentipedeSpawnWeight, 1, 1));
//        }
//        if (testBiome(BiomeConfig.warpedToad, biome) && AMConfig.warpedToadSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.WARPED_TOAD.get(), AMConfig.warpedToadSpawnWeight, 5, 5));
//        }
//        if (testBiome(BiomeConfig.moose, biome) && AMConfig.mooseSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.MOOSE.get(), AMConfig.mooseSpawnWeight, 3, 4));
//        }
//        if (testBiome(BiomeConfig.mimicube, biome) && AMConfig.mimicubeSpawnWeight > 0 && !AMConfig.mimicubeSpawnInEndCity) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.MONSTER).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.MIMICUBE.get(), AMConfig.mimicubeSpawnWeight, 1, 3));
//        }
//        if (testBiome(BiomeConfig.raccoon, biome) && AMConfig.raccoonSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.RACCOON.get(), AMConfig.raccoonSpawnWeight, 2, 4));
//        }
//        if (testBiome(BiomeConfig.blobfish, biome) && AMConfig.blobfishSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.WATER_AMBIENT).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.BLOBFISH.get(), AMConfig.blobfishSpawnWeight, 2, 2));
//        }
//        if (testBiome(BiomeConfig.seal, biome) && AMConfig.sealSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.SEAL.get(), AMConfig.sealSpawnWeight, 3, 8));
//        }
//        if (testBiome(BiomeConfig.cockroach, biome) && AMConfig.cockroachSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.AMBIENT).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.COCKROACH.get(), AMConfig.cockroachSpawnWeight, 5, 5));
//        }
//        if (testBiome(BiomeConfig.shoebill, biome) && AMConfig.shoebillSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.SHOEBILL.get(), AMConfig.shoebillSpawnWeight, 1, 2));
//        }
//        if (testBiome(BiomeConfig.elephant, biome) && AMConfig.elephantSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.ELEPHANT.get(), AMConfig.elephantSpawnWeight, 3, 5));
//        }
//        if (testBiome(BiomeConfig.soulVulture, biome) && AMConfig.soulVultureSpawnWeight > 0 && !AMConfig.soulVultureSpawnOnFossil) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.MONSTER).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.SOUL_VULTURE.get(), AMConfig.soulVultureSpawnWeight, 2, 3));
//        }
//        if (testBiome(BiomeConfig.snowLeopard, biome) && AMConfig.snowLeopardSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.SNOW_LEOPARD.get(), AMConfig.snowLeopardSpawnWeight, 1, 2));
//        }
//        if (testBiome(BiomeConfig.spectre, biome) && AMConfig.spectreSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.SPECTRE.get(), AMConfig.spectreSpawnWeight, 1, 2));
//        }
//        if (testBiome(BiomeConfig.crow, biome) && AMConfig.crowSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.CROW.get(), AMConfig.crowSpawnWeight, 3, 5));
//        }
//        if (testBiome(BiomeConfig.alligatorSnappingTurtle, biome) && AMConfig.alligatorSnappingTurtleSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.ALLIGATOR_SNAPPING_TURTLE.get(), AMConfig.alligatorSnappingTurtleSpawnWeight, 1, 2));
//        }
//        if (testBiome(BiomeConfig.mungus, biome) && AMConfig.mungusSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.MUNGUS.get(), AMConfig.mungusSpawnWeight, 3, 5));
//        }
//        if (testBiome(BiomeConfig.mantisShrimp, biome) && AMConfig.mantisShrimpSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.WATER_CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.MANTIS_SHRIMP.get(), AMConfig.mantisShrimpSpawnWeight, 1, 4));
//        }
//        if (testBiome(BiomeConfig.guster, biome) && AMConfig.gusterSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.MONSTER).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.GUSTER.get(), AMConfig.gusterSpawnWeight, 1, 2));
//        }
//        if (testBiome(BiomeConfig.warpedMosco, biome) && AMConfig.warpedMoscoSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.MONSTER).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.WARPED_MOSCO.get(), AMConfig.warpedMoscoSpawnWeight, 1, 1));
//        }
//        if (testBiome(BiomeConfig.straddler, biome) && AMConfig.straddlerSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.MONSTER).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.STRADDLER.get(), AMConfig.straddlerSpawnWeight, 1, 3));
//        }
//        if (testBiome(BiomeConfig.stradpole, biome) && AMConfig.stradpoleSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.WATER_CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.STRADPOLE.get(), AMConfig.stradpoleSpawnWeight, 1, 1));
//        }
//        if (testBiome(BiomeConfig.emu, biome) && AMConfig.emuSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.EMU.get(), AMConfig.emuSpawnWeight, 2, 5));
//        }
//        if (testBiome(BiomeConfig.platypus, biome) && AMConfig.platypusSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.PLATYPUS.get(), AMConfig.platypusSpawnWeight, 1, 2));
//        }
//        if (testBiome(BiomeConfig.dropbear, biome) && AMConfig.dropbearSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.MONSTER).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.DROPBEAR.get(), AMConfig.dropbearSpawnWeight, 1, 1));
//        }
//        if (testBiome(BiomeConfig.tasmanianDevil, biome) && AMConfig.tasmanianDevilSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.TASMANIAN_DEVIL.get(), AMConfig.tasmanianDevilSpawnWeight, 1, 2));
//        }
//        if (testBiome(BiomeConfig.kangaroo, biome) && AMConfig.kangarooSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.KANGAROO.get(), AMConfig.kangarooSpawnWeight, 3, 5));
//        }
//        if (testBiome(BiomeConfig.cachalot_whale_spawns, biome) && AMConfig.cachalotWhaleSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.WATER_CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.CACHALOT_WHALE.get(), AMConfig.cachalotWhaleSpawnWeight, 1, 2));
//        }
//        if (testBiome(BiomeConfig.enderiophage_spawns, biome) && AMConfig.enderiophageSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.ENDERIOPHAGE.get(), AMConfig.enderiophageSpawnWeight, 2, 2));
//        }
//        if (testBiome(BiomeConfig.baldEagle, biome) && AMConfig.baldEagleSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.BALD_EAGLE.get(), AMConfig.baldEagleSpawnWeight, 2, 4));
//        }
//        if (testBiome(BiomeConfig.tiger, biome) && AMConfig.tigerSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.TIGER.get(), AMConfig.tigerSpawnWeight, 1, 3));
//        }
//        if (testBiome(BiomeConfig.tarantula_hawk, biome) && AMConfig.tarantulaHawkSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.TARANTULA_HAWK.get(), AMConfig.tarantulaHawkSpawnWeight, 1, 1));
//        }
//        if (testBiome(BiomeConfig.void_worm, biome) && AMConfig.voidWormSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.MONSTER).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.VOID_WORM.get(), AMConfig.voidWormSpawnWeight, 1, 1));
//        }
//        if (testBiome(BiomeConfig.frilled_shark, biome) && AMConfig.frilledSharkSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.WATER_CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.FRILLED_SHARK.get(), AMConfig.frilledSharkSpawnWeight, 1, 1));
//        }
//        if (testBiome(BiomeConfig.mimic_octopus, biome) && AMConfig.mimicOctopusSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.WATER_CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.MIMIC_OCTOPUS.get(), AMConfig.mimicOctopusSpawnWeight, 1, 2));
//        }
//        if (testBiome(BiomeConfig.seagull, biome) && AMConfig.seagullSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.SEAGULL.get(), AMConfig.seagullSpawnWeight, 3, 6));
//        }
//        if (testBiome(BiomeConfig.froststalker, biome) && AMConfig.froststalkerSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.FROSTSTALKER.get(), AMConfig.froststalkerSpawnWeight, 5, 7));
//        }
//        if (testBiome(BiomeConfig.tusklin, biome) && AMConfig.tusklinSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.TUSKLIN.get(), AMConfig.tusklinSpawnWeight, 3, 5));
//        }
//        if (testBiome(BiomeConfig.laviathan, biome) && AMConfig.laviathanSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.LAVIATHAN.get(), AMConfig.laviathanSpawnWeight, 1, 1));
//        }
//        if (testBiome(BiomeConfig.cosmaw, biome) && AMConfig.cosmawSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.COSMAW.get(), AMConfig.cosmawSpawnWeight, 1, 2));
//        }
//        if (testBiome(BiomeConfig.toucan, biome) && AMConfig.toucanSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.TOUCAN.get(), AMConfig.toucanSpawnWeight, 5, 5));
//        }
//        if (testBiome(BiomeConfig.maned_wolf, biome) && AMConfig.manedWolfSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.MANED_WOLF.get(), AMConfig.manedWolfSpawnWeight, 1, 1));
//        }
//        if (testBiome(BiomeConfig.anaconda, biome) && AMConfig.anacondaSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.ANACONDA.get(), AMConfig.anacondaSpawnWeight, 1, 1));
//        }
//        if (testBiome(BiomeConfig.anteater, biome) && AMConfig.anteaterSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.ANTEATER.get(), AMConfig.anteaterSpawnWeight, 1, 3));
//        }
//        if (testBiome(BiomeConfig.rocky_roller, biome) && AMConfig.rockyRollerSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.MONSTER).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.ROCKY_ROLLER.get(), AMConfig.rockyRollerSpawnWeight, 1, 1));
//        }
//        if (testBiome(BiomeConfig.flutter, biome) && AMConfig.flutterSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.AMBIENT).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.FLUTTER.get(), AMConfig.flutterSpawnWeight, 2, 4));
//        }
//        if (testBiome(BiomeConfig.gelada_monkey, biome) && AMConfig.geladaMonkeySpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.GELADA_MONKEY.get(), AMConfig.geladaMonkeySpawnWeight, 9, 16));
//        }
//        if (testBiome(BiomeConfig.jerboa, biome) && AMConfig.jerboaSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.AMBIENT).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.JERBOA.get(), AMConfig.jerboaSpawnWeight, 1, 3));
//        }
//        if (testBiome(BiomeConfig.terrapin, biome) && AMConfig.terrapinSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.WATER_AMBIENT).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.TERRAPIN.get(), AMConfig.terrapinSpawnWeight, 1, 2));
//        }
//        if (testBiome(BiomeConfig.comb_jelly, biome) && AMConfig.combJellySpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.WATER_AMBIENT).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.COMB_JELLY.get(), AMConfig.combJellySpawnWeight, 2, 3));
//        }
//        if (testBiome(BiomeConfig.cosmic_cod, biome) && AMConfig.cosmicCodSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.AMBIENT).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.COSMIC_COD.get(), AMConfig.cosmicCodSpawnWeight, 9, 13));
//        }
//        if (testBiome(BiomeConfig.bunfungus, biome) && AMConfig.bunfungusSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.BUNFUNGUS.get(), AMConfig.bunfungusSpawnWeight, 1, 1));
//        }
//        if (testBiome(BiomeConfig.bison, biome) && AMConfig.bisonSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.BISON.get(), AMConfig.bisonSpawnWeight, 6, 10));
//        }
//        if (testBiome(BiomeConfig.giant_squid, biome) && AMConfig.giantSquidSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.WATER_CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.GIANT_SQUID.get(), AMConfig.giantSquidSpawnWeight, 1, 2));
//        }
//        if (testBiome(BiomeConfig.devils_hole_pupfish, biome) && AMConfig.devilsHolePupfishSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.WATER_AMBIENT).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.DEVILS_HOLE_PUPFISH.get(), AMConfig.devilsHolePupfishSpawnWeight, 5, 12));
//        }
//        if (testBiome(BiomeConfig.catfish, biome) && AMConfig.catfishSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.WATER_AMBIENT).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.CATFISH.get(), AMConfig.catfishSpawnWeight, 1, 3));
//        }
//        if (testBiome(BiomeConfig.flying_fish, biome) && AMConfig.flyingFishSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.WATER_AMBIENT).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.FLYING_FISH.get(), AMConfig.flyingFishSpawnWeight, 3, 6));
//        }
//        if (testBiome(BiomeConfig.skelewag, biome) && AMConfig.skelewagSpawnWeight > 0 && !AMConfig.restrictSkelewagSpawns) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.MONSTER).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.SKELEWAG.get(), AMConfig.skelewagSpawnWeight, 2, 3));
//        }
//        if (testBiome(BiomeConfig.rain_frog, biome) && AMConfig.rainFrogSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.AMBIENT).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.RAIN_FROG.get(), AMConfig.rainFrogSpawnWeight, 1, 3));
//        }
//        if (testBiome(BiomeConfig.potoo, biome) && AMConfig.potooSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.POTOO.get(), AMConfig.potooSpawnWeight, 1, 1));
//        }
//        if (testBiome(BiomeConfig.mudskipper, biome) && AMConfig.mudskipperSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.MUDSKIPPER.get(), AMConfig.mudskipperSpawnWeight, 2, 4));
//        }
//        if (testBiome(BiomeConfig.rhinoceros, biome) && AMConfig.rhinocerosSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.RHINOCEROS.get(), AMConfig.rhinocerosSpawnWeight, 3, 5));
//        }
//        if (testBiome(BiomeConfig.sugar_glider, biome) && AMConfig.sugarGliderSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.SUGAR_GLIDER.get(), AMConfig.sugarGliderSpawnWeight, 2, 4));
//        }
//        if (testBiome(BiomeConfig.farseer, biome) && AMConfig.farseerSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.MONSTER).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.FARSEER.get(), AMConfig.farseerSpawnWeight, 1, 1));
//        }
//        if (testBiome(BiomeConfig.skreecher, biome) && AMConfig.skreecherSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.MONSTER).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.SKREECHER.get(), AMConfig.skreecherSpawnWeight, 1, 1));
//        }
//        if (testBiome(BiomeConfig.underminer, biome) && AMConfig.underminerSpawnWeight > 0 && !AMConfig.restrictUnderminerSpawns) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.AMBIENT).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.UNDERMINER.get(), AMConfig.underminerSpawnWeight, 1, 1));
//        }
//        if (testBiome(BiomeConfig.murmur, biome) && AMConfig.murmurSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.MONSTER).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.MURMUR.get(), AMConfig.murmurSpawnWeight, 1, 1));
//        }
//        if (testBiome(BiomeConfig.skunk, biome) && AMConfig.skunkSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.SKUNK.get(), AMConfig.skunkSpawnWeight, 1, 2));
//        }
//        if (testBiome(BiomeConfig.banana_slug, biome) && AMConfig.bananaSlugSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.BANANA_SLUG.get(), AMConfig.bananaSlugSpawnWeight, 2, 3));
//        }
//        if (testBiome(BiomeConfig.blue_jay, biome) && AMConfig.blueJaySpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.BLUE_JAY.get(), AMConfig.blueJaySpawnWeight, 2, 4));
//        }
//        if (testBiome(BiomeConfig.caiman, biome) && AMConfig.caimanSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.CREATURE).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.CAIMAN.get(), AMConfig.caimanSpawnWeight, 2, 4));
//        }
//        if (testBiome(BiomeConfig.triops, biome) && AMConfig.triopsSpawnWeight > 0) {
//            builder.getMobSpawnSettings().getSpawner(SpawnGroup.WATER_AMBIENT).add(new SpawnSettings.SpawnEntry(AMEntityRegistry.TRIOPS.get(), AMConfig.triopsSpawnWeight, 2, 6));
//        }
//    }
}
