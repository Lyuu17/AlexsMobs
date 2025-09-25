package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.packet.CrowDismountPacket;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.EntityView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public class EntityCrow extends TameableEntity implements ITargetsDroppedItems {

    private static final TrackedData<Boolean> FLYING = DataTracker.registerData(EntityCrow.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> ATTACK_TICK = DataTracker.registerData(EntityCrow.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityCrow.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> COMMAND = DataTracker.registerData(EntityCrow.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Optional<BlockPos>> PERCH_POS = DataTracker.registerData(EntityCrow.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);
    public float prevFlyProgress;
    public float flyProgress;
    public float prevAttackProgress;
    public float attackProgress;
    public int fleePumpkinFlag = 0;
    public boolean aiItemFlag = false;
    public boolean aiItemFrameFlag = false;
    public float prevSitProgress;
    public float sitProgress;
    private boolean isLandNavigator;
    private int timeFlying = 0;
    @Nullable
    private UUID seedThrowerID;
    private int heldItemTime = 0;
    private int checkPerchCooldown = 0;
    private final boolean gatheringClockwise = false;

    public EntityCrow(EntityType<? extends EntityCrow> type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, -1.0F);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 16.0F);
        this.setPathfindingPenalty(PathNodeType.COCOA, -1.0F);
        this.setPathfindingPenalty(PathNodeType.FENCE, -1.0F);
        switchNavigator(false);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 8.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new SitGoal(this));
        this.goalSelector.add(2, new CrowAIMelee(this));
        this.goalSelector.add(3, new CrowAIFollowOwner(this, 1.0D, 4.0F, 2.0F, true));
        this.goalSelector.add(4, new AIDepositChests());
        this.goalSelector.add(4, new AIScatter());
        this.goalSelector.add(5, new AIAvoidPumpkins());
        this.goalSelector.add(5, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(6, new CrowAICircleCrops(this));
        this.goalSelector.add(7, new AIWalkIdle());
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(9, new LookAtEntityGoal(this, PathAwareEntity.class, 6.0F));
        this.goalSelector.add(10, new LookAroundGoal(this));
        this.targetSelector.add(1, new AITargetItems<>(this, false, false, 40, 16));
        this.targetSelector.add(2, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(3, new AttackWithOwnerGoal(this));
        this.targetSelector.add(4, (new RevengeGoal(this, PlayerEntity.class)).setGroupRevenge());

    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.crowSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static <T extends MobEntity> boolean canCrowSpawn(EntityType<EntityCrow> crow, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return isLightLevelValidForNaturalSpawn(worldIn, pos);
    }

    @Override
    public boolean isTeammate(Entity entityIn) {
        if (this.isTamed()) {
            var livingentity = this.getOwner();
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

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new MobNavigation(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new FlightMoveController(this, 0.7F, false);
            this.navigation = new DirectPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    // FIXME forge
    public boolean canTrample(BlockState state, BlockPos pos, float fallDistance) {
        return false;
    }

    @Override
    public boolean handleFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            final var entity = source.getAttacker();
            this.setSitting(false);
            if (entity != null && this.isTamed() && !(entity instanceof PlayerEntity) && !(entity instanceof PersistentProjectileEntity)) {
                amount = (amount + 1.0F) / 4.0F;
            }

            if(this.hasVehicle()){
                this.stopRiding();
            }
            final boolean prev = super.damage(source, amount);
            if (prev) {
                if (!this.getMainHandStack().isEmpty()) {
                    this.dropStack(this.getMainHandStack().copy());
                    this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                }
            }
            return prev;
        }
    }

    @Override
    public void tickRiding() {
        final Entity entity = this.getVehicle();
        if (this.hasVehicle() && !entity.isAlive()) {
            this.stopRiding();
        } else if (isTamed() && entity instanceof LivingEntity && isOwner((LivingEntity) entity)) {
            this.setVelocity(0, 0, 0);
            this.tick();
            final Entity riding = this.getVehicle();
            if (this.hasVehicle()) {
                final int i = riding.getPassengerList().indexOf(this);
                final float radius = 0.43F;
                final float angle = (Maths.STARTING_ANGLE * (((PlayerEntity) riding).bodyYaw + (i == 0 ? -90 : 90)));
                final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
                final double extraZ = radius * MathHelper.cos(angle);
                final double extraY = (riding.isSneaking() ? 1.25D : 1.45D);
                this.headYaw= ((PlayerEntity) riding).headYaw;
                this.prevYaw = ((PlayerEntity) riding).headYaw;
                this.setPos(riding.getX() + extraX, riding.getY() + extraY, riding.getZ() + extraZ);
                if (!riding.isAlive() || ridingCooldown == 0 && riding.isSneaking() || ((PlayerEntity) riding).isFallFlying() || this.getTarget() != null && this.getTarget().isAlive()) {
                    this.dismountVehicle();
                    if (!this.getWorld().isClient) {
                        AlexsMobs.sendMSGToAll(new CrowDismountPacket(this.getId(), riding.getId()));
                    }
                }
            }
        } else {
            super.tickRiding();
        }
    }

    public int getRidingCrows(LivingEntity player) {
        int crowCount = 0;
        for (final Entity e : player.getPassengerList()) {
            if (e instanceof EntityCrow) {
                crowCount++;
            }
        }
        return crowCount;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.CROW_BREEDABLES) && this.isTamed();
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        final ItemStack itemstack = player.getStackInHand(hand);
        final ActionResult type = super.interactMob(player, hand);
        if (!this.getMainHandStack().isEmpty() && type != ActionResult.SUCCESS) {
            this.dropStack(this.getMainHandStack().copy());
            this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            return ActionResult.SUCCESS;
        } else {
            final var actionResult = itemstack.useOnEntity(player, this, hand);
            if (actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS && isTamed() && isOwner(player) && !isBreedingItem(itemstack)) {
                if (isCrowEdible(itemstack) && this.getMainHandStack().isEmpty()) {
                    ItemStack cop = itemstack.copy();
                    cop.setCount(1);
                    this.setStackInHand(Hand.MAIN_HAND, cop);
                    itemstack.decrement(1);
                }
                this.setCommand(this.getCommand() + 1);
                if (this.getCommand() == 4) {
                    this.setCommand(0);
                }
                if(this.getCommand() == 3){
                    player.sendMessage(Text.translatable("entity.alexsmobs.crow.command_3", this.getName()), true);
                }else{
                    player.sendMessage(Text.translatable("entity.alexsmobs.all.command_" + this.getCommand(), this.getName()), true);
                }
                final boolean sit = this.getCommand() == 2;
                this.setSitting(sit);
                return ActionResult.SUCCESS;
            }
            return super.interactMob(player, hand);
        }
    }


    @Override
    public void tick() {
        super.tick();
        this.prevAttackProgress = attackProgress;
        prevFlyProgress = flyProgress;
        this.prevSitProgress = this.sitProgress;

        final boolean isSittingOrPassenger = (this.isSitting() || this.hasVehicle());
        if (isSittingOrPassenger) {
            if (sitProgress < 5F)
                sitProgress++;
        } else {
            if (sitProgress > 0F)
                sitProgress--;
        }

        if (isFlying()) {
            if (flyProgress < 5F)
                flyProgress++;
        } else {
            if (flyProgress > 0F)
                flyProgress--;
        }

        if (fleePumpkinFlag > 0) {
            fleePumpkinFlag--;
        }

        if (!this.getWorld().isClient) {
            final boolean isFlying = isFlying();
            if (isFlying && this.isLandNavigator) {
                switchNavigator(false);
            }
            if (!isFlying && !this.isLandNavigator) {
                switchNavigator(true);
            }
            if (isFlying) {
                timeFlying++;
                this.setNoGravity(true);
                if (this.isSitting() || this.hasVehicle() || this.isInLove()) {
                    this.setFlying(false);
                }
            } else {
                timeFlying = 0;
                this.setNoGravity(false);
            }
        }
        if (!this.getMainHandStack().isEmpty()) {
            heldItemTime++;
            if (heldItemTime > 60 && isCrowEdible(this.getMainHandStack()) && (!this.isTamed() || this.getHealth() < this.getMaxHealth())) {
                heldItemTime = 0;
                this.heal(4);
                this.emitGameEvent(GameEvent.EAT);
                this.playSound(SoundEvents.ENTITY_PARROT_EAT, this.getSoundVolume(), this.getSoundPitch());
                if (seedThrowerID != null && this.getMainHandStack().isIn(AMTagRegistry.CROW_TAMEABLES) && !this.isTamed()) {
                    if (getRandom().nextFloat() < 0.3F) {
                        this.setTamed(true);
                        this.setCommand(1);
                        this.setOwnerUuid(this.seedThrowerID);
                        final PlayerEntity player = getWorld().getPlayerByUuid(seedThrowerID);
                        if (player instanceof final ServerPlayerEntity serverPlayer) {
                            Criteria.TAME_ANIMAL.trigger(serverPlayer, this);
                        }
                        this.getWorld().sendEntityStatus(this, (byte) 7);
                    } else {
                        this.getWorld().sendEntityStatus(this, (byte) 6);
                    }
                }
                if (this.getMainHandStack().getItem().hasRecipeRemainder()) {
                    this.dropItem(this.getMainHandStack().getItem().getRecipeRemainder());
                }
                this.getMainHandStack().decrement(1);
            }
        } else {
            heldItemTime = 0;
        }
        if (ridingCooldown > 0) {
            ridingCooldown--;
        }
        if (this.dataTracker.get(ATTACK_TICK) > 0) {
            this.dataTracker.set(ATTACK_TICK, this.dataTracker.get(ATTACK_TICK) - 1);
            if (attackProgress < 5F) {
                attackProgress++;
            }
        } else {
            if (attackProgress > 0F) {
                attackProgress--;
            }
        }
        if(checkPerchCooldown > 0){
            checkPerchCooldown--;
        }


        if (this.isTamed()) {
            if (checkPerchCooldown == 0) {
                checkPerchCooldown = 50;
                final var below = this.getSteppingBlockState();
                if (below.isIn(AMTagRegistry.CROW_HOME_BLOCKS)) {
                    this.heal(1);
                    this.getWorld().sendEntityStatus(this, (byte) 67);
                    this.setPerchPos(this.getVelocityAffectingPos());
                }
            }
            if (this.getCommand() == 3 && getPerchPos() != null && checkPerchCooldown == 0) {
                checkPerchCooldown = 120;
                final var below = this.getWorld().getBlockState(getPerchPos());
                if (below.isIn(AMTagRegistry.CROW_HOME_BLOCKS)) {
                    this.getWorld().sendEntityStatus(this, (byte) 68);
                    this.setPerchPos(null);
                    this.setCommand(2);
                    this.setSitting(true);
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if (id == 67) {
            for(int i = 0; i < 7; ++i) {
                final double d0 = this.random.nextGaussian() * 0.02D;
                final double d1 = this.random.nextGaussian() * 0.02D;
                final double d2 = this.random.nextGaussian() * 0.02D;
                this.getWorld().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getParticleX(1.0D), this.getRandomBodyY() + 0.5D, this.getParticleZ(1.0D), d0, d1, d2);
            }
        } else if (id == 68) {
            for(int i = 0; i < 7; ++i) {
                final double d0 = this.random.nextGaussian() * 0.02D;
                final double d1 = this.random.nextGaussian() * 0.02D;
                final double d2 = this.random.nextGaussian() * 0.02D;
                this.getWorld().addParticle(ParticleTypes.ANGRY_VILLAGER, this.getParticleX(1.0D), this.getRandomBodyY() + 0.5D, this.getParticleZ(1.0D), d0, d1, d2);
            }
        } else {
            super.handleStatus(id);
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Flying", this.isFlying());
        compound.putBoolean("MonkeySitting", this.isSitting());
        compound.putInt("Command", this.getCommand());
        if (this.getPerchPos() != null) {
            compound.putInt("PerchX", this.getPerchPos().getX());
            compound.putInt("PerchY", this.getPerchPos().getY());
            compound.putInt("PerchZ", this.getPerchPos().getZ());
        }
    }

    @Override
    public void travel(Vec3d vec3d) {
        if (this.isSitting()) {
            if (this.getNavigation().getCurrentPath() != null) {
                this.getNavigation().stop();
            }
            vec3d = Vec3d.ZERO;
        }
        if(this.isTouchingWater() && this.getVelocity().y > 0F){
            this.setVelocity(this.getVelocity().multiply(1.0D, 0.5D, 1.0D));
        }
        super.travel(vec3d);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setFlying(compound.getBoolean("Flying"));
        this.setSitting(compound.getBoolean("MonkeySitting"));
        this.setCommand(compound.getInt("Command"));
        if (compound.contains("PerchX") && compound.contains("PerchY") && compound.contains("PerchZ")) {
            this.setPerchPos(new BlockPos(compound.getInt("PerchX"), compound.getInt("PerchY"), compound.getInt("PerchZ")));
        }
    }

    @Override
    public boolean isFlying() {
        return this.dataTracker.get(FLYING);
    }

    @Override
    public void setFlying(boolean flying) {
        if(flying && isBaby()){
            return;
        }
        this.dataTracker.set(FLYING, flying);
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

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FLYING, false);
        this.dataTracker.startTracking(ATTACK_TICK, 0);
        this.dataTracker.startTracking(COMMAND, 0);
        this.dataTracker.startTracking(SITTING, false);
        this.dataTracker.startTracking(PERCH_POS, Optional.empty());
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.IN_WALL)  || source.isOf(DamageTypes.FALL) || source.isOf(DamageTypes.CACTUS) || super.isInvulnerableTo(source);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.CROW.get().create(world);
    }

    public boolean isTargetBlocked(Vec3d target) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());

        return this.getWorld().raycast(new RaycastContext(Vector3d, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() != HitResult.Type.MISS;
    }

    @Override
    public int getMinAmbientSoundDelay() {
        return 60;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.CROW_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.CROW_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.CROW_HURT.get();
    }

    public Vec3d getBlockInViewAway(Vec3d fleePos, float radiusAdd) {
        final float radius = 3.15F * -3 - this.getRandom().nextInt(24) - radiusAdd;
        final float angle = getAngle1();
        final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        final double extraZ = radius * MathHelper.cos(angle);
        final BlockPos radialPos = new BlockPos((int) (fleePos.x + extraX), 0, (int) (fleePos.z + extraZ));
        final BlockPos ground = getCrowGround(radialPos);
        final int distFromGround = (int) this.getY() - ground.getY();

        final BlockPos newPos;
        if (distFromGround > 8) {
            final int flightHeight = 4 + this.getRandom().nextInt(10);
            newPos = ground.up(flightHeight);
        } else {
            newPos = ground.up(this.getRandom().nextInt(6) + 1);
        }

        if (!this.isTargetBlocked(Vec3d.ofCenter(newPos)) && this.squaredDistanceTo(Vec3d.ofCenter(newPos)) > 1) {
            return Vec3d.ofCenter(newPos);
        }
        return null;
    }

    private BlockPos getCrowGround(BlockPos in){
        BlockPos position = new BlockPos(in.getX(), (int) this.getY(), in.getZ());
        while (position.getY() > -64 && !getWorld().getBlockState(position).isSolid() && getWorld().getFluidState(position).isEmpty()) {
            position = position.down();
        }
        return position;
    }

    public Vec3d getBlockGrounding(Vec3d fleePos) {
        final float radius = 3.15F * -3 - this.getRandom().nextInt(24);
        final float angle = getAngle1();
        final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        final double extraZ = radius * MathHelper.cos(angle);
        final var radialPos = new BlockPos((int) (fleePos.x + extraX), (int) getY(), (int) (fleePos.z + extraZ));
        var ground = this.getCrowGround(radialPos);
        if (ground.getY() == -64) {
            return this.getPos();
        } else {
            ground = this.getBlockPos();
            while (ground.getY() > -64 && !getWorld().getBlockState(ground).isSolid()) {
                ground = ground.down();
            }
        }
        if (!this.isTargetBlocked(Vec3d.ofCenter(ground.up()))) {
            return Vec3d.ofCenter(ground);
        }
        return null;
    }

    private float getAngle1() {
        final float neg = this.getRandom().nextBoolean() ? 1 : -1;
        final float renderYawOffset = this.bodyYaw;
        return (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
    }

    private boolean isOverWater() {
        BlockPos position = this.getBlockPos();
        while (position.getY() > -64 && getWorld().isAir(position)) {
            position = position.down();
        }
        return !getWorld().getFluidState(position).isEmpty();
    }

    public void peck() {
        this.dataTracker.set(ATTACK_TICK, 7);
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack != null && isCrowEdible(stack) || this.isTamed();
    }

    private boolean isCrowEdible(ItemStack stack) {
        return stack.getItem().isFood() || stack.isIn(AMTagRegistry.CROW_FOODSTUFFS);
    }

    public double getMaxDistToItem() {
        return 1.0D;
    }

    @Override
    public void onGetItem(ItemEntity e) {
        final var duplicate = e.getStack().copy();
        duplicate.setCount(1);
        if (!this.getStackInHand(Hand.MAIN_HAND).isEmpty() && !this.getWorld().isClient) {
            this.dropStack(this.getStackInHand(Hand.MAIN_HAND), 0.0F);
        }
        this.setStackInHand(Hand.MAIN_HAND, duplicate);
        var itemThrower = e.getOwner();
        if (e.getStack().isIn(AMTagRegistry.CROW_TAMEABLES) && !this.isTamed() && itemThrower != null) {
            seedThrowerID = itemThrower.getUuid();
        } else {
            seedThrowerID = null;
        }
    }

    public BlockPos getPerchPos() {
        return this.dataTracker.get(PERCH_POS).orElse(null);
    }

    public void setPerchPos(BlockPos pos) {
        this.dataTracker.set(PERCH_POS, Optional.ofNullable(pos));
    }

    @Override
    public EntityView method_48926() {
        return getWorld();
    }

    private class AIWalkIdle extends Goal {
        protected final EntityCrow crow;
        protected double x;
        protected double y;
        protected double z;
        private boolean flightTarget = false;

        public AIWalkIdle() {
            super();
            this.setControls(EnumSet.of(Control.MOVE));
            this.crow = EntityCrow.this;
        }

        @Override
        public boolean canStart() {
            if (this.crow.hasPassengers() || EntityCrow.this.getCommand() == 1 || EntityCrow.this.aiItemFlag || (crow.getTarget() != null && crow.getTarget().isAlive()) || this.crow.hasVehicle() || this.crow.isSitting()) {
                return false;
            } else {
                if (this.crow.getRandom().nextInt(30) != 0 && !crow.isFlying()) {
                    return false;
                }
                if (this.crow.isOnGround()) {
                    this.flightTarget = random.nextBoolean();
                } else {
                    this.flightTarget = random.nextInt(5) > 0 && crow.timeFlying < 200;
                }
                if(crow.getCommand() == 3){
                    if(crow.aiItemFrameFlag){
                        return false;
                    }
                    this.flightTarget = true;
                }
                var lvt_1_1_ = this.getPosition();
                if (lvt_1_1_ == null) {
                    return false;
                } else {
                    this.x = lvt_1_1_.x;
                    this.y = lvt_1_1_.y;
                    this.z = lvt_1_1_.z;
                    return true;
                }
            }
        }

        @Override
        public void tick() {
            if (flightTarget) {
                crow.getMoveControl().moveTo(x, y, z, 1F);
            } else {
                this.crow.getNavigation().startMovingTo(this.x, this.y, this.z, 1F);

                if (isFlying() && crow.isOnGround()) {
                    crow.setFlying(false);
                }
            }

            if (isFlying() && crow.isOnGround() && crow.timeFlying > 10) {
                crow.setFlying(false);
            }
        }

        @Nullable
        protected Vec3d getPosition() {
            final Vec3d vector3d = crow.getPos();
            if (crow.getCommand() == 3 && crow.getPerchPos() != null) {
                return crow.getGatheringVec(vector3d, 4 + random.nextInt(2));
            }
            if(crow.isOverWater()){
                flightTarget = true;
            }
            if (flightTarget) {
                if (crow.timeFlying < 50 || crow.isOverWater()) {
                    return crow.getBlockInViewAway(vector3d, 0);
                } else {
                    return crow.getBlockGrounding(vector3d);
                }
            } else {
                return FuzzyTargeting.find(this.crow, 10, 7);
            }
        }

        @Override
        public boolean shouldContinue() {
            if (crow.aiItemFlag || crow.isSitting() || EntityCrow.this.getCommand() == 1) {
                return false;
            }
            if (flightTarget) {
                return crow.isFlying() && crow.squaredDistanceTo(x, y, z) > 2F;
            } else {
                return (!this.crow.getNavigation().isIdle()) && !this.crow.hasPassengers();
            }
        }

        @Override
        public void start() {
            if (flightTarget) {
                crow.setFlying(true);
                crow.getMoveControl().moveTo(x, y, z, 1F);
            } else {
                this.crow.getNavigation().startMovingTo(this.x, this.y, this.z, 1F);
            }
        }

        @Override
        public void stop() {
            this.crow.getNavigation().stop();
            super.stop();
        }
    }

    private Vec3d getGatheringVec(Vec3d vector3d, float gatheringCircleDist) {
        if (this.getPerchPos() != null) {
            final float angle = (Maths.EIGHT_STARTING_ANGLE * (gatheringClockwise ? - age : age));
            final double extraX = gatheringCircleDist * MathHelper.sin((angle));
            final double extraZ = gatheringCircleDist * MathHelper.cos(angle);
            final var pos = new Vec3d(getPerchPos().getX() + extraX, getPerchPos().getY() + 2, getPerchPos().getZ() + extraZ);
            if (this.getWorld().isAir(AMBlockPos.fromVec3(pos))) {
                return pos;
            }
        }
        return null;
    }

    private class AIScatter extends Goal {
        protected final AIScatter.Sorter theNearestAttackableTargetSorter;
        protected final Predicate<? super Entity> targetEntitySelector;
        protected int executionChance = 8;
        protected boolean mustUpdate;
        private Entity targetEntity;
        private Vec3d flightTarget = null;
        private int cooldown = 0;

        AIScatter() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
            this.theNearestAttackableTargetSorter = new AIScatter.Sorter(EntityCrow.this);
            this.targetEntitySelector = new Predicate<Entity>() {
                @Override
                public boolean test(@Nullable Entity e) {
                    return e.isAlive() && e.getType().isIn(AMTagRegistry.SCATTERS_CROWS) || e instanceof PlayerEntity && !((PlayerEntity) e).isCreative();
                }
            };
        }

        @Override
        public boolean canStart() {
            if (EntityCrow.this.hasVehicle() || EntityCrow.this.aiItemFlag || EntityCrow.this.hasPassengers() || EntityCrow.this.isTamed()) {
                return false;
            }
            if (!this.mustUpdate) {
                final long worldTime = EntityCrow.this.getWorld().getTime() % 10;
                if (worldTime != 0) {
                    if (EntityCrow.this.getDespawnCounter() >= 100) {
                        return false;
                    }
                    if (EntityCrow.this.getRandom().nextInt(this.executionChance) != 0) {
                        return false;
                    }
                }
            }
            final var list = EntityCrow.this.getWorld().getEntitiesByClass(Entity.class, this.getTargetableArea(this.getTargetDistance()), this.targetEntitySelector);
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
            return targetEntity != null && !EntityCrow.this.isTamed();
        }

        @Override
        public void stop() {
            flightTarget = null;
            this.targetEntity = null;
        }

        @Override
        public void tick() {
            if (cooldown > 0) {
                cooldown--;
            }
            if (flightTarget != null) {
                EntityCrow.this.setFlying(true);
                EntityCrow.this.getMoveControl().moveTo(flightTarget.x, flightTarget.y, flightTarget.z, 1F);
                if(cooldown == 0 && EntityCrow.this.isTargetBlocked(flightTarget)){
                    cooldown = 30;
                    flightTarget = null;
                }
            }

            if (targetEntity != null) {
                if (EntityCrow.this.isOnGround() || flightTarget == null || EntityCrow.this.squaredDistanceTo(flightTarget) < 3) {
                    final Vec3d vec = EntityCrow.this.getBlockInViewAway(targetEntity.getPos(), 0);
                    if (vec != null && vec.y > EntityCrow.this.getY()) {
                        flightTarget = vec;
                    }
                }
                if (EntityCrow.this.distanceTo(targetEntity) > 20.0F) {
                    this.stop();
                }
            }
        }

        protected double getTargetDistance() {
            return 4D;
        }

        protected Box getTargetableArea(double targetDistance) {
            final var renderCenter = new Vec3d(EntityCrow.this.getX(), EntityCrow.this.getY() + 0.5, EntityCrow.this.getZ());
            final var aabb = new Box(-2, -2, -2, 2, 2, 2);
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

    private class AIAvoidPumpkins extends Goal {
        private final int searchLength;
        private final int verticalSearchRange;
        protected BlockPos destinationBlock;
        protected int runDelay = 70;
        private Vec3d flightTarget;

        private AIAvoidPumpkins() {
            searchLength = 20;
            verticalSearchRange = 1;
        }

        @Override
        public boolean shouldContinue() {
            return destinationBlock != null && isPumpkin(EntityCrow.this.getWorld(), destinationBlock.mutableCopy()) && isCloseToPumpkin(16);
        }

        public boolean isCloseToPumpkin(double dist) {
            return destinationBlock == null || EntityCrow.this.squaredDistanceTo(Vec3d.ofCenter(destinationBlock)) < dist * dist;
        }

        @Override
        public boolean canStart() {
            if (EntityCrow.this.isTamed()) {
                return false;
            }
            if (this.runDelay > 0) {
                --this.runDelay;
                return false;
            } else {
                this.runDelay = 70 + EntityCrow.this.random.nextInt(150);
                return this.searchForDestination();
            }
        }

        @Override
        public void start() {
            EntityCrow.this.fleePumpkinFlag = 200;
            final Vec3d vec = EntityCrow.this.getBlockInViewAway(Vec3d.ofCenter(destinationBlock), 10);
            if (vec != null) {
                flightTarget = vec;
                EntityCrow.this.setFlying(true);
                EntityCrow.this.getMoveControl().moveTo(vec.x, vec.y, vec.z, 1F);
            }
        }

        @Override
        public void tick() {
            if (this.isCloseToPumpkin(16)) {
                EntityCrow.this.fleePumpkinFlag = 200;
                if (flightTarget == null || EntityCrow.this.squaredDistanceTo(flightTarget) < 2F) {
                    final Vec3d vec = EntityCrow.this.getBlockInViewAway(Vec3d.ofCenter(destinationBlock), 10);
                    if (vec != null) {
                        flightTarget = vec;
                        EntityCrow.this.setFlying(true);
                    }
                }
                if (flightTarget != null) {
                    EntityCrow.this.getMoveControl().moveTo(flightTarget.x, flightTarget.y, flightTarget.z, 1F);
                }
            }
        }

        @Override
        public void stop() {
            flightTarget = null;
        }

        protected boolean searchForDestination() {
            int lvt_1_1_ = this.searchLength;
            //int lvt_2_1_ = this.verticalSearchRange;
            BlockPos lvt_3_1_ = EntityCrow.this.getBlockPos();
            BlockPos.Mutable lvt_4_1_ = new BlockPos.Mutable();

            for (int lvt_5_1_ = -8; lvt_5_1_ <= 2; lvt_5_1_++) {
                for (int lvt_6_1_ = 0; lvt_6_1_ < lvt_1_1_; ++lvt_6_1_) {
                    for (int lvt_7_1_ = 0; lvt_7_1_ <= lvt_6_1_; lvt_7_1_ = lvt_7_1_ > 0 ? -lvt_7_1_ : 1 - lvt_7_1_) {
                        for (int lvt_8_1_ = lvt_7_1_ < lvt_6_1_ && lvt_7_1_ > -lvt_6_1_ ? lvt_6_1_ : 0; lvt_8_1_ <= lvt_6_1_; lvt_8_1_ = lvt_8_1_ > 0 ? -lvt_8_1_ : 1 - lvt_8_1_) {
                            lvt_4_1_.set(lvt_3_1_, lvt_7_1_, lvt_5_1_ - 1, lvt_8_1_);
                            if (this.isPumpkin(EntityCrow.this.getWorld(), lvt_4_1_)) {
                                this.destinationBlock = lvt_4_1_;
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }

        private boolean isPumpkin(World world, BlockPos.Mutable lvt_4_1_) {
            return world.getBlockState(lvt_4_1_).isIn(AMTagRegistry.CROW_FEARS);
        }
    }

    private static class AITargetItems<T extends ItemEntity> extends CreatureAITargetItems<T> {

        public AITargetItems(PathAwareEntity creature, boolean checkSight, boolean onlyNearby, int tickThreshold, int radius) {
            super(creature, checkSight, onlyNearby, tickThreshold, radius);
            this.executionChance = 1;
        }

        @Override
        public void stop() {
            super.stop();
            ((EntityCrow) mob).aiItemFlag = false;
        }

        @Override
        public boolean canStart() {
            return super.canStart()  &&  !((EntityCrow) mob).isSitting() && (mob.getTarget() == null || !mob.getTarget().isAlive());
        }

        @Override
        public boolean shouldContinue() {
            return super.shouldContinue() && !((EntityCrow) mob).isSitting() &&  (mob.getTarget() == null || !mob.getTarget().isAlive());
        }

        @Override
        protected void moveTo() {
            EntityCrow crow = (EntityCrow) mob;
            if (this.targetEntity != null) {
                crow.aiItemFlag = true;
                if (this.mob.distanceTo(targetEntity) < 2) {
                    crow.getMoveControl().moveTo(this.targetEntity.getX(), targetEntity.getY(), this.targetEntity.getZ(), 1);
                    crow.peck();
                }
                if (this.mob.distanceTo(this.targetEntity) > 8 || crow.isFlying()) {
                    crow.setFlying(true);

                    if(!crow.canSee(targetEntity)){
                        crow.getMoveControl().moveTo(this.targetEntity.getX(), 1 + crow.getY(), this.targetEntity.getZ(), 1);
                    }else{
                        final float f = (float) (crow.getX() - targetEntity.getX());
                        final float f2 = (float) (crow.getZ() - targetEntity.getZ());
                        final float xzDist = MathHelper.sqrt(f * f + f2 * f2);
                        final float f1 = xzDist < 5F ? 0 : 1.8F;

                        crow.getMoveControl().moveTo(this.targetEntity.getX(), f1 + this.targetEntity.getY(), this.targetEntity.getZ(), 1);
                    }
                } else {
                    this.mob.getNavigation().startMovingTo(this.targetEntity.getX(), this.targetEntity.getY(), this.targetEntity.getZ(), 1);
                }
            }
        }

        @Override
        public void tick() {
            super.tick();
            moveTo();
        }
    }


    private class AIDepositChests extends Goal {
        protected final AIDepositChests.Sorter theNearestAttackableTargetSorter;
        protected final Predicate<ItemFrameEntity> targetEntitySelector;
        protected int executionChance = 8;
        protected boolean mustUpdate;
        private ItemFrameEntity targetEntity;
        private Vec3d flightTarget = null;
        private int cooldown = 0;

        AIDepositChests() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
            this.theNearestAttackableTargetSorter = new AIDepositChests.Sorter(EntityCrow.this);
            // FIXME forge
//            this.targetEntitySelector = new Predicate<ItemFrameEntity>() {
//                @Override
//                public boolean test(@Nullable ItemFrameEntity e) {
//                    var hangingPosition = e.getPos().offset(e.getHorizontalFacing().getOpposite());
//                    var entity = e.getWorld().getBlockEntity(hangingPosition);
//                    if(entity != null){
//                        LazyOptional<IItemHandler> handler = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, e.getDirection().getOpposite());
//                        if(handler != null && handler.isPresent()){
//                            return ItemStack.isSameItem(e.getItem(), EntityCrow.this.getMainHandStack());
//                        }
//                    }
//                    return false;
//                }
//            };
            this.targetEntitySelector = new Predicate<ItemFrameEntity>() {
                @Override
                public boolean test(ItemFrameEntity itemFrameEntity) {
                    return false;
                }
            };
        }

        @Override
        public boolean canStart() {
            if (EntityCrow.this.hasVehicle() || EntityCrow.this.aiItemFlag || EntityCrow.this.hasPassengers() || EntityCrow.this.isSitting() || EntityCrow.this.getCommand() != 3) {
                return false;
            }
            if(EntityCrow.this.getMainHandStack().isEmpty()){
                return false;
            }
            if (!this.mustUpdate) {
                final long worldTime = EntityCrow.this.getWorld().getTime() % 10;
                if (worldTime != 0) {
                    if (EntityCrow.this.getDespawnCounter() >= 100) {
                        return false;
                    }
                    if (EntityCrow.this.getRandom().nextInt(this.executionChance) != 0) {
                        return false;
                    }
                }
            }
            final var list = EntityCrow.this.getWorld().getEntitiesByClass(ItemFrameEntity.class, this.getTargetableArea(this.getTargetDistance()), this.targetEntitySelector);
            if (list.isEmpty()) {
                return false;
            } else {
                list.sort(this.theNearestAttackableTargetSorter);
                this.targetEntity = list.get(0);
                this.mustUpdate = false;
                EntityCrow.this.aiItemFrameFlag = true;
                return true;
            }
        }

        @Override
        public boolean shouldContinue() {
            return targetEntity != null && EntityCrow.this.getCommand() == 3 && !EntityCrow.this.getMainHandStack().isEmpty();
        }

        @Override
        public void stop() {
            flightTarget = null;
            this.targetEntity = null;
            EntityCrow.this.aiItemFrameFlag = false;
        }

        @Override
        public void tick() {
            if (cooldown > 0) {
                cooldown--;
            }
            if (flightTarget != null) {
                EntityCrow.this.setFlying(true);
                if(EntityCrow.this.horizontalCollision){
                    EntityCrow.this.getMoveControl().moveTo(flightTarget.x, EntityCrow.this.getY() + 1F, flightTarget.z, 1F);

                }else{
                    EntityCrow.this.getMoveControl().moveTo(flightTarget.x, flightTarget.y, flightTarget.z, 1F);
                }
            }
            if (targetEntity != null) {
                flightTarget = targetEntity.getPos();
                if (EntityCrow.this.distanceTo(targetEntity) < 2.0F) {
                    //FIXME forge
//                    try{
//                        final var hangingPosition = targetEntity.getPos().relative(targetEntity.getDirection().getOpposite());
//                        final var entity = targetEntity.getWorld().getBlockEntity(hangingPosition);
//                        final var deposit = targetEntity.getDirection();
//                        final var handler = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, deposit);
//                        if(handler.orElse(null) != null && cooldown == 0) {
//                            var duplicate = EntityCrow.this.getStackInHand(Hand.MAIN_HAND).copy();
//                            var insertSimulate = ItemHandlerHelper.insertItem(handler.orElse(null), duplicate, true);
//                            if (!insertSimulate.equals(duplicate)) {
//                                var shrunkenStack = ItemHandlerHelper.insertItem(handler.orElse(null), duplicate, false);
//                                if(shrunkenStack.isEmpty()){
//                                    EntityCrow.this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
//                                }else{
//                                    EntityCrow.this.setStackInHand(Hand.MAIN_HAND, shrunkenStack);
//                                }
//                                EntityCrow.this.peck();
//                            }else{
//                                cooldown = 20;
//                            }
//                        }
//                    }catch (Exception e){
//                    }
                    this.stop();
                }
            }
        }

        protected double getTargetDistance() {
            return 4D;
        }

        protected Box getTargetableArea(double targetDistance) {
            final var renderCenter = new Vec3d(EntityCrow.this.getX(), EntityCrow.this.getY(), EntityCrow.this.getZ());
            final var aabb = new Box(-16, -16, -16, 16, 16, 16);
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
