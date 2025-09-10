package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityRattlesnake;
import com.github.alexthe666.alexsmobs.model.ModelRattlesnake;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderRattlesnake extends MobEntityRenderer<EntityRattlesnake, ModelRattlesnake> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/rattlesnake.png");

    public RenderRattlesnake(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelRattlesnake(), 0.2F);
    }

    @Override
    protected void scale(EntityRattlesnake entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }
    
    @Override
    public Identifier getTexture(EntityRattlesnake entity) {
        return TEXTURE;
    }
}
