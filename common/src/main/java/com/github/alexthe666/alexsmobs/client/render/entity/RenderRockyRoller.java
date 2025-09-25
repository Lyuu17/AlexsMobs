package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelRockyRoller;
import com.github.alexthe666.alexsmobs.entity.EntityRockyRoller;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderRockyRoller extends MobEntityRenderer<EntityRockyRoller, ModelRockyRoller> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/rocky_roller.png");
    private static final Identifier TEXTURE_ANGRY = new Identifier("alexsmobs:textures/entity/rocky_roller_angry.png");
    private static final Identifier TEXTURE_ROLLING = new Identifier("alexsmobs:textures/entity/rocky_roller_rolling.png");

    public RenderRockyRoller(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelRockyRoller(), 0.7F);
    }

    @Override
    protected void scale(EntityRockyRoller entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    public Identifier getTexture(EntityRockyRoller entity) {
        return entity.isRolling() ? TEXTURE_ROLLING : entity.isAngry() ? TEXTURE_ANGRY : TEXTURE;
    }
}
