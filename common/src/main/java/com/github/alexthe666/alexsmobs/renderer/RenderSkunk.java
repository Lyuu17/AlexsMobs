package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntitySkunk;
import com.github.alexthe666.alexsmobs.model.ModelSkunk;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class RenderSkunk extends MobEntityRenderer<EntitySkunk, ModelSkunk> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/skunk.png");

    public RenderSkunk(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelSkunk(), 0.45F);
    }

    @Override
    public Identifier getTexture(EntitySkunk entity) {
        return TEXTURE;
    }
}
