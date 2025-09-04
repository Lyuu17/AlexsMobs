package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.packet.HurtMultipartPacket;
import com.github.alexthe666.alexsmobs.packet.InteractMultipartPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EntityCachalotPart extends PartEntity<EntityCachalotWhale> {

    private final EntityDimensions size;
    public float scale = 1;

    public EntityCachalotPart(EntityCachalotWhale parent, float sizeX, float sizeY) {
        super(parent);
        this.size = EntityDimensions.changing(sizeX, sizeY);
        this.calculateDimensions();
    }

    public EntityCachalotPart(EntityCachalotWhale entityCachalotWhale, float sizeX, float sizeY, EntityDimensions size) {
        super(entityCachalotWhale);
        this.size = size;
    }

    protected void collideWithNearbyEntities() {
        final List<Entity> entities = this.getWorld().getOtherEntities(this, this.getBoundingBox().stretch(0.2D, 0.0D, 0.2D));
        Entity parent = this.getParent();
        if (parent != null) {
            entities.stream().filter(entity -> entity != parent && !(entity instanceof EntityCachalotPart && ((EntityCachalotPart) entity).getParent() == parent) && entity.isPushable()).forEach(entity -> entity.pushAwayFrom(parent));
        }
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (this.getWorld().isClient && this.getParent() != null){
            AlexsMobs.sendMSGToServer(new InteractMultipartPacket(this.getParent().getId(), hand == Hand.OFF_HAND));
        }
        return this.getParent() == null ? ActionResult.PASS : this.getParent().interact(player, hand);
    }

    @Deprecated
    protected void collideWithEntity(Entity entityIn) {
        entityIn.pushAwayFrom(this);
    }

    @Override
    public boolean canHit() {
        return true;
    }

    @Nullable
    @Override
    public ItemStack getPickBlockStack() {
        Entity parent = this.getParent();
        return parent != null ? parent.getPickBlockStack() : ItemStack.EMPTY;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if(this.getWorld().isClient && this.getParent() != null && !this.getParent().isInvulnerableTo(source)){
            var key = this.getWorld().getRegistryManager().getOptional(RegistryKeys.DAMAGE_TYPE).get().getKey(source.getType());
            if(key != null){
                AlexsMobs.sendMSGToServer(new HurtMultipartPacket(this.getId(), this.getParent().getId(), amount, key.toString()));
            }
        }
        return !this.isInvulnerableTo(source) && this.getParent().attackEntityPartFrom(this, source, amount);
    }

    public boolean is(Entity entityIn) {
        return this == entityIn || this.getParent() == entityIn;
    }

    @Override
    public EntityDimensions getDimensions(EntityPose poseIn) {
        return this.size == null ? EntityDimensions.changing(0, 0) : this.size.scaled(scale);
    }

    @Override
    protected void initDataTracker() {

    }

    @Override
    public void tick(){
        super.tick();
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound compound) {

    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound compound) {

    }
}
