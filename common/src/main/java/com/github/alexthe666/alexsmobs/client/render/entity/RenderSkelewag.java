package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelSkelewag;
import com.github.alexthe666.alexsmobs.entity.EntitySkelewag;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class RenderSkelewag extends MobEntityRenderer<EntitySkelewag, ModelSkelewag> {
    private static final Identifier TEXTURE_0 = new Identifier("alexsmobs:textures/entity/skelewag_0.png");
    private static final Identifier TEXTURE_1 = new Identifier("alexsmobs:textures/entity/skelewag_1.png");

    public RenderSkelewag(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelSkelewag(), 0.5F);
    }

    @Override
    protected void scale(EntitySkelewag entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    protected int getBlockLight(EntitySkelewag entityIn, BlockPos partialTicks) {
        return Math.max(2, super.getBlockLight(entityIn, partialTicks));
    }

    @Override
    public Identifier getTexture(EntitySkelewag entity) {
        return entity.getVariant() == 1 ? TEXTURE_1 : TEXTURE_0;
    }
}
