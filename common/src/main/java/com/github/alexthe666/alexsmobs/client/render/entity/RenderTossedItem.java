package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityTossedItem;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.client.model.ModelAncientDart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

public class RenderTossedItem  extends EntityRenderer<EntityTossedItem> {
    public static final Identifier DART_TEXTURE = new Identifier("alexsmobs:textures/entity/ancient_dart.png");
    public static final ModelAncientDart DART_MODEL = new ModelAncientDart();

    public RenderTossedItem(EntityRendererFactory.Context renderManager) {
        super(renderManager);
    }

    @NotNull
    @Override
    public Identifier getTexture(EntityTossedItem entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(EntityTossedItem entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn) {
        matrixStackIn.push();
        if(entityIn.isDart()){
            matrixStackIn.translate(0.0D, -0.15F, 0.0D);
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevYaw, entityIn.getYaw()) - 180F));
            matrixStackIn.push();
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevPitch, entityIn.getPitch())));
            matrixStackIn.translate(0, 0.5F, 0);
            matrixStackIn.scale(1F, 1F, 1F);
            var ivertexbuilder = bufferIn.getBuffer(DART_MODEL.getLayer(DART_TEXTURE));
            DART_MODEL.render(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
            matrixStackIn.pop();
        }else{
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevYaw, entityIn.getYaw()) - 90.0F));
            matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevPitch, entityIn.getPitch())));
            matrixStackIn.translate(0, 0.5F, 0);
            matrixStackIn.scale(1F, 1F, 1F);
            matrixStackIn.multiply((new Quaternionf()).rotateZ(Maths.rad(-(entityIn.age + partialTicks) * 30F)));
            matrixStackIn.translate(0, -0.15F, 0);
            MinecraftClient.getInstance().getItemRenderer().renderItem(entityIn.getStack(), ModelTransformationMode.GROUND, packedLightIn, OverlayTexture.DEFAULT_UV, matrixStackIn, bufferIn, entityIn.getWorld(), 0);
        }
        matrixStackIn.pop();
    }

}
