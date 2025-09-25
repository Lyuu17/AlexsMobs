package com.github.alexthe666.alexsmobs.entity;


import com.github.alexthe666.alexsmobs.block.entity.LeafcutterAnthillBlockEntity;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMPointOfInterestRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import com.google.common.base.Predicates;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import com.iafenvoy.uranus.object.entity.pathfinding.raycoms.AdvancedPathNavigate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.ai.pathing.SpiderNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.BlockView;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EntityLeafcutterAnt extends AnimalEntity implements Angerable, IAnimatedEntity {

    public static final Animation ANIMATION_BITE = Animation.create(13);
    protected static final EntityDimensions QUEEN_SIZE = EntityDimensions.fixed(1.25F, 0.98F);
    public static final Identifier QUEEN_LOOT = new Identifier("alexsmobs", "entities/leafcutter_ant_queen");
    private static final TrackedData<Optional<BlockPos>> LEAF_HARVESTED_POS = DataTracker.registerData(EntityLeafcutterAnt.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);
    private static final TrackedData<Optional<BlockState>> LEAF_HARVESTED_STATE = DataTracker.registerData(EntityLeafcutterAnt.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_STATE);
    private static final TrackedData<Boolean> HAS_LEAF = DataTracker.registerData(EntityLeafcutterAnt.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> ANT_SCALE = DataTracker.registerData(EntityLeafcutterAnt.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Direction> ATTACHED_FACE = DataTracker.registerData(EntityLeafcutterAnt.class, TrackedDataHandlerRegistry.FACING);
    private static final TrackedData<Byte> CLIMBING = DataTracker.registerData(EntityLeafcutterAnt.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Boolean> QUEEN = DataTracker.registerData(EntityLeafcutterAnt.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> ANGER_TIME = DataTracker.registerData(EntityLeafcutterAnt.class, TrackedDataHandlerRegistry.INTEGER);
    private static final Direction[] HORIZONTALS = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final UniformIntProvider ANGRY_TIMER = TimeHelper.betweenSeconds(10, 20);
    public float attachChangeProgress = 0F;
    public float prevAttachChangeProgress = 0F;
    private Direction prevAttachDir = Direction.DOWN;
    @Nullable
    private EntityLeafcutterAnt caravanHead;
    @Nullable
    private EntityLeafcutterAnt caravanTail;
    private UUID lastHurtBy;
    @Nullable
    private BlockPos hivePos = null;
    private int stayOutOfHiveCountdown;
    private int animationTick;
    private Animation currentAnimation;
    private boolean isUpsideDownNavigator;
    private static final Ingredient TEMPTATION_ITEMS = Ingredient.fromTag(AMTagRegistry.LEAFCUTTER_ANT_FOODSTUFFS);
    private int haveBabyCooldown = 0;

    public EntityLeafcutterAnt(EntityType<EntityLeafcutterAnt> type, World world) {
        super(type, world);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
        switchNavigator(true);
    }

    @Override
    public void setTarget(@Nullable LivingEntity entitylivingbaseIn) {
        if(entitylivingbaseIn instanceof PlayerEntity && ((PlayerEntity) entitylivingbaseIn).isCreative()){
            return;
        }
        super.setTarget(entitylivingbaseIn);
    }

    @Nullable
    @Override
    protected Identifier getLootTableId() {
        return this.isQueen() ? QUEEN_LOOT : super.getLootTableId();
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.ARTHROPOD;
    }

    private void switchNavigator(boolean rightsideUp) {
        if (rightsideUp) {
            this.moveControl = new MoveControl(this);
            this.navigation = new AdvancedPathNavigateNoTeleport(this, getWorld(), AdvancedPathNavigate.MovementType.WALKING, true, false);
            this.isUpsideDownNavigator = false;
        } else {
            this.moveControl = new FlightMoveController(this, 0.6F, false);
            this.navigation = new DirectPathNavigator(this, getWorld());
            this.isUpsideDownNavigator = true;
        }
    }

    @Override
    public boolean collidesWith(Entity entity) {
        return !(entity instanceof EntityAnteater) && super.collidesWith(entity);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 8.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2F);
    }

    private static boolean isSideSolid(BlockView reader, BlockPos pos, Entity entityIn, Direction direction) {
        return Block.isFaceFullSquare(reader.getBlockState(pos).getCollisionShape(reader, pos, ShapeContext.of(entityIn)), direction);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new ReturnToHiveGoal());
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.add(3, new TameableAITempt(this, 1.1D, TEMPTATION_ITEMS, false));
        this.goalSelector.add(4, new LeafcutterAntAIFollowCaravan(this, 1D));
        this.goalSelector.add(5, new LeafcutterAntAIForageLeaves(this));
        this.goalSelector.add(6, new AnimalAIWanderRanged(this, 30, 1.0D, 25, 7));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, (new AngerGoal(this)).setGroupRevenge(new Class[0]));
        this.targetSelector.add(2, new UniversalAngerGoal<>(this, true));
    }

    @Override
    public EntityDimensions getDimensions(EntityPose poseIn) {
        return isQueen() && !isBaby() ? QUEEN_SIZE : super.getDimensions(poseIn);
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

    public Direction getAttachmentFacing() {
        return this.dataTracker.get(ATTACHED_FACE);
    }

    @Override
    protected EntityNavigation createNavigation(World worldIn) {
        return new SpiderNavigation(this, worldIn);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return isQueen() ? AMSoundRegistry.LEAFCUTTER_ANT_QUEEN_HURT.get() : AMSoundRegistry.LEAFCUTTER_ANT_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return isQueen() ? AMSoundRegistry.LEAFCUTTER_ANT_QUEEN_HURT.get() : AMSoundRegistry.LEAFCUTTER_ANT_HURT.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public void pushAway(Entity entity) {
        if(!(entity instanceof EntityAnteater)){
            super.pushAway(entity);
        }
    }

    private void pacifyAllNearby(){
        stopAnger();
        var list = getWorld().getNonSpectatingEntities(EntityLeafcutterAnt.class, this.getBoundingBox().expand(20D, 6.0D, 20D));
        for(var ant : list){
            ant.stopAnger();
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        ActionResult type = super.interactMob(player, hand);
        if(type != ActionResult.SUCCESS && itemstack.isIn(AMTagRegistry.LEAFCUTTER_ANT_FOODSTUFFS)){
            if(isQueen() && haveBabyCooldown == 0){
                int babies = 1 + random.nextInt(1);
                pacifyAllNearby();
                for(int i = 0; i < babies; i++){
                    var leafcutterAnt = AMEntityRegistry.LEAFCUTTER_ANT.get().create(getWorld());
                    leafcutterAnt.copyPositionAndRotation(this);
                    leafcutterAnt.setBreedingAge(-24000);
                    if(!this.getWorld().isClient){
                        getWorld().sendEntityStatus(this, (byte)18);
                        getWorld().spawnEntity(leafcutterAnt);
                    }
                }
                if(!player.isCreative()){
                    itemstack.decrement(1);
                }
                haveBabyCooldown = 24000;
                this.setBaby(false);
            }else{
                pacifyAllNearby();
                if(!player.isCreative()){
                    itemstack.decrement(1);
                }
                getWorld().sendEntityStatus(this, (byte)48);
                this.heal(3);
            }

            return ActionResult.SUCCESS;

        }
        return type;
    }

    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if (id == 48) {
            for(int i = 0; i < 3; ++i) {
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = this.random.nextGaussian() * 0.02D;
                double d2 = this.random.nextGaussian() * 0.02D;
                this.getWorld().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getParticleX(1.0D), this.getRandomBodyY() + 0.5D, this.getParticleZ(1.0D), d0, d1, d2);
            }
        } else {
            super.handleStatus(id);
        }
    }

    @Override
    public void tick() {
        this.prevAttachChangeProgress = this.attachChangeProgress;
        super.tick();
        if (this.isQueen() && this.getWidth() < QUEEN_SIZE.width) {
            this.calculateDimensions();
        }
        if (attachChangeProgress > 0F) {
            attachChangeProgress -= 0.25F;
        }
        this.setStepHeight(isQueen() ? 1F : 0.5F);
        var vector3d = this.getVelocity();
        if (!this.getWorld().isClient && !this.isQueen()) {
            this.setBesideClimbableBlock(this.horizontalCollision || this.verticalCollision && !this.isOnGround());
            if (this.isOnGround() || this.isInsideWaterOrBubbleColumn() || this.isInLava()) {
                this.dataTracker.set(ATTACHED_FACE, Direction.DOWN);
            } else  if (this.verticalCollision) {
                this.dataTracker.set(ATTACHED_FACE, Direction.UP);
            }else {
                Direction closestDirection = Direction.DOWN;
                double closestDistance = 100D;
                for (Direction dir : HORIZONTALS) {
                    BlockPos antPos = new BlockPos(MathHelper.floor(this.getX()), MathHelper.floor(this.getY()), MathHelper.floor(this.getZ()));
                    BlockPos offsetPos = antPos.offset(dir);
                    var offset = Vec3d.ofCenter(offsetPos);
                    if (closestDistance > this.getPos().distanceTo(offset) && getWorld().isDirectionSolid(offsetPos, this, dir.getOpposite())) {
                        closestDistance = this.getPos().distanceTo(offset);
                        closestDirection = dir;
                    }
                }
                this.dataTracker.set(ATTACHED_FACE, closestDirection);
            }
        }
        boolean flag = false;
        final Direction attachmentFacing = this.getAttachmentFacing();
        if (attachmentFacing != Direction.DOWN) {
            if(attachmentFacing == Direction.UP){
                this.setVelocity(this.getVelocity().add(0, 1, 0));
            }else{
                if (!this.horizontalCollision && attachmentFacing != Direction.UP) {
                    Vec3d vec = Vec3d.of(attachmentFacing.getVector());
                    this.setVelocity(this.getVelocity().add(vec.normalize().multiply(0.1F, 0.1F, 0.1F)));
                }
                if (!this.isOnGround() && vector3d.y < 0.0D) {
                    this.setVelocity(this.getVelocity().multiply(1.0D, 0.5D, 1.0D));
                    flag = true;
                }
            }
        }
        if(attachmentFacing == Direction.UP) {
            this.setNoGravity(true);
            this.setVelocity(vector3d.multiply(0.7D, 1D, 0.7D));
        }else{
            this.setNoGravity(false);
        }
        if (!flag) {
            if (this.isClimbing()) {
                this.setVelocity(vector3d.multiply(1.0D, 0.4D, 1.0D));
            }
        }
        if (prevAttachDir != attachmentFacing) {
            attachChangeProgress = 1F;
        }
        this.prevAttachDir = attachmentFacing;
        if (!this.getWorld().isClient) {
            if (attachmentFacing == Direction.UP && !this.isUpsideDownNavigator) {
                switchNavigator(false);
            }
            if (attachmentFacing != Direction.UP && this.isUpsideDownNavigator) {
                switchNavigator(true);
            }
            if (this.stayOutOfHiveCountdown > 0) {
                --this.stayOutOfHiveCountdown;
            }

            if (this.age % 20 == 0 && !this.isHiveValid()) {
                this.hivePos = null;
            }
            LivingEntity attackTarget = this.getTarget();
            if (attackTarget != null && distanceTo(attackTarget) < attackTarget.getWidth() + this.getWidth() + 1 && this.canSee(attackTarget)) {
                if (this.getAnimation() == ANIMATION_BITE && this.getAnimationTick() == 6) {
                    float damage = (float) ((int) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                    attackTarget.damage(this.getDamageSources().mobAttack(this), damage);
                }
            }
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    private boolean isClimeableFromSide(BlockPos offsetPos, Direction opposite) {
        return false;
    }

    private boolean isHiveValid() {
        if (!this.hasHive()) {
            return false;
        } else {
            var tileentity = this.getWorld().getBlockEntity(this.hivePos);
            return tileentity instanceof LeafcutterAnthillBlockEntity;
        }
    }

    @Override
    protected void onBlockCollision(BlockState state) {
    }

    public boolean isClimbing() {
        return this.isBesideClimbableBlock();
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

    @Override
    public int getAngerTime() {
        return this.dataTracker.get(ANGER_TIME);
    }

    @Override
    public void setAngerTime(int time) {
        this.dataTracker.set(ANGER_TIME, time);
    }

    @Override
    public UUID getAngryAt() {
        return this.lastHurtBy;
    }

    @Override
    public void setAngryAt(@Nullable UUID target) {
        this.lastHurtBy = target;
    }

    @Override
    public void chooseRandomAngerTime() {
        this.setAngerTime(ANGRY_TIMER.get(this.random));
    }

    @Override
    protected void mobTick() {
        if (!this.getWorld().isClient) {
            this.tickAngerLogic((ServerWorld)this.getWorld(), false);
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CLIMBING, (byte) 0);
        this.dataTracker.startTracking(LEAF_HARVESTED_POS, Optional.empty());
        this.dataTracker.startTracking(LEAF_HARVESTED_STATE, Optional.empty());
        this.dataTracker.startTracking(HAS_LEAF, false);
        this.dataTracker.startTracking(QUEEN, false);
        this.dataTracker.startTracking(ATTACHED_FACE, Direction.DOWN);
        this.dataTracker.startTracking(ANT_SCALE, 1.0F);
        this.dataTracker.startTracking(ANGER_TIME, 0);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.setAntScale(0.75F + random.nextFloat() * 0.3F);
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    public float getAntScale() {
        return this.dataTracker.get(ANT_SCALE);
    }

    public void setAntScale(float scale) {
        this.dataTracker.set(ANT_SCALE, scale);
    }

    public BlockPos getHarvestedPos() {
        return this.dataTracker.get(LEAF_HARVESTED_POS).orElse(null);
    }

    public void setLeafHarvestedPos(BlockPos harvestedPos) {
        this.dataTracker.set(LEAF_HARVESTED_POS, Optional.ofNullable(harvestedPos));
    }

    public BlockState getHarvestedState() {
        return this.dataTracker.get(LEAF_HARVESTED_STATE).orElse(null);
    }

    public void setLeafHarvestedState(BlockState state) {
        this.dataTracker.set(LEAF_HARVESTED_STATE, Optional.ofNullable(state));
    }

    public boolean hasLeaf() {
        return this.dataTracker.get(HAS_LEAF);
    }

    public void setLeaf(boolean leaf) {
        this.dataTracker.set(HAS_LEAF, Boolean.valueOf(leaf));
    }

    public boolean isQueen() {
        return this.dataTracker.get(QUEEN);
    }

    public void setQueen(boolean queen) {
        boolean prev = isQueen();
        if (!prev && queen) {
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(36.0D);
            this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(6.0D);
            this.setHealth(36F);
        } else {
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(6.0D);
            this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(2.0D);
        }
        this.dataTracker.set(QUEEN, queen);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.dataTracker.set(ATTACHED_FACE, Direction.byId(compound.getByte("AttachFace")));
        this.setLeaf(compound.getBoolean("Leaf"));
        this.setQueen(compound.getBoolean("Queen"));
        this.setAntScale(compound.getFloat("AntScale"));
        BlockState blockstate = null;
        if (compound.contains("HarvestedLeafState", 10)) {
            blockstate = NbtHelper.toBlockState(this.getWorld().createCommandRegistryWrapper(RegistryKeys.BLOCK), compound.getCompound("HarvestedLeafState"));
            if (blockstate.isAir()) {
                blockstate = null;
            }
        }
        this.stayOutOfHiveCountdown = compound.getInt("CannotEnterHiveTicks");
        this.haveBabyCooldown = compound.getInt("BabyCooldown");
        this.hivePos = null;
        if (compound.contains("HivePos")) {
            this.hivePos = NbtHelper.toBlockPos(compound.getCompound("HivePos"));
        }
        this.setLeafHarvestedState(blockstate);
        if (compound.contains("HLPX")) {
            int i = compound.getInt("HLPX");
            int j = compound.getInt("HLPY");
            int k = compound.getInt("HLPZ");
            this.dataTracker.set(LEAF_HARVESTED_POS, Optional.of(new BlockPos(i, j, k)));
        } else {
            this.dataTracker.set(LEAF_HARVESTED_POS, Optional.empty());
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putByte("AttachFace", (byte) this.dataTracker.get(ATTACHED_FACE).getId());
        compound.putBoolean("Leaf", this.hasLeaf());
        compound.putBoolean("Queen", this.isQueen());
        compound.putFloat("AntScale", this.getAntScale());
        BlockState blockstate = this.getHarvestedState();
        if (blockstate != null) {
            compound.put("HarvestedLeafState", NbtHelper.fromBlockState(blockstate));
        }
        if (this.hasHive()) {
            compound.put("HivePos", NbtHelper.fromBlockPos(this.getHivePos()));
        }
        compound.putInt("CannotEnterHiveTicks", this.stayOutOfHiveCountdown);
        compound.putInt("BabyCooldown", this.haveBabyCooldown);
        BlockPos blockpos = this.getHarvestedPos();
        if (blockpos != null) {
            compound.putInt("HLPX", blockpos.getX());
            compound.putInt("HLPY", blockpos.getY());
            compound.putInt("HLPZ", blockpos.getZ());
        }

    }

    public void setStayOutOfHiveCountdown(int p_226450_1_) {
        this.stayOutOfHiveCountdown = p_226450_1_;
    }

    private boolean isHiveNearFire() {
        if (this.hivePos == null) {
            return false;
        } else {
            var tileentity = this.getWorld().getBlockEntity(this.hivePos);
            return tileentity instanceof LeafcutterAnthillBlockEntity && ((LeafcutterAnthillBlockEntity) tileentity).isNearFire();
        }
    }

    private boolean doesHiveHaveSpace(BlockPos pos) {
        var tileentity = this.getWorld().getBlockEntity(pos);
        if (tileentity instanceof LeafcutterAnthillBlockEntity) {
            return !((LeafcutterAnthillBlockEntity) tileentity).isFullOfAnts();
        } else {
            return false;
        }
    }

    public boolean hasHive() {
        return this.hivePos != null;
    }

    @Nullable
    public BlockPos getHivePos() {
        return this.hivePos;
    }

    public void leaveCaravan() {
        if (this.caravanHead != null) {
            this.caravanHead.caravanTail = null;
        }

        this.caravanHead = null;
    }

    public void joinCaravan(EntityLeafcutterAnt caravanHeadIn) {
        this.caravanHead = caravanHeadIn;
        this.caravanHead.caravanTail = this;
    }

    public boolean hasCaravanTrail() {
        return this.caravanTail != null;
    }

    public boolean inCaravan() {
        return this.caravanHead != null;
    }

    @Nullable
    public EntityLeafcutterAnt getCaravanHead() {
        return this.caravanHead;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    public boolean shouldLeadCaravan() {
        return !this.hasLeaf();
    }

    @Override
    public void updateLimbs(boolean flying) {
        float f1 = (float)MathHelper.magnitude(this.getX() - this.prevX, 2 * (this.getY() - this.prevY), this.getZ() - this.prevZ);
        float f2 = Math.min(f1 * 4.0F, 1.0F);
        this.limbAnimator.updateLimbs(f2, 0.4F);
    }

    @Override
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        currentAnimation = animation;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_BITE};
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int tick) {
        animationTick = tick;
    }

    @Override
    public boolean tryAttack(Entity entityIn) {
        this.setAnimation(ANIMATION_BITE);
        return true;
    }

    private class ReturnToHiveGoal extends Goal {

        private int searchCooldown = 1;
        private BlockPos hivePos;
        private int approachTime = 0;
        private int moveToCooldown = 0;

        public ReturnToHiveGoal() {
        }

        @Override
        public boolean canStart() {
            if(EntityLeafcutterAnt.this.stayOutOfHiveCountdown > 0){
                return false;
            }
            if (EntityLeafcutterAnt.this.hasLeaf() || EntityLeafcutterAnt.this.isQueen()) {
                searchCooldown--;
                BlockPos hive = EntityLeafcutterAnt.this.hivePos;
                if (hive != null && EntityLeafcutterAnt.this.getWorld().getBlockEntity(hive) instanceof LeafcutterAnthillBlockEntity) {
                    hivePos = hive;
                    return true;
                }
                if (searchCooldown <= 0) {
                    searchCooldown = 400;
                    var pointofinterestmanager = ((ServerWorld) getWorld()).getPointOfInterestStorage();
                    var stream = pointofinterestmanager.getPositions(poiTypeHolder -> poiTypeHolder.matchesKey(AMPointOfInterestRegistry.LEAFCUTTER_ANT_HILL.getKey()), Predicates.alwaysTrue(), EntityLeafcutterAnt.this.getBlockPos(), 100, PointOfInterestStorage.OccupationStatus.ANY);
                    List<BlockPos> listOfHives = stream.toList();
                    BlockPos ret = null;
                    for (BlockPos pos : listOfHives) {
                        if (ret == null || pos.getSquaredDistance(EntityLeafcutterAnt.this.getBlockPos()) < ret.getSquaredDistance(EntityLeafcutterAnt.this.getBlockPos())) {
                            ret = pos;
                        }
                    }
                    hivePos = ret;
                    EntityLeafcutterAnt.this.hivePos = ret;
                    return hivePos != null;
                }
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return hivePos != null && EntityLeafcutterAnt.this.squaredDistanceTo(Vec3d.ofCenter(hivePos, 1)) > 1F;
        }

        @Override
        public void stop() {
            this.hivePos = null;
            this.searchCooldown = 20;
            this.approachTime = 0;
        }

        @Override
        public void start() {
            this.searchCooldown = 20;
            this.approachTime = 0;
            moveToCooldown = 10 + random.nextInt(10);
        }

        @Override
        public void tick() {
            if(moveToCooldown > 0){
                moveToCooldown--;
            }
            if(hivePos != null){

                double dist = EntityLeafcutterAnt.this.squaredDistanceTo(Vec3d.ofCenter(hivePos, 1));
                if (dist < 1.2F && EntityLeafcutterAnt.this.getVelocityAffectingPos().equals(hivePos)) {
                    var tileentity = EntityLeafcutterAnt.this.getWorld().getBlockEntity(hivePos);
                    if (tileentity instanceof LeafcutterAnthillBlockEntity beehivetileentity) {
                        beehivetileentity.tryEnterHive(EntityLeafcutterAnt.this, EntityLeafcutterAnt.this.hasLeaf());
                    }
                }
                if (dist < 16) {
                    approachTime++;
                    if(dist < 4){
                        var center = Vec3d.ofCenter(hivePos, 1.1F);
                        var add = center.subtract(EntityLeafcutterAnt.this.getPos());
                        if(add.length() > 1F){
                            add = add.normalize();
                        }
                        add = add.multiply(0.2F);
                        EntityLeafcutterAnt.this.setVelocity(EntityLeafcutterAnt.this.getVelocity().add(add));
                    }
                    if(dist < (approachTime < 200 ? 2 : 10) && EntityLeafcutterAnt.this.getY() >= hivePos.getY()){
                        if(EntityLeafcutterAnt.this.getAttachmentFacing() != Direction.DOWN){
                            EntityLeafcutterAnt.this.setVelocity(EntityLeafcutterAnt.this.getVelocity().add(0, 0.1, 0));
                        }
                        EntityLeafcutterAnt.this.getMoveControl().moveTo((double) hivePos.getX() + 0.5F, (double) hivePos.getY() + 1.5F, (double) hivePos.getZ() + 0.5F, 1.0D);
                    }
                    if(moveToCooldown <= 0){
                        moveToCooldown = 50 + random.nextInt(30);
                        EntityLeafcutterAnt.this.navigation.resetRangeMultiplier();
                        EntityLeafcutterAnt.this.navigation.startMovingTo((double) hivePos.getX() + 0.5F, (double) hivePos.getY() + 1.6F, (double) hivePos.getZ() + 0.5F, 1.0D);
                    }
                } else {
                    startMovingToFar(this.hivePos);
                }
            }
        }

        private boolean startMovingToFar(BlockPos pos) {
            if(moveToCooldown <= 0){
                moveToCooldown = 50 + random.nextInt(30);
                EntityLeafcutterAnt.this.navigation.setRangeMultiplier(10.0F);
                EntityLeafcutterAnt.this.navigation.startMovingTo(pos.getX(), pos.getY(), pos.getZ(), 1.0D);
            }
            return EntityLeafcutterAnt.this.navigation.getCurrentPath() != null && EntityLeafcutterAnt.this.navigation.getCurrentPath().reachesTarget();
        }

    }

    class AngerGoal extends RevengeGoal {
        AngerGoal(EntityLeafcutterAnt beeIn) {
            super(beeIn);
            this.setGroupRevenge(EntityLeafcutterAnt.class);
        }

        /**
         * Returns whether an in-progress EntityAIBase should continue executing
         */
        @Override
        public boolean shouldContinue() {
            return EntityLeafcutterAnt.this.hasAngerTime() && super.shouldContinue();
        }

        @Override
        protected void setMobEntityTarget(MobEntity mobIn, LivingEntity targetIn) {
            if (mobIn instanceof EntityLeafcutterAnt && this.mob.canSee(targetIn)) {
                mobIn.setTarget(targetIn);
            }
        }
    }
}
