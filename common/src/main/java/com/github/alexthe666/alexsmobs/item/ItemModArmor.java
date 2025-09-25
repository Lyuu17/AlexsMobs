package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.registry.AMItemRegistry;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class ItemModArmor extends ArmorItem {
    private static final UUID[] ARMOR_MODIFIERS = new UUID[]{UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150"), UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"), UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"), UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B")};
    private Multimap<EntityAttribute, EntityAttributeModifier> attributeMapCroc;
    private Multimap<EntityAttribute, EntityAttributeModifier> attributeMapMoose;
    private Multimap<EntityAttribute, EntityAttributeModifier> attributeMapFlyingFish;
    private Multimap<EntityAttribute, EntityAttributeModifier> attributeMapKimono;

    public ItemModArmor(AMArmorMaterial armorMaterial, ArmorItem.Type slot) {
        super(armorMaterial, slot, new Item.Settings());
    }

    // FIXME forge
//    @Override
//    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
//        consumer.accept((IClientItemExtensions) AlexsMobs.PROXY.getArmorRenderProperties());
//    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World worldIn, List<Text> tooltip, TooltipContext flagIn) {
        if (this.material == AMItemRegistry.CENTIPEDE_ARMOR_MATERIAL) {
            tooltip.add(Text.translatable("item.alexsmobs.centipede_leggings.desc").formatted(Formatting.GRAY));
        }
        if (this.material == AMItemRegistry.EMU_ARMOR_MATERIAL) {
            tooltip.add(Text.translatable("item.alexsmobs.emu_leggings.desc").formatted(Formatting.GRAY));
        }
        super.appendTooltip(stack, worldIn, tooltip, flagIn);
        if (this.material == AMItemRegistry.ROADRUNNER_ARMOR_MATERIAL) {
            tooltip.add(Text.translatable("item.alexsmobs.roadrunner_boots.desc").formatted(Formatting.BLUE));
        }
        if (this.material == AMItemRegistry.RACCOON_ARMOR_MATERIAL) {
            tooltip.add(Text.translatable("item.alexsmobs.frontier_cap.desc").formatted(Formatting.BLUE));
        }
        if (this.material == AMItemRegistry.FROSTSTALKER_ARMOR_MATERIAL) {
            tooltip.add(Text.translatable("item.alexsmobs.froststalker_helmet.desc").formatted(Formatting.AQUA));
        }
        if (this.material == AMItemRegistry.ROCKY_ARMOR_MATERIAL) {
            tooltip.add(Text.translatable("item.alexsmobs.rocky_chestplate.desc").formatted(Formatting.GRAY));
        }
        if (this.material == AMItemRegistry.SOMBRERO_ARMOR_MATERIAL && AlexsMobs.isAprilFools()) {
            tooltip.add(Text.translatable("item.alexsmobs.sombrero.special_desc").formatted(Formatting.GRAY));
        }
        if (this.material == AMItemRegistry.FLYING_FISH_MATERIAL) {
            tooltip.add(Text.translatable("item.alexsmobs.flying_fish_boots.desc").formatted(Formatting.GRAY));
        }
        if (this.material == AMItemRegistry.NOVELTY_HAT_MATERIAL) {
            tooltip.add(Text.translatable("item.alexsmobs.novelty_hat.desc").formatted(Formatting.GRAY));
        }
        if (this.material == AMItemRegistry.KIMONO_MATERIAL) {
            tooltip.add(Text.translatable("item.alexsmobs.unsettling_kimono.desc").formatted(Formatting.GRAY));
        }
    }

    private void buildCrocAttributes(AMArmorMaterial materialIn) {
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
        UUID uuid = ARMOR_MODIFIERS[type.ordinal()];
        builder.put(EntityAttributes.GENERIC_ARMOR, new EntityAttributeModifier(uuid, "Armor modifier", materialIn.getProtection(this.type), EntityAttributeModifier.Operation.ADDITION));
        builder.put(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, new EntityAttributeModifier(uuid, "Armor toughness", materialIn.getToughness(), EntityAttributeModifier.Operation.ADDITION));
        // FIXME forge builder.put(ForgeMod.SWIM_SPEED.get(), new EntityAttribute(uuid, "Swim speed", 1, EntityAttribute.Operation.ADDITION));
        if (this.knockbackResistance > 0) {
            builder.put(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, new EntityAttributeModifier(uuid, "Armor knockback resistance", this.knockbackResistance, EntityAttributeModifier.Operation.ADDITION));
        }
        attributeMapCroc = builder.build();
    }

    private void buildFlyingFishAttributes(AMArmorMaterial materialIn) {
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
        UUID uuid = ARMOR_MODIFIERS[type.ordinal()];
        builder.put(EntityAttributes.GENERIC_ARMOR, new EntityAttributeModifier(uuid, "Armor modifier", materialIn.getProtection(this.type), EntityAttributeModifier.Operation.ADDITION));
        builder.put(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, new EntityAttributeModifier(uuid, "Armor toughness", materialIn.getToughness(), EntityAttributeModifier.Operation.ADDITION));
// FIXME forge
//        builder.put(ForgeMod.SWIM_SPEED.get(), new EntityAttributeModifier(uuid, "Swim speed", 0.5, EntityAttributeModifier.Operation.ADDITION));
        attributeMapFlyingFish = builder.build();
    }

    private void buildMooseAttributes(AMArmorMaterial materialIn) {
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
        UUID uuid = ARMOR_MODIFIERS[type.ordinal()];
        builder.put(EntityAttributes.GENERIC_ARMOR, new EntityAttributeModifier(uuid, "Armor modifier", materialIn.getProtection(this.type), EntityAttributeModifier.Operation.ADDITION));
        builder.put(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, new EntityAttributeModifier(uuid, "Armor toughness", materialIn.getToughness(), EntityAttributeModifier.Operation.ADDITION));
        builder.put(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, new EntityAttributeModifier(uuid, "Knockback", 2, EntityAttributeModifier.Operation.ADDITION));
        if (this.knockbackResistance > 0) {
            builder.put(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, new EntityAttributeModifier(uuid, "Armor knockback resistance", this.knockbackResistance, EntityAttributeModifier.Operation.ADDITION));
        }
        attributeMapMoose = builder.build();
    }

    private void buildKimonoAttributes(AMArmorMaterial materialIn) {
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
        UUID uuid = ARMOR_MODIFIERS[type.ordinal()];
        builder.put(EntityAttributes.GENERIC_ARMOR, new EntityAttributeModifier(uuid, "Armor modifier", materialIn.getProtection(this.type), EntityAttributeModifier.Operation.ADDITION));
        builder.put(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, new EntityAttributeModifier(uuid, "Armor toughness", materialIn.getToughness(), EntityAttributeModifier.Operation.ADDITION));
// FIXME forge
//        builder.put(ForgeMod.BLOCK_REACH.get(), new EntityAttributeModifier(uuid, "Block Reach distance", 2, EntityAttributeModifier.Operation.ADDITION));
//        builder.put(ForgeMod.ENTITY_REACH.get(), new EntityAttributeModifier(uuid, "Entity Reach distance", 2, EntityAttributeModifier.Operation.ADDITION));
        attributeMapKimono = builder.build();
    }

    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot equipmentSlot) {
        if (getMaterial() == AMItemRegistry.CROCODILE_ARMOR_MATERIAL && equipmentSlot == this.type.getEquipmentSlot()) {
            if (attributeMapCroc == null) {
                buildCrocAttributes(AMItemRegistry.CROCODILE_ARMOR_MATERIAL);
            }
            return attributeMapCroc;
        }
        if (getMaterial() == AMItemRegistry.MOOSE_ARMOR_MATERIAL && equipmentSlot == this.type.getEquipmentSlot()) {
            if (attributeMapMoose == null) {
                buildMooseAttributes(AMItemRegistry.MOOSE_ARMOR_MATERIAL);
            }
            return attributeMapMoose;
        }
        if (getMaterial() == AMItemRegistry.FLYING_FISH_MATERIAL && equipmentSlot == this.type.getEquipmentSlot()) {
            if (attributeMapFlyingFish == null) {
                buildFlyingFishAttributes(AMItemRegistry.FLYING_FISH_MATERIAL);
            }
            return attributeMapFlyingFish;
        }
        if (getMaterial() == AMItemRegistry.KIMONO_MATERIAL && equipmentSlot == this.type.getEquipmentSlot()) {
            if (attributeMapKimono == null) {
                buildKimonoAttributes(AMItemRegistry.KIMONO_MATERIAL);
            }
            return attributeMapKimono;
        }
        return super.getAttributeModifiers(equipmentSlot);
    }

    // FIXME forge
