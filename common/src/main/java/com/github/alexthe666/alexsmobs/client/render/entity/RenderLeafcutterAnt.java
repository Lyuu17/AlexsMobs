package com.github.alexthe666.alexsmobs.client.render.entity;

import com.github.alexthe666.alexsmobs.client.model.ModelLeafcutterAnt;
import com.github.alexthe666.alexsmobs.client.model.ModelLeafcutterAntQueen;
import com.github.alexthe666.alexsmobs.client.model.layered.LayerLeafcutterAntLeaf;
import com.github.alexthe666.alexsmobs.entity.EntityLeafcutterAnt;
import com.iafenvoy.uranus.client.model.AdvancedEntityModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class RenderLeafcutterAnt extends MobEntityRenderer<EntityLeafcutterAnt, AdvancedEntityModel<EntityLeafcutterAnt>> {
    private static final Identifier TEXTURE = new Identifier("alexsmobs:textures/entity/leafcutter_ant.png");
    private static final Identifier TEXTURE_QUEEN = new Identifier("alexsmobs:textures/entity/leafcutter_ant_queen.png");
    private static final Identifier TEXTURE_ANGRY = new Identifier("alexsmobs:textures/entity/leafcutter_ant_angry.png");
    private static final Identifier TEXTURE_QUEEN_ANGRY = new Identifier("alexsmobs:textures/entity/leafcutter_ant_queen_angry.png");
    private final ModelLeafcutterAnt modelAnt = new ModelLeafcutterAnt();
    private final ModelLeafcutterAntQueen modelQueen = new ModelLeafcutterAntQueen();

    public RenderLeafcutterAnt(EntityRendererFactory.Context renderManagerIn) {
        super(renderManagerIn, new ModelLeafcutterAnt(), 0.25F);
        this.addFeature(new LayerLeafcutterAntLeaf(this));
    }

    @Override
    protected void setupTransforms(EntityLeafcutterAnt entityLiving, MatrixStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks) {
        if (this.isShaking(entityLiving)) {
            rotationYaw += (float)(Math.cos((double)entityLiving.age * 3.25D) * Math.PI * (double)0.4F);
        }
        float trans = entityLiving.isBaby() ? 0.25F : 0.5F;
        var pose = entityLiving.getPose();
        if (pose != EntityPose.SLEEPING) {
            float progresso = 1F - (entityLiving.prevAttachChangeProgress + (entityLiving.attachChangeProgress - entityLiving.prevAttachChangeProgress) * partialTicks);

            if(entityLiving.getAttachmentFacing() == Direction.DOWN){
                matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees (180.0F - rotationYaw));
                matrixStackIn.translate(0.0D, trans, 0.0D);
                if(entityLiving.prevY < entityLiving.getY()){
                    matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90 * (1 - progresso)));
                }else{
                    matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90 * (1 - progresso)));
                }
                matrixStackIn.translate(0.0D, -trans, 0.0D);

            }else if(entityLiving.getAttachmentFacing() == Direction.UP){
                matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees (180.0F - rotationYaw));
                matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
                matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
                matrixStackIn.translate(0.0D, -trans, 0.0D);

            }else{
                matrixStackIn.translate(0.0D, trans, 0.0D);
                switch (entityLiving.getAttachmentFacing()){
                    case NORTH:
                        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F * progresso));
                        matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(0));
                        break;
                    case SOUTH:
                        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
                        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F * progresso ));
                        break;
                    case WEST:
                        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
                        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90F - 90.0F * progresso));
                        matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90.0F));
                        break;
                    case EAST:
                        matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F ));
                        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F * progresso - 90F));
                        matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F));
                        break;
                }
                if(entityLiving.getVelocity().y <= -0.001F){
                    matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-180.0F));
                }
                matrixStackIn.translate(0.0D, -trans, 0.0D);
            }
        }

        if (entityLiving.deathTime > 0) {
            float f = ((float)entityLiving.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
            f = MathHelper.sqrt(f);
            if (f > 1.0F) {
                f = 1.0F;
            }

            matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * this.getLyingAngle(entityLiving)));
        } else if (entityLiving.isUsingRiptide()) {
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F - entityLiving.getPitch()));
            matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(((float)entityLiving.age + partialTicks) * -75.0F));
        } else if (pose == EntityPose.SLEEPING) {

        } else if (entityLiving.hasCustomName() ) {
            String s = Formatting.strip(entityLiving.getName().getString());
            if (("Dinnerbone".equals(s) || "Grumm".equals(s))) {
                matrixStackIn.translate(0.0D, (double)(entityLiving.getHeight() + 0.1F), 0.0D);
                matrixStackIn.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
            }
        }
    }

    @Override
    protected void scale(EntityLeafcutterAnt entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        model = entitylivingbaseIn.isQueen() ? modelQueen : modelAnt;
    }
    
    @Override
    public Identifier getTexture(EntityLeafcutterAnt entity) {
        if(entity.getAngerTime() > 0){
            return entity.isQueen() ? TEXTURE_QUEEN_ANGRY : TEXTURE_ANGRY;
        }else {
            return entity.isQueen() ? TEXTURE_QUEEN : TEXTURE;
        }
    }
}
