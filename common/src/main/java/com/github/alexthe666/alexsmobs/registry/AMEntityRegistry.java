package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.*;
import com.github.alexthe666.alexsmobs.platform.PlatformEntityConstructors;
import com.google.common.base.Predicates;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.WorldView;

import java.util.function.Predicate;

public class AMEntityRegistry {

    public static final DeferredRegister<EntityType<?>> DEF_REG = DeferredRegister.create(AlexsMobs.MOD_ID, RegistryKeys.ENTITY_TYPE);
    public static final RegistrySupplier<EntityType<EntityGrizzlyBear>> GRIZZLY_BEAR = registerEntity("grizzly_bear",
            EntityType.Builder.create(EntityGrizzlyBear::new, SpawnGroup.CREATURE)
                    .setDimensions(1.6F, 1.8F)
                    .maxTrackingRange(10));
//    public static final RegistrySupplier<EntityType<EntityRoadrunner>> ROADRUNNER = DEF_REG.register("roadrunner", () -> registerEntity(EntityType.Builder.of(EntityRoadrunner::new, SpawnGroup.CREATURE).sized(0.45F, 0.75F).setTrackingRange(10), "roadrunner"));
    public static final RegistrySupplier<EntityType<EntityBoneSerpent>> BONE_SERPENT = registerEntity("bone_serpent",
        EntityType.Builder.create(EntityBoneSerpent::new, SpawnGroup.MONSTER)
                .setDimensions(1.2F, 1.15F)
                .makeFireImmune()
                .maxTrackingRange(10));
    public static final RegistrySupplier<EntityType<EntityBoneSerpentPart>> BONE_SERPENT_PART = registerEntity("bone_serpent_part",
        EntityType.Builder.<EntityBoneSerpentPart>create(EntityBoneSerpentPart::new, SpawnGroup.MONSTER)
                .setDimensions(1F, 1F)
                .makeFireImmune()
                .maxTrackingRange(10));
//    public static final RegistrySupplier<EntityType<EntityGazelle>> GAZELLE = DEF_REG.register("gazelle", () -> registerEntity(EntityType.Builder.of(EntityGazelle::new, SpawnGroup.CREATURE).sized(0.85F, 1.25F).setTrackingRange(10), "gazelle"));
    public static final RegistrySupplier<EntityType<EntityCrocodile>> CROCODILE = registerEntity("crocodile",
        EntityType.Builder.create(EntityCrocodile::new, SpawnGroup.CREATURE)
                .setDimensions(2.15F, 0.75F)
                .maxTrackingRange(10));
//    public static final RegistrySupplier<EntityType<EntityFly>> FLY = DEF_REG.register("fly", () -> registerEntity(EntityType.Builder.of(EntityFly::new, MobCategory.AMBIENT).sized(0.35F, 0.35F).setTrackingRange(4), "fly"));
//    public static final RegistrySupplier<EntityType<EntityHummingbird>> HUMMINGBIRD = DEF_REG.register("hummingbird", () -> registerEntity(EntityType.Builder.of(EntityHummingbird::new, SpawnGroup.CREATURE).sized(0.45F, 0.45F).setTrackingRange(5), "hummingbird"));
    public static final RegistrySupplier<EntityType<EntityOrca>> ORCA = registerEntity("orca",
        EntityType.Builder.create(EntityOrca::new, SpawnGroup.WATER_CREATURE)
                .setDimensions(3.75F, 1.75F)
                .maxTrackingRange(10));
//    public static final RegistrySupplier<EntityType<EntitySunbird>> SUNBIRD = DEF_REG.register("sunbird", () -> registerEntity(EntityType.Builder.of(EntitySunbird::new, SpawnGroup.CREATURE).sized(2.75F, 1.5F).fireImmune().setTrackingRange(12).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1), "sunbird"));
//    public static final RegistrySupplier<EntityType<EntityGorilla>> GORILLA = DEF_REG.register("gorilla", () -> registerEntity(EntityType.Builder.of(EntityGorilla::new, SpawnGroup.CREATURE).sized(1.15F, 1.35F).setTrackingRange(10), "gorilla"));
    public static final RegistrySupplier<EntityType<EntityCrimsonMosquito>> CRIMSON_MOSQUITO = registerEntity("crimson_mosquito",
        EntityType.Builder.create(EntityCrimsonMosquito::new, SpawnGroup.MONSTER)
                .setDimensions(1.25F, 1.15F)
                .makeFireImmune()
                .maxTrackingRange(8));
    public static final RegistrySupplier<EntityType<EntityMosquitoSpit>> MOSQUITO_SPIT = registerEntity("mosquito_spit",
            EntityType.Builder.<EntityMosquitoSpit>create(EntityMosquitoSpit::new, SpawnGroup.MISC)
                    .setDimensions(0.5F, 0.5F)
//                    .setCustomClientFactory(EntityMosquitoSpit::new)
                    .makeFireImmune());
//    public static final RegistrySupplier<EntityType<EntityRattlesnake>> RATTLESNAKE = DEF_REG.register("rattlesnake", () -> registerEntity(EntityType.Builder.of(EntityRattlesnake::new, SpawnGroup.CREATURE).sized(0.95F, 0.35F).setTrackingRange(10), "rattlesnake"));
    public static final RegistrySupplier<EntityType<EntityEndergrade>> ENDERGRADE = registerEntity("endergrade",
        EntityType.Builder.create(EntityEndergrade::new, SpawnGroup.CREATURE)
                .setDimensions(0.95F, 0.85F)
                .maxTrackingRange(10));
//    public static final RegistrySupplier<EntityType<EntityHammerheadShark>> HAMMERHEAD_SHARK = DEF_REG.register("hammerhead_shark", () -> registerEntity(EntityType.Builder.of(EntityHammerheadShark::new, SpawnGroup.WATER_CREATURE).sized(2.4F, 1.25F).setTrackingRange(10), "hammerhead_shark"));
//    public static final RegistrySupplier<EntityType<EntitySharkToothArrow>> SHARK_TOOTH_ARROW = DEF_REG.register("shark_tooth_arrow", () -> registerEntity(EntityType.Builder.of(EntitySharkToothArrow::new, SpawnGroup.MISC).sized(0.5F, 0.5F).setCustomClientFactory(EntitySharkToothArrow::new), "shark_tooth_arrow"));
    public static final RegistrySupplier<EntityType<EntityLobster>> LOBSTER = registerEntity("lobster",
        EntityType.Builder.create(EntityLobster::new, SpawnGroup.WATER_AMBIENT)
                .setDimensions(0.7F, 0.4F)
                .maxTrackingRange(5));
//    public static final RegistrySupplier<EntityType<EntityKomodoDragon>> KOMODO_DRAGON = DEF_REG.register("komodo_dragon", () -> registerEntity(EntityType.Builder.of(EntityKomodoDragon::new, SpawnGroup.CREATURE).sized(1.9F, 0.9F).setTrackingRange(10), "komodo_dragon"));
//    public static final RegistrySupplier<EntityType<EntityCapuchinMonkey>> CAPUCHIN_MONKEY = DEF_REG.register("capuchin_monkey", () -> registerEntity(EntityType.Builder.of(EntityCapuchinMonkey::new, SpawnGroup.CREATURE).sized(0.65F, 0.75F).setTrackingRange(10), "capuchin_monkey"));
//    public static final RegistrySupplier<EntityType<EntityTossedItem>> TOSSED_ITEM = DEF_REG.register("tossed_item", () -> registerEntity(EntityType.Builder.of(EntityTossedItem::new, SpawnGroup.MISC).sized(0.5F, 0.5F).setCustomClientFactory(EntityTossedItem::new).fireImmune(), "tossed_item"));
    public static final RegistrySupplier<EntityType<EntityCentipedeHead>> CENTIPEDE_HEAD = registerEntity("centipede_head",
        EntityType.Builder.create(EntityCentipedeHead::new, SpawnGroup.MONSTER)
                .setDimensions(0.9F, 0.9F)
                .maxTrackingRange(8));
    public static final RegistrySupplier<EntityType<EntityCentipedeBody>> CENTIPEDE_BODY = registerEntity("centipede_body",
            EntityType.Builder.<EntityCentipedeBody>create(EntityCentipedeBody::new, SpawnGroup.MISC)
                    .setDimensions(0.9F, 0.9F)
                    .makeFireImmune()
//                    .setShouldReceiveVelocityUpdates(true)
//                    .setUpdateInterval(1)
                    .maxTrackingRange(8));
    public static final RegistrySupplier<EntityType<EntityCentipedeTail>> CENTIPEDE_TAIL = registerEntity("centipede_tail",
            EntityType.Builder.create(EntityCentipedeTail::new, SpawnGroup.MISC)
                    .setDimensions(0.9F, 0.9F)
                    .makeFireImmune()
//                    .setShouldReceiveVelocityUpdates(true)
//                    .setUpdateInterval(1)
                    .maxTrackingRange(8));
    public static final RegistrySupplier<EntityType<EntityWarpedToad>> WARPED_TOAD = registerEntity("warped_toad",
            EntityType.Builder.create(EntityWarpedToad::new, SpawnGroup.CREATURE)
                    .setDimensions(0.9F, 1.4F)
                    .makeFireImmune()
                    //.setShouldReceiveVelocityUpdates(true)
                    .trackingTickInterval(1)
                    .maxTrackingRange(10));

//    public static final RegistrySupplier<EntityType<EntityMoose>> MOOSE = DEF_REG.register("moose", () -> registerEntity(EntityType.Builder.of(EntityMoose::new, SpawnGroup.CREATURE).sized(1.7F, 2.4F).setTrackingRange(10), "moose"));
//    public static final RegistrySupplier<EntityType<EntityMimicube>> MIMICUBE = DEF_REG.register("mimicube", () -> registerEntity(EntityType.Builder.of(EntityMimicube::new, SpawnGroup.MONSTER).sized(0.9F, 0.9F).setTrackingRange(8), "mimicube"));
    public static final RegistrySupplier<EntityType<EntityRaccoon>> RACCOON = registerEntity("raccoon",
        EntityType.Builder.create(EntityRaccoon::new, SpawnGroup.CREATURE)
                .setDimensions(0.8F, 0.9F)
                .maxTrackingRange(10));
    public static final RegistrySupplier<EntityType<EntityBlobfish>> BLOBFISH = registerEntity("blobfish",
        EntityType.Builder.create(EntityBlobfish::new, SpawnGroup.WATER_AMBIENT)
                .setDimensions(0.7F, 0.45F)
                .maxTrackingRange(5));
//    public static final RegistrySupplier<EntityType<EntitySeal>> SEAL = DEF_REG.register("seal", () -> registerEntity(EntityType.Builder.of(EntitySeal::new, SpawnGroup.CREATURE).sized(1.45F, 0.9F).setTrackingRange(10), "seal"));
    public static final RegistrySupplier<EntityType<EntityCockroach>> COCKROACH = registerEntity("cockroach",
        PlatformEntityConstructors.createCockroachEntityBuilder(SpawnGroup.CREATURE)
                .setDimensions(0.7F, 0.3F)
                .maxTrackingRange(5));
    public static final RegistrySupplier<EntityType<EntityCockroachEgg>> COCKROACH_EGG = registerEntity("cockroach_egg",
            EntityType.Builder.<EntityCockroachEgg>create(EntityCockroachEgg::new, SpawnGroup.MISC)
                    .setDimensions(0.5F, 0.5F)
//                    .setCustomClientFactory(EntityCockroachEgg::new)
                    .makeFireImmune());
//    public static final RegistrySupplier<EntityType<EntityShoebill>> SHOEBILL = DEF_REG.register("shoebill", () -> registerEntity(EntityType.Builder.of(EntityShoebill::new, SpawnGroup.CREATURE).sized(0.8F, 1.5F).setUpdateInterval(1).setTrackingRange(10), "shoebill"));
    public static final RegistrySupplier<EntityType<EntityElephant>> ELEPHANT = registerEntity("elephant",
        EntityType.Builder.create(EntityElephant::new, SpawnGroup.CREATURE)
                .setDimensions(3.1F, 3.5F)
//                .setUpdateInterval(1)
                .maxTrackingRange(10));
    public static final RegistrySupplier<EntityType<EntitySoulVulture>> SOUL_VULTURE = registerEntity("soul_vulture",
            EntityType.Builder.create(EntitySoulVulture::new, SpawnGroup.MONSTER)
                    .setDimensions(0.9F, 1.3F)
//                    .setUpdateInterval(1)
                    .makeFireImmune()
                    .maxTrackingRange(8));
//    public static final RegistrySupplier<EntityType<EntitySnowLeopard>> SNOW_LEOPARD = DEF_REG.register("snow_leopard", () -> registerEntity(EntityType.Builder.of(EntitySnowLeopard::new, SpawnGroup.CREATURE).sized(1.2F, 1.3F).immuneTo(Blocks.POWDER_SNOW).setTrackingRange(10), "snow_leopard"));
//    public static final RegistrySupplier<EntityType<EntitySpectre>> SPECTRE = DEF_REG.register("spectre", () -> registerEntity(EntityType.Builder.of(EntitySpectre::new, SpawnGroup.CREATURE).sized(3.15F, 0.8F).fireImmune().setTrackingRange(10).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1), "spectre"));
    public static final RegistrySupplier<EntityType<EntityCrow>> CROW = registerEntity("crow",
        EntityType.Builder.create(EntityCrow::new, SpawnGroup.CREATURE)
                .setDimensions(0.45F, 0.45F)
                .maxTrackingRange(10));
    public static final RegistrySupplier<EntityType<EntityAlligatorSnappingTurtle>> ALLIGATOR_SNAPPING_TURTLE = registerEntity("alligator_snapping_turtle",
            PlatformEntityConstructors.createAlligatorSnappingTurtleEntityBuilder(SpawnGroup.CREATURE)
                    .setDimensions(1.25F, 0.65F)
                    .maxTrackingRange(10));

