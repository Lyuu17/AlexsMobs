package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.EntityFart;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.function.Predicate;

public class ItemStinkRay extends Item implements IItemRender {

    public static final Predicate<ItemStack> IS_FART_BOTTLE = (stack) -> stack.getItem() == AMItemRegistry.STINK_BOTTLE.get();

    public ItemStinkRay(Settings properties) {
        super(properties);
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return isUsable(stack) ? 72000 : 0;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    public static boolean isUsable(ItemStack stack) {
        return stack.getDamage() < stack.getMaxDamage() - 1;
    }

    @Override
    public boolean isItemBarVisible(ItemStack itemStack) {
        return super.isItemBarVisible(itemStack) && isUsable(itemStack);
    }

    public static float getPowerForTime(int i) {
        float f = (float) i / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

    @Override
    public void onStoppedUsing(ItemStack itemStack, World level, LivingEntity entity, int time) {
        if (entity instanceof PlayerEntity player && isUsable(itemStack)) {
            int i = this.getMaxUseTime(itemStack) - time;
            if (i >= 10) {
                boolean left = false;
                if (entity.getActiveHand() == Hand.OFF_HAND && entity.getMainArm() == Arm.RIGHT || entity.getActiveHand() == Hand.MAIN_HAND && entity.getMainArm() == Arm.LEFT) {
                    left = true;
                }
                EntityFart blood = new EntityFart(level, entity, !left);
                Vec3d vector3d = entity.getRotationVec(1.0F);
                var rand = level.getRandom();
                entity.emitGameEvent(GameEvent.ITEM_INTERACT_START);
                entity.playSound(AMSoundRegistry.STINK_RAY.get(), 1.0F, 0.9F + (rand.nextFloat() - rand.nextFloat()) * 0.2F);
                blood.shoot(vector3d.x, vector3d.y, vector3d.z, 0.2F + getPowerForTime(i) * 0.4F, 10);
                if (!level.isClient) {
                    level.spawnEntity(blood);
                }
                itemStack.damage(1, entity, (breaker) -> {
                    breaker.sendToolBreakStatus(entity.getActiveHand());
                });

            }

        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack itemstack = playerIn.getStackInHand(handIn);
        playerIn.setCurrentHand(handIn);
        if (!isUsable(itemstack)) {
            ItemStack ammo = findAmmo(playerIn);
            boolean flag = playerIn.isCreative();
            if (!ammo.isEmpty()) {
                ammo.decrement(1);
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (!playerIn.giveItemStack(bottle)) {
                    playerIn.dropItem(bottle, false);
                }
                flag = true;
            }
            if (flag) {
                itemstack.setDamage(0);
            }
        }
        return TypedActionResult.consume(itemstack);
    }

    public ItemStack findAmmo(PlayerEntity entity) {
        if (entity.isCreative()) {
            return new ItemStack(AMItemRegistry.STINK_BOTTLE.get());
        }
        for (int i = 0; i < entity.getInventory().size(); ++i) {
            var itemstack1 = entity.getInventory().getStack(i);
            if (IS_FART_BOTTLE.test(itemstack1)) {
                return itemstack1;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        var world = MinecraftClient.getInstance().world;

        matrices.translate(0.5F, 0.5f, 0.5f);
        ItemStack hand = new ItemStack(ItemStinkRay.isUsable(stack) ? AMItemRegistry.STINK_RAY_HAND.get() : AMItemRegistry.STINK_RAY_EMPTY_HAND.get());
        ItemStack inventory = new ItemStack(ItemStinkRay.isUsable(stack) ? AMItemRegistry.STINK_RAY_INVENTORY.get() : AMItemRegistry.STINK_RAY_EMPTY_INVENTORY.get());
        if (mode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND || mode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND || mode == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND || mode == ModelTransformationMode.FIRST_PERSON_LEFT_HAND) {
            MinecraftClient.getInstance().getItemRenderer().renderItem(hand, mode, light, overlay, matrices, vertexConsumers, world, 0);
        } else {
            MinecraftClient.getInstance().getItemRenderer().renderItem(inventory, mode, mode == ModelTransformationMode.GROUND ? light : 240, overlay, matrices, vertexConsumers, world, 0);
        }
    }
}
