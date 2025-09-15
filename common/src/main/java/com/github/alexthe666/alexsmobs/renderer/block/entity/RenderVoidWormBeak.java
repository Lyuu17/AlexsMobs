package com.github.alexthe666.alexsmobs.renderer.block.entity;

import com.github.alexthe666.alexsmobs.block.VoidWormBeakBlock;
import com.github.alexthe666.alexsmobs.block.entity.VoidWormBeakBlockEntity;
import com.github.alexthe666.alexsmobs.model.ModelVoidWormBeak;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderVoidWormBeak<T extends VoidWormBeakBlockEntity> implements BlockEntityRenderer<T> {

    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/void_worm/void_worm_beak.png");
    private static final ModelVoidWormBeak HEAD_MODEL = new ModelVoidWormBeak();

    public RenderVoidWormBeak(BlockEntityRendererFactory.Context rendererDispatcherIn) {
    }

    @Override
    public void render(T tileEntityIn, float partialTicks, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int combinedLightIn, int combinedOverlayIn) {
        matrixStackIn.push();
        var dir = tileEntityIn.getCachedState().get(VoidWormBeakBlock.FACING);
        switch (dir) {
            case UP -> matrixStackIn.translate(0.5F, 1.5F, 0.5F);
            case DOWN -> matrixStackIn.translate(0.5F, -0.5F, 0.5F);
            case NORTH -> matrixStackIn.translate(0.5, 0.5F, -0.5F);
            case EAST -> matrixStackIn.translate(1.5F, 0.5F, 0.5F);
            case SOUTH -> matrixStackIn.translate(0.5, 0.5F, 1.5F);
            case WEST -> matrixStackIn.translate(-0.5F, 0.5F, 0.5F);
        }
        matrixStackIn.multiply(dir.getOpposite().getRotationQuaternion());
        matrixStackIn.push();
        matrixStackIn.translate(0, -0.01F, 0.0F);
        HEAD_MODEL.renderBeak(tileEntityIn, partialTicks);
        HEAD_MODEL.render(matrixStackIn, bufferIn.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)), combinedLightIn, combinedOverlayIn, 1, 1F, 1, 1);
        matrixStackIn.pop();
        matrixStackIn.pop();
    }
}
