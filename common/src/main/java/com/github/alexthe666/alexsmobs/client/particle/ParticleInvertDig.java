package com.github.alexthe666.alexsmobs.client.particle;

import com.github.alexthe666.alexsmobs.item.ItemDimensionalCarver;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.AnimatedParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

public class ParticleInvertDig extends AnimatedParticle {

    private final Entity creator;

    protected ParticleInvertDig(ClientWorld world, double x, double y, double z, SpriteProvider spriteWithAge, double creatorId) {
        super(world, x, y, z, spriteWithAge, 0);
        this.velocityX = 0;
        this.velocityY = 0;
        this.velocityZ = 0;
        this.scale = 0.1F;
        this.alpha = 1F;
        this.maxAge = ItemDimensionalCarver.DEFAULT_MAX_USE_TIME;
        this.collidesWithWorld = false;
        this.creator = world.getEntityById((int) creatorId);
    }

    @Override
    public int getBrightness(float p_189214_1_) {
        return 240;
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        boolean live = false;
        this.scale = 0.1F + Math.min((age / (float)this.maxAge), 0.5F) * 0.5F;
        if (this.age++ >= this.maxAge || creator == null) {
            this.markDead();
        } else {
            if (creator instanceof final PlayerEntity player) {
                var item = player.getActiveItem();
                if (item.getItem() instanceof ItemDimensionalCarver) {
                    this.age = MathHelper.clamp(this.maxAge - player.getItemUseTimeLeft(), 0, this.maxAge);
                    live = true;
                }
            }
        }
        if(!live){
            this.markDead();
        }
        this.setSpriteForAge(this.spriteProvider);
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteSet) {
            this.spriteProvider = spriteSet;
        }

        @Override
        public Particle createParticle(DefaultParticleType typeIn, ClientWorld worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            var heartparticle = new ParticleInvertDig(worldIn, x, y, z, this.spriteProvider, xSpeed);
            heartparticle.setSpriteForAge(this.spriteProvider);
            return heartparticle;
        }
    }
}
