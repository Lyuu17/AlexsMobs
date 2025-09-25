package com.github.alexthe666.alexsmobs.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

public class ParticleSimpleHeart extends SpriteBillboardParticle {

    protected ParticleSimpleHeart(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
        this.velocityX *= 0.01F;
        this.velocityY *= 0.01F;
        this.velocityZ *= 0.01F;
        this.velocityY += 0.1D;
        this.scale *= 2F;
        this.maxAge = 32;
        this.collidesWithWorld = false;
    }

    @Override
    public float getSize(float scaleFactor) {
        return this.scale * MathHelper.clamp(((float)this.age + scaleFactor) / (float)this.maxAge * 16.0F, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        if (this.age++ >= this.maxAge) {
            this.markDead();
        } else {
            this.move(this.velocityX, this.velocityY, this.velocityZ);
            if (this.y == this.prevPosY) {
                this.velocityX *= 1.1D;
                this.velocityZ *= 1.1D;
            }

            this.velocityX *= 0.86F;
            this.velocityY *= 0.86F;
            this.velocityZ *= 0.86F;
            if (this.onGround) {
                this.velocityX *= 0.7F;
                this.velocityZ *= 0.7F;
            }

        }
    }

    @NotNull
    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteSet) {
            this.spriteProvider = spriteSet;
        }

        @Override
        public Particle createParticle(DefaultParticleType typeIn, ClientWorld worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            ParticleSimpleHeart heartparticle = new ParticleSimpleHeart(worldIn, x, y, z);
            heartparticle.setSprite(this.spriteProvider);
            return heartparticle;
        }
    }
}
