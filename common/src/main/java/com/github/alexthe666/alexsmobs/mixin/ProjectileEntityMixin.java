package com.github.alexthe666.alexsmobs.mixin;

import com.github.alexthe666.alexsmobs.entity.EntityEmu;
import com.github.alexthe666.alexsmobs.registry.AMAdvancementTriggerRegistry;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileEntity.class)
public class ProjectileEntityMixin {

    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void onCollision(HitResult hitResult, CallbackInfo ci) {
        final var self = (ProjectileEntity) (Object)this;

        if (hitResult instanceof EntityHitResult hitResult1
                && hitResult1.getEntity() instanceof EntityEmu emu && !self.getWorld().isClient) {
            if (self instanceof PersistentProjectileEntity arrow) {
                //fixes soft crash with vanilla
                arrow.setPierceLevel((byte) 0);
            }
            if ((emu.getAnimation() == EntityEmu.ANIMATION_DODGE_RIGHT || emu.getAnimation() == EntityEmu.ANIMATION_DODGE_LEFT) && emu.getAnimationTick() < 7) {
                ci.cancel();
            }
            if (emu.getAnimation() != EntityEmu.ANIMATION_DODGE_RIGHT && emu.getAnimation() != EntityEmu.ANIMATION_DODGE_LEFT) {
                boolean left = true;
                var arrowPos = self.getPos();
                var rightVector = emu.getRotationVector().rotateY(0.5F * MathHelper.PI).add(emu.getPos());
                var leftVector = emu.getRotationVector().rotateY(-0.5F * MathHelper.PI).add(emu.getPos());
                if (arrowPos.distanceTo(rightVector) < arrowPos.distanceTo(leftVector)) {
                    left = false;
                } else if (arrowPos.distanceTo(rightVector) > arrowPos.distanceTo(leftVector)) {
                    left = true;
                } else {
                    left = emu.getRandom().nextBoolean();
                }
                var vector3d2 = self.getVelocity().rotateY((float) ((left ? -0.5F : 0.5F) * Math.PI)).normalize();
                emu.setAnimation(left ? EntityEmu.ANIMATION_DODGE_LEFT : EntityEmu.ANIMATION_DODGE_RIGHT);
                emu.velocityDirty = true;
                if (!emu.horizontalCollision) {
                    emu.move(MovementType.SELF, new Vec3d(vector3d2.x * 0.25F, 0.1F, vector3d2.z * 0.25F));
                }
                if (!self.getWorld().isClient) {
                    if (self.getOwner() instanceof ServerPlayerEntity serverPlayer) {
                        AMAdvancementTriggerRegistry.EMU_DODGE.trigger(serverPlayer);
                    }
                }
                emu.setVelocity(emu.getVelocity().add(vector3d2.x * 0.5F, 0.32F, vector3d2.z * 0.5F));
                ci.cancel();
            }
        }
    }
}