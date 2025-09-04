package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.control.YawAdjustingLookControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.ai.pathing.SwimNavigation;
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
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class EntityCachalotWhale extends AnimalEntity implements IMultipartEntity {

    private static final TargetPredicate REWARD_PLAYER_PREDICATE = TargetPredicate.createNonAttackable().setBaseMaxDistance(50.0D).ignoreVisibility();
    private static final TrackedData<Boolean> CHARGING = DataTracker.registerData(EntityCachalotWhale.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SLEEPING = DataTracker.registerData(EntityCachalotWhale.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BEACHED = DataTracker.registerData(EntityCachalotWhale.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> ALBINO = DataTracker.registerData(EntityCachalotWhale.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> DESPAWN_BEACH = DataTracker.registerData(EntityCachalotWhale.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> GRABBING = DataTracker.registerData(EntityCachalotWhale.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HOLDING_SQUID_LEFT = DataTracker.registerData(EntityCachalotWhale.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> CAUGHT_ID = DataTracker.registerData(EntityCachalotWhale.class, TrackedDataHandlerRegistry.INTEGER);
    public final double[][] ringBuffer = new double[64][3];
    public final EntityCachalotPart headPart;
    public final EntityCachalotPart bodyFrontPart;
    public final EntityCachalotPart bodyPart;
    public final EntityCachalotPart tail1Part;
    public final EntityCachalotPart tail2Part;
    public final EntityCachalotPart tail3Part;
    public final EntityCachalotPart[] whaleParts;
    private final boolean hasAlbinoAttribute = false;
    public int ringBufferIndex = -1;
    public float prevChargingProgress;
    public float chargeProgress;
    public float prevSleepProgress;
    public float sleepProgress;
    public float prevBeachedProgress;
    public float beachedProgress;
    public float prevGrabProgress;
    public float grabProgress;
    public int grabTime = 0;
    private boolean receivedEcho = false;
    private boolean waitForEchoFlag = true;
    private int echoTimer = 0;
    private boolean prevEyesInWater = false;
    private int spoutTimer = 0;
    private int chargeCooldown = 0;
    private float whaleSpeedMod = 1F;
    private int rewardTime = 0;
    private PlayerEntity rewardPlayer;
    private int blockBreakCounter;
    private int despawnDelay = 47999;
    private int echoSoundCooldown = 0;
    private boolean hasRewardedPlayer = false;

    public EntityCachalotWhale(EntityType<? extends EntityCachalotWhale> type, World world) {
        super(type, world);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.moveControl = new AnimalSwimMoveControllerSink(this, 1, 1, 6);
        this.lookControl = new YawAdjustingLookControl(this, 4);
        this.headPart = new EntityCachalotPart(this, 3.0F, 3.5F);
        this.bodyFrontPart = new EntityCachalotPart(this, 4.0F, 4.0F);
        this.bodyPart = new EntityCachalotPart(this, 5.0F, 4.0F);
        this.tail1Part = new EntityCachalotPart(this, 4.0F, 3.0F);
        this.tail2Part = new EntityCachalotPart(this, 3.0F, 2.0F);
        this.tail3Part = new EntityCachalotPart(this, 3.0F, 0.7F);
        this.whaleParts = new EntityCachalotPart[]{this.headPart, this.bodyFrontPart, this.bodyPart, this.tail1Part, this.tail2Part, this.tail3Part};
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 160.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 1.2F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 30F);
    }

    public static <T extends MobEntity> boolean canCachalotWhaleSpawn(EntityType<T> entityType, ServerWorldAccess iServerWorld, SpawnReason reason, BlockPos pos, Random random) {
        var up = pos;
        while(up.getY() < iServerWorld.getTopY() && iServerWorld.getFluidState(up).isIn(FluidTags.WATER)){
            up = up.up();
        }
        return iServerWorld.getFluidState(up.down()).isIn(FluidTags.WATER) && up.getY() < iServerWorld.getSeaLevel() + 15 && iServerWorld.isSkyVisible(up);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceToClosestPlayer) {
        return !this.isSleeping() && !this.isCharging() && !this.isDespawnBeach() && !isAlbino();
    }

    private boolean canDespawn() {
        return isDespawnBeach();
    }

    private void tryDespawn() {
        if (this.canDespawn()) {
            this.despawnDelay = this.despawnDelay - 1;
            if (this.despawnDelay <= 0) {
                this.detachLeash(true, false);
                this.remove(RemovalReason.DISCARDED);
            }
        }
    }

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.CACHALOT_WHALE_IDLE.get();
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.CACHALOT_WHALE_HURT.get();
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.CACHALOT_WHALE_HURT.get();
    }

    public void scaleParts() {
        for (var parts : whaleParts) {
            float prev = parts.scale;
            parts.scale = this.isBaby() ? 0.5F : 1F;
            if (prev != parts.scale) {
                parts.calculateDimensions();
            }
        }
    }

    @Override
    public boolean canHit() {
        return true;
    }

    @Override
    public void tickCramming() {
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        return super.interactMob(player, hand);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Albino", this.isAlbino());
        compound.putBoolean("Beached", this.isBeached());
        compound.putBoolean("BeachedDespawnFlag", this.isDespawnBeach());
        compound.putBoolean("GivenReward", this.hasRewardedPlayer);
        compound.putInt("DespawnDelay", this.despawnDelay);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setAlbino(compound.getBoolean("Albino"));
        this.setBeached(compound.getBoolean("Beached"));
        this.setDespawnBeach(compound.getBoolean("BeachedDespawnFlag"));
        if (compound.contains("DespawnDelay", 99)) {
            this.despawnDelay = compound.getInt("DespawnDelay");
        }
        this.hasRewardedPlayer = compound.getBoolean("GivenReward");

    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CHARGING, false);
        this.dataTracker.startTracking(SLEEPING, false);
        this.dataTracker.startTracking(BEACHED, false);
        this.dataTracker.startTracking(ALBINO, false);
        this.dataTracker.startTracking(GRABBING, false);
        this.dataTracker.startTracking(HOLDING_SQUID_LEFT, false);
        this.dataTracker.startTracking(DESPAWN_BEACH, false);
        this.dataTracker.startTracking(CAUGHT_ID, -1);
    }

    public boolean hasCaughtSquid() {
        return this.dataTracker.get(CAUGHT_ID) != -1;
    }

    private void setCaughtSquidId(int i) {
        this.dataTracker.set(CAUGHT_ID, i);
    }

    @Nullable
    public Entity getCaughtSquid() {
        if (!this.hasCaughtSquid()) {
            return null;
        } else {
            return this.getWorld().getEntityById(this.dataTracker.get(CAUGHT_ID));
        }
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new AIBreathe());
        this.goalSelector.add(1, new MoveIntoWaterGoal(this));
        this.goalSelector.add(2, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(3, new AnimalAIFollowParentRanged(this, 1.1F, 32, 10));
        this.goalSelector.add(4, new AnimalAIRandomSwimming(this, 0.6D, 10, 24, true) {
            @Override
            public boolean canStart() {
                return !EntityCachalotWhale.this.isSleeping() && !EntityCachalotWhale.this.isBeached() && super.canStart();
            }
        });
        this.goalSelector.add(5, new LookAroundGoal(this));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 20.0F));
        this.goalSelector.add(7, new ChaseBoatGoal(this));
        this.targetSelector.add(1, (new AnimalAIHurtByTargetNotBaby(this).setGroupRevenge()));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, LivingEntity.class, 30, false, true, AMEntityRegistry.buildPredicateFromTag(AMTagRegistry.CACHALOT_WHALE_TARGETS)) {
            @Override
            public boolean canStart() {
                return !EntityCachalotWhale.this.isSleeping() && !EntityCachalotWhale.this.isBeached() && super.canStart();
            }
        });
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new SwimNavigation(this, worldIn);
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
        //FiXME FORGE
//        if (!this.getWorld().isClient && this.blockBreakCounter == 0 && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(getWorld(), this)) {
//            final TagKey<Block> breakables = this.isCharging() && this.getTarget() != null && AMConfig.cachalotDestruction ? AMTagRegistry.CACHALOT_WHALE_BREAKABLES : AMTagRegistry.ORCA_BREAKABLES;
//            for (int a = (int) Math.round(this.getBoundingBox().minX); a <= (int) Math.round(this.getBoundingBox().maxX); a++) {
//                for (int b = (int) Math.round(this.getBoundingBox().minY) - 1; (b <= (int) Math.round(this.getBoundingBox().maxY) + 1) && (b <= 127); b++) {
//                    for (int c = (int) Math.round(this.getBoundingBox().minZ); c <= (int) Math.round(this.getBoundingBox().maxZ); c++) {
//                        final BlockPos pos = new BlockPos(a, b, c);
//                        final BlockState state = getWorld().getBlockState(pos);
//                        final FluidState fluidState = getWorld().getFluidState(pos);
//                        if (!state.isAir() && !state.getOutlineShape(getWorld(), pos).isEmpty() && state.isIn(breakables) && fluidState.isEmpty()) {
//                            final Block block = state.getBlock();
//                            if (block != Blocks.AIR) {
//                                this.setVelocity(this.getVelocity().multiply(0.6F, 1, 0.6F));
//                                flag = true;
//                                getWorld().breakBlock(pos, true);
//                                if (state.isIn(BlockTags.ICE)) {
//                                    getWorld().setBlockState(pos, Blocks.WATER.getDefaultState());
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
        if (flag) {
            blockBreakCounter = this.isCharging() && this.getTarget() != null ? 2 : 20;
        }
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.canMoveVoluntarily() && this.isTouchingWater()) {
            this.updateVelocity(this.getMovementSpeed(), travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.9D));

        } else {
            super.travel(travelVector);
        }
    }

    private void spawnSpoutParticles() {
        if (this.isAlive()) {
            final float radius = this.headPart.getWidth() * 0.5F;
            for (int j = 0; j < 5 + random.nextInt(4); ++j) {
                final float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
                final double extraX = (radius * (1F + random.nextFloat() * 0.13F)) * MathHelper.sin(MathHelper.PI + angle) + (random.nextFloat() - 0.5F) + this.getVelocity().x * 2F;
                final double extraZ = (radius * (1F + random.nextFloat() * 0.13F)) * MathHelper.cos(angle) + (random.nextFloat() - 0.5F) + this.getVelocity().z * 2F;
                final double motX = this.random.nextGaussian();
                final double motZ = this.random.nextGaussian();
                this.getWorld().addParticle(AMParticleRegistry.WHALE_SPLASH.get(), this.headPart.getX() + extraX, this.headPart.getY() + this.headPart.getHeight(), this.headPart.getZ() + extraZ, motX * 0.1F + this.getVelocity().x, 2F, motZ * 0.1F + this.getVelocity().z);
            }
        }
    }

    public boolean isCharging() {
        return this.dataTracker.get(CHARGING);
    }

    public void setCharging(boolean charging) {
        this.dataTracker.set(CHARGING, charging);
    }

    @Override
    public boolean isSleeping() {
        return this.dataTracker.get(SLEEPING);
    }

    public void setSleeping(boolean charging) {
        this.dataTracker.set(SLEEPING, charging);
    }

    public boolean isBeached() {
        return this.dataTracker.get(BEACHED);
    }

    public void setBeached(boolean charging) {
        this.dataTracker.set(BEACHED, charging);
    }

    public boolean isGrabbing() {
        return this.dataTracker.get(GRABBING);
    }

    public void setGrabbing(boolean charging) {
        this.dataTracker.set(GRABBING, charging);
    }

    public boolean isHoldingSquidLeft() {
        return this.dataTracker.get(HOLDING_SQUID_LEFT);
    }

    public void setHoldingSquidLeft(boolean charging) {
        this.dataTracker.set(HOLDING_SQUID_LEFT, charging);
    }

    public boolean isAlbino() {
        return this.dataTracker.get(ALBINO);
    }

    public void setAlbino(boolean albino) {
        boolean prev = isAlbino();
        if (!prev && albino) {
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(230.0D);
            this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(45.0D);
            this.setHealth(230.0F);
        } else {
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(160.0D);
            this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(30.0D);
        }
        this.dataTracker.set(ALBINO, albino);
    }

    public boolean isDespawnBeach() {
        return this.dataTracker.get(DESPAWN_BEACH);
    }

    public void setDespawnBeach(boolean despawn) {
        this.dataTracker.set(DESPAWN_BEACH, despawn);
    }

    @Override
    protected float getSoundVolume() {
        return this.isSilent() ? 0 : (float) AMConfig.cachalotVolume;
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        scaleParts();

        if (echoSoundCooldown > 0) {
            echoSoundCooldown--;
        }
        if (this.isSleeping()) {
            this.getNavigation().stop();
            this.setPitch(-90);
            this.whaleSpeedMod = 0;
            if (this.isSubmergedIn(FluidTags.WATER) && this.getAir() < 200) {
                this.setVelocity(this.getVelocity().add(0, 0.06, 0));
            } else {
                var waterPos = this.getBlockPos();
                while (getWorld().getFluidState(waterPos).isIn(FluidTags.WATER) && waterPos.getY() < 255) {
                    waterPos = waterPos.up();
                }
                if (waterPos.getY() - this.getY() < (isBaby() ? 7 : 12)) {
                    this.setVelocity(this.getVelocity().add(0, -0.06, 0));
                }
                if (random.nextInt(100) == 0) {
                    this.setVelocity(this.getVelocity().add(0, random.nextGaussian() * 0.06, 0));
                }
            }
        } else {
            if (this.whaleSpeedMod == 0) {
                this.whaleSpeedMod = 1;
            }
        }
        float rPitch = -((float) this.getVelocity().y * MathHelper.DEGREES_PER_RADIAN);
        if (this.isGrabbing()) {
            this.setPitch(0);
        } else {
            this.setPitch(MathHelper.clamp(rPitch, -90, 90));
        }
        if (this.isOnGround() && !this.isInsideWaterOrBubbleColumn()) {
            this.setBeached(true);
            this.setPitch(0);
            this.setSleeping(false);
        }
        if (this.isBeached()) {
            this.whaleSpeedMod = 0;
            this.setVelocity(this.getVelocity().multiply(0.5, 1F, 0.5));
            if (this.isSubmergedIn(FluidTags.WATER)) {
                var entity = this.getWorld().getClosestPlayer(REWARD_PLAYER_PREDICATE, this);
                if (this.getLastAttacker() != entity) {
                    rewardPlayer = entity;
                }
                this.despawnDelay = 47999;
                this.setBeached(false);
            }
        }
        if (rewardPlayer != null && !hasRewardedPlayer && this.isInsideWaterOrBubbleColumn()) {
            final double d0 = rewardPlayer.getX() - this.getX();
            final double d1 = rewardPlayer.getEyeY() - this.getEyeY();
            final double d2 = rewardPlayer.getZ() - this.getZ();
            final double d3 = MathHelper.sqrt((float) (d0 * d0 + d2 * d2));
            final float targetYaw = (float) (MathHelper.atan2(d2, d0) * MathHelper.DEGREES_PER_RADIAN) - 90.0F;
            final float targetPitch = (float) (-(MathHelper.atan2(d1, d3) * MathHelper.DEGREES_PER_RADIAN));
            this.setYaw((this.getYaw() + MathHelper.clamp(targetYaw - this.getYaw(), -2, 2)));
            this.setPitch((this.getPitch() + MathHelper.clamp(targetPitch - this.getPitch(), -2, 2)));
            this.bodyYaw = getYaw();
            this.whaleSpeedMod = 0.1F;
            this.getMoveControl().moveTo(rewardPlayer.getX(), rewardPlayer.getY(), rewardPlayer.getZ(), 0.5D);
            if (this.distanceTo(rewardPlayer) < 10F) {
                if (!this.getWorld().isClient) {
                    final Vec3d vec = this.getMouthVec();
                    final ItemEntity itementity = new ItemEntity(this.getWorld(), vec.x, vec.y, vec.z, new ItemStack(AMItemRegistry.AMBERGRIS.get(), 2 + random.nextInt(2)));
                    itementity.setToDefaultPickupDelay();
                    getWorld().spawnEntity(itementity);
                }
                hasRewardedPlayer = true;
                rewardPlayer = null;
            }
        }

        prevChargingProgress = chargeProgress;
        prevSleepProgress = sleepProgress;
        prevBeachedProgress = beachedProgress;
        prevGrabProgress = grabProgress;
        if (this.age % 200 == 0) {
            this.heal(2);
        }

        if (isCharging()) {
            if (this.chargeProgress < 10F)
                this.chargeProgress++;
        } else {
            if (this.chargeProgress > 0F)
                this.chargeProgress--;
        }

        if (isSleeping()) {
            if (this.sleepProgress < 10F)
                this.sleepProgress++;
        } else {
            if (this.sleepProgress > 0F)
                this.sleepProgress--;
        }

        if (isBeached()) {
            if (this.beachedProgress < 10F)
                this.beachedProgress++;
        } else {
            if (this.beachedProgress > 0F)
                this.beachedProgress--;
        }

        if (isGrabbing()) {
            if (this.grabProgress < 10F)
                this.grabProgress++;

            grabTime++;
        } else {
            if (this.grabProgress > 0F)
                this.grabProgress--;

            grabTime = 0;
        }

        this.headYaw = this.getYaw();
        this.bodyYaw = this.getYaw();

        if (!this.isAiDisabled()) {
            if (this.ringBufferIndex < 0) {
                for (int i = 0; i < this.ringBuffer.length; ++i) {
                    this.ringBuffer[i][0] = this.getYaw();
                    this.ringBuffer[i][1] = this.getY();
                }
            }
            this.ringBufferIndex++;
            if (this.ringBufferIndex == this.ringBuffer.length) {
                this.ringBufferIndex = 0;
            }
            this.ringBuffer[this.ringBufferIndex][0] = this.getYaw();
            this.ringBuffer[ringBufferIndex][1] = this.getY();
            Vec3d[] avector3d = new Vec3d[this.whaleParts.length];

            for (int j = 0; j < this.whaleParts.length; ++j) {
                this.whaleParts[j].collideWithNearbyEntities();
                avector3d[j] = new Vec3d(this.whaleParts[j].getX(), this.whaleParts[j].getY(), this.whaleParts[j].getZ());
            }
            final float f15 = (float) (this.getMovementOffsets(5, 1.0F)[1] - this.getMovementOffsets(10, 1.0F)[1]) * 10.0F * MathHelper.RADIANS_PER_DEGREE;
            final float f16 = MathHelper.cos(f15);
            final float f17 = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
            final float pitch = this.getPitch() * MathHelper.RADIANS_PER_DEGREE;
            final float rotateXDiv90 = Math.abs(this.getPitch() / 90F);
            final float f3 = MathHelper.sin(f17) * (1 - rotateXDiv90);
            final float f18 = MathHelper.cos(f17) * (1 - rotateXDiv90);

            this.setPartPosition(this.bodyPart, f3 * 0.5F, -pitch * 0.5F, -f18 * 0.5F);
            this.setPartPosition(this.bodyFrontPart, (f3) * -3.5F, -pitch * 3F, (f18) * 3.5F);
            this.setPartPosition(this.headPart, f3 * -7F, -pitch * 5F, -f18 * -7F);
            double[] adouble = this.getMovementOffsets(5, 1.0F);

            for (int k = 0; k < 3; ++k) {
                final EntityCachalotPart enderdragonpartentity;
                if (k == 0) {
                    enderdragonpartentity = this.tail1Part;
                } else if (k == 1) {
                    enderdragonpartentity = this.tail2Part;
                } else {
                    enderdragonpartentity = this.tail3Part;
                }

                final double[] adouble1 = this.getMovementOffsets(15 + k * 5, 1.0F);
                final float f7 = this.getYaw() * MathHelper.RADIANS_PER_DEGREE + (float) MathHelper.wrapDegrees(adouble1[0] - adouble[0]) * MathHelper.RADIANS_PER_DEGREE;
                final float f19 = 1 - Math.abs(this.getPitch() / 90F);
                final float f20 = MathHelper.sin(f7) * f19;
                final float f21 = MathHelper.cos(f7) * f19;
                final float f22 = -3.6F;
                final float f23 = (float) (k + 1) * f22 - 2F;
                this.setPartPosition(enderdragonpartentity, -(f3 * 0.5F + f20 * f23) * f16, pitch * 1.5F * (k + 1), (f18 * 0.5F + f21 * f23) * f16);
            }

            for (int l = 0; l < this.whaleParts.length; ++l) {
                this.whaleParts[l].prevX = avector3d[l].x;
                this.whaleParts[l].prevY = avector3d[l].y;
                this.whaleParts[l].prevZ = avector3d[l].z;
                this.whaleParts[l].lastRenderX = avector3d[l].x;
                this.whaleParts[l].lastRenderY = avector3d[l].y;
                this.whaleParts[l].lastRenderZ = avector3d[l].z;
            }
        }
        if (!this.getWorld().isClient) {
            var target = this.getTarget();
            if (target == null || !target.isAlive()) {
                this.setGrabbing(false);
                whaleSpeedMod = this.isSleeping() ? 0 : 1;
                this.setCharging(false);
                this.setCaughtSquidId(-1);
            } else if (!isBeached() && !isSleeping() && rewardPlayer == null) {
                if (isGrabbing() && this.getTarget().isAlive()) {
                    this.setCaughtSquidId(this.getTarget().getId());
                    whaleSpeedMod = 0.1F;
                    final float scale = this.isBaby() ? 0.5F : 1F;
                    final float offsetAngle = -(float) Math.cos(grabTime * 0.3F) * 0.1F * grabProgress;
                    final float renderYaw = (float) this.getMovementOffsets(0, 1.0F)[0];
                    final Vec3d extraVec = new Vec3d(0, 0, -3F).rotateX(-this.getPitch() * MathHelper.RADIANS_PER_DEGREE).rotateY(-renderYaw * MathHelper.RADIANS_PER_DEGREE);
                    final Vec3d backOfHead = this.headPart.getPos().add(extraVec);
                    final Vec3d swingVec = new Vec3d(isHoldingSquidLeft() ? 1.4F : -1.4F, -0.1, 3F).rotateX(-this.getPitch() * MathHelper.RADIANS_PER_DEGREE).rotateY(-renderYaw * MathHelper.RADIANS_PER_DEGREE).rotateY(offsetAngle);
                    final Vec3d mouth = backOfHead.add(swingVec).multiply(scale);
                    this.getTarget().setPos(mouth.x, mouth.y, mouth.z);
                    if (isHoldingSquidLeft()) {
                        this.getTarget().setYaw(this.bodyYaw + 90 - (float) Math.toDegrees(offsetAngle));
                    } else {
                        this.getTarget().setYaw(this.bodyYaw - 90 - (float) Math.toDegrees(offsetAngle));
                    }
                    if (this.getTarget() instanceof EntityGiantSquid) {
                        if (((EntityGiantSquid) this.getTarget()).tickCaptured(this)) {
                            this.setGrabbing(false);
                            this.getTarget().setPosition(this.getDismountLocationForPassenger(this.getTarget()));
                        }
                    }
                    if (grabTime % 20 == 0 && grabTime > 30) {
                        this.getTarget().damage(this.getDamageSources().mobAttack(this), 4 + random.nextInt(4));
                    }
                    if (grabTime > 300) {
                        this.setGrabbing(false);
                        this.getTarget().setPosition(this.getDismountLocationForPassenger(this.getTarget()));
                    }
                } else {
                    this.setCaughtSquidId(-1);
                    this.lookAtEntity(target, 360, 360);
                    waitForEchoFlag = this.getLastAttacker() == null || !this.getLastAttacker().isPartOf(target);
                    if (target instanceof PlayerEntity || !target.isInsideWaterOrBubbleColumn()) {
                        waitForEchoFlag = false;
                    }
                    if (waitForEchoFlag && !receivedEcho) {
                        this.setCharging(false);
                        whaleSpeedMod = 0.25F;
                        if (echoTimer % 10 == 0) {
                            if (echoTimer % 40 == 0) {
                                this.playSound(AMSoundRegistry.CACHALOT_WHALE_CLICK.get(), this.getSoundVolume(), this.getSoundPitch());
                                this.emitGameEvent(GameEvent.ENTITY_ROAR);
                            }
                            final var echo = new EntityCachalotEcho(this.getWorld(), this);
                            final float radius = this.headPart.getWidth() * 0.5F;
                            final float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
                            final double extraX = (radius * (1F + random.nextFloat() * 0.13F)) * MathHelper.sin(MathHelper.PI + angle) + (random.nextFloat() - 0.5F) + this.getVelocity().x * 2F;
                            final double extraZ = (radius * (1F + random.nextFloat() * 0.13F)) * MathHelper.cos(angle) + (random.nextFloat() - 0.5F) + this.getVelocity().z * 2F;
                            final double x = this.headPart.getX() + extraX;
                            final double y = this.headPart.getY() + this.headPart.getHeight() * 0.5D;
                            final double z = this.headPart.getZ() + extraZ;
                            echo.setPos(x, y, z);
                            final double d0 = target.getX() - x;
                            final double d1 = target.getBodyY(0.1D) - y;
                            final double d2 = target.getZ() - z;
                            echo.shoot(d0, d1, d2, 1F, 0.0F);
                            this.getWorld().spawnEntity(echo);
                        }
                        echoTimer++;
                    }
                    if (!waitForEchoFlag || receivedEcho) {
                        final double d0 = target.getX() - this.getX();
                        final double d1 = target.getEyeY() - this.getEyeY();
                        final double d2 = target.getZ() - this.getZ();
                        final double d3 = MathHelper.sqrt((float) (d0 * d0 + d2 * d2));
                        final float targetYaw = (float) (MathHelper.atan2(d2, d0) * MathHelper.DEGREES_PER_RADIAN) - 90.0F;
                        final float targetPitch = (float) (-(MathHelper.atan2(d1, d3) * MathHelper.DEGREES_PER_RADIAN));
                        this.setPitch((this.getPitch() + MathHelper.clamp(targetPitch - this.getPitch(), -2, 2)));
                        if (d0 * d0 + d2 * d2 >= 4) {
                            this.setYaw((this.getYaw() + MathHelper.clamp(targetYaw - this.getYaw(), -2, 2)));
                            this.bodyYaw = getYaw();
                        }
                        if (chargeCooldown <= 0 && Math.abs(MathHelper.wrapDegrees(targetYaw) - MathHelper.wrapDegrees(this.getYaw())) < 4) {
                            this.setCharging(true);
                            whaleSpeedMod = 1.2F;
                            final double distSq = d0 * d0 + d2 * d2;
                            if (distSq < 4) {
                                this.setYaw(prevYaw);
                                this.bodyYaw = prevYaw;
                                this.setVelocity(this.getVelocity().multiply(0.8, 1, 0.8));
                            } else {
                                if (this.isTouchingWater() && target.isTouchingWater()) {
                                    final Vec3d vector3d = this.getVelocity();
                                    Vec3d vector3d1 = new Vec3d(target.getX() - this.getX(), target.getY() - this.getY(), target.getZ() - this.getZ());
                                    if (vector3d1.lengthSquared() > 1.0E-7D) {
                                        vector3d1 = vector3d1.normalize().multiply(0.5D).add(vector3d.multiply(0.8D));
                                    }
                                    this.setVelocity(vector3d1.x, vector3d1.y, vector3d1.z);
                                }
                                this.getMoveControl().moveTo(target.getX(), target.getY(), target.getZ(), 1.0D);
                            }
                            if (this.isCharging()) {
                                if (this.distanceTo(target) < this.getWidth() && chargeProgress > 4) {
                                    if (target instanceof EntityGiantSquid && !this.isBaby()) {
                                        this.setGrabbing(true);
                                        this.setHoldingSquidLeft(random.nextBoolean());
                                    } else {
                                        target.damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                                    }
                                    this.setCharging(false);
                                    if (target.getVehicle() instanceof final BoatEntity boat) {
                                        for (int i = 0; i < 3; ++i) {
                                            this.dropItem(boat.getVariant().getBaseBlock());
                                        }
                                        for (int j = 0; j < 2; ++j) {
                                            this.dropItem(Items.STICK);
                                        }
                                        target.dismountVehicle();
                                        boat.damage(this.getDamageSources().mobAttack(this), 1000);
                                        boat.remove(RemovalReason.DISCARDED);
                                    }
                                    chargeCooldown = target instanceof PlayerEntity ? 30 : 100;
                                    if (random.nextInt(10) == 0) {
                                        var vec = this.getMouthVec();
                                        var itementity = new ItemEntity(this.getWorld(), vec.x, vec.y, vec.z, new ItemStack(AMItemRegistry.CACHALOT_WHALE_TOOTH.get()));
                                        itementity.setToDefaultPickupDelay();
                                        getWorld().spawnEntity(itementity);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (chargeCooldown > 0) {
                chargeCooldown--;
            }
            if (spoutTimer > 0) {
                getWorld().sendEntityStatus(this, (byte) 67);
                spoutTimer--;
                this.setPitch(0);
                this.setVelocity(this.getVelocity().multiply(0, 0, 0));
            }
            if (isSleepTime() && !this.isSleeping() && this.isInsideWaterOrBubbleColumn() && this.getTarget() == null) {
                this.setSleeping(true);
            }
            if (this.isSleeping() && (!isSleepTime() || this.getTarget() != null)) {
                this.setSleeping(false);
            }
            if (target instanceof PlayerEntity && ((PlayerEntity) target).isCreative()) {
                this.setTarget(null);
            }
        }

        if (this.isAlive() && isCharging()) {
            for (final Entity entity : this.getWorld().getNonSpectatingEntities(LivingEntity.class, this.headPart.getBoundingBox().expand(1.0D))) {
                if (!isTeammate(entity) && !(entity instanceof EntityCachalotPart) && entity != this) {
                    launch(entity, true);
                }
            }
        }
        if (this.isTouchingWater() && !this.isSubmergedIn(FluidTags.WATER) && this.getAir() > 140) {
            this.setVelocity(this.getVelocity().add(0, -0.06, 0));
        }
        if (!this.getWorld().isClient) {
            this.tryDespawn();
        }
        prevEyesInWater = this.isSubmergedIn(FluidTags.WATER);
    }

    private void launch(Entity e, boolean huge) {
        if ((e.isOnGround() || e.isTouchingWater()) && !(e instanceof EntityCachalotWhale)) {
            final double d0 = e.getX() - this.getX();
            final double d1 = e.getZ() - this.getZ();
            final double d2 = Math.max(d0 * d0 + d1 * d1, 0.001D);
            float f = huge ? 2F : 0.5F;
            e.addVelocity(d0 / d2 * f, huge ? 0.5D : 0.2F, d1 / d2 * f);
        }
    }

    private boolean isSleepTime() {
        final long time = getWorld().getTimeOfDay();
        return time > 18000 && time < 22812 && this.isInsideWaterOrBubbleColumn();
    }

    public Vec3d getReturnEchoVector() {
        return getVec(0.5D);
    }

    public Vec3d getMouthVec() {
        return getVec(0.25D);
    }

    private Vec3d getVec(final double yShift) {
        final float radius = this.headPart.getWidth() * 0.5F;
        final float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
        final double extraX = (radius * (1F + random.nextFloat() * 0.13F)) * MathHelper.sin(MathHelper.PI + angle) + (random.nextFloat() - 0.5F) + this.getVelocity().x * 2F;
        final double extraZ = (radius * (1F + random.nextFloat() * 0.13F)) * MathHelper.cos(angle) + (random.nextFloat() - 0.5F) + this.getVelocity().z * 2F;
        final double x = this.headPart.getX() + extraX;
        final double y = this.headPart.getY() + yShift;
        final double z = this.headPart.getZ() + extraZ;

        return new Vec3d(x, y, z);
    }

    @Override
    public void setTarget(@Nullable LivingEntity entitylivingbaseIn) {
        LivingEntity prev = this.getTarget();
        if (prev != entitylivingbaseIn && entitylivingbaseIn != null) {
            receivedEcho = false;
        }
        super.setTarget(entitylivingbaseIn);
    }

    public double[] getMovementOffsets(int p_70974_1_, float partialTicks) {
        if (this.isDead()) {
            partialTicks = 0.0F;
        }

        partialTicks = 1.0F - partialTicks;
        final int i = this.ringBufferIndex - p_70974_1_ & 63;
        final int j = this.ringBufferIndex - p_70974_1_ - 1 & 63;
        final double[] adouble = new double[3];
        double d0 = this.ringBuffer[i][0];
        double d1 = this.ringBuffer[j][0] - d0;
        adouble[0] = d0 + d1 * (double) partialTicks;
        d0 = this.ringBuffer[i][1];
        d1 = this.ringBuffer[j][1] - d0;
        adouble[1] = d0 + d1 * (double) partialTicks;
        adouble[2] = MathHelper.lerp(partialTicks, this.ringBuffer[i][2], this.ringBuffer[j][2]);
        return adouble;
    }

    @Override
    public void pushAwayFrom(Entity entityIn) {
    }

    private void setPartPosition(EntityCachalotPart part, double offsetX, double offsetY, double offsetZ) {
        part.setPos(this.getX() + offsetX * part.scale, this.getY() + offsetY * part.scale, this.getZ() + offsetZ * part.scale);
    }

    //FIXME forge
//
//    @Override
//    public PartEntity<?>[] getParts() {
//        return this.whaleParts;
//    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld serverWorld, PassiveEntity ageableEntity) {
        var whale = AMEntityRegistry.CACHALOT_WHALE.get().create(serverWorld);
        whale.setAlbino(this.isAlbino());
        return whale;
    }

    public boolean attackEntityPartFrom(EntityCachalotPart entityCachalotPart, DamageSource source, float amount) {
        return this.damage(source, amount);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, EntityData spawnDataIn, NbtCompound dataTag) {
        this.setAir(this.getMaxAir());
        this.setPitch(0.0F);
        if (spawnDataIn == null) {
            spawnDataIn = new PassiveEntity.PassiveData(0.75F);
        }
        this.setAlbino(random.nextInt(100) == 0);
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    public boolean canBreatheInWater() {
        return false;
    }

    @Override
    public void baseTick() {
        int i = this.getAir();
        super.baseTick();
        this.updateAir(i);
    }

    @Override
    public boolean isPushedByFluids() {
        return this.isBeached();
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.AQUATIC;
    }

    @Override
    public boolean canSpawn(WorldView worldIn) {
        return worldIn.doesNotIntersectEntities(this);
    }

    protected void updateAir(int p_209207_1_) {
    }

    @Override
    public int getMaxAir() {
        return 4000;
    }

    @Override
    public void handleStatus(byte id) {
        if (id == 67) {
            spawnSpoutParticles();
        } else {
            super.handleStatus(id);
        }
    }

    @Override
    protected int getNextAirOnLand(int currentAir) {
        if (!this.getWorld().isClient && prevEyesInWater && spoutTimer <= 0 && !this.isSubmergedIn(FluidTags.WATER) && currentAir < this.getMaxAir() / 2) {
            spoutTimer = 20 + random.nextInt(10);
        }
        return this.getMaxAir();
    }

    @Override
    public int getMaxLookPitchChange() {
        return 1;
    }

    @Override
    public int getMaxHeadRotation() {
        return 3;
    }

    public void receiveEcho() {
        this.receivedEcho = true;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.cachalotWhaleSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public Vec3d getDismountLocationForPassenger(LivingEntity dismount) {
        Vec3d mouth = this.getMouthVec();
        BlockPos pos = AMBlockPos.fromVec3(mouth);
        while(!getWorld().isAir(pos) && !getWorld().isWater(pos) && pos.getY() < getWorld().getTopY()){
            pos = pos.up();
        }
        return new Vec3d(mouth.x, pos.getY() + 0.5F, mouth.z);
    }


    class AIBreathe extends Goal {

        public AIBreathe() {
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return EntityCachalotWhale.this.getAir() < 140;
        }

        @Override
        public boolean shouldContinue() {
            return this.canStart();
        }

        @Override
        public boolean canStop() {
            return false;
        }

        @Override
        public void start() {
            this.navigate();
        }

        private void navigate() {
            final Iterable<BlockPos> lvt_1_1_ = BlockPos.iterate(MathHelper.floor(EntityCachalotWhale.this.getX() - 1.0D), MathHelper.floor(EntityCachalotWhale.this.getY()), MathHelper.floor(EntityCachalotWhale.this.getZ() - 1.0D), MathHelper.floor(EntityCachalotWhale.this.getX() + 1.0D), MathHelper.floor(EntityCachalotWhale.this.getY() + 8.0D), MathHelper.floor(EntityCachalotWhale.this.getZ() + 1.0D));
            BlockPos lvt_2_1_ = null;

            for (final BlockPos lvt_4_1_ : lvt_1_1_) {
                if (this.canBreatheAt(EntityCachalotWhale.this.getWorld(), lvt_4_1_)) {
                    lvt_2_1_ = lvt_4_1_.down((int) (EntityCachalotWhale.this.getHeight() * 0.25d));
                    break;
                }
            }

            if (lvt_2_1_ == null) {
                lvt_2_1_ = AMBlockPos.fromCoords(EntityCachalotWhale.this.getX(), EntityCachalotWhale.this.getY() + 4.0D, EntityCachalotWhale.this.getZ());
            }
            if (EntityCachalotWhale.this.isSubmergedIn(FluidTags.WATER)) {
                EntityCachalotWhale.this.setVelocity(EntityCachalotWhale.this.getVelocity().add(0, 0.05F, 0));
            }

            EntityCachalotWhale.this.getNavigation().startMovingTo(lvt_2_1_.getX(), lvt_2_1_.getY(), lvt_2_1_.getZ(), 0.7D);
        }

        @Override
        public void tick() {
            this.navigate();
        }

        private boolean canBreatheAt(WorldView p_205140_1_, BlockPos p_205140_2_) {
            final BlockState lvt_3_1_ = p_205140_1_.getBlockState(p_205140_2_);
            return (p_205140_1_.getFluidState(p_205140_2_).isEmpty() || lvt_3_1_.isOf(Blocks.BUBBLE_COLUMN)) && lvt_3_1_.canPathfindThrough(p_205140_1_, p_205140_2_, NavigationType.LAND);
        }
    }
}
