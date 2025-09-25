package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityFly;
import com.github.alexthe666.alexsmobs.client.model.ModelFly;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderFly extends MobEntityRenderer<EntityFly, ModelFly> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/fly.png");

    public RenderFly(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelFly(), 0.2F);
    }

    @Override
    protected void scale(EntityFly entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    protected boolean isShaking(EntityFly fly) {
        return fly.isInNether();
    }

    @Override
    protected void setupTransforms(EntityFly entityLiving, MatrixStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks) {
        if (this.isShaking(entityLiving)) {
            rotationYaw += (float)(Math.cos((double)entityLiving.age * 7F) * Math.PI * (double)0.9F);
            float vibrate = 0.05F;
            matrixStackIn.translate((entityLiving.getRandom().nextFloat() - 0.5F)* vibrate, (entityLiving.getRandom().nextFloat() - 0.5F) * vibrate, (entityLiving.getRandom().nextFloat() - 0.5F)* vibrate);
        }
        super.setupTransforms(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
    }

    @Override
    public Identifier getTexture(EntityFly entity) {
        return TEXTURE;
    }
}
