package com.github.alexthe666.alexsmobs.forge.entity;

import com.github.alexthe666.alexsmobs.entity.EntityBison;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EntityBisonForge extends EntityBison implements net.minecraftforge.common.IForgeShearable {

    public EntityBisonForge(EntityType<? extends Animal> animal, Level lvl) {
        super(animal, lvl);
    }

    @Override
    public boolean isShearable(@NotNull ItemStack item, Level world, BlockPos pos) {
        return this.readyForShearing();
    }

    @NotNull
    @Override
    public java.util.List<ItemStack> onSheared(@javax.annotation.Nullable Player player, @NotNull ItemStack item, Level world, BlockPos pos, int fortune) {
        world.playSound(null, this, SoundEvents.SHEEP_SHEAR, player == null ? SoundSource.BLOCKS : SoundSource.PLAYERS, 1.0F, 1.0F);
        this.gameEvent(GameEvent.ENTITY_INTERACT);
        final List<ItemStack> list = new ArrayList<>(6);
        for (int i = 0; i < 2 + random.nextInt(2); i++) {
            list.add(new ItemStack(AMItemRegistry.BISON_FUR.get()));
        }
        this.feedingsSinceLastShear = 0;
        this.setSheared(true);
        return list;
    }
}