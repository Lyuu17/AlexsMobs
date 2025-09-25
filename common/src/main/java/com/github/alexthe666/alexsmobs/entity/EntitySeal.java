package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.misc.IngredientUtil;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class EntitySeal extends AnimalEntity implements ISemiAquatic, IHerdPanic, ITargetsDroppedItems {

    private static final TrackedData<Float> SWIM_ANGLE = DataTracker.registerData(EntitySeal.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> BASKING = DataTracker.registerData(EntitySeal.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> DIGGING = DataTracker.registerData(EntitySeal.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> ARCTIC = DataTracker.registerData(EntitySeal.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(EntitySeal.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> BOB_TICKS = DataTracker.registerData(EntitySeal.class, TrackedDataHandlerRegistry.INTEGER);
    public float prevSwimAngle;
    public float prevBaskProgress;
    public float baskProgress;
    public float prevDigProgress;
    public float digProgress;
    public float prevBobbingProgress;
    public float bobbingProgress;
    public int revengeCooldown = 0;
    public UUID feederUUID = null;
    private int baskingTimer = 0;
    private int swimTimer = -1000;
    private int ticksSinceInWater = 0;
    private boolean isLandNavigator;
    public int fishFeedings = 0;

    public EntitySeal(EntityType<EntitySeal> type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 0.0F);
        switchNavigator(false);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.SEAL_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.SEAL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.SEAL_HURT.get();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.18F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    public static boolean canSealSpawn(EntityType<? extends AnimalEntity> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        var holder = worldIn.getBiome(pos);
        if (!holder.matchesKey(BiomeKeys.FROZEN_OCEAN) && !holder.matchesKey(BiomeKeys.DEEP_FROZEN_OCEAN)) {
            boolean spawnBlock = worldIn.getBlockState(pos.down()).isIn(AMTagRegistry.SEAL_SPAWNS);
            return spawnBlock && worldIn.getBaseLightLevel(pos, 0) > 8;
        } else {
            return worldIn.getBaseLightLevel(pos, 0) > 8 && worldIn.getBlockState(pos.down()).isOf(Blocks.ICE);
        }
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SealAIBask(this));
        this.goalSelector.add(1, new BreatheAirGoal(this));
        this.goalSelector.add(2, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(3, new AnimalAIFindWater(this));
        this.goalSelector.add(3, new AnimalAILeaveWater(this));
        this.goalSelector.add(4, new AnimalAIHerdPanic(this, 1.6D));
        this.goalSelector.add(5, new MeleeAttackGoal(this, 1, true));
        this.goalSelector.add(6, new SealAIDiveForItems(this));
        this.goalSelector.add(7, new SwimAroundGoal(this, 1.0D, 7));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(9, new FleeEntityGoal<>(this, EntityOrca.class, 20F, 1.3D, 1.0D));
        this.goalSelector.add(10, new TemptGoal(this, 1.1D, IngredientUtil.ingredientFromTags(AMTagRegistry.SEAL_BREEDABLES, AMTagRegistry.SEAL_OFFERINGS), false));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, EntityFlyingFish.class, 55, true, true, null));
        this.targetSelector.add(2, new CreatureAITargetItems<>(this, false));
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new GroundPathNavigatorWide(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new AquaticMoveController(this, 1.5F);
            this.navigation = new SemiAquaticPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        final boolean prev = super.damage(source, amount);
        if (prev) {
            final double range = 15;
            final int fleeTime = 100 + getRandom().nextInt(150);
            this.revengeCooldown = fleeTime;
            List<? extends EntitySeal> list = this.getWorld().getNonSpectatingEntities(this.getClass(), this.getBoundingBox().expand(range, range / 2, range));
            for (EntitySeal gaz : list) {
                gaz.revengeCooldown = fleeTime;
                gaz.setBasking(false);
            }
            this.setBasking(false);
        }
        return prev;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SWIM_ANGLE, 0F);
        this.dataTracker.startTracking(BASKING, false);
        this.dataTracker.startTracking(DIGGING, false);
        this.dataTracker.startTracking(ARCTIC, false);
        this.dataTracker.startTracking(VARIANT, 0);
        this.dataTracker.startTracking(BOB_TICKS, 0);
    }

    public boolean isTearsEasterEgg() {
        String s = Formatting.strip(this.getName().getString());
        return s != null && s.toLowerCase().contains("he was");
    }

    @Override
    public void updateLimbs(boolean flying) {
        float f1 = (float) MathHelper.magnitude(this.getX() - this.prevX, 0, this.getZ() - this.prevZ);
        float f2 = Math.min(f1 * (isTouchingWater() ? 4.0F : 48.0F), 1.0F);
        this.limbAnimator.updateLimbs(f2, 0.4F);
    }

    public float getSwimAngle() {
        return this.dataTracker.get(SWIM_ANGLE);
    }

    public void setSwimAngle(float progress) {
        this.dataTracker.set(SWIM_ANGLE, progress);
    }

    @Override
    public void tick() {
        super.tick();
        prevBaskProgress = baskProgress;
        prevDigProgress = digProgress;
        prevBobbingProgress = bobbingProgress;
        prevSwimAngle = this.getSwimAngle();
        boolean dig = isDigging() && isInsideWaterOrBubbleColumn();
        float f2 = (float) -((float) this.getVelocity().y * (double) MathHelper.DEGREES_PER_RADIAN);
        if (isTouchingWater()) {
            this.setPitch(f2 * 2.5F);

            if (this.isLandNavigator)
                switchNavigator(false);
        } else {
            if (!this.isLandNavigator)
                switchNavigator(true);
        }

        if (isBasking()) {
            if (baskProgress < 5F)
                baskProgress++;
        } else {
            if (baskProgress > 0F)
                baskProgress--;
        }

        if (dig) {
            if (digProgress < 5F)
                digProgress++;
        } else {
            if (digProgress > 0F)
                digProgress--;
        }

        if (dig && getWorld().getBlockState(this.getVelocityAffectingPos()).isOpaque()) {
            BlockPos posit = this.getVelocityAffectingPos();
            BlockState understate = getWorld().getBlockState(posit);
            for (int i = 0; i < 4 + random.nextInt(2); i++) {
                double particleX = posit.getX() + random.nextFloat();
                double particleY = posit.getY() + 1F;
                double particleZ = posit.getZ() + random.nextFloat();
                double motX = this.random.nextGaussian() * 0.02D;
                double motY = 0.1F + random.nextFloat() * 0.2F;
                double motZ = this.random.nextGaussian() * 0.02D;
                getWorld().addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, understate), particleX, particleY, particleZ, motX, motY, motZ);
            }
        }
        if (!this.getWorld().isClient) {
            if (isBasking()) {
                if (this.getAttacker() != null || isInLove() || revengeCooldown > 0 || this.isInsideWaterOrBubbleColumn() || this.getTarget() != null || baskingTimer > 1000 && this.getRandom().nextInt(100) == 0) {
                    this.setBasking(false);
                }
            } else {
                if (this.getTarget() == null && !isInLove() && this.getAttacker() == null && revengeCooldown == 0 && !isBasking() && baskingTimer == 0 && this.getRandom().nextInt(15) == 0) {
                    if (!isInsideWaterOrBubbleColumn()) {
                        this.setBasking(true);
                    }
                }
            }
            if (revengeCooldown > 0) {
                revengeCooldown--;
            }
            if (revengeCooldown == 0 && this.getAttacker() != null) {
                this.setAttacker(null);
            }
            float threshold = 0.05F;
            if (isTouchingWater() && this.prevYaw - this.getYaw() > threshold) {
                this.setSwimAngle(this.getSwimAngle() + 2);
            } else if (isTouchingWater() && this.prevYaw - this.getYaw() < -threshold) {
                this.setSwimAngle(this.getSwimAngle() - 2);
            } else if (this.getSwimAngle() > 0) {
                this.setSwimAngle(Math.max(this.getSwimAngle() - 10, 0));
            } else if (this.getSwimAngle() < 0) {
                this.setSwimAngle(Math.min(this.getSwimAngle() + 10, 0));
            }
            this.setSwimAngle(MathHelper.clamp(this.getSwimAngle(), -70, 70));
            if (isBasking()) {
                baskingTimer++;
            } else {
                baskingTimer = 0;
            }
            if (isTouchingWater()) {
                swimTimer++;
                ticksSinceInWater = 0;
            } else {
                ticksSinceInWater++;
                swimTimer--;
            }
        }
        int bob = this.dataTracker.get(BOB_TICKS);
        if(bob > 0){
            bob--;
            if(this.bobbingProgress < 5F){
                this.bobbingProgress++;
            }
            this.dataTracker.set(BOB_TICKS, bob);
        }else{
            if(this.bobbingProgress > 0F){
                this.bobbingProgress--;
            }
            if(!this.getWorld().isClient && random.nextInt(300) == 0 && !this.isTouchingWater() && this.revengeCooldown == 0){
                bob = 20 + random.nextInt(20);
                this.dataTracker.set(BOB_TICKS, bob);
            }
        }
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, variant);
    }

    public boolean isBasking() {
        return this.dataTracker.get(BASKING);
    }

    public void setBasking(boolean basking) {
        this.dataTracker.set(BASKING, basking);
    }

    public boolean isDigging() {
        return this.dataTracker.get(DIGGING);
    }

    public void setDigging(boolean digging) {
        this.dataTracker.set(DIGGING, digging);
    }

    public boolean isArctic() {
        return this.dataTracker.get(ARCTIC);
    }

    public void setArctic(boolean arctic) {
        this.dataTracker.set(ARCTIC, arctic);
    }

    public int getMaxAir() {
        return 4800;
    }

    protected int getNextAirOnLand(int currentAir) {
        return this.getMaxAir();
    }

    @Override
    public int getMaxLookPitchChange() {
        return 1;
    }

    @Override
    public int getMaxHeadRotation() {
        return 1;
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason
            reason, @Nullable EntityData data, @Nullable NbtCompound dataTag) {
        this.setArctic(this.isBiomeArctic(worldIn, this.getBlockPos()));
        int i;
        if (data instanceof SealGroupData) {
            i = ((SealGroupData)data).variant;
        } else {
            i = this.random.nextInt(2);
            data = new SealGroupData(i);
        }
        this.setVariant(i);
        this.setAir(this.getMaxAir());
        this.setPitch(0.0F);
        return super.initialize(worldIn, difficultyIn, reason, data, dataTag);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Arctic", this.isArctic());
        compound.putBoolean("Basking", this.isBasking());
        compound.putInt("BaskingTimer", this.baskingTimer);
        compound.putInt("SwimTimer", this.swimTimer);
        compound.putInt("FishFeedings", this.fishFeedings);
        compound.putInt("Variant", this.getVariant());
        if(feederUUID != null){
            compound.putUuid("FeederUUID", feederUUID);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setArctic(compound.getBoolean("Arctic"));
        this.setBasking(compound.getBoolean("Basking"));
        this.baskingTimer = compound.getInt("BaskingTimer");
        this.swimTimer = compound.getInt("SwimTimer");
        this.fishFeedings = compound.getInt("FishFeedings");
        if(compound.containsUuid("FeederUUID")){
            this.feederUUID = compound.getUuid("FeederUUID");
        }
        this.setVariant(compound.getInt("Variant"));
    }

    private boolean isBiomeArctic(WorldAccess worldIn, BlockPos position) {
        return worldIn.getBiome(position).isIn(AMTagRegistry.SPAWNS_WHITE_SEALS);
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.canMoveVoluntarily() && this.isTouchingWater()) {
            this.updateVelocity(this.getMovementSpeed(), travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.9D));
            if (this.getTarget() == null) {
                this.setVelocity(this.getVelocity().add(0.0D, -0.005D, 0.0D));
            }
            if (this.isDigging()) {
                this.setVelocity(this.getVelocity().add(0.0D, -0.02D, 0.0D));

            }
        } else {
            super.travel(travelVector);
        }

    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.SEAL_BREEDABLES);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        EntitySeal seal = AMEntityRegistry.SEAL.get().create(world);
        seal.setArctic(this.isBiomeArctic(world, this.getBlockPos()));
        return seal;
    }

    @Override
    public boolean shouldEnterWater() {
        return !shouldLeaveWater() && swimTimer <= -1000;
    }

    @Override
    public boolean shouldLeaveWater() {
        if (!this.getPassengerList().isEmpty()) {
            return false;
        }
        if (this.getTarget() != null && !this.getTarget().isTouchingWater()) {
            return true;
        }
        return swimTimer > 600;
    }

    @Override
    public boolean shouldStopMoving() {
        return isBasking();
    }

    @Override
    public int getWaterSearchRange() {
        return 32;
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.SEAL_OFFERINGS) || stack.isIn(AMTagRegistry.SEAL_BREEDABLES);
    }

    @Override
    public void onGetItem(ItemEntity e) {
        if (e.getStack().isIn(AMTagRegistry.SEAL_OFFERINGS)) {
            fishFeedings++;
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_CAT_EAT, this.getSoundVolume(), this.getSoundPitch());
            var itemThrower = e.getOwner();
            if (fishFeedings >= 3) {
                if(itemThrower != null){
                    feederUUID = itemThrower.getUuid();
                }
                fishFeedings = 0;
            }
        } else {
            feederUUID = null;
        }
        this.heal(10);
    }

    @Override
    public void onPanic() {
    }

    @Override
    public boolean canPanic() {
        return !isBasking();
    }

    public static class SealGroupData extends PassiveEntity.PassiveData {

        public final int variant;

        SealGroupData(int variant) {
            super(true);
            this.variant = variant;
        }

    }
}
