package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.IngredientUtil;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
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

public class EntityMantisShrimp extends TameableEntity implements ISemiAquatic, IFollower {

    private static final TrackedData<Float> RIGHT_EYE_PITCH = DataTracker.registerData(EntityMantisShrimp.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> RIGHT_EYE_YAW = DataTracker.registerData(EntityMantisShrimp.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> LEFT_EYE_PITCH = DataTracker.registerData(EntityMantisShrimp.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> LEFT_EYE_YAW = DataTracker.registerData(EntityMantisShrimp.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> PUNCH_TICK = DataTracker.registerData(EntityMantisShrimp.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityMantisShrimp.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> COMMAND = DataTracker.registerData(EntityMantisShrimp.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(EntityMantisShrimp.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> MOISTNESS = DataTracker.registerData(EntityMantisShrimp.class, TrackedDataHandlerRegistry.INTEGER);
    public float prevRightPitch;
    public float prevRightYaw;
    public float prevLeftPitch;
    public float prevLeftYaw;
    public float prevInWaterProgress;
    public float inWaterProgress;
    public float prevPunchProgress;
    public float punchProgress;
    private int leftLookCooldown = 0;
    private int rightLookCooldown = 0;
    private float targetRightPitch;
    private float targetRightYaw;
    private float targetLeftPitch;
    private float targetLeftYaw;
    private boolean isLandNavigator;
    private int fishFeedings;
    private int moistureAttackTime = 0;

    public EntityMantisShrimp(EntityType<EntityMantisShrimp> type, World world) {
        super(type, world);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 0.0F);
        switchNavigator(false);
        this.setStepHeight(1);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.MANTIS_SHRIMP_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.MANTIS_SHRIMP_HURT.get();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            var entity = source.getAttacker();
            if (entity instanceof ShulkerEntity || entity instanceof ShulkerBulletEntity) {
                amount = (amount + 1.0F) * 0.33F;
            }
            return super.damage(source, amount);
        }
    }

    //killEntity
    @Override
    public void updateKilledAdvancementCriterion(Entity entity, int score, DamageSource src) {
        if(entity instanceof LivingEntity living){
            if(living.getType() == EntityType.SHULKER){
                NbtCompound fishNbt = new NbtCompound();
                living.writeCustomDataToNbt(fishNbt);
                fishNbt.putString("DeathLootTable", LootTables.EMPTY.toString());
                living.readCustomDataFromNbt(fishNbt);
                living.dropItem(Items.SHULKER_SHELL);
            }
        }
        super.updateKilledAdvancementCriterion(entity, score, src);
    }

    public static boolean canMantisShrimpSpawn(EntityType type, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        BlockPos downPos = pos;
        while (downPos.getY() > 1 && !worldIn.getFluidState(downPos).isEmpty()) {
            downPos = downPos.down();
        }
        boolean spawnBlock = worldIn.getBlockState(downPos).isIn(AMTagRegistry.MANTIS_SHRIMP_SPAWNS);
        //limit spawns in mangrove biomes
        if(worldIn.getBiome(pos).isIn(AMTagRegistry.SPAWNS_WHITE_MANTIS_SHRIMP) && randomIn.nextFloat() < 0.5F){
            return false;
        }
        return spawnBlock && downPos.getY() < worldIn.getSeaLevel() + 1;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.1D)
                .add(EntityAttributes.GENERIC_ARMOR, 8D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3F);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceToClosestPlayer) {
        return !this.isTamed();
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.ARTHROPOD;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.mantisShrimpSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new MantisShrimpAIFryRice(this));
        this.goalSelector.add(0, new MantisShrimpAIBreakBlocks(this));
        this.goalSelector.add(1, new SitGoal(this));
        this.goalSelector.add(2, new FollowOwner(this, 1.3D, 4.0F, 2.0F, false));
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.2F, false));
        this.goalSelector.add(4, new AnimalAIFindWater(this));
        this.goalSelector.add(4, new AnimalAILeaveWater(this));
        this.goalSelector.add(5, new AnimalMateGoal(this, 0.8D));
        this.goalSelector.add(6, new TemptGoal(this, 1.0D, IngredientUtil.ingredientFromTags(AMTagRegistry.MANTIS_SHRIMP_BREEDABLES, AMTagRegistry.MANTIS_SHRIMP_TAMEABLES), false));
        this.goalSelector.add(7, new SemiAquaticAIRandomSwimming(this, 1.0D, 30));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, new EntityAINearestTarget3D<>(this, LivingEntity.class, 120, false, true, AMEntityRegistry.buildPredicateFromTag(AMTagRegistry.MANTIS_SHRIMP_TARGETS)) {
            @Override
            public boolean canStart() {
                return EntityMantisShrimp.this.getCommand() != 3 && !EntityMantisShrimp.this.isSitting() && super.canStart();
            }
        });
        this.targetSelector.add(4, new RevengeGoal(this));
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new GroundPathNavigatorWide(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new AnimalSwimMoveControllerSink(this, 1F, 1F);
            this.navigation = new SemiAquaticPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
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
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.DROWN) || source.isOf(DamageTypes.IN_WALL)  || super.isInvulnerableTo(source);
    }

