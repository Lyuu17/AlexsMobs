package com.github.alexthe666.alexsmobs.entity.effect;

import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class EffectClinging extends StatusEffect {

    public EffectClinging() {
        super(StatusEffectCategory.BENEFICIAL, 0XBD4B4B);
    }

    private static BlockPos getPositionUnderneath(Entity e) {
        return AMBlockPos.fromCoords(e.getX(), e.getBoundingBox().maxY + 1.51F, e.getZ());
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        entity.calculateDimensions();
        entity.setNoGravity(false);

        if (isUpsideDown(entity)) {
            entity.fallDistance = 0;
            if (!entity.isSneaking()) {
                if (!entity.horizontalCollision) {
                    entity.setVelocity(entity.getVelocity().add(0, 0.3F, 0));
                }
                entity.setVelocity(entity.getVelocity().multiply(0.998F, 1F, 0.998F));
            }
        }
    }

    public static boolean isUpsideDown(LivingEntity entity){
        var pos = getPositionUnderneath(entity);
        var ground = entity.getWorld().getBlockState(pos);
        return (entity.verticalCollision || ground.isSideSolidFullSquare(entity.getWorld(), pos, Direction.DOWN)) && !entity.isOnGround();
    }

    @Override
    public void onRemoved(LivingEntity entityLivingBaseIn, AttributeContainer attributeMapIn, int amplifier) {
        super.onRemoved(entityLivingBaseIn, attributeMapIn, amplifier);
        entityLivingBaseIn.calculateDimensions();
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration > 0;
    }

    @Override
    public String getTranslationKey() {
        return "alexsmobs.potion.clinging";
    }

}