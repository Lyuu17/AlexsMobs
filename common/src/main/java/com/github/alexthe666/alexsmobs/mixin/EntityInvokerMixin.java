package com.github.alexthe666.alexsmobs.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityInvokerMixin {

    @Invoker("adjustMovementForCollisions")
    Vec3d adjustMovementForCollisions(Vec3d movement);
}
