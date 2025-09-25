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
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ParticleStaticSpark extends Particle {
    private static final Identifier[] TEXTURES = new Identifier[]{
            new Identifier("textures/particle/generic_0.png"),
            new Identifier("textures/particle/generic_1.png"),
            new Identifier("textures/particle/generic_2.png"),
            new Identifier("textures/particle/generic_3.png"),
            new Identifier("textures/particle/generic_4.png"),
            new Identifier("textures/particle/generic_5.png"),
            new Identifier("textures/particle/generic_6.png"),
            new Identifier("textures/particle/generic_7.png")
    };
    private int decrement = 1;
    private int textureIndex = 0;
    private float size;

    private ParticleStaticSpark(ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        super(world, x, y, z);
        this.setBoundingBoxSpacing(1, 1);
        this.gravityStrength = 0.0F;
        this.velocityX = motionX;
        this.velocityY = motionY;
        this.velocityZ = motionZ;
        this.maxAge = 10 + this.random.nextInt(15);
        this.textureIndex = MathHelper.clamp(random.nextInt(8), 0, 7);
        this.decrement = this.textureIndex > 0 ? this.maxAge / this.textureIndex : this.maxAge;
        this.size = this.random.nextFloat() * 0.2F + 0.2F;
    }

    @Override
    public void tick(){
        super.tick();
        this.velocityX *= 0.97D;
        this.velocityY *= 0.97D;
        this.velocityZ *= 0.97D;
        if(this.textureIndex > 0){
            if(age % decrement == 0){
                textureIndex--;
            }
        }
        if(this.size > 0.2F){
            this.size -= 0.015F;
        }
    }

    @Override
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float partialTick) {
        var vec3 = camera.getPos();
        float f = (float)(MathHelper.lerp(partialTick, this.prevPosX, this.x) - vec3.x);
        float f1 = (float)(MathHelper.lerp(partialTick, this.prevPosY, this.y) - vec3.y);
        float f2 = (float)(MathHelper.lerp(partialTick, this.prevPosZ, this.z) - vec3.z);
        Quaternionf quaternion;
        if (this.angle == 0.0F) {
            quaternion = camera.getRotation();
        } else {
            quaternion = new Quaternionf(camera.getRotation());
            float f3 = MathHelper.lerp(partialTick, this.prevAngle, this.angle);
            quaternion.mul(RotationAxis.POSITIVE_Z.rotation(f3));
        }
        var VertexConsumerProvider$buffersource = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        var portalStatic = AMRenderLayers.createMergedVertexConsumer(VertexConsumerProvider$buffersource.getBuffer(AMRenderLayers.STATIC_PARTICLE), VertexConsumerProvider$buffersource.getBuffer(RenderLayer.getEntityTranslucent(TEXTURES[textureIndex])));
        var posestack = new MatrixStack();
        var posestack$pose = posestack.peek();
        //Matrix4f matrix4f = posestack$pose.pose();
        var matrix3f = posestack$pose.getNormalMatrix();

        Vector3f vector3f1 = new Vector3f(-1.0F, -1.0F, 0.0F);
        vector3f1.rotate(quaternion);
        Vector3f[] avector3f = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
        float f4 = size;

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
        portalStatic.vertex(avector3f[0].x(), avector3f[0].y(), avector3f[0].z()).color(this.red, this.green, this.blue, this.alpha).texture(f8, f6).overlay(OverlayTexture.DEFAULT_UV).light(j).normal(matrix3f, 0.0F, -1.0F, 0.0F).next();
        portalStatic.vertex(avector3f[1].x(), avector3f[1].y(), avector3f[1].z()).color(this.red, this.green, this.blue, this.alpha).texture(f8, f5).overlay(OverlayTexture.DEFAULT_UV).light(j).normal(matrix3f, 0.0F, -1.0F, 0.0F).next();
        portalStatic.vertex(avector3f[2].x(), avector3f[2].y(), avector3f[2].z()).color(this.red, this.green, this.blue, this.alpha).texture(f7, f5).overlay(OverlayTexture.DEFAULT_UV).light(j).normal(matrix3f, 0.0F, -1.0F, 0.0F).next();
        portalStatic.vertex(avector3f[3].x(), avector3f[3].y(), avector3f[3].z()).color(this.red, this.green, this.blue, this.alpha).texture(f7, f6).overlay(OverlayTexture.DEFAULT_UV).light(j).normal(matrix3f, 0.0F, -1.0F, 0.0F).next();

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
            return new ParticleStaticSpark(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }
}
