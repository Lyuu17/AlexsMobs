package com.github.alexthe666.alexsmobs.forge.entity;

import com.github.alexthe666.alexsmobs.entity.EntityBison;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EntityBisonForge extends EntityBison implements net.minecraftforge.common.IForgeShearable {

    public EntityBisonForge(EntityType<EntityBisonForge> animal, World lvl) {
        super(animal, lvl);
    }

    @NotNull
    @Override
    public List<ItemStack> onSheared(@javax.annotation.Nullable PlayerEntity player, @NotNull ItemStack item, World world, BlockPos pos, int fortune) {
        world.playSoundFromEntity(null, this, SoundEvents.ENTITY_SHEEP_SHEAR, player == null ? SoundCategory.BLOCKS : SoundCategory.PLAYERS, 1.0F, 1.0F);
        this.emitGameEvent(GameEvent.ENTITY_INTERACT);
        final List<ItemStack> list = new ArrayList<>(6);
        for (int i = 0; i < 2 + random.nextInt(2); i++) {
            list.add(new ItemStack(AMItemRegistry.BISON_FUR.get()));
        }
        this.feedingsSinceLastShear = 0;
        this.setSheared(true);
        return list;
    }
}