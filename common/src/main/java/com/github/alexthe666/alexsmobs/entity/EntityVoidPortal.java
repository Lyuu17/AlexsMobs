package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.event.CommonEvents;
import com.github.alexthe666.alexsmobs.item.ItemDimensionalCarver;
import com.github.alexthe666.alexsmobs.registry.*;
import com.iafenvoy.uranus.util.Tuple3;
import dev.architectury.networking.NetworkManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EntityVoidPortal extends Entity {

    protected static final TrackedData<Direction> ATTACHED_FACE = DataTracker.registerData(EntityVoidPortal.class, TrackedDataHandlerRegistry.FACING);
    protected static final TrackedData<Integer> LIFESPAN = DataTracker.registerData(EntityVoidPortal.class, TrackedDataHandlerRegistry.INTEGER);
    protected static final TrackedData<Boolean> SHATTERED = DataTracker.registerData(EntityVoidPortal.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Optional<BlockPos>> DESTINATION = DataTracker.registerData(EntityVoidPortal.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);
    private static final TrackedData<Optional<UUID>> SISTER_UUID = DataTracker.registerData(EntityVoidPortal.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    public RegistryKey<World> exitDimension;
    private boolean madeOpenNoise = false;
    private boolean madeCloseNoise = false;
    private boolean isDummy = false;
    private boolean hasClearedObstructions;

    public EntityVoidPortal(EntityType<EntityVoidPortal> entityTypeIn, World worldIn) {
        super(entityTypeIn, worldIn);
    }

    public EntityVoidPortal(World world, ItemDimensionalCarver item) {
        this(AMEntityRegistry.VOID_PORTAL.get(), world);
        if(item == AMItemRegistry.SHATTERED_DIMENSIONAL_CARVER.get()){
            this.setShattered(true);
            this.setLifespan(2000);
        }else{
            this.setShattered(false);
            this.setLifespan(1200);
        }
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return NetworkManager.createAddEntityPacket(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.age == 1) {
            if(this.getLifespan() == 0){
                this.setLifespan(2000);
            }
        }
        if(!madeOpenNoise){
            this.emitGameEvent(GameEvent.ENTITY_PLACE);
            this.playSound(AMSoundRegistry.VOID_PORTAL_OPEN.get(), 1.0F, 1 + random.nextFloat() * 0.2F);
            madeOpenNoise = true;
        }
        var direction2 = this.getAttachmentFacing().getOpposite();
        float minX = -0.15F;
        float minY = -0.15F;
        float minZ = -0.15F;
        float maxX = 0.15F;
        float maxY = 0.15F;
        float maxZ = 0.15F;
        switch (direction2) {
            case NORTH, SOUTH -> {
                minX = -1.5F;
                maxX = 1.5F;
                minY = -1.5F;
                maxY = 1.5F;
            }
            case EAST, WEST -> {
                minZ = -1.5F;
                maxZ = 1.5F;
                minY = -1.5F;
                maxY = 1.5F;
            }
            case UP, DOWN -> {
                minX = -1.5F;
                maxX = 1.5F;
                minZ = -1.5F;
                maxZ = 1.5F;
            }
        }
        var bb = new Box(this.getX() + minX, this.getY() + minY, this.getZ() + minZ, this.getX() + maxX, this.getY() + maxY, this.getZ() + maxZ);
        this.setBoundingBox(bb);
        if(this.getWorld().isClient && random.nextFloat() < 0.5F && Math.min(this.age, this.getLifespan()) >= 20){
            final double particleX = this.getBoundingBox().minX + random.nextFloat() * (this.getBoundingBox().maxX - this.getBoundingBox().minX);
            final double particleY = this.getBoundingBox().minY + random.nextFloat() * (this.getBoundingBox().maxY - this.getBoundingBox().minY);
            final double particleZ = this.getBoundingBox().minZ + random.nextFloat() * (this.getBoundingBox().maxZ - this.getBoundingBox().minZ);
            getWorld().addParticle(AMParticleRegistry.WORM_PORTAL.get(), particleX, particleY, particleZ, 0.1 * random.nextGaussian(), 0.1 * random.nextGaussian(), 0.1 * random.nextGaussian());
        }
        List<Entity> entities = new ArrayList<>();
        entities.addAll(this.getWorld().getOtherEntities(this, bb.contract(0.2F)));
        entities.addAll(this.getWorld().getNonSpectatingEntities(EntityVoidWorm.class, bb.expand(1.5F)));
        if (!this.getWorld().isClient) {
            MinecraftServer server = getWorld().getServer();
            if (this.getDestination() != null && this.getLifespan() > 20 && this.age > 20) {
                BlockPos offsetPos = this.getDestination().offset(this.getAttachmentFacing().getOpposite(), 2);
                for (Entity e : entities) {
                    if (e instanceof IMultipartEntity multipartEntity) {
                        if (multipartEntity.getParts() != null) {
                            continue;
                        }
                    }

                    if(e.hasPortalCooldown() || e.isSneaking() || e instanceof EntityVoidPortal || e instanceof PartEntity<?> || e.getType().isIn(AMTagRegistry.VOID_PORTAL_IGNORES)){
                        continue;
                    }
                    if (e instanceof EntityVoidWormPart) {
                        if (this.getLifespan() < 22) {
                            this.setLifespan(this.getLifespan() + 1);
                        }
                    } else if (e instanceof EntityVoidWorm) {
                        ((EntityVoidWorm) e).teleportTo(Vec3d.ofCenter(this.getDestination()));
                        e.resetPortalCooldown();
                        ((EntityVoidWorm) e).resetPortalLogic();
                    } else {
                        boolean flag = true;
                        if(exitDimension != null){
                            var dimWorld = server.getWorld(exitDimension);
                            if (dimWorld != null && this.getWorld().getRegistryKey() != exitDimension) {
                                teleportEntityFromDimension(e, dimWorld, offsetPos, true);
                                flag = false;
                            }
                        }
                        if(flag){
                            e.teleport(offsetPos.getX() + 0.5f, offsetPos.getY() + 0.5f, offsetPos.getZ() + 0.5f);
                            e.resetPortalCooldown();
                        }
                    }
                }
            }
        }
        this.setLifespan(this.getLifespan() - 1);
        if(this.getLifespan() <= 20){
            if(!madeCloseNoise){
                this.emitGameEvent(GameEvent.ENTITY_PLACE);
                this.playSound(AMSoundRegistry.VOID_PORTAL_CLOSE.get(), 1.0F, 1 + random.nextFloat() * 0.2F);
                madeCloseNoise = true;
            }
        }
        if (this.getLifespan() <= 0) {
            this.remove(RemovalReason.DISCARDED);
        }
        if(this.age > 1){
            clearObstructions();
        }
    }

    private void teleportEntityFromDimension(Entity entity, ServerWorld endpointWorld, BlockPos endpoint, boolean b) {
        if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
            CommonEvents.teleportPlayers.add(new Tuple3<>(serverPlayerEntity, endpointWorld, endpoint));
            if(this.getSisterId() == null){
                createAndSetSister(endpointWorld, Direction.DOWN);
            }
        } else {
            entity.detach();
            entity.setWorld(endpointWorld);
            Entity teleportedEntity = entity.getType().create(endpointWorld);
            if (teleportedEntity != null) {
                teleportedEntity.copyFrom(entity);
                teleportedEntity.refreshPositionAndAngles(endpoint.getX() + 0.5D, endpoint.getY() + 0.5D, endpoint.getZ() + 0.5D, entity.getYaw(), entity.getPitch());
                teleportedEntity.setHeadYaw(entity.getHeadYaw());
                teleportedEntity.resetPortalCooldown();
                endpointWorld.spawnEntity(teleportedEntity);
            }
            entity.remove(RemovalReason.DISCARDED);
        }
    }

    public void clearObstructions(){
        if(!hasClearedObstructions){
            if(isShattered() && this.getDestination() != null){
                hasClearedObstructions = true;
                for (int i = -1; i <= -1; i++){
                    for (int j = -1; j <= -1; j++){
                        for (int k = -1; k <= -1; k++){
                            BlockPos toAir = this.getDestination().add(i, j, k);
                            getWorld().breakBlock(toAir, true);
                        }
                    }
                }
            }
        }
    }

    public Direction getAttachmentFacing() {
        return this.dataTracker.get(ATTACHED_FACE);
    }

    public void setAttachmentFacing(Direction facing) {
        this.dataTracker.set(ATTACHED_FACE, facing);
    }

    public int getLifespan() {
        return this.dataTracker.get(LIFESPAN);
    }

    public void setLifespan(int i) {
        this.dataTracker.set(LIFESPAN, i);
    }

    public boolean isShattered() {
        return this.dataTracker.get(SHATTERED);
    }

    public void setShattered(boolean set) {
        this.dataTracker.set(SHATTERED, set);
    }

    public BlockPos getDestination() {
        return this.dataTracker.get(DESTINATION).orElse(null);
    }

    public void setDestination(BlockPos destination) {
        this.dataTracker.set(DESTINATION, Optional.ofNullable(destination));
        if (this.getSisterId() == null && (exitDimension == null || exitDimension == this.getWorld().getRegistryKey())) {
            createAndSetSister(getWorld(), null);
        }
    }

    public void createAndSetSister(World world, Direction dir){
        var portal = AMEntityRegistry.VOID_PORTAL.get().create(world);
        portal.setAttachmentFacing(dir != null ? dir : this.getAttachmentFacing().getOpposite());
        BlockPos safeDestination = this.getDestination();
        portal.teleport(safeDestination.getX() + 0.5f, safeDestination.getY() + 0.5f, safeDestination.getZ() + 0.5f);
        portal.link(this);
        portal.exitDimension = this.getWorld().getRegistryKey();
        world.spawnEntity(portal);
        portal.setShattered(this.isShattered());
    }

    public void setDestination(BlockPos destination, Direction dir) {
        this.dataTracker.set(DESTINATION, Optional.ofNullable(destination));
        if (this.getSisterId() == null && (exitDimension == null || exitDimension == this.getWorld().getRegistryKey())) {
            createAndSetSister(getWorld(), dir);
        }
    }

    public void link(EntityVoidPortal portal) {
        this.setSisterId(portal.getUuid());
        portal.setSisterId(this.getUuid());
        portal.setLifespan(this.getLifespan());
        this.setDestination(portal.getBlockPos());
        portal.setDestination(this.getBlockPos());
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(ATTACHED_FACE, Direction.DOWN);
        this.dataTracker.startTracking(LIFESPAN, 300);
        this.dataTracker.startTracking(SHATTERED, false);
        this.dataTracker.startTracking(SISTER_UUID, Optional.empty());
        this.dataTracker.startTracking(DESTINATION, Optional.empty());
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound compound) {
        this.dataTracker.set(ATTACHED_FACE, Direction.byId(compound.getByte("AttachFace")));
        this.setLifespan(compound.getInt("Lifespan"));
        if(compound.contains("Shattered")){
            this.setShattered(compound.getBoolean("Shattered"));
        }
        if (compound.contains("DX")) {
            final int i = compound.getInt("DX");
            final int j = compound.getInt("DY");
            final int k = compound.getInt("DZ");
            this.dataTracker.set(DESTINATION, Optional.of(new BlockPos(i, j, k)));
        } else {
            this.dataTracker.set(DESTINATION, Optional.empty());
        }
        if (compound.containsUuid("SisterUUID")) {
            this.setSisterId(compound.getUuid("SisterUUID"));
        }
        if (compound.contains("ExitDimension")) {
            this.exitDimension = World.CODEC.parse(NbtOps.INSTANCE, compound.get("ExitDimension"))
                    .resultOrPartial(AlexsMobs.LOGGER::error)
                    .orElse(World.OVERWORLD);
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound compound) {
        compound.putByte("AttachFace", (byte) this.dataTracker.get(ATTACHED_FACE).getId());
        compound.putInt("Lifespan", getLifespan());
        compound.putBoolean("Shattered", isShattered());
        BlockPos blockpos = this.getDestination();
        if (blockpos != null) {
            compound.putInt("DX", blockpos.getX());
            compound.putInt("DY", blockpos.getY());
            compound.putInt("DZ", blockpos.getZ());
        }
        if (this.getSisterId() != null) {
            compound.putUuid("SisterUUID", this.getSisterId());
        }
        if(this.exitDimension != null){
            Identifier.CODEC.encodeStart(NbtOps.INSTANCE, this.exitDimension.getValue()).resultOrPartial(AlexsMobs.LOGGER::error).ifPresent((p_241148_1_) -> {
                compound.put("ExitDimension", p_241148_1_);
            });
        }

    }

    public Entity getSister() {
        UUID id = getSisterId();
        if (id != null && !this.getWorld().isClient) {
            return ((ServerWorld) getWorld()).getEntity(id);
        }
        return null;
    }

    @Nullable
    public UUID getSisterId() {
        return this.dataTracker.get(SISTER_UUID).orElse(null);
    }

    public void setSisterId(@Nullable UUID uniqueId) {
        this.dataTracker.set(SISTER_UUID, Optional.ofNullable(uniqueId));
    }

}
