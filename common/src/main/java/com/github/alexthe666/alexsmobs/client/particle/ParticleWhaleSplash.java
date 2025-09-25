package com.github.alexthe666.alexsmobs.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.RainSplashParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

public class ParticleWhaleSplash extends RainSplashParticle {

    private ParticleWhaleSplash(ClientWorld p_i232433_1_, double p_i232433_2_, double p_i232433_4_, double p_i232433_6_, double p_i232433_8_, double p_i232433_10_, double p_i232433_12_) {
        super(p_i232433_1_, p_i232433_2_, p_i232433_4_, p_i232433_6_);
        this.gravityStrength = 0.04F;
        this.velocityY = 1D;
        this.maxAge = (int)(16.0D / (Math.random() * 0.4D + 0.1D));
        this.scale = 0.2F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;

    }

    @Override
    public void tick() {
        super.tick();
        if(this.velocityY < 0D){
            if(Math.abs(this.velocityX) < 0.23){
                this.velocityX *= 1.2D;
            }
            if(Math.abs(this.velocityZ) < 0.23){
                this.velocityZ *= 1.2D;
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider p_i50679_1_) {
            this.spriteProvider = p_i50679_1_;
        }

        @Override
        public Particle createParticle(DefaultParticleType p_199234_1_, ClientWorld p_199234_2_, double p_199234_3_, double p_199234_5_, double p_199234_7_, double p_199234_9_, double p_199234_11_, double p_199234_13_) {
            ParticleWhaleSplash lvt_15_1_ = new ParticleWhaleSplash(p_199234_2_, p_199234_3_, p_199234_5_, p_199234_7_, p_199234_9_, p_199234_11_, p_199234_13_);
            lvt_15_1_.setSprite(this.spriteProvider);
            return lvt_15_1_;
        }
    }

}
