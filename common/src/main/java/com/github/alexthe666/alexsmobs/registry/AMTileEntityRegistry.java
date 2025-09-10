package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.tileentity.TileEntityTerrapinEgg;
import com.github.alexthe666.alexsmobs.tileentity.TileEntityVoidWormBeak;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.RegistryKeys;

public class AMTileEntityRegistry {

    public static final DeferredRegister<BlockEntityType<?>> DEF_REG = DeferredRegister.create(AlexsMobs.MOD_ID, RegistryKeys.BLOCK_ENTITY_TYPE);

//    public static final RegistrySupplier<BlockEntityType<TileEntityLeafcutterAnthill>> LEAFCUTTER_ANTHILL = DEF_REG.register("leafcutter_anthill_te", () -> BlockEntityType.Builder.create(TileEntityLeafcutterAnthill::new, AMBlockRegistry.LEAFCUTTER_ANTHILL.get()).build(null));
//    public static final RegistrySupplier<BlockEntityType<TileEntityCapsid>> CAPSID = DEF_REG.register("capsid_te", () -> BlockEntityType.Builder.create(TileEntityCapsid::new, AMBlockRegistry.CAPSID.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<TileEntityVoidWormBeak>> VOID_WORM_BEAK = DEF_REG.register("void_worm_beak_te", () -> BlockEntityType.Builder.create(TileEntityVoidWormBeak::new, AMBlockRegistry.VOID_WORM_BEAK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<TileEntityTerrapinEgg>> TERRAPIN_EGG = DEF_REG.register("terrapin_egg_te", () -> BlockEntityType.Builder.create(TileEntityTerrapinEgg::new, AMBlockRegistry.TERRAPIN_EGG.get()).build(null));
//    public static final RegistrySupplier<BlockEntityType<TileEntityTransmutationTable>> TRANSMUTATION_TABLE = DEF_REG.register("transmutation_table", () -> BlockEntityType.Builder.create(TileEntityTransmutationTable::new, AMBlockRegistry.TRANSMUTATION_TABLE.get()).build(null));
//    public static final RegistrySupplier<BlockEntityType<TileEntitySculkBoomer>> SCULK_BOOMER = DEF_REG.register("sculk_boomer", () -> BlockEntityType.Builder.create(TileEntitySculkBoomer::new, AMBlockRegistry.SCULK_BOOMER.get()).build(null));
    //TODO alex reimplement
//    public static final RegistrySupplier<BlockEntityType<TileEntityEndPirateDoor>> END_PIRATE_DOOR = null;//DEF_REG.register("end_pirate_door_te", () -> BlockEntityType.Builder.create(TileEntityEndPirateDoor::new, AMBlockRegistry.END_PIRATE_DOOR.get()).build(null));
//    public static final RegistrySupplier<BlockEntityType<TileEntityEndPirateAnchor>> END_PIRATE_ANCHOR = null;// DEF_REG.register("end_pirate_anchor_te", () -> BlockEntityType.Builder.create(TileEntityEndPirateAnchor::new, AMBlockRegistry.END_PIRATE_ANCHOR.get()).build(null));
//    public static final RegistrySupplier<BlockEntityType<TileEntityEndPirateAnchorWinch>> END_PIRATE_ANCHOR_WINCH =  null;//DEF_REG.register("end_pirate_anchor_winch_te", () -> BlockEntityType.Builder.create(TileEntityEndPirateAnchorWinch::new, AMBlockRegistry.END_PIRATE_ANCHOR_WINCH.get()).build(null));
//    public static final RegistrySupplier<BlockEntityType<TileEntityEndPirateShipWheel>> END_PIRATE_SHIP_WHEEL = null;// DEF_REG.register("end_pirate_ship_wheel_te", () -> BlockEntityType.Builder.create(TileEntityEndPirateShipWheel::new, AMBlockRegistry.END_PIRATE_SHIP_WHEEL.get()).build(null));
//    public static final RegistrySupplier<BlockEntityType<TileEntityEndPirateFlag>> END_PIRATE_FLAG = null;// DEF_REG.register("end_pirate_flag_te", () -> BlockEntityType.Builder.create(TileEntityEndPirateFlag::new, AMBlockRegistry.END_PIRATE_FLAG.get()).build(null));

}
