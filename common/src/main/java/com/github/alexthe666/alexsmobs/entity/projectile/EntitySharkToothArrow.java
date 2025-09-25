package com.github.alexthe666.alexsmobs.entity.projectile;

import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import dev.architectury.networking.NetworkManager;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Objects;

public class EntitySharkToothArrow extends ArrowEntity {

    public EntitySharkToothArrow(EntityType<EntitySharkToothArrow> type, World worldIn) {
        super(type, worldIn);
    }

    public EntitySharkToothArrow(EntityType type, double x, double y, double z, World worldIn) {
        this(type, worldIn);
        this.setPos(x, y, z);
    }

    public EntitySharkToothArrow(World worldIn, LivingEntity shooter) {
        this(AMEntityRegistry.SHARK_TOOTH_ARROW.get(), shooter.getX(), shooter.getEyeY() - (double)0.1F, shooter.getZ(), worldIn);
        this.setOwner(shooter);
        if (shooter instanceof PlayerEntity) {
            this.pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
        }
    }

    protected void damageShield(PlayerEntity player, float damage) {
        if (damage >= 3.0F && player.getActiveItem().getUseAction().equals(UseAction.BLOCK)) {
            ItemStack copyBeforeUse = player.getActiveItem().copy();
            int i = 1 + MathHelper.floor(damage);
            player.getActiveItem().damage(i, player, (p_213360_0_) -> {
                p_213360_0_.sendEquipmentBreakStatus(EquipmentSlot.CHEST);
            });

            if (player.getActiveItem().isEmpty()) {
                var hand = player.getActiveHand();
                // FIXME forge
//                net.minecraftforge.event.ForgeEventFactory.onafterBreakItem(player, copyBeforeUse, Hand);

                if (hand == Hand.MAIN_HAND) {
                    this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                } else {
                    this.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                }
                player.stopUsingItem();
                this.playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.8F, 0.8F + this.getWorld().random.nextFloat() * 0.4F);
            }
        }
    }

    @Override
    protected void onHit(LivingEntity living) {
        if (living instanceof PlayerEntity) {
            this.damageShield((PlayerEntity) living, (float) this.getDamage());
        }
        Entity entity1 = this.getOwner();
        if(living.getGroup() == EntityGroup.AQUATIC || living instanceof DrownedEntity || living.getGroup() != EntityGroup.UNDEAD && living.canBreatheInWater()){
            DamageSource damagesource;
            damagesource = getDamageSources().arrow(this, Objects.requireNonNullElse(entity1, this));
            living.damage(damagesource, 7);
        }
    }

    @Override
    public boolean isTouchingWater() {
        return false;
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return NetworkManager.createAddEntityPacket(this);
    }

    @Override
    protected ItemStack asItemStack() {
        return new ItemStack(AMItemRegistry.SHARK_TOOTH_ARROW.get());
    }

}
