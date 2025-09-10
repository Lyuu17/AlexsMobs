package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.misc.IngredientUtil;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.google.common.base.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.AboveGroundTargeting;
import net.minecraft.entity.ai.NoPenaltySolidTargeting;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class EntityFly extends AnimalEntity implements Flutterer {

    private int conversionTime = 0;
    private static final TrackedData<Boolean> NO_DESPAWN = DataTracker.registerData(EntityFly.class, TrackedDataHandlerRegistry.BOOLEAN);

    public EntityFly(EntityType<? extends AnimalEntity> type, World worldIn) {
        super(type, worldIn);
        this.moveControl = new FlightMoveControl(this, 20, true);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, -1.0F);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 16.0F);
        this.setPathfindingPenalty(PathNodeType.COCOA, -1.0F);
        this.setPathfindingPenalty(PathNodeType.FENCE, -1.0F);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {}

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("NoFlyDespawn", this.isNoDespawn());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setNoDespawn(compound.getBoolean("NoFlyDespawn"));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(NO_DESPAWN, false);
    }

    public boolean isNoDespawn() {
        return this.dataTracker.get(NO_DESPAWN);
    }

    public void setNoDespawn(boolean despawn) {
        this.dataTracker.set(NO_DESPAWN, despawn);
    }

    public static boolean canFlySpawn(EntityType<EntityFly> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return reason == SpawnReason.SPAWNER || pos.getY() > 63 && random.nextInt(4) == 0 && worldIn.getBaseLightLevel(pos, 0) > 8 && worldIn.getLightLevel(LightType.BLOCK, pos) == 0 && worldIn.getBlockState(pos.down()).isIn(AMTagRegistry.FLY_SPAWNS);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceToClosestPlayer) {
        return !cannotDespawn();
    }

    @Override
    public boolean cannotDespawn() {
        return this.isNoDespawn()  || this.hasCustomName() || this.isLeashed() || super.cannotDespawn();
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.flySpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public boolean isInNether() {
        return this.getWorld().getRegistryKey() == World.NETHER && !this.isAiDisabled();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.FLY_IDLE.get();
    }

    @Override
    public int getMinAmbientSoundDelay() {
        return 30;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.FLY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.FLY_HURT.get();
    }

    @Override
    public int getLimitPerChunk() {
        return 2;
    }

    @Override
    public boolean spawnsTooManyForEachTry(int sizeIn) {
        return false;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 2.0D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.8F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    public float getPathfindingFavor(BlockPos pos, WorldView worldIn) {
        return worldIn.getBlockState(pos).isAir() ? 10.0F : 0.0F;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(2, new TemptGoal(this, 1.25D, IngredientUtil.ingredientFromTags(AMTagRegistry.FLY_BREEDABLES, AMTagRegistry.FLY_FOODSTUFFS), false));
        this.goalSelector.add(3, new FollowParentGoal(this, 1.25D));
        this.goalSelector.add(3, new FleeEntityGoal<>(this, SpiderEntity.class, 6.0F, 1.0D, 1.2D));
        this.goalSelector.add(4, new AnnoyZombieGoal());
        this.goalSelector.add(5, new WanderGoal());
        this.goalSelector.add(6, new SwimGoal(this));
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        var flyingpathnavigator = new BirdNavigation(this, worldIn) {
            @Override
            public boolean isValidPosition(BlockPos pos) {
                return !this.world.getBlockState(pos.down()).isAir();
            }
        };
        flyingpathnavigator.setCanPathThroughDoors(false);
        flyingpathnavigator.setCanSwim(false);
        flyingpathnavigator.setCanEnterOpenDoors(true);
        return flyingpathnavigator;
    }

    @Override
    protected float getActiveEyeHeight(EntityPose poseIn, EntityDimensions sizeIn) {
        return this.isBaby() ? sizeIn.height * 0.5F : sizeIn.height * 0.5F;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
        fallDistance = 0;
    }

    @Override
    public void tick(){
        super.tick();

        if(this.isBaby() && this.getStandingEyeHeight() > this.getHeight()){
            this.calculateDimensions();
        }
        if(this.isInLove() && !this.isNoDespawn()){
            this.setNoDespawn(true);
        }
        if(isInNether()){
            this.setNoDespawn(true);
            conversionTime++;
            if(conversionTime > 300){
                var mosquito = AMEntityRegistry.CRIMSON_MOSQUITO.get().create(getWorld());
                mosquito.copyPositionAndRotation(this);
                if(!this.getWorld().isClient){
                    mosquito.initialize((ServerWorldAccess) getWorld(), getWorld().getLocalDifficulty(this.getBlockPos()), SpawnReason.CONVERSION, null, null);
                }
                getWorld().spawnEntity(mosquito);
                mosquito.onSpawnFromFly();
                this.remove(RemovalReason.DISCARDED);
            }
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity p_230254_1_, Hand p_230254_2_) {
        ItemStack lvt_3_1_ = p_230254_1_.getStackInHand(p_230254_2_);
        if(lvt_3_1_.isIn(AMTagRegistry.FLY_FOODSTUFFS)){
            if(!p_230254_1_.isCreative()){
                lvt_3_1_.decrement(1);
            }
            this.setNoDespawn(true);
            this.heal(2);
            return ActionResult.SUCCESS;
        }
        return super.interactMob(p_230254_1_, p_230254_2_);
    }

    protected boolean makeFlySound() {
        return true;
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.ARTHROPOD;
    }

    @Override
    protected void swimUpward(TagKey<Fluid> fluidTag) {
        this.setVelocity(this.getVelocity().add(0.0D, 0.01D, 0.0D));
    }

    @Override
    public Vec3d getLeashOffset() {
        return new Vec3d(0.0D, 0.5F * this.getStandingEyeHeight(), this.getWidth() * 0.2F);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.FLY_BREEDABLES);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld level, PassiveEntity parent) {
        var fly = AMEntityRegistry.FLY.get().create(getWorld());
        fly.setNoDespawn(true);
        return fly;
    }

    @Override
    public boolean isInAir() {
        return true;
    }

    class WanderGoal extends Goal {
        WanderGoal() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        /**
         * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
         * method as well.
         */
        @Override
        public boolean canStart() {
            return EntityFly.this.navigation.isIdle() && EntityFly.this.random.nextInt(3) == 0;
        }

        /**
         * Returns whether an in-progress EntityAIBase should continue executing
         */
        @Override
        public boolean shouldContinue() {
            return EntityFly.this.navigation.isFollowingPath();
        }

        /**
         * Execute a one shot task or start executing a continuous task
         */
        @Override
        public void start() {
            Vec3d vector3d = this.getRandomLocation();
            if (vector3d != null) {
                EntityFly.this.navigation.startMovingAlong(EntityFly.this.navigation.findPathTo(AMBlockPos.fromVec3(vector3d), 1), 1.0D);
            }

        }

        @Nullable
        private Vec3d getRandomLocation() {
            var vec3 = EntityFly.this.getRotationVec(0.0F);
            int i = 8;
            var vec32 = AboveGroundTargeting.find(EntityFly.this, 8, 7, vec3.x, vec3.z, MathHelper.HALF_PI, 3, 1);
            return vec32 != null ? vec32 : NoPenaltySolidTargeting.find(EntityFly.this, 8, 4, -2, vec3.x, vec3.z, (double)MathHelper.HALF_PI);
        }
    }

    private class AnnoyZombieGoal extends Goal {
        protected final Sorter theNearestAttackableTargetSorter;
        protected final Predicate<? super Entity> targetEntitySelector;
        protected int executionChance = 8;
        protected boolean mustUpdate;
        private Entity targetEntity;
        private int cooldown = 0;

        AnnoyZombieGoal() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
            this.theNearestAttackableTargetSorter = new Sorter(EntityFly.this);
            this.targetEntitySelector = (Predicate<Entity>) e -> e.isAlive() && e.getType().isIn(AMTagRegistry.FLY_TARGETS) && (!(e instanceof LivingEntity) || ((LivingEntity) e).getHealth() >= 2D);
        }

        @Override
        public boolean canStart() {
            if (EntityFly.this.hasVehicle() || EntityFly.this.hasPassengers()) {
                return false;
            }
            if (!this.mustUpdate) {
                long worldTime = EntityFly.this.getWorld().getTime() % 10;
                if (EntityFly.this.getDespawnCounter() >= 100 && worldTime != 0) {
                    return false;
                }
                if (EntityFly.this.getRandom().nextInt(this.executionChance) != 0 && worldTime != 0) {
                    return false;
                }
            }
            List<Entity> list = EntityFly.this.getWorld().getEntitiesByClass(Entity.class, this.getTargetableArea(this.getTargetDistance()), this.targetEntitySelector);
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
            return targetEntity != null;
        }

        @Override
        public void stop() {
            this.targetEntity = null;
        }

        @Override
        public void tick() {
            if(cooldown > 0){
                cooldown--;
            }
            if(targetEntity != null){
                if (EntityFly.this.getNavigation().isIdle()) {
                    int i = EntityFly.this.getRandom().nextInt(3) - 1;
                    int k = EntityFly.this.getRandom().nextInt(3) - 1;
                    int l = (int) ((EntityFly.this.getRandom().nextInt(3) - 1) * Math.ceil(targetEntity.getHeight()));
                    EntityFly.this.getNavigation().startMovingTo(this.targetEntity.getX() + i, this.targetEntity.getY() + l, this.targetEntity.getZ() + k, 1);
                }
                if(EntityFly.this.squaredDistanceTo(targetEntity) < 3.0F){
                    if(targetEntity instanceof LivingEntity && ((LivingEntity) targetEntity).getHealth() > 2D){
                        if(cooldown == 0){
                            targetEntity.damage(getDamageSources().generic(), 1);
                            cooldown = 100;
                        }
                    }else{
                        this.stop();
                    }

                }
            }
        }

        protected double getTargetDistance() {
            return 16D;
        }

        protected Box getTargetableArea(double targetDistance) {
            var renderCenter = new Vec3d(EntityFly.this.getX() + 0.5, EntityFly.this.getY() + 0.5, EntityFly.this.getZ() + 0.5D);
            double renderRadius = 5;
            var aabb = new Box(-renderRadius, -renderRadius, -renderRadius, renderRadius, renderRadius, renderRadius);
            return aabb.offset(renderCenter);
        }

        public record Sorter(Entity theEntity) implements Comparator<Entity> {
            public int compare(Entity p_compare_1_, Entity p_compare_2_) {
                final double d0 = this.theEntity.squaredDistanceTo(p_compare_1_);
                final double d1 = this.theEntity.squaredDistanceTo(p_compare_2_);
                return Double.compare(d0, d1);
            }
        }
    }
}
