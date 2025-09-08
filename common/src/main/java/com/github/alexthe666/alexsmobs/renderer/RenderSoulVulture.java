package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntitySoulVulture;
import com.github.alexthe666.alexsmobs.model.ModelSoulVulture;
import com.github.alexthe666.alexsmobs.renderer.layer.LayerSoulVultureGlow;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderSoulVulture extends MobEntityRenderer<EntitySoulVulture, ModelSoulVulture> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/soul_vulture/soul_vulture.png");

    public RenderSoulVulture(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelSoulVulture(), 0.3F);
        this.addFeature(new LayerSoulVultureGlow(this));
    }

    @Override
    protected void scale(EntitySoulVulture entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
      //  matrixStackIn.scale(1.2F, 1.2F, 1.2F);
    }
    
    @Override
    public Identifier getTexture(EntitySoulVulture entity) {
        return TEXTURE;
    }
}
