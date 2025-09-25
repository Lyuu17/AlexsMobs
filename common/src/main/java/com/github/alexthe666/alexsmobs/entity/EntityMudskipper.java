package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class EntityMudskipper extends TameableEntity implements IFollower, ISemiAquatic, Bucketable {

    public float prevSitProgress;
    public float sitProgress;
    public float prevSwimProgress;
    public float swimProgress;
    public float prevDisplayProgress;
    public float displayProgress;
    public float prevMudProgress;
    public float mudProgress;
    public float nextDisplayAngleFromServer;
    public float prevDisplayAngle;
    public boolean displayDirection;
    public int displayTimer = 0;
    public boolean instantlyTriggerDisplayAI = false;
    public int displayCooldown = 100 + random.nextInt(100);
    private static final TrackedData<Boolean> DISPLAYING = DataTracker.registerData(EntityMudskipper.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> DISPLAY_ANGLE = DataTracker.registerData(EntityMudskipper.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Optional<UUID>> DISPLAYER_UUID = DataTracker.registerData(EntityMudskipper.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> MOUTH_TICKS = DataTracker.registerData(EntityMudskipper.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> FROM_BUCKET = DataTracker.registerData(EntityMudskipper.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityMudskipper.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> COMMAND = DataTracker.registerData(EntityMudskipper.class, TrackedDataHandlerRegistry.INTEGER);
    private boolean isLandNavigator;
    private int swimTimer = -1000;

    public EntityMudskipper(EntityType<EntityMudskipper> type, World level) {
        super(type, level);
        this.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
        this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 0.0F);
        switchNavigator(true);
    }

    @Override
    public void travel(Vec3d travelVector) {
        if (this.isSitting()) {
            if (this.getNavigation().getCurrentPath() != null) {
                this.getNavigation().stop();
            }
            travelVector = Vec3d.ZERO;
            super.travel(travelVector);
            return;
        }
        if (this.canMoveVoluntarily() && this.isTouchingWater()) {
            this.updateVelocity(this.getMovementSpeed(), travelVector);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.9D));
        } else {
            super.travel(travelVector);
        }
    }

    public static <T extends MobEntity> boolean canMudskipperSpawn(EntityType type, WorldAccess worldIn, SpawnReason reason, BlockPos p_223317_3_, Random random) {
        BlockState blockstate = worldIn.getBlockState(p_223317_3_.down());
        return blockstate.isOf(Blocks.MUD) || blockstate.isOf(Blocks.MUDDY_MANGROVE_ROOTS);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.mudskipperSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    public boolean canSpawn(WorldView worldIn) {
        BlockPos pos = AMBlockPos.fromCoords(this.getX(), this.getEyeY(), this.getZ());
        return !worldIn.getBlockState(pos).shouldSuffocate(worldIn, pos);
    }

    @Override
    public boolean canBreatheInWater() {
        return true;
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SitGoal(this));
        this.goalSelector.add(1, new TameableAIFollowOwnerWater(this, 1.3D, 4.0F, 2.0F, false));
        this.goalSelector.add(2, new MudskipperAIAttack(this));
        this.goalSelector.add(3, new AnimalAIFindWater(this));
        this.goalSelector.add(3, new AnimalAILeaveWater(this));
        this.goalSelector.add(4, new TemptGoal(this, 1.1D, Ingredient.fromTag(AMTagRegistry.MUDSKIPPER_TAMEABLES), false));
        this.goalSelector.add(5, new AnimalMateGoal(this, 0.8D));
        this.goalSelector.add(6, new EscapeDangerGoal(this, 1D));
        this.goalSelector.add(7, new MudskipperAIDisplay(this));
        this.goalSelector.add(8, new SemiAquaticAIRandomSwimming(this, 1.0D, 80));
        this.goalSelector.add(9, new WanderAroundGoal(this, 1.0D, 120));
        this.goalSelector.add(10, new LookAroundGoal(this));
        this.goalSelector.add(11, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, new RevengeGoal(this) {
            @Override
            public boolean canStart() {
                return EntityMudskipper.this.isTamed() && super.canStart();
            }
        });
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new GroundPathNavigatorWide(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new AnimalSwimMoveControllerSink(this, 1.3F, 1);
            this.navigation = new SemiAquaticPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(DISPLAYING, false);
        this.dataTracker.startTracking(FROM_BUCKET, false);
        this.dataTracker.startTracking(DISPLAY_ANGLE, 0F);
        this.dataTracker.startTracking(DISPLAYER_UUID, Optional.empty());
        this.dataTracker.startTracking(MOUTH_TICKS, 0);
        this.dataTracker.startTracking(COMMAND, 0);
        this.dataTracker.startTracking(SITTING, false);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 12.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("FromBucket", this.isFromBucket());
        compound.putInt("DisplayCooldown", this.displayCooldown);
        compound.putInt("MudskipperCommand", this.getCommand());
        compound.putBoolean("MudskipperSitting", this.isSitting());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setFromBucket(compound.getBoolean("FromBucket"));
        this.displayCooldown = compound.getInt("DisplayCooldown");
        this.setCommand(compound.getInt("MudskipperCommand"));
        this.setSitting(compound.getBoolean("MudskipperSitting"));
    }

    @Override
    public void tick(){
        super.tick();
        prevSwimProgress = swimProgress;
        prevSitProgress = sitProgress;
        prevDisplayProgress = displayProgress;
        prevMudProgress = mudProgress;
        if(displayProgress < 5F && this.isDisplaying()){
            displayProgress++;
        }
        if(displayProgress > 0F && !this.isDisplaying()){
            displayProgress--;
        }
        if(sitProgress < 5F && this.isSitting()){
            sitProgress++;
        }
        if(sitProgress > 0F && !this.isSitting()){
            sitProgress--;
        }
        //so the model does not sink in mud
        boolean mud = onMud();
        if (mud) {
            if (mudProgress < 1F)
                mudProgress += 0.5f;
        } else {
            if (mudProgress > 0)
                mudProgress -= 0.5f;
        }

        boolean swim = !this.isOnGround() && this.isInsideWaterOrBubbleColumn();
        if(swimProgress < 5F && swim){
            swimProgress++;
        }
        if(swimProgress > 0 && !swim){
            swimProgress--;
        }
        if (!this.getWorld().isClient) {
            if (isInsideWaterOrBubbleColumn()) {
                swimTimer++;
            } else {
                swimTimer--;
            }
        }
        if (displayCooldown > 0) {
            displayCooldown--;
        }
        if(!this.getWorld().isClient){
            if(this.getDisplayAngle() < nextDisplayAngleFromServer){
                this.setDisplayAngle(this.getDisplayAngle() + 1);

            }
            if(this.getDisplayAngle() > nextDisplayAngleFromServer) {
                this.setDisplayAngle(this.getDisplayAngle() - 1);
            }
        }
        if(this.isMouthOpen()){
            this.openMouth(this.getMouthTicks() - 1);
        }
        if (this.isTouchingWater() && this.isLandNavigator) {
            switchNavigator(false);
        }
        if (!this.isTouchingWater() && !this.isLandNavigator) {
            switchNavigator(true);
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean prev = super.damage(source, amount);
        if (prev && source.getSource() instanceof LivingEntity) {
            this.openMouth(10);
        }
        return prev;
    }

    public boolean isDisplaying() {
        return this.dataTracker.get(DISPLAYING);
    }

    public void setDisplaying(boolean display) {
        this.dataTracker.set(DISPLAYING, display);
    }

    public float getDisplayAngle() {
        return this.dataTracker.get(DISPLAY_ANGLE);
    }

    public void setDisplayAngle(float scale) {
        this.dataTracker.set(DISPLAY_ANGLE, scale);
    }

    public int getMouthTicks() {
        return this.dataTracker.get(MOUTH_TICKS);
    }

    public void openMouth(int time) {
        this.dataTracker.set(MOUTH_TICKS, time);
    }

    @Nullable
    public UUID getDisplayingPartnerUUID() {
        return this.dataTracker.get(DISPLAYER_UUID).orElse(null);
    }

    public void setDisplayingPartnerUUID(@Nullable UUID uniqueId) {
        this.dataTracker.set(DISPLAYER_UUID, Optional.ofNullable(uniqueId));
    }

    @Nullable
    public Entity getDisplayingPartner() {
        UUID id = getDisplayingPartnerUUID();
        if (id != null && !this.getWorld().isClient) {
            return ((ServerWorld) getWorld()).getEntity(id);
        }
        return null;
    }

    public void setDisplayingPartner(@Nullable Entity jostlingPartner) {
        if (jostlingPartner == null) {
            this.setDisplayingPartnerUUID(null);
        } else {
            this.setDisplayingPartnerUUID(jostlingPartner.getUuid());
        }
    }

    public boolean canDisplayWith(EntityMudskipper mudskipper) {
        return !mudskipper.isBaby() && !mudskipper.isSitting() && !mudskipper.shouldFollow() && mudskipper.isOnGround() && mudskipper.getDisplayingPartnerUUID() == null && mudskipper.displayCooldown == 0;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld serverLevel, PassiveEntity ageableMob) {
        return AMEntityRegistry.MUDSKIPPER.get().create(serverLevel);
    }

    public boolean isMouthOpen() {
        return this.getMouthTicks() > 0;
    }

    public boolean onMud() {
        BlockState below = this.getWorld().getBlockState(this.getVelocityAffectingPos());
        return below.isOf(Blocks.MUD);
    }

    @Override
    public void updateLimbs(boolean flying) {
        float f1 = (float) MathHelper.magnitude(this.getX() - this.prevX, 0, this.getZ() - this.prevZ);
        float f2 = Math.min(f1 * 8.0F, 1.0F);
        this.limbAnimator.updateLimbs(f2, 0.4F);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(AMSoundRegistry.MUDSKIPPER_WALK.get(), 1F, 1.0F);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.MUDSKIPPER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.MUDSKIPPER_HURT.get();
    }

    public int getCommand() {
        return this.dataTracker.get(COMMAND);
    }

    public void setCommand(int command) {
        this.dataTracker.set(COMMAND, Integer.valueOf(command));
    }

    @Override
    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    @Override
    public void setSitting(boolean sit) {
        this.dataTracker.set(SITTING, Boolean.valueOf(sit));
    }

    @Override
    public boolean shouldEnterWater() {
        return (this.getAttacker() != null || swimTimer <= -1000) && !this.isDisplaying();
    }

    @Override
    public boolean shouldLeaveWater() {
        return swimTimer > 200 || this.isDisplaying();
    }

    @Override
    public boolean shouldStopMoving() {
        return this.isSitting();
    }

    @Override
    public int getWaterSearchRange() {
        return 10;
    }

    @Override
    public boolean isFromBucket() {
        return this.dataTracker.get(FROM_BUCKET);
    }

    @Override
    public void setFromBucket(boolean bucket) {
        this.dataTracker.set(FROM_BUCKET, bucket);
    }

    @Override
    @NotNull
    public ItemStack getBucketItem() {
        ItemStack stack = new ItemStack(AMItemRegistry.MUDSKIPPER_BUCKET.get());
        if (this.hasCustomName()) {
            stack.setCustomName(this.getCustomName());
        }
        return stack;
    }

    @Override
    public void copyDataToStack(@NotNull ItemStack bucket) {
        if (this.hasCustomName()) {
            bucket.setCustomName(this.getCustomName());
        }
        NbtCompound platTag = new NbtCompound();
        this.writeCustomDataToNbt(platTag);
        NbtCompound compound = bucket.getOrCreateNbt();
        compound.put("MudskipperData", platTag);
    }

    @Override
    public void copyDataFromNbt(@NotNull NbtCompound compound) {
        if (compound.contains("MudskipperData")) {
            this.readCustomDataFromNbt(compound.getCompound("MudskipperData"));
        }
    }

    @Override
    @NotNull
    public SoundEvent getBucketFillSound() {
        return SoundEvents.ITEM_BUCKET_FILL_FISH;
    }

    @Override
    public boolean shouldFollow() {
        return this.getCommand() == 1;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.MUDSKIPPER_BREEDABLES);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        Item item = itemstack.getItem();
        ActionResult type = super.interactMob(player, hand);
        if (!isTamed() && itemstack.isIn(AMTagRegistry.MUDSKIPPER_TAMEABLES)) {
            this.eat(player, hand, itemstack);
            this.openMouth(10);
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_STRIDER_EAT, this.getSoundVolume(), this.getSoundPitch());
            if (getRandom().nextInt(2) == 0) {
                this.setOwner(player);
                this.getWorld().sendEntityStatus(this, (byte) 7);
            } else {
                this.getWorld().sendEntityStatus(this, (byte) 6);
            }
            return ActionResult.SUCCESS;
        }
        if (isTamed() && itemstack.isIn(AMTagRegistry.MUDSKIPPER_FOODSTUFFS)) {
            if (this.getHealth() < this.getMaxHealth()) {
                this.eat(player, hand, itemstack);
                this.openMouth(10);
                this.emitGameEvent(GameEvent.EAT);
                this.playSound(SoundEvents.ENTITY_STRIDER_EAT, this.getSoundVolume(), this.getSoundPitch());
                this.heal(5);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        }
        var actionResult = itemstack.useOnEntity(player, this, hand);
        if (item != Items.WATER_BUCKET && actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS && isTamed() && isOwner(player) && !isBreedingItem(itemstack)) {
            this.setCommand(this.getCommand() + 1);
            if (this.getCommand() == 3) {
                this.setCommand(0);
            }
            player.sendMessage(Text.translatable("entity.alexsmobs.all.command_" + this.getCommand(), this.getName()), true);
            boolean sit = this.getCommand() == 2;
            this.setSitting(sit);
            return ActionResult.SUCCESS;
        }
        return Bucketable.tryBucket(player, hand, this).orElse(type);
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
    public EntityView method_48926() {
        return getWorld();
    }
}
