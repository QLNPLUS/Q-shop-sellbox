package com.qshop.sellbox;

import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface DynamicPriceResolver {
    PriceQuote resolve(ItemStack stack);
}
