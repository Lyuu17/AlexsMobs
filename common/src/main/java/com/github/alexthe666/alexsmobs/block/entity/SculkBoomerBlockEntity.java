package com.github.alexthe666.alexsmobs.block.entity;

import com.github.alexthe666.alexsmobs.block.SculkBoomerBlock;
import com.github.alexthe666.alexsmobs.registry.AMBlockEntityRegistry;
import com.github.alexthe666.alexsmobs.registry.AMParticleRegistry;
import com.github.alexthe666.alexsmobs.registry.AMSoundRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.VibrationParticleEffect;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.BlockStateRaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.BlockPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.listener.GameEventListener;

public class SculkBoomerBlockEntity extends BlockEntity implements GameEventListener {

    private final BlockPositionSource blockPosSource = new BlockPositionSource(this.pos);
    private boolean prevOpen = false;
    private int screamTime = 0;

    public SculkBoomerBlockEntity(BlockPos pos, BlockState state) {
        super(AMBlockEntityRegistry.SCULK_BOOMER.get(), pos, state);
    }

    public static void commonTick(World level, BlockPos pos, BlockState state, SculkBoomerBlockEntity tileEntity) {
        boolean hasPower = false;
        if (state.getBlock() instanceof SculkBoomerBlock && !tileEntity.isRemoved()) {
            if(tileEntity.screamTime < 0 && !state.get(SculkBoomerBlock.POWERED)){
                var screamBox = new Box(pos.getX() - 4, pos.getY() - 0.25F, pos.getZ() - 4, pos.getX() + 4, pos.getY() + 0.25F, pos.getZ() + 4F);
                level.setBlockState(pos, state.with(SculkBoomerBlock.OPEN, true));
                tileEntity.screamTime++;
                if(tileEntity.screamTime >= 0){
                    tileEntity.screamTime = 100;
                    level.setBlockState(pos, state.with(SculkBoomerBlock.OPEN, false));
                }
                float screamProgress = 1F - (tileEntity.screamTime / -20F);
                var center = screamBox.getCenter();
                for(var entity : level.getNonSpectatingEntities(LivingEntity.class, screamBox)){
                    double distance = 0.5F + entity.getPos().subtract(center).horizontalLength();
                    if(distance < 4 * screamProgress && distance > 3.5F * screamProgress && !isOccluded(level, Vec3d.ofCenter(pos), entity.getPos())){
                        entity.damage(entity.getDamageSources().magic(), 6 + entity.getRandom().nextInt(3));
                        entity.takeKnockback(0.4F,  center.x - entity.getX(), center.z - entity.getZ());
                    }
                }
            }
            if(tileEntity.screamTime > 0){
                tileEntity.screamTime--;
            }
            boolean openNow = state.get(SculkBoomerBlock.OPEN);
            if(!tileEntity.prevOpen && openNow){
                SoundEvent sound = AMSoundRegistry.SCULK_BOOMER.get();
                if(level.getRandom().nextInt(100) == 0){
                    sound = AMSoundRegistry.SCULK_BOOMER_FART.get();
                }
                level.playSound(null, pos, sound, SoundCategory.BLOCKS, 4F, level.random.nextFloat() * 0.2F + 0.9F);
                level.addParticle(AMParticleRegistry.SKULK_BOOM.get(), pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, 0, 0, 0);
            }
            tileEntity.prevOpen = openNow;
        }
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        if (tag.contains("ScreamCooldown", 99)) {
            this.screamTime = tag.getInt("ScreamCooldown");
        }
    }

    @Override
    protected void writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        tag.putInt("ScreamCooldown", this.screamTime);
    }

    @Override
    public PositionSource getPositionSource() {
        return blockPosSource;
    }

    @Override
    public int getRange() {
        return 8;
    }

    @Override
    public boolean listen(ServerWorld serverLevel, GameEvent event, GameEvent.Emitter message, Vec3d from) {
        if(event == GameEvent.SCULK_SENSOR_TENDRILS_CLICKING && !isOccluded(serverLevel, Vec3d.ofCenter(this.getPos()), from)){
            double distance = from.distanceTo(Vec3d.ofCenter(this.getPos()));
            serverLevel.spawnParticles(new VibrationParticleEffect(new BlockPositionSource(this.getPos()), MathHelper.floor(distance)), from.x, from.y, from.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            if(screamTime == 0){
                screamTime = -20;
            }
        }
        return false;
    }

    private static boolean isOccluded(World level, Vec3d vec1, Vec3d vec2) {
        Vec3d vec3 = new Vec3d((double) MathHelper.floor(vec1.x) + 0.5D, (double)MathHelper.floor(vec1.y) + 0.5D, (double)MathHelper.floor(vec1.z) + 0.5D);
        Vec3d vec31 = new Vec3d((double)MathHelper.floor(vec2.x) + 0.5D, (double)MathHelper.floor(vec2.y) + 0.5D, (double)MathHelper.floor(vec2.z) + 0.5D);

        for(var direction : Direction.values()) {
            var vec32 = vec3.offset(direction, (double)MathHelper.EPSILON);
            if (level.raycast(new BlockStateRaycastContext(vec32, vec31, (p_223780_) -> p_223780_.isIn(BlockTags.OCCLUDES_VIBRATION_SIGNALS))).getType() != HitResult.Type.BLOCK) {
                return false;
            }
        }
        return true;
    }

}
