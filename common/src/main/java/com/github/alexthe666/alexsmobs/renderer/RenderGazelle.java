package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityGazelle;
import com.github.alexthe666.alexsmobs.model.ModelGazelle;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderGazelle extends MobEntityRenderer<EntityGazelle, ModelGazelle> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/gazelle.png");

    public RenderGazelle(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelGazelle(), 0.4F);
    }

    @Override
    protected void scale(EntityGazelle entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.8F, 0.8F, 0.8F);
    }

    @Override
    public Identifier getTexture(EntityGazelle entity) {
        return TEXTURE;
    }
}
