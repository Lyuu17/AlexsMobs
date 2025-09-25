package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelShoebill;
import com.github.alexthe666.alexsmobs.entity.EntityShoebill;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderShoebill extends MobEntityRenderer<EntityShoebill, ModelShoebill> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/shoebill.png");

    public RenderShoebill(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelShoebill(), 0.3F);
    }

    @Override
    protected void scale(EntityShoebill entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    public Identifier getTexture(EntityShoebill entity) {
        return TEXTURE;
    }
}
