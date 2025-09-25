package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIWanderRanged;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.SpiderNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public class EntityBananaSlug extends AnimalEntity {

    private static final TrackedData<Direction> ATTACHED_FACE = DataTracker.registerData(EntityBananaSlug.class, TrackedDataHandlerRegistry.FACING);
    private static final TrackedData<Byte> CLIMBING = DataTracker.registerData(EntityBananaSlug.class, TrackedDataHandlerRegistry.BYTE);

    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(EntityBananaSlug.class, TrackedDataHandlerRegistry.INTEGER);
    private static final Direction[] POSSIBLE_DIRECTIONS = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    public float trailYaw;
    public float prevTrailYaw;
    public float trailVisability;
    public float prevTrailVisability;

    public float attachChangeProgress = 0F;
    public float prevAttachChangeProgress = 0F;
    public Direction prevAttachDir = Direction.DOWN;
    public int timeUntilSlime = this.random.nextInt(12000) + 24000;

    public EntityBananaSlug(EntityType<EntityBananaSlug> animal, World level) {
        super(animal, level);
        prevTrailYaw = this.bodyYaw;
        trailYaw = this.bodyYaw;
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new SpiderNavigation(this, worldIn);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.BANANA_SLUG_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.BANANA_SLUG_HURT.get();
    }

    public static boolean checkBananaSlugSpawnRules(EntityType<? extends AnimalEntity> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return !worldIn.getBlockState(pos.down()).isAir();
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.bananaSlugSpawnRolls, this.getRandom(), spawnReasonIn) && super.canSpawn(worldIn, spawnReasonIn);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CLIMBING, (byte) 0);
        this.dataTracker.startTracking(ATTACHED_FACE, Direction.DOWN);
        this.dataTracker.startTracking(VARIANT, 0);
    }

    // FIXME forge
    public boolean canTrample(BlockState state, BlockPos pos, float fallDistance) {
        return false;
    }

    @Override
    public boolean handleFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    protected void onBlockCollision(BlockState state) {
    }

    @Override
    public boolean canBreatheInWater() {
        return true;
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.setVariant(random.nextInt(4));
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.IN_WALL) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean isClimbing() {
        return this.isBesideClimbableBlock() ;
    }

    public boolean isBesideClimbableBlock() {
        return (this.dataTracker.get(CLIMBING) & 1) != 0 && this.getAttachmentFacing() != Direction.DOWN;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.BANANA_SLUG_BREEDABLES);
    }

    public void setBesideClimbableBlock(boolean climbing) {
        byte b0 = this.dataTracker.get(CLIMBING);
        if (climbing) {
            b0 = (byte) (b0 | 1);
        } else {
            b0 = (byte) (b0 & -2);
        }

        this.dataTracker.set(CLIMBING, b0);
    }

    public Direction getAttachmentFacing() {
        return this.dataTracker.get(ATTACHED_FACE);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new TemptGoal(this, 1.0D, Ingredient.fromTag(AMTagRegistry.BANANA_SLUG_BREEDABLES), false));
        this.goalSelector.add(3, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(4, new AnimalAIWanderRanged(this, 40, 1.0D, 10, 7));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 5.0F));
        this.goalSelector.add(6, new LookAroundGoal(this));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 4.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.1F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    public void tick(){
        super.tick();
        this.prevTrailYaw = trailYaw;
        this.bodyYaw = MathHelper.stepUnwrappedAngleTowards(this.prevBodyYaw, bodyYaw, getMaxHeadRotation());
        this.trailYaw = MathHelper.stepUnwrappedAngleTowards(this.trailYaw, bodyYaw, 2);
        this.prevTrailVisability = trailVisability;
        prevAttachChangeProgress = attachChangeProgress;
        boolean showTrail = isTrailVisible() && this.getVelocity().length() > 0.03F;
        if (this.prevAttachDir != this.getAttachmentFacing()) {
            if (attachChangeProgress < 5.0F) {
                attachChangeProgress += 1F;
                this.trailYaw = bodyYaw;
            } else if (attachChangeProgress >= 5.0F) {
                this.prevAttachDir = this.getAttachmentFacing();
            }
        } else {
            this.attachChangeProgress = 5.0F;
        }

        if(trailVisability < 1.0F && showTrail){
            trailVisability = Math.min(1.0F, trailVisability + 0.1F);
        }
        if(trailVisability > 0.0F && !showTrail){
            float dec = this.getVelocity().length() > 0.03F ? 1.0F : 0.1F;
            trailVisability = Math.max(0.0F, trailVisability - dec);
        }

        Vec3d vector3d = this.getVelocity();
        if (!this.getWorld().isClient) {
            this.setBesideClimbableBlock(this.horizontalCollision);
            this.setBesideClimbableBlock(this.horizontalCollision || this.verticalCollision && !this.isOnGround());
            if (this.isOnGround() || this.isInsideWaterOrBubbleColumn() || this.isInLava()) {
                this.dataTracker.set(ATTACHED_FACE, Direction.DOWN);
            } else  if (this.verticalCollision) {
                this.dataTracker.set(ATTACHED_FACE, Direction.UP);
            }else {
                var closestDirection = Direction.DOWN;
                double closestDistance = 100;
                for (var dir : POSSIBLE_DIRECTIONS) {
                    var antPos = new BlockPos(MathHelper.floor(this.getX()), MathHelper.floor(this.getY()), MathHelper.floor(this.getZ()));
                    var offsetPos = antPos.offset(dir);
                    var offset = Vec3d.ofCenter(offsetPos);
                    if (closestDistance > this.getPos().distanceTo(offset) && getWorld().isDirectionSolid(offsetPos, this, dir.getOpposite())) {
                        closestDistance = this.getPos().distanceTo(offset);
                        closestDirection = dir;
                    }
                }
                this.dataTracker.set(ATTACHED_FACE, closestDirection);
            }
        }
        boolean flag = false;
        if (this.getAttachmentFacing() != Direction.DOWN) {
            if(this.getAttachmentFacing() == Direction.UP){
                this.setVelocity(this.getVelocity().add(0, 1, 0));
            }else{
                if (!this.horizontalCollision && this.getAttachmentFacing() != Direction.UP) {
                    var vec = Vec3d.of(this.getAttachmentFacing().getVector());
                    this.setVelocity(this.getVelocity().add(vec.normalize().multiply(0.1F, 0.1F, 0.1F)));
                }
                if (!this.isOnGround() && vector3d.y < 0.0D) {
                    this.setVelocity(this.getVelocity().multiply(1.0D, 0.5D, 1.0D));
                    flag = true;
                }
            }
        }
        if(this.getAttachmentFacing() == Direction.UP) {
            this.setNoGravity(true);
            this.setVelocity(vector3d.multiply(0.7D, 1D, 0.7D));
        }else{
            this.setNoGravity(false);
        }
        if (!flag) {
            if (this.isClimbing()) {
                this.setVelocity(vector3d.multiply(1.0D, 0.4D, 1.0D));
            }
        }
        if (!this.getWorld().isClient && this.isAlive() && !this.isBaby() && --this.timeUntilSlime <= 0) {
            this.dropItem(AMItemRegistry.BANANA_SLUG_SLIME.get());
            this.timeUntilSlime = this.random.nextInt(12000) + 24000;
        }
    }

    @Override
    protected float getJumpVelocity() {
        return super.getJumpVelocity();
    }

    @Override
    public int getMaxLookPitchChange() {
        return 1;
    }

    @Override
    public int getMaxHeadRotation() {
        return 4;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    private boolean isTrailVisible() {
        if(this.isInsideWaterOrBubbleColumn()){
            return false;
        }
        if(this.isOnGround()){
            var modelBack = new Vec3d(0, -0.1F, isBaby() ? -0.35F : -0.7F).rotateY(-this.trailYaw * MathHelper.RADIANS_PER_DEGREE);
            var slugBack = this.getPos().add(modelBack);
            var backPos = AMBlockPos.fromVec3(slugBack);
            var state = getWorld().getBlockState(backPos);
            var shape = state.getCollisionShape(getWorld(), backPos);
            if(shape.isEmpty()){
                return false;
            }else{
                var closest = shape.getClosestPointTo(modelBack.add(0, 1, 0));
                return closest.isPresent() && Math.min((float)closest.get().y, 1.0F) >= 0.8F;
            }
        }else if(this.getAttachmentFacing().getAxis() != Direction.Axis.Y){
            var pos = this.getBlockPos().offset(this.getAttachmentFacing()).up(this.getVelocity().y <= -0.001F ? 1 : -1);
            var state = getWorld().getBlockState(pos);
            var shape = state.getCollisionShape(getWorld(), pos);
            return !shape.isEmpty();
        }
        return this.getAttachmentFacing() != Direction.DOWN;
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> TrackedData) {
        super.onTrackedDataSet(TrackedData);
        if (ATTACHED_FACE.equals(TrackedData)) {
            this.prevAttachChangeProgress = 0.0F;
            this.attachChangeProgress = 0.0F;
        }
    }

    @Override
    public void updateLimbs(boolean flying) {
        float f1 = (float)MathHelper.magnitude(this.getX() - this.prevX, 0.5F * (this.getY() - this.prevY), this.getZ() - this.prevZ);
        float f2 = Math.min(f1 * 16.0F, 1.0F);
        this.limbAnimator.updateLimbs(f2, 0.4F);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld level, PassiveEntity mob) {
        var slug = AMEntityRegistry.BANANA_SLUG.get().create(getWorld());
        slug.setVariant(this.getVariant());
        return slug;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putInt("Variant", this.getVariant());
        compound.putInt("SlimeTime", this.timeUntilSlime);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        if (compound.contains("SlimeTime")) {
            this.timeUntilSlime = compound.getInt("SlimeTime");
        }
        this.setVariant(compound.getInt("Variant"));
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int i) {
        this.dataTracker.set(VARIANT, i);
    }
}
