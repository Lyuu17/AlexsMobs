package com.github.alexthe666.alexsmobs.world;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.config.BiomeConfig;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.SpawnHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class BeachedCachalotWhaleSpawner {
    private final Random random = new Random();
    private final ServerWorld world;
    private int timer;
    private int delay;
    private int chance;

    public BeachedCachalotWhaleSpawner(ServerWorld p_i50177_1_) {
        this.world = p_i50177_1_;
        this.timer = 1200;
        var worldinfo = AMWorldData.get(p_i50177_1_);
        this.delay = worldinfo.getBeachedCachalotSpawnDelay();
        this.chance = worldinfo.getBeachedCachalotSpawnChance();
        if (this.delay == 0 && this.chance == 0) {
            this.delay = AMConfig.beachedCachalotWhaleSpawnDelay;
            worldinfo.setBeachedCachalotSpawnDelay(this.delay);
            this.chance = 25;
            worldinfo.setBeachedCachalotSpawnChance(this.chance);
        }

    }

    public void tick() {
        if (AMConfig.beachedCachalotWhales && --this.timer <= 0 && world.isThundering()) {
            this.timer = 1200;
            var worldinfo = AMWorldData.get(world);
            this.delay -= 1200;
            if(delay < 0){
                delay = 0;
            }
            worldinfo.setBeachedCachalotSpawnDelay(this.delay);
            if (this.delay <= 0) {
                this.delay = AMConfig.beachedCachalotWhaleSpawnDelay;
                if (this.world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING)) {
                    int i = this.chance;
                    this.chance = MathHelper.clamp(this.chance + AMConfig.beachedCachalotWhaleSpawnChance, 5, 100);
                    worldinfo.setBeachedCachalotSpawnChance(this.chance);
                    if (this.random.nextInt(100) <= i && this.attemptSpawnWhale()) {
                        this.chance = AMConfig.beachedCachalotWhaleSpawnChance;
                    }
                }
            }
        }

    }

    private boolean attemptSpawnWhale() {
        var playerentity = this.world.getRandomAlivePlayer();
        if (playerentity == null) {
            return true;
        } else if (this.random.nextInt(5) != 0) {
            return false;
        } else {
            var blockpos = playerentity.getBlockPos();
            var blockpos2 = this.func_221244_a(blockpos, 84);
            if (blockpos2 != null && this.func_226559_a_(blockpos2) && blockpos2.getSquaredDistance(blockpos) > 225) {
                var upPos = new BlockPos(blockpos2.getX(), blockpos2.getY() + 2, blockpos2.getZ());
                var whale = AMEntityRegistry.CACHALOT_WHALE.get().create(world);
                whale.refreshPositionAndAngles(upPos.getX() + 0.5D, upPos.getY() + 0.5D, upPos.getZ() + 0.5D, random.nextFloat() * 360 - 180F, 0);
                whale.initialize(world, world.getLocalDifficulty(upPos), SpawnReason.SPAWNER, null, null);
                whale.setBeached(true);
                var worldinfo = AMWorldData.get(world);
                worldinfo.setBeachedCachalotID(whale.getUuid());
                whale.setPositionTarget(upPos, 16);
                whale.setDespawnBeach(true);
                world.spawnEntity(whale);
                return true;
            }
            return false;
        }
    }

    @Nullable
    private BlockPos func_221244_a(BlockPos p_221244_1_, int p_221244_2_) {
        BlockPos blockpos = null;

        for(int i = 0; i < 10; ++i) {
            int j = p_221244_1_.getX() + this.random.nextInt(p_221244_2_ * 2) - p_221244_2_;
            int k = p_221244_1_.getZ() + this.random.nextInt(p_221244_2_ * 2) - p_221244_2_;
            int l = this.world.getTopY(Heightmap.Type.WORLD_SURFACE, j, k);
            var blockpos1 = new BlockPos(j, l, k);
            if (AMWorldRegistry.testBiome(BiomeConfig.cachalot_whale_beached_spawns, world.getBiome(blockpos1)) && SpawnHelper.canSpawn(SpawnRestriction.Location.ON_GROUND, this.world, blockpos1, EntityType.WANDERING_TRADER)) {
                blockpos = blockpos1;
                break;
            }
        }

        return blockpos;
    }

    private boolean func_226559_a_(BlockPos p_226559_1_) {
        var var2 = BlockPos.iterate(p_226559_1_, p_226559_1_.add(1, 2, 1)).iterator();

        BlockPos blockpos;
        do {
            if (!var2.hasNext()) {
                return true;
            }

            blockpos = var2.next();
        } while(this.world.getBlockState(blockpos).getSidesShape(this.world, blockpos).isEmpty() && world.getFluidState(blockpos).isEmpty());

        return false;
    }
}
