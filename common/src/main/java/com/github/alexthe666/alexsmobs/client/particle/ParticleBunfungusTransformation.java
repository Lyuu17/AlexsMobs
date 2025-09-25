package com.github.alexthe666.alexsmobs.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

public class ParticleBunfungusTransformation  extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;

    public ParticleBunfungusTransformation(ClientWorld p_107483_, double p_107484_, double p_107485_, double p_107486_, double p_107487_, double p_107488_, double p_107489_, SpriteProvider p_107490_) {
        super(p_107483_, p_107484_, p_107485_, p_107486_, 0.0D, 0.0D, 0.0D);
        this.velocityMultiplier = 0.96F;
        this.spriteProvider = p_107490_;
        float f = 2.5F;
        this.velocityX *= 0.1F;
        this.velocityY *= 0.1F;
        this.velocityZ *= 0.1F;
        this.velocityX += p_107487_;
        this.velocityY += p_107488_;
        this.velocityZ += p_107489_;
        int i = (int)(8.0D / (Math.random() * 0.8D + 0.3D));
        this.maxAge = (int)Math.max((float)i * 2.5F, 1.0F);
        this.collidesWithWorld = false;
        this.setSpriteForAge(p_107490_);
        this.setAlpha((float) (Math.random() * 0.3F + 0.7F));
    }

    @NotNull
    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public float getSize(float p_107504_) {
        return this.scale * MathHelper.clamp(((float)this.age + p_107504_) / (float)this.maxAge * 32.0F, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.dead) {
            this.setSpriteForAge(this.spriteProvider);
            var player = this.world.getClosestPlayer(this.x, this.y, this.z, 2.0D, false);
            if (player != null) {
                double d0 = player.getY();
                if (this.y > d0) {
                    this.y += (d0 - this.y) * 0.2D;
                    this.velocityY += (player.getVelocity().y - this.velocityY) * 0.2D;
                    this.setPos(this.x, this.y, this.z);
                }
            }
        }

    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider p_107528_) {
            this.sprites = p_107528_;
        }

        @Override
        public Particle createParticle(DefaultParticleType p_107539_, ClientWorld p_107540_, double p_107541_, double p_107542_, double p_107543_, double p_107544_, double p_107545_, double p_107546_) {
            ParticleBunfungusTransformation particle = new ParticleBunfungusTransformation(p_107540_, p_107541_, p_107542_, p_107543_, p_107544_, p_107545_, p_107546_, this.sprites);
            particle.setColor(0.427451F, 0.4470588F, 0.5764706F);
            return particle;
        }
    }
}