    @Override
    public boolean canBreatheInWater() {
        return true;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(RIGHT_EYE_PITCH, 0F);
        this.dataTracker.startTracking(RIGHT_EYE_YAW, 0F);
        this.dataTracker.startTracking(LEFT_EYE_PITCH, 0F);
        this.dataTracker.startTracking(LEFT_EYE_YAW, 0F);
        this.dataTracker.startTracking(PUNCH_TICK, 0);
        this.dataTracker.startTracking(COMMAND, 0);
        this.dataTracker.startTracking(VARIANT, 0);
        this.dataTracker.startTracking(SITTING, false);
        this.dataTracker.startTracking(MOISTNESS, 60000);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        Item item = stack.getItem();
        return isTamed() && stack.isIn(AMTagRegistry.MANTIS_SHRIMP_BREEDABLES);
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        this.punch();
        return true;
    }

    public void punch() {
        this.dataTracker.set(PUNCH_TICK, 4);
    }

    public float getEyeYaw(boolean left) {
        return this.dataTracker.get(left ? LEFT_EYE_YAW : RIGHT_EYE_YAW);
    }

    public float getEyePitch(boolean left) {
        return this.dataTracker.get(left ? LEFT_EYE_PITCH : RIGHT_EYE_PITCH);
    }

    public void setEyePitch(boolean left, float pitch) {
        this.dataTracker.set(left ? LEFT_EYE_PITCH : RIGHT_EYE_PITCH, pitch);
    }

    public void setEyeYaw(boolean left, float yaw) {
        this.dataTracker.set(left ? LEFT_EYE_YAW : RIGHT_EYE_YAW, yaw);
    }

    public int getCommand() {
        return this.dataTracker.get(COMMAND);
    }

    public void setCommand(int command) {
        this.dataTracker.set(COMMAND, command);
    }

    @Override
    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    @Override
    public void setSitting(boolean sit) {
        this.dataTracker.set(SITTING, sit);
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int command) {
        this.dataTracker.set(VARIANT, command);
    }

    public int getMoistness() {
        return this.dataTracker.get(MOISTNESS);
    }

