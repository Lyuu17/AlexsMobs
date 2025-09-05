package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.misc.IngredientUtil;
import com.github.alexthe666.alexsmobs.registry.*;
import com.google.common.base.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.PufferfishEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class EntityMimicOctopus extends TameableEntity implements ISemiAquatic, IFollower, Bucketable {

    private static final TrackedData<Boolean> STOP_CHANGE = DataTracker.registerData(EntityMimicOctopus.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> FROM_BUCKET = DataTracker.registerData(EntityMimicOctopus.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> UPGRADED = DataTracker.registerData(EntityMimicOctopus.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> MIMIC_ORDINAL = DataTracker.registerData(EntityMimicOctopus.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PREV_MIMIC_ORDINAL = DataTracker.registerData(EntityMimicOctopus.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> MOISTNESS = DataTracker.registerData(EntityMimicOctopus.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> COMMAND = DataTracker.registerData(EntityMimicOctopus.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Optional<BlockState>> MIMICKED_BLOCK = DataTracker.registerData(EntityMimicOctopus.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_STATE);
    private static final TrackedData<Optional<BlockState>> PREV_MIMICKED_BLOCK = DataTracker.registerData(EntityMimicOctopus.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_STATE);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityMimicOctopus.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> LAST_SCARED_MOB_ID = DataTracker.registerData(EntityMimicOctopus.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> UPGRADED_LASER_ENTITY_ID = DataTracker.registerData(EntityMimicOctopus.class, TrackedDataHandlerRegistry.INTEGER);
    public MimicState localMimicState = MimicState.OVERLAY;
    public float transProgress = 0F;
    public float prevTransProgress = 0F;
    public float colorShiftProgress = 0F;
    public float prevColorShiftProgress = 0F;
    public float groundProgress = 5F;
    public float prevGroundProgress = 0F;
    public float sitProgress = 0F;
    public float prevSitProgress = 0F;
    private boolean isLandNavigator;
    private int moistureAttackTime = 0;
    private int camoCooldown = 120 + random.nextInt(1200);
    private int mimicCooldown = 0;
    private int stopMimicCooldown = -1;
    private int fishFeedings;
    private int mimicreamFeedings;
    private int exclaimTime = 0;
    private BlockState localMimic;
    private LivingEntity laserTargetEntity;
    private int guardianLaserTime;

    public EntityMimicOctopus(EntityType<? extends EntityMimicOctopus> type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 0.0F);
        switchNavigator(false);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 16D)
                .add(EntityAttributes.GENERIC_ARMOR, 0.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    public static boolean canMimicOctopusSpawn(EntityType<? extends AnimalEntity> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        BlockPos downPos = pos;
        while (downPos.getY() > 1 && !worldIn.getFluidState(downPos).isEmpty()) {
            downPos = downPos.down();
        }
        boolean spawnBlock = worldIn.getBlockState(downPos).isIn(AMTagRegistry.MIMIC_OCTOPUS_SPAWNS);
        return spawnBlock && downPos.getY() < worldIn.getSeaLevel() + 1;
    }

    public static MimicState getStateForItem(ItemStack stack) {
        if (stack.isIn(AMTagRegistry.MIMIC_OCTOPUS_CREEPER_ITEMS)) {
            return MimicState.CREEPER;
        }
        if (stack.isIn(AMTagRegistry.MIMIC_OCTOPUS_GUARDIAN_ITEMS)) {
            return MimicState.GUARDIAN;
        }
        if (stack.isIn(AMTagRegistry.MIMIC_OCTOPUS_PUFFERFISH_ITEMS)) {
            return MimicState.PUFFERFISH;
        }
        return null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.MIMIC_OCTOPUS_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.MIMIC_OCTOPUS_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.MIMIC_OCTOPUS_HURT.get();
    }

    @Override
    public boolean canSpawn(WorldView worldIn) {
        return worldIn.doesNotIntersectEntities(this);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.mimicOctopusSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.dataTracker.set(PREV_MIMIC_ORDINAL, 0);
        this.setMimickedBlock(null);
        this.setMimicState(MimicState.OVERLAY);
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.dataTracker.set(MIMIC_ORDINAL, compound.getInt("MimicState"));
        this.setUpgraded(compound.getBoolean("Upgraded"));
        this.setSitting(compound.getBoolean("Sitting"));
        this.setStopChange(compound.getBoolean("StopChange"));
        this.setCommand(compound.getInt("OctoCommand"));
        this.setMoistness(compound.getInt("Moistness"));
        this.setFromBucket(compound.getBoolean("FromBucket"));
        BlockState blockstate = null;
        if (compound.contains("MimickedBlockState", 10)) {
            blockstate = NbtHelper.toBlockState(this.getWorld().createCommandRegistryWrapper(RegistryKeys.BLOCK), compound.getCompound("MimickedBlockState"));
            if (blockstate.isAir()) {
                blockstate = null;
            }
        }
        this.setMimickedBlock(blockstate);
        this.camoCooldown = compound.getInt("CamoCooldown");
        this.mimicCooldown = compound.getInt("MimicCooldown");
        this.stopMimicCooldown = compound.getInt("StopMimicCooldown");
        this.fishFeedings = compound.getInt("FishFeedings");
        this.mimicreamFeedings = compound.getInt("MimicreamFeedings");
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putInt("MimicState", this.getMimicState().ordinal());
        compound.putBoolean("Upgraded", this.isUpgraded());
        compound.putBoolean("Sitting", this.isSitting());
        compound.putInt("OctoCommand", this.getCommand());
        compound.putInt("Moistness", this.getMoistness());
        compound.putBoolean("FromBucket", this.isFromBucket());
        compound.putBoolean("StopChange", this.isStopChange());
        BlockState blockstate = this.getMimickedBlock();
        if (blockstate != null) {
            compound.put("MimickedBlockState", NbtHelper.fromBlockState(blockstate));
        }
        compound.putInt("CamoCooldown", this.camoCooldown);
        compound.putInt("MimicCooldown", this.mimicCooldown);
        compound.putInt("StopMimicCooldown", this.stopMimicCooldown);
        compound.putInt("FishFeedings", this.fishFeedings);
        compound.putInt("MimicreamFeedings", this.mimicreamFeedings);
    }

    @Override
    @NotNull
    public ItemStack getBucketItem() {
        ItemStack stack = new ItemStack(AMItemRegistry.MIMIC_OCTOPUS_BUCKET.get());
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
        compound.put("MimicOctopusData", platTag);
    }

    @Override
    public void copyDataFromNbt(@NotNull NbtCompound compound) {
        if (compound.contains("MimicOctopusData")) {
            this.readCustomDataFromNbt(compound.getCompound("MimicOctopusData"));
        }
        this.setMoistness(60000);
    }

    @Override
    protected float getJumpVelocity() {
        return super.getJumpVelocity() * (this.isInsideWaterOrBubbleColumn() ? 1.3F : 1F);
    }

    @Override
    public boolean shouldFollow() {
        return this.getCommand() == 1;
    }

    @Override
    public boolean isTeammate(Entity entityIn) {
        if (this.isTamed()) {
            LivingEntity livingentity = this.getOwner();
            if (entityIn == livingentity) {
                return true;
            }
            if (entityIn instanceof TameableEntity) {
                return ((TameableEntity) entityIn).isOwner(livingentity);
            }
            if (livingentity != null) {
                return livingentity.isTeammate(entityIn);
            }
        }
        return super.isTeammate(entityIn);
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    protected void initGoals() {
        this.goalSelector.add(0, new AIAttack());
        this.goalSelector.add(1, new SitGoal(this));
        this.goalSelector.add(2, new TameableAIFollowOwnerWater(this, 1.3D, 4.0F, 2.0F, false));
        this.goalSelector.add(3, new AnimalAIFindWater(this));
        this.goalSelector.add(3, new AnimalAILeaveWater(this));
        this.goalSelector.add(4, new TemptGoal(this, 1.0D, IngredientUtil.ingredientFromTags(AMTagRegistry.MIMIC_OCTOPUS_BREEDABLES, AMTagRegistry.MIMIC_OCTOPUS_TAMEABLES), false) {
            @Override
            public void tick() {
                EntityMimicOctopus.this.setMimickedBlock(null);
                super.tick();
                EntityMimicOctopus.this.camoCooldown = 40;
                EntityMimicOctopus.this.stopMimicCooldown = 40;
            }
        });
        this.goalSelector.add(5, new AIFlee());
        this.goalSelector.add(7, new AnimalMateGoal(this, 0.8D));
        this.goalSelector.add(8, new AIMimicNearbyMobs());
        this.goalSelector.add(9, new AnimalMateGoal(this, 0.8D));
        this.goalSelector.add(10, new AISwim());
        this.goalSelector.add(11, new LookAroundGoal(this));
        this.goalSelector.add(11, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, new RevengeGoal(this) {
            @Override
            public boolean canStart() {
                return EntityMimicOctopus.this.isTamed() && super.canStart();
            }
        });
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        var item = stack.getItem();
        return isTamed() && stack.isIn(AMTagRegistry.MIMIC_OCTOPUS_BREEDABLES);
    }

    public boolean isActiveCamo() {
        return this.getMimicState() == MimicState.OVERLAY && this.getMimickedBlock() != null;
    }

    @Override
    public double getAttackDistanceScalingFactor(@Nullable Entity lookingEntity) {
        if (isActiveCamo()) {
            return super.getAttackDistanceScalingFactor(lookingEntity) * 0.1F;
        } else {
            return super.getAttackDistanceScalingFactor(lookingEntity);
        }
    }

    @Override
    @NotNull
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        var itemstack = player.getStackInHand(hand);
        var item = itemstack.getItem();
        var readState = getStateForItem(itemstack);
        var type = super.interactMob(player, hand);
        if (readState != null && this.isTamed()) {
            if (mimicCooldown == 0) {
                this.setMimicState(readState);
                mimicCooldown = 20;
                stopMimicCooldown = isUpgraded() ? 120 : 1200;
                camoCooldown = stopMimicCooldown;
                this.setMimickedBlock(null);
            }
            return ActionResult.SUCCESS;
        }
        final boolean tame = isTamed();
        if (tame && itemstack.isIn(AMTagRegistry.MIMIC_OCTOPUS_TOGGLES_MIMIC)) {
            this.setStopChange(!this.isStopChange());
            if (this.isStopChange()) {
                this.makeEatingParticles(itemstack);
            } else {
                this.getWorld().sendEntityStatus(this, (byte) 6);
                this.mimicEnvironment();
            }
            return ActionResult.SUCCESS;
        }
        if (!tame && itemstack.isIn(AMTagRegistry.MIMIC_OCTOPUS_TAMEABLES)) {
            this.eat(player, hand, itemstack);
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_DOLPHIN_EAT, this.getSoundVolume(), this.getSoundPitch());
            fishFeedings++;
            if (this.getMimicState() == MimicState.OVERLAY && this.getMimickedBlock() == null) {
                if (fishFeedings > 5 && getRandom().nextInt(2) == 0 || fishFeedings > 8) {
                    this.setOwner(player);
                    this.getWorld().sendEntityStatus(this, (byte) 7);
                } else {
                    this.getWorld().sendEntityStatus(this, (byte) 6);
                }
            }
            return ActionResult.SUCCESS;
        }
        if (tame && itemstack.isIn(AMTagRegistry.MIMIC_OCTOPUS_TAMEABLES)) {
            if (this.getHealth() < this.getMaxHealth()) {
                this.eat(player, hand, itemstack);
                this.emitGameEvent(GameEvent.EAT);
                this.playSound(SoundEvents.ENTITY_DOLPHIN_EAT, this.getSoundVolume(), this.getSoundPitch());
                this.heal(5);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        }
        if (tame) {
            var result = Bucketable.tryBucket(player, hand, this);
            if (result.isPresent()) {
                return result.get();
            }

            if (itemstack.isIn(AMTagRegistry.MIMIC_OCTOPUS_MOISTURIZES) && this.getMoistness() < 24000) {
                this.setMoistness(48000);
                this.makeEatingParticles(itemstack);
                this.eat(player, hand, itemstack);
                return ActionResult.SUCCESS;
            }

            if (!this.isUpgraded() && itemstack.isIn(AMTagRegistry.MIMIC_OCTOPUS_ATTACK_FOODS)) {
                mimicreamFeedings++;
                if (mimicreamFeedings > 5 || mimicreamFeedings > 2 && random.nextInt(2) == 0) {
                    this.getWorld().sendEntityStatus(this, (byte) 46);
                    this.setUpgraded(true);
                    this.setMimicState(MimicState.MIMICUBE);
                    this.setStopChange(false);
                    this.setMimickedBlock(null);
                    this.stopMimicCooldown = 40;
                }
                this.makeEatingParticles(itemstack);
                this.eat(player, hand, itemstack);
                return ActionResult.SUCCESS;
            }
        }

        var actionResult = itemstack.useOnEntity(player, this, hand);
        if (actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS && isTamed() && isOwner(player)) {
            if (player.isSneaking()) {
                if (this.getMainHandStack().isEmpty()) {
                    ItemStack cop = itemstack.copy();
                    cop.setCount(1);
                    this.setStackInHand(Hand.MAIN_HAND, cop);
                    itemstack.decrement(1);
                } else {
                    this.dropStack(this.getMainHandStack().copy());
                    this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                }
                return ActionResult.SUCCESS;
            } else if (!isBreedingItem(itemstack)) {
                this.setCommand(this.getCommand() + 1);
                if (this.getCommand() == 3) {
                    this.setCommand(0);
                }
                player.sendMessage(Text.translatable("entity.alexsmobs.all.command_" + this.getCommand(), this.getName()), true);
                boolean sit = this.getCommand() == 2;
                this.setSitting(sit);
                return ActionResult.SUCCESS;
            }
        }
        return type;
    }

    public int getCommand() {
        return this.dataTracker.get(COMMAND);
    }

    public void setCommand(int command) {
        this.dataTracker.set(COMMAND, command);
    }

    private void makeEatingParticles(ItemStack item) {
        for (int i = 0; i < 6 + random.nextInt(3); i++) {
            double d2 = this.random.nextGaussian() * 0.02D;
            double d0 = this.random.nextGaussian() * 0.02D;
            double d1 = this.random.nextGaussian() * 0.02D;
            this.getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, item), this.getX() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, this.getY() + this.getHeight() * 0.5F + (double) (this.random.nextFloat() * this.getHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, d0, d1, d2);
        }
    }

    @Override
    public void updateLimbs(boolean flying) {
        float f1 = (float)MathHelper.magnitude(this.getX() - this.prevX, this.getY() - this.prevY, this.getZ() - this.prevZ);
        float f2 = Math.min(f1 * (groundProgress < 2.5F ? 4.0F : 8.0F), 1.0F);
        this.limbAnimator.updateLimbs(f2, 0.4F);
    }

    @Override
    public boolean canBreatheInWater() {
        return true;
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new GroundPathNavigatorWide(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new AnimalSwimMoveControllerSink(this, 1.3F, 1);
            this.navigation = new SemiAquaticPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (localMimic != this.getPrevMimickedBlock()) {
            localMimic = this.getPrevMimickedBlock();
            colorShiftProgress = 0.0F;
        }
        if (localMimicState != this.getPrevMimicState()) {
            localMimicState = this.getPrevMimicState();
            transProgress = 0.0F;
        }

        if (this.isTouchingWater()) {
            if (this.isLandNavigator)
                switchNavigator(false);
        } else {
            if (!this.isLandNavigator)
                switchNavigator(true);
        }

        BlockPos pos = AMBlockPos.fromCoords(this.getX(), this.getEyeY() - 1F, this.getZ());
        boolean ground = getWorld().getBlockState(pos).isSideSolidFullSquare(getWorld(), pos, Direction.UP) && this.getMimicState() != MimicState.GUARDIAN || !this.isInsideWaterOrBubbleColumn() || this.isSitting();
        this.prevTransProgress = transProgress;
        this.prevColorShiftProgress = colorShiftProgress;
        this.prevGroundProgress = groundProgress;
        this.prevSitProgress = sitProgress;
        if (this.getPrevMimicState() != this.getMimicState() && transProgress < 5.0F) {
            transProgress += 0.25F;
        }
        if (this.getPrevMimicState() == this.getMimicState() && transProgress > 0F) {
            transProgress -= 0.25F;
        }
        if (getPrevMimickedBlock() != this.getMimickedBlock() && colorShiftProgress < 5.0F) {
            colorShiftProgress += 0.25F;
        }
        if (getPrevMimickedBlock() == this.getMimickedBlock() && colorShiftProgress > 0F) {
            colorShiftProgress -= 0.25F;
        }
        if (ground && groundProgress < 5F) {
            groundProgress += 0.5F;
        }
        if (!ground && groundProgress > 0F) {
            groundProgress -= 0.5F;
        }
        if (isSitting() && sitProgress < 5F) {
            sitProgress += 0.5F;
        }
        if (!isSitting() && sitProgress > 0F) {
            sitProgress -= 0.5F;
        }
        if (this.isInsideWaterOrBubbleColumn()) {
            float f2 = (float) -((float) this.getVelocity().y * 3 * (double) MathHelper.DEGREES_PER_RADIAN);
            this.setPitch(f2);
        }
        if (camoCooldown > 0) {
            camoCooldown--;
        }
        if (mimicCooldown > 0) {
            mimicCooldown--;
        }
        if (stopMimicCooldown > 0) {
            stopMimicCooldown--;
        }
        if (this.isAiDisabled()) {
            this.setAir(this.getMaxAir());
        } else {
            if (this.isWet() || this.getMainHandStack().getItem() == Items.WATER_BUCKET) {
                this.setMoistness(60000);
            } else {
                this.setMoistness(this.getMoistness() - 1);
                if (this.getMoistness() <= 0 && moistureAttackTime-- <= 0) {
                    this.setSitting(false);
                    this.damage(getDamageSources().dryOut(), random.nextInt(2) == 0 ? 1.0F : 0F);
                    moistureAttackTime = 20;
                }
            }
        }
        if (camoCooldown <= 0 && random.nextInt(300) == 0) {
            mimicEnvironment();
            camoCooldown = this.getRandom().nextInt(2200) + 200;
        }
        if ((this.getMimicState() != MimicState.OVERLAY || this.getMimickedBlock() != null) && stopMimicCooldown == 0 && !this.isStopChange()) {
            this.setMimicState(MimicState.OVERLAY);
            this.setMimickedBlock(null);
            stopMimicCooldown = -1;
        }
        if (this.getWorld().isClient && exclaimTime > 0) {
            exclaimTime--;
            if (exclaimTime == 0) {
                var e = getWorld().getEntityById(this.dataTracker.get(LAST_SCARED_MOB_ID));
                if (e != null && transProgress >= 5.0F) {
                    double d2 = this.random.nextGaussian() * 0.1D;
                    double d0 = this.random.nextGaussian() * 0.1D;
                    double d1 = this.random.nextGaussian() * 0.1D;
                    this.getWorld().addParticle(AMParticleRegistry.SHOCKED.get(), e.getX(), e.getEyeY() + e.getHeight() * 0.15F + (double) (this.random.nextFloat() * e.getHeight() * 0.15F), e.getZ(), d0, d1, d2);
                }
            }
        }

        if (this.hasGuardianLaser()) {
            if (this.guardianLaserTime < 30) {
                ++this.guardianLaserTime;
            }
            LivingEntity livingentity = this.getGuardianLaser();
            if (livingentity != null && this.isInsideWaterOrBubbleColumn()) {
                this.getLookControl().lookAt(livingentity, 90.0F, 90.0F);
                this.getLookControl().tick();
                double d5 = this.getLaserAttackAnimationScale(0.0F);
                double d0 = livingentity.getX() - this.getX();
                double d1 = livingentity.getBodyY(0.5D) - this.getEyeY();
                double d2 = livingentity.getZ() - this.getZ();
                double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                d0 = d0 / d3;
                d1 = d1 / d3;
                d2 = d2 / d3;
                double d4 = this.random.nextDouble();
                while (d4 < d3) {
                    d4 += 1.8D - d5 + this.random.nextDouble() * (1.7D - d5);
                    this.getWorld().addParticle(ParticleTypes.BUBBLE, this.getX() + d0 * d4, this.getEyeY() + d1 * d4, this.getZ() + d2 * d4, 0.0D, 0.0D, 0.0D);
                }
                if (guardianLaserTime == 30) {
                    livingentity.damage(this.getDamageSources().mobAttack(this), 5);
                    guardianLaserTime = 0;
                    this.dataTracker.set(UPGRADED_LASER_ENTITY_ID, -1);
                }
            }
        }
        if (!this.getWorld().isClient && age % 40 == 0) {
            this.heal(2);
        }
    /*if(!world.isRemote){
            if(ticksExisted % 80 == 0){
                mimicEnvironment();
            }else if(ticksExisted % 40 == 0){
                this.setMimicState(MimicState.OVERLAY);
                this.setMimickedBlock(null);
            }
        }*/
    }

    public float getLaserAttackAnimationScale(float p_175477_1_) {
        return ((float) this.guardianLaserTime + p_175477_1_) / 30F;
    }

    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if (id == 68) {
            if (exclaimTime == 0) {
                exclaimTime = 20;
            }
        } else if (id == 69) {
            this.creeperExplode();
        } else {
            super.handleStatus(id);
        }
    }

    public void mimicEnvironment() {
        if (!this.isStopChange()) {
            BlockPos down = getPositionDown();
            if (!getWorld().isAir(down)) {
                this.setMimicState(MimicState.OVERLAY);
                this.setMimickedBlock(getWorld().getBlockState(down));
            }
            stopMimicCooldown = this.getRandom().nextInt(2200);
        }
    }

    public int getMoistness() {
        return this.dataTracker.get(MOISTNESS);
    }

    public void setMoistness(int p_211137_1_) {
        this.dataTracker.set(MOISTNESS, p_211137_1_);
    }

    private BlockPos getPositionDown() {
        BlockPos pos = AMBlockPos.fromCoords(this.getX(), this.getEyeY(), this.getZ());
        while (pos.getY() > 1 && (getWorld().isAir(pos) || getWorld().isWater(pos))) {
            pos = pos.down();
        }
        return pos;
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.isSitting()) {
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
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    @Override
    public void setSitting(boolean sit) {
        this.dataTracker.set(SITTING, sit);
    }

    @Override
    public boolean isFromBucket() {
        return this.dataTracker.get(FROM_BUCKET);
    }

    @Override
    public void setFromBucket(boolean sit) {
        this.dataTracker.set(FROM_BUCKET, sit);
    }

    @Override
    @NotNull
    public SoundEvent getBucketFillSound() {
        return SoundEvents.ITEM_BUCKET_FILL_FISH;
    }

    public boolean isUpgraded() {
        return this.dataTracker.get(FROM_BUCKET);
    }

    public void setUpgraded(boolean sit) {
        this.dataTracker.set(FROM_BUCKET, sit);
    }

    public boolean isStopChange() {
        return this.dataTracker.get(STOP_CHANGE);
    }

    public void setStopChange(boolean sit) {
        this.dataTracker.set(STOP_CHANGE, sit);
    }

    public boolean hasGuardianLaser() {
        return this.dataTracker.get(UPGRADED_LASER_ENTITY_ID) != -1 && this.isUpgraded() && this.isInsideWaterOrBubbleColumn();
    }

    @Nullable
    public LivingEntity getGuardianLaser() {
        if (!this.hasGuardianLaser()) {
            return null;
        } else if (this.getWorld().isClient) {
            if (this.laserTargetEntity != null) {
                return this.laserTargetEntity;
            } else {
                var lvt_1_1_ = this.getWorld().getEntityById(this.dataTracker.get(UPGRADED_LASER_ENTITY_ID));
                if (lvt_1_1_ instanceof LivingEntity) {
                    this.laserTargetEntity = (LivingEntity) lvt_1_1_;
                    return this.laserTargetEntity;
                } else {
                    return null;
                }
            }
        } else {
            return this.getTarget();
        }
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.MIMIC_OCTOPUS.get().create(world);
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.isFromBucket() || this.isTamed();
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceToClosestPlayer) {
        return !this.isTamed() && !this.isFromBucket();
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(MIMIC_ORDINAL, 0);
        this.dataTracker.startTracking(PREV_MIMIC_ORDINAL, -1);
        this.dataTracker.startTracking(MOISTNESS, 60000);
        this.dataTracker.startTracking(MIMICKED_BLOCK, Optional.empty());
        this.dataTracker.startTracking(PREV_MIMICKED_BLOCK, Optional.empty());
        this.dataTracker.startTracking(SITTING, false);
        this.dataTracker.startTracking(COMMAND, 0);
        this.dataTracker.startTracking(LAST_SCARED_MOB_ID, -1);
        this.dataTracker.startTracking(FROM_BUCKET, false);
        this.dataTracker.startTracking(UPGRADED, false);
        this.dataTracker.startTracking(STOP_CHANGE, false);
        this.dataTracker.startTracking(UPGRADED_LASER_ENTITY_ID, -1);
    }

    public MimicState getMimicState() {
        return MimicState.values()[MathHelper.clamp(this.dataTracker.get(MIMIC_ORDINAL), 0, 4)];
    }

    public void setMimicState(MimicState state) {
        if (getMimicState() != state) {
            this.dataTracker.set(PREV_MIMIC_ORDINAL, this.dataTracker.get(MIMIC_ORDINAL));
        }
        this.dataTracker.set(MIMIC_ORDINAL, state.ordinal());
    }

    public MimicState getPrevMimicState() {
        if (this.dataTracker.get(PREV_MIMIC_ORDINAL) == -1) {
            return null;
        }
        return MimicState.values()[MathHelper.clamp(this.dataTracker.get(PREV_MIMIC_ORDINAL), 0, 4)];
    }

    @Nullable
    public BlockState getMimickedBlock() {
        return this.dataTracker.get(MIMICKED_BLOCK).orElse(null);
    }

    public void setMimickedBlock(@Nullable BlockState state) {
        if (getMimickedBlock() != state) {
            this.dataTracker.set(PREV_MIMICKED_BLOCK, Optional.ofNullable(getMimickedBlock()));
        }
        this.dataTracker.set(MIMICKED_BLOCK, Optional.ofNullable(state));
    }

    @Nullable
    public BlockState getPrevMimickedBlock() {
        return this.dataTracker.get(PREV_MIMICKED_BLOCK).orElse(null);
    }

    protected void updateAir(int p_209207_1_) {
        if (this.isAlive() && !this.isInsideWaterOrBubbleColumn()) {
            this.setAir(p_209207_1_ - 1);
            if (this.getAir() == -20) {
                this.setAir(0);
                this.damage(getDamageSources().dryOut(), 2.0F);
            }
        } else {
            this.setAir(1200);
        }
    }

    @Override
    public boolean shouldEnterWater() {
        return !this.isSitting() && (this.getTarget() == null || this.getTarget().isInsideWaterOrBubbleColumn());
    }

    @Override
    public boolean shouldLeaveWater() {
        return this.getTarget() != null && !this.getTarget().isInsideWaterOrBubbleColumn();
    }

    @Override
    public boolean shouldStopMoving() {
        return isSitting();
    }

    @Override
    public int getWaterSearchRange() {
        return 16;
    }

    public boolean isTargetBlocked(Vec3d target) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());

        return this.getWorld().raycast(new RaycastContext(Vector3d, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() != HitResult.Type.MISS;
    }

    public Vec3d getBlockInViewAway(Vec3d fleePos, float radiusAdd) {
        float radius = 0.75F * (0.7F * 6) * -3 - this.getRandom().nextInt(24) - radiusAdd;
        float neg = this.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = this.bodyYaw;
        float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = new BlockPos((int) (fleePos.x + extraX), 0, (int) (fleePos.z + extraZ));
        BlockPos ground = getOctopusGround(radialPos);

        return ground != null ? Vec3d.ofCenter(ground) : null;
    }

    private BlockPos getOctopusGround(BlockPos in) {
        BlockPos position = new BlockPos(in.getX(), (int) this.getY(), in.getZ());
        while (position.getY() > 2 && getWorld().getFluidState(position).isIn(FluidTags.WATER)) {
            position = position.down();
        }
        return position;
    }

    @Override
    public void onDataTrackerUpdate(List<DataTracker.SerializedEntry<?>> dataEntries) {
        super.onDataTrackerUpdate(dataEntries);
        if (UPGRADED_LASER_ENTITY_ID.equals(dataEntries)) {
            this.guardianLaserTime = 0;
            this.laserTargetEntity = null;
        }
    }

    private void creeperExplode() {
        var explosion = new Explosion(getWorld(), this,  this.getDamageSources().mobAttack(this), (ExplosionBehavior) null, this.getX(), this.getY(), this.getZ(), 1 + random.nextFloat(), false, Explosion.DestructionType.KEEP);
        explosion.collectBlocksAndDamageEntities();
        explosion.affectWorld(true);
    }

    @Override
    public EntityView method_48926() {
        return this.getWorld();
    }

    public enum MimicState {
        OVERLAY,
        CREEPER,
        GUARDIAN,
        PUFFERFISH,
        MIMICUBE
    }

    private class AISwim extends SemiAquaticAIRandomSwimming {

        public AISwim() {
            super(EntityMimicOctopus.this, 1, 35);
        }

        @Override
        protected Vec3d findSurfaceTarget(PathAwareEntity creature, int i, int i1) {
            if (creature.getRandom().nextInt(5) == 0) {
                return super.findSurfaceTarget(creature, i, i1);
            } else {
                BlockPos downPos = creature.getBlockPos();
                while (creature.getWorld().getFluidState(downPos).isIn(FluidTags.WATER) || creature.getWorld().getFluidState(downPos).isIn(FluidTags.LAVA)) {
                    downPos = downPos.down();
                }
                if (getWorld().getBlockState(downPos).isOpaque() && getWorld().getBlockState(downPos).getBlock() != Blocks.MAGMA_BLOCK) {
                    return new Vec3d(downPos.getX() + 0.5F, downPos.getY(), downPos.getZ() + 0.5F);
                }
            }
            return null;
        }

    }

    private class AIFlee extends Goal {
        protected final EntitySorter theNearestAttackableTargetSorter;
        protected final Predicate<? super Entity> targetEntitySelector;
        protected int executionChance = 8;
        protected boolean mustUpdate;
        private Entity targetEntity;
        private Vec3d flightTarget = null;
        private int cooldown = 0;

        AIFlee() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
            this.theNearestAttackableTargetSorter = new EntitySorter(EntityMimicOctopus.this);
            this.targetEntitySelector = (Predicate<Entity>) e -> e.isAlive() && e.getType().isIn(AMTagRegistry.MIMIC_OCTOPUS_FEARS) || e instanceof PlayerEntity && !((PlayerEntity) e).isCreative();
        }

        @Override
        public boolean canStart() {
            if (EntityMimicOctopus.this.hasVehicle() || EntityMimicOctopus.this.hasPassengers() || EntityMimicOctopus.this.isTamed()) {
                return false;
            }
            if (!this.mustUpdate) {
                long worldTime = EntityMimicOctopus.this.getWorld().getTime() % 10;
                if (EntityMimicOctopus.this.getDespawnCounter() >= 100 && worldTime != 0) {
                    return false;
                }
                if (EntityMimicOctopus.this.getRandom().nextInt(this.executionChance) != 0 && worldTime != 0) {
                    return false;
                }
            }
            List<Entity> list = EntityMimicOctopus.this.getWorld().getEntitiesByClass(Entity.class, this.getTargetableArea(this.getTargetDistance()), this.targetEntitySelector);
            if (list.isEmpty()) {
                return false;
            } else {
                list.sort(this.theNearestAttackableTargetSorter);
                this.targetEntity = list.get(0);
                this.mustUpdate = false;
                return true;
            }
        }

        @Override
        public boolean shouldContinue() {
            return targetEntity != null && !EntityMimicOctopus.this.isTamed() && EntityMimicOctopus.this.distanceTo(targetEntity) < 20;
        }

        @Override
        public void stop() {
            flightTarget = null;
            this.targetEntity = null;
            EntityMimicOctopus.this.setMimicState(MimicState.OVERLAY);
            EntityMimicOctopus.this.setMimickedBlock(null);
        }

        @Override
        public void tick() {
            if (cooldown > 0) {
                cooldown--;
            }
            if (!EntityMimicOctopus.this.isActiveCamo()) {
                EntityMimicOctopus.this.mimicEnvironment();
            }
            if (flightTarget != null) {
                EntityMimicOctopus.this.getNavigation().startMovingTo(flightTarget.x, flightTarget.y, flightTarget.z, 1.2F);
                if (cooldown == 0 && EntityMimicOctopus.this.isTargetBlocked(flightTarget)) {
                    cooldown = 30;
                    flightTarget = null;
                }
            }

            if (targetEntity != null) {
                if (flightTarget == null || flightTarget != null && EntityMimicOctopus.this.squaredDistanceTo(flightTarget) < 6) {
                    Vec3d vec;
                    vec = NoPenaltyTargeting.findFrom(EntityMimicOctopus.this, 16, 7, targetEntity.getPos());
                    if (vec != null) {
                        flightTarget = vec;
                    }
                }
                if (EntityMimicOctopus.this.distanceTo(targetEntity) > 20.0F) {
                    this.stop();
                }
            }
        }

        protected double getTargetDistance() {
            return 10;
        }

        protected Box getTargetableArea(double targetDistance) {
            var renderCenter = new Vec3d(EntityMimicOctopus.this.getX(), EntityMimicOctopus.this.getY() + 0.5, EntityMimicOctopus.this.getZ());
            var aabb = new Box(-targetDistance, -targetDistance, -targetDistance, targetDistance, targetDistance, targetDistance);
            return aabb.offset(renderCenter);
        }
    }

    public record EntitySorter(Entity theEntity) implements Comparator<Entity> {
        public int compare(Entity p_compare_1_, Entity p_compare_2_) {
            final double d0 = this.theEntity.squaredDistanceTo(p_compare_1_);
            final double d1 = this.theEntity.squaredDistanceTo(p_compare_2_);
            return Double.compare(d0, d1);
        }
    }

    private class AIMimicNearbyMobs extends Goal {
        protected final EntitySorter theNearestAttackableTargetSorter;
        protected final Predicate<? super Entity> targetEntitySelector;
        protected int executionChance = 30;
        protected boolean mustUpdate;
        private Entity targetEntity;
        private Vec3d flightTarget = null;
        private int cooldown = 0;

        AIMimicNearbyMobs() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
            this.theNearestAttackableTargetSorter = new EntitySorter(EntityMimicOctopus.this);
            this.targetEntitySelector = (Predicate<Entity>) e -> e.isAlive() && (e instanceof CreeperEntity || e instanceof GuardianEntity || e instanceof PufferfishEntity);
        }

        @Override
        public boolean canStart() {
            if (EntityMimicOctopus.this.hasVehicle() || EntityMimicOctopus.this.hasPassengers() || EntityMimicOctopus.this.getMimicState() != MimicState.OVERLAY || mimicCooldown > 0) {
                return false;
            }
            if (!this.mustUpdate) {
                long worldTime = EntityMimicOctopus.this.getWorld().getTime() % 10;
                if (EntityMimicOctopus.this.getDespawnCounter() >= 100 && worldTime != 0) {
                    return false;
                }
                if (EntityMimicOctopus.this.getRandom().nextInt(this.executionChance) != 0 && worldTime != 0) {
                    return false;
                }
            }
            List<Entity> list = EntityMimicOctopus.this.getWorld().getEntitiesByClass(Entity.class, this.getTargetableArea(this.getTargetDistance()), this.targetEntitySelector);
            if (list.isEmpty()) {
                return false;
            } else {
                list.sort(this.theNearestAttackableTargetSorter);
                this.targetEntity = list.get(0);
                this.mustUpdate = false;
                return true;
            }
        }

        @Override
        public boolean shouldContinue() {
            return targetEntity != null && EntityMimicOctopus.this.distanceTo(targetEntity) < 10 && EntityMimicOctopus.this.getMimicState() == MimicState.OVERLAY;
        }

        @Override
        public void stop() {
            EntityMimicOctopus.this.getNavigation().stop();
            flightTarget = null;
            this.targetEntity = null;
        }

        @Override
        public void tick() {
            if (cooldown > 0) {
                cooldown--;
            }
            if (targetEntity != null) {
                EntityMimicOctopus.this.getNavigation().startMovingTo(targetEntity, 1.2F);
                if (EntityMimicOctopus.this.distanceTo(targetEntity) > 20.0F) {
                    this.stop();
                    EntityMimicOctopus.this.setMimicState(MimicState.OVERLAY);
                    EntityMimicOctopus.this.setMimickedBlock(null);
                } else if (EntityMimicOctopus.this.distanceTo(targetEntity) < 5.0F && EntityMimicOctopus.this.canSee(targetEntity)) {
                    int i = 1200;
                    EntityMimicOctopus.this.stopMimicCooldown = i;
                    EntityMimicOctopus.this.camoCooldown = i + 40;
                    EntityMimicOctopus.this.mimicCooldown = 40;
                    if (targetEntity instanceof CreeperEntity) {
                        EntityMimicOctopus.this.setMimicState(MimicState.CREEPER);
                    } else if (targetEntity instanceof GuardianEntity) {
                        EntityMimicOctopus.this.setMimicState(MimicState.GUARDIAN);
                    } else if (targetEntity instanceof PufferfishEntity) {
                        EntityMimicOctopus.this.setMimicState(MimicState.PUFFERFISH);
                    } else {
                        EntityMimicOctopus.this.setMimicState(MimicState.OVERLAY);
                        EntityMimicOctopus.this.setMimickedBlock(null);
                    }
                    stop();
                }

            }
        }

        protected double getTargetDistance() {
            return 10;
        }

        protected Box getTargetableArea(double targetDistance) {
            var renderCenter = new Vec3d(EntityMimicOctopus.this.getX(), EntityMimicOctopus.this.getY() + 0.5, EntityMimicOctopus.this.getZ());
            var aabb = new Box(-targetDistance, -targetDistance, -targetDistance, targetDistance, targetDistance, targetDistance);
            return aabb.offset(renderCenter);
        }
    }

    private class AIAttack extends Goal {
        private int executionCooldown = 0;
        private int scareMobTime = 0;
        private Vec3d fleePosition = null;

        public AIAttack() {
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (executionCooldown > 0) {
                EntityMimicOctopus.this.dataTracker.set(UPGRADED_LASER_ENTITY_ID, -1);
                executionCooldown--;
            }
            if (EntityMimicOctopus.this.isStopChange() && EntityMimicOctopus.this.getMimicState() == MimicState.OVERLAY) {
                return false;
            }
            return executionCooldown == 0 && EntityMimicOctopus.this.isTamed() && EntityMimicOctopus.this.getTarget() != null && EntityMimicOctopus.this.getTarget().isAlive();
        }

        @Override
        public void stop() {
            fleePosition = null;
            scareMobTime = 0;
            executionCooldown = 100 + random.nextInt(200);
            if (EntityMimicOctopus.this.isUpgraded()) {
                executionCooldown = 30;
            } else {
                EntityMimicOctopus.this.setAttacker(null);
                EntityMimicOctopus.this.setTarget(null);
            }
            if (EntityMimicOctopus.this.stopMimicCooldown <= 0) {
                EntityMimicOctopus.this.mimicEnvironment();
            }
            EntityMimicOctopus.this.dataTracker.set(UPGRADED_LASER_ENTITY_ID, -1);
        }

        public Vec3d generateFleePosition(LivingEntity fleer) {
            for (int i = 0; i < 15; i++) {
                BlockPos pos = fleer.getBlockPos().add(random.nextInt(32) - 16, random.nextInt(16), random.nextInt(32) - 16);
                while (fleer.getWorld().isAir(pos) && pos.getY() > 1) {
                    pos = pos.down();
                }
                if (fleer instanceof PathAwareEntity) {
                    if (((PathAwareEntity) fleer).getPathfindingFavor(pos) >= 0.0F) {
                        return Vec3d.ofCenter(pos);
                    }
                } else {
                    return Vec3d.ofCenter(pos);
                }
            }
            return null;
        }

        @Override
        public void tick() {
            LivingEntity target = EntityMimicOctopus.this.getTarget();
            if (target != null) {
                if (scareMobTime > 0) {
                    if (fleePosition == null || target.squaredDistanceTo(fleePosition) < target.getWidth() * target.getWidth() * 2) {
                        fleePosition = generateFleePosition(target);
                    }
                    if (target instanceof MobEntity) {
                        if (fleePosition != null) {
                            ((MobEntity) target).getNavigation().startMovingTo(fleePosition.x, fleePosition.y, fleePosition.z, 1.5F);
                            ((MobEntity) target).getMoveControl().moveTo(fleePosition.x, fleePosition.y, fleePosition.z, 1.5F);
                            ((MobEntity) target).setTarget(null);
                        }
                    }
                    camoCooldown = Math.max(camoCooldown, 20);
                    stopMimicCooldown = Math.max(stopMimicCooldown, 20);
                    scareMobTime--;
                    if (scareMobTime == 0) {
                        stop();
                        return;
                    }
                }
                double dist = EntityMimicOctopus.this.distanceTo(target);
                boolean move = true;
                if (dist < 7F && EntityMimicOctopus.this.canSee(target) && EntityMimicOctopus.this.getMimicState() == MimicState.GUARDIAN && EntityMimicOctopus.this.isUpgraded()) {
                    EntityMimicOctopus.this.dataTracker.set(UPGRADED_LASER_ENTITY_ID, target.getId());
                    move = false;
                }
                if (dist < 3) {
                    EntityMimicOctopus.this.dataTracker.set(LAST_SCARED_MOB_ID, target.getId());
                    if (move) {
                        move = EntityMimicOctopus.this.isUpgraded() && dist > 2;
                    }
                    EntityMimicOctopus.this.getNavigation().stop();
                    if (!EntityMimicOctopus.this.isStopChange()) {
                        EntityMimicOctopus.this.setMimickedBlock(null);
                        MimicState prev = EntityMimicOctopus.this.getMimicState();
                        if (EntityMimicOctopus.this.isInsideWaterOrBubbleColumn()) {
                            if (prev != MimicState.GUARDIAN && prev != MimicState.PUFFERFISH) {
                                if (random.nextBoolean()) {
                                    EntityMimicOctopus.this.setMimicState(MimicState.GUARDIAN);
                                } else {
                                    EntityMimicOctopus.this.setMimicState(MimicState.PUFFERFISH);
                                }
                            }
                        } else {
                            EntityMimicOctopus.this.setMimicState(MimicState.CREEPER);
                        }
                    }
                    if (EntityMimicOctopus.this.getMimicState() != MimicState.OVERLAY) {
                        EntityMimicOctopus.this.mimicCooldown = 40;
                        EntityMimicOctopus.this.stopMimicCooldown = Math.max(EntityMimicOctopus.this.stopMimicCooldown, 60);
                    }
                    if (EntityMimicOctopus.this.isUpgraded() && EntityMimicOctopus.this.transProgress >= 5.0F) {
                        if (EntityMimicOctopus.this.getMimicState() == MimicState.PUFFERFISH) {
                            if (EntityMimicOctopus.this.getBoundingBox().stretch(2, 1.3, 2).intersects(target.getBoundingBox())) {
                                target.damage(EntityMimicOctopus.this.getDamageSources().mobAttack(EntityMimicOctopus.this), 4);
                                target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 400, 2));
                            }
                        }
                        if (EntityMimicOctopus.this.getMimicState() == MimicState.GUARDIAN) {
                            if (EntityMimicOctopus.this.getBoundingBox().stretch(1, 1, 1).intersects(target.getBoundingBox())) {
                                target.damage(EntityMimicOctopus.this.getDamageSources().mobAttack(EntityMimicOctopus.this), 1);
                            }
                            EntityMimicOctopus.this.dataTracker.set(UPGRADED_LASER_ENTITY_ID, target.getId());
                        }
                        if (EntityMimicOctopus.this.getMimicState() == MimicState.CREEPER) {
                            EntityMimicOctopus.this.creeperExplode();
                            EntityMimicOctopus.this.getWorld().sendEntityStatus(EntityMimicOctopus.this, (byte) 69);
                            executionCooldown = 300;
                        }
                    }
                    if (scareMobTime == 0) {
                        EntityMimicOctopus.this.getWorld().sendEntityStatus(EntityMimicOctopus.this, (byte) 68);
                        scareMobTime = 60 + random.nextInt(60);
                    }
                }
                if (move) {
                    EntityMimicOctopus.this.lookAtEntity(target, 30, 30);
                    EntityMimicOctopus.this.getNavigation().startMovingTo(target, 1.2F);
                }
            }
        }
    }
}
