package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelDevilsHolePupfish;
import com.github.alexthe666.alexsmobs.entity.EntityDevilsHolePupfish;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderDevilsHolePupfish extends MobEntityRenderer<EntityDevilsHolePupfish, ModelDevilsHolePupfish> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/devils_hole_pupfish.png");

    public RenderDevilsHolePupfish(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelDevilsHolePupfish(), 0.2F);
    }

    @Override
    protected void scale(EntityDevilsHolePupfish entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        float scale = entitylivingbaseIn.getPupfishScale();
        if(entitylivingbaseIn.isBaby()){
            scale *= 0.65F;
        }
        matrixStackIn.scale(scale, scale, scale);
    }

    @Override
    public Identifier getTexture(EntityDevilsHolePupfish entity) {
        return TEXTURE;
    }
}
