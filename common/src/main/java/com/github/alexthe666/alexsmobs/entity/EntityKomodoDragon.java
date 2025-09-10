package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public class EntityKomodoDragon extends TameableEntity implements ITargetsDroppedItems, IFollower {

    private static final Ingredient TEMPTATION_ITEMS = Ingredient.fromTag(AMTagRegistry.KOMODO_DRAGON_TAMEABLES);
    public int slaughterCooldown = 0;
    public int timeUntilSpit = this.random.nextInt(12000) + 24000;
    public float nextJostleAngleFromServer;
    private int riderAttackCooldown = 0;
    public static final Predicate<LivingEntity> HURT_OR_BABY = (p_213616_0_) -> p_213616_0_.isBaby() || p_213616_0_.getHealth() <= 0.7F * p_213616_0_.getMaxHealth();
    protected static final EntityDimensions JOSTLING_SIZE = EntityDimensions.changing(1.35F, 1.85F);
    private static final TrackedData<Integer> COMMAND = DataTracker.registerData(EntityKomodoDragon.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> JOSTLING = DataTracker.registerData(EntityKomodoDragon.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> JOSTLE_ANGLE = DataTracker.registerData(EntityKomodoDragon.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Optional<UUID>> JOSTLER_UUID = DataTracker.registerData(EntityKomodoDragon.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Boolean> SADDLED = DataTracker.registerData(EntityKomodoDragon.class, TrackedDataHandlerRegistry.BOOLEAN);
    public float prevJostleAngle;
    public float prevJostleProgress;
    public float jostleProgress;
    public float prevSitProgress;
    public float sitProgress;
    public boolean jostleDirection;
    public int jostleTimer = 0;
    public boolean instantlyTriggerJostleAI = false;
    public int jostleCooldown = 100 + random.nextInt(40);
    private boolean hasJostlingSize;

    public EntityKomodoDragon(EntityType<EntityKomodoDragon> type, World worldIn) {
        super(type, worldIn);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(COMMAND, 0);
        this.dataTracker.startTracking(JOSTLING, false);
        this.dataTracker.startTracking(SADDLED, false);
        this.dataTracker.startTracking(JOSTLE_ANGLE, 0F);
        this.dataTracker.startTracking(JOSTLER_UUID, Optional.empty());
    }

    public int getCommand() {
        return this.dataTracker.get(COMMAND);
    }

    public void setCommand(int command) {
        this.dataTracker.set(COMMAND, command);
    }

    public static <T extends MobEntity> boolean canKomodoDragonSpawn(EntityType<? extends AnimalEntity> animal, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random random) {
        boolean spawnBlock = worldIn.getBlockState(pos.down()).isIn(AMTagRegistry.KOMODO_DRAGON_SPAWNS);
        return spawnBlock && worldIn.getBaseLightLevel(pos, 0) > 8;
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.komodoDragonSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new SitGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 2D, false));
        this.goalSelector.add(3, new TameableAIFollowOwner(this, 1.2D, 6.0F, 3.0F, false));
        this.goalSelector.add(4, new KomodoDragonAIJostle(this));
        this.goalSelector.add(5, new TameableAITempt(this, 1.1D, TEMPTATION_ITEMS, false));
        this.goalSelector.add(5, new AnimalAIFleeAdult(this, 1.25D, 32));
        this.goalSelector.add(6, new KomodoDragonAIBreed(this, 1.0D));
        this.goalSelector.add(6, new WanderAroundGoal(this, 1D, 50));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, new RevengeGoal(this));
        this.targetSelector.add(4, new CreatureAITargetItems<>(this, false));
        this.targetSelector.add(6, new ActiveTargetGoal<>(this, EntityKomodoDragon.class, 50, true, false, HURT_OR_BABY));
        this.targetSelector.add(7, new ActiveTargetGoal<>(this, PlayerEntity.class, 150, true, true, null));
        this.targetSelector.add(8, new EntityAINearestTarget3D<>(this, LivingEntity.class, 180, false, true, AMEntityRegistry.buildPredicateFromTag(AMTagRegistry.KOMODO_DRAGON_TARGETS)));
    }

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
        return (float)(this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * 2);
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
        return AMSoundRegistry.KOMODO_DRAGON_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.KOMODO_DRAGON_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.KOMODO_DRAGON_HURT.get();
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        if (compound.contains("SpitTime")) {
            this.timeUntilSpit = compound.getInt("SpitTime");
        }
        this.setCommand(compound.getInt("KomodoCommand"));
        this.jostleCooldown = compound.getInt("JostlingCooldown");
        this.setSaddled(compound.getBoolean("Saddle"));

    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putInt("SpitTime", this.timeUntilSpit);
        compound.putInt("KomodoCommand", this.getCommand());
        compound.putBoolean("Saddle", this.isSaddled());
        compound.putInt("JostlingCooldown", this.jostleCooldown);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return isTamed() && stack.isIn(AMTagRegistry.KOMODO_DRAGON_BREEDABLES);
    }

    @Override
    public void tick() {
        prevJostleAngle = this.getJostleAngle();
        super.tick();
        prevJostleProgress = jostleProgress;
        prevSitProgress = sitProgress;

        if(slaughterCooldown > 0){
            slaughterCooldown--;
        }
        if (!this.getWorld().isClient && this.isAlive() && !this.isBaby() && --this.timeUntilSpit <= 0) {
            this.dropItem(AMItemRegistry.KOMODO_SPIT.get());
            this.timeUntilSpit = this.random.nextInt(12000) + 24000;
        }
        if(riderAttackCooldown > 0){
            riderAttackCooldown--;
        }
        if(this.getControllingPassenger() != null && this.getControllingPassenger() instanceof PlayerEntity rider){
            if(rider.getAttacking() != null && this.distanceTo(rider.getAttacking()) < this.getWidth() + 3F && !this.isTeammate(rider.getAttacking())){
                UUID preyUUID = rider.getAttacking().getUuid();
                if (!this.getUuid().equals(preyUUID) && riderAttackCooldown == 0) {
                    tryAttack(rider.getAttacking());
                    riderAttackCooldown = 20;
                }
            }
        }
        if (!hasJostlingSize && isJostling()){
            calculateDimensions();
            hasJostlingSize = true;
        }
        if (hasJostlingSize && !isJostling()){
            calculateDimensions();
            hasJostlingSize = false;
        }

        if (this.isJostling()) {
            if (jostleProgress < 5F)
                jostleProgress++;
        } else {
            if (jostleProgress > 0F)
                jostleProgress--;
        }

        if (this.isSitting()) {
            if (sitProgress < 5F)
                sitProgress++;
        } else {
            if (sitProgress > 0F)
                sitProgress--;
        }

        if(this.getCommand() == 2 && !this.hasPassengers()){
            this.setSitting(true);
        }else{
            this.setSitting(false);
        }

        if (jostleCooldown > 0) {
            jostleCooldown--;
        }

        if(!this.getWorld().isClient){
            if(this.getJostleAngle() < nextJostleAngleFromServer){
                this.setJostleAngle(this.getJostleAngle() + 1);

            }
            if(this.getJostleAngle() > nextJostleAngleFromServer) {
                this.setJostleAngle(this.getJostleAngle() - 1);
            }
        }
    }

    @Override
    public EntityDimensions getDimensions(EntityPose poseIn) {
        return isJostling() && !isBaby() ? JOSTLING_SIZE.scaled(this.getScaleFactor()) : super.getDimensions(poseIn);
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
    public boolean tryAttack(Entity entityIn) {
        if (super.tryAttack(entityIn)) {
            if (entityIn instanceof LivingEntity) {
                int i = 5;
                if (this.getWorld().getDifficulty() == Difficulty.NORMAL) {
                    i = 10;
                } else if (this.getWorld().getDifficulty() == Difficulty.HARD) {
                    i = 20;
                }
                ((LivingEntity)entityIn).addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, i * 20, 0));
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean canHaveStatusEffect(StatusEffectInstance potioneffectIn) {
        if (potioneffectIn.getEffectType() == StatusEffects.POISON) {
            return false;
        }
        return super.canHaveStatusEffect(potioneffectIn);
    }

    @Nullable
    public LivingEntity getControllingPassenger() {
        for (Entity passenger : this.getPassengerList()) {
            if (passenger instanceof PlayerEntity) {
                return (PlayerEntity) passenger;
            }
        }
        return null;
    }

    @Override
    public void updatePassengerPosition(Entity passenger, Entity.PositionUpdater moveFunc) {
        if (this.hasPassenger(passenger)) {
            float radius = 0;
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
        return (double)this.getHeight() - 0.2D + (double)(0.12F * MathHelper.cos(f1 * 0.7F) * 0.7F * f);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        ActionResult type = super.interactMob(player, hand);

        if(itemstack.isIn(AMTagRegistry.KOMODO_DRAGON_TAMEABLES)){
            if(!isTamed()){
                int size = itemstack.getCount();
                int tameAmount = 58 + random.nextInt(16);
                if(size > tameAmount){
                    this.setOwner(player);
                }
                itemstack.decrement(size);
                return ActionResult.SUCCESS;
            }else if(this.getHealth() <= this.getMaxHealth()){
                eat(player, hand, itemstack);
                this.heal(10);
                return ActionResult.SUCCESS;
            }
        }
        var actionResult = itemstack.useOnEntity(player, this, hand);
        if (actionResult != ActionResult.SUCCESS && type != ActionResult.SUCCESS && isTamed() && isOwner(player)){
            if(isBreedingItem(itemstack)){
                this.setLoveTicks(600);
                this.eat(player, hand, itemstack);
                return ActionResult.SUCCESS;
            }else if(itemstack.getItem() == Items.SADDLE && !this.isSaddled()){
                this.eat(player, hand, itemstack);
                this.setSaddled(true);
                return ActionResult.SUCCESS;
                //FIXME
//            }else if(itemstack.is(Tags.Items.SHEARS) && this.isSaddled()){
//                this.setSaddled(false);
//                this.spawnAtLocation(Items.SADDLE);
//                return ActionResult.SUCCESS;
            }else{
                if(!player.isSneaking() && !this.isBaby() && this.isSaddled()){
                    player.startRiding(this);
                }else{
                    this.setCommand((this.getCommand() + 1) % 3);

                    if (this.getCommand() == 3) {
                        this.setCommand(0);
                    }
                    player.sendMessage(Text.translatable("entity.alexsmobs.all.command_" + this.getCommand(), this.getName()), true);
                    boolean sit = this.getCommand() == 2;
                    this.setSitting(sit);
                }
                return ActionResult.SUCCESS;
            }
        }
        return type;
    }

    @Override
    protected float getBaseMovementSpeedMultiplier() {
        return 0.98F;
    }

    @Override
    public void setTarget(@Nullable LivingEntity entitylivingbaseIn) {
        if(!this.isBaby() || slaughterCooldown > 0){
            super.setTarget(entitylivingbaseIn);
        }
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30D)
                .add(EntityAttributes.GENERIC_ARMOR, 0.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.KOMODO_DRAGON.get().create(world);
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.KOMODO_DRAGON_TAMEABLES) || stack.getItem().getFoodComponent() != null && stack.getItem().getFoodComponent().isMeat();
    }

    public boolean isSaddled() {
        return this.dataTracker.get(SADDLED);
    }

    public void setSaddled(boolean saddled) {
        this.dataTracker.set(SADDLED, saddled);
    }

    public boolean isJostling() {
        return this.dataTracker.get(JOSTLING);
    }

    public void setJostling(boolean jostle) {
        this.dataTracker.set(JOSTLING, jostle);
    }

    public float getJostleAngle() {
        return this.dataTracker.get(JOSTLE_ANGLE);
    }

    public void setJostleAngle(float scale) {
        this.dataTracker.set(JOSTLE_ANGLE, scale);
    }

    @Nullable
    public UUID getJostlingPartnerUUID() {
        return this.dataTracker.get(JOSTLER_UUID).orElse(null);
    }

    public void setJostlingPartnerUUID(@Nullable UUID uniqueId) {
        this.dataTracker.set(JOSTLER_UUID, Optional.ofNullable(uniqueId));
    }

    @Nullable
    public Entity getJostlingPartner() {
        UUID id = getJostlingPartnerUUID();
        if (id != null && !this.getWorld().isClient) {
            return ((ServerWorld) getWorld()).getEntity(id);
        }
        return null;
    }

    public void setJostlingPartner(@Nullable Entity jostlingPartner) {
        if (jostlingPartner == null) {
            this.setJostlingPartnerUUID(null);
        } else {
            this.setJostlingPartnerUUID(jostlingPartner.getUuid());
        }
    }

    public void pushBackJostling(EntityKomodoDragon entityMoose, float strength) {
        applyKnockbackFromMoose(strength, entityMoose.getX() - this.getX(), entityMoose.getZ() - this.getZ());
    }

    private void applyKnockbackFromMoose(float strength, double ratioX, double ratioZ) {
        //FIXME
//        net.minecraftforge.event.entity.living.LivingKnockBackEvent event = net.minecraftforge.common.ForgeHooks.onLivingKnockBack(this, strength, ratioX, ratioZ);
//        if (event.isCanceled()) return;
//        strength = event.getStrength();
//        ratioX = event.getRatioX();
//        ratioZ = event.getRatioZ();
//        if (!(strength <= 0.0F)) {
//            this.velocityDirty = true;
//            Vec3d vector3d = this.getVelocity();
//            Vec3d vector3d1 = (new Vec3d(ratioX, 0.0D, ratioZ)).normalize().scale(strength);
//            this.setVelocity(vector3d.x / 2.0D - vector3d1.x, 0.3F, vector3d.z / 2.0D - vector3d1.z);
//        }
    }

    public boolean canJostleWith(EntityKomodoDragon moose) {
        return !moose.isSitting() && !moose.hasPassengers() && !moose.isBaby() && moose.getJostlingPartnerUUID() == null && moose.jostleCooldown == 0;
    }

    public void playJostleSound() {
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if (this.isSaddled()) {
            if (!this.getWorld().isClient) {
                this.dropItem(Items.SADDLE);
            }
        }
        this.setSaddled(false);
    }

    @Override
    public void onGetItem(ItemEntity e) {
        this.heal(10);
    }

    @Override
    public boolean shouldFollow() {
        return this.getCommand() == 1;
    }

    public boolean isMaid() {
        String s = Formatting.strip(this.getName().getString());
        return s != null && (s.toLowerCase().contains("maid") || s.toLowerCase().contains("coda"));

    }

    @Override
    public EntityView method_48926() {
        return getWorld();
    }
}
