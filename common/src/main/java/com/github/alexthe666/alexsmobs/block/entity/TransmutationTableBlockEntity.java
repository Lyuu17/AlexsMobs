package com.github.alexthe666.alexsmobs.block.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.misc.TransmutationData;
import com.github.alexthe666.alexsmobs.packet.UpdateTransmutablesToDisplayPacket;
import com.github.alexthe666.alexsmobs.registry.AMAdvancementTriggerRegistry;
import com.github.alexthe666.alexsmobs.registry.AMBlockEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.screen.TransmutationTableScreenHandler;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class TransmutationTableBlockEntity extends BlockEntity implements ExtendedMenuProvider, BlockEntityTicker<TransmutationTableBlockEntity> {

    private static final Identifier COMMON_ITEMS = new Identifier("alexsmobs", "gameplay/transmutation_table_common");
    private static final Identifier UNCOMMON_ITEMS = new Identifier("alexsmobs", "gameplay/transmutation_table_uncommon");
    private static final Identifier RARE_ITEMS = new Identifier("alexsmobs", "gameplay/transmutation_table_rare");

    public int ticksExisted;
    private int totalTransmuteCount = 0;
    private final Map<UUID, TransmutationData> playerToData = new HashMap<>();
    private final ItemStack[] possiblities = new ItemStack[3];
    private static final Random RANDOM = new Random();

    private UUID rerollPlayerUUID = null;

    public TransmutationTableBlockEntity(BlockPos pos, BlockState state) {
        super(AMBlockEntityRegistry.TRANSMUTATION_TABLE.get(), pos, state);
    }

    @Override
    public void saveExtraData(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("alexsmobs.container.transmutation_table");
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new TransmutationTableScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public void tick(World world, BlockPos pos, BlockState state, TransmutationTableBlockEntity blockEntity) {
        ticksExisted++;
        if(rerollPlayerUUID != null){
            var player = world.getPlayerByUuid(rerollPlayerUUID);
            if(player != null) {
                this.world.playSound(null, this.getPos(), AMSoundRegistry.TRANSMUTE_ITEM.get(), SoundCategory.BLOCKS, 1F, 0.9F + player.getRandom().nextFloat() * 0.2F);
                this.randomizeResults(pos, player);
            }
            rerollPlayerUUID = null;
        }
    }

    private static ItemStack createFromLootTable(BlockPos pos, PlayerEntity player, Identifier loc) {
        if (player.getWorld().isClient){
            return ItemStack.EMPTY;
        }else{
            var loottable = player.getWorld().getServer().getLootManager().getLootTable(loc);
            var loots = loottable.generateLoot(new LootContextParameterSet.Builder((ServerWorld) player.getWorld())
                    .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
                    .add(LootContextParameters.THIS_ENTITY, player)
                    .build(LootContextTypes.CHEST));
            return loots.isEmpty() ? ItemStack.EMPTY : loots.get(0);
        }
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        totalTransmuteCount = tag.getInt("TotalCount");
        var list = new NbtList();
        for(Map.Entry<UUID, TransmutationData> entry : playerToData.entrySet()){
            NbtCompound innerTag = new NbtCompound();
            innerTag.putUuid("UUID", entry.getKey());
            innerTag.put("TransmutationData", entry.getValue().saveAsNBT());
            list.add(innerTag);
        }
        tag.put("PlayerTransmutationData", list);
        for(int i = 0; i < 3; i++){
            if(tag.contains("Possibility" + i)){
                possiblities[i] = ItemStack.fromNbt(tag.getCompound("Possiblity" + i));
            }
        }
    }

    @Override
    protected void writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        tag.putInt("TotalCount", totalTransmuteCount);
        var list = tag.getList("PlayerTransmutationData", 10);
        if(!list.isEmpty()){
            for(int i = 0; i < list.size(); ++i) {
                NbtCompound NbtCompound = list.getCompound(i);
                UUID uuid = NbtCompound.getUuid("UUID");
                if(uuid != null){
                    playerToData.put(uuid, TransmutationData.fromNBT(NbtCompound.getCompound("TransmutationData")));
                }
            }
        }
        for(int i = 0; i < 3; i++){
            if(possiblities[i] != null && !possiblities[i].isEmpty()){
                tag.put("Possiblity" + i, possiblities[i].getNbt());
            }
        }
    }

    private void randomizeResults(BlockPos pos, PlayerEntity player){
        rollPossiblity(pos, player, 0);
        rollPossiblity(pos, player, 1);
        rollPossiblity(pos, player, 2);
        int dataIndex = RANDOM.nextInt(2);
        if(playerToData.containsKey(player.getUuid()) && !AMConfig.limitTransmutingToLootTables){
            TransmutationData data = playerToData.get(player.getUuid());
            if(RANDOM.nextFloat() < Math.min(0.01875F * data.getTotalWeight(), 0.2F)){
                ItemStack stack = data.getRandomItem(RANDOM);
                if(stack != null && !stack.isEmpty()){
                    possiblities[dataIndex] = stack;
                }
            }
        }
        AlexsMobs.sendMSGToAll(new UpdateTransmutablesToDisplayPacket(player.getId(), getPossibility(0), getPossibility(1), getPossibility(2)));
    }

    public void rollPossiblity(BlockPos pos, PlayerEntity player, int i){
        if(player == null || player.getWorld().isClient || !(player.getWorld() instanceof ServerWorld)){
            return;
        }
        int safeIndex = MathHelper.clamp(i, 0, 2);
        var loot = switch (safeIndex) {
            case 1 -> UNCOMMON_ITEMS;
            case 2 -> RARE_ITEMS;
            default -> COMMON_ITEMS;
        };
        possiblities[safeIndex] = createFromLootTable(pos, player, loot);
    }

    public boolean hasPossibilities(){
        for(int i = 0; i < 3; i++){
            if(possiblities[i] == null || possiblities[i].isEmpty()){
                return false;
            }
        }
        return true;
    }

    public ItemStack getPossibility(int i){
        int safeIndex = MathHelper.clamp(i, 0, 2);
        ItemStack possible = possiblities[safeIndex];
        return possible == null ? ItemStack.EMPTY : possible;
    }

    public void postTransmute(PlayerEntity player, ItemStack from, ItemStack to){
        TransmutationData data;
        if(playerToData.containsKey(player.getUuid())){
            data = playerToData.get(player.getUuid());
        }else{
            data = new TransmutationData();
        }
        data.onTransmuteItem(from, to);
        playerToData.put(player.getUuid(), data);
        totalTransmuteCount += from.getCount();
        if(player instanceof ServerPlayerEntity && totalTransmuteCount >= 1000){
            AMAdvancementTriggerRegistry.TRANSMUTE_1000_ITEMS.trigger((ServerPlayerEntity) player);
        }
        setRerollPlayerUUID(player.getUuid());
    }

    public void setRerollPlayerUUID(UUID uuid){
        this.rerollPlayerUUID = uuid;
    }
}
