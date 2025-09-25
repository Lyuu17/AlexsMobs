package com.github.alexthe666.alexsmobs.client.render.entity.layer;

import com.github.alexthe666.alexsmobs.client.model.ModelVoidWormBody;
import com.github.alexthe666.alexsmobs.client.model.ModelVoidWormTail;
import com.github.alexthe666.alexsmobs.client.render.entity.AMRenderLayers;
import com.github.alexthe666.alexsmobs.client.render.entity.misc.VoidWormMetadataSection;
import com.github.alexthe666.alexsmobs.entity.EntityVoidWormPart;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public abstract class LayerVoidWormGlow<T extends LivingEntity> extends FeatureRenderer<T, EntityModel<T>> {

    private final ResourceManager resourceManager;
    private final Object2BooleanMap<Identifier> mcmetaData;
    private EntityModel<T> layerModel;
    private final EntityModel bodyModel = new ModelVoidWormBody(1.001F);
    private final EntityModel tailModel = new ModelVoidWormTail(1.001F);

    public LayerVoidWormGlow(FeatureRendererContext<T, EntityModel<T>> renderer, ResourceManager resourceManager, EntityModel<T> layerModel) {
        super(renderer);
        this.resourceManager = resourceManager;
        this.mcmetaData = new Object2BooleanOpenHashMap<>();
        this.layerModel = layerModel;
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, T worm, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        Identifier texture = getGlowTexture(worm);
        boolean special = isSpecialRenderer(texture);

        if (isGlowing(worm) || special) {
            if(special){
                if(worm instanceof EntityVoidWormPart body){
                    this.layerModel = body.isTail() ? tailModel : bodyModel;
                }
                this.layerModel.setAngles(worm, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                VertexConsumer consumer = AMRenderLayers.createMergedVertexConsumer(bufferIn.getBuffer(AMRenderLayers.VOID_WORM_PORTAL_OVERLAY), bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(texture)));
                this.layerModel.render(matrixStackIn, consumer, 240, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
            }else{
                float f = getAlpha(worm);
                this.getContextModel().render(matrixStackIn, bufferIn.getBuffer(RenderLayer.getEyes(texture)), 240, LivingEntityRenderer.getOverlay(worm, 1.0F), 1.0F, 1.0F, 1.0F, f);
            }
        }
    }

    public abstract Identifier getGlowTexture(LivingEntity worm);

    public abstract boolean isGlowing(LivingEntity livingEntity);
    
    public abstract float getAlpha(LivingEntity livingEntity);

    private boolean isSpecialRenderer(Identifier Identifier){
        if(mcmetaData.containsKey(Identifier)){
            return mcmetaData.getBoolean(Identifier);
        }
        if(this.resourceManager.getResource(Identifier).isPresent()){
            var resource = this.resourceManager.getResource(Identifier).get();
            try {
                var section = resource.getMetadata().decode(VoidWormMetadataSection.SERIALIZER).orElse(new VoidWormMetadataSection());
                mcmetaData.put(Identifier, section.isEndPortalTexture());
                return section.isEndPortalTexture();
            } catch (Exception e) {
                e.printStackTrace();
                mcmetaData.put(Identifier, false);
                return false;
            }
        }
        mcmetaData.put(Identifier, false);
        return false;
    }

}
