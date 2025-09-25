package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityCaiman;
import com.github.alexthe666.alexsmobs.client.model.ModelCaiman;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class RenderCaiman extends MobEntityRenderer<EntityCaiman, ModelCaiman> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/caiman.png");

    public RenderCaiman(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelCaiman(), 0.4F);
    }

    @Override
    public Identifier getTexture(EntityCaiman entity) {
        return TEXTURE;
    }
}
