package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityCosmicCod;
import com.github.alexthe666.alexsmobs.model.ModelCosmicCod;
import com.github.alexthe666.alexsmobs.renderer.layer.LayerBasicGlow;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class RenderCosmicCod extends MobEntityRenderer<EntityCosmicCod, net.minecraft.client.render.entity.model.EntityModel<EntityCosmicCod>> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/cosmic_cod.png");
    private static final Identifier TEXTURE_EYES = new Identifier("alexsmobs:textures/entity/cosmic_cod_eyes.png");

    public RenderCosmicCod(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelCosmicCod(), 0.25F);
        this.addFeature(new LayerBasicGlow<>(this, TEXTURE_EYES));
    }

    @Override
    public Identifier getTexture(EntityCosmicCod entity) {
        return TEXTURE;
    }
}

