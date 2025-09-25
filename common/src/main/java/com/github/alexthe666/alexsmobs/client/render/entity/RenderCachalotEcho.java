package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityCachalotEcho;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RenderCachalotEcho extends EntityRenderer<EntityCachalotEcho> {
    private static final Identifier TEXTURE_0 = new Identifier("alexsmobs:textures/entity/cachalot/whale_echo_0.png");
    private static final Identifier TEXTURE_1 = new Identifier("alexsmobs:textures/entity/cachalot/whale_echo_1.png");
    private static final Identifier TEXTURE_2 = new Identifier("alexsmobs:textures/entity/cachalot/whale_echo_2.png");
    private static final Identifier TEXTURE_3 = new Identifier("alexsmobs:textures/entity/cachalot/whale_echo_3.png");
    private static final Identifier GREEN_TEXTURE_0 = new Identifier("alexsmobs:textures/entity/cachalot/whale_echo_0_green.png");
    private static final Identifier GREEN_TEXTURE_1 = new Identifier("alexsmobs:textures/entity/cachalot/whale_echo_1_green.png");
    private static final Identifier GREEN_TEXTURE_2 = new Identifier("alexsmobs:textures/entity/cachalot/whale_echo_2_green.png");
    private static final Identifier GREEN_TEXTURE_3 = new Identifier("alexsmobs:textures/entity/cachalot/whale_echo_3_green.png");

    public RenderCachalotEcho(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn);
    }

    @Override
    public void render(EntityCachalotEcho entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
        matrixStackIn.push();
        matrixStackIn.translate(0.0D, 0.25F, 0.0D);
        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevYaw, entityIn.getYaw()) - 90.0F));
        matrixStackIn.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevPitch, entityIn.getPitch())));
        int arcs = MathHelper.clamp(MathHelper.floor(entityIn.age / 5F), 1, 4);
        matrixStackIn.translate(0.0D, 0.0F, 0.4D);
        for(int i = 0; i < arcs; i++){
            matrixStackIn.push();
            matrixStackIn.translate(0, 0, -0.5F * i);
            renderArc(matrixStackIn, bufferIn, (i + 1) * 5, entityIn.isFasterAnimation(), entityIn.isGreen());
            matrixStackIn.pop();
        }
        matrixStackIn.pop();
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }

    private void renderArc(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int age, boolean fast, boolean green) {
        matrixStackIn.push();
        Identifier res;
        if(fast){
            res = getEntityTextureFaster(age, green);
        }else{
            res = getEntityTexture(age);
        }
        var ivertexbuilder = bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(res));
        var lvt_19_1_ = matrixStackIn.peek();
        var lvt_20_1_ = lvt_19_1_.getPositionMatrix();
        var lvt_21_1_ = lvt_19_1_.getNormalMatrix();
        this.drawVertex(lvt_20_1_, lvt_21_1_, ivertexbuilder, -1, 0, -1, 0, 0, 1, 0, 1, 240);
        this.drawVertex(lvt_20_1_, lvt_21_1_, ivertexbuilder, -1, 0, 1, 0, 1, 1, 0, 1, 240);
        this.drawVertex(lvt_20_1_, lvt_21_1_, ivertexbuilder, 1, 0, 1, 1, 1, 1, 0, 1, 240);
        this.drawVertex(lvt_20_1_, lvt_21_1_, ivertexbuilder, 1, 0, -1, 1, 0, 1, 0, 1, 240);
        matrixStackIn.pop();
    }

    @Override
    public Identifier getTexture(EntityCachalotEcho entity) {
        return TEXTURE_0;
    }

    public void drawVertex(Matrix4f p_229039_1_, Matrix3f p_229039_2_, VertexConsumer p_229039_3_, int p_229039_4_, int p_229039_5_, int p_229039_6_, float p_229039_7_, float p_229039_8_, int p_229039_9_, int p_229039_10_, int p_229039_11_, int p_229039_12_) {
        p_229039_3_
                .vertex(p_229039_1_, (float) p_229039_4_, (float) p_229039_5_, (float) p_229039_6_).color(255, 255, 255, 255)
                .texture(p_229039_7_, p_229039_8_)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(p_229039_12_)
                .normal(p_229039_2_, (float) p_229039_9_, (float) p_229039_11_, (float) p_229039_10_)
                .next();
    }

    /**
     * Returns the location of an entity's texture.
     */
    public Identifier getEntityTexture(int age) {
        if (age < 5) {
            return TEXTURE_0;
        } else if (age < 10) {
            return TEXTURE_1;
        } else if (age < 15) {
            return TEXTURE_2;
        } else {
            return TEXTURE_3;
        }
    }

    public Identifier getEntityTextureFaster(int age, boolean green) {
        if (age < 3) {
            return green ? GREEN_TEXTURE_0 : TEXTURE_0;
        } else if (age < 6) {
            return green ? GREEN_TEXTURE_1 : TEXTURE_1;
        } else if (age < 9) {
            return green ? GREEN_TEXTURE_2 : TEXTURE_2;
        } else {
            return green ? GREEN_TEXTURE_3 : TEXTURE_3;
        }
    }
}
