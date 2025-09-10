package com.github.alexthe666.alexsmobs.mixin;

import com.github.alexthe666.alexsmobs.entity.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(
        method = "handleFallDamage",
        at = @At("HEAD"),
        cancellable = true
    )
    private void handleFallDamage(
            float fallDistance,
            float damageMultiplier,
            DamageSource damageSource,
            CallbackInfoReturnable<Boolean> cir
    ) {
        var self = (Entity)(Object)this;
        if (self instanceof EntityBaldEagle) {
            cir.cancel();
        }

        if (self instanceof EntityBlueJay) {
            cir.cancel();
        }

        if (self instanceof EntityBunfungus) {
            cir.cancel();
        }

        if (self instanceof EntityCosmicCod) {
            cir.cancel();
        }

        if (self instanceof EntityCrimsonMosquito) {
            cir.cancel();
        }

        if (self instanceof EntityCapuchinMonkey) {
            cir.cancel();
        }

        if (self instanceof EntityCrow) {
            cir.cancel();
        }

        if (self instanceof EntityEndergrade) {
            cir.cancel();
        }

        if (self instanceof EntityEnderiophage) {
            cir.cancel();
        }

        if (self instanceof EntityFarseer) {
            cir.cancel();
        }

        if (self instanceof EntityFly) {
            cir.cancel();
        }

        if (self instanceof EntityFlyingFish) {
            cir.cancel();
        }

        if (self instanceof EntityFroststalker) {
            cir.cancel();
        }

        if (self instanceof EntityGuster) {
            cir.cancel();
        }

        if (self instanceof EntityHummingbird) {
            cir.cancel();
        }

        if (self instanceof EntityJerboa) {
            cir.cancel();
        }

        if (self instanceof EntityLaviathan) {
            cir.cancel();
        }

        if (self instanceof EntityMurmurHead) {
            cir.cancel();
        }

        if (self instanceof EntityRoadrunner) {
            cir.cancel();
        }

        if (self instanceof EntitySnowLeopard) {
            cir.cancel();
        }

        if (self instanceof EntitySoulVulture) {
            cir.cancel();
        }

        if (self instanceof EntityTarantulaHawk) {
            cir.cancel();
        }

        if (self instanceof EntityToucan) {
            cir.cancel();
        }

        if (self instanceof EntityUnderminer) {
            cir.cancel();
        }

        if (self instanceof EntityWarpedMosco) {
            cir.cancel();
        }

        if (self instanceof EntityWarpedToad) {
            cir.cancel();
        }
    }

    @Inject(
            method = "adjustMovementForCollisions",
            at = @At("HEAD"),
            cancellable = true
    )
    private void adjustMovementForCollisions(
            Vec3d movement,
            CallbackInfoReturnable<Vec3d> cir
    ) {
        var self = (Entity)(Object)this;
        if (self instanceof EntityGiantSquid entityGiantSquid) {
            if (entityGiantSquid.isRegionUnloaded() || !entityGiantSquid.isInsideWaterOrBubbleColumn()) {
                // call original
                return;
            }

            var aabb = entityGiantSquid.mantleCollisionPart.getBoundingBox();
            List<VoxelShape> list = entityGiantSquid.getWorld().getEntityCollisions(entityGiantSquid, aabb.stretch(movement));
            var vec3 = movement.lengthSquared() == 0.0D ? movement : Entity.adjustMovementForCollisions(entityGiantSquid, movement, aabb, entityGiantSquid.getWorld(), list);
            boolean flag = movement.x != vec3.x;
            boolean flag1 = movement.y != vec3.y;
            boolean flag2 = movement.z != vec3.z;
            boolean flag3 = entityGiantSquid.isOnGround() || flag1 && movement.y < 0.0D;
            if (entityGiantSquid.getStepHeight() > 0.0F && flag3 && (flag || flag2)) {
                var vec31 = Entity.adjustMovementForCollisions(entityGiantSquid, new Vec3d(movement.x, entityGiantSquid.getStepHeight(), movement.z), aabb, entityGiantSquid.getWorld(), list);
                var vec32 = Entity.adjustMovementForCollisions(entityGiantSquid, new Vec3d(0.0D, entityGiantSquid.getStepHeight(), 0.0D), aabb.expand(movement.x, 0.0D, movement.z), entityGiantSquid.getWorld(), list);
                if (vec32.y < (double) entityGiantSquid.getStepHeight()) {
                    var vec33 = Entity.adjustMovementForCollisions(entityGiantSquid, new Vec3d(movement.x, 0.0D, movement.z), aabb.offset(vec32), entityGiantSquid.getWorld(), list).add(vec32);
                    if (vec33.horizontalLengthSquared() > vec31.horizontalLengthSquared()) {
                        vec31 = vec33;
                    }
                }

                if (vec31.horizontalLengthSquared() > vec3.horizontalLengthSquared()) {
                    cir.setReturnValue(vec31.add(Entity.adjustMovementForCollisions(entityGiantSquid, new Vec3d(0.0D, -vec31.y + movement.y, 0.0D), aabb.offset(vec31), entityGiantSquid.getWorld(), list)));
                    return;
                }
            }

            cir.setReturnValue(vec3);

            return;
        }
    }
}
