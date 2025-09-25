package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.block.*;
import com.github.alexthe666.alexsmobs.item.AMBlockItem;
import com.github.alexthe666.alexsmobs.platform.PlatformRegisterItemRenderer;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.*;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;

import java.util.function.Supplier;

public class AMBlockRegistry {
    public static final AbstractBlock.Settings PURPUR_PLANKS_PROPERTIES = AbstractBlock.Settings.create()
            .mapColor(MapColor.PINK)
            .strength(0.5F, 1.0F)
            .sounds(BlockSoundGroup.WOOD);

    public static final DeferredRegister<Block> DEF_REG = DeferredRegister.create(AlexsMobs.MOD_ID, RegistryKeys.BLOCK);
    public static final RegistrySupplier<Block> BANANA_PEEL = registerBlockAndItem("banana_peel", BananaPeelBlock::new);
    public static final RegistrySupplier<Block> HUMMINGBIRD_FEEDER = registerBlockAndItem("hummingbird_feeder", HummingbirdFeederBlock::new);
    public static final RegistrySupplier<Block> CROCODILE_EGG = registerBlockAndItem("crocodile_egg", () -> new ReptileEggBlock<>(AMEntityRegistry.CROCODILE));
    public static final RegistrySupplier<Block> GUSTMAKER = registerBlockAndItem("gustmaker", GustmakerBlock::new);
    public static final RegistrySupplier<Block> STRADDLITE_BLOCK = registerBlockAndItem("straddlite_block", () -> new Block(AbstractBlock.Settings.create().mapColor(MapColor.IRON_GRAY).requiresTool().strength(1.0F, 1200.0F).sounds(BlockSoundGroup.ANCIENT_DEBRIS)), new Item.Settings().fireproof(), false);
    public static final RegistrySupplier<Block> PLATYPUS_EGG = registerBlockAndItem("platypus_egg", () -> new ReptileEggBlock<>(AMEntityRegistry.PLATYPUS));
    public static final RegistrySupplier<Block> LEAFCUTTER_ANTHILL = registerBlockAndItem("leafcutter_anthill", LeafcutterAnthillBlock::new);
    public static final RegistrySupplier<Block> LEAFCUTTER_ANT_CHAMBER = registerBlockAndItem("leafcutter_ant_chamber", LeafcutterAntChamberBlock::new);
    public static final RegistrySupplier<Block> CAPSID = registerBlockAndItem("capsid", CapsidBlock::new);
    public static final RegistrySupplier<Block> VOID_WORM_BEAK = registerBlockAndItem("void_worm_beak", VoidWormBeakBlock::new);
    public static final RegistrySupplier<Block> VOID_WORM_EFFIGY = registerBlockAndItem("void_worm_effigy", VoidWormEffigyBlock::new);
    public static final RegistrySupplier<Block> TERRAPIN_EGG = registerBlockAndItem("terrapin_egg", TerrapinEggBlock::new);
    public static final RegistrySupplier<Block> RAINBOW_GLASS = registerBlockAndItem("rainbow_glass", RainbowGlassBlock::new);
    public static final RegistrySupplier<Block> BISON_FUR_BLOCK = registerBlockAndItem("bison_fur_block", () -> new Block(AbstractBlock.Settings.create().mapColor(MapColor.BROWN).strength(0.6F, 1.0F).sounds(BlockSoundGroup.WOOL)));
    public static final RegistrySupplier<Block> BISON_CARPET = registerBlockAndItem("bison_carpet", BisonCarpetBlock::new);
    public static final RegistrySupplier<Block> SAND_CIRCLE = registerBlockAndItem("sand_circle", () -> new SandBlock(14406560, AbstractBlock.Settings.copy(Blocks.SAND)), new Item.Settings(), false);
    public static final RegistrySupplier<Block> RED_SAND_CIRCLE = registerBlockAndItem("red_sand_circle", () -> new SandBlock(11098145, AbstractBlock.Settings.copy(Blocks.RED_SAND)), new Item.Settings(), false);
    public static final RegistrySupplier<Block> ENDER_RESIDUE = registerBlockAndItem("ender_residue", EnderResidueBlock::new);
    public static final RegistrySupplier<Block> TRANSMUTATION_TABLE = registerBlock("transmutation_table", TransmutationTableBlock::new);
    public static final RegistrySupplier<Block> SCULK_BOOMER = registerBlockAndItem("sculk_boomer", SculkBoomerBlock::new);
    public static final RegistrySupplier<Block> SKUNK_SPRAY = DEF_REG.register("skunk_spray", SkunkSprayBlock::new);
    public static final RegistrySupplier<Block> BANANA_SLUG_SLIME_BLOCK = registerBlockAndItem("banana_slug_slime_block", BananaSlugSlimeBlock::new);
    public static final RegistrySupplier<Block> CRYSTALIZED_BANANA_SLUG_MUCUS = registerBlockAndItem("crystalized_banana_slug_mucus", CrystalizedMucusBlock::new);
    public static final RegistrySupplier<Block> CAIMAN_EGG = registerBlockAndItem("caiman_egg", () -> new ReptileEggBlock<>(AMEntityRegistry.CAIMAN));
    public static final RegistrySupplier<Block> TRIOPS_EGGS = registerBlockAndItem("triops_eggs", TriopsEggsBlock::new);
//    /*
//        public static final RegistrySupplier<Block> PURPUR_PLANKS = registerBlockAndItem("purpur_planks", () -> new Block(PURPUR_PLANKS_PROPERTIES));;
//    public static final RegistrySupplier<Block> PURPUR_PLANKS_STAIRS = registerBlockAndItem("purpur_planks_stairs", () -> new StairBlock(PURPUR_PLANKS.get().defaultBlockState(), PURPUR_PLANKS_PROPERTIES));;
//    public static final RegistrySupplier<Block> PURPUR_PLANKS_SLAB = registerBlockAndItem("purpur_planks_slab", () -> new SlabBlock(PURPUR_PLANKS_PROPERTIES));;
//    public static final RegistrySupplier<Block> PURPUR_PLANKS_WALL = registerBlockAndItem("purpur_planks_wall", () -> new WallBlock(PURPUR_PLANKS_PROPERTIES));;
//    public static final RegistrySupplier<Block> END_PIRATE_DOOR = registerBlockAndItem("end_pirate_door", () -> new BlockEndPirateDoor());
//    public static final RegistrySupplier<Block> END_PIRATE_TRAPDOOR = registerBlockAndItem("end_pirate_trapdoor", () -> new TrapDoorBlock(AbstractBlock.Settings.of(Material.GLASS, MaterialColor.TERRACOTTA_PURPLE).lightLevel((state) -> 3).strength(3.0F).sound(SoundType.GLASS).noOcclusion()));;
//    public static final RegistrySupplier<Block> END_PIRATE_ANCHOR = registerBlockAndItem("end_pirate_anchor", () -> new BlockEndPirateAnchor());
//    public static final RegistrySupplier<Block> END_PIRATE_ANCHOR_WINCH = registerBlockAndItem("end_pirate_anchor_winch", () -> new BlockEndPirateAnchorWinch());
//    public static final RegistrySupplier<Block> END_PIRATE_SHIP_WHEEL = registerBlockAndItem("end_pirate_ship_wheel", () -> new BlockEndPirateShipWheel());
//    public static final RegistrySupplier<Block> END_PIRATE_FLAG = registerBlockAndItem("end_pirate_flag", () -> new BlockEndPirateFlag());
//    public static final RegistrySupplier<Block> PHANTOM_SAIL = registerBlockAndItem("phantom_sail", () -> new BlockEndPirateSail(false));
//    public static final RegistrySupplier<Block> SPECTRE_SAIL = registerBlockAndItem("spectre_sail", () -> new BlockEndPirateSail(true));

    public static RegistrySupplier<Block> registerBlock(String name, Supplier<Block> block){
        return DEF_REG.register(name, block);
    }

    public static RegistrySupplier<Block> registerBlockAndItem(String name, Supplier<Block> block){
        return registerBlockAndItem(name, block, new Item.Settings(), false);
    }

    public static RegistrySupplier<Block> registerBlockAndItem(String name, Supplier<Block> block, Item.Settings blockItemProps, boolean specialRender){
        RegistrySupplier<Block> blockObj = registerBlock(name, block);
        AMItemRegistry.DEF_REG.register(name, () -> specialRender
                ? PlatformRegisterItemRenderer.register(blockObj, blockItemProps)
                : new AMBlockItem(blockObj, blockItemProps));
        return blockObj;
    }
}
