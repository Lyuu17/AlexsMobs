package com.github.alexthe666.alexsmobs.world;

//public class AMMobSpawnBiomeModifier implements BiomeModifier {
//    private static final RegistryObject<Codec<? extends BiomeModifier>> SERIALIZER = RegistryObject.create(new ResourceLocation(AlexsMobs.MOD_ID, "am_mob_spawns"), ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, AlexsMobs.MOD_ID);
//
//    public AMMobSpawnBiomeModifier() {
//    }
//
//    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
//        if (phase == Phase.ADD) {
//            AMWorldRegistry.addBiomeSpawns(biome, builder);
//        }
//    }
//
//    public Codec<? extends BiomeModifier> codec() {
//        return (Codec)SERIALIZER.get();
//    }
//
//    public static Codec<AMMobSpawnBiomeModifier> makeCodec() {
//        return Codec.unit(AMMobSpawnBiomeModifier::new);
//    }
//}
