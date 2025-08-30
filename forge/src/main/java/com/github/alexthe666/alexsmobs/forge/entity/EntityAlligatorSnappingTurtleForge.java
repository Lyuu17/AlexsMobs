package com.github.alexthe666.alexsmobs.forge.entity;

import com.github.alexthe666.alexsmobs.entity.EntityAlligatorSnappingTurtle;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.IForgeShearable;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class EntityAlligatorSnappingTurtleForge extends EntityAlligatorSnappingTurtle implements IForgeShearable {

    public EntityAlligatorSnappingTurtleForge(EntityType<? extends Animal> type, Level worldIn) {
        super(type, worldIn);
    }

    @Override
    public boolean isShearable(@NotNull ItemStack item, Level world, BlockPos pos) {
        return readyForShearing();
    }

    @NotNull
    @Override
    public java.util.List<ItemStack> onSheared(@javax.annotation.Nullable Player player, @NotNull ItemStack item, Level world, BlockPos pos, int fortune) {
        world.playSound(null, this, SoundEvents.SHEEP_SHEAR, player == null ? SoundSource.BLOCKS : SoundSource.PLAYERS, 1.0F, 1.0F);
        this.gameEvent(GameEvent.ENTITY_INTERACT);
        if (!world.isClientSide()) {
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
