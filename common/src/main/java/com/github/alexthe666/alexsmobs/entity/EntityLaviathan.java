package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.misc.IngredientUtil;
import com.github.alexthe666.alexsmobs.registry.*;
import com.google.common.collect.Sets;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class EntityLaviathan extends AnimalEntity implements ISemiAquatic, IHerdPanic, IMultipartEntity {

    private static final TrackedData<Boolean> OBSIDIAN = DataTracker.registerData(EntityLaviathan.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> HEAD_HEIGHT = DataTracker.registerData(EntityLaviathan.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> HEAD_YROT = DataTracker.registerData(EntityLaviathan.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> CHILL_TIME = DataTracker.registerData(EntityLaviathan.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ATTACK_TICK = DataTracker.registerData(EntityLaviathan.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> HAS_BODY_GEAR = DataTracker.registerData(EntityLaviathan.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HAS_HEAD_GEAR = DataTracker.registerData(EntityLaviathan.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final Predicate<EntityCrimsonMosquito> HEALTHY_MOSQUITOES = (mob) -> {
        return mob.isAlive() && mob.getHealth() > 0 && !mob.isSick();
    };
    public static final Identifier OBSIDIAN_LOOT = new Identifier("alexsmobs", "entities/laviathan_obsidian");
    public final EntityLaviathanPart headPart;
    public final EntityLaviathanPart neckPart1;
    public final EntityLaviathanPart neckPart2;
    public final EntityLaviathanPart neckPart3;
    public final EntityLaviathanPart neckPart4;
    public final EntityLaviathanPart neckPart5;
    public final EntityLaviathanPart seat1;
    public final EntityLaviathanPart seat2;
    public final EntityLaviathanPart seat3;
    public final EntityLaviathanPart seat4;
    public final EntityLaviathanPart[] theEntireNeck;
    public final EntityLaviathanPart[] allParts;
    public final EntityLaviathanPart[] seatParts;
    private final UUID[] riderPositionMap = new UUID[4];
    public float prevHeadHeight = 0F;
    public float swimProgress = 0F;
    public float prevSwimProgress = 0F;
    public float biteProgress;
    public float prevBiteProgress;
    public int revengeCooldown = 0;
    private boolean isLandNavigator;
    private int conversionTime = 0;
    private int dismountCooldown = 0;
    private int headPeakCooldown = 0;
    private boolean hasObsidianArmor;
    private int blockBreakCounter;
    private double lastX = 0;
    private double lastZ = 0;

    public EntityLaviathan(EntityType<? extends AnimalEntity> entityType, World level) {
        super(entityType, level);
        this.setStepHeight(1.3F);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.LAVA, 0.0F);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, 0.0F);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, 0.0F);
        this.headPart = new EntityLaviathanPart(this, 1.2F, 0.9F);
        this.neckPart1 = new EntityLaviathanPart(this, 0.9F, 0.9F);
        this.neckPart2 = new EntityLaviathanPart(this, 0.9F, 0.9F);
        this.neckPart3 = new EntityLaviathanPart(this, 0.9F, 0.9F);
        this.neckPart4 = new EntityLaviathanPart(this, 0.9F, 0.9F);
        this.neckPart5 = new EntityLaviathanPart(this, 0.9F, 0.9F);
        this.seat1 = new EntityLaviathanPart(this, 0.9F, 0.4F);
        this.seat2 = new EntityLaviathanPart(this, 0.9F, 0.4F);
        this.seat3 = new EntityLaviathanPart(this, 0.9F, 0.4F);
        this.seat4 = new EntityLaviathanPart(this, 0.9F, 0.4F);
        this.theEntireNeck = new EntityLaviathanPart[]{this.neckPart1, this.neckPart2, this.neckPart3, this.neckPart4, this.neckPart5, this.headPart};
        this.allParts = new EntityLaviathanPart[]{this.neckPart1, this.neckPart2, this.neckPart3, this.neckPart4, this.neckPart5, this.headPart, this.seat1, this.seat2, this.seat3, this.seat4};
        this.seatParts = new EntityLaviathanPart[]{this.seat1, this.seat2, this.seat3, this.seat4};
        switchNavigator(true);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.laviathanSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean canLaviathanSpawn(EntityType<EntityLaviathan> p_234314_0_, WorldAccess p_234314_1_, SpawnReason p_234314_2_, BlockPos p_234314_3_, Random p_234314_4_) {
        var blockpos$mutable = p_234314_3_.mutableCopy();
        do {
            blockpos$mutable.move(Direction.UP);
        } while (p_234314_1_.getFluidState(blockpos$mutable).isIn(FluidTags.LAVA));
        return p_234314_1_.getBlockState(blockpos$mutable).isAir();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.LAVIATHAN_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.LAVIATHAN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.LAVIATHAN_HURT.get();
    }

    @Nullable
    protected Identifier getLootTableId() {
        return this.isObsidian() ? OBSIDIAN_LOOT : super.getLootTableId();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 10D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3F)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    public boolean isCollidable() {
        return this.isAlive();
    }

    @Override
    protected boolean canAddPassenger(Entity p_38390_) {
        return this.getPassengerList().size() < 4 && !this.isSubmergedIn(FluidTags.LAVA) && !this.isSubmergedIn(FluidTags.WATER);
    }

    public void push(Entity entity) {
        if (!entity.isConnectedThroughVehicle(this)) {
            entity.setVelocity(entity.getVelocity().add(this.getVelocity()));
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.LAVIATHAN_BREEDABLES);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public float getTargetingMargin() {
        return 0.0F;
    }

    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        var itemstack = player.getStackInHand(hand);
        var item = itemstack.getItem();
        if (itemstack.isIn(AMTagRegistry.LAVIATHAN_FOODSTUFFS) && this.getHealth() < this.getMaxHealth()) {
            if (!player.isCreative()) {
                itemstack.decrement(1);
            }
            this.heal(10);
            return ActionResult.SUCCESS;
        }
        if (item == AMItemRegistry.STRADDLE_HELMET.get() && !this.hasHeadGear() && !this.isBaby()) {
            if (!player.isCreative()) {
                itemstack.decrement(1);
            }
            this.setHeadGear(true);
            return ActionResult.SUCCESS;
        }
        if (item == AMItemRegistry.STRADDLE_SADDLE.get() && !this.hasBodyGear() && !this.isBaby()) {
            if (!player.isCreative()) {
                itemstack.decrement(1);
            }
            this.setBodyGear(true);
            return ActionResult.SUCCESS;
        }
        var type = super.interactMob(player, hand);
        var actionResult = itemstack.useOnEntity(player, this, hand);
        if (actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS && !isBreedingItem(itemstack) && this.hasBodyGear()) {
            if (!this.isBaby()) {
                if (!player.isSneaking()) {
                    if (!this.getWorld().isClient) {
                        player.startRiding(this);
                    }
                } else {
                    this.removeAllPassengers();
                }
                return ActionResult.SUCCESS;
            }

        }
        return type;
    }

    public int getClosestOpenSeat(Vec3d entityPos) {
        int closest = -1;
        double closestDistance = Double.MAX_VALUE;
        for (int i = 0; i < seatParts.length; i++) {
            double dist = entityPos.distanceTo(seatParts[i].getPos());
            if (closest == -1 || closestDistance > dist) {
                if (riderPositionMap[i] == null) {
                    closest = i;
                    closestDistance = dist;
                }
            }
        }
        return closest;
    }

    @Nullable
    public LivingEntity getControllingPassenger() {
        int playerPosition = -1;
        PlayerEntity player = null;
        if (this.hasHeadGear() && this.hasBodyGear()) {
            for (Entity passenger : this.getPassengerList()) {
                if (passenger instanceof PlayerEntity player2) {
                    int player2Position = getRiderPosition(passenger);
                    if (player == null || playerPosition > player2Position) {
                        player = player2;
                        playerPosition = player2Position;
                    }
                }
            }
        }
        return player;
    }

    public int getSeatRaytrace(Entity player) {
        HitResult result = player.raycast(player.distanceTo(this), 0.0F, false);
        if (result != null) {
            var vec = result.getPos();
            return getClosestOpenSeat(vec);
        }
        return -1;
    }

    @Override
    public void removePassenger(Entity entity) {
        super.removePassenger(entity);
        dismountCooldown = 40 + random.nextInt(40);
        if (entity != null && entity.getUuid() != null) {
            for (int i = 0; i < riderPositionMap.length; i++) {
                if (riderPositionMap[i] != null && riderPositionMap[i].equals(entity.getUuid())) {
                    riderPositionMap[i] = null;
                }
            }
        }
    }

    public int getRiderPosition(Entity passenger) {
        int posit = -1;
        for (int i = 0; i < this.riderPositionMap.length; i++) {
            if (this.riderPositionMap[i] != null && passenger != null && passenger.getUuid().equals(riderPositionMap[i])) {
                posit = i;
            }
        }
        return posit;
    }

    @Override
    protected void addPassenger(Entity entity) {
        int rayTrace = getSeatRaytrace(entity);
        if (rayTrace >= 0 && rayTrace < 4) {
            if (riderPositionMap[rayTrace] != null) {
                if (!this.getWorld().isClient && getWorld() instanceof ServerWorld) {
                    Entity kickOff = ((ServerWorld) this.getWorld()).getEntity(riderPositionMap[rayTrace]);
                    riderPositionMap[rayTrace] = null;
                    if (kickOff != null) {
                        kickOff.stopRiding();
                    }
                }
            }
            riderPositionMap[rayTrace] = entity.getUuid();
            super.addPassenger(entity);
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Obsidian", this.isObsidian());
        compound.putBoolean("HeadGear", this.hasHeadGear());
        compound.putBoolean("BodyGear", this.hasBodyGear());
        compound.putInt("ChillTime", this.getChillTime());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setObsidian(compound.getBoolean("Obsidian"));
        this.setHeadGear(compound.getBoolean("HeadGear"));
        this.setBodyGear(compound.getBoolean("BodyGear"));
        this.setChillTime(compound.getInt("ChillTime"));
    }

    public void positionRider(Entity passenger, Entity.PositionUpdater moveFunc) {
        if (this.hasPassenger(passenger)) {
            int posit = getRiderPosition(passenger);
            if (posit < 0 || posit > 3) {
                passenger.stopRiding();
            } else {
                EntityLaviathanPart seat = seatParts[posit];
                passenger.setPos(seat.getX(), this.getY() + this.getPassengersRidingOffset() + passenger.getHeightOffset(), seat.getZ());
            }
        }
    }

    public double getPassengersRidingOffset() {
        float f = this.limbAnimator.getPos();
        float f1 = this.limbAnimator.getSpeed();
        float f2 = 0;
        return (double) this.getHeight() - 0.4F;
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if (this.hasBodyGear()) {
            if (!this.getWorld().isClient) {
                this.dropItem(AMItemRegistry.STRADDLE_SADDLE.get());
            }
        }
        if (this.hasHeadGear()) {
            if (!this.getWorld().isClient) {
                this.dropItem(AMItemRegistry.STRADDLE_HELMET.get());
            }
        }
        this.setBodyGear(false);
        this.setHeadGear(false);
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = createNavigation(getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new MoveController(this);
            this.navigation = new BoneSerpentPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    protected EntityNavigation createNavigation(World level) {
        return new GroundPathNavigatorWide(this, level);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new AnimalAIHerdPanic(this, 1.0D) {
            @Override
            public boolean canStart() {
                return super.canStart() && !EntityLaviathan.this.hasHeadGear();
            }
        });
        this.goalSelector.add(1, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(3, new TemptGoal(this, 1.1D, IngredientUtil.ingredientFromTags(AMTagRegistry.LAVIATHAN_BREEDABLES, AMTagRegistry.LAVIATHAN_FOODSTUFFS), false));
        this.goalSelector.add(4, new AnimalAIFindWaterLava(this, 1.0D));
        this.goalSelector.add(5, new LaviathanAIRandomSwimming(this, 1.0D, 22) {
            @Override
            public boolean canStart() {
                return super.canStart() && !EntityLaviathan.this.hasHeadGear() && !EntityLaviathan.this.hasBodyGear();
            }
        });
        this.goalSelector.add(6, new LookAroundGoal(this));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
    }

    //TODO
//    public boolean canBeRiddenUnderFluidType(FluidType type, Entity rider) {
//        return true;
//    }

    @Override
    protected float getVelocityMultiplier() {
        return shouldSwim() || this.isOnSoulSpeedBlock() ? 1.0F : super.getVelocityMultiplier();
    }

    public float getWalkTargetValue(BlockPos pos, WorldView worldIn) {
        if (worldIn.getBlockState(pos).getFluidState().isIn(FluidTags.WATER) || worldIn.getBlockState(pos).getFluidState().isIn(FluidTags.LAVA)) {
            return 10.0F;
        } else {
            return this.isInLava() ? -1.0F : 0.0F;
        }
    }

    @Override
    public int getSafeFallDistance() {
        return 256;
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
    protected Vec3d getControlledMovementInput(PlayerEntity player, Vec3d deltaIn) {
        if (player.forwardSpeed != 0) {
            float f = player.forwardSpeed < 0.0F ? 0.5F : 1.0F;
            var lookVec = player.getRotationVector();
            float y = (float) lookVec.y * 0.1F;
            float waterAt = (float) EntityLaviathan.this.getMaxFluidHeight();
            float half = EntityLaviathan.this.getHeight() * 0.5F;
            if (waterAt > half) {
                y = MathHelper.clamp(waterAt - half, 0F, 0.15F);
            } else if (waterAt < half) {
                y = MathHelper.clamp(waterAt - half, -0.15F, 0F);
            }
            if (horizontalCollision) {
                y += 0.4F;
            }
            var vec3 = new Vec3d(player.sidewaysSpeed * 0.25F, y, player.forwardSpeed * (shouldSwim() ? 0.75F : 0.25F) * f);
            this.setSprinting(true);
            return vec3;
        } else {
            this.setSprinting(false);
        }
        return Vec3d.ZERO;
    }

    @Override
    protected void tickControlled(PlayerEntity player, Vec3d vec3) {
        super.tickControlled(player, vec3);
        this.setRotation(player.getYaw(), player.getPitch() * 0.5F);
        this.setHeadYaw(player.getHeadYaw());
        this.setStepHeight(1.3F);
        this.setTarget(null);
    }

    @Override
    protected float getSaddledSpeed(PlayerEntity rider) {
        return (float) (this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
    }

    //FIXME forge
//    @Override
//    public double getFluidMotionScale(FluidType type) {
//        return type == ForgeMod.WATER_TYPE.get() || type == ForgeMod.LAVA_TYPE.get() ? 1.0F : super.getFluidMotionScale(type);
//    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean prev = super.damage(source, amount);
        if (prev && source.getAttacker() != null) {
            int fleeTime = 100 + getRandom().nextInt(150);
            this.revengeCooldown = fleeTime;
            this.setChillTime(0);
        }
        return prev;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(OBSIDIAN, false);
        this.dataTracker.startTracking(HAS_BODY_GEAR, false);
        this.dataTracker.startTracking(HAS_HEAD_GEAR, false);
        this.dataTracker.startTracking(HEAD_HEIGHT, 0F);
        this.dataTracker.startTracking(HEAD_YROT, 0F);
        this.dataTracker.startTracking(CHILL_TIME, 0);
        this.dataTracker.startTracking(ATTACK_TICK, 0);
    }

    @Override
    public void travel(Vec3d travelVector) {
        boolean liquid = this.isInLava() || this.isTouchingWater();
        if (this.isLogicalSideForUpdatingMovement() && liquid) {
            this.updateVelocity(this.getMovementSpeed(), travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(hasPassengers() ? 0.5D : 0.9D));
            this.updateLimbs(false);
            if(!this.hasPassengers() && !this.isChilling()){
                this.setVelocity(this.getVelocity().add(0, -0.01F, 0));
            }
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public void updateLimbs(boolean flying) {
        float f1 = (float) MathHelper.magnitude(this.getX() - this.lastX, 0, this.getZ() - this.lastZ);
        float walkSpeed = 4.0F;
        float f2 = Math.min(f1 * walkSpeed, 1.0F);
        this.limbAnimator.updateLimbs(f2, 0.4F);
    }

    @Override
    public int getMaxLookPitchChange() {
        return 50;
    }

    @Override
    public int getMaxHeadRotation() {
        return 50;
    }

    @Override
    public int getMaxLookYawChange() {
        return 4;
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
    public EntityGroup getGroup() {
        return EntityGroup.AQUATIC;
    }

    public void tick() {
        super.tick();
        this.bodyYaw = MathHelper.stepUnwrappedAngleTowards(this.prevBodyYaw, bodyYaw, getMaxLookYawChange());
        prevSwimProgress = swimProgress;
        prevBiteProgress = biteProgress;
        prevHeadHeight = this.getHeadHeight();
        if (shouldSwim()) {
            if (swimProgress < 5F)
                swimProgress++;
        } else {
            if (swimProgress > 0F)
                swimProgress--;
        }

        if (this.isObsidian()) {
            if (!hasObsidianArmor) {
                hasObsidianArmor = true;
                this.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(30F);
            }
        } else {
            if (hasObsidianArmor) {
                hasObsidianArmor = false;
                this.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(10F);
            }
        }

        if (!this.getWorld().isClient) {
            if (!this.isObsidian() && this.isInsideWaterOrBubbleColumn()) {
                if (conversionTime < 300) {
                    conversionTime++;
                } else {
                    this.setObsidian(true);
                }

            }
            if (shouldSwim()) {
                fallDistance = 0.0F;
            }
        }
        float neckBase = 0.8F;
        if (!this.isAiDisabled()) {
            var avector3d = new Vec3d[this.allParts.length];
            for (int j = 0; j < this.allParts.length; ++j) {
                this.allParts[j].collideWithNearbyEntities();
                avector3d[j] = new Vec3d(this.allParts[j].getX(), this.allParts[j].getY(), this.allParts[j].getZ());
            }
            float yaw = this.bodyYaw * MathHelper.RADIANS_PER_DEGREE;
            float neckContraction = 2.0F * Math.abs(getHeadHeight() / 3) + 0.5F * Math.abs(getHeadYaw(0) / 50F);

            for (int l = 0; l < this.theEntireNeck.length; ++l) {
                float f = l / ((float) this.theEntireNeck.length);
                float f1 = -(2.2F + l - f * neckContraction);
                float f2 = MathHelper.sin(yaw + Maths.rad(f * getHeadYaw(0))) * (1 - Math.abs((this.getPitch() / 90F)));
                float f3 = MathHelper.cos(yaw + Maths.rad(f * getHeadYaw(0))) * (1 - Math.abs((this.getPitch() / 90F)));
                this.setPartPosition(this.theEntireNeck[l], f2 * f1, neckBase + Math.sin(f * Math.PI * 0.5F) * (getHeadHeight() * 1.1F), -f3 * f1);
            }
            this.setPartPosition(this.seat1, getXForPart(yaw, 145) * 0.75F, 2F, getZForPart(yaw, 145) * 0.75F);
            this.setPartPosition(this.seat2, getXForPart(yaw, -145) * 0.75F, 2F, getZForPart(yaw, -145) * 0.75F);
            this.setPartPosition(this.seat3, getXForPart(yaw, 35) * 0.95F, 2F, getZForPart(yaw, 35) * 0.95F);
            this.setPartPosition(this.seat4, getXForPart(yaw, -35) * 0.95F, 2F, getZForPart(yaw, -35) * 0.95F);

            if (this.getWorld().isClient && this.isChilling()) {
                if (!this.isBaby()) {
                    this.getWorld().addParticle(ParticleTypes.SMOKE, this.getX() + getXForPart(yaw, 158) * 1.75F, this.getBodyY(1), this.getZ() + getZForPart(yaw, 158) * 1.75F, 0.0D, this.random.nextDouble() / 5.0D, 0.0D);
                    this.getWorld().addParticle(ParticleTypes.SMOKE, this.getX() + getXForPart(yaw, -166) * 1.48F, this.getBodyY(1), this.getZ() + getZForPart(yaw, -166) * 1.48F, 0.0D, this.random.nextDouble() / 5.0D, 0.0D);
                    this.getWorld().addParticle(ParticleTypes.SMOKE, this.getX() + getXForPart(yaw, 14) * 1.78F, this.getBodyY(0.9), this.getZ() + getZForPart(yaw, 14) * 1.78F, 0.0D, this.random.nextDouble() / 5.0D, 0.0D);
                    this.getWorld().addParticle(ParticleTypes.SMOKE, this.getX() + getXForPart(yaw, -14) * 1.6F, this.getBodyY(1.1), this.getZ() + getZForPart(yaw, -14) * 1.6F, 0.0D, this.random.nextDouble() / 5.0D, 0.0D);
                }
                this.getWorld().addParticle(ParticleTypes.SMOKE, this.headPart.getParticleX(0.6D), this.headPart.getBodyY(0.9), this.headPart.getParticleZ(0.6D), 0.0D, this.random.nextDouble() / 5.0D, 0.0D);

            }

            for (int l = 0; l < this.allParts.length; ++l) {
                this.allParts[l].prevX = avector3d[l].x;
                this.allParts[l].prevY = avector3d[l].y;
                this.allParts[l].prevZ = avector3d[l].z;
                this.allParts[l].lastRenderX = avector3d[l].x;
                this.allParts[l].lastRenderY = avector3d[l].y;
                this.allParts[l].lastRenderZ = avector3d[l].z;
            }
        }
        if ((this.isInLava() || this.isInsideWaterOrBubbleColumn()) && this.isLandNavigator) {
            switchNavigator(false);
        }
        if (!(this.isInLava() || this.isInsideWaterOrBubbleColumn()) && !this.isLandNavigator) {
            switchNavigator(true);
        }
        if (!this.getWorld().isClient) {
            if (this.getChillTime() > 0) {
                this.setChillTime(this.getChillTime() - 1);
            } else if (this.shouldSwim()) {
                if (random.nextInt(this.hasPassengers() ? 200 : 2000) == 0 && revengeCooldown == 0) {
                    this.setChillTime(100 + random.nextInt(500));
                }
            }
            if (revengeCooldown > 0) {
                revengeCooldown--;
            }
            if (headPeakCooldown > 0) {
                headPeakCooldown--;
            }
            if (revengeCooldown == 0 && this.getAttacker() != null) {
                this.setAttacker(null);
            }
        }
        if (!this.getWorld().isClient) {
            if (this.getControllingPassenger() == null && (this.getChillTime() > 0 || this.hasHeadGear() || dismountCooldown > 0)) {
                floatLaviathan();
            }

            if (!this.isChilling() && headPeakCooldown == 0) {
                float low = getLowHeadHeight();
                this.setHeadHeight(this.getHeadHeight() + (0.5F + ((getLowHeadHeight() + getHighHeadHeight(low)) / 2F) - this.getHeadHeight()) * 0.2F);
            } else {
                if (getMaxFluidHeight() <= this.getHeight() * 0.5F && getMaxFluidHeight() >= this.getHeight() * 0.25F) {
                    float mot = (float) this.getVelocity().lengthSquared();
                    this.setHeadHeight(MathHelper.clamp(this.getHeadHeight() + 0.1F - 0.2F * mot, 0, 2));
                    headPeakCooldown = 5;
                }
            }
        }
        if (this.isChilling()) {
            boolean keepChillin = false;
            boolean startBiting = false;
            for (var entity : this.getWorld().getEntitiesByClass(EntityCrimsonMosquito.class, this.getBoundingBox().expand(30.0D), HEALTHY_MOSQUITOES)) {
                entity.setLuringLaviathan(this.getId());
                keepChillin = true;
            }
            if (keepChillin) {
                this.setChillTime(Math.max(20, this.getChillTime()));
            }
            for (var entity : this.getWorld().getEntitiesByClass(EntityCrimsonMosquito.class, this.headPart.getBoundingBox().expand(1.0D), HEALTHY_MOSQUITOES)) {
                startBiting = true;
                if (this.biteProgress == 5.0F) {
                    entity.damage(this.getDamageSources().mobAttack(this), 1000);
                    entity.setShrink(true);
                    this.setChillTime(0);
                }
            }
            if (startBiting) {
                if (this.dataTracker.get(ATTACK_TICK) <= 0 && this.biteProgress == 0) {
                    this.dataTracker.set(ATTACK_TICK, 7);
                }
            }
        }
        if (this.dataTracker.get(ATTACK_TICK) > 0) {
            this.dataTracker.set(ATTACK_TICK, this.dataTracker.get(ATTACK_TICK) - 1);
        }
        if (this.dataTracker.get(ATTACK_TICK) > 0 && this.biteProgress < 5.0F) {
            this.biteProgress++;
        }
        if (this.dataTracker.get(ATTACK_TICK) <= 0 && this.biteProgress > 0.0F) {
            this.biteProgress--;
        }
        if (dismountCooldown > 0) {
            dismountCooldown--;
        }
        if (this.hasBodyGear()) {
            List<Entity> list = this.getWorld().getOtherEntities(this, this.getBoundingBox().expand(0.2F, -0.01F, 0.2F), EntityPredicates.canBePushedBy(this));
            if (!list.isEmpty()) {
                boolean flag2 = !this.getWorld().isClient;
                for (var entity : list) {
                    if (!entity.hasPassenger(this)) {
                        if (flag2 && !(entity instanceof PlayerEntity) && !entity.hasVehicle() && entity.getWidth() < this.getWidth() && !(entity instanceof EntityLaviathan) && !(entity instanceof Monster) && entity instanceof MobEntity && this.canAddPassenger(entity) && !(entity instanceof WaterCreatureEntity)) {
                            entity.startRiding(this);
                        } else {
                            this.push(entity);
                        }
                    }
                }
            }
        }
        if (this.hasPassengers() && !this.getWorld().isClient && age % 40 == 0 && this.getPassengerList().size() > 3) {
            for (Entity entity : this.getPassengerList()) {
                if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
                    AMAdvancementTriggerRegistry.LAVIATHAN_FOUR_PASSENGERS.trigger(serverPlayerEntity);
                }
            }
        }
        lastX = this.getX();
        lastZ = this.getZ();
    }

    @Override
    public void mobTick() {
        super.mobTick();
        breakBlock();
    }

    public void breakBlock() {
        if (this.blockBreakCounter > 0) {
            --this.blockBreakCounter;
            return;
        }
        boolean flag = false;
        //FIXME forge
//        if (!this.getWorld().isClient && this.hasPassengers() && this.blockBreakCounter == 0 && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(getWorld(), this)) {
//            for (int a = (int) Math.round(this.getBoundingBox().minX); a <= (int) Math.round(this.getBoundingBox().maxX); a++) {
//                for (int b = (int) Math.round(this.getBoundingBox().minY) - 1; (b <= (int) Math.round(this.getBoundingBox().maxY) + 1) && (b <= 127); b++) {
//                    for (int c = (int) Math.round(this.getBoundingBox().minZ); c <= (int) Math.round(this.getBoundingBox().maxZ); c++) {
//                        var pos = new BlockPos(a, b, c);
//                        var state = getWorld().getBlockState(pos);
//                        var fluidState = getWorld().getFluidState(pos);
//                        var block = state.getBlock();
//                        if (!state.isAir() && !state.getOutlineShape(getWorld(), pos).isEmpty() && state.isIn(AMTagRegistry.LAVIATHAN_BREAKABLES) && fluidState.isEmpty()) {
//                            if (block != Blocks.AIR) {
//                                this.setVelocity(this.getVelocity().multiply(0.6F, 1, 0.6F));
//                                flag = true;
//                                getWorld().breakBlock(pos, true);
//                            }
//                        }
//                    }
//                }
//            }
//        }
        if (flag) {
            blockBreakCounter = 10;
        }
    }


    public float getLowHeadHeight() {
        float checkAt = 0F;
        while (checkAt > -3F && !isHeadInWall((float) this.getY() + checkAt) && !isHeadInLava((float) this.getY() + checkAt)) {
            checkAt -= 0.2F;
        }
        return checkAt;
    }

    public float getHighHeadHeight(float low) {
        float checkAt = 3F;
        while (checkAt > 0) {
            if (isHeadInWall((float) this.getY() + checkAt) && !isHeadInLava((float) this.getY() + checkAt)) {
                break;
            }
            checkAt -= 0.2F;
        }

        return checkAt;
    }

    public boolean isHeadInWall(float offset) {
        if (this.noClip) {
            return false;
        } else {
            float f = 0.8F;
            var vec3 = new Vec3d(headPart.getX(), offset, headPart.getZ());
            var axisalignedbb = Box.of(vec3, f, 1.0E-6D, f);
            return this.getWorld().getStatesInBox(axisalignedbb).filter(Predicate.not(AbstractBlock.AbstractBlockState::isAir)).anyMatch((p_185969_) -> {
                BlockPos blockpos = AMBlockPos.fromVec3(vec3);
                return p_185969_.shouldSuffocate(this.getWorld(), blockpos) && VoxelShapes.matchesAnywhere(p_185969_.getCollisionShape(this.getWorld(), blockpos).offset(vec3.x, vec3.y, vec3.z), VoxelShapes.cuboid(axisalignedbb), BooleanBiFunction.AND);
            });
        }
    }

    public boolean isHeadInLava(float offset) {
        if (this.noClip) {
            return false;
        } else {
            float f = 0.8F;
            BlockPos pos = AMBlockPos.fromCoords(headPart.getX(), offset, headPart.getZ());
            return !getWorld().getFluidState(pos).isEmpty();
        }
    }

    private void floatLaviathan() {
        if (this.shouldSwim()) {
            if (getMaxFluidHeight() >= this.getHeight()) {
                this.setVelocity(this.getVelocity().x, 0.12F, this.getVelocity().z);
            } else if (getMaxFluidHeight() >= this.getHeight() * 0.5F) {
                this.setVelocity(this.getVelocity().x, 0.08F, this.getVelocity().z);
            } else {
                this.setVelocity(this.getVelocity().x, 0.0F, this.getVelocity().z);
            }
        }
    }

    @Override
    public Vec3d updatePassengerForDismount(LivingEntity livingEntity) {
        float expand = this.getWidth() + 1.0F;
        var avector3d = new Vec3d[]{getPassengerDismountOffset(expand, livingEntity.getWidth(), livingEntity.getYaw()), getPassengerDismountOffset(expand, livingEntity.getWidth(), livingEntity.getYaw() - 22.5F), getPassengerDismountOffset(expand, livingEntity.getWidth(), livingEntity.getYaw() + 22.5F), getPassengerDismountOffset(expand, livingEntity.getWidth(), livingEntity.getYaw() - 45.0F), getPassengerDismountOffset(expand, livingEntity.getWidth(), livingEntity.getYaw() + 45.0F)};
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

        for (var blockpos : set) {
            if (!this.getWorld().getFluidState(blockpos).isIn(FluidTags.LAVA)) {
                double d3 = this.getWorld().getDismountHeight(blockpos);
                if (Dismounting.canDismountInBlock(d3)) {
                    var vector3d1 = Vec3d.ofCenter(blockpos, d3);

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

    public float getWaterLevelAbove() {
        var axisalignedbb = this.getBoundingBox();
        int i = MathHelper.floor(axisalignedbb.minX);
        int j = MathHelper.ceil(axisalignedbb.maxX);
        int k = MathHelper.floor(axisalignedbb.maxY);
        int l = MathHelper.ceil(axisalignedbb.maxY);
        int i1 = MathHelper.floor(axisalignedbb.minZ);
        int j1 = MathHelper.ceil(axisalignedbb.maxZ);
        var blockpos$mutable = new BlockPos.Mutable();

        label39:
        for (int k1 = k; k1 < l; ++k1) {
            float f = 0.0F;

            for (int l1 = i; l1 < j; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    blockpos$mutable.set(l1, k1, i2);
                    var fluidstate = this.getWorld().getFluidState(blockpos$mutable);
                    if (fluidstate.isIn(FluidTags.WATER) || fluidstate.isIn(FluidTags.LAVA)) {
                        f = Math.max(f, fluidstate.getHeight(this.getWorld(), blockpos$mutable));
                    }

                    if (f >= 1.0F) {
                        continue label39;
                    }
                }
            }

            if (f < 1.0F) {
                return (float) blockpos$mutable.getY() + f;
            }
        }

        return (float) (l + 1);
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean canSpawn(WorldView worldIn) {
        return worldIn.doesNotIntersectEntities(this);
    }

    public boolean shouldSwim() {
        return getMaxFluidHeight() >= 0.1F || this.isInLava() || this.isInsideWaterOrBubbleColumn();
    }

    private float getXForPart(float yaw, float degree) {
        return MathHelper.sin((float) (yaw + Math.toRadians(degree)));
    }

    private float getZForPart(float yaw, float degree) {
        return -MathHelper.cos((float) (yaw + Math.toRadians(degree)));
    }

    public float getHeadHeight() {
        return MathHelper.clamp(this.dataTracker.get(HEAD_HEIGHT), -3, 3);
    }

    public void setHeadHeight(float height) {
        this.dataTracker.set(HEAD_HEIGHT, MathHelper.clamp(height, -3, 3));
    }

    public boolean isObsidian() {
        return this.dataTracker.get(OBSIDIAN);
    }

    public void setObsidian(boolean obsidian) {
        this.dataTracker.set(OBSIDIAN, obsidian);
    }

    public boolean hasHeadGear() {
        return this.dataTracker.get(HAS_HEAD_GEAR);
    }

    public void setHeadGear(boolean headGear) {
        this.dataTracker.set(HAS_HEAD_GEAR, headGear);
    }

    public boolean hasBodyGear() {
        return this.dataTracker.get(HAS_BODY_GEAR);
    }

    public void setBodyGear(boolean bodyGear) {
        this.dataTracker.set(HAS_BODY_GEAR, bodyGear);
    }

    public int getChillTime() {
        return this.dataTracker.get(CHILL_TIME);
    }

    public void setChillTime(int chillTime) {
        this.dataTracker.set(CHILL_TIME, chillTime);
    }

    public float getHeadYaw(float interp) {
        float f;
        if (interp == 0.0F) {
            f = getHeadYaw() - this.bodyYaw;
        } else {
            float bodyYaw1 = this.prevBodyYaw + (this.bodyYaw - this.prevBodyYaw) * interp;
            float yHeadRot1 = this.prevHeadYaw + (getHeadYaw() - this.prevHeadYaw) * interp;
            f = yHeadRot1 - bodyYaw1;
        }
        return MathHelper.clamp(MathHelper.wrapDegrees(f), -50, 50);
    }

    private void setPartPosition(EntityLaviathanPart part, double offsetX, double offsetY, double offsetZ) {
        part.setPos(this.getX() + offsetX * part.scale, this.getY() + offsetY * part.scale, this.getZ() + offsetZ * part.scale);
    }

    //FIXME forge
//
//    @Override
//    public net.minecraftforge.entity.PartEntity<?>[] getParts() {
//        return this.allParts;
//    }

    public boolean attackEntityPartFrom(EntityLaviathanPart part, DamageSource source, float amount) {
        return this.damage(source, amount);
    }

    @Override
    public boolean shouldEnterWater() {
        return !this.hasPassengers();
    }

    @Override
    public boolean shouldLeaveWater() {
        return this.hasPassengers();
    }

    @Override
    public boolean shouldStopMoving() {
        return this.hasPassengers();
    }

    @Override
    public int getWaterSearchRange() {
        return 15;
    }

    private double getMaxFluidHeight() {
        return Math.max(this.getFluidHeight(FluidTags.LAVA), this.getFluidHeight(FluidTags.WATER));
    }

    public boolean isChilling() {
        return this.getChillTime() > 0 && this.getMaxFluidHeight() <= this.getHeight() * 0.5F;
    }

    public void scaleParts() {
        for (var parts : allParts) {
            float prev = parts.scale;
            parts.scale = this.isBaby() ? 0.5F : 1F;
            if (prev != parts.scale) {
                parts.calculateDimensions();
            }
        }
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        scaleParts();
    }

    public Vec3d getLureMosquitoPos() {
        return new Vec3d(this.headPart.getX(), this.headPart.getBodyY(0.4F), this.headPart.getZ());
    }

    @Override
    public void onPanic() {
    }

    @Override
    public boolean canPanic() {
        return true;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.LAVIATHAN.get().create(world);
    }

    static class MoveController extends MoveControl {
        private final EntityLaviathan laviathan;

        public MoveController(EntityLaviathan dolphinIn) {
            super(dolphinIn);
            this.laviathan = dolphinIn;
        }

        public void tick() {
            float speed = (float) (this.speed * 3 * laviathan.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
            if (this.state == State.MOVE_TO && (!this.laviathan.getNavigation().isIdle() || laviathan.getControllingPassenger() != null)) {
                double lvt_1_1_ = this.targetX - laviathan.getX();
                double lvt_3_1_ = this.targetY - laviathan.getY();
                double lvt_5_1_ = this.targetZ - laviathan.getZ();

                double lvt_7_1_ = lvt_1_1_ * lvt_1_1_ + lvt_3_1_ * lvt_3_1_ + lvt_5_1_ * lvt_5_1_;
                if (lvt_7_1_ < 2.5F) {
                    this.laviathan.setForwardSpeed(0.0F);
                } else {
                    float lvt_9_1_ = (float) (MathHelper.atan2(lvt_5_1_, lvt_1_1_) * 57.2957763671875D) - 90.0F;
                    this.laviathan.setYaw(this.wrapDegrees(this.laviathan.getYaw(), lvt_9_1_, 5F));
                    this.laviathan.setHeadYaw(this.wrapDegrees(this.laviathan.getHeadYaw(), lvt_9_1_, 90.0F));
                    if (laviathan.shouldSwim()) {
                        laviathan.setMovementSpeed(speed * 0.03F);
                        float lvt_11_1_ = -((float) (MathHelper.atan2(lvt_3_1_, MathHelper.sqrt((float) (lvt_1_1_ * lvt_1_1_ + lvt_5_1_ * lvt_5_1_))) * 57.2957763671875D));
                        lvt_11_1_ = MathHelper.clamp(MathHelper.wrapDegrees(lvt_11_1_), -85.0F, 85.0F);
                        laviathan.setPitch(this.wrapDegrees(laviathan.getPitch(), lvt_11_1_, 25.0F));
                        float lvt_12_1_ = MathHelper.cos(laviathan.getPitch() * 0.017453292F);
                        float lvt_13_1_ = MathHelper.sin(laviathan.getPitch() * 0.017453292F);
                        laviathan.forwardSpeed = lvt_12_1_ * speed;
                        laviathan.upwardSpeed = -lvt_13_1_ * speed;
                    } else {
                        laviathan.setMovementSpeed(speed * 0.1F);
                    }

                }
            } else if (!laviathan.getWorld().getBlockState(this.laviathan.getBlockPos().up()).getFluidState().isEmpty() && laviathan.getChillTime() <= 0) {
                this.laviathan.setVelocity(this.laviathan.getVelocity().add(0.0D, -0.05D, 0.0D));
            }
        }
    }
}
