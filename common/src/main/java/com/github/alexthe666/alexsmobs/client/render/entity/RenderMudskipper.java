package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelMudskipper;
import com.github.alexthe666.alexsmobs.entity.EntityMudskipper;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderMudskipper extends MobEntityRenderer<EntityMudskipper, ModelMudskipper> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/mudskipper.png");
    private static final Identifier TEXTURE_SPIT = new Identifier("alexsmobs:textures/entity/mudskipper_spit.png");

    public RenderMudskipper(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelMudskipper(), 0.25F);
    }

    @Override
    protected void scale(EntityMudskipper rabbit, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    public Identifier getTexture(EntityMudskipper entity) {
        return entity.isMouthOpen() ? TEXTURE_SPIT : TEXTURE;
    }
}
