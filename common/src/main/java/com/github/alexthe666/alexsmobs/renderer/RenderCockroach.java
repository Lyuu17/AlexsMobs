package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityCockroach;
import com.github.alexthe666.alexsmobs.model.ModelCockroach;
import com.github.alexthe666.alexsmobs.renderer.layer.LayerCockroachMaracas;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderCockroach extends MobEntityRenderer<EntityCockroach, ModelCockroach> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/cockroach.png");

    public RenderCockroach(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelCockroach(), 0.3F);
        this.addFeature(new LayerCockroachMaracas(this, renderManagerIn));
    }

    @Override
    protected void scale(EntityCockroach entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.85F, 0.85F, 0.85F);
    }

    @Override
    public Identifier getTexture(EntityCockroach entity) {
        return TEXTURE;
    }
}
