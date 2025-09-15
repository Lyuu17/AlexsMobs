package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityStradpole;
import com.github.alexthe666.alexsmobs.model.ModelStradpole;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderStradpole extends MobEntityRenderer<EntityStradpole, ModelStradpole> {
    public static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/stradpole.png");

    public RenderStradpole(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelStradpole(), 0.25F);
    }

    @Override
    protected void scale(EntityStradpole entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        //matrixStackIn.scale(0.8F, 0.8F, 0.8F);
    }

    @Override
    public Identifier getTexture(EntityStradpole entity) {
        return TEXTURE;
    }
}
