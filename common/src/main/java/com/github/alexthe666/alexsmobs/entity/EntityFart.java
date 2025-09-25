package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMParticleRegistry;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class EntityFart extends Entity {
    private UUID ownerUUID;
    private int ownerNetworkId;
    private boolean leftOwner;

    public EntityFart(EntityType<EntityFart> p_i50162_1_, World p_i50162_2_) {
        super(p_i50162_1_, p_i50162_2_);
    }

    public EntityFart(World worldIn, LivingEntity p_i47273_2_, boolean right) {
        this(AMEntityRegistry.FART.get(), worldIn);
        this.setShooter(p_i47273_2_);
        float rot = p_i47273_2_.headYaw+ (right ? 60 : -60);
        this.setPos(p_i47273_2_.getX() - (double) (p_i47273_2_.getWidth()) * 0.5D * (double) MathHelper.sin(rot * MathHelper.RADIANS_PER_DEGREE), p_i47273_2_.getEyeY() - (double) 0.2F, p_i47273_2_.getZ() + (double) (p_i47273_2_.getWidth()) * 0.5D * (double) MathHelper.cos(rot * MathHelper.RADIANS_PER_DEGREE));
    }

    @Override
    public void tick(){
        super.tick();
        var vector3d = this.getVelocity();
        var raytraceresult = ProjectileUtil.getCollision(this, this::canHitEntity);
        if (raytraceresult != null && raytraceresult.getType() != HitResult.Type.MISS) {
            this.onImpact(raytraceresult);
        }
        updateRotation();
        double d0 = this.getX() + vector3d.x;
        double d1 = this.getY() + vector3d.y;
        double d2 = this.getZ() + vector3d.z;
        this.setVelocity(vector3d.multiply(0.95F));
        this.setPos(d0, d1, d2);
        if(this.age > 30){
            this.remove(RemovalReason.DISCARDED);
        }
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }

    protected void onImpact(HitResult result) {
        HitResult.Type raytraceresult$type = result.getType();
        if (raytraceresult$type == HitResult.Type.ENTITY) {
            this.onEntityHit((EntityHitResult) result);
        } else if (raytraceresult$type == HitResult.Type.BLOCK) {
            this.onHitBlock((BlockHitResult) result);
        }
    }

    protected void onEntityHit(EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity living) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 300, 0));
            for(int i = 0; i < 10 + random.nextInt(6); i++){
                getWorld().addParticle(AMParticleRegistry.SMELLY.get(), living.getParticleX(1.0F), living.getRandomBodyY(), living.getParticleZ(1.0F), 0, 0, 0);
            }
            for (var nearby : getWorld().getNonSpectatingEntities(MobEntity.class, living.getBoundingBox().expand(15))) {
                if(nearby == living || nearby.getId() == living.getId() ||nearby.getUuid().equals(living.getUuid()) || nearby.isTeammate(living) || living.isTeammate(nearby) || living instanceof IHurtableMultipart){
                    continue;
                }else{
                    nearby.setAttacker(living);
                    nearby.setTarget(living);
                }
            }
        }
    }

    protected void onHitBlock(BlockHitResult result) {
        this.remove(RemovalReason.DISCARDED);
    }

    protected boolean canHitEntity(Entity hit) {
        if (!hit.isSpectator() && hit.isAlive() && hit.canHit()) {
            Entity entity = this.getShooter();
            return entity == null || this.leftOwner || !entity.isConnectedThroughVehicle(hit);
        } else {
            return false;
        }
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

    protected void updateRotation() {
        Vec3d vector3d = this.getVelocity().normalize();
        float f = MathHelper.sqrt((float) vector3d.horizontalLengthSquared());
        this.setPitch(lerpRotation(this.prevPitch, (float) (MathHelper.atan2(vector3d.y, f) * (double) MathHelper.DEGREES_PER_RADIAN)));
        this.setYaw(lerpRotation(this.prevYaw, (float) (MathHelper.atan2(vector3d.x, vector3d.z) * (double) MathHelper.DEGREES_PER_RADIAN)));
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void setVelocityClient(double x, double y, double z) {
        this.setVelocity(x, y, z);
        if (this.prevPitch == 0.0F && this.prevYaw == 0.0F) {
            float f = MathHelper.sqrt((float) (x * x + z * z));
            this.setPitch((float) (MathHelper.atan2(y, f) * (double) MathHelper.DEGREES_PER_RADIAN));
            this.setYaw( (float) (MathHelper.atan2(x, z) * (double) MathHelper.DEGREES_PER_RADIAN));
            this.prevPitch = this.getPitch();
            this.prevYaw = this.getYaw();
            this.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
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
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return NetworkManager.createAddEntityPacket(this);
    }

    @Override
    protected void initDataTracker() {

    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound compound) {
        if (compound.containsUuid("Owner")) {
            this.ownerUUID = compound.getUuid("Owner");
        }

        this.leftOwner = compound.getBoolean("LeftOwner");
    }

    @Nullable
    public Entity getShooter() {
        if (this.ownerUUID != null && this.getWorld() instanceof ServerWorld) {
            return ((ServerWorld) this.getWorld()).getEntity(this.ownerUUID);
        } else {
            return this.ownerNetworkId != 0 ? this.getWorld().getEntityById(this.ownerNetworkId) : null;
        }
    }

    public void setShooter(@Nullable Entity entityIn) {
        if (entityIn != null) {
            this.ownerUUID = entityIn.getUuid();
            this.ownerNetworkId = entityIn.getId();
        }
    }

    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        Vec3d vector3d = (new Vec3d(x, y, z)).normalize().add(this.random.nextGaussian() * (double) 0.0075F * (double) inaccuracy, this.random.nextGaussian() * (double) 0.0075F * (double) inaccuracy, this.random.nextGaussian() * (double) 0.0075F * (double) inaccuracy).multiply(velocity);
        this.setVelocity(vector3d);
        float f = MathHelper.sqrt((float) vector3d.horizontalLengthSquared());
        this.setYaw( (float) (MathHelper.atan2(vector3d.x, vector3d.z) * (double) MathHelper.DEGREES_PER_RADIAN));
        this.setPitch((float) (MathHelper.atan2(vector3d.y, f) * (double) MathHelper.DEGREES_PER_RADIAN));
        this.prevYaw = this.getYaw();
        this.prevPitch = this.getPitch();
    }

    private boolean checkLeftOwner() {
        Entity entity = this.getShooter();
        if (entity != null) {
            for (var entity1 : this.getWorld().getOtherEntities(this, this.getBoundingBox().stretch(this.getVelocity()).expand(1.0D), (Entity p_234613_0_) -> !p_234613_0_.isSpectator() && p_234613_0_.canHit())) {
                if (entity1.getRootVehicle() == entity.getRootVehicle()) {
                    return false;
                }
            }
        }

        return true;
    }
}
