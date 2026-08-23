package com.qshop.sellbox.kubejs;

import dev.latvian.mods.rhino.Context;
import net.minecraft.world.item.ItemStack;

/** Event object passed to a KubeJS dynamic sell-price function. */
public final class SellBoxPriceEvent {
    private final ItemStack item;
    private final Context context;

    public SellBoxPriceEvent(ItemStack item) {
        this(item, Context.enter());
    }

    public SellBoxPriceEvent(ItemStack item, Context context) {
        this.item = item.copy();
        this.context = context;
    }

    public SellBoxItemView getItem() {
        return new SellBoxItemView(item, context);
    }
}
