package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityCrow;
import com.github.alexthe666.alexsmobs.model.ModelCrow;
import com.github.alexthe666.alexsmobs.renderer.layer.LayerCrowItem;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderCrow extends MobEntityRenderer<EntityCrow, ModelCrow> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/crow.png");

    public RenderCrow(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelCrow(), 0.2F);
        this.addFeature(new LayerCrowItem(this));
    }

    @Override
    protected void scale(EntityCrow entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }
    
    @Override
    public Identifier getTexture(EntityCrow entity) {
        return TEXTURE;
    }
}
