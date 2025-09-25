package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelElephant;
import com.github.alexthe666.alexsmobs.client.render.entity.layer.LayerElephantItem;
import com.github.alexthe666.alexsmobs.client.render.entity.layer.LayerElephantOverlays;
import com.github.alexthe666.alexsmobs.entity.EntityElephant;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderElephant extends MobEntityRenderer<EntityElephant, ModelElephant> {
    private static final Identifier TEXTURE_TUSK = new Identifier("alexsmobs:textures/entity/elephant/elephant_tusks.png");
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/elephant/elephant.png");

    public RenderElephant(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelElephant(0), 1.4F);
        this.addFeature(new LayerElephantOverlays(this));
        this.addFeature(new LayerElephantItem(this));
    }

    @Override
    protected void scale(EntityElephant entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
       if(entitylivingbaseIn.isTusked()){
           matrixStackIn.scale(1.1F, 1.1F, 1.1F);
       }
    }

    @Override
    public Identifier getTexture(EntityElephant entity) {
        return entity.isTusked() && !entity.isBaby() ? TEXTURE_TUSK : TEXTURE;
    }
}
