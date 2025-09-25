package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.StraddlerAIShoot;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.google.common.collect.Sets;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;

import java.util.Set;

public class EntityStraddler extends HostileEntity implements IAnimatedEntity {

    public static final Animation ANIMATION_LAUNCH = Animation.create(30);
    private static final TrackedData<Integer> STRADPOLE_COUNT = DataTracker.registerData(EntityStraddler.class, TrackedDataHandlerRegistry.INTEGER);
    private int animationTick;
    private Animation currentAnimation;

    public EntityStraddler(EntityType<EntityStraddler> type, World world) {
        super(type, world);
        this.setPathfindingPenalty(PathNodeType.LAVA, 0.0F);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, 0.0F);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, 0.0F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.STRADDLER_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.STRADDLER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.STRADDLER_HURT.get();
    }

    public static boolean canStraddlerSpawn(EntityType animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        boolean spawnBlock = worldIn.getBlockState(pos.down()).isIn(BlockTags.BASE_STONE_NETHER);
        return spawnBlock;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 28.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8D)
                .add(EntityAttributes.GENERIC_ARMOR, 5.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3F);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(STRADPOLE_COUNT, 0);
    }

    public int getStradpoleCount() {
        return this.dataTracker.get(STRADPOLE_COUNT);
    }

    public void setStradpoleCount(int index) {
        this.dataTracker.set(STRADPOLE_COUNT, index);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.straddlerSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new StraddlerAIShoot(this, 0.5F, 30, 16));
        this.goalSelector.add(7, new WanderAroundGoal(this, 1.0D, 60));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.goalSelector.add(9, new LookAtEntityGoal(this, StriderEntity.class, 8.0F));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, true));
    }

    @Override
    protected void fall(double p_184231_1_, boolean p_184231_3_, BlockState p_184231_4_, BlockPos p_184231_5_) {
        this.checkBlockCollision();
        if (this.isInLava()) {
            this.fallDistance = 0.0F;
        } else {
            super.fall(p_184231_1_, p_184231_3_, p_184231_4_, p_184231_5_);
        }
    }

    @Override
    public void travel(Vec3d travelVector) {
        this.setMovementSpeed((float) this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * (this.getAnimation() == ANIMATION_LAUNCH ? 0.5F : 1F) * (isInLava() ? 0.2F : 1F));
        if (this.canMoveVoluntarily() && (this.isTouchingWater() || this.isInLava())) {
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

    private void floatStrider() {
        if (this.isInLava()) {
            var lvt_1_1_ = ShapeContext.of(this);
            double d1 = this.getFluidHeight(FluidTags.LAVA);
            if(d1 <= 0.5F && d1 > 0){
                if(this.getVelocity().y < 0){
                    this.setVelocity(this.getVelocity().multiply(1, 0, 1));
                }
                this.setOnGround(true);
            }else if (lvt_1_1_.isAbove(FluidBlock.COLLISION_SHAPE, this.getBlockPos().down(), true) && !this.getWorld().getFluidState(this.getBlockPos().up()).isIn(FluidTags.LAVA)) {
                this.setOnGround(true);
            } else {
                this.setVelocity(0, Math.min((d1 - 0.5F), 1) * 0.2F, 0);
            }
        }

    }

    @Override
    public boolean canSpawn(WorldView worldIn) {
        return worldIn.doesNotIntersectEntities(this);
    }

    @Override
    protected float calculateNextStepSoundDistance() {
        return this.distanceTraveled + 0.6F;
    }

    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView worldIn) {
        if (worldIn.getBlockState(pos).getFluidState().isIn(FluidTags.LAVA)) {
            return 10.0F;
        } else {
            return this.isInLava() ? Float.NEGATIVE_INFINITY : 0.0F;
        }
    }

    @Override
    public Vec3d updatePassengerForDismount(LivingEntity livingEntity) {
        var avector3d = new Vec3d[]{
                getPassengerDismountOffset(this.getWidth(), livingEntity.getWidth(), livingEntity.getYaw()),
                getPassengerDismountOffset(this.getWidth(), livingEntity.getWidth(), livingEntity.getYaw() - 22.5F),
                getPassengerDismountOffset(this.getWidth(), livingEntity.getWidth(), livingEntity.getYaw() + 22.5F),
                getPassengerDismountOffset(this.getWidth(), livingEntity.getWidth(), livingEntity.getYaw() - 45.0F),
                getPassengerDismountOffset(this.getWidth(), livingEntity.getWidth(), livingEntity.getYaw() + 45.0F)};
        Set<BlockPos> set = Sets.newLinkedHashSet();
        double d0 = this.getBoundingBox().maxY;
        double d1 = this.getBoundingBox().minY - 0.5D;
        var blockpos$mutable = new BlockPos.Mutable();

        for (var vector3d : avector3d) {
            blockpos$mutable.set(this.getX() + vector3d.x, d0, this.getZ() + vector3d.z);

            for (double d2 = d0; d2 > d1; --d2) {
                set.add(blockpos$mutable.toImmutable());
                blockpos$mutable.move(Direction.DOWN);
            }
        }

        for (BlockPos blockpos : set) {
            if (!this.getWorld().getFluidState(blockpos).isIn(FluidTags.LAVA)) {
                double d3 = this.getWorld().getDismountHeight(blockpos);
                if (Dismounting.canDismountInBlock(d3)) {
                    Vec3d vector3d1 = Vec3d.ofCenter(blockpos, d3);

                    for (var pose : livingEntity.getPoses()) {
                        var axisalignedbb = livingEntity.getBoundingBox(pose);
                        if (Dismounting.canPlaceEntityAt(this.getWorld(), livingEntity, axisalignedbb.offset(vector3d1))) {
                            livingEntity.setPose(pose);
                            return vector3d1;
                        }
                    }
                }
            }
        }

        return new Vec3d(this.getX(), this.getBoundingBox().maxY, this.getZ());
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public boolean canWalkOnFluid(FluidState p_230285_1_) {
        return p_230285_1_.isIn(FluidTags.LAVA);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putInt("StradpoleCount", getStradpoleCount());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setStradpoleCount(compound.getInt("StradpoleCount"));
    }

    @Override
    public void tick() {
        super.tick();
        this.floatStrider();
        this.checkBlockCollision();
        if (this.getAnimation() == ANIMATION_LAUNCH && this.isAlive()){
            if(this.getAnimationTick() == 2){
                this.playSound(SoundEvents.ITEM_CROSSBOW_LOADING_MIDDLE, 2F, 1F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            }
        }
        if (this.getAnimation() == ANIMATION_LAUNCH && this.isAlive() && this.getAnimationTick() == 20 && this.getTarget() != null) {
            var pole = AMEntityRegistry.STRADPOLE.get().create(getWorld());
            pole.setParentId(this.getUuid());
            pole.setPos(this.getX(), this.getEyeY(), this.getZ());
            final double d0 = this.getTarget().getEyeY() - (double)1.1F;
            final double d1 = this.getTarget().getX() - this.getX();
            final double d2 = d0 - pole.getY();
            final double d3 = this.getTarget().getZ() - this.getZ();
            final float f3 = MathHelper.sqrt((float) (d1 * d1 + d2 * d2 + d3 * d3)) * 0.2F;
            this.emitGameEvent(GameEvent.PROJECTILE_SHOOT);
            this.playSound(SoundEvents.ITEM_CROSSBOW_LOADING_END, 2F, 1F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            pole.shoot(d1, d2 + (double)f3, d3, 2F, 0F);
            pole.setYaw(this.getYaw() % 360.0F);
            pole.setPitch(MathHelper.clamp(this.getYaw(), -90.0F, 90.0F) % 360.0F);
            if(!this.getWorld().isClient){
                this.getWorld().spawnEntity(pole);
            }
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
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
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int i) {
        animationTick = i;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_LAUNCH};
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new LavaPathNavigator(this, worldIn);
    }

    public boolean shouldShoot() {
        return true;
    }

    static class LavaPathNavigator extends MobNavigation {
        LavaPathNavigator(EntityStraddler p_i231565_1_, World p_i231565_2_) {
            super(p_i231565_1_, p_i231565_2_);
        }

        protected PathNodeNavigator createPathNodeNavigator(int p_179679_1_) {
            this.nodeMaker = new LandPathNodeMaker();
            return new PathNodeNavigator(this.nodeMaker, p_179679_1_);
        }

        @Override
        protected boolean canWalkOnPath(PathNodeType p_230287_1_) {
            return p_230287_1_ == PathNodeType.LAVA || p_230287_1_ == PathNodeType.DAMAGE_FIRE || p_230287_1_ == PathNodeType.DANGER_FIRE || super.canWalkOnPath(p_230287_1_);
        }

        public boolean isValidPosition(BlockPos pos) {
            return this.world.getBlockState(pos).isOf(Blocks.LAVA) || super.isValidPosition(pos);
        }
    }
}
