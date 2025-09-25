package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.google.common.collect.ImmutableSet;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.Set;

public class AMPointOfInterestRegistry {

    public static final DeferredRegister<PointOfInterestType> DEF_REG = DeferredRegister.create(AlexsMobs.MOD_ID, RegistryKeys.POINT_OF_INTEREST_TYPE);
    public static final RegistrySupplier<PointOfInterestType> END_PORTAL_FRAME = DEF_REG.register("end_portal_frame", () ->new PointOfInterestType(getBlockStates(Blocks.END_PORTAL_FRAME), 32, 6));
    public static final RegistrySupplier<PointOfInterestType> LEAFCUTTER_ANT_HILL = DEF_REG.register("leafcutter_anthill", () ->new PointOfInterestType(getBlockStates(AMBlockRegistry.LEAFCUTTER_ANTHILL.get()), 32, 6));
    public static final RegistrySupplier<PointOfInterestType> BEACON = DEF_REG.register("am_beacon", () -> new PointOfInterestType(getBlockStates(Blocks.BEACON), 32, 6));
    public static final RegistrySupplier<PointOfInterestType> HUMMINGBIRD_FEEDER = DEF_REG.register("hummingbird_feeder", () -> new PointOfInterestType(getBlockStates(AMBlockRegistry.HUMMINGBIRD_FEEDER.get()), 32, 6));

    private static Set<BlockState> getBlockStates(Block block) {
        return ImmutableSet.copyOf(block.getStateManager().getStates());
    }

}
