package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAISwimBottom;
import com.github.alexthe666.alexsmobs.entity.ai.AquaticMoveController;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MoveIntoWaterGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.SwimNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.InventoryChangedListener;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;

public class EntityCatfish extends WaterCreatureEntity implements Flutterer, Bucketable, InventoryChangedListener {

    private static final TrackedData<Boolean> FROM_BUCKET = DataTracker.registerData(EntityCatfish.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> CATFISH_SIZE = DataTracker.registerData(EntityCatfish.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SPIT_TIME = DataTracker.registerData(EntityCatfish.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> HAS_SWALLOWED_ENTITY = DataTracker.registerData(EntityCatfish.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<String> SWALLOWED_ENTITY_TYPE = DataTracker.registerData(EntityCatfish.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<NbtCompound> SWALLOWED_ENTITY_DATA = DataTracker.registerData(EntityCatfish.class, TrackedDataHandlerRegistry.NBT_COMPOUND);
    private static final EntityDimensions SMALL_SIZE = EntityDimensions.changing(0.9F, 0.6F);
    private static final EntityDimensions MEDIUM_SIZE = EntityDimensions.changing(1.25F, 0.9F);
    private static final EntityDimensions LARGE_SIZE = EntityDimensions.changing(1.9F, 0.9F);
    public static final Identifier MEDIUM_LOOT = new Identifier("alexsmobs", "entities/catfish_medium");
    public static final Identifier LARGE_LOOT = new Identifier("alexsmobs", "entities/catfish_large");
    public SimpleInventory catfishInventory;
    private int eatCooldown = 0;

    public EntityCatfish(EntityType<EntityCatfish> type, World level) {
        super(type, level);
        initCatfishInventory();
        this.moveControl = new AquaticMoveController(this, 1.0F, 15F);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    public int getLimitPerChunk() {
        return 2;
    }

    @Override
    public boolean spawnsTooManyForEachTry(int sze) {
        return sze > 2;
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(1, new MoveIntoWaterGoal(this));
        this.goalSelector.add(2, new EscapeDangerGoal(this, 1D));
        this.goalSelector.add(3, new TargetFoodGoal(this));
        this.goalSelector.add(4, new TemptGoal(this, 1.0D, Ingredient.fromTag(AMTagRegistry.CATFISH_ITEM_FASCINATIONS), false));
        this.goalSelector.add(5, new FascinateLanternGoal(this));
        this.goalSelector.add(6, new AnimalAISwimBottom(this, 1F, 7));
    }

    @Override
    public boolean canImmediatelyDespawn(double p_27492_) {
        return !this.isFromBucket() && !cannotDespawn() && !this.hasCustomName();
    }

    private void initCatfishInventory() {
        var animalchest = this.catfishInventory;
        int size = this.getCatfishSize() > 2 ? 1 : this.getCatfishSize() == 1 ? 9 : 3;
        this.catfishInventory = new SimpleInventory(size) {
            @Override
            public boolean canPlayerUse(PlayerEntity player) {
                return EntityCatfish.this.isAlive() && !EntityCatfish.this.inNetherPortal;
            }
        };
        catfishInventory.addListener(this);
        if (animalchest != null) {
            int i = Math.min(animalchest.size(), this.catfishInventory.size());
            for (int j = 0; j < i; ++j) {
                var itemstack = animalchest.getStack(j);
                if (!itemstack.isEmpty()) {
                    this.catfishInventory.setStack(j, itemstack.copy());
                }
            }
        }
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if (this.catfishInventory != null) {
            for (int i = 0; i < catfishInventory.size(); i++) {
                this.dropStack(catfishInventory.getStack(i));
            }
            catfishInventory.clear();
        }
        if(this.getCatfishSize() == 2){
            this.spit();
        }
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.hasCustomName() || this.isFromBucket() || this.hasSwallowedEntity() || this.catfishInventory != null && !this.catfishInventory.isEmpty();
    }

    public static boolean canCatfishSpawn(EntityType<EntityCatfish> entityType, ServerWorldAccess iServerWorld, SpawnReason reason, BlockPos pos, Random random) {
        return reason == SpawnReason.SPAWNER || iServerWorld.getBlockState(pos).getFluidState().isOf(Fluids.WATER) && random.nextInt(1) == 0;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.catfishSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new SwimNavigation(this, worldIn);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FROM_BUCKET, false);
        this.dataTracker.startTracking(CATFISH_SIZE, 0);
        this.dataTracker.startTracking(SPIT_TIME, 0);
        this.dataTracker.startTracking(SWALLOWED_ENTITY_TYPE, "minecraft:pig");
        this.dataTracker.startTracking(SWALLOWED_ENTITY_DATA, new NbtCompound());
        this.dataTracker.startTracking(HAS_SWALLOWED_ENTITY, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient) {
            if(this.getSpitTime() > 0){
                this.setSpitTime(this.getSpitTime() - 1);
            }
            if(eatCooldown > 0){
                eatCooldown--;
            }
        }
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        boolean inSeaPickle = false;
        final int width = (int)Math.ceil(this.getWidth() / 2F);
        final int height = (int)Math.ceil(this.getHeight() / 2F);
        final BlockPos.Mutable pos = this.getBlockPos().mutableCopy();
        BlockPos vomitTo = null;
        for(int i = -width; i <= width; i++){
            for(int j = -height; j <= height; j++){
                for(int k = -width; k <= width; k++){
                    pos.set(this.getX() + i, this.getY() + j, this.getZ() + k);
                    if(getWorld().getBlockState(pos).isOf(Blocks.SEA_PICKLE)){
                        inSeaPickle = true;
                        vomitTo = pos;
                        break;
                    }
                }
            }
        }
        if(inSeaPickle && this.canSpit()){
            if(this.getSpitTime() == 0){
                this.emitGameEvent(GameEvent.EAT);
                this.playSound(SoundEvents.ENTITY_PLAYER_BURP, this.getSoundVolume(), this.getSoundPitch());
            }
            if(vomitTo != null){
                final var face = Vec3d.ofCenter(vomitTo).subtract(this.getMouthVec());
                final double d0 = face.horizontalLength();
                this.setPitch((float)(-MathHelper.atan2(face.y, d0) * MathHelper.DEGREES_PER_RADIAN));
                this.setYaw(((float) MathHelper.atan2(face.z, face.x)) * MathHelper.DEGREES_PER_RADIAN - 90F);
                this.bodyYaw = this.getYaw();
                this.headYaw= this.getYaw();
            }
            this.spit();
        }
    }

    @Nullable
    @Override
    protected Identifier getLootTableId() {
        if (this.getCatfishSize() == 2) {
            return LARGE_LOOT;
        }
        return this.getCatfishSize() == 1 ? MEDIUM_LOOT : super.getLootTableId();
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> accessor) {
        if (CATFISH_SIZE.equals(accessor)) {
            this.calculateDimensions();
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(10F * this.getCatfishSize() + 10F);
            this.heal(50F);
        }
        super.onTrackedDataSet(accessor);
    }

    @Override
    public boolean isFromBucket() {
        return this.dataTracker.get(FROM_BUCKET);
    }

    @Override
    public void setFromBucket(boolean bucketed) {
        this.dataTracker.set(FROM_BUCKET, bucketed);
    }

    @Override
    public void copyDataToStack(@NotNull ItemStack bucket) {
        if (this.hasCustomName()) {
            bucket.setCustomName(this.getCustomName());
        }
        Bucketable.copyDataToStack(this, bucket);
        NbtCompound compound = bucket.getOrCreateNbt();
        writeCustomDataToNbt(compound);
    }

    @Override
    public void copyDataFromNbt(@NotNull NbtCompound compound) {
        Bucketable.copyDataFromNbt(this, compound);
        readCustomDataFromNbt(compound);
    }

    @Override
    public ItemStack getBucketItem() {
        final int catfishSize = this.getCatfishSize();
        final Item item = switch (catfishSize) {
            case 1 -> AMItemRegistry.MEDIUM_CATFISH_BUCKET.get();
            case 2 -> AMItemRegistry.LARGE_CATFISH_BUCKET.get();
            default -> AMItemRegistry.SMALL_CATFISH_BUCKET.get();
        };
        return new ItemStack(item);
    }

    @Override
    public SoundEvent getBucketFillSound() {
        return SoundEvents.ITEM_BUCKET_FILL_FISH;
    }

    public int getCatfishSize() {
        return MathHelper.clamp(this.dataTracker.get(CATFISH_SIZE), 0, 2);
    }

    public void setCatfishSize(int catfishSize) {
        this.dataTracker.set(CATFISH_SIZE, catfishSize);
    }

    public int getSpitTime() {
        return this.dataTracker.get(SPIT_TIME);
    }

    public void setSpitTime(int time) {
        this.dataTracker.set(SPIT_TIME, time);
    }

    public boolean isSpitting() {
        return getSpitTime() > 0;
    }

    public String getSwallowedEntityType() {
        return this.dataTracker.get(SWALLOWED_ENTITY_TYPE);
    }

    public void setSwallowedEntityType(String containedEntityType) {
        this.dataTracker.set(SWALLOWED_ENTITY_TYPE, containedEntityType);
    }

    public NbtCompound getSwallowedData() {
        return this.dataTracker.get(SWALLOWED_ENTITY_DATA);
    }

    public void setSwallowedData(NbtCompound containedData) {
        this.dataTracker.set(SWALLOWED_ENTITY_DATA, containedData);
    }

    public boolean hasSwallowedEntity() {
        return this.dataTracker.get(HAS_SWALLOWED_ENTITY);
    }

    public void setHasSwallowedEntity(boolean swallowedEntity) {
        this.dataTracker.set(HAS_SWALLOWED_ENTITY, swallowedEntity);
    }

    @Override
    public EntityDimensions getDimensions(EntityPose poseIn) {
        return getDimsForCatfish().scaled(this.getScaleFactor());
    }

    public boolean hurt(DamageSource source, float f) {
        if(super.damage(source, f)){
            this.spit();
            return true;
        }else{
            return false;
        }
    }

    @Override
    @NotNull
    protected ActionResult interactMob(@NotNull PlayerEntity player, @NotNull Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.getItem() == Items.SEA_PICKLE) {
            this.spit();
            return ActionResult.success(this.getWorld().isClient);
        }
        return Bucketable.tryBucket(player, hand, this).orElse(super.interactMob(player, hand));
    }

    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("FromBucket", this.isFromBucket());
        compound.putFloat("CatfishSize", this.getCatfishSize());
        if (catfishInventory != null) {
            final var nbttaglist = new NbtList();
            for (int i = 0; i < this.catfishInventory.size(); ++i) {
                final ItemStack itemstack = this.catfishInventory.getStack(i);
                if (!itemstack.isEmpty()) {
                    NbtCompound CompoundNBT = new NbtCompound();
                    CompoundNBT.putByte("Slot", (byte) i);
                    itemstack.writeNbt(CompoundNBT);
                    nbttaglist.add(CompoundNBT);
                }
            }
            compound.put("Items", nbttaglist);
        }
        compound.putString("ContainedEntityType", this.getSwallowedEntityType());
        compound.put("ContainedData", this.getSwallowedData());
        compound.putBoolean("HasSwallowedEntity", this.hasSwallowedEntity());
    }

    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setFromBucket(compound.getBoolean("FromBucket"));
        this.setCatfishSize(compound.getInt("CatfishSize"));
        if (catfishInventory != null) {
            final var nbttaglist = compound.getList("Items", 10);
            this.initCatfishInventory();
            for (int i = 0; i < nbttaglist.size(); ++i) {
                final NbtCompound CompoundNBT = nbttaglist.getCompound(i);
                final int j = CompoundNBT.getByte("Slot") & 255;
                this.catfishInventory.setStack(j, ItemStack.fromNbt(CompoundNBT));
            }
        }
        this.setSwallowedEntityType(compound.getString("ContainedEntityType"));
        if (!compound.getCompound("ContainedData").isEmpty()) {
            this.setSwallowedData(compound.getCompound("ContainedData"));
        }
        this.setHasSwallowedEntity(compound.getBoolean("HasSwallowedEntity"));
    }

    private EntityDimensions getDimsForCatfish() {
        return switch (this.getCatfishSize()) {
            case 1 -> MEDIUM_SIZE;
            case 2 -> LARGE_SIZE;
            default -> SMALL_SIZE;
        };
    }

    @Nullable
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.setCatfishSize(random.nextFloat() < 0.35F ? 1 : 0);
        if (random.nextFloat() < 0.1F) {
            final var holder = worldIn.getBiome(this.getBlockPos());
            if (holder.isIn(AMTagRegistry.SPAWNS_HUGE_CATFISH) || reason == SpawnReason.SPAWN_EGG) {
                this.setCatfishSize(2);
            }
        }
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.canMoveVoluntarily() && this.isTouchingWater()) {
            this.updateVelocity(this.getMovementSpeed(), travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.9D));
            if (this.getTarget() == null) {
                this.setVelocity(this.getVelocity().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    protected void playStepSound(BlockPos p_180429_1_, BlockState p_180429_2_) {
    }

    @Override
    public void onInventoryChanged(Inventory p_18983_) {

    }

    @Override
    protected void loot(ItemEntity itemEntity) {
        final var itemstack = itemEntity.getStack();
        if (this.getCatfishSize() != 2 && !isFull() && this.catfishInventory != null && this.catfishInventory.addStack(itemstack).isEmpty()) {
            this.triggerItemPickedUpByEntityCriteria(itemEntity);
            this.sendPickup(itemEntity, itemstack.getCount());
            itemEntity.discard();
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_GENERIC_EAT, this.getSoundVolume(), this.getSoundPitch());
        }
    }

    public boolean isFull() {
        if (this.getCatfishSize() == 2 || this.catfishInventory == null) {
            return this.hasSwallowedEntity();
        } else {
            for (int i = 0; i < this.catfishInventory.size(); i++) {
                if (this.catfishInventory.getStack(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public float getSoundPitch() {
        final float f = (3 - this.getCatfishSize()) * 0.33F;
        return (float) (super.getSoundPitch() * Math.sqrt(f) * 1.2F);
    }

    public boolean swallowEntity(Entity entity) {
        if (this.getCatfishSize() == 2 && entity instanceof final MobEntity mob) {
            this.setHasSwallowedEntity(true);
            final var EntityGroup = Registries.ENTITY_TYPE.getKey(mob.getType());
            if (EntityGroup.isPresent()) {
                this.setSwallowedEntityType(EntityGroup.toString());
            }
            final NbtCompound tag = new NbtCompound();
            mob.writeCustomDataToNbt(tag);
            this.setSwallowedData(tag);
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_GENERIC_EAT, this.getSoundVolume(), this.getSoundPitch());
            return true;
        }
        if (this.getCatfishSize() < 2 && entity instanceof final ItemEntity item) {
            this.loot(item);
        }
        return false;
    }

    public boolean canSpit() {
        return this.getCatfishSize() == 2 ? this.hasSwallowedEntity() : this.catfishInventory != null && !this.catfishInventory.isEmpty();
    }

    public void spit() {
        this.setSpitTime(10);
        this.eatCooldown = 60 + random.nextInt(60);
        if (this.getCatfishSize() == 2) {
            if (this.hasSwallowedEntity()) {
                var type = Registries.ENTITY_TYPE.get(new Identifier(this.getSwallowedEntityType()));
                var entity = type.create(getWorld());
                if (entity instanceof final LivingEntity alive) {
                    alive.readCustomDataFromNbt(this.getSwallowedData());
                    alive.setHealth(Math.max(2, alive.getMaxHealth() * 0.25F));
                    alive.setYaw(random.nextFloat() * 360 - 180);
                    alive.refreshPositionAfterTeleport(this.getMouthVec());
                    if (getWorld().spawnEntity(alive)) {
                        this.setHasSwallowedEntity(false);
                        this.setSwallowedEntityType("minecraft:pig");
                        this.setSwallowedData(new NbtCompound());
                    }
                }
            }
        } else {
            ItemStack itemStack = ItemStack.EMPTY;
            int index = -1;
            if(this.catfishInventory != null){
                for (int i = 0; i < this.catfishInventory.size(); i++) {
                    if (!this.catfishInventory.getStack(i).isEmpty()) {
                        itemStack = this.catfishInventory.getStack(i);
                        index = i;
                        break;
                    }
                }
            }
            if (!itemStack.isEmpty()) {
                final Vec3d vec3 = this.getMouthVec();
                final Vec3d vec32 = vec3.subtract(getPos()).normalize().multiply(0.14F);
                final ItemEntity item = new ItemEntity(getWorld(), vec3.x, vec3.y, vec3.z, itemStack, vec32.x, vec32.y, vec32.z);
                item.setVelocity(Vec3d.ZERO);
                item.setPickupDelay(30);
                if (getWorld().spawnEntity(item) && this.catfishInventory != null) {
                    this.catfishInventory.setStack(index, ItemStack.EMPTY);
                }
            }
        }
    }

    private Vec3d getMouthVec(){
        final var vec3 = new Vec3d(0, this.getHeight() * 0.25F, this.getWidth() * 0.8F).rotateX(this.getPitch() * MathHelper.RADIANS_PER_DEGREE).rotateY(-this.getYaw() * MathHelper.RADIANS_PER_DEGREE);
        return this.getPos().add(vec3);
    }

    private boolean isFood(Entity entity) {
        if (this.getCatfishSize() == 2) {
            return !entity.getType().isIn(AMTagRegistry.CATFISH_IGNORE_EATING) && entity instanceof MobEntity && !(entity instanceof EntityCatfish) && entity.getHeight() <= 1.0F;
        } else {
            return entity instanceof ItemEntity && ((ItemEntity) entity).getItemAge() > 35;
        }
    }

    private boolean canSeeBlock(BlockPos destinationBlock) {
        final var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        final var blockVec = Vec3d.ofCenter(destinationBlock);
        final var result = this.getWorld().raycast(new RaycastContext(Vector3d, blockVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        return result.getBlockPos().equals(destinationBlock);
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
    public boolean isInAir() {
        return false;
    }

    private class TargetFoodGoal extends Goal {
        private final EntityCatfish catfish;
        private Entity food;
        private int executionCooldown = 50;

        public TargetFoodGoal(EntityCatfish catfish) {
            this.setControls(EnumSet.of(Control.MOVE));
            this.catfish = catfish;
        }

        @Override
        public boolean canStart() {
            if (!catfish.isInsideWaterOrBubbleColumn() || catfish.eatCooldown > 0) {
                return false;
            }
            if (executionCooldown > 0) {
                executionCooldown--;
            } else {
                executionCooldown = 50 + random.nextInt(50);
                if (!this.catfish.isFull()) {
                    final var list = catfish.getWorld().getEntitiesByClass(Entity.class, catfish.getBoundingBox().expand(8, 8, 8), EntityPredicates.EXCEPT_SPECTATOR.and(entity -> entity != catfish && catfish.isFood(entity)));
                    list.sort(Comparator.comparingDouble(catfish::squaredDistanceTo));
                    if (!list.isEmpty()) {
                        food = list.get(0);
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return food != null && food.isAlive() && !this.catfish.isFull();
        }

        @Override
        public void stop() {
            executionCooldown = 5;
        }

        @Override
        public void tick() {
            catfish.getNavigation().startMovingTo(food.getX(), food.getBodyY(0.5F), food.getZ(), 1.0F);
            final float eatDist = catfish.getWidth() * 0.65F + food.getWidth();
            if (catfish.distanceTo(food) < eatDist + 3 && catfish.canSee(food)) {
                final var delta = catfish.getMouthVec().subtract(food.getPos()).normalize().multiply(0.1F);
                food.setVelocity(food.getVelocity().add(delta));
                if (catfish.distanceTo(food) < eatDist) {
                    if (food instanceof PlayerEntity) {
                        food.damage(catfish.getDamageSources().mobAttack(catfish), 12000);
                    } else if (catfish.swallowEntity(food)) {
                        catfish.emitGameEvent(GameEvent.EAT);
                        catfish.playSound(SoundEvents.ENTITY_GENERIC_EAT, catfish.getSoundVolume(), catfish.getSoundPitch());
                        food.discard();
                    }
                }
            }
        }
    }

    private class FascinateLanternGoal extends Goal {
        private final int searchLength;
        private final int verticalSearchRange;
        protected BlockPos destinationBlock;
        private final EntityCatfish fish;
        private int runDelay = 70;
        private int chillTime = 0;
        private int maxChillTime = 200;

        private FascinateLanternGoal(EntityCatfish fish) {
            this.setControls(EnumSet.of(Control.MOVE));
            this.fish = fish;
            searchLength = 16;
            verticalSearchRange = 6;
        }

        @Override
        public boolean shouldContinue() {
            return destinationBlock != null && isSeaLantern(fish.getWorld(), destinationBlock.mutableCopy()) && isCloseToLantern(16) && !fish.isFull();
        }

        public boolean isCloseToLantern(double dist) {
            return destinationBlock == null || fish.squaredDistanceTo(Vec3d.ofCenter(destinationBlock)) < dist * dist;
        }

        @Override
        public boolean canStart() {
            if (!fish.isInsideWaterOrBubbleColumn()) {
                return false;
            }
            if (this.runDelay > 0) {
                --this.runDelay;
                return false;
            } else {
                this.runDelay = 70 + fish.random.nextInt(70);
                return !this.fish.isFull() && this.searchForDestination();
            }
        }

        @Override
        public void start(){
            chillTime = 0;
            maxChillTime = 10 + random.nextInt(20);
        }

        @Override
        public void tick() {
            final Vec3d vec = Vec3d.ofCenter(destinationBlock);
            fish.getNavigation().startMovingTo(vec.x, vec.y, vec.z, 1F);
            if (fish.squaredDistanceTo(vec) < 1F + fish.getWidth() * 0.6F) {
                var face = vec.subtract(fish.getPos());
                fish.setVelocity(fish.getVelocity().add(face.normalize().multiply(0.1F)));
                if (chillTime++ > maxChillTime) {
                    destinationBlock = null;
                }
            }
        }

        @Override
        public void stop() {
            destinationBlock = null;
        }

        protected boolean searchForDestination() {
            int lvt_1_1_ = this.searchLength;
            //int lvt_2_1_ = this.verticalSearchRange;
            BlockPos lvt_3_1_ = fish.getBlockPos();
            var lvt_4_1_ = new BlockPos.Mutable();

            for (int lvt_5_1_ = -8; lvt_5_1_ <= 2; lvt_5_1_++) {
                for (int lvt_6_1_ = 0; lvt_6_1_ < lvt_1_1_; ++lvt_6_1_) {
                    for (int lvt_7_1_ = 0; lvt_7_1_ <= lvt_6_1_; lvt_7_1_ = lvt_7_1_ > 0 ? -lvt_7_1_ : 1 - lvt_7_1_) {
                        for (int lvt_8_1_ = lvt_7_1_ < lvt_6_1_ && lvt_7_1_ > -lvt_6_1_ ? lvt_6_1_ : 0; lvt_8_1_ <= lvt_6_1_; lvt_8_1_ = lvt_8_1_ > 0 ? -lvt_8_1_ : 1 - lvt_8_1_) {
                            lvt_4_1_.set(lvt_3_1_, lvt_7_1_, lvt_5_1_ - 1, lvt_8_1_);
                            if (this.isSeaLantern(fish.getWorld(), lvt_4_1_) && fish.canSeeBlock(lvt_4_1_)) {
                                this.destinationBlock = lvt_4_1_;
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }

        private boolean isSeaLantern(World world, BlockPos.Mutable pos) {
            return world.getBlockState(pos).isIn(AMTagRegistry.CATFISH_BLOCK_FASCINATIONS);
        }
    }
}
