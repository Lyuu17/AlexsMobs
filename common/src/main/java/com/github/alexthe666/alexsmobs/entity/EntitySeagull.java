package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.CreatureAITargetItems;
import com.github.alexthe666.alexsmobs.entity.ai.DirectPathNavigator;
import com.github.alexthe666.alexsmobs.entity.ai.SeagullAIRevealTreasure;
import com.github.alexthe666.alexsmobs.entity.ai.SeagullAIStealFromPlayers;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.misc.IngredientUtil;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.google.common.base.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapIcon;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EntitySeagull extends AnimalEntity implements ITargetsDroppedItems {

    private static final TrackedData<Boolean> FLYING = DataTracker.registerData(EntitySeagull.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> FLIGHT_LOOK_YAW = DataTracker.registerData(EntitySeagull.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> ATTACK_TICK = DataTracker.registerData(EntitySeagull.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntitySeagull.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Optional<BlockPos>> TREASURE_POS = DataTracker.registerData(EntitySeagull.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);
    public float prevFlyProgress;
    public float flyProgress;
    public float prevFlapAmount;
    public float flapAmount;
    public boolean aiItemFlag = false;
    public float attackProgress;
    public float prevAttackProgress;
    public float sitProgress;
    public float prevSitProgress;
    public int stealCooldown = random.nextInt(2500);
    private boolean isLandNavigator;
    private int timeFlying;
    private BlockPos orbitPos = null;
    private double orbitDist = 5D;
    private boolean orbitClockwise = false;
    private boolean fallFlag = false;
    private int flightLookCooldown = 0;
    private float targetFlightLookYaw;
    private int heldItemTime = 0;
    public int treasureSitTime;
    public UUID feederUUID = null;

    public EntitySeagull(EntityType<EntitySeagull> type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, -1.0F);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 16.0F);
        this.setPathfindingPenalty(PathNodeType.COCOA, -1.0F);
        this.setPathfindingPenalty(PathNodeType.FENCE, -1.0F);
        switchNavigator(false);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.SEAGULL_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.SEAGULL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.SEAGULL_HURT.get();
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Flying", this.isFlying());
        compound.putBoolean("Sitting", this.isSitting());
        compound.putInt("StealCooldown", this.stealCooldown);
        compound.putInt("TreasureSitTime", this.treasureSitTime);
        if(feederUUID != null){
            compound.putUuid("FeederUUID", feederUUID);
        }
        if(this.getTreasurePos() != null){
            compound.putInt("TresX", this.getTreasurePos().getX());
            compound.putInt("TresY", this.getTreasurePos().getY());
            compound.putInt("TresZ", this.getTreasurePos().getZ());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setFlying(compound.getBoolean("Flying"));
        this.setSitting(compound.getBoolean("Sitting"));
        this.stealCooldown = compound.getInt("StealCooldown");
        this.treasureSitTime = compound.getInt("TreasureSitTime");
        if(compound.containsUuid("FeederUUID")){
            this.feederUUID = compound.getUuid("FeederUUID");
        }
        if(compound.contains("TresX") && compound.contains("TresY") && compound.contains("TresZ")){
            this.setTreasurePos(new BlockPos(compound.getInt("TresX"), compound.getInt("TresY"), compound.getInt("TresZ")));
        }
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
        this.targetSelector.add(1, new SeagullAIRevealTreasure(this));
        this.targetSelector.add(2, new SeagullAIStealFromPlayers(this));
        this.goalSelector.add(3, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(4, new TemptGoal(this, 1.0D, IngredientUtil.ingredientFromTags(AMTagRegistry.SEAGULL_BREEDABLES, AMTagRegistry.SEAGULL_OFFERINGS), false){
            @Override
            public boolean canStart(){
                return !EntitySeagull.this.aiItemFlag && super.canStart();
            }
        });
        this.goalSelector.add(5, new AIWanderIdle());
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PathAwareEntity.class, 6.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.goalSelector.add(9, new AIScatter());
        this.targetSelector.add(1, new AITargetItems(this, false, false, 15, 16));
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.SEAGULL_BREEDABLES);
    }

    public static boolean canSeagullSpawn(EntityType<? extends AnimalEntity> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return worldIn.getBaseLightLevel(pos, 0) > 8 && worldIn.getFluidState(pos.down()).isEmpty();
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.seagullSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    public boolean handleFallDamage(float distance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new MobNavigation(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new MoveHelper(this);
            this.navigation = new DirectPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FLYING, false);
        this.dataTracker.startTracking(SITTING, false);
        this.dataTracker.startTracking(ATTACK_TICK, 0);
        this.dataTracker.startTracking(TREASURE_POS, Optional.empty());
        this.dataTracker.startTracking(FLIGHT_LOOK_YAW, 0F);
    }

    @Override
    public boolean isFlying() {
        return this.dataTracker.get(FLYING);
    }

    @Override
    public void setFlying(boolean flying) {
        if (flying && this.isBaby()) {
            flying = false;
        }
        this.dataTracker.set(FLYING, flying);
    }

    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    public void setSitting(boolean sitting) {
        this.dataTracker.set(SITTING, sitting);
    }

    public float getFlightLookYaw() {
        return this.dataTracker.get(FLIGHT_LOOK_YAW);
    }

    public void setFlightLookYaw(float yaw) {
        this.dataTracker.set(FLIGHT_LOOK_YAW, yaw);
    }

    public BlockPos getTreasurePos() {
        return this.dataTracker.get(TREASURE_POS).orElse(null);
    }

    public void setTreasurePos(BlockPos pos) {
        this.dataTracker.set(TREASURE_POS, Optional.ofNullable(pos));
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            final boolean prev = super.damage(source, amount);
            if (prev) {
                this.setSitting(false);
                if (!this.getMainHandStack().isEmpty()) {
                    this.dropStack(this.getMainHandStack());
                    this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    stealCooldown = 1500 + random.nextInt(1500);
                }
                this.feederUUID = null;
                this.treasureSitTime = 0;
            }
            return prev;
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.prevFlyProgress = flyProgress;
        this.prevFlapAmount = flapAmount;
        this.prevAttackProgress = attackProgress;
        this.prevSitProgress = sitProgress;
        final float yMot = (float) -((float) this.getVelocity().y * (double) MathHelper.DEGREES_PER_RADIAN);
        final float absYaw = Math.abs(this.getYaw() - this.prevYaw);

        final boolean flying = isFlying();
        final boolean sitting = isSitting();

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

        if (absYaw > 8) {
            flapAmount = Math.min(1F, flapAmount + 0.1F);
        } else if (yMot < 0.0F) {
            flapAmount = Math.min(-yMot * 0.2F, 1F);
        } else {
            if (flapAmount > 0.0F) {
                flapAmount -= Math.min(flapAmount, 0.05F);
            } else {
                flapAmount = 0;
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
        if (!this.getWorld().isClient) {
            if (isFlying()) {
                float lookYawDist = Math.abs(this.getFlightLookYaw() - targetFlightLookYaw);
                if (flightLookCooldown > 0) {
                    flightLookCooldown--;
                }
                if (flightLookCooldown == 0 && this.random.nextInt(4) == 0 && lookYawDist < 0.5F) {
                    targetFlightLookYaw = MathHelper.clamp(random.nextFloat() * 120F - 60, -60, 60);
                    flightLookCooldown = 3 + random.nextInt(15);
                }
                if (this.getFlightLookYaw() < this.targetFlightLookYaw && lookYawDist > 0.5F) {
                    this.setFlightLookYaw(this.getFlightLookYaw() + Math.min(lookYawDist, 4F));
                }
                if (this.getFlightLookYaw() > this.targetFlightLookYaw && lookYawDist > 0.5F) {
                    this.setFlightLookYaw(this.getFlightLookYaw() - Math.min(lookYawDist, 4F));
                }
                if (this.isOnGround() && !this.isInsideWaterOrBubbleColumn() && this.timeFlying > 30) {
                    this.setFlying(false);
                }
                timeFlying++;
                this.setNoGravity(true);
                if (this.hasVehicle() || this.isInLove()) {
                    this.setFlying(false);
                }
            } else {
                fallFlag = false;
                timeFlying = 0;
                this.setNoGravity(false);
            }
            if (isFlying() && this.isLandNavigator) {
                switchNavigator(false);
            }
            if (!isFlying() && !this.isLandNavigator) {
                switchNavigator(true);
            }
        }
        if (!this.getMainHandStack().isEmpty()) {
            heldItemTime++;
            if (heldItemTime > 200 && canTargetItem(this.getMainHandStack())) {
                heldItemTime = 0;
                this.heal(4);
                this.emitGameEvent(GameEvent.EAT);
                this.playSound(SoundEvents.ENTITY_GENERIC_EAT, this.getSoundVolume(), this.getSoundPitch());
                if (this.getMainHandStack().getItem().hasRecipeRemainder()) {
                    this.dropItem(this.getMainHandStack().getItem().getRecipeRemainder());
                }
                eatItemEffect(this.getMainHandStack());
                this.getMainHandStack().decrement(1);
            }
        } else {
            heldItemTime = 0;
        }
        if (stealCooldown > 0) {
            stealCooldown--;
        }
        if(treasureSitTime > 0){
            treasureSitTime--;
        }
        if(this.isSitting() && this.isInsideWaterOrBubbleColumn()){
            this.setVelocity(this.getVelocity().add(0, 0.02F, 0));
        }
    }

    public void eatItem(){
        heldItemTime = 200;
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack.getItem().isFood() && !this.isSitting();
    }

    private void eatItemEffect(ItemStack heldItemMainhand) {
        for (int i = 0; i < 2 + random.nextInt(2); i++) {
            final double d2 = this.random.nextGaussian() * 0.02D;
            final double d0 = this.random.nextGaussian() * 0.02D;
            final double d1 = this.random.nextGaussian() * 0.02D;
            final float radius = this.getWidth() * 0.65F;
            final float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
            final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            final double extraZ = radius * MathHelper.cos(angle);
            ParticleEffect data = new ItemStackParticleEffect(ParticleTypes.ITEM, heldItemMainhand);
            if (heldItemMainhand.getItem() instanceof BlockItem) {
                data = new BlockStateParticleEffect(ParticleTypes.BLOCK, ((BlockItem) heldItemMainhand.getItem()).getBlock().getDefaultState());
            }
            this.getWorld().addParticle(data, this.getX() + extraX, this.getY() + this.getHeight() * 0.6F, this.getZ() + extraZ, d0, d1, d2);
        }
    }

    public void setDataFromTreasureMap(PlayerEntity player){
        boolean flag = false;
        for(var map : player.getHandItems()){
            if(map.getItem() == Items.FILLED_MAP || map.getItem() == Items.MAP){
                if (map.hasNbt() && map.getNbt().contains("Decorations", 9)) {
                    var listnbt = map.getNbt().getList("Decorations", 10);
                    for(int i = 0; i < listnbt.size(); i++){
                        NbtCompound nbt = listnbt.getCompound(i);
                        byte type = nbt.getByte("type");
                        if(type == MapIcon.Type.RED_X.getId() || type == MapIcon.Type.TARGET_X.getId()){
                            int x = nbt.getInt("x");
                            int z = nbt.getInt("z");
                            if(this.squaredDistanceTo(x, this.getY(), z) <= 400){
                                flag = true;
                                this.setTreasurePos(new BlockPos(x, 0, z));
                            }
                        }
                    }
                }
            }
        }
        if(flag){
            this.feederUUID = player.getUuid();
            this.treasureSitTime = 300;
            this.stealCooldown = 1500 + random.nextInt(1500);
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
        super.travel(vec3d);
    }

    public boolean isWingull() {
        var s = Formatting.strip(this.getName().getString());
        return s != null && s.equalsIgnoreCase("wingull");
    }

    @Override
    public void onGetItem(ItemEntity e) {
        var duplicate = e.getStack().copy();
        duplicate.setCount(1);
        if (!this.getStackInHand(Hand.MAIN_HAND).isEmpty() && !this.getWorld().isClient) {
            this.dropStack(this.getStackInHand(Hand.MAIN_HAND), 0.0F);
        }
        stealCooldown += 600 + random.nextInt(1200);
        var thrower = e.getOwner();
        if (thrower != null && e.getStack().isIn(AMTagRegistry.SEAGULL_OFFERINGS)) {
            PlayerEntity player = getWorld().getPlayerByUuid(thrower.getUuid());
            if(player != null){
                setDataFromTreasureMap(player);
                feederUUID = thrower.getUuid();
            }
        }
        this.setFlying(true);
        this.setStackInHand(Hand.MAIN_HAND, duplicate);
    }

    public Vec3d getBlockInViewAway(Vec3d fleePos, float radiusAdd) {
        final float radius = 5 + radiusAdd + this.getRandom().nextInt(5);
        final float neg = this.getRandom().nextBoolean() ? 1 : -1;
        final float renderYawOffset = this.bodyYaw;
        final float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        final double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = new BlockPos((int) (fleePos.x + extraX), 0, (int) (fleePos.z + extraZ));
        BlockPos ground = getSeagullGround(radialPos);
        final int distFromGround = (int) this.getY() - ground.getY();
        final int flightHeight = 8 + this.getRandom().nextInt(4);
        BlockPos newPos = ground.up(distFromGround > 3 ? flightHeight : this.getRandom().nextInt(4) + 8);
        if (!this.isTargetBlocked(Vec3d.ofCenter(newPos)) && this.squaredDistanceTo(Vec3d.ofCenter(newPos)) > 1) {
            return Vec3d.ofCenter(newPos);
        }
        return null;
    }

    public BlockPos getSeagullGround(BlockPos in) {
        BlockPos position = new BlockPos(in.getX(), (int) this.getY(), in.getZ());
        while (position.getY() < 320 && !getWorld().getFluidState(position).isEmpty()) {
            position = position.up();
        }
        while (position.getY() > -64 && !getWorld().getBlockState(position).isSolid() && getWorld().getFluidState(position).isEmpty()) {
            position = position.down();
        }
        return position;
    }

    public Vec3d getBlockGrounding(Vec3d fleePos) {
        final float radius = 10 + this.getRandom().nextInt(15);
        final float neg = this.getRandom().nextBoolean() ? 1 : -1;
        final float renderYawOffset = this.bodyYaw;
        final float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        final double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = AMBlockPos.fromCoords(fleePos.x + extraX, getY(), fleePos.z + extraZ);
        BlockPos ground = this.getSeagullGround(radialPos);
        if (ground.getY() == 0) {
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

    private Vec3d getOrbitVec(Vec3d vector3d, float gatheringCircleDist) {
        final float angle = (Maths.STARTING_ANGLE * (float) this.orbitDist * (orbitClockwise ? -this.age : this.age));
        final double extraX = gatheringCircleDist * MathHelper.sin((angle));
        final double extraZ = gatheringCircleDist * MathHelper.cos(angle);
        if (this.orbitPos != null) {
            var pos = new Vec3d(orbitPos.getX() + extraX, orbitPos.getY() + random.nextInt(2), orbitPos.getZ() + extraZ);
            if (this.getWorld().isAir(AMBlockPos.fromVec3(pos))) {
                return pos;
            }
        }
        return null;
    }

    private boolean isOverWaterOrVoid() {
        BlockPos position = this.getBlockPos();
        while (position.getY() > -64 && getWorld().isAir(position)) {
            position = position.down();
        }
        return !getWorld().getFluidState(position).isEmpty() || position.getY() <= -64;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        var type = super.interactMob(player, hand);
        if (!this.getMainHandStack().isEmpty() && type != ActionResult.SUCCESS) {
            this.dropStack(this.getMainHandStack().copy());
            this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            stealCooldown = 1500 + random.nextInt(1500);
            return ActionResult.SUCCESS;
        } else {
            return type;
        }
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.SEAGULL.get().create(world);
    }

    public void peck() {
        this.dataTracker.set(ATTACK_TICK, 7);
    }

    private class AIScatter extends Goal {
        protected final Sorter theNearestAttackableTargetSorter;
        protected final Predicate<? super Entity> targetEntitySelector;
        protected int executionChance = 8;
        protected boolean mustUpdate;
        private Entity targetEntity;
        private Vec3d flightTarget = null;
        private int cooldown = 0;

        AIScatter() {
            this.setControls(EnumSet.of(Goal.Control.MOVE));
            this.theNearestAttackableTargetSorter = new Sorter(EntitySeagull.this);
            this.targetEntitySelector = (Predicate<Entity>) e -> e.isAlive() && e.getType().isIn(AMTagRegistry.SCATTERS_CROWS) || e instanceof PlayerEntity && !((PlayerEntity) e).isCreative();
        }

        @Override
        public boolean canStart() {
            if (EntitySeagull.this.hasVehicle() || EntitySeagull.this.isSitting() || EntitySeagull.this.aiItemFlag || EntitySeagull.this.hasPassengers()) {
                return false;
            }
            if (!this.mustUpdate) {
                long worldTime = EntitySeagull.this.getWorld().getTime() % 10;
                if (EntitySeagull.this.getDespawnCounter() >= 100 && worldTime != 0) {
                    return false;
                }
                if (EntitySeagull.this.getRandom().nextInt(this.executionChance) != 0 && worldTime != 0) {
                    return false;
                }
            }
            List<Entity> list = EntitySeagull.this.getWorld().getEntitiesByClass(Entity.class, this.getTargetableArea(this.getTargetDistance()), this.targetEntitySelector);
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
                EntitySeagull.this.setFlying(true);
                EntitySeagull.this.getMoveControl().moveTo(flightTarget.x, flightTarget.y, flightTarget.z, 1F);
                if (cooldown == 0 && EntitySeagull.this.isTargetBlocked(flightTarget)) {
                    cooldown = 30;
                    flightTarget = null;
                }
            }

            if (targetEntity != null) {
                if (EntitySeagull.this.isOnGround() || flightTarget == null || flightTarget != null && EntitySeagull.this.squaredDistanceTo(flightTarget) < 3) {
                    Vec3d vec = EntitySeagull.this.getBlockInViewAway(targetEntity.getPos(), 0);
                    if (vec != null && vec.y > EntitySeagull.this.getY()) {
                        flightTarget = vec;
                    }
                }
                if (EntitySeagull.this.distanceTo(targetEntity) > 20.0F) {
                    this.stop();
                }
            }
        }

        protected double getTargetDistance() {
            return 4D;
        }

        protected Box getTargetableArea(double targetDistance) {
            var renderCenter = new Vec3d(EntitySeagull.this.getX(), EntitySeagull.this.getY() + 0.5, EntitySeagull.this.getZ());
            var aabb = new Box(-2, -2, -2, 2, 2, 2);
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

    private class AIWanderIdle extends Goal {
        protected final EntitySeagull eagle;
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
            this.eagle = EntitySeagull.this;
        }

        @Override
        public boolean canStart() {
            if (orbitResetCooldown < 0) {
                orbitResetCooldown++;
            }
            if ((eagle.getTarget() != null && eagle.getTarget().isAlive() && !this.eagle.hasPassengers()) || eagle.isSitting() || this.eagle.hasVehicle()) {
                return false;
            } else {
                if (this.eagle.getRandom().nextInt(20) != 0 && !eagle.isFlying() || eagle.aiItemFlag) {
                    return false;
                }
                if (this.eagle.isBaby()) {
                    this.flightTarget = false;
                } else if (this.eagle.isInsideWaterOrBubbleColumn()) {
                    this.flightTarget = true;
                } else if (this.eagle.isOnGround()) {
                    this.flightTarget = random.nextInt(10) == 0;
                } else {
                    if (orbitResetCooldown == 0 && random.nextInt(6) == 0) {
                        orbitResetCooldown = 100 + random.nextInt(300);
                        eagle.orbitPos = eagle.getBlockPos();
                        eagle.orbitDist = 4 + random.nextInt(5);
                        eagle.orbitClockwise = random.nextBoolean();
                        orbitTime = 0;
                        maxOrbitTime = (int) (180 + 360 * random.nextFloat());
                    }
                    this.flightTarget = random.nextInt(5) != 0 && eagle.timeFlying < 400;
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
                if (!eagle.isFlying() || eagle.isOnGround()) {
                    this.eagle.getNavigation().startMovingTo(this.x, this.y, this.z, 1F);
                }
            }
            if (!flightTarget && isFlying()) {
                eagle.fallFlag = true;
                if (eagle.isOnGround()) {
                    eagle.setFlying(false);
                    orbitTime = 0;
                    eagle.orbitPos = null;
                    orbitResetCooldown = -400 - random.nextInt(400);
                }
            }
            if (isFlying() && (!getWorld().isAir(eagle.getVelocityAffectingPos()) || eagle.isOnGround()) && !eagle.isInsideWaterOrBubbleColumn() && eagle.timeFlying > 30) {
                eagle.setFlying(false);
                orbitTime = 0;
                eagle.orbitPos = null;
                orbitResetCooldown = -400 - random.nextInt(400);
            }
        }

        @Nullable
        protected Vec3d getPosition() {
            Vec3d vector3d = eagle.getPos();
            if (orbitResetCooldown > 0 && eagle.orbitPos != null) {
                return eagle.getOrbitVec(vector3d, 4 + random.nextInt(4));
            }
            if (eagle.hasPassengers() || eagle.isOverWaterOrVoid()) {
                flightTarget = true;
            }
            if (flightTarget) {
                if (eagle.timeFlying < 340 || eagle.hasPassengers() || eagle.isOverWaterOrVoid()) {
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
            if (flightTarget) {
                return eagle.isFlying() && eagle.squaredDistanceTo(x, y, z) > 5F;
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

    static class MoveHelper extends MoveControl {
        private final EntitySeagull parentEntity;

        public MoveHelper(EntitySeagull bird) {
            super(bird);
            this.parentEntity = bird;
        }

        @Override
        public void tick() {
            if (this.state == MoveControl.State.MOVE_TO) {
                Vec3d vector3d = new Vec3d(this.targetX - parentEntity.getX(), this.targetY - parentEntity.getY(), this.targetZ - parentEntity.getZ());
                double d5 = vector3d.length();
                if (d5 < 0.3) {
                    this.state = MoveControl.State.WAIT;
                    parentEntity.setVelocity(parentEntity.getVelocity().multiply(0.5D));
                } else {
                    parentEntity.setVelocity(parentEntity.getVelocity().add(vector3d.multiply(this.speed * 0.03D / d5)));
                    Vec3d vector3d1 = parentEntity.getVelocity();
                    parentEntity.setYaw(-((float) MathHelper.atan2(vector3d1.x, vector3d1.z)) * MathHelper.DEGREES_PER_RADIAN);
                    parentEntity.bodyYaw = parentEntity.getYaw();

                }

            }
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
            ((EntitySeagull) mob).aiItemFlag = false;
        }

        @Override
        public boolean canStart() {
            return super.canStart() && !((EntitySeagull) mob).isSitting() && (mob.getTarget() == null || !mob.getTarget().isAlive());
        }

        @Override
        public boolean shouldContinue() {
            return super.shouldContinue() && !((EntitySeagull) mob).isSitting() && (mob.getTarget() == null || !mob.getTarget().isAlive());
        }

        @Override
        protected void moveTo() {
            var crow = (EntitySeagull) mob;
            if (this.targetEntity != null) {
                crow.aiItemFlag = true;
                if (this.mob.distanceTo(targetEntity) < 2) {
                    crow.getMoveControl().moveTo(this.targetEntity.getX(), targetEntity.getY(), this.targetEntity.getZ(), 1.5F);
                    crow.peck();
                }
                if (this.mob.distanceTo(this.targetEntity) > 8 || crow.isFlying()) {
                    crow.setFlying(true);
                    final float f = (float) (crow.getX() - targetEntity.getX());
                    final float f2 = (float) (crow.getZ() - targetEntity.getZ());

                    if (!crow.canSee(targetEntity)) {
                        crow.getMoveControl().moveTo(this.targetEntity.getX(), 1 + crow.getY(), this.targetEntity.getZ(), 1.5F);
                    } else {
                        float f1 = 1.8F;
                        final float xzDist = MathHelper.sqrt(f * f + f2 * f2);
                        if (xzDist < 5) {
                            f1 = 0;
                        }
                        crow.getMoveControl().moveTo(this.targetEntity.getX(), f1 + this.targetEntity.getY(), this.targetEntity.getZ(), 1.5F);
                    }
                } else {
                    this.mob.getNavigation().startMovingTo(this.targetEntity.getX(), this.targetEntity.getY(), this.targetEntity.getZ(), 1.5F);
                }
            }
        }

        @Override
        public void tick() {
            super.tick();
            moveTo();
        }
    }
}
