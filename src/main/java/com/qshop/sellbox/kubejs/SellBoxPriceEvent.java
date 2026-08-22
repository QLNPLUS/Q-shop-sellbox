package com.qshop.sellbox.kubejs;

import net.minecraft.world.item.ItemStack;

/** Event object passed to a KubeJS dynamic sell-price function. */
public final class SellBoxPriceEvent {
    private final ItemStack item;

    public SellBoxPriceEvent(ItemStack item) {
        this.item = item.copy();
    }

    public ItemStack getItem() {
        return item.copy();
    }
}
