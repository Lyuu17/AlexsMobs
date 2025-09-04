package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIWanderRanged;
import com.github.alexthe666.alexsmobs.entity.ai.CreatureAITargetItems;
import com.github.alexthe666.alexsmobs.entity.ai.MungusAIAlertBunfungus;
import com.github.alexthe666.alexsmobs.entity.ai.MungusAITemptMushroom;
import com.github.alexthe666.alexsmobs.registry.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.registry.AMTagRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Fertilizable;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.ReadableContainer;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Optional;

public abstract class EntityMungus extends AnimalEntity implements ITargetsDroppedItems, Shearable {

    protected static final TrackedData<Optional<BlockPos>> TARGETED_BLOCK_POS = DataTracker.registerData(EntityMungus.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);
    private static final TrackedData<Boolean> ALT_ORDER_MUSHROOMS = DataTracker.registerData(EntityMungus.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> REVERTING = DataTracker.registerData(EntityMungus.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> MUSHROOM_COUNT = DataTracker.registerData(EntityMungus.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SACK_SWELL = DataTracker.registerData(EntityMungus.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> EXPLOSION_DISABLED = DataTracker.registerData(EntityMungus.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Optional<BlockState>> MUSHROOM_STATE = DataTracker.registerData(EntityMungus.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_STATE);

    //biome container constants
    private static final int WIDTH_BITS = MathHelper.ceilLog2(16) - 2;
    public static final int MAX_SIZE = 1 << WIDTH_BITS + WIDTH_BITS + DimensionType.SIZE_BITS_Y - 2;
    private static final int HORIZONTAL_MASK = (1 << WIDTH_BITS) - 1;
    private static final HashMap<String, String> MUSHROOM_TO_BIOME = new HashMap<>();
    private static final HashMap<String, String> MUSHROOM_TO_BLOCK = new HashMap<>();
    private static boolean initBiomeData = false;
    public float prevSwellProgress = 0;
    public float swellProgress = 0;
    protected int beamCounter = 0;
    private int mosquitoAttackCooldown = 0;
    private boolean hasExploded;
    public int timeUntilNextEgg = this.random.nextInt(24000) + 24000;

    protected EntityMungus(EntityType<? extends EntityMungus> type, World worldIn) {
        super(type, worldIn);
        initBiomeData();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 15D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D);
    }

    public static boolean canMungusSpawn(EntityType<? extends EntityMungus> type, WorldAccess worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        return worldIn.getBlockState(pos.down()).isOpaque();
    }

    public static BlockState getMushroomBlockstate(Item item) {
        if (item instanceof BlockItem) {
            var name = Registries.ITEM.getId(item);
            if (name != null && MUSHROOM_TO_BIOME.containsKey(name.toString())) {
                return ((BlockItem) item).getBlock().getDefaultState();
            }
        }
        return null;
    }

    private static void initBiomeData() {
        if (!initBiomeData || MUSHROOM_TO_BIOME.isEmpty()) {
            initBiomeData = true;
            for (String str : AMConfig.mungusBiomeMatches) {
                String[] split = str.split("\\|");
                if (split.length >= 2) {
                    MUSHROOM_TO_BIOME.put(split[0], split[1]);
                    MUSHROOM_TO_BLOCK.put(split[0], split[2]);
                }
            }
        }

    }

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.MUNGUS_IDLE.get();
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.MUNGUS_HURT.get();
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.MUNGUS_HURT.get();
    }

    @Override
    public boolean canSpawn(WorldAccess worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.mungusSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    protected void initGoals() {
        this.goalSelector.add(0, new AnimalMateGoal(this, 1.0D));
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new EscapeDangerGoal(this, 1.25D));
        this.goalSelector.add(3, new MungusAITemptMushroom(this, 1.0F));
        this.goalSelector.add(5, new AITargetMushrooms());
        this.goalSelector.add(6, new FollowParentGoal(this, 1.1D));
        this.goalSelector.add(7, new AnimalAIWanderRanged(this, 60, 1.0D, 14, 7));
        this.goalSelector.add(8, new LookAtEntityGoal(this, LivingEntity.class, 15.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new CreatureAITargetItems<>(this, false, 10));
        this.targetSelector.add(2, new MungusAIAlertBunfungus(this, EntityBunfungus.class));
    }

    @Override
    public void tick(){
        super.tick();
        if (!this.getWorld().isClient && this.isAlive() && !this.isBaby() && --this.timeUntilNextEgg <= 0) {
            var dropped = this.dropItem(AMItemRegistry.MUNGAL_SPORES.get());
            dropped.setToDefaultPickupDelay();
            this.timeUntilNextEgg = this.random.nextInt(24000) + 24000;
        }
    }

    @Override
    public void baseTick() {
        super.baseTick();
        this.prevSwellProgress = swellProgress;
        if (this.isReverting() && AMConfig.mungusBiomeTransformationType == 2) {
            swellProgress += 0.5F;
            if (swellProgress >= 10) {
                try {
                    explode();
                }catch (Exception e){
                    e.printStackTrace();
                }
                swellProgress = 0;
                this.dataTracker.set(REVERTING, false);
            }
        } else if (isAlive() && swellProgress > 0F) {
            swellProgress -= 1F;
        }
        if (dataTracker.get(EXPLOSION_DISABLED)) {
            if (mosquitoAttackCooldown < 0) {
                mosquitoAttackCooldown++;
            }
            if (mosquitoAttackCooldown > 200) {
                mosquitoAttackCooldown = 0;
                dataTracker.set(EXPLOSION_DISABLED, false);
            }
        }
    }

    @Override
    protected void updatePostDeath() {
        super.updatePostDeath();
        if (this.getMushroomCount() >= 5 && AMConfig.mungusBiomeTransformationType > 0 && !this.isBaby() && !this.dataTracker.get(EXPLOSION_DISABLED)) {
            this.swellProgress++;
            if (this.deathTime == 19 && !hasExploded) {
                hasExploded = true;
                try {
                    explode();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    private void explode() {
        for (int i = 0; i < 5; i++) {
            float r1 = 6F * (random.nextFloat() - 0.5F);
            float r2 = 2F * (random.nextFloat() - 0.5F);
            float r3 = 6F * (random.nextFloat() - 0.5F);
            this.getWorld().addParticle(ParticleTypes.EXPLOSION, this.getX() + r1, this.getY() + 0.5F + r2, this.getZ() + r3, r1 * 4, r2 * 4, r3 * 4);
        }
        if(!this.getWorld().isClient){
            ServerWorld ServerWorld = (ServerWorld) getWorld();
            final int radius = 3;
            final int j = radius + getWorld().random.nextInt(1);
            final int k = (radius + getWorld().random.nextInt(1));
            final int l = radius + getWorld().random.nextInt(1);
            final float f = (float) (j + k + l) * 0.333F + 0.5F;
            final float ff = f * f;
            final double ffDouble = ff;
            BlockPos center = this.getBlockPos();
            BlockState transformState = Blocks.MYCELIUM.getDefaultState();
            Registry<Biome> registry = getWorld().getServer().getRegistryManager().get(RegistryKeys.BIOME);
            RegistryEntry<Biome> biome = registry.getEntry(BiomeKeys.MUSHROOM_FIELDS).get();
            TagKey<Block> transformMatches = AMTagRegistry.MUNGUS_REPLACE_MUSHROOM;
            if (this.getMushroomState() != null) {
                String mushroomKey = Registries.BLOCK.getKey(this.getMushroomState().getBlock()).toString();
                if (MUSHROOM_TO_BLOCK.containsKey(mushroomKey)) {
                    var block = Registries.BLOCK.get(new Identifier(MUSHROOM_TO_BLOCK.get(mushroomKey)));
                    if (block != null) {
                        transformState = block.getDefaultState();
                        if (block == Blocks.WARPED_NYLIUM) {
                            transformMatches = AMTagRegistry.MUNGUS_REPLACE_NETHER;
                        }
                        if (block == Blocks.CRIMSON_NYLIUM) {
                            transformMatches = AMTagRegistry.MUNGUS_REPLACE_NETHER;
                        }
                    }
                }
                RegistryEntry<Biome> gottenFrom = getBiomeKeyFromShroom();
                if (gottenFrom != null) {
                    biome = gottenFrom;
                }
            }
            BlockState finalTransformState = transformState;
            TagKey<Block> finalTransformReplace = transformMatches;

            if (AMConfig.mungusBiomeTransformationType == 2 && !this.getWorld().isClient) {
                transformBiome(center, biome);
            }
            this.emitGameEvent(GameEvent.EXPLODE);
            this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, this.getSoundVolume(), this.getSoundPitch());
            if (!isReverting()) {
                BlockPos.stream(center.add(-j, -k, -l), center.add(j, k, l)).forEach(blockpos -> {
                    if (blockpos.getSquaredDistance(center) <= ffDouble) {
                        if (getWorld().random.nextFloat() > (float) blockpos.getSquaredDistance(center) / ff) {
                            if (getWorld().getBlockState(blockpos).isIn(finalTransformReplace) && !getWorld().getBlockState(blockpos.up()).isOpaque()) {
                                getWorld().setBlockState(blockpos, finalTransformState);
                            }
                            if (getWorld().random.nextInt(4) == 0 && getWorld().getBlockState(blockpos).isSolid() && getWorld().getFluidState(blockpos.up()).isEmpty() && !getWorld().getBlockState(blockpos.up()).isOpaque()) {
                                getWorld().setBlockState(blockpos.up(), this.getMushroomState());
                            }
                        }
                    }
                });
            }
        }
    }

    public void disableExplosion() {
        this.dataTracker.set(EXPLOSION_DISABLED, true);
    }

    private RegistryEntry<Biome> getBiomeKeyFromShroom() {
        var registry = this.getWorld().getRegistryManager().get(RegistryKeys.BIOME);
        BlockState state = this.getMushroomState();
        if (state == null) {
            return null;
        }
        Identifier blockRegName = Registries.BLOCK.getId(state.getBlock());
        if (blockRegName != null && MUSHROOM_TO_BIOME.containsKey(blockRegName.toString())) {
            String str = MUSHROOM_TO_BIOME.get(blockRegName.toString());
            Biome biome = registry.getOrEmpty(new Identifier(str)).orElse(null);
            var resourceKey = registry.getKey(biome).orElse(null);
            return registry.getEntry(resourceKey).orElse(null);
        }
        return null;
    }

    private ReadableContainer<RegistryEntry<Biome>> getChunkBiomes(WorldChunk chunk) {
        int i = BiomeCoords.fromBlock(chunk.getBottomY());
        int k = i + BiomeCoords.fromBlock(chunk.getHeight()) - 1;
        int l = MathHelper.clamp(BiomeCoords.fromBlock((int) this.getY()), i, k);
        int j = chunk.getSectionIndex(BiomeCoords.toBlock(l));
        var section = chunk.getSection(j);
        return section == null ? null : section.getBiomeContainer();
    }

    private void setChunkBiomes(WorldChunk chunk, PalettedContainer<RegistryEntry<Biome>> container) {
        int i = BiomeCoords.fromBlock(chunk.getBottomY());
        int k = i + BiomeCoords.fromBlock(chunk.getHeight()) - 1;
        int l = MathHelper.clamp(BiomeCoords.fromBlock((int) this.getY()), i, k);
        int j = chunk.getSectionIndex(BiomeCoords.toBlock(l));
        var section = chunk.getSection(j);
        if(section != null){
            section.biomeContainer = container;
        }
    }

    private void transformBiome(BlockPos pos, RegistryEntry<Biome> biome) {
        var chunk = getWorld().getWorldChunk(pos);
        PalettedContainer<RegistryEntry<Biome>> container = getChunkBiomes(chunk).slice();
        if (this.dataTracker.get(REVERTING)) {
            int lvt_4_1_ = chunk.getPos().getStartX() >> 2;
            int yChunk = (int)this.getY() >> 2;
            int lvt_5_1_ = chunk.getPos().getStartZ() >> 2;
            for(int k = 0; k < 4; ++k) {
                for(int l = 0; l < 4; ++l) {
                    for(int i1 = 0; i1 < 4; ++i1) {
                        container.swapUnsafe(k, l, i1, getWorld().getGeneratorStoredBiome(lvt_4_1_ + k, yChunk + l, lvt_5_1_ + i1));
                    }
                }
            }
            setChunkBiomes(chunk, container);
            //FIXME forge
//            if (!this.getWorld().isClient) {
//                AlexsMobs.sendMSGToAll(new MessageMungusBiomeChange(this.getId(), pos.getX(), pos.getZ(), ForgeRegistries.BIOMES.getKey(biome.value()).toString()));
//            }
        } else {
            if (biome == null) {
                return;
            }
            if (container != null && !this.getWorld().isClient) {
                for (int biomeX = 0; biomeX < 4; ++biomeX) {
                    for (int biomeY = 0; biomeY < 4; ++biomeY) {
                        for (int biomeZ = 0; biomeZ < 4; ++biomeZ) {
                            container.swapUnsafe(biomeX, biomeY, biomeZ, biome);
                        }
                    }
                }
                setChunkBiomes(chunk, container);
                //FIXME forge
//                Identifier biomeKey = Registries.BIOME_SOURCE.getKey(biome.value());
//                if(biomeKey != null){
//                    AlexsMobs.sendMSGToAll(new MessageMungusBiomeChange(this.getId(), pos.getX(), pos.getZ(), biomeKey.toString()));
//                }
            }
        }

    }

    public boolean shouldFollowMushroom(ItemStack stack) {
        BlockState state = getMushroomBlockstate(stack.getItem());
        if (state != null && !state.isAir()) {
            if (this.getMushroomCount() == 0) {
                return true;
            } else {
                return this.getMushroomState().getBlock() == state.getBlock();
            }
        }
        return false;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getStackInHand(hand);
        ActionResult type = super.interactMob(player, hand);
        if (itemstack.getItem() == Items.POISONOUS_POTATO && !this.isBaby()) {
            this.dataTracker.set(REVERTING, true);
            this.eat(player, hand, itemstack);
            return ActionResult.SUCCESS;
        }
        if (shouldFollowMushroom(itemstack) && this.getMushroomCount() < 5) {
            this.dataTracker.set(REVERTING, false);
            BlockState state = getMushroomBlockstate(itemstack.getItem());
            this.emitGameEvent(GameEvent.BLOCK_PLACE);
            this.playSound(SoundEvents.BLOCK_FUNGUS_PLACE, this.getSoundVolume(), this.getSoundPitch());
            if (this.getMushroomState() != null && state != null && state.getBlock() != this.getMushroomState().getBlock()) {
                this.setMushroomCount(0);
            }
            this.setMushroomState(state);
            this.eat(player, hand, itemstack);
            this.setMushroomCount(this.getMushroomCount() + 1);
            return ActionResult.SUCCESS;
        } else {
            return type;
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean prev = super.damage(source, amount);
        if (prev) {
            this.setBeamTarget(null);
            beamCounter = Math.min(beamCounter, -1200);
        }
        return prev;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(MUSHROOM_STATE, Optional.empty());
        this.dataTracker.startTracking(TARGETED_BLOCK_POS, Optional.empty());
        this.dataTracker.startTracking(ALT_ORDER_MUSHROOMS, false);
        this.dataTracker.startTracking(REVERTING, false);
        this.dataTracker.startTracking(EXPLOSION_DISABLED, false);
        this.dataTracker.startTracking(MUSHROOM_COUNT, 0);
        this.dataTracker.startTracking(SACK_SWELL, 0);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        BlockState blockstate = this.getMushroomState();
        if (blockstate != null) {
            compound.put("MushroomState", NbtHelper.fromBlockState(blockstate));
        }
        compound.putInt("MushroomCount", this.getMushroomCount());
        compound.putInt("Sack", this.getSackSwell());
        compound.putInt("BeamCounter", this.beamCounter);
        compound.putBoolean("AltMush", this.dataTracker.get(ALT_ORDER_MUSHROOMS));
        if (this.getBeamTarget() != null) {
            compound.put("BeamTarget", NbtHelper.fromBlockPos(this.getBeamTarget()));
        }
        compound.putInt("EggTime", this.timeUntilNextEgg);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        BlockState blockstate = null;
        if (compound.contains("MushroomState", 10)) {
            blockstate = NbtHelper.toBlockState(this.getWorld().createCommandRegistryWrapper(RegistryKeys.BLOCK), compound.getCompound("MushroomState"));
            if (blockstate.isAir()) {
                blockstate = null;
            }
        }
        if (compound.contains("BeamTarget", 10)) {
            this.setBeamTarget(NbtHelper.toBlockPos(compound.getCompound("BeamTarget")));
        }
        this.setMushroomState(blockstate);
        this.setMushroomCount(compound.getInt("MushroomCount"));
        this.setSackSwell(compound.getInt("Sack"));
        this.beamCounter = compound.getInt("BeamCounter");
        this.dataTracker.set(ALT_ORDER_MUSHROOMS, compound.getBoolean("AltMush"));
        if (compound.contains("EggTime")) {
            this.timeUntilNextEgg = compound.getInt("EggTime");
        }
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.getBeamTarget() != null) {
            BlockPos t = this.getBeamTarget();
            if (isMushroomTarget(t) && this.hasLineOfSightMushroom(t)) {
                this.getLookControl().lookAt(t.getX() + 0.5F, t.getY() + 0.15F, t.getZ() + 0.5F, 90.0F, 90.0F);
                this.getLookControl().tick();
                double d5 = 1.0F;
                double eyeHeight = this.getY() + 1.0F;
                if (beamCounter % 20 == 0) {
                    this.playSound(AMSoundRegistry.MUNGUS_LASER_LOOP.get(), this.getSoundPitch(), this.getSoundVolume());
                }
                beamCounter++;

                double d0 = t.getX() + 0.5F - this.getX();
                double d1 = t.getY() + 0.5F - eyeHeight;
                double d2 = t.getZ() + 0.5F - this.getZ();
                double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                d0 = d0 / d3;
                d1 = d1 / d3;
                d2 = d2 / d3;
                double d4 = this.random.nextDouble();
                while (d4 < d3 - 0.5F) {
                    d4 += 1.0D - d5 + this.random.nextDouble();
                    if (random.nextFloat() < 0.1F) {
                        float r1 = 0.3F * (random.nextFloat() - 0.5F);
                        float r2 = 0.3F * (random.nextFloat() - 0.5F);
                        float r3 = 0.3F * (random.nextFloat() - 0.5F);

                        this.getWorld().addParticle(ParticleTypes.MYCELIUM, this.getX() + d0 * d4 + r1, eyeHeight + d1 * d4 + r2, this.getZ() + d2 * d4 + r3, r1 * 4, r2 * 4, r3 * 4);
                    }
                }
                if (beamCounter > 200) {
                    BlockState state = getWorld().getBlockState(t);
                    if (state.getBlock() instanceof Fertilizable igrowable) {
                        boolean flag = false;
                        if (igrowable.isFertilizable(this.getWorld(), t, state, this.getWorld().isClient)) {
                            for (int i = 0; i < 5; i++) {
                                float r1 = 3F * (random.nextFloat() - 0.5F);
                                float r2 = 2F * (random.nextFloat() - 0.5F);
                                float r3 = 3F * (random.nextFloat() - 0.5F);
                                this.getWorld().addParticle(ParticleTypes.EXPLOSION, t.getX() + 0.5F + r1, t.getY() + 0.5F + r2, t.getZ() + 0.5F + r3, r1 * 4, r2 * 4, r3 * 4);
                            }
                            if (!this.getWorld().isClient) {
                                this.getWorld().syncWorldEvent(2005, t, 0);
                                igrowable.grow((ServerWorld) this.getWorld(), this.getWorld().random, t, state);
                                flag = getWorld().getBlockState(t).getBlock() != state.getBlock();
                            }
                        }
                        if (!flag) {
                            int grown = 0;
                            int maxGrow = 2 + random.nextInt(3);
                            for (int i = 0; i < 15; i++) {
                                BlockPos pos = t.add(random.nextInt(10) - 5, random.nextInt(4) - 2, random.nextInt(10) - 5);
                                if (grown < maxGrow) {
                                    if (getWorld().getBlockState(pos).isAir() && getWorld().getBlockState(pos.down()).isOpaque()) {
                                        getWorld().setBlockState(pos, state);
                                        grown++;
                                    }
                                }
                            }
                        }
                        this.playSound(AMSoundRegistry.MUNGUS_LASER_END.get(), this.getSoundPitch(), this.getSoundVolume());
                        if (flag) {
                            this.playSound(AMSoundRegistry.MUNGUS_LASER_GROW.get(), this.getSoundPitch(), this.getSoundVolume());
                        }
                        this.setBeamTarget(null);
                        beamCounter = -1200;
                        if (this.getMushroomCount() > 0) {
                            this.setMushroomCount(this.getMushroomCount() - 1);
                        }
                    }
                }
            } else {
                this.setBeamTarget(null);
            }
        }
        if (beamCounter < 0) {
            beamCounter++;
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return stack.isIn(AMTagRegistry.MUNGUS_BREEDABLES);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess worldIn, LocalDifficulty difficultyIn, SpawnReason reason, @Nullable EntityData spawnDataIn, @Nullable NbtCompound dataTag) {
        this.dataTracker.set(ALT_ORDER_MUSHROOMS, random.nextBoolean());
        this.setMushroomCount(random.nextInt(2));
        setMushroomState(random.nextBoolean() ? Blocks.BROWN_MUSHROOM.getDefaultState() : Blocks.RED_MUSHROOM.getDefaultState());
        return super.initialize(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    public int getMushroomCount() {
        return this.dataTracker.get(MUSHROOM_COUNT);
    }

    public void setMushroomCount(int command) {
        this.dataTracker.set(MUSHROOM_COUNT, command);
    }

    public int getSackSwell() {
        return this.dataTracker.get(SACK_SWELL);
    }

    public void setSackSwell(int command) {
        this.dataTracker.set(SACK_SWELL, command);
    }

    @Nullable
    public BlockPos getBeamTarget() {
        return this.dataTracker.get(TARGETED_BLOCK_POS).orElse(null);
    }

    public void setBeamTarget(@Nullable BlockPos beamTarget) {
        this.dataTracker.set(TARGETED_BLOCK_POS, Optional.ofNullable(beamTarget));
    }

    public boolean isAltOrderMushroom() {
        return this.dataTracker.get(ALT_ORDER_MUSHROOMS);
    }

    @Nullable
    public BlockState getMushroomState() {
        return this.dataTracker.get(MUSHROOM_STATE).orElse(null);
    }

    public void setMushroomState(@Nullable BlockState state) {
        this.dataTracker.set(MUSHROOM_STATE, Optional.ofNullable(state));
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return AMEntityRegistry.MUNGUS.get().create(world);
    }

    public boolean isMushroomTarget(BlockPos pos) {
        if (this.getMushroomState() != null) {
            return getWorld().getBlockState(pos).getBlock() == this.getMushroomState().getBlock();
        }
        return false;
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return shouldFollowMushroom(stack) && this.getMushroomCount() < 5;
    }

    @Override
    public void onGetItem(ItemEntity e) {
        if (shouldFollowMushroom(e.getStack())) {
            BlockState state = getMushroomBlockstate(e.getStack().getItem());
            if (this.getMushroomState() != null && state != null && state.getBlock() != this.getMushroomState().getBlock()) {
                this.setMushroomCount(0);
            }
            this.emitGameEvent(GameEvent.BLOCK_PLACE);
            this.playSound(SoundEvents.BLOCK_FUNGUS_PLACE, this.getSoundVolume(), this.getSoundPitch());
            this.setMushroomState(state);
            this.setMushroomCount(this.getMushroomCount() + 1);
        }
    }

    private boolean hasLineOfSightMushroom(BlockPos destinationBlock) {
        var Vector3d = new Vec3d(this.getX(), this.getEyeY(), this.getZ());
        var blockVec = Vec3d.ofCenter(destinationBlock);
        var result = this.getWorld().raycast(new RaycastContext(Vector3d, blockVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        return result.getBlockPos().equals(destinationBlock);
    }

    @Override
    public boolean isShearable() {
        return this.isAlive() && this.getMushroomState() != null && this.getMushroomCount() > 0;
    }

    @Override
    public void sheared(SoundCategory category) {
        this.emitGameEvent(GameEvent.ENTITY_INTERACT);
        getWorld().playSoundFromEntity(null, this, SoundEvents.ENTITY_SHEEP_SHEAR, category, 1.0F, 1.0F);
        if (!this.getWorld().isClient() && this.getMushroomState() != null && this.getMushroomCount() > 0) {
            this.setMushroomCount(this.getMushroomCount() - 1);
            if (this.getMushroomCount() <= 0) {
                this.setMushroomState(null);
                this.setBeamTarget(null);
                beamCounter = Math.min(-1200, beamCounter);
            }

        }
    }

    public boolean isReverting() {
        return dataTracker.get(REVERTING);
    }

    public boolean isWarpedMoscoReady() {
        return this.getMushroomState() == Blocks.WARPED_FUNGUS.getDefaultState() && this.getMushroomCount() >= 5;
    }

    private class AITargetMushrooms extends Goal {
        private final int searchLength;
        protected BlockPos destinationBlock;
        protected int runDelay = 70;

        private AITargetMushrooms() {
            searchLength = 20;
        }

        @Override
        public boolean shouldContinue() {
            return destinationBlock != null && EntityMungus.this.isMushroomTarget(destinationBlock.mutableCopy()) && isCloseToShroom(32);
        }

        public boolean isCloseToShroom(double dist) {
            return destinationBlock == null || EntityMungus.this.squaredDistanceTo(Vec3d.ofCenter(destinationBlock)) < dist * dist;
        }

        @Override
        public boolean canStart() {
            if (EntityMungus.this.getBeamTarget() != null || EntityMungus.this.beamCounter < 0 || EntityMungus.this.getMushroomCount() <= 0) {
                return false;
            }
            if (this.runDelay > 0) {
                --this.runDelay;
                return false;
            } else {
                this.runDelay = 70 + EntityMungus.this.random.nextInt(150);
                return this.searchForDestination();
            }
        }

        @Override
        public void start() {
        }

        @Override
        public void tick() {
            if (this.destinationBlock == null || !EntityMungus.this.isMushroomTarget(this.destinationBlock) || EntityMungus.this.beamCounter < 0) {
                stop();
            } else {
                if (!EntityMungus.this.hasLineOfSightMushroom(this.destinationBlock)) {
                    EntityMungus.this.getNavigation().startMovingTo(this.destinationBlock.getX(), this.destinationBlock.getY(), this.destinationBlock.getZ(), 1D);
                } else {
                    EntityMungus.this.setBeamTarget(this.destinationBlock);
                    if (!EntityMungus.this.isInLove()) {
                        EntityMungus.this.getNavigation().stop();
                    }
                }
            }
        }

        @Override
        public void stop() {
            EntityMungus.this.setBeamTarget(null);
        }

        protected boolean searchForDestination() {
            int lvt_1_1_ = this.searchLength;
            var lvt_3_1_ = EntityMungus.this.getBlockPos();
            var lvt_4_1_ = new BlockPos.Mutable();

            for (int lvt_5_1_ = -5; lvt_5_1_ <= 5; lvt_5_1_++) {
                for (int lvt_6_1_ = 0; lvt_6_1_ < lvt_1_1_; ++lvt_6_1_) {
                    for (int lvt_7_1_ = 0; lvt_7_1_ <= lvt_6_1_; lvt_7_1_ = lvt_7_1_ > 0 ? -lvt_7_1_ : 1 - lvt_7_1_) {
                        for (int lvt_8_1_ = lvt_7_1_ < lvt_6_1_ && lvt_7_1_ > -lvt_6_1_ ? lvt_6_1_ : 0; lvt_8_1_ <= lvt_6_1_; lvt_8_1_ = lvt_8_1_ > 0 ? -lvt_8_1_ : 1 - lvt_8_1_) {
                            lvt_4_1_.set(lvt_3_1_, lvt_7_1_, lvt_5_1_ - 1, lvt_8_1_);
                            if (this.isMushroom(EntityMungus.this.getWorld(), lvt_4_1_)) {
                                this.destinationBlock = lvt_4_1_;
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }

        private boolean isMushroom(World world, BlockPos.Mutable lvt_4_1_) {
            return EntityMungus.this.isMushroomTarget(lvt_4_1_);
        }
    }
}