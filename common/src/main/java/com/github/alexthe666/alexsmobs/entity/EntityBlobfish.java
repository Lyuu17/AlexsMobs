package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAISwimBottom;
import com.github.alexthe666.alexsmobs.entity.ai.AquaticMoveController;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.MoveIntoWaterGoal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.SwimNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EntityBlobfish extends WaterCreatureEntity implements Flutterer, Bucketable {

    private static final TrackedData<Boolean> FROM_BUCKET = DataTracker.registerData(EntityBlobfish.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> BLOBFISH_SCALE = DataTracker.registerData(EntityBlobfish.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> DEPRESSURIZED = DataTracker.registerData(EntityBlobfish.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SLIMED = DataTracker.registerData(EntityBlobfish.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float squishFactor;
    public float prevSquishFactor;
    public float squishAmount;
    private boolean wasOnGround;

    public EntityBlobfish(EntityType<? extends EntityBlobfish> type, World world) {
        super(type, world);
        this.moveControl = new AquaticMoveController(this, 1.0F);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.blobfishSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 8D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new SwimNavigation(this, worldIn);
    }

    @Override
    protected void tickWaterBreathingAir(int p_209207_1_) {
        if (this.isAlive() && !this.isInsideWaterOrBubbleColumn() && !isSlimed()) {
            this.setAir(p_209207_1_ - 1);
            if (this.getAir() == -20) {
                this.setAir(0);
                this.damage(getDamageSources().drown(), random.nextInt(2) == 0 ? 1F : 0F);
            }
        } else {
            this.setAir(2000);
        }
    }

    @Override
    protected float getActiveEyeHeight(EntityPose p_213348_1_, EntityDimensions p_213348_2_) {
        return p_213348_2_.height * 0.65F;
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.hasCustomName() || this.isFromBucket() || isSlimed();
    }

    @Override
    public boolean canImmediatelyDespawn(double p_213397_1_) {
        return !this.isFromBucket() && !this.hasCustomName();
    }

    @Override
    public int getLimitPerChunk() {
        return 4;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FROM_BUCKET, false);
        this.dataTracker.startTracking(BLOBFISH_SCALE, 1.0F);
        this.dataTracker.startTracking(DEPRESSURIZED, false);
        this.dataTracker.startTracking(SLIMED, false);
    }

    @Override
    public EntityDimensions getDimensions(EntityPose poseIn) {
        return super.getDimensions(poseIn).scaled(this.getBlobfishScale());
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

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("FromBucket", this.isFromBucket());
        compound.putBoolean("Depressurized", this.isDepressurized());
        compound.putBoolean("Slimed", this.isSlimed());
        compound.putFloat("BlobfishScale", this.getBlobfishScale());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setFromBucket(compound.getBoolean("FromBucket"));
        this.setDepressurized(compound.getBoolean("Depressurized"));
        this.setSlimed(compound.getBoolean("Slimed"));
        this.setBlobfishScale(compound.getFloat("BlobfishScale"));
    }

    private boolean hasClearance() {
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();
        for (int l1 = 0; l1 < 10; ++l1) {
            var blockstate = getWorld().getBlockState(blockpos$mutable.set(this.getX(), this.getY() + l1, this.getZ()));
            if (!blockstate.getFluidState().isIn(FluidTags.WATER) && !blockstate.isSolid()) {
                return false;
            }
        }

        return true;
    }

    public float getBlobfishScale() {
        return this.dataTracker.get(BLOBFISH_SCALE);
    }

    public void setBlobfishScale(float scale) {
        this.dataTracker.set(BLOBFISH_SCALE, scale);
    }

    public boolean isDepressurized() {
        return this.dataTracker.get(DEPRESSURIZED);
    }

    public void setDepressurized(boolean depressurized) {
        this.dataTracker.set(DEPRESSURIZED, depressurized);
    }

    public boolean isSlimed() {
        return this.dataTracker.get(SLIMED);
    }

    public void setSlimed(boolean slimed) {
        this.dataTracker.set(SLIMED, slimed);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(1, new MoveIntoWaterGoal(this));
        this.goalSelector.add(2, new EscapeDangerGoal(this, 1D));
        this.goalSelector.add(3, new AnimalAISwimBottom(this, 1F, 7));
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
    @NotNull
    protected ActionResult interactMob(@NotNull PlayerEntity player, @NotNull Hand hand) {
        final var lvt_3_1_ = player.getStackInHand(hand);
        if (lvt_3_1_.getItem() == Items.SLIME_BALL && this.isAlive() && !this.isSlimed()) {
            this.setSlimed(true);
            for (int i = 0; i < 6 + random.nextInt(3); i++) {
                final double d2 = this.random.nextGaussian() * 0.02D;
                final double d0 = this.random.nextGaussian() * 0.02D;
                final double d1 = this.random.nextGaussian() * 0.02D;
                this.getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, lvt_3_1_), this.getX() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, this.getY() + this.getHeight() * 0.5F + (double) (this.random.nextFloat() * this.getHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, d0, d1, d2);
            }
            lvt_3_1_.decrement(1);
            return ActionResult.success(this.getWorld().isClient);
        }
        return Bucketable.tryBucket(player, hand, this).orElse(super.interactMob(player, hand));
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.ENTITY_FISH_SWIM;
    }

    @Override
    protected void playStepSound(BlockPos p_180429_1_, BlockState p_180429_2_) {
    }

    @Override
    @NotNull
    public ItemStack getBucketItem() {
        var stack = new ItemStack(AMItemRegistry.BLOBFISH_BUCKET.get());
        if (this.hasCustomName()) {
            stack.setCustomName(this.getCustomName());
        }
        return stack;
    }

    @Override
    public void copyDataToStack(@NotNull ItemStack bucket) {
        if (this.hasCustomName()) {
            bucket.setCustomName(this.getCustomName());
        }
        Bucketable.copyDataToStack(this, bucket);
        NbtCompound compound = bucket.getOrCreateNbt();
        compound.putFloat("BucketScale", this.getBlobfishScale());
        compound.putBoolean("Slimed", this.isSlimed());
    }

    @Override
    public void copyDataFromNbt(@NotNull NbtCompound compound) {
        Bucketable.copyDataFromNbt(this, compound);
        if (compound.contains("BucketScale")){
            this.setBlobfishScale(compound.getFloat("BucketScale"));
        }
        if (compound.contains("Slimed")){
            this.setSlimed(compound.getBoolean("Slimed"));
        }
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.setBlobfishScale(0.75F + random.nextFloat() * 0.5F);
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    public void tick() {
        super.tick();
        this.prevSquishFactor = this.squishFactor;
        this.squishFactor += (this.squishAmount - this.squishFactor) * 0.5F;

        final float f2 = (float) -((float) this.getVelocity().y * 2.2F * MathHelper.DEGREES_PER_RADIAN);
        this.setPitch(f2);
        if (!isTouchingWater()) {
            if (this.isOnGround()) {
                if (!this.wasOnGround)
                    this.squishAmount = -0.35F;
            } else {
                if (this.wasOnGround)
                    this.squishAmount = 2F;
            }
        }
        this.wasOnGround = this.isOnGround();

        this.alterSquishAmount();
        final boolean clear = hasClearance();
        if (clear) {
            if (this.isDepressurized())
                this.setDepressurized(false);
        } else {
            if (!isDepressurized())
                this.setDepressurized(true);
        }
    }

    protected void alterSquishAmount() {
        this.squishAmount *= 0.6F;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_COD_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.ENTITY_COD_HURT;
    }

    public static boolean canBlobfishSpawn(EntityType<EntityBlobfish> entityType, ServerWorldAccess iServerWorld, SpawnReason reason, BlockPos pos, Random random) {
        return reason == SpawnReason.SPAWNER || pos.getY() <= AMConfig.blobfishSpawnHeight && iServerWorld.getFluidState(pos).isIn(FluidTags.WATER) && iServerWorld.getFluidState(pos.up()).isIn(FluidTags.WATER);
    }

    @Override
    public boolean isInAir() {
        return false;
    }
}
