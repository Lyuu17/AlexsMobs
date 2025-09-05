package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.IFalconry;
import com.github.alexthe666.alexsmobs.packet.SyncEntityPosPacket;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.google.common.base.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemFalconryGlove extends Item implements ILeftClick {

    public ItemFalconryGlove(Item.Settings properties) {
        super(properties);
    }

//    @Override
//    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
//        consumer.accept((IClientItemExtensions) AlexsMobs.PROXY.getISTERProperties());
//    }

    public boolean onLeftClick(ItemStack stack, LivingEntity playerIn) {
        if(stack.getItem() == AMItemRegistry.FALCONRY_GLOVE.get()){
            final float dist = 128;
            var Vector3d = playerIn.getCameraPosVec(1.0F);
            var Vector3d1 = playerIn.getRotationVec(1.0F);
            final double vector3d1xDist = Vector3d1.x * dist;
            final double vector3d1yDist = Vector3d1.y * dist;
            final double vector3d1zDist = Vector3d1.z * dist;
            var Vector3d2 = Vector3d.add(vector3d1xDist, vector3d1yDist, vector3d1zDist);
            double d1 = dist;
            Entity pointedEntity = null;
            List<Entity> list = playerIn.getWorld().getOtherEntities(playerIn, playerIn.getBoundingBox().stretch(vector3d1xDist, vector3d1yDist, vector3d1zDist).expand(1.0D, 1.0D, 1.0D), new Predicate<Entity>() {
                @Override
                public boolean apply(@Nullable Entity entity) {
                    return entity != null && entity.canHit() && entity instanceof LivingEntity;
                }
            });
            for (var entity1 : list) {
                var axisalignedbb = entity1.getBoundingBox().expand(entity1.getTargetingMargin());
                var optional = axisalignedbb.raycast(Vector3d, Vector3d2);

                if (axisalignedbb.contains(Vector3d)) {
                    if (d1 >= 0.0D) {
                        //pointedEntity = entity1;
                        d1 = 0.0D;
                    }
                } else if (optional.isPresent()) {
                    double d3 = Vector3d.distanceTo(optional.get());

                    if (d3 < d1 || d1 == 0.0D) {
                        if (entity1.getRootVehicle() == playerIn.getRootVehicle() /* FIXME forge && !playerIn.canRiderInteract() */) {
                            if (d1 == 0.0D) {
                                pointedEntity = entity1;
                            }
                        } else {
                            pointedEntity = entity1;
                            d1 = d3;
                        }
                    }
                }
            }

            if(!playerIn.getPassengerList().isEmpty()){
                for(Entity entity : playerIn.getPassengerList()){
                    if(entity instanceof IFalconry && entity instanceof AnimalEntity animal){
                        IFalconry falcon = (IFalconry)entity;
                        animal.dismountVehicle();
                        animal.refreshPositionAndAngles(playerIn.getX(), playerIn.getEyeY(), playerIn.getZ(), animal.getYaw(), animal.getPitch());
                        if(animal.getWorld().isClient){
                            AlexsMobs.sendMSGToServer(new SyncEntityPosPacket(animal.getId(), playerIn.getX(), playerIn.getEyeY(), playerIn.getZ()));
                        }else{
                            AlexsMobs.sendMSGToAll(new SyncEntityPosPacket(animal.getId(), playerIn.getX(), playerIn.getEyeY(), playerIn.getZ()));
                        }
                        if(playerIn instanceof PlayerEntity playerEntity){
                            falcon.onLaunch(playerEntity, pointedEntity);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
