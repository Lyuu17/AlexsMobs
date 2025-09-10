package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntitySeaBear;
import com.github.alexthe666.alexsmobs.model.ModelSeaBear;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderSeaBear extends MobEntityRenderer<EntitySeaBear, ModelSeaBear> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/sea_bear.png");

    public RenderSeaBear(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelSeaBear(), 1.2F);
    }

    @Override
    protected void scale(EntitySeaBear entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    public Identifier getTexture(EntitySeaBear entity) {
        return TEXTURE;
    }
}
