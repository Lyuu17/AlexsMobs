package com.github.alexthe666.alexsmobs.world;

//public class AMMobSpawnStructureModifier implements StructureModifier {
//
//    private static final RegistryObject<Codec<? extends StructureModifier>> SERIALIZER = RegistryObject.create(new ResourceLocation(AlexsMobs.MOD_ID, "am_structure_spawns"), ForgeRegistries.Keys.STRUCTURE_MODIFIER_SERIALIZERS, AlexsMobs.MOD_ID);
//
//    public AMMobSpawnStructureModifier() {
//    }
//
//    @Override
//    public void modify(Holder<Structure> structure, Phase phase, ModifiableStructureInfo.StructureInfo.Builder builder) {
//        if (phase == StructureModifier.Phase.ADD) {
//            AMWorldRegistry.modifyStructure(structure, builder);
//
//        }
//    }
//
//    public Codec<? extends StructureModifier> codec() {
//        return (Codec)SERIALIZER.get();
//    }
//
//    public static Codec<AMMobSpawnStructureModifier> makeCodec() {
//        return Codec.unit(AMMobSpawnStructureModifier::new);
//    }
//}
