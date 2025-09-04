package com.github.alexthe666.alexsmobs.forge.entity;

import com.github.alexthe666.alexsmobs.entity.EntityMungus;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class EntityMungusForge extends EntityMungus implements net.minecraftforge.common.IForgeShearable {

    public EntityMungusForge(EntityType<? extends EntityMungus> animal, World lvl) {
        super(animal, lvl);
    }

    @NotNull
    @Override
    public java.util.List<ItemStack> onSheared(@javax.annotation.Nullable PlayerEntity player, @NotNull ItemStack item, World world, BlockPos pos, int fortune) {
        world.playSoundFromEntity(null, this, SoundEvents.ENTITY_SHEEP_SHEAR, player == null ? SoundCategory.BLOCKS : SoundCategory.PLAYERS, 1.0F, 1.0F);
        if (!world.isClient() && this.getMushroomState() != null && this.getMushroomCount() > 0) {
            this.setMushroomCount(this.getMushroomCount() - 1);
            if (this.getMushroomCount() <= 0) {
                this.setMushroomState(null);
                this.setBeamTarget(null);
                beamCounter = Math.min(-1200, beamCounter);
            }

        }
        return java.util.Collections.emptyList();
    }
}