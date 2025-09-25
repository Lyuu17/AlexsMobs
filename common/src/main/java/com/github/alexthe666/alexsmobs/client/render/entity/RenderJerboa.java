package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityJerboa;
import com.github.alexthe666.alexsmobs.client.model.ModelJerboa;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderJerboa extends MobEntityRenderer<EntityJerboa, ModelJerboa> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/jerboa.png");
    private static final Identifier TEXTURE_SLEEPING = new Identifier("alexsmobs:textures/entity/jerboa_sleeping.png");

    public RenderJerboa(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelJerboa(), 0.1F);
    }

    @Override
    protected void scale(EntityJerboa entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.8F, 0.8F, 0.8F);
    }

    @Override
    public Identifier getTexture(EntityJerboa entity) {
        return entity.isSleeping() ? TEXTURE_SLEEPING : TEXTURE;
    }
}
