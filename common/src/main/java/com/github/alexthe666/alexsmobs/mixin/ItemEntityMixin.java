package com.github.alexthe666.alexsmobs.mixin;

import com.github.alexthe666.alexsmobs.item.IItemEntity;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    private void tick(CallbackInfo ci) {
        var self = (ItemEntity)(Object)this;
        if (self.getStack().getItem() instanceof IItemEntity itemEntity) {
            itemEntity.onEntityItemUpdate(self);
        }
    }
}
