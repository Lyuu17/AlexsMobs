package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.EntityCachalotEcho;
import com.github.alexthe666.alexsmobs.packet.SetPupfishChunkOnClientPacket;
import com.github.alexthe666.alexsmobs.registry.AMPointOfInterestRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.world.AMWorldData;
import com.google.common.base.Predicates;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.poi.PointOfInterestStorage;

import java.util.Collections;
import java.util.List;

public class ItemEcholocator extends Item {

    public EchoType type;

    public ItemEcholocator(Item.Settings properties, EchoType ender) {
        super(properties);
        this.type = ender;
    }

    private List<BlockPos> getNearbyPortals(BlockPos blockpos, ServerWorld world, int range) {
        if(type == EchoType.ENDER){
            var portals = world.getPointOfInterestStorage()
                    .getPositions(poiTypeHolder -> poiTypeHolder.matchesKey(AMPointOfInterestRegistry.END_PORTAL_FRAME.getKey()), Predicates.alwaysTrue(), blockpos, range, PointOfInterestStorage.OccupationStatus.ANY)
                    .toList();
            if(portals.isEmpty()){
                var nearestMapStructure = world.locateStructure(StructureTags.EYE_OF_ENDER_LOCATED, blockpos, 100, false);
                return nearestMapStructure == null ? Collections.emptyList() : List.of(nearestMapStructure);
            }else{
                return portals;
            }
        }else  if(type == EchoType.PUPFISH){
            AMWorldData data = AMWorldData.get(world);
            if(data != null && data.getPupfishChunk() != null){
                AlexsMobs.sendMSGToAll(new SetPupfishChunkOnClientPacket(data.getPupfishChunk().x, data.getPupfishChunk().z));
                if(!data.isInPupfishChunk(blockpos)){
                    return Collections.singletonList(data.getPupfishChunk().getCenterAtY(blockpos.getY()));
                }
            }
            return Collections.emptyList();
        }else{
            var random = world.getRandom();
            for(int i = 0; i < 256; i++){
                var checkPos = blockpos.add(random.nextInt(range) - range/2, random.nextInt(range)/2 - range/2, random.nextInt(range) - range/2);
                if(isCaveAir(world, checkPos)){
                    return Collections.singletonList(checkPos);
                }
            }
            return Collections.emptyList();
        }
    }

    private boolean isCaveAir(World world, BlockPos checkPos){
        return world.getBlockState(checkPos).isAir() && world.getLightLevel(LightType.SKY, checkPos) == 0 && world.getLightLevel(LightType.BLOCK, checkPos) < 4;
    }

    @Override
    public TypedActionResult<ItemStack> use(World worldIn, PlayerEntity livingEntityIn, Hand handIn) {
        ItemStack stack = livingEntityIn.getStackInHand(handIn);
        boolean left = false;
        if (livingEntityIn.getActiveHand() == Hand.OFF_HAND && livingEntityIn.getMainArm() == Arm.RIGHT || livingEntityIn.getActiveHand() == Hand.MAIN_HAND && livingEntityIn.getMainArm() == Arm.LEFT) {
            left = true;
        }
        EntityCachalotEcho whaleEcho = new EntityCachalotEcho(worldIn, livingEntityIn, !left, type == EchoType.PUPFISH);
        if (!worldIn.isClient && worldIn instanceof ServerWorld) {
            BlockPos playerPos = livingEntityIn.getBlockPos();
            List<BlockPos> portals = getNearbyPortals(playerPos, (ServerWorld) worldIn, 128);
            BlockPos pos = null;
            if(type == EchoType.ENDER){
                for (BlockPos portalPos : portals) {
                    if (pos == null || pos.getSquaredDistance(playerPos) > portalPos.getSquaredDistance(playerPos)) {
                        pos = portalPos;
                    }
                }
            }else if(type == EchoType.PUPFISH){
                for (BlockPos portalPos : portals) {
                    if (pos == null || pos.getSquaredDistance(playerPos) > portalPos.getSquaredDistance(playerPos)) {
                        pos = portalPos;
                    }
                }
            }else{
                var nbt = stack.getOrCreateNbt();
                if(nbt.contains("CavePos") && nbt.getBoolean("ValidCavePos")){
                    pos = BlockPos.fromLong(nbt.getLong("CavePos"));
                    if(isCaveAir(worldIn, pos) || 1000000 < pos.getSquaredDistance(playerPos)){
                        nbt.putBoolean("ValidCavePos", false);
                    }
                }else{
                    for (var portalPos : portals) {
                        if (pos == null || pos.getSquaredDistance(playerPos) < portalPos.getSquaredDistance(playerPos)) {
                            pos = portalPos;
                        }
                    }
                    if(pos != null){
                        nbt.putLong("CavePos", pos.asLong());
                        nbt.putBoolean("ValidCavePos", true);
                        stack.setNbt(nbt);
                    }
                }

            }
            if (pos != null) {
                double d0 = pos.getX() + 0.5F - whaleEcho.getX();
                double d1 = pos.getY() + 0.5F - whaleEcho.getY();
                double d2 = pos.getZ() + 0.5F - whaleEcho.getZ();
                whaleEcho.age = 15;
                whaleEcho.shoot(d0, d1, d2, 0.4F, 0.3F);
                worldIn.spawnEntity(whaleEcho);
                livingEntityIn.emitGameEvent(GameEvent.ITEM_INTERACT_START);
                worldIn.playSound(null, whaleEcho.getX(), whaleEcho.getY(), whaleEcho.getZ(), AMSoundRegistry.CACHALOT_WHALE_CLICK.get(), SoundCategory.PLAYERS, 1.0F, 1.0F);
                stack.damage(1, livingEntityIn, (player) -> {
                    player.sendToolBreakStatus(livingEntityIn.getActiveHand());
                });
            }
        }
        livingEntityIn.getItemCooldownManager().set(this, 5);

        return TypedActionResult.success(stack, worldIn.isClient());
    }

    public enum EchoType {
        ECHOLOCATION, ENDER, PUPFISH
    }
}