    public static final RegistrySupplier<EntityType<EntityMungus>> MUNGUS = registerEntity("mungus",
            PlatformEntityConstructors.createMungusEntityBuilder(SpawnGroup.CREATURE)
                    .setDimensions(0.75F, 1.45F)
                    .maxTrackingRange(10));

//    public static final RegistrySupplier<EntityType<EntityMantisShrimp>> MANTIS_SHRIMP = DEF_REG.register("mantis_shrimp", () -> registerEntity(EntityType.Builder.of(EntityMantisShrimp::new, SpawnGroup.WATER_CREATURE).sized(1.25F, 1.2F).setTrackingRange(10), "mantis_shrimp"));
//    public static final RegistrySupplier<EntityType<EntityGuster>> GUSTER = DEF_REG.register("guster", () -> registerEntity(EntityType.Builder.of(EntityGuster::new, SpawnGroup.MONSTER).sized(1.42F, 2.35F).fireImmune().setTrackingRange(8), "guster"));
//    public static final RegistrySupplier<EntityType<EntitySandShot>> SAND_SHOT = DEF_REG.register("sand_shot", () -> registerEntity(EntityType.Builder.of(EntitySandShot::new, SpawnGroup.MISC).sized(0.95F, 0.65F).setCustomClientFactory(EntitySandShot::new).fireImmune(), "sand_shot"));
//    public static final RegistrySupplier<EntityType<EntityGust>> GUST = DEF_REG.register("gust", () -> registerEntity(EntityType.Builder.of(EntityGust::new, SpawnGroup.MISC).sized(0.8F, 0.8F).setCustomClientFactory(EntityGust::new).fireImmune(), "gust"));
    public static final RegistrySupplier<EntityType<EntityWarpedMosco>> WARPED_MOSCO = registerEntity("warped_mosco",
        EntityType.Builder.create(EntityWarpedMosco::new, SpawnGroup.MONSTER)
                .setDimensions(1.99F, 3.25F)
                .makeFireImmune()
                .maxTrackingRange(10));

