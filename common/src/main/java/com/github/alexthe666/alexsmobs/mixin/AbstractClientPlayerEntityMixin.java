package com.github.alexthe666.alexsmobs.mixin;

import com.github.alexthe666.alexsmobs.registry.AMEffectRegistry;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {

    @Inject(
            method = "getFovMultiplier",
            at = @At("RETURN"),
            cancellable = true
    )
    private void getFovMultiplier(CallbackInfoReturnable<Float> cir) {
        final var player = (AbstractClientPlayerEntity)(Object)this;
        if (player.hasStatusEffect(AMEffectRegistry.FEAR.get()) || player.hasStatusEffect(AMEffectRegistry.POWER_DOWN.get())) {
            cir.setReturnValue(1.0F);
        }
    }
}