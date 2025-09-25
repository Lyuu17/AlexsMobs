package com.github.alexthe666.alexsmobs.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import org.jetbrains.annotations.NotNull;

public class ParticleWormPortal extends AnimatedParticle {

    private ParticleWormPortal(ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ, SpriteProvider sprites) {
        super(world, x, y, z, sprites, 0.0F);
        this.velocityX = (float) motionX;
        this.velocityY = (float) motionY;
        this.velocityZ = (float) motionZ;
        this.scale = 0.35F;
        this.maxAge = 10 + this.random.nextInt(12);
        this.gravityStrength = 0;
        this.setSpriteForAge(sprites);
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
        this.velocityX *= 0.8D;
        this.velocityY *= 0.8D;
        this.velocityZ *= 0.8D;
//        double lvt_1_1_ = this.x - this.prevPosX;
//        double lvt_5_1_ = this.z - this.prevPosZ;
        this.angle += 0.25F;
        this.setSpriteForAge(this.spriteProvider);
        this.scale = 0.35F * (1F - (this.age / (float)this.maxAge));
        this.setAlpha(1F - (this.age / (float)this.maxAge));
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
            return new ParticleWormPortal(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet);
        }
    }
}