    public static final RegistrySupplier<EntityType<EntityHemolymph>> HEMOLYMPH = registerEntity("hemolymph",
        EntityType.Builder.<EntityHemolymph>create(EntityHemolymph::new, SpawnGroup.MISC)
                .setDimensions(0.5F, 0.5F)
//                .setCustomClientFactory(EntityHemolymph::new)
                .makeFireImmune());
//    public static final RegistrySupplier<EntityType<EntityStraddler>> STRADDLER = DEF_REG.register("straddler", () -> registerEntity(EntityType.Builder.of(EntityStraddler::new, SpawnGroup.MONSTER).sized(1.65F, 3F).fireImmune().setTrackingRange(8), "straddler"));
//    public static final RegistrySupplier<EntityType<EntityStradpole>> STRADPOLE = DEF_REG.register("stradpole", () -> registerEntity(EntityType.Builder.of(EntityStradpole::new, MobCategory.WATER_AMBIENT).sized(0.5F, 0.5F).fireImmune().setTrackingRange(4), "stradpole"));
    public static final RegistrySupplier<EntityType<EntityStraddleboard>> STRADDLEBOARD = registerEntity("straddleboard",
        EntityType.Builder.<EntityStraddleboard>create(EntityStraddleboard::new, SpawnGroup.MISC)
                .setDimensions(1.5F, 0.35F)
//                .setCustomClientFactory(EntityStraddleboard::new)
                .makeFireImmune()
//                .setUpdateInterval(1)
                .maxTrackingRange(10)
                /*.setShouldReceiveVelocityUpdates(true)*/);
    public static final RegistrySupplier<EntityType<EntityEmu>> EMU = registerEntity("emu",
            EntityType.Builder.create(EntityEmu::new, SpawnGroup.CREATURE)
                    .setDimensions(1.1F, 1.8F)
                    .maxTrackingRange(10));
    public static final RegistrySupplier<EntityType<EntityEmuEgg>> EMU_EGG = registerEntity("emu_egg",
            EntityType.Builder.<EntityEmuEgg>create(EntityEmuEgg::new, SpawnGroup.MISC)
                    .setDimensions(0.5F, 0.5F)
//                    .setCustomClientFactory(EntityEmuEgg::new)
                    .makeFireImmune());
    public static final RegistrySupplier<EntityType<EntityPlatypus>> PLATYPUS = registerEntity("platypus",
        EntityType.Builder.create(EntityPlatypus::new, SpawnGroup.CREATURE)
                .setDimensions(0.8F, 0.5F)
                .maxTrackingRange(10));
//    public static final RegistrySupplier<EntityType<EntityDropBear>> DROPBEAR = DEF_REG.register("dropbear", () -> registerEntity(EntityType.Builder.of(EntityDropBear::new, SpawnGroup.MONSTER).sized(1.65F, 1.5F).fireImmune().setTrackingRange(8), "dropbear"));
//    public static final RegistrySupplier<EntityType<EntityTasmanianDevil>> TASMANIAN_DEVIL = DEF_REG.register("tasmanian_devil", () -> registerEntity(EntityType.Builder.of(EntityTasmanianDevil::new, SpawnGroup.CREATURE).sized(0.7F, 0.8F).setTrackingRange(10), "tasmanian_devil"));
//    public static final RegistrySupplier<EntityType<EntityKangaroo>> KANGAROO = DEF_REG.register("kangaroo", () -> registerEntity(EntityType.Builder.of(EntityKangaroo::new, SpawnGroup.CREATURE).sized(1.65F, 1.5F).setTrackingRange(10), "kangaroo"));
    public static final RegistrySupplier<EntityType<EntityCachalotWhale>> CACHALOT_WHALE = registerEntity("cachalot_whale",
        EntityType.Builder.create(EntityCachalotWhale::new, SpawnGroup.WATER_CREATURE)
                .setDimensions(9F, 4.0F));

    public static final RegistrySupplier<EntityType<EntityCachalotEcho>> CACHALOT_ECHO = registerEntity("cachalot_echo",
            EntityType.Builder.<EntityCachalotEcho>create(EntityCachalotEcho::new, SpawnGroup.MISC)
                    .setDimensions(2F, 2F)
                    .makeFireImmune());

