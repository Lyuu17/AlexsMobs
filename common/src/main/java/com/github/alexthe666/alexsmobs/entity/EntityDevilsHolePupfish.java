package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIRandomSwimming;
import com.github.alexthe666.alexsmobs.entity.ai.AquaticMoveController;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.github.alexthe666.alexsmobs.world.AMWorldData;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MoveIntoWaterGoal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.SwimNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class EntityDevilsHolePupfish extends WaterCreatureEntity implements Flutterer, Bucketable {

    public static final Identifier PUPFISH_REWARD = new Identifier("alexsmobs", "gameplay/pupfish_reward");
    private static final TrackedData<Boolean> FROM_BUCKET = DataTracker.registerData(EntityDevilsHolePupfish.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> PUPFISH_SCALE = DataTracker.registerData(EntityDevilsHolePupfish.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> FEEDING_TIME = DataTracker.registerData(EntityDevilsHolePupfish.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> BABY_AGE = DataTracker.registerData(EntityDevilsHolePupfish.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Optional<BlockPos>> FEEDING_POS = DataTracker.registerData(EntityDevilsHolePupfish.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);
    public float prevOnLandProgress;
    public float onLandProgress;
    public float prevFeedProgress;
    public float feedProgress;
    private EntityDevilsHolePupfish chasePartner;
    private int chaseTime = 0;
    private boolean chaseDriver;
    private boolean breedNextChase;
    private int chaseCooldown = 0;
    private int maxChaseTime = 300;

    public EntityDevilsHolePupfish(EntityType<EntityDevilsHolePupfish> type, World level) {
        super(type, level);
        this.moveControl = new AquaticMoveController(this, 1.0F, 15F);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new SwimNavigation(this, worldIn);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.DEVILS_HOLE_PUPFISH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.DEVILS_HOLE_PUPFISH_HURT.get();
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(1, new MoveIntoWaterGoal(this));
        this.goalSelector.add(2, new EatMossGoal(this));
        this.goalSelector.add(3, new ChaseGoal(this));
        this.goalSelector.add(4, new EscapeDangerGoal(this, 1D));
        this.goalSelector.add(5, new AnimalAIRandomSwimming(this, 1F, 12, 5));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.34F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.hasCustomName() || this.isFromBucket();
    }

    public static boolean canPupfishSpawn(EntityType<EntityDevilsHolePupfish> entityType, ServerWorldAccess iServerWorld, SpawnReason reason, BlockPos pos, Random random) {
        return reason == SpawnReason.SPAWNER || isPupfishChunk(iServerWorld, pos) && iServerWorld.getFluidState(pos).isIn(FluidTags.WATER) && isInCave(iServerWorld, pos);
    }

    private static boolean isPupfishChunk(ServerWorldAccess iServerWorld, BlockPos pos) {
        AMWorldData data = AMWorldData.get(iServerWorld.toServerWorld());
        return data != null && data.isInPupfishChunk(pos);
    }

    private static boolean isInCave(ServerWorldAccess iServerWorld, BlockPos pos) {
        while(iServerWorld.getFluidState(pos).isIn(FluidTags.WATER)){
            pos = pos.up();
        }
        return !iServerWorld.isSkyVisible(pos) && pos.getY() < iServerWorld.getSeaLevel();
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.devilsHolePupfishSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    public int getLimitPerChunk() {
        return 6;
    }

    @Override
    public boolean spawnsTooManyForEachTry(int sizeIn) {
        return false;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FROM_BUCKET, false);
        this.dataTracker.startTracking(PUPFISH_SCALE, 1.0F);
        this.dataTracker.startTracking(FEEDING_TIME, 0);
        this.dataTracker.startTracking(BABY_AGE, 0);
        this.dataTracker.startTracking(FEEDING_POS, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        this.prevOnLandProgress = onLandProgress;
        this.prevFeedProgress = feedProgress;
        if(chaseCooldown > 0){
            chaseCooldown--;
        }
        final boolean inWaterOrBubble = this.isInsideWaterOrBubbleColumn();
        if (!inWaterOrBubble && onLandProgress < 5F) {
            onLandProgress++;
        }
        if (inWaterOrBubble && onLandProgress > 0F) {
            onLandProgress--;
        }
        final int feedingTime = this.getFeedingTime();
        if (feedingTime > 0 && feedProgress < 5F) {
            feedProgress++;
        }
        if (feedingTime <= 0 && feedProgress > 0F) {
            feedProgress--;
        }
        if(this.isBaby()){
            this.setBabyAge(this.getBabyAge() + 1);
        }
        BlockPos feedingPos = this.dataTracker.get(FEEDING_POS).orElse(null);
        if(feedingPos == null){
            float f2 = (float) -((float) this.getVelocity().y * 2.2F * (double) MathHelper.DEGREES_PER_RADIAN);
            this.setPitch(f2);
        }else if(this.getFeedingTime() > 0){
            var face = Vec3d.ofCenter(feedingPos).subtract(this.getPos());
            double d0 = face.horizontalLength();
            this.setPitch((float)(-MathHelper.atan2(face.y, d0) * (double)MathHelper.DEGREES_PER_RADIAN));
            this.setYaw(((float) MathHelper.atan2(face.z, face.x)) * MathHelper.DEGREES_PER_RADIAN - 90F);
            this.bodyYaw = this.getYaw();
            this.headYaw= this.getYaw();
            BlockState state = getWorld().getBlockState(feedingPos);
            if(random.nextInt(2) == 0 && !state.isAir()){
                var mouth = new Vec3d(0, this.getHeight() * 0.5F, 0.4F * this.getPupfishScale()).rotateX(this.getPitch() * MathHelper.RADIANS_PER_DEGREE).rotateY(-this.getYaw() * MathHelper.RADIANS_PER_DEGREE);
                for (int i = 0; i < 4 + random.nextInt(2); i++) {
                    double motX = this.random.nextGaussian() * 0.02D;
                    double motY = 0.1F + random.nextFloat() * 0.2F;
                    double motZ = this.random.nextGaussian() * 0.02D;
                    getWorld().addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), this.getX() + mouth.x, this.getY() + mouth.y, this.getZ() + mouth.z, motX, motY, motZ);
                }
            }
        }
        if(!isInsideWaterOrBubbleColumn() && this.isAlive()){
            if (this.isOnGround() && random.nextFloat() < 0.5F) {
                this.setVelocity(this.getVelocity().add((this.random.nextFloat() * 2.0F - 1.0F) * 0.2F, 0.5D, (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F));
                this.setYaw(this.random.nextFloat() * 360.0F);
                this.playSound(SoundEvents.ENTITY_COD_FLOP, this.getSoundVolume(), this.getSoundPitch());
            }
        }
    }

    @Override
    public EntityDimensions getDimensions(EntityPose poseIn) {
        return super.getDimensions(poseIn).scaled(this.getPupfishScale());
    }

    @Override
    public boolean isFromBucket() {
        return this.dataTracker.get(FROM_BUCKET);
    }

    @Override
    public void setFromBucket(boolean bucketed) {
        this.dataTracker.set(FROM_BUCKET, bucketed);
    }

    @Override
    public void copyDataToStack(@NotNull ItemStack bucket) {
        if (this.hasCustomName()) {
            bucket.setCustomName(this.getCustomName());
        }
        Bucketable.copyDataToStack(this, bucket);
        NbtCompound compound = bucket.getOrCreateNbt();
        compound.putFloat("BucketScale", this.getPupfishScale());
        compound.putFloat("BabyAge", this.getBabyAge());
    }

    @Override
    public void copyDataFromNbt(@NotNull NbtCompound compound) {
        Bucketable.copyDataFromNbt(this, compound);
        if (compound.contains("BucketScale")){
            this.setPupfishScale(compound.getFloat("BucketScale"));
        }
        if (compound.contains("BabyAge")){
            this.setBabyAge(compound.getInt("BabyAge"));
        }
    }

    @Override
    @NotNull
    public ItemStack getBucketItem() {
        ItemStack stack = new ItemStack(AMItemRegistry.DEVILS_HOLE_PUPFISH_BUCKET.get());
        if (this.hasCustomName()) {
            stack.setCustomName(this.getCustomName());
        }
        return stack;
    }

    @Override
    public SoundEvent getBucketFillSound() {
        return SoundEvents.ITEM_BUCKET_FILL_FISH;
    }

    public float getPupfishScale() {
        return this.dataTracker.get(PUPFISH_SCALE);
    }

    public void setPupfishScale(float scale) {
        this.dataTracker.set(PUPFISH_SCALE, scale);
    }

    public int getFeedingTime() {
        return this.dataTracker.get(FEEDING_TIME);
    }

    public void setFeedingTime(int feedingTime) {
        this.dataTracker.set(FEEDING_TIME, feedingTime);
    }

    public int getBabyAge() {
        return this.dataTracker.get(BABY_AGE);
    }

    public void setBabyAge(int babyAge) {
        this.dataTracker.set(BABY_AGE, babyAge);
    }

    @Override
    public boolean isBaby(){
        return getBabyAge() < 0;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("FromBucket", this.isFromBucket());
        compound.putBoolean("BreedNextChase", this.breedNextChase);
        compound.putFloat("PupfishScale", this.getPupfishScale());
        compound.putInt("BabyAge", this.getBabyAge());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setFromBucket(compound.getBoolean("FromBucket"));
        this.breedNextChase = compound.getBoolean("BreedNextChase");
        this.setPupfishScale(compound.getFloat("PupfishScale"));
        this.setBabyAge(compound.getInt("BabyAge"));
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.setPupfishScale(0.65F + random.nextFloat() * 0.35F);
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    protected void tickWaterBreathingAir(int i) {
        if (this.isAlive() && !this.isInsideWaterOrBubbleColumn()) {
            this.setAir(i - 1);
            if (this.getAir() == -20) {
                this.setAir(0);
                this.damage(getDamageSources().dryOut(), 2.0F);
            }
        } else {
            this.setAir(getMaxAir());
        }
    }

    @Override
    public int getMaxAir() {
        return 600;
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.canMoveVoluntarily() && this.isTouchingWater()) {
            this.updateVelocity(this.getMovementSpeed(), travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            float f = 0.6F;
            this.setVelocity(this.getVelocity().multiply(0.9D, f, 0.9D));
            if (this.getTarget() == null) {
                this.setVelocity(this.getVelocity().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    protected void playSwimSound(float f) {
        if(random.nextInt(2) == 0){
            this.playSound(this.getSwimSound(), 0.2F, 1.3F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
        }
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.ENTITY_FISH_SWIM;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public boolean isInAir() {
        return false;
    }

    private boolean canSeeBlock(BlockPos destinationBlock) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        var blockVec = Vec3d.ofCenter(destinationBlock);
        var result = this.getWorld().raycast(new RaycastContext(Vector3d, blockVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        return result.getBlockPos().equals(destinationBlock);
    }

    private static List<ItemStack> getFoodLoot(EntityDevilsHolePupfish pupfish) {
        var loottable = pupfish.getWorld().getServer().getLootManager().getLootTable(PUPFISH_REWARD);
        return loottable.generateLoot((new LootContextParameterSet.Builder((ServerWorld) pupfish.getWorld()))
                .add(LootContextParameters.THIS_ENTITY, pupfish)
                .build(LootContextTypes.BARTER));
    }

    @Override
    public boolean canImmediatelyDespawn(double dist) {
        return !this.isFromBucket() && !this.hasCustomName() && !this.isBaby();
    }

    private class ChaseGoal extends Goal {
        private final EntityDevilsHolePupfish pupfish;
        private final Predicate<Entity> validChasePartner;
        private int executionCooldown = 50;

        public ChaseGoal(EntityDevilsHolePupfish pupfish) {
            this.setControls(EnumSet.of(Control.MOVE));
            this.pupfish = pupfish;
            this.validChasePartner = (pupfish1 -> pupfish1 instanceof EntityDevilsHolePupfish otherFish && otherFish.getId() != this.pupfish.getId() && otherFish.chasePartner == null && otherFish.chaseCooldown <= 0);
        }

        @Override
        public boolean canStart() {
            if(!pupfish.isInsideWaterOrBubbleColumn() || pupfish.chaseTime > pupfish.maxChaseTime || pupfish.chaseCooldown > 0){
                return false;
            }
            if(pupfish.chasePartner != null && pupfish.chasePartner.isAlive()){
                return true;
            }
            if(executionCooldown > 0){
                executionCooldown--;
            }else{
                executionCooldown = 50 + random.nextInt(50);
                if(pupfish.chasePartner == null || !pupfish.chasePartner.isAlive()){
                    List<EntityDevilsHolePupfish> list = pupfish.getWorld().getEntitiesByClass(EntityDevilsHolePupfish.class, pupfish.getBoundingBox().expand(10, 8, 10), EntityPredicates.EXCEPT_SPECTATOR.and(validChasePartner));
                    list.sort(Comparator.comparingDouble(pupfish::squaredDistanceTo));
                    if(!list.isEmpty()){
                        EntityDevilsHolePupfish closestPupfish = list.get(0);
                        if(closestPupfish != null){
                            pupfish.chasePartner = closestPupfish;
                            closestPupfish.chasePartner = pupfish;
                            pupfish.chaseDriver = true;
                            return true;
                        }
                    }
                    return false;
                }
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return pupfish.chasePartner != null && pupfish.chasePartner.isAlive() && pupfish.chaseTime < pupfish.maxChaseTime;
        }

        @Override
        public void start() {
            pupfish.chaseDriver = !pupfish.chasePartner.chaseDriver;
            pupfish.chaseTime = 0;
            pupfish.maxChaseTime = 600;
        }

        @Override
        public void stop() {
            pupfish.chaseTime = 0;
            pupfish.chaseCooldown = 100 + random.nextInt(100);
            executionCooldown = 50 + random.nextInt(20);
            if(pupfish.breedNextChase){
                pupfish.spawnBabiesWith(pupfish.chasePartner);
                pupfish.chasePartner.breedNextChase = false;
                pupfish.breedNextChase = false;
            }
            pupfish.chasePartner = null;
        }

        @Override
        public void tick() {
            pupfish.chaseTime++;
            if(pupfish.chasePartner == null || !pupfish.chaseDriver){
                return;
            }
            float chaserSpeed = 1.2F + random.nextFloat() * 0.45F;
            float chasedSpeed = 0.2F + chaserSpeed * 0.7F;
            EntityDevilsHolePupfish flee = pupfish.chaseDriver ? pupfish.chasePartner : pupfish;
            EntityDevilsHolePupfish driver = pupfish.chaseDriver ? pupfish : pupfish.chasePartner;
            driver.getNavigation().startMovingTo(flee.getX(), flee.getBodyY(0.5F), flee.getZ(), chaserSpeed);
            var from = flee.getPos().add(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F).subtract(driver.getPos()).normalize().multiply(2F + random.nextFloat() * 2F);
            var to = flee.getPos().add(from);
            flee.getNavigation().startMovingTo(to.x, to.y, to.z, chasedSpeed);
            if(random.nextInt(50) == 0){
                pupfish.chaseDriver = !pupfish.chaseDriver;
                pupfish.chasePartner.chaseDriver = !pupfish.chasePartner.chaseDriver;
            }
        }
    }

    private void spawnBabiesWith(EntityDevilsHolePupfish chasePartner) {
        EntityDevilsHolePupfish baby = AMEntityRegistry.DEVILS_HOLE_PUPFISH.get().create(getWorld());
        baby.copyPositionAndRotation(this);
        baby.setPupfishScale(0.65F + random.nextFloat() * 0.35F);
        baby.setBabyAge(-24000);
        getWorld().spawnEntity(baby);
    }

    private class EatMossGoal extends Goal {
        private final int searchLength;
        private final int verticalSearchRange;
        protected BlockPos destinationBlock;
        private final EntityDevilsHolePupfish pupfish;
        private int runDelay = 70;
        private int maxFeedTime = 200;

        private EatMossGoal(EntityDevilsHolePupfish pupfish) {
            this.setControls(EnumSet.of(Control.MOVE));
            this.pupfish = pupfish;
            searchLength = 16;
            verticalSearchRange = 6;
        }

        @Override
        public boolean shouldContinue() {
            return destinationBlock != null && isMossBlock(pupfish.getWorld(), destinationBlock.mutableCopy()) && isCloseToMoss(16);
        }

        public boolean isCloseToMoss(double dist) {
            return destinationBlock == null || pupfish.squaredDistanceTo(Vec3d.ofCenter(destinationBlock)) < dist * dist;
        }

        @Override
        public boolean canStart() {
            if (!pupfish.isInsideWaterOrBubbleColumn()) {
                return false;
            }
            if (this.runDelay > 0) {
                --this.runDelay;
                return false;
            } else {
                this.runDelay = 200 + pupfish.random.nextInt(150);
                return this.searchForDestination();
            }
        }

        @Override
        public void start(){
            maxFeedTime = 60 + random.nextInt(60);
        }

        @Override
        public void tick() {
            var vec = Vec3d.ofCenter(destinationBlock);
            if (vec != null) {
                pupfish.getNavigation().startMovingTo(vec.x, vec.y, vec.z, 1F);
                if(pupfish.squaredDistanceTo(vec) < 1.15F){
                    pupfish.dataTracker.set(FEEDING_POS, Optional.of(destinationBlock));
                    var face = vec.subtract(pupfish.getPos());
                    pupfish.setVelocity(pupfish.getVelocity().add(face.normalize().multiply(0.1F)));
                    pupfish.setFeedingTime(pupfish.getFeedingTime() + 1);
                    if(pupfish.getFeedingTime() > maxFeedTime){
                        destinationBlock = null;
                        if(random.nextInt(3) == 0){
                            List<ItemStack> lootList = getFoodLoot(pupfish);
                            if (!lootList.isEmpty()) {
                                for (var stack : lootList) {
                                    ItemEntity e = pupfish.dropStack(stack.copy());
                                    e.velocityDirty = true;
                                    e.setVelocity(e.getVelocity().multiply(0.2, 0.2, 0.2));
                                }
                            }
                        }
                        if(random.nextInt(3) == 0 && !pupfish.isBaby()){
                            pupfish.breedNextChase = true;
                        }
                    }
                }else{
                    pupfish.dataTracker.set(FEEDING_POS, Optional.empty());
                }
            }
        }

        @Override
        public void stop() {
            pupfish.dataTracker.set(FEEDING_POS, Optional.empty());
            destinationBlock = null;
            pupfish.setFeedingTime(0);
        }

        protected boolean searchForDestination() {
            int lvt_1_1_ = this.searchLength;
            //int lvt_2_1_ = this.verticalSearchRange;
            BlockPos lvt_3_1_ = pupfish.getBlockPos();
            BlockPos.Mutable lvt_4_1_ = new BlockPos.Mutable();

            for (int lvt_5_1_ = -8; lvt_5_1_ <= 2; lvt_5_1_++) {
                for (int lvt_6_1_ = 0; lvt_6_1_ < lvt_1_1_; ++lvt_6_1_) {
                    for (int lvt_7_1_ = 0; lvt_7_1_ <= lvt_6_1_; lvt_7_1_ = lvt_7_1_ > 0 ? -lvt_7_1_ : 1 - lvt_7_1_) {
                        for (int lvt_8_1_ = lvt_7_1_ < lvt_6_1_ && lvt_7_1_ > -lvt_6_1_ ? lvt_6_1_ : 0; lvt_8_1_ <= lvt_6_1_; lvt_8_1_ = lvt_8_1_ > 0 ? -lvt_8_1_ : 1 - lvt_8_1_) {
                            lvt_4_1_.set(lvt_3_1_, lvt_7_1_, lvt_5_1_ - 1, lvt_8_1_);
                            if (this.isMossBlock(pupfish.getWorld(), lvt_4_1_) && pupfish.canSeeBlock(lvt_4_1_)) {
                                this.destinationBlock = lvt_4_1_;
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }

        private boolean isMossBlock(World world, BlockPos.Mutable pos) {
            return world.getBlockState(pos).isIn(AMTagRegistry.PUPFISH_EATABLES);
        }

    }

    @Override
    @NotNull
    protected ActionResult interactMob(@NotNull PlayerEntity player, @NotNull Hand hand) {
        return Bucketable.tryBucket(player, hand, this).orElse(super.interactMob(player, hand));
    }

}