    public void setMoistness(int p_211137_1_) {
        this.dataTracker.set(MOISTNESS, p_211137_1_);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isAiDisabled()) {
            this.setAir(this.getMaxAir());
        } else {
            if (this.isWet() || this.getMainHandStack().getItem() == Items.WATER_BUCKET) {
                this.setMoistness(60000);
            } else {
                this.setMoistness(this.getMoistness() - 1);
                if (this.getMoistness() <= 0 && moistureAttackTime-- <= 0) {
                    this.setCommand(0);
                    this.setSitting(false);
                    this.damage(getDamageSources().dryOut(), random.nextInt(2) == 0 ? 1.0F : 0F);
                    moistureAttackTime = 20;
                }
            }
        }
        if(this.hasStatusEffect(StatusEffects.LEVITATION)){
            this.setVelocity(this.getVelocity().multiply(1F, 0.5F, 1F));
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        ActionResult type = super.interactMob(player, hand);
        if (!isTamed() && itemstack.isIn(AMTagRegistry.MANTIS_SHRIMP_TAMEABLES)) {
            this.eat(player, hand, itemstack);
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_STRIDER_EAT, this.getSoundVolume(), this.getSoundPitch());
            fishFeedings++;
            if (fishFeedings > 10 && getRandom().nextInt(6) == 0 || fishFeedings > 30) {
                this.setOwner(player);
                this.getWorld().sendEntityStatus(this, (byte) 7);
            } else {
                this.getWorld().sendEntityStatus(this, (byte) 6);
            }
            return ActionResult.SUCCESS;
        }
        if (isTamed() && itemstack.isIn(ItemTags.FISHES)) {
            if (this.getHealth() < this.getMaxHealth()) {
                this.eat(player, hand, itemstack);
                this.emitGameEvent(GameEvent.EAT);
                this.playSound(SoundEvents.ENTITY_STRIDER_EAT, this.getSoundVolume(), this.getSoundPitch());
                this.heal(5);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;

        }
        var actionResult = itemstack.useOnEntity(player, this, hand);
        if (actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS && isTamed() && isOwner(player)) {
            if (player.isSneaking() || itemstack.isIn(AMTagRegistry.SHRIMP_RICE_FRYABLES)) {
                if (this.getMainHandStack().isEmpty()) {
                    ItemStack cop = itemstack.copy();
                    cop.setCount(1);
                    this.setStackInHand(Hand.MAIN_HAND, cop);
                    itemstack.decrement(1);
                    return ActionResult.SUCCESS;
                } else {
                    this.dropStack(this.getMainHandStack().copy());
                    this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    return ActionResult.SUCCESS;
                }
            } else if (!isBreedingItem(itemstack)) {
                this.setCommand(this.getCommand() + 1);
                if (this.getCommand() == 4) {
                    this.setCommand(0);
                }
                if (this.getCommand() == 3) {
                    player.sendMessage(Text.translatable("entity.alexsmobs.mantis_shrimp.command_3", this.getName()), true);
                } else {
                    player.sendMessage(Text.translatable("entity.alexsmobs.all.command_" + this.getCommand(), this.getName()), true);
                }
                boolean sit = this.getCommand() == 2;
                this.setSitting(sit);
                return ActionResult.SUCCESS;
            }
        }
        return type;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("MantisShrimpSitting", this.isSitting());
        compound.putInt("Command", this.getCommand());
        compound.putInt("Moisture", this.getMoistness());
        compound.putInt("Variant", this.getVariant());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setSitting(compound.getBoolean("MantisShrimpSitting"));
        this.setCommand(compound.getInt("Command"));
        this.setVariant(compound.getInt("Variant"));
        this.setMoistness(compound.getInt("Moisture"));
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.isBaby() && this.getStandingEyeHeight() > this.getHeight()) {
            this.calculateDimensions();
        }
        prevLeftPitch = this.getEyePitch(true);
        prevRightPitch = this.getEyePitch(false);
        prevLeftYaw = this.getEyeYaw(true);
        prevRightYaw = this.getEyeYaw(false);
        prevInWaterProgress = this.inWaterProgress;
        prevPunchProgress = this.punchProgress;
        updateEyes();
        if (this.isSitting() && this.getNavigation().isIdle()) {
            this.getNavigation().stop();
        }

        if (this.isTouchingWater()) {
            if (inWaterProgress < 5F)
                inWaterProgress++;

            if (this.isLandNavigator)
                switchNavigator(false);
        } else {
            if (inWaterProgress > 0F)
                inWaterProgress--;

            if (!this.isLandNavigator)
                switchNavigator(true);
        }