    //    public static final RegistrySupplier<EntityType<EntityLeafcutterAnt>> LEAFCUTTER_ANT = DEF_REG.register("leafcutter_ant", () -> registerEntity(EntityType.Builder.of(EntityLeafcutterAnt::new, SpawnGroup.CREATURE).sized(0.8F, 0.5F).setTrackingRange(5), "leafcutter_ant"));
    public static final RegistrySupplier<EntityType<EntityEnderiophage>> ENDERIOPHAGE = registerEntity("enderiophage",
            EntityType.Builder.create(EntityEnderiophage::new, SpawnGroup.CREATURE)
                    .setDimensions(0.85F, 1.95F)
//                    .setUpdateInterval(1)
                    .maxTrackingRange(8));
    public static final RegistrySupplier<EntityType<EntityEnderiophageRocket>> ENDERIOPHAGE_ROCKET = registerEntity("enderiophage_rocket",
            EntityType.Builder.<EntityEnderiophageRocket>create(EntityEnderiophageRocket::new, SpawnGroup.MISC)
                    .setDimensions(0.5F, 0.5F)
//                    .setCustomClientFactory(EntityEnderiophageRocket::new)
                    .makeFireImmune());
    public static final RegistrySupplier<EntityType<EntityBaldEagle>> BALD_EAGLE = registerEntity("bald_eagle",
            EntityType.Builder.create(EntityBaldEagle::new, SpawnGroup.CREATURE)
                    .setDimensions(0.5F, 0.95F)
//                    .setUpdateInterval(1)
                    .maxTrackingRange(14));
//    public static final RegistrySupplier<EntityType<EntityTiger>> TIGER = DEF_REG.register("tiger", () -> registerEntity(EntityType.Builder.of(EntityTiger::new, SpawnGroup.CREATURE).sized(1.45F, 1.2F).setTrackingRange(10), "tiger"));
//    public static final RegistrySupplier<EntityType<EntityTarantulaHawk>> TARANTULA_HAWK = DEF_REG.register("tarantula_hawk", () -> registerEntity(EntityType.Builder.of(EntityTarantulaHawk::new, SpawnGroup.CREATURE).sized(1.2F, 0.9F).setTrackingRange(10), "tarantula_hawk"));
//    public static final RegistrySupplier<EntityType<EntityVoidWorm>> VOID_WORM = DEF_REG.register("void_worm", () -> registerEntity(EntityType.Builder.of(EntityVoidWorm::new, SpawnGroup.MONSTER).sized(3.4F, 3F).fireImmune().setTrackingRange(20).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1), "void_worm"));
//    public static final RegistrySupplier<EntityType<EntityVoidWormPart>> VOID_WORM_PART = DEF_REG.register("void_worm_part", () -> registerEntity(EntityType.Builder.of(EntityVoidWormPart::new, SpawnGroup.MONSTER).sized(1.2F, 1.35F).fireImmune().setTrackingRange(20).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1), "void_worm_part"));
//    public static final RegistrySupplier<EntityType<EntityVoidWormShot>> VOID_WORM_SHOT = DEF_REG.register("void_worm_shot", () -> registerEntity(EntityType.Builder.of(EntityVoidWormShot::new, SpawnGroup.MISC).sized(0.5F, 0.5F).setCustomClientFactory(EntityVoidWormShot::new).fireImmune(), "void_worm_shot"));
//    public static final RegistrySupplier<EntityType<EntityVoidPortal>> VOID_PORTAL = DEF_REG.register("void_portal", () -> registerEntity(EntityType.Builder.of(EntityVoidPortal::new, SpawnGroup.MISC).sized(0.5F, 0.5F).setCustomClientFactory(EntityVoidPortal::new).fireImmune(), "void_portal"));
    public static final RegistrySupplier<EntityType<EntityFrilledShark>> FRILLED_SHARK = registerEntity("frilled_shark",
        EntityType.Builder.create(EntityFrilledShark::new, SpawnGroup.WATER_CREATURE)
                .setDimensions(1.3F, 0.4F)
                .maxTrackingRange(8));
    public static final RegistrySupplier<EntityType<EntityMimicOctopus>> MIMIC_OCTOPUS = registerEntity("mimic_octopus",
        EntityType.Builder.create(EntityMimicOctopus::new, SpawnGroup.WATER_CREATURE)
                .setDimensions(0.9F, 0.6F)
                .maxTrackingRange(8));
//    public static final RegistrySupplier<EntityType<EntitySeagull>> SEAGULL = DEF_REG.register("seagull", () -> registerEntity(EntityType.Builder.of(EntitySeagull::new, SpawnGroup.CREATURE).sized(0.45F, 0.45F).setTrackingRange(10), "seagull"));
    public static final RegistrySupplier<EntityType<EntityFroststalker>> FROSTSTALKER = registerEntity("froststalker",
        EntityType.Builder.create(EntityFroststalker::new, SpawnGroup.CREATURE)
                .setDimensions(0.95F, 1.15F)
                .allowSpawningInside(Blocks.POWDER_SNOW));
    public static final RegistrySupplier<EntityType<EntityIceShard>> ICE_SHARD = registerEntity("ice_shard",
            EntityType.Builder.<EntityIceShard>create(EntityIceShard::new, SpawnGroup.MISC)
                    .setDimensions(0.45F, 0.45F)
//                    .setCustomClientFactory(EntityIceShard::new)
                    .makeFireImmune());
//    public static final RegistrySupplier<EntityType<EntityTusklin>> TUSKLIN = DEF_REG.register("tusklin", () -> registerEntity(EntityType.Builder.of(EntityTusklin::new, SpawnGroup.CREATURE).sized(2.2F, 1.9F).immuneTo(Blocks.POWDER_SNOW).setTrackingRange(10), "tusklin"));
    public static final RegistrySupplier<EntityType<EntityLaviathan>> LAVIATHAN = registerEntity("laviathan",
            EntityType.Builder.create(EntityLaviathan::new, SpawnGroup.CREATURE)
                    .setDimensions(3.3F, 2.4F)
                    .makeFireImmune()
//                    .setShouldReceiveVelocityUpdates(true)
//                    .setUpdateInterval(1)
                    .maxTrackingRange(10));
//    public static final RegistrySupplier<EntityType<EntityCosmaw>> COSMAW = DEF_REG.register("cosmaw", () -> registerEntity(EntityType.Builder.of(EntityCosmaw::new, SpawnGroup.CREATURE).sized(1.95F, 1.8F).setTrackingRange(10), "cosmaw"));
    public static final RegistrySupplier<EntityType<EntityToucan>> TOUCAN = registerEntity("toucan",
        EntityType.Builder.create(EntityToucan::new, SpawnGroup.CREATURE)
                .setDimensions(0.45F, 0.45F)
                .maxTrackingRange(10));
//    public static final RegistrySupplier<EntityType<EntityManedWolf>> MANED_WOLF = DEF_REG.register("maned_wolf", () -> registerEntity(EntityType.Builder.of(EntityManedWolf::new, SpawnGroup.CREATURE).sized(0.9F, 1.26F).setTrackingRange(10), "maned_wolf"));
    public static final RegistrySupplier<EntityType<EntityAnaconda>> ANACONDA = registerEntity("anaconda",
            EntityType.Builder.create(EntityAnaconda::new, SpawnGroup.CREATURE)
                .setDimensions(0.8F, 0.8F)
                .maxTrackingRange(10)
    );

    public static final RegistrySupplier<EntityType<EntityAnacondaPart>> ANACONDA_PART = registerEntity("anaconda_part",
            EntityType.Builder.<EntityAnacondaPart>create(EntityAnacondaPart::new, SpawnGroup.MISC)
                    .setDimensions(0.8F, 0.8F)
                    .trackingTickInterval(1)
                    .maxTrackingRange(10)
    );

    //    public static final RegistrySupplier<EntityType<EntityVineLasso>> VINE_LASSO = DEF_REG.register("vine_lasso", () -> registerEntity(EntityType.Builder.of(EntityVineLasso::new, SpawnGroup.MISC).sized(0.85F, 0.2F).setCustomClientFactory(EntityVineLasso::new).fireImmune(), "vine_lasso"));
//    public static final RegistrySupplier<EntityType<EntityAnteater>> ANTEATER = DEF_REG.register("anteater", () -> registerEntity(EntityType.Builder.of(EntityAnteater::new, SpawnGroup.CREATURE).sized(1.3F, 1.1F).setTrackingRange(10), "anteater"));
//    public static final RegistrySupplier<EntityType<EntityRockyRoller>> ROCKY_ROLLER = DEF_REG.register("rocky_roller", () -> registerEntity(EntityType.Builder.of(EntityRockyRoller::new, SpawnGroup.MONSTER).sized(1.2F, 1.45F).setTrackingRange(8), "rocky_roller"));
//    public static final RegistrySupplier<EntityType<EntityFlutter>> FLUTTER = DEF_REG.register("flutter", () -> registerEntity(EntityType.Builder.of(EntityFlutter::new, MobCategory.AMBIENT).sized(0.5F, 0.7F).setTrackingRange(6), "flutter"));
//    public static final RegistrySupplier<EntityType<EntityPollenBall>> POLLEN_BALL = DEF_REG.register("pollen_ball", () -> registerEntity(EntityType.Builder.of(EntityPollenBall::new, SpawnGroup.MISC).sized(0.35F, 0.35F).setCustomClientFactory(EntityPollenBall::new).fireImmune(), "pollen_ball"));
//    public static final RegistrySupplier<EntityType<EntityGeladaMonkey>> GELADA_MONKEY = DEF_REG.register("gelada_monkey", () -> registerEntity(EntityType.Builder.of(EntityGeladaMonkey::new, SpawnGroup.CREATURE).sized(1.2F, 1.2F).setTrackingRange(10), "gelada_monkey"));
//    public static final RegistrySupplier<EntityType<EntityJerboa>> JERBOA = DEF_REG.register("jerboa", () -> registerEntity(EntityType.Builder.of(EntityJerboa::new, MobCategory.AMBIENT).sized(0.5F, 0.5F).setTrackingRange(5), "jerboa"));
    public static final RegistrySupplier<EntityType<EntityTerrapin>> TERRAPIN = registerEntity("terrapin",
            EntityType.Builder.create(EntityTerrapin::new, SpawnGroup.WATER_AMBIENT)
                    .setDimensions(0.75F, 0.45F)
                    .maxTrackingRange(5));
    public static final RegistrySupplier<EntityType<EntityCombJelly>> COMB_JELLY = registerEntity("comb_jelly",
            EntityType.Builder.create(EntityCombJelly::new, SpawnGroup.WATER_AMBIENT)
                    .setDimensions(0.65F, 0.8F)
                    .maxTrackingRange(5));
//    public static final RegistrySupplier<EntityType<EntityCosmicCod>> COSMIC_COD = DEF_REG.register("cosmic_cod", () -> registerEntity(EntityType.Builder.of(EntityCosmicCod::new, MobCategory.AMBIENT).sized(0.85F, 0.4F).setTrackingRange(5), "cosmic_cod"));
    public static final RegistrySupplier<EntityType<EntityBunfungus>> BUNFUNGUS = registerEntity("bunfungus",
            EntityType.Builder.create(EntityBunfungus::new, SpawnGroup.CREATURE)
                    .setDimensions(1.85F, 2.1F)
                    .maxTrackingRange(10));
    public static final RegistrySupplier<EntityType<EntityBison>> BISON = registerEntity("bison",
            PlatformEntityConstructors.createBisonEntityBuilder(SpawnGroup.CREATURE)
                    .setDimensions(2.4F, 2.1F)
                    .maxTrackingRange(10));
    public static final RegistrySupplier<EntityType<EntityGiantSquid>> GIANT_SQUID = registerEntity("giant_squid",
            EntityType.Builder.create(EntityGiantSquid::new, SpawnGroup.WATER_CREATURE)
                    .setDimensions(0.9F, 1.2F)
                    .maxTrackingRange(10));

