package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.EntityAINearestTarget3D;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.github.alexthe666.alexsmobs.packet.MosquitoDismountPacket;
import com.github.alexthe666.alexsmobs.packet.MosquitoMountPlayerPacket;
import com.github.alexthe666.alexsmobs.registry.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.function.Predicate;

public class EntityCrimsonMosquito extends HostileEntity {

    public static final Identifier FULL_LOOT = new Identifier("alexsmobs", "entities/crimson_mosquito_full");
    public static final Identifier FROM_FLY_LOOT = new Identifier("alexsmobs", "entities/crimson_mosquito_fly");
    public static final Identifier FROM_FLY_FULL_LOOT = new Identifier("alexsmobs", "entities/crimson_mosquito_fly_full");
    protected static final EntityDimensions FLIGHT_SIZE = EntityDimensions.fixed(1.2F, 1.8F);
    private static final TrackedData<Boolean> FLYING = DataTracker.registerData(EntityCrimsonMosquito.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SHOOTING = DataTracker.registerData(EntityCrimsonMosquito.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> BLOOD_LEVEL = DataTracker.registerData(EntityCrimsonMosquito.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> SHRINKING = DataTracker.registerData(EntityCrimsonMosquito.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> FROM_FLY = DataTracker.registerData(EntityCrimsonMosquito.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> MOSQUITO_SCALE = DataTracker.registerData(EntityCrimsonMosquito.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> SICK = DataTracker.registerData(EntityCrimsonMosquito.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> LURING_LAVIATHAN = DataTracker.registerData(EntityCrimsonMosquito.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> FLEEING_ENTITY = DataTracker.registerData(EntityCrimsonMosquito.class, TrackedDataHandlerRegistry.INTEGER);
    private static final Predicate<LivingEntity> REPELLENT = (mob) -> mob.hasStatusEffect(AMEffectRegistry.MOSQUITO_REPELLENT.get()) || mob instanceof EntityTriops;
    private static final Predicate<LivingEntity> NO_REPELLENT = (mob) -> !mob.hasStatusEffect(AMEffectRegistry.MOSQUITO_REPELLENT.get());
    public float prevFlyProgress;
    public float flyProgress;
    public float prevShootProgress;
    public float shootProgress;
    public int shootingTicks;
    public int randomWingFlapTick = 0;
    private int flightTicks = 0;
    private int sickTicks = 0;
    private boolean prevFlying = false;
    private int spitCooldown = 0;
    private int loopSoundTick = 0;
    private int drinkTime = 0;
    public float prevMosquitoScale = 1F;
    private int repellentCheckTime = 0;
    private Vec3d fleePos = null;

    public EntityCrimsonMosquito(EntityType<? extends EntityCrimsonMosquito> type, World worldIn) {
        super(type, worldIn);
        this.moveControl = new EntityCrimsonMosquito.MoveHelperController(this);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
        this.setPathfindingPenalty(PathNodeType.LAVA, 0.0F);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, 0.0F);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, 0.0F);
    }

    public boolean hasLuringLaviathan() {
        return this.dataTracker.get(LURING_LAVIATHAN) != -1;
    }

    public void onSpawnFromFly() {
        prevMosquitoScale = 0.2F;
        this.setShrink(false);
        this.setMosquitoScale(0.2F);
        this.setFromFly(true);
        for (int j = 0; j < 4; ++j) {
            this.getWorld().addParticle(ParticleTypes.ENTITY_EFFECT, this.getX() + this.random.nextDouble() / 2.0D, this.getBodyY(0.5D), this.getZ() + this.random.nextDouble() / 2.0D, this.random.nextDouble() * 0.5F + 0.5F, 0, 0.0D);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.MOSQUITO_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.MOSQUITO_DIE.get();
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.crimsonMosquitoSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 0.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F);
    }

    @NotNull
    @Override
    protected Identifier getLootTableId() {
        if (this.getBloodLevel() > 0) {
            return this.isFromFly() ? FROM_FLY_FULL_LOOT : FULL_LOOT;
        }
        return this.isFromFly() ? FROM_FLY_LOOT : super.getLootTableId();
    }

    // FIXME forge
//    @Override
//    public boolean canRiderInteract() {
//        return true;
//    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(2, new FlyTowardsTarget(this));
        this.goalSelector.add(2, new FlyAwayFromTarget(this));
        this.goalSelector.add(3, new EntityCrimsonMosquito.RandomFlyGoal(this));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 32F));
        this.goalSelector.add(5, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this, EntityCrimsonMosquito.class, EntityWarpedMosco.class));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, PlayerEntity.class, 20, true, false, NO_REPELLENT));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, LivingEntity.class, 50, false, true, AMEntityRegistry.buildPredicateFromTag(AMTagRegistry.CRIMSON_MOSQUITO_TARGETS)));
        this.goalSelector.add(3, new FleeEntityGoal<>(this, EntityTriops.class, 16, 1.3D, 1.0D));
    }

    public static boolean canMosquitoSpawn(EntityType<? extends MobEntity> typeIn, ServerWorldAccess worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        var blockpos = pos.down();
        boolean spawnBlock = worldIn.getBlockState(blockpos).isOpaque();
        return reason == SpawnReason.SPAWNER || spawnBlock && worldIn.getBlockState(blockpos).allowsSpawning(worldIn, blockpos, typeIn) && isSpawnDark(worldIn, pos, randomIn) && canMobSpawn(AMEntityRegistry.CRIMSON_MOSQUITO.get(), worldIn, reason, pos, randomIn);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putInt("FlightTicks", this.flightTicks);
        compound.putInt("SickTicks", this.sickTicks);
        compound.putFloat("MosquitoScale", this.getMosquitoScale());
        compound.putBoolean("Flying", this.isFlying());
        compound.putBoolean("Shrinking", this.isShrinking());
        compound.putBoolean("IsFromFly", this.isFromFly());
        compound.putBoolean("Sick", this.isSick());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        this.flightTicks = compound.getInt("FlightTicks");
        this.sickTicks = compound.getInt("SickTicks");
        this.setMosquitoScale(compound.getFloat("MosquitoScale"));
        this.setFlying(compound.getBoolean("Flying"));
        this.setShrink(compound.getBoolean("Shrinking"));
        this.setFromFly(compound.getBoolean("IsFromFly"));
        this.setSick(compound.getBoolean("Sick"));
    }

    private void spit(LivingEntity target) {
        if (this.isSick()) {
            return;
        }
        final var llamaspitentity = new EntityMosquitoSpit(this.getWorld(), this);
        final double d0 = target.getX() - this.getX();
        final double d1 = target.getBodyY(0.3333333333333333D) - llamaspitentity.getY();
        final double d2 = target.getZ() - this.getZ();
        final float f = MathHelper.sqrt((float) (d0 * d0 + d2 * d2)) * 0.2F;
        llamaspitentity.shoot(d0, d1 + (double) f, d2, 1.5F, 10.0F);
        if (!this.isSilent()) {
            this.emitGameEvent(GameEvent.PROJECTILE_SHOOT);
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_LLAMA_SPIT, this.getSoundCategory(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
        }
        if (this.getBloodLevel() > 0) {
            this.setBloodLevel(this.getBloodLevel() - 1);
        }
        this.getWorld().spawnEntity(llamaspitentity);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isOf(DamageTypes.FALL)
                || source.isOf(DamageTypes.DROWN)
                || source.isOf(DamageTypes.IN_WALL)
                || source.isOf(DamageTypes.LAVA)
                || source.isIn(DamageTypeTags.IS_FIRE)
                || super.isInvulnerableTo(source);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.getSource() != null && this.getRootVehicle() == source.getSource().getRootVehicle()) {
            return super.damage(source, amount * 0.333F);
        }
        if (flightTicks < 0) {
            flightTicks = 0;
        }
        return super.damage(source, amount);
    }

    @Override
    public void tickRiding() {
        final Entity entity = this.getVehicle();
        if (this.hasVehicle() && !entity.isAlive()) {
            this.stopRiding();
        } else {
            this.setVelocity(0, 0, 0);
            this.tick();
            if (this.hasVehicle()) {
                final Entity mount = this.getVehicle();
                if (mount instanceof final LivingEntity livingEntity) {
                    this.bodyYaw = livingEntity.bodyYaw;
                    this.setYaw(livingEntity.getYaw());
                    this.headYaw= livingEntity.headYaw;
                    this.prevYaw = livingEntity.headYaw;
                    final float radius = 1F;
                    final float angle = (Maths.STARTING_ANGLE * livingEntity.bodyYaw);
                    final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
                    final double extraZ = radius * MathHelper.cos(angle);
                    this.setPos(mount.getX() + extraX, Math.max(mount.getY() + mount.getStandingEyeHeight() * 0.25F, mount.getY()), mount.getZ() + extraZ);
                    if (!mount.isAlive() || mount instanceof PlayerEntity && ((PlayerEntity) mount).isCreative()) {
                        this.dismountVehicle();
                    }
                    if (!this.getWorld().isClient) {
                        if (drinkTime % 20 == 0 && this.isAlive()) {
                            final boolean mungus = AMConfig.warpedMoscoTransformation && mount instanceof EntityMungus && ((EntityMungus) mount).isWarpedMoscoReady();
                            if (mount.damage(this.getDamageSources().mobAttack(this), mungus ? 7F : 2.0F)) {
                                if (mungus) {
                                    ((EntityMungus) mount).disableExplosion();
                                }
                                final boolean sick = this.isNonMungusWarpedTrigger(mount);
                                if (sick || mungus) {
                                    if (!this.isSick()) {
                                        for (ServerPlayerEntity serverplayerentity : this.getWorld().getNonSpectatingEntities(ServerPlayerEntity.class, this.getBoundingBox().expand(40.0D, 25.0D, 40.0D))) {
                                            AMAdvancementTriggerRegistry.MOSQUITO_SICK.trigger(serverplayerentity);
                                        }
                                    }
                                    this.setSick(true);
                                    this.setFlying(false);
                                    flightTicks = -150 - random.nextInt(200);
                                }
                                this.emitGameEvent(GameEvent.EAT);
                                this.playSound(SoundEvents.ITEM_HONEY_BOTTLE_DRINK, this.getSoundVolume(), this.getSoundPitch());
                                this.setBloodLevel(this.getBloodLevel() + 1);
                                if (this.getBloodLevel() > 3) {
                                    this.dismountVehicle();
                                    AlexsMobs.sendMSGToAll(new MosquitoDismountPacket(this.getId(), mount.getId()));
                                    this.setFlying(false);
                                    this.flightTicks = -15;
                                }
                            }
                        }

                        if (drinkTime > 81) {
                            drinkTime = -20 - random.nextInt(20);
                            this.dismountVehicle();
                            AlexsMobs.sendMSGToAll(new MosquitoDismountPacket(this.getId(), mount.getId()));
                            this.setFlying(false);
                            this.flightTicks = -15;
                        }
                    }
                }

            }
        }

    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FLYING, false);
        this.dataTracker.startTracking(SHOOTING, false);
        this.dataTracker.startTracking(SICK, false);
        this.dataTracker.startTracking(BLOOD_LEVEL, 0);
        this.dataTracker.startTracking(SHRINKING, false);
        this.dataTracker.startTracking(FROM_FLY, false);
        this.dataTracker.startTracking(MOSQUITO_SCALE, 1F);
        this.dataTracker.startTracking(LURING_LAVIATHAN, -1);
        this.dataTracker.startTracking(FLEEING_ENTITY, -1);
    }

    public boolean isFlying() {
        return this.dataTracker.get(FLYING);
    }

    public void setFlying(boolean flying) {
        this.dataTracker.set(FLYING, flying);
    }

    public void setupShooting() {
        this.dataTracker.set(SHOOTING, true);
        this.shootingTicks = 5;
    }

    public int getLuringLaviathan() {
        return this.dataTracker.get(LURING_LAVIATHAN);
    }

    public void setLuringLaviathan(int lure) {
        this.dataTracker.set(LURING_LAVIATHAN, lure);
    }

    public int getFleeingEntityId() {
        return this.dataTracker.get(FLEEING_ENTITY);
    }

    public void setFleeingEntityId(int lure) {
        this.dataTracker.set(FLEEING_ENTITY, lure);
    }

    public int getBloodLevel() {
        return Math.min(this.dataTracker.get(BLOOD_LEVEL), 4);
    }

    public void setBloodLevel(int bloodLevel) {
        this.dataTracker.set(BLOOD_LEVEL, bloodLevel);
    }

    public boolean isShrinking() {
        return this.dataTracker.get(SHRINKING);
    }

    public boolean isFromFly() {
        return this.dataTracker.get(FROM_FLY);
    }

    public void setShrink(boolean shrink) {
        this.dataTracker.set(SHRINKING, shrink);
    }

    public void setFromFly(boolean fromFly) {
        this.dataTracker.set(FROM_FLY, fromFly);
    }

    public float getMosquitoScale() {
        return this.dataTracker.get(MOSQUITO_SCALE);
    }

    public void setMosquitoScale(float scale) {
        this.dataTracker.set(MOSQUITO_SCALE, scale);
    }

    public boolean isSick() {
        return this.dataTracker.get(SICK);
    }

    public void setSick(boolean shrink) {
        this.dataTracker.set(SICK, shrink);
    }

    @Override
    public void tick() {
        super.tick();
        final boolean shooting = dataTracker.get(SHOOTING);
        if (prevFlying != this.isFlying()) {
            this.calculateDimensions();
        }

        if (shooting) {
            if (shootProgress < 5F)
                shootProgress++;
        } else {
            if (shootProgress > 0F)
                shootProgress--;
        }

        if (this.isFlying()) {
            if (flyProgress < 5F)
                flyProgress++;
        } else {
            if (flyProgress > 0F)
                flyProgress--;
        }

        if (!this.getWorld().isClient) {
            if (this.hasVehicle())
                this.setFlying(false);

            if (isFlying()) {
                this.setNoGravity(true);
            } else {
                this.setNoGravity(false);
            }
            LivingEntity target = this.getTarget();
            if (this.getFleeingEntityId() == -1) {
                if (target == null && age - repellentCheckTime > 50) {
                    repellentCheckTime = age;
                    LivingEntity closestRepel = null;
                    for (LivingEntity entity : this.getWorld().getEntitiesByClass(LivingEntity.class, this.getBoundingBox().expand(30), REPELLENT)) {
                        if(closestRepel == null || entity.distanceTo(this) < closestRepel.distanceTo(this)){
                            closestRepel = entity;
                        }
                    }
                    if(closestRepel != null){
                        this.setFleeingEntityId(closestRepel.getId());
                    }
                }
                if (target != null && REPELLENT.test(target) && this.distanceTo(target) < 20) {
                    this.setFleeingEntityId(target.getId());
                }
            } else {
                var fleeing = getWorld().getEntityById(this.getFleeingEntityId());
                if (fleeing instanceof LivingEntity living && REPELLENT.test(living) && this.distanceTo(living) < 20) {
                    this.setTarget(null);
                    this.setAttacker(null);
                    if(this.hasVehicle()){
                        this.stopRiding();
                    }
                    if(fleePos == null || fleePos.distanceTo(this.getPos()) < 3 || random.nextInt(40) == 0){
                        var vec = FuzzyTargeting.findFrom(this, 8, 4, fleeing.getPos());
                        if(vec != null){
                            fleePos = vec;
                        }
                    }else{
                        this.setFlying(true);
                        this.moveControl.moveTo(fleePos.x, fleePos.y + 1, fleePos.z, 1.2F);
                    }
                } else {
                    this.setFleeingEntityId(-1);
                }
            }
            if (hasLuringLaviathan()) {
                this.setTarget(null);
                this.setAttacker(null);
                final Entity entity = this.getWorld().getEntityById(this.getLuringLaviathan());
                if (entity instanceof EntityLaviathan && ((EntityLaviathan) entity).isChilling()) {
                    var vec = ((EntityLaviathan) entity).getLureMosquitoPos();
                    this.setFlying(true);
                    this.lookAtEntity(entity, 10, 10);
                    this.getMoveControl().moveTo(vec.x, vec.y, vec.z, 0.7F);
                } else {
                    this.setLuringLaviathan(-1);
                }
            }
        }
        if (this.flyProgress == 0 && random.nextInt(200) == 0) {
            randomWingFlapTick = 5 + random.nextInt(15);
        }
        if (randomWingFlapTick > 0) {
            randomWingFlapTick--;
        }
        if (!this.getWorld().isClient && isOnGround() && !this.isFlying() && (flightTicks >= 0 && random.nextInt(5) == 0 || this.getTarget() != null)) {
            this.setFlying(true);
            this.setVelocity(this.getVelocity().add((this.random.nextFloat() * 2.0F - 1.0F) * 0.2F, 0.5D, (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F));
            this.setOnGround(false);
            this.velocityDirty = true;
        }
        if (flightTicks < 0) {
            flightTicks++;
        }
        if (!this.getWorld().isClient && isFlying()) {
            flightTicks++;
            if (flightTicks > 200 && (this.getTarget() == null || !this.getTarget().isAlive())) {
                BlockPos above = this.getGroundPosition(this.getBlockPos().up());
                if (getWorld().getFluidState(above).isEmpty() && !getWorld().getBlockState(above).isAir()) {
                    this.getVelocity().add(0, -0.2D, 0);
                    if (this.isOnGround()) {
                        this.setFlying(false);
                        flightTicks = -150 - random.nextInt(200);
                    }
                }
            }
        }
        prevMosquitoScale = this.getMosquitoScale();
        if (isShrinking()) {
            if (this.getMosquitoScale() > 0.4F) {
                this.setMosquitoScale(this.getMosquitoScale() - 0.1F);
            }
        } else {
            if (this.getMosquitoScale() < 1F && !this.isSick()) {
                this.setMosquitoScale(this.getMosquitoScale() + 0.05F);
            }
        }
        if (!this.getWorld().isClient && shootingTicks > 0) {
            shootingTicks--;
            if (shootingTicks == 0) {
                if (this.getTarget() != null && this.getBloodLevel() > 0) {
                    this.spit(this.getTarget());
                }
                this.dataTracker.set(SHOOTING, false);
            }
        }
        if (isFlying()) {
            if (loopSoundTick == 0) {
                this.emitGameEvent(GameEvent.ENTITY_ROAR);
                this.playSound(AMSoundRegistry.MOSQUITO_LOOP.get(), this.getSoundVolume(), this.getSoundPitch());
            }
            loopSoundTick++;
            if (loopSoundTick > 100) {
                loopSoundTick = 0;
            }
        }

        if (hasVehicle()) {
            if (drinkTime < 0)
                drinkTime = 0;

            drinkTime++;
        } else {
            drinkTime = 0;
        }
        prevFlyProgress = flyProgress;
        prevShootProgress = shootProgress;
        prevFlying = this.isFlying();
        if (this.isSick()) {
            sickTicks++;
            if (this.getTarget() != null && !this.hasVehicle()) {
                this.setTarget(null);
            }
            if (sickTicks > 100) {
                this.setShrink(false);
                this.setMosquitoScale(this.getMosquitoScale() + 0.015F);
                if (sickTicks > 160) {
                    var mosco = AMEntityRegistry.WARPED_MOSCO.get().create(getWorld());
                    mosco.copyPositionAndRotation(this);
                    if (!this.getWorld().isClient) {
                        mosco.initialize((ServerWorldAccess) getWorld(), getWorld().getLocalDifficulty(this.getBlockPos()), SpawnReason.CONVERSION, null, null);
                    }

                    if (!this.getWorld().isClient) {
                        this.getWorld().sendEntityStatus(this, (byte) 79);
                        getWorld().spawnEntity(mosco);
                    }
                    this.remove(RemovalReason.DISCARDED);

                }
            }
        }
    }

    @Override
    public void handleStatus(byte id) {
        if (id == 79) {
            for (int i = 0; i < 27; ++i) {
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = this.random.nextGaussian() * 0.02D;
                double d2 = this.random.nextGaussian() * 0.02D;
                this.getWorld().addParticle(ParticleTypes.EXPLOSION, this.getParticleX(1.6D), this.getY() + random.nextFloat() * 3.4F, this.getParticleZ(1.6D), d0, d1, d2);
            }
        } else {
            super.handleStatus(id);
        }

    }

    @Override
    public boolean handleFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @NotNull
    @Override
    public EntityGroup getGroup() {
        return EntityGroup.ARTHROPOD;
    }

    @Override
    public EntityDimensions getDimensions(EntityPose poseIn) {
        return isFlying() ? FLIGHT_SIZE : super.getDimensions(poseIn);
    }

    @Override
    public void travel(Vec3d vec3d) {
        if (this.isOnGround() && !this.isFlying()) {
            if (this.getNavigation().getCurrentPath() != null) {
                this.getNavigation().stop();
            }
            vec3d = Vec3d.ZERO;
        }
        super.travel(vec3d);
    }

    @NotNull
    @Override
    public ActionResult interactMob(PlayerEntity player, @NotNull Hand hand) {
        var itemstack = player.getStackInHand(hand);
        var item = itemstack.getItem();
        ActionResult type = super.interactMob(player, hand);
        if (item == AMItemRegistry.WARPED_MIXTURE.get() && !this.isSick()) {
            this.dropStack(item.getRecipeRemainder().getDefaultStack());
            if (!player.isCreative()) {
                itemstack.decrement(1);
            }
            this.setSick(true);
            return ActionResult.SUCCESS;
        }
        return type;
    }

    public boolean isTargetBlocked(Vec3d target) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        return this.getWorld().raycast(new RaycastContext(Vector3d, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() != HitResult.Type.MISS;
    }

    private BlockPos getGroundPosition(BlockPos radialPos) {
        while (radialPos.getY() > 1 && getWorld().isAir(radialPos)) {
            radialPos = radialPos.down();
        }
        return radialPos;
    }

    static class RandomFlyGoal extends Goal {
        private final EntityCrimsonMosquito parentEntity;
        private BlockPos target = null;

        public RandomFlyGoal(EntityCrimsonMosquito mosquito) {
            this.parentEntity = mosquito;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            var movementcontroller = this.parentEntity.getMoveControl();
            if (!parentEntity.isFlying() || parentEntity.getTarget() != null || parentEntity.hasLuringLaviathan() || parentEntity.getFleeingEntityId() != -1) {
                return false;
            }
            if (!movementcontroller.isMoving() || target == null) {
                target = getBlockInViewMosquito();
                if (target != null) {
                    this.parentEntity.getMoveControl().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 1.0D);
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return target != null && parentEntity.isFlying() && parentEntity.squaredDistanceTo(Vec3d.ofCenter(target)) > 2.4D && parentEntity.getMoveControl().isMoving() && !parentEntity.horizontalCollision;
        }

        @Override
        public void stop() {
            target = null;
        }

        @Override
        public void tick() {
            if (target == null) {
                target = getBlockInViewMosquito();
            }
            if (target != null) {
                this.parentEntity.getMoveControl().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 1.0D);
                if (parentEntity.squaredDistanceTo(Vec3d.ofCenter(target)) < 2.5F) {
                    target = null;
                }
            }
        }

        public BlockPos getBlockInViewMosquito() {
            final float radius = 1 + parentEntity.getRandom().nextInt(5);
            final float neg = parentEntity.getRandom().nextBoolean() ? 1 : -1;
            final float renderYawOffset = parentEntity.bodyYaw;
            final float angle = (Maths.STARTING_ANGLE * renderYawOffset) + 3.15F + (parentEntity.getRandom().nextFloat() * neg);
            final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            final double extraZ = radius * MathHelper.cos(angle);
            final BlockPos radialPos = AMBlockPos.fromCoords(parentEntity.getX() + extraX, parentEntity.getY() + 2, parentEntity.getZ() + extraZ);
            final BlockPos ground = parentEntity.getGroundPosition(radialPos);
            final int up = parentEntity.isSick() ? 2 : 6;
            final BlockPos newPos = ground.up(1 + parentEntity.getRandom().nextInt(up));
            if (!parentEntity.isTargetBlocked(Vec3d.ofCenter(newPos)) && parentEntity.squaredDistanceTo(Vec3d.ofCenter(newPos)) > 6) {
                return newPos;
            }
            return null;
        }

    }

    static class MoveHelperController extends MoveControl {
        private final EntityCrimsonMosquito parentEntity;

        public MoveHelperController(EntityCrimsonMosquito sunbird) {
            super(sunbird);
            this.parentEntity = sunbird;
        }

        @Override
        public void tick() {
            if (speed >= 1 && parentEntity.isSick()) {
                speed = 0.35D;
            }
            if (parentEntity.isFlying()) {
                if (this.state == State.STRAFE) {
                    final var vector3d = new Vec3d(this.targetX - parentEntity.getX(), this.targetY - parentEntity.getY(), this.targetZ - parentEntity.getZ());
                    final double d0 = vector3d.length();
                    parentEntity.setVelocity(parentEntity.getVelocity().add(0, vector3d.multiply(this.speed * 0.05D / d0).y, 0));
                    final float f = (float) this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                    final float f1 = (float) this.speed * f;

                    this.forwardMovement = 1.0F;
                    this.sidewaysMovement = 0.0F;

                    this.entity.setMovementSpeed(f1);
                    this.entity.setForwardSpeed(this.forwardMovement);
                    this.entity.setSidewaysSpeed(this.sidewaysMovement);
                    this.state = MoveControl.State.WAIT;
                } else if (this.state == MoveControl.State.MOVE_TO) {
                    final var vector3d = new Vec3d(this.targetX - parentEntity.getX(), this.targetY - parentEntity.getY(), this.targetZ - parentEntity.getZ());
                    final double d0 = vector3d.length();
                    if (d0 < parentEntity.getBoundingBox().getAverageSideLength()) {
                        this.state = MoveControl.State.WAIT;
                        parentEntity.setVelocity(parentEntity.getVelocity().multiply(0.5D));
                    } else {
                        parentEntity.setVelocity(parentEntity.getVelocity().add(vector3d.multiply(this.speed * 0.05D / d0)));
                        if (parentEntity.getTarget() == null) {
                            final var vector3d1 = parentEntity.getVelocity();
                            parentEntity.setYaw(-((float) MathHelper.atan2(vector3d1.x, vector3d1.z)) * MathHelper.DEGREES_PER_RADIAN);
                            parentEntity.bodyYaw = parentEntity.getYaw();
                        } else {
                            final double d2 = parentEntity.getTarget().getX() - parentEntity.getX();
                            final double d1 = parentEntity.getTarget().getZ() - parentEntity.getZ();
                            parentEntity.setYaw(-((float) MathHelper.atan2(d2, d1)) * MathHelper.DEGREES_PER_RADIAN);
                            parentEntity.bodyYaw = parentEntity.getYaw();
                        }
                    }

                }
            } else {
                state = State.WAIT;
                this.entity.setMovementSpeed(0);
                this.entity.setForwardSpeed(0);
                this.entity.setSidewaysSpeed(0);

            }
        }

        private boolean canReach(Vec3d p_220673_1_, int p_220673_2_) {
            var axisalignedbb = this.parentEntity.getBoundingBox();

            for (int i = 1; i < p_220673_2_; ++i) {
                axisalignedbb = axisalignedbb.offset(p_220673_1_);
                if (!this.parentEntity.getWorld().isSpaceEmpty(this.parentEntity, axisalignedbb)) {
                    return false;
                }
            }

            return true;
        }
    }

    public static class FlyTowardsTarget extends Goal {
        private final EntityCrimsonMosquito parentEntity;

        public FlyTowardsTarget(EntityCrimsonMosquito mosquito) {
            this.parentEntity = mosquito;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (!parentEntity.isFlying() || parentEntity.getBloodLevel() > 0 || parentEntity.drinkTime < 0 || parentEntity.getFleeingEntityId() != -1) {
                return false;
            }
            return !parentEntity.hasVehicle() && parentEntity.getTarget() != null && !isBittenByMosquito(parentEntity.getTarget());
        }

        @Override
        public boolean shouldContinue() {
            return parentEntity.drinkTime >= 0 && parentEntity.getFleeingEntityId() == -1 && parentEntity.getTarget() != null && !isBittenByMosquito(parentEntity.getTarget()) && !parentEntity.horizontalCollision && parentEntity.getBloodLevel() == 0 && parentEntity.isFlying() && parentEntity.getMoveControl().isMoving();
        }

        public boolean isBittenByMosquito(Entity entity) {
            for (Entity e : entity.getPassengerList()) {
                if (e instanceof EntityCrimsonMosquito) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void stop() {
        }

        @Override
        public void tick() {
            if (parentEntity.getTarget() != null) {
                this.parentEntity.getMoveControl().moveTo(parentEntity.getTarget().getX(), parentEntity.getTarget().getY(), parentEntity.getTarget().getZ(), 1.0D);
                if (parentEntity.getBoundingBox().expand(0.3F, 0.3F, 0.3F).intersects(parentEntity.getTarget().getBoundingBox()) && !isBittenByMosquito(parentEntity.getTarget()) && parentEntity.drinkTime == 0) {
                    parentEntity.startRiding(parentEntity.getTarget(), true);
                    if (!parentEntity.getWorld().isClient) {
                        AlexsMobs.sendMSGToAll(new MosquitoMountPlayerPacket(parentEntity.getId(), parentEntity.getTarget().getId()));
                    }
                }
            }
        }
    }

    public static class FlyAwayFromTarget extends Goal {
        private final EntityCrimsonMosquito parentEntity;
        private int spitCooldown = 0;
        private BlockPos shootPos = null;

        public FlyAwayFromTarget(EntityCrimsonMosquito mosquito) {
            this.parentEntity = mosquito;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (!parentEntity.isFlying() || parentEntity.getBloodLevel() <= 0 && parentEntity.drinkTime >= 0 || parentEntity.getFleeingEntityId() != -1) {
                return false;
            }
            if (!parentEntity.hasVehicle() && parentEntity.getTarget() != null) {
                shootPos = getBlockInTargetsViewMosquito(parentEntity.getTarget());
                return true;
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return parentEntity.getTarget() != null && (parentEntity.getBloodLevel() > 0 || parentEntity.drinkTime < 0) && parentEntity.isFlying() && !parentEntity.horizontalCollision;
        }

        @Override
        public void stop() {
            spitCooldown = 20;
        }

        @Override
        public void tick() {
            if (spitCooldown > 0) {
                spitCooldown--;
            }
            if (parentEntity.getTarget() != null) {
                if (shootPos == null) {
                    shootPos = getBlockInTargetsViewMosquito(parentEntity.getTarget());
                } else {
                    this.parentEntity.getMoveControl().moveTo(shootPos.getX() + 0.5D, shootPos.getY() + 0.5D, shootPos.getZ() + 0.5D, 1.0D);
                    this.parentEntity.lookAtEntity(parentEntity.getTarget(), 30.0F, 30.0F);
                    if (parentEntity.squaredDistanceTo(Vec3d.ofCenter(shootPos)) < 2.5F) {
                        if (spitCooldown == 0 && parentEntity.getBloodLevel() > 0) {
                            parentEntity.setupShooting();
                            spitCooldown = 20;
                        }
                        shootPos = null;
                    }
                }
            }

        }

        public BlockPos getBlockInTargetsViewMosquito(LivingEntity target) {
            final float radius = 4 + parentEntity.getRandom().nextInt(5);
            final float angle = (Maths.STARTING_ANGLE * (target.headYaw+ 90F + parentEntity.getRandom().nextInt(180)));
            final double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            final double extraZ = radius * MathHelper.cos(angle);
            final BlockPos ground = AMBlockPos.fromCoords(target.getX() + extraX, target.getY() + 1, target.getZ() + extraZ);
            if (parentEntity.squaredDistanceTo(Vec3d.ofCenter(ground)) > 30) {
                if (!parentEntity.isTargetBlocked(Vec3d.ofCenter(ground)) && parentEntity.squaredDistanceTo(Vec3d.ofCenter(ground)) > 6) {
                    return ground;
                }
            }
            return parentEntity.getBlockPos();
        }
    }

    public boolean isNonMungusWarpedTrigger(Entity entity) {
        final Identifier EntityGroup = Registries.ENTITY_TYPE.getId(entity.getType());
        return EntityGroup != null && !AMConfig.warpedMoscoMobTriggers.isEmpty() && AMConfig.warpedMoscoMobTriggers.contains(EntityGroup.toString());
    }

}
