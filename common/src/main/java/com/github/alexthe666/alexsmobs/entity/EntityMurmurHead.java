package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.entity.ai.EntityAINearestTarget3D;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

public class EntityMurmurHead extends HostileEntity implements Flutterer {

    private static final TrackedData<Optional<UUID>> BODY_UUID = DataTracker.registerData(EntityMurmurHead.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> BODY_ID = DataTracker.registerData(EntityMurmurHead.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> PULLED_IN = DataTracker.registerData(EntityMurmurHead.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> ANGRY = DataTracker.registerData(EntityMurmurHead.class, TrackedDataHandlerRegistry.BOOLEAN);
    public double prevXHair;
    public double prevYHair;
    public double prevZHair;
    public double xHair;
    public double yHair;
    public double zHair;
    public float angerProgress;
    public float prevAngerProgress;
    private boolean prevLaunched = false;

    public EntityMurmurHead(EntityType type, World level) {
        super(type, level);
        this.moveControl = new MoveController();
    }

    protected EntityMurmurHead(EntityMurmur parent) {
        this(AMEntityRegistry.MURMUR_HEAD.get(), parent.getWorld());
        this.setBodyId(parent.getUuid());
        this.doSpawnPositioning(parent);
    }

    @Override
    protected EntityNavigation createNavigation(World level) {
        var flyingpathnavigation = new BirdNavigation(this, getWorld());
        flyingpathnavigation.setCanPathThroughDoors(false);
        flyingpathnavigation.setCanSwim(true);
        flyingpathnavigation.setCanEnterOpenDoors(true);
        return flyingpathnavigation;
    }

    @Override
    public int getXpToDrop() {
        return 0;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new AttackGoal());
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.targetSelector.add(1, (new RevengeGoal(this)));
        this.targetSelector.add(2, new EntityAINearestTarget3D<>(this, PlayerEntity.class, 10, false, true, null));
        this.targetSelector.add(3, new EntityAINearestTarget3D<>(this, MerchantEntity.class, 30, false, true, null));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(BODY_UUID, Optional.empty());
        this.dataTracker.startTracking(BODY_ID, -1);
        this.dataTracker.startTracking(PULLED_IN, true);
        this.dataTracker.startTracking(ANGRY, false);
    }

    private void doSpawnPositioning(EntityMurmur parent){
        this.setPosition(parent.getNeckBottom(1.0F).add(0, 0.5F, 0));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F);
    }

    @Override
    public boolean handleFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void fall(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }

    public boolean isPulledIn(){
        return this.dataTracker.get(PULLED_IN);
    }

    public void setPulledIn(boolean pulledIn){
        this.dataTracker.set(PULLED_IN, pulledIn);
    }

    public boolean isAngry(){
        return this.dataTracker.get(ANGRY) || !this.isAlive();
    }

    public void setAngry(boolean angry){
        this.dataTracker.set(ANGRY, angry);
    }

    public Vec3d getNeckTop(float partialTick){
        double d0 = MathHelper.lerp(partialTick, this.prevX, this.getX());
        double d1 = MathHelper.lerp(partialTick, this.prevY, this.getY());
        double d2 = MathHelper.lerp(partialTick, this.prevZ, this.getZ());
        double bounce = 0;
        var body = this.getBody();
        if(body instanceof EntityMurmur){
            bounce = ((EntityMurmur)body).calculateWalkBounce(partialTick);
        }
        return new Vec3d(d0, d1 + bounce, d2);
    }

    public Vec3d getNeckBottom(float partialTick){
        var body = this.getBody();
        var top = this.getNeckTop(partialTick);
        if(body instanceof EntityMurmur murmur){
            var bodyBase = murmur.getNeckBottom(partialTick);
            final double sub = top.subtract(bodyBase).horizontalLength();
            return sub <= 0.06 ? new Vec3d(top.x, bodyBase.y, top.z) : bodyBase;
        }
        return top.add(0, -0.5F, 0);
    }

    public boolean hasNeckBottom(){
        return true;
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.UNDEAD;
    }

    @Nullable
    public UUID getBodyId() {
        return this.dataTracker.get(BODY_UUID).orElse(null);
    }

    public void setBodyId(@Nullable UUID uniqueId) {
        this.dataTracker.set(BODY_UUID, Optional.ofNullable(uniqueId));
    }

    public Entity getBody() {
        if (!this.getWorld().isClient) {
            final UUID id = getBodyId();
            return id == null ? null : ((ServerWorld) getWorld()).getEntity(id);
        }else{
            int id = this.dataTracker.get(BODY_ID);
            return id == -1 ? null : getWorld().getEntityById(id);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        if (compound.containsUuid("BodyUUID")) {
            this.setBodyId(compound.getUuid("BodyUUID"));
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        if (this.getBodyId() != null) {
            compound.putUuid("BodyUUID", this.getBodyId());
        }
    }

    @Override
    protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.35F;
    }

    @Override
    public void tick(){
        super.tick();
        this.headYaw= MathHelper.clamp(this.headYaw, this.bodyYaw - 70, this.bodyYaw + 70);
        this.prevAngerProgress = angerProgress;
        if(this.isAngry() && angerProgress < 5F) {
            angerProgress++;
        }
        if(!this.isAngry() && angerProgress > 0F){
            angerProgress--;
        }
        moveHair();
        Entity body = getBody();
        if(!this.getWorld().isClient) {
            if (body instanceof EntityMurmur murmur) {
                this.dataTracker.set(BODY_ID, body.getId());
                if(this.isPulledIn() && murmur.isAlive()){
                    var base = murmur.getNeckBottom(1.0F).add(0, 0.55F, 0);
                    var vec3 = base.subtract(this.getPos());
                    if(vec3.length() < 1){
                        this.setPos(base.x, base.y, base.z);
                        this.noClip = false;
                    }else{
                        this.noClip = true;
                        vec3 = base.subtract(this.getPos()).normalize();
                        float f = this.getTarget() != null && this.getTarget().isAlive() ? 0.3F : 0.15F;
                        this.setVelocity(vec3.multiply(f));
                    }
                    this.setYaw(murmur.getYaw());
                    this.bodyYaw = murmur.getYaw();
                }else{
                    this.noClip = false;
                }
                LivingEntity headTarget = this.getTarget();
                LivingEntity bodyTarget = murmur.getTarget();
                if(headTarget != null && headTarget.isAlive()){
                    if(murmur.canTarget(headTarget)){
                        murmur.setTarget(headTarget);
                    }else{
                        this.setTarget(null);
                        murmur.setTarget(null);
                    }
                }else if(bodyTarget != null && bodyTarget.isAlive() && this.canTarget(bodyTarget)){
                    this.setTarget(bodyTarget);
                }
                if (body.isRemoved()) {
                    this.remove(RemovalReason.DISCARDED);
                }
            }
            if(body == null && this.age > 20){
                this.remove(RemovalReason.DISCARDED);
            }
        }else{
            if (body instanceof EntityMurmur murmur) {
                if (murmur.hurtTime > 0 || murmur.deathTime > 0) {
                    this.hurtTime = murmur.hurtTime;
                    this.deathTime = murmur.deathTime;
                }
            }
        }
        if(prevLaunched && !this.isPulledIn()){
            this.playSound(AMSoundRegistry.MURMUR_NECK.get(), 3F * this.getSoundVolume(), this.getSoundPitch());
        }
        prevLaunched = this.isPulledIn();
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float damage) {
        Entity body = this.getBody();
        if(isInvulnerableTo(source)){
            return false;
        }
        if(body != null && body.damage(source, 0.5F * damage)){
            return true;
        }
        return super.damage(source, damage);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return super.isInvulnerableTo(damageSource) || damageSource.isOf(DamageTypes.IN_WALL);
    }

    private void moveHair() {
        this.prevXHair = this.xHair;
        this.prevYHair = this.yHair;
        this.prevZHair = this.zHair;
        double d0 = this.getX() - this.xHair;
        double d1 = this.getY() - this.yHair;
        double d2 = this.getZ() - this.zHair;

        if (d0 > 10.0D) {
            this.xHair = this.getX();
            this.prevXHair = this.xHair;
        }

        if (d2 > 10.0D) {
            this.zHair = this.getZ();
            this.prevZHair = this.zHair;
        }

        if (d1 > 10.0D) {
            this.yHair = this.getY();
            this.prevYHair = this.yHair;
        }

        if (d0 < -10.0D) {
            this.xHair = this.getX();
            this.prevXHair = this.xHair;
        }

        if (d2 < -10.0D) {
            this.zHair = this.getZ();
            this.prevZHair = this.zHair;
        }

        if (d1 < -10.0D) {
            this.yHair = this.getY();
            this.prevYHair = this.yHair;
        }

        this.xHair += d0 * 0.25D;
        this.zHair += d2 * 0.25D;
        this.yHair += d1 * 0.25D;
    }

    @Override
    public boolean isTeammate(Entity entity) {
        return this.getBodyId() != null && entity.getUuid().equals(this.getBodyId()) || super.isTeammate(entity);
    }

    @Override
    public void playAmbientSound() {
        if(this.isPulledIn() && !this.isAngry()){
            super.playAmbientSound();
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.MURMUR_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return this.getBody() == null ? AMSoundRegistry.MURMUR_HURT.get() : null;
    }

    protected SoundEvent getDeathSound() {
        return this.getBody() == null ? AMSoundRegistry.MURMUR_HURT.get() : null;
    }

    @Override
    public boolean isInAir() {
        return true;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {
    }

    class MoveController extends MoveControl {
        private final MobEntity parentEntity;

        public MoveController() {
            super(EntityMurmurHead.this);
            this.parentEntity = EntityMurmurHead.this;
        }

        public void tick() {
            if(EntityMurmurHead.this.isPulledIn()){
                return;
            }
            float angle = (Maths.STARTING_ANGLE * (parentEntity.bodyYaw + 90));
            float radius = (float) Math.sin(parentEntity.age * 0.2F) * 2;
            double extraX = radius * MathHelper.sin(MathHelper.PI + angle);
            double extraY = radius * -Math.cos(angle - Math.PI / 2);
            double extraZ = radius * MathHelper.cos(angle);
            var strafPlus = new Vec3d(extraX, extraY, extraZ);
            if (this.state == MoveControl.State.MOVE_TO) {
                var vector3d = new Vec3d(this.targetX - parentEntity.getX(), this.targetY - parentEntity.getY(), this.targetZ - parentEntity.getZ());
                double d0 = vector3d.length();
                double width = parentEntity.getBoundingBox().getAverageSideLength();
                var shimmy = Vec3d.ZERO;
                LivingEntity attackTarget = parentEntity.getTarget();
                if (attackTarget != null) {
                    if (parentEntity.horizontalCollision) {
                        shimmy = new Vec3d(0, 0.005, 0);
                    }
                }

                Vec3d vector3d1 = vector3d.multiply(this.speed * 0.05D / d0);
                parentEntity.setVelocity(parentEntity.getVelocity().add(vector3d1.add(strafPlus.multiply(0.003D * Math.min(d0, 100)).add(shimmy))));

                if (attackTarget == null) {
                    if(d0 >= width){
                        var deltaMovement = parentEntity.getVelocity();
                        parentEntity.setYaw(-((float) MathHelper.atan2(deltaMovement.x, deltaMovement.z)) * MathHelper.DEGREES_PER_RADIAN);
                        parentEntity.bodyYaw = parentEntity.getYaw();
                    }
                } else {
                    double d2 = attackTarget.getX() - parentEntity.getX();
                    double d1 = attackTarget.getZ() - parentEntity.getZ();
                    parentEntity.setYaw(-((float) MathHelper.atan2(d2, d1)) * MathHelper.DEGREES_PER_RADIAN);
                    parentEntity.bodyYaw = parentEntity.getYaw();
                }
            } else if (this.state == MoveControl.State.WAIT) {
                parentEntity.setVelocity(parentEntity.getVelocity().add(strafPlus.multiply(0.003D)));
            }
        }
    }

    private class AttackGoal extends Goal {

        private int time;
        private int biteCooldown = 0;
        private Vec3d emergeFrom = Vec3d.ZERO;

        public AttackGoal() {
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return EntityMurmurHead.this.getTarget() != null && EntityMurmurHead.this.getTarget().isAlive();
        }

        public void start(){
            time = 0;
            biteCooldown = 0;
            EntityMurmurHead.this.setPulledIn(false);
        }

        public void stop(){
            time = 0;
            EntityMurmurHead.this.setPulledIn(true);
            EntityMurmurHead.this.setAngry(false);
        }

        public void tick(){
            LivingEntity target = EntityMurmurHead.this.getTarget();
            Entity body = EntityMurmurHead.this.getBody();
            if(target != null){
                double dist = Math.sqrt(EntityMurmurHead.this.squaredDistanceTo(target.getEyePos()));
                double bodyDist = body != null ? body.distanceTo(target) : 0.0;
                if(bodyDist > 16 && time > 30){
                    if(body instanceof EntityMurmur){
                        EntityMurmur murmur = (EntityMurmur) body;
                        murmur.setTarget(target);
                        murmur.getNavigation().startMovingTo(target, 1.35D);
                    }
                }
                if(bodyDist > 64){
                    EntityMurmurHead.this.setPulledIn(true);
                }else if(biteCooldown == 0){
                    EntityMurmurHead.this.setPulledIn(false);
                    var moveTo = target.getEyePos();
                    if(time > 30){
                        if(!EntityMurmurHead.this.isAngry()){
                            EntityMurmurHead.this.playSound(AMSoundRegistry.MURMUR_ANGER.get(), 1.5F * EntityMurmurHead.this.getSoundVolume(), EntityMurmurHead.this.getSoundPitch());
                            EntityMurmurHead.this.emitGameEvent(GameEvent.ENTITY_ROAR);
                        }
                        EntityMurmurHead.this.setAngry(true);
                        EntityMurmurHead.this.getNavigation().startMovingTo(moveTo.x, moveTo.y, moveTo.z, 1.3D);
                    }else{
                        if(time == 0){
                            emergeFrom = EntityMurmurHead.this.getNeckTop(1.0F).add(0, 0.5F, 0);
                        }
                        boolean clockwise = false;
                        float circleDistance = 2.5F;
                        float circlingTime = 30 * time;
                        float angle = (Maths.STARTING_ANGLE * (clockwise ? -circlingTime : circlingTime));
                        double extraX = circleDistance * MathHelper.sin(MathHelper.PI + angle);
                        double extraZ = circleDistance * MathHelper.cos(angle);
                        double y = Math.max(emergeFrom.y + 2, target.getEyeY());
                        Vec3d vec3 = new Vec3d(emergeFrom.x + extraX, y, emergeFrom.z + extraZ);
                        EntityMurmurHead.this.getNavigation().startMovingTo(vec3.x, vec3.y, vec3.z, 0.7D);
                    }
                    EntityMurmurHead.this.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, moveTo);
                    if(dist < 1.5F && EntityMurmurHead.this.canSee(target)){
                        EntityMurmurHead.this.playSound(AMSoundRegistry.MURMUR_ATTACK.get(), EntityMurmurHead.this.getSoundVolume(), EntityMurmurHead.this.getSoundPitch());
                        biteCooldown = 5 + EntityMurmurHead.this.getRandom().nextInt(15);
                        target.damage(EntityMurmurHead.this.getDamageSources().mobAttack(EntityMurmurHead.this), 5.0F);
                    }
                }else{
                    EntityMurmurHead.this.setPulledIn(true);
                    EntityMurmurHead.this.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, target.getEyePos());
                    EntityMurmurHead.this.setAngry(false);
                }
                time++;
            }
            if(biteCooldown > 0){
                biteCooldown--;
            }
        }
    }
}
