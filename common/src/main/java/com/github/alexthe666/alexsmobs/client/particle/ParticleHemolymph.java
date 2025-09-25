package com.github.alexthe666.alexsmobs.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import org.jetbrains.annotations.NotNull;

public class ParticleHemolymph extends SpriteBillboardParticle {

    private static final int[] POSSIBLE_COLORS = {0X70FFF8, 0X3BFFD0, 0X08DED9};

    private ParticleHemolymph(ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        super(world, x, y, z);
        int color = POSSIBLE_COLORS[random.nextInt(POSSIBLE_COLORS.length - 1)];
        float lvt_18_1_ = (float)(color >> 16 & 255) / 255.0F;
        float lvt_19_1_ = (float)(color >> 8 & 255) / 255.0F;
        float lvt_20_1_ = (float)(color & 255) / 255.0F;
        setColor(lvt_18_1_, lvt_19_1_, lvt_20_1_);
        this.velocityX = (float) motionX;
        this.velocityY = (float) motionY;
        this.velocityZ = (float) motionZ;
        this.scale *= 0.6F + this.random.nextFloat() * 1.4F;
        this.maxAge = 10 + this.random.nextInt(15);
        this.gravityStrength = 0.5F;

    }

    @Override
    public void tick() {
        super.tick();
        this.velocityY -= 0.004D + 0.04D * (double)this.gravityStrength;
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
            ParticleHemolymph p = new ParticleHemolymph(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
            p.setSprite(spriteProvider);
            return p;
        }
    }
}
