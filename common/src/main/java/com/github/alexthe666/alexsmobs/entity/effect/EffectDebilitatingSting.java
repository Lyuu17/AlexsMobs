package com.github.alexthe666.alexsmobs.entity.effect;

import com.github.alexthe666.alexsmobs.entity.EntityTarantulaHawk;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.ServerWorldAccess;

import java.util.function.Predicate;

public class EffectDebilitatingSting extends StatusEffect {

    private int lastDuration = -1;

    public EffectDebilitatingSting() {
        super(StatusEffectCategory.NEUTRAL, 0XFFF385);
        this.addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED, "7107DE5E-7CE8-4030-940E-514C1F160890", -1.0F, EntityAttributeModifier.Operation.MULTIPLY_BASE);
    }

    @Override
    public void onRemoved(LivingEntity entityLivingBaseIn, AttributeContainer attributeMapIn, int amplifier) {
        if (entityLivingBaseIn.getGroup() == EntityGroup.ARTHROPOD) {
            super.onRemoved(entityLivingBaseIn, attributeMapIn, amplifier);
        }
    }

    @Override
    public void onApplied(LivingEntity entityLivingBaseIn, AttributeContainer attributeMapIn, int amplifier) {
        if (entityLivingBaseIn.getGroup() == EntityGroup.ARTHROPOD) {
            super.onApplied(entityLivingBaseIn, attributeMapIn, amplifier);
        }
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (entity.getGroup() != EntityGroup.ARTHROPOD) {
            if (entity.getHealth() > entity.getMaxHealth() * 0.5F) {
                entity.damage(entity.getDamageSources().magic(), 1.0F);
            }
        } else {
            boolean suf = isEntityInsideOpaqueBlock(entity);
            if (suf) {
                entity.setVelocity(Vec3d.ZERO);
                entity.noClip = true;
            }
            entity.setNoGravity(suf);
            entity.setJumping(false);
            if (!entity.hasVehicle() && entity instanceof MobEntity && !(((MobEntity) entity).getMoveControl().getClass() == MoveControl.class)) {
                entity.setVelocity(new Vec3d(0, -1, 0));
            }
            if (lastDuration == 1) {
                entity.damage(entity.getDamageSources().magic(), (amplifier + 1) * 30);
                if (amplifier > 0) {
                    BlockPos surface = entity.getBlockPos();
                    while (!entity.getWorld().isAir(surface) && surface.getY() < 256) {
                        surface = surface.up();
                    }
                    EntityTarantulaHawk baby = AMEntityRegistry.TARANTULA_HAWK.get().create(entity.getWorld());
                    baby.setBaby(true);
                    baby.setPos(entity.getX(), surface.getY() + 0.1F, entity.getZ());
                    if (!entity.getWorld().isClient) {
                        baby.initialize((ServerWorldAccess) entity.getWorld(), entity.getWorld().getLocalDifficulty(entity.getBlockPos()), SpawnReason.BREEDING, null, null);
                        entity.getWorld().spawnEntity(baby);
                    }
                }
                entity.setNoGravity(false);
                entity.noClip = false;
            }
        }
    }

    public boolean isEntityInsideOpaqueBlock(Entity entity) {
        var vec3 = entity.getEyePos();
        float f = entity.getDimensions(entity.getPose()).width * 0.8F;
        var axisalignedbb = Box.of(vec3, f, 1.0E-6D, f);
        return entity.getWorld().getStatesInBox(axisalignedbb).filter(Predicate.not(AbstractBlock.AbstractBlockState::isAir)).anyMatch((p_185969_) -> {
            BlockPos blockpos = AMBlockPos.fromVec3(vec3);
            return p_185969_.shouldSuffocate(entity.getWorld(), blockpos) && VoxelShapes.matchesAnywhere(p_185969_.getCollisionShape(entity.getWorld(), blockpos).offset(vec3.x, vec3.y, vec3.z), VoxelShapes.cuboid(axisalignedbb), BooleanBiFunction.AND);
        });
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        lastDuration = duration;
        return duration > 0;
    }

    @Override
    public String getTranslationKey() {
        return "alexsmobs.potion.debilitating_sting";
    }
}
