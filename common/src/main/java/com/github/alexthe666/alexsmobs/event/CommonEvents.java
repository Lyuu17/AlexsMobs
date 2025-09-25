package com.github.alexthe666.alexsmobs.event;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.EntityBunfungus;
import com.github.alexthe666.alexsmobs.entity.EntityElephant;
import com.github.alexthe666.alexsmobs.entity.EntityEndergrade;
import com.github.alexthe666.alexsmobs.entity.EntityTiger;
import com.github.alexthe666.alexsmobs.entity.util.RainbowUtil;
import com.github.alexthe666.alexsmobs.entity.util.VineLassoUtil;
import com.github.alexthe666.alexsmobs.registry.*;
import com.github.alexthe666.alexsmobs.world.AMWorldData;
import com.github.alexthe666.alexsmobs.world.BeachedCachalotWhaleSpawner;
import com.iafenvoy.uranus.util.Tuple3;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ExperienceBarUpdateS2CPacket;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

//
//import com.github.alexthe666.alexsmobs.AlexsMobs;
//import com.github.alexthe666.alexsmobs.config.AMConfig;
//import com.github.alexthe666.alexsmobs.entity.EntityBunfungus;
//import com.github.alexthe666.alexsmobs.entity.EntityElephant;
//import com.github.alexthe666.alexsmobs.entity.EntityEndergrade;
//import com.github.alexthe666.alexsmobs.entity.util.RainbowUtil;
//import com.github.alexthe666.alexsmobs.entity.util.VineLassoUtil;
//import com.github.alexthe666.alexsmobs.registry.*;
//import com.github.alexthe666.alexsmobs.world.AMWorldData;
//import com.github.alexthe666.alexsmobs.world.BeachedCachalotWhaleSpawner;
//import dev.architectury.event.Event;
//import dev.architectury.event.EventResult;
//import dev.architectury.event.events.common.InteractionEvent;
//import dev.architectury.event.events.common.LightningEvent;
//import dev.architectury.event.events.common.PlayerEvent;
//import dev.architectury.event.events.common.TickEvent;
//import dev.architectury.registry.level.entity.trade.TradeRegistry;
//import it.unimi.dsi.fastutil.objects.ObjectArrayList;
//import it.unimi.dsi.fastutil.objects.ObjectList;
//import net.minecraft.block.Blocks;
//import net.minecraft.command.EntitySelector;
//import net.minecraft.entity.EntityType;
//import net.minecraft.entity.EquipmentSlot;
//import net.minecraft.entity.LivingEntity;
//import net.minecraft.entity.attribute.EntityAttributeModifier;
//import net.minecraft.entity.passive.RabbitEntity;
//import net.minecraft.entity.player.PlayerEntity;
//import net.minecraft.item.ItemStack;
//import net.minecraft.item.Items;
//import net.minecraft.predicate.entity.EntityPredicates;
//import net.minecraft.registry.tag.FluidTags;
//import net.minecraft.server.network.ServerPlayerEntity;
//import net.minecraft.server.world.ChunkTicketType;
//import net.minecraft.server.world.ServerWorld;
//import net.minecraft.sound.SoundCategory;
//import net.minecraft.sound.SoundEvents;
//import net.minecraft.stat.Stats;
//import net.minecraft.util.ActionResult;
//import net.minecraft.util.Hand;
//import net.minecraft.util.hit.BlockHitResult;
//import net.minecraft.util.hit.HitResult;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.Box;
//import net.minecraft.util.math.ChunkPos;
//import net.minecraft.util.math.MathHelper;
//import net.minecraft.world.Heightmap;
//import net.minecraft.world.RaycastContext;
//import net.minecraft.world.World;
//import net.minecraft.world.event.GameEvent;
//import org.apache.commons.lang3.tuple.Triple;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//import java.util.concurrent.ThreadLocalRandom;
//
public class CommonEvents {

    public static final UUID ALEX_UUID = UUID.fromString("71363abe-fd03-49c9-940d-aae8b8209b7c");
    public static final UUID CARRO_UUID = UUID.fromString("98905d4a-1cbc-41a4-9ded-2300404e2290");
    private static final Map<ServerWorld, BeachedCachalotWhaleSpawner> BEACHED_CACHALOT_WHALE_SPAWNER_MAP = new HashMap<>();
    public static final ObjectList<Tuple3<ServerPlayerEntity, ServerWorld, BlockPos>> teleportPlayers = new ObjectArrayList<>();

