package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

public class ItemSkelewagSword extends SwordItem implements IItemRender {

    private final ImmutableMultimap<EntityAttribute, EntityAttributeModifier> skelewagModifiers;

    public ItemSkelewagSword(Item.Settings props) {
        super(ToolMaterials.IRON, 2, 0, props);
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(Item.ATTACK_DAMAGE_MODIFIER_ID, "Weapon modifier", 3.5F, EntityAttributeModifier.Operation.ADDITION));
        builder.put(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(Item.ATTACK_SPEED_MODIFIER_ID, "Weapon modifier", 0, EntityAttributeModifier.Operation.ADDITION));
        this.skelewagModifiers = builder.build();
    }

    @Override
    public float getAttackDamage() {
        return 3.5F;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    @Override
    public TypedActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
        ItemStack lvt_4_1_ = player.getStackInHand(hand);
        player.setCurrentHand(hand);
        return TypedActionResult.consume(lvt_4_1_);
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack repairStack) {
        return repairStack.isOf(Items.BONE);
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.skelewagModifiers : super.getAttributeModifiers(slot);
    }

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        var world = MinecraftClient.getInstance().world;

        matrices.translate(0.5F, 0.5f, 0.5f);
        ItemStack spriteItem = new ItemStack(AMItemRegistry.SKELEWAG_SWORD_INVENTORY.get());
        ItemStack handItem = new ItemStack(AMItemRegistry.SKELEWAG_SWORD_HAND.get());
        spriteItem.setNbt(stack.getNbt());
        handItem.setNbt(stack.getNbt());
        if (mode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND || mode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND || mode == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND || mode == ModelTransformationMode.FIRST_PERSON_LEFT_HAND) {
            MinecraftClient.getInstance().getItemRenderer().renderItem(handItem, mode, light, overlay, matrices, vertexConsumers, world, 0);
        } else {
            MinecraftClient.getInstance().getItemRenderer().renderItem(spriteItem, mode, mode == ModelTransformationMode.GROUND ? light : 240, overlay, matrices, vertexConsumers, world, 0);
        }
    }
}
