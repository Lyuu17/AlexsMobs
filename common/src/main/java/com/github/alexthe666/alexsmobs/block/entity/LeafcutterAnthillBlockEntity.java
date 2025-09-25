package com.github.alexthe666.alexsmobs.block.entity;

import com.github.alexthe666.alexsmobs.block.LeafcutterAntChamberBlock;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.EntityLeafcutterAnt;
import com.github.alexthe666.alexsmobs.registry.AMBlockEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.google.common.collect.Lists;
import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class LeafcutterAnthillBlockEntity extends BlockEntity {

    private final List<Ant> ants = Lists.newArrayList();
    private int leafFeedings = 0;

    public LeafcutterAnthillBlockEntity(BlockPos pos, BlockState state) {
        super(AMBlockEntityRegistry.LEAFCUTTER_ANTHILL.get(), pos, state);
    }

    /* To avoid ants not being mapped to vanilla, we have to handle this seperately than the default entitytype implementation.*/
    @Nullable
    public static Entity loadEntityAndExecute(NbtCompound compound, World worldIn, Function<Entity, Entity> p_220335_2_) {
        return loadEntity(compound, worldIn).map(p_220335_2_).map((p_220346_3_) -> {
            if (compound.contains("Passengers", 9)) {
                var listnbt = compound.getList("Passengers", 10);

                for (int i = 0; i < listnbt.size(); ++i) {
                    Entity entity = loadEntityAndExecute(listnbt.getCompound(i), worldIn, p_220335_2_);
                    if (entity != null) {
                        entity.startRiding(p_220346_3_, true);
                    }
                }
            }

            return p_220346_3_;
        }).orElse(null);
    }

    private static Optional<Entity> loadEntity(NbtCompound compound, World worldIn) {
        try {
            return loadEntityUnchecked(compound, worldIn);
        } catch (RuntimeException runtimeexception) {
            return Optional.empty();
        }
    }

    public static Optional<Entity> loadEntityUnchecked(NbtCompound compound, World worldIn) {
        var leafcutterAnt = AMEntityRegistry.LEAFCUTTER_ANT.get().create(worldIn);
        leafcutterAnt.readNbt(compound);
        return Optional.of(leafcutterAnt);
    }

    public boolean hasNoAnts() {
        return this.ants.isEmpty();
    }

    public boolean hasAtleastThisManyAnts(int antCount){
        return this.ants.size() >= antCount;
    }

    public boolean isFullOfAnts() {
        return this.ants.size() == AMConfig.leafcutterAntColonySize;
    }

    public void angerAnts(@Nullable LivingEntity p_226963_1_, BlockState p_226963_2_, BeehiveBlockEntity.BeeState p_226963_3_) {
        var list = this.tryReleaseAnt(p_226963_2_, p_226963_3_);
        if (p_226963_1_ != null) {
            for (Entity entity : list) {
                if (entity instanceof EntityLeafcutterAnt entityLeafcutterAnt) {
                    if (p_226963_1_.getPos().squaredDistanceTo(entity.getPos()) <= 16.0D) {
                        entityLeafcutterAnt.setTarget(p_226963_1_);
                    }
                    entityLeafcutterAnt.setStayOutOfHiveCountdown(400);
                }
            }
        }

    }

    public void angerAntsBecauseAnteater(@Nullable LivingEntity p_226963_1_, BlockState p_226963_2_, BeehiveBlockEntity.BeeState p_226963_3_) {
        var list = this.tryReleaseAntAnteater(p_226963_2_, p_226963_3_);
        if (p_226963_1_ != null) {
            for (Entity entity : list) {
                if (entity instanceof EntityLeafcutterAnt entityLeafcutterAnt) {
                    if (p_226963_1_.getPos().squaredDistanceTo(entity.getPos()) <= 16.0D) {
                        entityLeafcutterAnt.setTarget(p_226963_1_);
                    }
                    entityLeafcutterAnt.setStayOutOfHiveCountdown(400);
                }
            }
        }

    }

    private List<Entity> tryReleaseAnt(BlockState p_226965_1_, BeehiveBlockEntity.BeeState p_226965_2_) {
        List<Entity> list = Lists.newArrayList();
        this.ants.removeIf((p_226966_4_) -> this.addAntToWorld(p_226965_1_, p_226966_4_, list, p_226965_2_));
        return list;
    }

    private List<Entity> tryReleaseAntAnteater(BlockState p_226965_1_, BeehiveBlockEntity.BeeState p_226965_2_) {
        List<Entity> list = Lists.newArrayList();

        this.ants.removeIf((ant) -> !ant.queen && this.addAntToWorld(p_226965_1_, ant, list, p_226965_2_));
        return list;
    }

    private boolean addAntToWorld(BlockState p_235651_1_, Ant p_235651_2_, @Nullable List<Entity> p_235651_3_, BeehiveBlockEntity.BeeState p_235651_4_) {
        var blockpos = this.getPos();
        NbtCompound compoundnbt = p_235651_2_.entityData;
        compoundnbt.remove("Passengers");
        compoundnbt.remove("Leash");
        compoundnbt.remove("UUID");
        BlockPos blockpos1 = blockpos.up();
        boolean flag = !this.world.getBlockState(blockpos1).getSidesShape(this.world, blockpos1).isEmpty();
        if (flag && p_235651_4_ != BeehiveBlockEntity.BeeState.EMERGENCY) {
            return false;
        } else {
            Entity entity = loadEntityAndExecute(compoundnbt, this.world, (p_226960_0_) -> p_226960_0_);
            if (entity != null) {

                if (entity instanceof EntityLeafcutterAnt entityLeafcutterAnt) {
                    entityLeafcutterAnt.setLeaf(false);
                    if (p_235651_3_ != null) {
                        p_235651_3_.add(entityLeafcutterAnt);
                    }

                    float f = entity.getWidth();
                    double d0 = (double) blockpos.getX() + 0.5D;
                    double d1 = (double) blockpos.getY() + 1.0D;
                    double d2 = (double) blockpos.getZ() + 0.5D;
                    entity.refreshPositionAndAngles(d0, d1, d2, entity.getYaw(), entity.getPitch());
                    if (entityLeafcutterAnt.isQueen()) {
                        entityLeafcutterAnt.setStayOutOfHiveCountdown(400);
                    }
                }

                this.world.emitGameEvent(GameEvent.BLOCK_ACTIVATE, this.getPos(), GameEvent.Emitter.of(this.getCachedState()));
                this.world.playSound(null, blockpos, SoundEvents.BLOCK_BEEHIVE_EXIT, SoundCategory.BLOCKS, 1.0F, 1.0F);
                return this.world.spawnEntity(entity);

            } else {
                return false;
            }
        }

    }

    public void tryEnterHive(EntityLeafcutterAnt p_226962_1_, boolean p_226962_2_, int p_226962_3_) {
        if (this.ants.size() < AMConfig.leafcutterAntColonySize) {
            p_226962_1_.stopRiding();
            p_226962_1_.removeAllPassengers();
            var compoundnbt = new NbtCompound();
            p_226962_1_.writeNbt(compoundnbt);
            if (p_226962_2_) {
                if (!world.isClient && p_226962_1_.getRandom().nextFloat() < AMConfig.leafcutterAntFungusGrowChance) {
                    growFungus();
                }
                leafFeedings++;
                if (leafFeedings >= AMConfig.leafcutterAntRepopulateFeedings && this.getAntsInAreaCount(32D) < MathHelper.ceil(AMConfig.leafcutterAntColonySize * 0.5F) && hasQueen()) {
                    leafFeedings = 0;
                    this.ants.add(new Ant(new NbtCompound(), 0, 100, false));
                }
            }
            this.ants.add(new Ant(compoundnbt, p_226962_3_, p_226962_2_ ? 100 : 200, p_226962_1_.isQueen()));
            if (this.world != null) {
                var blockpos = this.getPos();
                this.world.emitGameEvent(GameEvent.BLOCK_ACTIVATE, this.getPos(), GameEvent.Emitter.of(this.getCachedState()));
                this.world.playSound(null, blockpos.getX(), blockpos.getY(), blockpos.getZ(), SoundEvents.BLOCK_BEEHIVE_ENTER, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }

            p_226962_1_.remove(Entity.RemovalReason.DISCARDED);
        }
    }

    private int getAntsInAreaCount(double size) {
        int ants = this.getAntCount();
        Vec3d vec = Vec3d.ofCenter(this.getPos());
        var box = new Box(vec.add(-size, -size, -size), vec.add(size, size, size));
        ants += world.getNonSpectatingEntities(EntityLeafcutterAnt.class, box).size();
        return ants;
    }

    public boolean hasQueen() {
        for (Ant ant : ants) {
            if (ant.queen) {
                return true;
            }
        }
        return false;
    }

    public void releaseQueens() {
        this.ants.removeIf((p_226966_4_) -> p_226966_4_.queen && this.addAntToWorld(getCachedState(), p_226966_4_, null, BeehiveBlockEntity.BeeState.BEE_RELEASED));
    }

    public void tryEnterHive(EntityLeafcutterAnt p_226961_1_, boolean p_226961_2_) {
        this.tryEnterHive(p_226961_1_, p_226961_2_, 0);
    }

    public int getAntCount() {
        return this.ants.size();
    }

    @Override
    public void markDirty() {
        if (this.isNearFire()) {
            this.angerAnts(null, this.world.getBlockState(this.getPos()), BeehiveBlockEntity.BeeState.EMERGENCY);
        }

        super.markDirty();
    }

    public boolean isNearFire() {
        if (this.world != null) {
            for (BlockPos blockpos : BlockPos.iterate(this.pos.add(-1, -1, -1), this.pos.add(1, 1, 1))) {
                if (this.world.getBlockState(blockpos).getBlock() instanceof FireBlock) {
                    return true;
                }
            }

        }
        return false;
    }

    public BlockState shrinkFungus() {
        BlockPos bottomChamber = this.getPos().down();
        while (world.getBlockState(bottomChamber.down()).getBlock() == AMBlockRegistry.LEAFCUTTER_ANT_CHAMBER.get() && bottomChamber.getY() > 0) {
            bottomChamber = bottomChamber.down();
        }
        BlockPos chamber = bottomChamber;
        if (!isUnfilledChamber(chamber)) {
            BlockState prev = world.getBlockState(chamber);
            if(prev.isOf(AMBlockRegistry.LEAFCUTTER_ANT_CHAMBER.get())){
                int fungalLevel = prev.get(LeafcutterAntChamberBlock.FUNGUS);
                world.setBlockState(chamber, AMBlockRegistry.LEAFCUTTER_ANT_CHAMBER.get().getDefaultState().with(LeafcutterAntChamberBlock.FUNGUS, Math.min(0, fungalLevel-1)));
                return prev;
            }
        } else {
            boolean flag = false;
            List<BlockPos> possibleChambers = new ArrayList<>();
            while (!flag) {
                for (BlockPos blockpos : BlockPos.iterate(chamber.add(-4, 0, -4), chamber.add(4, 0, 4))) {
                    if (isUnfilledChamber(blockpos)) {
                        possibleChambers.add(blockpos.toImmutable());
                        flag = true;
                    }
                }
                if (!flag) {
                    chamber = chamber.up();
                    if (chamber.getY() > this.pos.getY()) {
                        return null;
                    }
                }
            }
            Collections.shuffle(possibleChambers);
            if (!possibleChambers.isEmpty()) {
                BlockPos newChamber = possibleChambers.get(0);
                if (newChamber != null && !isUnfilledChamber(newChamber)) {
                    BlockState prev = world.getBlockState(newChamber);
                    if(prev.isOf(AMBlockRegistry.LEAFCUTTER_ANT_CHAMBER.get())) {
                        int fungalLevel = prev.get(LeafcutterAntChamberBlock.FUNGUS);
                        world.setBlockState(newChamber, AMBlockRegistry.LEAFCUTTER_ANT_CHAMBER.get().getDefaultState().with(LeafcutterAntChamberBlock.FUNGUS, Math.min(fungalLevel - 1, 0)));
                        return prev;
                    }
                }
            }
        }
        return null;
    }

    public void growFungus() {
        if (!this.hasNoAnts()) {
            BlockPos bottomChamber = this.getPos().down();
            while (world.getBlockState(bottomChamber.down()).getBlock() == AMBlockRegistry.LEAFCUTTER_ANT_CHAMBER.get() && bottomChamber.getY() > 0) {
                bottomChamber = bottomChamber.down();
            }
            BlockPos chamber = bottomChamber;
            if (isUnfilledChamber(chamber)) {
                int fungalLevel = world.getBlockState(chamber).get(LeafcutterAntChamberBlock.FUNGUS);
                int fungalLevel2 = MathHelper.clamp(fungalLevel + 1 + world.getRandom().nextInt(1), 0, 5);
                world.setBlockState(chamber, AMBlockRegistry.LEAFCUTTER_ANT_CHAMBER.get().getDefaultState().with(LeafcutterAntChamberBlock.FUNGUS, fungalLevel2));
            } else {
                boolean flag = false;
                List<BlockPos> possibleChambers = new ArrayList<>();
                while (!flag) {
                    for (BlockPos blockpos : BlockPos.iterate(chamber.add(-4, 0, -4), chamber.add(4, 0, 4))) {
                        if (isUnfilledChamber(blockpos)) {
                            possibleChambers.add(blockpos.toImmutable());
                            flag = true;
                        }
                    }
                    if (!flag) {
                        chamber = chamber.up();
                        if (chamber.getY() > this.pos.getY()) {
                            return;
                        }
                    }
                }
                Collections.shuffle(possibleChambers);
                if (!possibleChambers.isEmpty()) {
                    BlockPos newChamber = possibleChambers.get(0);
                    if (newChamber != null && isUnfilledChamber(newChamber)) {
                        int fungalLevel = world.getBlockState(newChamber).get(LeafcutterAntChamberBlock.FUNGUS);
                        int fungalLevel2 = MathHelper.clamp(fungalLevel + 1 + world.getRandom().nextInt(1), 0, 5);
                        world.setBlockState(newChamber, AMBlockRegistry.LEAFCUTTER_ANT_CHAMBER.get().getDefaultState().with(LeafcutterAntChamberBlock.FUNGUS, fungalLevel2));
                    }
                }
            }
        }
    }

    private boolean isUnfilledChamber(BlockPos pos) {
        return world.getBlockState(pos).getBlock() == AMBlockRegistry.LEAFCUTTER_ANT_CHAMBER.get() && world.getBlockState(pos).get(LeafcutterAntChamberBlock.FUNGUS) < 5;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.ants.clear();
        this.leafFeedings = nbt.getInt("LeafFeedings");
        var listnbt = nbt.getList("Ants", 10);

        for (int i = 0; i < listnbt.size(); ++i) {
            NbtCompound compoundnbt = listnbt.getCompound(i);
            Ant beehiveTileEntity$ant = new Ant(compoundnbt.getCompound("EntityData"), compoundnbt.getInt("TicksInHive"), compoundnbt.getInt("MinOccupationTicks"), compoundnbt.getBoolean("Queen"));
            this.ants.add(beehiveTileEntity$ant);
        }
    }

    public NbtList getAnts() {
        var listnbt = new NbtList();

        for (Ant beehiveTileEntity$ant : this.ants) {
            beehiveTileEntity$ant.entityData.remove("UUID");
            NbtCompound compoundnbt = new NbtCompound();
            compoundnbt.put("EntityData", beehiveTileEntity$ant.entityData);
            compoundnbt.putInt("TicksInHive", beehiveTileEntity$ant.ticksInHive);
            compoundnbt.putInt("MinOccupationTicks", beehiveTileEntity$ant.minOccupationTicks);
            listnbt.add(compoundnbt);
        }

        return listnbt;
    }

    @Override
    public void writeNbt(NbtCompound compound) {
        super.writeNbt(compound);
        compound.put("Ants", this.getAnts());
        compound.putInt("LeafFeedings", leafFeedings);
    }

    static class Ant {
        private final NbtCompound entityData;
        private final int minOccupationTicks;
        private int ticksInHive;
        private final boolean queen;

        private Ant(NbtCompound p_i225767_1_, int p_i225767_2_, int p_i225767_3_, boolean queen) {
            p_i225767_1_.remove("UUID");
            this.entityData = p_i225767_1_;
            this.ticksInHive = p_i225767_2_;
            this.minOccupationTicks = p_i225767_3_;
            this.queen = queen;
        }
    }

}
