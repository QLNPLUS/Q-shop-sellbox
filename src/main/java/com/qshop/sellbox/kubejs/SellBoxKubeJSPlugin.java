package com.qshop.sellbox.kubejs;

import com.qshop.sellbox.SellBoxSaleEvents;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.plugin.ClassFilter;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

/** Optional KubeJS bridge. It is only loaded when KubeJS discovers kubejs.plugins.txt. */
public final class SellBoxKubeJSPlugin implements KubeJSPlugin {
    public static final EventGroup SELL_BOX_EVENTS = EventGroup.of("SellBoxEvents");
    public static final EventHandler AFTER_SELL =
            SELL_BOX_EVENTS.server("afterSell", () -> SellBoxAfterSellEvent.class);

    @Override
    public void init() {
        SellBoxSaleEvents.afterSell = (item, player, level, itemPrice, currency) ->
                AFTER_SELL.post(new SellBoxAfterSellEvent(item, player, level, itemPrice, currency));
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(SELL_BOX_EVENTS);
    }

    @Override
    public void registerBindings(BindingRegistry registry) {
        registry.add("SellBox", SellBoxApi.INSTANCE);
    }

    @Override
    public void registerClasses(ClassFilter filter) {
        filter.allow("com.qshop.sellbox.kubejs.SellBoxApi");
        filter.allow("com.qshop.sellbox.kubejs.PriceBuilder");
        filter.allow("com.qshop.sellbox.kubejs.SellBoxPriceEvent");
        filter.allow("com.qshop.sellbox.kubejs.SellBoxItemView");
        filter.allow("com.qshop.sellbox.kubejs.SellBoxAfterSellEvent");
    }
}
