package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.projectile.EntitySharkToothArrow;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.util.Identifier;

public class RenderSharkToothArrow extends ProjectileEntityRenderer<EntitySharkToothArrow> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/shark_tooth_arrow.png");

    public RenderSharkToothArrow(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn);
    }

    @Override
    public Identifier getTexture(EntitySharkToothArrow entity) {
        return TEXTURE;
    }
}
