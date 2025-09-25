package com.github.alexthe666.alexsmobs.client.particle;

import com.github.alexthe666.alexsmobs.client.render.entity.AMRenderLayers;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class ParticleSkulkBoom extends Particle {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/particle/skulk_boom.png");
    private float size;
    private float prevSize;
    private float prevAlpha;
    private final float alphaDecrease;


    private ParticleSkulkBoom(ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        super(world, x, y, z);
        this.setBoundingBoxSpacing(1, 0.1F);
        this.alpha = 1F;
        this.gravityStrength = 0.0F;
        this.velocityX = motionX;
        this.velocityY = motionY;
        this.velocityZ = motionZ;
        this.maxAge = 20 + this.random.nextInt(20);
        this.alphaDecrease = 1F / (float)Math.max(this.maxAge, 1F);
        this.size = 0.3F;
    }

    @Override
    public void tick(){
        super.tick();
        this.prevSize = size;
        this.prevAlpha = alpha;
        this.size += 0.3F;
        this.velocityX *= 0.1D;
        this.velocityY *= 0.8D;
        this.velocityZ *= 0.1D;
        if(this.alpha > 0.0F){
            this.alpha = Math.max(this.alpha - alphaDecrease, 0.0F);
        }
        this.setBoundingBoxSpacing(1 + size, 0.1F);
    }

    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float partialTick) {
        var vec3 = camera.getPos();
        float f = (float)(MathHelper.lerp(partialTick, this.prevPosX, this.x) - vec3.x);
        float f1 = (float)(MathHelper.lerp(partialTick, this.prevPosY, this.y) - vec3.y);
        float f2 = (float)(MathHelper.lerp(partialTick, this.prevPosZ, this.z) - vec3.z);
        var quaternion = RotationAxis.POSITIVE_X.rotationDegrees(90F);
        var VertexConsumerProvider$buffersource = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        var portalStatic = VertexConsumerProvider$buffersource.getBuffer(AMRenderLayers.getSkulkBoom());
        var posestack = new MatrixStack();
        var posestack$pose = posestack.peek();
        var matrix4f = posestack$pose.getPositionMatrix();
        var matrix3f = posestack$pose.getNormalMatrix();
        float f4 = prevSize + partialTick * (size - prevSize);
        float alphaLerp = prevAlpha + partialTick * (alpha - prevAlpha);
        var vector3f1 = new Vector3f(-1.0F, -1.0F, 0.0F);
        vector3f1.rotate(quaternion);
        var avector3f = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};

        for(int i = 0; i < 4; ++i) {
            Vector3f vector3f = avector3f[i];
            vector3f.rotate(quaternion);
            vector3f.mul(f4);
            vector3f.add(f, f1, f2);
        }
        float f7 = 0;
        float f8 = 1;
        float f5 = 0;
        float f6 = 1;
        int j = 240;
        portalStatic.vertex(avector3f[0].x(), avector3f[0].y(), avector3f[0].z()).color(this.red, this.green, this.blue, alphaLerp).texture(f8, f6).overlay(OverlayTexture.DEFAULT_UV).light(j).normal(matrix3f, 0.0F, -1.0F, 0.0F).next();
        portalStatic.vertex(avector3f[1].x(), avector3f[1].y(), avector3f[1].z()).color(this.red, this.green, this.blue, alphaLerp).texture(f8, f5).overlay(OverlayTexture.DEFAULT_UV).light(j).normal(matrix3f, 0.0F, -1.0F, 0.0F).next();
        portalStatic.vertex(avector3f[2].x(), avector3f[2].y(), avector3f[2].z()).color(this.red, this.green, this.blue, alphaLerp).texture(f7, f5).overlay(OverlayTexture.DEFAULT_UV).light(j).normal(matrix3f, 0.0F, -1.0F, 0.0F).next();
        portalStatic.vertex(avector3f[3].x(), avector3f[3].y(), avector3f[3].z()).color(this.red, this.green, this.blue, alphaLerp).texture(f7, f6).overlay(OverlayTexture.DEFAULT_UV).light(j).normal(matrix3f, 0.0F, -1.0F, 0.0F).next();

        VertexConsumerProvider$buffersource.draw();
    }
    @NotNull
    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.CUSTOM;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        @Override
        public Particle createParticle(DefaultParticleType typeIn, ClientWorld worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new ParticleSkulkBoom(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }
}
