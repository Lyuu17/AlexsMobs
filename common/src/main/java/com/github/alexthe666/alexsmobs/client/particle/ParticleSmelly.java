package com.github.alexthe666.alexsmobs.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import org.jetbrains.annotations.NotNull;

public class ParticleSmelly extends AnimatedParticle {

    private ParticleSmelly(ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ, SpriteProvider spriteProvider) {
        super(world, x, y, z, spriteProvider, 0.0F);
        this.velocityX = (float) motionX;
        this.velocityY = (float) motionY;
        this.velocityZ = (float) motionZ;
        this.scale *= 0.7F + this.random.nextFloat() * 0.6F;
        this.maxAge = 15 + this.random.nextInt(15);
        this.gravityStrength = -0.1F;
        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public void tick() {
        super.tick();
        this.prevAngle = this.angle;
        this.velocityX += ((this.random.nextFloat() - this.random.nextFloat()) * 0.05F);
        this.velocityY += ((this.random.nextFloat() - this.random.nextFloat()) * 0.05F);
        this.velocityZ += ((this.random.nextFloat() - this.random.nextFloat()) * 0.05F);
        this.setSpriteForAge(this.spriteProvider);
    }

    @NotNull
    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteSet) {
            this.spriteProvider = spriteSet;
        }

        @Override
        public Particle createParticle(DefaultParticleType typeIn, ClientWorld worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new ParticleSmelly(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, spriteProvider);
        }
    }
}
