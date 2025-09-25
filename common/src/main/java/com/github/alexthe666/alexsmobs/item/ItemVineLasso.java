package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.client.render.item.AMItemstackRenderer;
import com.github.alexthe666.alexsmobs.entity.EntityVineLasso;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class ItemVineLasso extends Item implements IItemRender {

    public ItemVineLasso(Item.Settings props) {
        super(props);
    }

    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    public static boolean isItemInUse(ItemStack stack){
        return stack.getNbt() != null && stack.getNbt().contains("Swinging") && stack.getNbt().getBoolean("Swinging");
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int i, boolean b) {
        if(entity instanceof LivingEntity){
            if(stack.getNbt() != null){
                stack.getNbt().putBoolean("Swinging", ((LivingEntity) entity).getActiveItem() == stack && ((LivingEntity) entity).isUsingItem());
            }else{
                stack.setNbt(new NbtCompound());
            }
        }
    }

    @Override
    public int getMaxUseTime(ItemStack p_40680_) {
        return 72000;
    }

    @Override
    public TypedActionResult<ItemStack> use(World p_40672_, PlayerEntity p_40673_, Hand p_40674_) {
        var itemstack = p_40673_.getStackInHand(p_40674_);
        p_40673_.setCurrentHand(p_40674_);

        return TypedActionResult.success(itemstack);
    }

    @Override
    public void usageTick(World worldIn, LivingEntity livingEntityIn, ItemStack stack, int count) {
        if(count % 7 == 0){
            livingEntityIn.emitGameEvent(GameEvent.ITEM_INTERACT_START);
            livingEntityIn.playSound(AMSoundRegistry.VINE_LASSO.get(),1.0F, 1.0F + (livingEntityIn.getRandom().nextFloat() - livingEntityIn.getRandom().nextFloat()) * 0.2F);
        }
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World worldIn, LivingEntity livingEntityIn, int i) {
        if (!worldIn.isClient) {
            boolean left = false;
            if (livingEntityIn.getActiveHand() == Hand.OFF_HAND && livingEntityIn.getMainArm() == Arm.RIGHT || livingEntityIn.getActiveHand() == Hand.MAIN_HAND && livingEntityIn.getMainArm() == Arm.LEFT) {
                left = true;
            }
            int power = this.getMaxUseTime(stack) - i;
            var lasso = new EntityVineLasso(worldIn, livingEntityIn);
            Vec3d vector3d = livingEntityIn.getRotationVec(1.0F);
            lasso.shoot(vector3d.x, vector3d.y, vector3d.z, getPowerForTime(power), 1);
            if (!worldIn.isClient) {
                worldIn.spawnEntity(lasso);
            }
            stack.decrement(1);
        }
        //livingEntityIn.incrementStat(Stats.USED.getOrCreateStat(this));
    }

    public static float getPowerForTime(int p) {
        float f = (float)p / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        var world = MinecraftClient.getInstance().world;

        matrices.translate(0.5F, 0.5f, 0.5f);
        if (mode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND || mode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND || mode == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND || mode == ModelTransformationMode.FIRST_PERSON_LEFT_HAND) {
            if (ItemVineLasso.isItemInUse(stack)) {
                if (mode.isFirstPerson()) {
                    matrices.translate(mode == ModelTransformationMode.FIRST_PERSON_LEFT_HAND ? -0.3F : 0.3F, 0.0f, -0.5f);
                }
                matrices.multiply(RotationAxis.POSITIVE_Y.rotation(AMItemstackRenderer.getTicks() + MinecraftClient.getInstance().getTickDelta()));
            }
            MinecraftClient.getInstance().getItemRenderer().renderItem(new ItemStack(AMItemRegistry.VINE_LASSO_HAND.get()), mode, light, overlay, matrices, vertexConsumers, world, 0);
        } else {
            MinecraftClient.getInstance().getItemRenderer().renderItem(new ItemStack(AMItemRegistry.VINE_LASSO_INVENTORY.get()), mode, mode == ModelTransformationMode.GROUND ? light : 240, overlay, matrices, vertexConsumers, world, 0);
        }
    }
}
