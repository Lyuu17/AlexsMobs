package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityBoneSerpent;
import com.github.alexthe666.alexsmobs.model.ModelBoneSerpentHead;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderBoneSerpent extends MobEntityRenderer<EntityBoneSerpent, ModelBoneSerpentHead> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/bone_serpent_head.png");

    public RenderBoneSerpent(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelBoneSerpentHead(), 0.3F);
    }

    @Override
    protected void scale(EntityBoneSerpent entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
      //  matrixStackIn.scale(1.2F, 1.2F, 1.2F);
    }

    @Override
    public Identifier getTexture(EntityBoneSerpent entity) {
        return TEXTURE;
    }
}
