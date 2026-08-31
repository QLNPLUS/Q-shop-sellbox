package com.qshop.sellbox.kubejs;

import com.qshop.sellbox.NbtStrings;
import com.qshop.sellbox.PriceQuote;
import com.qshop.sellbox.SellBoxConfig;
import com.qshop.sellbox.SellBoxPrices;
import com.qshop.util.ItemStackData;
import com.google.gson.Gson;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContextFactory;
import dev.latvian.mods.rhino.Function;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import dev.latvian.mods.rhino.Wrapper;
import net.minecraft.world.item.ItemStack;

public final class SellBoxApi {
    public static final SellBoxApi INSTANCE = new SellBoxApi();
    private static final ContextFactory CONTEXT_FACTORY = new ContextFactory();
    private static final Gson GSON = new Gson();

    private SellBoxApi() {}

    /** Sets a KubeJS exact price, overriding the JSON config for this item. */
    public void price(String itemId, double price, String currency) {
        SellBoxPrices.setKubeJsPrice(itemId, new net.minecraft.nbt.CompoundTag(), price, currency);
    }

    public void price(String itemId, double price) {
        price(itemId, price, null);
    }

    /** Sets a KubeJS exact price for an item whose NBT contains the supplied subset. */
    public void price(String itemId, double price, String currency, Object nbt) {
        SellBoxPrices.setKubeJsPrice(itemId, parse(itemId, nbt), price, currency);
    }

    /** Multiplies the resolved base price when an NBT subset matches. */
    public void multiplier(String itemId, Object nbt, double multiplier) {
        SellBoxPrices.setKubeJsMultiplier(itemId, parse(itemId, nbt), multiplier, null);
    }

    /** Applies a multiplier to every item whose NBT contains the supplied subset. */
    public void nbtMultiplier(Object nbt, double multiplier) {
        SellBoxPrices.setKubeJsMultiplier("*", parse("*", nbt), multiplier, null);
    }

    /** Builder shorthand for a rule that applies to every item. */
    public PriceBuilder anyItem() {
        return new PriceBuilder("*");
    }

    public PriceBuilder item(String itemId) {
        return new PriceBuilder(itemId);
    }

    /** Short form: SellBox.price(event => { ... return price }) */
    public void price(Object callback) {
        priceFunction(callback);
    }

    /** Registers a server-side function that returns the final price and currency. */
    public void priceFunction(Object callback) {
        priceFunction(callback, null);
    }

    /** Alias for priceFunction, useful when reading scripts aloud. */
    public void dynamicPrice(Object callback) {
        priceFunction(callback, null);
    }

    public void priceFunction(Object callback, String currency) {
        if (!(callback instanceof Function function)) {
            throw new IllegalArgumentException("SellBox.priceFunction expects a function");
        }
        SellBoxPrices.setKubeJsPriceFunction(stack -> invoke(function, stack, currency));
    }

    private static PriceQuote invoke(Function function, ItemStack stack, String currency) {
        Context context = CONTEXT_FACTORY.enter();
        try {
            Scriptable scope = function.getParentScope();
            SellBoxPriceEvent priceEvent = new SellBoxPriceEvent(stack, context);
            Object event = context.javaToJS(priceEvent, scope);
            debugInput(context, event, stack);
            Object result = function.call(context, scope, scope, new Object[]{event});
            result = Wrapper.unwrapped(result);

            double price;
            String resolvedCurrencyId = currency;
            if (result instanceof Number number) {
                // Numeric returns remain valid and use the configured default currency.
                price = number.doubleValue();
            } else if (result instanceof Scriptable object) {
                Object priceValue = Wrapper.unwrapped(
                        ScriptableObject.getProperty(object, "price", context));
                if (!(priceValue instanceof Number number)) return null;
                price = number.doubleValue();

                Object currencyValue = Wrapper.unwrapped(
                        ScriptableObject.getProperty(object, "currency", context));
                if (currencyValue != null
                        && currencyValue != Context.getUndefinedValue()
                        && !(currencyValue instanceof Scriptable)) {
                    String returnedCurrency = String.valueOf(currencyValue).trim();
                    if (!returnedCurrency.isEmpty()) resolvedCurrencyId = returnedCurrency;
                }
            } else {
                return null;
            }

            if (!Double.isFinite(price) || price <= 0) return null;
            String resolvedCurrency = resolvedCurrencyId == null || resolvedCurrencyId.isBlank()
                    ? SellBoxConfig.defaultCurrency() : resolvedCurrencyId;
            debugResult(result, price, resolvedCurrency);
            return new PriceQuote(price, resolvedCurrency);
        } catch (Throwable error) {
            System.err.println("[QShop SellBox] Dynamic KubeJS price function failed: "
                    + error.getMessage());
            return null;
        }
    }

    private static void debugInput(Context context, Object event, ItemStack stack) {
        if (!SellBoxConfig.debugDynamicPrice()) return;
        Object item = "<unavailable>";
        Object nbt = "<unavailable>";
        Object rarity = "<unavailable>";
        Object damage = "<unavailable>";
        if (event instanceof Scriptable eventObject) {
            item = ScriptableObject.getProperty(eventObject, "item", context);
            if (item instanceof Scriptable itemObject) {
                nbt = ScriptableObject.getProperty(itemObject, "nbt", context);
                if (nbt instanceof Scriptable nbtObject) {
                    rarity = ScriptableObject.getProperty(nbtObject, "rarity", context);
                    damage = ScriptableObject.getProperty(nbtObject, "Damage", context);
                }
            }
        }
        System.out.println("[QShop SellBox] Dynamic price debug input: id="
                + stack.getItem() + ", tag=" + (ItemStackData.getCustomTag(stack) == null
                ? "<none>" : ItemStackData.getCustomTag(stack))
                + ", itemJsType=" + typeName(item)
                + ", nbtJsType=" + typeName(nbt)
                + ", rarity=" + value(rarity)
                + ", Damage=" + value(damage));
    }

    private static void debugResult(Object result, double price, String currency) {
        if (!SellBoxConfig.debugDynamicPrice()) return;
        System.out.println("[QShop SellBox] Dynamic price debug result: scriptResult="
                + value(result) + ", price=" + price + ", currency=" + currency);
    }

    private static String typeName(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private static String value(Object value) {
        return value == null || value == Context.getUndefinedValue() ? "<undefined>" : String.valueOf(value);
    }

    private static net.minecraft.nbt.CompoundTag parse(String itemId, Object nbt) {
        try {
            if (nbt != null && !(nbt instanceof String)
                    && !(nbt instanceof net.minecraft.nbt.CompoundTag)
                    && !(nbt instanceof com.google.gson.JsonElement)) {
                nbt = GSON.toJsonTree(nbt);
            }
            return NbtStrings.parse(nbt);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid NBT for SellBox item " + itemId, exception);
        }
    }
}
