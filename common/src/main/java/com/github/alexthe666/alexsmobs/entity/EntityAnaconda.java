package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.AnacondaPartIndex;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
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
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EntityAnaconda extends AnimalEntity implements ISemiAquatic {

    private static final TrackedData<Optional<UUID>> CHILD_UUID = DataTracker.registerData(EntityAnaconda.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> CHILD_ID = DataTracker.registerData(EntityAnaconda.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> STRANGLING = DataTracker.registerData(EntityAnaconda.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> YELLOW = DataTracker.registerData(EntityAnaconda.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> SHEDTIME = DataTracker.registerData(EntityAnaconda.class, TrackedDataHandlerRegistry.INTEGER);
    public final float[] ringBuffer = new float[64];
    public int ringBufferIndex = -1;
    private EntityAnacondaPart[] parts;
    private float prevStrangleProgress = 0F;
    private float strangleProgress = 0F;
    private int strangleTimer = 0;
    private int shedCooldown = 0;
    private int feedings = 0;
    private boolean isLandNavigator;
    private int swimTimer = -1000;
    private int passiveFor = 0;

    public EntityAnaconda(EntityType<? extends AnimalEntity> t, World world) {
        super(t, world);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 0.0F);
        switchNavigator(true);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.ANACONDA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.ANACONDA_HURT.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!isBaby()) {
            this.playSound(AMSoundRegistry.ANACONDA_SLITHER.get(), 1.0F, 1.0F);
        } else {
            super.playStepSound(pos, state);
        }
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.15F);
    }

    public static boolean canAnacondaSpawn(EntityType<? extends AnimalEntity> type, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        final boolean spawnBlock = worldIn.getBlockState(pos.down()).isIn(AMTagRegistry.ANACONDA_SPAWNS);
        return spawnBlock && pos.getY() < worldIn.getSeaLevel() + 4;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, @NotNull SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.anacondaSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new GroundPathNavigatorWide(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new AnimalSwimMoveControllerSink(this, 1.3F, 1F);
            this.navigation = new SemiAquaticPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new AnimalAIPanicBaby(this, 1.25D));
        this.goalSelector.add(2, new AIMelee());
        this.goalSelector.add(3, new AnimalAIFindWater(this));
        this.goalSelector.add(3, new AnimalAILeaveWater(this));
        this.goalSelector.add(4, new TemptGoal(this, 1.25D, Ingredient.fromTag(AMTagRegistry.ANACONDA_FOODSTUFFS), false));
        this.goalSelector.add(5, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(6, new FollowParentGoal(this, 1.1D));
        this.goalSelector.add(7, new AnimalAIWanderRanged(this, 60, 1.0D, 14, 7));
        this.goalSelector.add(8, new SemiAquaticAIRandomSwimming(this, 1.5D, 7));
        this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 25F));
        this.goalSelector.add(9, new LookAroundGoal(this));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, LivingEntity.class, 200, false, false, AMEntityRegistry.buildPredicateFromTag(AMTagRegistry.ANACONDA_TARGETS)));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, PlayerEntity.class, 110, false, true, null) {
            @Override
            public boolean canStart() {
                return !isBaby() && passiveFor == 0 && getWorld().getDifficulty() != Difficulty.PEACEFUL && !EntityAnaconda.this.isInLove() && super.canStart();
            }
        });
        this.targetSelector.add(3, new RevengeGoal(this));
    }

    @Override
    protected float getActiveEyeHeight(EntityPose p_33799_, EntityDimensions p_33800_) {
        return this.isBaby() ? 0.15F : 0.3F;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        final var itemstack = player.getStackInHand(hand);
        if (isBreedingItem(itemstack)) {
            this.setTarget(null);
            this.passiveFor = 3600 + random.nextInt(3600);
        }
        return super.interactMob(player, hand);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        if (compound.containsUuid("ChildUUID")) {
            this.setChildId(compound.getUuid("ChildUUID"));
        }
        feedings = compound.getInt("Feedings");
        this.setSheddingTime(compound.getInt("ShedTime"));
        this.setYellow(compound.getBoolean("Yellow"));
        shedCooldown = compound.getInt("ShedCooldown");
        passiveFor = compound.getInt("PassiveFor");
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        if (this.getChildId() != null) {
            compound.putUuid("ChildUUID", this.getChildId());
        }
        compound.putInt("Feedings", feedings);
        compound.putInt("ShedTime", getSheddingTime());
        compound.putBoolean("Yellow", isYellow());
        compound.putInt("ShedCooldown", shedCooldown);
        compound.putInt("PassiveFor", passiveFor);
    }

    @Override
    public void tickCramming() {
        final List<Entity> entities = this.getWorld().getOtherEntities(this, this.getBoundingBox().stretch(0.2D, 0.0D, 0.2D));
        entities.stream().filter(entity -> !(entity instanceof EntityAnacondaPart) && entity.isPushable()).forEach(entity -> entity.pushAwayFrom(this));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CHILD_UUID, Optional.empty());
        this.dataTracker.startTracking(CHILD_ID, -1);
        this.dataTracker.startTracking(STRANGLING, false);
        this.dataTracker.startTracking(YELLOW, false);
        this.dataTracker.startTracking(SHEDTIME, 0);
    }

    @Nullable
    public UUID getChildId() {
        return this.dataTracker.get(CHILD_UUID).orElse(null);
    }

    public void setChildId(@Nullable UUID uniqueId) {
        this.dataTracker.set(CHILD_UUID, Optional.ofNullable(uniqueId));
    }

    public int getSheddingTime() {
        return this.dataTracker.get(SHEDTIME);
    }

    public void setSheddingTime(int shedtime) {
        this.dataTracker.set(SHEDTIME, shedtime);
    }

    public boolean isStrangling() {
        return this.dataTracker.get(STRANGLING);
    }

    public void setStrangling(boolean running) {
        this.dataTracker.set(STRANGLING, running);
    }

    public boolean isYellow() {
        return this.dataTracker.get(YELLOW);
    }

    public void setYellow(boolean yellow) {
        this.dataTracker.set(YELLOW, yellow);
    }

    @Override
    public int getMaxLookPitchChange() {
        return 1;
    }

    @Override
    public int getMaxHeadRotation() {
        return 3;
    }

    public Entity getChild() {
        UUID id = getChildId();
        if (id != null && !getWorld().isClient) {
            return ((ServerWorld) getWorld()).getEntity(id);
        }
        return null;
    }

    @Override
    public boolean canBreatheInWater() {
        return true;
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Override
    public boolean canSpawn(WorldView worldIn) {
        return worldIn.doesNotIntersectEntities(this);
    }

    public void tick() {
        super.tick();
        if(this.passiveFor > 0){
            passiveFor--;
        }
        if (this.isTouchingWater()) {
            if (this.isLandNavigator)
                switchNavigator(false);
        } else {
            if (!this.isLandNavigator)
                switchNavigator(true);
        }

        this.prevStrangleProgress = strangleProgress;
        if (this.isStrangling()) {
            if (strangleProgress < 5F)
                strangleProgress++;
        } else {
            if (strangleProgress > 0F)
                strangleProgress--;
        }

        this.bodyYaw = this.getYaw();
        this.headYaw = MathHelper.clamp(this.headYaw, this.bodyYaw - 70, this.bodyYaw + 70);

        if (this.isStrangling()) {
            if (!getWorld().isClient && this.getTarget() != null && this.getTarget().isAlive()) {
                this.setPitch(0);
                final LivingEntity target = this.getTarget();
                final float radius = this.getTarget().getWidth() * -0.5F;
                final float angle = (Maths.STARTING_ANGLE * (target.bodyYaw - 45F));
                final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
                final double extraZ = radius * MathHelper.cos(angle);
                var targetVec = new Vec3d(extraX + target.getX(), target.getBodyY(1.0F), extraZ + target.getZ());
                var moveVec = targetVec.subtract(this.getPos()).multiply(1F);
                this.setVelocity(moveVec);
                if (!target.isOnGround()) {
                    target.setVelocity(new Vec3d(0, -0.08F, 0));
                } else {
                    target.setVelocity(Vec3d.ZERO);
                }
                if (strangleTimer >= 40 && strangleTimer % 20 == 0) {
                    final double health = MathHelper.clamp(this.getTarget().getMaxHealth(), 4, 50);
                    this.getTarget().damage(this.getDamageSources().mobAttack(this), (float) Math.max(4F, 0.25F * health));
                }
                if (this.getTarget() == null || !this.getTarget().isAlive()) {
                    strangleTimer = 0;
                    this.setStrangling(false);
                }
            }
            fallDistance = 0;
            strangleTimer++;
            this.setNoGravity(true);
        } else {
            this.setNoGravity(false);
        }
        if (this.ringBufferIndex < 0) {
            for (int i = 0; i < this.ringBuffer.length; ++i) {
                this.ringBuffer[i] = this.getYaw();
            }
        }
        this.ringBufferIndex++;
        if (this.ringBufferIndex == this.ringBuffer.length) {
            this.ringBufferIndex = 0;
        }
        this.ringBuffer[this.ringBufferIndex] = this.getYaw();

        if (!this.getWorld().isClient) {
            final int segments = 7;
            final Entity child = getChild();
            if (child == null) {
                LivingEntity partParent = this;
                parts = new EntityAnacondaPart[segments];
                var partIndex = AnacondaPartIndex.HEAD;
                var prevPos = this.getPos();
                for (int i = 0; i < segments; i++) {
                    final float prevReqRot = calcPartRotation(i) + getYawForPart(i);
                    final float reqRot = calcPartRotation(i + 1) + getYawForPart(i);
                    var part = new EntityAnacondaPart(AMEntityRegistry.ANACONDA_PART.get(), this);
                    part.setParent(partParent);
                    part.copyDataFrom(this);
                    part.setBodyIndex(i);
                    part.setPartType(AnacondaPartIndex.sizeAt(1 + i));
                    if (partParent == this) {
                        this.setChildId(part.getUuid());
                        this.dataTracker.set(CHILD_ID, part.getId());
                    }
                    if (partParent instanceof EntityAnacondaPart) {
                        ((EntityAnacondaPart) partParent).setChildId(part.getUuid());
                    }
                    part.setPosition(part.tickMultipartPosition(this.getId(), partIndex, prevPos, this.getPitch(), prevReqRot, reqRot, false));
                    partParent = part;
                    getWorld().spawnEntity(part);
                    parts[i] = part;
                    partIndex = part.getPartType();
                    prevPos = part.getPos();
                }
            }
            if (shouldReplaceParts() && this.getChild() instanceof EntityAnacondaPart) {
                parts = new EntityAnacondaPart[segments];
                parts[0] = (EntityAnacondaPart) this.getChild();
                this.dataTracker.set(CHILD_ID, parts[0].getId());
                int i = 1;
                while (i < parts.length && parts[i - 1].getChild() instanceof EntityAnacondaPart) {
                    parts[i] = (EntityAnacondaPart) parts[i - 1].getChild();
                    i++;
                }
            }
            AnacondaPartIndex partIndex = AnacondaPartIndex.HEAD;
            var prev = this.getPos();
            float xRot = this.getPitch();
//                float yRot = this.getYaw();
//                float headRot = MathHelper.wrapDegrees(this.getYaw());
            for (int i = 0; i < segments; i++) {
                if (this.parts[i] != null) {
                    final float prevReqRot = calcPartRotation(i) + getYawForPart(i);
                    final float reqRot = calcPartRotation(i + 1) + getYawForPart(i);
                    parts[i].setStrangleProgress(this.strangleProgress);
                    parts[i].copyDataFrom(this);
                    prev = parts[i].tickMultipartPosition(this.getId(), partIndex, prev, xRot, prevReqRot, reqRot, true);
                    partIndex = parts[i].getPartType();
                    xRot = parts[i].getPitch();
                }
            }

            if (isTouchingWater()) swimTimer = Math.max(swimTimer + 1, 0);
            else swimTimer = Math.min(swimTimer - 1, 0);
        }
        if (shedCooldown > 0) {
            shedCooldown--;
        }
        if (this.getSheddingTime() > 0) {
            this.setSheddingTime(this.getSheddingTime() - 1);
            if (this.getSheddingTime() == 0) {
                this.spawnItemAtOffset(new ItemStack(AMItemRegistry.SHED_SNAKE_SKIN.get()), 1 + random.nextFloat(), 0.2F);
                shedCooldown = 1000 + random.nextInt(2000);
            }
        }
    }

    private boolean shouldReplaceParts() {
        if (parts == null || parts[0] == null)
            return true;

        for (int i = 0; i < 7; i++) {
            if (parts[i] == null) {
                return true;
            }
        }

        return false;
    }

    private float getYawForPart(int i) {
        return this.getRingBuffer(4 + i * 2, 1.0F);
    }

    public float getRingBuffer(int bufferOffset, float partialTicks) {
        if (this.isDead()) {
            partialTicks = 0.0F;
        }

        partialTicks = 1.0F - partialTicks;
        final int i = this.ringBufferIndex - bufferOffset & 63;
        final int j = this.ringBufferIndex - bufferOffset - 1 & 63;
        final float d0 = this.ringBuffer[i];
        final float d1 = this.ringBuffer[j] - d0;
        return MathHelper.wrapDegrees(d0 + d1 * partialTicks);
    }

    @Override
    public float getScaleFactor() {
        return this.isBaby() ? 0.75F : 1.0F;
    }

    @Override
    public boolean isPushable() {
        return !this.isStrangling();
    }

    public boolean shouldMove() {
        return !this.isStrangling();
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.ANACONDA_FOODSTUFFS);
    }

    public void travel(Vec3d travelVector) {
        if (!this.shouldMove()) {
            if (this.getNavigation().getCurrentPath() != null) {
                this.getNavigation().stop();
            }
            travelVector = Vec3d.ZERO;
            super.travel(travelVector);
            return;
        }
        if (this.canMoveVoluntarily() && this.isTouchingWater()) {
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

    public float getStrangleProgress(float partialTick) {
        return this.prevStrangleProgress + (this.strangleProgress - this.prevStrangleProgress) * partialTick;
    }

    private float calcPartRotation(int i) {
        final float f = 1 - (this.strangleProgress * 0.2F);
        final float strangleIntensity = (float) (MathHelper.clamp(strangleTimer * 3, 0, 100F) * (1.0F + 0.2F * Math.sin(0.15F * strangleTimer)));
        return (float) (40 * -Math.sin(this.horizontalSpeed * 3 - (i))) * f + this.strangleProgress * 0.2F * i * strangleIntensity;
    }

    @Nullable
    public ItemEntity spawnItemAtOffset(@NotNull ItemStack stack, float f, float f1) {
        if (stack.isEmpty()) {
            return null;
        } else if (this.getWorld().isClient) {
            return null;
        } else {
            final var vec = new Vec3d(0, 0, f).rotateY(-f * MathHelper.RADIANS_PER_DEGREE);
            final var itementity = new ItemEntity(this.getWorld(), this.getX() + vec.x, this.getY() + (double) f1, this.getZ() + vec.z, stack);
            itementity.setToDefaultPickupDelay();
            //FIXME forge
//            if (captureDrops() != null) captureDrops().add(itementity);
//            else
            this.getWorld().spawnEntity(itementity);
            return itementity;
        }
    }

    @Override
    public boolean shouldEnterWater() {
        return this.getTarget() == null && !shouldLeaveWater() && swimTimer <= -1000;
    }

    public boolean shouldLeaveWater() {
        if (!this.getPassengerList().isEmpty())
            return false;

        if (this.getTarget() != null && !this.getTarget().isTouchingWater())
            return true;

        return swimTimer > 600 || this.isShedding();
    }

    @Override
    public boolean shouldStopMoving() {
        return !this.shouldMove();
    }

    @Override
    public int getWaterSearchRange() {
        return 12;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        EntityAnaconda anaconda = AMEntityRegistry.ANACONDA.get().create(world);
        anaconda.setYellow(this.isYellow());
        return anaconda;
    }

    @Override
    public void updateKilledAdvancementCriterion(@NotNull Entity entity, int score, @NotNull DamageSource src) {
        if(entity instanceof LivingEntity living){
            final NbtCompound emptyNbt = new NbtCompound();
            living.writeCustomDataToNbt(emptyNbt);
            emptyNbt.putString("DeathLootTable", LootTables.EMPTY.toString());
            living.readCustomDataFromNbt(emptyNbt);

            if (this.getChild() instanceof EntityAnacondaPart)
                ((EntityAnacondaPart) this.getChild()).setSwell(5);

        }
        super.updateKilledAdvancementCriterion(entity, score, src);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.IN_WALL)  || super.isInvulnerableTo(source);
    }

    @Override
    public boolean canTarget(@NotNull LivingEntity livingEntity) {
        boolean prev = super.canTarget(livingEntity);
        if(prev && passiveFor > 0 && livingEntity instanceof PlayerEntity  && (this.getAttacker() == null || !this.getAttacker().getUuid().equals(livingEntity.getUuid()))){
            return false;
        }
        return prev;
    }

    public void feed() {
        this.heal(10);
        this.feedings++;
        if (feedings >= 3 && feedings % 3 == 0 && shedCooldown <= 0) {
            this.setSheddingTime(this.getRandom().nextInt(500) + 500);
        }
    }

    public boolean isShedding() {
        return this.getSheddingTime() > 0;
    }

    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, EntityData spawnDataIn, NbtCompound dataTag) {
        this.setYellow(random.nextBoolean());
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    private class AIMelee extends Goal {
        private final EntityAnaconda snake;
        private int jumpAttemptCooldown = 0;

        public AIMelee() {
            snake = EntityAnaconda.this;
        }

        @Override
        public boolean canStart() {
            return snake.getTarget() != null && snake.getTarget().isAlive();
        }

        public void tick() {
            if (jumpAttemptCooldown > 0)
                jumpAttemptCooldown--;

            final LivingEntity target = snake.getTarget();
            if (target != null && target.isAlive()) {
                if (jumpAttemptCooldown == 0 && snake.distanceTo(target) < 1 + target.getWidth() && !snake.isStrangling()) {
                    target.damage(snake.getDamageSources().mobAttack(snake), 4);
                    snake.setStrangling(target.getWidth() <= 2.0F && !(target instanceof EntityAnaconda));
                    snake.playSound(AMSoundRegistry.ANACONDA_ATTACK.get(), snake.getSoundVolume(), snake.getSoundPitch());
                    jumpAttemptCooldown = 5 + random.nextInt(5);
                }
                if (snake.isStrangling()) {
                    snake.getNavigation().stop();
                } else {
                    try {
                        snake.getNavigation().startMovingTo(target, 1.3F);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void stop() {
            snake.setStrangling(false);
        }
    }
}