    //    public static final RegistrySupplier<EntityType<EntitySquidGrapple>> SQUID_GRAPPLE = DEF_REG.register("squid_grapple", () -> registerEntity(EntityType.Builder.of(EntitySquidGrapple::new, SpawnGroup.MISC).sized(0.5F, 0.5F).setCustomClientFactory(EntitySquidGrapple::new).fireImmune(), "squid_grapple"));
//    public static final RegistrySupplier<EntityType<EntitySeaBear>> SEA_BEAR = DEF_REG.register("sea_bear", () -> registerEntity(EntityType.Builder.of(EntitySeaBear::new, SpawnGroup.WATER_CREATURE).sized(2.4F, 1.99F).setTrackingRange(10), "sea_bear"));
//    public static final RegistrySupplier<EntityType<EntityDevilsHolePupfish>> DEVILS_HOLE_PUPFISH = DEF_REG.register("devils_hole_pupfish", () -> registerEntity(EntityType.Builder.of(EntityDevilsHolePupfish::new, MobCategory.WATER_AMBIENT).sized(0.6F, 0.4F).setTrackingRange(4), "devils_hole_pupfish"));
//    public static final RegistrySupplier<EntityType<EntityCatfish>> CATFISH = DEF_REG.register("catfish", () -> registerEntity(EntityType.Builder.of(EntityCatfish::new, MobCategory.WATER_AMBIENT).sized(0.9F, 0.6F).setTrackingRange(5), "catfish"));
//    public static final RegistrySupplier<EntityType<EntityFlyingFish>> FLYING_FISH = DEF_REG.register("flying_fish", () -> registerEntity(EntityType.Builder.of(EntityFlyingFish::new, MobCategory.WATER_AMBIENT).sized(0.6F, 0.4F).setTrackingRange(5), "flying_fish"));
//    public static final RegistrySupplier<EntityType<EntitySkelewag>> SKELEWAG = DEF_REG.register("skelewag", () -> registerEntity(EntityType.Builder.of(EntitySkelewag::new, SpawnGroup.MONSTER).sized(2F, 1.2F).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).setTrackingRange(8), "skelewag"));
//    public static final RegistrySupplier<EntityType<EntityRainFrog>> RAIN_FROG = DEF_REG.register("rain_frog", () -> registerEntity(EntityType.Builder.of(EntityRainFrog::new, MobCategory.AMBIENT).sized(0.55F, 0.5F).setTrackingRange(5), "rain_frog"));
//    public static final RegistrySupplier<EntityType<EntityPotoo>> POTOO = DEF_REG.register("potoo", () -> registerEntity(EntityType.Builder.of(EntityPotoo::new, SpawnGroup.CREATURE).sized(0.6F, 0.8F).setTrackingRange(10), "potoo"));
//    public static final RegistrySupplier<EntityType<EntityMudskipper>> MUDSKIPPER = DEF_REG.register("mudskipper", () -> registerEntity(EntityType.Builder.of(EntityMudskipper::new, SpawnGroup.CREATURE).sized(0.7F, 0.44F).setTrackingRange(10), "mudskipper"));
//    public static final RegistrySupplier<EntityType<EntityMudBall>> MUD_BALL = DEF_REG.register("mud_ball", () -> registerEntity(EntityType.Builder.of(EntityMudBall::new, SpawnGroup.MISC).sized(0.35F, 0.35F).setCustomClientFactory(EntityMudBall::new).fireImmune(), "mud_ball"));
//    public static final RegistrySupplier<EntityType<EntityRhinoceros>> RHINOCEROS = DEF_REG.register("rhinoceros", () -> registerEntity(EntityType.Builder.of(EntityRhinoceros::new, SpawnGroup.CREATURE).sized(2.3F, 2.4F).setTrackingRange(10), "rhinoceros"));
//    public static final RegistrySupplier<EntityType<EntitySugarGlider>> SUGAR_GLIDER = DEF_REG.register("sugar_glider", () -> registerEntity(EntityType.Builder.of(EntitySugarGlider::new, SpawnGroup.CREATURE).sized(0.8F, 0.45F).setTrackingRange(10), "sugar_glider"));
//    public static final RegistrySupplier<EntityType<EntityFarseer>> FARSEER = DEF_REG.register("farseer", () -> registerEntity(EntityType.Builder.of(EntityFarseer::new, SpawnGroup.MONSTER).sized(0.99F, 1.5F).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).fireImmune().setTrackingRange(8), "farseer"));
//    public static final RegistrySupplier<EntityType<EntitySkreecher>> SKREECHER = DEF_REG.register("skreecher", () -> registerEntity(EntityType.Builder.of(EntitySkreecher::new, SpawnGroup.CREATURE).sized(0.99F, 0.95F).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).setTrackingRange(8), "skreecher"));
//    public static final RegistrySupplier<EntityType<EntityUnderminer>> UNDERMINER = DEF_REG.register("underminer", () -> registerEntity(EntityType.Builder.of(EntityUnderminer::new, MobCategory.AMBIENT).sized(0.8F, 1.8F).setTrackingRange(8), "underminer"));
//    public static final RegistrySupplier<EntityType<EntityMurmur>> MURMUR = DEF_REG.register("murmur", () -> registerEntity(EntityType.Builder.of(EntityMurmur::new, SpawnGroup.MONSTER).sized(0.7F, 1.45F).setTrackingRange(8), "murmur"));
//    public static final RegistrySupplier<EntityType<EntityMurmurHead>> MURMUR_HEAD = DEF_REG.register("murmur_head", () -> registerEntity(EntityType.Builder.of(EntityMurmurHead::new, SpawnGroup.MONSTER).sized(0.55F, 0.55F).setTrackingRange(8), "murmur_head"));
//    public static final RegistrySupplier<EntityType<EntityTendonSegment>> TENDON_SEGMENT = DEF_REG.register("tendon_segment", () -> registerEntity(EntityType.Builder.of(EntityTendonSegment::new, SpawnGroup.MISC).sized(0.1F, 0.1F).setCustomClientFactory(EntityTendonSegment::new).fireImmune(), "tendon_segment"));
//    public static final RegistrySupplier<EntityType<EntitySkunk>> SKUNK = DEF_REG.register("skunk", () -> registerEntity(EntityType.Builder.of(EntitySkunk::new, SpawnGroup.CREATURE).sized(0.85F, 0.65F).setTrackingRange(10), "skunk"));
//    public static final RegistrySupplier<EntityType<EntityFart>> FART = DEF_REG.register("fart", () -> registerEntity(EntityType.Builder.of(EntityFart::new, SpawnGroup.MISC).sized(0.7F, 0.3F).setCustomClientFactory(EntityFart::new).fireImmune(), "fart"));
//    public static final RegistrySupplier<EntityType<EntityBananaSlug>> BANANA_SLUG = DEF_REG.register("banana_slug", () -> registerEntity(EntityType.Builder.of(EntityBananaSlug::new, SpawnGroup.CREATURE).sized(0.8F, 0.4F).setTrackingRange(10), "banana_slug"));
    public static final RegistrySupplier<EntityType<EntityBlueJay>> BLUE_JAY = registerEntity("blue_jay",
            EntityType.Builder.create(EntityBlueJay::new, SpawnGroup.CREATURE)
                    .setDimensions(0.5F, 0.6F)
                    .maxTrackingRange(10));
    public static final RegistrySupplier<EntityType<EntityCaiman>> CAIMAN = registerEntity("caiman",
            EntityType.Builder.create(EntityCaiman::new, SpawnGroup.CREATURE)
                    .setDimensions(1.3F, 0.6F)
                    .maxTrackingRange(10));
    public static final RegistrySupplier<EntityType<EntityTriops>> TRIOPS = registerEntity("triops",
            EntityType.Builder.create(EntityTriops::new, SpawnGroup.WATER_AMBIENT)
                    .setDimensions(0.7F, 0.25F)
                    .maxTrackingRange(5));

