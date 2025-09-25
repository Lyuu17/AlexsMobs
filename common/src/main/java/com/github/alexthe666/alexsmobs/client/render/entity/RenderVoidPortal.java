package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityVoidPortal;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RenderVoidPortal extends EntityRenderer<EntityVoidPortal> {
    private static final Identifier TEXTURE_0 = new Identifier("alexsmobs:textures/entity/void_worm/portal/portal_idle_0.png");
    private static final Identifier TEXTURE_1 = new Identifier("alexsmobs:textures/entity/void_worm/portal/portal_idle_1.png");
    private static final Identifier TEXTURE_2 = new Identifier("alexsmobs:textures/entity/void_worm/portal/portal_idle_2.png");
    private static final Identifier TEXTURE_SHATTERED_0 = new Identifier("alexsmobs:textures/entity/void_worm/portal/shattered/portal_idle_0.png");
    private static final Identifier TEXTURE_SHATTERED_1 = new Identifier("alexsmobs:textures/entity/void_worm/portal/shattered/portal_idle_1.png");
    private static final Identifier TEXTURE_SHATTERED_2 = new Identifier("alexsmobs:textures/entity/void_worm/portal/shattered/portal_idle_2.png");
    private static final Identifier[] TEXTURE_PROGRESS = new Identifier[10];
    private static final Identifier[] TEXTURE_SHATTERED_PROGRESS = new Identifier[10];
    public RenderVoidPortal(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn);
        for(int i = 0; i < 10; i++){
            TEXTURE_PROGRESS[i] = new Identifier("alexsmobs:textures/entity/void_worm/portal/portal_grow_" + i + ".png");
            TEXTURE_SHATTERED_PROGRESS[i] = new Identifier("alexsmobs:textures/entity/void_worm/portal/shattered/portal_grow_" + i + ".png");
        }
    }

    @Override
    public void render(EntityVoidPortal entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
        matrixStackIn.push();
        matrixStackIn.multiply(entityIn.getAttachmentFacing().getOpposite().getRotationQuaternion());
        matrixStackIn.translate(0.5D, 0, 0.5D);
        matrixStackIn.scale(2F, 2F, 2F);
        renderPortal(entityIn, matrixStackIn, bufferIn, false);
        if(entityIn.isShattered()){
            float off = 0.01F;
            matrixStackIn.push();
            matrixStackIn.translate(0F, off, 0F);
            renderPortal(entityIn, matrixStackIn, bufferIn, true);
            matrixStackIn.pop();
            matrixStackIn.push();
            matrixStackIn.translate(0F, -off, 0F);
            renderPortal(entityIn, matrixStackIn, bufferIn, true);
            matrixStackIn.pop();
        }
        matrixStackIn.pop();
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }

    private void renderPortal(EntityVoidPortal entityIn, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, boolean shattered){
        Identifier tex;
        if(entityIn.getLifespan() < 20){
            tex = getGrowingTexture((int) ((entityIn.getLifespan() * 0.5F) % 10), shattered);
        }else if(entityIn.age < 20){
            tex = getGrowingTexture((int) ((entityIn.age * 0.5F) % 10), shattered);
        }else{
            tex = getIdleTexture(entityIn.age % 9, shattered);
        }
        VertexConsumer ivertexbuilder = shattered ? AMRenderLayers.createMergedVertexConsumer(bufferIn.getBuffer(AMRenderLayers.STATIC_PORTAL), bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(tex))) : bufferIn.getBuffer(AMRenderLayers.getFullBright(tex));
        renderArc(matrixStackIn, ivertexbuilder);
    }

    private void renderArc(MatrixStack matrixStackIn, VertexConsumer ivertexbuilder) {
        matrixStackIn.push();
        MatrixStack.Entry lvt_19_1_ = matrixStackIn.peek();
        Matrix4f lvt_20_1_ = lvt_19_1_.getPositionMatrix();
        Matrix3f lvt_21_1_ = lvt_19_1_.getNormalMatrix();
        this.drawVertex(lvt_20_1_, lvt_21_1_, ivertexbuilder, -1, 0, -1, 0, 0, 1, 0, 1, 240);
        this.drawVertex(lvt_20_1_, lvt_21_1_, ivertexbuilder, -1, 0, 1, 0, 1, 1, 0, 1, 240);
        this.drawVertex(lvt_20_1_, lvt_21_1_, ivertexbuilder, 1, 0, 1, 1, 1, 1, 0, 1, 240);
        this.drawVertex(lvt_20_1_, lvt_21_1_, ivertexbuilder, 1, 0, -1, 1, 0, 1, 0, 1, 240);
        matrixStackIn.pop();
    }

    @NotNull
    @Override
    public Identifier getTexture(EntityVoidPortal entity) {
        return TEXTURE_0;
    }

    public void drawVertex(Matrix4f p_229039_1_, Matrix3f p_229039_2_, VertexConsumer p_229039_3_, int p_229039_4_, int p_229039_5_, int p_229039_6_, float p_229039_7_, float p_229039_8_, int p_229039_9_, int p_229039_10_, int p_229039_11_, int p_229039_12_) {
        p_229039_3_.vertex(p_229039_1_, (float) p_229039_4_, (float) p_229039_5_, (float) p_229039_6_).color(255, 255, 255, 255).texture(p_229039_7_, p_229039_8_).overlay(OverlayTexture.DEFAULT_UV).light(p_229039_12_).normal(p_229039_2_, (float) p_229039_9_, (float) p_229039_11_, (float) p_229039_10_).next();
    }

    public Identifier getIdleTexture(int age, boolean shattered) {
        if (age < 3) {
            return shattered ? TEXTURE_SHATTERED_0 : TEXTURE_0;
        } else if (age < 6) {
            return shattered ? TEXTURE_SHATTERED_1 : TEXTURE_1;
        } else if (age < 10) {
            return shattered ? TEXTURE_SHATTERED_2 : TEXTURE_2;
        } else {
            return shattered ? TEXTURE_SHATTERED_0 : TEXTURE_0;
        }
    }

    public Identifier getGrowingTexture(int age, boolean shattered) {
        return shattered ? TEXTURE_SHATTERED_PROGRESS[MathHelper.clamp(age, 0, 9)] : TEXTURE_PROGRESS[MathHelper.clamp(age, 0, 9)];
    }
}
