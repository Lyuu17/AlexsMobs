package com.github.alexthe666.alexsmobs.world;

//public class AMLeafcutterAntBiomeModifier implements BiomeModifier {
//    private static final RegistrySupplier<Codec<? extends BiomeModifier>> SERIALIZER = RegistrySupplier.create(new ResourceLocation(AlexsMobs.MOD_ID, "am_leafcutter_ant_spawns"), ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, AlexsMobs.MOD_ID);
//    private final HolderSet<PlacedFeature> features;
//
//    public AMLeafcutterAntBiomeModifier(HolderSet<PlacedFeature> features) {
//        this.features = features;
//    }
//
//    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
//        if (phase == Phase.ADD) {
//            AMWorldRegistry.addLeafcutterAntSpawns(biome, this.features, builder);
//        }
//    }
//
//    public Codec<? extends BiomeModifier> codec() {
//        return (Codec)SERIALIZER.get();
//    }
//
//    public static Codec<AMLeafcutterAntBiomeModifier> makeCodec() {
//        return RecordCodecBuilder.create((config) -> {
//            return config.group(PlacedFeature.LIST_CODEC.fieldOf("features").forGetter((otherConfig) -> {
//                return otherConfig.features;
//            })).apply(config, AMLeafcutterAntBiomeModifier::new);
//        });
//    }
//}
