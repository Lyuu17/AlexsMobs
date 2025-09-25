package com.github.alexthe666.alexsmobs.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

public class ParticleTeethGlint extends SpriteBillboardParticle {

    private float initScale = 0.25F;

    private ParticleTeethGlint(ClientWorld p_i232444_1_, double p_i232444_2_, double p_i232444_4_, double p_i232444_6_, double p_i232444_8_, double p_i232444_10_, double p_i232444_12_) {
        super(p_i232444_1_, p_i232444_2_, p_i232444_4_, p_i232444_6_, p_i232444_8_, p_i232444_10_, p_i232444_12_);
        float lvt_14_1_ = this.random.nextFloat() * 0.1F + 0.2F;
        this.red = lvt_14_1_;
        this.green = lvt_14_1_;
        this.blue = lvt_14_1_;
        this.setBoundingBoxSpacing(0.02F, 0.02F);
        this.scale = initScale = 0.1F * (this.random.nextFloat() * 0.3F + 0.5F);
        this.velocityX *= 0.019999999552965164D;
        this.velocityY *= 0.019999999552965164D;
        this.velocityZ *= 0.019999999552965164D;
        this.maxAge = 3 + this.random.nextInt(5);
        this.setAlpha(1F - (this.age / (float)this.maxAge) * 0.5F);
    }

    @Override
    public int getBrightness(float p_189214_1_) {
        int lvt_2_1_ = super.getBrightness(p_189214_1_);
        int lvt_4_1_ = lvt_2_1_ >> 16 & 255;
        return 240 | lvt_4_1_ << 16;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void move(double p_187110_1_, double p_187110_3_, double p_187110_5_) {
        this.setBoundingBox(this.getBoundingBox().offset(p_187110_1_, p_187110_3_, p_187110_5_));
        this.repositionFromBoundingBox();
    }

    @Override
    public void tick() {
        super.tick();
        this.prevAngle = this.angle;
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        if (this.maxAge-- <= 0) {
            this.markDead();
        } else {
            this.move(this.velocityX, this.velocityY, this.velocityZ);
            this.velocityX *= 0.99D;
            this.velocityY *= 0.99D;
            this.velocityZ *= 0.99D;
        }
        this.angle += 0.25F * Math.sin(this.age * 2);
        this.scale = initScale * (1F - (this.age / (float)this.maxAge) * 0.5F);
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider p_i50522_1_) {
            this.spriteProvider = p_i50522_1_;
        }

        @Override
        public Particle createParticle(DefaultParticleType p_199234_1_, ClientWorld p_199234_2_, double p_199234_3_, double p_199234_5_, double p_199234_7_, double p_199234_9_, double p_199234_11_, double p_199234_13_) {
            ParticleTeethGlint lvt_15_1_ = new ParticleTeethGlint(p_199234_2_, p_199234_3_, p_199234_5_, p_199234_7_, p_199234_9_, p_199234_11_, p_199234_13_);
            lvt_15_1_.setSprite(this.spriteProvider);
            lvt_15_1_.setColor(1.0F, 1.0F, 1.0F);
            return lvt_15_1_;
        }
    }


}
