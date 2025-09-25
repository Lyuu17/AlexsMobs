package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelTusklin;
import com.github.alexthe666.alexsmobs.client.render.entity.layer.LayerTusklinGear;
import com.github.alexthe666.alexsmobs.entity.EntityTusklin;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class RenderTusklin extends MobEntityRenderer<EntityTusklin, ModelTusklin> {

    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/tusklin.png");

    public RenderTusklin(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelTusklin(), 1.0F);
        this.addFeature(new LayerTusklinGear(this));
    }

    @Override
    protected boolean isShaking(EntityTusklin entity) {
        return entity.isInNether();
    }

    @NotNull
    @Override
    public Identifier getTexture(EntityTusklin tusklin) {
        return TEXTURE;
    }
}
