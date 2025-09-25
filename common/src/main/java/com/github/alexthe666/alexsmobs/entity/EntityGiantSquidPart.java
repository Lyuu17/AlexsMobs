package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.packet.HurtMultipartPacket;
import com.github.alexthe666.alexsmobs.packet.InteractMultipartPacket;
import dev.architectury.networking.NetworkManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class EntityGiantSquidPart extends PartEntity<EntityGiantSquid> implements IHurtableMultipart {

    private final EntityDimensions size;
    public float scale = 1;
    private boolean collisionOnly = false;

    public EntityGiantSquidPart(EntityGiantSquid parent, float sizeX, float sizeY) {
        super(parent);
        this.size = EntityDimensions.changing(sizeX, sizeY);
        this.calculateDimensions();
    }

    public EntityGiantSquidPart(EntityGiantSquid parent, float sizeX, float sizeY, boolean collisionOnly) {
        this(parent, sizeX, sizeY);
        this.collisionOnly = collisionOnly;
    }

    @Override
    public boolean isFireImmune() {
        return true;
    }

    @Override
    public Vec3d getLeashOffset() {
        return new Vec3d(0.0D, (double)this.getStandingEyeHeight() * 0.15F, (double)(this.getWidth() * 0.1F));
    }

    @Deprecated
    protected void collideWithNearbyEntities() {

    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if(this.getWorld().isClient && this.getParent() != null){
            AlexsMobs.sendMSGToServer(new InteractMultipartPacket(this.getParent().getId(), hand == Hand.OFF_HAND));
        }
        return this.getParent() == null ? ActionResult.PASS : this.getParent().interactMob(player, hand);
    }

    @Override
    public boolean isCollidable() {
        return !collisionOnly;
    }

    @Deprecated
    protected void collideWithEntity(Entity entityIn) {
        if(!collisionOnly){
            entityIn.pushAwayFrom(this);
        }
    }

    @Override
    public boolean canHit() {
        return !collisionOnly;
    }

    @Nullable
    @Override
    public ItemStack getPickBlockStack() {
        Entity parent = this.getParent();
        return parent != null ? parent.getPickBlockStack() : ItemStack.EMPTY;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.getWorld().isClient && this.getParent() != null && !this.getParent().isInvulnerableTo(source) && !collisionOnly){
            AlexsMobs.sendMSGToServer(new HurtMultipartPacket(this.getId(), this.getParent().getId(), amount, source.getName()));
        }
        return !collisionOnly && !this.isInvulnerableTo(source) && this.getParent().attackEntityPartFrom(this, source, amount);
    }

    @Override
    public boolean isPartOf(Entity entityIn) {
        return this == entityIn || this.getParent() == entityIn;
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return NetworkManager.createAddEntityPacket(this);
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

    @Override
    public void onAttackedFromServer(LivingEntity parent, float damage, DamageSource damageSource) {
        parent.damage(damageSource, damage);
    }
}
