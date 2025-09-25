package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelRainFrog;
import com.github.alexthe666.alexsmobs.entity.EntityRainFrog;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderRainFrog extends MobEntityRenderer<EntityRainFrog, ModelRainFrog> {
    private static final Identifier TEXTURE_0 = new Identifier("alexsmobs:textures/entity/rain_frog_0.png");
    private static final Identifier TEXTURE_1 = new Identifier("alexsmobs:textures/entity/rain_frog_1.png");
    private static final Identifier TEXTURE_2 = new Identifier("alexsmobs:textures/entity/rain_frog_2.png");

    public RenderRainFrog(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelRainFrog(), 0.2F);
    }

    @Override
    protected void scale(EntityRainFrog entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.9F, 0.9F, 0.9F);
    }

    @Override
    public Identifier getTexture(EntityRainFrog entity) {
        return entity.getVariant() == 2 ? TEXTURE_2 : entity.getVariant() == 1 ? TEXTURE_1 : TEXTURE_0;
    }
}
