package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.misc.IngredientUtil;
import com.github.alexthe666.alexsmobs.packet.MosquitoDismountPacket;
import com.github.alexthe666.alexsmobs.packet.MosquitoMountPlayerPacket;
import com.github.alexthe666.alexsmobs.registry.*;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.EntityView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class EntityBaldEagle extends TameableEntity implements IFollower, IFalconry {

    private static final TrackedData<Boolean> FLYING = DataTracker.registerData(EntityBaldEagle.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> TACKLING = DataTracker.registerData(EntityBaldEagle.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HAS_CAP = DataTracker.registerData(EntityBaldEagle.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> ATTACK_TICK = DataTracker.registerData(EntityBaldEagle.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> COMMAND = DataTracker.registerData(EntityBaldEagle.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityBaldEagle.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> LAUNCHED = DataTracker.registerData(EntityBaldEagle.class, TrackedDataHandlerRegistry.BOOLEAN);
    //FIXME unused?
    //private static final Ingredient TEMPT_ITEMS = Ingredient.ofItems(Items.ROTTEN_FLESH, AMItemRegistry.FISH_OIL.get());
    public float prevAttackProgress;
    public float attackProgress;
    public float prevFlyProgress;
    public float flyProgress;
    public float prevTackleProgress;
    public float tackleProgress;
    public float prevSwoopProgress;
    public float swoopProgress;
    public float prevFlapAmount;
    public float flapAmount;
    public float birdPitch = 0;
    public float prevBirdPitch = 0;
    public float prevSitProgress;
    public float sitProgress;
    private boolean isLandNavigator;
    private int timeFlying;
    private BlockPos orbitPos = null;
    private double orbitDist = 5D;
    private boolean orbitClockwise = false;
    private int passengerTimer = 0;
    private int launchTime = 0;
    private int lastPlayerControlTime = 0;
    private int returnControlTime = 0;
    private int tackleCapCooldown = 0;
    private boolean controlledFlag = false;
    private int chunkLoadCooldown;
    private int stillTicksCounter = 0;

    public EntityBaldEagle(EntityType<? extends TameableEntity> type, World worldIn) {
        super(type, worldIn);
        switchNavigator(true);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 16.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3F);
    }

    public static boolean canEagleSpawn(EntityType<? extends AnimalEntity> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return worldIn.getBaseLightLevel(pos, 0) > 8;
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this) {
            @Override
            public boolean canStart() {
                return super.canStart() && (EntityBaldEagle.this.getAir() < 30 || EntityBaldEagle.this.getTarget() == null || !EntityBaldEagle.this.getTarget().isInsideWaterOrBubbleColumn() && EntityBaldEagle.this.getY() > EntityBaldEagle.this.getTarget().getY());
            }
        });
        this.goalSelector.add(1, new SitGoal(this));
        this.goalSelector.add(2, new FlyingAIFollowOwner(this, 1.0D, 25.0F, 2.0F, false));
        this.goalSelector.add(3, new AITackle());
        this.goalSelector.add(4, new AILandOnGlove());
        this.goalSelector.add(5, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(6, new TemptGoal(this, 1.1D, IngredientUtil.ingredientFromTags(AMTagRegistry.BALD_EAGLE_TAMEABLES, AMTagRegistry.BALD_EAGLE_FOODSTUFFS), false));
        this.goalSelector.add(7, new AIWanderIdle());
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F) {
            @Override
            public boolean canStart() {
                return EntityBaldEagle.this.returnControlTime == 0 && super.canStart();
            }
        });
        this.goalSelector.add(9, new LookAroundGoal(this) {
            @Override
            public boolean canStart() {
                return EntityBaldEagle.this.returnControlTime == 0 && super.canStart();
            }
        });
        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, (new AnimalAIHurtByTargetNotBaby(this)));
        this.targetSelector.add(4, new EntityAINearestTarget3D<>(this, LivingEntity.class, 55, true, true, AMEntityRegistry.buildPredicateFromTag(AMTagRegistry.BALD_EAGLE_TARGETS)) {
            @Override
            public boolean canStart() {
                return super.canStart() && !EntityBaldEagle.this.isLaunched() && EntityBaldEagle.this.getCommand() == 0;
            }
        });
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.BALD_EAGLE_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.BALD_EAGLE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.BALD_EAGLE_HURT.get();
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.baldEagleSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    public boolean isTeammate(Entity entityIn) {
        if (this.isTamed()) {
            LivingEntity livingentity = this.getOwner();
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

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.BALD_EAGLE_BREEDABLES);
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new GroundPathNavigatorWide(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new MoveHelper(this);
            this.navigation = new DirectPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    public boolean saveNbt(NbtCompound compound) {
        var s = this.getSavedEntityId();
        compound.putString("id", s);
        super.saveNbt(compound);
        return true;
    }

    @Override
    public boolean saveSelfNbt(NbtCompound compound) {
        if (!this.isTamed()) {
            return super.saveSelfNbt(compound);
        }

        var s = this.getSavedEntityId();
        compound.putString("id", s);
        this.writeNbt(compound);
        return true;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("BirdSitting", this.isSitting());
        compound.putBoolean("Launched", this.isLaunched());
        compound.putBoolean("HasCap", this.hasCap());
        compound.putInt("EagleCommand", this.getCommand());
        compound.putInt("LaunchTime", this.launchTime);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setSitting(compound.getBoolean("BirdSitting"));
        this.setLaunched(compound.getBoolean("Launched"));
        this.setCap(compound.getBoolean("HasCap"));
        this.setCommand(compound.getInt("EagleCommand"));
        this.launchTime = compound.getInt("LaunchTime");
    }

    @Override
    public void travel(Vec3d vec3d) {
        if (!this.shouldHoodedReturn() && this.hasCap() && this.isTamed() && !this.hasVehicle() || this.isSitting()) {
            super.travel(Vec3d.ZERO);
            return;
        }
        super.travel(vec3d);
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        if (this.attackProgress == 0 && this.dataTracker.get(ATTACK_TICK) == 0 && entityIn.isAlive()) {
            final double dist = this.isSitting() ? entityIn.getWidth() + 1 : entityIn.getWidth() + 5;
            if (this.distanceTo(entityIn) < dist) {
                this.dataTracker.set(ATTACK_TICK, 5);
            }
        }
        return true;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FLYING, false);
        this.dataTracker.startTracking(HAS_CAP, false);
        this.dataTracker.startTracking(TACKLING, false);
        this.dataTracker.startTracking(LAUNCHED, false);
        this.dataTracker.startTracking(ATTACK_TICK, 0);
        this.dataTracker.startTracking(COMMAND, 0);
        this.dataTracker.startTracking(SITTING, false);
    }

    @Override
    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    @Override
    public void setSitting(boolean sit) {
        this.dataTracker.set(SITTING, sit);
    }

    public int getCommand() {
        return this.dataTracker.get(COMMAND);
    }

    public void setCommand(int command) {
        this.dataTracker.set(COMMAND, command);
    }

    public boolean isLaunched() {
        return this.dataTracker.get(LAUNCHED);
    }

    public void setLaunched(boolean flying) {
        this.dataTracker.set(LAUNCHED, flying);
    }

    public boolean isFlying() {
        return this.dataTracker.get(FLYING);
    }

    public void setFlying(boolean flying) {
        if (flying && this.isBaby()) {
            flying = false;
        }
        this.dataTracker.set(FLYING, flying);
    }

    public boolean hasCap() {
        return this.dataTracker.get(HAS_CAP);
    }

    public void setCap(boolean cap) {
        this.dataTracker.set(HAS_CAP, cap);
    }

    public boolean isTackling() {
        return this.dataTracker.get(TACKLING);
    }

    public void setTackling(boolean tackling) {
        this.dataTracker.set(TACKLING, tackling);
    }

    @Override
    public void followEntity(TameableEntity tameable, LivingEntity owner, double followSpeed) {
        if (this.distanceTo(owner) > 15) {
            this.setFlying(true);
            this.getMoveControl().moveTo(owner.getX(), owner.getY() + owner.getHeight(), owner.getZ(), followSpeed);
        } else {
            if (this.isFlying() && !this.isOverWaterOrVoid()) {
                BlockPos vec = this.getCrowGround(this.getBlockPos());
                if (vec != null) {
                    this.getMoveControl().moveTo(vec.getX(), vec.getY(), vec.getZ(), followSpeed);
                }
                if (this.isOnGround()) {
                    this.setFlying(false);
                }
            } else {
                this.getNavigation().startMovingTo(owner, followSpeed);
            }
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.IN_WALL) || super.isInvulnerableTo(source);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        var itemstack = player.getStackInHand(hand);
        var item = itemstack.getItem();
        var type = super.interactMob(player, hand);
        if (itemstack.isIn(AMTagRegistry.BALD_EAGLE_FOODSTUFFS) && this.getHealth() < this.getMaxHealth()) {
            this.heal(10);
            if (!player.isCreative()) {
                itemstack.decrement(1);
            }
            this.getWorld().sendEntityStatus(this, (byte) 7);
            return ActionResult.CONSUME;
        } else if (itemstack.isIn(AMTagRegistry.BALD_EAGLE_TAMEABLES)) {
            if (itemstack.getItem().hasRecipeRemainder() && !player.getAbilities().creativeMode) {
                this.dropItem(itemstack.getItem().getRecipeRemainder());
            }
            if (!player.isCreative()) {
                itemstack.decrement(1);
            }
            if (random.nextBoolean()) {
                this.getWorld().sendEntityStatus(this, (byte) 7);
                this.setOwner(player);
                this.setCommand(1);
            } else {
                this.getWorld().sendEntityStatus(this, (byte) 6);
            }
            return ActionResult.CONSUME;
        } else if (isTamed() && !isBreedingItem(itemstack)) {
            if (!this.isBaby() && item == AMItemRegistry.FALCONRY_HOOD.get()) {
                if (!this.hasCap()) {
                    this.setCap(true);
                    if (!player.isCreative()) {
                        itemstack.decrement(1);
                    }
                    this.emitGameEvent(GameEvent.ENTITY_INTERACT);
                    this.playSound(SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, this.getSoundVolume(), this.getSoundPitch());
                    return ActionResult.SUCCESS;
                }
                //FIXME forge
//            } else if (itemstack.isIn(Tags.Items.SHEARS) && this.hasCap()) {
//                this.emitGameEvent(GameEvent.ENTITY_INTERACT);
//                this.playSound(SoundEvents.ENTITY_SHEEP_SHEAR, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
//                if (!this.getWorld().isClient) {
//                    if (player instanceof ServerPlayerEntity serverPlayerEntity) {
//                        itemstack.damage(1, random, serverPlayerEntity);
//                    }
//                }
//                this.dropItem(AMItemRegistry.FALCONRY_HOOD.get());
//                this.setCap(false);
//                return ActionResult.SUCCESS;
            } else if (!this.isBaby() && getRidingFalcons(player) <= 0 && (player.getStackInHand(Hand.MAIN_HAND).getItem() == AMItemRegistry.FALCONRY_GLOVE.get() || player.getStackInHand(Hand.OFF_HAND).getItem() == AMItemRegistry.FALCONRY_GLOVE.get())) {
                ridingCooldown = 30;
                this.setLaunched(false);
                this.removeAllPassengers();
                this.startRiding(player, true);
                if (!this.getWorld().isClient) {
                    AlexsMobs.sendMSGToAll(new MosquitoMountPlayerPacket(this.getId(), player.getId()));
                }
                return ActionResult.SUCCESS;
            } else {
                var actionResult = itemstack.useOnEntity(player, this, hand);
                if (actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS) {
                    this.setCommand((this.getCommand() + 1) % 3);

                    if (this.getCommand() == 3) {
                        this.setCommand(0);
                    }
                    player.sendMessage(Text.translatable("entity.alexsmobs.all.command_" + this.getCommand(), this.getName()), true);
                    boolean sit = this.getCommand() == 2;
                    this.setSitting(sit);
                    return ActionResult.SUCCESS;
                }
            }
        }
        return type;
    }

    @Override
    public boolean shouldFollow() {
        return this.getCommand() == 1 && !isLaunched();
    }

    @Override
    public void tickRiding() {
        var entity = this.getVehicle();
        if (this.hasVehicle() && (!entity.isAlive() || !this.isAlive())) {
            this.stopRiding();
        } else if (isTamed() && entity instanceof LivingEntity && isOwner((LivingEntity) entity)) {
            this.setVelocity(0, 0, 0);
            this.tick();
            if (this.hasVehicle()) {
                Entity mount = this.getVehicle();
                if (mount instanceof PlayerEntity playerEntity) {
                    float yawAdd = 0;
                    if (playerEntity.getStackInHand(Hand.MAIN_HAND).getItem() == AMItemRegistry.FALCONRY_GLOVE.get()) {
                        yawAdd = playerEntity.getMainArm() == Arm.LEFT ? 135 : -135;
                    } else if (playerEntity.getStackInHand(Hand.OFF_HAND).getItem() == AMItemRegistry.FALCONRY_GLOVE.get()) {
                        yawAdd = playerEntity.getMainArm() == Arm.LEFT ? -135 : 135;
                    } else {
                        this.setCommand(2);
                        this.setSitting(true);
                        this.dismountVehicle();
                        this.copyPositionAndRotation(mount);
                    }
                    float birdYaw = yawAdd * 0.5F;
                    this.bodyYaw = MathHelper.wrapDegrees(((LivingEntity) mount).bodyYaw + birdYaw);
                    this.setYaw(MathHelper.wrapDegrees(mount.getYaw() + birdYaw));
                    this.headYaw= MathHelper.wrapDegrees(((LivingEntity) mount).headYaw+ birdYaw);
                    float radius = 0.6F;
                    float angle = (Maths.STARTING_ANGLE * (((LivingEntity) mount).bodyYaw - 180F + yawAdd));
                    double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
                    double extraZ = radius * MathHelper.cos(angle);
                    this.setPos(mount.getX() + extraX, Math.max(mount.getY() + mount.getHeight() * 0.45F, mount.getY()), mount.getZ() + extraZ);
                }
                if (!mount.isAlive()) {
                    this.dismountVehicle();
                }
            }
        } else {
            super.tickRiding();
        }
    }

    @Override
    public void tick() {
        super.tick();

        this.prevAttackProgress = attackProgress;
        this.prevBirdPitch = birdPitch;
        this.prevTackleProgress = tackleProgress;
        this.prevFlyProgress = flyProgress;
        this.prevFlapAmount = flapAmount;
        this.prevSwoopProgress = swoopProgress;
        this.prevSitProgress = sitProgress;
        float yMot = -((float) this.getVelocity().y * MathHelper.DEGREES_PER_RADIAN);
        this.birdPitch = yMot;

        if (isFlying()) {
            if (flyProgress < 5F)
                flyProgress++;
        } else {
            if (flyProgress > 0F)
                flyProgress--;
        }

        if (isTackling()) {
            if (tackleProgress < 5F)
                tackleProgress++;
        } else {
            if (tackleProgress > 0F)
                tackleProgress--;
        }

        final boolean sit = isSitting() || this.hasVehicle();
        if (sit) {
            if (sitProgress < 5F)
                sitProgress++;
        } else {
            if (sitProgress > 0F)
                sitProgress--;
        }

        if (this.isLaunched()) {
            launchTime++;
        } else {
            launchTime = 0;
        }

        if (lastPlayerControlTime > 0) {
            lastPlayerControlTime--;
        }
        if (lastPlayerControlTime <= 0) {
            controlledFlag = false;
        }

        if (yMot < 0.1F) {
            flapAmount = Math.min(-yMot * 0.2F, 1F);
            if (swoopProgress > 0) {
                swoopProgress--;
            }
        } else {
            if (flapAmount > 0.0F) {
                flapAmount -= Math.min(flapAmount, 0.1F);
            } else {
                flapAmount = 0;
            }
            if (swoopProgress < yMot * 0.2F) {
                swoopProgress = Math.min(yMot * 0.2F, swoopProgress + 1);
            }
        }

        if (this.isTackling()) {
            flapAmount = Math.min(2, flapAmount + 0.2F);
        }

        if (!this.getWorld().isClient) {
            if (isFlying()) {
                if (this.isLandNavigator)
                    switchNavigator(false);
            } else {
                if (!this.isLandNavigator)
                    switchNavigator(true);
            }

            if (tackleCapCooldown == 0 && this.isTackling() && !this.hasPassengers() && (this.getTarget() == null || !this.getTarget().isAlive())) {
                this.setTackling(false);
            }

            if (isFlying()) {
                timeFlying++;
                this.setNoGravity(true);
                if (this.isSitting() || this.hasVehicle() || this.isInLove()) {
                    if (!isLaunched()) {
                        this.setFlying(false);
                    }
                }
                if (this.getTarget() != null && this.getTarget().getY() < this.getX() && !this.hasPassengers()) {
                    this.setVelocity(this.getVelocity().multiply(1.0, 0.9, 1.0));
                }
            } else {
                timeFlying = 0;
                this.setNoGravity(false);
            }

            if (this.isInsideWaterOrBubbleColumn() && this.hasPassengers()) {
                this.setVelocity(this.getVelocity().add(0, 0.1F, 0));
            }

            if (this.isSitting() && !this.isLaunched()) {
                this.setVelocity(this.getVelocity().add(0, -0.1F, 0));
            }

            if (this.getTarget() != null && this.isInsideWaterOrBubbleColumn()) {
                timeFlying = 0;
                this.setFlying(true);
            }

            if (this.isOnGround() && this.timeFlying > 30 && isFlying() && !this.isInsideWaterOrBubbleColumn()) {
                this.setFlying(false);
            }
        }

        final int attackTick = this.dataTracker.get(ATTACK_TICK);
        if (attackTick > 0) {
            if (attackTick == 2 && this.getTarget() != null && this.distanceTo(this.getTarget()) < this.getTarget().getWidth() + 2D) {
                this.getTarget().damage(this.getDamageSources().mobAttack(this), 2);
            }
            this.dataTracker.set(ATTACK_TICK, this.dataTracker.get(ATTACK_TICK) - 1);
            if (attackProgress < 5F) {
                attackProgress++;
            }
        } else {
            if (attackProgress > 0F) {
                attackProgress--;
            }
        }

        if (this.hasVehicle()) {
            this.setFlying(false);
            this.setTackling(false);
        }

        if (ridingCooldown > 0) {
            ridingCooldown--;
        }
        if (returnControlTime > 0) {
            returnControlTime--;
        }
        if (tackleCapCooldown > 0) {
            tackleCapCooldown--;
        }
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.BALD_EAGLE.get().create(world);
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    public Vec3d getBlockInViewAway(Vec3d fleePos, float radiusAdd) {
        final float radius = 0.75F * (0.7F * 6) * -3 - this.getRandom().nextInt(24) - radiusAdd;
        final float neg = this.getRandom().nextBoolean() ? 1 : -1;
        final float renderYawOffset = this.bodyYaw;
        final float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        final double extraZ = radius * MathHelper.cos(angle);
        final var radialPos = new BlockPos((int) (fleePos.x + extraX), 0, (int) (fleePos.z + extraZ));
        final var ground = getCrowGround(radialPos);
        final int distFromGround = (int) this.getY() - ground.getY();
        final int flightHeight = 7 + this.getRandom().nextInt(10);
        final var newPos = ground.up(distFromGround > 8 ? flightHeight : this.getRandom().nextInt(7) + 4);
        if (!this.isTargetBlocked(Vec3d.ofCenter(newPos)) && this.squaredDistanceTo(Vec3d.ofCenter(newPos)) > 1) {
            return Vec3d.ofCenter(newPos);
        }
        return null;
    }

    private BlockPos getCrowGround(BlockPos in) {
        var position = new BlockPos(in.getX(), (int) this.getY(), in.getZ());
        while (position.getY() < 320 && !getWorld().getFluidState(position).isEmpty()) {
            position = position.up();
        }
        while (position.getY() > -64 && !getWorld().getBlockState(position).isSolid()) {
            position = position.down();
        }
        return position;
    }

    public Vec3d getBlockGrounding(Vec3d fleePos) {
        final float radius = 0.75F * (0.7F * 6) * -3 - this.getRandom().nextInt(24);
        final float neg = this.getRandom().nextBoolean() ? 1 : -1;
        final float renderYawOffset = this.bodyYaw;
        final float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        final double extraZ = radius * MathHelper.cos(angle);
        final var radialPos = AMBlockPos.fromCoords(fleePos.x + extraX, getY(), fleePos.z + extraZ);
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

    public boolean isTargetBlocked(Vec3d target) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());

        return this.getWorld().raycast(new RaycastContext(Vector3d, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() != HitResult.Type.MISS;
    }

    private Vec3d getOrbitVec(Vec3d vector3d, float gatheringCircleDist) {
        final float angle = (Maths.STARTING_ANGLE * (float) this.orbitDist * (orbitClockwise ? -age : age));
        final double extraX = gatheringCircleDist * MathHelper.sin((angle));
        final double extraZ = gatheringCircleDist * MathHelper.cos(angle);
        if (this.orbitPos != null) {
            final var pos = new Vec3d(orbitPos.getX() + extraX, orbitPos.getY() + random.nextInt(2) - 2, orbitPos.getZ() + extraZ);
            if (this.getWorld().isAir(AMBlockPos.fromVec3(pos))) {
                return pos;
            }
        }
        return null;
    }

    private boolean isOverWaterOrVoid() {
        var position = this.getBlockPos();
        while (position.getY() > -64 && getWorld().isAir(position)) {
            position = position.down();
        }
        return !getWorld().getFluidState(position).isEmpty() || position.getY() <= -64;
    }

    @Override
    public void updatePassengerPosition(Entity passenger, Entity.PositionUpdater moveFunc) {
        if (this.hasPassenger(passenger)) {
            final float radius = 0.3F;
            final float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
            final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            final double extraZ = radius * MathHelper.cos(angle);
            passenger.setYaw(this.bodyYaw + 90F);
            if (passenger instanceof LivingEntity livingEntity) {
                livingEntity.bodyYaw = this.bodyYaw + 90F;
            }
            float extraY = 0F;
            if (passenger instanceof FishEntity && !passenger.isInsideWaterOrBubbleColumn()) {
                extraY = 0.1F;
            }
            moveFunc.accept(passenger, this.getX() + extraX, this.getY() - 0.3F + extraY + passenger.getHeight() * 0.3F, this.getZ() + extraZ);
            passengerTimer++;
            if (this.isAlive() && passengerTimer > 0 && passengerTimer % 40 == 0) {
                passenger.damage(this.getDamageSources().mobAttack(this), 1);
            }
        }
    }

    @Override
    public Vec3d updatePassengerForDismount(LivingEntity livingEntity) {
        return new Vec3d(this.getX(), this.getBoundingBox().minY, this.getZ());
    }

    public boolean shouldHoodedReturn() {
        if (this.getOwner() != null) {
            if (!this.getOwner().isAlive() || this.getOwner().isSneaking()) {
                return true;
            }
        }
        return !this.isAlive() || this.inNetherPortal || launchTime > 12000 || this.netherPortalTime > 0 || this.isRemoved();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.lastPlayerControlTime == 0 && !this.hasVehicle()) {
            super.remove(reason);
        }
    }

    public void directFromPlayer(float rotationYaw, float rotationPitch, boolean loadChunk, Entity over) {
        final Entity owner = this.getOwner();
        if (owner != null && this.distanceTo(owner) > 150) {
            returnControlTime = 100;
        }
        if (Math.abs(prevX - this.getX()) > 0.1F || Math.abs(prevY - this.getY()) > 0.1F || Math.abs(prevZ - this.getZ()) > 0.1F) {
            stillTicksCounter = 0;
        } else {
            stillTicksCounter++;
        }
        int stillTPthreshold = AMConfig.falconryTeleportsBack ? 200 : 6000;
        this.setSitting(false);
        this.setLaunched(true);
        if (owner != null && (returnControlTime > 0 && AMConfig.falconryTeleportsBack || stillTicksCounter > stillTPthreshold && this.distanceTo(owner) > 30)) {
            this.copyPositionAndRotation(owner);
            returnControlTime = 0;
            stillTicksCounter = 0;
            launchTime = Math.max(launchTime, 12000);
        }
        if (!this.getWorld().isClient) {
            if (returnControlTime > 0 && owner != null) {
                this.getLookControl().lookAt(owner, 30, 30);
            } else {
                this.bodyYaw = rotationYaw;
                this.setYaw(rotationYaw);
                this.headYaw= rotationYaw;
                this.setPitch(rotationPitch);
            }
            if (rotationPitch < 10 && this.isOnGround()) {
                this.setFlying(true);
            }
            final float yawOffset = rotationYaw + 90;
            final float rad = 3F;
            final float speed = 1.2F;
            if (returnControlTime > 0) {
                this.getMoveControl().moveTo(owner.getX(), owner.getY() + 10, owner.getZ(), speed);
            } else {
                this.getMoveControl().moveTo(this.getX() + rad * 1.5F * Math.cos(yawOffset * MathHelper.RADIANS_PER_DEGREE), this.getY() - rad * Math.sin(rotationPitch * MathHelper.RADIANS_PER_DEGREE), this.getZ() + rad * Math.sin(yawOffset * MathHelper.RADIANS_PER_DEGREE), speed);
            }
            if (loadChunk) {
                loadChunkOnServer(this.getBlockPos());
            }
            this.setAttacker(null);
            this.setTarget(null);
            if (over == null) {
                final List<Entity> list = this.getWorld().getOtherEntities(this, this.getBoundingBox().expand(3.0D), EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR);
                Entity closest = null;
                for (final Entity e : list) {
                    if (closest == null || this.distanceTo(e) < this.distanceTo(closest)) {
                        closest = e;
                    }
                }
                over = closest;
            }
        }
        if (over != null && over != owner && !this.isTeammate(over) && canFalconryAttack(over)) {
            if (tackleCapCooldown == 0 && this.distanceTo(over) <= over.getWidth() + 4D) {
                this.setTackling(true);
                if (this.distanceTo(over) <= over.getWidth() + 2D) {
                    final float speedDamage = (float) Math.ceil(MathHelper.clamp(this.getVelocity().length() + 0.2, 0, 1.2D) * 3.333);
                    over.damage(this.getDamageSources().mobAttack(this), 5 + speedDamage + random.nextInt(2));
                    tackleCapCooldown = 22;
                }
            }
        }
        this.lastPlayerControlTime = 10;
        this.controlledFlag = true;
    }

    @Override
    public float getHandOffset() {
        return 0.8F;
    }

    private boolean canFalconryAttack(Entity over) {
        return !(over instanceof ItemEntity) && (!(over instanceof LivingEntity) || !this.isOwner((LivingEntity) over));
    }

    //killEntity
    @Override
    public void updateKilledAdvancementCriterion(Entity entity, int score, DamageSource src) {
        if (this.isLaunched() && this.hasCap() && this.isTamed() && this.getOwner() != null) {
            if (this.getOwner() instanceof ServerPlayerEntity owner && this.distanceTo(this.getOwner()) >= 100) {
                AMAdvancementTriggerRegistry.BALD_EAGLE_CHALLENGE.trigger(owner);
            }
        }
        super.updateKilledAdvancementCriterion(entity, score, src);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            final var entity = source.getAttacker();
            if (entity != null && this.isTamed() && !(entity instanceof PlayerEntity) && !(entity instanceof PersistentProjectileEntity) && this.isLaunched()) {
                amount = (amount + 1.0F) / 4.0F;
            }
            return super.damage(source, amount);
        }
    }

    public void loadChunkOnServer(BlockPos center) {
        if (!this.getWorld().isClient) {
            ServerWorld serverWorld = (ServerWorld) getWorld();
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    var pos = new ChunkPos(this.getBlockPos().add(i * 16, 0, j * 16));
                    serverWorld.setChunkForced(pos.x, pos.z, true);
                }
            }
        }
    }

    @Override
    public void onLaunch(PlayerEntity player, Entity pointedEntity) {
        this.setLaunched(true);
        this.setSitting(false);
        this.setCommand(0);
        if (this.hasCap()) {
            this.setFlying(true);
            this.getMoveControl().moveTo(this.getX(), this.getY(), this.getZ(), 0.1F);
            if (this.getWorld().isClient) {
                AlexsMobs.sendMSGToServer(new MosquitoDismountPacket(this.getId(), player.getId()));
            }
            //FIXME
//            AlexsMobs.PROXY.setRenderViewEntity(this);
        } else {
            this.getNavigation().stop();
            this.getMoveControl().moveTo(this.getX(), this.getY(), this.getZ(), 0.1F);
            if (pointedEntity != null && pointedEntity.isAlive() && !this.isTeammate(pointedEntity)) {
                this.setFlying(true);
                if (pointedEntity instanceof final LivingEntity pointedLivingEntity) {
                    this.setTarget(pointedLivingEntity);
                }
            } else {
                this.setFlying(false);
                this.setCommand(2);
                this.setSitting(true);
            }
        }
    }

    @Override
    public EntityView method_48926() {
        return this.getWorld();
    }

    static class MoveHelper extends MoveControl {
        private final EntityBaldEagle parentEntity;

        public MoveHelper(EntityBaldEagle bird) {
            super(bird);
            this.parentEntity = bird;
        }

        @Override
        public void tick() {
            if (this.state == MoveControl.State.MOVE_TO) {
                final var vector3d = new Vec3d(this.targetX - parentEntity.getX(), this.targetY - parentEntity.getY(), this.targetZ - parentEntity.getZ());
                final double d5 = vector3d.length();
                if (d5 < 0.3) {
                    this.state = MoveControl.State.WAIT;
                    parentEntity.setVelocity(parentEntity.getVelocity().multiply(0.5D));
                } else {
                    parentEntity.setVelocity(parentEntity.getVelocity().add(vector3d.multiply(this.speed * 0.05D / d5)));
                    final var vector3d1 = parentEntity.getVelocity();
                    parentEntity.setYaw(-((float) MathHelper.atan2(vector3d1.x, vector3d1.z)) * MathHelper.DEGREES_PER_RADIAN);
                    parentEntity.bodyYaw = parentEntity.getYaw();
                }
            }
        }

        private boolean canReach(Vec3d p_220673_1_, int p_220673_2_) {
            var axisalignedbb = this.parentEntity.getBoundingBox();

            for (int i = 1; i < p_220673_2_; ++i) {
                axisalignedbb = axisalignedbb.offset(p_220673_1_);
                if (!this.parentEntity.getWorld().isSpaceEmpty(this.parentEntity, axisalignedbb)) {
                    return false;
                }
            }

            return true;
        }
    }

    private class AIWanderIdle extends Goal {
        protected final EntityBaldEagle eagle;
        protected double x;
        protected double y;
        protected double z;
        private boolean flightTarget = false;
        private int orbitResetCooldown = 0;
        private int maxOrbitTime = 360;
        private int orbitTime = 0;

        public AIWanderIdle() {
            super();
            this.setControls(EnumSet.of(Control.MOVE));
            this.eagle = EntityBaldEagle.this;
        }

        @Override
        public boolean canStart() {
            if (orbitResetCooldown < 0) {
                orbitResetCooldown++;
            }
            if ((eagle.getTarget() != null && eagle.getTarget().isAlive() && !this.eagle.hasPassengers()) || this.eagle.hasVehicle() || this.eagle.isSitting() || eagle.controlledFlag) {
                return false;
            } else {
                if (this.eagle.getRandom().nextInt(15) != 0 && !eagle.isFlying()) {
                    return false;
                }
                if (this.eagle.isBaby()) {
                    this.flightTarget = false;
                } else if (this.eagle.isInsideWaterOrBubbleColumn()) {
                    this.flightTarget = true;
                } else if (this.eagle.hasCap()) {
                    this.flightTarget = false;
                } else if (this.eagle.isOnGround()) {
                    this.flightTarget = random.nextBoolean();
                } else {
                    if (orbitResetCooldown == 0 && random.nextInt(6) == 0) {
                        orbitResetCooldown = 400;
                        eagle.orbitPos = eagle.getBlockPos();
                        eagle.orbitDist = 4 + random.nextInt(5);
                        eagle.orbitClockwise = random.nextBoolean();
                        orbitTime = 0;
                        maxOrbitTime = (int) (360 + 360 * random.nextFloat());
                    }
                    this.flightTarget = eagle.hasPassengers() || random.nextInt(7) > 0 && eagle.timeFlying < 700;
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
            if (orbitResetCooldown > 0) {
                orbitResetCooldown--;
            }
            if (orbitResetCooldown < 0) {
                orbitResetCooldown++;
            }
            if (orbitResetCooldown > 0 && eagle.orbitPos != null) {
                if (orbitTime < maxOrbitTime && !eagle.isInsideWaterOrBubbleColumn()) {
                    orbitTime++;
                } else {
                    orbitTime = 0;
                    eagle.orbitPos = null;
                    orbitResetCooldown = -400 - random.nextInt(400);
                }
            }
            if (eagle.horizontalCollision && !eagle.isOnGround()) {
                stop();
            }
            if (flightTarget) {
                eagle.getMoveControl().moveTo(x, y, z, 1F);
            } else {
                if (!eagle.isOnGround() && eagle.isFlying()) {
                    if (!eagle.isInsideWaterOrBubbleColumn()) {
                        eagle.setVelocity(eagle.getVelocity().multiply(1.2F, 0.6F, 1.2F));
                    }
                } else {
                    this.eagle.getNavigation().startMovingTo(this.x, this.y, this.z, 1F);
                }
            }
            if (!flightTarget && eagle.isOnGround() && isFlying()) {
                eagle.setFlying(false);
                orbitTime = 0;
                eagle.orbitPos = null;
                orbitResetCooldown = -400 - random.nextInt(400);
            }
            if (eagle.timeFlying > 30 && isFlying() && (!getWorld().isAir(eagle.getVelocityAffectingPos()) || eagle.isOnGround()) && !eagle.isInsideWaterOrBubbleColumn()) {
                eagle.setFlying(false);
                orbitTime = 0;
                eagle.orbitPos = null;
                orbitResetCooldown = -400 - random.nextInt(400);
            }
        }

        @Nullable
        protected Vec3d getPosition() {
            var vector3d = eagle.getPos();
            if (eagle.isTamed() && eagle.getCommand() == 1 && eagle.getOwner() != null) {
                vector3d = eagle.getOwner().getPos();
                eagle.orbitPos = eagle.getOwner().getBlockPos();
            }
            if (orbitResetCooldown > 0 && eagle.orbitPos != null) {
                return eagle.getOrbitVec(vector3d, 4 + random.nextInt(2));
            }
            if (eagle.hasPassengers() || eagle.isOverWaterOrVoid()) {
                flightTarget = true;
            }
            if (flightTarget) {
                if (eagle.timeFlying < 500 || eagle.hasPassengers() || eagle.isOverWaterOrVoid()) {
                    return eagle.getBlockInViewAway(vector3d, 0);
                } else {
                    return eagle.getBlockGrounding(vector3d);
                }
            } else {
                return FuzzyTargeting.find(this.eagle, 10, 7);
            }
        }

        @Override
        public boolean shouldContinue() {
            if (eagle.isSitting()) {
                return false;
            }
            if (flightTarget) {
                return eagle.isFlying() && eagle.squaredDistanceTo(x, y, z) > 2F;
            } else {
                return (!this.eagle.getNavigation().isIdle()) && !this.eagle.hasPassengers();
            }
        }

        @Override
        public void start() {
            if (flightTarget) {
                eagle.setFlying(true);
                eagle.getMoveControl().moveTo(x, y, z, 1F);
            } else {
                this.eagle.getNavigation().startMovingTo(this.x, this.y, this.z, 1F);
            }
        }

        @Override
        public void stop() {
            this.eagle.getNavigation().stop();
            super.stop();
        }
    }

    private class AITackle extends Goal {
        protected EntityBaldEagle eagle;
        private int circleTime;
        private int maxCircleTime = 10;

        public AITackle() {
            this.eagle = EntityBaldEagle.this;
        }

        @Override
        public boolean canStart() {
            return eagle.getTarget() != null && !eagle.controlledFlag && !eagle.hasPassengers();
        }

        @Override
        public void start() {
            eagle.orbitPos = null;
        }

        @Override
        public void stop() {
            circleTime = 0;
            maxCircleTime = 60 + random.nextInt(60);
        }

        @Override
        public void tick() {
            final LivingEntity target = eagle.getTarget();
            boolean smallPrey = target != null && target.getHeight() < 1F && target.getWidth() < 0.7F && !(target instanceof EntityBaldEagle) || target instanceof FishEntity;
            if (eagle.orbitPos != null && circleTime < maxCircleTime) {
                circleTime++;
                eagle.setTackling(false);
                eagle.setFlying(true);
                if (target != null) {
                    int i = 0;
                    final int up = 2 + eagle.getRandom().nextInt(4);
                    eagle.orbitPos = target.getBlockPos().up((int) (target.getHeight()));
                    while (eagle.getWorld().isAir(eagle.orbitPos) && i < up) {
                        i++;
                        eagle.orbitPos = eagle.orbitPos.up();
                    }
                }
                final var vec = eagle.getOrbitVec(Vec3d.ZERO, 4 + random.nextInt(2));
                if (vec != null) {
                    eagle.getMoveControl().moveTo(vec.x, vec.y, vec.z, 1.2F);
                }
            } else if (target != null) {
                if (eagle.isFlying() || eagle.isInsideWaterOrBubbleColumn()) {
                    final double d0 = eagle.getX() - target.getX();
                    final double d2 = eagle.getZ() - target.getZ();
                    final double xzDist = Math.sqrt(d0 * d0 + d2 * d2);
                    double yAddition = target.getHeight();
                    if (xzDist > 15D) {
                        yAddition = 3D;
                    }
                    eagle.setTackling(true);
                    eagle.getMoveControl().moveTo(target.getX(), target.getY() + yAddition, target.getZ(), eagle.isInsideWaterOrBubbleColumn() ? 1.3F : 1.0F);
                } else {
                    this.eagle.getNavigation().startMovingTo(target, 1F);
                }
                if (eagle.distanceTo(target) < target.getWidth() + 2.5F) {
                    if (eagle.isTackling()) {
                        if (smallPrey) {
                            eagle.setFlying(true);
                            eagle.timeFlying = 0;
                            final float radius = 0.3F;
                            final float angle = (Maths.STARTING_ANGLE * eagle.bodyYaw);
                            final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
                            final double extraZ = radius * MathHelper.cos(angle);
                            target.setYaw(eagle.bodyYaw + 90F);
                            target.bodyYaw = eagle.bodyYaw + 90F;
                            target.setPos(eagle.getX() + extraX, eagle.getY() - 0.4F + target.getHeight() * 0.45F, eagle.getZ() + extraZ);
                            target.startRiding(eagle, true);
                        } else {
                            target.damage(eagle.getDamageSources().mobAttack(eagle), 5);
                            eagle.setFlying(false);
                            eagle.orbitPos = target.getBlockPos().up(2);
                            circleTime = 0;
                            maxCircleTime = 60 + random.nextInt(60);
                        }
                    } else {
                        eagle.tryAttack(target);
                    }
                } else if (eagle.distanceTo(target) > 12 || target.isInsideWaterOrBubbleColumn()) {
                    eagle.setFlying(true);
                }
            }
            if (eagle.isLaunched()) {
                eagle.setFlying(true);
            }
        }
    }

    private class AILandOnGlove extends Goal {
        protected EntityBaldEagle eagle;
        private int seperateTime = 0;

        public AILandOnGlove() {
            this.eagle = EntityBaldEagle.this;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return eagle.isLaunched() && !eagle.controlledFlag && eagle.isTamed() && !eagle.hasVehicle() && !eagle.hasPassengers() && (eagle.getTarget() == null || !eagle.getTarget().isAlive());
        }

        @Override
        public void tick() {
            if (eagle.getVelocity().lengthSquared() < 0.03D) {
                seperateTime++;
            }
            final LivingEntity owner = eagle.getOwner();
            if (owner != null) {
                if (seperateTime > 200) {
                    seperateTime = 0;
                    eagle.copyPositionAndRotation(owner);
                }
                eagle.setFlying(true);
                final double d0 = eagle.getX() - owner.getX();
                final double d2 = eagle.getZ() - owner.getZ();
                final double xzDist = Math.sqrt(d0 * d0 + d2 * d2);
                final double yAdd = xzDist > 14 ? 5 : 0;
                eagle.getMoveControl().moveTo(owner.getX(), owner.getY() + yAdd + owner.getStandingEyeHeight(), owner.getZ(), 1);

                if (this.eagle.distanceTo(owner) < owner.getWidth() + 1.4D) {
                    this.eagle.setLaunched(false);
                    if (this.eagle.getRidingFalcons(owner) <= 0) {
                        this.eagle.startRiding(owner);
                        if (!eagle.getWorld().isClient) {
                            AlexsMobs.sendMSGToAll(new MosquitoMountPlayerPacket(eagle.getId(), owner.getId()));
                        }
                    } else {
                        this.eagle.setCommand(2);
                        this.eagle.setSitting(true);
                    }
                }
            }
        }

        @Override
        public void stop() {
            seperateTime = 0;
        }
    }
}
