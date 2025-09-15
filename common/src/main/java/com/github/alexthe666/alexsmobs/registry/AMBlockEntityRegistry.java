package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.block.entity.*;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.RegistryKeys;

public class AMBlockEntityRegistry {

    public static final DeferredRegister<BlockEntityType<?>> DEF_REG = DeferredRegister.create(AlexsMobs.MOD_ID, RegistryKeys.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<CapsidBlockEntity>> CAPSID = registerBlockEntity("capsid_te",
            BlockEntityType.Builder.create(CapsidBlockEntity::new, AMBlockRegistry.CAPSID.get()));
    public static final RegistrySupplier<BlockEntityType<SculkBoomerBlockEntity>> SCULK_BOOMER = registerBlockEntity("sculk_boomer",
            BlockEntityType.Builder.create(SculkBoomerBlockEntity::new, AMBlockRegistry.SCULK_BOOMER.get()));
    public static final RegistrySupplier<BlockEntityType<TerrapinEggBlockEntity>> TERRAPIN_EGG = registerBlockEntity("terrapin_egg_te",
            BlockEntityType.Builder.create(TerrapinEggBlockEntity::new, AMBlockRegistry.TERRAPIN_EGG.get()));
    public static final RegistrySupplier<BlockEntityType<TransmutationTableBlockEntity>> TRANSMUTATION_TABLE = registerBlockEntity("transmutation_table",
            BlockEntityType.Builder.create(TransmutationTableBlockEntity::new, AMBlockRegistry.TRANSMUTATION_TABLE.get()));
    public static final RegistrySupplier<BlockEntityType<VoidWormBeakBlockEntity>> VOID_WORM_BEAK = registerBlockEntity("void_worm_beak_te",
            BlockEntityType.Builder.create(VoidWormBeakBlockEntity::new, AMBlockRegistry.VOID_WORM_BEAK.get()));


    //    public static final RegistrySupplier<BlockEntityType<TileEntityLeafcutterAnthill>> LEAFCUTTER_ANTHILL = DEF_REG.register("leafcutter_anthill_te", () -> BlockEntityType.Builder.create(TileEntityLeafcutterAnthill::new, AMBlockRegistry.LEAFCUTTER_ANTHILL.get()).build(null));

    //TODO alex reimplement
//    public static final RegistrySupplier<BlockEntityType<TileEntityEndPirateDoor>> END_PIRATE_DOOR = null;//DEF_REG.register("end_pirate_door_te", () -> BlockEntityType.Builder.create(TileEntityEndPirateDoor::new, AMBlockRegistry.END_PIRATE_DOOR.get()).build(null));
//    public static final RegistrySupplier<BlockEntityType<TileEntityEndPirateAnchor>> END_PIRATE_ANCHOR = null;// DEF_REG.register("end_pirate_anchor_te", () -> BlockEntityType.Builder.create(TileEntityEndPirateAnchor::new, AMBlockRegistry.END_PIRATE_ANCHOR.get()).build(null));
//    public static final RegistrySupplier<BlockEntityType<TileEntityEndPirateAnchorWinch>> END_PIRATE_ANCHOR_WINCH =  null;//DEF_REG.register("end_pirate_anchor_winch_te", () -> BlockEntityType.Builder.create(TileEntityEndPirateAnchorWinch::new, AMBlockRegistry.END_PIRATE_ANCHOR_WINCH.get()).build(null));
//    public static final RegistrySupplier<BlockEntityType<TileEntityEndPirateShipWheel>> END_PIRATE_SHIP_WHEEL = null;// DEF_REG.register("end_pirate_ship_wheel_te", () -> BlockEntityType.Builder.create(TileEntityEndPirateShipWheel::new, AMBlockRegistry.END_PIRATE_SHIP_WHEEL.get()).build(null));
//    public static final RegistrySupplier<BlockEntityType<TileEntityEndPirateFlag>> END_PIRATE_FLAG = null;// DEF_REG.register("end_pirate_flag_te", () -> BlockEntityType.Builder.create(TileEntityEndPirateFlag::new, AMBlockRegistry.END_PIRATE_FLAG.get()).build(null));


    private static <T extends BlockEntity> RegistrySupplier<BlockEntityType<T>> registerBlockEntity(final String id, final BlockEntityType.Builder<T> builder) {
        return DEF_REG.register(id, () -> builder.build(null));
    }
}