//    @Nullable
//    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
//        if (this.material == AMItemRegistry.CROCODILE_ARMOR_MATERIAL) {
//            return "alexsmobs:textures/armor/crocodile_layer_1.png";
//        } else if (this.material == AMItemRegistry.ROADRUNNER_ARMOR_MATERIAL) {
//            return "alexsmobs:textures/armor/roadrunner_boots.png";
//        } else if (this.material == AMItemRegistry.CENTIPEDE_ARMOR_MATERIAL) {
//            return "alexsmobs:textures/armor/centipede_layer_2.png";
//        } else if (this.material == AMItemRegistry.MOOSE_ARMOR_MATERIAL) {
//            return "alexsmobs:textures/armor/moose_headgear.png";
//        } else if (this.material == AMItemRegistry.RACCOON_ARMOR_MATERIAL) {
//            return "alexsmobs:textures/armor/frontier_cap.png";
//        } else if (this.material == AMItemRegistry.SOMBRERO_ARMOR_MATERIAL) {
//            return "alexsmobs:textures/armor/sombrero.png";
//        } else if (this.material == AMItemRegistry.SPIKED_TURTLE_SHELL_ARMOR_MATERIAL) {
//            return "alexsmobs:textures/armor/spiked_turtle_shell.png";
//        } else if (this.material == AMItemRegistry.FEDORA_ARMOR_MATERIAL) {
//            return "alexsmobs:textures/armor/fedora.png";
//        } else if (this.material == AMItemRegistry.EMU_ARMOR_MATERIAL) {
//            return "alexsmobs:textures/armor/emu_layer_2.png";
//        } else if (this.material == AMItemRegistry.FROSTSTALKER_ARMOR_MATERIAL) {
//            return "alexsmobs:textures/armor/froststalker_helmet.png";
//        } else if (this.material == AMItemRegistry.ROCKY_ARMOR_MATERIAL) {
//            return "alexsmobs:textures/armor/rocky_chestplate.png";
//        } else if (this.material == AMItemRegistry.FLYING_FISH_MATERIAL) {
//            return "alexsmobs:textures/armor/flying_fish_boots.png";
//        } else if (this.material == AMItemRegistry.NOVELTY_HAT_MATERIAL) {
//            return "alexsmobs:textures/armor/novelty_hat.png";
//        } else if (this.material == AMItemRegistry.KIMONO_MATERIAL) {
//            return "alexsmobs:textures/armor/unsettling_kimono.png";
//        }
//        return super.getArmorTexture(stack, entity, slot, type);
//    }
}
