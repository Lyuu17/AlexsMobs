package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.block.BlockTerrapinEgg;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.TerrapinTypes;
import com.github.alexthe666.alexsmobs.registry.*;
import com.github.alexthe666.alexsmobs.tileentity.TileEntityTerrapinEgg;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
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
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EntityTerrapin extends AnimalEntity implements ISemiAquatic, Bucketable {

    private static final TrackedData<Integer> TURTLE_TYPE = DataTracker.registerData(EntityTerrapin.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SHELL_TYPE = DataTracker.registerData(EntityTerrapin.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SKIN_TYPE = DataTracker.registerData(EntityTerrapin.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TURTLE_COLOR = DataTracker.registerData(EntityTerrapin.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SHELL_COLOR = DataTracker.registerData(EntityTerrapin.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SKIN_COLOR = DataTracker.registerData(EntityTerrapin.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> RETREATED = DataTracker.registerData(EntityTerrapin.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SPINNING = DataTracker.registerData(EntityTerrapin.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HAS_EGG = DataTracker.registerData(EntityTerrapin.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> FROM_BUCKET = DataTracker.registerData(EntityTerrapin.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float clientSpin = 0;
    public int spinCounter = 0;
    public float prevSwimProgress;
    public float swimProgress;
    public float prevRetreatProgress;
    public float retreatProgress;
    public float prevSpinProgress;
    public float spinProgress;
    private int maxRollTime = 50;
    private boolean isLandNavigator;
    private int swimTimer = -1000;
    private int hideInShellTimer = 0;
    private Vec3d spinDelta;
    private float spinYRot;
    private int changeSpinAngleCooldown = 0;
    private LivingEntity lastLauncher = null;
    private TileEntityTerrapinEgg.ParentData partnerData;

    public EntityTerrapin(EntityType<? extends EntityTerrapin> animal, World level) {
        super(animal, level);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 0.0F);
        switchNavigator(true);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 10.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.1F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.TERRAPIN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.TERRAPIN_HURT.get();
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.terrapinSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean canTerrapinSpawn(EntityType<EntityTerrapin> entityType, ServerWorldAccess iServerWorld, SpawnReason reason, BlockPos pos, Random random) {
        return reason == SpawnReason.SPAWNER || iServerWorld.getBlockState(pos).getFluidState().isOf(Fluids.WATER);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new BreatheAirGoal(this));
        this.goalSelector.add(1, new MateGoal(this, 1.0D));
        this.goalSelector.add(1, new LayEggGoal(this, 1.0D));
        this.goalSelector.add(2, new TemptGoal(this, 1.1D, Ingredient.fromTag(AMTagRegistry.TERRAPIN_BREEDABLES), false));
        this.goalSelector.add(3, new AnimalAIFindWater(this));
        this.goalSelector.add(3, new AnimalAILeaveWater(this));
        this.goalSelector.add(4, new SemiAquaticAIRandomSwimming(this, 1.0D, 30));
        this.goalSelector.add(6, new EscapeDangerGoal(this, 1.1D));
        this.goalSelector.add(7, new WanderAroundGoal(this, 1.0D, 60));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
    }

    @Override
    public void tick() {
        super.tick();
        prevSwimProgress = swimProgress;
        prevRetreatProgress = retreatProgress;
        prevSpinProgress = spinProgress;

        final boolean inWaterOrBubble = this.isInsideWaterOrBubbleColumn();
        final boolean spinning = this.isSpinning();
        final boolean retreated = this.hasRetreated();

        if (inWaterOrBubble) {
            if (swimProgress < 5F)
                swimProgress++;
        } else {
            if (swimProgress > 0F)
                swimProgress--;
        }

        if (spinning) {
            if (spinProgress < 5F)
                spinProgress++;
        } else {
            if (spinProgress > 0F)
                spinProgress--;
        }

        if (retreated) {
            if (retreatProgress < 5F)
                retreatProgress++;
        } else {
            if (retreatProgress > 0F)
                retreatProgress--;
        }

        if (spinning) {
            this.handleSpin();
            if (this.isAlive() && spinCounter > 5 && !this.isBaby()) {
                for (var entity : this.getWorld().getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox().expand(0.3F))) {
                    if (!isTeammate(entity) && !(entity instanceof EntityTerrapin)) {
                        entity.damage(this.getDamageSources().mobAttack(lastLauncher == null ? this : lastLauncher), 4.0F + random.nextFloat() * 4.0F);
                    }
                }
            }
            if (!this.isAlive()) {
                this.setSpinning(false);
            }
            if (this.horizontalCollision) {
                if(changeSpinAngleCooldown == 0){
                    changeSpinAngleCooldown = 10;
                    float f = collideDirectionAndSound().getAxis() == Direction.Axis.X ? this.spinYRot - 180 : 180 - this.spinYRot;
                    f += random.nextInt(40) - 20;
                    this.setYaw(f);
                    this.copySpinDelta(f, Vec3d.ZERO);
                }else{
                    maxRollTime -= 30;
                }

            }
            if (changeSpinAngleCooldown > 0) {
                changeSpinAngleCooldown--;
            }
        }
        if (!this.getWorld().isClient) {
            if (this.isInsideWaterOrBubbleColumn() && this.isLandNavigator) {
                switchNavigator(false);
            }
            if (!this.isInsideWaterOrBubbleColumn() && !this.isLandNavigator) {
                switchNavigator(true);
            }
            if (isTouchingWater()) {
                swimTimer = Math.max(0, swimTimer + 1);
            } else {
                swimTimer = Math.min(0, swimTimer - 1);
                List<PlayerEntity> list = this.getWorld().getNonSpectatingEntities(PlayerEntity.class, this.getBoundingBox().expand(0, 0.15F, 0));
                for (PlayerEntity player : list) {
                    if ((player.jumping || !player.isOnGround()) && player.getY() > this.getEyeY()) {
                        if (!hasRetreated()) {
                            this.hideInShellTimer += 40 + random.nextInt(40);
                        } else if (!isSpinning()) {
                            lastLauncher = player;
                            final int spin = 100 + random.nextInt(100);
                            this.hideInShellTimer = spin;
                            this.setYaw(player.getHeadYaw());
                            spinFor(spin);
                        }
                    }
                }
            }

            if (swimProgress > 0) {
                this.setStepHeight(1);
            } else {
                this.setStepHeight(0.6F);
            }
            if (hideInShellTimer > 0) {
                hideInShellTimer--;
            }
            this.setRetreated(hideInShellTimer > 0 && !this.isSpinning());
        }
    }

    private Direction collideDirectionAndSound(){
        var raytraceresult = ProjectileUtil.getCollision(this, entity -> false);
        if(raytraceresult instanceof BlockHitResult){
            BlockState state = getWorld().getBlockState(((BlockHitResult) raytraceresult).getBlockPos());
            if(state != null && !this.isSilent()){
            }
            return ((BlockHitResult) raytraceresult).getSide();
        }
        return Direction.DOWN;
    }

    private boolean isMoving() {
        return this.getVelocity().lengthSquared() > 0.02D;
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new MobNavigation(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new AnimalSwimMoveControllerSink(this, 2.5F, 1.15F);
            this.navigation = new SemiAquaticPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(TURTLE_TYPE, 0);
        this.dataTracker.startTracking(SHELL_TYPE, 0);
        this.dataTracker.startTracking(SKIN_TYPE, 0);
        this.dataTracker.startTracking(SHELL_COLOR, 0);
        this.dataTracker.startTracking(SKIN_COLOR, 0);
        this.dataTracker.startTracking(TURTLE_COLOR, 0);
        this.dataTracker.startTracking(RETREATED, false);
        this.dataTracker.startTracking(SPINNING, false);
        this.dataTracker.startTracking(HAS_EGG, false);
        this.dataTracker.startTracking(FROM_BUCKET, false);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putInt("TurtleType", this.getTurtleTypeOrdinal());
        compound.putInt("ShellType", this.getShellType());
        compound.putInt("SkinType", this.getSkinType());
        compound.putInt("TurtleColor", this.getTurtleColor());
        compound.putInt("ShellColor", this.getShellColor());
        compound.putInt("SkinColor", this.getSkinColor());
        compound.putBoolean("HasEgg", this.hasEgg());
        compound.putBoolean("Bucketed", this.isFromBucket());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setTurtleTypeOrdinal(compound.getInt("TurtleType"));
        this.setShellType(compound.getInt("ShellType"));
        this.setSkinType(compound.getInt("SkinType"));
        this.setTurtleColor(compound.getInt("TurtleColor"));
        this.setShellColor(compound.getInt("ShellColor"));
        this.setSkinColor(compound.getInt("SkinColor"));
        this.setHasEgg(compound.getBoolean("HasEgg"));
        this.setFromBucket(compound.getBoolean("Bucketed"));
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if(!this.isSpinning()){
            super.playStepSound(pos, state);
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.TERRAPIN_BREEDABLES);
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
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.isFromBucket() || this.hasCustomName();
    }

    @Override
    public boolean canImmediatelyDespawn(double d) {
        return !this.isFromBucket() && !this.hasCustomName();
    }

    private int getTurtleTypeOrdinal() {
        return MathHelper.clamp(this.dataTracker.get(TURTLE_TYPE), 0, TerrapinTypes.values().length - 1);
    }

    private void setTurtleTypeOrdinal(int i) {
        this.dataTracker.set(TURTLE_TYPE, i);
    }

    public int getShellType() {
        return this.dataTracker.get(SHELL_TYPE);
    }

    public void setShellType(int i) {
        this.dataTracker.set(SHELL_TYPE, i);
    }

    public int getSkinType() {
        return this.dataTracker.get(SKIN_TYPE);
    }

    public void setSkinType(int i) {
        this.dataTracker.set(SKIN_TYPE, i);
    }

    public int getShellColor() {
        return this.dataTracker.get(SHELL_COLOR);
    }

    public void setShellColor(int i) {
        this.dataTracker.set(SHELL_COLOR, i);
    }

    public int getSkinColor() {
        return this.dataTracker.get(SKIN_COLOR);
    }

    public void setSkinColor(int i) {
        this.dataTracker.set(SKIN_COLOR, i);
    }

    public int getTurtleColor() {
        return this.dataTracker.get(TURTLE_COLOR);
    }

    public void setTurtleColor(int i) {
        this.dataTracker.set(TURTLE_COLOR, i);
    }

    public TerrapinTypes getTurtleType() {
        return TerrapinTypes.values()[getTurtleTypeOrdinal()];
    }

    public void setTurtleType(TerrapinTypes type) {
        this.setTurtleTypeOrdinal(type.ordinal());
    }

    public boolean isSpinning() {
        return this.dataTracker.get(SPINNING);
    }

    public void setSpinning(boolean b) {
        this.dataTracker.set(SPINNING, b);
    }

    public boolean hasRetreated() {
        return this.dataTracker.get(RETREATED);
    }

    public void setRetreated(boolean b) {
        this.dataTracker.set(RETREATED, b);
    }

    public boolean hasEgg() {
        return this.dataTracker.get(HAS_EGG);
    }

    private void setHasEgg(boolean hasEgg) {
        this.dataTracker.set(HAS_EGG, hasEgg);
    }

    @Override
    public int getMaxAir() {
        return 4800;
    }

    @Override
    protected int getNextAirOnLand(int currentAir) {
        return this.getMaxAir();
    }

    @Override
    public void pushAway(Entity entity) {
        if (this.isInsideWaterOrBubbleColumn() || entity instanceof EntityTerrapin) {
            super.pushAway(entity);
        } else {
            entity.setVelocity(entity.getVelocity().add(this.getVelocity()));
        }
    }

    @Override
    public boolean isCollidable() {
        return this.isInsideWaterOrBubbleColumn() ? super.isCollidable() : this.isAlive();
    }

    private void spinFor(int time) {
        this.maxRollTime = time;
        this.setSpinning(true);
    }

    private void copySpinDelta(float spinRot, Vec3d motionIn) {
        final float f = spinRot * MathHelper.RADIANS_PER_DEGREE;
        final float f1 = this.isBaby() ? 0.3F : 0.5F;
        this.spinYRot = spinRot;
        this.spinDelta = new Vec3d(motionIn.x + (double) (-MathHelper.sin(f) * f1), 0.0D, motionIn.z + (double) (MathHelper.cos(f) * f1));
        this.setVelocity(this.spinDelta.add(0.0D, 0.0D, 0.0D));

    }

    private void handleSpin() {
        this.setRetreated(true);
        ++this.spinCounter;
        if (!this.getWorld().isClient) {
            if (this.spinCounter > maxRollTime) {
                this.setSpinning(false);
                this.hideInShellTimer = 10 + random.nextInt(30);
                this.spinCounter = 0;
            } else {
                Vec3d vec3 = this.getVelocity();
                if (this.spinCounter == 1) {
                    copySpinDelta(this.getYaw(), vec3);
                } else {
                    this.setYaw(spinYRot);
                    this.setHeadYaw(spinYRot);
                    this.setBodyYaw(spinYRot);
                    this.setVelocity(this.spinDelta.x, vec3.y, this.spinDelta.z);
                }
            }
        }
    }


    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.setAir(this.getMaxAir());
        this.setTurtleType(TerrapinTypes.getRandomType(random));
        this.setShellType(random.nextInt(7));
        this.setSkinType(random.nextInt(4));
        this.setTurtleColor(TerrapinTypes.generateRandomColor(random));
        this.setShellColor(TerrapinTypes.generateRandomColor(random));
        this.setSkinColor(TerrapinTypes.generateRandomColor(random));
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.TERRAPIN.get().create(world);
    }

    @Override
    public boolean shouldStopMoving() {
        return this.isSpinning() || this.hasRetreated();
    }

    @Override
    public boolean shouldEnterWater() {
        return this.getTarget() == null && !shouldLeaveWater() && swimTimer <= -1000;
    }

    @Override
    public boolean shouldLeaveWater() {
        return swimTimer > 600 || this.hasEgg();
    }

    @Override
    public int getWaterSearchRange() {
        return 10;
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.shouldStopMoving()) {
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

    @Override
    @NotNull
    public ItemStack getBucketItem() {
        ItemStack stack = new ItemStack(AMItemRegistry.TERRAPIN_BUCKET.get());
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
        NbtCompound platTag = new NbtCompound();
        this.writeCustomDataToNbt(platTag);
        NbtCompound compound = bucket.getOrCreateNbt();
        compound.put("TerrapinData", platTag);
    }

    @Override
    public void copyDataFromNbt(@NotNull NbtCompound compound) {
        if (compound.contains("TerrapinData")) {
            this.readCustomDataFromNbt(compound.getCompound("TerrapinData"));
        }
    }

    @Override
    @NotNull
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        var itemstack = player.getStackInHand(hand);
        if (itemstack.isIn(AMTagRegistry.TERRAPIN_BREEDABLES)){
            this.setPersistent();
        }
        return Bucketable.tryBucket(player, hand, this).orElse(super.interactMob(player, hand));
    }

    @Override
    public void updateLimbs(boolean flying) {
        final float f1 = (float) MathHelper.magnitude(this.getX() - this.prevX, 0, this.getZ() - this.prevZ);
        final float f2 = Math.min(f1 * (isSpinning() ? 4.0F : 32.0F), 1.0F);
        this.limbAnimator.updateLimbs(f2, 0.4F);
    }

    public boolean isKoopa() {
        var s = Formatting.strip(this.getName().getString());
        return s != null && s.toLowerCase().contains("koopa");
    }

    public EntityGroup getGroup() {
        return EntityGroup.AQUATIC;
    }

    @Override
    public boolean canSpawn(WorldView worldIn) {
        return worldIn.doesNotIntersectEntities(this);
    }

    static class MateGoal extends AnimalMateGoal {
        private final EntityTerrapin turtle;

        MateGoal(EntityTerrapin turtle, double speedIn) {
            super(turtle, speedIn);
            this.turtle = turtle;
        }

        @Override
        public boolean canStart() {
            return super.canStart() && !this.turtle.hasEgg();
        }

        @Override
        protected void breed() {
            var serverplayerentity = this.animal.getLovingPlayer();
            if (serverplayerentity == null && this.mate.getLovingPlayer() != null) {
                serverplayerentity = this.mate.getLovingPlayer();
            }

            if (serverplayerentity != null) {
                serverplayerentity.incrementStat(Stats.ANIMALS_BRED);
                Criteria.BRED_ANIMALS.trigger(serverplayerentity, this.animal, this.mate, this.animal);
            }
            if(mate instanceof EntityTerrapin terrapin){
                this.turtle.partnerData = new TileEntityTerrapinEgg.ParentData(terrapin.getTurtleType(), terrapin.getShellType(), terrapin.getSkinType(), terrapin.getTurtleColor(), terrapin.getShellColor(), terrapin.getSkinColor());
            }
            this.turtle.setHasEgg(true);
            this.animal.resetLoveTicks();
            this.mate.resetLoveTicks();
            this.animal.setBreedingAge(6000);
            this.mate.setBreedingAge(6000);
            Random random = this.animal.getRandom();
            if (this.world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
                this.world.spawnEntity(new ExperienceOrbEntity(this.world, this.animal.getX(), this.animal.getY(), this.animal.getZ(), random.nextInt(7) + 1));
            }
        }
    }
    
    static class LayEggGoal extends MoveToTargetPosGoal {
        private final EntityTerrapin turtle;
        private int digTime;

        LayEggGoal(EntityTerrapin turtle, double speedIn) {
            super(turtle, speedIn, 16);
            this.turtle = turtle;
        }

        @Override
        public void stop() {
            digTime = 0;
        }

        @Override
        public boolean canStart() {
            return this.turtle.hasEgg() && super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            return super.shouldContinue() && this.turtle.hasEgg();
        }

        @Override
        public double getDesiredDistanceToTarget() {
            return turtle.getWidth() + 0.5D;
        }

        @Override
        public void tick() {
            super.tick();
            var blockpos = this.turtle.getBlockPos();
            turtle.swimTimer = 1000;
            if (!this.turtle.isTouchingWater() && this.hasReached()) {
                var world = this.turtle.getWorld();
                turtle.emitGameEvent(GameEvent.BLOCK_PLACE);
                world.playSound(null, blockpos, SoundEvents.ENTITY_TURTLE_LAY_EGG, SoundCategory.BLOCKS, 0.3F, 0.9F + world.random.nextFloat() * 0.2F);
                world.setBlockState(this.targetPos.up(), AMBlockRegistry.TERRAPIN_EGG.get().getDefaultState().with(BlockTerrapinEgg.EGGS, this.turtle.random.nextInt(1) + 3), 3);
                if(world.getBlockEntity(this.targetPos.up()) instanceof TileEntityTerrapinEgg eggTe){
                    eggTe.parent1 = new TileEntityTerrapinEgg.ParentData(turtle.getTurtleType(), turtle.getShellType(), turtle.getSkinType(), turtle.getTurtleColor(), turtle.getShellColor(), turtle.getSkinColor());
                    eggTe.parent2 = turtle.partnerData == null ? eggTe.parent1 : turtle.partnerData;
                }
                this.turtle.setHasEgg(false);
                this.turtle.setLoveTicks(600);
            }
        }

        @Override
        protected boolean isTargetPos(WorldView worldIn, BlockPos pos) {
            return worldIn.isAir(pos.up()) && BlockTerrapinEgg.isProperHabitat(worldIn, pos);
        }
    }
}
