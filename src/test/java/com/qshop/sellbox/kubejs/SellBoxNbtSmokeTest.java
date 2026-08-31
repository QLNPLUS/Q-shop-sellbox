package com.qshop.sellbox.kubejs;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContextFactory;
import dev.latvian.mods.rhino.Function;
import dev.latvian.mods.rhino.NativeObject;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import net.minecraft.nbt.CompoundTag;

/** Small runtime check for the public event.item.nbt JavaScript contract. */
public final class SellBoxNbtSmokeTest {
    private SellBoxNbtSmokeTest() {}

    public static void main(String[] args) {
        Context context = new ContextFactory().enter();
        Scriptable scope = context.initStandardObjects();

        CompoundTag tag = new CompoundTag();
        tag.putString("rarity", "rare");
        tag.putInt("Damage", 7);
        CompoundTag nested = new CompoundTag();
        nested.putInt("value", 3);
        tag.put("nested", nested);

        NativeObject item = new NativeObject(context.factory);
        item.put(context, "nbt", item, SellBoxItemView.toJsNbt(context, tag));
        NativeObject event = new NativeObject(context.factory);
        event.put(context, "item", event, item);
        Object script = context.evaluateString(scope,
                "(function(event) {"
                        + "let nbt = event.item.nbt;"
                        + "if (!nbt || nbt.rarity != 'rare' || nbt.Damage != 7) return 0;"
                        + "if (!nbt.nested || nbt.nested.value != 3) return 0;"
                        + "return 1000 * 1.2 * 1.2;"
                        + "})",
                "sellbox-nbt-smoke-test", 1, null);
        Object result = ((Function) script).call(context, scope, scope, new Object[]{event});
        if (!(result instanceof Number number) || Math.abs(number.doubleValue() - 1440D) > 0.000001D) {
            throw new AssertionError("event.item.nbt was not readable by the script: " + result);
        }

        Object jsItem = ScriptableObject.getProperty(event, "item", context);
        Object jsNbt = ScriptableObject.getProperty((Scriptable) jsItem, "nbt", context);
        System.out.println("SellBox NBT smoke test passed: " + jsNbt);
    }
}
