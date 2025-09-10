package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.CosmicCodAIFollowLeader;
import com.github.alexthe666.alexsmobs.entity.ai.FlightMoveController;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class EntityCosmicCod extends MobEntity implements Bucketable {

    private static final TrackedData<Float> FISH_PITCH = DataTracker.registerData(EntityCosmicCod.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> FROM_BUCKET = DataTracker.registerData(EntityCosmicCod.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float prevFishPitch;
    private int baitballCooldown = 100 + random.nextInt(100);
    private int circleTime = 0;
    private int maxCircleTime = 300;
    private BlockPos circlePos;
    private int teleportIn;
    private EntityCosmicCod groupLeader;
    private int groupSize = 1;

    public EntityCosmicCod(EntityType<EntityCosmicCod> mob, World level) {
        super(mob, level);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
        this.moveControl = new FlightMoveController(this, 1F, false, true);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.cosmicCodSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.COSMIC_COD_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.COSMIC_COD_HURT.get();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 4.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new AISwimIdle(this));
        this.goalSelector.add(1, new CosmicCodAIFollowLeader(this));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FISH_PITCH, 0F);
        this.dataTracker.startTracking(FROM_BUCKET, false);
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
    @NotNull
    public ItemStack getBucketItem() {
        ItemStack stack = new ItemStack(AMItemRegistry.COSMIC_COD_BUCKET.get());
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
        compound.put("CosmicCodData", platTag);
    }

    @Override
    public void copyDataFromNbt(@NotNull NbtCompound compound) {
        if (compound.contains("CosmicCodData")) {
            this.readCustomDataFromNbt(compound.getCompound("CosmicCodData"));
        }
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.hasCustomName() || this.isFromBucket();
    }

    @Override
    public boolean canImmediatelyDespawn(double p_213397_1_) {
        return !this.isFromBucket() && !this.hasCustomName();
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("FromBucket", this.isFromBucket());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setFromBucket(compound.getBoolean("FromBucket"));
    }

    @Override
    public boolean hurtByWater() {
        return true;
    }

    private void doInitialPosing(WorldAccess world, EntityCosmicCod.GroupData data) {
        BlockPos down = this.getBlockPos();
        while(world.isAir(down) && down.getY() > -62){
            down = down.down();
        }
        if(down.getY() <= -60){
            if(data != null && data.groupLeader != null){
                this.setPos(down.getX() + 0.5F, data.groupLeader.getY() - 1 + random.nextInt(1), down.getZ() + 0.5F);
            }else{
                this.setPos(down.getX() + 0.5F, down.getY() + 90 + random.nextInt(60), down.getZ() + 0.5F);
            }
        }else{
            this.setPos(down.getX() + 0.5F, down.getY() + 1, down.getZ() + 0.5F);
        }
    }

    @Override
    protected Entity.MoveEffect getMoveEffect() {
        return Entity.MoveEffect.EVENTS;
    }

    @Override
    public void tick() {
        super.tick();
        this.prevFishPitch = this.getFishPitch();
        if (!this.getWorld().isClient) {
            final double ydist = (this.prevY - this.getY());//down 0.4 up -0.38
            final float fishDist = (float) ((Math.abs(this.getVelocity().x) + Math.abs(this.getVelocity().z)) * 6F) / getPitchSensitivity();
            this.incrementFishPitch((float) (ydist) * 10 * getPitchSensitivity());
            this.setFishPitch(MathHelper.clamp(this.getFishPitch(), -60, 40));
            if (this.getFishPitch() > 2F) {
                this.decrementFishPitch(fishDist * Math.abs(this.getFishPitch()) / 90);
            }
            if (this.getFishPitch() < -2F) {
                this.incrementFishPitch(fishDist * Math.abs(this.getFishPitch()) / 90);
            }
            if (this.getFishPitch() > 2F) {
                this.decrementFishPitch(1);
            } else if (this.getFishPitch() < -2F) {
                this.incrementFishPitch(1);
            }
            if (baitballCooldown > 0) {
                baitballCooldown--;
            }
        }
        if(teleportIn > 0){
            teleportIn--;
            if(teleportIn == 0 && !this.getWorld().isClient){
                final double range = 8;
                final var bb = new Box(this.getX() - range, this.getY() - range, this.getZ() - range, this.getX() + range, this.getY() + range, this.getZ() + range);
                final var list = this.getWorld().getNonSpectatingEntities(EntityCosmicCod.class, bb);
                final var vec3 = this.teleport();
                if (vec3 != null) {
                    baitballCooldown = 5;
                    for (final EntityCosmicCod cod : list) {
                        if (cod != this) {
                            cod.baitballCooldown = 5;
                            cod.teleport(vec3.x, vec3.y, vec3.z);
                        }
                    }
                }
            }
        }
    }

    public void handleStatus(byte msg) {
        if (msg == 46) {
            this.emitGameEvent(GameEvent.TELEPORT);
            this.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
        }
        super.handleStatus(msg);
    }

    public void resetBaitballCooldown(){
        baitballCooldown = 120 + random.nextInt(100);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean prev = super.damage(source, amount);
        if(prev){
            teleportIn = 5;

        }
        return prev;
    }

    private float getPitchSensitivity() {
        return 3F;
    }

    public boolean hasNoGravity() {
        return true;
    }

    @Override
    public boolean canBreatheInWater() {
        return true;
    }

    // FIXME should be isPushedByFluids?
    public boolean isPushedByWater() {
        return false;
    }

    public float getFishPitch() {
        return this.dataTracker.get(FISH_PITCH);
    }

    public void setFishPitch(float pitch) {
        this.dataTracker.set(FISH_PITCH, pitch);
    }

    public void incrementFishPitch(float pitch) {
        this.dataTracker.set(FISH_PITCH, getFishPitch() + pitch);
    }

    public void decrementFishPitch(float pitch) {
        this.dataTracker.set(FISH_PITCH, getFishPitch() - pitch);
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    public boolean canBlockPosBeSeen(BlockPos pos) {
        final double x = pos.getX() + 0.5F;
        final double y = pos.getY() + 0.5F;
        final double z = pos.getZ() + 0.5F;
        final var result = this.getWorld().raycast(new RaycastContext(this.getEyePos(), new Vec3d(x, y, z), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        final double dist = result.getPos().squaredDistanceTo(x, y, z);
        return dist <= 1.0D || result.getType() == HitResult.Type.MISS;
    }

    protected Vec3d teleport() {
        if (!this.getWorld().isClient() && this.isAlive()) {
            final double d0 = this.getX() + (this.random.nextDouble() - 0.5D) * 64.0D;
            final double d1 = this.getY() + (double) (this.random.nextInt(64) - 32);
            final double d2 = this.getZ() + (this.random.nextDouble() - 0.5D) * 64.0D;
            // FIXME forge
//            if (this.teleport(d0, d1, d2)) {
//                this.circlePos = null;
//                return new Vec3d(d0, d1, d2);
//            }
        }
        return null;
    }

//    public boolean teleport(double x, double y, double z) {
//        final var blockpos$mutableblockpos = new BlockPos.Mutable(x, y, z);
//        final var blockstate = this.getWorld().getBlockState(blockpos$mutableblockpos);
//        final boolean flag = blockstate.isAir();
//        if (flag && !blockstate.getFluidState().isIn(FluidTags.WATER)) {
//            this.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
//            //FIXME forge
////            net.minecraftforge.event.entity.EntityTeleportEvent.EnderEntity event = net.minecraftforge.event.ForgeEventFactory.onEnderTeleport(this, x, y, z);
////            if (event.isCanceled()) return false;
////            getWorld().sendEntityStatus(this, (byte) 46);
////            this.requestTeleport(event.getTargetX(), event.getTargetY(), event.getTargetZ());
//            return true;
//        } else {
//            return false;
//        }
//    }

    public void leaveGroup() {
        if(this.groupLeader != null){
            this.groupLeader.decreaseGroupSize();
        }
        this.groupLeader = null;
    }

    protected boolean hasNoLeader() {
        return !this.hasGroupLeader();
    }

    public boolean hasGroupLeader() {
        return this.groupLeader != null && this.groupLeader.isAlive();
    }

    private void increaseGroupSize() {
        ++this.groupSize;
    }

    private void decreaseGroupSize() {
        --this.groupSize;
    }

    public boolean canGroupGrow() {
        return this.isGroupLeader() && this.groupSize < this.getMaxGroupSize();
    }

    private int getMaxGroupSize() {
        return 15;
    }

    public int getLimitPerChunk() {
        return 7;
    }

    public boolean spawnsTooManyForEachTry(int sizeIn) {
        return false;
    }

    public boolean isGroupLeader() {
        return this.groupSize > 1;
    }

    public boolean inRangeOfGroupLeader() {
        return this.squaredDistanceTo(this.groupLeader) <= 121.0D;
    }

    public void moveToGroupLeader() {
        if (this.hasGroupLeader()) {
            this.getMoveControl().moveTo(this.groupLeader.getX(), this.groupLeader.getY(), this.groupLeader.getZ(), 1.0D);
        }
    }

    public EntityCosmicCod createAndSetLeader(EntityCosmicCod leader) {
        this.groupLeader = leader;
        leader.increaseGroupSize();
        return leader;
    }

    public void createFromStream(Stream<EntityCosmicCod> stream) {
        stream.limit(this.getMaxGroupSize() - this.groupSize).filter((fishe) -> fishe != this).forEach((fishe) -> {
            fishe.createAndSetLeader(this);
        });
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        if (spawnDataIn == null) {
            spawnDataIn = new EntityCosmicCod.GroupData(this);
        } else {
            this.createAndSetLeader(((EntityCosmicCod.GroupData) spawnDataIn).groupLeader);
        }
        if (reason == SpawnReason.NATURAL && spawnDataIn instanceof EntityCosmicCod.GroupData) {
            doInitialPosing(worldIn, (GroupData) spawnDataIn);
        }
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    public boolean isCircling() {
        return circlePos != null && circleTime < maxCircleTime;
    }

    @Override
    @NotNull
    protected ActionResult interactMob(@NotNull PlayerEntity player, @NotNull Hand hand) {
        final ItemStack itemstack = player.getStackInHand(hand);
        if (itemstack.getItem() == Items.BUCKET && this.isAlive()) {
            this.emitGameEvent(GameEvent.ENTITY_INTERACT);
            this.playSound(this.getBucketFillSound(), 1.0F, 1.0F);
            final var itemstack1 = this.getBucketItem();
            this.copyDataToStack(itemstack1);
            final var itemstack2 = ItemUsage.exchangeStack(itemstack, player, itemstack1, false);
            player.setStackInHand(hand, itemstack2);
            final World level = this.getWorld();
            if (!this.getWorld().isClient) {
                Criteria.FILLED_BUCKET.trigger((ServerPlayerEntity) player, itemstack1);
            }

            this.discard();
            return ActionResult.success(this.getWorld().isClient);
        }
        return super.interactMob(player, hand);
    }

    public static class GroupData extends PassiveEntity.PassiveData {
        public final EntityCosmicCod groupLeader;

        public GroupData(EntityCosmicCod groupLeaderIn) {
            super(0.05F);
            this.groupLeader = groupLeaderIn;
        }
    }

    private static class AISwimIdle extends Goal {

        private final EntityCosmicCod cod;
        float circleDistance = 5;
        boolean clockwise = false;

        public AISwimIdle(EntityCosmicCod cod) {
            this.cod = cod;
        }

        @Override
        public boolean canStart() {
            return this.cod.isGroupLeader() || cod.hasNoLeader() || cod.hasGroupLeader() && cod.groupLeader.circlePos != null;
        }

        @Override
        public void tick() {
            if(cod.circleTime > cod.maxCircleTime){
                cod.circleTime = 0;
                cod.circlePos = null;
            }
            if(cod.circlePos != null && cod.circleTime <= cod.maxCircleTime){
                cod.circleTime++;
                var movePos = getSharkCirclePos(cod.circlePos);
                cod.getMoveControl().moveTo(movePos.x, movePos.y, movePos.z, 1.0F);
            }else if (this.cod.isGroupLeader()) {
                if (cod.baitballCooldown == 0) {
                    cod.resetBaitballCooldown();
                    if (cod.circlePos == null || cod.circleTime >= cod.maxCircleTime) {
                        cod.circleTime = 0;
                        cod.maxCircleTime = 360 + this.cod.random.nextInt(80);
                        circleDistance = 1 + this.cod.random.nextFloat();
                        clockwise = this.cod.random.nextBoolean();
                        cod.circlePos = cod.getBlockPos().up();
                    }
                }
            } else if (cod.random.nextInt(40) == 0 || cod.hasNoLeader()) {
                final var movepos = cod.getPos().add(cod.random.nextInt(4) - 2, cod.getY() < 0 ? 1 : cod.random.nextInt(4) - 2, cod.random.nextInt(4) - 2);
                cod.getMoveControl().moveTo(movepos.x, movepos.y, movepos.z, 1.0F);
            } else if (cod.hasGroupLeader() && cod.groupLeader.circlePos != null) {
                if (cod.circlePos == null) {
                    cod.circlePos = cod.groupLeader.circlePos;
                    cod.circleTime = cod.groupLeader.circleTime;
                    cod.maxCircleTime = cod.groupLeader.maxCircleTime;
                    circleDistance = 1 + this.cod.random.nextFloat();
                    clockwise = this.cod.random.nextBoolean();
                }
            }
        }

        public Vec3d getSharkCirclePos(BlockPos target) {
            final float prog = 1F - (cod.circleTime / (float) cod.maxCircleTime);
            final float angle = (Maths.STARTING_ANGLE * 10 * (clockwise ? -cod.circleTime : cod.circleTime));
            final float circleDistanceTimesProg = circleDistance * prog;
            final double extraX = (circleDistanceTimesProg + 0.75F) * MathHelper.sin((angle));
            final double extraZ =  (circleDistanceTimesProg + 0.75F) * prog * MathHelper.cos(angle);
            return new Vec3d(target.getX() + 0.5F + extraX, Math.max(target.getY() + cod.random.nextInt(4) - 2, -62), target.getZ() + 0.5F + extraZ);
        }
    }
}
