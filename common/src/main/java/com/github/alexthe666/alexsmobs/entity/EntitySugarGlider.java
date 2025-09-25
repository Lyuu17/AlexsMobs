package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.IngredientUtil;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.google.common.collect.Maps;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.ai.pathing.SpiderNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class EntitySugarGlider extends TameableEntity implements IFollower {

    public static final Identifier SUGAR_GLIDER_REWARD = new Identifier("alexsmobs", "gameplay/sugar_glider_reward");
    public static final Map<Block, Item> LEAF_TO_SAPLING = Util.make(Maps.newHashMap(), (map) -> {
        map.put(Blocks.OAK_LEAVES, Items.OAK_SAPLING);
        map.put(Blocks.BIRCH_LEAVES, Items.BIRCH_SAPLING);
        map.put(Blocks.SPRUCE_LEAVES, Items.SPRUCE_SAPLING);
        map.put(Blocks.JUNGLE_LEAVES, Items.JUNGLE_SAPLING);
        map.put(Blocks.ACACIA_LEAVES, Items.ACACIA_SAPLING);
        map.put(Blocks.DARK_OAK_LEAVES, Items.DARK_OAK_SAPLING);
        map.put(Blocks.MANGROVE_LEAVES, Items.MANGROVE_PROPAGULE);
    });

    private static final TrackedData<Direction> ATTACHED_FACE = DataTracker.registerData(EntitySugarGlider.class, TrackedDataHandlerRegistry.FACING);
    private static final TrackedData<Byte> CLIMBING = DataTracker.registerData(EntitySugarGlider.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Boolean> GLIDING = DataTracker.registerData(EntitySugarGlider.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> FORAGING_TIME = DataTracker.registerData(EntitySugarGlider.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> COMMAND = DataTracker.registerData(EntitySugarGlider.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntitySugarGlider.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final Direction[] POSSIBLE_DIRECTIONS = new Direction[]{Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private final int attachChangeCooldown = 0;
    public float glideProgress;
    public float prevGlideProgress;
    public float forageProgress;
    public float prevForageProgress;
    public float sitProgress;
    public float prevSitProgress;
    public float attachChangeProgress = 0F;
    public float prevAttachChangeProgress = 0F;
    public Direction prevAttachDir = Direction.DOWN;
    private boolean isGlidingNavigator;
    private boolean stopClimbing = false;
    private int forageCooldown = 0;
    private int detachCooldown = 0;
    private int rideCooldown = 0;

    public EntitySugarGlider(EntityType<EntitySugarGlider> type, World level) {
        super(type, level);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
        switchNavigator(true);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 8.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new SitGoal(this));
        this.goalSelector.add(2, new FlyingAIFollowOwner(this, 1.0D, 5.0F, 2.0F, true));
        this.goalSelector.add(3, new TemptGoal(this, 1.1D, IngredientUtil.ingredientFromTags(AMTagRegistry.SUGAR_GLIDER_BREEDABLES, AMTagRegistry.SUGAR_GLIDER_TAMEABLES), false){
            @Override
            public void start(){
                super.start();
                EntitySugarGlider.this.dataTracker.set(ATTACHED_FACE, Direction.DOWN);
            }
        });
        this.goalSelector.add(4, new AnimalMateGoal(this, 0.8D){
            @Override
            public void start(){
                super.start();
                EntitySugarGlider.this.dataTracker.set(ATTACHED_FACE, Direction.DOWN);
            }
        });
        this.goalSelector.add(5, new GlideGoal());
        this.goalSelector.add(6, new EscapeDangerGoal(this, 1D));
        this.goalSelector.add(7, new AnimalAIWanderRanged(this, 100, 1.0D, 10, 7));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(9, new LookAroundGoal(this));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CLIMBING, (byte) 0);
        this.dataTracker.startTracking(ATTACHED_FACE, Direction.DOWN);
        this.dataTracker.startTracking(GLIDING, false);
        this.dataTracker.startTracking(FORAGING_TIME, 0);
        this.dataTracker.startTracking(COMMAND, 0);
        this.dataTracker.startTracking(SITTING, false);
    }

    private void switchNavigator(boolean onGround) {
        if (onGround) {
            this.moveControl = new MoveControl(this);
            this.navigation = new SmartClimbPathNavigator(this, getWorld());
            this.isGlidingNavigator = false;
        } else {
            this.moveControl = new FlightMoveController(this, 0.6F, false);
            this.navigation = new DirectPathNavigator(this, getWorld());
            this.isGlidingNavigator = true;
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.SUGAR_GLIDER_BREEDABLES);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.SUGAR_GLIDER_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.SUGAR_GLIDER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.SUGAR_GLIDER_HURT.get();
    }

    public static boolean canSugarGliderSpawn(EntityType<?> type, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        var blockstate = worldIn.getBlockState(pos.down());
        return blockstate.isIn(BlockTags.LEAVES) && isLightLevelValidForNaturalSpawn(worldIn, pos);
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
        return AMEntityRegistry.rollSpawn(AMConfig.sugarGliderSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    public void tick() {
        super.tick();
        this.setStepHeight(1F);
        prevGlideProgress = glideProgress;
        prevAttachChangeProgress = attachChangeProgress;
        prevForageProgress = forageProgress;
        prevSitProgress = sitProgress;
        if (attachChangeProgress > 0F) {
            attachChangeProgress -= 0.25F;
        }
        //boolean glideVisual = this.isGliding() || this.getVehicle() != null && this.getVehicle().fallDistance != 0;
        if (glideProgress < 5F && isGliding()) {
            glideProgress += 2.5F;
        }
        if (glideProgress > 0F && !isGliding()) {
            glideProgress -= 2.5F;
        }
        if (forageProgress < 5F && getForagingTime() > 0) {
            forageProgress++;
        }
        if (forageProgress > 0F && getForagingTime() <= 0) {
            forageProgress--;
        }
        boolean sitVisual = this.isSitting() && !this.isTouchingWater() && this.isOnGround();
        if (sitProgress < 5F && sitVisual) {
            sitProgress++;
        }
        if (sitProgress > 0F && !sitVisual) {
            sitProgress--;
        }
        if (isGliding()) {
            if (shouldStopGliding()) {
                this.setGliding(false);
            } else {
                this.setVelocity(this.getVelocity().multiply(0.99D, 0.5D, 0.99D));
            }
        }
        Vec3d vector3d = this.getVelocity();
        if (!this.getWorld().isClient) {
            this.setBesideClimbableBlock(this.horizontalCollision);
            if (this.isOnGround() || this.isSitting() || this.isInsideWaterOrBubbleColumn() || this.isInLava() || this.isGliding() || this.hasVehicle()) {
                this.dataTracker.set(ATTACHED_FACE, Direction.DOWN);
            } else {
                var closestDirection = Direction.DOWN;
                double closestDistance = 100;
                for (var dir : POSSIBLE_DIRECTIONS) {
                    var antPos = new BlockPos(MathHelper.floor(this.getX()), MathHelper.floor(this.getY()), MathHelper.floor(this.getZ()));
                    var offsetPos = antPos.offset(dir);
                    var offset = Vec3d.ofCenter(offsetPos);
                    if (closestDistance > this.getPos().distanceTo(offset) && getWorld().isDirectionSolid(offsetPos, this, dir.getOpposite())) {
                        closestDistance = this.getPos().distanceTo(offset);
                        closestDirection = dir;
                    }
                }
                this.dataTracker.set(ATTACHED_FACE, closestDistance > this.getWidth() * 0.5F + 0.7F ? Direction.DOWN : closestDirection);
            }
        }
        boolean flag = false;
        if (this.getAttachmentFacing() != Direction.DOWN) {
            if (!this.horizontalCollision && this.getAttachmentFacing() != Direction.UP) {
                Vec3d vec = Vec3d.of(this.getAttachmentFacing().getVector());
                this.setVelocity(vector3d.add(vec.normalize().multiply(0.1F, 0.1F, 0.1F)));
            }
            if (!this.isOnGround() && vector3d.y < 0.0D) {
                this.setVelocity(vector3d.multiply(1.0D, 0.5D, 1.0D));
                flag = true;
            }
        }
        if (this.getAttachmentFacing() != Direction.DOWN && !this.isGliding()) {
            this.setNoGravity(true);
            this.setVelocity(vector3d.multiply(0.6D, 0.4D, 0.6D));
        } else {
            this.setNoGravity(false);
        }
        if (prevAttachDir != this.getAttachmentFacing()) {
            attachChangeProgress = 1F;
        }
        this.prevAttachDir = this.getAttachmentFacing();
        if (!this.getWorld().isClient) {
            if ((this.getAttachmentFacing() == Direction.UP || this.isGliding()) && !this.isGlidingNavigator) {
                switchNavigator(false);
            }
            if (this.getAttachmentFacing() != Direction.UP && this.isGlidingNavigator) {
                switchNavigator(true);
            }
        }
        BlockPos on = this.getBlockPos().offset(this.getAttachmentFacing());
        if (shouldForage() && getWorld().getBlockState(on).isIn(BlockTags.LEAVES)) {
            BlockState state = getWorld().getBlockState(on);
            if (this.getForagingTime() < 100) {
                if (random.nextInt(2) == 0) {
                    for (int i = 0; i < 4 + random.nextInt(2); i++) {
                        double motX = this.random.nextGaussian() * 0.02D;
                        double motY = this.random.nextGaussian() * 0.02D;
                        double motZ = this.random.nextGaussian() * 0.02D;
                        getWorld().addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), on.getX() + random.nextFloat(), on.getY() + random.nextFloat(), on.getZ() + random.nextFloat(), motX, motY, motZ);
                    }
                }
                this.setForagingTime(this.getForagingTime() + 1);
            } else {
                if (!this.getWorld().isClient) {
                    List<ItemStack> lootList = getForageLoot(state);
                    if (!lootList.isEmpty()) {
                        for (ItemStack stack : lootList) {
                            var e = this.dropStack(stack.copy());
                            if(e != null){
                                e.velocityDirty = true;
                                e.setVelocity(e.getVelocity().multiply(0.2, 0.2, 0.2));
                            }
                        }
                    }
                }
                this.forageCooldown = 8000 + 8000 * random.nextInt(2);
                this.setForagingTime(0);
            }
        } else {
            this.setForagingTime(0);
        }
        if (detachCooldown > 0) {
            detachCooldown--;
        }
        if (rideCooldown > 0) {
            rideCooldown--;
        }
    }

    @Override
    public void tickRiding() {
        var entity = this.getVehicle();
        if (this.hasVehicle() && !entity.isAlive()) {
            this.stopRiding();
        } else if (isTamed() && entity instanceof LivingEntity && isOwner((LivingEntity) entity)) {
            this.setVelocity(0, 0, 0);
            this.tick();
            if (this.hasVehicle()) {
                var mount = this.getVehicle();
                if (mount instanceof PlayerEntity) {
                    ((LivingEntity) mount).addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 100, 0, true, false));
                    this.bodyYaw = ((LivingEntity) mount).bodyYaw;
                    this.setYaw(mount.getYaw());
                    this.headYaw= ((LivingEntity) mount).headYaw;
                    this.prevYaw = ((LivingEntity) mount).headYaw;
                    final float radius = 0F;
                    final float angle = (Maths.STARTING_ANGLE * (((LivingEntity) mount).bodyYaw - 180F));
                    double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
                    double extraZ = radius * MathHelper.cos(angle);
                    this.setPos(mount.getX() + extraX, Math.max(mount.getY() + mount.getHeight() + 0.1, mount.getY()), mount.getZ() + extraZ);
                    if (!mount.isAlive() || rideCooldown == 0 && mount.isSneaking()) {
                        this.dismountVehicle();
                    }
                }

            }
        } else {
            super.tickRiding();
        }

    }

    private List<ItemStack> getForageLoot(BlockState leafState) {
        // TODO find a better place
        final Map<Block, List<Item>> LEAF_TO_RARES = Util.make(Maps.newHashMap(), (map) -> {
            map.put(Blocks.OAK_LEAVES, List.of(Items.APPLE));
            map.put(Blocks.JUNGLE_LEAVES, List.of(AMItemRegistry.BANANA.get(), AMItemRegistry.LEAFCUTTER_ANT_PUPA.get(), Items.COCOA_BEANS));
            map.put(Blocks.ACACIA_LEAVES, List.of(AMItemRegistry.ACACIA_BLOSSOM.get()));
        });

        Item sapling = LEAF_TO_SAPLING.get(leafState.getBlock());
        List<Item> rares = LEAF_TO_RARES.get(leafState.getBlock());
        final float rng = this.getRandom().nextFloat();
        if (rng < 0.1F && rares != null) {
            Item item = rares.size() <= 1 ? rares.get(0) : rares.get(this.getRandom().nextInt(rares.size()));
            return List.of(new ItemStack(item));
        }
        if (rng < 0.25F && sapling != null) {
            return List.of(new ItemStack(sapling));
        }
        var loottable = this.getWorld().getServer().getLootManager().getLootTable(SUGAR_GLIDER_REWARD);
        return loottable.generateLoot((new LootContextParameterSet.Builder((ServerWorld) this.getWorld())).add(LootContextParameters.THIS_ENTITY, this).add(LootContextParameters.BLOCK_STATE, leafState).build(LootContextTypes.BARTER));
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.isSitting()) {
            if (this.getNavigation().getCurrentPath() != null) {
                this.getNavigation().stop();
            }
            travelVector = Vec3d.ZERO;
        }
        if(this.isTouchingWater() && this.getVelocity().y > 0F){
            this.setVelocity(this.getVelocity().multiply(1.0D, 0.5D, 1.0D));
        }
        super.travel(travelVector);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.dataTracker.set(ATTACHED_FACE, Direction.byId(compound.getByte("AttachFace")));
        this.setCommand(compound.getInt("SugarGliderCommand"));
        this.setSitting(compound.getBoolean("SugarGliderSitting"));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putByte("AttachFace", (byte) this.dataTracker.get(ATTACHED_FACE).getId());
        compound.putInt("SugarGliderCommand", this.getCommand());
        compound.putBoolean("SugarGliderSitting", this.isSitting());
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
    protected void onBlockCollision(BlockState state) {
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.IN_WALL) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean isClimbing() {
        return this.isBesideClimbableBlock() && !this.isGliding() && !stopClimbing && !this.isSitting();
    }

    public boolean isBesideClimbableBlock() {
        return (this.dataTracker.get(CLIMBING) & 1) != 0;
    }

    public void setBesideClimbableBlock(boolean climbing) {
        byte b0 = this.dataTracker.get(CLIMBING);
        if (climbing) {
            b0 = (byte) (b0 | 1);
        } else {
            b0 = (byte) (b0 & -2);
        }

        this.dataTracker.set(CLIMBING, b0);
    }

    public Direction getAttachmentFacing() {
        return this.dataTracker.get(ATTACHED_FACE);
    }

    public boolean isGliding() {
        return this.dataTracker.get(GLIDING);
    }

    public void setGliding(boolean gliding) {
        this.dataTracker.set(GLIDING, gliding);
    }

    public int getForagingTime() {
        return this.dataTracker.get(FORAGING_TIME);
    }

    public void setForagingTime(int feedingTime) {
        this.dataTracker.set(FORAGING_TIME, feedingTime);
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

    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        Item item = itemstack.getItem();
        ActionResult type = super.interactMob(player, hand);
        if (!isTamed() && itemstack.isIn(AMTagRegistry.SUGAR_GLIDER_TAMEABLES)) {
            this.eat(player, hand, itemstack);
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_FOX_EAT, this.getSoundVolume(), this.getSoundPitch());
            if (getRandom().nextInt(2) == 0) {
                this.setOwner(player);
                this.getWorld().sendEntityStatus(this, (byte) 7);
            } else {
                this.getWorld().sendEntityStatus(this, (byte) 6);
            }
            return ActionResult.SUCCESS;
        }
        if (isTamed() && itemstack.isIn(AMTagRegistry.INSECT_ITEMS)) {
            if (this.getHealth() < this.getMaxHealth()) {
                this.eat(player, hand, itemstack);
                this.emitGameEvent(GameEvent.EAT);
                this.playSound(SoundEvents.ENTITY_FOX_EAT, this.getSoundVolume(), this.getSoundPitch());
                this.heal(5);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;

        }
        var actionResult = itemstack.useOnEntity(player, this, hand);
        if (actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS && isTamed() && isOwner(player) && !isBreedingItem(itemstack)) {
            if (player.isSneaking() && player.getPassengerList().isEmpty()) {
                this.startRiding(player);
                rideCooldown = 20;
                return ActionResult.SUCCESS;
            } else {
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
    public void updateLimbs(boolean b) {
        float f1 = (float) MathHelper.magnitude(this.getX() - this.prevX, (this.getY() - this.prevY) * 2, this.getZ() - this.prevZ);
        float f2 = Math.min(f1 * 6.0F, 1.0F);
        this.limbAnimator.updateLimbs(f2, 0.4F);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new SpiderNavigation(this, worldIn) {
            @Override
            protected boolean isAtValidPosition() {
                return super.isAtValidPosition() || ((EntitySugarGlider) entity).isBesideClimbableBlock() || entity.jumping;
            }
        };
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld serverLevel, PassiveEntity ageableMob) {
        return AMEntityRegistry.SUGAR_GLIDER.get().create(serverLevel);
    }

    private boolean shouldStopGliding() {
        return this.isOnGround() || this.getAttachmentFacing() != Direction.DOWN;
    }

    private boolean shouldForage() {
        return this.isTamed() && !this.isBaby() && forageCooldown == 0;
    }

    @Override
    public boolean shouldFollow() {
        return this.getCommand() == 1;
    }

    @Override
    public void followEntity(TameableEntity tameable, LivingEntity owner, double followSpeed) {
        if (this.distanceTo(owner) < 5 || this.isBaby()) {
            this.setGliding(!this.isOnGround());
            this.getNavigation().startMovingTo(owner, followSpeed);

        } else {
            var fly = new Vec3d(0, 0, 0);
            float f = 0.5F;
            if (this.isOnGround()) {
                fly = fly.add(0, 0.4, 0);
                f = 0.9F;
            }
            fly = fly.add(owner.getEyePos().subtract(EntitySugarGlider.this.getPos()).normalize().multiply(f));
            this.setVelocity(fly);
            var move = this.getVelocity();
            double d0 = move.horizontalLength();
            this.setPitch((float) (-MathHelper.atan2(move.y, d0) * (double) MathHelper.DEGREES_PER_RADIAN));
            this.setYaw(((float) MathHelper.atan2(move.z, move.x)) * MathHelper.DEGREES_PER_RADIAN - 90F);
            this.setGliding(true);
        }
    }

    private boolean canSeeBlock(BlockPos destinationBlock) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        var blockVec = Vec3d.ofCenter(destinationBlock);
        var result = this.getWorld().raycast(new RaycastContext(Vector3d, blockVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        return result.getBlockPos().equals(destinationBlock);
    }

    @Override
    public EntityView method_48926() {
        return getWorld();
    }

    private class GlideGoal extends Goal {

        private boolean climbing;
        private int climbTime = 0;
        private int leapSearchCooldown = 0;
        private int climbTimeout = 0;
        private BlockPos climb;
        private BlockPos glide;
        private boolean itsOver = false;
        private int airtime = 0;
        private Direction climbOffset = Direction.UP;

        private GlideGoal() {
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (EntitySugarGlider.this.getForagingTime() <= 0 && !EntitySugarGlider.this.isBaby() && !EntitySugarGlider.this.isSitting() && EntitySugarGlider.this.getRandom().nextInt(45) == 0) {
                if (EntitySugarGlider.this.getAttachmentFacing() != Direction.DOWN) {
                    climb = EntitySugarGlider.this.getBlockPos().offset(EntitySugarGlider.this.getAttachmentFacing());
                } else {
                    climb = findClimbPos();
                }
                return climb != null;
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return climb != null && !itsOver && climbTimeout < 30 && (!climbing || !getWorld().isAir(climb) && !EntitySugarGlider.this.getNavigation().isNearPathStartPos()) && EntitySugarGlider.this.getForagingTime() <= 0 && !EntitySugarGlider.this.isSitting();
        }

        @Override
        public void start() {
            climbTimeout = 0;
            leapSearchCooldown = 0;
            airtime = 0;
            climbing = true;
            climbTime = 0;
            EntitySugarGlider.this.getNavigation().stop();
        }

        @Override
        public void stop() {
            climbTimeout = 0;
            climb = null;
            glide = null;
            itsOver = false;
            EntitySugarGlider.this.stopClimbing = false;
            EntitySugarGlider.this.setGliding(false);
            EntitySugarGlider.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (leapSearchCooldown > 0) {
                leapSearchCooldown--;
            }
            if (climbing) {
                float inDir = EntitySugarGlider.this.getAttachmentFacing() == Direction.DOWN && EntitySugarGlider.this.getY() > climb.getY() + 0.3F ? 0.5F + EntitySugarGlider.this.getWidth() * 0.5F : 0.5F;
                var offset = Vec3d.ofCenter(climb).subtract(0, 0, 0).add(climbOffset.getOffsetX() * inDir, climbOffset.getOffsetY() * inDir, climbOffset.getOffsetZ() * inDir);
                final double d0 = climb.getX() + 0.5F - EntitySugarGlider.this.getX();
                final double d2 = climb.getZ() + 0.5F - EntitySugarGlider.this.getZ();
                final double xzDistSqr = d0 * d0 + d2 * d2;
                if (EntitySugarGlider.this.getY() > offset.y - 0.3F - EntitySugarGlider.this.getHeight()) {
                    EntitySugarGlider.this.stopClimbing = true;
                }
                if (xzDistSqr < 3 && EntitySugarGlider.this.getAttachmentFacing() != Direction.DOWN) {
                    var silly = new Vec3d(d0, 0, d2).normalize().multiply(0.1);
                    EntitySugarGlider.this.setVelocity(EntitySugarGlider.this.getVelocity().add(silly));
                } else {
                    EntitySugarGlider.this.getNavigation().startMovingTo(offset.x, offset.y, offset.z, 1F);
                }
                if (EntitySugarGlider.this.getAttachmentFacing() == Direction.DOWN) {
                    climbTimeout++;
                    climbTime = 0;
                } else {
                    climbTimeout = 0;
                    climbTime++;
                    if (climbTime > 40 && leapSearchCooldown == 0) {
                        BlockPos leapTo = findLeapPos(EntitySugarGlider.this.shouldForage() && random.nextInt(5) != 0);
                        leapSearchCooldown = 5 + EntitySugarGlider.this.getRandom().nextInt(10);
                        if (leapTo != null) {
                            EntitySugarGlider.this.stopClimbing = false;
                            EntitySugarGlider.this.setGliding(true);
                            EntitySugarGlider.this.getNavigation().stop();
                            EntitySugarGlider.this.dataTracker.set(ATTACHED_FACE, Direction.DOWN);
                            glide = leapTo;
                            climbing = false;
                        }
                    }
                }
            } else if (glide != null) {
                EntitySugarGlider.this.stopClimbing = false;
                EntitySugarGlider.this.setGliding(true);
                if (airtime > 5 && (EntitySugarGlider.this.horizontalCollision || EntitySugarGlider.this.isOnGround()
                        || Math.sqrt(EntitySugarGlider.this.squaredDistanceTo(Vec3d.ofCenter(glide))) < 1.1F)) {
                    EntitySugarGlider.this.setGliding(false);
                    EntitySugarGlider.this.detachCooldown = 20 + random.nextInt(80);
                    itsOver = true;
                }
                var fly = Vec3d.ofCenter(glide).subtract(EntitySugarGlider.this.getPos()).normalize().multiply(0.3F);
                EntitySugarGlider.this.setVelocity(fly);

                var move = EntitySugarGlider.this.getVelocity();
                double d0 = move.horizontalLength();
                EntitySugarGlider.this.setPitch((float) (-MathHelper.atan2(move.y, d0) * (double) MathHelper.DEGREES_PER_RADIAN));
                EntitySugarGlider.this.setYaw(((float) MathHelper.atan2(move.z, move.x)) * MathHelper.DEGREES_PER_RADIAN - 90F);
                airtime++;
            }
        }

        private BlockPos findClimbPos() {
            BlockPos mobPos = EntitySugarGlider.this.getBlockPos();
            for (int i = 0; i < 15; i++) {
                BlockPos offset = mobPos.add(EntitySugarGlider.this.random.nextInt(16) - 8, random.nextInt(4) + 1, EntitySugarGlider.this.random.nextInt(16) - 8);
                final double d0 = offset.getX() + 0.5F - EntitySugarGlider.this.getX();
                final double d2 = offset.getZ() + 0.5F - EntitySugarGlider.this.getZ();
                final double xzDistSqr = d0 * d0 + d2 * d2;
                var blockVec = Vec3d.ofCenter(offset);
                var result = getWorld().raycast(new RaycastContext(EntitySugarGlider.this.getEyePos(), blockVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, EntitySugarGlider.this));
                if (result.getType() != HitResult.Type.MISS && xzDistSqr > 4 && result.getSide().getAxis() != Direction.Axis.Y && getDistanceOffGround(result.getBlockPos().offset(result.getSide())) > 3 && isPositionEasilyClimbable(result.getBlockPos())) {
                    climbOffset = result.getSide();
                    return result.getBlockPos();
                }
            }
            return null;
        }

        private BlockPos findLeapPos(boolean leavesOnly) {
            BlockPos mobPos = EntitySugarGlider.this.getBlockPos().offset(climbOffset.getOpposite());
            for (int i = 0; i < 15; i++) {
                BlockPos offset = mobPos.add(EntitySugarGlider.this.random.nextInt(32) - 16, -1 - random.nextInt(4), EntitySugarGlider.this.random.nextInt(32) - 16);
                var blockVec = Vec3d.ofCenter(offset);
                var result = getWorld().raycast(new RaycastContext(EntitySugarGlider.this.getEyePos(), blockVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, EntitySugarGlider.this));
                if (result.getType() != HitResult.Type.MISS && result.getBlockPos().getSquaredDistance(mobPos) > 4) {
                    if (leavesOnly && !getWorld().getBlockState(result.getBlockPos()).isIn(BlockTags.LEAVES)) {
                        continue;
                    }
                    return result.getBlockPos();
                }
            }
            return null;
        }

        private int getDistanceOffGround(BlockPos pos) {
            int dist = 0;
            while (pos.getY() > -64 && EntitySugarGlider.this.getWorld().isAir(pos)) {
                pos = pos.down();
                dist++;
            }
            return dist;
        }

        private boolean isPositionEasilyClimbable(BlockPos pos) {
            do {
                pos = pos.down();
            } while (pos.getY() > EntitySugarGlider.this.getY() && !EntitySugarGlider.this.getWorld().isAir(pos));
            return pos.getY() <= EntitySugarGlider.this.getY();
        }
    }
}
