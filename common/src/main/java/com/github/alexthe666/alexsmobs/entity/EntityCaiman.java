package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.block.BlockReptileEgg;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.registry.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Blocks;
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
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
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

public class EntityCaiman extends TameableEntity implements ISemiAquatic,IFollower {

    private static final TrackedData<Integer> COMMAND = DataTracker.registerData(EntityCaiman.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityCaiman.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BELLOWING = DataTracker.registerData(EntityCaiman.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> HELD_MOB_ID = DataTracker.registerData(EntityCaiman.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> HAS_EGG = DataTracker.registerData(EntityCaiman.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float prevSitProgress;
    public float sitProgress;
    public float prevHoldProgress;
    public float holdProgress;
    public float prevSwimProgress;
    public float swimProgress;
    public float prevVibrateProgress;
    public float vibrateProgress;
    private int swimTimer = -1000;
    public int bellowCooldown = 100 + random.nextInt(1000);
    private boolean isLandNavigator;
    public boolean tameAttackFlag = false;

    public EntityCaiman(EntityType<? extends EntityCaiman> type, World level) {
        super(type, level);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 0.0F);
        switchNavigator(false);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(COMMAND, 0);
        this.dataTracker.startTracking(BELLOWING, false);
        this.dataTracker.startTracking(SITTING, false);
        this.dataTracker.startTracking(HAS_EGG, false);
        this.dataTracker.startTracking(HELD_MOB_ID, -1);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SitGoal(this));
        this.goalSelector.add(1, new MateGoal(this, 1.0D));
        this.goalSelector.add(1, new LayEggGoal(this, 1.0D));
        this.goalSelector.add(2, new CaimanAIMelee(this));
        this.goalSelector.add(3, new BreatheAirGoal(this));
        this.goalSelector.add(4, new TameableAIFollowOwnerWater(this, 1.1D, 4.0F, 2.0F, false));
        this.goalSelector.add(5, new MeleeAttackGoal(this, 1.2F, false));
        this.goalSelector.add(6, new TemptGoal(this, 1.1D, Ingredient.fromTag(AMTagRegistry.CAIMAN_BREEDABLES), false));
        this.goalSelector.add(7, new AnimalAIFindWater(this));
        this.goalSelector.add(7, new AnimalAILeaveWater(this));
        this.goalSelector.add(8, new CaimanAIBellow(this));
        this.goalSelector.add(9, new SemiAquaticAIRandomSwimming(this, 1.0D, 30));
        this.goalSelector.add(10, new WanderAroundGoal(this, 1.0D, 60));
        this.goalSelector.add(11, new LookAroundGoal(this));
        this.goalSelector.add(11, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.targetSelector.add(1, (new AnimalAIHurtByTargetNotBaby(this)).setGroupRevenge());
        this.targetSelector.add(2, new TrackOwnerAttackerGoal(this) {
            @Override
            public void start() {
                super.start();
                tameAttackFlag = true;
            }

            @Override
            public void stop() {
                super.start();
                tameAttackFlag = false;
            }
        });
        this.targetSelector.add(3, new AttackWithOwnerGoal(this) {
            @Override
            public void start() {
                super.start();
                tameAttackFlag = true;
            }

            @Override
            public void stop() {
                super.start();
                tameAttackFlag = false;
            }
        });
        this.targetSelector.add(5, new EntityAINearestTarget3D<>(this, LivingEntity.class, 180, false, true, AMEntityRegistry.buildPredicateFromTag(AMTagRegistry.CAIMAN_TARGETS)) {
            @Override
            public boolean canStart() {
                return !isBaby() && !isTamed() && super.canStart();
            }
        });
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.caimanSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static <T extends MobEntity> boolean canCaimanSpawn(EntityType type, WorldAccess worldIn, SpawnReason reason, BlockPos p_223317_3_, Random random) {
        var blockstate = worldIn.getBlockState(p_223317_3_.down());
        return blockstate.isOf(Blocks.MUD) || blockstate.isOf(Blocks.MUDDY_MANGROVE_ROOTS) || blockstate.isIn(AMTagRegistry.CAIMAN_SPAWNS);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.CAIMAN_BREEDABLES);
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new MobNavigation(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new AquaticMoveController(this, 1.1F);
            this.navigation = new SemiAquaticPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    public int getLimitPerChunk() {
        return 2;
    }

    @Override
    public boolean spawnsTooManyForEachTry(int sizeIn) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isBaby() ? AMSoundRegistry.CROCODILE_BABY.get() : AMSoundRegistry.CAIMAN_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.CAIMAN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.CAIMAN_HURT.get();
    }

    @Override
    public void tick() {
        super.tick();
        this.prevHoldProgress = holdProgress;
        this.prevSwimProgress = swimProgress;
        this.prevSitProgress = sitProgress;
        this.prevVibrateProgress = vibrateProgress;

        final boolean ground = !this.isInsideWaterOrBubbleColumn();
        final boolean bellowing = this.isBellowing();
        final boolean grabbing = this.getHeldMobId() != -1;
        final boolean sitting = this.isSitting() && ground;

        if (!ground && this.isLandNavigator) {
            switchNavigator(false);
        }
        if (ground && !this.isLandNavigator) {
            switchNavigator(true);
        }
        if (ground && swimProgress > 0) {
            swimProgress--;
        }
        if (!ground && swimProgress < 5F) {
            swimProgress++;
        }
        if (bellowing && vibrateProgress < 5) {
            vibrateProgress++;
        }
        if (!bellowing && vibrateProgress > 0F) {
            vibrateProgress--;
        }
        if (sitting && sitProgress < 5) {
            sitProgress++;
        }
        if (!sitting && sitProgress > 0F) {
            sitProgress--;
        }
        if (grabbing && holdProgress < 5) {
            holdProgress += 2.5F;
        }
        if (!grabbing && holdProgress > 0F) {
            holdProgress -= 2.5F;
        }
        if (!this.getWorld().isClient) {
            if (isTouchingWater()) {
                swimTimer++;
            } else {
                if(this.isBellowing()){
                    this.setBellowing(false);
                }
                swimTimer--;
            }
            if (this.getTarget() instanceof WaterCreatureEntity fish && !this.isTamed()) {
                NbtCompound fishNbt = new NbtCompound();
                fish.writeCustomDataToNbt(fishNbt);
                fishNbt.putString("DeathLootTable", LootTables.EMPTY.toString());
                fish.readCustomDataFromNbt(fishNbt);
            }
        } else {
            if (this.isInsideWaterOrBubbleColumn() && this.isBellowing()) {
                int particles = 4 + getRandom().nextInt(3);
                for (int i = 0; i <= particles; i++) {
                    var particleVec = new Vec3d(0, 0, 1.0F).rotateY((i / (float) particles) * (MathHelper.PI) * 2F).add(this.getPos());
                    //FIXME forge
//                    double particleY = this.getBoundingBox().minY + getFluidTypeHeight(ForgeMod.WATER_TYPE.get());
//                    this.getWorld().addParticle(ParticleTypes.SPLASH, particleVec.x, particleY, particleVec.z, 0, 0.3F, 0);
                }
            }
        }
        if (bellowCooldown > 0) {
            bellowCooldown--;
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        ActionResult type = super.interactMob(player, hand);
        if (isTamed() && itemstack.isIn(AMTagRegistry.CAIMAN_FOODSTUFFS) && this.getHealth() < this.getMaxHealth()) {
            this.eat(player, hand, itemstack);
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_CAT_EAT, this.getSoundVolume(), this.getSoundPitch());
            this.heal(5);
            return ActionResult.SUCCESS;
        }
        var actionResult = itemstack.useOnEntity(player, this, hand);
        if (actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS && isTamed() && isOwner(player) && !isBreedingItem(itemstack)) {
            this.setCommand(this.getCommand() + 1);
            if (this.getCommand() == 3) {
                this.setCommand(0);
            }
            player.sendMessage(Text.translatable("entity.alexsmobs.all.command_" + this.getCommand(), this.getName()), true);
            boolean sit = this.getCommand() == 2;
            this.setSitting(sit);
            return ActionResult.SUCCESS;
        }
        return type;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Override
    public boolean canSpawn(WorldView worldIn) {
        return worldIn.doesNotIntersectEntities(this);
    }

    public int getCommand() {
        return this.dataTracker.get(COMMAND);
    }

    public void setCommand(int command) {
        this.dataTracker.set(COMMAND, Integer.valueOf(command));
    }

    public void setHeldMobId(int i) {
        this.dataTracker.set(HELD_MOB_ID, i);
    }

    public int getHeldMobId() {
        return this.dataTracker.get(HELD_MOB_ID);
    }

    public boolean hasEgg() {
        return this.dataTracker.get(HAS_EGG);
    }

    private void setHasEgg(boolean hasEgg) {
        this.dataTracker.set(HAS_EGG, hasEgg);
    }

    public Entity getHeldMob() {
        final int id = getHeldMobId();
        return id == -1 ? null : getWorld().getEntityById(id);
    }

    @Override
    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    @Override
    public void setSitting(boolean sit) {
        this.dataTracker.set(SITTING, sit);
    }

    public boolean isBellowing() {
        return this.dataTracker.get(BELLOWING);
    }

    public void setBellowing(boolean bellowing) {
        this.dataTracker.set(BELLOWING, bellowing);
    }

    @Override
    public void travel(Vec3d travelVector) {
        if(isSitting()){
            super.travel(Vec3d.ZERO);
        }else if (this.canMoveVoluntarily() && this.isTouchingWater()) {
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

    public void updateLimbs(LivingEntity living, boolean flying) {
        float f1 = (float) MathHelper.magnitude(this.getX() - this.prevX, 0, this.getZ() - this.prevZ);
        float f2 = Math.min(f1 * 8.0F, 1.0F);
        this.limbAnimator.updateLimbs(f2, 0.4F);
    }

    @Override
    public boolean canBreatheInWater() {
        return true;
    }

    @Override
    public boolean shouldEnterWater() {
        return !shouldLeaveWater() && swimTimer <= -1000 || bellowCooldown == 0;
    }

    @Override
    public boolean shouldLeaveWater() {
        LivingEntity target = this.getTarget();
        if (target != null && !target.isTouchingWater()) {
            return true;
        }
        return swimTimer > 600 && !this.isBellowing();
    }

    @Override
    public boolean shouldStopMoving() {
        return this.isSitting();
    }

    @Override
    public int getWaterSearchRange() {
        return 12;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld serverLevel, PassiveEntity ageableMob) {
        return AMEntityRegistry.CAIMAN.get().create(serverLevel);
    }

    public Vec3d getShakePreyPos() {
        var jaw = new Vec3d(0, -0.1, 1F);
        var head = jaw.rotateX(-this.getPitch() * MathHelper.RADIANS_PER_DEGREE).rotateY(-this.getHeadYaw() * MathHelper.RADIANS_PER_DEGREE);
        return this.getEyePos().add(head);
    }

    @Override
    public void addVelocity(double x, double y, double z) {
        if (this.getHeldMobId() == -1) {
            super.addVelocity(x, y, z);
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("HasEgg", this.hasEgg());
        compound.putBoolean("Bellowing", this.isBellowing());
        compound.putInt("CaimanCommand", this.getCommand());
        compound.putBoolean("CaimanSitting", this.isSitting());
        compound.putInt("BellowCooldown", this.bellowCooldown);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setHasEgg(compound.getBoolean("HasEgg"));
        this.setBellowing(compound.getBoolean("Bellowing"));
        this.bellowCooldown = compound.getInt("BellowCooldown");
        this.setCommand(compound.getInt("CaimanCommand"));
        this.setSitting(compound.getBoolean("CaimanSitting"));
    }

    @Override
    public boolean shouldFollow() {
        return this.getCommand() == 1;
    }

    @Override
    public EntityView method_48926() {
        return getWorld();
    }

    static class MateGoal extends AnimalMateGoal {
        private final EntityCaiman caiman;

        MateGoal(EntityCaiman caiman, double speedIn) {
            super(caiman, speedIn);
            this.caiman = caiman;
        }

        @Override
        public boolean canStart() {
            return super.canStart() && !this.caiman.hasEgg();
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
            this.caiman.setHasEgg(true);
            this.animal.resetLoveTicks();
            this.mate.resetLoveTicks();
            this.animal.setBreedingAge(6000);
            this.mate.setBreedingAge(6000);
            var random = this.animal.getRandom();
            if (this.world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
                this.world.spawnEntity(new ExperienceOrbEntity(this.world, this.animal.getX(), this.animal.getY(), this.animal.getZ(), random.nextInt(7) + 1));
            }
        }
    }

    static class LayEggGoal extends MoveToTargetPosGoal {
        private final EntityCaiman caiman;

        LayEggGoal(EntityCaiman caiman, double speedIn) {
            super(caiman, speedIn, 16);
            this.caiman = caiman;
        }

        @Override
        public void stop() {
        }

        @Override
        public boolean canStart() {
            return this.caiman.hasEgg() && super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            return super.shouldContinue() && this.caiman.hasEgg();
        }

        @Override
        public double getDesiredDistanceToTarget() {
            return caiman.getWidth() + 0.5D;
        }

        @Override
        public void tick() {
            super.tick();
            BlockPos blockpos = this.caiman.getBlockPos();
            caiman.swimTimer = 1000;
            if (!this.caiman.isTouchingWater() && this.hasReached()) {
                World world = this.caiman.getWorld();
                caiman.emitGameEvent(GameEvent.BLOCK_PLACE);
                world.playSound(null, blockpos, SoundEvents.ENTITY_TURTLE_LAY_EGG, SoundCategory.BLOCKS, 0.3F, 0.9F + world.random.nextFloat() * 0.2F);
                world.setBlockState(this.targetPos.up(), AMBlockRegistry.CAIMAN_EGG.get().getDefaultState().with(BlockReptileEgg.EGGS, this.caiman.random.nextInt(1) + 3), 3);
                this.caiman.setHasEgg(false);
                this.caiman.setLoveTicks(600);
            }

        }

        @Override
        protected boolean isTargetPos(WorldView worldIn, BlockPos pos) {
            return worldIn.isAir(pos.up()) && BlockReptileEgg.isProperHabitat(worldIn, pos);
        }
    }
}
