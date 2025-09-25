package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelCapuchinMonkey;
import com.github.alexthe666.alexsmobs.client.render.entity.layer.LayerCapuchinItem;
import com.github.alexthe666.alexsmobs.entity.EntityCapuchinMonkey;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderCapuchinMonkey extends MobEntityRenderer<EntityCapuchinMonkey, ModelCapuchinMonkey> {
    private static final Identifier TEXTURE_0 = new Identifier("alexsmobs:textures/entity/capuchin_monkey_0.png");
    private static final Identifier TEXTURE_1 = new Identifier("alexsmobs:textures/entity/capuchin_monkey_1.png");
    private static final Identifier TEXTURE_2 = new Identifier("alexsmobs:textures/entity/capuchin_monkey_2.png");
    private static final Identifier TEXTURE_3 = new Identifier("alexsmobs:textures/entity/capuchin_monkey_3.png");

    public RenderCapuchinMonkey(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelCapuchinMonkey(), 0.25F);
        this.addFeature(new LayerCapuchinItem(this));
    }

    @Override
    protected void scale(EntityCapuchinMonkey entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.8F, 0.8F, 0.8F);
    }

    @Override
    public Identifier getTexture(EntityCapuchinMonkey entity) {
        return switch (entity.getVariant()) {
            case 1 -> TEXTURE_1;
            case 2 -> TEXTURE_2;
            case 3 -> TEXTURE_3;
            default -> TEXTURE_0;
        };
    }
}
