package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntitySnowLeopard;
import com.github.alexthe666.alexsmobs.client.model.ModelSnowLeopard;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderSnowLeopard extends MobEntityRenderer<EntitySnowLeopard, ModelSnowLeopard> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/snow_leopard.png");
    private static final Identifier TEXTURE_SLEEPING = new Identifier("alexsmobs:textures/entity/snow_leopard_sleeping.png");

    public RenderSnowLeopard(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelSnowLeopard(), 0.4F);
    }

    @Override
    protected void scale(EntitySnowLeopard entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.9F,0.9F, 0.9F);
    }
    
    @Override
    public Identifier getTexture(EntitySnowLeopard entity) {
        return entity.isSleeping() ? TEXTURE_SLEEPING : TEXTURE;
    }
}
