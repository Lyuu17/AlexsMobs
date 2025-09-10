package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityRoadrunner;
import com.github.alexthe666.alexsmobs.model.ModelRoadrunner;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class RenderRoadrunner extends MobEntityRenderer<EntityRoadrunner, ModelRoadrunner> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/roadrunner.png");
    private static final Identifier TEXTURE_MEEP = new Identifier("alexsmobs:textures/entity/roadrunner_meep.png");

    public RenderRoadrunner(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelRoadrunner(), 0.3F);
    }

    @Override
    public Identifier getTexture(EntityRoadrunner entity) {
        return entity.isMeep() ? TEXTURE_MEEP : TEXTURE;
    }
}
