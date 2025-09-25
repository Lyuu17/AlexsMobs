package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityTriops;
import com.github.alexthe666.alexsmobs.client.model.ModelTriops;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class RenderTriops extends MobEntityRenderer<EntityTriops, ModelTriops> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/triops.png");

    public RenderTriops(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelTriops(), 0.2F);
    }

    @Override
    protected void scale(EntityTriops entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        float scale = entitylivingbaseIn.getTriopsScale();
        if(entitylivingbaseIn.isBaby()){
            scale *= 0.65F;
        }
        matrixStackIn.scale(scale, scale, scale);
    }

    @NotNull
    @Override
    public Identifier getTexture(EntityTriops entity) {
        return TEXTURE;
    }
}