    protected static BlockHitResult rayTrace(World worldIn, PlayerEntity player, RaycastContext.FluidHandling fluidMode) {
        final float x = player.getPitch();
        final float y = player.getYaw();
        var vector3d = player.getCameraPosVec(1.0F);
        final float f0 = -y * MathHelper.RADIANS_PER_DEGREE - MathHelper.PI;
        final float f1 = -x * MathHelper.RADIANS_PER_DEGREE;
        final float f2 = MathHelper.cos(f0);
        final float f3 = MathHelper.sin(f0);
        final float f4 = -MathHelper.cos(f1);
        final float f5 = MathHelper.sin(f1);
        final float f6 = f3 * f4;
        final float f7 = f2 * f4;
        //FIXME forge
//        final double d0 = player.getAttributeInstance(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get()).getValue();
        final var d0 = 4.5;
        var vector3d1 = vector3d.add(f6 * d0, f5 * d0, f7 * d0);
        return worldIn.raycast(new RaycastContext(vector3d, vector3d1, RaycastContext.ShapeType.OUTLINE, fluidMode, player));
    }

    public static void init() {

        EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            if (!entity.getActiveItem().isEmpty() && source != null && source.getAttacker() != null) {
                if (entity.getActiveItem().getItem() == AMItemRegistry.SHIELD_OF_THE_DEEP.get()) {
                    if (source.getAttacker() instanceof LivingEntity living) {
                        boolean flag = false;
                        if (living.distanceTo(entity) <= 4
                                && !living.hasStatusEffect(AMEffectRegistry.EXSANGUINATION.get())) {
                            living.addStatusEffect(new StatusEffectInstance(AMEffectRegistry.EXSANGUINATION.get(), 60, 2));
                            flag = true;
                        }
                        if (entity.isInsideWaterOrBubbleColumn()) {
                            entity.setAir(Math.min(entity.getMaxAir(), entity.getAir() + 150));
                            flag = true;
                        }
                        if (flag) {
                            entity.getActiveItem().damage(1, entity,
                                    player -> player.sendToolBreakStatus(entity.getActiveHand()));
                        }
                    }
                }
            }
            return EventResult.pass();
        });

        TickEvent.SERVER_LEVEL_PRE.register(serverWorld -> {
            if (!serverWorld.isClient) {
                BEACHED_CACHALOT_WHALE_SPAWNER_MAP.computeIfAbsent(serverWorld,
                        k -> new BeachedCachalotWhaleSpawner(serverWorld));
                BeachedCachalotWhaleSpawner spawner = BEACHED_CACHALOT_WHALE_SPAWNER_MAP.get(serverWorld);
                spawner.tick();

                if (!teleportPlayers.isEmpty()) {
                    for (final var triple : teleportPlayers) {
                        ServerPlayerEntity player = triple.getA();
                        ServerWorld endpointWorld = triple.getB();
                        BlockPos endpoint = triple.getC();
                        final int heightFromMap = endpointWorld.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, endpoint.getX(), endpoint.getZ());
                        endpoint = new BlockPos(endpoint.getX(), Math.max(heightFromMap, endpoint.getY()), endpoint.getZ());
                        player.teleport(endpointWorld, endpoint.getX() + 0.5D, endpoint.getY() + 0.5D, endpoint.getZ() + 0.5D, player.getYaw(), player.getPitch());
                        var chunkpos = new ChunkPos(endpoint);
                        endpointWorld.getChunkManager().addTicket(ChunkTicketType.POST_TELEPORT, chunkpos, 1, player.getId());
                        player.networkHandler.sendPacket(new ExperienceBarUpdateS2CPacket(player.experienceProgress, player.totalExperience, player.experienceLevel));

                    }
                    teleportPlayers.clear();
                }
            }
            var data = AMWorldData.get(serverWorld);
            if (data != null) {
                data.tickPupfish();
            }
        });
