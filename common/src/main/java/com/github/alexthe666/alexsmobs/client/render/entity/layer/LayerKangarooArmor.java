package com.github.alexthe666.alexsmobs.client.render.entity.layer;

import com.github.alexthe666.alexsmobs.client.model.ModelKangaroo;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderKangaroo;
import com.github.alexthe666.alexsmobs.entity.EntityKangaroo;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

public class LayerKangarooArmor extends FeatureRenderer<EntityKangaroo, ModelKangaroo> {

    private static final Map<String, Identifier> ARMOR_TEXTURE_RES_MAP = new HashMap<>();
    private final BipedEntityModel<EntityKangaroo> defaultBipedModel;
    private final RenderKangaroo renderer;

    public LayerKangarooArmor(RenderKangaroo render, EntityRendererFactory.Context context) {
        super(render);
        this.defaultBipedModel = new BipedEntityModel<>(context.getPart(EntityModelLayers.ARMOR_STAND_OUTER_ARMOR));
        this.renderer = render;
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
        var s1 = String.format("%s:textures/models/armor/%s_layer_%d%s.png", domain, texture, (slot == EquipmentSlot.LEGS ? 2 : 1), type == null ? "" : String.format("_%s", type));

        var resourcelocation = ARMOR_TEXTURE_RES_MAP.get(s1);

        if (resourcelocation == null) {
            resourcelocation = new Identifier(s1);
            ARMOR_TEXTURE_RES_MAP.put(s1, resourcelocation);
        }

        return resourcelocation;
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, EntityKangaroo roo, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        matrixStackIn.push();
        if(roo.isRoger()){
            ItemStack haloStack = new ItemStack(AMItemRegistry.HALO.get());
            matrixStackIn.push();
            translateToHead(matrixStackIn);
            float f = 0.1F * (float) Math.sin((roo.age + partialTicks) * 0.1F) + (roo.isBaby() ? 0.2F : 0F);
            matrixStackIn.translate(0.0F, -0.75F - f, -0.2F);
            matrixStackIn.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90F));
            matrixStackIn.scale(1.3F, 1.3F, 1.3F);
            var renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer();
            renderer.renderItem(roo, haloStack, ModelTransformationMode.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.pop();
        }
        if (!roo.isBaby()) {
            {
                matrixStackIn.push();
                ItemStack itemstack = roo.getEquippedStack(EquipmentSlot.HEAD);
                if (itemstack.getItem() instanceof ArmorItem armoritem) {
                    if (MobEntity.getPreferredEquipmentSlot(itemstack) == EquipmentSlot.HEAD) {
                        var a = defaultBipedModel;
                        //a = getArmorModelHook(roo, itemstack, EquipmentSlot.HEAD, a);
                        final boolean notAVanillaModel = a != defaultBipedModel;
                        this.setModelSlotVisible(a, EquipmentSlot.HEAD);
                        translateToHead(matrixStackIn);
                        matrixStackIn.translate(0, 0.015F, -0.05F);
                        if (itemstack.getItem() == AMItemRegistry.FEDORA.get()) {
                            matrixStackIn.translate(0, 0.05F, 0F);

                        }
                        matrixStackIn.scale(0.7F, 0.7F, 0.7F);
                        final boolean flag1 = itemstack.hasGlint();
                        if (armoritem instanceof DyeableItem) { // Allow this for anything, not only cloth
                            final int i = ((DyeableItem) armoritem).getColor(itemstack);
                            final float f = (float) (i >> 16 & 255) / 255.0F;
                            final float f1 = (float) (i >> 8 & 255) / 255.0F;
                            final float f2 = (float) (i & 255) / 255.0F;
                            renderHelmet(roo, matrixStackIn, bufferIn, packedLightIn, flag1, a, f, f1, f2, getArmorResource(roo, itemstack, EquipmentSlot.HEAD, null), notAVanillaModel);
                            renderHelmet(roo, matrixStackIn, bufferIn, packedLightIn, flag1, a, 1.0F, 1.0F, 1.0F, getArmorResource(roo, itemstack, EquipmentSlot.HEAD, "overlay"), notAVanillaModel);
                        } else {
                            renderHelmet(roo, matrixStackIn, bufferIn, packedLightIn, flag1, a, 1.0F, 1.0F, 1.0F, getArmorResource(roo, itemstack, EquipmentSlot.HEAD, null), notAVanillaModel);
                        }
                    }
                } else {
                    translateToHead(matrixStackIn);
                    matrixStackIn.translate(0, -0.2, -0.1F);
                    matrixStackIn.multiply((new Quaternionf()).rotateX(MathHelper.PI));
                    matrixStackIn.multiply((new Quaternionf()).rotateY(MathHelper.PI));
                    matrixStackIn.scale(1.0F, 1.0F, 1.0F);
                    MinecraftClient.getInstance().getItemRenderer().renderItem(itemstack, ModelTransformationMode.FIXED, packedLightIn, OverlayTexture.DEFAULT_UV, matrixStackIn, bufferIn, roo.getWorld(), 0);
                }
                matrixStackIn.pop();
            }
            {
                matrixStackIn.push();
                ItemStack itemstack = roo.getEquippedStack(EquipmentSlot.CHEST);
                if (itemstack.getItem() instanceof ArmorItem armoritem) {
                    if (armoritem.getSlotType() == EquipmentSlot.CHEST) {
                        var a = defaultBipedModel;
                        //a = getArmorModelHook(roo, itemstack, EquipmentSlot.CHEST, a);
                        boolean notAVanillaModel = a != defaultBipedModel;
                        this.setModelSlotVisible(a, EquipmentSlot.CHEST);
                        translateToChest(matrixStackIn);
                        matrixStackIn.translate(0, 0.25F, 0F);
                        matrixStackIn.scale(1F, 1F, 1F);
                        boolean flag1 = itemstack.hasGlint();
                        if (armoritem instanceof DyeableItem) { // Allow this for anything, not only cloth
                            int i = ((DyeableItem) armoritem).getColor(itemstack);
                            float f = (float) (i >> 16 & 255) / 255.0F;
                            float f1 = (float) (i >> 8 & 255) / 255.0F;
                            float f2 = (float) (i & 255) / 255.0F;
                            renderChestplate(roo, matrixStackIn, bufferIn, packedLightIn, flag1, a, f, f1, f2, getArmorResource(roo, itemstack, EquipmentSlot.CHEST, null), notAVanillaModel);
                            renderChestplate(roo, matrixStackIn, bufferIn, packedLightIn, flag1, a, 1.0F, 1.0F, 1.0F, getArmorResource(roo, itemstack, EquipmentSlot.CHEST, "overlay"), notAVanillaModel);
                        } else {
                            renderChestplate(roo, matrixStackIn, bufferIn, packedLightIn, flag1, a, 1.0F, 1.0F, 1.0F, getArmorResource(roo, itemstack, EquipmentSlot.CHEST, null), notAVanillaModel);
                        }

                    }
                }
                matrixStackIn.pop();
            }
        }
        matrixStackIn.pop();

    }

    private void translateToHead(MatrixStack matrixStackIn) {
        translateToChest(matrixStackIn);
        this.renderer.getModel().neck.translateAndRotate(matrixStackIn);
        this.renderer.getModel().head.translateAndRotate(matrixStackIn);
    }

    private void translateToChest(MatrixStack matrixStackIn) {
        this.renderer.getModel().root.translateAndRotate(matrixStackIn);
        this.renderer.getModel().body.translateAndRotate(matrixStackIn);
        this.renderer.getModel().chest.translateAndRotate(matrixStackIn);
    }

    private void renderChestplate(EntityKangaroo entity, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, boolean glintIn, BipedEntityModel<EntityKangaroo> modelIn, float red, float green, float blue, Identifier armorResource, boolean notAVanillaModel) {
        var ivertexbuilder = ItemRenderer.getItemGlintConsumer(bufferIn, RenderLayer.getEntityCutoutNoCull(armorResource), false, glintIn);
        renderer.getModel().copyStateTo(modelIn);
        float sitProgress = entity.prevSitProgress + (entity.sitProgress - entity.prevSitProgress) * MinecraftClient.getInstance().getTickDelta();
        modelIn.body.pitch = 90 * 0.017453292F;
        modelIn.body.yaw = 0;
        modelIn.body.roll = 0;
        modelIn.body.pivotX = 0;
        modelIn.body.pivotY = 0.25F;
        modelIn.body.pivotZ = -7.6F;
        modelIn.rightArm.pivotX = renderer.getModel().arm_right.rotationPointX;
        modelIn.rightArm.pivotY = renderer.getModel().arm_right.rotationPointY;
        modelIn.rightArm.pivotZ = renderer.getModel().arm_right.rotationPointZ;
        modelIn.rightArm.pitch = renderer.getModel().arm_right.rotateAngleX;
        modelIn.rightArm.yaw = renderer.getModel().arm_right.rotateAngleY;
        modelIn.rightArm.roll = renderer.getModel().arm_right.rotateAngleZ;
        modelIn.leftArm.pivotX = renderer.getModel().arm_left.rotationPointX;
        modelIn.leftArm.pivotY = renderer.getModel().arm_left.rotationPointY;
        modelIn.leftArm.pivotZ = renderer.getModel().arm_left.rotationPointZ;
        modelIn.leftArm.pitch = renderer.getModel().arm_left.rotateAngleX;
        modelIn.leftArm.yaw = renderer.getModel().arm_left.rotateAngleY;
        modelIn.leftArm.roll = renderer.getModel().arm_left.rotateAngleZ;
        modelIn.leftArm.pivotY = renderer.getModel().arm_left.rotationPointY - 4 + (sitProgress * 0.25F);
        modelIn.rightArm.pivotY = renderer.getModel().arm_right.rotationPointY - 4 + (sitProgress * 0.25F);
        modelIn.leftArm.pivotZ = renderer.getModel().arm_left.rotationPointZ - 0.5F;
        modelIn.rightArm.pivotZ = renderer.getModel().arm_right.rotationPointZ - 0.5F;
        modelIn.body.visible = false;
        modelIn.render(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.DEFAULT_UV, red, green, blue, 1.0F);
        modelIn.body.visible = true;
        modelIn.rightArm.visible = false;
        modelIn.leftArm.visible = false;
        matrixStackIn.push();
        matrixStackIn.scale(1.1F, 1.65F, 1.1F);
        modelIn.render(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.DEFAULT_UV, red, green, blue, 1.0F);
        matrixStackIn.pop();
        modelIn.rightArm.visible = true;
        modelIn.leftArm.visible = true;

    }

    private void renderHelmet(EntityKangaroo entity, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, boolean glintIn, BipedEntityModel<EntityKangaroo> modelIn, float red, float green, float blue, Identifier armorResource, boolean notAVanillaModel) {
        var ivertexbuilder = ItemRenderer.getItemGlintConsumer(bufferIn, RenderLayer.getEntityCutoutNoCull(armorResource), false, glintIn);
        renderer.getModel().copyStateTo(modelIn);
        modelIn.head.pitch = 0F;
        modelIn.head.yaw = 0F;
        modelIn.head.roll = 0F;
        modelIn.hat.pitch = 0F;
        modelIn.hat.yaw = 0F;
        modelIn.hat.roll = 0F;
        modelIn.head.pivotX = 0F;
        modelIn.head.pivotY = 0F;
        modelIn.head.pivotZ = 0F;
        modelIn.hat.pivotX = 0F;
        modelIn.hat.pivotY = 0F;
        modelIn.hat.pivotZ = 0F;
        modelIn.render(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.DEFAULT_UV, red, green, blue, 1.0F);
    }

    protected void setModelSlotVisible(BipedEntityModel<?> p_188359_1_, EquipmentSlot slotIn) {
        this.setModelVisible(p_188359_1_);
        switch (slotIn) {
            case HEAD -> {
                p_188359_1_.head.visible = true;
                p_188359_1_.hat.visible = true;
            }
            case CHEST -> {
                p_188359_1_.body.visible = true;
                p_188359_1_.rightArm.visible = true;
                p_188359_1_.leftArm.visible = true;
            }
            case LEGS -> {
                p_188359_1_.body.visible = true;
                p_188359_1_.rightLeg.visible = true;
                p_188359_1_.leftLeg.visible = true;
            }
            case FEET -> {
                p_188359_1_.rightLeg.visible = true;
                p_188359_1_.leftLeg.visible = true;
            }
        }
    }

    protected void setModelVisible(BipedEntityModel<?> model) {
        model.setVisible(false);
    }

    // FIXME forge non-vanilla models
//    protected BipedEntityModel<?> getArmorModelHook(LivingEntity entity, ItemStack itemStack, EquipmentSlot slot, BipedEntityModel model) {
//         Model basicModel = net.minecraftforge.client.ForgeHooksClient.getArmorModel(entity, itemStack, slot, model);
//         return basicModel instanceof BipedEntityModel ? (BipedEntityModel<?>) basicModel : model;
//    }
}
