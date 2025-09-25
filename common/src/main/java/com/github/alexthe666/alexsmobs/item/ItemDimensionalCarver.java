package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.EntityVoidPortal;
import com.github.alexthe666.alexsmobs.registry.AMParticleRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class ItemDimensionalCarver extends Item {

    public ItemDimensionalCarver(Item.Settings props) {
        super(props);
    }

    protected static BlockHitResult rayTracePortal(World worldIn, PlayerEntity player, RaycastContext.FluidHandling fluidMode) {
        final float f = player.getPitch();
        final float f1 = player.getYaw();
        var vector3d = player.getCameraPosVec(1.0F);
        final float f11 = -f1 * MathHelper.RADIANS_PER_DEGREE - MathHelper.PI;
        final float f12 = -f * MathHelper.RADIANS_PER_DEGREE;
        final float f2 = MathHelper.cos(f11);
        final float f3 = MathHelper.sin(f11);
        final float f4 = -MathHelper.cos(f12);
        final float f5 = MathHelper.sin(f12);
        final float f6 = f3 * f4;
        final float f7 = f2 * f4;
        final double d0 = 1.5F;
        Vec3d vector3d1 = vector3d.add((double) f6 * d0, (double) f5 * d0, (double) f7 * d0);
        return worldIn.raycast(new RaycastContext(vector3d, vector3d1, RaycastContext.ShapeType.OUTLINE, fluidMode, player));
    }

    @Override
    public TypedActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack itemstack = playerIn.getStackInHand(handIn);
        if (itemstack.getDamage() >= itemstack.getMaxDamage()) {
            return TypedActionResult.fail(itemstack);
        } else {
            playerIn.setCurrentHand(handIn);
            var raytraceresult = rayTracePortal(worldIn, playerIn, RaycastContext.FluidHandling.ANY);
            var dir = Direction.getEntityFacingOrder(playerIn)[0];

            double x = raytraceresult.getPos().x - dir.getVector().getX() * 0.1F;
            double y = raytraceresult.getPos().y - dir.getVector().getY() * 0.1F;
            double z = raytraceresult.getPos().z - dir.getVector().getZ() * 0.1F;
            if (itemstack.getOrCreateNbt().getBoolean("HASBLOCK")) {
                x = itemstack.getOrCreateNbt().getDouble("BLOCKX");
                y = itemstack.getOrCreateNbt().getDouble("BLOCKY");
                z = itemstack.getOrCreateNbt().getDouble("BLOCKZ");
            } else {
                itemstack.getOrCreateNbt().putBoolean("HASBLOCK", true);
                itemstack.getOrCreateNbt().putDouble("BLOCKX", x);
                itemstack.getOrCreateNbt().putDouble("BLOCKY", y);
                itemstack.getOrCreateNbt().putDouble("BLOCKZ", z);
                itemstack.setNbt(itemstack.getOrCreateNbt());
            }
            worldIn.addParticle(AMParticleRegistry.INVERT_DIG.get(), x, y, z, playerIn.getId(), 0, 0);
            return TypedActionResult.consume(itemstack);
        }

    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 200;
    }

    //FIXME forge
    public float getXpRepairRatio(ItemStack stack) {
        return 100F;
    }

    @Override
    public void usageTick(World level, LivingEntity player, ItemStack itemstack, int count) {
        player.swingHand(player.getActiveHand());
        var random = player.getRandom();
        if (count % 5 == 0) {
            player.emitGameEvent(GameEvent.ITEM_INTERACT_START);
            player.playSound(SoundEvents.BLOCK_NETHERITE_BLOCK_HIT, 1, 0.5F + random.nextFloat());
        }
        boolean flag = false;
        if (itemstack.getOrCreateNbt().getBoolean("HASBLOCK")) {
            double x = itemstack.getOrCreateNbt().getDouble("BLOCKX");
            double y = itemstack.getOrCreateNbt().getDouble("BLOCKY");
            double z = itemstack.getOrCreateNbt().getDouble("BLOCKZ");
            if (random.nextFloat() < 0.2) {
                player.getWorld().addParticle(AMParticleRegistry.WORM_PORTAL.get(), x + random.nextGaussian() * 0.1F, y + random.nextGaussian() * 0.1F, z + random.nextGaussian() * 0.1F, random.nextGaussian() * 0.1F, -0.1F, random.nextGaussian() * 0.1F);
            }
            if (player.squaredDistanceTo(x, y, z) > 9) {
                flag = true;
                if (player instanceof PlayerEntity) {
                    ((PlayerEntity) player).getItemCooldownManager().set(this, 40);
                }
            }
            if (count == 1 && !player.getWorld().isClient) {
                player.emitGameEvent(GameEvent.ITEM_INTERACT_START);
                player.playSound(SoundEvents.BLOCK_GLASS_BREAK, 1, 0.5F);
                var portal = new EntityVoidPortal(player.getWorld(), this);
                portal.setPos(x, y, z);
                var dir = Direction.getEntityFacingOrder(player)[0].getOpposite();
                if (dir == Direction.UP) {
                    dir = Direction.DOWN;
                }
                portal.setAttachmentFacing(dir);
                player.getWorld().spawnEntity(portal);
                onPortalOpen(player.getWorld(), player, portal, dir);
                itemstack.damage(1, player, (playerIn) -> {
                    player.sendToolBreakStatus(playerIn.getActiveHand());
                });
                flag = true;
                if (player instanceof PlayerEntity) {
                    ((PlayerEntity) player).getItemCooldownManager().set(this, 200);
                }
            }
        }
        if (flag) {
            player.stopUsingItem();
            itemstack.getOrCreateNbt().putBoolean("HASBLOCK", false);
            itemstack.getOrCreateNbt().putDouble("BLOCKX", 0);
            itemstack.getOrCreateNbt().putDouble("BLOCKY", 0);
            itemstack.getOrCreateNbt().putDouble("BLOCKZ", 0);
            itemstack.setNbt(itemstack.getOrCreateNbt());
        }
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World worldIn, LivingEntity entityLiving, int timeLeft) {
        stack.getOrCreateNbt().putBoolean("HASBLOCK", false);
        stack.getOrCreateNbt().putDouble("BLOCKX", 0);
        stack.getOrCreateNbt().putDouble("BLOCKY", 0);
        stack.getOrCreateNbt().putDouble("BLOCKZ", 0);
        stack.setNbt(stack.getOrCreateNbt());
    }

    public void onPortalOpen(World worldIn, LivingEntity player, EntityVoidPortal portal, Direction dir){
        portal.setLifespan(1200);
        var respawnDimension = World.OVERWORLD;
        BlockPos respawnPosition = player.getSleepingPosition().isPresent() ? player.getSleepingPosition().get() : player.getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING, BlockPos.ORIGIN);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            respawnDimension = serverPlayer.getSpawnPointDimension();
            if (serverPlayer.getSpawnPointPosition() != null) {
                respawnPosition = serverPlayer.getSpawnPointPosition();
            }
        }
        portal.exitDimension = respawnDimension;
        portal.setDestination(respawnPosition.up(2));
    }
}
