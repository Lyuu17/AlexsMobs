package com.github.alexthe666.alexsmobs.item;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import java.util.Random;

public class FancyItem extends HiddenItemCreative implements IItemRender {

    public FancyItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        float partialTicks = MinecraftClient.getInstance().getTickDelta();
        float ticksExisted = Util.getMeasuringTimeMs() / 50F + partialTicks;
        int id = MinecraftClient.getInstance().player == null ? 0 : MinecraftClient.getInstance().player.getId();
        var random = new Random();
        boolean animateAnyways = false;
        ItemStack toRender = null;
        if (stack.getNbt() != null && stack.getNbt().contains("DisplayItem")) {
            String displayID = stack.getNbt().getString("DisplayItem");
            toRender = new ItemStack(Registries.ITEM.get(new Identifier(displayID)));
            if (stack.getNbt().contains("DisplayItemNBT")) {
                try {
                    toRender.setNbt(stack.getNbt().getCompound("DisplayItemNBT"));
                } catch (Exception e) {
                    toRender = new ItemStack(Items.BARRIER);
                }
            }
        }
        if (toRender == null) {
            animateAnyways = true;
            toRender = new ItemStack(Items.BARRIER);
        }
        matrices.push();
        matrices.translate(0.5F, 0.5f, 0.5f);
        if(stack.getNbt() != null && stack.getNbt().contains("DisplayShake") && stack.getNbt().getBoolean("DisplayShake")) {
            matrices.translate((random.nextFloat() - 0.5F) * 0.1F, (random.nextFloat() - 0.5F) * 0.1F, (random.nextFloat() - 0.5F) * 0.1F);
        }
        if(animateAnyways || stack.getNbt() != null && stack.getNbt().contains("DisplayBob") && stack.getNbt().getBoolean("DisplayBob")){
            matrices.translate(0, 0.05F + 0.1F * MathHelper.sin(0.3F * ticksExisted), 0);
        }
        if(stack.getNbt() != null && stack.getNbt().contains("DisplaySpin") && stack.getNbt().getBoolean("DisplaySpin")){
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(6 * ticksExisted));
        }
        if(animateAnyways || stack.getNbt() != null && stack.getNbt().contains("DisplayZoom") && stack.getNbt().getBoolean("DisplayZoom")) {
            float scale = (float) (1F + 0.15F * (Math.sin(ticksExisted * 0.3F) + 1F));
            matrices.scale(scale, scale, scale);
        }
        if(stack.getNbt() != null && stack.getNbt().contains("DisplayScale") && stack.getNbt().getFloat("DisplayScale") != 1.0F){
            float scale = stack.getNbt().getFloat("DisplayScale");
            matrices.scale(scale, scale, scale);
        }
        MinecraftClient.getInstance().getItemRenderer().renderItem(toRender, mode, light, overlay, matrices, vertexConsumers, null, id);
        matrices.pop();
    }
}