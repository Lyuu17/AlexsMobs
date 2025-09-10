package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class EntityVoidWormShot extends Entity {
    private UUID ownerUUID;
    private int ownerNetworkId;
    private boolean leftOwner;
    private static final TrackedData<Float> STOP_HOMING_PROGRESS = DataTracker.registerData(EntityVoidWormShot.class, TrackedDataHandlerRegistry.FLOAT);
    public float prevStopHomingProgress = 0.0F;
    public static final float HOME_FOR = 40F;

    public EntityVoidWormShot(EntityType<EntityVoidWormShot> p_i50162_1_, World p_i50162_2_) {
        super(p_i50162_1_, p_i50162_2_);
    }

    public EntityVoidWormShot(World worldIn, EntityVoidWorm p_i47273_2_) {
        this(AMEntityRegistry.VOID_WORM_SHOT.get(), worldIn);
        this.setShooter(p_i47273_2_);
        this.setPos(p_i47273_2_.getX() - (double) (p_i47273_2_.getWidth() + 1.0F) * 0.35D * (double) MathHelper.sin(p_i47273_2_.bodyYaw * MathHelper.RADIANS_PER_DEGREE), p_i47273_2_.getY() + (double) 1F, p_i47273_2_.getZ() + (double) (p_i47273_2_.getWidth() + 1.0F) * 0.35D * (double) MathHelper.cos(p_i47273_2_.bodyYaw * MathHelper.RADIANS_PER_DEGREE));
    }

    public EntityVoidWormShot(World worldIn, LivingEntity p_i47273_2_, boolean right) {
        this(AMEntityRegistry.VOID_WORM_SHOT.get(), worldIn);
        this.setShooter(p_i47273_2_);
        float rot = p_i47273_2_.headYaw+ (right ? 60 : -60);
        this.setPos(p_i47273_2_.getX() - (double) (p_i47273_2_.getWidth()) * 0.9F * (double) MathHelper.sin(rot * MathHelper.RADIANS_PER_DEGREE), p_i47273_2_.getY() + (double) 1F, p_i47273_2_.getZ() + (double) (p_i47273_2_.getWidth()) * 0.9D * (double) MathHelper.cos(rot * MathHelper.RADIANS_PER_DEGREE));
    }

    protected static float lerpRotation(float p_234614_0_, float p_234614_1_) {
        while (p_234614_1_ - p_234614_0_ < -180.0F) {
            p_234614_0_ -= 360.0F;
        }

        while (p_234614_1_ - p_234614_0_ >= 180.0F) {
            p_234614_0_ += 360.0F;
        }

        return MathHelper.lerp(0.2F, p_234614_0_, p_234614_1_);
    }

    //FIXME
//    @Override
//    public Packet<ClientPlayPacketListener> createSpawnPacket() {
//        return (Packet<ClientPlayPacketListener>) NetworkHooks.getEntitySpawningPacket(this);
//    }

    @Override
    public void tick() {
        this.prevStopHomingProgress = this.getStopHomingProgress();
        if (!this.leftOwner) {
            this.leftOwner = this.checkLeftOwner();
        }
        if (this.age > 400) {
            this.remove(RemovalReason.DISCARDED);
        }
        if (this.age > 40) {
            Entity entity = this.getShooter();
            float stopHomingProgress = this.getStopHomingProgress();
            if(stopHomingProgress < HOME_FOR){
                stopHomingProgress += 1.0F;
                this.setStopHomingProgress(stopHomingProgress);
            }
            final float homeScale = 1F - (stopHomingProgress / HOME_FOR);
            if (entity instanceof MobEntity && ((MobEntity) entity).getTarget() != null && homeScale > 0.0F) {
                LivingEntity target = ((MobEntity) entity).getTarget();
                if(target == null){
                    this.kill();
                }
                final double d0 = target.getX() - this.getX();
                final double d1 = target.getEyeY() - this.getY();
                final double d2 = target.getZ() - this.getZ();
                Vec3d vec = new Vec3d(d0, d1, d2).normalize().multiply(Math.max(homeScale, 0.5F) * 1.2F);
                this.setVelocity(vec);
            }else{
                this.setVelocity(this.getVelocity().add(0, -0.09, 0));
            }
        }
        super.tick();
        Vec3d vector3d = this.getVelocity();
        HitResult raytraceresult = ProjectileUtil.getCollision(this, this::canHitEntity);
        if (raytraceresult != null && raytraceresult.getType() != HitResult.Type.MISS) {
            this.onImpact(raytraceresult);
        }
        final double d0 = this.getX() + vector3d.x;
        final double d1 = this.getY() + vector3d.y;
        final double d2 = this.getZ() + vector3d.z;
        this.setNoGravity(true);
        this.updateRotation();
        if (this.getWorld().getStatesInBox(this.getBoundingBox()).noneMatch(AbstractBlock.AbstractBlockState::isAir)) {
            this.remove(RemovalReason.DISCARDED);
        } else if (this.isInsideWaterOrBubbleColumn()) {
            this.remove(RemovalReason.DISCARDED);
        } else {
            this.setVelocity(vector3d.multiply(0.99F));
            this.setPos(d0, d1, d2);
        }
    }

    protected void onEntityHit(EntityHitResult p_213868_1_) {
        Entity entity = this.getShooter();
        if (entity instanceof LivingEntity && !(p_213868_1_.getEntity() instanceof EntityVoidWorm || p_213868_1_.getEntity() instanceof EntityVoidWormPart)) {
            final boolean b = wormAttack(p_213868_1_.getEntity(), getDamageSources().mobProjectile(this, (LivingEntity) entity), (float) (AMConfig.voidWormDamageModifier * 4F));
            if(b && p_213868_1_.getEntity() instanceof PlayerEntity){
                PlayerEntity player = ((PlayerEntity)p_213868_1_.getEntity());
                //FIXME forge
//                if(player.getActiveItem().canPerformAction(ToolActions.SHIELD_BLOCK)){
//                    player.disableShield(true);
//                }
            }
        }

        this.remove(RemovalReason.DISCARDED);
    }

    private boolean wormAttack(Entity entity, DamageSource source, float dmg){
        return entity.damage(source, dmg);
    }

    protected void onHitBlock(BlockHitResult p_230299_1_) {
        if (!this.getWorld().isClient) {
            this.remove(RemovalReason.DISCARDED);
        }
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(STOP_HOMING_PROGRESS, 0.0F);
    }

    public float getStopHomingProgress() {
        return this.dataTracker.get(STOP_HOMING_PROGRESS);
    }

    public void setStopHomingProgress(float progress) {
        this.dataTracker.set(STOP_HOMING_PROGRESS, progress);
    }

    public void setShooter(@Nullable Entity entityIn) {
        if (entityIn != null) {
            this.ownerUUID = entityIn.getUuid();
            this.ownerNetworkId = entityIn.getId();
        }

    }

    @Nullable
    public Entity getShooter() {
        if (this.ownerUUID != null && this.getWorld() instanceof ServerWorld) {
            return ((ServerWorld) this.getWorld()).getEntity(this.ownerUUID);
        } else {
            return this.ownerNetworkId != 0 ? this.getWorld().getEntityById(this.ownerNetworkId) : null;
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound compound) {
        if (this.ownerUUID != null) {
            compound.putUuid("Owner", this.ownerUUID);
        }

        if (this.leftOwner) {
            compound.putBoolean("LeftOwner", true);
        }
        compound.putFloat("HomeTime", this.getStopHomingProgress());
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    protected void readCustomDataFromNbt(NbtCompound compound) {
        if (compound.containsUuid("Owner")) {
            this.ownerUUID = compound.getUuid("Owner");
        }
        this.setStopHomingProgress(compound.getFloat("HomeTime"));
        this.leftOwner = compound.getBoolean("LeftOwner");
    }

    private boolean checkLeftOwner() {
        Entity entity = this.getShooter();
        if (entity != null) {
            for (Entity entity1 : this.getWorld().getOtherEntities(this, this.getBoundingBox().stretch(this.getVelocity()).expand(1.0D), (p_234613_0_) -> !p_234613_0_.isSpectator() && p_234613_0_.canHit())) {
                if (entity1.getRootVehicle() == entity.getRootVehicle()) {
                    return false;
                }
            }
        }

        return true;
    }

    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        Vec3d vector3d = (new Vec3d(x, y, z)).normalize().add(this.random.nextGaussian() * (double) 0.0075F * (double) inaccuracy, this.random.nextGaussian() * (double) 0.0075F * (double) inaccuracy, this.random.nextGaussian() * (double) 0.0075F * (double) inaccuracy).multiply(velocity);
        this.setVelocity(this.getVelocity().add(vector3d));
        float f = MathHelper.sqrt((float) vector3d.horizontalLengthSquared());
        this.setYaw( (float) (MathHelper.atan2(vector3d.x, vector3d.z) * (double) MathHelper.DEGREES_PER_RADIAN));
        this.setPitch((float) (MathHelper.atan2(vector3d.y, f) * (double) MathHelper.DEGREES_PER_RADIAN));
        this.prevYaw = this.getYaw();
        this.prevPitch = this.getPitch();
    }


    /**
     * Called when this EntityFireball hits a block or entity.
     */
    protected void onImpact(HitResult result) {
        HitResult.Type raytraceresult$type = result.getType();
        if (raytraceresult$type == HitResult.Type.ENTITY) {
            this.onEntityHit((EntityHitResult) result);
        } else if (raytraceresult$type == HitResult.Type.BLOCK) {
            this.onHitBlock((BlockHitResult) result);
        }
        this.emitGameEvent(GameEvent.ENTITY_DIE);
        this.playSound(SoundEvents.BLOCK_GLASS_BREAK, 1F, 0.5F);
    }

    @Override
    public void setVelocityClient(double x, double y, double z) {
        this.setVelocity(x, y, z);
        if (this.prevPitch == 0.0F && this.prevYaw == 0.0F) {
            final float f = MathHelper.sqrt((float) (x * x + z * z));
            this.setPitch((float) (MathHelper.atan2(y, f) * (double) MathHelper.DEGREES_PER_RADIAN));
            this.setYaw( (float) (MathHelper.atan2(x, z) * (double) MathHelper.DEGREES_PER_RADIAN));
            this.prevPitch = this.getPitch();
            this.prevYaw = this.getYaw();
            this.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
        }

    }

    protected boolean canHitEntity(Entity p_230298_1_) {
        if (!p_230298_1_.isSpectator() && p_230298_1_.isAlive() && p_230298_1_.canHit()) {
            Entity entity = this.getShooter();
            return (entity == null || this.leftOwner || !entity.isConnectedThroughVehicle(p_230298_1_)) && !(p_230298_1_ instanceof EntityVoidWormShot || p_230298_1_ instanceof EntityVoidWormPart);
        } else {
            return false;
        }
    }

    protected void updateRotation() {
        Vec3d vector3d = this.getVelocity();
        final float f = MathHelper.sqrt((float) vector3d.horizontalLength());
        this.setPitch(lerpRotation(this.prevPitch, (float) (MathHelper.atan2(vector3d.y, f) * (double) MathHelper.DEGREES_PER_RADIAN)));
        this.setYaw( lerpRotation(this.prevYaw, (float) (MathHelper.atan2(vector3d.x, vector3d.z) * (double) MathHelper.DEGREES_PER_RADIAN)));
    }
}
