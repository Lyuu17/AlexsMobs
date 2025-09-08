package com.github.alexthe666.alexsmobs.entity;


import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.google.common.base.Predicate;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.BreatheAirGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EntityBoneSerpent extends HostileEntity {

    private static final TrackedData<Optional<UUID>> CHILD_UUID = DataTracker.registerData(EntityBoneSerpent.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final Predicate<LivingEntity> NOT_RIDING_STRADDLEBOARD_FRIENDLY = (entity) -> entity.isAlive() && (entity.getVehicle() == null || !(entity.getVehicle() instanceof EntityStraddleboard) || !((EntityStraddleboard)entity.getVehicle()).shouldSerpentFriend());;
    private static final Predicate<EntityStraddleboard> STRADDLEBOARD_FRIENDLY = (entity) -> entity.hasPassengers() && entity.shouldSerpentFriend();;

    public int jumpCooldown = 0;
    private boolean isLandNavigator;
    private int boardCheckCooldown = 0;
    private EntityStraddleboard boardToBoast = null;

    public EntityBoneSerpent(EntityType<? extends EntityBoneSerpent> type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.LAVA, 0.0F);
        switchNavigator(false);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.boneSeprentSpawnRolls, this.getRandom(), spawnReasonIn) && super.canSpawn(worldIn, spawnReasonIn);
    }

    @Override
    public int getLimitPerChunk() {
        return 1;
    }

    @Override
    public boolean spawnsTooManyForEachTry(int sizeIn) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.BONE_SERPENT_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.BONE_SERPENT_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.BONE_SERPENT_HURT.get();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 25.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 1.45F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    public int getSafeFallDistance() {
        return 256;
    }

    @Override
    public boolean canHaveStatusEffect(StatusEffectInstance potioneffectIn) {
        if (potioneffectIn.getEffectType() == StatusEffects.WITHER) {
            return false;
        }
        return super.canHaveStatusEffect(potioneffectIn);
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.UNDEAD;
    }

    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView worldIn) {
        if (worldIn.getBlockState(pos).getFluidState().isIn(FluidTags.WATER) || worldIn.getBlockState(pos).getFluidState().isIn(FluidTags.LAVA)) {
            return 10.0F;
        } else {
            return this.isInLava() ? Float.NEGATIVE_INFINITY : 0.0F;
        }
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Override
    public boolean canBeLeashedBy(PlayerEntity player) {
        return true;
    }

    @Override
    public boolean canSpawn(WorldView worldIn) {
        return worldIn.doesNotIntersectEntities(this);
    }

    public static boolean canBoneSerpentSpawn(EntityType<EntityBoneSerpent> p_234314_0_, WorldAccess p_234314_1_, SpawnReason p_234314_2_, BlockPos p_234314_3_, Random p_234314_4_) {
        var blockpos$mutable = p_234314_3_.mutableCopy();

        do {
            blockpos$mutable.move(Direction.UP);
        } while(p_234314_1_.getFluidState(blockpos$mutable).isIn(FluidTags.LAVA));

        return p_234314_1_.getBlockState(blockpos$mutable).isAir();
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new BreatheAirGoal(this));
        this.goalSelector.add(0, new BoneSerpentAIFindLava(this));
        this.goalSelector.add(1, new BoneSerpentAIMeleeJump(this));
        this.goalSelector.add(2, new BoneSerpentAIJump(this, 10));
        this.goalSelector.add(3, new LavaAndWaterAIRandomSwimming(this, 1.0D, 8));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.targetSelector.add(1, (new RevengeGoal(this)).setGroupRevenge());
        if(!AMConfig.neutralBoneSerpents){
            this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, PlayerEntity.class, 10, true, false, NOT_RIDING_STRADDLEBOARD_FRIENDLY));
            this.targetSelector.add(3, new EntityAINearestTarget3D<>(this, MerchantEntity.class, 10, true, false, NOT_RIDING_STRADDLEBOARD_FRIENDLY));
        }
        this.targetSelector.add(4, new EntityAINearestTarget3D<>(this, WitherSkeletonEntity.class, 10, true, false, NOT_RIDING_STRADDLEBOARD_FRIENDLY));
        this.targetSelector.add(5, new EntityAINearestTarget3D<>(this, EntitySoulVulture.class, 10, true, false, NOT_RIDING_STRADDLEBOARD_FRIENDLY));
    }

    @Override
    public void travel(Vec3d travelVector) {
        boolean liquid = this.isInLava() || this.isTouchingWater();
        if (this.canMoveVoluntarily() && liquid) {
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

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = this.createNavigation(getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new BoneSerpentMoveController(this);
            this.navigation = new BoneSerpentPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        if (this.getChildId() != null) {
            compound.putUuid("ChildUUID", this.getChildId());
        }
    }

    @Override
    public void tickCramming() {
        final List<Entity> entities = this.getWorld().getOtherEntities(this, this.getBoundingBox().stretch(0.2D, 0.0D, 0.2D));
        entities.stream().filter(entity -> !(entity instanceof EntityBoneSerpentPart) && entity.isPushable()).forEach(entity -> entity.pushAwayFrom(this));
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.FALL) || source.isOf(DamageTypes.DROWN) || source.isOf(DamageTypes.IN_WALL)  || source.isOf(DamageTypes.LAVA) || source.isIn(DamageTypeTags.IS_FIRE) || super.isInvulnerableTo(source);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        if (compound.containsUuid("ChildUUID")) {
            this.setChildId(compound.getUuid("ChildUUID"));
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CHILD_UUID, Optional.empty());
    }

    @Nullable
    public UUID getChildId() {
        return this.dataTracker.get(CHILD_UUID).orElse(null);
    }

    public void setChildId(@Nullable UUID uniqueId) {
        this.dataTracker.set(CHILD_UUID, Optional.ofNullable(uniqueId));
    }

    public Entity getChild() {
        UUID id = getChildId();
        if (id != null && !this.getWorld().isClient) {
            return ((ServerWorld) getWorld()).getEntity(id);
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        inNetherPortal = false;
        final boolean ground = !this.isInLava() && !this.isTouchingWater() && this.isOnGround();
        if (jumpCooldown > 0) {
            jumpCooldown--;
            final float f2 = -((float) this.getVelocity().y * MathHelper.DEGREES_PER_RADIAN);
            this.setPitch(f2);

        }
        if (ground) {
            if (!this.isLandNavigator)
                switchNavigator(true);
        } else {
            if (this.isLandNavigator)
                switchNavigator(false);
        }

        if (!this.getWorld().isClient) {
            final Entity child = getChild();
            if (child == null) {
                LivingEntity partParent = this;
                final int segments = 7 + getRandom().nextInt(8);
                for (int i = 0; i < segments; i++) {
                    EntityBoneSerpentPart part = new EntityBoneSerpentPart(AMEntityRegistry.BONE_SERPENT_PART.get(), partParent, 0.9F, 180, 0);
                    part.setParent(partParent);
                    part.setBodyIndex(i);
                    if (partParent == this) {
                        this.setChildId(part.getUuid());
                    }
                    part.setInitialPartPos(this);
                    partParent = part;
                    if (i == segments - 1) {
                        part.setTail(true);
                    }
                    getWorld().spawnEntity(part);
                }
            }

            if (boardCheckCooldown <= 0) {
                boardCheckCooldown = 100 + random.nextInt(150);
                final var list = this.getWorld().getEntitiesByClass(EntityStraddleboard.class, this.getBoundingBox().expand(100, 15, 100), STRADDLEBOARD_FRIENDLY);
                EntityStraddleboard closestBoard = null;
                for (final EntityStraddleboard board : list) {
                    if (closestBoard == null || this.distanceTo(closestBoard) > this.distanceTo(board)) {
                        closestBoard = board;
                    }
                }
                boardToBoast = closestBoard;
            } else {
                boardCheckCooldown--;
            }

            if (boardToBoast != null) {
                if (this.distanceTo(boardToBoast) > 200) {
                    boardToBoast = null;
                } else {
                    if (jumpCooldown == 0 && (this.isInLava() || this.isTouchingWater()) && this.distanceTo(boardToBoast) < 15) {
                        final float up = 0.7F + this.getRandom().nextFloat() * 0.8F;
                        final Vec3d vector3d1 = this.getRotationVector();
                        this.setVelocity(this.getVelocity().add(vector3d1.x * 0.6D, up, vector3d1.y * 0.6D));
                        this.getNavigation().stop();
                        this.jumpCooldown = this.getRandom().nextInt(300) + 100;
                    }
                    if (this.distanceTo(boardToBoast) > 5) {
                        this.getNavigation().startMovingTo(boardToBoast, 1.5F);
                    } else {
                        this.getNavigation().stop();
                    }
                }
            }
        }
    }

    @Override
    public boolean canBreatheInWater() {
        return true;
    }

    static class BoneSerpentMoveController extends MoveControl {
        private final EntityBoneSerpent dolphin;

        public BoneSerpentMoveController(EntityBoneSerpent dolphinIn) {
            super(dolphinIn);
            this.dolphin = dolphinIn;
        }

        public void tick() {
            if (this.dolphin.isTouchingWater() || this.dolphin.isInLava()) {
                this.dolphin.setVelocity(this.dolphin.getVelocity().add(0.0D, 0.005D, 0.0D));
            }

            if (this.state == MoveControl.State.MOVE_TO && !this.dolphin.getNavigation().isIdle()) {
                final double d0 = this.targetX - this.dolphin.getX();
                final double d1 = this.targetY - this.dolphin.getY();
                final double d2 = this.targetZ - this.dolphin.getZ();
                final double d3 = d0 * d0 + d1 * d1 + d2 * d2;
                if (d3 < (double) 2.5000003E-7F) {
                    this.entity.setForwardSpeed(0.0F);
                } else {
                    float f = (float) (MathHelper.atan2(d2, d0) * MathHelper.DEGREES_PER_RADIAN) - 90.0F;
                    this.dolphin.setYaw(this.wrapDegrees(this.dolphin.getYaw(), f, 10.0F));
                    this.dolphin.bodyYaw = this.dolphin.getYaw();
                    this.dolphin.headYaw= this.dolphin.getYaw();
                    float f1 = (float) (this.speed * this.dolphin.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
                    if (this.dolphin.isTouchingWater() || this.dolphin.isInLava()) {
                        this.dolphin.setMovementSpeed(f1 * 0.02F);
                        float f2 = -((float) (MathHelper.atan2(d1, MathHelper.sqrt((float)(d0 * d0 + d2 * d2))) * MathHelper.DEGREES_PER_RADIAN));
                        f2 = MathHelper.clamp(MathHelper.wrapDegrees(f2), -85.0F, 85.0F);
                        this.dolphin.setVelocity(this.dolphin.getVelocity().add(0.0D, (double) this.dolphin.getMovementSpeed() * d1 * 0.6D, 0.0D));
                        this.dolphin.setPitch(this.wrapDegrees(this.dolphin.getPitch(), f2, 1.0F));
                        final float f3 = MathHelper.cos(this.dolphin.getPitch() * MathHelper.RADIANS_PER_DEGREE);
                        final float f4 = MathHelper.sin(this.dolphin.getPitch() * MathHelper.RADIANS_PER_DEGREE);
                        this.dolphin.forwardSpeed = f3 * f1;
                        this.dolphin.upwardSpeed = -f4 * f1;
                    } else {
                        this.dolphin.setMovementSpeed(f1 * 0.1F);
                    }

                }
            } else {
                this.dolphin.setMovementSpeed(0.0F);
                this.dolphin.setSidewaysSpeed(0.0F);
                this.dolphin.setUpwardSpeed(0.0F);
                this.dolphin.setForwardSpeed(0.0F);
            }
        }
    }


}
