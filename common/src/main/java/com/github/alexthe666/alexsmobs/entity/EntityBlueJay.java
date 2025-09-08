package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMParticleRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.google.common.base.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EntityBlueJay extends AnimalEntity implements ITargetsDroppedItems{

    private static final TrackedData<Boolean> FLYING = DataTracker.registerData(EntityBlueJay.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> ATTACK_TICK = DataTracker.registerData(EntityBlueJay.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> CREST_TARGET = DataTracker.registerData(EntityBlueJay.class, TrackedDataHandlerRegistry.FLOAT);

    private static final TrackedData<Optional<UUID>> LAST_FEEDER_UUID = DataTracker.registerData(EntityBlueJay.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    private static final TrackedData<Optional<UUID>> RACCOON_UUID = DataTracker.registerData(EntityBlueJay.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    private static final TrackedData<Integer> FEED_TIME = DataTracker.registerData(EntityBlueJay.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SING_TIME = DataTracker.registerData(EntityBlueJay.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> BLUE_VISUAL_FLAG = DataTracker.registerData(EntityBlueJay.class, TrackedDataHandlerRegistry.BOOLEAN);

    private static final java.util.function.Predicate<Entity> HIGHLIGHTS_WITH_SONG = (entity) -> entity instanceof HostileEntity;

    public float prevFlyProgress;
    public float flyProgress;
    public float prevFlapAmount;
    public float flapAmount;
    public float attackProgress;
    public float prevAttackProgress;
    public float prevCrestAmount;
    public float crestAmount;
    private boolean isLandNavigator;
    private int timeFlying;
    public float birdPitch = 0;
    public float prevBirdPitch = 0;
    public boolean aiItemFlag = false;
    private int prevSingTime = 0;
    private int blueTime = 0;
    private int raiseCrestOvertickRidings;

    public EntityBlueJay(EntityType<? extends AnimalEntity> animal, World level) {
        super(animal, level);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, -1.0F);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 16.0F);
        this.setPathfindingPenalty(PathNodeType.COCOA, -1.0F);
        this.setPathfindingPenalty(PathNodeType.FENCE, -1.0F);
        switchNavigator(false);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(2, new BlueJayAIMelee(this));
        this.goalSelector.add(3, new FollowParentGoal(this, 1D));
        this.goalSelector.add(4, new FlyingAITempt(this, 1.0D, Ingredient.fromTag(AMTagRegistry.BLUE_JAY_FOODSTUFFS), false));
        this.goalSelector.add(5, new AIFollowFeederOrRaccoon());
        this.goalSelector.add(6, new AIFlyIdle());
        this.goalSelector.add(7, new AIScatter());
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(9, new LookAtEntityGoal(this, PathAwareEntity.class, 6.0F));
        this.goalSelector.add(10, new LookAroundGoal(this));
        this.targetSelector.add(1, new AITargetItems(this, false, false, 40, 16));
        this.targetSelector.add(4, (new RevengeGoal(this, PlayerEntity.class)).setGroupRevenge());
    }

    public static boolean checkBlueJaySpawnRules(EntityType type, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        return isLightLevelValidForNaturalSpawn(worldIn, pos);
    }

    @Override
    public boolean canSpawn(WorldView reader) {
        if (reader.doesNotIntersectEntities(this) && !reader.containsFluid(this.getBoundingBox())) {
            var blockpos = this.getBlockPos();
            var blockstate2 = reader.getBlockState(blockpos.down());
            return blockstate2.isIn(BlockTags.LEAVES) || blockstate2.isIn(BlockTags.LOGS) || blockstate2.isOf(Blocks.GRASS_BLOCK);
        }
        return false;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.blueJaySpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.BLUE_JAY_BREEDABLES);
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FLYING, false);
        this.dataTracker.startTracking(ATTACK_TICK, 0);
        this.dataTracker.startTracking(FEED_TIME, 0);
        this.dataTracker.startTracking(SING_TIME, 0);
        this.dataTracker.startTracking(CREST_TARGET, 0F);
        this.dataTracker.startTracking(BLUE_VISUAL_FLAG, false);
        this.dataTracker.startTracking(RACCOON_UUID, Optional.empty());
        this.dataTracker.startTracking(LAST_FEEDER_UUID, Optional.empty());
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new MobNavigation(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new FlightMoveController(this, 1, false);
            this.navigation = new DirectPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    public void tick(){
        super.tick();
        this.prevCrestAmount = crestAmount;
        this.prevAttackProgress = attackProgress;
        this.prevFlapAmount = flapAmount;
        this.prevFlyProgress = flyProgress;
        this.prevBirdPitch = birdPitch;

        if (isFlying()) {
            if (flyProgress < 5F)
                flyProgress++;
        } else {
            if (flyProgress > 0F)
                flyProgress--;
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
        float yMov = (float) this.getVelocity().y;
        this.birdPitch = yMov * 2 * -(float) MathHelper.DEGREES_PER_RADIAN;
        if(yMov >= 0){
            if(flapAmount < 1F){
                flapAmount += 0.25F;
            }
        }else if(yMov < -0.07F){
            if(flapAmount > 0){
                flapAmount -= 0.25F;
            }
        }

        if(raiseCrestOvertickRidings > 0){
            raiseCrestOvertickRidings--;
            this.crestAmount = 0.75F;
        }else{
            this.crestAmount = MathHelper.stepTowards(this.crestAmount, this.getTargetCrest(), 0.3F);
        }
        if(!this.getWorld().isClient){
            if (isFlying()) {
                if (this.isLandNavigator)
                    switchNavigator(false);
            } else {
                if (!this.isLandNavigator)
                    switchNavigator(true);
            }

            if (isFlying()) {
                timeFlying++;
                this.setNoGravity(true);
                if (this.hasVehicle() || this.isInLove()) {
                    this.setFlying(false);
                }
            } else {
                timeFlying = 0;
                this.setNoGravity(false);
            }
            if(this.getTarget() != null){
                this.setCrestTarget(1F);
            }else if(this.getRaccoonUUID() != null){
                this.setCrestTarget(0.5F);
            }else{
                this.setCrestTarget(0.0F);
            }
        }
        if(this.getFeedTime() > 0){
            this.setFeedTime(this.getFeedTime() - 1);
            if(this.getFeedTime() == 0){
                this.setLastFeeder(null);
            }
        }
        if(this.getVehicle() instanceof EntityRaccoon riddenRaccoon){
            this.bodyYaw = riddenRaccoon.bodyYaw;
        }
        Entity owner = this.getRaccoon();
        if(owner instanceof EntityRaccoon raccoon){
            var jayTarget = this.getTarget();
            var raccoonTarget = raccoon.getTarget();
            if(jayTarget != null && jayTarget.isAlive()){
                if(this.hasVehicle()){
                    this.stopRiding();
                }
            }else if(raccoonTarget != null && raccoonTarget.isAlive()) {
                if (this.canTarget(raccoonTarget)) {
                    this.setTarget(raccoonTarget);
                }
            }
        }
        if(this.getSingTime() > 0){
            this.setSingTime(this.getSingTime() - 1);
            if(this.prevSingTime % 15 == 0){
               this.playSound(AMSoundRegistry.BLUE_JAY_SONG.get(), this.getSoundVolume(), this.getSoundPitch());
            }
            if(this.getWorld().isClient){
                if(this.getSingTime() % 5 == 0 && this.getWorld().isClient){
                    var modelFront = new Vec3d(0, 0.2F, 0.3F)
                            .multiply(this.getScaleFactor())
                            .rotateX(-this.getPitch() * MathHelper.RADIANS_PER_DEGREE)
                            .rotateY(-this.getYaw() * MathHelper.RADIANS_PER_DEGREE);
                    var particleFrom = this.getPos().add(modelFront);
                    this.getWorld().addParticle(AMParticleRegistry.BIRD_SONG.get(), particleFrom.x, particleFrom.y, particleFrom.z, modelFront.x, modelFront.y, modelFront.z);
                }
            }
        }
        if(prevSingTime < getSingTime() && !this.getWorld().isClient){
            blueTime = 1200;
            this.dataTracker.set(BLUE_VISUAL_FLAG, true);
            highlightMonsters();
        }
        if(blueTime > 0){
            blueTime--;
            if(blueTime == 0){
                this.dataTracker.set(BLUE_VISUAL_FLAG, false);
                this.getWorld().sendEntityStatus(this, (byte) 68);
            }else{
                this.getWorld().sendEntityStatus(this, (byte) 67);
            }
        }
        prevSingTime = getSingTime();
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        raiseCrestOvertickRidings = 15;
    }

    private boolean highlightMonsters() {
        var allyBox = this.getBoundingBox().expand(64);
        allyBox = allyBox.withMinY(-64);
        allyBox = allyBox.withMaxY(320);
        boolean any = false;
        for (var entity : this.getWorld().getEntitiesByClass(LivingEntity.class, allyBox, HIGHLIGHTS_WITH_SONG)) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, blueTime, 0, true, false));
        }
        return any;
    }

    public boolean isMakingMonstersBlue(){
        return this.dataTracker.get(BLUE_VISUAL_FLAG);
    }

    @Override
    public void remove(Entity.RemovalReason removalReason) {
        if(this.getSingTime() > 0 && !this.getWorld().isClient){
            this.dataTracker.set(BLUE_VISUAL_FLAG, false);
            this.getWorld().sendEntityStatus(this, (byte) 68);
        }
        super.remove(removalReason);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.IN_WALL)  || super.isInvulnerableTo(source);
    }

    @Override
    public void travel(Vec3d vec3d) {
        if(this.isTouchingWater() && this.getVelocity().y > 0F){
            this.setVelocity(this.getVelocity().multiply(1.0D, 0.5D, 1.0D));
        }
        super.travel(vec3d);
    }

    public BlockPos getBlueJayGround(BlockPos in) {
        BlockPos position = new BlockPos(in.getX(), (int) this.getY(), in.getZ());
        while (position.getY() < 320 && !getWorld().getFluidState(position).isEmpty()) {
            position = position.up();
        }
        while (position.getY() > -64 && !getWorld().getBlockState(position).isSolid() && getWorld().getFluidState(position).isEmpty()) {
            position = position.down();
        }
        return position;
    }

    @Override
    public boolean isTeammate(Entity entityIn) {
        if(this.getRaccoonUUID() != null){
            if(entityIn instanceof EntityRaccoon && this.getRaccoonUUID().equals(entityIn.getUuid())) {
                return true;
            }else{
                Entity raccoon = getRaccoon();
                if(raccoon != null && (raccoon.isTeammate(entityIn) || entityIn.isTeammate(raccoon))){
                    return true;
                }
            }
        }
        return super.isTeammate(entityIn);
    }

    public Vec3d getBlockGrounding(Vec3d fleePos) {
        float radius = 10 + this.getRandom().nextInt(15);
        float neg = this.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = this.bodyYaw;
        float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = new BlockPos((int) (fleePos.x + extraX), (int) getY(), (int) (fleePos.z + extraZ));
        BlockPos ground = this.getBlueJayGround(radialPos);
        if (ground.getY() < -64) {
            return null;
        } else {
            ground = this.getBlockPos();
            while (ground.getY() > -64 && !getWorld().getBlockState(ground).isSolid()) {
                ground = ground.down();
            }
        }
        if (!this.isTargetBlocked(Vec3d.ofCenter(ground.up()))) {
            return Vec3d.ofCenter(ground.down());
        }
        return null;
    }

    public Vec3d getBlockInViewAway(Vec3d fleePos, float radiusAdd) {
        float radius = 5 + radiusAdd + this.getRandom().nextInt(5);
        float neg = this.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = this.bodyYaw;
        float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = new BlockPos((int) (fleePos.x + extraX), 0, (int) (fleePos.z + extraZ));
        BlockPos ground = getBlueJayGround(radialPos);
        int distFromGround = (int) this.getY() - ground.getY();
        int flightHeight = 5 + this.getRandom().nextInt(5);
        int j = this.getRandom().nextInt(5) + 5;

        BlockPos newPos = ground.up(distFromGround > 5 ? flightHeight : j);
        if (getWorld().getBlockState(ground).isIn(BlockTags.LEAVES)) {
            newPos = ground.up(1 + this.getRandom().nextInt(3));
        }
        if (!this.isTargetBlocked(Vec3d.ofCenter(newPos)) && this.squaredDistanceTo(Vec3d.ofCenter(newPos)) > 1) {
            return Vec3d.ofCenter(newPos);
        }
        return null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.BLUE_JAY_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.BLUE_JAY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.BLUE_JAY_HURT.get();
    }

    public boolean isTargetBlocked(Vec3d target) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        return this.getWorld().raycast(new RaycastContext(Vector3d, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() != HitResult.Type.MISS;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
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

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setFlying(compound.getBoolean("Flying"));
        this.blueTime = compound.getInt("BlueTime");
        if (compound.containsUuid("FeederUUID")) {
            this.setLastFeederUUID(compound.getUuid("FeederUUID"));
        }
        if (compound.containsUuid("RaccoonUUID")) {
            this.setRaccoonUUID(compound.getUuid("RaccoonUUID"));
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Flying", this.isFlying());
        compound.putInt("BlueTime", this.blueTime);
        if (this.getLastFeederUUID() != null) {
            compound.putUuid("FeederUUID", this.getLastFeederUUID());
        }
        if (this.getRaccoonUUID() != null) {
            compound.putUuid("RaccoonUUID", this.getRaccoonUUID());
        }
    }

    public int getFeedTime() {
        return this.dataTracker.get(FEED_TIME);
    }

    public void setFeedTime(int feedTime) {
        this.dataTracker.set(FEED_TIME, feedTime);
    }

    public int getSingTime() {
        return this.dataTracker.get(SING_TIME);
    }

    public void setSingTime(int singTime) {
        this.dataTracker.set(SING_TIME, singTime);
    }

    public float getTargetCrest() {
        return this.dataTracker.get(CREST_TARGET);
    }

    public void setCrestTarget(float crestTarget) {
        this.dataTracker.set(CREST_TARGET, crestTarget);
    }

    @Nullable
    public UUID getLastFeederUUID() {
        return this.dataTracker.get(LAST_FEEDER_UUID).orElse(null);
    }

    public void setLastFeederUUID(@Nullable UUID uniqueId) {
        this.dataTracker.set(LAST_FEEDER_UUID, Optional.ofNullable(uniqueId));
    }

    @Nullable
    public Entity getLastFeeder() {
        UUID id = getLastFeederUUID();
        if (id != null && !this.getWorld().isClient) {
            return ((ServerWorld) getWorld()).getEntity(id);
        }
        return null;
    }

    public void setLastFeeder(@Nullable Entity feeder) {
        if (feeder == null) {
            this.setLastFeederUUID(null);
        } else {
            this.setLastFeederUUID(feeder.getUuid());
        }
    }

    @Nullable
    public UUID getRaccoonUUID() {
        return this.dataTracker.get(RACCOON_UUID).orElse(null);
    }

    public void setRaccoonUUID(@Nullable UUID uniqueId) {
        this.dataTracker.set(RACCOON_UUID, Optional.ofNullable(uniqueId));
    }

    @Nullable
    public Entity getRaccoon() {
        UUID id = getRaccoonUUID();
        if (id != null && !this.getWorld().isClient) {
            return ((ServerWorld) getWorld()).getEntity(id);
        }
        return null;
    }

    public void setRaccoon(@Nullable Entity feeder) {
        if (feeder == null) {
            this.setRaccoonUUID(null);
        } else {
            this.setRaccoonUUID(feeder.getUuid());
        }
    }

    private boolean isOverWaterOrVoid() {
        BlockPos position = this.getBlockPos();
        while (position.getY() > -65 && getWorld().isAir(position)) {
            position = position.down();
        }
        return !getWorld().getFluidState(position).isEmpty() || getWorld().getBlockState(position).isOf(Blocks.VINE) || position.getY() <= -65;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld level, PassiveEntity mob) {
        return AMEntityRegistry.BLUE_JAY.get().create(getWorld());
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack.getItem().isFood() || stack.isIn(AMTagRegistry.BLUE_JAY_FOODSTUFFS);
    }

    public double getMaxDistToItem() {
        return 1.0D;
    }

    @Override
    public void onGetItem(ItemEntity e) {
        if (!this.getStackInHand(Hand.MAIN_HAND).isEmpty() && !this.getWorld().isClient) {
            this.dropStack(this.getStackInHand(Hand.MAIN_HAND), 0.0F);
        }
        this.heal(3);
        Entity itemThrower = e.getOwner();
        if(itemThrower != null && e.getStack().isIn(AMTagRegistry.BLUE_JAY_TEAMING_FOODS)){
            this.setLastFeederUUID(itemThrower.getUuid());
            this.setFeedTime(1200);
            this.stopRiding();
        }
        if(e.getOwner() != null && e.getStack().isIn(AMTagRegistry.BLUE_JAY_ALERT_FOODS)){
            this.setSingTime(40);
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        ActionResult type = super.interactMob(player, hand);
        if (!type.isAccepted()) {
            if(itemstack.isIn(AMTagRegistry.BLUE_JAY_TEAMING_FOODS) && this.getFeedTime() <= 0){
                this.heal(3);
                this.eat(player, hand, itemstack);
                this.setRaccoonUUID(null);
                this.stopRiding();
                this.setLastFeeder(player);
                this.setFeedTime(1200);
                return ActionResult.SUCCESS;
            }else if(itemstack.isIn(AMTagRegistry.BLUE_JAY_ALERT_FOODS) && this.getSingTime() <= 0){
                this.heal(3);
                this.setSingTime(40);
                this.eat(player, hand, itemstack);
                return ActionResult.SUCCESS;
            }
        }
        return type;
    }

    public void peck() {
        this.dataTracker.set(ATTACK_TICK, 7);
    }

    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if (id == 67 || id == 68) {
            // TODO
//            AlexsMobs.PROXY.onEntityStatus(this, id);
        } else {
            super.handleStatus(id);
        }
    }

    private class AIFlyIdle extends Goal {
        protected double x;
        protected double y;
        protected double z;
        private boolean flightTarget;

        public AIFlyIdle() {
            super();
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (EntityBlueJay.this.hasPassengers() ||  (EntityBlueJay.this.getTarget() != null && EntityBlueJay.this.getTarget().isAlive()) || EntityBlueJay.this.hasVehicle() || EntityBlueJay.this.aiItemFlag || EntityBlueJay.this.getSingTime() > 0) {
                return false;
            } else {
                if (EntityBlueJay.this.getRandom().nextInt(45) != 0 && !EntityBlueJay.this.isFlying()) {
                    return false;
                }
                if (EntityBlueJay.this.isOnGround()) {
                    this.flightTarget = random.nextBoolean();
                } else {
                    this.flightTarget = random.nextInt(5) > 0 && EntityBlueJay.this.timeFlying < 200;
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
                EntityBlueJay.this.getMoveControl().moveTo(this.x, this.y, this.z, 1F);
            } else {
                EntityBlueJay.this.getNavigation().startMovingTo(this.x, this.y, this.z, 1F);
            }
            if (!flightTarget && isFlying() && EntityBlueJay.this.isOnGround()) {
                EntityBlueJay.this.setFlying(false);
            }
            if (isFlying() && EntityBlueJay.this.isOnGround() && EntityBlueJay.this.timeFlying > 10) {
                EntityBlueJay.this.setFlying(false);
            }
        }

        @Nullable
        protected Vec3d getPosition() {
            Vec3d vector3d = EntityBlueJay.this.getPos();

            if(EntityBlueJay.this.isOverWaterOrVoid()){
                flightTarget = true;
            }
            if (flightTarget) {
                if (EntityBlueJay.this.timeFlying < 200 || EntityBlueJay.this.isOverWaterOrVoid()) {
                    return EntityBlueJay.this.getBlockInViewAway(vector3d, 0);
                } else {
                    return EntityBlueJay.this.getBlockGrounding(vector3d);
                }
            } else {
                return FuzzyTargeting.find(EntityBlueJay.this, 10, 7);
            }
        }

        @Override
        public boolean shouldContinue() {
            if (flightTarget) {
                return EntityBlueJay.this.isFlying() && EntityBlueJay.this.squaredDistanceTo(x, y, z) > 5F;
            } else {
                return (!EntityBlueJay.this.getNavigation().isIdle()) && !EntityBlueJay.this.hasPassengers();
            }
        }

        @Override
        public void start() {
            if (flightTarget) {
                EntityBlueJay.this.setFlying(true);
                EntityBlueJay.this.getMoveControl().moveTo(x, y, z, 1F);
            } else {
                EntityBlueJay.this.getNavigation().startMovingTo(this.x, this.y, this.z, 1F);
            }
        }

        @Override
        public void stop() {
            EntityBlueJay.this.getNavigation().stop();
            x = 0;
            y = 0;
            z = 0;
            super.stop();
        }

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
            this.theNearestAttackableTargetSorter = new AIScatter.Sorter(EntityBlueJay.this);
            this.targetEntitySelector = (Predicate<Entity>) e -> e.isAlive() && e.getType().isIn(AMTagRegistry.SCATTERS_CROWS) || e instanceof PlayerEntity && !((PlayerEntity) e).isCreative();
        }

        @Override
        public boolean canStart() {
            Entity entity = EntityBlueJay.this.getTarget();
            if (EntityBlueJay.this.hasVehicle()  || EntityBlueJay.this.hasPassengers() || entity != null && entity.isAlive() || EntityBlueJay.this.isTrusting()) {
                return false;
            }
            if (!this.mustUpdate) {
                long worldTime = EntityBlueJay.this.getWorld().getTime() % 10;
                if (EntityBlueJay.this.getDespawnCounter() >= 100 && worldTime != 0) {
                    return false;
                }
                if (EntityBlueJay.this.getRandom().nextInt(this.executionChance) != 0 && worldTime != 0) {
                    return false;
                }
            }
            List<Entity> list = EntityBlueJay.this.getWorld().getEntitiesByClass(Entity.class, this.getTargetableArea(this.getTargetDistance()), this.targetEntitySelector);
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
            flightTarget = null;
            this.targetEntity = null;
        }

        @Override
        public void tick() {
            if (cooldown > 0) {
                cooldown--;
            }
            if (flightTarget != null) {
                EntityBlueJay.this.setFlying(true);
                EntityBlueJay.this.getMoveControl().moveTo(flightTarget.x, flightTarget.y, flightTarget.z, 1F);
                if(cooldown == 0 && EntityBlueJay.this.isTargetBlocked(flightTarget)){
                    cooldown = 30;
                    flightTarget = null;
                }
            }

            if (targetEntity != null) {
                if (EntityBlueJay.this.isOnGround() || flightTarget == null || flightTarget != null && EntityBlueJay.this.squaredDistanceTo(flightTarget) < 3) {
                    Vec3d vec = EntityBlueJay.this.getBlockInViewAway(targetEntity.getPos(), 0);
                    if (vec != null && vec.y > EntityBlueJay.this.getY()) {
                        flightTarget = vec;
                    }
                }
                if (EntityBlueJay.this.distanceTo(targetEntity) > 20.0F) {
                    this.stop();
                }
            }
        }

        protected double getTargetDistance() {
            return 4D;
        }

        protected Box getTargetableArea(double targetDistance) {
            var renderCenter = new Vec3d(EntityBlueJay.this.getX(), EntityBlueJay.this.getY() + 0.5, EntityBlueJay.this.getZ());
            var aabb = new Box(-targetDistance, -targetDistance, -targetDistance, targetDistance, targetDistance, targetDistance);
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

    private boolean isTrusting() {
        return this.getFeedTime() > 0 || this.getSingTime() > 0 || this.getRaccoonUUID() != null || aiItemFlag;
    }

    private static class AITargetItems extends CreatureAITargetItems {

        public AITargetItems(PathAwareEntity creature, boolean checkSight, boolean onlyNearby, int tickThreshold, int radius) {
            super(creature, checkSight, onlyNearby, tickThreshold, radius);
            this.executionChance = 1;
        }

        @Override
        public void stop() {
            super.stop();
            ((EntityBlueJay) mob).aiItemFlag = false;
        }

        @Override
        public boolean canStart() {
            return super.canStart() && (mob.getTarget() == null || !mob.getTarget().isAlive());
        }

        @Override
        public boolean shouldContinue() {
            return super.shouldContinue() && (mob.getTarget() == null || !mob.getTarget().isAlive());
        }

        @Override
        protected void moveTo() {
            EntityBlueJay jay = (EntityBlueJay) mob;
            if (this.targetEntity != null) {
                jay.aiItemFlag = true;
                if (this.mob.distanceTo(targetEntity) < 2) {
                    jay.getMoveControl().moveTo(this.targetEntity.getX(), targetEntity.getY(), this.targetEntity.getZ(), 1);
                    jay.peck();
                }
                if (this.mob.distanceTo(this.targetEntity) > 8 || jay.isFlying()) {
                    jay.setFlying(true);
                    float f = (float) (jay.getX() - targetEntity.getX());
                    float f1 = 1.8F;
                    float f2 = (float) (jay.getZ() - targetEntity.getZ());
                    float xzDist = MathHelper.sqrt(f * f + f2 * f2);

                    if(!jay.canSee(targetEntity)){
                        jay.getMoveControl().moveTo(this.targetEntity.getX(), 1 + jay.getY(), this.targetEntity.getZ(), 1);
                    }else{
                        if (xzDist < 5) {
                            f1 = 0;
                        }
                        jay.getMoveControl().moveTo(this.targetEntity.getX(), f1 + this.targetEntity.getY(), this.targetEntity.getZ(), 1);
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

    private class AIFollowFeederOrRaccoon extends Goal {
        private Entity following;

        AIFollowFeederOrRaccoon() {
            this.setControls(EnumSet.of(Goal.Control.LOOK, Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if(EntityBlueJay.this.hasVehicle() || EntityBlueJay.this.getTarget() != null && EntityBlueJay.this.getTarget().isAlive()){
                return false;
            }
            if(EntityBlueJay.this.getRaccoonUUID() != null){
                Entity raccoon = EntityBlueJay.this.getRaccoon();
                if(raccoon != null){
                    following = raccoon;
                    return true;
                }
            }
            if(EntityBlueJay.this.getFeedTime() > 0){
                Entity feeder = EntityBlueJay.this.getLastFeeder();
                if(feeder != null){
                    following = feeder;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean shouldContinue(){
            var target = EntityBlueJay.this.getTarget();
            return following != null && following.isAlive() && (target == null || !target.isAlive()) && (following instanceof EntityRaccoon || EntityBlueJay.this.getFeedTime() > 0) && !EntityBlueJay.this.hasVehicle();
        }

        @Override
        public void tick() {
            double dist = EntityBlueJay.this.distanceTo(this.following);
            if (dist > 6 || EntityBlueJay.this.isFlying()) {
                EntityBlueJay.this.setFlying(true);
                EntityBlueJay.this.getMoveControl().moveTo(this.following.getX(), this.following.getY(), this.following.getZ(), 1);
            }else{
                EntityBlueJay.this.getNavigation().startMovingTo(this.following.getX(), this.following.getY(), this.following.getZ(), 1);
            }
            if(EntityBlueJay.this.isFlying() && EntityBlueJay.this.isOnGround() && dist < 3){
                EntityBlueJay.this.setFlying(false);
            }
            if (this.following instanceof EntityRaccoon raccoon) {
                if(dist > 40){
                    EntityBlueJay.this.teleport(this.following.getX(), this.following.getY(), this.following.getZ());
                }
                if(dist < 2.5F){
                    EntityBlueJay.this.getMoveControl().moveTo(this.following.getX(), this.following.getY(), this.following.getZ(), 1);
                }
                if(dist < 1F && raccoon.getPassengerList().isEmpty()){
                    EntityBlueJay.this.startRiding(raccoon, false);
                }
            }
        }
    }
}
