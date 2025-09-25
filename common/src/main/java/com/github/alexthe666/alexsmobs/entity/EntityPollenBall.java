package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class EntityPollenBall extends EntityMobProjectile {

    public EntityPollenBall(EntityType<EntityPollenBall> type, World level) {
        super(type, level);
    }

    public EntityPollenBall(World worldIn, EntityFlutter flutter) {
        super(AMEntityRegistry.POLLEN_BALL.get(), worldIn, flutter);
        Vec3d vec3 = flutter.getPos().add(calcOffsetVec(new Vec3d(0, 0.4F * flutter.getScaleFactor(), 0), flutter.getFlutterPitch(), flutter.getYaw()));
        this.setPos(vec3.x, vec3.y, vec3.z);
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }

    public void doBehavior() {
        var entity = this.getShooter();
        if (entity instanceof MobEntity && ((MobEntity) entity).getTarget() != null) {
            var target = ((MobEntity) entity).getTarget();
            if (target == null) {
                this.kill();
            }
            final double d0 = target.getX() - this.getX();
            final double d1 = target.getY() + target.getHeight() * 0.5F - this.getY();
            final double d2 = target.getZ() - this.getZ();
            final float speed = 0.35F;
            shoot(d0, d1, d2, speed, 0);
            this.setYaw(-((float) MathHelper.atan2(d0, d2)) * MathHelper.DEGREES_PER_RADIAN);
        }
        if(this.getWorld().isClient && random.nextInt(2) == 0){
            final float r1 = (random.nextFloat() - 0.5F) * 0.5F;
            final float r2 = (random.nextFloat() - 0.5F) * 0.5F;
            final float r3 = (random.nextFloat() - 0.5F) * 0.5F;
            this.getWorld().addParticle(ParticleTypes.FALLING_NECTAR, this.getX() + r1, this.getY() + r2, this.getZ() + r3, r1 * 0.1F, r2 * 0.1F, r3 * 0.1F);
        }
    }

    @Override
    protected float getDamage() {
        return 3;
    }
}
