package com.github.alexthe666.alexsmobs.client.particle;

import com.github.alexthe666.alexsmobs.client.model.ModelGrizzlyBear;
import com.github.alexthe666.alexsmobs.client.render.entity.AMRenderLayers;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderGrizzlyBear;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;

public class ParticleBearFreddy extends Particle {
    private final ModelGrizzlyBear model = new ModelGrizzlyBear();

    ParticleBearFreddy(ClientWorld lvl, double x, double y, double z) {
        super(lvl, x, y, z);
        this.setBoundingBoxSpacing(2, 2);
        this.gravityStrength = 0.0F;
        this.maxAge = 15;
    }

    @NotNull
    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.CUSTOM;
    }

    @Override
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float partialTick) {
        float fogBefore = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogEnd(40);
        float f = ((float) this.age + partialTick) / (float) this.maxAge;
        float initalFlip = Math.min(f, 0.1F) / 0.1F;
        float laterFlip = MathHelper.clamp(f - 0.1F, 0F, 0.1F) / 0.1F;
        float scale = 1;
        var matrixStack = new MatrixStack();
        matrixStack.multiply(camera.getRotation());
        matrixStack.translate(0.0D, -1, 0);
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10F - laterFlip * 35F));
        matrixStack.scale(-scale, -scale, scale);
        matrixStack.translate(0.0D, 0.5F, 2 + (1F - initalFlip));
        var VertexConsumerProvider$buffersource = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        var vertexconsumer = VertexConsumerProvider$buffersource.getBuffer(AMRenderLayers.getFreddy(RenderGrizzlyBear.TEXTURE_FREDDY));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(initalFlip * 20F - 5F));
        float swing = laterFlip * (float) Math.sin((age + partialTick) * 0.3F) * 20;
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((1F - initalFlip) * 45F + swing));
        boolean baby = this.model.child;
        this.model.child = false;
        this.model.positionForParticle(partialTick, age + partialTick);
        this.model.render(matrixStack, vertexconsumer, 240, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
        this.model.child = baby;
        VertexConsumerProvider$buffersource.draw();
        RenderSystem.setShaderFogEnd(fogBefore);
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        @Override
        public Particle createParticle(DefaultParticleType typeIn, ClientWorld worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new ParticleBearFreddy(worldIn, x, y, z);
        }
    }
}