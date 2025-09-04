package com.github.alexthe666.alexsmobs.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class EntityLaviathanPart extends PartEntity<EntityLaviathan> {

    private final EntityDimensions size;
    public float scale = 1;

    public EntityLaviathanPart(EntityLaviathan parent, float sizeX, float sizeY) {
        super(parent);
        this.size = EntityDimensions.changing(sizeX, sizeY);
        this.calculateDimensions();
    }

    public EntityLaviathanPart(EntityLaviathan entityCachalotWhale, float sizeX, float sizeY, EntityDimensions size) {
        super(entityCachalotWhale);
        this.size = size;
    }

    @Override
    public boolean isFireImmune() {
        return true;
    }

    @Override
    public Vec3d getLeashOffset() {
        return new Vec3d(0.0D, (double)this.getStandingEyeHeight() * 0.15F, (double)(this.getWidth() * 0.1F));
    }

    protected void collideWithNearbyEntities() {

    }

    public ActionResult getEntityInteractionResult(PlayerEntity player, Hand hand) {
        return this.getParent() == null ? ActionResult.PASS : this.getParent().interactMob(player, hand);
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    protected void collideWithEntity(Entity entityIn) {
        if(!(entityIn instanceof EntityLaviathan)){
            entityIn.pushAwayFrom(this);
        }
    }

    public boolean canHit() {
        return true;
    }

    @Nullable
    public ItemStack getPickBlockStack() {
        Entity parent = this.getParent();
        return parent != null ? parent.getPickBlockStack() : ItemStack.EMPTY;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return !this.isInvulnerableTo(source) && this.getParent().attackEntityPartFrom(this, source, amount);
    }

    @Override
    public boolean isPartOf(Entity entityIn) {
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
