package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EntityCombJelly extends WaterCreatureEntity implements Bucketable {

    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(EntityCombJelly.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> JELLYPITCH = DataTracker.registerData(EntityCombJelly.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> FROM_BUCKET = DataTracker.registerData(EntityCombJelly.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> JELLY_SCALE = DataTracker.registerData(EntityCombJelly.class, TrackedDataHandlerRegistry.FLOAT);
    public float prevOnLandProgress;
    public float onLandProgress;
    private BlockPos moveTarget;
    public float prevjellyPitch = 0;
    public float spin;
    public float prevSpin;

    public EntityCombJelly(EntityType<? extends WaterCreatureEntity> animal, World level) {
        super(animal, level);
    }

    public int getLimitPerChunk() {
        return 4;
    }

    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.hasCustomName() || this.isFromBucket();
    }

    public boolean canImmediatelyDespawn(double p_213397_1_) {
        return !this.isFromBucket() && !this.hasCustomName();
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.terrapinSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean canCombJellySpawn(EntityType<EntityCombJelly> entityType, ServerWorldAccess iServerWorld, SpawnReason reason, BlockPos pos, Random random) {
        return reason == SpawnReason.SPAWNER || iServerWorld.isWater(pos) && iServerWorld.isWater(pos.up()) && isLightLevelOk(pos, iServerWorld);
    }

    private static boolean isLightLevelOk(BlockPos pos, ServerWorldAccess iServerWorld) {
        float time = iServerWorld.getSkyAngle(1.0F);
        int light = iServerWorld.getLightLevel(pos);
        return light <= 4 && time > 0.27F && time <= 0.8F;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VARIANT, 0);
        this.dataTracker.startTracking(JELLYPITCH, 0F);
        this.dataTracker.startTracking(FROM_BUCKET, false);
        this.dataTracker.startTracking(JELLY_SCALE, 1.0F);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.COMB_JELLY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.COMB_JELLY_HURT.get();
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, variant);
    }

    public float getJellyPitch() {
        return MathHelper.clamp(this.dataTracker.get(JELLYPITCH), -90, 90);
    }

    public void setJellyPitch(float pitch) {
        this.dataTracker.set(JELLYPITCH, MathHelper.clamp(pitch, -90, 90));
    }

    public float getJellyScale() {
        return this.dataTracker.get(JELLY_SCALE);
    }

    public void setJellyScale(float scale) {
        this.dataTracker.set(JELLY_SCALE, scale);
    }

    @Override
    public boolean isFromBucket() {
        return this.dataTracker.get(FROM_BUCKET);
    }

    @Override
    public void setFromBucket(boolean p_203706_1_) {
        this.dataTracker.set(FROM_BUCKET, p_203706_1_);
    }

    @Override
    @NotNull
    public SoundEvent getBucketFillSound() {
        return SoundEvents.ITEM_BUCKET_FILL_FISH;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 6.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D);
    }

    @Override
    @NotNull
    public ItemStack getBucketItem() {
        var stack = new ItemStack(AMItemRegistry.COMB_JELLY_BUCKET.get());
        if (this.hasCustomName()) {
            stack.setCustomName(this.getCustomName());
        }
        return stack;
    }

    @Override
    public void tick() {
        super.tick();
        this.prevOnLandProgress = onLandProgress;
        this.prevjellyPitch = this.getJellyPitch();
        this.prevSpin = this.spin;
        if (!this.isTouchingWater() && onLandProgress < 5F) {
            onLandProgress++;
        }
        if (this.isTouchingWater() && onLandProgress > 0F) {
            onLandProgress--;
        }

        if (!this.getWorld().isClient) {
            if (this.isTouchingWater()) {
                this.setNoGravity(true);
                if(moveTarget == null || this.random.nextInt(120) == 0 || this.squaredDistanceTo(moveTarget.getX() + 0.5F, moveTarget.getY() + 0.5F, moveTarget.getZ() + 0.5F) < 5 || age % 10 == 0 && !canBlockPosBeSeen(moveTarget)){
                    var randPos = this.getBlockPos().add(random.nextInt(10) - 5, random.nextInt(6) - 3, random.nextInt(10) - 5);
                    if(getWorld().getFluidState(randPos).isOf(Fluids.WATER) && getWorld().getFluidState(randPos.up()).isOf(Fluids.WATER)){
                        moveTarget = randPos;
                    }
                }
                if(this.getFluidHeight(FluidTags.WATER) < this.getHeight()){
                    moveTarget = null;
                    this.setVelocity(this.getVelocity().add(0, -0.02, 0));
                }
                if(moveTarget != null){
                    final double d0 = moveTarget.getX() + 0.5F - this.getX();
                    final double d1 = moveTarget.getY() + 0.5F - this.getY();
                    final double d2 = moveTarget.getZ() + 0.5F - this.getZ();
                    final double d3 = MathHelper.sqrt((float) (d0 * d0 + d1 * d1 + d2 * d2));
                    final float f = (float)(MathHelper.atan2(d2, d0) * 57.2957763671875D) - 90.0F;
                    this.setYaw(rotlerp(this.getYaw(), f, 1));
                    this.bodyYaw = this.getYaw();
                    final float movSpeed = 0.004F;
                    final var movingVec = new Vec3d(d0/d3, d1/d3, d2/d3).normalize();
                    this.setVelocity(this.getVelocity().add(movingVec.multiply(movSpeed)));
                }
                final float dist = (float) ((Math.abs(this.getVelocity().x) + Math.abs(this.getVelocity().z)) * 30);
                this.incrementJellyPitch(dist);
                if (this.horizontalCollision) {
                    this.setVelocity(this.getVelocity().add(0, 0.2F, 0));
                }
                if (this.getJellyPitch() > 0F) {
                    float decrease = Math.min(0.5F, this.getJellyPitch());
                    this.decrementJellyPitch(decrease);
                }
                if (this.getJellyPitch() < 0F) {
                    float decrease = Math.min(0.5F, -this.getJellyPitch());
                    this.incrementJellyPitch(decrease);
                }
            } else {
                this.setNoGravity(false);
            }
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("FromBucket", this.isFromBucket());
        compound.putFloat("JellyScale", this.getJellyScale());
        compound.putInt("Variant", this.getVariant());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setFromBucket(compound.getBoolean("FromBucket"));
        this.setJellyScale(compound.getFloat("JellyScale"));
        this.setVariant(compound.getInt("Variant"));
    }

    public boolean canBlockPosBeSeen(BlockPos pos) {
        final double x = pos.getX() + 0.5F;
        final double y = pos.getY() + 0.5F;
        final double z = pos.getZ() + 0.5F;
        final var result = this.getWorld().raycast(new RaycastContext(this.getEyePos(), new Vec3d(x, y, z), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        final double dist = result.getPos().squaredDistanceTo(x, y, z);
        return dist <= 1.0D || result.getType() == HitResult.Type.MISS;
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.canMoveVoluntarily() && this.isTouchingWater()) {
            this.updateVelocity(this.getMovementSpeed(), travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.9D, 0.6D, 0.9D));
        } else {
            super.travel(travelVector);
        }

    }

    @Override
    @NotNull
    protected ActionResult interactMob(@NotNull PlayerEntity player, @NotNull Hand hand) {
        return Bucketable.tryBucket(player, hand, this).orElse(super.interactMob(player, hand));
    }

    @Override
    public void copyDataToStack(@NotNull ItemStack bucket) {
        if (this.hasCustomName()) {
            bucket.setCustomName(this.getCustomName());
        }
        Bucketable.copyDataToStack(this, bucket);
        NbtCompound compoundnbt = bucket.getOrCreateNbt();
        compoundnbt.putFloat("BucketScale", this.getJellyScale());
        compoundnbt.putInt("BucketVariantTag", this.getVariant());
    }

    @Override
    public void copyDataFromNbt(@NotNull NbtCompound compound) {
        Bucketable.copyDataFromNbt(this, compound);
        if (compound.contains("BucketScale")){
            this.setJellyScale(compound.getFloat("BucketScale"));
        }
        if (compound.contains("BucketVariantTag")){
            this.setVariant(compound.getInt("BucketVariantTag"));
        }
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.setVariant(random.nextInt(3));
        this.setJellyScale(0.8F + random.nextFloat() * 0.4F);
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    public void incrementJellyPitch(float pitch) {
        dataTracker.set(JELLYPITCH, getJellyPitch() + pitch);
    }

    public void decrementJellyPitch(float pitch) {
        dataTracker.set(JELLYPITCH, getJellyPitch() - pitch);
    }

    protected float rotlerp(float angle, float targetAngle, float maxIncrease) {
        float f = MathHelper.wrapDegrees(targetAngle - angle);
        if (f > maxIncrease) {
            f = maxIncrease;
        }

        if (f < -maxIncrease) {
            f = -maxIncrease;
        }

        float f1 = angle + f;
        if (f1 < 0.0F) {
            f1 += 360.0F;
        } else if (f1 > 360.0F) {
            f1 -= 360.0F;
        }

        return f1;
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.AQUATIC;
    }

    @Override
    public boolean canSpawn(WorldView worldIn) {
        return worldIn.doesNotIntersectEntities(this);
    }

}
