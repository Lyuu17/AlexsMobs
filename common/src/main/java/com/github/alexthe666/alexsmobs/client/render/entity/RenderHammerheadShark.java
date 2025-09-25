package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityHammerheadShark;
import com.github.alexthe666.alexsmobs.client.model.ModelHammerheadShark;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderHammerheadShark extends MobEntityRenderer<EntityHammerheadShark, ModelHammerheadShark> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/hammerhead_shark.png");

    public RenderHammerheadShark(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelHammerheadShark(), 0.8F);
    }

    @Override
    protected void scale(EntityHammerheadShark entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }

    @Override
    public Identifier getTexture(EntityHammerheadShark entity) {
        return TEXTURE;
    }
}
