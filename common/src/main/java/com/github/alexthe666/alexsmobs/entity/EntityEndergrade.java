package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.DirectPathNavigator;
import com.github.alexthe666.alexsmobs.entity.ai.EndergradeAIBreakFlowers;
import com.github.alexthe666.alexsmobs.entity.ai.EndergradeAITargetItems;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class EntityEndergrade extends AnimalEntity implements Flutterer {

    private static final TrackedData<Integer> BITE_TICK = DataTracker.registerData(EntityEndergrade.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> SADDLED = DataTracker.registerData(EntityEndergrade.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float tartigradePitch = 0;
    public float prevTartigradePitch = 0;
    public float biteProgress = 0;
    public float prevBiteProgress = 0;
    public boolean stopWandering = false;
    public boolean hasItemTarget = false;

    public EntityEndergrade(EntityType type, World worldIn) {
        super(type, worldIn);
        this.moveControl = new EntityEndergrade.MoveHelperController(this);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20D)
                .add(EntityAttributes.GENERIC_ARMOR, 0.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.15F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    public static boolean canEndergradeSpawn(EntityType<? extends AnimalEntity> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return !worldIn.getBlockState(pos.down()).isAir();
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new DirectPathNavigator(this, worldIn);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Saddled", this.isSaddled());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setSaddled(compound.getBoolean("Saddled"));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(BITE_TICK, 0);
        this.dataTracker.startTracking(SADDLED, false);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new EndergradeAIBreakFlowers(this));
        this.goalSelector.add(2, new AnimalMateGoal(this, 1.2D) {
            @Override
            public void start() {
                super.start();
                EntityEndergrade.this.stopWandering = true;
            }

            @Override
            public void stop() {
                super.stop();
                EntityEndergrade.this.stopWandering = false;
            }
        });
        this.goalSelector.add(3, new TemptGoal(this, 1.1D, Ingredient.fromTag(AMTagRegistry.ENDERGRADE_BREEDABLES), false) {
            @Override
            public void start() {
                super.start();
                EntityEndergrade.this.stopWandering = true;
            }

            @Override
            public void stop() {
                super.stop();
                EntityEndergrade.this.stopWandering = false;
            }
        });
        this.goalSelector.add(4, new RandomFlyGoal(this));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 10));
        this.goalSelector.add(5, new LookAroundGoal(this));
        this.targetSelector.add(1, new EndergradeAITargetItems<>(this, true));
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        for (Entity passenger : this.getPassengerList()) {
            if (passenger instanceof PlayerEntity player) {
                if (player.getMainHandStack().isIn(AMTagRegistry.ENDERGRADE_FOLLOWS) || player.getOffHandStack().isIn(AMTagRegistry.ENDERGRADE_FOLLOWS)) {
                    return player;
                }
            }
        }
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.ENDERGRADE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.ENDERGRADE_HURT.get();
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        var itemstack = player.getStackInHand(hand);
        var item = itemstack.getItem();
        if (item == Items.SADDLE && !this.isSaddled()) {
            if (!player.isCreative()) {
                itemstack.decrement(1);
            }
            this.setSaddled(true);
            return ActionResult.SUCCESS;
        }
        if (itemstack.isIn(AMTagRegistry.ENDERGRADE_BREEDABLES) && this.hasStatusEffect(AMEffectRegistry.ENDER_FLU.get())) {
            if (!player.isCreative()) {
                itemstack.decrement(1);
            }
            this.heal(8);
            this.removeStatusEffect(AMEffectRegistry.ENDER_FLU.get());
            return ActionResult.SUCCESS;
        }
        ActionResult type = super.interactMob(player, hand);
        if (type != ActionResult.SUCCESS && !isBreedingItem(itemstack)) {
            if (!player.isSneaking() && this.isSaddled()) {
                player.startRiding(this);
                return ActionResult.SUCCESS;
            }
        }
        return type;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.ENDERGRADE_BREEDABLES);
    }

    @Override
    public void updatePassengerPosition(Entity passenger, Entity.PositionUpdater moveFunc) {
        if (this.hasPassenger(passenger)) {
            float radius = -0.25F;
            float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
            double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            double extraZ = radius * MathHelper.cos(angle);
            passenger.setPos(this.getX() + extraX, this.getY() + this.getMountedHeightOffset() + passenger.getHeightOffset(), this.getZ() + extraZ);
        }
    }

    @Override
    public double getMountedHeightOffset() {
        float f = Math.min(0.25F, this.limbAnimator.getSpeed());
        float f1 = this.limbAnimator.getPos();
        return (double) this.getHeight() - 0.1D + (double) (0.12F * MathHelper.cos(f1 * 0.7F) * 0.7F * f);
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }

    public boolean isSaddled() {
        return this.dataTracker.get(SADDLED);
    }

    public void setSaddled(boolean saddled) {
        this.dataTracker.set(SADDLED, saddled);
    }

    @Override
    public void tick() {
        super.tick();
        prevTartigradePitch = this.tartigradePitch;
        prevBiteProgress = this.biteProgress;
        float f2 = (float) -((float) this.getVelocity().y * 3 * (double) MathHelper.DEGREES_PER_RADIAN);
        this.tartigradePitch = f2;
        if (this.getVelocity().lengthSquared() > 0.005F) {
            float angleMotion = (Maths.STARTING_ANGLE * this.bodyYaw);
            double extraXMotion = -0.2F * MathHelper.sin((float) (Math.PI + angleMotion));
            double extraZMotion = -0.2F * MathHelper.cos(angleMotion);
            this.getWorld().addParticle(ParticleTypes.END_ROD, this.getParticleX(0.5D), this.getY() + 0.3, this.getParticleZ(0.5D), extraXMotion, 0D, extraZMotion);
        }
        int tick = this.dataTracker.get(BITE_TICK);
        if (tick > 0) {
            this.dataTracker.set(BITE_TICK, tick - 1);
            this.biteProgress++;
        } else if (biteProgress > 0) {
            biteProgress--;
        }
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.ARTHROPOD;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    private BlockPos getGroundPosition(BlockPos radialPos) {
        while (radialPos.getY() > 1 && getWorld().isAir(radialPos)) {
            radialPos = radialPos.down();
        }
        if (radialPos.getY() <= 1) {
            return new BlockPos(radialPos.getX(), getWorld().getSeaLevel(), radialPos.getZ());
        }
        return radialPos;
    }

    public boolean isTargetBlocked(Vec3d target) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        return this.getWorld().raycast(new RaycastContext(Vector3d, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() != HitResult.Type.MISS;
    }

    public boolean canTargetItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.ENDERGRADE_FOODSTUFFS);
    }

    public void onGetItem(ItemEntity targetEntity) {
        this.emitGameEvent(GameEvent.EAT);
        this.playSound(SoundEvents.ENTITY_CAT_EAT, this.getSoundVolume(), this.getSoundPitch());
        this.heal(5);
    }

    public void bite() {
        this.dataTracker.set(BITE_TICK, 5);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.endergradeSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.ENDERGRADE.get().create(world);
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if (this.isSaddled()) {
            if (!this.getWorld().isClient) {
                this.dropItem(Items.SADDLE);
            }
        }
    }

    @Override
    protected Vec3d getControlledMovementInput(PlayerEntity player, Vec3d deltaIn) {
        if (player.forwardSpeed != 0) {
            this.setSprinting(true);
            var lookVec = player.getRotationVector();
            if (player.forwardSpeed < 0) {
                lookVec = lookVec.rotateY(MathHelper.PI);
            }
            double y = lookVec.y * 0.35F;
            return new Vec3d(player.sidewaysSpeed, y, player.forwardSpeed);
        } else {
            this.setSprinting(false);
        }
        return Vec3d.ZERO;
    }

    @Override
    protected void tickControlled(PlayerEntity player, Vec3d vec3) {
        super.tickControlled(player, vec3);
        if(player.forwardSpeed != 0 || player.sidewaysSpeed != 0){
            this.setRotation(player.getYaw(), player.getPitch() * 0.25F);
            this.prevYaw = this.bodyYaw = this.headYaw= this.getYaw();
            this.setStepHeight(1);
            this.getNavigation().stop();
            this.setTarget(null);
            this.setSprinting(true);
        }
    }

    @Override
    protected float getSaddledSpeed(PlayerEntity rider) {
        return (float)(this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * (this.isOnGround() ? 0.2F : 0.8F));
    }

    @Override
    public boolean isInAir() {
        return true;
    }

    static class RandomFlyGoal extends Goal {
        private final EntityEndergrade parentEntity;
        private BlockPos target = null;

        public RandomFlyGoal(EntityEndergrade mosquito) {
            this.parentEntity = mosquito;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            var movementcontroller = this.parentEntity.getMoveControl();
            if (parentEntity.stopWandering || parentEntity.hasItemTarget) {
                return false;
            }
            if (!movementcontroller.isMoving() || target == null) {
                target = getBlockInViewEndergrade();
                if (target != null) {
                    this.parentEntity.getMoveControl().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 1.0D);
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return target != null && !parentEntity.stopWandering && !parentEntity.hasItemTarget && parentEntity.squaredDistanceTo(Vec3d.ofCenter(target)) > 2.4D && parentEntity.getMoveControl().isMoving() && !parentEntity.horizontalCollision;
        }

        @Override
        public void stop() {
            target = null;
        }

        @Override
        public void tick() {
            if (target == null) {
                target = getBlockInViewEndergrade();
            }
            if (target != null) {
                this.parentEntity.getMoveControl().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 1.0D);
                if (parentEntity.squaredDistanceTo(Vec3d.ofCenter(target)) < 2.5F) {
                    target = null;
                }
            }
        }

        public BlockPos getBlockInViewEndergrade() {
            float radius = 1 + parentEntity.getRandom().nextInt(5);
            float neg = parentEntity.getRandom().nextBoolean() ? 1 : -1;
            float renderYawOffset = parentEntity.bodyYaw;
            float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (parentEntity.getRandom().nextFloat() * neg);
            double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            double extraZ = radius * MathHelper.cos(angle);
            var radialPos = AMBlockPos.fromCoords(parentEntity.getX() + extraX, parentEntity.getY() + 2, parentEntity.getZ() + extraZ);
            var ground = parentEntity.getGroundPosition(radialPos);
            var newPos = ground.up(1 + parentEntity.getRandom().nextInt(6));
            if (!parentEntity.isTargetBlocked(Vec3d.ofCenter(newPos)) && parentEntity.squaredDistanceTo(Vec3d.ofCenter(newPos)) > 6) {
                return newPos;
            }
            return null;
        }
    }

    static class MoveHelperController extends MoveControl {
        private final EntityEndergrade parentEntity;

        public MoveHelperController(EntityEndergrade sunbird) {
            super(sunbird);
            this.parentEntity = sunbird;
        }

        @Override
        public void tick() {
            if (this.state == State.STRAFE) {
                Vec3d vector3d = new Vec3d(this.targetX - parentEntity.getX(), this.targetY - parentEntity.getY(), this.targetZ - parentEntity.getZ());
                double d0 = vector3d.length();
                parentEntity.setVelocity(parentEntity.getVelocity().add(0, vector3d.multiply(this.speed * 0.05D / d0).y, 0));
                float f = (float) this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                float f1 = (float) this.speed * f;

                this.forwardMovement = 1.0F;
                this.sidewaysMovement = 0.0F;

                this.entity.setMovementSpeed(f1);
                this.entity.setForwardSpeed(this.forwardMovement);
                this.entity.setSidewaysSpeed(this.sidewaysMovement);
                this.state = MoveControl.State.WAIT;
            } else if (this.state == MoveControl.State.MOVE_TO) {
                Vec3d vector3d = new Vec3d(this.targetX - parentEntity.getX(), this.targetY - parentEntity.getY(), this.targetZ - parentEntity.getZ());
                double d0 = vector3d.length();
                if (d0 < parentEntity.getBoundingBox().getAverageSideLength()) {
                    this.state = MoveControl.State.WAIT;
                    parentEntity.setVelocity(parentEntity.getVelocity().multiply(0.5D));
                } else {
                    double localSpeed = this.speed;
                    if (parentEntity.hasPassengers()) {
                        localSpeed *= 1.5D;
                    }
                    parentEntity.setVelocity(parentEntity.getVelocity().add(vector3d.multiply(localSpeed * 0.005D / d0)));
                    if (parentEntity.getTarget() == null) {
                        Vec3d vector3d1 = parentEntity.getVelocity();
                        parentEntity.setYaw(-((float) MathHelper.atan2(vector3d1.x, vector3d1.z)) * MathHelper.DEGREES_PER_RADIAN);
                        parentEntity.bodyYaw = parentEntity.getYaw();
                    } else {
                        double d2 = parentEntity.getTarget().getX() - parentEntity.getX();
                        double d1 = parentEntity.getTarget().getZ() - parentEntity.getZ();
                        parentEntity.setYaw(-((float) MathHelper.atan2(d2, d1)) * MathHelper.DEGREES_PER_RADIAN);
                        parentEntity.bodyYaw = parentEntity.getYaw();
                    }
                }

            }
        }

        private boolean canReach(Vec3d p_220673_1_, int p_220673_2_) {
            var axisalignedbb = this.parentEntity.getBoundingBox();

            for (int i = 1; i < p_220673_2_; ++i) {
                axisalignedbb = axisalignedbb.offset(p_220673_1_);
                if (!this.parentEntity.getWorld().isSpaceEmpty(this.parentEntity, axisalignedbb)) {
                    return false;
                }
            }

            return true;
        }
    }
}
