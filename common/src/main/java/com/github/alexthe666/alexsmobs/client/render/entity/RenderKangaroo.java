package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelKangaroo;
import com.github.alexthe666.alexsmobs.client.render.entity.layer.LayerKangarooArmor;
import com.github.alexthe666.alexsmobs.client.render.entity.layer.LayerKangarooBaby;
import com.github.alexthe666.alexsmobs.client.render.entity.layer.LayerKangarooItem;
import com.github.alexthe666.alexsmobs.entity.EntityKangaroo;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderKangaroo extends MobEntityRenderer<EntityKangaroo, ModelKangaroo> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/kangaroo.png");

    public RenderKangaroo(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelKangaroo(), 0.5F);
        this.addFeature(new LayerKangarooItem(this));
        this.addFeature(new LayerKangarooArmor(this, renderManagerIn));
        this.addFeature(new LayerKangarooBaby(this));
    }

    @Override
    public boolean shouldRender(EntityKangaroo kangaroo, Frustum p_225626_2_, double p_225626_3_, double p_225626_5_, double p_225626_7_) {
        if(kangaroo.isBaby() && kangaroo.hasVehicle() && kangaroo.getVehicle() instanceof EntityKangaroo){
            return false;
        }
        return super.shouldRender(kangaroo, p_225626_2_, p_225626_3_, p_225626_5_, p_225626_7_);
    }

    @Override
    protected void scale(EntityKangaroo entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    public Identifier getTexture(EntityKangaroo entity) {
        return TEXTURE;
    }
}
