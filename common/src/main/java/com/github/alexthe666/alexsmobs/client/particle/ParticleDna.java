package com.github.alexthe666.alexsmobs.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

public class ParticleDna extends AnimatedParticle {

    private ParticleDna(ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ, SpriteProvider sprites) {
        super(world, x, y, z, sprites, 0.0F);
        this.velocityX = (float) motionX;
        this.velocityY = (float) motionY;
        this.velocityZ = (float) motionZ;
        this.scale *= 1.5F + this.random.nextFloat() * 0.6F;
        this.maxAge = 15 + this.random.nextInt(15);
        this.gravityStrength = 0.1F;
        int color = 15916745;
        float lvt_18_1_ = (float)(color >> 16 & 255) / 255.0F;
        float lvt_19_1_ = (float)(color >> 8 & 255) / 255.0F;
        float lvt_20_1_ = (float)(color & 255) / 255.0F;
        setColor(lvt_18_1_, lvt_19_1_, lvt_20_1_);
        this.setSpriteForAge(sprites);
        this.alpha = 0F;
        this.angle = (float) (Math.PI * 0.5F * random.nextFloat());
    }

    @Override
    public int getBrightness(float p_189214_1_) {
        int lvt_2_1_ = super.getBrightness(p_189214_1_);
        int lvt_4_1_ = lvt_2_1_ >> 16 & 255;
        return 240 | lvt_4_1_ << 16;
    }

    @Override
    public void tick() {
        super.tick();
        this.prevAngle = this.angle;
        this.velocityX += (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
        this.velocityY += (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
        this.velocityZ += (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;

        float subAlpha = 1F;
        if(this.age > 5){
            subAlpha = 1 - (float)(this.age - 5) / (this.getMaxAge() - 5);
        }
        this.alpha = subAlpha;
        this.setSpriteForAge(this.spriteProvider);
        this.angle += (float)Math.random() * (MathHelper.PI * 0.3F * alpha);
    }

    @NotNull
    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteSet;

        public Factory(SpriteProvider spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(DefaultParticleType typeIn, ClientWorld worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new ParticleDna(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet);
        }
    }
}
