package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.entity.ai.AnimalAISwimBottom;
import com.github.alexthe666.alexsmobs.entity.ai.AquaticMoveController;
import com.github.alexthe666.alexsmobs.entity.ai.SemiAquaticPathNavigator;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MoveIntoWaterGoal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Predicate;

public class EntitySeaBear extends WaterCreatureEntity implements IAnimatedEntity {

    public static final Animation ANIMATION_ATTACK = Animation.create(17);
    public static final Animation ANIMATION_POINT = Animation.create(25);
    public float prevOnLandProgress;
    public float onLandProgress;
    public int circleCooldown = 0;
    private int animationTick;
    private Animation currentAnimation;
    private BlockPos lastCircle = null;
    public static final Predicate<LivingEntity> SOMBRERO = (player) -> player.getEquippedStack(EquipmentSlot.HEAD).isOf(AMItemRegistry.SOMBRERO.get());

    public EntitySeaBear(EntityType<? extends EntitySeaBear> entityType, World level) {
        super(entityType, level);
        this.moveControl = new AquaticMoveController(this, 1F, 10);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceToClosestPlayer) {
        return !cannotDespawn();
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.hasCustomName();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 200.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.325F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    public static boolean isMobSafe(Entity entity) {
        if(entity instanceof PlayerEntity && ((PlayerEntity) entity).isCreative()){
            return true;
        }
        BlockState state = entity.getWorld().getBlockState(entity.getBlockPos().down());
        return state.isOf(AMBlockRegistry.SAND_CIRCLE.get()) || state.isOf(AMBlockRegistry.RED_SAND_CIRCLE.get());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.GRIZZLY_BEAR_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.GRIZZLY_BEAR_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.GRIZZLY_BEAR_DIE.get();
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(1, new MoveIntoWaterGoal(this));
        this.goalSelector.add(2, new AttackAI());
        this.goalSelector.add(3, new AvoidCircleAI());
        this.goalSelector.add(4, new AnimalAISwimBottom(this, 1F, 7){

            @Override
            public boolean canStart() {
                return super.canStart() && EntitySeaBear.this.getAnimation() == NO_ANIMATION;
            }

            @Override
            public boolean shouldContinue() {
                return super.shouldContinue() && EntitySeaBear.this.getAnimation() == NO_ANIMATION;
            }
        });
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, LivingEntity.class, false, SOMBRERO));
    }

    @Override
    public void tick() {
        super.tick();
        this.prevOnLandProgress = onLandProgress;

        if (this.isTouchingWater()) {
            if (onLandProgress > 0F)
                onLandProgress--;
        } else {
            if (onLandProgress < 5F)
                onLandProgress++;
        }

        if (this.isOnGround() && !this.isTouchingWater()) {
            this.setVelocity(this.getVelocity().add((this.random.nextFloat() * 2.0F - 1.0F) * 0.2F, 0.5D, (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F));
            this.setYaw(this.random.nextFloat() * 360.0F);
            this.setOnGround(false);
            this.velocityDirty = true;
        }
        if (circleCooldown > 0) {
            circleCooldown--;
            this.setTarget(null);
            this.setAttacker(null);
        }
        if(this.getAnimation() == ANIMATION_POINT){
            this.bodyYaw = this.getHeadYaw();
            this.field_6215 = this.getHeadYaw();
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new SemiAquaticPathNavigator(this, worldIn);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean collidesWith(Entity e) {
        return !isMobSafe(e);
    }

    @Override
    public void travel(Vec3d travelVector) {
        if(this.getAnimation() == ANIMATION_POINT){
            super.travel(Vec3d.ZERO);
        }else {
            if (this.canMoveVoluntarily() && this.isTouchingWater()) {
                this.updateVelocity(this.getMovementSpeed(), travelVector);
                this.move(MovementType.SELF, this.getVelocity());
                this.setVelocity(this.getVelocity().multiply(0.9D));
                if (this.getTarget() == null) {
                    this.setVelocity(this.getVelocity().add(0.0D, -0.005D, 0.0D));
                }
            } else {
                super.travel(travelVector);
            }
        }
    }

    @Override
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        currentAnimation = animation;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_POINT, ANIMATION_ATTACK};
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int tick) {
        animationTick = tick;
    }

    @Override
    public void setTarget(@Nullable LivingEntity entity) {
        if (entity == null || !isMobSafe(entity)) {
            super.setTarget(entity);
        }
    }

    @Override
    public void pushAway(Entity entity) {
        if (!isMobSafe(entity)) {
            super.pushAway(entity);
        }
    }

    private class AttackAI extends Goal {

        public AttackAI() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return EntitySeaBear.this.getTarget() != null && EntitySeaBear.this.getTarget().isInsideWaterOrBubbleColumn() && EntitySeaBear.this.getTarget().isAlive() && (EntitySeaBear.this.circleCooldown == 0 || EntitySeaBear.this.getAnimation() == ANIMATION_POINT);
        }

        @Override
        public void tick() {
            LivingEntity enemy = EntitySeaBear.this.getTarget();
            if(EntitySeaBear.this.getAnimation() == ANIMATION_POINT){
                EntitySeaBear.this.getNavigation().stop();
                EntitySeaBear.this.setVelocity(EntitySeaBear.this.getVelocity().multiply(0, 1, 0));
                EntitySeaBear.this.lookAtEntity(enemy, 360, 50);
            }else if (isMobSafe(enemy) && EntitySeaBear.this.distanceTo(enemy) < 6) {
                EntitySeaBear.this.circleCooldown = 100 + random.nextInt(100);
                EntitySeaBear.this.setAnimation(ANIMATION_POINT);
                EntitySeaBear.this.lookAtEntity(enemy, 360, 50);
                EntitySeaBear.this.lastCircle = enemy.getBlockPos();
            } else {
                EntitySeaBear.this.getNavigation().startMovingTo(enemy.getX(), enemy.getBodyY(0.5F), enemy.getZ(), 1.6D);
                if (EntitySeaBear.this.canSee(enemy) && EntitySeaBear.this.distanceTo(enemy) < 3.5F) {
                    EntitySeaBear.this.setAnimation(ANIMATION_ATTACK);
                    if (EntitySeaBear.this.getAnimationTick() % 5 == 0) {
                        enemy.damage(EntitySeaBear.this.getDamageSources().mobAttack(EntitySeaBear.this), 6);
                    }
                }
            }
        }
    }

    private class AvoidCircleAI extends Goal {
        private Vec3d target = null;

        public AvoidCircleAI() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return EntitySeaBear.this.circleCooldown > 0 && EntitySeaBear.this.lastCircle != null && EntitySeaBear.this.getAnimation() != ANIMATION_POINT;
        }

        @Override
        public void tick() {
            BlockPos pos = EntitySeaBear.this.lastCircle;
            if (target == null || EntitySeaBear.this.squaredDistanceTo(target) < 2 || !EntitySeaBear.this.getWorld().getFluidState(AMBlockPos.fromVec3(target).up()).isIn(FluidTags.WATER)) {
                target = NoPenaltyTargeting.findFrom(EntitySeaBear.this, 20, 7, Vec3d.ofCenter(pos));
            }
            if (target != null && EntitySeaBear.this.getWorld().getFluidState(AMBlockPos.fromVec3(target).up()).isIn(FluidTags.WATER)) {
                EntitySeaBear.this.getNavigation().startMovingTo(target.x, target.y, target.z, 1.0D);
            }
        }
    }
}
