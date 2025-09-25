package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityTasmanianDevil;
import com.github.alexthe666.alexsmobs.client.model.ModelTasmanianDevil;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class RenderTasmanianDevil extends MobEntityRenderer<EntityTasmanianDevil, ModelTasmanianDevil> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/tasmanian_devil.png");
    private static final Identifier TEXTURE_ANGRY = new Identifier("alexsmobs:textures/entity/tasmanian_devil_angry.png");

    public RenderTasmanianDevil(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelTasmanianDevil(), 0.3F);
    }

    @Override
    protected void scale(EntityTasmanianDevil entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @NotNull
    @Override
    public Identifier getTexture(EntityTasmanianDevil entity) {
        return entity.getAnimation() == EntityTasmanianDevil.ANIMATION_HOWL && entity.getAnimationTick() < 34 ? TEXTURE_ANGRY : TEXTURE;
    }
}