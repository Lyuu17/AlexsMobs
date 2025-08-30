package com.github.alexthe666.alexsmobs.forge.entity;

import com.github.alexthe666.alexsmobs.entity.EntityAlligatorSnappingTurtle;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraftforge.common.IForgeShearable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class EntityAlligatorSnappingTurtleForge extends EntityAlligatorSnappingTurtle implements IForgeShearable {

    public EntityAlligatorSnappingTurtleForge(EntityType<EntityAlligatorSnappingTurtleForge> type, World worldIn) {
        super(type, worldIn);
    }

    @Override
    public boolean isShearable(@NotNull ItemStack item, World world, BlockPos pos) {
        return isShearable();
    }

    @NotNull
    @Override
    public List<ItemStack> onSheared(@Nullable PlayerEntity player, @NotNull ItemStack item, World level, BlockPos pos, int fortune) {
        level.playSoundFromEntity(null, this, SoundEvents.ENTITY_SHEEP_SHEAR, player == null ? SoundCategory.BLOCKS : SoundCategory.PLAYERS, 1.0F, 1.0F);
        this.emitGameEvent(GameEvent.ENTITY_INTERACT);
        if (!level.isClient()) {
            if (random.nextFloat() < this.getMoss() * 0.05F) {
                this.setMoss(0);
                return Collections.singletonList(new ItemStack(AMItemRegistry.SPIKED_SCUTE.get()));
            } else {
                this.setMoss(0);
                return Collections.singletonList(new ItemStack(Items.SEAGRASS));
            }
        }
        return Collections.emptyList();
    }
}
