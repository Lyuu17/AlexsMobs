package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityHummingbird;
import com.github.alexthe666.alexsmobs.client.model.ModelHummingbird;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderHummingbird extends MobEntityRenderer<EntityHummingbird, ModelHummingbird> {
    private static final Identifier TEXTURE_0 = new Identifier("alexsmobs:textures/entity/hummingbird_0.png");
    private static final Identifier TEXTURE_1 = new Identifier("alexsmobs:textures/entity/hummingbird_1.png");
    private static final Identifier TEXTURE_2 = new Identifier("alexsmobs:textures/entity/hummingbird_2.png");

    public RenderHummingbird(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelHummingbird(), 0.15F);
    }

    @Override
    protected void scale(EntityHummingbird entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.75F, 0.75F, 0.75F);
    }
    
    @Override
    public Identifier getTexture(EntityHummingbird entity) {
        return entity.getVariant() == 0 ? TEXTURE_0 : entity.getVariant() == 1 ? TEXTURE_1 : TEXTURE_2;
    }
}
