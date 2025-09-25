package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Flutterer;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class EntitySpectre extends AnimalEntity implements Flutterer {

    private static final TrackedData<Integer> CARDINAL_ORDINAL = DataTracker.registerData(EntitySpectre.class, TrackedDataHandlerRegistry.INTEGER);
    public float birdPitch = 0;
    public float prevBirdPitch = 0;
    public Vec3d lurePos = null;

    public EntitySpectre(EntityType<EntitySpectre> type, World world) {
        super(type, world);
        this.moveControl = new MoveHelperController(this);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.spectreSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean canSpectreSpawn(EntityType<? extends AnimalEntity> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        BlockState blockstate = worldIn.getBlockState(pos.down());
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.SPECTRE_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.SPECTRE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.SPECTRE_HURT.get();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 50.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 1F);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CARDINAL_ORDINAL, Direction.NORTH.getId());
    }

    public int getCardinalInt() {
        return this.dataTracker.get(CARDINAL_ORDINAL);
    }

    public void setCardinalInt(int command) {
        this.dataTracker.set(CARDINAL_ORDINAL, command);
    }

    public Direction getCardinalDirection() {
        return Direction.byId(getCardinalInt());
    }

    public void setCardinalDirection(Direction dir) {
        setCardinalInt(dir.getId());
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new TemptHeartGoal(this, 1.0D, Ingredient.ofItems(AMItemRegistry.SOUL_HEART.get()), false));
        this.goalSelector.add(2, new FlyGoal(this));
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.isOf(DamageTypes.MAGIC) && !source.isOf(DamageTypes.OUT_OF_WORLD) && !source.isSourceCreativePlayer() && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(source);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.setPitch(0.0F);
        this.randomizeDirection();
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);

    }

    public float getBrightness() {
        return 1.0F;
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }

    @Override
    public boolean handleFallDamage(float distance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3d vector3d1 = this.getVelocity();
        this.setYaw( -((float) MathHelper.atan2(vector3d1.x, vector3d1.z)) * MathHelper.DEGREES_PER_RADIAN);
        this.bodyYaw = this.getYaw();

        prevBirdPitch = this.birdPitch;
        this.noClip = true;
        this.birdPitch = (float) -((float) this.getVelocity().y * 0.5F * (double) MathHelper.DEGREES_PER_RADIAN);
        if (this.getHoldingEntity() != null && !(this.getHoldingEntity() instanceof LeashKnotEntity)) {
            var entity = this.getHoldingEntity();
            float f = this.distanceTo(entity);
            if (f > 10) {
                final double d0 = (this.getX() - entity.getX()) / (double) f;
                final double d1 = (this.getY() - entity.getY()) / (double) f;
                final double d2 = (this.getZ() - entity.getZ()) / (double) f;
                entity.setVelocity(entity.getVelocity().add(Math.copySign(d0 * d0 * 0.4D, d0), Math.copySign(d1 * d1 * 0.4D, d1), Math.copySign(d2 * d2 * 0.4D, d2)));
            }
            entity.fallDistance = 0.0F;
            if (entity.getVelocity().y < 0.0D) {
                entity.setVelocity(entity.getVelocity().multiply(1, 0.7F, 1));
            }
            if (entity.isSneaking()) {
                this.detachLeash(true, true);
            }
        }
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    @Override
    protected void updateLeash() {
        if (this.getHoldingEntity() != null) {
            if (this.getHoldingEntity().hasVehicle() || this.getHoldingEntity() instanceof LeashKnotEntity) {
                super.updateLeash();
                return;
            }
            float f = this.distanceTo(this.getHoldingEntity());
            if (f > 30) {
                double lvt_3_1_ = (this.getHoldingEntity().getX() - this.getX()) / (double) f;
                double lvt_5_1_ = (this.getHoldingEntity().getY() - this.getY()) / (double) f;
                double lvt_7_1_ = (this.getHoldingEntity().getZ() - this.getZ()) / (double) f;
                this.setVelocity(this.getVelocity().add(Math.copySign(lvt_3_1_ * lvt_3_1_ * 0.4D, lvt_3_1_), Math.copySign(lvt_5_1_ * lvt_5_1_ * 0.4D, lvt_5_1_), Math.copySign(lvt_7_1_ * lvt_7_1_ * 0.4D, lvt_7_1_)));
            }
        }

        if (this.leashNbt != null) {
            this.readLeashNbt();
        }

        if (this.getHoldingEntity() != null) {
            if (!this.isAlive() || !this.getHoldingEntity().isAlive()) {
                this.detachLeash(true, true);
            }

        }
    }

    private void randomizeDirection() {
        this.setCardinalInt(2 + random.nextInt(3));
    }

    @Override
    public boolean isInAir() {
        return true;
    }

    static class MoveHelperController extends MoveControl {
        private final EntitySpectre parentEntity;

        public MoveHelperController(EntitySpectre sunbird) {
            super(sunbird);
            this.parentEntity = sunbird;
        }

        @Override
        public void tick() {
            if (this.state == MoveControl.State.MOVE_TO) {
                Vec3d vector3d = new Vec3d(this.targetX - parentEntity.getX(), this.targetY - parentEntity.getY(), this.targetZ - parentEntity.getZ());
                double d5 = vector3d.length();
                if (d5 < 0.3) {
                    this.state = MoveControl.State.WAIT;
                    parentEntity.setVelocity(parentEntity.getVelocity().multiply(0.5D));
                } else {
                    double d0 = this.targetX - this.parentEntity.getX();
                    double d1 = this.targetY - this.parentEntity.getY();
                    double d2 = this.targetZ - this.parentEntity.getZ();
                    parentEntity.setVelocity(parentEntity.getVelocity().add(vector3d.multiply(this.speed * 0.05D / d5)));
                    Vec3d vector3d1 = parentEntity.getVelocity();
                    parentEntity.setYaw(-((float) MathHelper.atan2(vector3d1.x, vector3d1.z)) * MathHelper.DEGREES_PER_RADIAN);
                    parentEntity.bodyYaw = parentEntity.getYaw();

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

    private class FlyGoal extends Goal {
        private final EntitySpectre parentEntity;
        boolean island = false;
        float circlingTime = 0;
        float circleDistance = 14;
        float maxCirclingTime = 80;
        boolean clockwise = false;
        private BlockPos target = null;
        private int islandCheckTime = 20;

        public FlyGoal(EntitySpectre sunbird) {
            this.parentEntity = sunbird;
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (parentEntity.lurePos != null) {
                return false;
            }
            var movementcontroller = this.parentEntity.getMoveControl();
            clockwise = random.nextBoolean();
            circleDistance = 5 + random.nextInt(10);
            if (!movementcontroller.isMoving() || target == null) {
                target = island ? getIslandPos(this.parentEntity.getBlockPos()) : getBlockFromDirection();
                if (target != null) {
                    this.parentEntity.getMoveControl().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 1.0D);
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return parentEntity.lurePos == null;
        }

        @Override
        public void stop() {
            island = false;
            islandCheckTime = 0;
            circleDistance = 5 + random.nextInt(10);
            circlingTime = 0;
            clockwise = random.nextBoolean();
            target = null;
        }

        @Override
        public void tick() {
            if (islandCheckTime-- <= 0) {
                islandCheckTime = 20;
                if (circlingTime == 0) {
                    island = this.parentEntity.getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, parentEntity.getBlockPos()).getY() > 2;
                    if (island) {
                        parentEntity.randomizeDirection();
                    }
                }
            }
            if (island) {
                circlingTime++;
                if (circlingTime > 100) {
                    island = false;
                    islandCheckTime = 1200;
                }
            } else if (circlingTime > 0) {
                circlingTime--;
            }
            if (target == null) {
                target = island ? getIslandPos(this.parentEntity.getBlockPos()) : getBlockFromDirection();
            }
            if (!island) {
                parentEntity.setYaw(parentEntity.getCardinalDirection().asRotation());
            }
            if (target != null) {
                this.parentEntity.getMoveControl().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 1.0D);
                if (parentEntity.squaredDistanceTo(Vec3d.ofCenter(target)) < 5.5F) {
                    target = null;
                }
            }
        }

        public BlockPos getBlockFromDirection() {
            float radius = 15;
            BlockPos forwards = parentEntity.getBlockPos().offset(parentEntity.getCardinalDirection(), (int) Math.ceil(radius));
            int height = 0;
            if (getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, forwards).getY() < 15) {
                height = 70 + random.nextInt(2);
            } else {
                height = getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, forwards).getY() + 10 + random.nextInt(10);
            }
            return new BlockPos(forwards.getX(), height, forwards.getZ());
        }

        public BlockPos getIslandPos(BlockPos orbit) {
            final float angle = (Maths.THREE_STARTING_ANGLE * (clockwise ? -circlingTime : circlingTime));
            final double extraX = circleDistance * MathHelper.sin((angle));
            final double extraZ = circleDistance * MathHelper.cos(angle);
            final int height = getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, orbit).getY();
            if (height < 3) {
                island = false;
                return getBlockFromDirection();
            }
            return new BlockPos((int) (orbit.getX() + extraX), Math.min(height + 10, orbit.getY() + random.nextInt(3) - random.nextInt(1)), (int) (orbit.getZ() + extraZ));
        }

    }

    static class TemptHeartGoal extends Goal {
        protected final EntitySpectre creature;
        private final TargetPredicate ENTITY_PREDICATE = TargetPredicate.createNonAttackable()
                .setBaseMaxDistance(64D)
                .ignoreDistanceScalingFactor()
                .ignoreVisibility();
        private final double speed;
        private final Ingredient temptItem;
        protected PlayerEntity closestPlayer;
        private int delayTemptCounter;

        public TemptHeartGoal(EntitySpectre p_i47822_1_, double p_i47822_2_, Ingredient p_i47822_4_, boolean p_i47822_5_) {
            this(p_i47822_1_, p_i47822_2_, p_i47822_5_, p_i47822_4_);
        }

        public TemptHeartGoal(EntitySpectre p_i47823_1_, double p_i47823_2_, boolean p_i47823_4_, Ingredient p_i47823_5_) {
            this.creature = p_i47823_1_;
            this.speed = p_i47823_2_;
            this.temptItem = p_i47823_5_;
        }

        @Override
        public boolean canStart() {
            if (this.delayTemptCounter > 0) {
                --this.delayTemptCounter;
                return false;
            } else {
                this.closestPlayer = this.creature.getWorld().getClosestPlayer(ENTITY_PREDICATE, this.creature);
                if (this.closestPlayer == null || this.creature.getHoldingEntity() == closestPlayer) {
                    return false;
                } else {
                    return this.isTempting(this.closestPlayer.getMainHandStack()) || this.isTempting(this.closestPlayer.getOffHandStack());
                }
            }
        }

        protected boolean isTempting(ItemStack p_188508_1_) {
            return this.temptItem.test(p_188508_1_);
        }

        @Override
        public boolean shouldContinue() {
            return this.canStart();
        }

        @Override
        public void start() {
            creature.lurePos = this.closestPlayer.getPos();
        }

        @Override
        public void stop() {
            this.closestPlayer = null;
            this.delayTemptCounter = 100;
            creature.lurePos = null;
        }

        @Override
        public void tick() {
            this.creature.getLookControl().lookAt(this.closestPlayer, (float) (this.creature.getMaxHeadRotation() + 20), (float) this.creature.getMaxLookPitchChange());
            if (this.creature.squaredDistanceTo(this.closestPlayer) < 6.25D) {
                this.creature.getNavigation().stop();
            } else {
                this.creature.getMoveControl().moveTo(this.closestPlayer.getX(), closestPlayer.getY() + closestPlayer.getStandingEyeHeight(), closestPlayer.getZ(), this.speed);
            }

        }
    }

}
