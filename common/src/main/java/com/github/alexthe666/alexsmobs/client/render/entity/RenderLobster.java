package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.entity.EntityLobster;
import com.github.alexthe666.alexsmobs.client.model.ModelLobster;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderLobster extends MobEntityRenderer<EntityLobster, ModelLobster> {
    private static final Identifier TEXTURE_RED = new Identifier("alexsmobs:textures/entity/lobster_red.png");
    private static final Identifier TEXTURE_BLUE = new Identifier("alexsmobs:textures/entity/lobster_blue.png");
    private static final Identifier TEXTURE_YELLOW = new Identifier("alexsmobs:textures/entity/lobster_yellow.png");
    private static final Identifier TEXTURE_REDBLUE = new Identifier("alexsmobs:textures/entity/lobster_redblue.png");
    private static final Identifier TEXTURE_BLACK = new Identifier("alexsmobs:textures/entity/lobster_black.png");
    private static final Identifier TEXTURE_WHITE = new Identifier("alexsmobs:textures/entity/lobster_white.png");

    public RenderLobster(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelLobster(), 0.25F);
    }

    @Override
    protected void scale(EntityLobster entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    }
    
    @Override
    public Identifier getTexture(EntityLobster entity) {
        return switch (entity.getVariant()) {
            case 1 -> TEXTURE_BLUE;
            case 2 -> TEXTURE_YELLOW;
            case 3 -> TEXTURE_REDBLUE;
            case 4 -> TEXTURE_BLACK;
            case 5 -> TEXTURE_WHITE;
            default -> TEXTURE_RED;
        };
    }
}
