package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIRandomSwimming;
import com.github.alexthe666.alexsmobs.entity.ai.AquaticMoveController;
import com.github.alexthe666.alexsmobs.entity.ai.SwimmerJumpPathNavigator;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MoveIntoWaterGoal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class EntityFlyingFish extends WaterCreatureEntity implements Flutterer, Bucketable {

    private static final TrackedData<Boolean> GLIDING = DataTracker.registerData(EntityFlyingFish.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(EntityFlyingFish.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> FROM_BUCKET = DataTracker.registerData(EntityFlyingFish.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float prevOnLandProgress;
    public float onLandProgress;
    public float prevFlyProgress;
    public float flyProgress;
    private int glideIn = random.nextInt(75) + 50;

    public EntityFlyingFish(EntityType<? extends WaterCreatureEntity> type, World level) {
        super(type, level);
        this.moveControl = new AquaticMoveController(this, 1.0F, 15F);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(1, new MoveIntoWaterGoal(this));
        this.goalSelector.add(2, new GlideGoal(this));
        this.goalSelector.add(3, new EscapeDangerGoal(this, 1D));
        this.goalSelector.add(4, new AnimalAIRandomSwimming(this, 1F, 12, 5));
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_COD_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.ENTITY_COD_HURT;
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.isFromBucket();
    }

    @Override
    public boolean canImmediatelyDespawn(double p_27492_) {
        return !this.isFromBucket() && !this.hasCustomName();
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new SwimmerJumpPathNavigator(this, worldIn);
    }

    @Override
    public int getLimitPerChunk() {
        return 8;
    }

    @Override
    public boolean spawnsTooManyForEachTry(int sizeIn) {
        return false;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FROM_BUCKET, false);
        this.dataTracker.startTracking(GLIDING, false);
        this.dataTracker.startTracking(VARIANT, 0);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 6.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.flyingFishSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean prev = super.damage(source, amount);
        if(prev && source.getAttacker() != null){
            double range = 15;
            this.glideIn = 0;
            List<? extends EntityFlyingFish> list = this.getWorld().getNonSpectatingEntities(this.getClass(), this.getBoundingBox().expand(range, range/2, range));
            for(EntityFlyingFish fsh : list){
                fsh.glideIn = 0;
            }
        }
        return prev;
    }

    @Override
    public void tick(){
        super.tick();
        this.prevOnLandProgress = onLandProgress;
        this.prevFlyProgress = flyProgress;
        boolean onLand = !this.isInsideWaterOrBubbleColumn() && this.isOnGround();
        if (onLand && onLandProgress < 5F) {
            onLandProgress++;
        }
        if (!onLand && onLandProgress > 0F) {
            onLandProgress--;
        }

        if (isGliding()) {
            if (flyProgress < 5F)
                flyProgress++;

            if (!this.isInsideWaterOrBubbleColumn() && this.getVelocity().y < 0.0)
                this.setVelocity(this.getVelocity().multiply(1.0F, 0.5F, 1.0F));
        } else {
            if (flyProgress > 0F)
                flyProgress--;
        }

        if(glideIn > 0){
            glideIn--;
        }
        this.bodyYaw = this.getYaw();
        float f2 = (float) -((float) this.getVelocity().y * 3F * (double) MathHelper.DEGREES_PER_RADIAN);
        if(this.isGliding()){
            f2 = -f2;
        }
        this.setPitch(changeAngle(this.getPitch(), f2, 9));
        if(!isInsideWaterOrBubbleColumn() && this.isAlive()){
            if (this.isOnGround() && random.nextFloat() < 0.05F) {
                this.setVelocity(this.getVelocity().add((this.random.nextFloat() * 2.0F - 1.0F) * 0.2F, 0.5D, (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F));
                this.setYaw(this.random.nextFloat() * 360.0F);
                this.playSound(SoundEvents.ENTITY_COD_FLOP, this.getSoundVolume(), this.getSoundPitch());
            }
        }
    }

    @Override
    protected void tickWaterBreathingAir(int i) {
        if (this.isAlive() && !this.isInsideWaterOrBubbleColumn()) {
            this.setAir(i - 1);
            if (this.getAir() == -20) {
                this.setAir(0);
                this.damage(getDamageSources().dryOut(), 2.0F);
            }
        } else {
            this.setAir(1000);
        }
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.canMoveVoluntarily() && this.isTouchingWater()) {
            this.updateVelocity(this.getMovementSpeed(), travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            float f = 0.6F;
            this.setVelocity(this.getVelocity().multiply(0.9D, f, 0.9D));
            if (this.getTarget() == null) {
                this.setVelocity(this.getVelocity().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.ENTITY_FISH_SWIM;
    }

    @Override
    public boolean handleFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, variant);
    }

    public boolean isGliding() {
        return this.dataTracker.get(GLIDING);
    }

    public void setGliding(boolean flying) {
        this.dataTracker.set(GLIDING, flying);
    }

    private boolean canSeeBlock(BlockPos destinationBlock) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        var blockVec = Vec3d.ofCenter(destinationBlock);
        var result = this.getWorld().raycast(new RaycastContext(Vector3d, blockVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        return result.getBlockPos().equals(destinationBlock);
    }

    @Override
    public boolean isInAir() {
        return true;
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
        compound.putInt("Variant", this.getVariant());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setFromBucket(compound.getBoolean("FromBucket"));
        this.setVariant(compound.getInt("Variant"));
    }

    @NotNull
    @Override
    public ItemStack getBucketItem() {
        var stack = new ItemStack(AMItemRegistry.FLYING_FISH_BUCKET.get());
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
        compound.putInt("Variant", this.getVariant());
    }

    @Override
    public void copyDataFromNbt(@NotNull NbtCompound compound) {
        Bucketable.copyDataFromNbt(this, compound);
        if (compound.contains("Variant")){
            this.setVariant(compound.getInt("Variant"));
        }
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty diff, SpawnReason spawnType, @Nullable EntityData data, @Nullable NbtCompound tag) {
        int i;
        if (data instanceof FlyingFishGroupData) {
            i = ((FlyingFishGroupData)data).variant;
        } else {
            i = this.random.nextInt(3);
            data = new FlyingFishGroupData(i);
        }

        this.setVariant(i);
        return super.initialize(world, diff, spawnType, data, tag);
    }

    @Override
    @NotNull
    protected ActionResult interactMob(@NotNull PlayerEntity player, @NotNull Hand hand) {
        return Bucketable.tryBucket(player, hand, this).orElse(super.interactMob(player, hand));
    }

    public static class FlyingFishGroupData extends PassiveEntity.PassiveData {

        public final int variant;

        FlyingFishGroupData(int variant) {
            super(true);
            this.variant = variant;
        }
    }

    private class GlideGoal extends Goal {
        private final EntityFlyingFish fish;
        private final World level;
        private BlockPos surface;
        private BlockPos glide;

        public GlideGoal(EntityFlyingFish fish) {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
            this.fish = fish;
            this.level = fish.getWorld();
        }

        @Override
        public boolean canStart() {
            if(!fish.isInsideWaterOrBubbleColumn()){
                return false;
            }else if(fish.glideIn == 0 || fish.getRandom().nextInt(80) == 0){
                BlockPos found = findSurfacePos();
                if(found != null){
                    BlockPos glideTo = findGlideToPos(fish.getBlockPos(), found);
                    if(glideTo != null){
                        surface = found;
                        glide = glideTo;
                        fish.glideIn = 0;
                        return true;
                    }
                }
            }
            return false;
        }

        private BlockPos findSurfacePos(){
            var fishPos = fish.getBlockPos();
            for(int i = 0; i < 15; i++){
                var offset = fishPos.add(fish.random.nextInt(16) - 8, 0, fish.random.nextInt(16) - 8);
                while(level.isWater(offset) && offset.getY() < level.getTopY()){
                    offset = offset.up();
                }
                if(!level.isWater(offset) && level.isWater(offset.down()) && fish.canSeeBlock(offset)){
                    return offset;
                }
            }
            return null;
        }

        private BlockPos findGlideToPos(BlockPos fishPos, BlockPos surface) {
            var sub = Vec3d.of(surface.subtract(fishPos)).normalize();
            double scale = random.nextDouble() * 8 + 1;

            while(scale > 2){
                var scaled = sub.multiply(scale);
                BlockPos at = surface.add((int) scaled.x, 0, (int) scaled.z);
                if(!level.isWater(at) && level.isWater(at.down()) && fish.canSeeBlock(at)){
                    return at;
                }
                scale -= 1;
            }
            return null;
        }

        @Override
        public boolean shouldContinue() {
            return surface != null && glide != null && (!fish.isOnGround() || fish.isInsideWaterOrBubbleColumn());
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
            surface = null;
            glide = null;
            fish.glideIn = random.nextInt(75) + 150;
            fish.setGliding(false);
        }

        @Override
        public void tick() {
            if(fish.isInsideWaterOrBubbleColumn() && fish.squaredDistanceTo(Vec3d.ofCenter(surface)) > 3F){
                fish.getNavigation().startMovingTo(surface.getX() + 0.5F, surface.getY() + 1F, surface.getZ() + 0.5F, 1.2F);
                if(fish.isGliding()){
                    stop();
                }
            }else{
                fish.getNavigation().stop();
                var face = Vec3d.ofCenter(glide).subtract(Vec3d.ofCenter(surface));
                if(face.length() < 0.2F){
                    face = fish.getRotationVector();
                }
                var target = face.normalize().multiply(0.1F);
                double y = 0;
                if(!fish.isGliding()){
                    y = 0.4F + random.nextFloat() * 0.2F;
                }else if(fish.isGliding() && fish.isInsideWaterOrBubbleColumn()){
                    stop();
                }
                var move = fish.getVelocity().add(target.x, y, (double) (target.y));
                fish.setVelocity(move);
                double d0 = move.horizontalLength();
                fish.setPitch((float)(-MathHelper.atan2(move.y, d0) * (double)MathHelper.DEGREES_PER_RADIAN));
                fish.setYaw(((float) MathHelper.atan2(move.z, move.x)) * MathHelper.DEGREES_PER_RADIAN - 90F);
                fish.setGliding(true);
            }
        }
    }
}
