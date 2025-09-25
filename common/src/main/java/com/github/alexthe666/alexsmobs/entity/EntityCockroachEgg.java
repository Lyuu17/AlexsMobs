package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class EntityCockroachEgg extends ThrownItemEntity {

    public EntityCockroachEgg(EntityType<? extends EntityCockroachEgg> p_i50154_1_, World p_i50154_2_) {
        super(p_i50154_1_, p_i50154_2_);
    }

    public EntityCockroachEgg(World worldIn, LivingEntity throwerIn) {
        super(AMEntityRegistry.COCKROACH_EGG.get(), throwerIn, worldIn);
    }

    public EntityCockroachEgg(World worldIn, double x, double y, double z) {
        super(AMEntityRegistry.COCKROACH_EGG.get(), x, y, z, worldIn);
    }

    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if (id == 3) {
            for (int i = 0; i < 8; ++i) {
                this.getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, this.getItem()), this.getX(), this.getY(), this.getZ(), ((double)this.random.nextFloat() - 0.5D) * 0.08D, ((double)this.random.nextFloat() - 0.5D) * 0.08D, ((double)this.random.nextFloat() - 0.5D) * 0.08D);
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
            this.getWorld().sendEntityStatus(this, (byte)3);
            int i = random.nextInt(3);
            for (int j = 0; j < i; ++j) {
                final var croc = AMEntityRegistry.COCKROACH.get().create(this.getWorld());
                croc.setBreedingAge(-24000);
                croc.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), this.getYaw(), 0.0F);
                croc.initialize((ServerWorld)getWorld(), getWorld().getLocalDifficulty(this.getBlockPos()), SpawnReason.TRIGGERED, null, null);
                croc.setPositionTarget(this.getBlockPos(), 20);
                this.getWorld().spawnEntity(croc);
            }
            this.getWorld().sendEntityStatus(this, (byte)3);
            this.remove(RemovalReason.DISCARDED);
        }

    }

    @Override
    protected Item getDefaultItem() {
        return AMItemRegistry.COCKROACH_OOTHECA.get();
    }
}
