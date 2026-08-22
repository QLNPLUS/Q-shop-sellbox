package com.qshop.sellbox.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;

/** Optional KubeJS bridge. It is only loaded when KubeJS discovers kubejs.plugins.txt. */
public final class SellBoxKubeJSPlugin extends KubeJSPlugin {
    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("SellBox", SellBoxApi.INSTANCE);
    }

    @Override
    public void registerClasses(ScriptType type, ClassFilter filter) {
        filter.allow("com.qshop.sellbox.kubejs.SellBoxApi");
        filter.allow("com.qshop.sellbox.kubejs.PriceBuilder");
        filter.allow("com.qshop.sellbox.kubejs.SellBoxPriceEvent");
    }
}
