package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class EntityEmuEgg extends ThrownItemEntity {

    public EntityEmuEgg(EntityType<? extends EntityEmuEgg> p_i50154_1_, World p_i50154_2_) {
        super(p_i50154_1_, p_i50154_2_);
    }

    public EntityEmuEgg(World worldIn, LivingEntity throwerIn) {
        super(AMEntityRegistry.EMU_EGG.get(), throwerIn, worldIn);
    }

    public EntityEmuEgg(World worldIn, double x, double y, double z) {
        super(AMEntityRegistry.EMU_EGG.get(), x, y, z, worldIn);
    }

    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if (id == 3) {
            for (int i = 0; i < 8; ++i) {
                this.getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, this.getItem()), this.getX(), this.getY(), this.getZ(), ((double) this.random.nextFloat() - 0.5D) * 0.08D, ((double) this.random.nextFloat() - 0.5D) * 0.08D, ((double) this.random.nextFloat() - 0.5D) * 0.08D);
            }
        }
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return NetworkManager.createAddEntityPacket(this);
    }

    @Override
    protected void onCollision(HitResult result) {
        super.onCollision(result);
        if (!this.getWorld().isClient) {
            if (this.random.nextInt(8) == 0) {
                int lvt_2_1_ = 1;
                if (this.random.nextInt(32) == 0) {
                    lvt_2_1_ = 4;
                }
                for (int lvt_3_1_ = 0; lvt_3_1_ < lvt_2_1_; ++lvt_3_1_) {
                    EntityEmu lvt_4_1_ = AMEntityRegistry.EMU.get().create(this.getWorld());
                    if(this.random.nextInt(50) == 0){
                        lvt_4_1_.setVariant(2);
                    }else if(random.nextInt(3) == 0){
                        lvt_4_1_.setVariant(1);
                    }
                    lvt_4_1_.setBreedingAge(-24000);
                    lvt_4_1_.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), this.getYaw(), 0.0F);
                    this.getWorld().spawnEntity(lvt_4_1_);
                }
            }
            this.getWorld().sendEntityStatus(this, (byte) 3);
            this.remove(RemovalReason.DISCARDED);
        }

    }

    @Override
    protected Item getDefaultItem() {
        return AMItemRegistry.EMU_EGG.get();
    }
}
