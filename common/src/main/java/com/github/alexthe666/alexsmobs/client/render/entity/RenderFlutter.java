package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelFlutter;
import com.github.alexthe666.alexsmobs.client.model.ModelFlutterPotted;
import com.github.alexthe666.alexsmobs.client.render.entity.layer.LayerBasicGlow;
import com.github.alexthe666.alexsmobs.entity.EntityFlutter;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderFlutter extends MobEntityRenderer<EntityFlutter, EntityModel<EntityFlutter>> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/flutter.png");
    private static final Identifier TEXTURE_EYES = new Identifier("alexsmobs:textures/entity/flutter_eyes.png");
    private final ModelFlutter modelFlutter = new ModelFlutter();
    private final ModelFlutterPotted modelPotted = new ModelFlutterPotted();

    public RenderFlutter(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelFlutter(), 0.25F);
        this.addFeature(new LayerBasicGlow<>(this, TEXTURE_EYES));
    }

    @Override
    protected void scale(EntityFlutter entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        if(entitylivingbaseIn.isPotted()){
            model = modelPotted;
        }else{
            model = modelFlutter;
        }
    }

    @Override
    public Identifier getTexture(EntityFlutter entity) {
        return TEXTURE;
    }
}
