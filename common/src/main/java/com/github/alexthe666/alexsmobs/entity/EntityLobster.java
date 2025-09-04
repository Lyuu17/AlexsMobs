package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIFindWater;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAILeaveWater;
import com.github.alexthe666.alexsmobs.entity.ai.BottomFeederAIWander;
import com.github.alexthe666.alexsmobs.entity.ai.SemiAquaticPathNavigator;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EntityLobster extends WaterCreatureEntity implements ISemiAquatic, Bucketable {

    private static final TrackedData<Boolean> FROM_BUCKET = DataTracker.registerData(EntityLobster.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> ATTACK_TICK = DataTracker.registerData(EntityLobster.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(EntityLobster.class, TrackedDataHandlerRegistry.INTEGER);
    public float attackProgress;
    public float prevAttackProgress;
    private int attackCooldown = 0;

    public EntityLobster(EntityType<? extends EntityLobster> type, World world) {
        super(type, world);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 0.0F);
    }

    public int getLimitPerChunk() {
        return 7;
    }

    public boolean spawnsTooManyForEachTry(int sizeIn) {
        return false;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 5D)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.15F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.lobsterSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.LOBSTER_HURT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.LOBSTER_HURT.get();
    }

    @Override
    public boolean canSpawn(WorldView worldIn) {
        return worldIn.doesNotIntersectEntities(this);
    }

    public static String getVariantName(int variant) {
        return switch (variant) {
            case 1 -> "blue";
            case 2 -> "yellow";
            case 3 -> "redblue";
            case 4 -> "black";
            case 5 -> "white";
            default -> "red";
        };
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new AnimalAIFindWater(this));
        this.goalSelector.add(1, new AnimalAILeaveWater(this));
        this.goalSelector.add(3, new BottomFeederAIWander(this, 1.0D, 10, 50));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.targetSelector.add(1, (new RevengeGoal(this)));
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.ARTHROPOD;
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.canMoveVoluntarily() && this.isTouchingWater()) {
            this.updateVelocity(this.getMovementSpeed(), travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            if(this.jumping){
                this.setVelocity(this.getVelocity().multiply(1.4D));
                this.setVelocity(this.getVelocity().add(0.0D, 0.72D, 0.0D));
            }else{
                this.setVelocity(this.getVelocity().multiply(0.4D));
                this.setVelocity(this.getVelocity().add(0.0D, -0.08D, 0.0D));
            }

        } else {
            super.travel(travelVector);
        }

    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VARIANT, 0);
        this.dataTracker.startTracking(ATTACK_TICK, 0);
        this.dataTracker.startTracking(FROM_BUCKET, false);
    }

    @Override
    @NotNull
    public ItemStack getBucketItem() {
        var stack = new ItemStack(AMItemRegistry.LOBSTER_BUCKET.get());
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
        var compoundnbt = bucket.getOrCreateNbt();
        compoundnbt.putInt("BucketVariantTag", this.getVariant());
    }

    @Override
    public void copyDataFromNbt(@NotNull NbtCompound compound) {
        Bucketable.copyDataFromNbt(this, compound);
        if (compound.contains("BucketVariantTag", 3)) {
            this.setVariant(compound.getInt("BucketVariantTag"));
        }
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.isFromBucket();
    }

    @Override
    public boolean canImmediatelyDespawn(double p_27492_) {
        return !this.isFromBucket() && !this.hasCustomName();
    }

    @NotNull
    protected ActionResult interactMob(@NotNull PlayerEntity player, @NotNull Hand hand) {
        return Bucketable.tryBucket(player, hand, this).orElse(super.interactMob(player, hand));
    }

    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView worldIn) {
        return worldIn.getFluidState(pos.down()).isEmpty() && worldIn.getFluidState(pos).isIn(FluidTags.WATER) ? 10.0F : super.getPathfindingFavor(pos, worldIn);
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        this.dataTracker.set(ATTACK_TICK, 5);
        return super.tryAttack(entityIn);
    }

    @Override
    public void tick() {
        super.tick();
        prevAttackProgress = attackProgress;
        if (this.dataTracker.get(ATTACK_TICK) > 0) {
            if(attackProgress == 3){
                this.playSound(AMSoundRegistry.LOBSTER_ATTACK.get(), this.getSoundVolume(), this.getSoundPitch());
            }
            if (this.dataTracker.get(ATTACK_TICK) == 2 && this.getTarget() != null && this.distanceTo(this.getTarget()) < 1.3D) {
                this.getTarget().damage(this.getDamageSources().mobAttack(this), 2);
            }
            this.dataTracker.set(ATTACK_TICK, this.dataTracker.get(ATTACK_TICK) - 1);
            if (attackProgress < 5F) {
                attackProgress++;
            }
        } else {
            if (attackProgress > 0F) {
                attackProgress--;
            }
        }
        if(attackCooldown > 0){
            attackCooldown--;
        }
        if(this.getTarget() != null && this.distanceTo(this.getTarget()) <= 1F && attackCooldown == 0){
            this.lookAtEntity(this.getTarget(), 180F, 20F);
            tryAttack(this.getTarget());
            attackCooldown = 20;
        }
    }

    @Override
    protected void tickWaterBreathingAir(int air) {
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, Integer.valueOf(variant));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putInt("Variant", this.getVariant());
        compound.putBoolean("FromBucket", this.isFromBucket());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setVariant(compound.getInt("Variant"));
        this.setFromBucket(compound.getBoolean("FromBucket"));
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

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        float variantChange = this.getRandom().nextFloat();
        if(variantChange <= 0.00001){
            this.setVariant(5);
        }else if(variantChange <= 0.00002){
            this.setVariant(4);
        }else if(variantChange <= 0.05F){
            this.setVariant(3);
        }else if(variantChange <= 0.1F){
            this.setVariant(2);
        }else if(variantChange <= 0.25F){
            this.setVariant(1);
        }else{
            this.setVariant(0);
        }
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        var flyingpathnavigator = new SemiAquaticPathNavigator(this, worldIn) {
            public boolean isStableDestination(BlockPos pos) {
                return this.world.getBlockState(pos).getFluidState().isEmpty();
            }
        };
        return flyingpathnavigator;
    }

    @Override
    public boolean shouldEnterWater() {
        return true;
    }

    @Override
    public boolean shouldLeaveWater() {
        return false;
    }

    @Override
    public boolean shouldStopMoving() {
        return false;
    }

    @Override
    public int getWaterSearchRange() {
        return 5;
    }

    public static <T extends MobEntity> boolean canLobsterSpawn(EntityType type, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        boolean spawnBlock = worldIn.getBlockState(pos.down()).isIn(AMTagRegistry.LOBSTER_SPAWNS);
        return spawnBlock || worldIn.getFluidState(pos).isIn(FluidTags.WATER);
    }
}
