package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMParticleRegistry;
import dev.architectury.networking.NetworkManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class EntityGust extends Entity {
    protected static final TrackedData<Boolean> VERTICAL = DataTracker.registerData(EntityGust.class, TrackedDataHandlerRegistry.BOOLEAN);
    protected static final TrackedData<Float> X_DIR = DataTracker.registerData(EntityGust.class, TrackedDataHandlerRegistry.FLOAT);
    protected static final TrackedData<Float> Y_DIR = DataTracker.registerData(EntityGust.class, TrackedDataHandlerRegistry.FLOAT);
    protected static final TrackedData<Float> Z_DIR = DataTracker.registerData(EntityGust.class, TrackedDataHandlerRegistry.FLOAT);
    private Entity pushedEntity = null;

    public EntityGust(EntityType<EntityGust> p_i50162_1_, World p_i50162_2_) {
        super(p_i50162_1_, p_i50162_2_);
    }

    public EntityGust(World worldIn) {
        this(AMEntityRegistry.GUST.get(), worldIn);
    }

    @Override
    public void pushAwayFrom(Entity entityIn) {
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

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return NetworkManager.createAddEntityPacket(this);
    }

    @Override
    public void tick() {
        super.tick();
        if(this.age > 300){
            this.remove(RemovalReason.DISCARDED);
        }
        for (int i = 0; i < 1 + random.nextInt(1); ++i) {
            getWorld().addParticle(AMParticleRegistry.GUSTER_SAND_SPIN.get(), this.getX() + 0.5F * (random.nextFloat() - 0.5F), this.getY() + 0.5F * (random.nextFloat() - 0.5F), this.getZ() + 0.5F * (random.nextFloat() - 0.5F), this.getX(), this.getY() + 0.5F, this.getZ());
        }
        var vector3d = new Vec3d(this.dataTracker.get(X_DIR), this.dataTracker.get(Y_DIR), this.dataTracker.get(Z_DIR));
        var raytraceresult = ProjectileUtil.getCollision(this, this::canHitEntity);
        if (raytraceresult != null && raytraceresult.getType() != HitResult.Type.MISS && this.age > 4) {
            this.onImpact(raytraceresult);
        }
        var list = this.getWorld().getNonSpectatingEntities(Entity.class, this.getBoundingBox().expand(0.1));

        if(pushedEntity != null && this.distanceTo(pushedEntity) > 2){
            pushedEntity = null;
        }
        double d0 = this.getX() + vector3d.x;
        double d1 = this.getY() + vector3d.y;
        double d2 = this.getZ() + vector3d.z;
        if(this.getY() > this.getWorld().getTopY()){
            this.remove(RemovalReason.DISCARDED);
        }
        this.updateRotation();
         if (this.isInsideWaterOrBubbleColumn()) {
            this.remove(RemovalReason.DISCARDED);
        } else {
            this.setVelocity(vector3d);
            this.setVelocity(this.getVelocity().add(0.0D, -0.06F, 0.0D));
            this.setPos(d0, d1, d2);
            if(pushedEntity != null){
                pushedEntity.setVelocity(this.getVelocity().add(0, 0.063, 0));
            }
            for(Entity e : list){
                e.setVelocity(this.getVelocity().add(0, 0.068, 0));
                if(e.getVelocity().y < 0){
                    e.setVelocity(e.getVelocity().multiply(1, 0, 1));
                }
                e.fallDistance = 0;
            }
        }
    }

    public void setGustDir(float x, float y, float z){
        this.dataTracker.set(X_DIR, x);
        this.dataTracker.set(Y_DIR, y);
        this.dataTracker.set(Z_DIR, z);
    }

    public float getGustDir(int xyz){
       return this.dataTracker.get(xyz == 2 ? Z_DIR : xyz == 1 ? Y_DIR : X_DIR);
    }

    protected void onEntityHit(EntityHitResult result) {
        var entity = result.getEntity();
        if(entity instanceof EntityGust other){
            double avgX = (other.getX() + this.getX()) / 2F;
            double avgY = (other.getY() + this.getY()) / 2F;
            double avgZ = (other.getZ() + this.getZ()) / 2F;
            other.setPos(avgX, avgY, avgZ);
            other.setGustDir(other.getGustDir(0) + this.getGustDir(0), other.getGustDir(1) + this.getGustDir(1), other.getGustDir(2) + this.getGustDir(2));
            if(this.isAlive() && other.isAlive()){
                this.remove(RemovalReason.DISCARDED);
            }
        }else if(entity != null){
            pushedEntity = entity;
        }
    }


    protected boolean canHitEntity(Entity p_230298_1_) {
        return !p_230298_1_.isSpectator();
    }

    protected void onHitBlock(BlockHitResult p_230299_1_) {
        if( p_230299_1_.getBlockPos() != null){
            BlockPos pos = p_230299_1_.getBlockPos();
            if(getWorld().isWater(pos)){
                if (!this.getWorld().isClient) {
                    this.remove(RemovalReason.DISCARDED);

                }
            }
        }

    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(VERTICAL, false);
        this.dataTracker.startTracking(X_DIR, 0f);
        this.dataTracker.startTracking(Y_DIR, 0F);
        this.dataTracker.startTracking(Z_DIR, 0F);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound compound) {
        compound.putBoolean("VerticalTornado", getVertical());
        compound.putFloat("GustDirX", this.dataTracker.get(X_DIR));
        compound.putFloat("GustDirY", this.dataTracker.get(Y_DIR));
        compound.putFloat("GustDirZ", this.dataTracker.get(Z_DIR));
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    protected void readCustomDataFromNbt(NbtCompound compound) {
        this.dataTracker.set(X_DIR, compound.getFloat("GustDirX"));
        this.dataTracker.set(Y_DIR, compound.getFloat("GustDirX"));
        this.dataTracker.set(Z_DIR, compound.getFloat("GustDirX"));
        this.setVertical((compound.getBoolean("VerticalTornado")));
    }

    public void setVertical(boolean vertical){
        this.dataTracker.set(VERTICAL, vertical);
    }

    public boolean getVertical(){
        return this.dataTracker.get(VERTICAL);
    }

    protected void onImpact(HitResult result) {
        HitResult.Type raytraceresult$type = result.getType();
        if (raytraceresult$type == HitResult.Type.ENTITY) {
            this.onEntityHit((EntityHitResult) result);
        } else if (raytraceresult$type == HitResult.Type.BLOCK) {
            this.onHitBlock((BlockHitResult) result);
        }

    }

    @Override
    public void setVelocityClient(double x, double y, double z) {
        this.setVelocity(x, y, z);
        if (this.prevPitch == 0.0F && this.prevYaw == 0.0F) {
            float f = MathHelper.sqrt((float)(x * x + z * z));
            this.setPitch((float) (MathHelper.atan2(y, f) * (double) MathHelper.DEGREES_PER_RADIAN));
            this.setYaw( (float) (MathHelper.atan2(x, z) * (double) MathHelper.DEGREES_PER_RADIAN));
            this.prevPitch = this.getPitch();
            this.prevYaw = this.getYaw();
            this.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
        }
    }

    protected void updateRotation() {
        var vector3d = this.getVelocity();
        float f = MathHelper.sqrt((float)(vector3d.x * vector3d.x + vector3d.z * vector3d.z));
        this.setPitch(lerpRotation(this.prevPitch, (float) (MathHelper.atan2(vector3d.y, f) * (double) MathHelper.DEGREES_PER_RADIAN)));
        this.setYaw( lerpRotation(this.prevYaw, (float) (MathHelper.atan2(vector3d.x, vector3d.z) * (double) MathHelper.DEGREES_PER_RADIAN)));
    }
}
