package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityBoneSerpentPart;
import com.github.alexthe666.alexsmobs.client.model.ModelBoneSerpentBody;
import com.github.alexthe666.alexsmobs.client.model.ModelBoneSerpentTail;
import com.iafenvoy.uranus.client.model.AdvancedEntityModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderBoneSerpentPart extends LivingEntityRenderer<EntityBoneSerpentPart, AdvancedEntityModel<EntityBoneSerpentPart>> {
    private static final Identifier TEXTURE_BODY = new Identifier("alexsmobs:textures/entity/bone_serpent_mid.png");
    private static final Identifier TEXTURE_TAIL = new Identifier("alexsmobs:textures/entity/bone_serpent_tail.png");
    private final ModelBoneSerpentBody bodyModel = new ModelBoneSerpentBody();
    private final ModelBoneSerpentTail tailModel = new ModelBoneSerpentTail();

    public RenderBoneSerpentPart(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelBoneSerpentBody(), 0.3F);
    }

    @Override
    protected boolean hasLabel(EntityBoneSerpentPart entity) {
        return super.hasLabel(entity) && (entity.shouldRenderName() || entity.hasCustomName() && entity == this.dispatcher.targetedEntity);
    }

    @Override
    protected void scale(EntityBoneSerpentPart entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        this.model = entitylivingbaseIn.isTail() ? tailModel : bodyModel;
      //  matrixStackIn.scale(1.2F, 1.2F, 1.2F);
    }

    @Override
    public Identifier getTexture(EntityBoneSerpentPart entity) {
        return entity.isTail() ? TEXTURE_TAIL : TEXTURE_BODY;
    }
}
