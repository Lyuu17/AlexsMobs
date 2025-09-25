package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.*;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import vazkii.patchouli.api.PatchouliAPI;

import java.util.List;

public class ItemAnimalDictionary extends Item {

    private boolean usedOnEntity = false;

    public ItemAnimalDictionary(Settings properties) {
        super(properties);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity playerIn, LivingEntity target, Hand hand) {
        var itemStackIn = playerIn.getStackInHand(hand);
        if (playerIn instanceof ServerPlayerEntity serverplayerentity) {
            Criteria.CONSUME_ITEM.trigger(serverplayerentity, itemStackIn);
            serverplayerentity.incrementStat(Stats.USED.getOrCreateStat(this));

            if (target.getSavedEntityId() != null && target.getSavedEntityId().contains(AlexsMobs.MOD_ID + ":")) {
                usedOnEntity = true;
                var entryId = target.getSavedEntityId();
                if(target instanceof EntityBoneSerpent || target instanceof EntityBoneSerpentPart){
                    entryId = "bone_serpent";
                }
                if(target instanceof EntityCentipedeHead || target instanceof EntityCentipedeBody || target instanceof EntityCentipedeTail){
                    entryId = "cave_centipede";
                }
                if(target instanceof EntityVoidWorm || target instanceof EntityVoidWormPart){
                    entryId = "void_worm";
                }
                if(target instanceof EntityAnaconda || target instanceof EntityAnacondaPart){
                    entryId = "anaconda";
                }
                if(target instanceof EntityMurmur || target instanceof EntityMurmurHead){
                    entryId = "murmur";
                }
                PatchouliAPI.get().openBookGUI(serverplayerentity, new Identifier(entryId));
            }
        }
        return ActionResult.CONSUME;
    }

    @Override
    public TypedActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack itemStackIn = playerIn.getStackInHand(handIn);
        if (!usedOnEntity) {
            if (playerIn instanceof ServerPlayerEntity serverplayerentity) {
                Criteria.CONSUME_ITEM.trigger(serverplayerentity, itemStackIn);
                serverplayerentity.incrementStat(Stats.USED.getOrCreateStat(this));

                PatchouliAPI.get().openBookGUI(serverplayerentity, new Identifier(AlexsMobs.MOD_ID, "animal_dictionary"));
            }
        }
        usedOnEntity = false;

        return new TypedActionResult<>(ActionResult.PASS, itemStackIn);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World worldIn, List<Text> tooltip, TooltipContext flagIn) {
        tooltip.add(Text.translatable("item.alexsmobs.animal_dictionary.desc").formatted(Formatting.GRAY));
    }
}
