package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityFlyingFish;
import com.github.alexthe666.alexsmobs.client.model.ModelFlyingFish;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderFlyingFish extends MobEntityRenderer<EntityFlyingFish, ModelFlyingFish> {
    private static final Identifier TEXTURE_0 = new Identifier("alexsmobs:textures/entity/flying_fish_0.png");
    private static final Identifier TEXTURE_1 = new Identifier("alexsmobs:textures/entity/flying_fish_1.png");
    private static final Identifier TEXTURE_2 = new Identifier("alexsmobs:textures/entity/flying_fish_2.png");

    public RenderFlyingFish(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelFlyingFish(), 0.2F);
    }

    @Override
    protected void scale(EntityFlyingFish entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.8F, 0.8F, 0.8F);
    }

    @Override
    public Identifier getTexture(EntityFlyingFish entity) {
        return switch (entity.getVariant()) {
            case 0 -> TEXTURE_0;
            case 1 -> TEXTURE_1;
            case 2 -> TEXTURE_2;
            default -> TEXTURE_0;
        };
    }
}
