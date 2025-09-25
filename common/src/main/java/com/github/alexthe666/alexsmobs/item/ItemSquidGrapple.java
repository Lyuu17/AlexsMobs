package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.EntitySquidGrapple;
import com.github.alexthe666.alexsmobs.entity.util.SquidGrappleUtil;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemSquidGrapple extends Item {

    public ItemSquidGrapple(Item.Settings properties) {
        super(properties);
    }

    @Override
    public int getMaxUseTime(ItemStack p_40680_) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public TypedActionResult<ItemStack> use(World p_40672_, PlayerEntity p_40673_, Hand p_40674_) {
        var itemstack = p_40673_.getStackInHand(p_40674_);
        p_40673_.setCurrentHand(p_40674_);

        return TypedActionResult.pass(itemstack);
    }

    @Override
    public void usageTick(World worldIn, LivingEntity livingEntityIn, ItemStack stack, int count) {

    }

    @Override
    public void onStoppedUsing(ItemStack stack, World worldIn, LivingEntity livingEntityIn, int i) {
        if(livingEntityIn.isFallFlying()){
            return;
        }
        livingEntityIn.playSound(AMSoundRegistry.GIANT_SQUID_TENTACLE.get(),1.0F, 1.0F + (livingEntityIn.getRandom().nextFloat() - livingEntityIn.getRandom().nextFloat()) * 0.2F);
        livingEntityIn.emitGameEvent(GameEvent.ITEM_INTERACT_FINISH);
        if (!worldIn.isClient) {
            boolean left = false;
            if (livingEntityIn.getActiveHand() == Hand.OFF_HAND && livingEntityIn.getMainArm() == Arm.RIGHT || livingEntityIn.getActiveHand() == Hand.MAIN_HAND && livingEntityIn.getMainArm() == Arm.LEFT) {
                left = true;
            }
            int power = this.getMaxUseTime(stack) - i;
            EntitySquidGrapple hook = new EntitySquidGrapple(worldIn, livingEntityIn, !left);
            Vec3d vector3d = livingEntityIn.getRotationVec(1.0F);
            hook.shoot(vector3d.x, vector3d.y, vector3d.z, getPowerForTime(power) * 3, 1);
            hook.setPitch(livingEntityIn.getPitch());
            hook.setYaw(livingEntityIn.getYaw());
            if (!worldIn.isClient) {
                worldIn.spawnEntity(hook);
            }
            stack.damage(1, livingEntityIn, (playerIn) -> {
                livingEntityIn.sendToolBreakStatus(playerIn.getActiveHand());
            });
            SquidGrappleUtil.onFireHook(livingEntityIn, hook.getUuid());
        }
    }

    @Override
    public boolean canRepair(ItemStack s, ItemStack s1) {
        return s1.isOf(AMItemRegistry.LOST_TENTACLE.get());
    }

    public static float getPowerForTime(int p) {
        float f = (float)p / 20.0F;
        f = (f * f + f + f * 2.0F) / 4.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World worldIn, List<Text> tooltip, TooltipContext flagIn) {
        tooltip.add(Text.translatable("item.alexsmobs.squid_grapple.desc").formatted(Formatting.GRAY));

    }
}
