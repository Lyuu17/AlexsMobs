package com.github.alexthe666.alexsmobs.forge.entity;

import com.github.alexthe666.alexsmobs.entity.EntityCockroach;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EntityCockroachForge extends EntityCockroach implements net.minecraftforge.common.IForgeShearable {

    public EntityCockroachForge(EntityType type, Level world) {
        super(type, world);
    }

    @Override
    public boolean isShearable(@NotNull ItemStack item, Level world, BlockPos pos) {
        return readyForShearing();
    }

    @NotNull
    @Override
    public java.util.List<ItemStack> onSheared(@Nullable Player player, @NotNull ItemStack item, Level world, BlockPos pos, int fortune) {
        world.playSound(null, this, SoundEvents.SHEEP_SHEAR, player == null ? SoundSource.BLOCKS : SoundSource.PLAYERS, 1.0F, 1.0F);
        this.gameEvent(GameEvent.ENTITY_INTERACT);
        this.hurt(damageSources().generic(), 0F);
        if (!world.isClientSide) {
            for (int i = 0; i < 3; i++) {
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.SNEEZE, this.getRandomX(0.52F), this.getY(1D), this.getRandomZ(0.52F), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        this.setHeadless(true);
        return java.util.Collections.emptyList();
    }
}