package com.github.alexthe666.alexsmobs.client.render.entity;


import com.github.alexthe666.alexsmobs.client.model.ModelSeal;
import com.github.alexthe666.alexsmobs.client.model.layered.LayerSealItem;
import com.github.alexthe666.alexsmobs.entity.EntitySeal;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;

public class RenderSeal extends MobEntityRenderer<EntitySeal, ModelSeal> {
    private static final Identifier TEXTURE_BROWN_0 = new Identifier("alexsmobs:textures/entity/seal/seal_brown_0.png");
    private static final Identifier TEXTURE_BROWN_1 = new Identifier("alexsmobs:textures/entity/seal/seal_brown_1.png");
    private static final Identifier TEXTURE_ARCTIC_0 = new Identifier("alexsmobs:textures/entity/seal/seal_arctic_0.png");
    private static final Identifier TEXTURE_ARCTIC_1 = new Identifier("alexsmobs:textures/entity/seal/seal_arctic_1.png");
    private static final Identifier TEXTURE_ARCTIC_BABY = new Identifier("alexsmobs:textures/entity/seal/seal_arctic_baby.png");
    private static final Identifier TEXTURE_TEARS = new Identifier("alexsmobs:textures/entity/seal/seal_crying.png");
    private static final Identifier TEXTURE_TONGUE = new Identifier("alexsmobs:textures/entity/seal/seal_tongue.png");

    public RenderSeal(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelSeal(), 0.45F);
        this.addFeature(new LayerSealItem(this));
        this.addFeature(new SealTearsLayer(this));
    }

    @Override
    protected boolean hasLabel(EntitySeal seal) {
        return super.hasLabel(seal) || seal.isTearsEasterEgg();
    }

    @Override
    public Identifier getTexture(EntitySeal entity) {
        if(entity.isArctic()){
            return entity.isBaby() ? TEXTURE_ARCTIC_BABY : entity.getVariant() == 1 ? TEXTURE_ARCTIC_1 : TEXTURE_ARCTIC_0;
        }
        return entity.getVariant() == 1 ? TEXTURE_BROWN_1 : TEXTURE_BROWN_0;
    }

    @Override
    protected void renderLabelIfPresent(EntitySeal seal, Text text, MatrixStack poseStack, VertexConsumerProvider bufferSrc, int numberIn) {
        if(seal.isTearsEasterEgg()){
            double d0 = this.dispatcher.getSquaredDistanceToCamera(seal);
            /*FIXME forge if (net.minecraftforge.client.ForgeHooksClient.isNameplateInRenderDistance(seal, d0))*/ {
                boolean flag = !seal.isSneaky();
                float f = seal.getHeight() + 0.5F;
                String[] split = text.asTruncatedString(512).split(" ");
                StringBuilder recombined = new StringBuilder();
                var strings = new ArrayList<String>();
                for(int wordIndex = 0; wordIndex < split.length; wordIndex++){
                    recombined.append(split[wordIndex]).append(" ");
                    if(recombined.length() > 15 || wordIndex == split.length - 1){
                        strings.add(recombined.toString());
                        recombined = new StringBuilder();
                    }
                }
                int i = 10 - 10 * strings.size();

                poseStack.push();
                poseStack.translate(0.0D, (double)f, 0.0D);
                poseStack.multiply(this.dispatcher.getRotation());
                poseStack.scale(-0.025F, -0.025F, 0.025F);
                var matrix4f = poseStack.peek().getPositionMatrix();
                float f1 = 1F;//MinecraftClient.getInstance().options.getBackgroundOpacity(1.25F);
                int j = 0XFFFFFFFF;
                var font = this.getTextRenderer();
                String widest = "";
                for(String print : strings) {
                    if(font.getWidth(widest) < font.getWidth(print)){
                        widest = print;
                    }
                }
                float widestCenter = (float)(-font.getWidth(widest) / 2);
                for(String print : strings){
                    float f2 = (float)(-font.getWidth(print) / 2);
                    poseStack.translate(0.0D, 0.0D, 0.1D);
                    font.draw(widest, widestCenter, (float)i, j, false, matrix4f, bufferSrc, TextRenderer.TextLayerType.NORMAL, j, 240);
                    poseStack.translate(0.0D, 0.0D, -0.1D);
                    font.draw(print, f2, (float)i, 1, false, matrix4f, bufferSrc, TextRenderer.TextLayerType.NORMAL, j, 240);
                    font.draw(print, f2, (float)i, 0, false, matrix4f, bufferSrc, TextRenderer.TextLayerType.NORMAL, j, 240);
                    i += 10;
                }

                poseStack.pop();
            }
        }else{
            super.renderLabelIfPresent(seal, text, poseStack, bufferSrc, numberIn);
        }
    }

    static class SealTearsLayer extends FeatureRenderer<EntitySeal, ModelSeal> {

        public SealTearsLayer(RenderSeal p_i50928_1_) {
            super(p_i50928_1_);
        }

        @Override
        public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntitySeal entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if(entitylivingbaseIn.isTearsEasterEgg()){
                var lead = bufferIn.getBuffer(AMRenderLayers.getEntityCutoutNoCull(TEXTURE_TEARS));
                this.getContextModel().render(matrixStackIn, lead, packedLightIn, LivingEntityRenderer.getOverlay(entitylivingbaseIn, 0), 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }
}
