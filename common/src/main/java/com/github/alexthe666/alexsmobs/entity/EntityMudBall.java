package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class EntityMudBall extends EntityMobProjectile {

    public EntityMudBall(EntityType<EntityMudBall> type, World level) {
        super(type, level);
    }

    public EntityMudBall(World worldIn, EntityMudskipper mudskipper) {
        super(AMEntityRegistry.MUD_BALL.get(), worldIn, mudskipper);
        Vec3d vec3 = mudskipper.getPos().add(calcOffsetVec(new Vec3d(0, 0, 0.2F * mudskipper.getScaleFactor()), 0F, mudskipper.getYaw()));
        this.setPos(vec3.x, vec3.y, vec3.z);
    }

    @Override
    public void doBehavior() {
        this.setVelocity(this.getVelocity().multiply((double)0.9F));
        if (!this.hasNoGravity()) {
            this.setVelocity(this.getVelocity().add(0.0D, (double)-0.06F, 0.0D));
        }
    }

    @Override
    protected boolean removeInWater(){
        return false;
    }

    @Override
    protected float getDamage() {
        return 1 + random.nextInt(3);
    }

    @Override
    protected void onEntityHit(EntityHitResult result) {
        super.onEntityHit(result);
        if(result.getEntity() instanceof LivingEntity hurt){
            hurt.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60));
        }
    }

    @Override
    public void handleStatus(byte event) {
        if (event == 3) {
            var particle = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.MUD.getDefaultState());
            for(int i = 0; i < 8; ++i) {
                this.getWorld().addParticle(particle, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            }
        }else{
            super.handleStatus(event);
        }
    }

    @Override
    protected void onImpact(HitResult result) {
        if (!this.getWorld().isClient) {
            this.getWorld().sendEntityStatus(this, (byte)3);
        }
        super.onImpact(result);
    }
}
