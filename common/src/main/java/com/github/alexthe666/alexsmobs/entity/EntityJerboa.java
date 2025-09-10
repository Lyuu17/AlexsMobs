package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIWanderRanged;
import com.github.alexthe666.alexsmobs.entity.ai.JerboaAIBeg;
import com.github.alexthe666.alexsmobs.registry.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.control.JumpControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

public class EntityJerboa extends AnimalEntity {

    private static final TrackedData<Boolean> JUMP_ACTIVE = DataTracker.registerData(EntityJerboa.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BEGGING = DataTracker.registerData(EntityJerboa.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SLEEPING = DataTracker.registerData(EntityJerboa.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BEFRIENDED = DataTracker.registerData(EntityJerboa.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float jumpProgress;
    public float prevJumpProgress;
    public float reboundProgress;
    public float prevReboundProgress;
    public float begProgress;
    public float prevBegProgress;
    public float sleepProgress;
    public float prevSleepProgress;
    private int jumpTicks;
    private int jumpDuration;
    private boolean wasOnGround;
    private int currentMoveTypeDuration;

    public EntityJerboa(EntityType<? extends AnimalEntity> jerboa, World lvl) {
        super(jerboa, lvl);
        this.moveControl = new EntityJerboa.MoveHelperController(this);
        this.jumpControl = new JumpHelperController(this);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 4.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.45F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(JUMP_ACTIVE, false);
        this.dataTracker.startTracking(BEGGING, false);
        this.dataTracker.startTracking(SLEEPING, false);
        this.dataTracker.startTracking(BEFRIENDED, false);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new JerboaAIBeg(this, 1.0D));
        this.goalSelector.add(2, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(3, new FleeEntityGoal<>(this, PlayerEntity.class, 5, 1.3D, 1.0D) {
            @Override
            public boolean canStart() {
                return !EntityJerboa.this.isBefriended() && super.canStart();
            }
        });
        this.goalSelector.add(4, new FleeEntityGoal<>(this, CatEntity.class, 9, 1.3D, 1.0D));
        this.goalSelector.add(5, new FleeEntityGoal<>(this, OcelotEntity.class, 9, 1.3D, 1.0D));
        this.goalSelector.add(6, new FleeEntityGoal<>(this, EntityRattlesnake.class, 9, 1.3D, 1.0D));
        this.goalSelector.add(7, new EscapeDangerGoal(this, 1.1D));
        this.goalSelector.add(8, new AnimalAIWanderRanged(this, 20, 1.0D, 10, 7));
        this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(10, new LookAroundGoal(this));
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setBefriended(compound.getBoolean("Befriended"));
        this.setSleeping(compound.getBoolean("Sleeping"));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Befriended", isBefriended());
        compound.putBoolean("Sleeping", isSleeping());
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceToClosestPlayer) {
        return !cannotDespawn();
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.isBefriended();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.JERBOA_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.JERBOA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.JERBOA_HURT.get();
    }

    @Override
    public void tick() {
        super.tick();
        this.prevJumpProgress = jumpProgress;
        this.prevReboundProgress = reboundProgress;
        this.prevSleepProgress = sleepProgress;
        this.prevBegProgress = begProgress;
        if (!this.getWorld().isClient) {
            this.dataTracker.set(JUMP_ACTIVE, !this.isOnGround());
        }
        if (this.dataTracker.get(JUMP_ACTIVE)) {
            if (jumpProgress < 5F) {
                jumpProgress += 1F;
                if (reboundProgress > 0) {
                    reboundProgress--;
                }
            }
            if (jumpProgress >= 5F) {
                if (reboundProgress < 5F) {
                    reboundProgress += 1;
                }
            }
        } else {
            if (reboundProgress > 0) {
                reboundProgress = Math.max(reboundProgress - 1F, 0);
            }
            if (jumpProgress > 0) {
                jumpProgress = Math.max(jumpProgress - 1F, 0);
            }
        }

        if (this.isBegging()) {
            if (begProgress < 5F)
                begProgress++;
        } else {
            if (begProgress > 0F)
                begProgress--;
        }

        if (this.isSleeping()) {
            if (sleepProgress < 5F)
                sleepProgress++;
        } else {
            if (sleepProgress > 0F)
                sleepProgress--;
        }

        if (!this.getWorld().isClient) {
            if (this.getWorld().isDay() && this.getAttacker() == null && !this.isBegging()) {
                if (age % 10 == 0 && this.getRandom().nextInt(750) == 0) {
                    this.setSleeping(true);
                }
            } else if (this.isSleeping()) {
                this.setSleeping(false);
            }
        }
    }

    public boolean isBegging() {
        return this.dataTracker.get(BEGGING);
    }

    public void setBegging(boolean begging) {
        this.dataTracker.set(BEGGING, begging);
    }

    public boolean isSleeping() {
        return this.dataTracker.get(SLEEPING);
    }

    public void setSleeping(boolean sleeping) {
        this.dataTracker.set(SLEEPING, sleeping);
    }

    public boolean isBefriended() {
        return this.dataTracker.get(BEFRIENDED);
    }

    public void setBefriended(boolean befriended) {
        this.dataTracker.set(BEFRIENDED, befriended);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        if ((itemstack.isIn(AMTagRegistry.JERBOA_BEGS_FOR) || isBreedingItem(itemstack)) && (this.getHealth() < this.getMaxHealth() || !this.isBefriended())) {
            if (!player.isCreative()) {
                itemstack.decrement(1);
            }
            this.setBefriended(true);
            this.heal(4);
            return ActionResult.SUCCESS;
        }
        ActionResult type = super.interactMob(player, hand);
        if (type != ActionResult.SUCCESS && !isBreedingItem(itemstack) && itemstack.isIn(AMTagRegistry.JERBOA_BEGS_FOR)) {
            this.setSleeping(false);
            this.emitGameEvent(GameEvent.ENTITY_INTERACT);
            this.playSound(SoundEvents.ENTITY_PARROT_EAT, this.getSoundPitch(), this.getSoundVolume());
            for (int i = 0; i < 6 + random.nextInt(3); i++) {
                double d2 = this.random.nextGaussian() * 0.02D;
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = this.random.nextGaussian() * 0.02D;
                this.getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, itemstack), this.getX() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, this.getY() + this.getHeight() * 0.5F + (double) (this.random.nextFloat() * this.getHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, d0, d1, d2);
            }
            if (random.nextFloat() <= 0.3F) {
                player.addStatusEffect(new StatusEffectInstance(AMEffectRegistry.FLEET_FOOTED.get(), 12000));
            }
            return ActionResult.SUCCESS;
        }
        return type;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean prev = super.damage(source, amount);
        if (prev) {
            this.setSleeping(false);
            if (source.getAttacker() != null) {
                if (source.getAttacker() instanceof LivingEntity hurter) {
                    if (hurter.hasStatusEffect(AMEffectRegistry.FLEET_FOOTED.get())) {
                        hurter.removeStatusEffect(AMEffectRegistry.FLEET_FOOTED.get());
                    }
                }
            }
            return prev;
        }
        return prev;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.JERBOA_BREEDABLES);
    }

    public boolean shouldMove() {
        return !isSleeping();
    }

    public float getJumpCompletion(float partialTicks) {
        return this.jumpDuration == 0 ? 0.0F : ((float) this.jumpTicks + partialTicks) / (float) this.jumpDuration;
    }

    @Override
    protected float getJumpVelocity() {
        return horizontalCollision ? super.getJumpVelocity() + 0.2F : 0.25F + random.nextFloat() * 0.15F;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }


    public static boolean isValidLightLevel(ServerWorldAccess p_223323_0_, BlockPos p_223323_1_, Random p_223323_2_) {
        int light = p_223323_0_.getLightLevel(p_223323_1_);
        return light <= 4;

    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.jerboaSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean canMonsterSpawnInLight(EntityType<? extends EntityJerboa> p_223325_0_, ServerWorldAccess p_223325_1_, SpawnReason p_223325_2_, BlockPos p_223325_3_, Random p_223325_4_) {
        return isValidLightLevel(p_223325_1_, p_223325_3_, p_223325_4_) && canMobSpawn(p_223325_0_, p_223325_1_, p_223325_2_, p_223325_3_, p_223325_4_);
    }

    public static <T extends MobEntity> boolean canJerboaSpawn(EntityType<EntityJerboa> entityType, ServerWorldAccess iServerWorld, SpawnReason reason, BlockPos pos, Random random) {
        return reason == SpawnReason.SPAWNER || iServerWorld.isSkyVisible(pos.up()) && canMonsterSpawnInLight(entityType, iServerWorld, reason, pos, random);
    }

    @Override
    protected void jump() {
        super.jump();

        if (!this.getWorld().isClient) {
            this.getWorld().sendEntityStatus(this, (byte) 1);
        }
    }

    public void setMovementSpeed(double newSpeed) {
        this.getNavigation().setSpeed(newSpeed);
        this.moveControl.moveTo(this.moveControl.getTargetX(), this.moveControl.getTargetY(), this.moveControl.getTargetZ(), newSpeed);
    }

    public void startJumping() {
        this.setJumping(true);
        this.jumpDuration = 10;
        this.jumpTicks = 0;
    }

    private void checkLandingDelay() {
        this.updateMoveTypeDuration();
        this.disableJumpControl();
    }

    private void calculateRotationYaw(double x, double z) {
        this.setYaw((float) (MathHelper.atan2(z - this.getZ(), x - this.getX()) * (double) MathHelper.DEGREES_PER_RADIAN) - 90.0F);
    }

    private void enableJumpControl() {
        if (jumpControl instanceof EntityJerboa.JumpHelperController) {
            ((EntityJerboa.JumpHelperController) this.jumpControl).setCanJump(true);
        }
    }

    private void disableJumpControl() {
        if (jumpControl instanceof EntityJerboa.JumpHelperController) {
            ((EntityJerboa.JumpHelperController) this.jumpControl).setCanJump(false);
        }
    }

    private void updateMoveTypeDuration() {
        if (this.moveControl.getSpeed() < 2.2D) {
            this.currentMoveTypeDuration = 2;
        } else {
            this.currentMoveTypeDuration = 1;
        }

    }

    @Override
    public void mobTick() {
        super.mobTick();

        if (this.currentMoveTypeDuration > 0) {
            --this.currentMoveTypeDuration;
        }

        if (this.isOnGround() && this.shouldMove()) {
            if (!this.wasOnGround) {
                this.setJumping(false);
                this.checkLandingDelay();
            }

            if (this.currentMoveTypeDuration == 0) {
                LivingEntity livingentity = this.getTarget();
                if (livingentity != null && this.squaredDistanceTo(livingentity) < 16.0D) {
                    this.calculateRotationYaw(livingentity.getX(), livingentity.getZ());
                    this.moveControl.moveTo(livingentity.getX(), livingentity.getY(), livingentity.getZ(), this.moveControl.getSpeed());
                    this.startJumping();
                    this.wasOnGround = true;
                }
            }
            if (this.jumpControl instanceof JumpHelperController rabbitController) {
                if (!rabbitController.isActive()) {
                    if (this.moveControl.isMoving() && this.currentMoveTypeDuration == 0) {
                        var path = this.navigation.getCurrentPath();
                        var vector3d = new Vec3d(this.moveControl.getTargetX(), this.moveControl.getTargetY(), this.moveControl.getTargetZ());
                        if (path != null && !path.isFinished()) {
                            vector3d = path.getNodePosition(this);
                        }

                        this.calculateRotationYaw(vector3d.x, vector3d.z);
                        this.startJumping();
                    }
                } else if (!rabbitController.canJump()) {
                    this.enableJumpControl();
                }
            }
        } else if (!this.shouldMove()) {
            this.setJumping(false);
            this.checkLandingDelay();
        }

        this.wasOnGround = this.isOnGround();
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.jumpTicks != this.jumpDuration) {
            ++this.jumpTicks;
        } else if (this.jumpDuration != 0) {
            this.jumpTicks = 0;
            this.jumpDuration = 0;
            this.setJumping(false);
        }
    }

    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if (id == 1) {
            this.spawnSprintingParticles();
            this.jumpDuration = 10;
            this.jumpTicks = 0;
        } else {
            super.handleStatus(id);
        }

    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld p_146743_, PassiveEntity p_146744_) {
        EntityJerboa boa = AMEntityRegistry.JERBOA.get().create(p_146743_);
        boa.setBefriended(true);
        return boa;
    }

    public boolean hasJumper() {
        return jumpControl instanceof JumpHelperController;
    }

    static class MoveHelperController extends MoveControl {
        private final EntityJerboa jerboa;
        private double nextJumpSpeed;

        public MoveHelperController(EntityJerboa jerboa) {
            super(jerboa);
            this.jerboa = jerboa;
        }

        @Override
        public void tick() {
            if (this.jerboa.hasJumper() && this.jerboa.isOnGround() && !this.jerboa.jumping && !((EntityJerboa.JumpHelperController) this.jerboa.jumpControl).isActive()) {
                this.jerboa.setMovementSpeed(0.0D);
            } else if (this.isMoving()) {
                this.jerboa.setMovementSpeed(this.nextJumpSpeed);
            }
            if (this.state == MoveControl.State.MOVE_TO) {
                this.state = MoveControl.State.WAIT;
                var vector3d = new Vec3d(this.targetX - jerboa.getX(), this.targetY - jerboa.getY(), this.targetZ - jerboa.getZ());
                double d0 = vector3d.length();
                jerboa.setVelocity(jerboa.getVelocity().add(vector3d.multiply(this.speed * 1.0F * 0.05D / d0)));

            }
            super.tick();

        }

        /**
         * Sets the speed and location to move to
         */
        @Override
        public void moveTo(double x, double y, double z, double speedIn) {
            if (this.jerboa.isTouchingWater()) {
                speedIn = 1.5D;
            }

            super.moveTo(x, y, z, speedIn);
            if (speedIn > 0.0D) {
                this.nextJumpSpeed = speedIn;
            }

        }
    }

    public static class JumpHelperController extends JumpControl {
        private final EntityJerboa jerboa;
        private boolean canJump;

        public JumpHelperController(EntityJerboa jerboa) {
            super(jerboa);
            this.jerboa = jerboa;
        }

        public boolean isActive() {
            return this.active;
        }

        public boolean canJump() {
            return this.canJump;
        }

        public void setCanJump(boolean canJumpIn) {
            this.canJump = canJumpIn;
        }

        public void tick() {
            if (this.active) {
                this.jerboa.startJumping();
                this.active = false;
            }

        }
    }
}