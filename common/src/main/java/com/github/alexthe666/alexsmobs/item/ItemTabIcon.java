package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.client.render.AMMobIcons;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderLaviathan;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderMurmurBody;
import com.github.alexthe666.alexsmobs.client.render.entity.RenderUnderminer;
import com.github.alexthe666.alexsmobs.client.render.item.AMItemstackRenderer;
import com.github.alexthe666.alexsmobs.entity.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemTabIcon extends ItemInventoryOnly implements IItemRender {

    private final Map<String, Entity> renderedEntites = new HashMap<>();
    private final List<EntityType<?>> blockedRenderEntities = new ArrayList<>();

    public ItemTabIcon(Item.Settings properties) {
        super(properties);
    }

    public static boolean hasCustomEntityDisplay(ItemStack stack){
        return stack.getNbt() != null && stack.getNbt().contains("DisplayEntityType");
    }

    public static String getCustomDisplayEntityString(ItemStack stack){
        return stack.getNbt().getString("DisplayEntityType");
    }

    @Nullable
    public static EntityType<?> getEntityType(@Nullable NbtCompound tag) {
        if (tag != null && tag.contains("DisplayEntityType")) {
            String entityType = tag.getString("DisplayEntityType");
           return Registries.ENTITY_TYPE.get(Identifier.tryParse(entityType));
        }
        return null;
    }

    private static float getScaleFor(EntityType<?> type, List<Pair<EntityType<?>, Float>> mobIcons) {
        for (var pair : mobIcons) {
            if (pair.getFirst() == type) {
                return pair.getSecond();
            }
        }
        return 1.0F;
    }

    private static void drawEntityOnScreen(MatrixStack matrixstack, int posX, int posY, float scale, boolean follow, double xRot, double yRot, double zRot, float mouseX, float mouseY, Entity entity) {
        float f = (float) Math.atan(-mouseX / 40.0F);
        float f1 = (float) Math.atan(mouseY / 40.0F);
        matrixstack.scale(scale, scale, scale);
        entity.setOnGround(false);
        float partialTicks = MinecraftClient.getInstance().getTickDelta();
        Quaternionf quaternion = RotationAxis.POSITIVE_Z.rotationDegrees(180.0F);
        Quaternionf quaternion1 = RotationAxis.POSITIVE_X.rotationDegrees(20.0F);
        float partialTicksForRender = MinecraftClient.getInstance().isPaused() || entity instanceof EntityMimicOctopus ? 0 : partialTicks;
        int tick = AMItemstackRenderer.getTicks();
        if (follow) {
            float yaw = f * 45.0F;
            entity.setYaw(yaw);
            entity.age = tick;
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).bodyYaw = yaw;
                ((LivingEntity) entity).prevBodyYaw = yaw;
                ((LivingEntity) entity).headYaw= yaw;
                ((LivingEntity) entity).prevHeadYaw = yaw;
            }

            quaternion1 = RotationAxis.POSITIVE_X.rotationDegrees(f1 * 20.0F);
            quaternion.mul(quaternion1);
        }

        matrixstack.multiply(quaternion);
        matrixstack.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) (-xRot)));
        matrixstack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) yRot));
        matrixstack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) zRot));
        EntityRenderDispatcher entityrenderdispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        quaternion1.conjugate();
        entityrenderdispatcher.setRotation(quaternion1);
        entityrenderdispatcher.setRenderShadows(false);
        var VertexConsumerProvider$buffersource = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        RenderSystem.runAsFancy(() -> {
            entityrenderdispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, partialTicksForRender, matrixstack, VertexConsumerProvider$buffersource, 15728880);
        });
        VertexConsumerProvider$buffersource.draw();
        entityrenderdispatcher.setRenderShadows(true);
        entity.setYaw(0.0F);
        entity.setPitch(0.0F);
        if (entity instanceof LivingEntity) {
            ((LivingEntity) entity).bodyYaw = 0.0F;
            ((LivingEntity) entity).prevHeadYaw = 0.0F;
            ((LivingEntity) entity).headYaw= 0.0F;
        }
        RenderSystem.applyModelViewMatrix();
        DiffuseLighting.enableGuiDepthLighting();
    }

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        var level = MinecraftClient.getInstance().world;

        Entity fakeEntity = null;
        var mobIcons = AMMobIcons.getMobIcons();
        int entityIndex = (AMItemstackRenderer.getTicks()/ 40) % (mobIcons.size());
        float scale = 1.0F;
        int flags = 0;
        if (level != null) {
            if (ItemTabIcon.hasCustomEntityDisplay(stack)) {
                flags = stack.getNbt().getInt("DisplayMobFlags");
                String index = ItemTabIcon.getCustomDisplayEntityString(stack);
                var local = ItemTabIcon.getEntityType(stack.getNbt());
                scale = getScaleFor(local, mobIcons);
                if (stack.getNbt().getFloat("DisplayMobScale") > 0) {
                    scale = stack.getNbt().getFloat("DisplayMobScale");
                }
                if (this.renderedEntites.get(index) == null && !blockedRenderEntities.contains(local)) {
                    try {
                        var entity = local.create(level);
                        if (entity instanceof EntityBlobfish) {
                            ((EntityBlobfish) entity).setDepressurized(true);
                        }
                        this.renderedEntites.put(local.getTranslationKey(), entity);
                        fakeEntity = entity;
                    } catch (Exception e) {
                        blockedRenderEntities.add(local);
                        AlexsMobs.LOGGER.error("Could not render item for entity: " + local);
                    }
                } else {
                    fakeEntity = this.renderedEntites.get(local.getTranslationKey());
                }
            } else {
                var type = mobIcons.get(entityIndex).getFirst();
                scale = mobIcons.get(entityIndex).getSecond();
                if (type != null) {
                    if (this.renderedEntites.get(type.getTranslationKey()) == null && !blockedRenderEntities.contains(type)) {
                        try {
                            Entity entity = type.create(level);
                            if (entity instanceof EntityBlobfish) {
                                ((EntityBlobfish) entity).setDepressurized(true);
                            }
                            this.renderedEntites.put(type.getTranslationKey(), entity);
                            fakeEntity = entity;
                        } catch (Exception e) {
                            blockedRenderEntities.add(type);
                            AlexsMobs.LOGGER.error("Could not render item for entity: " + type);
                        }
                    } else {
                        fakeEntity = this.renderedEntites.get(type.getTranslationKey());
                    }
                }
            }
        }

        if (fakeEntity instanceof EntityCockroach) {
            if (flags == 99) {
                matrices.translate(0, 0.25F, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80));
                ((EntityCockroach) fakeEntity).setMaracas(true);
            } else {
                ((EntityCockroach) fakeEntity).setMaracas(false);
            }
        }
        if (fakeEntity instanceof EntityElephant) {
            if (flags == 99) {
                ((EntityElephant) fakeEntity).setTusked(true);
                ((EntityElephant) fakeEntity).setColor(null);
            } else if (flags == 98) {
                ((EntityElephant) fakeEntity).setTusked(false);
                ((EntityElephant) fakeEntity).setColor(DyeColor.BROWN);
            } else {
                ((EntityElephant) fakeEntity).setTusked(false);
                ((EntityElephant) fakeEntity).setColor(null);
            }
        }
        if (fakeEntity instanceof EntityBaldEagle) {
            if (flags == 98) {
                ((EntityBaldEagle) fakeEntity).setCap(true);
            } else {
                ((EntityBaldEagle) fakeEntity).setCap(false);
            }
        }
        if (fakeEntity instanceof EntityVoidWorm) {
            matrices.translate(0, 0.5F, 0);
        }
        if (fakeEntity instanceof EntityMimicOctopus) {
            matrices.translate(0, 0.5F, 0);
        }
        if (fakeEntity instanceof EntityLaviathan) {
            RenderLaviathan.renderWithoutShaking = true;
            matrices.translate(0, 0.3F, 0);
        }
        if (fakeEntity instanceof EntityCosmaw) {
            matrices.translate(0, 0.2F, 0);
        }
        if (fakeEntity instanceof EntityGiantSquid) {
            matrices.translate(0, 0.5F, 0.3F);
        }
        if (fakeEntity instanceof EntityUnderminer) {
            RenderUnderminer.renderWithPickaxe = true;
        }
        if (fakeEntity instanceof EntityMurmur) {
            RenderMurmurBody.renderWithHead = true;
            matrices.translate(0, -0.2F, 0);
        }
        if (fakeEntity != null) {
            var mouseHelper = MinecraftClient.getInstance().mouse;
            double mouseX = (mouseHelper.getX() * (double) MinecraftClient.getInstance().getWindow().getScaledWidth()) / (double) MinecraftClient.getInstance().getWindow().getWidth();
            double mouseY = mouseHelper.getY() * (double) MinecraftClient.getInstance().getWindow().getScaledHeight() / (double) MinecraftClient.getInstance().getWindow().getHeight();
            matrices.translate(0.5F, 0F, 0);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));
            if (mode != ModelTransformationMode.GUI) {
                mouseX = 0;
                mouseY = 0;
            }
            try {
                drawEntityOnScreen(matrices, 0, 0, scale, true, 0, -45, 0, (float) mouseX, (float) mouseY, fakeEntity);
            } catch (Exception ignored) {
            }
        }
        if (fakeEntity instanceof EntityLaviathan) {
            RenderLaviathan.renderWithoutShaking = false;
        }
        if (fakeEntity instanceof EntityUnderminer) {
            RenderUnderminer.renderWithPickaxe = false;
        }
        if (fakeEntity instanceof EntityMurmur) {
            RenderMurmurBody.renderWithHead = false;
        }
    }
}