    private static <T extends Entity> RegistrySupplier<EntityType<T>> registerEntity(String name, EntityType.Builder<T> builder) {
        return DEF_REG.register(name, () -> builder.build(name));
    }

    public static void initializeAttributes() {
        //FIXME forge
//        SpawnRestriction.Location spawnsOnLeaves = SpawnRestriction.Location.create("am_leaves", AMEntityRegistry::createLeavesSpawnPlacement);
        SpawnRestriction.register(GRIZZLY_BEAR.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::isValidNaturalSpawn);
//        SpawnRestriction.register(ROADRUNNER.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityRoadrunner::canRoadrunnerSpawn);
        SpawnRestriction.register(BONE_SERPENT.get(), SpawnRestriction.Location.IN_LAVA, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityBoneSerpent::canBoneSerpentSpawn);
//        SpawnRestriction.register(GAZELLE.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::isValidNaturalSpawn);
        SpawnRestriction.register(CROCODILE.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityCrocodile::canCrocodileSpawn);
//        SpawnRestriction.register(FLY.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityFly::canFlySpawn);
//        SpawnRestriction.register(HUMMINGBIRD.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING, EntityHummingbird::canHummingbirdSpawn);
        SpawnRestriction.register(ORCA.get(), SpawnRestriction.Location.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityOrca::canOrcaSpawn);
//        SpawnRestriction.register(SUNBIRD.get(), SpawnRestriction.Location.NO_RESTRICTIONS, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntitySunbird::canSunbirdSpawn);
//        SpawnRestriction.register(GORILLA.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING, EntityGorilla::canGorillaSpawn);
        SpawnRestriction.register(CRIMSON_MOSQUITO.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityCrimsonMosquito::canMosquitoSpawn);
//        SpawnRestriction.register(RATTLESNAKE.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityRattlesnake::canRattlesnakeSpawn);
        SpawnRestriction.register(ENDERGRADE.get(), SpawnRestriction.Location.NO_RESTRICTIONS, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityEndergrade::canEndergradeSpawn);
//        SpawnRestriction.register(HAMMERHEAD_SHARK.get(), SpawnRestriction.Location.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityHammerheadShark::canHammerheadSharkSpawn);
        SpawnRestriction.register(LOBSTER.get(), SpawnRestriction.Location.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityLobster::canLobsterSpawn);
//        SpawnRestriction.register(KOMODO_DRAGON.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityKomodoDragon::canKomodoDragonSpawn);
//        SpawnRestriction.register(CAPUCHIN_MONKEY.get(), spawnsOnLeaves, Heightmap.Type.MOTION_BLOCKING, EntityCapuchinMonkey::canCapuchinSpawn);
        SpawnRestriction.register(CENTIPEDE_HEAD.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityCentipedeHead::canCentipedeSpawn);
        SpawnRestriction.register(WARPED_TOAD.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING, EntityWarpedToad::canWarpedToadSpawn);
//        SpawnRestriction.register(MOOSE.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMoose::canMooseSpawn);
//        SpawnRestriction.register(MIMICUBE.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules);
        SpawnRestriction.register(RACCOON.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::isValidNaturalSpawn);
        SpawnRestriction.register(BLOBFISH.get(), SpawnRestriction.Location.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityBlobfish::canBlobfishSpawn);
//        SpawnRestriction.register(SEAL.get(), SpawnRestriction.Location.NO_RESTRICTIONS, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntitySeal::canSealSpawn);
        SpawnRestriction.register(COCKROACH.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityCockroach::canCockroachSpawn);
//        SpawnRestriction.register(SHOEBILL.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::isValidNaturalSpawn);
        SpawnRestriction.register(ELEPHANT.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::isValidNaturalSpawn);
//        SpawnRestriction.register(SOUL_VULTURE.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntitySoulVulture::canVultureSpawn);
//        SpawnRestriction.register(SNOW_LEOPARD.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntitySnowLeopard::canSnowLeopardSpawn);
        SpawnRestriction.register(CROW.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING, EntityCrow::canCrowSpawn);
        SpawnRestriction.register(ALLIGATOR_SNAPPING_TURTLE.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityAlligatorSnappingTurtle::canTurtleSpawn);
        SpawnRestriction.register(MUNGUS.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMungus::canMungusSpawn);
//        SpawnRestriction.register(MANTIS_SHRIMP.get(), SpawnRestriction.Location.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMantisShrimp::canMantisShrimpSpawn);
//        SpawnRestriction.register(GUSTER.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityGuster::canGusterSpawn);
//        SpawnRestriction.register(WARPED_MOSCO.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, Monster::checkAnyLightMonsterSpawnRules);
//        SpawnRestriction.register(STRADDLER.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityStraddler::canStraddlerSpawn);
//        SpawnRestriction.register(STRADPOLE.get(), SpawnRestriction.Location.IN_LAVA, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityStradpole::canStradpoleSpawn);
        SpawnRestriction.register(EMU.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityEmu::canEmuSpawn);
        SpawnRestriction.register(PLATYPUS.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityPlatypus::canPlatypusSpawn);
//        SpawnRestriction.register(DROPBEAR.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, Monster::checkAnyLightMonsterSpawnRules);
//        SpawnRestriction.register(TASMANIAN_DEVIL.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::isValidNaturalSpawn);
//        SpawnRestriction.register(KANGAROO.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityKangaroo::canKangarooSpawn);
        SpawnRestriction.register(CACHALOT_WHALE.get(), SpawnRestriction.Location.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityCachalotWhale::canCachalotWhaleSpawn);
//        SpawnRestriction.register(LEAFCUTTER_ANT.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::isValidNaturalSpawn);
        SpawnRestriction.register(ENDERIOPHAGE.get(), SpawnRestriction.Location.NO_RESTRICTIONS, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityEnderiophage::canEnderiophageSpawn);
        SpawnRestriction.register(BALD_EAGLE.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING, EntityBaldEagle::canEagleSpawn);
//        SpawnRestriction.register(TIGER.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityTiger::canTigerSpawn);
//        SpawnRestriction.register(TARANTULA_HAWK.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityTarantulaHawk::canTarantulaHawkSpawn);
//        SpawnRestriction.register(VOID_WORM.get(), SpawnRestriction.Location.NO_RESTRICTIONS, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityVoidWorm::canVoidWormSpawn);
        SpawnRestriction.register(FRILLED_SHARK.get(), SpawnRestriction.Location.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityFrilledShark::canFrilledSharkSpawn);
        SpawnRestriction.register(MIMIC_OCTOPUS.get(), SpawnRestriction.Location.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMimicOctopus::canMimicOctopusSpawn);
//        SpawnRestriction.register(SEAGULL.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntitySeagull::canSeagullSpawn);
//        SpawnRestriction.register(FROSTSTALKER.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityFroststalker::canFroststalkerSpawn);
//        SpawnRestriction.register(TUSKLIN.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityTusklin::canTusklinSpawn);
        SpawnRestriction.register(LAVIATHAN.get(), SpawnRestriction.Location.IN_LAVA, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityLaviathan::canLaviathanSpawn);
//        SpawnRestriction.register(COSMAW.get(), SpawnRestriction.Location.NO_RESTRICTIONS, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityCosmaw::canCosmawSpawn);
//        FIXME SpawnRestriction.register(TOUCAN.get(), spawnsOnLeaves, Heightmap.Type.MOTION_BLOCKING, EntityToucan::canToucanSpawn);
//        SpawnRestriction.register(MANED_WOLF.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityManedWolf::isValidNaturalSpawn);
        SpawnRestriction.register(ANACONDA.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityAnaconda::canAnacondaSpawn);
//        SpawnRestriction.register(ANTEATER.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityAnteater::canAnteaterSpawn);
//        SpawnRestriction.register(ROCKY_ROLLER.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityRockyRoller::checkRockyRollerSpawnRules);
//        SpawnRestriction.register(FLUTTER.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityFlutter::canFlutterSpawn);
//        SpawnRestriction.register(GELADA_MONKEY.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityGeladaMonkey::isValidNaturalSpawn);
//        SpawnRestriction.register(JERBOA.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityJerboa::canJerboaSpawn);
        SpawnRestriction.register(TERRAPIN.get(), SpawnRestriction.Location.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityTerrapin::canTerrapinSpawn);
        SpawnRestriction.register(COMB_JELLY.get(), SpawnRestriction.Location.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityCombJelly::canCombJellySpawn);
        SpawnRestriction.register(BUNFUNGUS.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityBunfungus::canBunfungusSpawn);
        SpawnRestriction.register(BISON.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityBison::isValidNaturalSpawn);
        SpawnRestriction.register(GIANT_SQUID.get(), SpawnRestriction.Location.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityGiantSquid::canGiantSquidSpawn);
//        SpawnRestriction.register(DEVILS_HOLE_PUPFISH.get(), SpawnRestriction.Location.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityDevilsHolePupfish::canPupfishSpawn);
//        SpawnRestriction.register(CATFISH.get(), SpawnRestriction.Location.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityCatfish::canCatfishSpawn);
//        SpawnRestriction.register(FLYING_FISH.get(), SpawnRestriction.Location.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, WaterAnimal::checkSurfaceWaterAnimalSpawnRules);
//        SpawnRestriction.register(SKELEWAG.get(), SpawnRestriction.Location.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntitySkelewag::canSkelewagSpawn);
//        SpawnRestriction.register(RAIN_FROG.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityRainFrog::canRainFrogSpawn);
//        SpawnRestriction.register(POTOO.get(), spawnsOnLeaves, Heightmap.Type.MOTION_BLOCKING, EntityPotoo::canPotooSpawn);
//        SpawnRestriction.register(MUDSKIPPER.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMudskipper::canMudskipperSpawn);
//        SpawnRestriction.register(RHINOCEROS.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityRhinoceros::isValidNaturalSpawn);
//        SpawnRestriction.register(SUGAR_GLIDER.get(), spawnsOnLeaves, Heightmap.Type.MOTION_BLOCKING, EntitySugarGlider::canSugarGliderSpawn);
//        SpawnRestriction.register(FARSEER.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityFarseer::checkFarseerSpawnRules);
//        SpawnRestriction.register(SKREECHER.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntitySkreecher::checkSkreecherSpawnRules);
//        SpawnRestriction.register(UNDERMINER.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityUnderminer::checkUnderminerSpawnRules);
//        SpawnRestriction.register(MURMUR.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityMurmur::checkMurmurSpawnRules);
//        SpawnRestriction.register(SKUNK.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntitySkunk::isValidNaturalSpawn);
//        SpawnRestriction.register(BANANA_SLUG.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityBananaSlug::checkBananaSlugSpawnRules);
//        FIXME SpawnRestriction.register(BLUE_JAY.get(), spawnsOnLeaves, Heightmap.Type.MOTION_BLOCKING, EntityBlueJay::checkBlueJaySpawnRules);
        SpawnRestriction.register(CAIMAN.get(), SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EntityCaiman::canCaimanSpawn);
        SpawnRestriction.register(TRIOPS.get(), SpawnRestriction.Location.IN_WATER, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, WaterCreatureEntity::canSpawn);
        EntityAttributeRegistry.register(GRIZZLY_BEAR, EntityGrizzlyBear::createAttributes);
//        event.put(ROADRUNNER.get(), EntityRoadrunner.bakeAttributes().build());
        EntityAttributeRegistry.register(BONE_SERPENT, EntityBoneSerpent::createAttributes);
        EntityAttributeRegistry.register(BONE_SERPENT_PART, EntityBoneSerpentPart::createAttributes);
//        event.put(GAZELLE.get(), EntityGazelle.bakeAttributes().build());
        EntityAttributeRegistry.register(CROCODILE, EntityCrocodile::createAttributes);
//        event.put(FLY.get(), EntityFly.bakeAttributes().build());
//        event.put(HUMMINGBIRD.get(), EntityHummingbird.bakeAttributes().build());
        EntityAttributeRegistry.register(ORCA, EntityOrca::createAttributes);
//        event.put(SUNBIRD.get(), EntitySunbird.bakeAttributes().build());
//        event.put(GORILLA.get(), EntityGorilla.bakeAttributes().build());
        EntityAttributeRegistry.register(CRIMSON_MOSQUITO, EntityCrimsonMosquito::createAttributes);
//        event.put(RATTLESNAKE.get(), EntityRattlesnake.bakeAttributes().build());
        EntityAttributeRegistry.register(ENDERGRADE, EntityEndergrade::createAttributes);
//        event.put(HAMMERHEAD_SHARK.get(), EntityHammerheadShark.bakeAttributes().build());
        EntityAttributeRegistry.register(LOBSTER, EntityLobster::createAttributes);
//        event.put(KOMODO_DRAGON.get(), EntityKomodoDragon.bakeAttributes().build());
//        event.put(CAPUCHIN_MONKEY.get(), EntityCapuchinMonkey.bakeAttributes().build());
        EntityAttributeRegistry.register(CENTIPEDE_HEAD, EntityCentipedeHead::createAttributes);
        EntityAttributeRegistry.register(CENTIPEDE_BODY, EntityCentipedeBody::createAttributes);
        EntityAttributeRegistry.register(CENTIPEDE_TAIL, EntityCentipedeTail::createAttributes);
        EntityAttributeRegistry.register(WARPED_TOAD, EntityWarpedToad::createAttributes);
//        event.put(MOOSE.get(), EntityMoose.bakeAttributes().build());
//        event.put(MIMICUBE.get(), EntityMimicube.bakeAttributes().build());
        EntityAttributeRegistry.register(RACCOON, EntityRaccoon::createAttributes);
        EntityAttributeRegistry.register(BLOBFISH, EntityBlobfish::createAttributes);
//        event.put(SEAL.get(), EntitySeal.bakeAttributes().build());
        EntityAttributeRegistry.register(COCKROACH, EntityCockroach::createAttributes);
//        event.put(SHOEBILL.get(), EntityShoebill.bakeAttributes().build());
        EntityAttributeRegistry.register(ELEPHANT, EntityElephant::createAttributes);
        EntityAttributeRegistry.register(SOUL_VULTURE, EntitySoulVulture::createAttributes);
//        event.put(SNOW_LEOPARD.get(), EntitySnowLeopard.bakeAttributes().build());
//        event.put(SPECTRE.get(), EntitySpectre.bakeAttributes().build());
        EntityAttributeRegistry.register(CROW, EntityCrow::createAttributes);
        EntityAttributeRegistry.register(ALLIGATOR_SNAPPING_TURTLE, EntityAlligatorSnappingTurtle::createAttributes);
        EntityAttributeRegistry.register(MUNGUS, EntityMungus::createAttributes);
//        event.put(MANTIS_SHRIMP.get(), EntityMantisShrimp.bakeAttributes().build());
//        event.put(GUSTER.get(), EntityGuster.bakeAttributes().build());
        EntityAttributeRegistry.register(WARPED_MOSCO, EntityWarpedMosco::createAttributes);
//        event.put(STRADDLER.get(), EntityStraddler.bakeAttributes().build());
//        event.put(STRADPOLE.get(), EntityStradpole.bakeAttributes().build());
        EntityAttributeRegistry.register(EMU, EntityEmu::createAttributes);
        EntityAttributeRegistry.register(PLATYPUS, EntityPlatypus::createAttributes);
//        event.put(DROPBEAR.get(), EntityDropBear.bakeAttributes().build());
//        event.put(TASMANIAN_DEVIL.get(), EntityTasmanianDevil.bakeAttributes().build());
//        event.put(KANGAROO.get(), EntityKangaroo.bakeAttributes().build());
        EntityAttributeRegistry.register(CACHALOT_WHALE, EntityCachalotWhale::createAttributes);
//        event.put(LEAFCUTTER_ANT.get(), EntityLeafcutterAnt.bakeAttributes().build());
        EntityAttributeRegistry.register(ENDERIOPHAGE, EntityEnderiophage::createAttributes);
        EntityAttributeRegistry.register(BALD_EAGLE, EntityBaldEagle::createAttributes);
//        event.put(TIGER.get(), EntityTiger.bakeAttributes().build());
//        event.put(TARANTULA_HAWK.get(), EntityTarantulaHawk.bakeAttributes().build());
//        event.put(VOID_WORM.get(), EntityVoidWorm.bakeAttributes().build());
//        event.put(VOID_WORM_PART.get(), EntityVoidWormPart.bakeAttributes().build());
        EntityAttributeRegistry.register(FRILLED_SHARK, EntityFrilledShark::createAttributes);
        EntityAttributeRegistry.register(MIMIC_OCTOPUS, EntityMimicOctopus::createAttributes);
//        event.put(SEAGULL.get(), EntitySeagull.bakeAttributes().build());
//        event.put(FROSTSTALKER.get(), EntityFroststalker.bakeAttributes().build());
//        event.put(TUSKLIN.get(), EntityTusklin.bakeAttributes().build());
        EntityAttributeRegistry.register(LAVIATHAN, EntityLaviathan::createAttributes);
//        event.put(COSMAW.get(), EntityCosmaw.bakeAttributes().build());
        EntityAttributeRegistry.register(TOUCAN, EntityToucan::createAttributes);
//        event.put(MANED_WOLF.get(), EntityManedWolf.bakeAttributes().build());
        EntityAttributeRegistry.register(ANACONDA, EntityAnaconda::createAttributes);
        EntityAttributeRegistry.register(ANACONDA_PART, EntityAnacondaPart::createAttributes);
//        event.put(ANTEATER.get(), EntityAnteater.bakeAttributes().build());
//        event.put(ROCKY_ROLLER.get(), EntityRockyRoller.bakeAttributes().build());
//        event.put(FLUTTER.get(), EntityFlutter.bakeAttributes().build());
//        event.put(GELADA_MONKEY.get(), EntityGeladaMonkey.bakeAttributes().build());
//        event.put(JERBOA.get(), EntityJerboa.bakeAttributes().build());
        EntityAttributeRegistry.register(TERRAPIN, EntityTerrapin::createAttributes);
        EntityAttributeRegistry.register(COMB_JELLY, EntityCombJelly::createAttributes);
//        event.put(COSMIC_COD.get(), EntityCosmicCod.bakeAttributes().build());
        EntityAttributeRegistry.register(BUNFUNGUS, EntityBunfungus::createAttributes);
        EntityAttributeRegistry.register(BISON, EntityBison::createAttributes);
        EntityAttributeRegistry.register(GIANT_SQUID, EntityGiantSquid::createAttributes);
//        event.put(SEA_BEAR.get(), EntitySeaBear.bakeAttributes().build());
//        event.put(DEVILS_HOLE_PUPFISH.get(), EntityDevilsHolePupfish.bakeAttributes().build());
//        event.put(CATFISH.get(), EntityCatfish.bakeAttributes().build());
//        event.put(FLYING_FISH.get(), EntityFlyingFish.bakeAttributes().build());
//        event.put(SKELEWAG.get(), EntitySkelewag.bakeAttributes().build());
//        event.put(RAIN_FROG.get(), EntityRainFrog.bakeAttributes().build());
//        event.put(POTOO.get(), EntityPotoo.bakeAttributes().build());
//        event.put(MUDSKIPPER.get(), EntityMudskipper.bakeAttributes().build());
//        event.put(RHINOCEROS.get(), EntityRhinoceros.bakeAttributes().build());
//        event.put(SUGAR_GLIDER.get(), EntitySugarGlider.bakeAttributes().build());
//        event.put(FARSEER.get(), EntityFarseer.bakeAttributes().build());
//        event.put(SKREECHER.get(), EntitySkreecher.bakeAttributes().build());
//        event.put(UNDERMINER.get(), EntityUnderminer.bakeAttributes().build());
//        event.put(MURMUR.get(), EntityMurmur.bakeAttributes().build());
//        event.put(MURMUR_HEAD.get(), EntityMurmurHead.bakeAttributes().build());
//        event.put(SKUNK.get(), EntitySkunk.bakeAttributes().build());
//        event.put(BANANA_SLUG.get(), EntityBananaSlug.bakeAttributes().build());
        EntityAttributeRegistry.register(BLUE_JAY, EntityBlueJay::createAttributes);
        EntityAttributeRegistry.register(CAIMAN, EntityCaiman::createAttributes);
        EntityAttributeRegistry.register(TRIOPS, EntityTriops::createAttributes);
    }

