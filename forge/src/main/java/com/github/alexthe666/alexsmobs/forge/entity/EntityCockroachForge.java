package com.github.alexthe666.alexsmobs.forge.entity;

import com.github.alexthe666.alexsmobs.entity.EntityCockroach;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EntityCockroachForge extends EntityCockroach implements net.minecraftforge.common.IForgeShearable {

    public EntityCockroachForge(EntityType<EntityCockroachForge> type, World world) {
        super(type, world);
    }

    @NotNull
    @Override
    public List<ItemStack> onSheared(@Nullable PlayerEntity player, @NotNull ItemStack item, World world, BlockPos pos, int fortune) {
        world.playSoundFromEntity(null, this, SoundEvents.ENTITY_SHEEP_SHEAR, player == null ? SoundCategory.BLOCKS : SoundCategory.PLAYERS, 1.0F, 1.0F);
        this.emitGameEvent(GameEvent.ENTITY_INTERACT);
        this.damage(getDamageSources().generic(), 0F);
        if (!world.isClient) {
            for (int i = 0; i < 3; i++) {
                ((ServerWorld) this.getWorld()).spawnParticles(ParticleTypes.SNEEZE, this.getParticleX(0.52F), this.getBodyY(1D), this.getParticleZ(0.52F), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        this.setHeadless(true);
        return java.util.Collections.emptyList();
    }
}