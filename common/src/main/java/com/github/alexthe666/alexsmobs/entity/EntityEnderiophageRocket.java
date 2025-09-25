package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobsClient;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMParticleRegistry;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

public class EntityEnderiophageRocket extends FireworkRocketEntity {

    private int phageAge = 0;

    public EntityEnderiophageRocket(EntityType<? extends EntityEnderiophageRocket> p_i50164_1_, World p_i50164_2_) {
        super(p_i50164_1_, p_i50164_2_);
    }

    public EntityEnderiophageRocket(World worldIn, double x, double y, double z, ItemStack givenItem) {
        super(AMEntityRegistry.ENDERIOPHAGE_ROCKET.get(), worldIn);
        this.setPos(x, y, z);
        if (!givenItem.isEmpty() && givenItem.hasNbt()) {
            this.dataTracker.set(ITEM, givenItem.copy());
        }

        this.setVelocity(this.random.nextGaussian() * 0.001D, 0.05D, this.random.nextGaussian() * 0.001D);
        this.lifeTime = 18 + this.random.nextInt(14);
    }

    public EntityEnderiophageRocket(World p_i231581_1_, @Nullable Entity p_i231581_2_, double p_i231581_3_, double p_i231581_5_, double p_i231581_7_, ItemStack p_i231581_9_) {
        this(p_i231581_1_, p_i231581_3_, p_i231581_5_, p_i231581_7_, p_i231581_9_);
        this.setOwner(p_i231581_2_);
    }

    public EntityEnderiophageRocket(World p_i47367_1_, ItemStack p_i47367_2_, LivingEntity p_i47367_3_) {
        this(p_i47367_1_, p_i47367_3_, p_i47367_3_.getX(), p_i47367_3_.getY(), p_i47367_3_.getZ(), p_i47367_2_);
        this.dataTracker.set(SHOOTER_ENTITY_ID, OptionalInt.of(p_i47367_3_.getId()));
    }

    @Override
    public void tick() {
        super.tick();
        ++this.phageAge;
        if (this.getWorld().isClient) {
            this.getWorld().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY() - 0.3D, this.getZ(), this.random.nextGaussian() * 0.05D, -this.getVelocity().y * 0.5D, this.random.nextGaussian() * 0.05D);
        }
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return NetworkManager.createAddEntityPacket(this);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void handleStatus(byte id) {
        if (id == 17) {
            this.getWorld().addParticle(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), this.random.nextGaussian() * 0.05D, 0.005D, this.random.nextGaussian() * 0.05D);
            for(int i = 0; i < this.random.nextInt(15) + 30; ++i) {
                this.getWorld().addParticle(AMParticleRegistry.DNA.get(), this.getX(), this.getY(), this.getZ(), this.random.nextGaussian() * 0.25D, this.random.nextGaussian() * 0.25D, this.random.nextGaussian() * 0.25D);
            }
            for(int i = 0; i < this.random.nextInt(15) + 15; ++i) {
                this.getWorld().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), this.random.nextGaussian() * 0.15D, this.random.nextGaussian() * 0.15D, this.random.nextGaussian() * 0.15D);
            }
            var soundEvent = AlexsMobsClient.isFarFromCamera(this.getX(), this.getY(), this.getZ()) ? SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST : SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST_FAR;
            this.getWorld().playSound(this.getX(), this.getY(), this.getZ(), soundEvent, SoundCategory.AMBIENT, 20.0F, 0.95F + this.random.nextFloat() * 0.1F, true);
        }else{
            super.handleStatus(id);
        }
    }

    @Environment(EnvType.CLIENT)
    public ItemStack getItem() {
        return new ItemStack(AMItemRegistry.ENDERIOPHAGE_ROCKET.get());
    }

}