    public static Predicate<LivingEntity> buildPredicateFromTag(TagKey<EntityType<?>> entityTag){
        if(entityTag == null){
            return Predicates.alwaysFalse();
        }else{
            return (com.google.common.base.Predicate<LivingEntity>) e -> e.isAlive() && e.getType().isIn(entityTag);
        }
    }

    public static Predicate<LivingEntity> buildPredicateFromTagTameable(TagKey<EntityType<?>> entityTag, LivingEntity owner){
        if(entityTag == null){
            return Predicates.alwaysFalse();
        }else{
            return (com.google.common.base.Predicate<LivingEntity>) e -> e.isAlive() && e.getType().isIn(entityTag) && !owner.isTeammate(e);
        }
    }

    public static boolean rollSpawn(int rolls, Random random, SpawnReason reason){
        if(reason == SpawnReason.SPAWNER){
            return true;
        }else{
            return rolls <= 0 || random.nextInt(rolls) == 0;
        }
    }

    // FIXME forge
    public static boolean createLeavesSpawnPlacement(WorldView level, BlockPos pos, EntityType<?> type){
        var blockpos = pos.up();
        var blockpos1 = pos.down();
        var fluidstate = level.getFluidState(pos);
        var blockstate = level.getBlockState(pos);
        var blockstate1 = level.getBlockState(blockpos1);
        if (!blockstate1.allowsSpawning(level, blockpos1/* TODO check, SpawnRestriction.Location.ON_GROUND */, type) && !blockstate1.isIn(BlockTags.LEAVES)) {
            return false;
        } else {
            return SpawnHelper.isClearForSpawn(level, pos, blockstate, fluidstate, type)
                    && SpawnHelper.isClearForSpawn(level, blockpos, level.getBlockState(blockpos), level.getFluidState(blockpos), type);
        }
    }

}
