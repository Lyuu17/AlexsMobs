package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.client.model.ModelShieldOfTheDeep;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;

public class ItemShieldOfTheDeep extends Item implements IItemRender {

    private static final ModelShieldOfTheDeep SHIELD_OF_THE_DEEP_MODEL = new ModelShieldOfTheDeep();
    private static final Identifier SHIELD_OF_THE_DEEP_TEXTURE = new Identifier("alexsmobs:textures/armor/shield_of_the_deep.png");

    public ItemShieldOfTheDeep(Item.Settings group) {
        super(group);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK;
    }

    @Override
    public int getMaxUseTime(ItemStack p_77626_1_) {
        return 72000;
    }

    @Override
    public TypedActionResult<ItemStack> use(World p_77659_1_, PlayerEntity p_77659_2_, Hand p_77659_3_) {
        ItemStack lvt_4_1_ = p_77659_2_.getStackInHand(p_77659_3_);
        p_77659_2_.setCurrentHand(p_77659_3_);
        return TypedActionResult.consume(lvt_4_1_);
    }

    @Override
    public boolean canRepair(ItemStack p_82789_1_, ItemStack p_82789_2_) {
        return AMItemRegistry.SERRATED_SHARK_TOOTH.get() == p_82789_2_.getItem() || super.canRepair(p_82789_1_, p_82789_2_);
    }

    @Override
    public void render(ItemStack itemStackIn, ModelTransformationMode transformType, MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int combinedLightIn, int combinedOverlayIn) {
        matrixStackIn.push();
        matrixStackIn.translate(0.4F, -0.75F, 0.5F);
        matrixStackIn.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-180));
        VertexConsumer vertexconsumer = ItemRenderer.getArmorGlintConsumer(bufferIn, RenderLayer.getArmorCutoutNoCull(SHIELD_OF_THE_DEEP_TEXTURE), false, itemStackIn.hasGlint());
        SHIELD_OF_THE_DEEP_MODEL.render(matrixStackIn, vertexconsumer, combinedLightIn, combinedOverlayIn, 1.0F, 1.0F, 1.0F, 1.0F);
        matrixStackIn.pop();
    }
}
