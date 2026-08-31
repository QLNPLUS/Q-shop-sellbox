package com.qshop.sellbox.kubejs;

import dev.latvian.mods.kubejs.plugin.ClassFilter;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

/** Optional KubeJS bridge. It is only loaded when KubeJS discovers kubejs.plugins.txt. */
public final class SellBoxKubeJSPlugin implements KubeJSPlugin {
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
    }
}
