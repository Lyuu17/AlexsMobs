package com.github.alexthe666.alexsmobs.client.particle;

import com.github.alexthe666.alexsmobs.entity.util.Maths;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

public class ParticleSunbirdFeather extends AnimatedParticle {

    private ParticleSunbirdFeather(ClientWorld world, double x, double y, double z, double xd, double yd, double zd, SpriteProvider spriteProvider) {
        super(world, x, y, z, spriteProvider, 0.0F);
        this.velocityMultiplier = 0.96F;
        this.ascending = true;
        this.velocityX = xd;
        this.velocityY = yd;
        this.velocityZ = zd;
        this.scale = 0.15F + this.random.nextFloat() * 0.2F;
        this.maxAge = 20 + this.random.nextInt(20);
        this.gravityStrength = 0.02F;
        this.setSprite(spriteProvider);
        float f = MathHelper.sqrt((float) (xd * xd + zd * zd));
        float f1 = -(float) MathHelper.atan2(yd, f) + Maths.rad(135);
        this.angle = f1 * 2F;
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        this.velocityX *= 0.8;
        this.velocityY *= 0.8;
        this.velocityZ *= 0.8;
        if (this.age++ >= this.maxAge) {
            this.markDead();
        } else {
            this.prevAngle = this.angle;
            if (!this.onGround) {
                //float dist = -initialRoll / (this.maxAge - 6) * Math.min(this.age, this.maxAge - 6);
                this.angle += 0 + (float)Math.sin(age * 0.3F) * 0.5F * (this.age / (float)this.maxAge);
            }
            this.move(this.velocityX, this.velocityY, this.velocityZ);
            this.velocityY -= this.gravityStrength;
        }
    }

    @Override
    public int getBrightness(float p_189214_1_) {
        int lvt_2_1_ = super.getBrightness(p_189214_1_);
        int lvt_4_1_ = lvt_2_1_ >> 16 & 255;
        return 240 | lvt_4_1_ << 16;
    }

    @NotNull
    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteSet) {
            this.spriteProvider = spriteSet;
        }

        @Override
        public Particle createParticle(DefaultParticleType typeIn, ClientWorld worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new ParticleSunbirdFeather(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, spriteProvider);
        }
    }
}
