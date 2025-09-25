package com.github.alexthe666.alexsmobs.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import org.jetbrains.annotations.NotNull;

public class ParticleFungusBubble extends AnimatedParticle {

    private ParticleFungusBubble(ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ, SpriteProvider sprites) {
        super(world, x, y, z, sprites, 0.0F);
        this.velocityX = (float) motionX;
        this.velocityY = (float) motionY + 0.04D;
        this.velocityZ = (float) motionZ;
        this.scale *= 0.5F + this.random.nextFloat() * 0.5F;
        this.maxAge = 15 + this.random.nextInt(20);
        this.gravityStrength = -0.1F;
        this.setSprite(this.spriteProvider.getSprite(0, this.maxAge));
    }

    @Override
    public void tick() {
        super.tick();
        int halflife = this.maxAge / 2;
        int spriteMod = halflife / 6;
        boolean flag = false;
        if (this.age < halflife) {
            flag = true;
            this.setSprite(this.spriteProvider.getSprite(0, 6));
        } else if (this.age < halflife + spriteMod) {
            this.setSprite(this.spriteProvider.getSprite(1, 6));
        }else if (this.age < halflife + spriteMod * 2) {
            this.setSprite(this.spriteProvider.getSprite(2, 6));
        }else if (this.age < halflife + spriteMod * 3) {
            this.setSprite(this.spriteProvider.getSprite(3, 6));
        }else if (this.age < halflife + spriteMod * 4) {
            this.setSprite(this.spriteProvider.getSprite(4, 6));
        }else if (this.age < halflife + spriteMod * 5) {
            this.setSprite(this.spriteProvider.getSprite(5, 6));
        }else{
            this.setSprite(this.spriteProvider.getSprite(6, 6));
        }
        if(!flag){
            this.velocityY = 0;
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
            return new ParticleFungusBubble(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, spriteProvider);
        }
    }
}