        if (this.dataTracker.get(PUNCH_TICK) > 0) {
            if (this.dataTracker.get(PUNCH_TICK) == 2 && this.getTarget() != null && this.distanceTo(this.getTarget()) < 2.8D) {
                if (this.getTarget() instanceof FishEntity fish && !this.isTamed()) {
                    NbtCompound fishNbt = new NbtCompound();
                    fish.writeCustomDataToNbt(fishNbt);
                    fishNbt.putString("DeathLootTable", LootTables.EMPTY.toString());
                    fish.readCustomDataFromNbt(fishNbt);
                }
                this.getTarget().takeKnockback(1.7F, this.getX() - this.getTarget().getX(), this.getZ() - this.getTarget().getZ());
                float knockbackResist = (float) MathHelper.clamp((1.0D - this.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)), 0, 1);
                this.getTarget().setVelocity(this.getTarget().getVelocity().add(0, knockbackResist * 0.8F, 0));
                if (!this.getTarget().isTouchingWater()) {
                    this.getTarget().setOnFireFor(2);
                }
                this.getTarget().damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
            }
            if(punchProgress == 1){
                this.playSound(AMSoundRegistry.MANTIS_SHRIMP_SNAP.get(), this.getSoundPitch(), this.getSoundVolume());
            }
            if (punchProgress == 2 && this.getWorld().isClient && this.isTouchingWater()) {
                for (int i = 0; i < 10 + random.nextInt(8); i++) {
                    double d2 = this.random.nextGaussian() * 0.6D;
                    double d0 = this.random.nextGaussian() * 0.2D;
                    double d1 = this.random.nextGaussian() * 0.6D;
                    float radius = this.getWidth() * 0.85F;
                    float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
                    double extraX = radius * MathHelper.sin(MathHelper.PI + angle) + random.nextFloat() * 0.5F - 0.25F;
                    double extraZ = radius * MathHelper.cos(angle) + random.nextFloat() * 0.5F - 0.25F;
                    var data = ParticleTypes.BUBBLE;
                    this.getWorld().addParticle(data, this.getX() + extraX, this.getY() + this.getHeight() * 0.3F + random.nextFloat() * 0.15F, this.getZ() + extraZ, d0, d1, d2);
                }
            }
            if (punchProgress < 2F) {
                punchProgress++;
            }
            this.dataTracker.set(PUNCH_TICK, this.dataTracker.get(PUNCH_TICK) - 1);
        } else {
            if (punchProgress > 0F) {
                punchProgress -= 0.25F;
            }
        }
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

    private void updateEyes() {
        float leftPitchDist = Math.abs(this.getEyePitch(true) - targetLeftPitch);
        float rightPitchDist = Math.abs(this.getEyePitch(false) - targetRightPitch);
        float leftYawDist = Math.abs(this.getEyeYaw(true) - targetLeftYaw);
        float rightYawDist = Math.abs(this.getEyeYaw(false) - targetRightYaw);
        if (rightLookCooldown == 0 && this.random.nextInt(20) == 0 && rightPitchDist < 0.5F && rightYawDist < 0.5F) {
            targetRightPitch = MathHelper.clamp(random.nextFloat() * 60F - 30, -30, 30);
            targetRightYaw = MathHelper.clamp(random.nextFloat() * 60F - 30, -30, 30);
            rightLookCooldown = 3 + random.nextInt(15);
        }
        if (leftLookCooldown == 0 && this.random.nextInt(20) == 0 && leftPitchDist < 0.5F && leftYawDist < 0.5F) {
            targetLeftPitch = MathHelper.clamp(random.nextFloat() * 60F - 30, -30, 30);
            targetLeftYaw = MathHelper.clamp(random.nextFloat() * 60F - 30, -30, 30);
            leftLookCooldown = 3 + random.nextInt(15);
        }

        if (leftPitchDist > 0.5F) {
            if (this.getEyePitch(true) < this.targetLeftPitch) {
                this.setEyePitch(true, this.getEyePitch(true) + Math.min(leftPitchDist, 4F));
            }
            if (this.getEyePitch(true) > this.targetLeftPitch) {
                this.setEyePitch(true, this.getEyePitch(true) - Math.min(leftPitchDist, 4F));
            }
        }

        if (rightPitchDist > 0.5F) {
            if (this.getEyePitch(false) < this.targetRightPitch) {
                this.setEyePitch(false, this.getEyePitch(false) + Math.min(rightPitchDist, 4F));
            }
            if (this.getEyePitch(false) > this.targetRightPitch) {
                this.setEyePitch(false, this.getEyePitch(false) - Math.min(rightPitchDist, 4F));
            }
        }

        if (leftYawDist > 0.5F) {
            if (this.getEyeYaw(true) < this.targetLeftYaw) {
                this.setEyeYaw(true, this.getEyeYaw(true) + Math.min(leftYawDist, 4F));
            }
            if (this.getEyeYaw(true) > this.targetLeftYaw) {
                this.setEyeYaw(true, this.getEyeYaw(true) - Math.min(leftYawDist, 4F));
            }
        }

        if (rightYawDist > 0.5F) {
            if (this.getEyeYaw(false) < this.targetRightYaw) {
                this.setEyeYaw(false, this.getEyeYaw(false) + Math.min(rightYawDist, 4F));
            }
            if (this.getEyeYaw(false) > this.targetRightYaw) {
                this.setEyeYaw(false, this.getEyeYaw(false) - Math.min(rightYawDist, 4F));
            }
        }

        if (rightLookCooldown > 0) {
            rightLookCooldown--;
        }

        if (leftLookCooldown > 0) {
            leftLookCooldown--;
        }
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Nullable
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        int i;
        if(reason == SpawnReason.SPAWN_EGG){
            i = this.getRandom().nextInt(4);
        }else if(worldIn.getBiome(this.getBlockPos()).isIn(AMTagRegistry.SPAWNS_WHITE_MANTIS_SHRIMP)){
            i = 3;
        }else{
            i = this.getRandom().nextInt(3);
        }
        this.setVariant(i);
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        EntityMantisShrimp shrimp = AMEntityRegistry.MANTIS_SHRIMP.get().create(world);
        shrimp.setVariant(getRandom().nextInt(3));
        return shrimp;
    }

    @Override
    public boolean shouldEnterWater() {
        return (this.getMainHandStack().isEmpty() || this.getMainHandStack().getItem() != Items.WATER_BUCKET) && !this.isSitting();
    }

    @Override
    public boolean shouldLeaveWater() {
        return this.getMainHandStack().getItem() == Items.WATER_BUCKET;
    }

    @Override
    public boolean shouldStopMoving() {
        return isSitting();
    }

    @Override
    public int getWaterSearchRange() {
        return 16;
    }

    @Override
    public boolean shouldFollow() {
        return this.getCommand() == 1;
    }

    @Override
    public boolean canSpawn(WorldView worldIn) {
        return worldIn.doesNotIntersectEntities(this);
    }

    protected void updateAir(int p_209207_1_) {
    }

    @Override
    public EntityView method_48926() {
        return getWorld();
    }

    public static class FollowOwner extends Goal {
        private final EntityMantisShrimp tameable;
        private final WorldView world;
        private final double followSpeed;
        private final float maxDist;
        private final float minDist;
        private final boolean teleportToLeaves;
        private LivingEntity owner;
        private int timeToRecalcPath;
        private float oldWaterCost;

        public FollowOwner(EntityMantisShrimp p_i225711_1_, double p_i225711_2_, float p_i225711_4_, float p_i225711_5_, boolean p_i225711_6_) {
            this.tameable = p_i225711_1_;
            this.world = p_i225711_1_.getWorld();
            this.followSpeed = p_i225711_2_;
            this.minDist = p_i225711_4_;
            this.maxDist = p_i225711_5_;
            this.teleportToLeaves = p_i225711_6_;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity lvt_1_1_ = this.tameable.getOwner();
            if (lvt_1_1_ == null) {
                return false;
            } else if (lvt_1_1_.isSpectator()) {
                return false;
            } else if (this.tameable.isSitting() || tameable.getCommand() != 1) {
                return false;
            } else if (this.tameable.squaredDistanceTo(lvt_1_1_) < (double) (this.minDist * this.minDist)) {
                return false;
            } else if (this.tameable.getTarget() != null && this.tameable.getTarget().isAlive()) {
                return false;
            } else {
                this.owner = lvt_1_1_;
                return true;
            }
        }

        @Override
        public boolean shouldContinue() {
            if (this.tameable.getNavigation().isIdle()) {
                return false;
            } else if (this.tameable.isSitting() || tameable.getCommand() != 1) {
                return false;
            } else if (this.tameable.getTarget() != null && this.tameable.getTarget().isAlive()) {
                return false;
            } else {
                return this.tameable.squaredDistanceTo(this.owner) > (double) (this.maxDist * this.maxDist);
            }
        }

        @Override
        public void start() {
            this.timeToRecalcPath = 0;
            this.oldWaterCost = this.tameable.getPathfindingPenalty(PathNodeType.WATER);
            this.tameable.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        }

        @Override
        public void stop() {
            this.owner = null;
            this.tameable.getNavigation().stop();
            this.tameable.setPathfindingPenalty(PathNodeType.WATER, this.oldWaterCost);
        }

        @Override
        public void tick() {

            this.tameable.getLookControl().lookAt(this.owner, 10.0F, (float) this.tameable.getMaxLookPitchChange());
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                if (!this.tameable.isLeashed() && !this.tameable.hasVehicle()) {
                    if (this.tameable.squaredDistanceTo(this.owner) >= 144.0D) {
                        this.tryToTeleportNearEntity();
                    } else {
                        this.tameable.getNavigation().startMovingTo(this.owner, this.followSpeed);
                    }

                }
            }
        }

        private void tryToTeleportNearEntity() {
            BlockPos lvt_1_1_ = this.owner.getBlockPos();

            for (int lvt_2_1_ = 0; lvt_2_1_ < 10; ++lvt_2_1_) {
                int lvt_3_1_ = this.getRandomNumber(-3, 3);
                int lvt_4_1_ = this.getRandomNumber(-1, 1);
                int lvt_5_1_ = this.getRandomNumber(-3, 3);
                boolean lvt_6_1_ = this.tryToTeleportToLocation(lvt_1_1_.getX() + lvt_3_1_, lvt_1_1_.getY() + lvt_4_1_, lvt_1_1_.getZ() + lvt_5_1_);
                if (lvt_6_1_) {
                    return;
                }
            }

        }

        private boolean tryToTeleportToLocation(int p_226328_1_, int p_226328_2_, int p_226328_3_) {
            if (Math.abs((double) p_226328_1_ - this.owner.getX()) < 2.0D && Math.abs((double) p_226328_3_ - this.owner.getZ()) < 2.0D) {
                return false;
            } else if (!this.isTeleportFriendlyBlock(new BlockPos(p_226328_1_, p_226328_2_, p_226328_3_))) {
                return false;
            } else {
                this.tameable.refreshPositionAndAngles((double) p_226328_1_ + 0.5D, p_226328_2_, (double) p_226328_3_ + 0.5D, this.tameable.getYaw(), this.tameable.getPitch());
                this.tameable.getNavigation().stop();
                return true;
            }
        }

        private boolean isTeleportFriendlyBlock(BlockPos p_226329_1_) {
            var lvt_2_1_ = LandPathNodeMaker.getLandNodeType(this.world, p_226329_1_.mutableCopy());
            if (world.getFluidState(p_226329_1_).isIn(FluidTags.WATER) || !world.getFluidState(p_226329_1_).isIn(FluidTags.WATER) && world.getFluidState(p_226329_1_.down()).isIn(FluidTags.WATER)) {
                return true;
            }
            if (lvt_2_1_ != PathNodeType.WALKABLE || tameable.getMoistness() < 2000) {
                return false;
            } else {
                var lvt_3_1_ = this.world.getBlockState(p_226329_1_.down());
                if (!this.teleportToLeaves && lvt_3_1_.getBlock() instanceof LeavesBlock) {
                    return false;
                } else {
                    var lvt_4_1_ = p_226329_1_.subtract(this.tameable.getBlockPos());
                    return this.world.isSpaceEmpty(this.tameable, this.tameable.getBoundingBox().offset(lvt_4_1_));
                }
            }
        }

        private int getRandomNumber(int p_226327_1_, int p_226327_2_) {
            return this.tameable.getRandom().nextInt(p_226327_2_ - p_226327_1_ + 1) + p_226327_1_;
        }
    }
}
