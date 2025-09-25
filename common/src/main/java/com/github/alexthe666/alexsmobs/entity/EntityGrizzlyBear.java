package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.client.sound.SoundBearMusicBox;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.registry.*;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.AnimationHandler;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShovelItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class EntityGrizzlyBear extends TameableEntity implements Angerable, IAnimatedEntity, ITargetsDroppedItems, IFollower {

    public static final Animation ANIMATION_MAUL = Animation.create(20);
    public static final Animation ANIMATION_SNIFF = Animation.create(12);
    public static final Animation ANIMATION_SWIPE_R = Animation.create(15);
    public static final Animation ANIMATION_SWIPE_L = Animation.create(20);
    private static final TrackedData<Boolean> STANDING = DataTracker.registerData(EntityGrizzlyBear.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SITTING = DataTracker.registerData(EntityGrizzlyBear.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HONEYED = DataTracker.registerData(EntityGrizzlyBear.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> EATING = DataTracker.registerData(EntityGrizzlyBear.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SNOWY = DataTracker.registerData(EntityGrizzlyBear.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> APRIL_FOOLS_MODE = DataTracker.registerData(EntityGrizzlyBear.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> COMMAND = DataTracker.registerData(EntityGrizzlyBear.class, TrackedDataHandlerRegistry.INTEGER);
    private static final UniformIntProvider angerLogic = TimeHelper.betweenSeconds(20, 39);
    public float prevStandProgress;
    public float prevSitProgress;
    public float standProgress;
    public float sitProgress;
    public int maxStandTime = 75;
    public boolean forcedSit = false;
    private int animationTick;
    private Animation currentAnimation;
    private int standingTime = 0;
    private int sittingTime = 0;
    private int maxSitTime = 75;
    private int eatingTime = 0;
    private int angerTime;
    private UUID angerTarget;
    private int honeyedTime;
    @Nullable
    private UUID salmonThrowerID = null;
    private static final Ingredient TEMPTATION_ITEMS = Ingredient.fromTag(AMTagRegistry.GORILLA_FOODSTUFFS);
    public int timeUntilNextFur = this.random.nextInt(24000) + 24000;
    protected static final EntityDimensions STANDING_SIZE = EntityDimensions.changing(1.7F,  2.75F);
    private boolean recalcSize = false;
    private int snowTimer = 0;
    private boolean permSnow = false;

    public EntityGrizzlyBear(EntityType<? extends EntityGrizzlyBear> type, World worldIn) {
        super(type, worldIn);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 55.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.0D);
    }

    @Override
    public EntityDimensions getDimensions(EntityPose poseIn) {
        return isStanding() ? STANDING_SIZE.scaled(this.getScaleFactor()) : super.getDimensions(poseIn);
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.grizzlyBearSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            var entity = source.getAttacker();
            this.setSitting(false);
            if (entity != null && this.isTamed() && !(entity instanceof PlayerEntity) && !(entity instanceof PersistentProjectileEntity)) {
                amount = (amount + 1.0F) / 3.0F;
            }
            return super.damage(source, amount);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.GRIZZLY_BEAR_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.GRIZZLY_BEAR_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.GRIZZLY_BEAR_DIE.get();
    }

    @Override
    public void updatePassengerPosition(Entity passenger, Entity.PositionUpdater moveFunc) {
        if (this.hasPassenger(passenger)) {
            float sitAdd = -0.065F * this.sitProgress;
            float standAdd = -0.07F * this.standProgress;
            float radius = standAdd + sitAdd;
            float angle = (Maths.STARTING_ANGLE * this.bodyYaw);
            double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            double extraZ = radius * MathHelper.cos(angle);
            passenger.setPos(this.getX() + extraX, this.getY() + this.getMountedHeightOffset() + passenger.getHeightOffset(), this.getZ() + extraZ);
        }
    }

    @Override
    public double getMountedHeightOffset() {
        float f = Math.min(0.25F, this.limbAnimator.getSpeed());
        float f1 = this.limbAnimator.getPos();
        float sitAdd = 0.01F * this.sitProgress;
        float standAdd = 0.07F * this.standProgress;
        return (double)this.getHeight() - 0.3D + (double)(0.12F * MathHelper.cos(f1 * 0.7F) * 0.7F * f) + sitAdd + standAdd;
    }

    @Override
    public void playAmbientSound() {
        if(!isFreddy()){
            super.playAmbientSound();
        }
    }

    @Override
    protected float getBaseMovementSpeedMultiplier() {
        return hasPassengers() ? 0.9F : 0.98F;
    }

    @Override
    public void chooseRandomAngerTime() {
        this.setAngerTime(angerLogic.get(this.random));
    }

    @Override
    public int getAngerTime() {
        return this.angerTime;
    }

    @Override
    public void setAngerTime(int time) {
        this.angerTime = time;
    }

    @Override
    public UUID getAngryAt() {
        return this.angerTarget;
    }

    @Override
    public void setAngryAt(@Nullable UUID target) {
        this.angerTarget = target;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.getName() != null && source.getName().equals("sting") || source.isOf(DamageTypes.IN_WALL) ||super.isInvulnerableTo(source);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new SitGoal(this));
        this.goalSelector.add(2, new TameableAIFollowOwner(this, 1.2D, 5.0F, 2.0F, false));
        this.goalSelector.add(3, new GrizzlyBearAIAprilFools(this));
        this.goalSelector.add(4, new MyMeleeAttackGoal());
        this.goalSelector.add(4, new MyEscapeDangerGoal());
        this.goalSelector.add(5, new TameableAITempt(this, 1.1D, TEMPTATION_ITEMS, false));
        this.goalSelector.add(5, new FollowParentGoal(this, 1.25D));
        this.goalSelector.add(5, new GrizzlyBearAIBeehive(this));
        this.goalSelector.add(6, new GrizzlyBearAIFleeBees(this, 14, 1D, 1D));
        this.goalSelector.add(6, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(7, new WanderAroundGoal(this, 0.75D));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, new MyRevengeGoal());
        this.targetSelector.add(4, new CreatureAITargetItems<>(this, false));
        this.targetSelector.add(5, new EntityGrizzlyBear.AttackPlayerGoal());
        this.targetSelector.add(6, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
        this.targetSelector.add(7, new UntamedActiveTargetGoal<>(this, FoxEntity.class, false, null));
        this.targetSelector.add(8, new UntamedActiveTargetGoal<>(this, WolfEntity.class, false, null));
        this.targetSelector.add(7, new UniversalAngerGoal<>(this, false));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean("Honeyed", this.isHoneyed());
        compound.putBoolean("Snowy", this.isSnowy());
        compound.putBoolean("Standing", this.isStanding());
        compound.putBoolean("BearSitting", this.isSitting());
        compound.putBoolean("ForcedToSit", this.forcedSit);
        compound.putBoolean("SnowPerm", this.permSnow);
        compound.putInt("FurTime", this.timeUntilNextFur);
        compound.putInt("BearCommand", this.getCommand());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.setHoneyed(compound.getBoolean("Honeyed"));
        this.setSnowy(compound.getBoolean("Snowy"));
        this.setStanding(compound.getBoolean("Standing"));
        this.setSitting(compound.getBoolean("BearSitting"));
        this.setCommand(compound.getInt("BearCommand"));
        this.forcedSit = compound.getBoolean("ForcedToSit");
        this.permSnow = compound.getBoolean("SnowPerm");
        this.timeUntilNextFur = compound.getInt("FurTime");
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return isTamed() && stack.isIn(AMTagRegistry.GRIZZLY_BREEDABLES);
    }

    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if (id == 67) {
            SoundBearMusicBox sound;
            if (SoundBearMusicBox.BEAR_MUSIC_BOX_SOUND_MAP.get(this.getId()) == null) {
                sound = new SoundBearMusicBox(this);
                SoundBearMusicBox.BEAR_MUSIC_BOX_SOUND_MAP.put(this.getId(), sound);
            } else {
                sound = SoundBearMusicBox.BEAR_MUSIC_BOX_SOUND_MAP.get(this.getId());
            }
            if (!MinecraftClient.getInstance().getSoundManager().isPlaying(sound) && sound.canPlay() && sound.isOnlyMusicBox()) {
                MinecraftClient.getInstance().getSoundManager().play(sound);
            }
        } else  if (id == 68) {
            MinecraftClient.getInstance().world.addParticle(
                    AMParticleRegistry.BEAR_FREDDY.get(),
                    MinecraftClient.getInstance().player.getX(),
                    MinecraftClient.getInstance().player.getY(),
                    MinecraftClient.getInstance().player.getZ(),
                    0, 0, 0);
        } else{
            super.handleStatus(id);
        }
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        for (var passenger : this.getPassengerList()) {
            if (passenger instanceof PlayerEntity player) {
                return player;
            }
        }
        return null;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        var itemstack = player.getStackInHand(hand);
        var item = itemstack.getItem();
        var type = super.interactMob(player, hand);
        if(item == Items.SNOW && !this.isSnowy() && !this.getWorld().isClient){
            this.eat(player, hand, itemstack);
            this.permSnow = true;
            this.setSnowy(true);
            this.emitGameEvent(GameEvent.ENTITY_INTERACT);
            this.playSound(SoundEvents.BLOCK_SNOW_PLACE, this.getSoundVolume(), this.getSoundPitch());
            return ActionResult.SUCCESS;
        }
        if(item instanceof ShovelItem && this.isSnowy() && !this.getWorld().isClient){
            this.permSnow = false;
            if(!player.isCreative()){
                itemstack.damage(1, this.getRandom(), player instanceof ServerPlayerEntity ? (ServerPlayerEntity) player : null);
            }
            this.setSnowy(false);
            this.emitGameEvent(GameEvent.ENTITY_INTERACT);
            this.playSound(SoundEvents.BLOCK_SNOW_BREAK, this.getSoundVolume(), this.getSoundPitch());
            return ActionResult.SUCCESS;
        }
        var actionResult = itemstack.useOnEntity(player, this, hand);
        if (actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS && isTamed() && isOwner(player) && !isBreedingItem(itemstack)){
            if(!player.isSneaking() && !this.isBaby()){
                player.startRiding(this);
            }else{
                this.setCommand((this.getCommand() + 1) % 3);
                if (this.getCommand() == 3) {
                    this.setCommand(0);
                }
                player.sendMessage(Text.translatable("entity.alexsmobs.all.command_" + this.getCommand(), this.getName()), true);
                boolean sit = this.getCommand() == 2;
                this.forcedSit = sit;
                this.setSitting(sit);
            }
            return ActionResult.SUCCESS;
        }
        return type;
    }

    @Override
    protected Vec3d getControlledMovementInput(PlayerEntity player, Vec3d deltaIn) {
        if (player.forwardSpeed != 0) {
            float f = player.forwardSpeed < 0.0F ? 0.5F : 1.0F;
            return new Vec3d(player.sidewaysSpeed * 0.25F, 0.0D, player.forwardSpeed * 0.5F * f);
        } else {
            this.setSprinting(false);
        }
        return Vec3d.ZERO;
    }

    @Override
    protected void tickControlled(PlayerEntity player, Vec3d vec3) {
        super.tickControlled(player, vec3);
        if(player.forwardSpeed != 0 || player.sidewaysSpeed != 0){
            this.setRotation(player.getYaw(), player.getPitch() * 0.25F);
            this.prevYaw = this.bodyYaw = this.headYaw= this.getYaw();
            this.setStepHeight(1);
            this.getNavigation().stop();
            this.setTarget(null);
            this.setSprinting(true);
        }
    }

    @Override
    protected float getSaddledSpeed(PlayerEntity rider) {
        return (float)(this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
    }

    @Override
    public void travel(Vec3d vec3d) {
        if (!this.shouldMove()) {
            if (this.getNavigation().getCurrentPath() != null) {
                this.getNavigation().stop();
            }
            vec3d = Vec3d.ZERO;
        }
        super.travel(vec3d);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isBaby() || this.getStandingEyeHeight() > this.getHeight()) {
            this.calculateDimensions();
        }
        if(!isStanding() && this.getHeight() >= 2.75F){
            this.calculateDimensions();
        }
        this.prevStandProgress = this.standProgress;
        this.prevSitProgress = this.sitProgress;

        if (this.isSitting()) {
            if (sitProgress < 10F)
                sitProgress++;
        } else {
            if (sitProgress > 0F)
                sitProgress--;
        }

        if (this.isStanding()) {
            if (standProgress < 10F)
                standProgress++;
        } else {
            if (standProgress > 0F)
                standProgress--;
        }

        if(!this.getStackInHand(Hand.MAIN_HAND).isEmpty() && this.canTargetItem(this.getStackInHand(Hand.MAIN_HAND))){
            this.setEating(true);
            this.setSitting(true);
            this.setStanding(false);
        }
        if(recalcSize){
            recalcSize = false;
            this.calculateDimensions();
        }

        if(isEating() && !this.canTargetItem(this.getStackInHand(Hand.MAIN_HAND))){
            this.setEating(false);
            eatingTime = 0;
            if(!forcedSit){
                this.setSitting(true);
            }
        }
        if(isEating()){
            eatingTime++;
            for(int i = 0; i < 3; i++){
                double d2 = this.random.nextGaussian() * 0.02D;
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = this.random.nextGaussian() * 0.02D;
                this.getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, this.getStackInHand(Hand.MAIN_HAND)), this.getX() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, this.getY() + this.getHeight() * 0.5F + (double) (this.random.nextFloat() * this.getHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getWidth()) - (double) this.getWidth() * 0.5F, d0, d1, d2);
            }
            if(eatingTime % 5 == 0){
                this.emitGameEvent(GameEvent.EAT);
                this.playSound(SoundEvents.ENTITY_GENERIC_EAT, this.getSoundVolume(), this.getSoundPitch());
            }
            if(eatingTime > 100){
                ItemStack stack = this.getStackInHand(Hand.MAIN_HAND);
                if(!stack.isEmpty()){
                    if(stack.isIn(AMTagRegistry.GRIZZLY_HONEY)){
                        this.setHoneyed(true);
                        this.heal(10);
                        this.honeyedTime = 700;
                    }else{
                        this.heal(4);
                    }
                    if(stack.isIn(AMTagRegistry.GRIZZLY_TAMEABLES) && !this.isTamed() && this.salmonThrowerID != null){
                       if(getRandom().nextFloat() < 0.3F){
                           this.setTamed(true);
                           this.setOwnerUuid(this.salmonThrowerID);
                           PlayerEntity player = getWorld().getPlayerByUuid(salmonThrowerID);
                           if (player instanceof ServerPlayerEntity playerEntity) {
                               Criteria.TAME_ANIMAL.trigger(playerEntity, this);
                           }
                           this.getWorld().sendEntityStatus(this, (byte)7);
                       }else{
                           this.getWorld().sendEntityStatus(this, (byte)6);
                       }
                    }
                    if(stack.getItem().hasRecipeRemainder()){
                        this.dropItem(stack.getItem().getRecipeRemainder());
                    }
                    stack.decrement(1);
                }
                eatingTime = 0;
            }
        }
        if (isStanding() && ++standingTime > maxStandTime) {
            this.setStanding(false);
            standingTime = 0;
            maxStandTime = 75 + random.nextInt(50);
        }
        if (isSitting() && !forcedSit && ++sittingTime > maxSitTime) {
            this.setSitting(false);
            sittingTime = 0;
            maxSitTime = 75 + random.nextInt(50);
        }
        if (!this.getWorld().isClient && this.getAnimation() == NO_ANIMATION && !this.isStanding() && !this.isSitting() && random.nextInt(1500) == 0) {
            maxSitTime = 300 + random.nextInt(250);
            this.setSitting(true);
        }
        /*
        if(this.getAnimation() == NO_ANIMATION && !this.isStanding() && !this.isSitting() && rand.nextInt(1500) == 0){
            maxStandTime = 75 + rand.nextInt(50);
            this.setStanding(true);
        }
         */
        if (!forcedSit && this.isSitting() && (this.getTarget() != null || this.isStanding()) && !this.isEating()) {
            this.setSitting(false);
        }
        if (this.getAnimation() == NO_ANIMATION && this.getAprilFoolsFlag() < 1 && random.nextInt(isStanding() ? 350 : 2500) == 0) {
            this.setAnimation(ANIMATION_SNIFF);
        }
        if (this.isSitting()) {
            this.getNavigation().stop();
        }
        LivingEntity attackTarget = this.getTarget();
        if(this.getControllingPassenger() != null && this.getControllingPassenger() instanceof PlayerEntity){
            var rider = (PlayerEntity)this.getControllingPassenger();
            if(rider.getAttacking() != null && this.distanceTo(rider.getAttacking()) < this.getWidth() + 3F && !this.isTeammate(rider.getAttacking())){
                UUID preyUUID = rider.getAttacking().getUuid();
                if (!this.getUuid().equals(preyUUID)) {
                    attackTarget = rider.getAttacking();
                    if (getAnimation() == NO_ANIMATION || getAnimation() == ANIMATION_SNIFF) {
                        EntityGrizzlyBear.this.setAnimation(random.nextBoolean() ? ANIMATION_MAUL : random.nextBoolean() ? ANIMATION_SWIPE_L : ANIMATION_SWIPE_R);
                    }
                }
            }
        }
        if (attackTarget != null) {
            if(!this.getWorld().isClient){
                this.setSprinting(true);
            }
            if (distanceTo(attackTarget) < attackTarget.getWidth() + this.getWidth() + 2.5F) {
                if (this.getAnimation() == ANIMATION_MAUL && this.getAnimationTick() % 5 == 0 && this.getAnimationTick() > 3) {
                    tryAttack(attackTarget);
                }
                if ((this.getAnimation() == ANIMATION_SWIPE_L) && this.getAnimationTick() == 7) {
                    tryAttack(attackTarget);
                    float rot = getYaw() + 90;
                    attackTarget.takeKnockback(0.5F, MathHelper.sin(rot * MathHelper.RADIANS_PER_DEGREE), -MathHelper.cos(rot * MathHelper.RADIANS_PER_DEGREE));
                }
                if ((this.getAnimation() == ANIMATION_SWIPE_R) && this.getAnimationTick() == 7) {
                    tryAttack(attackTarget);
                    float rot = getYaw() - 90;
                    attackTarget.takeKnockback(0.5F, MathHelper.sin(rot * MathHelper.RADIANS_PER_DEGREE), -MathHelper.cos(rot * MathHelper.RADIANS_PER_DEGREE));
                }

            }
        }else{
            if(!this.getWorld().isClient && this.getControllingPassenger() == null){
                this.setSprinting(false);
            }
        }
        if(!this.getWorld().isClient && isHoneyed() && --honeyedTime <= 0){
            this.setHoneyed(false);
            honeyedTime = 0;
        }
        if(this.forcedSit && !this.hasPassengers() && this.isTamed()){
            this.setSitting(true);
        }
        if(this.hasPassengers() && this.isSitting()){
            this.setSitting(false);
        }
        if (!this.getWorld().isClient && this.isAlive() && isTamed() && !this.isBaby() && --this.timeUntilNextFur <= 0) {
            this.dropItem(AMItemRegistry.BEAR_FUR.get());
            this.timeUntilNextFur = this.random.nextInt(24000) + 24000;
        }
        if(snowTimer > 0){
            snowTimer--;
        }
        if (snowTimer == 0 && !this.getWorld().isClient) {
            snowTimer = 200 + random.nextInt(400);
            if(this.isSnowy()){
               if(!permSnow){
                   if (!this.getWorld().isClient || this.getFireTicks() > 0 || this.isInsideWaterOrBubbleColumn() || !isSnowingAt(getWorld(), this.getBlockPos().up())) {
                       this.setSnowy(false);
                   }
               }
            }else{
                if (!this.getWorld().isClient &&  isSnowingAt(getWorld(), this.getBlockPos())) {
                    this.setSnowy(true);
                }
            }
        }
        if(this.isFreddy()){
            this.setStanding(true);
            this.standingTime = 0;
            this.maxStandTime = 40;
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    public static boolean isSnowingAt(World world, BlockPos position) {
        if (!world.isRaining()) {
            return false;
        } else if (!world.isSkyVisible(position)) {
            return false;
        } else if (world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, position).getY() > position.getY()) {
            return false;
        } else {
            return world.getBiome(position).value().getPrecipitation(position) == Biome.Precipitation.SNOW;
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
    public void setSitting(boolean sit) {
        this.dataTracker.set(SITTING, sit);
    }

    @Override
    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(STANDING, false);
        this.dataTracker.startTracking(SITTING, false);
        this.dataTracker.startTracking(HONEYED, false);
        this.dataTracker.startTracking(SNOWY, false);
        this.dataTracker.startTracking(EATING, false);
        this.dataTracker.startTracking(APRIL_FOOLS_MODE, 0);
        this.dataTracker.startTracking(COMMAND, 0);
    }

    public boolean isEating() {
        return this.dataTracker.get(EATING);
    }

    public void setEating(boolean eating) {
        this.dataTracker.set(EATING, eating);
    }

    public boolean isHoneyed() {
        return this.dataTracker.get(HONEYED);
    }

    public void setHoneyed(boolean honeyed) {
        this.dataTracker.set(HONEYED, honeyed);
    }

    public boolean isSnowy() {
        return this.dataTracker.get(SNOWY);
    }

    public void setSnowy(boolean honeyed) {
        this.dataTracker.set(SNOWY, honeyed);
    }

    public boolean isStanding() {
        return this.dataTracker.get(STANDING);
    }

    public void setStanding(boolean standing) {
        this.dataTracker.set(STANDING, standing);
        this.recalcSize = true;
    }

    public int getAprilFoolsFlag() {
        return this.dataTracker.get(APRIL_FOOLS_MODE);
    }

    /*
        0 - default bear mode
        1 - stalking player, normal bear texture
        2 - freddy texture
        3 - freddy texture, blind player
        4 - freddy texture, music box
        5 - freddy texture, attack
     */
    public void setAprilFoolsFlag(int i) {
        this.dataTracker.set(APRIL_FOOLS_MODE, i);
    }

    public int getCommand() {
        return this.dataTracker.get(COMMAND);
    }

    public void setCommand(int command) {
        this.dataTracker.set(COMMAND, command);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity p_241840_2_) {
        return AMEntityRegistry.GRIZZLY_BEAR.get().create(world);
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
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        currentAnimation = animation;
        if (animation == ANIMATION_MAUL) {
            maxStandTime = 21;
            this.setStanding(true);
        }
        if (animation == ANIMATION_SWIPE_R || animation == ANIMATION_SWIPE_L) {
            maxStandTime = 2 + random.nextInt(5);
            this.setStanding(true);
        }
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_MAUL, ANIMATION_SNIFF, ANIMATION_SWIPE_R, ANIMATION_SWIPE_L};
    }

    public boolean shouldMove() {
        return !isSitting();
    }

    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        if (spawnDataIn == null) {
            spawnDataIn = new PassiveEntity.PassiveData(1.0F);
        }

        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.GRIZZLY_FOODSTUFFS);
    }

    @Override
    public void onGetItem(ItemEntity targetEntity) {
        var duplicate = targetEntity.getStack().copy();
        duplicate.setCount(1);
        if (!this.getStackInHand(Hand.MAIN_HAND).isEmpty() && !this.getWorld().isClient) {
            this.dropStack(this.getStackInHand(Hand.MAIN_HAND), 0.0F);
        }
        this.setStackInHand(Hand.MAIN_HAND, duplicate);
        Entity thrower = targetEntity.getOwner();
        if(targetEntity.getStack().isIn(AMTagRegistry.GRIZZLY_TAMEABLES) && thrower != null && this.isHoneyed()){
            salmonThrowerID = thrower.getUuid();
        }else{
            salmonThrowerID = null;
        }
    }

    public boolean isEatingHeldItem() {
        return false;
    }

    public boolean isFreddy() {
        return getAprilFoolsFlag() > 1;
    }

    @Override
    public boolean shouldFollow() {
        return this.getAprilFoolsFlag() == 0 && this.getCommand() == 1;
    }

    @Override
    public EntityView method_48926() {
        return this.getWorld();
    }

    class MyRevengeGoal extends RevengeGoal {
        public MyRevengeGoal() {
            super(EntityGrizzlyBear.this);
        }

        /**
         * Execute a one shot task or start executing a continuous task
         */
        @Override
        public void start() {
            super.start();
            if (EntityGrizzlyBear.this.isBaby()) {
                this.callSameTypeForRevenge();
                this.stop();
            }

        }

        @Override
        protected void setMobEntityTarget(MobEntity mobIn, LivingEntity targetIn) {
            if (mobIn instanceof EntityGrizzlyBear && !mobIn.isBaby()) {
                super.setMobEntityTarget(mobIn, targetIn);
            }
        }
    }

    class MyMeleeAttackGoal extends MeleeAttackGoal {
        public MyMeleeAttackGoal() {
            super(EntityGrizzlyBear.this, 1.25D, true);
        }

        @Override
        protected void attack(LivingEntity enemy, double distToEnemySqr) {
            double d0 = this.getSquaredMaxAttackDistance(enemy);
            if (distToEnemySqr <= d0) {
                if (getAnimation() == NO_ANIMATION || getAnimation() == ANIMATION_SNIFF) {
                    EntityGrizzlyBear.this.setAnimation(random.nextBoolean() ? ANIMATION_MAUL : random.nextBoolean() ? ANIMATION_SWIPE_L : ANIMATION_SWIPE_R);
                }
            }
        }

        @Override
        public void stop() {
            EntityGrizzlyBear.this.setStanding(false);
            super.stop();
        }

        @Override
        protected double getSquaredMaxAttackDistance(LivingEntity attackTarget) {
            return 3.0F + attackTarget.getWidth();
        }
    }

    class AttackPlayerGoal extends ActiveTargetGoal<PlayerEntity> {
        public AttackPlayerGoal() {
            super(EntityGrizzlyBear.this, PlayerEntity.class, 3, true, true, null);
        }

        @Override
        public boolean canStart() {
            if (EntityGrizzlyBear.this.isBaby() || EntityGrizzlyBear.this.getAprilFoolsFlag() >= 1 || EntityGrizzlyBear.this.isHoneyed()) {
                return false;
            } else {
                return super.canStart();
            }
        }

        @Override
        protected double getFollowRange() {
            return 5.0D;
        }
    }

    class MyEscapeDangerGoal extends EscapeDangerGoal {
        public MyEscapeDangerGoal() {
            super(EntityGrizzlyBear.this, 2.0D);
        }

        @Override
        public boolean canStart() {
            return (EntityGrizzlyBear.this.isBaby() || EntityGrizzlyBear.this.isOnFire()) && super.canStart();
        }
    }
}
