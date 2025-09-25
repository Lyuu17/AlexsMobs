package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityFroststalker;
import com.github.alexthe666.alexsmobs.client.model.ModelFroststalker;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderFroststalker extends MobEntityRenderer<EntityFroststalker, ModelFroststalker> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/froststalker.png");
    private static final Identifier TEXTURE_NOSPIKES = new Identifier("alexsmobs:textures/entity/froststalker_nospikes.png");

    public RenderFroststalker(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelFroststalker(), 0.4F);
    }

    @Override
    protected void scale(EntityFroststalker entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    public Identifier getTexture(EntityFroststalker entity) {
        return entity.hasSpikes() ? TEXTURE : TEXTURE_NOSPIKES;
    }

    @Override
    protected boolean isShaking(EntityFroststalker entity) {
        return entity.isWet() && !entity.hasSpikes();
    }
}
