package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.misc.ModTagsCompat;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class EntityFlutter extends TameableEntity implements IFollower, Flutterer {

    private static final TrackedData<Float> FLUTTER_PITCH = DataTracker.registerData(EntityFlutter.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> FLYING = DataTracker.registerData(EntityFlutter.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> POTTED = DataTracker.registerData(EntityFlutter.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityFlutter.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> TENTACLING = DataTracker.registerData(EntityFlutter.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> COMMAND = DataTracker.registerData(EntityFlutter.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> SHOOTING = DataTracker.registerData(EntityFlutter.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> SHAKING_HEAD_TICKS = DataTracker.registerData(EntityFlutter.class, TrackedDataHandlerRegistry.INTEGER);
    public float prevFlyProgress;
    public float flyProgress;
    public float prevShootProgress;
    public float shootProgress;
    public float prevSitProgress;
    public float sitProgress;
    public float prevFlutterPitch;
    public float tentacleProgress;
    public float prevTentacleProgress;
    public float FlutterRotation;
    private float rotationVelocity;
    private int squishCooldown = 0;
    private float randomMotionSpeed;
    private boolean isLandNavigator;
    private int timeFlying;
    private List<String> flowersEaten = new ArrayList<>();
    private boolean hasPotStats = false;

    public EntityFlutter(EntityType<EntityFlutter> type, World level) {
        super(type, level);
        this.rotationVelocity = 1.0F / (this.random.nextFloat() + 1.0F) * 0.5F;
        switchNavigator(false);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 8.0D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.8F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.21F);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceToClosestPlayer) {
        return !cannotDespawn() && !this.hasCustomName();
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.hasCustomName() || this.isTamed() || this.isPotted();
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.flutterSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean canFlutterSpawnInLight(EntityType<? extends EntityFlutter> p_223325_0_, ServerWorldAccess p_223325_1_, SpawnReason p_223325_2_, BlockPos p_223325_3_, Random p_223325_4_) {
        return canMobSpawn(p_223325_0_, p_223325_1_, p_223325_2_, p_223325_3_, p_223325_4_);
    }

    public static <T extends MobEntity> boolean canFlutterSpawn(EntityType<EntityFlutter> entityType, ServerWorldAccess iServerWorld, SpawnReason reason, BlockPos pos, Random random) {
        BlockState blockstate = iServerWorld.getBlockState(pos.down());
        return reason == SpawnReason.SPAWNER || !iServerWorld.isSkyVisible(pos) && blockstate.isIn(AMTagRegistry.FLUTTER_SPAWNS) && pos.getY() <= 64 && canFlutterSpawnInLight(entityType, iServerWorld, reason, pos, random);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new FlyAwayFromTarget(this));
        this.goalSelector.add(2, new TameableAITempt(this, 1.1D, Ingredient.fromTag(AMTagRegistry.FLUTTER_BREEDABLES), false) {
            @Override
            public boolean shouldFollowAM(LivingEntity le) {
                return EntityFlutter.this.canEatFlower(le.getMainHandStack()) || EntityFlutter.this.canEatFlower(le.getOffHandStack()) || super.shouldFollowAM(le);
            }
        });
        this.goalSelector.add(3, new FlyingAIFollowOwner(this, 1.3D, 7.0F, 2.0F, false));
        this.goalSelector.add(4, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(5, new AIWalkIdle());
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 15.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MoveControl(this);
            this.navigation = new GroundPathNavigatorWide(this, getWorld());
            this.isLandNavigator = true;
        } else {
            this.moveControl = new FlightMoveController(this, 1F, false, true);
            this.navigation = new DirectPathNavigator(this, getWorld());
            this.isLandNavigator = false;
        }
    }

    @Override
    public boolean handleFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FLUTTER_PITCH, 0F);
        this.dataTracker.startTracking(FLYING, false);
        this.dataTracker.startTracking(POTTED, false);
        this.dataTracker.startTracking(COMMAND, 0);
        this.dataTracker.startTracking(SITTING, false);
        this.dataTracker.startTracking(TENTACLING, false);
        this.dataTracker.startTracking(SHOOTING, false);
        this.dataTracker.startTracking(SHAKING_HEAD_TICKS, 0);
    }

    public int getCommand() {
        return this.dataTracker.get(COMMAND);
    }

    public void setCommand(int command) {
        this.dataTracker.set(COMMAND, command);
    }

    @Override
    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    @Override
    public void setSitting(boolean sit) {
        this.dataTracker.set(SITTING, sit);
    }

    @Override
    public boolean isInAir() {
        return this.dataTracker.get(FLYING);
    }

    public void setFlying(boolean flying) {
        this.dataTracker.set(FLYING, flying);
    }

    public boolean isPotted() {
        return this.dataTracker.get(POTTED);
    }

    public void setPotted(boolean potted) {
        this.dataTracker.set(POTTED, potted);
    }

    public float getFlutterPitch() {
        return MathHelper.clamp(this.dataTracker.get(FLUTTER_PITCH), -90, 90);
    }

    public void setFlutterPitch(float pitch) {
        this.dataTracker.set(FLUTTER_PITCH, pitch);
    }

    public void incrementFlutterPitch(float pitch) {
        this.dataTracker.set(FLUTTER_PITCH, getFlutterPitch() + pitch);
    }

    public void decrementFlutterPitch(float pitch) {
        this.dataTracker.set(FLUTTER_PITCH, getFlutterPitch() - pitch);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.FLUTTER_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.FLUTTER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.FLUTTER_HURT.get();
    }

    @Override
    public void tick() {
        super.tick();
        prevShootProgress = shootProgress;
        prevFlyProgress = flyProgress;
        prevFlutterPitch = this.getFlutterPitch();
        prevSitProgress = sitProgress;
        float extraMotionSlow = 1.0F;
        float extraMotionSlowY = 1.0F;
        this.bodyYaw = this.getYaw();
        this.headYaw= this.getYaw();
        prevFlutterPitch = this.getFlutterPitch();
        prevTentacleProgress = this.tentacleProgress;

        if (isInAir()) {
            if (flyProgress < 5F)
                flyProgress++;
        } else {
            if (flyProgress > 0F)
                flyProgress--;
        }

        if (isSitting()) {
            if (sitProgress < 5F)
                sitProgress++;
        } else {
            if (sitProgress > 0F)
                sitProgress--;
        }

        if (tentacleProgress < 5F && this.dataTracker.get(TENTACLING)) {
            tentacleProgress++;
        }

        if(tentacleProgress == 5F && !this.dataTracker.get(TENTACLING)){
            if (squishCooldown == 0 && this.isInAir()) {
                squishCooldown = 10;
                this.playSound(AMSoundRegistry.FLUTTER_FLAP.get(), this.getSoundVolume(), 1.5F * this.getSoundPitch());
            }
        }
        if (tentacleProgress > 0F && !this.dataTracker.get(TENTACLING)) {
            tentacleProgress--;
        }
        this.FlutterRotation += this.rotationVelocity;
        if ((double) this.FlutterRotation > (MathHelper.TAU)) {
            if (this.getWorld().isClient) {
                this.FlutterRotation = MathHelper.TAU;
            } else {
                this.FlutterRotation = (float) ((double) this.FlutterRotation - (MathHelper.TAU));
                if (this.random.nextInt(10) == 0) {
                    this.rotationVelocity = 1.0F / (this.random.nextFloat() + 1.0F) * 0.5F;
                }
                this.getWorld().sendEntityStatus(this, (byte) 19);
            }
        }
        if (this.FlutterRotation < MathHelper.PI) {
            float f = this.FlutterRotation / MathHelper.PI;
            if ((double) f >= 0.95F) {
                this.dataTracker.set(TENTACLING, true);
                if (squishCooldown == 0 && this.isInAir()) {
                    squishCooldown = 10;
                    this.emitGameEvent(GameEvent.ENTITY_ROAR);
                    this.playSound(AMSoundRegistry.FLUTTER_FLAP.get(), 3F, 1.5F * this.getSoundPitch());
                }
                this.randomMotionSpeed = 0.8F;
            } else {
                this.dataTracker.set(TENTACLING, false);
                randomMotionSpeed = 0.01F;
            }
        }
        if (!this.getWorld().isClient) {
            if (isInAir() && this.isLandNavigator) {
                switchNavigator(false);
            }
            if (!isInAir() && !this.isLandNavigator) {
                switchNavigator(true);
            }
            if (this.isInAir()) {
                this.setVelocity(this.getVelocity().x * this.randomMotionSpeed * extraMotionSlow, this.getVelocity().y * this.randomMotionSpeed * extraMotionSlowY, this.getVelocity().z * this.randomMotionSpeed * extraMotionSlow);
                timeFlying++;
                if (this.isOnGround() && timeFlying > 20 || this.isSitting()) {
                    this.setFlying(false);
                }
            } else {
                timeFlying = 0;
            }
        }
        if (!this.isOnGround() && this.getVelocity().y < 0.0D) {
            this.setVelocity(this.getVelocity().multiply(1.0D, 0.8D, 1.0D));
        }
        if (this.isInAir()) {
            float dist = (float) ((Math.abs(this.getVelocity().x) + Math.abs(this.getVelocity().z)) * 30);
            this.incrementFlutterPitch(-dist);
            if (this.horizontalCollision) {
                this.setVelocity(this.getVelocity().add(0, 0.2F, 0));
            }
        }
        if (this.getFlutterPitch() > 0F) {
            float decrease = Math.min(2.5F, this.getFlutterPitch());
            this.decrementFlutterPitch(decrease);
        }
        if (this.getFlutterPitch() < 0F) {
            float decrease = Math.min(2.5F, -this.getFlutterPitch());
            this.incrementFlutterPitch(decrease);
        }
        boolean shooting = this.dataTracker.get(SHOOTING);
        if (shooting && shootProgress < 5) {
            shootProgress += 1;
        }
        if (!shooting && shootProgress > 0) {
            shootProgress -= 1;
        }
        if (shooting) {
            this.incrementFlutterPitch(-30);
        }
        if (!this.getWorld().isClient && shooting && shootProgress == 5F) {
            if (this.getTarget() != null) {
                this.spit(this.getTarget());
            }
            this.dataTracker.set(SHOOTING, false);
        }
        if (this.hasPotStats && !this.isPotted()) {
            this.hasPotStats = false;
            this.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(0.21D);
            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.21D);
        }
        if (!this.hasPotStats && this.isPotted()) {
            this.hasPotStats = true;
            this.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(16.0D);
            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.18D);
        }
        if (this.dataTracker.get(SHAKING_HEAD_TICKS) > 0) {
            this.dataTracker.set(SHAKING_HEAD_TICKS, this.dataTracker.get(SHAKING_HEAD_TICKS) - 1);
        }
        if(squishCooldown > 0){
            squishCooldown--;
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.FLUTTER_BREEDABLES) && this.isTamed();
    }

    private void spit(LivingEntity target) {
        var llamaspitentity = new EntityPollenBall(this.getWorld(), this);
        double d0 = target.getX() - this.getX();
        double d1 = target.getBodyY(0.3333333333333333D) - llamaspitentity.getY();
        double d2 = target.getZ() - this.getZ();
        float f = MathHelper.sqrt((float) (d0 * d0 + d2 * d2)) * 0.2F;
        llamaspitentity.shoot(d0, d1 + (double) f, d2, 0.5F, 13.0F);
        if (!this.isSilent()) {
            this.emitGameEvent(GameEvent.PROJECTILE_SHOOT);
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_LLAMA_SPIT, this.getSoundCategory(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
        }
        this.getWorld().spawnEntity(llamaspitentity);
    }

    public boolean isShakingHead() {
        return this.dataTracker.get(SHAKING_HEAD_TICKS) > 0;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        Item item = itemstack.getItem();
        ActionResult type = super.interactMob(player, hand);
        if (!isTamed() && canEatFlower(itemstack)) {
            this.eat(player, hand, itemstack);
            this.flowersEaten.add(Registries.ITEM.getKey(itemstack.getItem()).get().getValue().toString());
            this.emitGameEvent(GameEvent.ENTITY_INTERACT);
            this.playSound(AMSoundRegistry.FLUTTER_YES.get(), this.getSoundVolume(), this.getSoundPitch());
            if (this.flowersEaten.size() > 3 && getRandom().nextInt(3) == 0 || this.flowersEaten.size() > 6) {
                this.setOwner(player);
                this.getWorld().sendEntityStatus(this, (byte) 7);
            } else {
                this.getWorld().sendEntityStatus(this, (byte) 6);
            }
            return ActionResult.SUCCESS;
        } else if (!isTamed() && itemstack.isIn(ItemTags.FLOWERS)) {
            this.emitGameEvent(GameEvent.ENTITY_INTERACT);
            this.playSound(AMSoundRegistry.FLUTTER_NO.get(), this.getSoundVolume(), this.getSoundPitch());
            this.dataTracker.set(SHAKING_HEAD_TICKS, 20);
        }
        if (isTamed() && itemstack.isIn(ItemTags.FLOWERS) && this.getHealth() < this.getMaxHealth()) {
            this.eat(player, hand, itemstack);
            this.emitGameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.ENTITY_CAT_EAT, this.getSoundVolume(), this.getSoundPitch());
            this.heal(5);
            return ActionResult.SUCCESS;
        }
        var actionResult = itemstack.useOnEntity(player, this, hand);
        if (actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS && isTamed() && isOwner(player) && !isBreedingItem(itemstack) && !itemstack.isIn(ItemTags.FLOWERS)) {
            if (item == Items.FLOWER_POT && !this.isPotted()) {
                this.setPotted(true);
                return ActionResult.SUCCESS;
            } else if (ModTagsCompat.isAnyShear(itemstack) && this.isPotted()) {
                this.setPotted(false);
                this.dropItem(Items.FLOWER_POT);
                return ActionResult.SUCCESS;
            } else if(this.isPotted() && player.isSneaking()){
                ItemStack fish = getFishBucket();
                if (!player.giveItemStack(fish)) {
                    player.dropItem(fish, false);
                }
                this.remove(RemovalReason.DISCARDED);
                return ActionResult.success(this.getWorld().isClient);
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
    public void followEntity(TameableEntity tameable, LivingEntity owner, double followSpeed) {
        if (this.distanceTo(owner) > 8) {
            this.setFlying(true);
            this.getNavigation().startMovingTo(owner.getX(), owner.getY() + owner.getHeight(), owner.getZ(), followSpeed);
        } else {
            if (this.isInAir() && !this.isOverWaterOrVoid()) {
                BlockPos vec = this.getFlutterGround(this.getBlockPos());
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
    public boolean shouldFollow() {
        return this.getCommand() == 1;
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if (this.isPotted()) {
            if (!this.getWorld().isClient) {
                this.dropItem(Items.FLOWER_POT);
            }
        }
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
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Flying", this.isInAir());
        compound.putBoolean("Potted", this.isPotted());
        compound.putInt("FlowersEaten", flowersEaten.size());
        for (int i = 0; i < flowersEaten.size(); i++) {
            compound.putString("FlowerEaten" + i, flowersEaten.get(i));
        }
        compound.putInt("FlutterCommand", this.getCommand());
        compound.putBoolean("FlutterSitting", this.isSitting());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setFlying(compound.getBoolean("Flying"));
        this.setPotted(compound.getBoolean("Potted"));
        int flowerCount = compound.getInt("FlowersEaten");
        this.flowersEaten = new ArrayList<>();
        for (int i = 0; i < flowerCount; i++) {
            String s = compound.getString("FlowerEaten" + i);
            if (s != null) {
                flowersEaten.add(s);
            }
        }
        this.setCommand(compound.getInt("FlutterCommand"));
        this.setSitting(compound.getBoolean("FlutterSitting"));
    }

    private boolean isOverWaterOrVoid() {
        BlockPos position = this.getBlockPos();
        while (position.getY() > -63 && !getWorld().getBlockState(position).isSolid()) {
            position = position.down();
        }
        return !getWorld().getFluidState(position).isEmpty() || position.getY() < -63;
    }

    private BlockPos getFlutterGround(BlockPos in) {
        BlockPos position = new BlockPos(in.getX(), (int) this.getY(), in.getZ());
        while (position.getY() > -63 && !getWorld().getBlockState(position).isSolid()) {
            position = position.down();
        }
        if (position.getY() < -62) {
            return position.up(120 + random.nextInt(5));
        }

        return position;
    }

    public Vec3d getBlockInViewAway(Vec3d fleePos, float radiusAdd) {
        float radius = 1 + this.getRandom().nextInt(3) + radiusAdd;
        float neg = this.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = this.bodyYaw;
        float angle = (Maths.STARTING_ANGLE * renderYawOffset) + (this.getRandom().nextFloat() * neg) * 0.2F;
        double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = new BlockPos((int) (fleePos.x + extraX), 0, (int) (fleePos.z + extraZ));
        BlockPos ground = getFlutterGround(radialPos);
        int distFromGround = (int) this.getY() - ground.getY();
        int flightHeight = 3 + this.getRandom().nextInt(2);
        BlockPos newPos = ground.up(distFromGround > 4 ? flightHeight : distFromGround - 2 + this.getRandom().nextInt(4));
        if (!this.isTargetBlocked(Vec3d.ofCenter(newPos)) && this.squaredDistanceTo(Vec3d.ofCenter(newPos)) > 1) {
            return Vec3d.ofCenter(newPos);
        }
        return null;
    }

    public Vec3d getBlockGrounding(Vec3d fleePos) {
        float radius = 0.75F * (0.7F * 6) * -3 - this.getRandom().nextInt(24);
        float neg = this.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = this.bodyYaw;
        float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
        double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = AMBlockPos.fromCoords(fleePos.x + extraX, getY(), fleePos.z + extraZ);
        BlockPos ground = this.getFlutterGround(radialPos);
        if (ground.getY() <= -63) {
            return Vec3d.ofCenter(ground, 110 + random.nextInt(20));
        } else {
            ground = this.getBlockPos();
            while (ground.getY() > -63 && !getWorld().getBlockState(ground).isSolid()) {
                ground = ground.down();
            }
        }
        if (!this.isTargetBlocked(Vec3d.ofCenter(ground.up()))) {
            return Vec3d.ofCenter(ground.down());
        }
        return null;
    }

    public boolean isTargetBlocked(Vec3d target) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        return this.getWorld().raycast(new RaycastContext(Vector3d, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() != HitResult.Type.MISS;
    }

    protected ItemStack getFishBucket() {
        ItemStack stack = new ItemStack(AMItemRegistry.POTTED_FLUTTER.get());
        NbtCompound platTag = new NbtCompound();
        this.writeCustomDataToNbt(platTag);
        stack.getOrCreateNbt().put("FlutterData", platTag);
        if (this.hasCustomName()) {
            stack.setCustomName(this.getCustomName());
        }
        return stack;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld level, PassiveEntity mobo) {
        EntityFlutter baby = AMEntityRegistry.FLUTTER.get().create(getWorld());
        baby.setPersistent();
        return baby;
    }

    public boolean hasEatenFlower(ItemStack stack) {
        return flowersEaten != null && flowersEaten.contains(Registries.ITEM.getKey(stack.getItem()).get().getValue().toString());
    }

    public boolean canEatFlower(ItemStack stack) {
        return !hasEatenFlower(stack) && stack.isIn(ItemTags.FLOWERS);
    }

    private void setupShooting() {
        this.dataTracker.set(SHOOTING, true);
    }

    @Override
    public void breed(ServerWorld world, AnimalEntity partner) {
        super.breed(world, partner);
        for(int i = 0; i < 15 + random.nextInt(10); i++){
            BlockPos nearby = this.getBlockPos().add(random.nextInt(16) - 8, random.nextInt(2), random.nextInt(16) - 8);
            if(world.getBlockState(nearby).getBlock() == Blocks.AZALEA){
                world.setBlockState(nearby, Blocks.FLOWERING_AZALEA.getDefaultState());
                world.syncWorldEvent(1505, nearby, 0);
            }
            if(world.getBlockState(nearby).getBlock() == Blocks.AZALEA_LEAVES){
                world.setBlockState(nearby, Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState());
                world.syncWorldEvent(1505, nearby, 0);
            }
        }
    }

    @Override
    public EntityView method_48926() {
        return getWorld();
    }

    private class AIWalkIdle extends Goal {
        protected final EntityFlutter phage;
        protected double x;
        protected double y;
        protected double z;
        private boolean flightTarget = false;

        public AIWalkIdle() {
            super();
            this.setControls(EnumSet.of(Control.MOVE));
            this.phage = EntityFlutter.this;
        }

        @Override
        public boolean canStart() {
            if (this.phage.hasPassengers() || phage.isSitting() || phage.shouldFollow() || (phage.getTarget() != null && phage.getTarget().isAlive()) || this.phage.hasVehicle()) {
                return false;
            } else {
                if (this.phage.getRandom().nextInt(30) != 0 && !phage.isInAir() && !phage.isInsideWaterOrBubbleColumn()) {
                    return false;
                }
                if (this.phage.isOnGround() && !phage.isInsideWaterOrBubbleColumn()) {
                    this.flightTarget = random.nextInt(4) == 0 && !phage.isBaby();
                } else {
                    this.flightTarget = random.nextInt(5) > 0 && phage.timeFlying < 100 && !phage.isBaby();
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
                phage.getMoveControl().moveTo(x, y, z, 1F);
            } else {
                this.phage.getNavigation().startMovingTo(this.x, this.y, this.z, 1F);
            }
            if (!flightTarget && isInAir() && phage.isOnGround()) {
                phage.setFlying(false);
            }
            if (isInAir() && phage.isOnGround() && phage.timeFlying > 40) {
                phage.setFlying(false);
            }
        }

        @Nullable
        protected Vec3d getPosition() {
            Vec3d vector3d = phage.getPos();
            if (phage.isOverWaterOrVoid()) {
                flightTarget = true;
            }
            if (flightTarget) {
                if (phage.timeFlying < 180 || phage.isOverWaterOrVoid()) {
                    return phage.getBlockInViewAway(vector3d, 0);
                } else {
                    return phage.getBlockGrounding(vector3d);
                }
            } else {
                return FuzzyTargeting.find(this.phage, 5, 5);
            }
        }

        @Override
        public boolean shouldContinue() {
            if (phage.isSitting()) {
                return false;
            }
            if (flightTarget) {
                return phage.isInAir() && phage.squaredDistanceTo(x, y, z) > 2F && !phage.isBaby();
            } else {
                return (!this.phage.getNavigation().isIdle()) && !this.phage.hasPassengers();
            }
        }

        @Override
        public void start() {
            if (flightTarget) {
                phage.setFlying(true);
                phage.getMoveControl().moveTo(x, y, z, 1F);
            } else {
                this.phage.getNavigation().startMovingTo(this.x, this.y, this.z, 1F);
            }
        }

        @Override
        public void stop() {
            this.phage.getNavigation().stop();
            super.stop();
        }
    }

    private class FlyAwayFromTarget extends Goal {

        private final EntityFlutter parentEntity;
        private int spitCooldown = 0;
        private BlockPos shootPos = null;

        public FlyAwayFromTarget(EntityFlutter entityFlutter) {
            this.parentEntity = entityFlutter;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return !parentEntity.isSitting() && parentEntity.getTarget() != null && parentEntity.getTarget().isAlive()  && !parentEntity.isBaby();
        }

        @Override
        public void tick() {
            if (spitCooldown > 0) {
                spitCooldown--;
            }
            if (parentEntity.getTarget() != null) {
                parentEntity.setFlying(true);
                if (shootPos == null || parentEntity.distanceTo(parentEntity.getTarget()) >= 10F || parentEntity.getTarget().squaredDistanceTo(shootPos.getX() + 0.5F, shootPos.getY(), shootPos.getZ() + 0.5F) < 4) {
                    shootPos = getShootFromPos(parentEntity.getTarget());
                }
                if (shootPos != null) {
                    this.parentEntity.getMoveControl().moveTo(shootPos.getX() + 0.5D, shootPos.getY() + 0.5D, shootPos.getZ() + 0.5D, 1.5D);
                }
                if (parentEntity.distanceTo(parentEntity.getTarget()) < 25F) {
                    this.parentEntity.lookAtEntity(parentEntity.getTarget(), 30.0F, 30.0F);
                    if (spitCooldown == 0) {
                        parentEntity.setupShooting();
                        spitCooldown = 10 + random.nextInt(10);
                    }
                    shootPos = null;
                }
            }

        }

        public BlockPos getShootFromPos(LivingEntity target) {
            float radius = 3 + parentEntity.getRandom().nextInt(5);
            float angle = (Maths.STARTING_ANGLE * (target.headYaw+ 90F + parentEntity.getRandom().nextInt(180)));
            double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            double extraZ = radius * MathHelper.cos(angle);
            BlockPos radialPos = AMBlockPos.fromCoords(target.getX() + extraX, target.getY() + 2, target.getZ() + extraZ);
            if (!parentEntity.isTargetBlocked(Vec3d.ofCenter(radialPos))) {
                return radialPos;
            }
            return parentEntity.getBlockPos().up((int) Math.ceil(target.getHeight() + 1));
        }
    }
}
