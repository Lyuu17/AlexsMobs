package com.github.alexthe666.alexsmobs.registry;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import dev.architectury.registry.level.entity.trade.SimpleTrade;
import dev.architectury.registry.level.entity.trade.TradeRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.village.VillagerProfession;

public class AMTradeRegistry {

    public static void init() {

        TradeRegistry.registerVillagerTrade(VillagerProfession.FISHERMAN, 2, new SimpleTrade(AMItemRegistry.AMBERGRIS.get().getDefaultStack(), ItemStack.EMPTY, new ItemStack(AMItemRegistry.AMBERGRIS.get()), 1, 10, 1));

        if (AMConfig.wanderingTraderOffers) {
            TradeRegistry.registerTradeForWanderingTrader(false,
                    new SimpleTrade(
                            new ItemStack(Items.EMERALD, 4), ItemStack.EMPTY,
                            new ItemStack(AMItemRegistry.ANIMAL_DICTIONARY.get()),
                            1, 2, 1f
                    ),
                    new SimpleTrade(
                            new ItemStack(Items.EMERALD, 3), ItemStack.EMPTY,
                            new ItemStack(AMItemRegistry.ACACIA_BLOSSOM.get(), 2),
                            2, 2, 1f
                    ),
                    new SimpleTrade(
                            new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY,
                            new ItemStack(AMItemRegistry.BEAR_FUR.get()),
                            1, 2, 1f
                    ),
                    new SimpleTrade(
                            new ItemStack(Items.EMERALD, 5), ItemStack.EMPTY,
                            new ItemStack(AMItemRegistry.CROCODILE_SCUTE.get()),
                            1, 2, 1f
                    ),
                    new SimpleTrade(
                            new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY,
                            new ItemStack(AMItemRegistry.ROADRUNNER_FEATHER.get(), 2),
                            2, 2, 2f
                    ),
                    new SimpleTrade(
                            new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY,
                            new ItemStack(AMItemRegistry.MOSQUITO_LARVA.get(), 3),
                            5, 1, 1f
                    )
            );

            if (AMConfig.cockroachSpawnWeight > 0) {
                TradeRegistry.registerTradeForWanderingTrader(false,
                        new SimpleTrade(
                                new ItemStack(Items.EMERALD, 2), ItemStack.EMPTY,
                                new ItemStack(AMItemRegistry.COCKROACH_OOTHECA.get()),
                                1, 2, 1f
                        )
                );
            }

            if (AMConfig.blobfishSpawnWeight > 0) {
                TradeRegistry.registerTradeForWanderingTrader(false,
                        new SimpleTrade(
                                new ItemStack(Items.EMERALD, 4), ItemStack.EMPTY,
                                new ItemStack(AMItemRegistry.BLOBFISH_BUCKET.get()),
                                1, 3, 1f
                        )
                );
            }

            if (AMConfig.crocodileSpawnWeight > 0) {
                TradeRegistry.registerTradeForWanderingTrader(false,
                        new SimpleTrade(
                                new ItemStack(Items.EMERALD, 6), ItemStack.EMPTY,
                                new ItemStack(AMBlockRegistry.CROCODILE_EGG.get().asItem()),
                                1, 2, 1f
                        )
                );
            }

            TradeRegistry.registerTradeForWanderingTrader(true,
                    new SimpleTrade(
                            new ItemStack(Items.EMERALD, 20), ItemStack.EMPTY,
                            new ItemStack(AMItemRegistry.SOMBRERO.get()),
                            1, 1, 1f
                    ),
                    new SimpleTrade(
                            new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY,
                            new ItemStack(AMBlockRegistry.BANANA_PEEL.get(), 2),
                            1, 1, 1f
                    ),
                    new SimpleTrade(
                            new ItemStack(Items.EMERALD, 5), ItemStack.EMPTY,
                            new ItemStack(AMItemRegistry.BLOOD_SAC.get(), 2),
                            3, 1, 1f
                    )
            );
        }
    }
}
