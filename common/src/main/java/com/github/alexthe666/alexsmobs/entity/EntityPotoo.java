package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AdvancedPathNavigateNoTeleport;
import com.github.alexthe666.alexsmobs.entity.ai.FlightMoveController;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.packet.MosquitoMountPlayerPacket;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.iafenvoy.uranus.object.entity.pathfinding.raycoms.AdvancedPathNavigate;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
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
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;

public class EntityPotoo extends AnimalEntity implements IFalconry {

    private static final TrackedData<Boolean> FLYING = DataTracker.registerData(EntityPotoo.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> PERCHING = DataTracker.registerData(EntityPotoo.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SLEEPING = DataTracker.registerData(EntityPotoo.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Optional<BlockPos>> PERCH_POS = DataTracker.registerData(EntityPotoo.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);
    private static final TrackedData<Direction> PERCH_DIRECTION = DataTracker.registerData(EntityPotoo.class, TrackedDataHandlerRegistry.FACING);
    private static final TrackedData<Integer> MOUTH_TICK = DataTracker.registerData(EntityPotoo.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TEMP_BRIGHTNESS = DataTracker.registerData(EntityPotoo.class, TrackedDataHandlerRegistry.INTEGER);
    public final float[] ringBuffer = new float[64];
    public float prevFlyProgress;
    public float flyProgress;
    public float mouthProgress;
    public float prevMouthProgress;
    public float prevPerchProgress;
    public float perchProgress;
    public int ringBufferIndex = -1;
    private int lastScreamTimestamp;
    private int perchCooldown = 100;
    private boolean isLandNavigator;
    private int timeFlying;

    public EntityPotoo(EntityType<EntityPotoo> type, World level) {
        super(type, level);
        switchNavigator(true);
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
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new TemptGoal(this, 1.0D, Ingredient.fromTag(AMTagRegistry.POTOO_BREEDABLES), false));
        this.goalSelector.add(2, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(3, new EscapeDangerGoal(this, 1D));
        this.goalSelector.add(4, new AIPerch());
        this.goalSelector.add(5, new AIMelee());
        this.goalSelector.add(6, new AIFlyIdle());
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, EntityFly.class, 100, true, true, null));
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new AdvancedPathNavigateNoTeleport(this, getWorld(), false);
            this.isLandNavigator = true;
        } else {
            this.moveControl = new FlightMoveController(this, 0.6F, false, true);
            this.navigation = new AdvancedPathNavigateNoTeleport(this, getWorld(), AdvancedPathNavigate.MovementType.FLYING, false, false) {
                public boolean isValidPosition(BlockPos pos) {
                    return !this.world.getBlockState(pos.down(2)).isAir();
                }
            };
            navigation.setCanSwim(false);
            this.isLandNavigator = false;
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FLYING, false);
        this.dataTracker.startTracking(PERCHING, false);
        this.dataTracker.startTracking(PERCH_POS, Optional.empty());
        this.dataTracker.startTracking(PERCH_DIRECTION, Direction.NORTH);
        this.dataTracker.startTracking(SLEEPING, false);
        this.dataTracker.startTracking(MOUTH_TICK, 0);
        this.dataTracker.startTracking(TEMP_BRIGHTNESS, 0);
    }

    @Override
    public boolean isSleeping() {
        return this.dataTracker.get(SLEEPING);
    }

    public void setSleeping(boolean sleeping) {
        this.dataTracker.set(SLEEPING, sleeping);
    }

    public static boolean canPotooSpawn(EntityType<?> type, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        var blockstate = worldIn.getBlockState(pos.down());
        return blockstate.isIn(BlockTags.LEAVES) && isLightLevelValidForNaturalSpawn(worldIn, pos);
    }

    @Override
    public boolean canSpawn(WorldView reader) {
        if (reader.doesNotIntersectEntities(this) && !reader.containsFluid(this.getBoundingBox())) {
            BlockPos blockpos = this.getBlockPos();
            BlockState blockstate2 = reader.getBlockState(blockpos.down());
            return blockstate2.isIn(BlockTags.LEAVES) || blockstate2.isIn(BlockTags.LOGS);
        }
        return false;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.potooSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    public void tick() {
        super.tick();
        this.prevPerchProgress = perchProgress;
        this.prevMouthProgress = mouthProgress;
        this.prevFlyProgress = flyProgress;

        if (this.isFlying()) {
            if (flyProgress < 5F)
                flyProgress++;
        } else {
            if (flyProgress > 0F)
                flyProgress--;
        }

        if (this.isPerching()) {
            if (perchProgress < 5F)
                perchProgress++;
        } else {
            if (perchProgress > 0F)
                perchProgress--;
        }

        if (this.ringBufferIndex < 0) {
            //initial population of buffer
            Arrays.fill(this.ringBuffer, 15);
        }
        this.ringBufferIndex++;
        if (this.ringBufferIndex == this.ringBuffer.length) {
            this.ringBufferIndex = 0;
        }
        this.ringBuffer[this.ringBufferIndex] = this.dataTracker.get(TEMP_BRIGHTNESS);
        if (perchCooldown > 0) {
            perchCooldown--;
        }
        if (!this.getWorld().isClient) {
            this.dataTracker.set(TEMP_BRIGHTNESS, getWorld().getLightLevel(this.getBlockPos()));
            if (isFlying()) {
                if (this.isLandNavigator)
                    switchNavigator(false);
            } else {
                if (!this.isLandNavigator)
                    switchNavigator(true);
            }

            if (this.isFlying()) {
                if (!this.isOnGround()) {
                    if (!this.isInsideWaterOrBubbleColumn()) {
                        this.setVelocity(this.getVelocity().multiply(1F, 0.6F, 1F));
                    }
                } else if (timeFlying > 20) {
                    this.setFlying(false);
                }
                this.timeFlying++;
            } else {
                this.timeFlying = 0;
            }
            if (this.isPerching() && !this.hasPassengers()) {
                this.setSleeping(this.getWorld().isDay() && (this.getTarget() == null || !this.getTarget().isAlive()));
            } else if (isSleeping()) {
                this.setSleeping(false);
            }
            if(isPerching() && this.getPerchPos() != null) {
                if ((!getWorld().getBlockState(this.getPerchPos()).isIn(AMTagRegistry.POTOO_PERCHES) || this.squaredDistanceTo(Vec3d.ofCenter(this.getPerchPos())) > 2.25F)) {
                    this.setPerching(false);
                } else {
                    slideTowardsPerch();
                }
            }
        }
        if (this.dataTracker.get(MOUTH_TICK) > 0) {
            this.dataTracker.set(MOUTH_TICK, this.dataTracker.get(MOUTH_TICK) - 1);
            if (mouthProgress < 5F) {
                mouthProgress++;
            }
        } else {
            if (mouthProgress > 0F) {
                mouthProgress--;
            }
        }
        if (!isSleeping() && (this.getTarget() == null || !this.getTarget().isAlive())) {
            int j = this.age - lastScreamTimestamp;
            if (getEyeScale(10, 1.0F) == 0F) {
                if (j > 40) {
                    this.openMouth(30);
                    this.playSoundIfNotSilent(AMSoundRegistry.POTOO_CALL.get());
                    this.emitGameEvent(GameEvent.ENTITY_ROAR);
                }
            } else if (getEyeScale(10, 1.0F) < 7) {
                if (j > 300 && j % 300 == 0 && random.nextInt(4) == 0) {
                    this.openMouth(30);
                    this.playSoundIfNotSilent(AMSoundRegistry.POTOO_CALL.get());
                    this.emitGameEvent(GameEvent.ENTITY_ROAR);
                }
            }
        }
    }

    @Override
    public float getHandOffset(){
        return 1.0F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.POTOO_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.POTOO_HURT.get();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean prev = super.damage(source, amount);
        if (prev && source.getSource() instanceof LivingEntity) {
            this.setPerching(false);
        }
        return prev;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.IN_WALL)  || super.isInvulnerableTo(source);
    }

    @Override
    public void tickRiding() {
        var entity = this.getVehicle();
        if (this.hasVehicle() && (!entity.isAlive() || !this.isAlive())) {
            this.stopRiding();
        } else if (entity instanceof LivingEntity) {
            this.setVelocity(0, 0, 0);
            this.tick();
            this.setFlying(false);
            this.setSleeping(false);
            this.setPerching(false);
            if (this.hasVehicle()) {
                var mount = this.getVehicle();
                if (mount instanceof PlayerEntity playerEntity) {
                    float yawAdd = 0;
                    if (playerEntity.getStackInHand(Hand.MAIN_HAND).getItem() == AMItemRegistry.FALCONRY_GLOVE.get()) {
                        yawAdd = playerEntity.getMainArm() == Arm.LEFT ? 135 : -135;
                    } else if (playerEntity.getStackInHand(Hand.OFF_HAND).getItem() == AMItemRegistry.FALCONRY_GLOVE.get()) {
                        yawAdd = playerEntity.getMainArm() == Arm.LEFT ? -135 : 135;
                    } else {
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

    public void openMouth(int duration) {
        this.dataTracker.set(MOUTH_TICK, duration);
        lastScreamTimestamp = this.age;
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

    public BlockPos getPerchPos() {
        return this.dataTracker.get(PERCH_POS).orElse(null);
    }

    public void setPerchPos(BlockPos pos) {
        this.dataTracker.set(PERCH_POS, Optional.ofNullable(pos));
    }

    public Direction getPerchDirection() {
        return this.dataTracker.get(PERCH_DIRECTION);
    }

    public void setPerchDirection(Direction direction) {
        this.dataTracker.set(PERCH_DIRECTION, direction);
    }

    public boolean isPerching() {
        return this.dataTracker.get(PERCHING);
    }

    public void setPerching(boolean perching) {
        this.dataTracker.set(PERCHING, perching);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Flying", this.isFlying());
        compound.putBoolean("Perching", this.isPerching());
        compound.putInt("PerchDir", this.getPerchDirection().ordinal());
        if (this.getPerchPos() != null) {
            compound.putInt("PerchX", this.getPerchPos().getX());
            compound.putInt("PerchY", this.getPerchPos().getY());
            compound.putInt("PerchZ", this.getPerchPos().getZ());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setFlying(compound.getBoolean("Flying"));
        this.setPerching(compound.getBoolean("Perching"));
        this.setPerchDirection(Direction.byId(compound.getInt("PerchDir")));
        if (compound.contains("PerchX") && compound.contains("PerchY") && compound.contains("PerchZ")) {
            this.setPerchPos(new BlockPos(compound.getInt("PerchX"), compound.getInt("PerchY"), compound.getInt("PerchZ")));
        }
    }

    public boolean isValidPerchFromSide(BlockPos pos, Direction direction) {
        BlockPos offset = pos.offset(direction);
        BlockState state = getWorld().getBlockState(pos);
        return state.isIn(AMTagRegistry.POTOO_PERCHES) && (!getWorld().getBlockState(pos.up()).isFullCube(getWorld(), pos.up()) || getWorld().isAir(pos.up())) && (!getWorld().getBlockState(offset).isFullCube(getWorld(), offset) && !getWorld().getBlockState(offset).isIn(AMTagRegistry.POTOO_PERCHES) || getWorld().isAir(offset));
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld serverLevel, PassiveEntity ageableMob) {
        return AMEntityRegistry.POTOO.get().create(serverLevel);
    }

    public float getEyeScale(int bufferOffset, float partialTicks) {
        int i = (this.ringBufferIndex - bufferOffset) & 63;
        int j = (this.ringBufferIndex - bufferOffset - 1) & 63;
        float prevBuffer = this.ringBuffer[j];
        float buffer = this.ringBuffer[i];
        return prevBuffer + (buffer - prevBuffer) * partialTicks;
    }

    private void slideTowardsPerch() {
        var block = Vec3d.ofCenter(this.getPerchPos(), 1.0F);
        var look = block.subtract(this.getPos()).normalize();
        var onBlock = block.add(this.getPerchDirection().getOffsetX() * 0.35F, 0F, this.getPerchDirection().getOffsetZ() * 0.35F);
        var diff = onBlock.subtract(this.getPos());
        float f = (float)diff.length();
        float f1 = f > 1F ? 0.25F : f * 0.1F;
        var sub = diff.normalize().multiply(f1);
        float f2 = -(float) (MathHelper.atan2(look.x, look.z) * (double) MathHelper.DEGREES_PER_RADIAN);
        EntityPotoo.this.setYaw(f2);
        EntityPotoo.this.headYaw= f2;
        EntityPotoo.this.bodyYaw = f2;

        this.setVelocity(this.getVelocity().add(sub));

    }

    public BlockPos getToucanGround(BlockPos in) {
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
        BlockPos ground = this.getToucanGround(radialPos);
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

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        ActionResult type = super.interactMob(player, hand);
        if (!this.isBaby() && getRidingFalcons(player) <= 0 && (player.getStackInHand(Hand.MAIN_HAND).getItem() == AMItemRegistry.FALCONRY_GLOVE.get() || player.getStackInHand(Hand.OFF_HAND).getItem() == AMItemRegistry.FALCONRY_GLOVE.get())) {
            ridingCooldown = 30;
            this.removeAllPassengers();
            this.startRiding(player, true);
            if (!this.getWorld().isClient) {
                AlexsMobs.sendMSGToAll(new MosquitoMountPlayerPacket(this.getId(), player.getId()));
            }
            return ActionResult.SUCCESS;
        } else {
            return type;
        }
    }

    public boolean isTargetBlocked(Vec3d target) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());

        return this.getWorld().raycast(new RaycastContext(Vector3d, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() != HitResult.Type.MISS;
    }

    public Vec3d getBlockInViewAway(Vec3d fleePos, float radiusAdd) {
        final float radius = 5 + radiusAdd + this.getRandom().nextInt(5);
        final float neg = this.getRandom().nextBoolean() ? 1 : -1;
        final float renderYawOffset = this.bodyYaw;
        final float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        final double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = new BlockPos((int) (fleePos.x + extraX), 0, (int) (fleePos.z + extraZ));
        BlockPos ground = getToucanGround(radialPos);
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
    public boolean handleFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    private boolean isOverWaterOrVoid() {
        BlockPos position = this.getBlockPos();
        while (position.getY() > -65 && getWorld().isAir(position)) {
            position = position.down();
        }
        return !getWorld().getFluidState(position).isEmpty() || getWorld().getBlockState(position).isOf(Blocks.VINE) || position.getY() <= -65;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.POTOO_BREEDABLES);
    }

    @Override
    public void onLaunch(PlayerEntity player, Entity pointedEntity) {
    }

    private class AIFlyIdle extends Goal {
        protected double x;
        protected double y;
        protected double z;

        public AIFlyIdle() {
            super();
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (EntityPotoo.this.hasPassengers() || EntityPotoo.this.isPerching() || (EntityPotoo.this.getTarget() != null && EntityPotoo.this.getTarget().isAlive()) || EntityPotoo.this.hasVehicle()) {
                return false;
            } else {
                if (EntityPotoo.this.getRandom().nextInt(45) != 0 && !EntityPotoo.this.isFlying()) {
                    return false;
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
            EntityPotoo.this.getMoveControl().moveTo(this.x, this.y, this.z, 1F);
            if (isFlying() && EntityPotoo.this.isOnGround() && EntityPotoo.this.timeFlying > 10) {
                EntityPotoo.this.setFlying(false);
            }
        }

        @Nullable
        protected Vec3d getPosition() {
            Vec3d vector3d = EntityPotoo.this.getPos();
            if (EntityPotoo.this.timeFlying < 200 || EntityPotoo.this.isOverWaterOrVoid()) {
                return EntityPotoo.this.getBlockInViewAway(vector3d, 0);
            } else {
                return EntityPotoo.this.getBlockGrounding(vector3d);
            }
        }

        @Override
        public boolean shouldContinue() {
            return EntityPotoo.this.isFlying() && EntityPotoo.this.squaredDistanceTo(x, y, z) > 5F;
        }

        @Override
        public void start() {
            EntityPotoo.this.setFlying(true);
            EntityPotoo.this.getMoveControl().moveTo(this.x, this.y, this.z, 1F);
        }

        @Override
        public void stop() {
            EntityPotoo.this.getNavigation().stop();
            x = 0;
            y = 0;
            z = 0;
            super.stop();
        }
    }

    private class AIPerch extends Goal {
        private BlockPos perch = null;
        private Direction perchDirection = null;
        private int perchingTime = 0;
        private int runCooldown = 0;

        private int pathRecalcTime = 0;

        public AIPerch() {
            this.setControls(EnumSet.of(Goal.Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (EntityPotoo.this.getTarget() != null && EntityPotoo.this.getTarget().isAlive()) {
                return false;
            }
            if(runCooldown > 0){
                runCooldown--;
            }else if (!EntityPotoo.this.isPerching() && EntityPotoo.this.perchCooldown == 0 && EntityPotoo.this.random.nextInt(35) == 0) {
                this.perchingTime = 0;
                if (EntityPotoo.this.getPerchPos() != null && EntityPotoo.this.isValidPerchFromSide(EntityPotoo.this.getPerchPos(), EntityPotoo.this.getPerchDirection())) {
                    perch = EntityPotoo.this.getPerchPos();
                    perchDirection = EntityPotoo.this.getPerchDirection();
                } else {
                    findPerch();
                }
                runCooldown = 120 + EntityPotoo.this.getRandom().nextInt(140);
                return perch != null && perchDirection != null;
            }
            return false;
        }

        private void findPerch() {
            Random random = EntityPotoo.this.getRandom();
            Direction[] horiz = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
            if (isValidPerchFromSide(EntityPotoo.this.getVelocityAffectingPos(), EntityPotoo.this.getHorizontalFacing())) {
                perch = EntityPotoo.this.getVelocityAffectingPos();
                perchDirection = EntityPotoo.this.getHorizontalFacing();
                return;
            }
            for(Direction dir : horiz){
                if (isValidPerchFromSide(EntityPotoo.this.getVelocityAffectingPos(), dir)) {
                    perch = EntityPotoo.this.getVelocityAffectingPos();
                    perchDirection = dir;
                    return;
                }
            }
            int range = 14;
            for (int i = 0; i < 15; i++) {
                BlockPos blockpos1 = EntityPotoo.this.getBlockPos().add(random.nextInt(range) - range / 2, 3, random.nextInt(range) - range / 2);
                if(!EntityPotoo.this.getWorld().isChunkLoaded(blockpos1)){
                    continue;
                }
                while (EntityPotoo.this.getWorld().isAir(blockpos1) && blockpos1.getY() > -64) {
                    blockpos1 = blockpos1.down();
                }
                Direction dir = Direction.fromHorizontal(random.nextInt(3));
                if (isValidPerchFromSide(blockpos1, dir)) {
                    perch = blockpos1;
                    perchDirection = dir;
                    break;
                }
            }
        }

        @Override
        public void start(){
            pathRecalcTime = 0;
        }

        @Override
        public boolean shouldContinue() {
            return (perchingTime < 300 || EntityPotoo.this.getWorld().isDay()) && (EntityPotoo.this.getTarget() == null || !EntityPotoo.this.getTarget().isAlive()) && !EntityPotoo.this.hasVehicle();
        }

        @Override
        public void tick() {
            if (EntityPotoo.this.isPerching()) {
                perchingTime++;
                EntityPotoo.this.getNavigation().stop();
                var block = Vec3d.ofCenter(EntityPotoo.this.getPerchPos(), 1.0F);
                var onBlock = block.add(EntityPotoo.this.getPerchDirection().getOffsetX() * 0.35F, 0F, EntityPotoo.this.getPerchDirection().getOffsetZ() * 0.35F);
                final double dist = EntityPotoo.this.squaredDistanceTo(onBlock);
                var dirVec = block.subtract(EntityPotoo.this.getPos());
                if (perchingTime > 10 && (dist > 2.3F || !EntityPotoo.this.isValidPerchFromSide(EntityPotoo.this.getPerchPos(), EntityPotoo.this.getPerchDirection()))) {
                    EntityPotoo.this.setPerching(false);
                } else if (dist > 1F) {
                    EntityPotoo.this.slideTowardsPerch();
                    if (EntityPotoo.this.getPerchPos().getY() + 1.2F > EntityPotoo.this.getBoundingBox().minY) {
                        EntityPotoo.this.setVelocity(EntityPotoo.this.getVelocity().add(0, 0.2F, 0));
                    }
                    final float f = -(float) (MathHelper.atan2(dirVec.x, dirVec.z) * (double) MathHelper.DEGREES_PER_RADIAN);
                    EntityPotoo.this.setYaw(f);
                    EntityPotoo.this.headYaw= f;
                    EntityPotoo.this.bodyYaw = f;
                }
            } else if (perch != null) {
                if (EntityPotoo.this.squaredDistanceTo(Vec3d.ofCenter(perch)) > 100) {
                    EntityPotoo.this.setFlying(true);
                }
                final double distX = perch.getX() + 0.5F - EntityPotoo.this.getX();
                final double distZ = perch.getZ() + 0.5F - EntityPotoo.this.getZ();
                if (distX * distX + distZ * distZ < 1F || !EntityPotoo.this.isFlying()) {
                    if(pathRecalcTime <= 0){
                        pathRecalcTime = EntityPotoo.this.getRandom().nextInt(30) + 30;
                        EntityPotoo.this.getNavigation().startMovingTo(perch.getX() + 0.5F, perch.getY() + 1.5F, perch.getZ() + 0.5F, 1F);
                    }
                    if(EntityPotoo.this.getNavigation().isIdle()){
                        EntityPotoo.this.getMoveControl().moveTo(perch.getX() + 0.5F, perch.getY() + 1.5F, perch.getZ() + 0.5F, 1F);
                    }
                } else {
                    if(pathRecalcTime <= 0) {
                        pathRecalcTime = EntityPotoo.this.getRandom().nextInt(30) + 30;
                        EntityPotoo.this.getNavigation().startMovingTo(perch.getX() + 0.5F, perch.getY() + 2.5F, perch.getZ() + 0.5F, 1F);
                    }
                }
                if (EntityPotoo.this.getVelocityAffectingPos().equals(perch)) {
                    EntityPotoo.this.setVelocity(Vec3d.ZERO);
                    EntityPotoo.this.setPerching(true);
                    EntityPotoo.this.setFlying(false);
                    EntityPotoo.this.setPerchPos(perch);
                    EntityPotoo.this.setPerchDirection(perchDirection);
                    EntityPotoo.this.getNavigation().stop();
                    perch = null;
                } else {
                    EntityPotoo.this.setPerching(false);
                }
            }
            if(pathRecalcTime > 0){
                pathRecalcTime--;
            }
        }

        @Override
        public void stop() {
            EntityPotoo.this.setPerching(false);
            EntityPotoo.this.perchCooldown = 120 + random.nextInt(1200);
            this.perch = null;
            this.perchDirection = null;
        }
    }

    private class AIMelee extends Goal {

        private int biteCooldown = 0;

        public AIMelee() {
            this.setControls(EnumSet.of(Goal.Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return !EntityPotoo.this.isSleeping() && !EntityPotoo.this.hasVehicle() && EntityPotoo.this.getTarget() != null && EntityPotoo.this.getTarget().isAlive();
        }

        @Override
        public void tick() {
            if (biteCooldown > 0) {
                biteCooldown--;
            }
            LivingEntity entity = EntityPotoo.this.getTarget();
            if (entity != null) {
                EntityPotoo.this.setFlying(true);
                EntityPotoo.this.setPerching(false);
                EntityPotoo.this.getMoveControl().moveTo(entity.getX(), entity.getBodyY(0.5F), entity.getZ(), 1.5F);
                if (EntityPotoo.this.distanceTo(entity) < 1.4F) {
                    if (biteCooldown == 0) {
                        EntityPotoo.this.openMouth(7);
                        biteCooldown = 10;
                    }
                    if (EntityPotoo.this.mouthProgress >= 4.5F) {
                        entity.damage(EntityPotoo.this.getDamageSources().mobAttack(EntityPotoo.this), 2);
                        if (entity.getWidth() <= 0.5F) {
                            entity.remove(RemovalReason.KILLED);
                        }
                    }
                }
            }

        }
    }

}
