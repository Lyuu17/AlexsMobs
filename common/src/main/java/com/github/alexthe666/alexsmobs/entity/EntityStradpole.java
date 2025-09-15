package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AquaticMoveController;
import com.github.alexthe666.alexsmobs.entity.ai.BoneSerpentPathNavigator;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class EntityStradpole extends WaterCreatureEntity implements Bucketable {

    private static final TrackedData<Boolean> FROM_BUCKET = DataTracker.registerData(EntityStradpole.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> DESPAWN_SOON = DataTracker.registerData(EntityStradpole.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> LAUNCHED = DataTracker.registerData(EntityStradpole.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Optional<UUID>> PARENT_UUID = DataTracker.registerData(EntityStradpole.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    public float swimPitch = 0;
    public float prevSwimPitch = 0;
    private int despawnTimer = 0;
    private int ricochetCount = 0;

    public EntityStradpole(EntityType<EntityStradpole> type, World world) {
        super(type, world);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.LAVA, 0.0F);
        this.moveControl = new AquaticMoveController(this, 1.4F);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_COD_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.ENTITY_COD_HURT;
    }

    @Override
    public int getLimitPerChunk() {
        return 2;
    }

    @Override
    @NotNull
    public ItemStack getBucketItem() {
        ItemStack stack = new ItemStack(AMItemRegistry.STRADPOLE_BUCKET.get());
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
        Bucketable.copyDataToStack(this, bucket);
    }

    @Override
    public void copyDataFromNbt(@NotNull NbtCompound compound) {
        Bucketable.copyDataFromNbt(this, compound);
    }

    @Override
    @NotNull
    protected ActionResult interactMob(@NotNull PlayerEntity player, @NotNull Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        if(itemstack.isIn(AMTagRegistry.STRADPOLE_GROWABLES)){
            if(!player.isCreative()){
                itemstack.decrement(1);
            }
            if(random.nextFloat() < 0.45F){
                var straddler = AMEntityRegistry.STRADDLER.get().create(getWorld());
                straddler.copyPositionAndRotation(this);
                if(!this.getWorld().isClient && getWorld().spawnEntity(straddler)){
                    this.remove(RemovalReason.DISCARDED);

                }
            }
            return ActionResult.success(this.getWorld().isClient);
        }
        if (itemstack.getItem() == Items.LAVA_BUCKET && this.isAlive()) {
            this.emitGameEvent(GameEvent.ENTITY_INTERACT);
            this.playSound(this.getBucketFillSound(), 1.0F, 1.0F);
            ItemStack itemstack1 = this.getBucketItem();
            this.copyDataToStack(itemstack1);
            ItemStack itemstack2 = ItemUsage.exchangeStack(itemstack, player, itemstack1, false);
            player.setStackInHand(hand, itemstack2);
            World level = this.getWorld();
            if (!level.isClient) {
                Criteria.FILLED_BUCKET.trigger((ServerPlayerEntity) player, itemstack1);
            }

            this.discard();
            return ActionResult.success(level.isClient);
        }
        return super.interactMob(player, hand);
    }


    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 4.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(PARENT_UUID, Optional.empty());
        this.dataTracker.startTracking(DESPAWN_SOON, false);
        this.dataTracker.startTracking(LAUNCHED, false);
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

    @Nullable
    public UUID getParentId() {
        return this.dataTracker.get(PARENT_UUID).orElse(null);
    }

    public void setParentId(@Nullable UUID uniqueId) {
        this.dataTracker.set(PARENT_UUID, Optional.ofNullable(uniqueId));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        if (this.getParentId() != null) {
            compound.putUuid("ParentUUID", this.getParentId());
        }
        compound.putBoolean("FromBucket", this.isFromBucket());
        compound.putBoolean("DespawnSoon", this.isDespawnSoon());
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.isFromBucket();
    }

    @Override
    public boolean canImmediatelyDespawn(double p_27492_) {
        return !this.isFromBucket() && !this.hasCustomName();
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.stradpoleSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean canStradpoleSpawn(EntityType<EntityStradpole> p_234314_0_, WorldAccess p_234314_1_, SpawnReason p_234314_2_, BlockPos p_234314_3_, Random p_234314_4_) {
        if(p_234314_1_.getFluidState(p_234314_3_).isIn(FluidTags.LAVA)){
            if(!p_234314_1_.getFluidState(p_234314_3_.down()).isIn(FluidTags.LAVA)){

                return p_234314_1_.isAir(p_234314_3_.up());
            }
        }
        return false;
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        if (compound.containsUuid("ParentUUID")) {
            this.setParentId(compound.getUuid("ParentUUID"));
        }
        this.setFromBucket(compound.getBoolean("FromBucket"));
        this.setDespawnSoon(compound.getBoolean("DespawnSoon"));
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new StradpoleAISwim(this, 1.0D, 10));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
    }

    public float getPathfindingFavor(BlockPos pos, WorldView worldIn) {
        if (!worldIn.getBlockState(pos).getFluidState().isEmpty()) {
            return 15.0F;
        } else {
            return Float.NEGATIVE_INFINITY;
        }
    }

    public boolean isDespawnSoon() {
        return this.dataTracker.get(DESPAWN_SOON);
    }

    public void setDespawnSoon(boolean despawnSoon) {
        this.dataTracker.set(DESPAWN_SOON, despawnSoon);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new BoneSerpentPathNavigator(this, worldIn);
    }

    @Override
    public void tick() {
        float f = 1.0F;
        if (this.dataTracker.get(LAUNCHED)) {
            this.bodyYaw = this.getYaw();
            var raytraceresult = ProjectileUtil.getCollision(this, this::canHitEntity);
            if (raytraceresult != null && raytraceresult.getType() != HitResult.Type.MISS) {
                this.onImpact(raytraceresult);
            }
            f = 0.1F;
        }
        super.tick();
        final boolean liquid = this.isTouchingWater() || this.isInLava();
        prevSwimPitch = this.swimPitch;

        this.swimPitch = (float) -((float) this.getVelocity().y * (liquid ? 2.5F : f) * (double) MathHelper.DEGREES_PER_RADIAN);
        if (this.isOnGround() && !this.isTouchingWater() && !this.isInLava()) {
            this.setVelocity(this.getVelocity().add((this.random.nextFloat() * 2.0F - 1.0F) * 0.2F, 0.5D, (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F));
            this.setYaw( this.random.nextFloat() * 360.0F);
            this.setOnGround(false);
            this.velocityDirty = true;
        }
        this.setNoGravity(false);
        if (liquid) {
            this.setNoGravity(true);
        }
        if (isDespawnSoon()) {
            despawnTimer++;
            if (despawnTimer > 100) {
                despawnTimer = 0;
                this.playSpawnEffects();
                this.remove(RemovalReason.DISCARDED);
            }
        }
    }

    private void onImpact(HitResult raytraceresult) {
        HitResult.Type raytraceresult$type = raytraceresult.getType();
        if (raytraceresult$type == HitResult.Type.ENTITY) {
            this.onEntityHit((EntityHitResult) raytraceresult);
        } else if (raytraceresult$type == HitResult.Type.BLOCK) {
            var traceResult = (BlockHitResult) raytraceresult;
            BlockState blockstate = this.getWorld().getBlockState(traceResult.getBlockPos());
            if (!blockstate.getSidesShape(this.getWorld(), traceResult.getBlockPos()).isEmpty()) {
                var face = traceResult.getSide();
                var prevMotion = this.getVelocity();
                double motionX = prevMotion.x;
                double motionY = prevMotion.y;
                double motionZ = prevMotion.z;
                switch (face) {
                    case EAST, WEST -> motionX = -motionX;
                    case SOUTH, NORTH -> motionZ = -motionZ;
                    default -> motionY = -motionY;
                }
                this.setVelocity(motionX, motionY, motionZ);
                if (this.age > 200 || ricochetCount > 20) {
                   this.dataTracker.set(LAUNCHED, false);
                } else {
                    ricochetCount++;
                }
            }
        }
    }

    public Entity getParent() {
        UUID id = getParentId();
        if (id != null && !this.getWorld().isClient) {
            return ((ServerWorld) getWorld()).getEntity(id);
        }
        return null;
    }

    private void onEntityHit(EntityHitResult raytraceresult) {
        Entity entity = this.getParent();
        if (entity instanceof LivingEntity && !this.getWorld().isClient && raytraceresult.getEntity() instanceof LivingEntity target) {
            if(!target.isBlocking()){
                target.damage(getDamageSources().mobProjectile(this, (LivingEntity)entity), 3.0F);
                target.takeKnockback(0.7F, entity.getX() - this.getX(), entity.getZ() - this.getZ());
            }else{
                if (this.getTarget() instanceof PlayerEntity) {
                    this.damageShieldFor(((PlayerEntity) this.getTarget()), 3.0F);
                }
            }
            this.dataTracker.set(LAUNCHED, false);
        }
    }

    protected void damageShieldFor(PlayerEntity holder, float damage) {
        if (holder.getActiveItem().getUseAction().equals(UseAction.BLOCK)) {
            if (!this.getWorld().isClient) {
                holder.incrementStat(Stats.USED.getOrCreateStat(holder.getActiveItem().getItem()));
            }

            if (damage >= 3.0F) {
                int i = 1 + MathHelper.floor(damage);
                var hand = holder.getActiveHand();
                holder.getActiveItem().damage(i, holder, playerEntity -> {
                    playerEntity.sendToolBreakStatus(hand);
                    // FIXME forge
//                    net.minecraftforge.event.ForgeEventFactory.onafterBreakItem(holder, holder.getActiveItem(), hand);
                });
                if (holder.getActiveItem().isEmpty()) {
                    if (hand == Hand.MAIN_HAND) {
                        holder.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    } else {
                        holder.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                    }
                    holder.playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.8F, 0.8F + this.getWorld().random.nextFloat() * 0.4F);
                }
            }
        }
    }

    protected boolean canHitEntity(Entity p_230298_1_) {
        return !p_230298_1_.isSpectator() && !(p_230298_1_ instanceof EntityStraddler) && !(p_230298_1_ instanceof EntityStradpole);
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public boolean canWalkOnFluid(FluidState p_230285_1_) {
        return p_230285_1_.isIn(FluidTags.LAVA);
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.canMoveVoluntarily() && (this.isTouchingWater() || this.isInLava())) {
            this.updateVelocity(this.getMovementSpeed(), travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.9D));
            if (this.getTarget() == null) {
                this.setVelocity(this.getVelocity().add(0.0D, -0.05D, 0.0D));
            }
        } else {
            super.travel(travelVector);
        }

    }

    @Override
    protected void tickWaterBreathingAir(int p_209207_1_) {
    }

    public void shoot(double p_70186_1_, double p_70186_3_, double p_70186_5_, float p_70186_7_, float p_70186_8_) {
        var lvt_9_1_ = (new Vec3d(p_70186_1_, p_70186_3_, p_70186_5_)).normalize().add(this.random.nextGaussian() * 0.007499999832361937D * (double) p_70186_8_, this.random.nextGaussian() * 0.007499999832361937D * (double) p_70186_8_, this.random.nextGaussian() * 0.007499999832361937D * (double) p_70186_8_).multiply(p_70186_7_);
        this.setVelocity(lvt_9_1_);
        float lvt_10_1_ = (float) lvt_9_1_.horizontalLengthSquared();
        this.setYaw( (float) (MathHelper.atan2(lvt_9_1_.x, lvt_9_1_.z) * 57.2957763671875D));
        this.setPitch((float) (MathHelper.atan2(lvt_9_1_.y, lvt_10_1_) * 57.2957763671875D));
        this.prevPitch = this.getPitch();
        this.bodyYaw = getYaw();
        this.headYaw= getYaw();
        this.prevHeadYaw = getYaw();
        this.prevYaw = getYaw();
        this.setDespawnSoon(true);
        this.dataTracker.set(LAUNCHED, true);
    }

    static class StradpoleAISwim extends WanderAroundGoal {
        public StradpoleAISwim(EntityStradpole creature, double speed, int chance) {
            super(creature, speed, chance, false);
        }

        @Override
        public boolean canStart() {
            if (!this.mob.isInLava() && !this.mob.isTouchingWater() || this.mob.hasVehicle() || mob.getTarget() != null || !this.mob.isTouchingWater() && !this.mob.isInLava() && this.mob instanceof ISemiAquatic && !((ISemiAquatic) this.mob).shouldEnterWater()) {
                return false;
            } else {
                if (!this.ignoringChance) {
                    if (this.mob.getRandom().nextInt(this.chance) != 0) {
                        return false;
                    }
                }
                var vector3d = this.getPosition();
                if (vector3d == null) {
                    return false;
                } else {
                    this.targetX = vector3d.x;
                    this.targetY = vector3d.y;
                    this.targetZ = vector3d.z;
                    this.ignoringChance = false;
                    return true;
                }
            }
        }

        @Nullable
        protected Vec3d getPosition() {
            if (this.mob.getRandom().nextFloat() < 0.3F) {
                Vec3d vector3d = findSurfaceTarget(this.mob, 15, 7);
                if (vector3d != null) {
                    return vector3d;
                }
            }
            var vector3d = FuzzyTargeting.find(this.mob, 7, 3);

            for (int i = 0; vector3d != null && !this.mob.getWorld().getFluidState(AMBlockPos.fromVec3(vector3d)).isIn(FluidTags.LAVA) && !this.mob.getWorld().getBlockState(AMBlockPos.fromVec3(vector3d)).canPathfindThrough(this.mob.getWorld(), AMBlockPos.fromVec3(vector3d), NavigationType.WATER) && i++ < 15; vector3d = FuzzyTargeting.find(this.mob, 10, 7)) {
            }

            return vector3d;
        }

        private boolean canJumpTo(BlockPos pos, int dx, int dz, int scale) {
            BlockPos blockpos = pos.add(dx * scale, 0, dz * scale);
            return this.mob.getWorld().getFluidState(blockpos).isIn(FluidTags.LAVA) || this.mob.getWorld().getFluidState(blockpos).isIn(FluidTags.WATER) && !this.mob.getWorld().getBlockState(blockpos).blocksMovement();
        }

        private boolean isAirAbove(BlockPos pos, int dx, int dz, int scale) {
            return this.mob.getWorld().getBlockState(pos.add(dx * scale, 1, dz * scale)).isAir() && this.mob.getWorld().getBlockState(pos.add(dx * scale, 2, dz * scale)).isAir();
        }

        private Vec3d findSurfaceTarget(PathAwareEntity creature, int i, int i1) {
            BlockPos upPos = creature.getBlockPos();
            while (creature.getWorld().getFluidState(upPos).isIn(FluidTags.WATER) || creature.getWorld().getFluidState(upPos).isIn(FluidTags.LAVA)) {
                upPos = upPos.up();
            }
            if (isAirAbove(upPos.down(), 0, 0, 0) && canJumpTo(upPos.down(), 0, 0, 0)) {
                return new Vec3d(upPos.getX() + 0.5F, upPos.getY() - 1F, upPos.getZ() + 0.5F);
            }
            return null;
        }
    }

}
