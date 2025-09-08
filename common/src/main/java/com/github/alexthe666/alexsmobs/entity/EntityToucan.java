package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.DirectPathNavigator;
import com.github.alexthe666.alexsmobs.entity.ai.FlightMoveController;
import com.github.alexthe666.alexsmobs.entity.ai.FlyingAITargetDroppedItems;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityData;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EntityToucan extends AnimalEntity implements ITargetsDroppedItems {

    private static final TrackedData<Boolean> FLYING = DataTracker.registerData(EntityToucan.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> PECK_TICK = DataTracker.registerData(EntityToucan.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(EntityToucan.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> GOLDEN_TIME = DataTracker.registerData(EntityToucan.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> ENCHANTED = DataTracker.registerData(EntityToucan.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Optional<BlockState>> SAPLING_STATE = DataTracker.registerData(EntityToucan.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_STATE);
    private static final TrackedData<Integer> SAPLING_TIME = DataTracker.registerData(EntityToucan.class, TrackedDataHandlerRegistry.INTEGER);
    private static final HashMap<String, String> FEEDING_DATA = new HashMap<>();
    private static final List<ItemStack> FEEDING_STACKS = new ArrayList<>();
    private static boolean initFeedingData = false;
    public float prevFlyProgress;
    public float flyProgress;
    public float prevPeckProgress;
    public float peckProgress;
    private boolean isLandNavigator;
    private int timeFlying;
    private int heldItemTime;
    private boolean aiItemFlag;

    public EntityToucan(EntityType<? extends EntityToucan> type, World worldIn) {
        super(type, worldIn);
        initFeedingData();
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, -1.0F);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, -1.0F);
        this.setPathfindingPenalty(PathNodeType.COCOA, -1.0F);
        this.setPathfindingPenalty(PathNodeType.LEAVES, 0.0F);
        switchNavigator(true);
    }

    public static boolean canToucanSpawn(EntityType type, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        return true;
    }

    private static void initFeedingData() {
        if (!initFeedingData || FEEDING_DATA.isEmpty()) {
            initFeedingData = true;
            for (String str : AMConfig.toucanFruitMatches) {
                String[] split = str.split("\\|");
                if (split.length >= 2) {
                    FEEDING_DATA.put(split[0], split[1]);
                    FEEDING_STACKS.add(new ItemStack(Registries.ITEM.get(new Identifier(split[0]))));
                }
            }
        }
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 6.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.TOUCAN_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.TOUCAN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.TOUCAN_HURT.get();
    }

    @Override
    public boolean canSpawn(WorldView p_29005_) {
        if (p_29005_.doesNotIntersectEntities(this) && !p_29005_.containsFluid(this.getBoundingBox())) {
            BlockPos blockpos = this.getBlockPos();
            if (blockpos.getY() < p_29005_.getSeaLevel()) {
                return false;
            }
            var blockstate2 = p_29005_.getBlockState(blockpos.down());
            return blockstate2.isOf(Blocks.GRASS_BLOCK) || blockstate2.isIn(BlockTags.LEAVES);
        }
        return false;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.toucanSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Nullable
    private BlockState getSaplingFor(ItemStack stack) {
        var name = Registries.ITEM.getId(stack.getItem());
        if (!stack.isEmpty() && name != null && FEEDING_DATA.containsKey(name.toString())) {
            String str = FEEDING_DATA.get(name.toString());
            var block = Registries.BLOCK.get(new Identifier(str));
            if (block != null) {
                return block.getDefaultState();
            }
        }
        return null;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        ActionResult type = super.interactMob(player, hand);
        if (getSaplingFor(itemstack) != null && this.getSaplingTime() <= 0 && this.getMainHandStack().isEmpty()) {
            peck();
            ItemStack duplicate = itemstack.copy();
            duplicate.setCount(1);
            this.setStackInHand(Hand.MAIN_HAND, duplicate);
            this.eat(player, hand, itemstack);
            return ActionResult.SUCCESS;
        } else {
            return type;
        }
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        initFeedingData();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.3D));
        this.goalSelector.add(2, new AIPlantTrees());
        this.goalSelector.add(3, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(4, new TemptGoal(this, 1.0D, Ingredient.ofStacks(FEEDING_STACKS.stream()), false) {
            @Override
            public boolean canStart() {
                return !EntityToucan.this.aiItemFlag && super.canStart();
            }
        });
        this.goalSelector.add(5, new AIWanderIdle());
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PathAwareEntity.class, 6.0F));
        this.goalSelector.add(9, new LookAroundGoal(this));
        this.targetSelector.add(1, new FlyingAITargetDroppedItems(this, false, false, 15, 16));
    }

    @Override
    public void setItemFlag(boolean itemAIFlag) {
        aiItemFlag = itemAIFlag;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        var item = stack.getItem();
        return stack.isIn(AMTagRegistry.TOUCAN_BREEDABLES);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SAPLING_STATE, Optional.empty());
        this.dataTracker.startTracking(FLYING, false);
        this.dataTracker.startTracking(PECK_TICK, 0);
        this.dataTracker.startTracking(VARIANT, 0);
        this.dataTracker.startTracking(GOLDEN_TIME, 0);
        this.dataTracker.startTracking(SAPLING_TIME, 0);
        this.dataTracker.startTracking(ENCHANTED, false);
    }

    @Override
    public boolean isClimbing() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        prevFlyProgress = flyProgress;
        prevPeckProgress = peckProgress;
        if (this.getGoldenTime() > 0 && !this.getWorld().isClient) {
            this.setGoldenTime(this.getGoldenTime() - 1);
        }

        final boolean flying = isFlying();
        if (flying) {
            if (flyProgress < 5F)
                flyProgress++;
        } else {
            if (flyProgress > 0F)
                flyProgress--;
        }
        if (!this.getWorld().isClient) {
            if (flying) {
                if (this.isLandNavigator)
                    switchNavigator(false);
            } else {
                if (!this.isLandNavigator)
                    switchNavigator(true);
            }
            if (flying) {
                this.setNoGravity(true);
                if (this.isFlying() && !this.isOnGround()) {
                    if (!this.isInsideWaterOrBubbleColumn()) {
                        this.setVelocity(this.getVelocity().multiply(1F, 0.6F, 1F));
                    }
                }
                this.timeFlying++;
            } else {
                this.setNoGravity(false);
                this.timeFlying = 0;
            }
        }
        if (this.dataTracker.get(PECK_TICK) > 0) {
            this.dataTracker.set(PECK_TICK, this.dataTracker.get(PECK_TICK) - 1);
            if (peckProgress < 5F) {
                peckProgress++;
            }
        } else {
            if (peckProgress > 0F) {
                peckProgress--;
            }
        }
        if (peckProgress >= 5 && this.getMainHandStack().isEmpty() && this.getSaplingState() != null) {
            peckBlockEffect();
        }
        if (!this.getMainHandStack().isEmpty()) {
            heldItemTime++;
            if (heldItemTime > 10 && canTargetItem(this.getMainHandStack())) {
                heldItemTime = 0;
                this.heal(4);
                this.emitGameEvent(GameEvent.EAT);
                this.playSound(SoundEvents.ENTITY_GENERIC_EAT, this.getSoundVolume(), this.getSoundPitch());
                if (this.getMainHandStack().getItem().hasRecipeRemainder()) {
                    this.dropItem(this.getMainHandStack().getItem().getRecipeRemainder());
                }
                final var mainHandItem = this.getMainHandStack();
                if (mainHandItem.isIn(AMTagRegistry.TOUCAN_GOLDEN_FOODS)) {
                    this.setGoldenTime(12000);
                } else if (mainHandItem.isIn(AMTagRegistry.TOUCAN_ENCHANTED_GOLDEN_FOODS)) {
                    this.setGoldenTime(-1);
                    this.setEnchanted(true);
                }
                this.setSaplingState(getSaplingFor(this.getMainHandStack()));
                eatItemEffect(this.getMainHandStack());
                this.getMainHandStack().decrement(1);
            }
        } else {
            heldItemTime = 0;
        }
        if (this.isFlying() && this.getBlockStateAtPos().isOf(Blocks.VINE)) {
            float f = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
            this.setVelocity(this.getVelocity().add(-MathHelper.sin(f) * 0.2F, 0.4F, MathHelper.cos(f) * 0.2F));
        }
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean isFlying() {
        return this.dataTracker.get(FLYING);
    }

    @Override
    public void setFlying(boolean flying) {
        if (flying && isBaby()) {
            return;
        }
        this.dataTracker.set(FLYING, flying);
    }

    @Override
    public void peck() {
        if (this.peckProgress == 0) {
            this.dataTracker.set(PECK_TICK, 7);
        }
    }

    public Vec3d getBlockInViewAway(Vec3d fleePos, float radiusAdd) {
        final float radius = 7 + radiusAdd + this.getRandom().nextInt(8);
        final float neg = this.getRandom().nextBoolean() ? 1 : -1;
        final float renderYawOffset = this.bodyYaw;
        final float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        final double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = new BlockPos((int) (fleePos.x + extraX), 0, (int) (fleePos.z + extraZ));
        BlockPos ground = getToucanGround(radialPos);
        final int distFromGround = (int) this.getY() - ground.getY();
        final int flightHeight = 8 + this.getRandom().nextInt(4);
        final int j = this.getRandom().nextInt(6) + 18;

        BlockPos newPos = ground.up(distFromGround > 9 ? flightHeight : j);
        if (getWorld().getBlockState(ground).isIn(BlockTags.LEAVES)) {
            newPos = ground.up(1 + this.getRandom().nextInt(3));
        }
        if (!this.isTargetBlocked(Vec3d.ofCenter(newPos)) && this.squaredDistanceTo(Vec3d.ofCenter(newPos)) > 1) {
            return Vec3d.ofCenter(newPos);
        }
        return null;
    }

    public BlockPos getToucanGround(BlockPos in) {
        var position = new BlockPos(in.getX(), (int) this.getY(), in.getZ());
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

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new MobNavigation(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new FlightMoveController(this, 0.6F, false, true);
            this.navigation = new DirectPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        BlockState blockstate = this.getSaplingState();
        if (blockstate != null) {
            compound.put("SaplingState", NbtHelper.fromBlockState(blockstate));
        }
        compound.putInt("Variant", this.getVariant());
        compound.putInt("GoldenTime", this.getGoldenTime());
        compound.putBoolean("Enchanted", this.isEnchanted());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        BlockState blockstate = null;
        if (compound.contains("SaplingState", 10)) {
            blockstate = NbtHelper.toBlockState(this.getWorld().createCommandRegistryWrapper(RegistryKeys.BLOCK), compound.getCompound("SaplingState"));
            if (blockstate.isAir()) {
                blockstate = null;
            }
        }
        this.setSaplingState(blockstate);
        this.setVariant(compound.getInt("Variant"));
        this.setGoldenTime(compound.getInt("GoldenTime"));
        this.setEnchanted(compound.getBoolean("Enchanted"));
    }

    public boolean isSam() {
        var s = Formatting.strip(this.getName().getString());
        return s != null && s.toLowerCase().contains("sam");
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, variant);
    }

    public int getSaplingTime() {
        return this.dataTracker.get(SAPLING_TIME);
    }

    public void setSaplingTime(int time) {
        this.dataTracker.set(SAPLING_TIME, time);
    }

    public boolean isGolden() {
        return this.getGoldenTime() > 0 || this.getGoldenTime() == -1 || this.isEnchanted();
    }

    public int getGoldenTime() {
        return this.dataTracker.get(GOLDEN_TIME);
    }

    public void setGoldenTime(int goldenTime) {
        this.dataTracker.set(GOLDEN_TIME, goldenTime);
    }

    public boolean isEnchanted() {
        return this.dataTracker.get(ENCHANTED);
    }

    public void setEnchanted(boolean enchanted) {
        this.dataTracker.set(ENCHANTED, enchanted);
    }

    @Nullable
    public BlockState getSaplingState() {
        return this.dataTracker.get(SAPLING_STATE).orElse(null);
    }

    public void setSaplingState(@Nullable BlockState state) {
        this.dataTracker.set(SAPLING_STATE, Optional.ofNullable(state));
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.setVariant(this.getRandom().nextInt(4));
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld level, PassiveEntity parent) {
        EntityToucan toucan = AMEntityRegistry.TOUCAN.get().create(getWorld());
        toucan.setVariant(this.getVariant());
        return toucan;
    }

    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView worldIn) {
        return worldIn.getBlockState(pos).isIn(BlockTags.LEAVES) ? 10.0F : super.getPathfindingFavor(pos, worldIn);
    }

    private boolean isOverWaterOrVoid() {
        BlockPos position = this.getBlockPos();
        while (position.getY() > -62 && getWorld().isAir(position)) {
            position = position.down();
        }
        return !getWorld().getFluidState(position).isEmpty() || getWorld().getBlockState(position).isOf(Blocks.VINE) || position.getY() <= 0;
    }

    private boolean isOverLeaves() {
        BlockPos position = this.getBlockPos();
        while (position.getY() > -62 && getWorld().isAir(position)) {
            position = position.down();
        }
        return getWorld().getBlockState(position).isIn(BlockTags.LEAVES) || getWorld().getBlockState(position).isOf(Blocks.VINE);
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return this.getSaplingTime() <= 0 && getSaplingFor(stack) != null;
    }

    private void peckBlockEffect() {
        var beneath = this.getSteppingBlockState();
        if (this.getWorld().isClient && !beneath.isAir() && beneath.getFluidState().isEmpty()) {
            for (int i = 0; i < 2 + random.nextInt(2); i++) {
                final double d2 = this.random.nextGaussian() * 0.02D;
                final double d0 = this.random.nextGaussian() * 0.02D;
                final double d1 = this.random.nextGaussian() * 0.02D;
                final float radius = this.getWidth() * 0.65F;
                final float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
                final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
                final double extraZ = radius * MathHelper.cos(angle);
                ParticleEffect data = new BlockStateParticleEffect(ParticleTypes.BLOCK, beneath);
                this.getWorld().addParticle(data, this.getX() + extraX, this.getY() + 0.1F, this.getZ() + extraZ, d0, d1, d2);
            }
        }
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

    @Override
    public void onGetItem(ItemEntity e) {
        ItemStack duplicate = e.getStack().copy();
        duplicate.setCount(1);
        if (!this.getStackInHand(Hand.MAIN_HAND).isEmpty() && !this.getWorld().isClient) {
            this.dropStack(this.getStackInHand(Hand.MAIN_HAND), 0.0F);
        }
        peck();
        this.setFlying(true);
        this.setStackInHand(Hand.MAIN_HAND, duplicate);
    }

    private boolean hasLineOfSightSapling(BlockPos destinationBlock) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        var blockVec = Vec3d.ofCenter(destinationBlock);
        var result = this.getWorld().raycast(new RaycastContext(Vector3d, blockVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        return result.getBlockPos().equals(destinationBlock);
    }

    private class AIWanderIdle extends Goal {
        protected final EntityToucan toucan;
        protected double x;
        protected double y;
        protected double z;
        private boolean flightTarget = false;

        public AIWanderIdle() {
            super();
            this.setControls(EnumSet.of(Control.MOVE));
            this.toucan = EntityToucan.this;
        }

        @Override
        public boolean canStart() {
            if (this.toucan.hasPassengers() || toucan.getSaplingState() != null || EntityToucan.this.aiItemFlag || (toucan.getTarget() != null && toucan.getTarget().isAlive()) || this.toucan.hasVehicle()) {
                return false;
            } else {
                if (this.toucan.getRandom().nextInt(45) != 0 && !toucan.isFlying()) {
                    return false;
                }
                if (this.toucan.isOnGround()) {
                    this.flightTarget = random.nextInt(6) == 0;
                } else {
                    this.flightTarget = random.nextInt(5) != 0 && toucan.timeFlying < 200;
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
                toucan.getMoveControl().moveTo(x, y, z, 1F);
            } else {
                this.toucan.getNavigation().startMovingTo(this.x, this.y, this.z, 1F);
            }
            if (!flightTarget && isFlying() && toucan.isOnGround()) {
                toucan.setFlying(false);
            }
            if (isFlying() && toucan.isOnGround() && toucan.timeFlying > 10) {
                toucan.setFlying(false);
            }
        }

        @Nullable
        protected Vec3d getPosition() {
            Vec3d vector3d = toucan.getPos();
            if (toucan.isOverWaterOrVoid()) {
                flightTarget = true;
            }
            if (flightTarget) {
                if (toucan.timeFlying > 50 && toucan.isOverLeaves() && !toucan.isOnGround()) {
                    return toucan.getBlockGrounding(vector3d);
                } else if (toucan.timeFlying < 200 || toucan.isOverWaterOrVoid()) {
                    return toucan.getBlockInViewAway(vector3d, 0);
                } else {
                    return toucan.getBlockGrounding(vector3d);
                }
            } else if (!toucan.isOnGround()) {
                return toucan.getBlockGrounding(vector3d);
            } else {
                if (this.toucan.isOverLeaves()) {
                    for (int i = 0; i < 15; i++) {
                        BlockPos pos = this.toucan.getBlockPos().add(random.nextInt(16) - 8, random.nextInt(8) - 4, random.nextInt(16) - 8);
                        if (!toucan.getWorld().getBlockState(pos.up()).isSolid() && toucan.getWorld().getBlockState(pos).isSolid() && toucan.getPathfindingFavor(pos) >= 0.0F) {
                            return Vec3d.ofBottomCenter(pos);
                        }
                    }
                }
                return FuzzyTargeting.find(this.toucan, 16, 7);
            }
        }

        public boolean shouldContinue() {
            if (toucan.aiItemFlag) {
                return false;
            }
            if (flightTarget) {
                return toucan.isFlying() && toucan.squaredDistanceTo(x, y, z) > 2F;
            } else {
                return (!this.toucan.getNavigation().isIdle()) && !this.toucan.hasPassengers();
            }
        }

        public void start() {
            if (flightTarget) {
                toucan.setFlying(true);
                toucan.getMoveControl().moveTo(x, y, z, 1F);
            } else {
                this.toucan.getNavigation().startMovingTo(this.x, this.y, this.z, 1F);
            }
        }

        public void stop() {
            this.toucan.getNavigation().stop();
            super.stop();
        }

    }

    private class AIPlantTrees extends Goal {
        protected final EntityToucan toucan;
        protected BlockPos pos;
        private int runCooldown = 0;
        private int encircleTime = 0;
        private int plantTime = 0;
        private boolean clockwise;

        public AIPlantTrees() {
            super();
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
            this.toucan = EntityToucan.this;
        }

        @Override
        public boolean canStart() {
            if (toucan.getSaplingState() != null) {
                if (runCooldown-- <= 0) {
                    BlockPos target = getSaplingPlantPos();
                    runCooldown = resetCooldown();
                    if (target != null) {
                        pos = target;
                        clockwise = random.nextBoolean();
                        encircleTime = (toucan.isGolden() ? 20 : 100) + random.nextInt(100);
                        return true;
                    }
                }
            }
            return false;
        }

        private int resetCooldown() {
            return toucan.isGolden() && !toucan.isEnchanted() ? 50 + random.nextInt(40) : 200 + random.nextInt(200);
        }

        public void tick() {
            toucan.aiItemFlag = true;
            double up = 3.0;
            if (encircleTime > 0) {
                encircleTime--;
            }
            if (isWithinXZDist(pos, toucan.getPos(), 5) && encircleTime <= 0) {
                up = 0.0D;
            }
            if (toucan.squaredDistanceTo(Vec3d.ofCenter(pos)) < 3.0D) {
                toucan.setFlying(false);
                toucan.peck();
                plantTime++;
                if (plantTime > 60) {
                    BlockState state = toucan.getSaplingState();
                    if (state != null) {
                        if (state.canPlaceAt(toucan.getWorld(), pos) && toucan.getWorld().getBlockState(pos).isReplaceable()) {
                            toucan.getWorld().setBlockState(pos, state);
                            if (!toucan.isEnchanted()) {
                                toucan.setSaplingState(null);
                            }
                        }
                    }
                    stop();
                }
            } else {
                BlockPos moveTo = pos;

                if (encircleTime > 0) {
                    moveTo = getVultureCirclePos(pos, 3, up);
                }
                if (moveTo != null) {
                    if (encircleTime <= 0 && !toucan.hasLineOfSightSapling(pos)) {
                        toucan.setFlying(false);
                        toucan.getNavigation().startMovingTo(moveTo.getX() + 0.5F, moveTo.getY() + up + 0.5F, moveTo.getZ() + 0.5F, 1F);
                    } else {
                        toucan.setFlying(true);
                        toucan.getMoveControl().moveTo(moveTo.getX() + 0.5F, moveTo.getY() + up + 0.5F, moveTo.getZ() + 0.5F, 1F);
                    }
                }
            }
        }

        public BlockPos getVultureCirclePos(BlockPos target, float circleDistance, double yLevel) {
            final float angle = (Maths.EIGHT_STARTING_ANGLE * (clockwise ? -encircleTime : encircleTime));
            final double extraX = circleDistance * MathHelper.sin((angle));
            final double extraZ = circleDistance * MathHelper.cos(angle);
            BlockPos pos;
            pos = new BlockPos((int) (target.getX() + 0.5F + extraX), (int) (target.getY() + 1 + yLevel), (int) (target.getZ() + 0.5F + extraZ));
            if (toucan.getWorld().isAir(pos)) {
                return pos;
            }
            return null;
        }

        public void stop() {
            toucan.aiItemFlag = false;
            pos = null;
            plantTime = 0;
            encircleTime = 0;
        }

        public boolean shouldContinue() {
            return pos != null && toucan.getSaplingState() != null;
        }

        private boolean isWithinXZDist(BlockPos blockpos, Vec3d positionVec, double distance) {
            return blockpos.getSquaredDistance(new BlockPos((int) positionVec.x, blockpos.getY(), (int) positionVec.z)) < distance * distance;
        }

        private BlockPos getSaplingPlantPos() {
            BlockState state = toucan.getSaplingState();
            if (state != null) {
                for (int i = 0; i < 15; i++) {
                    BlockPos pos = this.toucan.getBlockPos().add(random.nextInt(10) - 8, random.nextInt(8) - 4, random.nextInt(16) - 8);
                    if (state.canPlaceAt(toucan.getWorld(), pos) && toucan.getWorld().isAir(pos.up()) && toucan.hasLineOfSightSapling(pos)) {
                        return pos;
                    }
                }
            }
            return null;
        }
    }
}
