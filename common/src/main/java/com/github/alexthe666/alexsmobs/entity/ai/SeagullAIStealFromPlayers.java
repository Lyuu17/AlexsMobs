package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.EntitySeagull;
import com.github.alexthe666.alexsmobs.registry.AMAdvancementTriggerRegistry;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class SeagullAIStealFromPlayers extends Goal {

    private final EntitySeagull seagull;
    private Vec3d fleeVec = null;
    private PlayerEntity target;
    private int fleeTime = 0;

    public SeagullAIStealFromPlayers(EntitySeagull entitySeagull) {
        this.setControls(EnumSet.of(Control.MOVE, Control.TARGET));
        this.seagull = entitySeagull;
    }

    @Override
    public boolean canStart() {
        long worldTime = this.seagull.getWorld().getTime() % 10;
        if (this.seagull.getDespawnCounter() >= 100 && worldTime != 0 || seagull.isSitting() || !AMConfig.seagullStealing) {
            return false;
        }
        if (this.seagull.getRandom().nextInt(12) != 0 && worldTime != 0 || seagull.stealCooldown > 0) {
            return false;
        }
        if(this.seagull.getMainHandStack().isEmpty()){
            var valid = getClosestValidPlayer();
            if(valid != null){
                target = valid;
                return true;
            }
        }
        return false;
    }

    @Override
    public void start(){
        this.seagull.aiItemFlag = true;
    }

    @Override
    public void stop(){
        this.seagull.aiItemFlag = false;
        target = null;
        fleeVec = null;
        fleeTime = 0;
    }

    @Override
    public boolean shouldContinue() {
        return target != null && !target.isCreative() && (seagull.getMainHandStack().isEmpty() || fleeTime > 0);
    }

    @Override
    public void tick(){
        seagull.setFlying(true);
        seagull.getMoveControl().moveTo(target.getX(), target.getEyeY(), target.getZ(), 1.2F);
        if(seagull.distanceTo(target) < 2F && seagull.getMainHandStack().isEmpty()){
            if(hasFoods(target)){
                var foodStack = getFoodItemFrom(target);
                if(!foodStack.isEmpty()){
                    var copy = foodStack.copy();
                    foodStack.decrement(1);
                    copy.setCount(1);
                    seagull.peck();
                    seagull.setStackInHand(Hand.MAIN_HAND, copy);
                    fleeTime = 60;
                    seagull.stealCooldown = 1500 + seagull.getRandom().nextInt(1500);
                    if(target instanceof ServerPlayerEntity){
                        AMAdvancementTriggerRegistry.SEAGULL_STEAL.trigger((ServerPlayerEntity) target);
                    }
                }else{
                    stop();
                }
            }else{
                stop();
            }
        }
        if(fleeTime > 0){
            if(fleeVec == null){
                fleeVec = seagull.getBlockInViewAway(target.getPos(), 4);
            }
            if(fleeVec != null){
                seagull.setFlying(true);
                seagull.getMoveControl().moveTo(fleeVec.x, fleeVec.y, fleeVec.z, 1.2F);
                if(seagull.squaredDistanceTo(fleeVec) < 5){
                    fleeVec = seagull.getBlockInViewAway(fleeVec, 4);
                }
            }
            fleeTime--;
        }
    }

    private PlayerEntity getClosestValidPlayer(){
        var list = seagull.getWorld().getEntitiesByClass(PlayerEntity.class, seagull.getBoundingBox().expand(10, 25, 10), EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR);
        PlayerEntity closest = null;
        if(!list.isEmpty()){
            for(var player : list){
                if((closest == null || closest.distanceTo(seagull) > player.distanceTo(seagull)) && hasFoods(player)){
                    closest = player;
                }
            }
        }
        return closest;
    }

    private boolean hasFoods(PlayerEntity player){
        for(int i = 0; i < 9; i++){
            var stackIn = player.getInventory().main.get(i);
            if(stackIn.isFood() && !isBlacklisted(stackIn)){
                return true;
            }
        }
        return false;
    }

    private boolean isBlacklisted(ItemStack stack){
        Identifier loc = Registries.ITEM.getId(stack.getItem());
        if(loc != null){
            for(String str : AMConfig.seagullStealingBlacklist){
                if(loc.toString().equals(str)){
                    return true;
                }
            }
        }
        return false;
    }

    private ItemStack getFoodItemFrom(PlayerEntity player){
        List<ItemStack> foods = new ArrayList<>();
        for(int i = 0; i < 9; i++){
            ItemStack stackIn = player.getInventory().main.get(i);
            if(stackIn.isFood() && !isBlacklisted(stackIn)){
                foods.add(stackIn);
            }
        }
        if(!foods.isEmpty()){
            return foods.get(foods.size() <= 1 ? 0 : seagull.getRandom().nextInt(foods.size() - 1));
        }
        return ItemStack.EMPTY;
    }
}
