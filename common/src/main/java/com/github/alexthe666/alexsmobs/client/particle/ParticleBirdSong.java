package com.github.alexthe666.alexsmobs.client.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import org.jetbrains.annotations.NotNull;

public class ParticleBirdSong extends SpriteBillboardParticle {

    private ParticleBirdSong(ClientWorld world, double x, double y, double z, double xd, double yd, double zd, SpriteProvider spriteProvider) {
        super(world, x, y, z);
        this.velocityMultiplier = 0.7F;
        this.gravityStrength = 0.0F;
        this.ascending = true;
        this.velocityX = xd;
        this.velocityY = yd;
        this.velocityZ = zd;
        this.scale = 0.15F + this.random.nextFloat() * 0.2F;
        this.maxAge = 20 + this.random.nextInt(20);
        this.setSprite(spriteProvider);
        this.red = 0.294F;
        this.green = 0.584F;
        this.blue = 1.0F;
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        this.velocityY = Math.sin(age * 0.3F) * 0.3F;
        if (this.age++ >= this.maxAge) {
            this.markDead();
        } else {
            this.move(this.velocityX, this.velocityY, this.velocityZ);
            this.velocityY -= this.gravityStrength;
        }
        float subAlpha = 1F;
        if(this.age > 5){
            subAlpha = 1 - (float)(this.age - 5) / (this.getMaxAge() - 5);
        }
        this.velocityX *= 0.99D;
        this.velocityY *= 0.99D;
        this.velocityZ *= 0.99D;
        this.alpha = subAlpha;
        this.angle = (float) (Math.toRadians(Math.sin(age * 0.01F) * 5));

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

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(DefaultParticleType typeIn, ClientWorld worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new ParticleBirdSong(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, spriteProvider);
        }
    }
}
