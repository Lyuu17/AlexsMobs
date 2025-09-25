package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelMantisShrimp;
import com.github.alexthe666.alexsmobs.client.model.layered.LayerMantisShrimpItem;
import com.github.alexthe666.alexsmobs.entity.EntityMantisShrimp;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderMantisShrimp extends MobEntityRenderer<EntityMantisShrimp, ModelMantisShrimp> {
    private static final Identifier TEXTURE_0 = new Identifier("alexsmobs:textures/entity/mantis_shrimp_0.png");
    private static final Identifier TEXTURE_1 = new Identifier("alexsmobs:textures/entity/mantis_shrimp_1.png");
    private static final Identifier TEXTURE_2 = new Identifier("alexsmobs:textures/entity/mantis_shrimp_2.png");
    private static final Identifier TEXTURE_3 = new Identifier("alexsmobs:textures/entity/mantis_shrimp_3.png");

    public RenderMantisShrimp(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelMantisShrimp(), 0.6F);
        this.addFeature(new LayerMantisShrimpItem(this));
    }

    @Override
    protected void scale(EntityMantisShrimp entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.8F, 0.8F, 0.8F);
    }

    @Override
    public Identifier getTexture(EntityMantisShrimp entity) {
        return entity.getVariant() == 3 ? TEXTURE_3 : entity.getVariant() == 2 ? TEXTURE_2 : entity.getVariant() == 1 ? TEXTURE_1 : TEXTURE_0;
    }
}
