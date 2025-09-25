package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.util.TendonWhipUtil;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class ItemTendonWhip extends SwordItem implements ILeftClick {

    private final ImmutableMultimap<EntityAttribute, EntityAttributeModifier> tendonModifiers;

    public ItemTendonWhip(Item.Settings props) {
        super(ToolMaterials.IRON, 3, 0, props);
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, "Weapon modifier", 4F, EntityAttributeModifier.Operation.ADDITION));
        builder.put(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID, "Weapon modifier", -3.0F, EntityAttributeModifier.Operation.ADDITION));
        this.tendonModifiers = builder.build();
    }

    public static boolean isActive(ItemStack stack, LivingEntity holder) {
        if (holder != null && (holder.getMainHandStack() == stack || holder.getOffHandStack() == stack)) {
            return !TendonWhipUtil.canLaunchTendons(holder.getWorld(), holder);
        }
        return false;
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.tendonModifiers : super.getAttributeModifiers(slot);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity entity, LivingEntity player) {
        launchTendonsAt(stack, player, entity);
        return super.postHit(stack, entity, player);
    }

    private boolean isCharged(PlayerEntity player, ItemStack stack){
        return player.getAttackCooldownProgress(0.5F) > 0.9F;
    }

    @Override
    public boolean onLeftClick(ItemStack stack, LivingEntity playerIn){
        if(stack.isOf(AMItemRegistry.TENDON_WHIP.get()) && (!(playerIn instanceof PlayerEntity) || isCharged((PlayerEntity) playerIn, stack))){
            World worldIn = playerIn.getWorld();
            Entity closestValid = null;
            var playerEyes = playerIn.getCameraPosVec(1.0F);
            HitResult hitresult = worldIn.raycast(new RaycastContext(playerEyes, playerEyes.add(playerIn.getRotationVector().multiply(12.0D)), RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, playerIn));
            if (hitresult instanceof EntityHitResult) {
                Entity entity = ((EntityHitResult) hitresult).getEntity();
                if (!entity.equals(playerIn) && !playerIn.isTeammate(entity) && !entity.isTeammate(playerIn) && entity instanceof MobEntity && playerIn.canSee(entity)) {
                    closestValid = entity;
                }
            } else {
                for (var entity : worldIn.getNonSpectatingEntities(LivingEntity.class, playerIn.getBoundingBox().expand(12.0D))) {
                    if (!entity.equals(playerIn) && !playerIn.isTeammate(entity) && !entity.isTeammate(playerIn) && entity instanceof MobEntity && playerIn.canSee(entity)) {
                        if (closestValid == null || playerIn.distanceTo(entity) < playerIn.distanceTo(closestValid)) {
                            closestValid = entity;
                        }
                    }
                }
            }
            if(closestValid != null){
                stack.damage(1, playerIn, (player) -> {
                    player.sendToolBreakStatus(playerIn.getActiveHand());
                });
            }
            return launchTendonsAt(stack, playerIn, closestValid);
        }
        return false;
    }

    public boolean launchTendonsAt(ItemStack stack, LivingEntity playerIn, Entity closestValid) {
        var worldIn = playerIn.getWorld();
        if (TendonWhipUtil.canLaunchTendons(worldIn, playerIn)) {
            TendonWhipUtil.retractFarTendons(worldIn, playerIn);
            if (!worldIn.isClient) {
                if (closestValid != null) {
                    var segment = AMEntityRegistry.TENDON_SEGMENT.get().create(worldIn);
                    segment.copyPositionAndRotation(playerIn);
                    worldIn.spawnEntity(segment);
                    segment.setCreatorEntityUUID(playerIn.getUuid());
                    segment.setFromEntityID(playerIn.getId());
                    segment.setToEntityID(closestValid.getId());
                    segment.copyPositionAndRotation(playerIn);
                    segment.setProgress(0.0F);
                    segment.setHasGlint(stack.hasGlint());
                    TendonWhipUtil.setLastTendon(playerIn, segment);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canRepair(ItemStack pickaxe, ItemStack stack) {
        return stack.isOf(AMItemRegistry.ELASTIC_TENDON.get());
    }

}
