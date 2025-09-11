package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMParticleRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;

public class EntityBunfungus extends PathAwareEntity implements IAnimatedEntity {

    public static final Animation ANIMATION_SLAM = Animation.create(20);
    public static final Animation ANIMATION_BELLY = Animation.create(10);
    public static final Animation ANIMATION_EAT = Animation.create(20);
    private static final TrackedData<Boolean> JUMP_ACTIVE = DataTracker.registerData(EntityBunfungus.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SLEEPING = DataTracker.registerData(EntityBunfungus.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BEGGING = DataTracker.registerData(EntityBunfungus.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> CARROTED = DataTracker.registerData(EntityBunfungus.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> TRANSFORMS_IN = DataTracker.registerData(EntityBunfungus.class, TrackedDataHandlerRegistry.INTEGER);
    public float jumpProgress;
    public float prevJumpProgress;
    public float reboundProgress;
    public float prevReboundProgress;
    public float sleepProgress;
    public float prevSleepProgress;
    public float interestedProgress;
    public float prevInterestedProgress;
    private int animationTick;
    private Animation currentAnimation;
    public int prevTransformTime;
    public static final int MAX_TRANSFORM_TIME = 50;

    public EntityBunfungus(EntityType t, World lvl) {
        super(t, lvl);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 80.0D).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.21F);
    }

    @Override
    public void playAmbientSound() {
        if(!this.isSleeping()){
            super.playAmbientSound();
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.BUNFUNGUS_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.BUNFUNGUS_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.BUNFUNGUS_HURT.get();
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceToClosestPlayer) {
        return false;
    }

    public static boolean canBunfungusSpawn(EntityType type, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        return worldIn.getBlockState(pos.down()).isOpaque();
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.mungusSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new GroundPathNavigatorWide(this, worldIn);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new BunfungusAIMelee(this));
        this.goalSelector.add(2, new BunfungusAIBeg(this, 1.0D));
        this.goalSelector.add(3, new AnimalAIWanderRanged(this, 60, 1.0D, 16, 7){
            @Override
            public boolean canStart(){
                return super.canStart() && EntityBunfungus.this.canUseComplexAI();
            }
        });
        this.goalSelector.add(4, new AnimalAILeapRandomly(this, 60, 7){
            @Override
            public boolean canStart(){
                return super.canStart() && EntityBunfungus.this.canUseComplexAI();
            }
        });
        this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F){
            @Override
            public boolean canStart(){
                return super.canStart() && EntityBunfungus.this.canUseComplexAI();
            }
        });
        this.goalSelector.add(10, new LookAroundGoal(this){
            @Override
            public boolean canStart(){
                return super.canStart() && EntityBunfungus.this.canUseComplexAI();
            }
        });
        this.targetSelector.add(2, new RevengeGoal(this));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MobEntity.class, 5, false, false, (mob) -> mob instanceof Monster && !(mob instanceof CreeperEntity) && !(mob.getGroup() == EntityGroup.AQUATIC && mob.isInsideWaterOrBubbleColumn()) && !mob.getType().isIn(AMTagRegistry.BUNFUNGUS_IGNORES)));
    }

    private boolean canUseComplexAI() {
        return !this.isRabbitForm() && !this.isSleeping();
    }

    @Override
    protected float getBaseMovementSpeedMultiplier() {
        return 0.98F;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(JUMP_ACTIVE, false);
        this.dataTracker.startTracking(SLEEPING, false);
        this.dataTracker.startTracking(BEGGING, false);
        this.dataTracker.startTracking(CARROTED, false);
        this.dataTracker.startTracking(TRANSFORMS_IN, 0);
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    public void tick() {
        super.tick();
        prevJumpProgress = jumpProgress;
        prevReboundProgress = reboundProgress;
        prevSleepProgress = sleepProgress;
        prevInterestedProgress = interestedProgress;
        prevTransformTime = this.transformsIn();

        if (!this.getWorld().isClient) {
            this.dataTracker.set(JUMP_ACTIVE, !this.isOnGround());
        }

        if (this.dataTracker.get(JUMP_ACTIVE) && !isInsideWaterOrBubbleColumn()) {
            if (jumpProgress < 5F) {
                jumpProgress += 0.5F;
                if (reboundProgress > 0) {
                    reboundProgress--;
                }
            }
            if (jumpProgress >= 5F) {
                if (reboundProgress < 5F) {
                    reboundProgress += 0.5F;
                }
            }
        } else {
            if (reboundProgress > 0) {
                reboundProgress = Math.max(reboundProgress - 1F, 0);
            }
            if (jumpProgress > 0) {
                jumpProgress = Math.max(jumpProgress - 1F, 0);
            }
        }

        if (this.isSleepingPose()) {
            if (sleepProgress < 5F)
                sleepProgress++;
        } else {
            if (sleepProgress > 0F)
                sleepProgress--;
        }

        if (this.isBegging()) {
            if (interestedProgress < 5F)
                interestedProgress++;
        } else {
            if (interestedProgress > 0F)
                interestedProgress--;
        }

        if (!this.getWorld().isClient) {
            final LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                if (this.isSleeping()) {
                    this.setSleeping(false);
                }
                final double dist = this.distanceTo(target);
                boolean flag = false;
                if (this.getAnimationTick() == 5) {
                    if (dist < 3.5D && this.getAnimation() == ANIMATION_BELLY) {
                        for (final LivingEntity entity : this.getWorld().getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox().expand(2.0D))) {
                            if ((entity == target || entity instanceof Monster) && !entity.getType().isIn(AMTagRegistry.BUNFUNGUS_IGNORE_AOE_ATTACKS)) {
                                flag = true;
                                launch(entity);
                                entity.damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue());
                            }
                        }
                    } else if (dist < 2.5D && this.getAnimation() == ANIMATION_SLAM) {
                        for (final LivingEntity entity : this.getWorld().getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox().expand(2.0D))) {
                            if ((entity == target || entity instanceof Monster) && !entity.getType().isIn(AMTagRegistry.BUNFUNGUS_IGNORE_AOE_ATTACKS)) {
                                flag = true;
                                entity.takeKnockback(0.2F, entity.getX() - this.getX(), entity.getZ() - this.getZ());
                                entity.damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue());
                            }
                        }
                    }
                }
                if (flag) {
                    this.playSound(AMSoundRegistry.BUNFUNGUS_ATTACK.get(), this.getSoundVolume(), this.getSoundPitch());
                }
            }
            if (this.age % 40 == 0) {
                this.heal(1);
            }
        }
        if (this.getAnimation() == NO_ANIMATION && this.isCarrot(this.getStackInHand(Hand.MAIN_HAND))) {
            this.setAnimation(ANIMATION_EAT);
        }
        if (this.getAnimation() == ANIMATION_EAT) {
            if (this.getAnimationTick() % 4 == 0) {
                this.emitGameEvent(GameEvent.EAT);
                this.playSound(SoundEvents.ENTITY_GENERIC_EAT, this.getSoundVolume(), this.getSoundPitch());
            }
            if (this.getAnimationTick() >= 18) {
                var stack = this.getStackInHand(Hand.MAIN_HAND);
                if (!stack.isEmpty()) {
                    stack.decrement(1);
                    this.setCarroted(true);
                    this.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 1000));
                    this.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 1000, 1));
                    this.heal(8);
                }
            } else {
                for (int i = 0; i < 3; i++) {
                    final double d2 = this.random.nextGaussian() * 0.02D;
                    final double d0 = this.random.nextGaussian() * 0.02D;
                    final double d1 = this.random.nextGaussian() * 0.02D;
                    this.getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, this.getStackInHand(Hand.MAIN_HAND)), this.getX() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, this.getY() + this.getHeight() * 0.5F + (double) (this.random.nextFloat() * this.getHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, d0, d1, d2);
                }
            }
        }
        if (!this.getWorld().isClient) {
            if (this.transformsIn() > 0) {
                this.setTransformsIn(this.transformsIn() - 1);
            }
        }

        if (this.getWorld().isClient) {
            if (isRabbitForm()){
                for (int i = 0; i < 3; i++) {
                    final double d2 = this.random.nextGaussian() * 0.02D;
                    final double d0 = this.random.nextGaussian() * 0.02D;
                    final double d1 = this.random.nextGaussian() * 0.02D;
                    final float f1 = (EntityBunfungus.MAX_TRANSFORM_TIME - this.transformsIn()) / (float)EntityBunfungus.MAX_TRANSFORM_TIME;
                    final float scale = f1 * 0.5F + 0.15F;
                    this.getWorld().addParticle(AMParticleRegistry.BUNFUNGUS_TRANSFORMATION.get(), this.getParticleX(scale), this.getBodyY(this.random.nextDouble() * scale), this.getParticleZ(scale), d0, d1, d2);
                }
            }

            if (isSleeping() && random.nextFloat() < 0.3F) {
                final double d0 = this.random.nextGaussian() * 0.02D;
                final float radius = this.getWidth() * (0.7F + random.nextFloat() * 0.1F);
                final float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
                final double extraX = radius * MathHelper.sin(MathHelper.PI + angle) + random.nextFloat() * 0.5F - 0.25F;
                final double extraZ = radius * MathHelper.cos(angle) + random.nextFloat() * 0.5F - 0.25F;
                var data = random.nextFloat() < 0.3F ? AMParticleRegistry.BUNFUNGUS_TRANSFORMATION.get() : AMParticleRegistry.FUNGUS_BUBBLE.get();
                this.getWorld().addParticle(data, this.getX() + extraX, this.getY() + random.nextFloat() * 0.1F, this.getZ() + extraZ, 0, d0, 0);
            }
        } else {
            if (this.getWorld().isDay() && this.getTarget() == null && !this.isBegging() && !this.isInsideWaterOrBubbleColumn()) {
                if (this.age % 10 == 0 && this.getRandom().nextInt(300) == 0) {
                    this.setSleeping(true);
                }
            } else if (this.isSleeping()) {
                this.setSleeping(false);
            }
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    private void launch(LivingEntity target) {
        if (target.isOnGround()) {
            final double d0 = target.getX() - this.getX();
            final double d1 = target.getZ() - this.getZ();
            final double d2 = Math.max(d0 * d0 + d1 * d1, 0.001D);
            final float f = 6 + random.nextFloat() * 2;
            target.addVelocity(d0 / d2 * f, 0.6F + random.nextFloat() * 0.7F, d1 / d2 * f);
        }
    }

    @Override
    public boolean isSleeping() {
        return this.dataTracker.get(SLEEPING);
    }

    public void setSleeping(boolean sleeping) {
        this.dataTracker.set(SLEEPING, sleeping);
    }

    public boolean isSleepingPose() {
        return this.isSleeping() || this.getAnimation() == ANIMATION_SLAM && this.getAnimationTick() < 10;
    }

    public boolean isCarroted() {
        return this.dataTracker.get(CARROTED);
    }

    public void setCarroted(boolean head) {
        this.dataTracker.set(CARROTED, head);
    }

    public boolean isBegging() {
        return this.dataTracker.get(BEGGING) && this.getAnimation() != ANIMATION_EAT;
    }

    public void setBegging(boolean begging) {
        this.dataTracker.set(BEGGING, begging);
    }

    public int transformsIn() {
        return Math.min(this.dataTracker.get(TRANSFORMS_IN), MAX_TRANSFORM_TIME);
    }

    public boolean isRabbitForm() {
        return this.transformsIn() > 0;
    }

    public void setTransformsIn(int time) {
        this.dataTracker.set(TRANSFORMS_IN, time);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        final var itemstack = player.getStackInHand(hand);
        final var type = super.interactMob(player, hand);
        final var actionResult = itemstack.useOnEntity(player, this, hand);
        if (actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS && this.getStackInHand(Hand.MAIN_HAND).isEmpty()) {
            if (isCarrot(itemstack) && this.getMainHandStack().isEmpty()) {
                ItemStack cop = itemstack.copy();
                cop.setCount(1);
                this.setStackInHand(Hand.MAIN_HAND, cop);
                if(!player.isCreative()){
                    itemstack.decrement(1);
                }
            }
        }
        return type;
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (!this.isRabbitForm() && !this.isSleeping()) {
            super.travel(travelVector);
        }else{
            super.travel(Vec3d.ZERO);
        }
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
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        currentAnimation = animation;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_EAT, ANIMATION_BELLY, ANIMATION_SLAM};
    }

    public boolean isCarrot(ItemStack stack) {
        return stack.isIn(AMTagRegistry.BUNFUNGUS_FOODSTUFFS);
    }

    public boolean defendsMungusAgainst(LivingEntity lastHurtByMob) {
        return !(lastHurtByMob instanceof PlayerEntity) || this.isCarroted();
    }

    public void onJump() {
        //sound was too annoying
        //this.playSound(AMSoundRegistry.BUNFUNGUS_JUMP, this.getSoundVolume(), this.getSoundPitch());
    }
}
