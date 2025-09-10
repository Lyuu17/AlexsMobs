package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIHurtByTargetNotBaby;
import com.github.alexthe666.alexsmobs.entity.ai.DirectPathNavigator;
import com.github.alexthe666.alexsmobs.entity.ai.EntityAINearestTarget3D;
import com.github.alexthe666.alexsmobs.entity.ai.FlyingAIFollowOwner;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.misc.IngredientUtil;
import com.github.alexthe666.alexsmobs.packet.TarantulaHawkStingPacket;
import com.github.alexthe666.alexsmobs.registry.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class EntityTarantulaHawk extends TameableEntity implements IFollower {

    public static final int STING_DURATION = 2400;
    protected static final EntityDimensions FLIGHT_SIZE = EntityDimensions.fixed(0.9F, 1.5F);
    private static final TrackedData<Float> FLY_ANGLE = DataTracker.registerData(EntityTarantulaHawk.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> NETHER = DataTracker.registerData(EntityTarantulaHawk.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityTarantulaHawk.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> DRAGGING = DataTracker.registerData(EntityTarantulaHawk.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> FLYING = DataTracker.registerData(EntityTarantulaHawk.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> DIGGING = DataTracker.registerData(EntityTarantulaHawk.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SCARED = DataTracker.registerData(EntityTarantulaHawk.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> ATTACK_TICK = DataTracker.registerData(EntityTarantulaHawk.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> COMMAND = DataTracker.registerData(EntityTarantulaHawk.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> ANGRY = DataTracker.registerData(EntityTarantulaHawk.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float prevFlyAngle;
    public float prevSitProgress;
    public float sitProgress;
    public float prevDragProgress;
    public float dragProgress;
    public float prevFlyProgress;
    public float flyProgress;
    public float prevAttackProgress;
    public float attackProgress;
    public float prevDigProgress;
    public float digProgress;
    private boolean isLandNavigator;
    private boolean flightSize = false;
    private int timeFlying = 0;
    private boolean bredBuryFlag = false;
    private int spiderFeedings = 0;
    private int dragTime = 0;

    public EntityTarantulaHawk(EntityType<? extends EntityTarantulaHawk> type, World worldIn) {
        super(type, worldIn);
        switchNavigator(false);
    }

    public static boolean canTarantulaHawkSpawn(EntityType<? extends AnimalEntity> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return worldIn.getBlockState(pos.down()).isIn(AMTagRegistry.TARANTULA_HAWK_SPAWNS) && worldIn.getBaseLightLevel(pos, 0) > 8 || isBiomeNether(worldIn, pos) || AMConfig.fireproofTarantulaHawk;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 18.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.tarantulaHawkSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        if (isBiomeNether(worldIn, this.getBlockPos())){
            this.setNether(true);
        }
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    private static boolean isBiomeNether(WorldAccess worldIn, BlockPos position) {
        return worldIn.getBiome(position).isIn(AMTagRegistry.SPAWNS_NETHER_TARANTULA_HAWKS);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new SitGoal(this));
        this.goalSelector.add(2, new FlyingAIFollowOwner(this, 1.0D, 10.0F, 2.0F, false));
        this.goalSelector.add(3, new AIFleeRoadrunners());
        this.goalSelector.add(4, new AIMelee());
        this.goalSelector.add(5, new AIBury());
        this.goalSelector.add(6, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(7, new TemptGoal(this, 1.1D, IngredientUtil.ingredientFromTags(AMTagRegistry.TARANTULA_HAWK_BREEDABLES, AMTagRegistry.TARANTULA_HAWK_TAMEABLES, AMTagRegistry.TARANTULA_HAWK_FOODSTUFFS), false));
        this.goalSelector.add(8, new AIWalkIdle());
        this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(10, new LookAroundGoal(this));
        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, (new AnimalAIHurtByTargetNotBaby(this)));
        this.targetSelector.add(4, new EntityAINearestTarget3D<>(this, SpiderEntity.class, 15, true, true, null) {
            @Override
            public boolean canStart() {
                return super.canStart() && !EntityTarantulaHawk.this.isBaby() && !EntityTarantulaHawk.this.isSitting();
            }
        });
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.TARANTULA_HAWK_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.TARANTULA_HAWK_HURT.get();
    }

    @Override
    public boolean isFireImmune() {
        return isNether() || AMConfig.fireproofTarantulaHawk;
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new MobNavigation(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new MoveController();
            this.navigation = new DirectPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FLY_ANGLE, 0F);
        this.dataTracker.startTracking(NETHER, false);
        this.dataTracker.startTracking(FLYING, false);
        this.dataTracker.startTracking(SITTING, false);
        this.dataTracker.startTracking(DRAGGING, false);
        this.dataTracker.startTracking(DIGGING, false);
        this.dataTracker.startTracking(SCARED, false);
        this.dataTracker.startTracking(ANGRY, false);
        this.dataTracker.startTracking(ATTACK_TICK, 0);
        this.dataTracker.startTracking(COMMAND, 0);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.getAttacker() instanceof LivingEntity && ((LivingEntity) source.getAttacker()).getGroup() == EntityGroup.ARTHROPOD && ((LivingEntity) source.getAttacker()).hasStatusEffect(AMEffectRegistry.DEBILITATING_STING.get())) {
            return false;
        }
        return super.damage(source, amount);
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.ARTHROPOD;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("HawkSitting", this.isSitting());
        compound.putBoolean("Nether", this.isNether());
        compound.putBoolean("Digging", this.isDigging());
        compound.putBoolean("Flying", this.isFlying());
        compound.putInt("Command", this.getCommand());
        compound.putInt("SpiderFeedings", this.spiderFeedings);
        compound.putBoolean("BreedFlag", this.bredBuryFlag);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setSitting(compound.getBoolean("HawkSitting"));
        this.setNether(compound.getBoolean("Nether"));
        this.setDigging(compound.getBoolean("Digging"));
        this.setFlying(compound.getBoolean("Flying"));
        this.setCommand(compound.getInt("Command"));
        this.spiderFeedings = compound.getInt("SpiderFeedings");
        this.bredBuryFlag = compound.getBoolean("BreedFlag");
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

    public float getFlyAngle() {
        return this.dataTracker.get(FLY_ANGLE);
    }

    public void setFlyAngle(float progress) {
        this.dataTracker.set(FLY_ANGLE, progress);
    }

    public boolean isFlying() {
        return this.dataTracker.get(FLYING);
    }

    public void setFlying(boolean flying) {
        if (flying && isBaby()) {
            return;
        }
        this.dataTracker.set(FLYING, flying);
    }

    public boolean isNether() {
        return this.dataTracker.get(NETHER);
    }

    public void setNether(boolean sit) {
        this.dataTracker.set(NETHER, sit);
    }

    public boolean isScared() {
        return this.dataTracker.get(SCARED);
    }

    public void setScared(boolean sit) {
        this.dataTracker.set(SCARED, sit);
    }

    @Override
    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    @Override
    public void setSitting(boolean sit) {
        this.dataTracker.set(SITTING, sit);
    }

    public boolean isDragging() {
        return this.dataTracker.get(DRAGGING);
    }

    public void setDragging(boolean sit) {
        this.dataTracker.set(DRAGGING, sit);
    }

    public boolean isDigging() {
        return this.dataTracker.get(DIGGING);
    }

    public void setDigging(boolean sit) {
        this.dataTracker.set(DIGGING, sit);
    }

    @Override
    public EntityDimensions getDimensions(EntityPose poseIn) {
        return isFlying() && !isBaby() ? FLIGHT_SIZE : super.getDimensions(poseIn);
    }

    @Override
    public void tick() {
        prevFlyAngle = this.getFlyAngle();
        super.tick();
        prevAttackProgress = attackProgress;
        prevFlyProgress = flyProgress;
        prevSitProgress = sitProgress;
        prevDragProgress = dragProgress;
        prevDigProgress = digProgress;

        final boolean flying = this.isFlying();
        final boolean sitting = this.isSitting();
        final boolean dragging = this.isDragging();
        final boolean digging = this.isDigging();

        if (flying) {
            if (flyProgress < 5F)
                flyProgress++;
        } else {
            if (flyProgress > 0F)
                flyProgress--;
        }

        if (sitting) {
            if (sitProgress < 5F)
                sitProgress++;
        } else {
            if (sitProgress > 0F)
                sitProgress--;
        }

        if (dragging) {
            if (dragProgress < 5F)
                dragProgress++;
        } else {
            if (dragProgress > 0F)
                dragProgress--;
        }

        if (digging) {
            if (digProgress < 5F)
                digProgress++;
        } else {
            if (digProgress > 0F)
                digProgress--;
        }

        if (flightSize && !flying) {
            this.calculateDimensions();
            flightSize = false;
        }
        if (!flightSize && isFlying()) {
            this.calculateDimensions();
            flightSize = true;
        }
        float threshold = 0.015F;
        if (isFlying() && this.prevYaw - this.getYaw() > threshold) {
            this.setFlyAngle(this.getFlyAngle() + 5);
        } else if (isFlying() && this.prevYaw - this.getYaw() < -threshold) {
            this.setFlyAngle(this.getFlyAngle() - 5);
        } else if (this.getFlyAngle() > 0) {
            this.setFlyAngle(Math.max(this.getFlyAngle() - 4, 0));
        } else if (this.getFlyAngle() < 0) {
            this.setFlyAngle(Math.min(this.getFlyAngle() + 4, 0));
        }
        this.setFlyAngle(MathHelper.clamp(this.getFlyAngle(), -30, 30));
        if (!this.getWorld().isClient) {
            if (isFlying() && this.isLandNavigator) {
                switchNavigator(false);
            }
            if (!isFlying() && !this.isLandNavigator) {
                switchNavigator(true);
            }
            if (isFlying()) {
                if(timeFlying % 25 == 0){
                    this.playSound(AMSoundRegistry.TARANTULA_HAWK_WING.get(), this.getSoundVolume(), this.getSoundPitch());
                }
                timeFlying++;
                this.setNoGravity(true);
                if (this.isSitting() || this.hasVehicle() || this.isInLove()) {
                    this.setFlying(false);
                }
            } else {
                timeFlying = 0;
                this.setNoGravity(false);
            }
            if (this.getTarget() != null && this.getTarget() instanceof PlayerEntity && !this.isTamed()) {
                this.dataTracker.set(ANGRY, true);
            } else {
                this.dataTracker.set(ANGRY, false);
            }
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
        if (isDigging() && getWorld().getBlockState(this.getVelocityAffectingPos()).isOpaque()) {
            var posit = this.getVelocityAffectingPos();
            var understate = getWorld().getBlockState(posit);
            for (int i = 0; i < 4 + random.nextInt(2); i++) {
                final double particleX = posit.getX() + random.nextFloat();
                final double particleY = posit.getY() + 1F;
                final double particleZ = posit.getZ() + random.nextFloat();
                final double motX = this.random.nextGaussian() * 0.02D;
                final double motY = 0.1F + random.nextFloat() * 0.2F;
                final double motZ = this.random.nextGaussian() * 0.02D;
                getWorld().addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, understate), particleX, particleY, particleZ, motX, motY, motZ);
            }
        }
        if(this.age > 0 && this.age % 300 == 0 && this.getHealth() < this.getMaxHealth()){
            this.heal(1);
        }
        if(!this.getWorld().isClient && this.isDragging() && this.getPassengerList().isEmpty() && !this.isDigging()){
            dragTime++;
            if(dragTime > 5000){
                dragTime = 0;
                for(Entity e : this.getPassengerList()){
                    e.damage(this.getDamageSources().mobAttack(this), 10);
                }
                this.removeAllPassengers();
                this.setDragging(false);
            }
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        ActionResult type = super.interactMob(player, hand);
        if (!isTamed() && itemstack.isIn(AMTagRegistry.TARANTULA_HAWK_TAMEABLES)) {
            this.eat(player, hand, itemstack);
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_STRIDER_EAT, this.getSoundVolume(), this.getSoundPitch());
            spiderFeedings++;
            if (spiderFeedings >= 15 && getRandom().nextInt(6) == 0 || spiderFeedings > 25) {
                this.setOwner(player);
                this.getWorld().sendEntityStatus(this, (byte) 7);
            } else {
                this.getWorld().sendEntityStatus(this, (byte) 6);
            }
            return ActionResult.SUCCESS;
        }
        if (isTamed() && itemstack.isIn(AMTagRegistry.TARANTULA_HAWK_FOODSTUFFS)) {
            if (this.getHealth() < this.getMaxHealth()) {
                this.eat(player, hand, itemstack);
                this.emitGameEvent(GameEvent.EAT);
                this.playSound(SoundEvents.ENTITY_STRIDER_EAT, this.getSoundVolume(), this.getSoundPitch());
                this.heal(5);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;

        }
        var actionResult = itemstack.useOnEntity(player, this, hand);
        if (actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS && isTamed() && isOwner(player)) {
            if (player.isSneaking()) {
                if (this.getMainHandStack().isEmpty()) {
                    ItemStack cop = itemstack.copy();
                    cop.setCount(1);
                    this.setStackInHand(Hand.MAIN_HAND, cop);
                    itemstack.decrement(1);
                    return ActionResult.SUCCESS;
                } else {
                    this.dropStack(this.getMainHandStack().copy());
                    this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    return ActionResult.SUCCESS;
                }
            } else if (!isBreedingItem(itemstack)) {
                this.setCommand(this.getCommand() + 1);
                if (this.getCommand() == 3) {
                    this.setCommand(0);
                }
                player.sendMessage(Text.translatable("entity.alexsmobs.all.command_" + this.getCommand(), this.getName()), true);
                boolean sit = this.getCommand() == 2;
                this.setSitting(sit);
                return ActionResult.SUCCESS;
            }
        }
        return type;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return isTamed() && stack.isIn(AMTagRegistry.TARANTULA_HAWK_BREEDABLES);
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.CACTUS) || super.isInvulnerableTo(source);
    }

    @Override
    public void breed(ServerWorld world, AnimalEntity animalEntity) {
        bredBuryFlag = true;
        var serverplayerentity = this.getLovingPlayer();
        if (serverplayerentity == null && animalEntity.getLovingPlayer() != null) {
            serverplayerentity = animalEntity.getLovingPlayer();
        }

        if (serverplayerentity != null) {
            serverplayerentity.incrementStat(Stats.ANIMALS_BRED);
            Criteria.BRED_ANIMALS.trigger(serverplayerentity, this, animalEntity, this);
        }

        this.setBreedingAge(6000);
        animalEntity.setBreedingAge(6000);
        this.resetLoveTicks();
        animalEntity.resetLoveTicks();
        world.sendEntityStatus(this, (byte) 7);
        world.sendEntityStatus(this, (byte) 18);
        if (world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
            world.spawnEntity(new ExperienceOrbEntity(world, this.getX(), this.getY(), this.getZ(), this.getRandom().nextInt(7) + 1));
        }

    }

    @Override
    public void followEntity(TameableEntity tameable, LivingEntity owner, double followSpeed) {
        if (this.distanceTo(owner) > 5) {
            this.setFlying(true);
            this.getMoveControl().moveTo(owner.getX(), owner.getY() + owner.getHeight(), owner.getZ(), followSpeed);
        } else {
            if (this.isOnGround()) {
                this.setFlying(false);
            }
            if (this.isFlying() && !this.isOverWater()) {
                BlockPos vec = this.getCrowGround(this.getBlockPos());
                if (vec != null) {
                    this.getMoveControl().moveTo(vec.getX(), vec.getY(), vec.getZ(), followSpeed);
                }
            } else {
                this.getNavigation().startMovingTo(owner, followSpeed);
            }
        }
    }

    @Override
    public void updatePassengerPosition(Entity passenger, Entity.PositionUpdater moveFunc) {
        this.setPitch(0);
        float radius = 1.0F + passenger.getWidth() * 0.5F;
        float angle = (Maths.STARTING_ANGLE * (this.bodyYaw - 180));
        double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        double extraZ = radius * MathHelper.cos(angle);
        double extraY = 0;
        passenger.setPos(this.getX() + extraX, this.getY() + extraY, this.getZ() + extraZ);
    }

    private boolean isOverWater() {
        BlockPos position = this.getBlockPos();
        while (position.getY() > 0 && getWorld().isAir(position)) {
            position = position.down();
        }
        return !getWorld().getFluidState(position).isEmpty() || position.getY() <= 0;
    }

    public Vec3d getBlockInViewAway(Vec3d fleePos, float radiusAdd) {
        float radius = 0.75F * (0.7F * 6) * -3 - this.getRandom().nextInt(24) - radiusAdd;
        float neg = this.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = this.bodyYaw;
        float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = new BlockPos((int) (fleePos.x + extraX), 0, (int) (fleePos.z + extraZ));
        BlockPos ground = getCrowGround(radialPos);
        int distFromGround = (int) this.getY() - ground.getY();
        int flightHeight = 4 + this.getRandom().nextInt(10);
        BlockPos newPos = ground.up(distFromGround > 8 ? flightHeight : this.getRandom().nextInt(6) + 1);
        if (!this.isTargetBlocked(Vec3d.ofCenter(newPos)) && this.squaredDistanceTo(Vec3d.ofCenter(newPos)) > 1) {
            return Vec3d.ofCenter(newPos);
        }
        return null;
    }

    private BlockPos getCrowGround(BlockPos in) {
        var position = new BlockPos(in.getX(), (int) this.getY(), in.getZ());
        while (position.getY() > -64 && !getWorld().getBlockState(position).isSolid() && getWorld().getFluidState(position).isEmpty()) {
            position = position.down();
        }
        return position;
    }

    public Vec3d getBlockGrounding(Vec3d fleePos) {
        float radius = 0.75F * (0.7F * 6) * -3 - this.getRandom().nextInt(24);
        float neg = this.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = this.bodyYaw;
        float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = new BlockPos((int) (fleePos.x + extraX), (int) getY(), (int) (fleePos.z + extraZ));
        BlockPos ground = this.getCrowGround(radialPos);
        if (ground.getY() == -64) {
            return this.getPos();
        } else {
            ground = this.getBlockPos();
            while (ground.getY() > -62 && !getWorld().getBlockState(ground).isSolid()) {
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

    private Vec3d getOrbitVec(Vec3d vector3d, float gatheringCircleDist, boolean orbitClockwise) {
        float angle = (Maths.STARTING_ANGLE * (float) 2 * (orbitClockwise ? -age : age));
        double extraX = gatheringCircleDist * MathHelper.sin((angle));
        double extraZ = gatheringCircleDist * MathHelper.cos(angle);
        if (vector3d != null) {
            var pos = new Vec3d(vector3d.x + extraX, vector3d.y + random.nextInt(2) + 4, vector3d.z + extraZ);
            if (this.getWorld().isAir(AMBlockPos.fromVec3(pos))) {
                return pos;
            }
        }
        return null;
    }

    public int getCommand() {
        return this.dataTracker.get(COMMAND);
    }

    public void setCommand(int command) {
        this.dataTracker.set(COMMAND, Integer.valueOf(command));
    }

    private BlockPos genSandPos(BlockPos parent) {
        WorldAccess world = this.getWorld();
        var random = Random.create();
        int range = 24;
        for (int i = 0; i < 15; i++) {
            var sandAir = parent.add(random.nextInt(range) - range / 2, -5, random.nextInt(range) - range / 2);
            while (!world.isAir(sandAir) && sandAir.getY() < 255) {
                sandAir = sandAir.up();
            }
            BlockState state = world.getBlockState(sandAir.down());
            if (state.isIn(BlockTags.SAND)) {
                return sandAir.down();
            }
        }
        return null;
    }

    @Override
    public boolean shouldFollow() {
        return getCommand() == 1 && !this.isDragging() && !this.isDigging() && (this.getTarget() == null || !this.getTarget().isAlive());
    }

    public boolean isAngry() {
        return this.dataTracker.get(ANGRY);
    }

    @Override
    public EntityView method_48926() {
        return getWorld();
    }

    class MoveController extends MoveControl {
        private final MobEntity parentEntity;

        public MoveController() {
            super(EntityTarantulaHawk.this);
            this.parentEntity = EntityTarantulaHawk.this;
        }

        @Override
        public void tick() {
            if (this.state == MoveControl.State.MOVE_TO) {
                Vec3d vector3d = new Vec3d(this.targetX - parentEntity.getX(), this.targetY - parentEntity.getY(), this.targetZ - parentEntity.getZ());
                double d0 = vector3d.length();
                double width = parentEntity.getBoundingBox().getAverageSideLength();
                if (d0 < width) {
                    this.state = MoveControl.State.WAIT;
                    parentEntity.setVelocity(parentEntity.getVelocity().multiply(0.5D));
                } else {
                    final float angle = (Maths.STARTING_ANGLE * (parentEntity.bodyYaw + 90));
                    final float radius = (float) Math.sin(parentEntity.age * 0.2F) * 2;
                    final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
                    final double extraZ = radius * MathHelper.cos(angle);
                    var vector3d1 = vector3d.multiply(this.speed * 0.05D / d0);
                    var strafPlus = new Vec3d(extraX, 0, extraZ).multiply(0.003D * Math.min(d0, 100));
                    parentEntity.setVelocity(parentEntity.getVelocity().add(strafPlus));
                    parentEntity.setVelocity(parentEntity.getVelocity().add(vector3d1));
                    parentEntity.setYaw(-((float) MathHelper.atan2(vector3d1.x, vector3d1.z)) * MathHelper.DEGREES_PER_RADIAN);
                    if (!EntityTarantulaHawk.this.isDragging()) {
                        parentEntity.bodyYaw = parentEntity.getYaw();
                    }
                }

            }
        }
    }

    private class AIMelee extends Goal {
        private final EntityTarantulaHawk hawk;
        private int orbitCooldown = 0;
        private boolean clockwise = false;
        private Vec3d orbitVec = null;
        private BlockPos sandPos = null;

        public AIMelee() {
            hawk = EntityTarantulaHawk.this;
        }

        @Override
        public boolean canStart() {
            return hawk.getTarget() != null && !hawk.isSitting() && !hawk.isScared() && hawk.getTarget().isAlive() && !hawk.isDragging() && !hawk.isDigging() && !hawk.getTarget().noClip && !hawk.getTarget().hasVehicle();
        }

        @Override
        public void start() {
            hawk.setDragging(false);
            clockwise = random.nextBoolean();
        }

        @Override
        public void tick() {
            LivingEntity target = hawk.getTarget();
            boolean paralized = target != null && target.getGroup() == EntityGroup.ARTHROPOD && !target.noClip && target.hasStatusEffect(AMEffectRegistry.DEBILITATING_STING.get());
            boolean paralizedWithChild = paralized && target.getStatusEffect(AMEffectRegistry.DEBILITATING_STING.get()).getAmplifier() > 0;
            if (sandPos == null || !getWorld().getBlockState(sandPos).isIn(BlockTags.SAND)) {
                sandPos = hawk.genSandPos(target.getBlockPos());
            }
            if (orbitCooldown > 0) {
                orbitCooldown--;
                hawk.setFlying(true);
                if (target != null) {
                    if (orbitVec == null || hawk.squaredDistanceTo(orbitVec) < 4F || !hawk.getMoveControl().isMoving()) {
                        orbitVec = hawk.getOrbitVec(target.getPos().add(0, target.getHeight(), 0), 10 + random.nextInt(2), false);
                        if (orbitVec != null) {
                            hawk.getMoveControl().moveTo(orbitVec.x, orbitVec.y, orbitVec.z, 1F);
                        }
                    }
                }
            } else if (((paralized && !hawk.isTamed()) || (paralizedWithChild && hawk.bredBuryFlag)) && sandPos != null) {
                if (hawk.isOnGround()) {
                    hawk.setFlying(false);
                    hawk.getNavigation().startMovingTo(target, 1);
                } else {
                    Vec3d vector3d = hawk.getBlockGrounding(hawk.getPos());
                    if (vector3d != null && hawk.isFlying()) {
                        hawk.getMoveControl().moveTo(vector3d.x, vector3d.y, vector3d.z, 1F);
                    }
                }
                if (hawk.distanceTo(target) < target.getWidth() + 1.5F && !target.hasVehicle()) {
                    hawk.setDragging(true);
                    hawk.setFlying(false);
                    target.startRiding(hawk, true);
                }
            } else {
                if (target != null && !paralizedWithChild) {
                    double dist = hawk.distanceTo(target);
                    if (dist < 10 && !hawk.isFlying()) {
                        if (hawk.isOnGround()) {
                            hawk.setFlying(false);
                        }
                        hawk.getNavigation().startMovingTo(target, 1);
                    } else {
                        hawk.setFlying(true);
                        hawk.getMoveControl().moveTo(target.getX(), target.getEyeY(), target.getZ(), 1F);
                    }
                    if (dist < target.getWidth() + 2.5F) {
                        if (hawk.dataTracker.get(ATTACK_TICK) == 0 && hawk.attackProgress == 0) {
                            hawk.dataTracker.set(ATTACK_TICK, 7);
                        }
                        if (hawk.attackProgress == 5F) {
                            hawk.tryAttack(target);
                            if(hawk.bredBuryFlag){
                                if(target.getHealth() <= 1.0F){
                                    target.heal(5);
                                }
                            }
                            target.addStatusEffect(new StatusEffectInstance(AMEffectRegistry.DEBILITATING_STING.get(), target.getGroup() == EntityGroup.ARTHROPOD ? EntityTarantulaHawk.STING_DURATION : 600, hawk.bredBuryFlag ? 1 : 0));
                            if (!hawk.getWorld().isClient && target.getGroup() == EntityGroup.ARTHROPOD) {
                                AlexsMobs.sendMSGToAll(new TarantulaHawkStingPacket(hawk.getId(), target.getId()));
                            }
                            orbitCooldown = target.getGroup() == EntityGroup.ARTHROPOD ? 200 + random.nextInt(200) : 10 + random.nextInt(20);
                        }
                    }
                }
            }
        }

        @Override
        public void stop() {
            orbitCooldown = 0;
            hawk.bredBuryFlag = false;
            clockwise = random.nextBoolean();
            orbitVec = null;
            if(hawk.getPassengerList().isEmpty()){
                hawk.setTarget(null);
            }
        }
    }

    private class AIWalkIdle extends Goal {
        protected final EntityTarantulaHawk hawk;
        protected double x;
        protected double y;
        protected double z;
        private boolean flightTarget = false;

        public AIWalkIdle() {
            super();
            this.setControls(EnumSet.of(Control.MOVE));
            this.hawk = EntityTarantulaHawk.this;
        }

        @Override
        public boolean canStart() {
            if (this.hawk.hasPassengers() || hawk.isScared() || hawk.isDragging() || EntityTarantulaHawk.this.getCommand() == 1 || (hawk.getTarget() != null && hawk.getTarget().isAlive()) || this.hawk.hasVehicle() || this.hawk.isSitting()) {
                return false;
            } else {
                if (this.hawk.getRandom().nextInt(30) != 0 && !hawk.isFlying()) {
                    return false;
                }
                if (this.hawk.isOnGround()) {
                    this.flightTarget = random.nextBoolean();
                } else {
                    this.flightTarget = random.nextInt(5) > 0 && hawk.timeFlying < 200;
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
                hawk.getMoveControl().moveTo(x, y, z, 1F);
            } else {
                this.hawk.getNavigation().startMovingTo(this.x, this.y, this.z, 1F);
            }
            if (!flightTarget && isFlying() && hawk.isOnGround()) {
                hawk.setFlying(false);
            }
            if (isFlying() && hawk.isOnGround() && hawk.timeFlying > 10) {
                hawk.setFlying(false);
            }
        }

        @Nullable
        protected Vec3d getPosition() {
            Vec3d vector3d = hawk.getPos();
            if (hawk.isOverWater()) {
                flightTarget = true;
            }
            if (flightTarget) {
                if (hawk.timeFlying < 50 || hawk.isOverWater()) {
                    return hawk.getBlockInViewAway(vector3d, 0);
                } else {
                    return hawk.getBlockGrounding(vector3d);
                }
            } else {

                return FuzzyTargeting.find(this.hawk, 10, 7);
            }
        }

        @Override
        public boolean shouldContinue() {
            if (hawk.isSitting() || EntityTarantulaHawk.this.getCommand() == 1) {
                return false;
            }
            if (flightTarget) {
                return hawk.isFlying() && hawk.squaredDistanceTo(x, y, z) > 2F;
            } else {
                return (!this.hawk.getNavigation().isIdle()) && !this.hawk.hasPassengers();
            }
        }

        @Override
        public void start() {
            if (flightTarget) {
                hawk.setFlying(true);
                hawk.getMoveControl().moveTo(x, y, z, 1F);
            } else {
                this.hawk.getNavigation().startMovingTo(this.x, this.y, this.z, 1F);
            }
        }

        @Override
        public void stop() {
            this.hawk.getNavigation().stop();
            super.stop();
        }
    }

    private class AIBury extends Goal {
        private final EntityTarantulaHawk hawk;
        private BlockPos buryPos = null;
        private int digTime = 0;
        private double stageX;
        private double stageY;
        private double stageZ;

        private AIBury() {
            hawk = EntityTarantulaHawk.this;
        }

        @Override
        public boolean canStart() {
            if (hawk.isDragging() && hawk.getTarget() != null) {
                BlockPos pos = hawk.genSandPos(hawk.getBlockPos());
                if (pos != null) {
                    buryPos = pos;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return hawk.isDragging() && digTime < 200 && hawk.getTarget() != null && buryPos != null && getWorld().getBlockState(buryPos).isIn(BlockTags.SAND);
        }

        @Override
        public void start() {
            digTime = 0;
            stageX = hawk.getX();
            stageY = hawk.getY();
            stageZ = hawk.getZ();
        }

        @Override
        public void stop() {
            digTime = 0;
            hawk.setDigging(false);
            hawk.setDragging(false);
            hawk.setTarget(null);
            hawk.setAttacker(null);
        }

        @Override
        public void tick() {
            hawk.setFlying(false);
            hawk.setDragging(true);
            LivingEntity target = hawk.getTarget();
            if (hawk.squaredDistanceTo(Vec3d.ofCenter(buryPos)) < 9) {
                if (!hawk.isDigging()) {
                    hawk.setDigging(true);
                    stageX = target.getX();
                    stageY = target.getY();
                    stageZ = target.getZ();
                }
            }
            if (hawk.isDigging()) {
                target.noClip = true;
                digTime++;
                hawk.removeAllPassengers();
                target.setPos(stageX, stageY - Math.min(3, digTime * 0.05F), stageZ);
                hawk.getNavigation().startMovingTo(stageX, stageY, stageZ, 0.85F);
            } else {
                hawk.getNavigation().startMovingTo(buryPos.getX(), buryPos.getY(), buryPos.getZ(), 0.5F);
            }
        }
    }

    private class AIFleeRoadrunners extends Goal {
        private int searchCooldown = 0;
        private LivingEntity fear = null;
        private Vec3d fearVec = null;

        @Override
        public boolean canStart() {
            if (searchCooldown <= 0) {
                searchCooldown = 100 + EntityTarantulaHawk.this.random.nextInt(100);
                var list = EntityTarantulaHawk.this.getWorld().getNonSpectatingEntities(EntityRoadrunner.class, EntityTarantulaHawk.this.getBoundingBox().expand(15, 32, 15));
                for (var roadrunner : list) {
                    if (fear == null || EntityTarantulaHawk.this.distanceTo(fear) > EntityTarantulaHawk.this.distanceTo(roadrunner)) {
                        fear = roadrunner;
                    }
                }
            } else {
                searchCooldown--;
            }
            return EntityTarantulaHawk.this.isAlive() && fear != null;
        }

        @Override
        public boolean shouldContinue() {
            return fear != null && fear.isAlive() && EntityTarantulaHawk.this.distanceTo(fear) < 32F;
        }

        @Override
        public void start() {
            super.start();
            EntityTarantulaHawk.this.setScared(true);
        }

        @Override
        public void tick() {
            if (fear != null) {
                if (fearVec == null || EntityTarantulaHawk.this.squaredDistanceTo(fearVec) < 4) {
                    fearVec = EntityTarantulaHawk.this.getBlockInViewAway(fearVec == null ? fear.getPos() : fearVec, 12);
                }
                if (fearVec != null) {
                    EntityTarantulaHawk.this.setFlying(true);
                    EntityTarantulaHawk.this.getMoveControl().moveTo(fearVec.x, fearVec.y, fearVec.z, 1.1F);
                }
            }
        }

        @Override
        public void stop() {
            EntityTarantulaHawk.this.setScared(false);
            fear = null;
            fearVec = null;
        }
    }
}
