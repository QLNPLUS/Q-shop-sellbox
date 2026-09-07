package com.qshop.sellbox.kubejs;

import com.qshop.sellbox.SellBoxSaleEvents;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;

/** Optional KubeJS bridge. It is only loaded when KubeJS discovers kubejs.plugins.txt. */
public final class SellBoxKubeJSPlugin extends KubeJSPlugin {
    public static final EventGroup SELL_BOX_EVENTS = EventGroup.of("SellBoxEvents");
    public static final EventHandler AFTER_SELL =
            SELL_BOX_EVENTS.server("afterSell", () -> SellBoxAfterSellEvent.class);

    @Override
    public void init() {
        SellBoxSaleEvents.afterSell = (item, player, level, itemPrice, currency) ->
                AFTER_SELL.post(new SellBoxAfterSellEvent(item, player, level, itemPrice, currency));
    }

    @Override
    public void registerEvents() {
        SELL_BOX_EVENTS.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("SellBox", SellBoxApi.INSTANCE);
    }

    @Override
    public void registerClasses(ScriptType type, ClassFilter filter) {
        filter.allow("com.qshop.sellbox.kubejs.SellBoxApi");
        filter.allow("com.qshop.sellbox.kubejs.PriceBuilder");
        filter.allow("com.qshop.sellbox.kubejs.SellBoxPriceEvent");
        filter.allow("com.qshop.sellbox.kubejs.SellBoxItemView");
        filter.allow("com.qshop.sellbox.kubejs.SellBoxAfterSellEvent");
    }
}