//
//        LightningEvent.STRIKE.register((bolt, level, pos, toStrike) -> {
//            if (event.getEntity().getType() == EntityType.SQUID && !event.getEntity().getWorld().isClient) {
//                ServerWorld level = (ServerWorld) event.getEntity().getWorld();
//                event.setCanceled(true);
//                var squid = AMEntityRegistry.GIANT_SQUID.get().create(level);
//                squid.moveTo(event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity().getYaw(), event.getEntity().getPitch();
//                squid.initialize(level, level.getLocalDifficulty(squid.getBlockPos()), SpawnReason.CONVERSION, null, null);
//                if (event.getEntity().hasCustomName()) {
//                    squid.setCustomName(event.getEntity().getCustomName());
//                    squid.setCustomNameVisible(event.getEntity().isCustomNameVisible());
//                }
//                squid.setBlue(true);
//                squid.setPersistent();
//                level.spawnEntityAndPassengers(squid);
//                event.getEntity().discard();
//            }
//        });
//
//        PlayerEvent.PLAYER_JOIN.register(player -> {
//            if (AMConfig.giveBookOnStartup) {
//                var playerData = player.getPersistentData();
//                var data = playerData.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
//                if (data != null && !data.getBoolean("alexsmobs_has_book")) {
//                    ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(AMItemRegistry.ANIMAL_DICTIONARY.get()));
//                    final boolean isAlex = Objects.equals(player.getUuid(), ALEX_UUID);
//                    if (isAlex || Objects.equals(player.getUuid(), CARRO_UUID)) {
//                        ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(AMItemRegistry.BEAR_DUST.get()));
//                    }
//                    if (isAlex) {
//                        ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(AMItemRegistry.NOVELTY_HAT.get()));
//                    }
//                    data.putBoolean("alexsmobs_has_book", true);
//                    playerData.put(PlayerEntity.PERSISTED_NBT_TAG, data);
//                }
//            }
//        });

        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> {
            if (player.getStackInHand(hand).getItem() == Items.WHEAT && player.getVehicle() instanceof EntityElephant elephant) {
                if (elephant.triggerCharge(player.getStackInHand(hand))) {
                    player.swingHand(hand);
                    if (!player.isCreative()) {
                        player.getStackInHand(hand).decrement(1);
                    }
                }
            }
            if (player.getStackInHand(hand).getItem() == Items.GLASS_BOTTLE && AMConfig.lavaBottleEnabled) {
                var raytraceresult = rayTrace(player.getWorld(), player, RaycastContext.FluidHandling.SOURCE_ONLY);
                if (raytraceresult.getType() == HitResult.Type.BLOCK) {
                    var blockpos = raytraceresult.getBlockPos();
                    if (player.getWorld().canPlayerModifyAt(player, blockpos)) {
                        if (player.getWorld().getFluidState(blockpos).isIn(FluidTags.LAVA)) {
                            player.emitGameEvent(GameEvent.ITEM_INTERACT_START);
                            player.getWorld().playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                            player.incrementStat(Stats.USED.getOrCreateStat(Items.GLASS_BOTTLE));
                            player.setOnFireFor(6);
                            if (!player.giveItemStack(new ItemStack(AMItemRegistry.LAVA_BOTTLE.get()))) {
                                player.dropStack(new ItemStack(AMItemRegistry.LAVA_BOTTLE.get()));
                            }
                            player.swingHand(hand);
                            if (!player.isCreative()) {
                                player.getActiveItem().decrement(1);
                            }
                        }
                    }
                }
            }
            return CompoundEventResult.pass();
        });

        InteractionEvent.CLIENT_RIGHT_CLICK_AIR.register((player, hand) -> {
            var stack = player.getStackInHand(hand);
            if (stack.isEmpty()) {
                stack = player.getEquippedStack(EquipmentSlot.MAINHAND);
            }
            if (RainbowUtil.getRainbowType(player) > 0 && (stack.isOf(Items.SPONGE))) {
                player.swingHand(Hand.MAIN_HAND);
                RainbowUtil.setRainbowType(player, 0);
                if (!player.isCreative()) {
                    stack.decrement(1);
                }
                var wetSponge = new ItemStack(Items.WET_SPONGE);
                if (!player.giveItemStack(wetSponge)) {
                    player.dropItem(wetSponge, true);
                }
            }
        });

        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) -> {
            if (AlexsMobs.isAprilFools() && player.getStackInHand(hand).isOf(Items.STICK)
                    && !player.getItemCooldownManager().isCoolingDown(Items.STICK)) {
                var state = player.getWorld().getBlockState(pos);
                boolean flag = false;
                if (state.isOf(Blocks.SAND)) {
                    flag = true;
                    player.getWorld().setBlockState(pos, AMBlockRegistry.SAND_CIRCLE.get().getDefaultState());
                } else if (state.isOf(Blocks.RED_SAND)) {
                    flag = true;
                    player.getWorld().setBlockState(pos, AMBlockRegistry.RED_SAND_CIRCLE.get().getDefaultState());
                }
                if (flag) {
                    player.emitGameEvent(GameEvent.BLOCK_PLACE);
                    player.playSound(SoundEvents.BLOCK_SAND_BREAK, 1, 1);
                    player.getItemCooldownManager().set(Items.STICK, 30);
                    return EventResult.interruptTrue();
                }
            }
            return EventResult.pass();
        });

        PlayerEvent.ATTACK_ENTITY.register((player, level, target, hand, result) -> {
            if (target instanceof LivingEntity living) {
                if (player.getEquippedStack(EquipmentSlot.HEAD).getItem() == AMItemRegistry.MOOSE_HEADGEAR.get()) {
                    living.takeKnockback(1F, MathHelper.sin(player.getYaw() * MathHelper.RADIANS_PER_DEGREE),
                            -MathHelper.cos(player.getYaw() * MathHelper.RADIANS_PER_DEGREE));
                }
                if (player.hasStatusEffect(AMEffectRegistry.TIGERS_BLESSING.get())
                        && !target.isTeammate(player) && !(target instanceof EntityTiger)) {
                    var bb = new Box(player.getX() - 32, player.getY() - 32, player.getZ() - 32, player.getZ() + 32, player.getY() + 32, player.getZ() + 32);
                    final var tigers = player.getWorld().getEntitiesByClass(EntityTiger.class, bb,
                            EntityPredicates.VALID_ENTITY);
                    for (var tiger : tigers) {
                        if (!tiger.isBaby()) {
                            tiger.setTarget(living);
                        }
                    }
                }
            }
            return EventResult.pass();
        });

        InteractionEvent.INTERACT_ENTITY.register((player, entity, hand) -> {
            var result = EventResult.pass();
            if (entity instanceof LivingEntity living) {
                if (!player.isSneaking() && VineLassoUtil.hasLassoData(living)) {
                    if (!player.getWorld().isClient) {
                        entity.dropStack(new ItemStack(AMItemRegistry.VINE_LASSO.get()));
                    }
                    VineLassoUtil.lassoTo(null, living);
                    result = EventResult.interruptTrue();
                }
                if (!(entity instanceof PlayerEntity) && !(entity instanceof EntityEndergrade)
                        && living.hasStatusEffect(AMEffectRegistry.ENDER_FLU.get())) {
                    if (player.getActiveItem().getItem() == Items.CHORUS_FRUIT) {
                        if (!player.isCreative()) {
                            player.getActiveItem().decrement(1);
                        }
                        entity.emitGameEvent(GameEvent.EAT);
                        entity.playSound(SoundEvents.ENTITY_GENERIC_EAT, 1.0F, 0.5F + player.getRandom().nextFloat());
                        if (player.getRandom().nextFloat() < 0.4F) {
                            living.removeStatusEffect(AMEffectRegistry.ENDER_FLU.get());
                            Items.CHORUS_FRUIT.finishUsing(player.getActiveItem().copy(), player.getWorld(), living);
                        }
                        result = EventResult.interruptTrue();
                    }
                }
                if (RainbowUtil.getRainbowType(living) > 0 && (player.getActiveItem().getItem() == Items.SPONGE)) {
                    result = EventResult.interruptTrue();
                    RainbowUtil.setRainbowType(living, 0);
                    if (!player.isCreative()) {
                        player.getActiveItem().decrement(1);
                    }
                    ItemStack wetSponge = new ItemStack(Items.WET_SPONGE);
                    if (!player.giveItemStack(wetSponge)) {
                        player.dropItem(wetSponge, true);
                    }

                }
                if (living instanceof RabbitEntity rabbit && player.getActiveItem().getItem() == AMItemRegistry.MUNGAL_SPORES.get()
                        && AMConfig.bunfungusTransformation) {
                    final var random = ThreadLocalRandom.current();
                    if (!player.getWorld().isClient && random.nextFloat() < 0.15F) {
                        final var bunfungus = rabbit.convertTo(AMEntityRegistry.BUNFUNGUS.get(), true);
                        if (bunfungus != null) {
                            player.getWorld().spawnEntity(bunfungus);
                            bunfungus.setTransformsIn(EntityBunfungus.MAX_TRANSFORM_TIME);
                        }
                    } else {
                        for (int i = 0; i < 2 + random.nextInt(2); i++) {
                            final double d0 = random.nextGaussian() * 0.02D;
                            final double d1 = 0.05F + random.nextGaussian() * 0.02D;
                            final double d2 = random.nextGaussian() * 0.02D;
                            entity.getWorld().addParticle(AMParticleRegistry.BUNFUNGUS_TRANSFORMATION.get(), entity.getParticleX(0.7F), entity.getBodyY(0.6F), entity.getParticleZ(0.7F), d0, d1, d2);
                        }
                    }
                    if (!player.isCreative()) {
                        player.getActiveItem().decrement(1);
                    }
                    result = EventResult.interruptTrue();
                }
            }
            return result;
        });
    }
}
