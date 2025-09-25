package com.github.alexthe666.alexsmobs.mixin;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.EntityMimicOctopus;
import com.github.alexthe666.alexsmobs.entity.EntitySeaBear;
import com.github.alexthe666.alexsmobs.entity.util.FlyingFishBootsUtil;
import com.github.alexthe666.alexsmobs.entity.util.RockyChestplateUtil;
import com.github.alexthe666.alexsmobs.entity.util.VineLassoUtil;
import com.github.alexthe666.alexsmobs.registry.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private static final UUID SAND_SPEED_MODIFIER = UUID.fromString("7E0292F2-9434-48D5-A29F-9583AF7DF28E");
    @Unique
    private static final UUID SNEAK_SPEED_MODIFIER = UUID.fromString("7E0292F2-9434-48D5-A29F-9583AF7DF28F");
    @Unique
    private static final EntityAttributeModifier SAND_SPEED_BONUS = new EntityAttributeModifier(SAND_SPEED_MODIFIER, "roadrunner speed bonus", 0.1F, EntityAttributeModifier.Operation.ADDITION);
    @Unique
    private static final EntityAttributeModifier SNEAK_SPEED_BONUS = new EntityAttributeModifier(SNEAK_SPEED_MODIFIER, "frontier cap speed bonus", 0.1F, EntityAttributeModifier.Operation.ADDITION);

    @Unique
    private BlockPos alexsmobs$getDownPos(BlockPos entered, WorldAccess world) {
        int i = 0;
        while (world.isAir(entered) && i < 3) {
            entered = entered.down();
            i++;
        }
        return entered;
    }

    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    private void tick(CallbackInfo ci) {

        final var entity = (LivingEntity) (Object)this;
        if (entity instanceof PlayerEntity player) {
            if (player.getStandingEyeHeight() < player.getHeight() * 0.5D) {
                player.calculateDimensions();
            }
            if(player.getAttributes().hasAttribute(EntityAttributes.GENERIC_MOVEMENT_SPEED)){
                final var attributes = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                if (player.getEquippedStack(EquipmentSlot.FEET).getItem() == AMItemRegistry.ROADDRUNNER_BOOTS.get()
                        || attributes.hasModifier(SAND_SPEED_BONUS)) {
                    final boolean sand = player.getWorld().getBlockState(alexsmobs$getDownPos(player.getBlockPos(), player.getWorld()))
                            .isIn(BlockTags.SAND);
                    if (sand && !attributes.hasModifier(SAND_SPEED_BONUS)) {
                        attributes.addPersistentModifier(SAND_SPEED_BONUS);
                    }
                    if (player.age % 25 == 0
                            && (player.getEquippedStack(EquipmentSlot.FEET).getItem() != AMItemRegistry.ROADDRUNNER_BOOTS.get()
                            || !sand)
                            && attributes.hasModifier(SAND_SPEED_BONUS)) {
                        attributes.removeModifier(SAND_SPEED_BONUS);
                    }
                }
                if (player.getEquippedStack(EquipmentSlot.HEAD).getItem() == AMItemRegistry.FRONTIER_CAP.get()
                        || attributes.hasModifier(SNEAK_SPEED_BONUS)) {
                    final var shift = player.isSneaking();
                    if (shift && !attributes.hasModifier(SNEAK_SPEED_BONUS)) {
                        attributes.addPersistentModifier(SNEAK_SPEED_BONUS);
                    }
                    if ((!shift || player.getEquippedStack(EquipmentSlot.HEAD).getItem() != AMItemRegistry.FRONTIER_CAP.get())
                            && attributes.hasModifier(SNEAK_SPEED_BONUS)) {
                        attributes.removeModifier(SNEAK_SPEED_BONUS);
                    }
                }
            }
            if (player.getEquippedStack(EquipmentSlot.HEAD).getItem() == AMItemRegistry.SPIKED_TURTLE_SHELL.get()) {
                if (!player.isSubmergedIn(FluidTags.WATER)) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 310, 0, false, false, true));
                }
            }
        }
        final var boots = entity.getEquippedStack(EquipmentSlot.FEET);
        if (!boots.isEmpty() && boots.hasNbt() && boots.getOrCreateNbt().contains("BisonFur") && boots.getOrCreateNbt().getBoolean("BisonFur")) {
            var posBelow = new BlockPos((int) entity.getX(), (int) (entity.getBoundingBox().minY - 0.1F), (int) entity.getZ());
            if (entity.getWorld().getBlockState(posBelow).isOf(Blocks.POWDER_SNOW)) {
                entity.setOnGround(true);
                entity.setFrozenTicks(0);
                entity.setPos(entity.getX(), Math.max(entity.getY(), posBelow.getY() + 1F), entity.getZ());
            }
            if (entity.inPowderSnow) {
                entity.setOnGround(true);
                entity.setVelocity(entity.getVelocity().add(0, 0.1F, 0));
            }
        }
        if (entity.getEquippedStack(EquipmentSlot.LEGS).getItem() == AMItemRegistry.CENTIPEDE_LEGGINGS.get()) {
            if (entity.horizontalCollision && !entity.isTouchingWater()) {
                entity.fallDistance = 0.0F;
                var motion = entity.getVelocity();
                double d2 = 0.1D;
                if (entity.isSneaking() || !entity.getBlockStateAtPos().isOf(Blocks.SCAFFOLDING) && entity.isHoldingOntoLadder()) {
                    d2 = 0.0D;
                }
                motion = new Vec3d(MathHelper.clamp(motion.x, -0.15F, 0.15F), d2, MathHelper.clamp(motion.z, -0.15F, 0.15F));
                entity.setVelocity(motion);
            }
        }
        if (entity.getEquippedStack(EquipmentSlot.HEAD).getItem() == AMItemRegistry.SOMBRERO.get() && !entity.getWorld().isClient && AlexsMobs.isAprilFools() && entity.isInsideWaterOrBubbleColumn()) {
            var random = entity.getRandom();
            if (random.nextInt(245) == 0 && !EntitySeaBear.isMobSafe(entity)) {
                final int dist = 32;
                final var nearbySeabears = entity.getWorld().getNonSpectatingEntities(EntitySeaBear.class,
                        entity.getBoundingBox().expand(dist, dist, dist));
                if (nearbySeabears.isEmpty()) {
                    final var bear = AMEntityRegistry.SEA_BEAR.get().create(entity.getWorld());
                    final var at = entity.getBlockPos();
                    BlockPos farOff = null;
                    for (int i = 0; i < 15; i++) {
                        final int f1 = (int) Math.signum(random.nextInt() - 0.5F);
                        final int f2 = (int) Math.signum(random.nextInt() - 0.5F);
                        final BlockPos pos1 = at.add(f1 * (10 + random.nextInt(dist - 10)), random.nextInt(1),
                                f2 * (10 + random.nextInt(dist - 10)));
                        if (entity.getWorld().isWater(pos1)) {
                            farOff = pos1;
                        }
                    }
                    if (farOff != null) {
                        bear.setPos(farOff.getX() + 0.5F, farOff.getY() + 0.5F, farOff.getZ() + 0.5F);
                        bear.setYaw(random.nextFloat() * 360F);
                        bear.setTarget(entity);
                        entity.getWorld().spawnEntity(bear);
                    }
                } else {
                    for (var bear : nearbySeabears) {
                        bear.setTarget(entity);
                    }
                }
            }
        }
        if (VineLassoUtil.hasLassoData(entity)) {
            VineLassoUtil.tickLasso(entity);
        }
        if (RockyChestplateUtil.isWearing(entity)) {
            RockyChestplateUtil.tickRockyRolling(entity);
        }
        if (FlyingFishBootsUtil.isWearing(entity)) {
            FlyingFishBootsUtil.tickFlyingFishBoots(entity);
        }
    }

    @Inject(
            method = "consumeItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;finishUsing(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/item/ItemStack;"
            )
    )
    private void consumeItem(CallbackInfo ci) {

        final var self = (LivingEntity) (Object)this;
        final var item = self.getActiveItem();

        if (item.getItem() == Items.CHORUS_FRUIT && new Random().nextInt(3) == 0
                && self.hasStatusEffect(AMEffectRegistry.ENDER_FLU.get())) {
            self.removeStatusEffect(AMEffectRegistry.ENDER_FLU.get());
        }
    }

    @Inject(method = "dropLoot", at = @At("TAIL"))
    private void dropLoot(DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
        final var self = (LivingEntity)(Object)this;

        if (VineLassoUtil.hasLassoData(self)) {
            VineLassoUtil.lassoTo(null, self);
            self.dropStack(new ItemStack(AMItemRegistry.VINE_LASSO.get()));
        }
    }

    @Inject(method = "canTarget(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void canTarget(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        final var self = (LivingEntity)(Object)this;

        if (target != null && self instanceof MobEntity mob) {
            if (mob.getGroup() == EntityGroup.ARTHROPOD) {
                if (target.hasStatusEffect(AMEffectRegistry.BUG_PHEROMONES.get()) && self.getAttacker() != target) {
                    cir.setReturnValue(false);
                }
            }
            if (mob.getGroup() == EntityGroup.UNDEAD && !mob.getType().isIn(AMTagRegistry.IGNORES_KIMONO)) {
                if (target.getEquippedStack(EquipmentSlot.CHEST).isOf(AMItemRegistry.UNSETTLING_KIMONO.get()) && self.getAttacker() != target) {
                    cir.setReturnValue(false);
                }
            }
        }
    }

    @Inject(
            method = "damage",
            at = @At("HEAD"),
            cancellable = true
    )
    private void damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        final var self = (LivingEntity) (Object) this;

        if (source.getAttacker() instanceof final LivingEntity attacker) {
            if (amount > 0 && attacker.hasStatusEffect(AMEffectRegistry.SOULSTEAL.get()) && attacker.getStatusEffect(AMEffectRegistry.SOULSTEAL.get()) != null) {
                final int level = attacker.getStatusEffect(AMEffectRegistry.SOULSTEAL.get()).getAmplifier() + 1;
                if (attacker.getHealth() < attacker.getMaxHealth()
                        && ThreadLocalRandom.current().nextFloat() < (0.25F + (level * 0.25F))) {
                    attacker.heal(Math.min(amount / 2F * level, 2 + 2 * level));
                }
            }

            if (self instanceof final PlayerEntity player) {
                if (attacker instanceof final EntityMimicOctopus octupus && octupus.isOwner(player)) {
                    cir.cancel();
                    return;
                }
                if (player.getEquippedStack(EquipmentSlot.HEAD).getItem() == AMItemRegistry.SPIKED_TURTLE_SHELL.get()) {
                    if (attacker.distanceTo(player) < attacker.getWidth() + player.getWidth() + 0.5F) {
                        attacker.damage(attacker.getDamageSources().thorns(player), 1F);
                        attacker.takeKnockback(0.5F, MathHelper.sin((attacker.getYaw() + 180) * MathHelper.RADIANS_PER_DEGREE),
                                -MathHelper.cos((attacker.getYaw() + 180) * MathHelper.RADIANS_PER_DEGREE));
                    }
                }
            }
        }
        if (!self.getEquippedStack(EquipmentSlot.LEGS).isEmpty() && self.getEquippedStack(EquipmentSlot.LEGS).getItem() == AMItemRegistry.EMU_LEGGINGS.get()) {
            if (source.isIn(DamageTypeTags.IS_PROJECTILE) && self.getRandom().nextFloat() < AMConfig.emuPantsDodgeChance) {
                cir.cancel();
            }
        }
    }
}
