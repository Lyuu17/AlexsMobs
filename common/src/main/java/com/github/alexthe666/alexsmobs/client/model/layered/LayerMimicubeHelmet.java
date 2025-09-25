package com.github.alexthe666.alexsmobs.client.model.layered;

import com.github.alexthe666.alexsmobs.client.model.ModelMimicube;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderMimicube;
import com.github.alexthe666.alexsmobs.entity.EntityMimicube;
import com.google.common.collect.Maps;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class LayerMimicubeHelmet extends FeatureRenderer<EntityMimicube, ModelMimicube> {

    private static final Map<String, Identifier> ARMOR_TEXTURE_RES_MAP = Maps.newHashMap();
    private final BipedEntityModel<?> defaultBipedModel;
    private final RenderMimicube renderer;

    public LayerMimicubeHelmet(RenderMimicube render, EntityRendererFactory.Context renderManagerIn) {
        super(render);
        this.renderer = render;
        defaultBipedModel = new BipedEntityModel<>(renderManagerIn.getPart(EntityModelLayers.ARMOR_STAND_OUTER_ARMOR));
    }

    public static Identifier getArmorResource(Entity entity, ItemStack stack, EquipmentSlot slot, @Nullable String type) {
        ArmorItem item = (ArmorItem) stack.getItem();
        String texture = item.getMaterial().getName();
        String domain = "minecraft";
        int idx = texture.indexOf(':');
        if (idx != -1) {
            domain = texture.substring(0, idx);
            texture = texture.substring(idx + 1);
        }
        String s1 = String.format("%s:textures/models/armor/%s_layer_%d%s.png", domain, texture, (1), type == null ? "" : String.format("_%s", type));

        // FIXME forge
//        s1 = net.minecraftforge.client.ForgeHooksClient.getArmorTexture(entity, stack, s1, slot, type);
        Identifier Identifier = ARMOR_TEXTURE_RES_MAP.get(s1);

        if (Identifier == null) {
            Identifier = new Identifier(s1);
            ARMOR_TEXTURE_RES_MAP.put(s1, Identifier);
        }

        return Identifier;
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityMimicube cube, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        matrixStackIn.push();
        ItemStack itemstack = cube.getEquippedStack(EquipmentSlot.HEAD);
        float helmetSwap = MathHelper.lerp(partialTicks, cube.prevHelmetSwapProgress, cube.helmetSwapProgress) * 0.2F;
        if (itemstack.getItem() instanceof ArmorItem) {
            ArmorItem armoritem = (ArmorItem) itemstack.getItem();
            if (armoritem.getSlotType() == EquipmentSlot.HEAD) {
                BipedEntityModel a = defaultBipedModel;
//                a = getArmorModelHook(cube, itemstack, EquipmentSlot.HEAD, a);
                boolean notAVanillaModel = a != defaultBipedModel;

                this.setModelSlotVisible(a, EquipmentSlot.HEAD);
                boolean flag = false;
                this.renderer.getModel().root.translateAndRotate(matrixStackIn);
                this.renderer.getModel().innerbody.translateAndRotate(matrixStackIn);
                matrixStackIn.translate(0,  notAVanillaModel ? 0.25F : -0.75F, 0F);
                matrixStackIn.scale(1F + 0.3F * (1 - helmetSwap), 1F + 0.3F * (1 - helmetSwap), 1F + 0.3F * (1 - helmetSwap));
                boolean flag1 = itemstack.hasGlint();
                int clampedLight = helmetSwap > 0 ? (int) (-100 * helmetSwap) : packedLightIn;
                matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(360 * helmetSwap));
                if (armoritem instanceof DyeableItem) { // Allow this for anything, not only cloth
                    int i = ((DyeableItem) armoritem).getColor(itemstack);
                    float f = (float) (i >> 16 & 255) / 255.0F;
                    float f1 = (float) (i >> 8 & 255) / 255.0F;
                    float f2 = (float) (i & 255) / 255.0F;
                    renderArmor(cube, matrixStackIn, bufferIn, clampedLight, flag1, a, f, f1, f2, getArmorResource(cube, itemstack, EquipmentSlot.HEAD, null), notAVanillaModel);
                    renderArmor(cube, matrixStackIn, bufferIn, clampedLight, flag1, a, 1.0F, 1.0F, 1.0F, getArmorResource(cube, itemstack, EquipmentSlot.HEAD, "overlay"), notAVanillaModel);
                } else {
                    renderArmor(cube, matrixStackIn, bufferIn, clampedLight, flag1, a, 1.0F, 1.0F, 1.0F, getArmorResource(cube, itemstack, EquipmentSlot.HEAD, null), notAVanillaModel);
                }

            }
        }
        matrixStackIn.pop();
    }

    private void renderArmor(EntityMimicube entity, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, boolean glintIn, BipedEntityModel<EntityMimicube> modelIn, float red, float green, float blue, Identifier armorResource, boolean notAVanillaModel) {
        VertexConsumer ivertexbuilder = ItemRenderer.getItemGlintConsumer(bufferIn, RenderLayer.getEntityCutoutNoCull(armorResource), false, glintIn);
        if(notAVanillaModel){
            renderer.getModel().copyStateTo(modelIn);
            modelIn.body.pivotY = 0;
            modelIn.head.setPivot(0.0F, 1.0F, 0.0F);
            modelIn.hat.pivotY = 0;
            modelIn.head.pitch = renderer.getModel().body.rotateAngleX;
            modelIn.head.yaw = renderer.getModel().body.rotateAngleY;
            modelIn.head.roll = renderer.getModel().body.rotateAngleZ;
            modelIn.head.pivotX = renderer.getModel().body.rotationPointX;
            modelIn.head.pivotY = renderer.getModel().body.rotationPointY;
            modelIn.head.pivotZ = renderer.getModel().body.rotationPointZ;
            modelIn.hat.copyTransform(modelIn.head);
            modelIn.body.copyTransform(modelIn.head);
        }
        modelIn.render(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.DEFAULT_UV, red, green, blue, 1.0F);
    }

    protected void setModelSlotVisible(BipedEntityModel<?> p_188359_1_, EquipmentSlot slotIn) {
        this.setModelVisible(p_188359_1_);
        switch (slotIn) {
            case HEAD:
                p_188359_1_.head.visible = true;
                p_188359_1_.hat.visible = true;
                break;
            case CHEST:
                p_188359_1_.body.visible = true;
                p_188359_1_.rightArm.visible = true;
                p_188359_1_.leftArm.visible = true;
                break;
            case LEGS:
                p_188359_1_.body.visible = true;
                p_188359_1_.rightLeg.visible = true;
                p_188359_1_.leftLeg.visible = true;
                break;
            case FEET:
                p_188359_1_.rightLeg.visible = true;
                p_188359_1_.leftLeg.visible = true;
        }
    }

    protected void setModelVisible(BipedEntityModel<?> model) {
        model.setVisible(false);
    }

    // FIXME forge
//    protected BipedEntityModel<?> getArmorModelHook(LivingEntity entity, ItemStack itemStack, EquipmentSlot slot, BipedEntityModel model) {
//        try{
//            Model basicModel = net.minecraftforge.client.ForgeHooksClient.getArmorModel(entity, itemStack, slot, model);
//            return basicModel instanceof BipedEntityModel ? (BipedEntityModel<?>) basicModel : model;
//        }catch (Exception e){
//            return model;
//        }
//    }
}
