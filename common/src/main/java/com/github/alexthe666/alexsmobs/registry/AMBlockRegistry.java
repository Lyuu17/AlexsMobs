package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.block.BlockReptileEgg;
import com.github.alexthe666.alexsmobs.block.BlockTerrapinEgg;
import com.github.alexthe666.alexsmobs.block.BlockTriopsEggs;
import com.github.alexthe666.alexsmobs.item.AMBlockItem;
import com.github.alexthe666.alexsmobs.item.BlockItemAMRender;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
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
//    public static final RegistrySupplier<Block> BANANA_PEEL = registerBlockAndItem("banana_peel", BlockBananaPeel::new);
//    public static final RegistrySupplier<Block> HUMMINGBIRD_FEEDER = registerBlockAndItem("hummingbird_feeder", BlockHummingbirdFeeder::new);
    public static final RegistrySupplier<Block> CROCODILE_EGG = registerBlockAndItem("crocodile_egg", () -> new BlockReptileEgg<>(AMEntityRegistry.CROCODILE));
//    public static final RegistrySupplier<Block> GUSTMAKER = registerBlockAndItem("gustmaker", BlockGustmaker::new);
//    public static final RegistrySupplier<Block> STRADDLITE_BLOCK = registerBlockAndItem("straddlite_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(1.0F, 1200.0F).sound(SoundType.ANCIENT_DEBRIS)), new Item.Properties().fireResistant(), false);
    public static final RegistrySupplier<Block> PLATYPUS_EGG = registerBlockAndItem("platypus_egg", () -> new BlockReptileEgg<>(AMEntityRegistry.PLATYPUS));
//    public static final RegistrySupplier<Block> LEAFCUTTER_ANTHILL = registerBlockAndItem("leafcutter_anthill", BlockLeafcutterAnthill::new);
//    public static final RegistrySupplier<Block> LEAFCUTTER_ANT_CHAMBER = registerBlockAndItem("leafcutter_ant_chamber", BlockLeafcutterAntChamber::new);
//    public static final RegistrySupplier<Block> CAPSID = registerBlockAndItem("capsid", BlockCapsid::new);
//    public static final RegistrySupplier<Block> VOID_WORM_BEAK = registerBlockAndItem("void_worm_beak", BlockVoidWormBeak::new);
//    public static final RegistrySupplier<Block> VOID_WORM_EFFIGY = registerBlockAndItem("void_worm_effigy", BlockVoidWormEffigy::new);
    public static final RegistrySupplier<Block> TERRAPIN_EGG = registerBlockAndItem("terrapin_egg", BlockTerrapinEgg::new);
//    public static final RegistrySupplier<Block> RAINBOW_GLASS = registerBlockAndItem("rainbow_glass", BlockRainbowGlass::new);
//    public static final RegistrySupplier<Block> BISON_FUR_BLOCK = registerBlockAndItem("bison_fur_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.6F, 1.0F).sound(SoundType.WOOL)));
//    public static final RegistrySupplier<Block> BISON_CARPET = registerBlockAndItem("bison_carpet", BlockBisonCarpet::new);
//    public static final RegistrySupplier<Block> SAND_CIRCLE = registerBlockAndItem("sand_circle", () -> new SandBlock(14406560, BlockBehaviour.Properties.copy(Blocks.SAND)), new Item.Properties(), false);
//    public static final RegistrySupplier<Block> RED_SAND_CIRCLE = registerBlockAndItem("red_sand_circle", () -> new SandBlock(11098145, BlockBehaviour.Properties.copy(Blocks.RED_SAND)), new Item.Properties(), false);
//    public static final RegistrySupplier<Block> ENDER_RESIDUE = registerBlockAndItem("ender_residue", BlockEnderResidue::new);
//    public static final RegistrySupplier<Block> TRANSMUTATION_TABLE = registerBlockAndItem("transmutation_table", BlockTransmutationTable::new, new Item.Properties().rarity(Rarity.EPIC).fireResistant(), true);
//    public static final RegistrySupplier<Block> SCULK_BOOMER = registerBlockAndItem("sculk_boomer", BlockSculkBoomer::new);
//    public static final RegistrySupplier<Block> SKUNK_SPRAY = DEF_REG.register("skunk_spray", BlockSkunkSpray::new);
//    public static final RegistrySupplier<Block> BANANA_SLUG_SLIME_BLOCK = registerBlockAndItem("banana_slug_slime_block", BlockBananaSlugSlime::new);
//    public static final RegistrySupplier<Block> CRYSTALIZED_BANANA_SLUG_MUCUS = registerBlockAndItem("crystalized_banana_slug_mucus", BlockCrystalizedMucus::new);
    public static final RegistrySupplier<Block> CAIMAN_EGG = registerBlockAndItem("caiman_egg", () -> new BlockReptileEgg<>(AMEntityRegistry.CAIMAN));
    public static final RegistrySupplier<Block> TRIOPS_EGGS = registerBlockAndItem("triops_eggs", BlockTriopsEggs::new);
