package com.github.alexthe666.alexsmobs.world;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;

public class AMWorldData extends PersistentState {

    private static final String IDENTIFIER = "alexsmobs_world_data";
    private ServerWorld level;
    private int tickCounter;
    private int beachedCachalotSpawnDelay;
    private int beachedCachalotSpawnChance;
    private UUID beachedCachalotID;
    private ChunkPos pupfishChunk;
    private int pupfishChunkTime = 0;
    private int pupfishSeedAddition = 0;
    private long startPupfishSearchTimestamp = -1;
    private boolean noPupfishChunk;
    private static final Map<World, AMWorldData> dataMap = new HashMap<>();
    private static final Predicate<BlockState> IS_WATER = (state -> state.isOf(Blocks.WATER));

    public AMWorldData() {
        super();
    }

    public static AMWorldData get(World world) {
        if (world instanceof ServerWorld) {
            var overworld = world.getServer().getWorld(World.OVERWORLD);
            var fromMap = dataMap.get(overworld);
            if(fromMap == null){
                var storage = overworld.getPersistentStateManager();
                var data = storage.getOrCreate(AMWorldData::load, AMWorldData::new, IDENTIFIER);
                if (data != null) {
                    data.level = overworld;
                    data.markDirty();
                }
                dataMap.put(world, data);
                return data;
            }
            return fromMap;
        }
        return null;
    }

    public static AMWorldData load(NbtCompound nbt) {
        var data = new AMWorldData();
        if (nbt.contains("BeachedCachalotSpawnDelay", 99)) {
            data.beachedCachalotSpawnDelay = nbt.getInt("BeachedCachalotSpawnDelay");
        }
        if (nbt.contains("BeachedCachalotSpawnChance", 99)) {
            data.beachedCachalotSpawnChance = nbt.getInt("BeachedCachalotSpawnChance");
        }
        if (nbt.contains("BeachedCachalotId", 8)) {
            data.beachedCachalotID = UUID.fromString(nbt.getString("BeachedCachalotId"));
        }
        if (nbt.contains("PupfishChunkX") && nbt.contains("PupfishChunkZ")) {
            data.pupfishChunk = new ChunkPos(nbt.getInt("PupfishChunkX"), nbt.getInt("PupfishChunkZ"));
        }
        if (nbt.contains("NoPupfishChunk")) {
            data.noPupfishChunk = nbt.getBoolean("NoPupfishChunk");
        }
        return data;
    }

    public int getBeachedCachalotSpawnDelay() {
        return this.beachedCachalotSpawnDelay;
    }

    public void setBeachedCachalotSpawnDelay(int delay) {
        this.beachedCachalotSpawnDelay = delay;
    }

    public int getBeachedCachalotSpawnChance() {
        return this.beachedCachalotSpawnChance;
    }

    public void setBeachedCachalotSpawnChance(int chance) {
        this.beachedCachalotSpawnChance = chance;
    }

    public void setBeachedCachalotID(UUID id) {
        this.beachedCachalotID = id;
    }

    public void tick() {
        ++this.tickCounter;
    }

    @NotNull
    @Override
    public NbtCompound writeNbt(NbtCompound compound) {
        compound.putInt("beachedCachalotSpawnDelay", this.beachedCachalotSpawnDelay);
        compound.putInt("beachedCachalotSpawnChance", this.beachedCachalotSpawnChance);
        if (this.beachedCachalotID != null) {
            compound.putString("beachedCachalotId", this.beachedCachalotID.toString());
        }
        if (this.pupfishChunk != null) {
            compound.putInt("PupfishChunkX", this.pupfishChunk.x);
            compound.putInt("PupfishChunkZ", this.pupfishChunk.z);
        }
        if(this.noPupfishChunk){
            compound.putBoolean("NoPupfishChunk", noPupfishChunk);
        }
        return compound;
    }

    @Nullable
    public ChunkPos getPupfishChunk() {
        return pupfishChunk;
    }

    public boolean isInPupfishChunk(BlockPos pos) {
        if(pupfishChunk != null){
            return pos.getX() >= pupfishChunk.getStartX() && pos.getX() <= pupfishChunk.getEndX() && pos.getZ() >= pupfishChunk.getStartZ() && pos.getZ() <= pupfishChunk.getEndZ();
        }
        return false;
    }

    public void tickPupfish() {
        if(AMConfig.restrictPupfishSpawns && !noPupfishChunk){
            if(pupfishChunk == null && startPupfishSearchTimestamp == -1){
                startPupfishSearchTimestamp = System.currentTimeMillis();
            }
            if (pupfishChunk == null && pupfishChunkTime % 10 == 0) {
                long seconds = (System.currentTimeMillis() - startPupfishSearchTimestamp) / 1000L;
                if(seconds / 60 > 5) {
                    AlexsMobs.LOGGER.info("Giving up search for pupfish chunk after {} minutes. no pupfish will spawn in this world :( ", seconds / 60);
                    noPupfishChunk = true;
                }else{
                    searchForPupfishChunk();
                }
            }
            pupfishChunkTime++;
        }
    }

    private void searchForPupfishChunk() {
        if (level != null && level.getChunkManager().getChunkGenerator() instanceof NoiseChunkGenerator chunkGenerator) {
            var random = new Random(level.getSeed() + pupfishSeedAddition);
            int randomXCoord = random.nextInt(AMConfig.pupfishChunkSpawnDistance * 2) - AMConfig.pupfishChunkSpawnDistance;
            int randomZCoord = random.nextInt(AMConfig.pupfishChunkSpawnDistance * 2) - AMConfig.pupfishChunkSpawnDistance;
            var checkPos = new ChunkPos(randomXCoord >> 4, randomZCoord >> 4);
            var center = new BlockPos(checkPos.getCenterX(), chunkGenerator.getSeaLevel(), checkPos.getCenterZ());
            int maxWater = getWaterHeight(chunkGenerator, level.getChunkManager().getNoiseConfig(), center.getX(), center.getZ(), level);
            if(maxWater > 31 && maxWater < 63){
                pupfishChunk = checkPos;
                AlexsMobs.LOGGER.info("Found Pupfish chunk at {} ~ {} after {} tries", pupfishChunk.getEndX(), pupfishChunk.getStartZ(), pupfishSeedAddition);
            }
        }
        pupfishSeedAddition++;
    }

    public int getWaterHeight(NoiseChunkGenerator generator, NoiseConfig rand, int x, int z, HeightLimitView level) {
        var noiseSettings = generator.getSettings().value().generationShapeConfig();
        int i = Math.max(noiseSettings.minimumY(), level.getBottomY());
        int j = Math.min(noiseSettings.minimumY() + noiseSettings.height(), level.getTopY());
        int k = MathHelper.floorDiv(i, noiseSettings.verticalCellBlockCount());
        int l = MathHelper.floorDiv(j - i, noiseSettings.verticalCellBlockCount());
        return generator.sampleHeightmap(level, rand, x, z, null, IS_WATER).orElse(level.getBottomY());
    }
}
