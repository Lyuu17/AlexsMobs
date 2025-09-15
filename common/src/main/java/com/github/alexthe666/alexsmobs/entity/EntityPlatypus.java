package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.block.ReptileEggBlock;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.registry.*;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.*;
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
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EntityPlatypus extends AnimalEntity implements ISemiAquatic, ITargetsDroppedItems, Bucketable {

    private static final TrackedData<Boolean> SENSING = DataTracker.registerData(EntityPlatypus.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SENSING_VISUAL = DataTracker.registerData(EntityPlatypus.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> DIGGING = DataTracker.registerData(EntityPlatypus.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> FEDORA = DataTracker.registerData(EntityPlatypus.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> FROM_BUCKET = DataTracker.registerData(EntityPlatypus.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HAS_EGG = DataTracker.registerData(EntityPlatypus.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float prevInWaterProgress;
    public float inWaterProgress;
    public float prevDigProgress;
    public float digProgress;
    public boolean superCharged = false;
    private boolean isLandNavigator;
    private int swimTimer = -1000;

    public EntityPlatypus(EntityType<? extends EntityPlatypus> type, World world) {
        super(type, world);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 0.0F);
        switchNavigator(false);
    }

    public static boolean canPlatypusSpawn(EntityType type, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        return worldIn.getBlockState(pos.down()).isIn(AMTagRegistry.PLATYPUS_SPAWNS) && pos.getY() < worldIn.getSeaLevel() + 4;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.platypusSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.PLATYPUS_BREEDABLES);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.PLATYPUS_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.PLATYPUS_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.PLATYPUS_HURT.get();
    }

    @Override
    @NotNull
    public ItemStack getBucketItem() {
        var stack = new ItemStack(AMItemRegistry.PLATYPUS_BUCKET.get());
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
        compound.put("PlatypusData", platTag);
    }

    @Override
    public void copyDataFromNbt(@NotNull NbtCompound compound) {
        if (compound.contains("PlatypusData")) {
            this.readCustomDataFromNbt(compound.getCompound("PlatypusData"));
        }
    }

    @Override
    @NotNull
    public ActionResult interactMob(PlayerEntity player, Hand hand) {

        ItemStack itemstack = player.getStackInHand(hand);
        if(itemstack.getItem() == AMItemRegistry.FEDORA.get() && !this.hasFedora()){
            if (!player.isCreative()) {
                itemstack.decrement(1);
            }
            this.setFedora(true);
            return ActionResult.success(this.getWorld().isClient);
        }
        if (itemstack.isIn(AMTagRegistry.PLATYPUS_CHARGEABLES) && !this.isSensing()) {
            superCharged = itemstack.isIn(AMTagRegistry.PLATYPUS_SUPER_CHARGEABLES);
            if (!player.isCreative()) {
                itemstack.decrement(1);
            }
            this.setSensing(true);
            return ActionResult.success(this.getWorld().isClient);
        }
        return Bucketable.tryBucket(player, hand, this).orElse(super.interactMob(player, hand));
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new BreatheAirGoal(this));
        this.goalSelector.add(1, new AnimalAIFindWater(this));
        this.goalSelector.add(1, new AnimalAILeaveWater(this));
        this.goalSelector.add(2, new MateGoal(this, 1.0D));
        this.goalSelector.add(2, new LayEggGoal(this, 1.0D));
        this.goalSelector.add(2, new AnimalMateGoal(this, 0.8D));
        this.goalSelector.add(3, new EscapeDangerGoal(this, 1.1D));
        this.goalSelector.add(3, new TemptGoal(this, 1.0D, Ingredient.fromTag(AMTagRegistry.PLATYPUS_CHARGEABLES), false){
            @Override
            public void start() {
                super.start();
                EntityPlatypus.this.setSensingVisual(true);
            }

            @Override
            public boolean canStart() {
                return super.canStart() && !EntityPlatypus.this.isSensing();
            }

            @Override
            public void stop() {
                super.stop();
                EntityPlatypus.this.setSensingVisual(false);
            }
        });
        this.goalSelector.add(5, new TemptGoal(this, 1.1D, Ingredient.fromTag(AMTagRegistry.PLATYPUS_FOODSTUFFS), false){
            @Override
            public boolean canStart(){
                return super.canStart() && !EntityPlatypus.this.isSensing();
            }
        });
        this.goalSelector.add(5, new PlatypusAIDigForItems(this));
        this.goalSelector.add(6, new SemiAquaticAIRandomSwimming(this, 1.0D, 30));
        this.goalSelector.add(7, new WanderAroundGoal(this, 1.0D, 60));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.targetSelector.add(1, new CreatureAITargetItems<>(this, false, false, 40, 15){
            @Override
            public boolean canStart(){
                return super.canStart() && !EntityPlatypus.this.isSensing();
            }

            @Override
            public boolean shouldContinue(){
                return super.shouldContinue() && !EntityPlatypus.this.isSensing();
            }
        });
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean prev = super.damage(source, amount);
        if(prev && source.getSource() instanceof LivingEntity entity){
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100));
        }
        return prev;
    }

    public boolean isPerry() {
        String s = Formatting.strip(this.getName().getString());
        return s != null && s.toLowerCase().contains("perry");
    }

    @Override
    public int getMaxAir() {
        return 4800;
    }

    @Override
    protected int getNextAirOnLand(int currentAir) {
        return this.getMaxAir();
    }

    public void spawnGroundEffects() {
        float radius = 0.3F;
        for (int i1 = 0; i1 < 3; i1++) {
            double motionX = getRandom().nextGaussian() * 0.07D;
            double motionY = getRandom().nextGaussian() * 0.07D;
            double motionZ = getRandom().nextGaussian() * 0.07D;
            float angle = (Maths.STARTING_ANGLE * this.bodyYaw) + i1;
            double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            double extraY = 0.8F;
            double extraZ = radius * MathHelper.cos(angle);
            var ground = this.getVelocityAffectingPos();
            var state = this.getWorld().getBlockState(ground);
            if (state.isSolid()) {
                if (this.getWorld().isClient) {
                    getWorld().addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), true, this.getX() + extraX, ground.getY() + extraY, this.getZ() + extraZ, motionX, motionY, motionZ);
                }
            }
        }
    }

    @Nullable
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason
            reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.setAir(this.getMaxAir());
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
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

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(DIGGING, false);
        this.dataTracker.startTracking(SENSING, false);
        this.dataTracker.startTracking(SENSING_VISUAL, false);
        this.dataTracker.startTracking(FEDORA, false);
        this.dataTracker.startTracking(FROM_BUCKET, false);
        this.dataTracker.startTracking(HAS_EGG, false);
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if (this.hasFedora()) {
            this.dropItem(AMItemRegistry.FEDORA.get());
        }
    }

    public boolean isSensing() {
        return this.dataTracker.get(SENSING);
    }

    public void setSensing(boolean sensing) {
        this.dataTracker.set(SENSING, Boolean.valueOf(sensing));
    }

    public boolean isSensingVisual() {
        return this.dataTracker.get(SENSING_VISUAL);
    }

    public void setSensingVisual(boolean sensing) {
        this.dataTracker.set(SENSING_VISUAL, Boolean.valueOf(sensing));
    }

    public boolean hasFedora() {
        return this.dataTracker.get(FEDORA);
    }

    public void setFedora(boolean sensing) {
        this.dataTracker.set(FEDORA, Boolean.valueOf(sensing));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Fedora", this.hasFedora());
        compound.putBoolean("Sensing", this.isSensing());
        compound.putBoolean("FromBucket", this.isFromBucket());
        compound.putBoolean("HasEgg", this.hasEgg());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setFedora(compound.getBoolean("Fedora"));
        this.setSensing(compound.getBoolean("Sensing"));
        this.setFromBucket(compound.getBoolean("FromBucket"));
        this.setHasEgg(compound.getBoolean("HasEgg"));
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
    public boolean canImmediatelyDespawn(double dist) {
        return !this.isFromBucket() && !this.cannotDespawn();
    }

    @Override
    public void tick() {
        super.tick();
        prevInWaterProgress = inWaterProgress;
        prevDigProgress = digProgress;
        boolean dig = isDigging() && isInsideWaterOrBubbleColumn();
        if (dig && digProgress < 5F) {
            digProgress++;
        }
        if (!dig && digProgress > 0F) {
            digProgress--;
        }

        if (this.isInsideWaterOrBubbleColumn()) {
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

        if (this.isOnGround() && isDigging()) {
            spawnGroundEffects();
        }
        if (inWaterProgress > 0) {
            this.setStepHeight(1);
        } else {
            this.setStepHeight(0.6F);
        }
        if (!this.getWorld().isClient) {
            if (isTouchingWater()) {
                swimTimer++;
            } else {
                swimTimer--;
            }
        }
        if (this.isAlive() && (this.isSensing() || this.isSensingVisual())) {
            for (int j = 0; j < 2; ++j) {
                float radius = this.getWidth() * 0.65F;
                float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
                double extraX = (radius * (1.5F + random.nextFloat() * 0.3F)) * MathHelper.sin(MathHelper.PI + angle) + (random.nextFloat() - 0.5F) + this.getVelocity().x * 2F;
                double extraZ = (radius * (1.5F + random.nextFloat() * 0.3F)) * MathHelper.cos(angle) + (random.nextFloat() - 0.5F) + this.getVelocity().z * 2F;
                double actualX = radius * MathHelper.sin(MathHelper.PI + angle);
                double actualZ = radius * MathHelper.cos(angle);
                double motX = actualX - extraX;
                double motZ = actualZ - extraZ;
                this.getWorld().addParticle(AMParticleRegistry.PLATYPUS_SENSE.get(), this.getX() + extraX, this.getHeight() * 0.3F + this.getY(), this.getZ() + extraZ, motX * 0.1F, 0, motZ * 0.1F);
            }
        }
    }

    public boolean isDigging() {
        return this.dataTracker.get(DIGGING);
    }

    public void setDigging(boolean digging) {
        this.dataTracker.set(DIGGING, digging);
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new GroundPathNavigatorWide(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new AnimalSwimMoveControllerSink(this, 1.2F, 1.6F);
            this.navigation = new SemiAquaticPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    public boolean shouldEnterWater() {
        return (this.getAttacker() != null || swimTimer <= -1000 || this.isSensing()) && !this.hasEgg();
    }

    @Override
    public boolean shouldLeaveWater() {
        return swimTimer > 600 && !this.isSensing() || this.hasEgg();
    }

    @Override
    public boolean shouldStopMoving() {
        return this.isDigging();
    }

    @Override
    public int getWaterSearchRange() {
        return 10;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.PLATYPUS.get().create(world);
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return !this.isSensing() && stack.isIn(AMTagRegistry.PLATYPUS_FOODSTUFFS);
    }

    @Override
    public void onGetItem(ItemEntity e) {
        this.emitGameEvent(GameEvent.EAT);
        this.playSound(SoundEvents.ENTITY_CAT_EAT, this.getSoundVolume(), this.getSoundPitch());
        if(e.getStack().isIn(AMTagRegistry.PLATYPUS_CHARGEABLES)) {
            superCharged = e.getStack().isIn(AMTagRegistry.PLATYPUS_SUPER_CHARGEABLES);
            this.setSensing(true);
        }
    }

    public boolean hasEgg() {
        return this.dataTracker.get(HAS_EGG);
    }

    private void setHasEgg(boolean hasEgg) {
        this.dataTracker.set(HAS_EGG, hasEgg);
    }

    static class MateGoal extends AnimalMateGoal {
        private final EntityPlatypus platypus;

        MateGoal(EntityPlatypus platypus, double speedIn) {
            super(platypus, speedIn);
            this.platypus = platypus;
        }

        @Override
        public boolean canStart() {
            return super.canStart() && !this.platypus.hasEgg();
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

            this.platypus.setHasEgg(true);
            this.animal.resetLoveTicks();
            this.mate.resetLoveTicks();
            this.animal.setBreedingAge(6000);
            this.mate.setBreedingAge(6000);

            if (this.world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
                final Random random = this.animal.getRandom();
                this.world.spawnEntity(new ExperienceOrbEntity(this.world, this.animal.getX(), this.animal.getY(), this.animal.getZ(), random.nextInt(7) + 1));
            }

        }
    }

    static class LayEggGoal extends MoveToTargetPosGoal {
        private final EntityPlatypus turtle;

        LayEggGoal(EntityPlatypus turtle, double speedIn) {
            super(turtle, speedIn, 16);
            this.turtle = turtle;
        }

        @Override
        public void stop() {
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
            if (!this.turtle.isTouchingWater() && this.hasReached()) {
                final BlockPos blockpos = this.turtle.getBlockPos();
                final World world = this.turtle.getWorld();
                turtle.emitGameEvent(GameEvent.BLOCK_PLACE);
                world.playSound(null, blockpos, SoundEvents.ENTITY_TURTLE_LAY_EGG, SoundCategory.BLOCKS, 0.3F, 0.9F + world.random.nextFloat() * 0.2F);
                world.setBlockState(this.targetPos.up(), AMBlockRegistry.PLATYPUS_EGG.get().getDefaultState().with(ReptileEggBlock.EGGS, this.turtle.random.nextInt(3) + 1), 3);
                this.turtle.setHasEgg(false);
                this.turtle.setDigging(false);
                this.turtle.setLoveTicks(600);
            }
        }

        @Override
        protected boolean isTargetPos(WorldView worldIn, BlockPos pos) {
            return worldIn.isAir(pos.up()) && ReptileEggBlock.isProperHabitat(worldIn, pos);
        }
    }
}
