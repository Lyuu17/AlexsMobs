package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.entity.ai.AnimalAISwimBottom;
import com.github.alexthe666.alexsmobs.entity.ai.AquaticMoveController;
import com.github.alexthe666.alexsmobs.entity.ai.CreatureAITargetItems;
import com.github.alexthe666.alexsmobs.registry.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.Goal;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class EntityTriops extends WaterCreatureEntity implements ITargetsDroppedItems, Bucketable {

    private static final TrackedData<Boolean> FROM_BUCKET = DataTracker.registerData(EntityTriops.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> TRIOPS_SCALE = DataTracker.registerData(EntityTriops.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> BABY_AGE = DataTracker.registerData(EntityTriops.class, TrackedDataHandlerRegistry.INTEGER);
    public float prevOnLandProgress;
    public float onLandProgress;
    public float prevSwimRot;
    public float swimRot;
    public boolean fedCarrot = false;
    public int breedCooldown = 0;
    public float tail1Yaw;
    public float prevTail1Yaw;
    public float tail2Yaw;
    public float prevTail2Yaw;
    public float moveDistance;

    private EntityTriops breedWith;
    private boolean pregnant;

    public EntityTriops(EntityType<? extends WaterCreatureEntity> type, World level) {
        super(type, level);
        this.moveControl = new AquaticMoveController(this, 1.0F, 15F);
        tail1Yaw = this.getYaw();
        prevTail1Yaw = this.getYaw();
        tail2Yaw = this.getYaw();
        prevTail2Yaw = this.getYaw();
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FROM_BUCKET, false);
        this.dataTracker.startTracking(TRIOPS_SCALE, 1F);
        this.dataTracker.startTracking(BABY_AGE, 0);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new AnimalMateGoal());
        this.goalSelector.add(1, new LayEggGoal());
        this.goalSelector.add(2, new MoveIntoWaterGoal(this));
        this.goalSelector.add(3, new EscapeDangerGoal(this, 1D));
        this.goalSelector.add(4, new AnimalAISwimBottom(this, 1F, 7));
        this.targetSelector.add(1, new CreatureAITargetItems<>(this, false, 10));
    }

    @Override
    public int getLimitPerChunk() {
        return 5;
    }

    @Override
    public boolean spawnsTooManyForEachTry(int sizeIn) {
        return false;
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new SwimNavigation(this, worldIn);
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.canMoveVoluntarily() && this.isInsideWaterOrBubbleColumn()) {
            this.updateVelocity(this.getMovementSpeed(), travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.9D, 0.8D, 0.9D));
            if (this.getTarget() == null) {
                this.setVelocity(this.getVelocity().add(0.0D, -0.005D, 0.0D));
            }
            moveDistance += (float) travelVector.horizontalLength();
        } else {
            super.travel(travelVector);
        }

    }

    @Override
    protected void playSwimSound(float f) {
        if (random.nextInt(2) == 0) {
            this.playSound(this.getSwimSound(), 0.2F, 1.3F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
        }
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.ENTITY_FISH_SWIM;
    }

    @Override
    public boolean isFromBucket() {
        return this.dataTracker.get(FROM_BUCKET);
    }

    @Override
    public void setFromBucket(boolean sit) {
        this.dataTracker.set(FROM_BUCKET, sit);
    }

    @Override
    @NotNull
    public SoundEvent getBucketFillSound() {
        return SoundEvents.ITEM_BUCKET_FILL_FISH;
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.isFromBucket() || this.isBaby() || fedCarrot;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceToClosestPlayer) {
        return !this.isBaby() && !this.isFromBucket() && !fedCarrot;
    }

    @Override
    protected void tickWaterBreathingAir(int i) {
        if (this.isAlive() && !this.isInsideWaterOrBubbleColumn()) {
            this.setAir(i - 1);
            if (this.getAir() == -20) {
                this.setAir(0);
                this.damage(getDamageSources().dryOut(), random.nextInt(2) == 0 ? 1F : 0F);
            }
        } else {
            this.setAir(2000);
        }
    }

    public int getBabyAge() {
        return this.dataTracker.get(BABY_AGE);
    }

    public void setBabyAge(int babyAge) {
        this.dataTracker.set(BABY_AGE, babyAge);
    }

    public float getTriopsScale() {
        return this.dataTracker.get(TRIOPS_SCALE);
    }

    public void setTriopsScale(float scale) {
        this.dataTracker.set(TRIOPS_SCALE, scale);
    }

    @Override
    public boolean isBaby() {
        return getBabyAge() < 0;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("FromBucket", this.isFromBucket());
        compound.putBoolean("FedCarrot", this.fedCarrot);
        compound.putBoolean("Pregnant", this.pregnant);
        compound.putInt("BreedCooldown", this.breedCooldown);
        compound.putFloat("TriopsScale", this.getTriopsScale());
        compound.putInt("BabyAge", this.getBabyAge());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setFromBucket(compound.getBoolean("FromBucket"));
        this.fedCarrot = compound.getBoolean("FedCarrot");
        this.pregnant = compound.getBoolean("Pregnant");
        this.breedCooldown = compound.getInt("BreedCooldown");
        this.setTriopsScale(compound.getFloat("TriopsScale"));
        this.setBabyAge(compound.getInt("BabyAge"));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 4.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.setTriopsScale(0.9F + random.nextFloat() * 0.2F);
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public void tick() {
        super.tick();
        this.prevOnLandProgress = onLandProgress;
        this.prevSwimRot = swimRot;
        this.prevTail1Yaw = tail1Yaw;
        this.prevTail2Yaw = tail2Yaw;
        final boolean onLand = !this.isInsideWaterOrBubbleColumn() && this.isOnGround();
        this.setPitch(-((float) this.getVelocity().y * 2.2F * MathHelper.DEGREES_PER_RADIAN));
        if (onLand && onLandProgress < 5F) {
            onLandProgress++;
        }
        if (!onLand && onLandProgress > 0F) {
            onLandProgress--;
        }
        if (breedCooldown > 0) {
            breedCooldown--;
        }
        tail1Yaw = MathHelper.stepUnwrappedAngleTowards(this.tail1Yaw, bodyYaw, 7);
        tail2Yaw = MathHelper.stepUnwrappedAngleTowards(this.tail2Yaw, this.tail1Yaw, 7);
        if (onLandProgress == 0) {
            float f = (float) (20 * Math.sin(this.limbAnimator.getPos()) * limbAnimator.getSpeed());
            swimRot = MathHelper.stepUnwrappedAngleTowards(this.swimRot, f, 2);
        }
    }

    @Override
    public void updateLimbs(boolean flying) {
        float f1 = (float) MathHelper.magnitude(this.getX() - this.prevX, this.getY() - this.prevY, this.getZ() - this.prevZ);
        float f2 = Math.min(f1 * 6, 1.0F);
        this.limbAnimator.updateLimbs(f2, 0.4F);
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.ARTHROPOD;
    }

    @Override
    public void handleStatus(byte id) {
        if (id == 67) {
            for (int i = 0; i < 5; i++) {
                getWorld().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getParticleX(0.5F), this.getBodyY(0.8F), this.getParticleZ(0.5F), 0.0D, 0.0D, 0.0D);
            }
        } else if (id == 68) {
            getWorld().addParticle(ParticleTypes.HEART, this.getX(), this.getBodyY(0.8F), this.getZ(), 0.0D, 0.0D, 0.0D);
        } else {
            super.handleStatus(id);
        }
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return (stack.isIn(AMTagRegistry.TRIOPS_BREEDABLES) || stack.isOf(AMItemRegistry.MOSQUITO_LARVA.get())) && !fedCarrot;
    }

    @Override
    public void onGetItem(ItemEntity e) {
        var stack = e.getStack();
        if (stack.getItem().isFood() && stack.getItem().getFoodComponent() != null) {
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_CAT_EAT, this.getSoundPitch(), this.getSoundVolume());
            this.heal(5);
            if (!this.getWorld().isClient) {
                if (breedCooldown == 0 && !fedCarrot) {
                    this.fedCarrot = true;
                    this.getWorld().sendEntityStatus(this, (byte) 67);
                }
            }
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        var itemstack = player.getStackInHand(hand);
        var type = super.interactMob(player, hand);
        if (!type.isAccepted() && canTargetItem(itemstack) && !this.fedCarrot) {
            if (!player.getAbilities().creativeMode) {
                itemstack.decrement(1);
            }
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_CAT_EAT, this.getSoundPitch(), this.getSoundVolume());
            this.heal(5);
            if (itemstack.isIn(AMTagRegistry.TRIOPS_BREEDABLES)) {
                if (!this.getWorld().isClient) {
                    if (breedCooldown == 0) {
                        this.getWorld().sendEntityStatus(this, (byte) 67);
                    }
                }
                this.fedCarrot = true;
            }
            return ActionResult.SUCCESS;
        }
        return Bucketable.tryBucket(player, hand, this).orElse(type);
    }

    public boolean isSearchingForMate() {
        return this.isAlive() && this.isInsideWaterOrBubbleColumn() && this.fedCarrot && this.breedCooldown <= 0;
    }

    @Override
    public void copyDataToStack(@NotNull ItemStack bucket) {
        if (this.hasCustomName()) {
            bucket.setCustomName(this.getCustomName());
        }
        NbtCompound platTag = new NbtCompound();
        this.writeCustomDataToNbt(platTag);
        NbtCompound compound = bucket.getOrCreateNbt();
        compound.put("TriopsTag", platTag);
    }

    @Override
    public void copyDataFromNbt(@NotNull NbtCompound compound) {
        if (compound.contains("TriopsTag")) {
            this.readCustomDataFromNbt(compound.getCompound("TriopsTag"));
        }
        this.setAir(2000);
    }

    @Override
    public ItemStack getBucketItem() {
        ItemStack stack = new ItemStack(AMItemRegistry.TRIOPS_BUCKET.get());
        if (this.hasCustomName()) {
            stack.setCustomName(this.getCustomName());
        }
        return stack;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.TRIOPS_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.TRIOPS_HURT.get();
    }

    private class AnimalMateGoal extends Goal {
        private final Predicate<Entity> validBreedPartner;
        private EntityTriops breedPartner;

        private int executionCooldown = 50;

        public AnimalMateGoal() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
            this.validBreedPartner = (shrimp -> shrimp instanceof EntityTriops otherFish && otherFish.getId() != EntityTriops.this.getId() && otherFish.isSearchingForMate());
        }

        @Override
        public boolean canStart() {
            if (!EntityTriops.this.isInsideWaterOrBubbleColumn() || !EntityTriops.this.fedCarrot || EntityTriops.this.breedCooldown > 0 || EntityTriops.this.breedWith != null) {
                return false;
            }
            if (executionCooldown > 0) {
                executionCooldown--;
            } else {
                executionCooldown = 50 + random.nextInt(50);
                List<EntityTriops> list = EntityTriops.this.getWorld().getEntitiesByClass(EntityTriops.class, EntityTriops.this.getBoundingBox().expand(10, 8, 10), EntityPredicates.EXCEPT_SPECTATOR.and(validBreedPartner));
                list.sort(Comparator.comparingDouble(EntityTriops.this::squaredDistanceTo));
                if (!list.isEmpty()) {
                    EntityTriops closestPupfish = list.get(0);
                    if (closestPupfish != null) {
                        breedPartner = closestPupfish;
                        breedPartner.breedWith = EntityTriops.this;
                        return true;
                    }
                }

            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return breedPartner != null && !EntityTriops.this.pregnant && !breedPartner.pregnant && EntityTriops.this.breedWith == null && breedPartner.isSearchingForMate() && EntityTriops.this.isSearchingForMate();
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
            EntityTriops.this.fedCarrot = false;
            EntityTriops.this.breedCooldown = 1200 + random.nextInt(3600);
        }

        @Override
        public void tick() {
            EntityTriops.this.getNavigation().startMovingTo(breedPartner, 1D);
            breedPartner.getNavigation().startMovingTo(EntityTriops.this, 1D);
            if (EntityTriops.this.distanceTo(breedPartner) < 1.2F) {
                EntityTriops.this.getWorld().sendEntityStatus(EntityTriops.this, (byte) 68);
                EntityTriops.this.pregnant = true;
            }
        }
    }

    class LayEggGoal extends Goal {
        private BlockPos eggPos;

        LayEggGoal() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public void stop() {
            eggPos = null;
        }

        @Override
        public boolean canStart() {
            if (EntityTriops.this.pregnant && EntityTriops.this.getRandom().nextInt(30) == 0) {
                BlockPos egg = getEggLayPos();
                if (egg != null) {
                    eggPos = egg;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return eggPos != null && EntityTriops.this.pregnant && EntityTriops.this.getWorld().getBlockState(eggPos).isAir();
        }

        public boolean isValidPos(BlockPos pos) {
            var state = EntityTriops.this.getWorld().getBlockState(pos);
            var stateBelow = EntityTriops.this.getWorld().getFluidState(pos.down());
            return stateBelow.isIn(FluidTags.WATER) && state.isAir();
        }

        public BlockPos getEggLayPos() {
            for (int i = 0; i < 10; i++) {
                BlockPos offset = EntityTriops.this.getBlockPos().add(EntityTriops.this.getRandom().nextInt(10) - 5, 10, EntityTriops.this.getRandom().nextInt(10) - 5);
                while (getWorld().getBlockState(offset.down()).isAir() && offset.getY() > EntityTriops.this.getWorld().getBottomY()) {
                    offset = offset.down();
                }
                if (isValidPos(offset)) {
                    return offset;
                }
            }
            return null;
        }

        @Override
        public void tick() {
            super.tick();
            EntityTriops.this.getNavigation().startMovingTo(eggPos.getX(), eggPos.getY(), eggPos.getZ(), 1);
            if (EntityTriops.this.squaredDistanceTo(Vec3d.ofBottomCenter(eggPos)) < 2.0F) {
                EntityTriops.this.pregnant = false;
                EntityTriops.this.getWorld().setBlockState(eggPos, AMBlockRegistry.TRIOPS_EGGS.get().getDefaultState());
            }
        }
    }
}
