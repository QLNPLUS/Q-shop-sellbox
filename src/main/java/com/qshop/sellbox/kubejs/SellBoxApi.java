package com.qshop.sellbox.kubejs;

import com.qshop.sellbox.NbtStrings;
import com.qshop.sellbox.PriceQuote;
import com.qshop.sellbox.SellBoxConfig;
import com.qshop.sellbox.SellBoxPrices;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Function;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import dev.latvian.mods.rhino.Wrapper;
import net.minecraft.world.item.ItemStack;

public final class SellBoxApi {
    public static final SellBoxApi INSTANCE = new SellBoxApi();

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
        Context context = Context.enter();
        try {
            Scriptable scope = function.getParentScope();
            Object event = Context.javaToJS(context, new SellBoxPriceEvent(stack), scope);
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
            return new PriceQuote(price, resolvedCurrency);
        } catch (Throwable error) {
            System.err.println("[QShop SellBox] Dynamic KubeJS price function failed: "
                    + error.getMessage());
            return null;
        }
    }

    private static net.minecraft.nbt.CompoundTag parse(String itemId, Object nbt) {
        try {
            if (nbt != null && !(nbt instanceof String)
                    && !(nbt instanceof net.minecraft.nbt.CompoundTag)
                    && !(nbt instanceof com.google.gson.JsonElement)) {
                nbt = dev.latvian.mods.kubejs.util.JsonIO.of(nbt);
            }
            return NbtStrings.parse(nbt);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid NBT for SellBox item " + itemId, exception);
        }
    }
}
