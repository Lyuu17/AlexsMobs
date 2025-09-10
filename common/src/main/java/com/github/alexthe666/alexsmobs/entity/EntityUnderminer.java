package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.EtherealMoveController;
import com.github.alexthe666.alexsmobs.entity.ai.MonsterAIWalkThroughHallsOfStructure;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.*;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class EntityUnderminer extends PathAwareEntity {

    protected static final TrackedData<Optional<BlockPos>> TARGETED_BLOCK_POS = DataTracker.registerData(EntityUnderminer.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);
    private static final TrackedData<Boolean> DWARF = DataTracker.registerData(EntityUnderminer.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> MINING_PROGRESS = DataTracker.registerData(EntityUnderminer.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(EntityUnderminer.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> HIDING = DataTracker.registerData(EntityUnderminer.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> VISUALLY_MINING = DataTracker.registerData(EntityUnderminer.class, TrackedDataHandlerRegistry.BOOLEAN);

    private int mineCooldown = 100;
    private int resetStackTime = 0;
    private ItemStack lastGivenStack = null;
    public float hidingProgress = 0;
    public float prevHidingProgress = 0;
    private boolean mineAIFlag = false;
    private BlockPos lastPosition = this.getBlockPos();

    public EntityUnderminer(EntityType<EntityUnderminer> type, World level) {
        super(type, level);
        this.moveControl = new EtherealMoveController(this, 1F);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64F);
    }

    @Override
    protected EntityNavigation createNavigation(World level) {
        return new PathNavigator(this, getWorld());
    }

    public static <T extends MobEntity> boolean checkUnderminerSpawnRules(EntityType<EntityUnderminer> entityType, ServerWorldAccess iServerWorld, SpawnReason reason, BlockPos pos, Random random) {
        if (reason == SpawnReason.SPAWNER) {
            return true;
        }else{
            int j = 3;
            if(pos.getY() >= iServerWorld.getSeaLevel()){
                return false;
            }else if (AlexsMobs.isHalloween()) {
                j = 7;
            } else if (random.nextBoolean()) {
                return false;
            }

            final int i = iServerWorld.getLightLevel(pos);
            return i > random.nextInt(j) ? false : canMobSpawn(entityType, iServerWorld, reason, pos, random);
        }
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceToClosestPlayer) {
        return !cannotDespawn() && !this.hasCustomName();
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.hasCustomName() || lastGivenStack != null;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.underminerSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(DWARF, true);
        this.dataTracker.startTracking(HIDING, false);
        this.dataTracker.startTracking(VISUALLY_MINING, false);
        this.dataTracker.startTracking(TARGETED_BLOCK_POS, Optional.empty());
        this.dataTracker.startTracking(MINING_PROGRESS, 0.0F);
        this.dataTracker.startTracking(VARIANT, 0);
    }

    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Dwarf", this.isDwarf());
        compound.putBoolean("Hiding", this.isHiding());
        compound.putInt("Variant", this.getVariant());
        compound.putInt("ResetItemTime", resetStackTime);
        compound.putInt("MineCooldown", mineCooldown);
        if(lastGivenStack != null){
            compound.put("MineStack", lastGivenStack.getNbt());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setDwarf(compound.getBoolean("Dwarf"));
        this.setHiding(compound.getBoolean("Hiding"));
        this.setVariant(compound.getInt("Variant"));
        this.resetStackTime = compound.getInt("ResetItemTime");
        this.mineCooldown = compound.getInt("MineCooldown");
        if(compound.contains("MineStack")){
            this.lastGivenStack = ItemStack.fromNbt(compound.getCompound("MineStack"));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.UNDERMINER_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.UNDERMINER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.UNDERMINER_HURT.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {
    }

    public boolean isDwarf() {
        return this.dataTracker.get(DWARF) && !isExtraSpooky();
    }

    public void setDwarf(boolean phasing) {
        this.dataTracker.set(DWARF, phasing);
    }

    public int getVariant() {
        return isExtraSpooky() ? 1 : this.dataTracker.get(VARIANT);
    }

    public void setVariant(int i) {
        this.dataTracker.set(VARIANT, i);
    }

    public boolean isHiding() {
        return this.dataTracker.get(HIDING);
    }

    public void setHiding(boolean phasing) {
        this.dataTracker.set(HIDING, phasing);
    }

    @Nullable
    public BlockPos getMiningPos() {
        return this.dataTracker.get(TARGETED_BLOCK_POS).orElse(null);
    }

    public void setMiningPos(@Nullable BlockPos beamTarget) {
        this.dataTracker.set(TARGETED_BLOCK_POS, Optional.ofNullable(beamTarget));
    }

    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.add(2, new MineGoal());
        this.goalSelector.add(3, new MonsterAIWalkThroughHallsOfStructure(this, 0.5D, 60, StructureTags.MINESHAFT, 50));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.targetSelector.add(1, (new RevengeGoal(this)).setGroupRevenge());
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.isOf(DamageTypes.MAGIC) && source.isOf(DamageTypes.OUT_OF_WORLD) && !source.isSourceCreativePlayer() || super.isInvulnerableTo(source);
    }

    private float calculateDistanceToFloor() {
        var floor = AMBlockPos.fromCoords(this.getX(), this.getBoundingBox().maxY, this.getZ());
        while (!getWorld().getBlockState(floor).isSideSolidFullSquare(getWorld(), floor, Direction.UP) && floor.getY() > getWorld().getBottomY()) {
            floor = floor.down();
        }
        return (float) (this.getBoundingBox().minY - (floor.getY() + 1));
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.canMoveVoluntarily()) {
            this.updateVelocity(this.getMovementSpeed(), travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.9D, 0.6D, 0.9D));
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    protected void initEquipment(Random p_218949_, LocalDifficulty p_218950_) {
        super.initEquipment(p_218949_, p_218950_);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(AMItemRegistry.GHOSTLY_PICKAXE.get()));
    }

    @Override
    protected float getDropChance(EquipmentSlot slot) {
        if(slot == EquipmentSlot.MAINHAND){
            return 0.5F;
        }
        return super.getDropChance(slot);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess level, LocalDifficulty LocalDifficulty, SpawnReason SpawnReason, @Nullable EntityData spawnData, @Nullable NbtCompound tag) {
        spawnData = super.initialize(level, LocalDifficulty, SpawnReason, spawnData, tag);
        var random = level.getRandom();
        this.initEquipment(random, LocalDifficulty);
        if(random.nextFloat() < 0.3F){
            this.setVariant(random.nextInt(2));
            this.setDwarf(false);
        }else{
            this.setDwarf(true);
        }
        return spawnData;
    }

    public boolean isFullyHidden(){
        return this.isHiding() && hidingProgress >= 10F;
    }

    @Override
    public void tick() {
        this.noClip = true;
        super.tick();
        this.prevHidingProgress = hidingProgress;
        this.noClip = false;
        if(this.isHiding() && hidingProgress < 10F){
            this.hidingProgress++;
        }
        if(!this.isHiding() && hidingProgress > 0F){
            hidingProgress--;
        }
        if (!this.getWorld().isClient) {
            final double xzSpeed = this.getVelocity().horizontalLength();
            final double distToFloor = MathHelper.clamp(calculateDistanceToFloor(), -1F, 1F);
            if (Math.abs(distToFloor) > 0.01 && xzSpeed < 0.05 && !this.isActuallyInAWall()) {
                if (distToFloor < 0.0) {
                    this.setVelocity(this.getVelocity().add(0, -Math.min(distToFloor * 0.1F, 0F), 0));
                } else if (distToFloor > 0.0) {
                    this.setVelocity(this.getVelocity().add(0, -Math.max(distToFloor * 0.1F, 0F), 0));
                }
            }
            if(lastPosition != null && lastPosition.getSquaredDistance(this.getBlockPos()) > 2.5F && Math.abs(distToFloor) < 0.5){
                this.playSound(AMSoundRegistry.UNDERMINER_STEP.get(), 1F, 0.75F + random.nextFloat() * 0.25F);
                lastPosition = this.getBlockPos();
                if(random.nextFloat() < 0.015F && !getWorld().isSkyVisible(lastPosition)){
                    this.playSound(SoundEvents.AMBIENT_CAVE.value(), 3F, 0.75F + random.nextFloat() * 0.25F);
                }
            }
            var player = this.getWorld().getClosestPlayer(this.getX(), this.getY(), this.getZ(), AMConfig.underminerDisappearDistance, true);
            if(player != null && lastGivenStack == null && (this.getTarget() == null || !this.getTarget().isAlive())){
                this.setHiding(true);
                this.lookAtEntity(player, 360F, 360F);
            }else{
                this.setHiding(false);
            }
        }
        this.setBodyYaw(this.getYaw());

        if(mineCooldown > 0){
            mineCooldown--;
        }
        if(resetStackTime > 0){
            resetStackTime--;
            if(resetStackTime == 0){
                lastGivenStack = null;
            }
        }
        if(this.dataTracker.get(VISUALLY_MINING)){
            this.swingHand(Hand.MAIN_HAND);
        }
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return true;
    }

    @Override
    public boolean canGather(ItemStack stack) {
        return stack.isIn(AMTagRegistry.UNDERMINER_ORES);
    }

    @Override
    protected void loot(ItemEntity itemEntity) {
        var itemstack = itemEntity.getStack();
        if (itemstack.isIn(AMTagRegistry.UNDERMINER_ORES)) {
            this.triggerItemPickedUpByEntityCriteria(itemEntity);
            this.sendPickup(itemEntity, itemstack.getCount());
            itemEntity.discard();
            this.mineAIFlag = this.lastGivenStack == null || !ItemStack.areItemsEqual(this.lastGivenStack, itemEntity.getStack());
            this.lastGivenStack = itemEntity.getStack();
            this.resetStackTime = 2000 + random.nextInt(1200);
            this.mineCooldown = 0;
        }else{
            super.loot(itemEntity);
        }

    }

    @Override
    protected void jump() {
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }

    public boolean isExtraSpooky(){
        var s = Formatting.strip(this.getName().getString());
        return AlexsMobs.isAprilFools() || AlexsMobs.isHalloween() || s != null && s.toLowerCase().contains("herobrine");
    }

    private boolean isActuallyInAWall() {
        final float f = this.getDimensions(this.getPose()).width * 0.1F;
        var aabb = Box.of(this.getEyePos(), f, 1.0E-6D, f);
        return BlockPos.stream(aabb).anyMatch((p_201942_) -> {
            BlockState blockstate = this.getWorld().getBlockState(p_201942_);
            return !blockstate.isAir() && blockstate.shouldSuffocate(this.getWorld(), p_201942_) && VoxelShapes.matchesAnywhere(blockstate.getCollisionShape(this.getWorld(), p_201942_).offset(p_201942_.getX(), p_201942_.getY(), p_201942_.getZ()), VoxelShapes.cuboid(aabb), BooleanBiFunction.AND);
        });
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    public float getBrightness() {
        return 1.0F;
    }

    public float getMiningProgress() {
        return this.dataTracker.get(MINING_PROGRESS);
    }

    public void setMiningProgress(float f) {
        this.dataTracker.set(MINING_PROGRESS, f);
    }

    private List<BlockPos> getNearbyObscuredOres(int range, int maxOres) {
        List<BlockPos> obscuredBlocks = new ArrayList<>();
        BlockPos blockpos = this.getBlockPos();
        final int half = range / 2;
        for (int i = 0; i <= half && i >= -half; i = (i <= 0 ? 1 : 0) - i) {
            for (int j = 0; j <= range && j >= -range; j = (j <= 0 ? 1 : 0) - j) {
                for (int k = 0; k <= range && k >= -range; k = (k <= 0 ? 1 : 0) - k) {
                    BlockPos offset = blockpos.add(j, i, k);
                    BlockState state = this.getWorld().getBlockState(offset);
                    //FIXME forge
//                    if (isValidMiningBlock(state)) {
//                        if (obscuredBlocks.size() < maxOres) {
//                            BlockPos obscured = getObscuringBlockOf(offset);
//                            if(obscured != null){
//                                obscuredBlocks.add(obscured);
//                            }
//                        } else {
//                            break;
//                        }
//                    }
                }
            }
        }
        return obscuredBlocks;
    }

    //FIXME forge
//    private boolean isValidMiningBlock(BlockState state) {
//        if(lastGivenStack != null){
//            return lastGivenStack.getItem() == state.getBlock().asItem();
//        }
//        return state.is(Tags.Blocks.ORES);
//    }

    @Override
    public void tickMovement() {
        this.tickHandSwing();
        super.tickMovement();
    }

    @Override
    public boolean isAttackable() {
        return !this.isFullyHidden() && super.isAttackable();
    }

    @Override
    public boolean handleAttack(Entity entity) {
        return this.isFullyHidden() || super.handleAttack(entity);
    }

    private BlockPos getObscuringBlockOf(BlockPos target) {
        var eyes = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        var hitResult = this.getWorld().raycast(new RaycastContext(eyes, Vec3d.ofCenter(target), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        if (hitResult instanceof BlockHitResult && !hitResult.getBlockPos().equals(target)) {
            BlockPos pos = hitResult.getBlockPos();
            return pos.getSquaredDistance(target) > 4 ? null : pos;
        }
        return null;
    }

    private boolean hasPick(){
        return this.getStackInHand(Hand.MAIN_HAND).isOf(AMItemRegistry.GHOSTLY_PICKAXE.get());
    }

    private class PathNavigator extends MobNavigation {

        public PathNavigator(EntityUnderminer underminer, World level) {
            super(underminer, getWorld());
        }

        @Override
        protected boolean isAtValidPosition() {
            return !this.entity.hasVehicle();
        }

        @Override
        protected Vec3d getPos() {
            return this.entity.getPos();
        }
    }

    private class MineGoal extends Goal {

        private BlockPos minePretendPos = null;
        private BlockState minePretendStartState = null;
        private int mineTime = 0;

        public MineGoal() {
            this.setControls(EnumSet.of(Goal.Control.LOOK, Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (EntityUnderminer.this.mineCooldown == 0 && EntityUnderminer.this.hasPick() && !EntityUnderminer.this.isHiding() && !EntityUnderminer.this.isActuallyInAWall() && EntityUnderminer.this.getRandom().nextInt(30) == 0) {
                List<BlockPos> obscuredOres = EntityUnderminer.this.getNearbyObscuredOres(16, 8);
                BlockPos nearest = null;
                double nearestDist = Double.MAX_VALUE;
                if (!obscuredOres.isEmpty()) {
                    for (BlockPos obscuredPos : obscuredOres) {
                        final double dist = EntityUnderminer.this.getPos().distanceTo(Vec3d.ofCenter(obscuredPos));
                        if (nearestDist > dist) {
                            nearest = obscuredPos;
                            nearestDist = dist;
                        }
                    }
                }
                EntityUnderminer.this.mineAIFlag = false;
                minePretendPos = nearest;
                return minePretendPos != null;
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return minePretendPos != null && EntityUnderminer.this.hasPick() && !EntityUnderminer.this.isHiding() && !EntityUnderminer.this.mineAIFlag && minePretendStartState != null && minePretendStartState.equals(getWorld().getBlockState(minePretendPos)) && mineTime < 200;
        }

        @Override
        public void start() {
            if (minePretendPos != null) {
                minePretendStartState = EntityUnderminer.this.getWorld().getBlockState(minePretendPos);
            }
        }

        @Override
        public void stop() {
            if(minePretendPos != null && minePretendStartState != null && !minePretendStartState.equals(getWorld().getBlockState(minePretendPos))){
                for(var serverplayerentity : EntityUnderminer.this.getWorld().getNonSpectatingEntities(ServerPlayerEntity.class, EntityUnderminer.this.getBoundingBox().expand(12.0D, 12.0D, 12.0D))) {
                    AMAdvancementTriggerRegistry.UNDERMINE_UNDERMINER.trigger(serverplayerentity);
                }
            }
            minePretendPos = null;
            minePretendStartState = null;
            mineTime = 0;
            EntityUnderminer.this.dataTracker.set(VISUALLY_MINING, false);
            EntityUnderminer.this.setMiningPos(null);
            EntityUnderminer.this.setMiningProgress(0.0F);
            if(EntityUnderminer.this.resetStackTime > 0){
                EntityUnderminer.this.mineCooldown = 40;
            }else{
                EntityUnderminer.this.mineCooldown = 200 + random.nextInt(200);
            }
        }

        @Override
        public void tick() {
            if (minePretendPos != null && minePretendStartState != null) {
                mineTime++;
                final double distSqr = EntityUnderminer.this.squaredDistanceTo(minePretendPos.getX() + 0.5F, minePretendPos.getY() + 0.5F, minePretendPos.getZ() + 0.5F);
                if (distSqr < 6.5F) {
                    EntityUnderminer.this.getNavigation().stop();
                    if(EntityUnderminer.this.getNavigation().isIdle()) {
                        EntityUnderminer.this.setMiningPos(minePretendPos);
                        EntityUnderminer.this.setMiningProgress((1F + (float) Math.cos(mineTime * 0.1F + Math.PI)) * 0.5F);
                        final double d1 = minePretendPos.getZ() + 0.5F - EntityUnderminer.this.getZ();
                        final double d3 = minePretendPos.getY() + 0.5F - EntityUnderminer.this.getY();
                        final double d2 = minePretendPos.getX() + 0.5F - EntityUnderminer.this.getX();
                        final float f = MathHelper.sqrt((float) (d2 * d2 + d1 * d1));
                        EntityUnderminer.this.setYaw(-((float) MathHelper.atan2(d2, d1)) * MathHelper.DEGREES_PER_RADIAN);
                        EntityUnderminer.this.setPitch((float) (MathHelper.atan2(d3, f) * (double) MathHelper.DEGREES_PER_RADIAN) + (float) Math.sin(EntityUnderminer.this.age * 0.1F));
                        EntityUnderminer.this.dataTracker.set(VISUALLY_MINING, true);
                        if (mineTime % 10 == 0) {
                            var soundType = minePretendStartState.getBlock().getSoundGroup(minePretendStartState);
                            EntityUnderminer.this.playSoundIfNotSilent(soundType.getHitSound());
                        }
                    }
                } else {
                    EntityUnderminer.this.dataTracker.set(VISUALLY_MINING, false);
                    EntityUnderminer.this.setMiningPos(null);
                    EntityUnderminer.this.getNavigation().startMovingTo(minePretendPos.getX() + 0.5F, minePretendPos.getY() + 0.5F, minePretendPos.getZ() + 0.5F, 1.0D);
                }
            }
        }

    }
}
