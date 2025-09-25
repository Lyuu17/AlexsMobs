package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelMimicube;
import com.github.alexthe666.alexsmobs.client.model.layered.LayerMimicubeHeldItem;
import com.github.alexthe666.alexsmobs.client.model.layered.LayerMimicubeHelmet;
import com.github.alexthe666.alexsmobs.client.model.layered.LayerMimicubeTexture;
import com.github.alexthe666.alexsmobs.entity.EntityMimicube;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderMimicube extends MobEntityRenderer<EntityMimicube, ModelMimicube> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/mimicube.png");

    public RenderMimicube(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelMimicube(), 0.5F);
        this.addFeature(new LayerMimicubeHelmet(this, renderManagerIn));
        this.addFeature(new LayerMimicubeHeldItem(this));
        this.addFeature(new LayerMimicubeTexture(this));
    }

    @Override
    protected void scale(EntityMimicube entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    public Identifier getTexture(EntityMimicube entity) {
        return TEXTURE;
    }
}
