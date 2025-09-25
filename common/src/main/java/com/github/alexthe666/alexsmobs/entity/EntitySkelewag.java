package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIRandomSwimming;
import com.github.alexthe666.alexsmobs.entity.ai.AquaticMoveController;
import com.github.alexthe666.alexsmobs.entity.ai.EntityAINearestTarget3D;
import com.github.alexthe666.alexsmobs.entity.ai.SemiAquaticPathNavigator;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MoveIntoWaterGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import org.jetbrains.annotations.Nullable;

public class EntitySkelewag extends HostileEntity implements IAnimatedEntity {

    public static final Animation ANIMATION_STAB = Animation.create(10);
    public static final Animation ANIMATION_SLASH = Animation.create(25);
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(EntitySkelewag.class, TrackedDataHandlerRegistry.INTEGER);
    private int animationTick;
    private Animation currentAnimation;
    public float prevOnLandProgress;
    public float onLandProgress;

    public EntitySkelewag(EntityType<EntitySkelewag> monster, World level) {
        super(monster, level);
        this.experiencePoints = 10;
        this.moveControl = new AquaticMoveController(this, 1.0F, 15F);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 0.0F);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new SemiAquaticPathNavigator(this, worldIn);
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.UNDEAD;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.skelewagSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean canSkelewagSpawn(EntityType<EntitySkelewag> type, ServerWorldAccess levelAccessor, SpawnReason p_32352_, BlockPos below, Random random) {
        if (!levelAccessor.getFluidState(below.down()).isIn(FluidTags.WATER)) {
            return false;
        } else {
            return levelAccessor.getDifficulty() != Difficulty.PEACEFUL && isSpawnDark(levelAccessor, below, random) && (p_32352_ == SpawnReason.SPAWNER || random.nextInt(40) == 0 && levelAccessor.getFluidState(below).isIn(FluidTags.WATER));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.SKELEWAG_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.SKELEWAG_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.SKELEWAG_HURT.get();
    }

    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView level) {
        return getWorld().getFluidState(pos).isIn(FluidTags.WATER) ? 10.0F + level.getBrightness(pos) - 0.5F : super.getPathfindingFavor(pos, getWorld());
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new MoveIntoWaterGoal(this));
        this.goalSelector.add(2, new AttackGoal(this));
        this.goalSelector.add(3, new AnimalAIRandomSwimming(this, 1F, 12, 5));
        this.targetSelector.add(1, new RevengeGoal(this, DrownedEntity.class, EntitySkelewag.class));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new EntityAINearestTarget3D<>(this, DolphinEntity.class, true));

    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VARIANT, 0);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.45D)
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    public int getLimitPerChunk() {
        return 1;
    }

    @Override
    public void tick(){
        super.tick();
        this.prevOnLandProgress = onLandProgress;
        boolean onLand = !this.isInsideWaterOrBubbleColumn() && this.isOnGround();
        if (onLand && onLandProgress < 5F) {
            onLandProgress++;
        }
        if (!onLand && onLandProgress > 0F) {
            onLandProgress--;
        }
        float targetXRot = 0;
        if(this.getVelocity().length() > 0.09){
            targetXRot = -((float)(MathHelper.atan2(this.getVelocity().y, this.getVelocity().horizontalLength()) * (double)MathHelper.DEGREES_PER_RADIAN));
        }
        if(targetXRot < this.getPitch() - 5){
            targetXRot = this.getPitch() - 5;
        }
        if(targetXRot > this.getPitch() + 5){
            targetXRot = this.getPitch() + 5;
        }
        this.setPitch(targetXRot);
        if (!this.getWorld().isClient && this.getTarget() != null && this.distanceTo(this.getTarget()) < 2.0F + this.getTarget().getWidth()) {
            this.lookAtEntity(this.getTarget(), 350, 200);
            if(this.getAnimation() == ANIMATION_STAB && this.getAnimationTick() == 7 && this.canSee(this.getTarget())){
                float f1 = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
                this.setVelocity(this.getVelocity().add(-MathHelper.sin(f1) * 0.02F, 0.0D, MathHelper.cos(f1) * 0.02F));
                getTarget().takeKnockback(1F, getTarget().getX() - this.getX(), getTarget().getZ() - this.getZ());
                this.getTarget().damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue());
            }
            if(this.getAnimation() == ANIMATION_SLASH && this.getAnimationTick() % 5 == 0 && this.getAnimationTick() > 0 && this.getAnimationTick() < 25 && this.canSee(this.getTarget())){
                for (var entity : this.getWorld().getNonSpectatingEntities(LivingEntity.class, this.getTarget().getBoundingBox().expand(2.0D))) {
                    if (!entity.isConnectedThroughVehicle(this) && entity != this && !entity.isTeammate(this)) {
                        entity.damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue() * 0.5F);
                    }
                }
            }
        }
        if(onLandProgress >= 5.0F && this.hasPassengers()){
            this.removeAllPassengers();
        }
        if(!isInsideWaterOrBubbleColumn()){
            if (this.isOnGround() && random.nextFloat() < 0.2F) {
                this.setVelocity(this.getVelocity().add((this.random.nextFloat() * 2.0F - 1.0F) * 0.2F, 0.5D, (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F));
                this.setYaw(this.random.nextFloat() * 360.0F);
                this.playSound(AMSoundRegistry.SKELEWAG_HURT.get(), this.getSoundVolume(), this.getSoundPitch());
            }
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int command) {
        this.dataTracker.set(VARIANT, command);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putInt("Variant", this.getVariant());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setVariant(compound.getInt("Variant"));
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Override
    public boolean canSpawn(WorldView worldIn) {
        return worldIn.doesNotIntersectEntities(this);
    }

    @Override
    public void travel(Vec3d travelVector) {
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

    @Override
    public void updatePassengerPosition(Entity passenger, Entity.PositionUpdater moveFunc) {
        if (this.hasPassenger(passenger)) {
            passenger.setBodyYaw(this.bodyYaw);
            var vec = new Vec3d(0, this.getHeight() * 0.4F, this.getWidth() * -0.2F).rotateX(-this.getPitch() * MathHelper.RADIANS_PER_DEGREE).rotateY(-this.getYaw() * MathHelper.RADIANS_PER_DEGREE);
            passenger.setPos(this.getX() + vec.x, this.getY() + vec.y + passenger.getHeightOffset(), this.getZ() + vec.z);
        }
    }

    //FIXME forge
//    @Override
//    public boolean canBeRiddenUnderFluidType(FluidType type, Entity rider) {
//        return true;
//    }

    @Nullable
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.setVariant(this.getRandom().nextFloat() < 0.3F ? 1 : 0);
        if (this.random.nextFloat() < 0.2F) {
            var drowned = EntityType.DROWNED.create(getWorld());
            drowned.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
            drowned.copyPositionAndRotation(this);
            drowned.startRiding(this);
            worldIn.spawnEntityAndPassengers(drowned);
        }
        if(reason == SpawnReason.STRUCTURE){
            this.setPositionTarget(this.getBlockPos(), 15);
        }
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    public boolean canBreatheInWater() {
        return true;
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int i) {
        animationTick = i;
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
        return new Animation[]{ANIMATION_SLASH, ANIMATION_STAB};
    }

    private class AttackGoal extends Goal {
        private final EntitySkelewag fish;
        private boolean isCharging = false;

        public AttackGoal(EntitySkelewag skelewag) {
            this.fish = skelewag;
        }

        @Override
        public boolean canStart() {
            return this.fish.getTarget() != null;
        }

        @Override
        public void tick(){
            LivingEntity target = this.fish.getTarget();
            if(target != null){
                double dist = this.fish.distanceTo(target);
                if(dist > 5){
                    isCharging = true;
                }
                this.fish.getNavigation().startMovingTo(target, isCharging ? 1.3F : 0.8F);
                if(dist < 2.0F + 3.0F + target.getWidth() / 2){
                    this.fish.setAnimation(isCharging ? ANIMATION_STAB : random.nextBoolean() ? ANIMATION_SLASH : ANIMATION_STAB);
                    isCharging = false;
                }
            }
        }

        @Override
        public void stop(){
            isCharging = false;
        }
    }
}
