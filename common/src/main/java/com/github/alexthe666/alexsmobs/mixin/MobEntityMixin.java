package com.github.alexthe666.alexsmobs.mixin;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.*;
import com.github.alexthe666.alexsmobs.registry.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.goal.UntamedActiveTargetGoal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {

    @Unique
    private static final Random alexsmobs$RAND = new Random();

    @Inject(method = "cannotDespawn", at = @At("HEAD"), cancellable = true)
    private void cannotDespawn(CallbackInfoReturnable<Boolean> cir) {
        var self = (MobEntity)(Object)this;

        if (self.hasStatusEffect(AMEffectRegistry.DEBILITATING_STING.get())
                && self.getStatusEffect(AMEffectRegistry.DEBILITATING_STING.get()) != null
                && self.getStatusEffect(AMEffectRegistry.DEBILITATING_STING.get()).getAmplifier() > 0) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "initialize", at = @At("RETURN"))
    private void initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 EntityData entityData, NbtCompound entityNbt, CallbackInfoReturnable<EntityData> cir
    ) {
        var self = (MobEntity)(Object)this;

        if (self instanceof WanderingTraderEntity trader && AMConfig.elephantTraderSpawnChance > 0) {
            var biome = world.getBiome(self.getBlockPos()).value();
            if (alexsmobs$RAND.nextFloat() <= AMConfig.elephantTraderSpawnChance && (!AMConfig.limitElephantTraderBiomes || biome.getTemperature() >= 1.0F)) {
                var chunkPos = new ChunkPos(trader.getBlockPos());
                if (world.getChunkManager().getWorldChunk(chunkPos.x, chunkPos.z) != null) {
                    var elephant = AMEntityRegistry.ELEPHANT.get().create(trader.getWorld());
                    elephant.copyPositionAndRotation(trader);
                    if (elephant.canSpawnWithTraderHere()) {
                        elephant.setTrader(true);
                        elephant.setChested(true);
                        if (!world.isClient()) {
                            trader.getWorld().spawnEntity(elephant);
                            trader.startRiding(elephant, true);
                        }
                        elephant.addElephantLoot(null, alexsmobs$RAND.nextInt());
                    }
                }
            }
        }
        try {
            if (AMConfig.spidersAttackFlies && self instanceof final SpiderEntity spider) {
                spider.targetSelector.add(4,
                        new ActiveTargetGoal<>(spider, EntityFly.class, 1, true, false, null));
            }
            else if (AMConfig.wolvesAttackMoose && self instanceof final WolfEntity wolf) {
                wolf.targetSelector.add(6, new UntamedActiveTargetGoal<>(wolf, EntityMoose.class, false, null));
            }
            else if (AMConfig.polarBearsAttackSeals && self instanceof final PolarBearEntity bear) {
                bear.targetSelector.add(6,
                        new ActiveTargetGoal<>(bear, EntitySeal.class, 15, true, true, null));
            }
            else if (self instanceof final CreeperEntity creeper) {
                creeper.targetSelector.add(3, new FleeEntityGoal<>(creeper, EntitySnowLeopard.class, 6.0F, 1.0D, 1.2D));
                creeper.targetSelector.add(3, new FleeEntityGoal<>(creeper, EntityTiger.class, 6.0F, 1.0D, 1.2D));
            }
            else if (AMConfig.catsAndFoxesAttackJerboas
                    && (self instanceof FoxEntity || self instanceof CatEntity || self instanceof OcelotEntity)) {
                self.targetSelector.add(6,
                        new ActiveTargetGoal<>(self, EntityJerboa.class, 45, true, true, null));
            }
            else if (AMConfig.bunfungusTransformation && self instanceof final RabbitEntity rabbit) {
                rabbit.goalSelector.add(3, new TemptGoal(rabbit, 1.0D, Ingredient.ofItems(AMItemRegistry.MUNGAL_SPORES.get()), false));
            }
            else if (AMConfig.dolphinsAttackFlyingFish && self instanceof final DolphinEntity dolphin) {
                dolphin.targetSelector.add(2,
                        new ActiveTargetGoal<>(dolphin, EntityFlyingFish.class, 70, true, true, null));
            }
        } catch (Exception e) {
            AlexsMobs.LOGGER.warn("Tried to add unique behaviors to vanilla mobs and encountered an error");
        }
    }
}
