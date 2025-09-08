package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.entity.EntityEmu;
import com.github.alexthe666.alexsmobs.model.ModelEmu;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderEmu extends MobEntityRenderer<EntityEmu, ModelEmu> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/emu.png");
    private static final Identifier TEXTURE_BABY = new Identifier("alexsmobs:textures/entity/emu_baby.png");
    private static final Identifier TEXTURE_BLONDE = new Identifier("alexsmobs:textures/entity/emu_blonde.png");
    private static final Identifier TEXTURE_BLONDE_BABY = new Identifier("alexsmobs:textures/entity/emu_baby_blonde.png");
    private static final Identifier TEXTURE_BLUE = new Identifier("alexsmobs:textures/entity/emu_blue.png");

    public RenderEmu(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelEmu(), 0.45F);
    }

    @Override
    protected void scale(EntityEmu entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.85F, 0.85F, 0.85F);
    }

    @Override
    public Identifier getTexture(EntityEmu entity) {
        if(entity.getVariant() == 2){
            return entity.isBaby() ? TEXTURE_BLONDE_BABY : TEXTURE_BLONDE;
        }
        if(entity.getVariant() == 1 && !entity.isBaby()){
            return  TEXTURE_BLUE;
        }
        return entity.isBaby() ? TEXTURE_BABY : TEXTURE;
    }
}
