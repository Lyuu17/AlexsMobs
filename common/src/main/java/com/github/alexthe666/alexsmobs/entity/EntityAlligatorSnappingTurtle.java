package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public abstract class EntityAlligatorSnappingTurtle extends TameableEntity implements ISemiAquatic, Shearable {

    public static final Predicate<LivingEntity> TARGET_PRED = (animal) -> !(animal instanceof EntityAlligatorSnappingTurtle) && !(animal instanceof ArmorStandEntity) && EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(animal) && animal.isAlive();
    private static final TrackedData<Byte> CLIMBING = DataTracker.registerData(EntityAlligatorSnappingTurtle.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Integer> MOSS = DataTracker.registerData(EntityAlligatorSnappingTurtle.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> WAITING = DataTracker.registerData(EntityAlligatorSnappingTurtle.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> ATTACK_TARGET_FLAG = DataTracker.registerData(EntityAlligatorSnappingTurtle.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> LUNGE_FLAG = DataTracker.registerData(EntityAlligatorSnappingTurtle.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> TURTLE_SCALE = DataTracker.registerData(EntityAlligatorSnappingTurtle.class, TrackedDataHandlerRegistry.FLOAT);
    public float openMouthProgress;
    public float prevOpenMouthProgress;
    public float attackProgress;
    public float prevAttackProgress;
    public int chaseTime = 0;
    private int biteTick = 0;
    private int waitTime = 0;
    private int timeUntilWait = 0;
    private int mossTime = 0;

    protected EntityAlligatorSnappingTurtle(EntityType<? extends EntityAlligatorSnappingTurtle> type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 0.0F);
        this.setStepHeight(1);
    }

    @Override
    public EntityView method_48926() {
        return this.getWorld();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.ALLIGATOR_SNAPPING_TURTLE_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSourceIn) {
        return AMSoundRegistry.ALLIGATOR_SNAPPING_TURTLE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.ALLIGATOR_SNAPPING_TURTLE_HURT.get();
    }

    public static boolean canTurtleSpawn(EntityType<?> type, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        boolean spawnBlock = worldIn.getBlockState(pos.down()).isIn(AMTagRegistry.ALLIGATOR_SNAPPING_TURTLE_SPAWNS);
        return spawnBlock && pos.getY() < worldIn.getSeaLevel() + 4;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.alligatorSnappingTurtleSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 18.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.7D)
                .add(EntityAttributes.GENERIC_ARMOR, 8D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F);
    }

    @Override
    public float getScaleFactor() {
        return this.isBaby() ? 0.3F : 1.0F;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.3D, false));
        this.goalSelector.add(2, new AnimalAIFindWater(this));
        this.goalSelector.add(2, new AnimalAILeaveWater(this));
        this.goalSelector.add(3, new BottomFeederAIWander(this, 1.0D, 120, 150, 10));
        this.goalSelector.add(3, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(5, new LookAroundGoal(this));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.targetSelector.add(1, (new RevengeGoal(this) {
            public boolean canStart() {
                return chaseTime >= 0 && super.canStart();
            }
        }));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, LivingEntity.class, 2, false, true, TARGET_PRED) {
            protected @NotNull Box getSearchBox(double targetDistance) {
                return this.mob.getBoundingBox().expand(0.5D, 2D, 0.5D);
            }
        });
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.ALLIGATOR_SNAPPING_TURTLE_BREEDABLES);
    }

    @Override
    public boolean isClimbing() {
        return this.isBesideClimbableBlock();
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        return true;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CLIMBING, (byte) 0);
        this.dataTracker.startTracking(MOSS, 0);
        this.dataTracker.startTracking(TURTLE_SCALE, 1F);
        this.dataTracker.startTracking(WAITING, false);
        this.dataTracker.startTracking(ATTACK_TARGET_FLAG, false);
        this.dataTracker.startTracking(LUNGE_FLAG, false);
    }

    @Override
    public void tick() {
        super.tick();
        prevOpenMouthProgress = openMouthProgress;
        prevAttackProgress = attackProgress;
        final boolean attack = this.dataTracker.get(LUNGE_FLAG);
        final boolean open = this.isWaiting() || this.dataTracker.get(ATTACK_TARGET_FLAG) && !attack;

        if (attack) {
            if (attackProgress < 5F)
                attackProgress++;
        } else {
            if (attackProgress > 0F)
                attackProgress--;
        }

        if (open) {
            if (openMouthProgress < 5F)
                openMouthProgress++;
        } else {
            if (openMouthProgress > 0F)
                openMouthProgress--;
        }

        if (this.attackProgress == 4 && this.getTarget() != null && this.isAlive() && this.canSee(this.getTarget()) && this.distanceTo(this.getTarget()) < 2.3F) {
            final float dmg = this.isBaby() ? 1F : (float) this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue();
            this.getTarget().damage(this.getDamageSources().mobAttack(this), dmg);
        }

        if (this.attackProgress > 4)
            biteTick = 5;

        if (biteTick > 0)
            biteTick--;

        if (chaseTime < 0)
            chaseTime++;

        if (!this.getWorld().isClient()) {
            this.setBesideClimbableBlock(this.horizontalCollision && this.isTouchingWater());
            if (this.isWaiting()) {
                waitTime++;
                timeUntilWait = 1500;
                if (waitTime > 1500 || this.getTarget() != null) {
                    this.setWaiting(false);
                }
            } else {
                timeUntilWait--;
                waitTime = 0;
            }
            if ((this.getTarget() == null || !this.getTarget().isAlive()) && timeUntilWait <= 0 && this.isTouchingWater()) {
                this.setWaiting(true);
            }
            if (this.getTarget() != null && biteTick == 0) {
                this.setWaiting(false);
                chaseTime++;
                this.dataTracker.set(ATTACK_TARGET_FLAG, true);
                this.lookAtEntity(this.getTarget(), 360, 40);
                this.bodyYaw = this.getYaw();
                if (openMouthProgress > 4 && this.canSee(this.getTarget()) && this.distanceTo(this.getTarget()) < 2.3F) {
                    this.dataTracker.set(LUNGE_FLAG, true);
                }
                if (chaseTime > 40 && this.distanceTo(this.getTarget()) > (this.getTarget() instanceof PlayerEntity ? 5 : 10)) {
                    chaseTime = -50;
                    this.setTarget(null);
                    this.setAttacker(null);
                    this.onAttacking(null);
                    this.attackingPlayer = null;
                }
            } else {
                this.dataTracker.set(ATTACK_TARGET_FLAG, false);
                this.dataTracker.set(LUNGE_FLAG, false);
            }
            mossTime++;
            if (this.isTouchingWater() && mossTime > 12000) {
                mossTime = 0;
                this.setMoss(Math.min(10, this.getMoss() + 1));
            }
        }
    }

    @Override
    public LivingEntity getTarget() {
        return this.chaseTime < 0 ? null : super.getTarget();
    }

    @Override
    public void setTarget(@Nullable LivingEntity entitylivingbaseIn) {
        if (this.chaseTime >= 0) {
            super.setTarget(entitylivingbaseIn);
        } else {
            super.setTarget(null);
        }
    }

    @Override
    public LivingEntity getAttacker() {
        return this.chaseTime < 0 ? null : super.getAttacker();
    }

    @Override
    public void setAttacker(LivingEntity entitylivingbaseIn) {
        if (this.chaseTime >= 0) {
            super.setAttacker(entitylivingbaseIn);
        } else {
            super.setAttacker(null);
        }
    }

    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, EntityData spawnDataIn, NbtCompound dataTag) {
        this.setMoss(random.nextInt(6));
        this.setTurtleScale(0.8F + random.nextFloat() * 0.2F);
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    public float getTurtleScale() {
        return this.dataTracker.get(TURTLE_SCALE);
    }

    public void setTurtleScale(float scale) {
        this.dataTracker.set(TURTLE_SCALE, scale);
    }

    @NotNull
    @Override
    protected EntityNavigation createNavigation(@NotNull World worldIn) {
        return new SemiAquaticPathNavigator(EntityAlligatorSnappingTurtle.this, worldIn) {
            public boolean isStableDestination(BlockPos pos) {
                return this.world.getBlockState(pos).getFluidState().isEmpty();
            }
        };
    }

    public boolean isWaiting() {
        return this.dataTracker.get(WAITING);
    }

    public void setWaiting(boolean sit) {
        this.dataTracker.set(WAITING, sit);
    }

    public int getMoss() {
        return this.dataTracker.get(MOSS);
    }

    public void setMoss(int moss) {
        this.dataTracker.set(MOSS, moss);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Waiting", this.isWaiting());
        compound.putInt("MossLevel", this.getMoss());
        compound.putFloat("TurtleScale", this.getTurtleScale());
        compound.putInt("MossTime", this.mossTime);
        compound.putInt("WaitTime", this.waitTime);
        compound.putInt("WaitTime2", this.timeUntilWait);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setWaiting(compound.getBoolean("Waiting"));
        this.setMoss(compound.getInt("MossLevel"));
        this.setTurtleScale(compound.getFloat("TurtleScale"));
        this.mossTime = compound.getInt("MossTime");
        this.waitTime = compound.getInt("WaitTime");
        this.timeUntilWait = compound.getInt("WaitTime2");
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
        return this.isWaiting();
    }

    @Override
    public int getWaterSearchRange() {
        return 10;
    }

    @Override
    public boolean canBreatheInWater() {
        return true;
    }

    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView worldIn) {
        return worldIn.getFluidState(pos.down()).isEmpty() && worldIn.getFluidState(pos).isIn(FluidTags.WATER)
                ? 10.0F
                : super.getPathfindingFavor(pos, worldIn);
    }

    public boolean isBesideClimbableBlock() {
        return (this.dataTracker.get(CLIMBING) & 1) != 0;
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

    @Override
    public boolean canSpawn(WorldView worldIn) {
        return worldIn.doesNotIntersectEntities(this);
    }

    @Override
    public void travel(@NotNull Vec3d travelVector) {
        if (this.canMoveVoluntarily() && this.isTouchingWater()) {
            this.updateVelocity(this.getMovementSpeed(), travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            if (this.jumping) {
                this.setVelocity(this.getVelocity().multiply(1D));
                this.setVelocity(this.getVelocity().add(0.0D, 0.72D, 0.0D));
            } else {
                this.setVelocity(this.getVelocity().multiply(0.4D));
                this.setVelocity(this.getVelocity().add(0.0D, -0.08D, 0.0D));
            }

        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public boolean isShearable() {
        return this.isAlive() && this.getMoss() > 0;
    }

    @Override
    public void sheared(SoundCategory category) {
        this.getWorld().playSoundFromEntity(null, this, SoundEvents.ENTITY_SHEEP_SHEAR, category, 1.0F, 1.0F);
        this.emitGameEvent(GameEvent.ENTITY_INTERACT);
        if (!this.getWorld().isClient()) {
            if (random.nextFloat() < this.getMoss() * 0.05F) {
                this.dropItem(AMItemRegistry.SPIKED_SCUTE.get());
            } else {
                this.dropItem(Items.SEAGRASS);
            }
            this.setMoss(0);
        }
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.ALLIGATOR_SNAPPING_TURTLE.get().create(world);
    }
}
