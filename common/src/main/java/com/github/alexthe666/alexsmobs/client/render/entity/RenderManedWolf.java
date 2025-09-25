package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelManedWolf;
import com.github.alexthe666.alexsmobs.entity.EntityManedWolf;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderManedWolf extends MobEntityRenderer<EntityManedWolf, ModelManedWolf> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/maned_wolf.png");
    private static final Identifier TEXTURE_ENDER = new Identifier("alexsmobs:textures/entity/maned_wolf_ender.png");

    public RenderManedWolf(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelManedWolf(), 0.45F);
    }

    @Override
    protected void scale(EntityManedWolf entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.85F, 0.85F, 0.85F);
    }

    @Override
    public Identifier getTexture(EntityManedWolf entity) {
        return entity.isEnder() ? TEXTURE_ENDER : TEXTURE;
    }
}
