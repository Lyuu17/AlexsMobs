package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelGeladaMonkey;
import com.github.alexthe666.alexsmobs.entity.EntityGeladaMonkey;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderGeladaMonkey extends MobEntityRenderer<EntityGeladaMonkey, ModelGeladaMonkey> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/gelada_monkey.png");
    private static final Identifier TEXTURE_ANGRY = new Identifier("alexsmobs:textures/entity/gelada_monkey_angry.png");
    private static final Identifier TEXTURE_LEADER = new Identifier("alexsmobs:textures/entity/gelada_monkey_leader.png");
    private static final Identifier TEXTURE_LEADER_ANGRY = new Identifier("alexsmobs:textures/entity/gelada_monkey_leader_angry.png");

    public RenderGeladaMonkey(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelGeladaMonkey(), 0.45F);
    }

    @Override
    protected void scale(EntityGeladaMonkey entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(entitylivingbaseIn.getGeladaScale(), entitylivingbaseIn.getGeladaScale(), entitylivingbaseIn.getGeladaScale());
    }

    @Override
    public Identifier getTexture(EntityGeladaMonkey entity) {
        return entity.isLeader() ? entity.isAggro() ? TEXTURE_LEADER_ANGRY : TEXTURE_LEADER : entity.isAggro() ? TEXTURE_ANGRY : TEXTURE;
    }
}