//    /*
//        public static final RegistrySupplier<Block> PURPUR_PLANKS = registerBlockAndItem("purpur_planks", () -> new Block(PURPUR_PLANKS_PROPERTIES));;
//    public static final RegistrySupplier<Block> PURPUR_PLANKS_STAIRS = registerBlockAndItem("purpur_planks_stairs", () -> new StairBlock(PURPUR_PLANKS.get().defaultBlockState(), PURPUR_PLANKS_PROPERTIES));;
//    public static final RegistrySupplier<Block> PURPUR_PLANKS_SLAB = registerBlockAndItem("purpur_planks_slab", () -> new SlabBlock(PURPUR_PLANKS_PROPERTIES));;
//    public static final RegistrySupplier<Block> PURPUR_PLANKS_WALL = registerBlockAndItem("purpur_planks_wall", () -> new WallBlock(PURPUR_PLANKS_PROPERTIES));;
//    public static final RegistrySupplier<Block> END_PIRATE_DOOR = registerBlockAndItem("end_pirate_door", () -> new BlockEndPirateDoor());
//    public static final RegistrySupplier<Block> END_PIRATE_TRAPDOOR = registerBlockAndItem("end_pirate_trapdoor", () -> new TrapDoorBlock(BlockBehaviour.Properties.of(Material.GLASS, MaterialColor.TERRACOTTA_PURPLE).lightLevel((state) -> 3).strength(3.0F).sound(SoundType.GLASS).noOcclusion()));;
//    public static final RegistrySupplier<Block> END_PIRATE_ANCHOR = registerBlockAndItem("end_pirate_anchor", () -> new BlockEndPirateAnchor());
//    public static final RegistrySupplier<Block> END_PIRATE_ANCHOR_WINCH = registerBlockAndItem("end_pirate_anchor_winch", () -> new BlockEndPirateAnchorWinch());
//    public static final RegistrySupplier<Block> END_PIRATE_SHIP_WHEEL = registerBlockAndItem("end_pirate_ship_wheel", () -> new BlockEndPirateShipWheel());
//    public static final RegistrySupplier<Block> END_PIRATE_FLAG = registerBlockAndItem("end_pirate_flag", () -> new BlockEndPirateFlag());
//    public static final RegistrySupplier<Block> PHANTOM_SAIL = registerBlockAndItem("phantom_sail", () -> new BlockEndPirateSail(false));
//    public static final RegistrySupplier<Block> SPECTRE_SAIL = registerBlockAndItem("spectre_sail", () -> new BlockEndPirateSail(true));

    public static RegistrySupplier<Block> registerBlockAndItem(String name, Supplier<Block> block){
        return registerBlockAndItem(name, block, new Item.Settings(), false);
    }

    public static RegistrySupplier<Block> registerBlockAndItem(String name, Supplier<Block> block, Item.Settings blockItemProps, boolean specialRender){
        RegistrySupplier<Block> blockObj = DEF_REG.register(name, block);
        AMItemRegistry.DEF_REG.register(name, () -> specialRender ?  new BlockItemAMRender(blockObj, blockItemProps) :  new AMBlockItem(blockObj, blockItemProps));
        return blockObj;
    }
}
