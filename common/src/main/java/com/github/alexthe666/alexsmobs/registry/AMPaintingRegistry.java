package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.registry.RegistryKeys;

public class AMPaintingRegistry {
    public static final DeferredRegister<PaintingVariant> DEF_REG = DeferredRegister.create(AlexsMobs.MOD_ID, RegistryKeys.PAINTING_VARIANT);

    public static final RegistrySupplier<PaintingVariant> NFT = DEF_REG.register("nft", () -> new PaintingVariant(32, 32));
    public static final RegistrySupplier<PaintingVariant> DOG_POKER = DEF_REG.register("dog_poker", () -> new PaintingVariant(32, 16));
}
