package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.block.HummingbirdFeederBlock;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.FlightMoveController;
import com.github.alexthe666.alexsmobs.entity.ai.HummingbirdAIPollinate;
import com.github.alexthe666.alexsmobs.entity.ai.HummingbirdAIWander;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMPointOfInterestRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.google.common.base.Predicates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.*;
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
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EntityHummingbird extends AnimalEntity {

    private static final TrackedData<Boolean> FLYING = DataTracker.registerData(EntityHummingbird.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(EntityHummingbird.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CROPS_POLLINATED = DataTracker.registerData(EntityHummingbird.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Optional<BlockPos>> FEEDER_POS = DataTracker.registerData(EntityHummingbird.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);
    public float flyProgress;
    public float prevFlyProgress;
    public float movingProgress;
    public float prevMovingProgress;
    public int hummingStill = 0;
    public int pollinateCooldown = 0;
    public int sipCooldown = 0;
    private int loopSoundTick = 0;
    private boolean sippy;
    public float sipProgress;
    public float prevSipProgress;

    public EntityHummingbird(EntityType<? extends EntityHummingbird> type, World worldIn) {
        super(type, worldIn);
        this.moveControl = new FlightMoveController(this, 1.5F);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, -1.0F);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 16.0F);
        this.setPathfindingPenalty(PathNodeType.COCOA, -1.0F);
        this.setPathfindingPenalty(PathNodeType.FENCE, -1.0F);
        this.setPathfindingPenalty(PathNodeType.LEAVES, 0.0F);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.hummingbirdSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.HUMMINGBIRD_IDLE.get();
    }

    @Override
    public int getMinAmbientSoundDelay() {
        return 60;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.HUMMINGBIRD_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.HUMMINGBIRD_HURT.get();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 4.0D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 7F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.45F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.HUMMINGBIRD_BREEDABLES);
    }

    @Override
    public int getLimitPerChunk() {
        return 7;
    }

    @Override
    public boolean spawnsTooManyForEachTry(int sizeIn) {
        return false;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new AnimalMateGoal(this, 1));
        this.goalSelector.add(2, new TemptGoal(this, 1, Ingredient.fromTag(AMTagRegistry.HUMMINGBIRD_BREEDABLES), false));
        this.goalSelector.add(3, new FollowParentGoal(this, 1));
        this.goalSelector.add(4, new AIUseFeeder(this));
        this.goalSelector.add(4, new HummingbirdAIPollinate(this));
        this.goalSelector.add(5, new HummingbirdAIWander(this, 16, 6, 15, 1));
        this.goalSelector.add(6, new SwimGoal(this));
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {}

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        var flyingpathnavigator = new BirdNavigation(this, worldIn) {
            @Override
            public boolean isValidPosition(BlockPos pos) {
                return !this.world.getBlockState(pos.down(2)).isAir();
            }
        };
        flyingpathnavigator.setCanPathThroughDoors(false);
        flyingpathnavigator.setCanSwim(false);
        flyingpathnavigator.setCanEnterOpenDoors(true);
        return flyingpathnavigator;
    }

    @Override
    public boolean handleFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    protected boolean makeFlySound() {
        return true;
    }

    @Override
    protected float getActiveEyeHeight(EntityPose poseIn, EntityDimensions sizeIn) {
        return this.isBaby() ? sizeIn.height * 0.5F : sizeIn.height * 0.5F;
    }

    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView worldIn) {
        return worldIn.getBlockState(pos).isAir() ? 10.0F : 0.0F;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putInt("Variant", this.getVariant());
        compound.putInt("CropsPollinated", this.getCropsPollinated());
        compound.putInt("PollinateCooldown", this.pollinateCooldown);
        BlockPos blockpos = this.getFeederPos();
        if (blockpos != null) {
            compound.putInt("HLPX", blockpos.getX());
            compound.putInt("HLPY", blockpos.getY());
            compound.putInt("HLPZ", blockpos.getZ());
        }

    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setVariant(compound.getInt("Variant"));
        this.setCropsPollinated(compound.getInt("CropsPollinated"));
        this.pollinateCooldown = compound.getInt("PollinateCooldown");
        if (compound.contains("HLPX")) {
            int i = compound.getInt("HLPX");
            int j = compound.getInt("HLPY");
            int k = compound.getInt("HLPZ");
            this.dataTracker.set(FEEDER_POS, Optional.of(new BlockPos(i, j, k)));
        } else {
            this.dataTracker.set(FEEDER_POS, Optional.empty());
        }
    }

    public BlockPos getFeederPos() {
        return this.dataTracker.get(FEEDER_POS).orElse(null);
    }

    public void setFeederPos(BlockPos pos) {
        this.dataTracker.set(FEEDER_POS, Optional.ofNullable(pos));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FLYING, false);
        this.dataTracker.startTracking(VARIANT, 0);
        this.dataTracker.startTracking(CROPS_POLLINATED, 0);
        this.dataTracker.startTracking(FEEDER_POS, Optional.empty());
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.setVariant(this.getRandom().nextInt(3));
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    private List<BlockPos> getNearbyFeeders(BlockPos blockpos, ServerWorld world, int range) {
        var pointofinterestmanager = world.getPointOfInterestStorage();
        var stream = pointofinterestmanager.getPositions(poiTypeHolder -> poiTypeHolder.matchesKey(AMPointOfInterestRegistry.HUMMINGBIRD_FEEDER.getKey()), Predicates.alwaysTrue(), blockpos, range, PointOfInterestStorage.OccupationStatus.ANY);
        return stream.collect(Collectors.toList());
    }

    public boolean isFlying() {
        return this.dataTracker.get(FLYING);
    }

    public void setFlying(boolean flying) {
        this.dataTracker.set(FLYING, flying);
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, variant);
    }

    public int getCropsPollinated() {
        return this.dataTracker.get(CROPS_POLLINATED);
    }

    public void setCropsPollinated(int crops) {
        this.dataTracker.set(CROPS_POLLINATED, crops);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3d vector3d = this.getVelocity();
        boolean flag = this.getVelocity().x * this.getVelocity().x + this.getVelocity().z * this.getVelocity().z >= 1.0E-3D;
        if (!this.isOnGround() && vector3d.y < 0.0D) {
            this.setVelocity(vector3d.multiply(1.0D, 0.4D, 1.0D));
        }
        this.setFlying(true);
        this.setNoGravity(true);
        if (this.isFlying() && flyProgress < 5F) {
            flyProgress++;
        }
        if (!this.isFlying() && flyProgress > 0F) {
            flyProgress--;
        }
        if(sippy && sipProgress < 5F){
            sipProgress++;
        }
        if(!sippy && sipProgress > 0F){
            sipProgress--;
        }
        if(sippy && sipProgress == 5F){
            sippy = false;
        }
        if (flag && movingProgress < 5F) {
            movingProgress++;
        }
        if (!flag && movingProgress > 0F) {
            movingProgress--;
        }
        if(this.getVelocity().lengthSquared() < 1.0E-7D){
            hummingStill++;
        }else{
            hummingStill = 0;
        }
        if(pollinateCooldown > 0){
            pollinateCooldown--;
        }
        if(sipCooldown > 0){
            sipCooldown--;
        }
        if(loopSoundTick == 0){
            this.playSound(AMSoundRegistry.HUMMINGBIRD_LOOP.get(), this.getSoundVolume() * 0.33F, this.getSoundPitch());
        }
        loopSoundTick++;
        if(loopSoundTick > 27){
            loopSoundTick = 0;
        }
        prevFlyProgress = flyProgress;
        prevMovingProgress = movingProgress;
        prevSipProgress = sipProgress;
    }

    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if(id == 68){
            if(this.getFeederPos() != null){
                if(random.nextFloat() < 0.2F){
                    double d2 = this.random.nextGaussian() * 0.02D;
                    double d0 = this.random.nextGaussian() * 0.02D;
                    double d1 = this.random.nextGaussian() * 0.02D;
                    this.getWorld().addParticle(ParticleTypes.FALLING_NECTAR, this.getFeederPos().getX() + 0.2F + (double) (this.random.nextFloat() * 0.6F), this.getFeederPos().getY() + 0.1F, this.getFeederPos().getZ() + 0.2F + (double) (this.random.nextFloat() * 0.6F), d0, d1, d2);
                }
                this.sippy = true;
            }
        }else{
            super.handleStatus(id);
        }
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.HUMMINGBIRD.get().create(world);
    }

    public static <T extends MobEntity> boolean canHummingbirdSpawn(EntityType<EntityHummingbird> hummingbird, WorldAccess worldIn, SpawnReason reason, BlockPos p_223317_3_, Random random) {
        BlockState blockstate = worldIn.getBlockState(p_223317_3_.down());
        return (blockstate.isIn(AMTagRegistry.HUMMINGBIRD_SPAWNS) || blockstate.isOf(Blocks.AIR)) && worldIn.getBaseLightLevel(p_223317_3_, 0) > 8;
    }

    public boolean canBlockBeSeen(BlockPos pos) {
        double x = pos.getX() + 0.5F;
        double y = pos.getY() + 0.5F;
        double z = pos.getZ() + 0.5F;
        var result = this.getWorld().raycast(new RaycastContext(new Vec3d(this.getX(), this.getY() + (double) this.getStandingEyeHeight(), this.getZ()), new Vec3d(x, y, z), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        double dist = result.getPos().squaredDistanceTo(x, y, z);
        return dist <= 1.0D || result.getType() == HitResult.Type.MISS;
    }

    private class AIUseFeeder extends Goal {
        int runCooldown = 0;
        private int idleAtFlowerTime = 0;
        private BlockPos localFeeder;

        public AIUseFeeder(EntityHummingbird entityHummingbird) {
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP));
        }

        @Override
        public void stop(){
            localFeeder = null;
            idleAtFlowerTime = 0;
        }

        @Override
        public boolean canStart() {
            if(EntityHummingbird.this.sipCooldown > 0){
                return false;
            }
           if(runCooldown > 0){
               runCooldown--;
           }else{
               BlockPos feedPos = getFeederPos();
               if(feedPos != null && isValidFeeder(getWorld().getBlockState(feedPos))){
                   localFeeder = feedPos;
                   return true;
               }else{
                   List<BlockPos> beacons = getNearbyFeeders(EntityHummingbird.this.getBlockPos(), (ServerWorld) getWorld(), 64);
                   BlockPos closest = null;
                   for (BlockPos pos : beacons) {
                       if (closest == null || EntityHummingbird.this.squaredDistanceTo(closest.getX(), closest.getY(), closest.getZ()) > EntityHummingbird.this.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ())) {
                           if (isValidFeeder(getWorld().getBlockState(pos))) {
                               closest = pos;
                           }
                       }
                   }
                   if (closest != null && isValidFeeder(getWorld().getBlockState(closest))) {
                       localFeeder = closest;
                       return true;
                   }
               }
           }
           runCooldown = 400 + random.nextInt(600);
           return false;
        }

        @Override
        public boolean shouldContinue(){
            return localFeeder != null && isValidFeeder(getWorld().getBlockState(localFeeder)) && EntityHummingbird.this.sipCooldown == 0;
        }

        @Override
        public void tick(){
            if(localFeeder != null && isValidFeeder(getWorld().getBlockState(localFeeder))){
                if(EntityHummingbird.this.getY() > localFeeder.getY() && !EntityHummingbird.this.isOnGround()){
                    EntityHummingbird.this.getMoveControl().moveTo(localFeeder.getX() + 0.5F, localFeeder.getY() + 0.1F, localFeeder.getZ() + 0.5F, 1F);
                }else{
                    EntityHummingbird.this.getMoveControl().moveTo(localFeeder.getX() + random.nextInt(4) - 2, EntityHummingbird.this.getY() + 1F, localFeeder.getZ() + random.nextInt(4) - 2, 1F);
                }
                Vec3d vec = Vec3d.ofCenter(localFeeder, 0.1F);
                double dist = MathHelper.sqrt((float)EntityHummingbird.this.squaredDistanceTo(vec));
                if(dist < 2.5F && EntityHummingbird.this.getY() > localFeeder.getY()){
                    EntityHummingbird.this.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, vec);
                    idleAtFlowerTime++;
                    EntityHummingbird.this.setFeederPos(localFeeder);
                    EntityHummingbird.this.getWorld().sendEntityStatus(EntityHummingbird.this, (byte)68);
                    if(idleAtFlowerTime > 55){
                        if(EntityHummingbird.this.getCropsPollinated() > 2 && random.nextInt(25) == 0 && isValidFeeder(getWorld().getBlockState(localFeeder))){
                            getWorld().setBlockState(localFeeder, getWorld().getBlockState(localFeeder).with(HummingbirdFeederBlock.CONTENTS, 0));
                        }
                        EntityHummingbird.this.setCropsPollinated(EntityHummingbird.this.getCropsPollinated() + 1);
                        EntityHummingbird.this.sipCooldown = 120 + random.nextInt(1200);
                        EntityHummingbird.this.pollinateCooldown = Math.max(0, EntityHummingbird.this.pollinateCooldown / 3);
                        runCooldown = 400 + random.nextInt(600);
                        stop();
                    }
                }
            }
        }

        public boolean isValidFeeder(BlockState state){
            return state.getBlock() instanceof HummingbirdFeederBlock && state.get(HummingbirdFeederBlock.CONTENTS) == 3;
        }
    }
}
