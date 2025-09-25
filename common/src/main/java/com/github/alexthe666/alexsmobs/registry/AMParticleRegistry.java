package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.client.particle.*;
import com.github.alexthe666.alexsmobs.misc.SimpleParticleType;
import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.RegistryKeys;

public class AMParticleRegistry {

    public static final DeferredRegister<ParticleType<?>> DEF_REG = DeferredRegister.create(AlexsMobs.MOD_ID, RegistryKeys.PARTICLE_TYPE);
    
    public static final RegistrySupplier<DefaultParticleType> GUSTER_SAND_SPIN = DEF_REG.register("guster_sand_spin", ()-> new SimpleParticleType(false));
    public static final RegistrySupplier<DefaultParticleType> GUSTER_SAND_SHOT = DEF_REG.register("guster_sand_shot", ()-> new SimpleParticleType(false));
    public static final RegistrySupplier<DefaultParticleType> GUSTER_SAND_SPIN_RED = DEF_REG.register("guster_sand_spin_red", ()-> new SimpleParticleType(false));
    public static final RegistrySupplier<DefaultParticleType> GUSTER_SAND_SHOT_RED = DEF_REG.register("guster_sand_shot_red", ()-> new SimpleParticleType(false));
    public static final RegistrySupplier<DefaultParticleType> GUSTER_SAND_SPIN_SOUL = DEF_REG.register("guster_sand_spin_soul", ()-> new SimpleParticleType(false));
    public static final RegistrySupplier<DefaultParticleType> GUSTER_SAND_SHOT_SOUL = DEF_REG.register("guster_sand_shot_soul", ()-> new SimpleParticleType(false));
    public static final RegistrySupplier<DefaultParticleType> HEMOLYMPH = DEF_REG.register("hemolymph", ()-> new SimpleParticleType(false));
    public static final RegistrySupplier<DefaultParticleType> PLATYPUS_SENSE = DEF_REG.register("platypus_sense", ()-> new SimpleParticleType(false));
    public static final RegistrySupplier<DefaultParticleType> WHALE_SPLASH = DEF_REG.register("whale_splash", ()-> new SimpleParticleType(false));
    public static final RegistrySupplier<DefaultParticleType> DNA = DEF_REG.register("dna", ()-> new SimpleParticleType(false));
    public static final RegistrySupplier<DefaultParticleType> SHOCKED = DEF_REG.register("shocked", ()-> new SimpleParticleType(false));
    public static final RegistrySupplier<DefaultParticleType> WORM_PORTAL = DEF_REG.register("worm_portal", ()-> new SimpleParticleType(false));
    public static final RegistrySupplier<DefaultParticleType> INVERT_DIG = DEF_REG.register("invert_dig", ()-> new SimpleParticleType(true));
    public static final RegistrySupplier<DefaultParticleType> TEETH_GLINT = DEF_REG.register("teeth_glint", ()-> new SimpleParticleType(false));
    public static final RegistrySupplier<DefaultParticleType> SMELLY = DEF_REG.register("smelly", ()-> new SimpleParticleType(false));
    public static final RegistrySupplier<DefaultParticleType> BUNFUNGUS_TRANSFORMATION = DEF_REG.register("bunfungus_transformation", ()-> new SimpleParticleType(false));
    public static final RegistrySupplier<DefaultParticleType> FUNGUS_BUBBLE = DEF_REG.register("fungus_bubble", ()-> new SimpleParticleType(false));
    public static final RegistrySupplier<DefaultParticleType> BEAR_FREDDY = DEF_REG.register("bear_freddy", ()-> new SimpleParticleType(true));
    public static final RegistrySupplier<DefaultParticleType> SUNBIRD_FEATHER = DEF_REG.register("sunbird_feather", ()-> new SimpleParticleType(false));
    public static final RegistrySupplier<DefaultParticleType> STATIC_SPARK = DEF_REG.register("static_spark", ()-> new SimpleParticleType(false));
    public static final RegistrySupplier<DefaultParticleType> SKULK_BOOM = DEF_REG.register("skulk_boom", ()-> new SimpleParticleType(false));

    public static final RegistrySupplier<DefaultParticleType> BIRD_SONG = DEF_REG.register("bird_song", ()-> new SimpleParticleType(false));

    public static void init() {

        ParticleProviderRegistry.register(AMParticleRegistry.GUSTER_SAND_SPIN.get(), ParticleGusterSandSpin.Factory::new);
        ParticleProviderRegistry.register(AMParticleRegistry.GUSTER_SAND_SHOT.get(), ParticleGusterSandShot.Factory::new);
        ParticleProviderRegistry.register(AMParticleRegistry.GUSTER_SAND_SPIN_RED.get(), ParticleGusterSandSpin.FactoryRed::new);
        ParticleProviderRegistry.register(AMParticleRegistry.GUSTER_SAND_SHOT_RED.get(), ParticleGusterSandShot.FactoryRed::new);
        ParticleProviderRegistry.register(AMParticleRegistry.GUSTER_SAND_SPIN_SOUL.get(), ParticleGusterSandSpin.FactorySoul::new);
        ParticleProviderRegistry.register(AMParticleRegistry.GUSTER_SAND_SHOT_SOUL.get(), ParticleGusterSandShot.FactorySoul::new);
        ParticleProviderRegistry.register(AMParticleRegistry.HEMOLYMPH.get(), ParticleHemolymph.Factory::new);
        ParticleProviderRegistry.register(AMParticleRegistry.PLATYPUS_SENSE.get(), ParticlePlatypus.Factory::new);
        ParticleProviderRegistry.register(AMParticleRegistry.WHALE_SPLASH.get(), ParticleWhaleSplash.Factory::new);
        ParticleProviderRegistry.register(AMParticleRegistry.DNA.get(), ParticleDna.Factory::new);
        ParticleProviderRegistry.register(AMParticleRegistry.SHOCKED.get(), ParticleSimpleHeart.Factory::new);
        ParticleProviderRegistry.register(AMParticleRegistry.WORM_PORTAL.get(), ParticleWormPortal.Factory::new);
        ParticleProviderRegistry.register(AMParticleRegistry.INVERT_DIG.get(), ParticleInvertDig.Factory::new);
        ParticleProviderRegistry.register(AMParticleRegistry.TEETH_GLINT.get(), ParticleTeethGlint.Factory::new);
        ParticleProviderRegistry.register(AMParticleRegistry.SMELLY.get(), ParticleSmelly.Factory::new);
        ParticleProviderRegistry.register(AMParticleRegistry.BUNFUNGUS_TRANSFORMATION.get(), ParticleBunfungusTransformation.Factory::new);
        ParticleProviderRegistry.register(AMParticleRegistry.FUNGUS_BUBBLE.get(), ParticleFungusBubble.Factory::new);
        ParticleProviderRegistry.register(AMParticleRegistry.BEAR_FREDDY.get(), new ParticleBearFreddy.Factory());
        ParticleProviderRegistry.register(AMParticleRegistry.SUNBIRD_FEATHER.get(), ParticleSunbirdFeather.Factory::new);
        ParticleProviderRegistry.register(AMParticleRegistry.STATIC_SPARK.get(), new ParticleStaticSpark.Factory());
        ParticleProviderRegistry.register(AMParticleRegistry.SKULK_BOOM.get(), new ParticleSkulkBoom.Factory());
        ParticleProviderRegistry.register(AMParticleRegistry.BIRD_SONG.get(), ParticleBirdSong.Factory::new);
    }
}
